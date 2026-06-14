package librarysystem;

import librarysystem.ui.LoginFrame;
import librarysystem.util.UiUtil;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class App {
    public static void main(String[] args) {
        UiUtil.installModernLookAndFeel();
        UiUtil.applyGlobalFont(17f);

        try {
            Database.initialize();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "資料庫初始化失敗。\n請確認已加入 sqlite-jdbc 依賴後再執行。\n\n錯誤：" + e.getMessage(),
                    "啟動失敗", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return;
        }

        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
