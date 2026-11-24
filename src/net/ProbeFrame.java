package net;

import java.nio.charset.StandardCharsets;

public class ProbeFrame {
    public static final String TYPE_PROBE = "PROBE";
    public static final String TYPE_ALIVE = "ALIVE";

    private final String type;
    private final String groupName;
    private final String userName;
    private final String senderAddress;

    public ProbeFrame(String type, String groupName, String userName, String senderAddress) {
        this.type          = type;
        this.groupName     = groupName;
        this.userName      = userName;
        this.senderAddress = senderAddress;
    }

    public String getType() {
        return type;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getUserName() {
        return userName;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public static byte[] encode(String type, String groupName, String userName) {
        String raw = type + "|" + groupName + "|" + userName;
        return raw.getBytes(StandardCharsets.UTF_8);
    }

    public static ProbeFrame decode(byte[] data, int offset, int length, String senderAddress) {
        String message = new String(data, offset, length, StandardCharsets.UTF_8);
        int firstSep = message.indexOf('|');
        if (firstSep < 0) {
            return null;
        }
        int secondSep = message.indexOf('|', firstSep + 1);
        if (secondSep < 0) {
            return null;
        }
        String type      = message.substring(0, firstSep);
        String groupName = message.substring(firstSep + 1, secondSep).trim();
        String peerName  = message.substring(secondSep + 1).trim();
        if (groupName.isEmpty() || peerName.isEmpty()) {
            return null;
        }
        return new ProbeFrame(type, groupName, peerName, senderAddress);
    }
}
