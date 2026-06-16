package com.gymapp.view.panel;

import com.gymapp.dao.BranchDAO;
import com.gymapp.model.Branch;
import com.gymapp.model.BranchOccupant;
import com.gymapp.service.BranchService;
import com.gymapp.util.DateTimeUtil;
import com.gymapp.util.UiUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BranchPanel extends BasePanel {
    private final boolean editable;
    private final BranchDAO branchDAO = new BranchDAO();
    private final BranchService branchService = new BranchService();
    private final DefaultTableModel model = UiUtil.readOnlyModel(new String[]{"場館ID", "名稱", "最大容留", "目前人數", "剩餘"});
    private final DefaultTableModel occupantModel = UiUtil.readOnlyModel(new String[]{"場館ID", "場館名稱", "會員ID", "會員姓名", "帳號", "會員狀態", "進場時間"});
    private final JTable table = new JTable(model);
    private final JTable occupantTable = new JTable(occupantModel);
    private final JTextField idField = new JTextField(8);
    private final JTextField nameField = new JTextField(14);
    private final JTextField maxField = new JTextField("50", 8);

    public BranchPanel() {
        this(true);
    }

    public BranchPanel(boolean editable) {
        this.editable = editable;
        buildUi();
        refreshData();
    }

    private void buildUi() {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.58);
        split.setTopComponent(tablePanel("場館容留狀態", table, "輸入場館ID、名稱或人數"));
        split.setBottomComponent(tablePanel("目前在場會員（選取場館可只看該場館；未選取則看全部）", occupantTable, "輸入場館、會員ID、姓名、帳號、狀態或進場時間"));
        add(split, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                if (editable) fillFromSelected();
                refreshOccupantsForSelection();
            }
        });

        if (editable) {
            idField.setEditable(false);
            JPanel form = UiUtil.formPanel();
            UiUtil.addField(form, 0, "場館ID", idField);
            UiUtil.addField(form, 1, "名稱", nameField);
            UiUtil.addField(form, 2, "最大容留", maxField);
            JButton add = new JButton("新增");
            JButton update = new JButton("修改");
            JButton delete = new JButton("刪除");
            JButton recalc = new JButton("重新計算人數");
            JButton showAll = new JButton("顯示全部場內會員");
            JButton refresh = new JButton("刷新");
            JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
            buttons.add(add); buttons.add(update); buttons.add(delete); buttons.add(recalc); buttons.add(showAll); buttons.add(refresh);
            JPanel east = new JPanel(new BorderLayout());
            east.setBorder(BorderFactory.createTitledBorder("場館資料 / 場內會員查詢"));
            east.add(form, BorderLayout.CENTER);
            east.add(buttons, BorderLayout.SOUTH);
            add(east, BorderLayout.EAST);
            add.addActionListener(e -> addBranch());
            update.addActionListener(e -> updateBranch());
            delete.addActionListener(e -> deleteBranch());
            UiUtil.installDeleteShortcut(table, this::deleteBranch);
            recalc.addActionListener(e -> recalc());
            showAll.addActionListener(e -> {
                table.clearSelection();
                refreshOccupants(null);
            });
            refresh.addActionListener(e -> refreshData());
        } else {
            JButton refresh = new JButton("刷新場館即時人數與場內會員");
            refresh.addActionListener(e -> refreshData());
            JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
            south.add(new JLabel("目前人數由 access_log 的最後一筆進/出場紀錄動態計算；下方可查看目前在場會員。"));
            south.add(refresh);
            add(south, BorderLayout.SOUTH);
        }
    }

    private void fillFromSelected() {
        int r = table.convertRowIndexToModel(table.getSelectedRow());
        idField.setText(String.valueOf(model.getValueAt(r, 0)));
        nameField.setText(String.valueOf(model.getValueAt(r, 1)));
        maxField.setText(String.valueOf(model.getValueAt(r, 2)));
    }

    private void addBranch() {
        try {
            Branch b = new Branch(0, nameField.getText().trim(), UiUtil.intValue(maxField.getText(), "最大容留"), 0);
            if (b.getBranchName().isBlank()) throw new IllegalArgumentException("場館名稱不可空白");
            branchDAO.insert(b);
            refreshData();
        } catch (Exception e) { showError(e); }
    }

    private void updateBranch() {
        try {
            int id = UiUtil.intValue(idField.getText(), "場館ID");
            int current = branchDAO.calculateCurrentCapacity(id);
            branchDAO.update(new Branch(id, nameField.getText().trim(), UiUtil.intValue(maxField.getText(), "最大容留"), current));
            refreshData();
        } catch (Exception e) { showError(e); }
    }

    private void deleteBranch() {
        try {
            int id = selectedId(table, 0);
            if (UiUtil.confirm(this, "確定刪除場館 ID " + id + "？")) {
                branchDAO.delete(id);
                refreshData();
            }
        } catch (Exception e) { showError(e); }
    }

    private void recalc() {
        try {
            if (table.getSelectedRow() >= 0) {
                branchDAO.recalculateAndUpdate(selectedId(table, 0));
            } else {
                for (Branch b : branchDAO.findAll()) branchDAO.recalculateAndUpdate(b.getBranchId());
            }
            refreshData();
            UiUtil.info(this, "場館人數已重新計算");
        } catch (Exception e) { showError(e); }
    }

    private void refreshOccupantsForSelection() {
        try {
            if (table.getSelectedRow() >= 0) {
                refreshOccupants(selectedId(table, 0));
            } else {
                refreshOccupants(null);
            }
        } catch (Exception e) {
            refreshOccupants(null);
        }
    }

    private void refreshOccupants(Integer branchId) {
        try {
            List<Object[]> rows = new ArrayList<>();
            for (BranchOccupant o : branchDAO.findCurrentOccupants(branchId)) {
                rows.add(new Object[]{o.getBranchId(), o.getBranchName(), o.getMemberId(), o.getMemberName(), o.getAccount(), o.getStatus(), DateTimeUtil.format(o.getEnteredAt())});
            }
            setRows(occupantModel, rows);
        } catch (Exception e) { showError(e); }
    }

    @Override
    public void refreshData() {
        try {
            List<Object[]> rows = new ArrayList<>();
            for (Branch b : branchService.findAllWithDynamicCapacity()) {
                rows.add(new Object[]{b.getBranchId(), b.getBranchName(), b.getMaxCapacity(), b.getCurrentCapacity(), b.getRemainingCapacity()});
            }
            setRows(model, rows);
            refreshOccupantsForSelection();
        } catch (Exception e) { showError(e); }
    }
}
