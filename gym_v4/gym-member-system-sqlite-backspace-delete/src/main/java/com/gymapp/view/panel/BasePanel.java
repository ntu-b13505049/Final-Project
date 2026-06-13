package com.gymapp.view.panel;

import com.gymapp.util.UiUtil;
import com.gymapp.view.Refreshable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public abstract class BasePanel extends JPanel implements Refreshable {
    protected BasePanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    protected int selectedId(JTable table, int idColumn) {
        int row = table.getSelectedRow();
        if (row < 0) {
            throw new IllegalArgumentException("請先選取表格資料");
        }
        int modelRow = table.convertRowIndexToModel(row);
        Object value = table.getModel().getValueAt(modelRow, idColumn);
        return Integer.parseInt(String.valueOf(value));
    }

    protected void setRows(DefaultTableModel model, java.util.List<Object[]> rows) {
        model.setRowCount(0);
        for (Object[] row : rows) {
            model.addRow(row);
        }
    }

    protected JScrollPane scroll(JTable table) {
        UiUtil.styleTable(table);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
        UiUtil.fitColumnsToScrollPane(table, scrollPane);
        return scrollPane;
    }

    protected JPanel tablePanel(JTable table, String searchHint) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JScrollPane scrollPane = scroll(table);
        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.add(UiUtil.autoSearchPanel(searchHint, table), BorderLayout.CENTER);
        top.add(UiUtil.detailButton(table, "資料詳細內容"), BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    protected JPanel tablePanel(String title, JTable table, String searchHint) {
        JPanel panel = tablePanel(table, searchHint);
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    protected void showError(Exception e) {
        UiUtil.error(this, e);
    }
}
