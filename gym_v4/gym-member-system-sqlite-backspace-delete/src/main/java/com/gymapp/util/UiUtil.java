package com.gymapp.util;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableModel;
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

    private UiUtil() {}

    public static void applyGlobalFont(float size) {
        UIDefaults defaults = UIManager.getDefaults();
        for (Object key : defaults.keySet().toArray()) {
            Object value = defaults.get(key);
            if (value instanceof FontUIResource font) {
                defaults.put(key, new FontUIResource(font.getFamily(), font.getStyle(), Math.round(size)));
            } else if (value instanceof Font font) {
                defaults.put(key, font.deriveFont(size));
            }
        }
        UIManager.put("Table.rowHeight", Math.max(30, Math.round(size * 1.9f)));
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
    }

    public static JLabel label(String text) {
        return new JLabel(text);
    }

    public static JTextField field(int columns) {
        return new JTextField(columns);
    }

    public static JPasswordField passwordField(int columns) {
        return new JPasswordField(columns);
    }

    public static JButton button(String text) {
        return new JButton(text);
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
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return panel;
    }

    public static void addField(JPanel panel, int row, String label, JComponent field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    public static void styleTable(JTable table) {
        table.setFont(table.getFont().deriveFont(BASE_FONT_SIZE));
        table.setRowHeight(Math.max(30, Math.round(BASE_FONT_SIZE * 1.9f)));
        table.setIntercellSpacing(new Dimension(6, 4));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setShowGrid(true);
        table.setGridColor(new Color(228, 228, 228));
        table.setFillsViewportHeight(true);
        if (table.getTableHeader() != null) {
            table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD, BASE_FONT_SIZE));
            table.getTableHeader().setReorderingAllowed(false);
            table.getTableHeader().setResizingAllowed(true);
        }
        installResponsiveColumnFit(table);
        installCellTooltips(table);
        installRowDetailDialog(table, "資料詳細內容");
        SwingUtilities.invokeLater(() -> fitColumnsToViewport(table));
    }

    /**
     * 讓表格欄位依照目前頁面寬度自動分配，不再出現水平捲軸或欄位忽大忽小。
     * 內容較長的欄位會拿到較多寬度；完整內容仍可雙擊資料列開啟詳細視窗查看。
     */
    public static void fitColumnsToScrollPane(JTable table, JScrollPane scrollPane) {
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
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
            if (text.contains(keyword)) {
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
        fields.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;

        for (int c = 0; c < model.getColumnCount(); c++) {
            String columnName = model.getColumnName(c);
            Object value = model.getValueAt(modelRow, c);
            String text = value == null ? "" : String.valueOf(value);
            int rows = needsLargeTextArea(columnName, text) ? 4 : 2;
            JLabel label = new JLabel(columnName);
            label.setFont(label.getFont().deriveFont(Font.BOLD, BASE_FONT_SIZE));
            JTextArea area = new JTextArea(text, rows, 44);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setEditable(false);
            area.setFont(area.getFont().deriveFont(BASE_FONT_SIZE));
            area.setOpaque(false);
            area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            JPanel valueBox = new JPanel(new BorderLayout());
            valueBox.setBackground(UIManager.getColor("TextArea.background"));
            valueBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(205, 205, 205)),
                    BorderFactory.createEmptyBorder(0, 0, 0, 0)
            ));
            valueBox.add(area, BorderLayout.CENTER);
            valueBox.setPreferredSize(new Dimension(580, rows >= 4 ? 122 : 72));

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
        JLabel hint = new JLabel("完整資料內容");
        hint.setBorder(BorderFactory.createEmptyBorder(10, 14, 0, 14));
        hint.setFont(hint.getFont().deriveFont(Font.BOLD, BASE_FONT_SIZE + 2f));
        root.add(hint, BorderLayout.NORTH);
        JScrollPane detailScroll = new JScrollPane(fields);
        detailScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        detailScroll.getVerticalScrollBar().setUnitIncrement(18);
        makeScrollPaneScrollAnywhere(detailScroll);
        root.add(detailScroll, BorderLayout.CENTER);
        JButton close = new JButton("關閉");
        close.addActionListener(e -> dialog.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(close);
        root.add(south, BorderLayout.SOUTH);
        makeComponentScrollAnywhere(root, detailScroll);
        dialog.setContentPane(root);
        dialog.setSize(800, 580);
        dialog.setLocationRelativeTo(parent);
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
        for (JTable table : tables) {
            installAutoSearch(table, searchField);
        }

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panel.add(new JLabel("自動搜尋"));
        panel.add(searchField);
        JLabel help = new JLabel("輸入後即時篩選");
        help.setForeground(Color.GRAY);
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
