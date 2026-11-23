public class Message {
    private String writer;
    private String content;
    private long   time;

    private long hash;

    public Message(String writer, String content, long time) {
        this.writer  = writer;
        this.content = content;
        this.time    = time;

        long h = 1125899906842597L;
        if (writer != null) {
            for (int i = 0; i < writer.length(); i++) {
                h = 31L * h + writer.charAt(i);
            }
        }
        if (content != null) {
            for (int i = 0; i < content.length(); i++) {
                h = 31L * h + content.charAt(i);
            }
        }
        h = 31L * h + (time ^ (time >>> 32));
        this.hash = h;
    }

    public String getWriter() {
        return writer;
    }

    public String getContent() {
        return content;
    }

    public long getTime() {
        return time;
    }

    public long getHash() {
        return hash;
    }
}
