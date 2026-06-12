package com.gymapp.view;

import com.gymapp.database.DatabaseInitializer;
import com.gymapp.model.Role;
import com.gymapp.model.User;
import com.gymapp.service.AuthService;
import com.gymapp.util.UiUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class LoginFrame extends JFrame {
    private final AuthService authService;
    private final JTextField accountField = new JTextField("admin", 18);
    private final JPasswordField passwordField = new JPasswordField("admin123", 18);
    private final JComboBox<String> roleBox = new JComboBox<>(new String[]{"管理員", "教練", "會員"});
    private final JLabel statusLabel = new JLabel("SQLite 資料庫尚未檢查");

    public LoginFrame(AuthService authService) {
        this(authService, null, JFrame.NORMAL);
    }

    public LoginFrame(AuthService authService, Rectangle bounds, int extendedState) {
        super("健身房會員系統 - 登入");
        this.authService = authService;
        buildUi();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        UiUtil.prepareFrame(this, bounds, extendedState);
        SwingUtilities.invokeLater(this::initDatabase);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("健身房會員系統", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 32f));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("帳號"), gbc);
        gbc.gridx = 1; form.add(accountField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; form.add(new JLabel("密碼"), gbc);
        gbc.gridx = 1; form.add(passwordField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; form.add(new JLabel("身份"), gbc);
        gbc.gridx = 1; form.add(roleBox, gbc);

        root.add(form, BorderLayout.CENTER);

        JButton loginButton = new JButton("登入");
        JButton initButton = new JButton("建立/檢查 SQLite 資料庫");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        buttons.add(initButton);
        buttons.add(loginButton);

        JPanel south = new JPanel(new BorderLayout());
        south.add(buttons, BorderLayout.CENTER);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        south.add(statusLabel, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        initButton.addActionListener(e -> initDatabase());
        loginButton.addActionListener(e -> login());
        accountField.addActionListener(e -> login());
        passwordField.addActionListener(e -> login());
        roleBox.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "login");
        roleBox.getActionMap().put("login", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                login();
            }
        });
        getRootPane().setDefaultButton(loginButton);
        setContentPane(root);
    }

    private void initDatabase() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            DatabaseInitializer.ensureSchema();
            statusLabel.setText("SQLite 資料庫檢查完成，可登入。預設：admin/admin123、trainer/trainer123、member/member123");
        } catch (Exception ex) {
            UiUtil.error(this, "SQLite 資料庫初始化失敗：" + ex.getMessage() + "\n請確認 sqlite-jdbc 在 classpath，且 db.properties 的 db.url 設定正確。");
            statusLabel.setText("SQLite 資料庫初始化失敗");
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void login() {
        try {
            Role role = Role.fromDisplayName((String) roleBox.getSelectedItem());
            User user = authService.login(accountField.getText(), new String(passwordField.getPassword()), role);
            Rectangle bounds = getBounds();
            int state = getExtendedState();
            SwingUtilities.invokeLater(() -> {
                MainFrame main = new MainFrame(user, bounds, state);
                main.setVisible(true);
                dispose();
            });
        } catch (Exception ex) {
            UiUtil.error(this, ex);
        }
    }
}
