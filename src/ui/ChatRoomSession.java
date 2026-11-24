package ui;

import core.Message;
import core.MessageTree;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.IntStream;

public class ChatRoomSession {
    public String appendMessage(String writer, String content) {
        MessageTree messageTree = MessageTree.getInstance();
        Message     message     = new Message(writer, content, System.currentTimeMillis());
        synchronized (messageTree) {
            messageTree.insert(message);
        }
        return getTranscript();
    }

    public void clear() {
        MessageTree messageTree = MessageTree.getInstance();
        synchronized (messageTree) {
            messageTree.clear();
        }
    }

    public String getTranscript() {
        MessageTree messageTree = MessageTree.getInstance();
        Message[]   snapshot;
        synchronized (messageTree) {
            snapshot = messageTree.toArray();
        }
        if (snapshot.length == 0) {
            return "";
        }

        String[] lines = new String[snapshot.length];
        IntStream.range(0, snapshot.length).parallel().forEach(i -> {
            Message msg       = snapshot[i];
            String  timestamp = new SimpleDateFormat("HH:mm").format(new Date(msg.getTime()));
            String  writer    = msg.getWriter() != null ? msg.getWriter() : "";
            String  content   = msg.getContent() != null ? msg.getContent() : "";
            lines[i] = "[" + timestamp + "]  " + writer + ": " + content;
        });

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            builder.append(lines[i]).append('\n');
        }
        return builder.toString();
    }
}
