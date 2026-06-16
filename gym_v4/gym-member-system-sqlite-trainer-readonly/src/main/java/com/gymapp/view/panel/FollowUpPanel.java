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
    private final JTextArea goalArea = largeArea(3, 22);
    private final JTextArea currentArea = largeArea(3, 22);
    private final JTextField nextDateField = new JTextField("2026-04-01", 12);
    private final JTextArea suggestionArea = largeArea(6, 22);

    public FollowUpPanel(User user) {
        this.user = user;
        buildUi();
        refreshData();
    }

    private static JTextArea largeArea(int rows, int columns) {
        JTextArea area = new JTextArea(rows, columns);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(area.getFont().deriveFont(UiUtil.BASE_FONT_SIZE));
        return area;
    }

    private static JScrollPane areaScroll(JTextArea area) {
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(300, Math.max(80, area.getRows() * 34)));
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        UiUtil.makeScrollPaneScrollAnywhere(scroll);
        return scroll;
    }

    private void buildUi() {
        boolean member = user.hasRole(Role.MEMBER);
        if (member) {
            memberIdField.setText(String.valueOf(user.getId()));
            memberIdField.setEditable(false);
        }
        if (user.hasRole(Role.TRAINER)) {
            trainerIdField.setText(String.valueOf(user.getId()));
            trainerIdField.setEditable(false);
        }
        idField.setEditable(false);
        add(tablePanel(table, "輸入會員ID、教練ID、目標、狀況、日期或建議"), BorderLayout.CENTER);

        JPanel form = UiUtil.formPanel();
        UiUtil.addField(form, 0, "追蹤ID", idField);
        UiUtil.addField(form, 1, "會員ID", memberIdField);
        UiUtil.addField(form, 2, "教練ID", trainerIdField);
        UiUtil.addField(form, 3, "目標", areaScroll(goalArea));
        UiUtil.addField(form, 4, "目前狀況", areaScroll(currentArea));
        UiUtil.addField(form, 5, "下次追蹤 yyyy-MM-dd", nextDateField);
        UiUtil.addField(form, 6, "飲食/訓練建議", areaScroll(suggestionArea));

        JButton add = new JButton("新增追蹤");
        JButton update = new JButton("修改追蹤");
        JButton delete = new JButton("刪除追蹤");
        JButton detail = new JButton("放大查看選取建議");
        JButton refresh = new JButton("刷新");
        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        if (!member) {
            buttons.add(add); buttons.add(update); buttons.add(delete);
        } else {
            setMemberReadOnlyMode();
        }
        buttons.add(detail);
        buttons.add(refresh);

        JPanel east = new JPanel(new BorderLayout(6, 6));
        east.setBorder(BorderFactory.createTitledBorder(member ? "我的追蹤建議詳細內容" : "後續追蹤管理"));
        JLabel hint = new JLabel(member
                ? "<html>點選左方紀錄即可在此查看完整目標、狀況與教練建議；也可雙擊表格列開啟大視窗。</html>"
                : "<html>可新增/修改會員追蹤建議；長文字欄位已改成較大的輸入區。</html>");
        hint.setBorder(BorderFactory.createEmptyBorder(6, 8, 0, 8));
        east.add(hint, BorderLayout.NORTH);
        JScrollPane formScroll = new JScrollPane(form);
        formScroll.getVerticalScrollBar().setUnitIncrement(18);
        UiUtil.makeScrollPaneScrollAnywhere(formScroll);
        east.add(formScroll, BorderLayout.CENTER);
        east.add(buttons, BorderLayout.SOUTH);
        add(east, BorderLayout.EAST);

        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) fillFromSelected(); });
        add.addActionListener(e -> addRecord());
        update.addActionListener(e -> updateRecord());
        delete.addActionListener(e -> deleteRecord());
        if (!member) {
            UiUtil.installDeleteShortcut(table, this::deleteRecord);
        }
        detail.addActionListener(e -> UiUtil.showTableRowDetail(this, table, "追蹤建議完整內容"));
        refresh.addActionListener(e -> refreshData());
    }

    private void setMemberReadOnlyMode() {
        idField.setEditable(false);
        memberIdField.setEditable(false);
        trainerIdField.setEditable(false);
        goalArea.setEditable(false);
        currentArea.setEditable(false);
        nextDateField.setEditable(false);
        suggestionArea.setEditable(false);
        Color bg = UIManager.getColor("TextField.inactiveBackground");
        if (bg != null) {
            goalArea.setBackground(bg);
            currentArea.setBackground(bg);
            suggestionArea.setBackground(bg);
        }
    }

    private FollowUpRecord readForm(boolean needId) {
        FollowUpRecord r = new FollowUpRecord();
        if (needId) r.setFollowId(UiUtil.intValue(idField.getText(), "追蹤ID"));
        r.setMemberId(UiUtil.intValue(memberIdField.getText(), "會員ID"));
        r.setTrainerId(UiUtil.nullableInt(trainerIdField.getText()));
        r.setGoal(goalArea.getText().trim());
        r.setCurrentStatus(currentArea.getText().trim());
        r.setNextFollowDate(DateTimeUtil.parseDate(nextDateField.getText()));
        r.setSuggestion(suggestionArea.getText().trim());
        if (r.getGoal().isBlank() && r.getSuggestion().isBlank()) {
            throw new IllegalArgumentException("目標或建議至少需要填寫一項");
        }
        return r;
    }

    private void fillFromSelected() {
        int r = table.convertRowIndexToModel(table.getSelectedRow());
        idField.setText(String.valueOf(model.getValueAt(r, 0)));
        memberIdField.setText(String.valueOf(model.getValueAt(r, 1)));
        trainerIdField.setText(String.valueOf(model.getValueAt(r, 2)));
        goalArea.setText(String.valueOf(model.getValueAt(r, 3)));
        goalArea.setCaretPosition(0);
        currentArea.setText(String.valueOf(model.getValueAt(r, 4)));
        currentArea.setCaretPosition(0);
        nextDateField.setText(String.valueOf(model.getValueAt(r, 5)));
        suggestionArea.setText(String.valueOf(model.getValueAt(r, 6)));
        suggestionArea.setCaretPosition(0);
    }

    private void clearDetail() {
        idField.setText("");
        if (!user.hasRole(Role.MEMBER)) memberIdField.setText("");
        if (!user.hasRole(Role.TRAINER)) trainerIdField.setText("");
        goalArea.setText("");
        currentArea.setText("");
        nextDateField.setText("");
        suggestionArea.setText("");
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
            if (table.getRowCount() > 0) {
                table.setRowSelectionInterval(0, 0);
                fillFromSelected();
            } else {
                clearDetail();
            }
        } catch (Exception e) { showError(e); }
    }
}
