package librarysystem.util;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public final class UiUtil {
    private static final Dimension MAIN_WINDOW_SIZE = new Dimension(1400, 860);
    private static final Dimension MAIN_WINDOW_MINIMUM_SIZE = new Dimension(1050, 700);

    public static final Color BACKGROUND = new Color(244, 247, 251);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(30, 41, 59);
    public static final Color MUTED = new Color(100, 116, 139);
    public static final Color BORDER = new Color(226, 232, 240);
    public static final Color PRIMARY = new Color(79, 70, 229);
    public static final Color PRIMARY_DARK = new Color(49, 46, 129);
    public static final Color PRIMARY_SOFT = new Color(238, 242, 255);
    public static final Color ACCENT = new Color(124, 58, 237);
    public static final Color SUCCESS = new Color(5, 150, 105);
    public static final Color WARNING = new Color(217, 119, 6);
    public static final Color DANGER = new Color(220, 38, 38);
    public static final Color DANGER_SOFT = new Color(254, 226, 226);

    private UiUtil() {
    }

    public static Dimension mainWindowSize() {
        return new Dimension(MAIN_WINDOW_SIZE);
    }

    public static Dimension mainWindowMinimumSize() {
        return new Dimension(MAIN_WINDOW_MINIMUM_SIZE);
    }


    private static Rectangle lastWindowBounds;
    private static int lastWindowExtendedState = JFrame.NORMAL;

    /**
     * Initial size for Login/User/Admin windows.
     * Default opening size is 1400 x 860, but the minimum is deliberately smaller
     * so switching Login <-> Dashboard will not force the window to enlarge.
     */
    public static void setupStableMainWindow(JFrame frame) {
        frame.setMinimumSize(mainWindowMinimumSize());
        if (lastWindowBounds == null) {
            frame.setSize(mainWindowSize());
            frame.setLocationRelativeTo(null);
        } else {
            frame.setBounds(lastWindowBounds);
            frame.setExtendedState(lastWindowExtendedState);
        }
    }

    public static void rememberWindowState(JFrame frame) {
        if (frame == null) {
            return;
        }
        lastWindowExtendedState = frame.getExtendedState() & ~Frame.ICONIFIED;
        lastWindowBounds = new Rectangle(frame.getBounds());
    }

    public static void switchFrame(JFrame currentFrame, JFrame nextFrame) {
        replaceWindow(currentFrame, nextFrame);
    }

    public static void installModernLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignoredAgain) {
                // Use Swing default.
            }
        }

        UIManager.put("control", BACKGROUND);
        UIManager.put("info", CARD);
        UIManager.put("nimbusBase", PRIMARY_DARK);
        UIManager.put("nimbusBlueGrey", new Color(203, 213, 225));
        UIManager.put("nimbusFocus", new Color(129, 140, 248));
        UIManager.put("text", TEXT);
        UIManager.put("Table.alternateRowColor", new Color(248, 250, 252));
        UIManager.put("TabbedPane.contentBorderInsets", new java.awt.Insets(0, 0, 0, 0));
        UIManager.put("TabbedPane.tabInsets", new java.awt.Insets(10, 18, 10, 18));
        UIManager.put("OptionPane.background", BACKGROUND);
        UIManager.put("Panel.background", BACKGROUND);
    }

    public static void applyGlobalFont(float size) {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource fontResource) {
                UIManager.put(key, new FontUIResource(fontResource.deriveFont(size)));
            }
        }
    }

    public static void styleFrame(JFrame frame) {
        frame.getContentPane().setBackground(BACKGROUND);
    }

    /**
     * Replace one application window with another without changing the user's current
     * window size, position, or maximized state.
     *
     * This method is used for:
     * - Login -> User dashboard
     * - Login -> Admin dashboard
     * - User/Admin dashboard -> Login
     *
     * The next frame is constructed first but stays invisible, then we copy the current
     * frame state before showing it. This avoids the visible shrink/expand effect.
     */
    public static void replaceWindow(JFrame currentFrame, JFrame nextFrame) {
        if (nextFrame == null) {
            return;
        }
        copyWindowState(currentFrame, nextFrame);
        nextFrame.setVisible(true);
        if (currentFrame != null) {
            currentFrame.dispose();
        }
    }

    /**
     * Copy bounds and maximized state from the current frame to the next frame.
     */
    public static void copyWindowState(JFrame currentFrame, JFrame nextFrame) {
        if (currentFrame == null || nextFrame == null) {
            return;
        }

        int state = currentFrame.getExtendedState() & ~Frame.ICONIFIED;
        Rectangle bounds = new Rectangle(currentFrame.getBounds());
        lastWindowExtendedState = state;
        lastWindowBounds = bounds;

        // Keep minimum size reasonable but do not let it force the window to enlarge.
        nextFrame.setMinimumSize(mainWindowMinimumSize());
        nextFrame.setBounds(bounds);

        if ((state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
            nextFrame.setExtendedState(state);
        } else {
            nextFrame.setExtendedState(Frame.NORMAL);
        }
    }

    public static void applyModernStyle(Component root) {
        if (root == null) {
            return;
        }
        styleSingleComponent(root);
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyModernStyle(child);
            }
        }
    }

    private static void styleSingleComponent(Component component) {
        if (component instanceof GradientPanel) {
            return;
        }
        if (component instanceof JPanel panel) {
            if (SwingUtilities.getAncestorOfClass(GradientPanel.class, panel) != null) {
                panel.setOpaque(false);
            } else {
                panel.setBackground(BACKGROUND);
            }
            if (panel.getBorder() instanceof TitledBorder titledBorder) {
                panel.setBackground(CARD);
                titledBorder.setTitleColor(TEXT);
                titledBorder.setTitleFont(new Font("SansSerif", Font.BOLD, 16));
                titledBorder.setBorder(new LineBorder(BORDER, 1, true));
            }
        }
        if (component instanceof JLabel label) {
            label.setFont(label.getFont().deriveFont(label.getFont().getStyle(), Math.max(15f, label.getFont().getSize2D())));
        }
        if (component instanceof JButton button) {
            styleButton(button);
        }
        if (component instanceof JTextField field) {
            styleTextField(field);
        }
        if (component instanceof JPasswordField field) {
            styleTextField(field);
        }
        if (component instanceof JTextArea area) {
            styleTextArea(area);
        }
        if (component instanceof JComboBox<?> comboBox) {
            styleComboBox(comboBox);
        }
        if (component instanceof JTable table) {
            applyDefaultTableStyle(table);
        }
        if (component instanceof JScrollPane scrollPane) {
            scrollPane.setBorder(new LineBorder(BORDER, 1, true));
            scrollPane.getViewport().setBackground(CARD);
            scrollPane.setBackground(CARD);
        }
        if (component instanceof JTabbedPane tabbedPane) {
            tabbedPane.setFont(tabbedPane.getFont().deriveFont(Font.BOLD, 16f));
            tabbedPane.setBackground(BACKGROUND);
            tabbedPane.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        }
        if (component instanceof JSplitPane splitPane) {
            splitPane.setBorder(BorderFactory.createEmptyBorder());
            splitPane.setDividerSize(8);
            splitPane.setBackground(BACKGROUND);
        }
    }

    public static void styleButton(JButton button) {
        String text = button.getText() == null ? "" : button.getText();
        ButtonTone tone = classifyButton(text);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(button.getFont().deriveFont(Font.BOLD, 15f));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(new CompoundBorder(new LineBorder(tone.border, 1, true), new EmptyBorder(8, 14, 8, 14)));
        button.setBackground(tone.background);
        button.setForeground(tone.foreground);
        if (button.getPreferredSize() == null || button.getPreferredSize().height < 38) {
            button.setPreferredSize(buttonSize());
        }
    }

    private static ButtonTone classifyButton(String text) {
        if (containsAny(text, "登出", "取消", "停權", "退回", "下架", "移出")) {
            return new ButtonTone(DANGER_SOFT, DANGER, new Color(252, 165, 165));
        }
        if (containsAny(text, "登入", "註冊", "新增", "修改", "借閱", "歸還", "啟動", "送出", "核准", "設定", "預約", "加入", "儲存")) {
            return new ButtonTone(PRIMARY, Color.WHITE, PRIMARY);
        }
        return new ButtonTone(CARD, TEXT, BORDER);
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private record ButtonTone(Color background, Color foreground, Color border) {
    }

    public static void styleTextField(JTextField field) {
        field.setFont(field.getFont().deriveFont(16f));
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT);
        field.setCaretColor(PRIMARY);
        field.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(8, 10, 8, 10)));
        Dimension preferred = field.getPreferredSize();
        field.setPreferredSize(new Dimension(Math.max(preferred.width, 120), Math.max(preferred.height, 40)));
    }

    public static void styleTextArea(JTextArea area) {
        area.setFont(area.getFont().deriveFont(16f));
        area.setBackground(Color.WHITE);
        area.setForeground(TEXT);
        area.setCaretColor(PRIMARY);
        area.setBorder(new EmptyBorder(8, 10, 8, 10));
    }

    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(comboBox.getFont().deriveFont(16f));
        comboBox.setBackground(Color.WHITE);
        comboBox.setForeground(TEXT);
        comboBox.setBorder(new LineBorder(BORDER, 1, true));
        Dimension preferred = comboBox.getPreferredSize();
        comboBox.setPreferredSize(new Dimension(Math.max(preferred.width, 120), Math.max(preferred.height, 40)));
    }

    public static void applyDefaultTableStyle(JTable table) {
        table.setRowHeight(40);
        table.setFont(table.getFont().deriveFont(15f));
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setSelectionForeground(TEXT);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setBackground(CARD);
        table.setForeground(TEXT);

        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setFont(header.getFont().deriveFont(Font.BOLD, 15f));
            header.setBackground(new Color(241, 245, 249));
            header.setForeground(TEXT);
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, 42));
            header.setBorder(new LineBorder(BORDER));
        }

        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(86);
        }
    }

    public static void applyOverdueRowHighlight(JTable table, int overdueColumnIndex, String... overdueMarkers) {
        Set<String> markers = new HashSet<>();
        for (String marker : overdueMarkers) {
            if (marker != null) {
                markers.add(marker.trim());
            }
        }
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                boolean overdue = false;
                try {
                    int modelRow = table.convertRowIndexToModel(row);
                    Object markerValue = table.getModel().getValueAt(modelRow, overdueColumnIndex);
                    String markerText = markerValue == null ? "" : markerValue.toString().trim();
                    overdue = markers.contains(markerText) || markerText.contains("逾期");
                } catch (Exception ignored) {
                    overdue = false;
                }

                if (isSelected) {
                    component.setBackground(overdue ? new Color(254, 202, 202) : table.getSelectionBackground());
                    component.setForeground(overdue ? new Color(127, 29, 29) : table.getSelectionForeground());
                } else if (overdue) {
                    component.setBackground(DANGER_SOFT);
                    component.setForeground(new Color(153, 27, 27));
                } else {
                    component.setBackground(row % 2 == 0 ? CARD : new Color(248, 250, 252));
                    component.setForeground(TEXT);
                }
                component.setFont(component.getFont().deriveFont(overdue ? Font.BOLD : Font.PLAIN, 15f));
                if (component instanceof JComponent jComponent) {
                    jComponent.setBorder(new EmptyBorder(0, 8, 0, 8));
                }
                return component;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
    }

    public static JPanel createTitledCard(String title, Component child) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(CARD);
        panel.setBorder(cardBorder());

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(child, BorderLayout.CENTER);
        return panel;
    }

    public static JPanel createCardPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(CARD);
        panel.setBorder(cardBorder());
        return panel;
    }

    public static Border cardBorder() {
        return new CompoundBorder(new LineBorder(new Color(218, 226, 238), 1, true), new EmptyBorder(12, 14, 14, 14));
    }

    public static JLabel createMutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        return label;
    }

    public static void pad(JComponent component) {
        component.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    public static Dimension buttonSize() {
        return new Dimension(150, 42);
    }

    public static class GradientPanel extends JPanel {
        public GradientPanel() {
            super();
            setOpaque(false);
        }

        public GradientPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint paint = new GradientPaint(0, 0, PRIMARY_DARK, getWidth(), getHeight(), ACCENT);
            g2.setPaint(paint);
            g2.fillRoundRect(0, 0, getWidth(), getHeight() + 22, 0, 0);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class SoftCardPanel extends JPanel {
        public SoftCardPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBorder(new EmptyBorder(14, 14, 14, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
            g2.setColor(new Color(218, 226, 238));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
