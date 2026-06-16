package com.gymapp;

import com.gymapp.service.AuthService;
import com.gymapp.util.UiUtil;
import com.gymapp.view.LoginFrame;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setupLookAndFeel();
            UiUtil.applyModernTheme();
            UiUtil.applyGlobalFont(UiUtil.BASE_FONT_SIZE);
            new LoginFrame(new AuthService()).setVisible(true);
        });
    }

    private static void setupLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }
}
