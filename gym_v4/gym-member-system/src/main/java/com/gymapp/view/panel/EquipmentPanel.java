package com.gymapp.view.panel;

import com.gymapp.dao.EquipmentDAO;
import com.gymapp.model.Equipment;
import com.gymapp.util.DateTimeUtil;
import com.gymapp.util.UiUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EquipmentPanel extends BasePanel {
    private final EquipmentDAO equipmentDAO = new EquipmentDAO();
    private final DefaultTableModel model = UiUtil.readOnlyModel(new String[]{"器材ID", "名稱", "類型", "狀態", "購買日", "上次維護", "下次維護", "場館ID", "備註"});
    private final JTable table = new JTable(model);
    private final JTextField idField = new JTextField(8);
    private final JTextField nameField = new JTextField(14);
    private final JTextField typeField = new JTextField(12);
    private final JComboBox<String> statusBox = new JComboBox<>(new String[]{"正常", "維修中", "停用"});
    private final JTextField purchaseField = new JTextField("2026-01-01", 12);
    private final JTextField lastField = new JTextField("2026-01-01", 12);
    private final JTextField nextField = new JTextField("2026-04-01", 12);
    private final JTextField branchField = new JTextField(8);
    private final JTextField notesField = new JTextField(14);

    public EquipmentPanel() {
        buildUi();
        refreshData();
    }

    private void buildUi() {
        add(scroll(table), BorderLayout.CENTER);
        idField.setEditable(false);
        JPanel form = UiUtil.formPanel();
        UiUtil.addField(form, 0, "器材ID", idField);
        UiUtil.addField(form, 1, "名稱", nameField);
        UiUtil.addField(form, 2, "類型", typeField);
        UiUtil.addField(form, 3, "狀態", statusBox);
        UiUtil.addField(form, 4, "購買日 yyyy-MM-dd", purchaseField);
        UiUtil.addField(form, 5, "上次維護", lastField);
        UiUtil.addField(form, 6, "下次維護", nextField);
        UiUtil.addField(form, 7, "場館ID", branchField);
        UiUtil.addField(form, 8, "備註", notesField);
        JButton add = new JButton("新增");
        JButton update = new JButton("修改");
        JButton delete = new JButton("刪除");
        JButton maintained = new JButton("完成維護(+90天)");
        JButton refresh = new JButton("刷新");
        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        buttons.add(add); buttons.add(update); buttons.add(delete); buttons.add(maintained); buttons.add(refresh);
        JPanel east = new JPanel(new BorderLayout());
        east.setBorder(BorderFactory.createTitledBorder("器材資料"));
        east.add(form, BorderLayout.CENTER);
        east.add(buttons, BorderLayout.SOUTH);
        add(east, BorderLayout.EAST);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) fillFromSelected(); });
        add.addActionListener(e -> addEquipment());
        update.addActionListener(e -> updateEquipment());
        delete.addActionListener(e -> deleteEquipment());
        maintained.addActionListener(e -> maintained());
        refresh.addActionListener(e -> refreshData());
    }

    private Equipment readForm(boolean needId) {
        Equipment e = new Equipment();
        if (needId) e.setEquipmentId(UiUtil.intValue(idField.getText(), "器材ID"));
        e.setEquipmentName(nameField.getText().trim());
        e.setType(typeField.getText().trim());
        e.setStatus(String.valueOf(statusBox.getSelectedItem()));
        e.setPurchaseDate(DateTimeUtil.parseDate(purchaseField.getText()));
        e.setLastMaintenanceDate(DateTimeUtil.parseDate(lastField.getText()));
        e.setNextMaintenanceDate(DateTimeUtil.parseDate(nextField.getText()));
        e.setBranchId(UiUtil.nullableInt(branchField.getText()));
        e.setNotes(notesField.getText().trim());
        if (e.getEquipmentName().isBlank()) throw new IllegalArgumentException("器材名稱不可空白");
        return e;
    }

    private void fillFromSelected() {
        int r = table.convertRowIndexToModel(table.getSelectedRow());
        idField.setText(String.valueOf(model.getValueAt(r, 0)));
        nameField.setText(String.valueOf(model.getValueAt(r, 1)));
        typeField.setText(String.valueOf(model.getValueAt(r, 2)));
        statusBox.setSelectedItem(String.valueOf(model.getValueAt(r, 3)));
        purchaseField.setText(String.valueOf(model.getValueAt(r, 4)));
        lastField.setText(String.valueOf(model.getValueAt(r, 5)));
        nextField.setText(String.valueOf(model.getValueAt(r, 6)));
        branchField.setText(String.valueOf(model.getValueAt(r, 7)));
        notesField.setText(String.valueOf(model.getValueAt(r, 8)));
    }

    private void addEquipment() { try { equipmentDAO.insert(readForm(false)); refreshData(); } catch (Exception e) { showError(e); } }
    private void updateEquipment() { try { equipmentDAO.update(readForm(true)); refreshData(); } catch (Exception e) { showError(e); } }
    private void deleteEquipment() { try { int id = selectedId(table, 0); if (UiUtil.confirm(this, "確定刪除器材 ID " + id + "？")) { equipmentDAO.delete(id); refreshData(); } } catch (Exception e) { showError(e); } }

    private void maintained() {
        try {
            Equipment e = readForm(true);
            LocalDate today = LocalDate.now();
            e.setLastMaintenanceDate(today);
            e.setNextMaintenanceDate(today.plusDays(90));
            e.setStatus("正常");
            equipmentDAO.update(e);
            refreshData();
            UiUtil.info(this, "已更新維護日期與狀態");
        } catch (Exception e) { showError(e); }
    }

    @Override
    public void refreshData() {
        try {
            List<Object[]> rows = new ArrayList<>();
            for (Equipment e : equipmentDAO.findAll()) {
                rows.add(new Object[]{e.getEquipmentId(), e.getEquipmentName(), e.getType(), e.getStatus(), DateTimeUtil.format(e.getPurchaseDate()), DateTimeUtil.format(e.getLastMaintenanceDate()), DateTimeUtil.format(e.getNextMaintenanceDate()), e.getBranchId() == null ? "" : e.getBranchId(), e.getNotes()});
            }
            setRows(model, rows);
        } catch (Exception e) { showError(e); }
    }
}
