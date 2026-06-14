package com.gymapp.view.panel;

import com.gymapp.dao.MemberDAO;
import com.gymapp.model.Member;
import com.gymapp.util.UiUtil;

import javax.swing.*;
import java.awt.*;

public class ProfilePanel extends BasePanel {
    private final Member member;
    private final MemberDAO memberDAO = new MemberDAO();
    private final JLabel info = new JLabel();
    private final JTextField nameField = new JTextField(18);
    private final JTextField phoneField = new JTextField(18);
    private final JTextField emailField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);

    public ProfilePanel(Member member) {
        this.member = member;
        buildUi();
        refreshData();
    }

    private void buildUi() {
        JPanel root = UiUtil.formPanel();
        UiUtil.addField(root, 0, "會員資訊", info);
        UiUtil.addField(root, 1, "姓名", nameField);
        UiUtil.addField(root, 2, "手機", phoneField);
        UiUtil.addField(root, 3, "Email", emailField);
        UiUtil.addField(root, 4, "新密碼(空白=不變)", passwordField);
        JButton save = new JButton("儲存個人資料");
        save.addActionListener(e -> save());
        JPanel center = new JPanel(new BorderLayout());
        center.add(root, BorderLayout.NORTH);
        center.add(save, BorderLayout.CENTER);
        add(center, BorderLayout.NORTH);
    }

    private void save() {
        try {
            Member latest = memberDAO.findById(member.getId()).orElseThrow();
            latest.setName(nameField.getText().trim());
            latest.setPhone(phoneField.getText().trim());
            latest.setEmail(emailField.getText().trim());
            memberDAO.update(latest, new String(passwordField.getPassword()));
            UiUtil.info(this, "個人資料已更新");
            refreshData();
        } catch (Exception e) {
            showError(e);
        }
    }

    @Override
    public void refreshData() {
        try {
            Member latest = memberDAO.findById(member.getId()).orElse(member);
            info.setText("ID：" + latest.getId() + " / 帳號：" + latest.getAccount() + " / 狀態：" + latest.getStatus() + " / 點數：" + latest.getWallet().getBalance());
            nameField.setText(latest.getName());
            phoneField.setText(latest.getPhone());
            emailField.setText(latest.getEmail());
            passwordField.setText("");
        } catch (Exception e) {
            showError(e);
        }
    }
}
