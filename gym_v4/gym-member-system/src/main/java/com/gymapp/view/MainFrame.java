package com.gymapp.view;

import com.gymapp.model.Member;
import com.gymapp.model.Role;
import com.gymapp.model.Trainer;
import com.gymapp.model.User;
import com.gymapp.view.panel.*;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private final User currentUser;
    private final JTabbedPane tabs = new JTabbedPane();

    public MainFrame(User currentUser) {
        super("健身房會員系統 - " + currentUser.getDisplayRole() + "：" + currentUser.getName());
        this.currentUser = currentUser;
        buildUi();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        add(header(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        buildTabsByRole();
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        JLabel label = new JLabel("登入者：" + currentUser.getName() + " / " + currentUser.getDisplayRole());
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
        JButton logout = new JButton("登出");
        logout.addActionListener(e -> {
            new LoginFrame(new com.gymapp.service.AuthService()).setVisible(true);
            dispose();
        });
        panel.add(label, BorderLayout.WEST);
        panel.add(logout, BorderLayout.EAST);
        return panel;
    }

    private void buildTabsByRole() {
        tabs.addTab("首頁總覽", new DashboardPanel(currentUser));
        if (currentUser.hasRole(Role.ADMIN)) {
            tabs.addTab("會員管理", new MemberPanel());
            tabs.addTab("教練管理", new TrainerPanel());
            tabs.addTab("課程管理", new CoursePanel(null, true));
            tabs.addTab("預約/候補", new ReservationPanel(currentUser));
            tabs.addTab("場館管理", new BranchPanel());
            tabs.addTab("器材管理", new EquipmentPanel());
            tabs.addTab("儲值方案", new WalletPanel(currentUser));
            tabs.addTab("商品販售", new ProductPanel(currentUser));
            tabs.addTab("健身紀錄", new FitnessRecordPanel(currentUser));
            tabs.addTab("後續追蹤", new FollowUpPanel(currentUser));
            tabs.addTab("進出場", new AccessPanel(currentUser));
        } else if (currentUser.hasRole(Role.TRAINER)) {
            Integer trainerId = ((Trainer) currentUser).getId();
            tabs.addTab("我的排課", new CoursePanel(trainerId, false));
            tabs.addTab("會員預約", new ReservationPanel(currentUser));
            tabs.addTab("健身紀錄", new FitnessRecordPanel(currentUser));
            tabs.addTab("後續追蹤", new FollowUpPanel(currentUser));
            tabs.addTab("場館人數", new BranchPanel(false));
        } else {
            Member member = (Member) currentUser;
            tabs.addTab("個人資料", new ProfilePanel(member));
            tabs.addTab("課程預約", new ReservationPanel(currentUser));
            tabs.addTab("電子錢包", new WalletPanel(currentUser));
            tabs.addTab("商品購買", new ProductPanel(currentUser));
            tabs.addTab("健身紀錄", new FitnessRecordPanel(currentUser));
            tabs.addTab("追蹤建議", new FollowUpPanel(currentUser));
            tabs.addTab("進出場", new AccessPanel(currentUser));
        }
    }
}
