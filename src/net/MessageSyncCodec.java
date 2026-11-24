package net;

import core.Message;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MessageSyncCodec {
    public static final String REQUEST  = "SYNC_REQUEST";
    public static final String RESPONSE = "SYNC_RESPONSE";

    private static final int MAX_PACKET_SIZE  = 60000;
    private static final int MAX_MESSAGES     = 10000;
    private static final int MAX_STRING_BYTES = 16384;

    public byte[] encodeRequest() {
        return REQUEST.getBytes(StandardCharsets.UTF_8);
    }

    public boolean isRequest(byte[] data, int offset, int length) {
        String text = new String(data, offset, length, StandardCharsets.UTF_8);
        return REQUEST.equals(text);
    }

    public byte[] encodeTree(Message[] messages) throws IOException {
        if (messages.length > MAX_MESSAGES) {
            throw new IOException("Message tree exceeds sync limit");
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeUTF(RESPONSE);
        out.writeInt(messages.length);
        for (int i = 0; i < messages.length; i++) {
            Message message = messages[i];
            writeString(out, message.getWriter());
            writeString(out, message.getContent());
            out.writeLong(message.getTime());
        }
        out.flush();
        byte[] payload = bytes.toByteArray();
        if (payload.length > MAX_PACKET_SIZE) {
            throw new IOException("Message tree is too large to fit in a UDP packet");
        }
        return payload;
    }

    public List<Message> decodeTree(byte[] data, int offset, int length) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data, offset, length));
        String command = in.readUTF();
        if (!RESPONSE.equals(command)) {
            return null;
        }
        int count = in.readInt();
        if (count < 0 || count > MAX_MESSAGES) {
            throw new IOException("Invalid message count in sync response");
        }

        List<Message> messages = new ArrayList<Message>(count);
        for (int i = 0; i < count; i++) {
            String writer  = readString(in);
            String content = readString(in);
            long   time    = in.readLong();
            messages.add(new Message(writer, content, time));
        }
        if (in.available() != 0) {
            throw new IOException("Unexpected trailing data in sync response");
        }
        return messages;
    }

    private void writeString(DataOutputStream out, String value) throws IOException {
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

    private String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length == -1) {
            return null;
        }
        if (length < 0 || length > MAX_STRING_BYTES || length > in.available()) {
            throw new IOException("Invalid string length in sync response");
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
