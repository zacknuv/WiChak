package ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.BiConsumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class StartupPanel extends JPanel {
    private final JTextField                 nameField;
    private final JTextField                 groupField;
    private final BiConsumer<String, String> joinHandler;

    public StartupPanel(BiConsumer<String, String> joinHandler) {
        super(new GridBagLayout());
        this.joinHandler = joinHandler;
        this.nameField   = new JTextField();
        this.groupField  = new JTextField();

        setBackground(UiTheme.BACKGROUND);
        add(buildCard());
    }

    private JPanel buildCard() {
        JPanel card = new JPanel();
        card.setBackground(UiTheme.SURFACE);
        card.setBorder(new EmptyBorder(38, 42, 38, 42));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("WiChak");
        brand.setFont(UiTheme.BRAND_LARGE_FONT);
        brand.setForeground(UiTheme.ACCENT);
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tagline = new JLabel("Chat together, right here.");
        tagline.setFont(UiTheme.TAGLINE_FONT);
        tagline.setForeground(UiTheme.MUTED);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel form = buildForm();
        form.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(brand);
        card.add(Box.createVerticalStrut(8));
        card.add(tagline);
        card.add(Box.createVerticalStrut(28));
        card.add(form);
        return card;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        JLabel nameLabel = UiTheme.createFormLabel("Username");
        UiTheme.styleFormInput(nameField, "e.g. Alex", "Enter the name other people will see");
        JLabel nameHint = UiTheme.createFormHint("This name will appear next to your messages.");

        JLabel groupLabel = UiTheme.createFormLabel("Group");
        UiTheme.styleFormInput(groupField, "e.g. Study group", "Enter the group you want to join");
        JLabel groupHint = UiTheme.createFormHint("Connect to a group on your local network.");

        JButton startButton = UiTheme.createPrimaryButton("Join group");
        startButton.addActionListener(event -> handleJoin());

        groupField.addActionListener(event -> startButton.doClick());
        nameField.addActionListener(event -> groupField.requestFocusInWindow());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx   = 0;
        gbc.weightx = 1.0;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.anchor  = GridBagConstraints.WEST;

        gbc.gridy  = 0;
        gbc.insets = new Insets(0, 0, 6, 0);
        form.add(nameLabel, gbc);

        gbc.gridy  = 1;
        gbc.insets = new Insets(0, 0, 5, 0);
        form.add(nameField, gbc);

        gbc.gridy  = 2;
        gbc.insets = new Insets(0, 0, 18, 0);
        form.add(nameHint, gbc);

        gbc.gridy  = 3;
        gbc.insets = new Insets(0, 0, 6, 0);
        form.add(groupLabel, gbc);

        gbc.gridy  = 4;
        gbc.insets = new Insets(0, 0, 5, 0);
        form.add(groupField, gbc);

        gbc.gridy  = 5;
        gbc.insets = new Insets(0, 0, 22, 0);
        form.add(groupHint, gbc);

        gbc.gridy  = 6;
        gbc.insets = new Insets(0, 0, 0, 0);
        form.add(startButton, gbc);

        return form;
    }

    private void handleJoin() {
        String enteredName  = nameField.getText().trim();
        String enteredGroup = groupField.getText().trim();
        if (enteredName.isEmpty() || enteredGroup.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Enter both a username and a group to continue.",
                "Information required",
                JOptionPane.INFORMATION_MESSAGE
            );
            (enteredName.isEmpty() ? nameField : groupField).requestFocusInWindow();
            return;
        }
        if (joinHandler != null) {
            joinHandler.accept(enteredName, enteredGroup);
        }
    }
}
