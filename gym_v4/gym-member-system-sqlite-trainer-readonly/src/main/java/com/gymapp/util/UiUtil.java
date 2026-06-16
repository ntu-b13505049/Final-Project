package com.gymapp.util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.sql.SQLException;
import java.util.Locale;

public final class UiUtil {
    public static final float BASE_FONT_SIZE = 16f;
    public static final Dimension DEFAULT_WINDOW_SIZE = new Dimension(1180, 760);
    public static final Dimension MIN_WINDOW_SIZE = new Dimension(1000, 680);

    public static final Color APP_BG = new Color(245, 247, 251);
    public static final Color SURFACE = Color.WHITE;
    public static final Color SURFACE_ALT = new Color(249, 250, 252);
    public static final Color PRIMARY = new Color(37, 99, 235);
    public static final Color PRIMARY_DARK = new Color(29, 78, 216);
    public static final Color PRIMARY_SOFT = new Color(232, 240, 255);
    public static final Color DANGER = new Color(220, 38, 38);
    public static final Color DANGER_DARK = new Color(185, 28, 28);
    public static final Color BORDER = new Color(220, 226, 235);
    public static final Color TEXT = new Color(17, 24, 39);
    public static final Color MUTED = new Color(107, 114, 128);
    public static final Color TABLE_ALT_ROW = new Color(248, 250, 252);
    public static final Color TABLE_SELECTION = new Color(219, 234, 254);

    private UiUtil() {}

    public static void applyModernTheme() {
        UIManager.put("control", APP_BG);
        UIManager.put("info", SURFACE);
        UIManager.put("nimbusBase", PRIMARY_DARK);
        UIManager.put("nimbusBlueGrey", new Color(203, 213, 225));
        UIManager.put("nimbusFocus", PRIMARY);
        UIManager.put("nimbusLightBackground", SURFACE);
        UIManager.put("text", TEXT);
        UIManager.put("Panel.background", APP_BG);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Viewport.background", SURFACE);
        UIManager.put("ScrollPane.background", SURFACE);
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(BORDER));
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.alternateRowColor", TABLE_ALT_ROW);
        UIManager.put("Table.selectionBackground", TABLE_SELECTION);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.gridColor", new Color(232, 236, 242));
        UIManager.put("TableHeader.background", PRIMARY);
        UIManager.put("TableHeader.foreground", Color.WHITE);
        UIManager.put("TabbedPane.background", APP_BG);
        UIManager.put("TabbedPane.contentAreaColor", APP_BG);
        UIManager.put("TabbedPane.selected", SURFACE);
        UIManager.put("TitledBorder.titleColor", PRIMARY_DARK);
        UIManager.put("OptionPane.background", APP_BG);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("TextField.caretForeground", PRIMARY);
        UIManager.put("PasswordField.caretForeground", PRIMARY);
        UIManager.put("TextArea.caretForeground", PRIMARY);
    }

    public static void applyGlobalFont(float size) {
        String[] preferred = {"Microsoft JhengHei UI", "Microsoft JhengHei", "Noto Sans CJK TC", "PingFang TC", "Dialog"};
        String family = chooseInstalledFont(preferred);
        UIDefaults defaults = UIManager.getDefaults();
        for (Object key : defaults.keySet().toArray()) {
            Object value = defaults.get(key);
            if (value instanceof FontUIResource font) {
                defaults.put(key, new FontUIResource(family, font.getStyle(), Math.round(size)));
            } else if (value instanceof Font font) {
                defaults.put(key, new Font(family, font.getStyle(), Math.round(size)));
            }
        }
        UIManager.put("Table.rowHeight", Math.max(34, Math.round(size * 2.05f)));
    }

    private static String chooseInstalledFont(String[] preferred) {
        java.util.Set<String> installed = new java.util.HashSet<>();
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            installed.add(name.toLowerCase(Locale.ROOT));
        }
        for (String name : preferred) {
            if (installed.contains(name.toLowerCase(Locale.ROOT))) {
                return name;
            }
        }
        return Font.DIALOG;
    }

    public static void prepareFrame(JFrame frame, Rectangle bounds, int extendedState) {
        frame.setMinimumSize(MIN_WINDOW_SIZE);
        if (bounds != null && bounds.width >= MIN_WINDOW_SIZE.width && bounds.height >= MIN_WINDOW_SIZE.height) {
            frame.setBounds(bounds);
        } else {
            frame.setSize(DEFAULT_WINDOW_SIZE);
            frame.setLocationRelativeTo(null);
        }
        frame.setExtendedState(extendedState);
        SwingUtilities.invokeLater(() -> polishWindow(frame));
    }

    public static void polishWindow(Window window) {
        if (window == null) {
            return;
        }
        if (window instanceof JFrame frame && frame.getJMenuBar() != null) {
            polishTree(frame.getJMenuBar());
        }
        polishTree(window);
        window.validate();
        window.repaint();
    }

    public static void polishTree(Component component) {
        if (component == null) {
            return;
        }
        polishComponent(component);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                polishTree(child);
            }
        }
    }

    private static void polishComponent(Component component) {
        if (component instanceof JPanel panel) {
            if (panel.getBorder() instanceof TitledBorder || hasTitledBorder(panel.getBorder())) {
                panel.setBackground(SURFACE);
                panel.setBorder(polishedBorder(panel.getBorder()));
            } else if (!(component instanceof JViewport)
                    && !SURFACE.equals(panel.getBackground())
                    && !Color.WHITE.equals(panel.getBackground())
                    && !PRIMARY.equals(panel.getBackground())) {
                panel.setBackground(APP_BG);
            }
        }
        if (component instanceof JScrollPane scrollPane) {
            scrollPane.setBackground(SURFACE);
            scrollPane.getViewport().setBackground(SURFACE);
            scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
            scrollPane.getVerticalScrollBar().setUnitIncrement(18);
            scrollPane.getHorizontalScrollBar().setUnitIncrement(18);
        }
        if (component instanceof JButton button) {
            styleButton(button);
        }
        if (component instanceof JTextField field) {
            styleInput(field);
        }
        if (component instanceof JPasswordField field) {
            styleInput(field);
        }
        if (component instanceof JTextArea area) {
            styleTextArea(area);
        }
        if (component instanceof JComboBox<?> combo) {
            styleCombo(combo);
        }
        if (component instanceof JLabel label) {
            if (label.getForeground() == null || Color.BLACK.equals(label.getForeground())) {
                label.setForeground(TEXT);
            }
        }
        if (component instanceof JTable table) {
            styleTable(table);
        }
        if (component instanceof JTabbedPane tabs) {
            styleTabs(tabs);
        }
        if (component instanceof JSplitPane split) {
            split.setBorder(BorderFactory.createEmptyBorder());
            split.setDividerSize(8);
            split.setBackground(APP_BG);
        }
    }

    private static boolean hasTitledBorder(Border border) {
        if (border instanceof TitledBorder) {
            return true;
        }
        if (border instanceof CompoundBorder compound) {
            return hasTitledBorder(compound.getOutsideBorder()) || hasTitledBorder(compound.getInsideBorder());
        }
        return false;
    }

    private static Border polishedBorder(Border current) {
        TitledBorder titled = findTitledBorder(current);
        if (titled == null) {
            return current;
        }
        TitledBorder titleBorder = BorderFactory.createTitledBorder(
                new LineBorder(BORDER, 1, true),
                "  " + titled.getTitle().trim() + "  ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UIManager.getFont("Label.font").deriveFont(Font.BOLD, BASE_FONT_SIZE + 0.5f),
                PRIMARY_DARK
        );
        return new CompoundBorder(titleBorder, new EmptyBorder(8, 10, 10, 10));
    }

    private static TitledBorder findTitledBorder(Border border) {
        if (border instanceof TitledBorder titled) {
            return titled;
        }
        if (border instanceof CompoundBorder compound) {
            TitledBorder outside = findTitledBorder(compound.getOutsideBorder());
            return outside != null ? outside : findTitledBorder(compound.getInsideBorder());
        }
        return null;
    }

    private static void styleTabs(JTabbedPane tabs) {
        if (Boolean.TRUE.equals(tabs.getClientProperty("gymapp.tabsStyled"))) {
            return;
        }
        tabs.putClientProperty("gymapp.tabsStyled", Boolean.TRUE);
        tabs.setFont(tabs.getFont().deriveFont(Font.BOLD, BASE_FONT_SIZE));
        tabs.setBackground(APP_BG);
        tabs.setForeground(TEXT);
        tabs.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    public static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        return label;
    }

    public static JTextField field(int columns) {
        JTextField field = new JTextField(columns);
        styleInput(field);
        return field;
    }

    public static JPasswordField passwordField(int columns) {
        JPasswordField field = new JPasswordField(columns);
        styleInput(field);
        return field;
    }

    public static JButton button(String text) {
        JButton button = new JButton(text);
        styleButton(button);
        return button;
    }

    public static JPanel cardPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(SURFACE);
        panel.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(14, 14, 14, 14)));
        return panel;
    }

    public static JPanel toolbarPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        panel.setBackground(SURFACE);
        panel.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(6, 8, 6, 8)));
        return panel;
    }

    public static Border titledCardBorder(String title) {
        TitledBorder titled = BorderFactory.createTitledBorder(
                new LineBorder(BORDER, 1, true),
                "  " + title + "  ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UIManager.getFont("Label.font").deriveFont(Font.BOLD, BASE_FONT_SIZE + 0.5f),
                PRIMARY_DARK
        );
        return new CompoundBorder(titled, new EmptyBorder(8, 10, 10, 10));
    }

    public static void styleButton(JButton button) {
        if (button == null) {
            return;
        }
        String text = button.getText() == null ? "" : button.getText();
        Color bg;
        Color hover;
        Color fg;
        if (containsAny(text, "刪除", "取消", "登出", "清空")) {
            bg = DANGER;
            hover = DANGER_DARK;
            fg = Color.WHITE;
        } else if (containsAny(text, "登入", "新增", "儲存", "預約", "購買", "銷售", "進場", "建立", "完成維護", "依方案", "自訂儲值")) {
            bg = PRIMARY;
            hover = PRIMARY_DARK;
            fg = Color.WHITE;
        } else {
            bg = SURFACE;
            hover = PRIMARY_SOFT;
            fg = PRIMARY_DARK;
        }
        button.setForeground(fg);
        button.setBackground(bg);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(new CompoundBorder(new LineBorder(bg.equals(SURFACE) ? BORDER : bg, 1, true), new EmptyBorder(8, 14, 8, 14)));
        button.setFont(button.getFont().deriveFont(Font.BOLD, BASE_FONT_SIZE));
        installHover(button, bg, hover, fg);
    }

    private static void installHover(JButton button, Color normal, Color hover, Color fg) {
        if (Boolean.TRUE.equals(button.getClientProperty("gymapp.hoverInstalled"))) {
            return;
        }
        button.putClientProperty("gymapp.hoverInstalled", Boolean.TRUE);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hover);
                    button.setForeground(fg.equals(PRIMARY_DARK) ? PRIMARY_DARK : Color.WHITE);
                    button.setBorder(new CompoundBorder(new LineBorder(hover.equals(PRIMARY_SOFT) ? new Color(191, 219, 254) : hover, 1, true), new EmptyBorder(8, 14, 8, 14)));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(normal);
                button.setForeground(fg);
                button.setBorder(new CompoundBorder(new LineBorder(normal.equals(SURFACE) ? BORDER : normal, 1, true), new EmptyBorder(8, 14, 8, 14)));
            }
        });
    }

    private static void styleInput(JTextField field) {
        field.setBackground(SURFACE);
        field.setForeground(TEXT);
        field.setSelectionColor(TABLE_SELECTION);
        field.setSelectedTextColor(TEXT);
        field.setCaretColor(PRIMARY);
        field.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(7, 10, 7, 10)));
    }

    private static void styleTextArea(JTextArea area) {
        area.setBackground(SURFACE);
        area.setForeground(TEXT);
        area.setSelectionColor(TABLE_SELECTION);
        area.setSelectedTextColor(TEXT);
        area.setCaretColor(PRIMARY);
        if (area.isEditable()) {
            area.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(7, 10, 7, 10)));
        }
    }

    private static void styleCombo(JComboBox<?> combo) {
        combo.setBackground(SURFACE);
        combo.setForeground(TEXT);
        combo.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(3, 8, 3, 8)));
    }

    public static DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    public static void addField(JPanel panel, int row, String label, JComponent field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = row;
        JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(MUTED);
        labelComponent.setFont(labelComponent.getFont().deriveFont(Font.BOLD, BASE_FONT_SIZE - 0.5f));
        panel.add(labelComponent, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        polishTree(field);
        panel.add(field, gbc);
    }

    public static void styleTable(JTable table) {
        if (table == null) {
            return;
        }
        table.setFont(table.getFont().deriveFont(BASE_FONT_SIZE));
        table.setRowHeight(Math.max(34, Math.round(BASE_FONT_SIZE * 2.05f)));
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(232, 236, 242));
        table.setFillsViewportHeight(true);
        table.setBackground(SURFACE);
        table.setForeground(TEXT);
        table.setSelectionBackground(TABLE_SELECTION);
        table.setSelectionForeground(TEXT);
        table.setDefaultRenderer(Object.class, new ModernTableCellRenderer());
        if (table.getTableHeader() != null) {
            JTableHeader header = table.getTableHeader();
            header.setFont(header.getFont().deriveFont(Font.BOLD, BASE_FONT_SIZE));
            header.setReorderingAllowed(false);
            header.setResizingAllowed(true);
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
            header.setDefaultRenderer(new ModernHeaderRenderer());
        }
        installResponsiveColumnFit(table);
        installCellTooltips(table);
        installRowDetailDialog(table, "資料詳細內容");
        SwingUtilities.invokeLater(() -> fitColumnsToViewport(table));
    }

    private static class ModernTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setBorder(new EmptyBorder(0, 10, 0, 10));
            label.setForeground(TEXT);
            if (isSelected) {
                label.setBackground(TABLE_SELECTION);
            } else {
                label.setBackground(row % 2 == 0 ? SURFACE : TABLE_ALT_ROW);
            }
            if (value instanceof Number || isLikelyNumeric(value)) {
                label.setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                label.setHorizontalAlignment(SwingConstants.LEFT);
            }
            return label;
        }
    }

    private static class ModernHeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setOpaque(true);
            label.setBackground(PRIMARY);
            label.setForeground(Color.WHITE);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD, BASE_FONT_SIZE));
            label.setBorder(new CompoundBorder(new LineBorder(PRIMARY_DARK, 0), new EmptyBorder(8, 8, 8, 8)));
            return label;
        }
    }

    private static boolean isLikelyNumeric(Object value) {
        if (value == null) {
            return false;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || text.length() > 12) {
            return false;
        }
        return text.matches("-?\\d+(\\.\\d+)?") || text.matches("\\d+/\\d+");
    }

    /**
     * 讓表格欄位依照目前頁面寬度自動分配，不再出現水平捲軸或欄位忽大忽小。
     * 內容較長的欄位會拿到較多寬度；完整內容仍可雙擊資料列開啟詳細視窗查看。
     */
    public static void fitColumnsToScrollPane(JTable table, JScrollPane scrollPane) {
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(SURFACE);
        Runnable fit = () -> fitColumnsToViewport(table);
        scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(fit);
            }
        });
        SwingUtilities.invokeLater(fit);
    }

    private static void installResponsiveColumnFit(JTable table) {
        if (Boolean.TRUE.equals(table.getClientProperty("gymapp.responsiveColumnFitInstalled"))) {
            return;
        }
        table.putClientProperty("gymapp.responsiveColumnFitInstalled", Boolean.TRUE);
        table.getModel().addTableModelListener(e -> SwingUtilities.invokeLater(() -> fitColumnsToViewport(table)));
        table.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> fitColumnsToViewport(table));
            }

            @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> fitColumnsToViewport(table));
            }
        });
    }

    public static void fitColumnsToViewport(JTable table) {
        fitColumnsToAvailableWidth(table, viewportWidth(table));
    }

    public static void fitColumnsToAvailableWidth(JTable table, int viewportWidth) {
        if (table == null || table.getColumnModel().getColumnCount() == 0) {
            return;
        }
        int targetWidth = viewportWidth > 0 ? viewportWidth : table.getWidth();
        if (targetWidth <= 80) {
            return;
        }
        targetWidth = Math.max(80, targetWidth - 2);

        int count = table.getColumnModel().getColumnCount();
        int[] min = new int[count];
        int[] preferred = new int[count];
        int minTotal = 0;
        int preferredTotal = 0;
        for (int i = 0; i < count; i++) {
            String name = table.getColumnName(i);
            min[i] = minWidthForColumn(name, count);
            preferred[i] = Math.max(min[i], naturalColumnWidth(table, i));
            preferred[i] = Math.min(preferred[i], softMaxWidthForColumn(name));
            minTotal += min[i];
            preferredTotal += preferred[i];
        }

        int[] widths = new int[count];
        if (targetWidth <= minTotal) {
            double factor = targetWidth / (double) Math.max(1, minTotal);
            for (int i = 0; i < count; i++) {
                widths[i] = Math.max(28, (int) Math.floor(min[i] * factor));
            }
        } else if (targetWidth <= preferredTotal) {
            double factor = (targetWidth - minTotal) / (double) Math.max(1, preferredTotal - minTotal);
            for (int i = 0; i < count; i++) {
                widths[i] = min[i] + (int) Math.floor((preferred[i] - min[i]) * factor);
            }
        } else {
            System.arraycopy(preferred, 0, widths, 0, count);
            int extra = targetWidth - preferredTotal;
            int flexTotal = 0;
            int[] flex = new int[count];
            for (int i = 0; i < count; i++) {
                flex[i] = flexWeightForColumn(table.getColumnName(i));
                flexTotal += flex[i];
            }
            for (int i = 0; i < count; i++) {
                widths[i] += Math.floorDiv(extra * flex[i], Math.max(1, flexTotal));
            }
        }

        normalizeWidths(widths, targetWidth);
        for (int i = 0; i < count; i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setMinWidth(28);
            column.setMaxWidth(Integer.MAX_VALUE);
            column.setPreferredWidth(Math.max(28, widths[i]));
            column.setWidth(Math.max(28, widths[i]));
            column.setResizable(true);
        }
        table.doLayout();
        table.revalidate();
        table.repaint();
    }

    private static int viewportWidth(JTable table) {
        Container parent = table.getParent();
        if (parent instanceof JViewport viewport && viewport.getWidth() > 0) {
            return viewport.getWidth();
        }
        return table.getWidth();
    }

    private static int naturalColumnWidth(JTable table, int viewColumn) {
        int width = headerWidth(table, viewColumn);
        int rowsToMeasure = Math.min(table.getRowCount(), 40);
        for (int row = 0; row < rowsToMeasure; row++) {
            TableCellRenderer renderer = table.getCellRenderer(row, viewColumn);
            Component comp = table.prepareRenderer(renderer, row, viewColumn);
            width = Math.max(width, comp.getPreferredSize().width + 20);
        }
        return width;
    }

    private static int headerWidth(JTable table, int viewColumn) {
        if (table.getTableHeader() == null) {
            return table.getColumnName(viewColumn).length() * 18 + 28;
        }
        TableCellRenderer renderer = table.getTableHeader().getDefaultRenderer();
        Component comp = renderer.getTableCellRendererComponent(
                table,
                table.getColumnName(viewColumn),
                false,
                false,
                -1,
                viewColumn
        );
        return comp.getPreferredSize().width + 26;
    }

    private static int minWidthForColumn(String name, int columnCount) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (isIdColumn(n)) {
            return columnCount >= 9 ? 42 : 52;
        }
        if (containsAny(n, "狀態", "角色", "種類", "類別", "動作", "權限")) {
            return columnCount >= 9 ? 54 : 66;
        }
        if (containsAny(n, "日期", "時間", "timestamp", "created", "schedule")) {
            return columnCount >= 9 ? 88 : 108;
        }
        if (containsAny(n, "內容", "建議", "備註", "狀況", "目標", "地址", "說明")) {
            return columnCount >= 9 ? 92 : 110;
        }
        if (containsAny(n, "email", "電子信箱", "名稱", "姓名", "帳號", "手機", "場館", "教練", "課程", "商品", "器材", "會員")) {
            return columnCount >= 9 ? 72 : 88;
        }
        return columnCount >= 9 ? 48 : 64;
    }

    private static int softMaxWidthForColumn(String name) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (containsAny(n, "內容", "建議", "備註", "狀況", "目標", "地址", "說明")) {
            return 260;
        }
        if (containsAny(n, "email", "電子信箱")) {
            return 210;
        }
        if (containsAny(n, "時間", "timestamp", "created", "schedule")) {
            return 175;
        }
        if (containsAny(n, "日期", "date")) {
            return 150;
        }
        if (containsAny(n, "名稱", "姓名", "課程", "品名", "場館", "器材", "商品")) {
            return 190;
        }
        if (isIdColumn(n) || containsAny(n, "餘額", "點數", "金額", "人數", "庫存", "價格", "數量", "重量", "次數", "組數", "容量")) {
            return 105;
        }
        return 155;
    }

    private static int flexWeightForColumn(String name) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (containsAny(n, "內容", "建議", "備註", "狀況", "目標", "地址", "說明")) {
            return 5;
        }
        if (containsAny(n, "email", "電子信箱", "名稱", "姓名", "課程", "品名", "場館", "帳號", "器材", "商品")) {
            return 3;
        }
        if (containsAny(n, "日期", "時間", "timestamp", "created", "schedule")) {
            return 2;
        }
        return 1;
    }

    private static boolean isIdColumn(String n) {
        return n.contains("id") || n.contains("編號") || n.contains("單號") || n.contains("流水號");
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text != null && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static void normalizeWidths(int[] widths, int targetWidth) {
        int total = 0;
        for (int width : widths) {
            total += width;
        }
        int diff = targetWidth - total;
        int index = 0;
        int safety = Math.max(1, widths.length * Math.max(20, Math.abs(diff) + 4));
        while (diff != 0 && widths.length > 0 && safety-- > 0) {
            int i = index % widths.length;
            if (diff > 0) {
                widths[i]++;
                diff--;
            } else if (widths[i] > 28) {
                widths[i]--;
                diff++;
            }
            index++;
        }
    }

    private static void installCellTooltips(JTable table) {
        if (Boolean.TRUE.equals(table.getClientProperty("gymapp.cellTooltipInstalled"))) {
            return;
        }
        table.putClientProperty("gymapp.cellTooltipInstalled", Boolean.TRUE);
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());
                if (row < 0 || column < 0) {
                    table.setToolTipText(null);
                    return;
                }
                Object value = table.getValueAt(row, column);
                table.setToolTipText(value == null ? null : value.toString());
            }
        });
    }

    public static JButton detailButton(JTable table, String title) {
        JButton button = new JButton("查看選取詳細");
        button.setToolTipText("選取表格列後按此按鈕，或直接雙擊列，也可以按 Enter 開啟完整內容");
        button.addActionListener(e -> showTableRowDetail(table, table, title));
        styleButton(button);
        return button;
    }

    public static void installRowDetailDialog(JTable table, String title) {
        if (Boolean.TRUE.equals(table.getClientProperty("gymapp.detailDialogInstalled"))) {
            return;
        }
        table.putClientProperty("gymapp.detailDialogInstalled", Boolean.TRUE);
        table.setToolTipText("雙擊資料列或選取後按 Enter，可開啟完整詳細內容");
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        showTableRowDetail(table, table, title);
                    }
                }
            }
        });
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "gymapp.showDetail");
        table.getActionMap().put("gymapp.showDetail", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showTableRowDetail(table, table, title);
            }
        });
    }

    public static void installDeleteShortcut(JTable table, Runnable deleteAction) {
        if (table == null || deleteAction == null) {
            return;
        }
        String actionName = "gymapp.deleteSelectedRow";
        InputMap focusedMap = table.getInputMap(JComponent.WHEN_FOCUSED);
        InputMap ancestorMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        focusedMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), actionName);
        focusedMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), actionName);
        ancestorMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), actionName);
        ancestorMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), actionName);
        table.getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (table.getSelectedRow() < 0) {
                    UiUtil.info(table, "請先選取要刪除的資料");
                    return;
                }
                deleteAction.run();
            }
        });
        String tip = table.getToolTipText();
        String deleteTip = "選取資料列後可按 Delete 或 Backspace 刪除";
        if (tip == null || tip.isBlank()) {
            table.setToolTipText(deleteTip);
        } else if (!tip.contains("Backspace")) {
            table.setToolTipText(tip + "；" + deleteTip);
        }
    }

    public static void showTableRowDetail(Component parent, JTable table, String title) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            info(parent, "請先選取一筆資料");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        TableModel model = table.getModel();

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBackground(SURFACE);
        fields.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 7, 7, 7);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;

        for (int c = 0; c < model.getColumnCount(); c++) {
            String columnName = model.getColumnName(c);
            Object value = model.getValueAt(modelRow, c);
            String text = value == null ? "" : String.valueOf(value);
            int rows = needsLargeTextArea(columnName, text) ? 5 : 2;
            JLabel label = new JLabel(columnName);
            label.setForeground(PRIMARY_DARK);
            label.setFont(label.getFont().deriveFont(Font.BOLD, BASE_FONT_SIZE));
            JTextArea area = new JTextArea(text, rows, 44);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setEditable(false);
            area.setFont(area.getFont().deriveFont(BASE_FONT_SIZE));
            area.setBackground(SURFACE_ALT);
            area.setForeground(TEXT);
            area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel valueBox = new JPanel(new BorderLayout());
            valueBox.setBackground(SURFACE_ALT);
            valueBox.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(0, 0, 0, 0)));
            valueBox.add(area, BorderLayout.CENTER);
            valueBox.setPreferredSize(new Dimension(600, rows >= 5 ? 136 : 76));

            gbc.gridx = 0;
            gbc.gridy = c;
            gbc.weightx = 0;
            fields.add(label, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            fields.add(valueBox, gbc);
        }

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title, Dialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(APP_BG);
        JLabel hint = new JLabel("完整資料內容");
        hint.setOpaque(true);
        hint.setBackground(PRIMARY);
        hint.setForeground(Color.WHITE);
        hint.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        hint.setFont(hint.getFont().deriveFont(Font.BOLD, BASE_FONT_SIZE + 3f));
        root.add(hint, BorderLayout.NORTH);
        JScrollPane detailScroll = new JScrollPane(fields);
        detailScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        detailScroll.getVerticalScrollBar().setUnitIncrement(18);
        makeScrollPaneScrollAnywhere(detailScroll);
        root.add(detailScroll, BorderLayout.CENTER);
        JButton close = new JButton("關閉");
        close.addActionListener(e -> dialog.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setBackground(APP_BG);
        south.add(close);
        root.add(south, BorderLayout.SOUTH);
        makeComponentScrollAnywhere(root, detailScroll);
        dialog.setContentPane(root);
        dialog.setSize(840, 620);
        dialog.setLocationRelativeTo(parent);
        polishWindow(dialog);
        dialog.setVisible(true);
    }

    public static void makeScrollPaneScrollAnywhere(JScrollPane scrollPane) {
        if (scrollPane == null || Boolean.TRUE.equals(scrollPane.getClientProperty("gymapp.scrollAnywhereInstalled"))) {
            return;
        }
        scrollPane.putClientProperty("gymapp.scrollAnywhereInstalled", Boolean.TRUE);
        java.awt.event.MouseWheelListener listener = e -> scrollByWheel(scrollPane, e);
        installWheelForwarder(scrollPane, listener);
        installWheelForwarder(scrollPane.getViewport(), listener);
        Component view = scrollPane.getViewport().getView();
        installWheelForwarder(view, listener);
    }

    public static void makeComponentScrollAnywhere(Component component, JScrollPane targetScrollPane) {
        if (component == null || targetScrollPane == null) {
            return;
        }
        java.awt.event.MouseWheelListener listener = e -> scrollByWheel(targetScrollPane, e);
        installWheelForwarder(component, listener);
    }

    private static void installWheelForwarder(Component component, java.awt.event.MouseWheelListener listener) {
        if (component == null) {
            return;
        }
        if (component instanceof JComponent jComponent) {
            if (Boolean.TRUE.equals(jComponent.getClientProperty("gymapp.scrollAnywhereWheelForwarderInstalled"))) {
                return;
            }
            jComponent.putClientProperty("gymapp.scrollAnywhereWheelForwarderInstalled", Boolean.TRUE);
        }
        component.addMouseWheelListener(listener);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                installWheelForwarder(child, listener);
            }
        }
    }

    private static void scrollByWheel(JScrollPane scrollPane, MouseWheelEvent e) {
        JScrollBar bar = e.isShiftDown() ? scrollPane.getHorizontalScrollBar() : scrollPane.getVerticalScrollBar();
        if (bar == null || !bar.isVisible()) {
            bar = scrollPane.getVerticalScrollBar();
        }
        if (bar == null) {
            return;
        }
        int amount;
        if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            amount = e.getUnitsToScroll() * Math.max(1, bar.getUnitIncrement());
        } else {
            amount = e.getWheelRotation() * Math.max(1, bar.getBlockIncrement());
        }
        if (amount == 0) {
            amount = e.getWheelRotation() * Math.max(1, bar.getUnitIncrement());
        }
        int max = Math.max(bar.getMinimum(), bar.getMaximum() - bar.getVisibleAmount());
        int next = Math.max(bar.getMinimum(), Math.min(max, bar.getValue() + amount));
        bar.setValue(next);
        e.consume();
    }

    private static boolean needsLargeTextArea(String columnName, String text) {
        return text.length() > 45
                || columnName.contains("內容")
                || columnName.contains("建議")
                || columnName.contains("備註")
                || columnName.contains("狀況")
                || columnName.contains("目標");
    }

    public static JPanel autoSearchPanel(String hint, JTable... tables) {
        JTextField searchField = new JTextField(24);
        searchField.setToolTipText("輸入關鍵字後會立即篩選，不需要按搜尋按鈕");
        searchField.putClientProperty("JTextField.placeholderText", hint);
        styleInput(searchField);
        for (JTable table : tables) {
            installAutoSearch(table, searchField);
        }

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panel.setBackground(SURFACE);
        JLabel label = new JLabel("搜尋");
        label.setForeground(PRIMARY_DARK);
        label.setFont(label.getFont().deriveFont(Font.BOLD, BASE_FONT_SIZE));
        panel.add(label);
        panel.add(searchField);
        JLabel help = new JLabel("輸入後即時篩選");
        help.setForeground(MUTED);
        panel.add(help);
        return panel;
    }

    public static void installAutoSearch(JTable table, JTextField searchField) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        Runnable applyFilter = () -> {
            String keyword = searchField.getText();
            if (keyword == null || keyword.trim().isEmpty()) {
                sorter.setRowFilter(null);
                return;
            }
            String normalized = keyword.trim().toLowerCase(Locale.ROOT);
            sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    for (int i = 0; i < entry.getValueCount(); i++) {
                        Object value = entry.getValue(i);
                        if (value != null && value.toString().toLowerCase(Locale.ROOT).contains(normalized)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        };
        onTextChanged(searchField, applyFilter);
    }

    public static void onTextChanged(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override
            public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override
            public void changedUpdate(DocumentEvent e) { action.run(); }
        });
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "訊息", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "錯誤", JOptionPane.ERROR_MESSAGE);
    }

    public static void error(Component parent, Exception e) {
        String msg = e.getMessage();
        if (e instanceof SQLException) {
            msg = "資料庫錯誤：" + e.getMessage();
        }
        error(parent, msg == null ? e.toString() : msg);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "請確認", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public static int intValue(String text, String fieldName) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " 必須是整數");
        }
    }

    public static float floatValue(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            return 0f;
        }
        try {
            return Float.parseFloat(text.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " 必須是數字");
        }
    }

    public static Integer nullableInt(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return Integer.parseInt(text.trim());
    }
}
