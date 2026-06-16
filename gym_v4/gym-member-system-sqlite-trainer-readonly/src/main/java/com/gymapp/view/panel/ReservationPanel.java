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
        boolean isMember = user.hasRole(Role.MEMBER);
        boolean isTrainer = user.hasRole(Role.TRAINER);
        boolean canOperateReservation = !isTrainer;

        if (isMember) {
            memberIdField.setText(String.valueOf(user.getId()));
            memberIdField.setEditable(false);
        }

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setBackground(UiUtil.APP_BG);
        top.setBorder(BorderFactory.createTitledBorder(isTrainer ? "我的課程 / 學員預約檢視" : "課程探索 / 預約"));
        top.add(tablePanel(courseTable, isTrainer ? "輸入課程ID、名稱、類型、場館ID或時間" : "輸入課程ID、名稱、類型、教練ID、場館ID 或時間"), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setBackground(UiUtil.APP_BG);
        JButton refresh = new JButton("刷新");
        if (canOperateReservation) {
            controls.add(new JLabel("會員ID"));
            controls.add(memberIdField);
            JButton reserve = new JButton("預約選取課程 / 額滿自動候補");
            JButton cancel = new JButton("取消選取預約");
            controls.add(reserve);
            controls.add(cancel);
            reserve.addActionListener(e -> reserveCourse());
            cancel.addActionListener(e -> cancelReservation());
            UiUtil.installDeleteShortcut(reservationTable, this::cancelReservation);
        } else {
            JLabel readOnlyHint = new JLabel("教練僅可檢視自己的課程、學員預約與候補名單，不可代會員預約或取消。");
            readOnlyHint.setForeground(UiUtil.MUTED);
            controls.add(readOnlyHint);
        }
        controls.add(refresh);
        top.add(controls, BorderLayout.SOUTH);

        JSplitPane bottom = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        bottom.setResizeWeight(0.65);
        JPanel reservationPanel = new JPanel(new BorderLayout());
        reservationPanel.setBorder(BorderFactory.createTitledBorder(isMember ? "我的預約" : (isTrainer ? "我的課程學員預約" : "預約紀錄")));
        reservationPanel.add(tablePanel(reservationTable, "輸入預約ID、會員ID、課程ID、狀態、扣點或時間"), BorderLayout.CENTER);
        JPanel waitlistPanel = new JPanel(new BorderLayout());
        waitlistPanel.setBorder(BorderFactory.createTitledBorder("所選課程候補名單"));
        waitlistPanel.add(tablePanel(waitlistTable, "輸入候補ID、課程ID、會員ID、狀態或時間"), BorderLayout.CENTER);
        bottom.setLeftComponent(reservationPanel);
        bottom.setRightComponent(waitlistPanel);

        JSplitPane root = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        root.setResizeWeight(0.55);
        add(root, BorderLayout.CENTER);

        refresh.addActionListener(e -> refreshData());
        if (!isMember && !isTrainer) {
            UiUtil.onTextChanged(memberIdField, () -> {
                String text = memberIdField.getText().trim();
                if (text.isEmpty() || text.matches("\\d+")) {
                    refreshData();
                }
            });
        }
        courseTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && courseTable.getSelectedRow() >= 0) refreshWaitlist();
        });
    }

    private void reserveCourse() {
        try {
            if (user.hasRole(Role.TRAINER)) {
                throw new IllegalArgumentException("教練不能代會員預約課程");
            }
            int memberId = UiUtil.intValue(memberIdField.getText(), "會員ID");
            int courseId = selectedId(courseTable, 0);
            String result = reservationService.reserve(memberId, courseId);
            UiUtil.info(this, result);
            refreshData();
        } catch (Exception e) { showError(e); }
    }

    private void cancelReservation() {
        try {
            if (user.hasRole(Role.TRAINER)) {
                throw new IllegalArgumentException("教練不能代會員取消預約");
            }
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
            List<GymClass> courses = user.hasRole(Role.TRAINER)
                    ? reservationService.getCourseDAO().findByTrainer(user.getId())
                    : reservationService.getCourseDAO().findAll();
            for (GymClass c : courses) {
                courseRows.add(new Object[]{c.getCourseId(), c.getCourseName(), c.getCourseType(), c.getTrainerId() == null ? "" : c.getTrainerId(),
                        c.getBranchId() == null ? "" : c.getBranchId(), DateTimeUtil.format(c.getScheduleTime()), c.getEnrolledCount(), c.getMaxCapacity(), c.getRemainingSpots(), c.getPointsRequired()});
            }
            setRows(courseModel, courseRows);

            List<Reservation> reservations;
            if (user.hasRole(Role.MEMBER)) {
                reservations = reservationService.getReservationDAO().findByMember(user.getId());
            } else if (user.hasRole(Role.TRAINER)) {
                reservations = reservationService.getReservationDAO().findByTrainer(user.getId());
            } else if (memberIdField.getText() != null && !memberIdField.getText().isBlank()) {
                reservations = reservationService.getReservationDAO().findByMember(UiUtil.intValue(memberIdField.getText(), "會員ID"));
            } else {
                reservations = reservationService.getReservationDAO().findAll();
            }
            List<Object[]> reservationRows = new ArrayList<>();
            for (Reservation r : reservations) {
                reservationRows.add(new Object[]{r.getReservationId(), r.getMemberId(), r.getCourseId(), r.getStatus(), r.getPointsDeducted(), DateTimeUtil.format(r.getCreatedTime())});
            }
            setRows(reservationModel, reservationRows);
            refreshWaitlist();
        } catch (Exception e) { showError(e); }
    }
}
