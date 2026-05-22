package com.gymapp.view.panel;

import com.gymapp.dao.CourseDAO;
import com.gymapp.model.GymClass;
import com.gymapp.model.GymClassFactory;
import com.gymapp.util.DateTimeUtil;
import com.gymapp.util.UiUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CoursePanel extends BasePanel {
    private final Integer trainerFilter;
    private final boolean editable;
    private final CourseDAO courseDAO = new CourseDAO();
    private final DefaultTableModel model = UiUtil.readOnlyModel(new String[]{"課程ID", "名稱", "類型", "教練ID", "場館ID", "時間", "上限", "已報名", "點數", "剩餘"});
    private final JTable table = new JTable(model);
    private final JTextField idField = new JTextField(8);
    private final JTextField nameField = new JTextField(16);
    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{"團課", "一對一"});
    private final JTextField trainerField = new JTextField(8);
    private final JTextField branchField = new JTextField(8);
    private final JTextField timeField = new JTextField(16);
    private final JTextField capacityField = new JTextField("10", 8);
    private final JTextField enrolledField = new JTextField("0", 8);
    private final JTextField pointsField = new JTextField("100", 8);

    public CoursePanel(Integer trainerFilter, boolean editable) {
        this.trainerFilter = trainerFilter;
        this.editable = editable;
        buildUi();
        refreshData();
    }

    private void buildUi() {
        add(scroll(table), BorderLayout.CENTER);
        if (editable) {
            idField.setEditable(false);
            JPanel form = UiUtil.formPanel();
            UiUtil.addField(form, 0, "課程ID", idField);
            UiUtil.addField(form, 1, "名稱", nameField);
            UiUtil.addField(form, 2, "類型", typeBox);
            UiUtil.addField(form, 3, "教練ID", trainerField);
            UiUtil.addField(form, 4, "場館ID", branchField);
            UiUtil.addField(form, 5, "時間 yyyy-MM-dd HH:mm", timeField);
            UiUtil.addField(form, 6, "人數上限", capacityField);
            UiUtil.addField(form, 7, "已報名", enrolledField);
            UiUtil.addField(form, 8, "花費點數", pointsField);
            JButton add = new JButton("新增");
            JButton update = new JButton("修改");
            JButton delete = new JButton("刪除");
            JButton clear = new JButton("清空");
            JButton refresh = new JButton("刷新");
            JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
            buttons.add(add); buttons.add(update); buttons.add(delete); buttons.add(clear); buttons.add(refresh);
            JPanel east = new JPanel(new BorderLayout());
            east.setBorder(BorderFactory.createTitledBorder("課程資料"));
            east.add(form, BorderLayout.CENTER);
            east.add(buttons, BorderLayout.SOUTH);
            add(east, BorderLayout.EAST);
            table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) fillFromSelected(); });
            add.addActionListener(e -> addCourse());
            update.addActionListener(e -> updateCourse());
            delete.addActionListener(e -> deleteCourse());
            clear.addActionListener(e -> clearForm());
            refresh.addActionListener(e -> refreshData());
        } else {
            JButton refresh = new JButton("刷新我的排課");
            refresh.addActionListener(e -> refreshData());
            JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
            south.add(new JLabel("僅顯示目前教練排課；新增/修改請由管理員操作。"));
            south.add(refresh);
            add(south, BorderLayout.SOUTH);
        }
    }

    private GymClass readForm(boolean needId) {
        int id = needId ? UiUtil.intValue(idField.getText(), "課程ID") : 0;
        String name = nameField.getText().trim();
        String type = String.valueOf(typeBox.getSelectedItem());
        Integer trainerId = UiUtil.nullableInt(trainerField.getText());
        Integer branchId = UiUtil.nullableInt(branchField.getText());
        int capacity = UiUtil.intValue(capacityField.getText(), "人數上限");
        int enrolled = UiUtil.intValue(enrolledField.getText(), "已報名");
        int points = UiUtil.intValue(pointsField.getText(), "點數");
        if (name.isBlank()) throw new IllegalArgumentException("課程名稱不可空白");
        return GymClassFactory.create(id, name, type, trainerId, branchId, DateTimeUtil.parseDateTime(timeField.getText()), capacity, enrolled, points);
    }

    private void fillFromSelected() {
        int r = table.convertRowIndexToModel(table.getSelectedRow());
        idField.setText(String.valueOf(model.getValueAt(r, 0)));
        nameField.setText(String.valueOf(model.getValueAt(r, 1)));
        typeBox.setSelectedItem(String.valueOf(model.getValueAt(r, 2)));
        trainerField.setText(String.valueOf(model.getValueAt(r, 3)));
        branchField.setText(String.valueOf(model.getValueAt(r, 4)));
        timeField.setText(String.valueOf(model.getValueAt(r, 5)));
        capacityField.setText(String.valueOf(model.getValueAt(r, 6)));
        enrolledField.setText(String.valueOf(model.getValueAt(r, 7)));
        pointsField.setText(String.valueOf(model.getValueAt(r, 8)));
    }

    private void addCourse() {
        try {
            courseDAO.insert(readForm(false));
            refreshData();
            clearForm();
            UiUtil.info(this, "新增課程成功");
        } catch (Exception e) { showError(e); }
    }

    private void updateCourse() {
        try {
            courseDAO.update(readForm(true));
            refreshData();
            UiUtil.info(this, "修改課程成功");
        } catch (Exception e) { showError(e); }
    }

    private void deleteCourse() {
        try {
            int id = selectedId(table, 0);
            if (UiUtil.confirm(this, "確定刪除課程 ID " + id + "？")) {
                courseDAO.delete(id);
                refreshData();
                clearForm();
            }
        } catch (Exception e) { showError(e); }
    }

    private void clearForm() {
        idField.setText(""); nameField.setText(""); typeBox.setSelectedItem("團課"); trainerField.setText(""); branchField.setText(""); timeField.setText("2026-03-10 18:30"); capacityField.setText("10"); enrolledField.setText("0"); pointsField.setText("100");
    }

    @Override
    public void refreshData() {
        try {
            List<GymClass> courses = trainerFilter == null ? courseDAO.findAll() : courseDAO.findByTrainer(trainerFilter);
            List<Object[]> rows = new ArrayList<>();
            for (GymClass c : courses) {
                rows.add(new Object[]{c.getCourseId(), c.getCourseName(), c.getCourseType(), c.getTrainerId() == null ? "" : c.getTrainerId(),
                        c.getBranchId() == null ? "" : c.getBranchId(), DateTimeUtil.format(c.getScheduleTime()), c.getMaxCapacity(), c.getEnrolledCount(), c.getPointsRequired(), c.getRemainingSpots()});
            }
            setRows(model, rows);
        } catch (Exception e) { showError(e); }
    }
}
