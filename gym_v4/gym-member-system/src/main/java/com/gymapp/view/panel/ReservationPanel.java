package com.gymapp.view.panel;

import com.gymapp.model.*;
import com.gymapp.service.ReservationService;
import com.gymapp.util.DateTimeUtil;
import com.gymapp.util.UiUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationPanel extends BasePanel {
    private final User user;
    private final ReservationService reservationService = new ReservationService();
    private final DefaultTableModel courseModel = UiUtil.readOnlyModel(new String[]{"課程ID", "名稱", "類型", "教練ID", "場館ID", "時間", "已報名", "上限", "剩餘", "點數"});
    private final DefaultTableModel reservationModel = UiUtil.readOnlyModel(new String[]{"預約ID", "會員ID", "課程ID", "狀態", "扣點", "建立時間"});
    private final DefaultTableModel waitlistModel = UiUtil.readOnlyModel(new String[]{"候補ID", "課程ID", "會員ID", "狀態", "加入時間"});
    private final JTable courseTable = new JTable(courseModel);
    private final JTable reservationTable = new JTable(reservationModel);
    private final JTable waitlistTable = new JTable(waitlistModel);
    private final JTextField memberIdField = new JTextField(8);

    public ReservationPanel(User user) {
        this.user = user;
        buildUi();
        refreshData();
    }

    private void buildUi() {
        if (user.hasRole(Role.MEMBER)) {
            memberIdField.setText(String.valueOf(user.getId()));
            memberIdField.setEditable(false);
        }

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createTitledBorder("課程探索 / 預約"));
        top.add(scroll(courseTable), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("會員ID"));
        controls.add(memberIdField);
        JButton reserve = new JButton("預約選取課程 / 額滿自動候補");
        JButton cancel = new JButton("取消選取預約");
        JButton refresh = new JButton("刷新");
        controls.add(reserve);
        controls.add(cancel);
        controls.add(refresh);
        top.add(controls, BorderLayout.SOUTH);

        JSplitPane bottom = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        bottom.setResizeWeight(0.65);
        JPanel reservationPanel = new JPanel(new BorderLayout());
        reservationPanel.setBorder(BorderFactory.createTitledBorder(user.hasRole(Role.MEMBER) ? "我的預約" : "預約紀錄"));
        reservationPanel.add(scroll(reservationTable), BorderLayout.CENTER);
        JPanel waitlistPanel = new JPanel(new BorderLayout());
        waitlistPanel.setBorder(BorderFactory.createTitledBorder("所選課程候補名單"));
        waitlistPanel.add(scroll(waitlistTable), BorderLayout.CENTER);
        bottom.setLeftComponent(reservationPanel);
        bottom.setRightComponent(waitlistPanel);

        JSplitPane root = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        root.setResizeWeight(0.55);
        add(root, BorderLayout.CENTER);

        reserve.addActionListener(e -> reserveCourse());
        cancel.addActionListener(e -> cancelReservation());
        refresh.addActionListener(e -> refreshData());
        courseTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && courseTable.getSelectedRow() >= 0) refreshWaitlist();
        });
    }

    private void reserveCourse() {
        try {
            int memberId = UiUtil.intValue(memberIdField.getText(), "會員ID");
            int courseId = selectedId(courseTable, 0);
            String result = reservationService.reserve(memberId, courseId);
            UiUtil.info(this, result);
            refreshData();
        } catch (Exception e) { showError(e); }
    }

    private void cancelReservation() {
        try {
            int reservationId = selectedId(reservationTable, 0);
            if (UiUtil.confirm(this, "確定取消預約單號 " + reservationId + "？")) {
                String result = reservationService.cancelReservation(reservationId);
                UiUtil.info(this, result);
                refreshData();
            }
        } catch (Exception e) { showError(e); }
    }

    private void refreshWaitlist() {
        try {
            int courseId = selectedId(courseTable, 0);
            List<Object[]> rows = new ArrayList<>();
            for (WaitlistEntry w : reservationService.getWaitlistDAO().findByCourse(courseId)) {
                rows.add(new Object[]{w.getWaitlistId(), w.getCourseId(), w.getMemberId(), w.getStatus(), DateTimeUtil.format(w.getCreatedTime())});
            }
            setRows(waitlistModel, rows);
        } catch (Exception e) {
            setRows(waitlistModel, new ArrayList<>());
        }
    }

    @Override
    public void refreshData() {
        try {
            List<Object[]> courseRows = new ArrayList<>();
            for (GymClass c : reservationService.getCourseDAO().findAll()) {
                courseRows.add(new Object[]{c.getCourseId(), c.getCourseName(), c.getCourseType(), c.getTrainerId() == null ? "" : c.getTrainerId(),
                        c.getBranchId() == null ? "" : c.getBranchId(), DateTimeUtil.format(c.getScheduleTime()), c.getEnrolledCount(), c.getMaxCapacity(), c.getRemainingSpots(), c.getPointsRequired()});
            }
            setRows(courseModel, courseRows);

            List<Reservation> reservations = user.hasRole(Role.MEMBER)
                    ? reservationService.getReservationDAO().findByMember(user.getId())
                    : reservationService.getReservationDAO().findAll();
            List<Object[]> reservationRows = new ArrayList<>();
            for (Reservation r : reservations) {
                reservationRows.add(new Object[]{r.getReservationId(), r.getMemberId(), r.getCourseId(), r.getStatus(), r.getPointsDeducted(), DateTimeUtil.format(r.getCreatedTime())});
            }
            setRows(reservationModel, reservationRows);
            refreshWaitlist();
        } catch (Exception e) { showError(e); }
    }
}
