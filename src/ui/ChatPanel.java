package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class ChatPanel extends JPanel {
    private final JLabel           roomTitle;
    private final JTextArea        conversation;
    private final JTextField       messageInput;
    private final Consumer<String> sendHandler;

    public ChatPanel(Consumer<String> sendHandler) {
        super(new BorderLayout());
        this.sendHandler  = sendHandler;
        this.roomTitle    = new JLabel("Chat");
        this.conversation = new JTextArea();
        this.messageInput = new JTextField();

        setBackground(UiTheme.BACKGROUND);
        add(new TopBarPanel(null, null), BorderLayout.NORTH);
        add(buildChatContent(), BorderLayout.CENTER);
    }

    private JPanel buildChatContent() {
        JPanel chatContent = new JPanel(new BorderLayout());
        chatContent.add(buildHeader(), BorderLayout.NORTH);
        chatContent.add(buildConversationScroll(), BorderLayout.CENTER);
        chatContent.add(buildComposer(), BorderLayout.SOUTH);
        return chatContent;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiTheme.SURFACE);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));
        roomTitle.setFont(UiTheme.HEADER_FONT);
        roomTitle.setForeground(UiTheme.TEXT);
        header.add(roomTitle, BorderLayout.CENTER);
        return header;
    }

    private JScrollPane buildConversationScroll() {
        conversation.setEditable(false);
        conversation.setLineWrap(true);
        conversation.setWrapStyleWord(true);
        conversation.setFont(UiTheme.BODY_FONT);
        conversation.setForeground(UiTheme.TEXT);
        conversation.setBackground(UiTheme.BACKGROUND);
        conversation.setBorder(new EmptyBorder(20, 24, 20, 24));

        JScrollPane scrollPane = new JScrollPane(conversation);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(UiTheme.BACKGROUND);
        return scrollPane;
    }

    private JPanel buildComposer() {
        JPanel composer = new JPanel(new BorderLayout(12, 0));
        composer.setBackground(UiTheme.SURFACE);
        composer.setBorder(new EmptyBorder(16, 20, 16, 20));

        UiTheme.styleComposerInput(
            messageInput,
            "Type a message...",
            "Write a message and press Enter to send"
        );

        JButton sendButton = UiTheme.createPrimaryButton("Send");
        sendButton.setPreferredSize(new Dimension(100, 42));
        sendButton.addActionListener(event -> submitMessage());
        messageInput.addActionListener(event -> submitMessage());

        composer.add(messageInput, BorderLayout.CENTER);
        composer.add(sendButton, BorderLayout.EAST);
        return composer;
    }

    private void submitMessage() {
        String text = messageInput.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        if (sendHandler != null) {
            sendHandler.accept(text);
        }
        messageInput.setText("");
    }

    public void setRoomTitle(String title) {
        roomTitle.setText(title);
    }

    public void setConversationText(String text) {
        if (text.equals(conversation.getText())) {
            return;
        }
        conversation.setText(text);
        conversation.setCaretPosition(conversation.getDocument().getLength());
    }

    public void focusMessageInput() {
        messageInput.requestFocusInWindow();
    }
}
