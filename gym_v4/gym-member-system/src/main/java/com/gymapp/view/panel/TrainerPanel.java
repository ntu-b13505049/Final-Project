package com.gymapp.view.panel;

import com.gymapp.dao.StaffDAO;
import com.gymapp.model.Role;
import com.gymapp.model.Staff;
import com.gymapp.util.UiUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TrainerPanel extends BasePanel {
    private final StaffDAO staffDAO = new StaffDAO();
    private final DefaultTableModel model = UiUtil.readOnlyModel(new String[]{"ID", "角色", "姓名", "帳號", "手機", "專長", "場館ID"});
    private final JTable table = new JTable(model);
    private final JTextField idField = new JTextField(8);
    private final JComboBox<String> roleBox = new JComboBox<>(new String[]{"教練", "管理員"});
    private final JTextField nameField = new JTextField(14);
    private final JTextField accountField = new JTextField(14);
    private final JPasswordField passwordField = new JPasswordField(14);
    private final JTextField phoneField = new JTextField(14);
    private final JTextField specialtyField = new JTextField(14);
    private final JTextField branchField = new JTextField(8);

    public TrainerPanel() {
        buildUi();
        refreshData();
    }

    private void buildUi() {
        add(scroll(table), BorderLayout.CENTER);
        idField.setEditable(false);
        JPanel form = UiUtil.formPanel();
        UiUtil.addField(form, 0, "ID", idField);
        UiUtil.addField(form, 1, "角色", roleBox);
        UiUtil.addField(form, 2, "姓名", nameField);
        UiUtil.addField(form, 3, "帳號", accountField);
        UiUtil.addField(form, 4, "密碼(空白=不變)", passwordField);
        UiUtil.addField(form, 5, "手機", phoneField);
        UiUtil.addField(form, 6, "專長", specialtyField);
        UiUtil.addField(form, 7, "場館ID", branchField);
        JButton add = new JButton("新增");
        JButton update = new JButton("修改");
        JButton delete = new JButton("刪除");
        JButton clear = new JButton("清空");
        JButton refresh = new JButton("刷新");
        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        buttons.add(add); buttons.add(update); buttons.add(delete); buttons.add(clear); buttons.add(refresh);
        JPanel east = new JPanel(new BorderLayout());
        east.setBorder(BorderFactory.createTitledBorder("員工/教練資料"));
        east.add(form, BorderLayout.CENTER);
        east.add(buttons, BorderLayout.SOUTH);
        add(east, BorderLayout.EAST);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) fillFromSelected(); });
        add.addActionListener(e -> addStaff());
        update.addActionListener(e -> updateStaff());
        delete.addActionListener(e -> deleteStaff());
        clear.addActionListener(e -> clearForm());
        refresh.addActionListener(e -> refreshData());
    }

    private Staff readForm(boolean needId) {
        Staff s = new Staff();
        if (needId) s.setId(UiUtil.intValue(idField.getText(), "ID"));
        s.setRole(Role.fromDisplayName(String.valueOf(roleBox.getSelectedItem())));
        s.setName(nameField.getText().trim());
        s.setAccount(accountField.getText().trim());
        s.setPhone(phoneField.getText().trim());
        s.setSpecialty(specialtyField.getText().trim());
        s.setBranchId(UiUtil.nullableInt(branchField.getText()));
        if (s.getName().isBlank() || s.getAccount().isBlank()) throw new IllegalArgumentException("姓名與帳號不可空白");
        return s;
    }

    private void fillFromSelected() {
        int r = table.convertRowIndexToModel(table.getSelectedRow());
        idField.setText(String.valueOf(model.getValueAt(r, 0)));
        roleBox.setSelectedItem(String.valueOf(model.getValueAt(r, 1)));
        nameField.setText(String.valueOf(model.getValueAt(r, 2)));
        accountField.setText(String.valueOf(model.getValueAt(r, 3)));
        phoneField.setText(String.valueOf(model.getValueAt(r, 4)));
        specialtyField.setText(String.valueOf(model.getValueAt(r, 5)));
        branchField.setText(String.valueOf(model.getValueAt(r, 6)));
        passwordField.setText("");
    }

    private void addStaff() {
        try {
            staffDAO.insert(readForm(false), new String(passwordField.getPassword()));
            refreshData();
            clearForm();
            UiUtil.info(this, "新增成功");
        } catch (Exception e) { showError(e); }
    }

    private void updateStaff() {
        try {
            staffDAO.update(readForm(true), new String(passwordField.getPassword()));
            refreshData();
            UiUtil.info(this, "修改成功");
        } catch (Exception e) { showError(e); }
    }

    private void deleteStaff() {
        try {
            int id = selectedId(table, 0);
            if (UiUtil.confirm(this, "確定刪除員工 ID " + id + "？")) {
                staffDAO.delete(id);
                refreshData();
                clearForm();
            }
        } catch (Exception e) { showError(e); }
    }

    private void clearForm() {
        idField.setText(""); roleBox.setSelectedItem("教練"); nameField.setText(""); accountField.setText(""); passwordField.setText(""); phoneField.setText(""); specialtyField.setText(""); branchField.setText("");
    }

    @Override
    public void refreshData() {
        try {
            List<Object[]> rows = new ArrayList<>();
            for (Staff s : staffDAO.findAll()) {
                rows.add(new Object[]{s.getId(), s.getDisplayRole(), s.getName(), s.getAccount(), s.getPhone(), s.getSpecialty(), s.getBranchId() == null ? "" : s.getBranchId()});
            }
            setRows(model, rows);
        } catch (Exception e) { showError(e); }
    }
}
