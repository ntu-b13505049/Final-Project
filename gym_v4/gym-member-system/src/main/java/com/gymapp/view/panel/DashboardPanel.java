package com.gymapp.view.panel;

import com.gymapp.dao.CourseDAO;
import com.gymapp.model.Branch;
import com.gymapp.model.GymClass;
import com.gymapp.model.User;
import com.gymapp.service.BranchService;
import com.gymapp.util.DateTimeUtil;
import com.gymapp.util.UiUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DashboardPanel extends BasePanel {
    private final User user;
    private final BranchService branchService = new BranchService();
    private final CourseDAO courseDAO = new CourseDAO();
    private final DefaultTableModel branchModel = UiUtil.readOnlyModel(new String[]{"場館ID", "場館", "目前人數", "上限", "剩餘"});
    private final DefaultTableModel courseModel = UiUtil.readOnlyModel(new String[]{"課程ID", "課程", "類型", "教練ID", "場館ID", "時間", "已報名/上限", "點數"});

    public DashboardPanel(User user) {
        this.user = user;
        buildUi();
        refreshData();
    }

    private void buildUi() {
        JLabel title = new JLabel("系統總覽與場館容留人數動態計算");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        add(title, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.38);
        split.setTopComponent(panelWithTitle("場館即時人數（由進出場紀錄動態計算）", new JTable(branchModel)));
        split.setBottomComponent(panelWithTitle("近期課程", new JTable(courseModel)));
        add(split, BorderLayout.CENTER);

        JButton refresh = new JButton("重新整理");
        refresh.addActionListener(e -> refreshData());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.add(new JLabel("目前使用者：" + user.getName() + " / " + user.getDisplayRole()));
        south.add(refresh);
        add(south, BorderLayout.SOUTH);
    }

    private JPanel panelWithTitle(String title, JTable table) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(scroll(table), BorderLayout.CENTER);
        return p;
    }

    @Override
    public void refreshData() {
        try {
            List<Object[]> branches = new ArrayList<>();
            for (Branch b : branchService.findAllWithDynamicCapacity()) {
                branches.add(new Object[]{b.getBranchId(), b.getBranchName(), b.getCurrentCapacity(), b.getMaxCapacity(), b.getRemainingCapacity()});
            }
            setRows(branchModel, branches);

            List<Object[]> courses = new ArrayList<>();
            for (GymClass c : courseDAO.findAll()) {
                courses.add(new Object[]{c.getCourseId(), c.getCourseName(), c.getCourseType(), c.getTrainerId(), c.getBranchId(),
                        DateTimeUtil.format(c.getScheduleTime()), c.getEnrolledCount() + "/" + c.getMaxCapacity(), c.getPointsRequired()});
            }
            setRows(courseModel, courses);
        } catch (Exception e) {
            showError(e);
        }
    }
}
