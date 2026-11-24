package ui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.net.SocketException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import net.MessageSync;
import net.UserProbe;

public class ChatApp extends JFrame {
    private static final String CARD_STARTUP          = "startup";
    private static final String CARD_CHAT             = "chat";
    private static final int    REFRESH_INTERVAL_MS   = 250;

    private final CardLayout   cardLayout;
    private final JPanel       cards;
    private final StartupPanel startupPanel;
    private final ChatPanel    chatPanel;
    private final Timer        refreshTimer;

    private String          userName;
    private String          groupName;
    private ChatRoomSession currentRoom;
    private UserProbe       userProbe;
    private MessageSync     messageSync;

    private ChatApp() {
        super("WiChak");

        this.cardLayout   = new CardLayout();
        this.cards        = new JPanel(cardLayout);
        this.userName     = "Guest";
        this.groupName    = "General";
        this.startupPanel = new StartupPanel(this::onJoinGroup);
        this.chatPanel    = new ChatPanel(this::onSendMessage);
        this.refreshTimer = new Timer(REFRESH_INTERVAL_MS, event -> refreshConversation());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 560));
        setSize(960, 680);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UiTheme.BACKGROUND);
        getContentPane().add(cards);

        cards.add(startupPanel, CARD_STARTUP);
        cards.add(chatPanel, CARD_CHAT);
        cardLayout.show(cards, CARD_STARTUP);
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new ChatApp().setVisible(true);
        });
    }

    private void onJoinGroup(String enteredName, String enteredGroup) {
        this.userName    = enteredName;
        this.groupName   = enteredGroup;
        this.currentRoom = new ChatRoomSession();

        startNetworking();

        chatPanel.setRoomTitle(groupName);
        refreshConversation();
        refreshTimer.start();
        cardLayout.show(cards, CARD_CHAT);
        chatPanel.focusMessageInput();
    }

    private void startNetworking() {
        if (userProbe != null) {
            userProbe.close();
        }
        if (messageSync != null) {
            messageSync.close();
        }
        userProbe   = new UserProbe(userName, groupName);
        messageSync = new MessageSync();
        try {
            userProbe.start();
        } catch (SocketException e) {
            System.err.println("Failed to start user probe: " + e.getMessage());
        }
        try {
            messageSync.start();
        } catch (SocketException e) {
            System.err.println("Failed to start message sync: " + e.getMessage());
        }
    }

    private void onSendMessage(String text) {
        if (currentRoom == null) {
            return;
        }
        String transcript = currentRoom.appendMessage(userName, text);
        chatPanel.setConversationText(transcript);
    }

    private void refreshConversation() {
        if (currentRoom == null) {
            return;
        }
        chatPanel.setConversationText(currentRoom.getTranscript());
    }
}
