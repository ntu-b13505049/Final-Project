package com.gymapp;

import com.gymapp.service.AuthService;
import com.gymapp.util.UiUtil;
import com.gymapp.view.LoginFrame;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            UiUtil.applyGlobalFont(UiUtil.BASE_FONT_SIZE);
            new LoginFrame(new AuthService()).setVisible(true);
        });
    }
}
