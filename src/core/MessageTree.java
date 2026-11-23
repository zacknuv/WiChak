public class MessageTree {

    private MessageNode root;
    private int size;

    public MessageTree() {
        this.root = null;
        this.size = 0;
    }

    public void insert(Message message) {
        if (message == null) {
            return;
        }
        MessageNode newNode = new MessageNode(message);
        if (root == null) {
            root = newNode;
            size++;
            return;
        }

        MessageNode current = root;
        while (true) {
            if (message.getTime() < current.getData().getTime()) {
                if (current.getLeft() == null) {
                    current.setLeft(newNode);
                    size++;
                    return;
                }
                current = current.getLeft();
            } else {
                if (current.getRight() == null) {
                    current.setRight(newNode);
                    size++;
                    return;
                }
                current = current.getRight();
            }
        }
    }

    public Message[] toArray() {
        Message[] result = new Message[size];
        fillInOrder(root, result, new int[] { 0 });
        return result;
    }

    private void fillInOrder(MessageNode node, Message[] result, int[] index) {
        if (node == null) {
            return;
        }
        fillInOrder(node.getLeft(), result, index);
        result[index[0]++] = node.getData();
        fillInOrder(node.getRight(), result, index);
    }

    public void clear() {
        root = null;
        size = 0;
    }

    public int getNodeCount() {
        return size;
    }

    public int size() {
        return size;
    }

    public MessageNode getRoot() {
        return root;
    }
}
