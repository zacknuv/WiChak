package net;

import core.Message;
import core.MessageTree;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageSync implements Runnable, AutoCloseable {
    public static final int  PORT                 = 6767;
    public static final long SYNC_INTERVAL_MILLIS = 500L;

    private static final int SOCKET_TIMEOUT_MILLIS = 100;
    private static final int MAX_PACKET_SIZE       = 60000;

    private final UdpEndpoint       endpoint;
    private final MessageSyncCodec  codec;
    private final MessageTreeMerger merger;
    private final AtomicBoolean     running;
    private volatile Thread         worker;

    public MessageSync() {
        this.endpoint = new UdpEndpoint(PORT, SOCKET_TIMEOUT_MILLIS);
        this.codec    = new MessageSyncCodec();
        this.merger   = new MessageTreeMerger();
        this.running  = new AtomicBoolean(false);
    }

    public synchronized void start() throws SocketException {
        if (running.get()) {
            return;
        }
        endpoint.open();
        running.set(true);
        worker = new Thread(this, "message-sync");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public void run() {
        long   nextSyncAt = 0L;
        byte[] buffer     = new byte[MAX_PACKET_SIZE];
        try {
            while (running.get()) {
                long now = System.currentTimeMillis();
                if (now >= nextSyncAt) {
                    endpoint.broadcast(codec.encodeRequest());
                    nextSyncAt = now + SYNC_INTERVAL_MILLIS;
                }
                try {
                    DatagramPacket packet = endpoint.receive(buffer);
                    handlePacket(packet);
                } catch (SocketTimeoutException ignored) {
                }
            }
        } catch (SocketException e) {
            if (running.get()) {
                System.err.println("Message sync socket failed: " + e.getMessage());
            }
        } catch (IOException e) {
            if (running.get()) {
                System.err.println("Message sync failed: " + e.getMessage());
            }
        } finally {
            running.set(false);
            endpoint.close();
        }
    }

    private void handlePacket(DatagramPacket packet) throws IOException {
        byte[] data   = packet.getData();
        int    offset = packet.getOffset();
        int    length = packet.getLength();

        if (codec.isRequest(data, offset, length)) {
            MessageTree messageTree = MessageTree.getInstance();
            Message[] snapshot;
            synchronized (messageTree) {
                snapshot = messageTree.toArray();
            }
            byte[] payload = codec.encodeTree(snapshot);
            endpoint.send(payload, packet.getAddress(), packet.getPort());
            return;
        }

        List<Message> remoteMessages = codec.decodeTree(data, offset, length);
        if (remoteMessages != null) {
            merger.mergeIfRemoteIsLarger(remoteMessages);
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
