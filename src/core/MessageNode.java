public class MessageNode {
    private Message     data;
    private MessageNode left;
    private MessageNode right;

    public MessageNode(Message data) {
        this.data  = data;
        this.left  = null;
        this.right = null;
    }

    public Message getData() {
        return data;
    }

    public MessageNode getLeft() {
        return left;
    }

    public void setLeft(MessageNode left) {
        this.left = left;
    }

    public MessageNode getRight() {
        return right;
    }

    public void setRight(MessageNode right) {
        this.right = right;
    }
}
