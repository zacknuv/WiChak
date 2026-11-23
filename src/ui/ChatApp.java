package ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/** Swing user interface for the WiChak local-network chat prototype. */
public class ChatApp extends JFrame {

    private static final Color BACKGROUND = new Color(244, 247, 249);
    private static final Color SURFACE = Color.WHITE;
    private static final Color ACCENT = new Color(16, 139, 118);
    private static final Color TEXT = new Color(34, 48, 56);
    private static final Color MUTED = new Color(112, 128, 136);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private String userName = "Guest";
    private String groupName = "General";

    private JLabel roomTitle;
    private JTextArea conversation;
    private JTextField messageInput;
    private ChatRoom currentRoom;

    private ChatApp() {
        super("WiChak");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 560));
        setSize(960, 680);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);
        getContentPane().add(cards);

        cards.add(createStartupScreen(), "startup");

        cards.add(createChatScreen(), "chat");
        cardLayout.show(cards, "startup");
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {
                // Use the default Swing look and feel if the platform theme is unavailable.
            }
            new ChatApp().setVisible(true);
        });
    }

    private JPanel createStartupScreen() {
        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(BACKGROUND);
        JPanel card = new JPanel();
        card.setBackground(SURFACE);
        card.setBorder(new EmptyBorder(38, 42, 38, 42));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(430, 390));

        JLabel brand = new JLabel("WiChak");
        brand.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 38));
        brand.setForeground(ACCENT);
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel tagline = new JLabel("Chat together, right here.");
        tagline.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        tagline.setForeground(MUTED);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.setMaximumSize(new Dimension(340, 250));
        form.setPreferredSize(new Dimension(340, 250));

        JLabel nameLabel = formLabel("Username");
        JTextField nameField = new JTextField();
        styleInput(
            nameField,
            "e.g. Alex",
            "Enter the name other people will see"
        );
        JLabel nameHint = formHint(
            "This name will appear next to your messages."
        );

        JLabel groupLabel = formLabel("Group");
        JTextField groupField = new JTextField();
        styleInput(
            groupField,
            "e.g. Study group",
            "Enter the group you want to join"
        );
        JLabel groupHint = formHint(
            "Connect to a group on your local network."
        );

        JButton startButton = primaryButton("Join group");
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.setForeground(Color.BLACK);
        startButton.addActionListener(event -> {
            String enteredName = nameField.getText().trim();
            String enteredGroup = groupField.getText().trim();
            if (enteredName.isEmpty() || enteredGroup.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "Enter both a username and a group to continue.",
                    "Information required",
                    JOptionPane.INFORMATION_MESSAGE
                );
                (enteredName.isEmpty()
                    ? nameField
                    : groupField
                ).requestFocusInWindow();
                return;
            }
            setUserName(enteredName);
            groupName = enteredGroup;
            currentRoom = new ChatRoom();
            roomTitle.setText(groupName);
            conversation.setText("");
            cardLayout.show(cards, "chat");
            messageInput.requestFocusInWindow();
        });
        groupField.addActionListener(event -> startButton.doClick());
        nameField.addActionListener(event -> groupField.requestFocusInWindow());

        form.add(nameLabel);
        form.add(Box.createVerticalStrut(6));
        form.add(nameField);
        form.add(Box.createVerticalStrut(5));
        form.add(nameHint);
        form.add(Box.createVerticalStrut(17));
        form.add(groupLabel);
        form.add(Box.createVerticalStrut(6));
        form.add(groupField);
        form.add(Box.createVerticalStrut(5));
        form.add(groupHint);
        form.add(Box.createVerticalStrut(18));
        form.add(startButton);

        card.add(brand);
        card.add(Box.createVerticalStrut(8));
        card.add(tagline);
        card.add(Box.createVerticalStrut(30));
        card.add(form);
        page.add(card);
        return page;
    }

    private JPanel createChatScreen() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BACKGROUND);
        page.add(createTopBar(null, null), BorderLayout.NORTH);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SURFACE);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));
        roomTitle = new JLabel("Chat");
        roomTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        roomTitle.setForeground(TEXT);
        header.add(roomTitle, BorderLayout.CENTER);

        conversation = new JTextArea();
        conversation.setEditable(false);
        conversation.setLineWrap(true);
        conversation.setWrapStyleWord(true);
        conversation.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        conversation.setForeground(TEXT);
        conversation.setBackground(BACKGROUND);
        conversation.setBorder(new EmptyBorder(20, 24, 20, 24));
        JScrollPane conversationScroll = new JScrollPane(conversation);
        conversationScroll.setBorder(null);
        conversationScroll.getViewport().setBackground(BACKGROUND);

        JPanel composer = new JPanel(new BorderLayout(12, 0));
        composer.setBackground(SURFACE);
        composer.setBorder(new EmptyBorder(16, 20, 16, 20));
        messageInput = new JTextField();
        messageInput.setToolTipText("Write a message and press Enter to send");
        messageInput.putClientProperty(
            "JTextField.placeholderText",
            "Type a message..."
        );
        messageInput.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        messageInput.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 228, 231)),
                new EmptyBorder(10, 12, 10, 12)
            )
        );
        JButton sendButton = primaryButton("Send");
        sendButton.setPreferredSize(new Dimension(100, 42));
        sendButton.setForeground(Color.BLACK);
        sendButton.addActionListener(event -> sendMessage());
        messageInput.addActionListener(event -> sendMessage());
        composer.add(messageInput, BorderLayout.CENTER);
        composer.add(sendButton, BorderLayout.EAST);

        JPanel chatContent = new JPanel(new BorderLayout());
        chatContent.add(header, BorderLayout.NORTH);
        chatContent.add(conversationScroll, BorderLayout.CENTER);
        chatContent.add(composer, BorderLayout.SOUTH);
        page.add(chatContent, BorderLayout.CENTER);
        return page;
    }

    private JPanel createTopBar(String title, Runnable backAction) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SURFACE);
        bar.setBorder(new EmptyBorder(14, 24, 14, 24));
        JLabel brand = new JLabel("WiChak");
        brand.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        brand.setForeground(ACCENT);
        bar.add(brand, BorderLayout.WEST);

        if (backAction != null) {
            JButton back = new JButton("‹  Chats");
            back.setForeground(TEXT);
            back.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            back.setFocusPainted(false);
            back.setBorderPainted(false);
            back.setContentAreaFilled(false);
            back.addActionListener(event -> backAction.run());
            bar.add(back, BorderLayout.CENTER);
        } else if (title != null) {
            JLabel welcome = new JLabel(title);
            welcome.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            welcome.setForeground(MUTED);
            welcome.setHorizontalAlignment(SwingConstants.RIGHT);
            bar.add(welcome, BorderLayout.EAST);
        }
        return bar;
    }

    private void sendMessage() {
        String text = messageInput.getText().trim();
        if (text.isEmpty() || currentRoom == null) {
            return;
        }
        String timestamp = new SimpleDateFormat("HH:mm").format(new Date());
        String line = "[" + timestamp + "]  " + userName + ": " + text;
        currentRoom.messages.append(line).append('\n');
        conversation.setText(currentRoom.messages.toString());
        conversation.setCaretPosition(conversation.getDocument().getLength());
        messageInput.setText("");
    }

    private JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel formHint(String text) {
        JLabel hint = new JLabel(text);
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        hint.setForeground(MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        return hint;
    }

    private void styleInput(
        JTextField field,
        String placeholder,
        String tooltip
    ) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        field.setToolTipText(tooltip);
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 228, 231)),
                new EmptyBorder(8, 12, 8, 12)
            )
        );
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void setUserName(String name) {
        userName = name;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(ACCENT);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(12, 18, 12, 18));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        return button;
    }

    private static final class ChatRoom {

        private final StringBuilder messages = new StringBuilder();
    }
}
