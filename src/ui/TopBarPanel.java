package ui;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class TopBarPanel extends JPanel {
    public TopBarPanel(String title, Runnable backAction) {
        super(new BorderLayout());
        setBackground(UiTheme.SURFACE);
        setBorder(new EmptyBorder(14, 24, 14, 24));

        JLabel brand = new JLabel("WiChak");
        brand.setFont(UiTheme.BRAND_BAR_FONT);
        brand.setForeground(UiTheme.ACCENT);
        add(brand, BorderLayout.WEST);

        if (backAction != null) {
            JButton back = new JButton("\u2039  Chats");
            back.setForeground(UiTheme.TEXT);
            back.setFont(UiTheme.BODY_FONT);
            back.setFocusPainted(false);
            back.setBorderPainted(false);
            back.setContentAreaFilled(false);
            back.addActionListener(event -> backAction.run());
            add(back, BorderLayout.CENTER);
        } else if (title != null) {
            JLabel welcome = new JLabel(title);
            welcome.setFont(UiTheme.BODY_FONT);
            welcome.setForeground(UiTheme.MUTED);
            welcome.setHorizontalAlignment(SwingConstants.RIGHT);
            add(welcome, BorderLayout.EAST);
        }
    }
}
