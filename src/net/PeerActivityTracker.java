package net;

import core.User;
import core.UserBoard;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerActivityTracker {
    private final long              offlineAfterMillis;
    private final Map<String, Long> lastSeen;

    public PeerActivityTracker(long offlineAfterMillis) {
        this.offlineAfterMillis = offlineAfterMillis;
        this.lastSeen           = new ConcurrentHashMap<String, Long>();
    }

    public void recordPeer(String name, String address, long now) {
        String key = buildKey(name, address);
        lastSeen.put(key, now);
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

    public void markTimedOutUsersOffline(long now) {
        UserBoard userBoard = UserBoard.getInstance();
        synchronized (userBoard) {
            ArrayList<User> users = userBoard.getUsers();
            users.parallelStream().forEach(user -> {
                if (user != null) {
                    Long seenAt = lastSeen.get(buildKey(user.getName(), user.getAddress()));
                    if (seenAt != null && now - seenAt >= offlineAfterMillis) {
                        user.isActive = false;
                    }
                }
            });
        }
    }

    private User findUser(UserBoard userBoard, String name, String address) {
        ArrayList<User> users = userBoard.getUsers();
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            if (user != null && name.equals(user.getName()) && address.equals(user.getAddress())) {
                return user;
            }
        }
        return null;
    }

    private String buildKey(String name, String address) {
        return name + "\u0000" + address;
    }
}
