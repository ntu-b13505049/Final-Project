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
    private final JTextField accountField = new JTextField("admin", 20);
    private final JPasswordField passwordField = new JPasswordField("admin123", 20);
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
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(UiUtil.APP_BG);
        root.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        JPanel card = UiUtil.cardPanel(new BorderLayout(18, 18));
        card.setPreferredSize(new Dimension(520, 500));

        JPanel heading = new JPanel(new BorderLayout(6, 6));
        heading.setBackground(UiUtil.SURFACE);
        JLabel title = new JLabel("健身房會員系統", SwingConstants.CENTER);
        title.setForeground(UiUtil.TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 34f));
        JLabel subtitle = new JLabel("會員、課程、場館、器材與紀錄管理", SwingConstants.CENTER);
        subtitle.setForeground(UiUtil.MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(16f));
        heading.add(title, BorderLayout.CENTER);
        heading.add(subtitle, BorderLayout.SOUTH);
        card.add(heading, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UiUtil.SURFACE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addLoginField(form, gbc, 0, "帳號", accountField);
        addLoginField(form, gbc, 1, "密碼", passwordField);
        addLoginField(form, gbc, 2, "身份", roleBox);
        card.add(form, BorderLayout.CENTER);

        JButton loginButton = new JButton("登入");
        JButton initButton = new JButton("建立/檢查 SQLite 資料庫");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        buttons.setBackground(UiUtil.SURFACE);
        buttons.add(initButton);
        buttons.add(loginButton);

        JPanel south = new JPanel(new BorderLayout(8, 8));
        south.setBackground(UiUtil.SURFACE);
        south.add(buttons, BorderLayout.CENTER);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setForeground(UiUtil.MUTED);
        south.add(statusLabel, BorderLayout.SOUTH);
        card.add(south, BorderLayout.SOUTH);

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

        root.add(card);
        setContentPane(root);
        UiUtil.polishTree(root);
    }

    private void addLoginField(JPanel form, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(UiUtil.PRIMARY_DARK);
        labelComponent.setFont(labelComponent.getFont().deriveFont(Font.BOLD, 16f));
        form.add(labelComponent, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        form.add(field, gbc);
    }

    private void initDatabase() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            DatabaseInitializer.ensureSchema();
            statusLabel.setText("SQLite 資料庫檢查完成，可登入。預設：admin/admin123、trainer/trainer123、member/member123");
            statusLabel.setForeground(new Color(22, 101, 52));
        } catch (Exception ex) {
            UiUtil.error(this, "SQLite 資料庫初始化失敗：" + ex.getMessage() + "\n請確認 sqlite-jdbc 在 classpath，且 db.properties 的 db.url 設定正確。");
            statusLabel.setText("SQLite 資料庫初始化失敗");
            statusLabel.setForeground(UiUtil.DANGER);
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
