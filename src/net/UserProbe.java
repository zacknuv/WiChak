package net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class UserProbe implements Runnable, AutoCloseable {
    public static final int  PORT                  = 6969;
    public static final long PROBE_INTERVAL_MILLIS = 5000L;

    private static final int  SOCKET_TIMEOUT_MILLIS = 1000;
    private static final long OFFLINE_AFTER_MILLIS  = PROBE_INTERVAL_MILLIS * 3;
    private static final int  MAX_PACKET_SIZE       = 1024;

    private final String              userName;
    private final String              groupName;
    private final UdpEndpoint         endpoint;
    private final PeerActivityTracker tracker;
    private final AtomicBoolean       running;
    private volatile Thread           worker;

    public UserProbe(String userName, String groupName) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("userName must not be blank");
        }
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("groupName must not be blank");
        }
        this.userName  = userName.trim();
        this.groupName = groupName.trim();
        this.endpoint  = new UdpEndpoint(PORT, SOCKET_TIMEOUT_MILLIS);
        this.tracker   = new PeerActivityTracker(OFFLINE_AFTER_MILLIS);
        this.running   = new AtomicBoolean(false);
    }

    public String getGroupName() {
        return groupName;
    }

    public synchronized void start() throws SocketException {
        if (running.get()) {
            return;
        }
        endpoint.open();
        running.set(true);
        worker = new Thread(this, "user-probe-" + groupName + "-" + userName);
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public void run() {
        long   nextProbeAt   = 0L;
        byte[] receiveBuffer = new byte[MAX_PACKET_SIZE];
        try {
            while (running.get()) {
                long now = System.currentTimeMillis();
                if (now >= nextProbeAt) {
                    endpoint.broadcast(ProbeFrame.encode(ProbeFrame.TYPE_PROBE, groupName, userName));
                    nextProbeAt = now + PROBE_INTERVAL_MILLIS;
                }
                try {
                    DatagramPacket packet = endpoint.receive(receiveBuffer);
                    handlePacket(packet);
                } catch (SocketTimeoutException ignored) {
                }
                tracker.markTimedOutUsersOffline(System.currentTimeMillis());
            }
        } catch (SocketException e) {
            if (running.get()) {
                System.err.println("User probe socket failed: " + e.getMessage());
            }
        } catch (IOException e) {
            if (running.get()) {
                System.err.println("User probe failed: " + e.getMessage());
            }
        } finally {
            running.set(false);
            endpoint.close();
        }
    }

    private void handlePacket(DatagramPacket packet) throws IOException {
        ProbeFrame frame = ProbeFrame.decode(
            packet.getData(),
            packet.getOffset(),
            packet.getLength(),
            packet.getAddress().getHostAddress()
        );
        if (frame == null) {
            return;
        }
        if (!groupName.equals(frame.getGroupName()) || userName.equals(frame.getUserName())) {
            return;
        }

        long now = System.currentTimeMillis();
        if (ProbeFrame.TYPE_PROBE.equals(frame.getType())) {
            byte[] reply = ProbeFrame.encode(ProbeFrame.TYPE_ALIVE, groupName, userName);
            endpoint.send(reply, packet.getAddress(), PORT);
            tracker.recordPeer(frame.getUserName(), frame.getSenderAddress(), now);
        } else if (ProbeFrame.TYPE_ALIVE.equals(frame.getType())) {
            tracker.recordPeer(frame.getUserName(), frame.getSenderAddress(), now);
        }
    }

    @Override
    public synchronized void close() {
        running.set(false);
        endpoint.close();
        Thread currentWorker = worker;
        if (currentWorker != null && currentWorker != Thread.currentThread()) {
            currentWorker.interrupt();
        }
    }
}
