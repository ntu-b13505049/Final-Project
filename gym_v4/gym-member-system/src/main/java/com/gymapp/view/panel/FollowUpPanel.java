package com.gymapp.view.panel;

import com.gymapp.model.FollowUpRecord;
import com.gymapp.model.Role;
import com.gymapp.model.User;
import com.gymapp.service.FollowUpService;
import com.gymapp.util.DateTimeUtil;
import com.gymapp.util.UiUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FollowUpPanel extends BasePanel {
    private final User user;
    private final FollowUpService followUpService = new FollowUpService();
    private final DefaultTableModel model = UiUtil.readOnlyModel(new String[]{"追蹤ID", "會員ID", "教練ID", "目標", "目前狀況", "下次日期", "建議", "建立時間"});
    private final JTable table = new JTable(model);
    private final JTextField idField = new JTextField(8);
    private final JTextField memberIdField = new JTextField(8);
    private final JTextField trainerIdField = new JTextField(8);
    private final JTextField goalField = new JTextField(18);
    private final JTextField currentField = new JTextField(18);
    private final JTextField nextDateField = new JTextField("2026-04-01", 12);
    private final JTextField suggestionField = new JTextField(18);

    public FollowUpPanel(User user) {
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
        idField.setEditable(false);
        add(scroll(table), BorderLayout.CENTER);
        JPanel form = UiUtil.formPanel();
        UiUtil.addField(form, 0, "追蹤ID", idField);
        UiUtil.addField(form, 1, "會員ID", memberIdField);
        UiUtil.addField(form, 2, "教練ID", trainerIdField);
        UiUtil.addField(form, 3, "目標", goalField);
        UiUtil.addField(form, 4, "目前狀況", currentField);
        UiUtil.addField(form, 5, "下次追蹤 yyyy-MM-dd", nextDateField);
        UiUtil.addField(form, 6, "飲食/訓練建議", suggestionField);
        JButton add = new JButton("新增追蹤");
        JButton update = new JButton("修改追蹤");
        JButton delete = new JButton("刪除追蹤");
        JButton refresh = new JButton("刷新");
        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        if (!user.hasRole(Role.MEMBER)) {
            buttons.add(add); buttons.add(update); buttons.add(delete);
        }
        buttons.add(refresh);
        JPanel east = new JPanel(new BorderLayout());
        east.setBorder(BorderFactory.createTitledBorder("後續追蹤管理"));
        east.add(form, BorderLayout.CENTER);
        east.add(buttons, BorderLayout.SOUTH);
        add(east, BorderLayout.EAST);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) fillFromSelected(); });
        add.addActionListener(e -> addRecord());
        update.addActionListener(e -> updateRecord());
        delete.addActionListener(e -> deleteRecord());
        refresh.addActionListener(e -> refreshData());
    }

    private FollowUpRecord readForm(boolean needId) {
        FollowUpRecord r = new FollowUpRecord();
        if (needId) r.setFollowId(UiUtil.intValue(idField.getText(), "追蹤ID"));
        r.setMemberId(UiUtil.intValue(memberIdField.getText(), "會員ID"));
        r.setTrainerId(UiUtil.nullableInt(trainerIdField.getText()));
        r.setGoal(goalField.getText().trim());
        r.setCurrentStatus(currentField.getText().trim());
        r.setNextFollowDate(DateTimeUtil.parseDate(nextDateField.getText()));
        r.setSuggestion(suggestionField.getText().trim());
        return r;
    }

    private void fillFromSelected() {
        int r = table.convertRowIndexToModel(table.getSelectedRow());
        idField.setText(String.valueOf(model.getValueAt(r, 0)));
        memberIdField.setText(String.valueOf(model.getValueAt(r, 1)));
        trainerIdField.setText(String.valueOf(model.getValueAt(r, 2)));
        goalField.setText(String.valueOf(model.getValueAt(r, 3)));
        currentField.setText(String.valueOf(model.getValueAt(r, 4)));
        nextDateField.setText(String.valueOf(model.getValueAt(r, 5)));
        suggestionField.setText(String.valueOf(model.getValueAt(r, 6)));
    }

    private void addRecord() { try { followUpService.getFollowUpDAO().insert(readForm(false)); refreshData(); } catch (Exception e) { showError(e); } }
    private void updateRecord() { try { followUpService.getFollowUpDAO().update(readForm(true)); refreshData(); } catch (Exception e) { showError(e); } }
    private void deleteRecord() { try { int id = selectedId(table, 0); if (UiUtil.confirm(this, "確定刪除追蹤 ID " + id + "？")) { followUpService.getFollowUpDAO().delete(id); refreshData(); } } catch (Exception e) { showError(e); } }

    @Override
    public void refreshData() {
        try {
            Integer memberFilter = user.hasRole(Role.MEMBER) ? user.getId() : null;
            List<Object[]> rows = new ArrayList<>();
            for (FollowUpRecord r : followUpService.findRecords(memberFilter)) {
                rows.add(new Object[]{r.getFollowId(), r.getMemberId(), r.getTrainerId() == null ? "" : r.getTrainerId(), r.getGoal(), r.getCurrentStatus(), DateTimeUtil.format(r.getNextFollowDate()), r.getSuggestion(), DateTimeUtil.format(r.getCreatedAt())});
            }
            setRows(model, rows);
        } catch (Exception e) { showError(e); }
    }
}
