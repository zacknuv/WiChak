#set page(paper: "a4", margin: 2cm)
#set text(font: "Linux Libertine", size: 11pt)

= WiHak Core, Networking & UI Documentation

== Message Hashing (`core/Message.java`)
The `Message` class computes a 64-bit `long` hash directly inside its constructor from scratch without any third-party libraries.

=== Hashing Implementation
Inside `Message(String writer, String content, long time)`, the hash is computed using a 64-bit polynomial rolling hash:
1. The accumulator `h` starts with a large 64-bit prime seed (`1125899906842597L`, which is $2^50 - 27$).
2. Each character of `writer` (if non-null) is folded into `h` via `h = 31L * h + writer.charAt(i)`.
3. Each character of `content` (if non-null) is folded into `h` via `h = 31L * h + content.charAt(i)`.
4. The 64-bit `time` value has its upper and lower 32 bits XOR-folded (`time ^ (time >>> 32)`) and combined into `h`, which is then stored in `this.hash`.

== Message Binary Tree (`core/MessageNode.java` & `core/MessageTree.java`)
To keep components separated into distinct classes, tree nodes are represented by `MessageNode` while the tree structure and operations are managed by the singleton `MessageTree`.

=== `MessageNode`
Holds a `Message` reference (`data`) along with `left` and `right` child references (`MessageNode`), providing getters and setters for tree traversal and linking.

=== `MessageTree`
A singleton Binary Search Tree (BST) accessed via `MessageTree.getInstance()` and ordered by `Message.getTime()` from smallest to largest:
- `getInstance()`: Returns the single shared `MessageTree` instance.
- `insert(Message message)`: Traverses the tree iteratively from `root`. If the incoming message's `time` is strictly less than the current node's `time`, it branches left; otherwise it branches right. Once an empty child slot is found, it attaches a new `MessageNode` and increments `size`.
- `toArray()`: Allocates a `Message[size]` array and performs an in-order traversal (`fillInOrder`) of the binary search tree to populate and return the messages sorted by `time` from smallest to largest.
- `clear()`: Resets `root` to `null` and `size` to `0`.
- `getNodeCount()` / `size()`: Returns the total number of nodes currently in the tree in $O(1)$ time.
- `getRoot()`: Returns the root `MessageNode` of the tree.

== User Tracking (`core/User.java` & `core/UserBoard.java`)
=== `User`
Represents a known network peer with a public `isActive` boolean flag, a `name`, and a network `address`. Rather than deleting users, their active status is toggled via `isActive`.

=== `UserBoard`
A singleton accessed via `UserBoard.getInstance()` that maintains an insert-only list of known `User` instances using `ArrayList<User>`:
- `addUser(User user)`: Appends a new `User` to the list.
- `getUsers()` & `toArray()`: Returns the known users as an `ArrayList<User>` or `User[]`.
- `getUserCount()`: Returns the total number of known users.

== UDP Networking (`src/net/`)
The networking layer separates transport, wire encoding/decoding, peer state tracking, and tree synchronization into dedicated single-responsibility classes.

=== `UdpEndpoint`
Encapsulates `DatagramSocket` lifecycle and UDP packet I/O:
- Configures address reuse (`SO_REUSEADDR`), broadcast mode, port binding, and receive timeout (`SO_TIMEOUT`).
- Provides `receive(byte[])`, unicast `send(byte[], InetAddress, int)`, subnet `broadcast(byte[])` to `255.255.255.255`, and `close()`.

=== `MessageSyncCodec`
Handles the binary wire format for `MessageTree` synchronization packets:
- `encodeRequest()` & `isRequest(...)`: Serializes and detects `SYNC_REQUEST` UDP datagrams.
- `encodeTree(Message[])`: Serializes an array of `Message` objects prefixed by the `SYNC_RESPONSE` header and message count.
- `decodeTree(byte[], int, int)`: Validates the `SYNC_RESPONSE` UTF header via `DataInputStream.readUTF()` and reconstructs the `List<Message>`.

=== `MessageTreeMerger`
Synchronizes remote messages into the singleton `MessageTree.getInstance()`:
- Parallelizes the membership check across `remoteMessages` using `IntStream.range(0, remoteMessages.size()).parallel()`, comparing each remote message's 64-bit `hash` and `time` against the local snapshot in parallel, and inserts only the missing messages into `MessageTree.getInstance()`.

=== `MessageSync`
Coordinates periodic broadcast sync requests and incoming sync responses on UDP port `6767` by composing `UdpEndpoint`, `MessageSyncCodec`, and `MessageTreeMerger` against `MessageTree.getInstance()`.

=== `ProbeFrame`
Models and serializes/deserializes the group-scoped UDP discovery wire format: `PROBE|<groupName>|<userName>` and `ALIVE|<groupName>|<userName>`.

=== `PeerActivityTracker`
Manages peer liveness state against `UserBoard.getInstance()`:
- `recordPeer(String name, String address, long now)`: Updates the last-seen timestamp in a `ConcurrentHashMap`, inserts the `User` into `UserBoard.getInstance()` if not already known, and marks `user.isActive = true`.
- `markTimedOutUsersOffline(long now)`: Evaluates known users in parallel (`users.parallelStream()`) and sets `user.isActive = false` for any peer whose elapsed time since `lastSeen` exceeds the timeout threshold.

=== `UserProbe`
Coordinates periodic peer discovery broadcasts and liveness checks on UDP port `6969` for a specific `(userName, groupName)` pair:
- Broadcasts `PROBE|<groupName>|<userName>` across the local subnet every 5 seconds.
- Filters incoming `PROBE` and `ALIVE` frames in `handlePacket`, ignoring any packet whose `groupName` does not match the local `groupName` (or whose `userName` matches the local user).
- Replies with `ALIVE|<groupName>|<userName>` and records active peers in `UserBoard.getInstance()` via `PeerActivityTracker` only when both devices belong to the same group.

== Swing User Interface (`src/ui/`)
The Swing UI layer decomposes the formerly monolithic `ChatApp` frame into dedicated visual and session classes with zero comments:

=== `UiTheme`
Centralizes all Swing color palettes (`BACKGROUND`, `SURFACE`, `ACCENT`, `TEXT`, `MUTED`, `BORDER`), typography constants, and factory/styling helpers (`createFormLabel`, `createFormHint`, `styleFormInput`, `styleComposerInput`, `createPrimaryButton`).

=== `TopBarPanel`
A reusable `JPanel` top navigation bar displaying the `WiChak` brand label, optional back button action, or right-aligned welcome title.

=== `StartupPanel`
A `JPanel` encapsulating the login card (`Username` and `Group` fields, validation dialog, and Enter-key focus/submit bindings). Uses a single-column `GridBagLayout` with `GridBagConstraints.HORIZONTAL` and explicit row `Insets` so labels, text fields, hints, and the primary button all share a uniform left edge and full 340px width without clipping or `BoxLayout` mixed-alignment shift.

=== `ChatPanel`
A `JPanel` encapsulating the chat room view: `TopBarPanel`, room header title, scrollable read-only `JTextArea` conversation view, and bottom message composer bar (`JTextField` and `Send` button). Invokes a `Consumer<String>` callback when a message is submitted, and avoids redundant caret resets in `setConversationText` when the rendered transcript has not changed.

=== `ChatRoomSession`
Bridges the group chat UI directly to `MessageTree.getInstance()`:
- `appendMessage(String writer, String content)`: Instantiates a new `Message(writer, content, System.currentTimeMillis())`, inserts it into `MessageTree.getInstance()` under synchronization, and returns the updated transcript.
- `getTranscript()`: Reads the sorted `Message[]` snapshot from `MessageTree.getInstance().toArray()`, formats each message line (`[HH:mm]  writer: content`) in parallel across indices using `IntStream.range(0, snapshot.length).parallel()`, and joins them in chronological order.

=== `ChatApp`
Top-level `JFrame` controller that hosts `StartupPanel` and `ChatPanel` inside a `CardLayout`:
- When the user joins a group (`onJoinGroup`), starts `UserProbe(userName, groupName)` and `MessageSync()` and runs a Swing `Timer` (`250ms`) to refresh the `ChatPanel` conversation view from `ChatRoomSession.getTranscript()` whenever `MessageTree.getInstance()` receives new messages.
