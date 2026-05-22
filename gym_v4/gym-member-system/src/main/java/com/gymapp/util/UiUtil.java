package com.gymapp.util;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

public final class UiUtil {
    private UiUtil() {}

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
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
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
