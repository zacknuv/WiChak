package net;

import core.Message;
import core.MessageTree;
import java.util.List;
import java.util.stream.IntStream;

public class MessageTreeMerger {
    public void mergeIfRemoteIsLarger(List<Message> remoteMessages) {
        if (remoteMessages == null || remoteMessages.isEmpty()) {
            return;
        }
        MessageTree messageTree = MessageTree.getInstance();
        synchronized (messageTree) {
            Message[] localMessages = messageTree.toArray();
            if (remoteMessages.size() <= localMessages.length) {
                return;
            }

            boolean[] existsLocally = new boolean[remoteMessages.size()];
            IntStream.range(0, remoteMessages.size()).parallel().forEach(idx -> {
                Message remote = remoteMessages.get(idx);
                for (int i = 0; i < localMessages.length; i++) {
                    if (localMessages[i].getHash() == remote.getHash()) {
                        existsLocally[idx] = true;
                        break;
                    }
                }
            });

            for (int i = 0; i < remoteMessages.size(); i++) {
                if (!existsLocally[i]) {
                    messageTree.insert(remoteMessages.get(i));
                }
            }
        }
    }
}
