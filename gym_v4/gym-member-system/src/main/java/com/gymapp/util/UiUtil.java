package com.gymapp.util;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
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
        table.setIntercellSpacing(new Dimension(8, 6));
        if (table.getTableHeader() != null) {
            table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD, BASE_FONT_SIZE));
            table.getTableHeader().setReorderingAllowed(false);
        }
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
