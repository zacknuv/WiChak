import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodically discovers peers on the local network using UDP port 6969.
 *
 * Peers using this class must use the same wire format: PROBE|name and
 * ALIVE|name. The listening socket responds to probes while the worker
 * broadcasts one every five seconds.
 */
public class UserProbe implements Runnable, AutoCloseable {
    public static final int PORT = 6969;
    public static final long PROBE_INTERVAL_MILLIS = 5_000L;
    private static final int SOCKET_TIMEOUT_MILLIS = 1_000;
    private static final long OFFLINE_AFTER_MILLIS = PROBE_INTERVAL_MILLIS * 3;
    private static final int MAX_PACKET_SIZE = 1_024;

    private final String userName;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<String, Long> lastSeen = new ConcurrentHashMap<>();

    private volatile DatagramSocket socket;
    private volatile Thread worker;

    public UserProbe(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("userName must not be blank");
        }
        this.userName = userName.trim();
    }

    /** Starts the background probe thread. Calling this more than once is safe. */
    public synchronized void start() throws SocketException {
        if (running.get()) {
            return;
        }
        DatagramSocket newSocket = new DatagramSocket(null);
        newSocket.setReuseAddress(true);
        newSocket.setBroadcast(true);
        newSocket.bind(new InetSocketAddress(PORT));
        newSocket.setSoTimeout(SOCKET_TIMEOUT_MILLIS);

        socket = newSocket;
        running.set(true);
        worker = new Thread(this, "user-probe-" + userName);
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public void run() {
        long nextProbeAt = 0L;
        byte[] receiveBuffer = new byte[MAX_PACKET_SIZE];

        try {
            while (running.get()) {
                long now = System.currentTimeMillis();
                if (now >= nextProbeAt) {
                    broadcastProbe();
                    nextProbeAt = now + PROBE_INTERVAL_MILLIS;
                }

                DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                try {
                    socket.receive(packet);
                    handlePacket(packet);
                } catch (SocketTimeoutException ignored) {
                    // The timeout lets this thread periodically check stop state.
                }
                markTimedOutUsersOffline(System.currentTimeMillis());
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
            DatagramSocket currentSocket = socket;
            if (currentSocket != null) {
                currentSocket.close();
            }
        }
    }

    private void broadcastProbe() throws IOException {
        byte[] payload = encode("PROBE|" + userName);
        DatagramPacket packet = new DatagramPacket(
                payload, payload.length, InetAddress.getByName("255.255.255.255"), PORT);
        socket.send(packet);
    }

    private void handlePacket(DatagramPacket packet) throws IOException {
        String message = new String(packet.getData(), packet.getOffset(), packet.getLength(),
                StandardCharsets.UTF_8);
        int separator = message.indexOf('|');
        if (separator < 0) {
            return;
        }

        String type = message.substring(0, separator);
        String peerName = message.substring(separator + 1).trim();
        if (peerName.isEmpty() || peerName.equals(userName)) {
            return;
        }

        String peerAddress = packet.getAddress().getHostAddress();
        if ("PROBE".equals(type)) {
            sendReply(packet.getAddress(), peerName);
            recordPeer(peerName, peerAddress);
        } else if ("ALIVE".equals(type)) {
            recordPeer(peerName, peerAddress);
        }
    }

    private void sendReply(InetAddress address, String requestedBy) throws IOException {
        byte[] payload = encode("ALIVE|" + userName);
        DatagramPacket reply = new DatagramPacket(payload, payload.length, address, PORT);
        socket.send(reply);
    }

    private void recordPeer(String name, String address) {
        String key = key(name, address);
        lastSeen.put(key, System.currentTimeMillis());
        UserBoard userBoard = UserBoard.getInstance();
        synchronized (userBoard) {
            User existing = findUser(userBoard, name, address);
            if (existing == null) {
                existing = new User(name, address);
                userBoard.addUser(existing);
            }
            existing.isActive = true;
        }
    }

    private void markTimedOutUsersOffline(long now) {
        UserBoard userBoard = UserBoard.getInstance();
        synchronized (userBoard) {
            for (User user : userBoard.getUsers()) {
                if (user == null) {
                    continue;
                }
                Long seenAt = lastSeen.get(key(user.getName(), user.getAddress()));
                if (seenAt != null && now - seenAt >= OFFLINE_AFTER_MILLIS) {
                    user.isActive = false;
                }
            }
        }
    }

    private User findUser(UserBoard userBoard, String name, String address) {
        for (User user : userBoard.getUsers()) {
            if (user != null && name.equals(user.getName()) && address.equals(user.getAddress())) {
                return user;
            }
        }
        return null;
    }

    private static String key(String name, String address) {
        return name + '\u0000' + address;
    }

    private static byte[] encode(String message) {
        return message.getBytes(StandardCharsets.UTF_8);
    }

    /** Stops probing and closes the UDP socket. */
    @Override
    public synchronized void close() {
        running.set(false);
        DatagramSocket currentSocket = socket;
        if (currentSocket != null) {
            currentSocket.close();
        }
        Thread currentWorker = worker;
        if (currentWorker != null && currentWorker != Thread.currentThread()) {
            currentWorker.interrupt();
        }
    }
}
