import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Periodically synchronizes message trees with active peers on UDP port 6767. */
public class MessageSync implements Runnable, AutoCloseable {

    public static final int PORT = 6767;
    public static final long SYNC_INTERVAL_MILLIS = 500L;

    private static final int SOCKET_TIMEOUT_MILLIS = 100;
    private static final int MAX_PACKET_SIZE = 60_000;
    private static final int MAX_MESSAGES = 10_000;
    private static final int MAX_STRING_BYTES = 16_384;
    private static final String REQUEST = "SYNC_REQUEST";
    private static final String RESPONSE = "SYNC_RESPONSE";

    private final MessageTree messageTree;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile DatagramSocket socket;
    private volatile Thread worker;

    public MessageSync(MessageTree messageTree) {
        if (messageTree == null) {
            throw new IllegalArgumentException("messageTree must not be null");
        }
        this.messageTree = messageTree;
    }

    /** Starts the background synchronization thread. */
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
        worker = new Thread(this, "message-sync");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public void run() {
        long nextSyncAt = 0L;
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        try {
            while (running.get()) {
                long now = System.currentTimeMillis();
                if (now >= nextSyncAt) {
                    broadcastSyncRequest();
                    nextSyncAt = now + SYNC_INTERVAL_MILLIS;
                }

                DatagramPacket packet = new DatagramPacket(
                    buffer,
                    buffer.length
                );
                try {
                    socket.receive(packet);
                    handlePacket(packet);
                } catch (SocketTimeoutException ignored) {
                    // Allows timely stop checks between packets.
                }
            }
        } catch (SocketException e) {
            if (running.get()) {
                System.err.println(
                    "Message sync socket failed: " + e.getMessage()
                );
            }
        } catch (IOException e) {
            if (running.get()) {
                System.err.println("Message sync failed: " + e.getMessage());
            }
        } finally {
            running.set(false);
            DatagramSocket currentSocket = socket;
            if (currentSocket != null) {
                currentSocket.close();
            }
        }
    }

    private void broadcastSyncRequest() throws IOException {
        byte[] payload = REQUEST.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(
            payload,
            payload.length,
            java.net.InetAddress.getByName("255.255.255.255"),
            PORT
        );
        socket.send(packet);
    }

    private void handlePacket(DatagramPacket packet) throws IOException {
        String command = new String(
            packet.getData(),
            packet.getOffset(),
            packet.getLength(),
            StandardCharsets.UTF_8
        );
        if (REQUEST.equals(command)) {
            sendTree(packet);
        } else if (command.startsWith(RESPONSE + "\n")) {
            List<Message> remoteMessages = decodeTree(
                packet.getData(),
                packet.getOffset(),
                packet.getLength()
            );
            mergeIfRemoteIsLarger(remoteMessages);
        }
    }

    private void sendTree(DatagramPacket request) throws IOException {
        byte[] payload = encodeTree();
        DatagramPacket response = new DatagramPacket(
            payload,
            payload.length,
            request.getAddress(),
            request.getPort()
        );
        socket.send(response);
    }

    private byte[] encodeTree() throws IOException {
        Message[] messages;
        synchronized (messageTree) {
            messages = messageTree.toArray();
        }
        if (messages.length > MAX_MESSAGES) {
            throw new IOException("Message tree exceeds sync limit");
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeUTF(RESPONSE);
        out.writeInt(messages.length);
        for (Message message : messages) {
            writeString(out, message.getWriter());
            writeString(out, message.getContent());
            out.writeLong(message.getTime());
        }
        out.flush();
        byte[] payload = bytes.toByteArray();
        if (payload.length > MAX_PACKET_SIZE) {
            throw new IOException(
                "Message tree is too large to fit in a UDP packet"
            );
        }
        return payload;
    }

    private List<Message> decodeTree(
        byte[] data,
        int offset,
        int length
    ) throws IOException {
        DataInputStream in = new DataInputStream(
            new ByteArrayInputStream(data, offset, length)
        );
        String command = in.readUTF();
        if (!RESPONSE.equals(command)) {
            throw new IOException("Invalid sync response");
        }
        int count = in.readInt();
        if (count < 0 || count > MAX_MESSAGES) {
            throw new IOException("Invalid message count in sync response");
        }

        List<Message> messages = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String writer = readString(in);
            String content = readString(in);
            long time = in.readLong();
            messages.add(new Message(writer, content, time));
        }
        if (in.available() != 0) {
            throw new IOException("Unexpected trailing data in sync response");
        }
        return messages;
    }

    private void mergeIfRemoteIsLarger(List<Message> remoteMessages) {
        synchronized (messageTree) {
            Message[] localMessages = messageTree.toArray();
            if (remoteMessages.size() <= localMessages.length) {
                return;
            }

            long localHash = combinedHash(Arrays.asList(localMessages));
            long remoteHash = combinedHash(remoteMessages);
            if (localHash == remoteHash) {
                return;
            }

            messageTree.clear();
            for (Message message : remoteMessages) {
                messageTree.insert(message);
            }
        }
    }

    private static long combinedHash(List<Message> messages) {
        List<Message> ordered = new ArrayList<>(messages);
        ordered.sort(
            Comparator.comparingLong(Message::getTime)
                .thenComparing(
                    Message::getWriter,
                    Comparator.nullsFirst(String::compareTo)
                )
                .thenComparing(
                    Message::getContent,
                    Comparator.nullsFirst(String::compareTo)
                )
        );
        long hash = 1L;
        for (Message message : ordered) {
            hash = 31L * hash + message.getHash();
        }
        return hash;
    }

    private static void writeString(
        DataOutputStream out,
        String value
    ) throws IOException {
        if (value == null) {
            out.writeInt(-1);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_BYTES) {
            throw new IOException("Message field exceeds sync limit");
        }
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length == -1) {
            return null;
        }
        if (
            length < 0 || length > MAX_STRING_BYTES || length > in.available()
        ) {
            throw new IOException("Invalid string length in sync response");
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** Stops the background synchronization thread and closes its socket. */
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
