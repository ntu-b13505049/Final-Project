package com.gymapp.view.panel;

import com.gymapp.model.*;
import com.gymapp.service.FitnessService;
import com.gymapp.util.DateTimeUtil;
import com.gymapp.util.UiUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FitnessRecordPanel extends BasePanel {
    private final User user;
    private final FitnessService fitnessService = new FitnessService();
    private final DefaultTableModel recordModel = UiUtil.readOnlyModel(new String[]{"紀錄ID", "會員ID", "教練ID", "體重", "體脂", "肌肉量", "訓練內容", "建議", "時間"});
    private final DefaultTableModel logModel = UiUtil.readOnlyModel(new String[]{"流水號", "會員ID", "動作", "重量", "次數", "時間"});
    private final JTable recordTable = new JTable(recordModel);
    private final JTable logTable = new JTable(logModel);
    private final JTextField recordIdField = new JTextField(8);
    private final JTextField memberIdField = new JTextField(8);
    private final JTextField trainerIdField = new JTextField(8);
    private final JTextField weightField = new JTextField("70", 8);
    private final JTextField fatField = new JTextField("20", 8);
    private final JTextField muscleField = new JTextField("30", 8);
    private final JTextField trainingField = new JTextField(18);
    private final JTextField suggestionField = new JTextField(18);
    private final JTextField exerciseField = new JTextField(12);
    private final JTextField logWeightField = new JTextField("20", 6);
    private final JTextField repsField = new JTextField("12", 6);

    public FitnessRecordPanel(User user) {
        this.user = user;
        buildUi();
        refreshData();
    }

    private void buildUi() {
        if (user.hasRole(Role.MEMBER)) {
            memberIdField.setText(String.valueOf(user.getId()));
            memberIdField.setEditable(false);
        }
        if (user.hasRole(Role.TRAINER)) {
            trainerIdField.setText(String.valueOf(user.getId()));
            trainerIdField.setEditable(false);
        }
        recordIdField.setEditable(false);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.62);
        JPanel records = new JPanel(new BorderLayout());
        records.setBorder(BorderFactory.createTitledBorder("健身狀況紀錄（體重、體脂、肌肉量、訓練內容與建議）"));
        records.add(scroll(recordTable), BorderLayout.CENTER);
        JPanel logs = new JPanel(new BorderLayout());
        logs.setBorder(BorderFactory.createTitledBorder("運動執行紀錄"));
        logs.add(scroll(logTable), BorderLayout.CENTER);
        split.setTopComponent(records);
        split.setBottomComponent(logs);
        add(split, BorderLayout.CENTER);

        JPanel form = UiUtil.formPanel();
        UiUtil.addField(form, 0, "紀錄ID", recordIdField);
        UiUtil.addField(form, 1, "會員ID", memberIdField);
        UiUtil.addField(form, 2, "教練ID", trainerIdField);
        UiUtil.addField(form, 3, "體重kg", weightField);
        UiUtil.addField(form, 4, "體脂%", fatField);
        UiUtil.addField(form, 5, "肌肉量kg", muscleField);
        UiUtil.addField(form, 6, "訓練內容", trainingField);
        UiUtil.addField(form, 7, "建議", suggestionField);
        UiUtil.addField(form, 8, "動作名稱", exerciseField);
        UiUtil.addField(form, 9, "動作重量", logWeightField);
        UiUtil.addField(form, 10, "實際次數", repsField);
        JButton add = new JButton("新增身體紀錄");
        JButton update = new JButton("修改身體紀錄");
        JButton delete = new JButton("刪除身體紀錄");
        JButton addWorkout = new JButton("新增動作紀錄");
        JButton refresh = new JButton("刷新");
        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        buttons.add(add); buttons.add(update); buttons.add(delete); buttons.add(addWorkout); buttons.add(refresh);
        JPanel east = new JPanel(new BorderLayout());
        east.setBorder(BorderFactory.createTitledBorder("紀錄輸入"));
        east.add(form, BorderLayout.CENTER);
        east.add(buttons, BorderLayout.SOUTH);
        add(east, BorderLayout.EAST);

        recordTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && recordTable.getSelectedRow() >= 0) fillFromSelected(); });
        add.addActionListener(e -> addRecord());
        update.addActionListener(e -> updateRecord());
        delete.addActionListener(e -> deleteRecord());
        addWorkout.addActionListener(e -> addWorkoutLog());
        refresh.addActionListener(e -> refreshData());
    }

    private FitnessRecord readRecord(boolean needId) {
        FitnessRecord r = new FitnessRecord();
        if (needId) r.setRecordId(UiUtil.intValue(recordIdField.getText(), "紀錄ID"));
        r.setMemberId(UiUtil.intValue(memberIdField.getText(), "會員ID"));
        r.setTrainerId(UiUtil.nullableInt(trainerIdField.getText()));
        r.setWeightKg(UiUtil.floatValue(weightField.getText(), "體重"));
        r.setBodyFat(UiUtil.floatValue(fatField.getText(), "體脂"));
        r.setMuscleMass(UiUtil.floatValue(muscleField.getText(), "肌肉量"));
        r.setTrainingContent(trainingField.getText().trim());
        r.setSuggestion(suggestionField.getText().trim());
        return r;
    }

    private void fillFromSelected() {
        int r = recordTable.convertRowIndexToModel(recordTable.getSelectedRow());
        recordIdField.setText(String.valueOf(recordModel.getValueAt(r, 0)));
        memberIdField.setText(String.valueOf(recordModel.getValueAt(r, 1)));
        trainerIdField.setText(String.valueOf(recordModel.getValueAt(r, 2)));
        weightField.setText(String.valueOf(recordModel.getValueAt(r, 3)));
        fatField.setText(String.valueOf(recordModel.getValueAt(r, 4)));
        muscleField.setText(String.valueOf(recordModel.getValueAt(r, 5)));
        trainingField.setText(String.valueOf(recordModel.getValueAt(r, 6)));
        suggestionField.setText(String.valueOf(recordModel.getValueAt(r, 7)));
    }

    private void addRecord() { try { fitnessService.getFitnessRecordDAO().insert(readRecord(false)); refreshData(); } catch (Exception e) { showError(e); } }
    private void updateRecord() { try { fitnessService.getFitnessRecordDAO().update(readRecord(true)); refreshData(); } catch (Exception e) { showError(e); } }
    private void deleteRecord() { try { int id = selectedId(recordTable, 0); if (UiUtil.confirm(this, "確定刪除紀錄 ID " + id + "？")) { fitnessService.getFitnessRecordDAO().delete(id); refreshData(); } } catch (Exception e) { showError(e); } }

    private void addWorkoutLog() {
        try {
            WorkoutLog log = new WorkoutLog();
            log.setMemberId(UiUtil.intValue(memberIdField.getText(), "會員ID"));
            log.setExerciseName(exerciseField.getText().trim());
            log.setWeight(UiUtil.floatValue(logWeightField.getText(), "動作重量"));
            log.setReps(UiUtil.intValue(repsField.getText(), "實際次數"));
            if (log.getExerciseName().isBlank()) throw new IllegalArgumentException("動作名稱不可空白");
            fitnessService.getWorkoutLogDAO().insert(log);
            refreshData();
        } catch (Exception e) { showError(e); }
    }

    @Override
    public void refreshData() {
        try {
            Integer memberFilter = user.hasRole(Role.MEMBER) ? user.getId() : null;
            List<Object[]> records = new ArrayList<>();
            for (FitnessRecord r : fitnessService.findRecords(memberFilter)) {
                records.add(new Object[]{r.getRecordId(), r.getMemberId(), r.getTrainerId() == null ? "" : r.getTrainerId(), r.getWeightKg(), r.getBodyFat(), r.getMuscleMass(), r.getTrainingContent(), r.getSuggestion(), DateTimeUtil.format(r.getRecordedAt())});
            }
            setRows(recordModel, records);
            List<Object[]> logs = new ArrayList<>();
            for (WorkoutLog l : fitnessService.findWorkoutLogs(memberFilter)) {
                logs.add(new Object[]{l.getLogId(), l.getMemberId(), l.getExerciseName(), l.getWeight(), l.getReps(), DateTimeUtil.format(l.getWorkoutTime())});
            }
            setRows(logModel, logs);
        } catch (Exception e) { showError(e); }
    }
}
