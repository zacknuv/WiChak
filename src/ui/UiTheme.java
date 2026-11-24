package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class UiTheme {
    public static final Color BACKGROUND = new Color(244, 247, 249);
    public static final Color SURFACE    = Color.WHITE;
    public static final Color ACCENT     = new Color(16, 139, 118);
    public static final Color TEXT       = new Color(34, 48, 56);
    public static final Color MUTED      = new Color(112, 128, 136);
    public static final Color BORDER     = new Color(220, 228, 231);

    public static final Font BRAND_LARGE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 38);
    public static final Font BRAND_BAR_FONT   = new Font(Font.SANS_SERIF, Font.BOLD, 19);
    public static final Font HEADER_FONT      = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    public static final Font TAGLINE_FONT     = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
    public static final Font INPUT_LARGE_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
    public static final Font BODY_FONT        = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    public static final Font BUTTON_FONT      = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    public static final Font LABEL_FONT       = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    public static final Font HINT_FONT        = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

    public static JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(LABEL_FONT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel createFormHint(String text) {
        JLabel hint = new JLabel(text);
        hint.setFont(HINT_FONT);
        hint.setForeground(MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        return hint;
    }

    public static void styleFormInput(JTextField field, String placeholder, String tooltip) {
        field.setPreferredSize(new Dimension(340, 42));
        field.setMinimumSize(new Dimension(340, 42));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setFont(INPUT_LARGE_FONT);
        field.setToolTipText(tooltip);
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 12, 8, 12)
            )
        );
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    public static void styleComposerInput(JTextField field, String placeholder, String tooltip) {
        field.setToolTipText(tooltip);
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.setFont(BODY_FONT);
        field.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(10, 12, 10, 12)
            )
        );
    }

    public static JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setForeground(Color.BLACK);
        button.setBackground(ACCENT);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(12, 18, 12, 18));
        button.setPreferredSize(new Dimension(340, 44));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        return button;
    }
}
