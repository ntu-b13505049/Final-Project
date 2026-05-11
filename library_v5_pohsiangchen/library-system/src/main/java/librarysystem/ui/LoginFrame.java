package librarysystem.ui;

import librarysystem.model.Admin;
import librarysystem.model.User;
import librarysystem.service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class LoginFrame extends JFrame {
    private final AuthService authService = new AuthService();

    public LoginFrame() {
        setTitle("圖書館借還書系統");
        setSize(640, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 6, 18));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Java 圖書館借還書系統", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("學生 / 管理者雙角色、借還紀錄、提醒、預約、書評、等級申請、Web 報表", SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setAlignmentX(CENTER_ALIGNMENT);

        panel.add(title);
        panel.add(Box.createVerticalStrut(8));
        panel.add(subtitle);
        return panel;
    }

    private JTabbedPane createCenterPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));
        tabbedPane.addTab("學生登入", createUserLoginPanel());
        tabbedPane.addTab("學生註冊", createRegisterPanel());
        tabbedPane.addTab("管理者登入", createAdminLoginPanel());
        return tabbedPane;
    }

    private JPanel createUserLoginPanel() {
        JPanel panel = buildFormPanel();
        JTextField studentNoField = new JTextField(18);
        JPasswordField passwordField = new JPasswordField(18);
        JButton loginButton = new JButton("登入使用者系統");
        loginButton.setPreferredSize(new Dimension(180, 36));

        addRow(panel, 0, "學號", studentNoField);
        addRow(panel, 1, "密碼", passwordField);
        addButtonRow(panel, 2, loginButton);

        loginButton.addActionListener(e -> {
            try {
                User user = authService.loginUser(studentNoField.getText(), new String(passwordField.getPassword()));
                new UserDashboardFrame(user).setVisible(true);
                dispose();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = buildFormPanel();
        JTextField studentNoField = new JTextField(18);
        JTextField nameField = new JTextField(18);
        JPasswordField passwordField = new JPasswordField(18);
        JLabel roleHint = new JLabel("新帳號預設 NORMAL；VIP / GOLD / PLATINUM 需登入後送出申請，由管理者審核。");
        JButton registerButton = new JButton("註冊並建立帳號");
        registerButton.setPreferredSize(new Dimension(180, 36));

        addRow(panel, 0, "學號", studentNoField);
        addRow(panel, 1, "姓名", nameField);
        addRow(panel, 2, "密碼", passwordField);
        addRow(panel, 3, "權限", roleHint);
        addButtonRow(panel, 4, registerButton);

        registerButton.addActionListener(e -> {
            try {
                authService.registerUser(
                        studentNoField.getText(),
                        nameField.getText(),
                        new String(passwordField.getPassword())
                );
                JOptionPane.showMessageDialog(this, "註冊成功，帳號預設為 NORMAL。請返回登入頁登入；升級可在使用者中心申請。", "成功", JOptionPane.INFORMATION_MESSAGE);
                studentNoField.setText("");
                nameField.setText("");
                passwordField.setText("");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        return panel;
    }

    private JPanel createAdminLoginPanel() {
        JPanel panel = buildFormPanel();
        JTextField usernameField = new JTextField(18);
        JPasswordField passwordField = new JPasswordField(18);
        JButton loginButton = new JButton("登入管理者系統");
        loginButton.setPreferredSize(new Dimension(180, 36));

        addRow(panel, 0, "管理者帳號", usernameField);
        addRow(panel, 1, "管理者密碼", passwordField);
        addButtonRow(panel, 2, loginButton);

        loginButton.addActionListener(e -> {
            try {
                Admin admin = authService.loginAdmin(usernameField.getText(), new String(passwordField.getPassword()));
                new AdminDashboardFrame(admin).setVisible(true);
                dispose();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(4, 18, 18, 18));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel tip1 = new JLabel("初始學生資料改為從 data/Users.json 匯入", SwingConstants.CENTER);
        JLabel tip2 = new JLabel("例如：A12345678 / 2a9f8e7d6c5b4a3f2e1d9c8b7a（NORMAL；可申請升級）", SwingConstants.CENTER);
        JLabel tip3 = new JLabel("管理者：admin / admin123    librarian / lib123", SwingConstants.CENTER);
        tip1.setAlignmentX(CENTER_ALIGNMENT);
        tip2.setAlignmentX(CENTER_ALIGNMENT);
        tip3.setAlignmentX(CENTER_ALIGNMENT);
        tip1.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tip2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tip3.setFont(new Font("SansSerif", Font.PLAIN, 13));

        panel.add(tip1);
        panel.add(Box.createVerticalStrut(4));
        panel.add(tip2);
        panel.add(Box.createVerticalStrut(4));
        panel.add(tip3);
        return panel;
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        return panel;
    }

    private void addRow(JPanel panel, int row, String labelText, java.awt.Component field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(labelText + "："), gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private void addButtonRow(JPanel panel, int row, JButton button) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 8, 8, 8);
        panel.add(button, gbc);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "操作失敗", JOptionPane.ERROR_MESSAGE);
    }
}
