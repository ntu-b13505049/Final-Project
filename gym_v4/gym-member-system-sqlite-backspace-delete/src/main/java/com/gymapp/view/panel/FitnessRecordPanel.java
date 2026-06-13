package com.gymapp.view.panel;

import com.gymapp.model.*;
import com.gymapp.service.FitnessService;
import com.gymapp.util.DateTimeUtil;
import com.gymapp.util.UiUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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
    private final JTextField workoutLogIdField = new JTextField(8);
    private final JTextField exerciseField = new JTextField(12);
    private final JTextField logWeightField = new JTextField("20", 6);
    private final JTextField repsField = new JTextField("12", 6);
    private JTable activeTable = recordTable;

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
        workoutLogIdField.setEditable(false);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.62);
        JPanel records = new JPanel(new BorderLayout());
        records.setBorder(BorderFactory.createTitledBorder("健身狀況紀錄（體重、體脂、肌肉量、訓練內容與建議）"));
        records.add(tablePanel(recordTable, "輸入紀錄ID、會員ID、教練ID、體重、訓練內容或建議"), BorderLayout.CENTER);
        JPanel logs = new JPanel(new BorderLayout());
        logs.setBorder(BorderFactory.createTitledBorder("運動執行紀錄"));
        logs.add(tablePanel(logTable, "輸入流水號、會員ID、動作、重量、次數或時間"), BorderLayout.CENTER);
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
        UiUtil.addField(form, 8, "動作流水號", workoutLogIdField);
        UiUtil.addField(form, 9, "動作名稱", exerciseField);
        UiUtil.addField(form, 10, "動作重量", logWeightField);
        UiUtil.addField(form, 11, "實際次數", repsField);

        JButton add = new JButton("新增身體紀錄");
        JButton addWorkout = new JButton("新增動作紀錄");
        JButton editDelete = new JButton("修改/刪除選取");
        JButton refresh = new JButton("刷新");
        editDelete.setToolTipText("先選取身體紀錄或動作紀錄，修改右側欄位後按此鍵；也可在表格選取列後按 Delete 或 Backspace 直接刪除。");
        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        buttons.add(add);
        buttons.add(addWorkout);
        buttons.add(editDelete);
        buttons.add(refresh);
        JPanel east = new JPanel(new BorderLayout());
        east.setBorder(BorderFactory.createTitledBorder("紀錄輸入"));
        east.add(form, BorderLayout.CENTER);
        east.add(buttons, BorderLayout.SOUTH);
        add(east, BorderLayout.EAST);

        registerActiveTable(recordTable);
        registerActiveTable(logTable);
        bindDeleteShortcut(recordTable, this::deleteSelectedFitnessRecord);
        bindDeleteShortcut(logTable, this::deleteSelectedWorkoutLog);

        recordTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && recordTable.getSelectedRow() >= 0) {
                activeTable = recordTable;
                fillRecordFromSelected();
            }
        });
        logTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && logTable.getSelectedRow() >= 0) {
                activeTable = logTable;
                fillWorkoutLogFromSelected();
            }
        });
        add.addActionListener(e -> addRecord());
        addWorkout.addActionListener(e -> addWorkoutLog());
        editDelete.addActionListener(e -> editOrDeleteSelected());
        refresh.addActionListener(e -> refreshData());
    }

    private void registerActiveTable(JTable table) {
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                activeTable = table;
            }
        });
        table.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                activeTable = table;
            }
        });
    }

    private void bindDeleteShortcut(JTable table, Runnable deleteAction) {
        UiUtil.installDeleteShortcut(table, () -> {
            activeTable = table;
            deleteAction.run();
        });
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

    private WorkoutLog readWorkoutLog(boolean needId) {
        WorkoutLog log = new WorkoutLog();
        if (needId) log.setLogId(UiUtil.intValue(workoutLogIdField.getText(), "動作流水號"));
        log.setMemberId(UiUtil.intValue(memberIdField.getText(), "會員ID"));
        log.setExerciseName(exerciseField.getText().trim());
        log.setWeight(UiUtil.floatValue(logWeightField.getText(), "動作重量"));
        log.setReps(UiUtil.intValue(repsField.getText(), "實際次數"));
        if (log.getExerciseName().isBlank()) throw new IllegalArgumentException("動作名稱不可空白");
        return log;
    }

    private void fillRecordFromSelected() {
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

    private void fillWorkoutLogFromSelected() {
        int r = logTable.convertRowIndexToModel(logTable.getSelectedRow());
        workoutLogIdField.setText(String.valueOf(logModel.getValueAt(r, 0)));
        memberIdField.setText(String.valueOf(logModel.getValueAt(r, 1)));
        exerciseField.setText(String.valueOf(logModel.getValueAt(r, 2)));
        logWeightField.setText(String.valueOf(logModel.getValueAt(r, 3)));
        repsField.setText(String.valueOf(logModel.getValueAt(r, 4)));
    }

    private void addRecord() {
        try {
            fitnessService.getFitnessRecordDAO().insert(readRecord(false));
            refreshData();
        } catch (Exception e) { showError(e); }
    }

    private void updateRecord() {
        try {
            if (recordTable.getSelectedRow() < 0) throw new IllegalArgumentException("請先選取要修改的身體紀錄");
            fitnessService.getFitnessRecordDAO().update(readRecord(true));
            refreshData();
            UiUtil.info(this, "身體紀錄已更新");
        } catch (Exception e) { showError(e); }
    }

    private void deleteSelectedFitnessRecord() {
        try {
            int id = selectedId(recordTable, 0);
            if (UiUtil.confirm(this, "確定刪除身體紀錄 ID " + id + "？")) {
                fitnessService.getFitnessRecordDAO().delete(id);
                refreshData();
            }
        } catch (Exception e) { showError(e); }
    }

    private void addWorkoutLog() {
        try {
            fitnessService.getWorkoutLogDAO().insert(readWorkoutLog(false));
            refreshData();
        } catch (Exception e) { showError(e); }
    }

    private void updateWorkoutLog() {
        try {
            if (logTable.getSelectedRow() < 0) throw new IllegalArgumentException("請先選取要修改的動作紀錄");
            fitnessService.getWorkoutLogDAO().update(readWorkoutLog(true));
            refreshData();
            UiUtil.info(this, "動作紀錄已更新");
        } catch (Exception e) { showError(e); }
    }

    private void deleteSelectedWorkoutLog() {
        try {
            int id = selectedId(logTable, 0);
            if (UiUtil.confirm(this, "確定刪除動作紀錄流水號 " + id + "？")) {
                fitnessService.getWorkoutLogDAO().delete(id);
                refreshData();
            }
        } catch (Exception e) { showError(e); }
    }

    private void editOrDeleteSelected() {
        JTable target = resolveActiveTable();
        String itemName = target == logTable ? "動作紀錄" : "身體紀錄";
        Object[] options = {"修改", "刪除", "取消"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "請選擇要對選取的「" + itemName + "」執行的動作。\n\n修改：會使用右側目前欄位內容更新資料。\n刪除：也可以直接在表格選取列後按 Delete 或 Backspace。",
                "修改/刪除選取資料",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        if (choice == 0) {
            if (target == logTable) updateWorkoutLog(); else updateRecord();
        } else if (choice == 1) {
            if (target == logTable) deleteSelectedWorkoutLog(); else deleteSelectedFitnessRecord();
        }
    }

    private JTable resolveActiveTable() {
        if (activeTable == logTable && logTable.getSelectedRow() >= 0) return logTable;
        if (activeTable == recordTable && recordTable.getSelectedRow() >= 0) return recordTable;
        if (logTable.getSelectedRow() >= 0) return logTable;
        if (recordTable.getSelectedRow() >= 0) return recordTable;
        throw new IllegalArgumentException("請先選取身體紀錄或動作紀錄");
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
