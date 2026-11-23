#set page(paper: "a4", margin: 2cm)
#set text(font: "Linux Libertine", size: 11pt)

= WiHak Core Documentation

== Message Hashing (`Message.java`)
The `Message` class computes a 64-bit `long` hash directly inside its constructor from scratch without any third-party libraries.

=== Hashing Implementation
Inside `Message(String writer, String content, long time)`, the hash is computed using a 64-bit polynomial rolling hash:
1. The accumulator `h` starts with a large 64-bit prime seed (`1125899906842597L`, which is $2^50 - 27$).
2. Each character of `writer` (if non-null) is folded into `h` via `h = 31L * h + writer.charAt(i)`.
3. Each character of `content` (if non-null) is folded into `h` via `h = 31L * h + content.charAt(i)`.
4. The 64-bit `time` value has its upper and lower 32 bits XOR-folded (`time ^ (time >>> 32)`) and combined into `h`, which is then stored in `this.hash`.

== Message Binary Tree (`MessageNode.java` & `MessageTree.java`)
To keep components separated into distinct classes, tree nodes are represented by `MessageNode` while the tree structure and operations are managed by `MessageTree`.

=== `MessageNode`
Holds a `Message` reference (`data`) along with `left` and `right` child references (`MessageNode`), providing getters and setters for tree traversal and linking.

=== `MessageTree`
An append-only Binary Search Tree (BST) ordered by `Message.getTime()` from smallest to largest:
- `insert(Message message)`: Traverses the tree iteratively from `root`. If the incoming message's `time` is strictly less than the current node's `time`, it branches left; otherwise it branches right. Once an empty child slot is found, it attaches a new `MessageNode` and increments `size`.
- `toArray()`: Allocates a `Message[size]` array and performs an in-order traversal (`fillInOrder`) of the binary search tree to populate and return the messages sorted by `time` from smallest to largest.
- `getNodeCount()` / `size()`: Returns the total number of nodes currently in the tree in $O(1)$ time.
- `getRoot()`: Returns the root `MessageNode` of the tree.
- No deletion methods are exposed, enforcing the insert-only invariant.

== User Tracking (`User.java` & `UserBoard.java`)
=== `User`
Represents a known network peer with a public `isActive` boolean flag, a `name`, and a network `address`. Rather than deleting users, their active status is toggled via `isActive`.

=== `UserBoard`
Maintains an insert-only list of known `User` instances using `ArrayList<User>`:
- `addUser(User user)`: Appends a new `User` to the list.
- `getUsers()` & `toArray()`: Returns the known users as an `ArrayList<User>` or `User[]`.
- `getUserCount()`: Returns the total number of known users.
