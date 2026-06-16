package com.gymapp.view;

import com.gymapp.model.Member;
import com.gymapp.model.Role;
import com.gymapp.model.Trainer;
import com.gymapp.model.User;
import com.gymapp.util.UiUtil;
import com.gymapp.view.panel.*;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private final User currentUser;
    private final JTabbedPane tabs = new JTabbedPane();

    public MainFrame(User currentUser) {
        this(currentUser, null, JFrame.NORMAL);
    }

    public MainFrame(User currentUser, Rectangle bounds, int extendedState) {
        super("健身房會員系統 - " + currentUser.getDisplayRole() + "：" + currentUser.getName());
        this.currentUser = currentUser;
        buildUi();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        UiUtil.prepareFrame(this, bounds, extendedState);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiUtil.APP_BG);
        root.add(header(), BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
        buildTabsByRole();
        tabs.addChangeListener(e -> refreshSelectedTab());
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(UiUtil.PRIMARY);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JPanel texts = new JPanel(new GridLayout(0, 1, 2, 2));
        texts.setOpaque(false);
        JLabel title = new JLabel("健身房會員系統");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        JLabel label = new JLabel("登入者：" + currentUser.getName() + " / " + currentUser.getDisplayRole());
        label.setForeground(new Color(219, 234, 254));
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
        texts.add(title);
        texts.add(label);

        JButton logout = new JButton("登出");
        logout.addActionListener(e -> {
            Rectangle bounds = getBounds();
            int state = getExtendedState();
            new LoginFrame(new com.gymapp.service.AuthService(), bounds, state).setVisible(true);
            dispose();
        });
        UiUtil.styleButton(logout);

        panel.add(texts, BorderLayout.WEST);
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
            tabs.addTab("學員預約檢視", new ReservationPanel(currentUser));
            tabs.addTab("健身紀錄", new FitnessRecordPanel(currentUser));
            tabs.addTab("後續追蹤", new FollowUpPanel(currentUser));
            tabs.addTab("場館人數", new BranchPanel(false));
        } else {
            Member member = (Member) currentUser;
            tabs.addTab("個人資料", new ProfilePanel(member));
            tabs.addTab("課程預約", new ReservationPanel(currentUser));
            tabs.addTab("錢包紀錄", new WalletPanel(currentUser));
            tabs.addTab("商品購買", new ProductPanel(currentUser));
            tabs.addTab("健身紀錄", new FitnessRecordPanel(currentUser));
            tabs.addTab("追蹤建議", new FollowUpPanel(currentUser));
            tabs.addTab("進出場", new AccessPanel(currentUser));
        }
    }

    public void refreshSelectedTab() {
        Component selected = tabs.getSelectedComponent();
        if (selected instanceof Refreshable refreshable) {
            refreshable.refreshData();
        }
    }

    public void refreshAllTabs() {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component component = tabs.getComponentAt(i);
            if (component instanceof Refreshable refreshable) {
                refreshable.refreshData();
            }
        }
    }
}
