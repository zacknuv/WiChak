package net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class UdpEndpoint {
    private static final String BROADCAST_HOST = "255.255.255.255";

    private final int      port;
    private final int      timeoutMillis;
    private DatagramSocket socket;

    public UdpEndpoint(int port, int timeoutMillis) {
        this.port          = port;
        this.timeoutMillis = timeoutMillis;
    }

    public void open() throws SocketException {
        DatagramSocket newSocket = new DatagramSocket(null);
        newSocket.setReuseAddress(true);
        newSocket.setBroadcast(true);
        newSocket.bind(new InetSocketAddress(port));
        newSocket.setSoTimeout(timeoutMillis);
        this.socket = newSocket;
    }

    public DatagramPacket receive(byte[] buffer) throws IOException {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    public void send(byte[] payload, InetAddress address, int targetPort) throws IOException {
        DatagramPacket packet = new DatagramPacket(payload, payload.length, address, targetPort);
        socket.send(packet);
    }

    public void broadcast(byte[] payload) throws IOException {
        send(payload, InetAddress.getByName(BROADCAST_HOST), port);
    }

    public void close() {
        DatagramSocket current = socket;
        if (current != null) {
            current.close();
        }
    }
}
