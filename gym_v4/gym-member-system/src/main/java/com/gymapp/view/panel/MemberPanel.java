package com.gymapp.view.panel;

import com.gymapp.dao.MemberDAO;
import com.gymapp.model.Member;
import com.gymapp.model.Wallet;
import com.gymapp.util.UiUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MemberPanel extends BasePanel {
    private final MemberDAO memberDAO = new MemberDAO();
    private final DefaultTableModel model = UiUtil.readOnlyModel(new String[]{"ID", "姓名", "帳號", "手機", "Email", "狀態", "點數"});
    private final JTable table = new JTable(model);
    private final JTextField idField = new JTextField(8);
    private final JTextField nameField = new JTextField(14);
    private final JTextField accountField = new JTextField(14);
    private final JPasswordField passwordField = new JPasswordField(14);
    private final JTextField phoneField = new JTextField(14);
    private final JTextField emailField = new JTextField(14);
    private final JComboBox<String> statusBox = new JComboBox<>(new String[]{"Active", "Suspended", "Leave"});
    private final JTextField balanceField = new JTextField("0", 8);

    public MemberPanel() {
        buildUi();
        refreshData();
    }

    private void buildUi() {
        add(scroll(table), BorderLayout.CENTER);
        idField.setEditable(false);
        JPanel form = UiUtil.formPanel();
        UiUtil.addField(form, 0, "ID", idField);
        UiUtil.addField(form, 1, "姓名", nameField);
        UiUtil.addField(form, 2, "帳號", accountField);
        UiUtil.addField(form, 3, "密碼(空白=不變)", passwordField);
        UiUtil.addField(form, 4, "手機", phoneField);
        UiUtil.addField(form, 5, "Email", emailField);
        UiUtil.addField(form, 6, "狀態", statusBox);
        UiUtil.addField(form, 7, "點數", balanceField);

        JButton add = new JButton("新增");
        JButton update = new JButton("修改");
        JButton delete = new JButton("刪除");
        JButton clear = new JButton("清空");
        JButton refresh = new JButton("刷新");
        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        buttons.add(add); buttons.add(update); buttons.add(delete); buttons.add(clear); buttons.add(refresh);
        JPanel east = new JPanel(new BorderLayout());
        east.setBorder(BorderFactory.createTitledBorder("會員資料"));
        east.add(form, BorderLayout.CENTER);
        east.add(buttons, BorderLayout.SOUTH);
        add(east, BorderLayout.EAST);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                fillFromSelected();
            }
        });
        add.addActionListener(e -> addMember());
        update.addActionListener(e -> updateMember());
        delete.addActionListener(e -> deleteMember());
        clear.addActionListener(e -> clearForm());
        refresh.addActionListener(e -> refreshData());
    }

    private void fillFromSelected() {
        int r = table.convertRowIndexToModel(table.getSelectedRow());
        idField.setText(String.valueOf(model.getValueAt(r, 0)));
        nameField.setText(String.valueOf(model.getValueAt(r, 1)));
        accountField.setText(String.valueOf(model.getValueAt(r, 2)));
        phoneField.setText(String.valueOf(model.getValueAt(r, 3)));
        emailField.setText(String.valueOf(model.getValueAt(r, 4)));
        statusBox.setSelectedItem(String.valueOf(model.getValueAt(r, 5)));
        balanceField.setText(String.valueOf(model.getValueAt(r, 6)));
        passwordField.setText("");
    }

    private Member readForm(boolean needId) {
        Member m = new Member();
        if (needId) m.setId(UiUtil.intValue(idField.getText(), "ID"));
        m.setName(nameField.getText().trim());
        m.setAccount(accountField.getText().trim());
        m.setPhone(phoneField.getText().trim());
        m.setEmail(emailField.getText().trim());
        m.setStatus(String.valueOf(statusBox.getSelectedItem()));
        m.setWallet(new Wallet(Math.max(0, UiUtil.intValue(balanceField.getText(), "點數"))));
        if (m.getName().isBlank() || m.getAccount().isBlank()) {
            throw new IllegalArgumentException("姓名與帳號不可空白");
        }
        return m;
    }

    private void addMember() {
        try {
            memberDAO.insert(readForm(false), new String(passwordField.getPassword()));
            UiUtil.info(this, "新增成功");
            refreshData();
            clearForm();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void updateMember() {
        try {
            memberDAO.update(readForm(true), new String(passwordField.getPassword()));
            UiUtil.info(this, "修改成功");
            refreshData();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void deleteMember() {
        try {
            int id = selectedId(table, 0);
            if (UiUtil.confirm(this, "確定刪除會員 ID " + id + "？")) {
                memberDAO.delete(id);
                refreshData();
                clearForm();
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        accountField.setText("");
        passwordField.setText("");
        phoneField.setText("");
        emailField.setText("");
        statusBox.setSelectedItem("Active");
        balanceField.setText("0");
    }

    @Override
    public void refreshData() {
        try {
            List<Object[]> rows = new ArrayList<>();
            for (Member m : memberDAO.findAll()) {
                rows.add(new Object[]{m.getId(), m.getName(), m.getAccount(), m.getPhone(), m.getEmail(), m.getStatus(), m.getWallet().getBalance()});
            }
            setRows(model, rows);
        } catch (Exception e) {
            showError(e);
        }
    }
}
