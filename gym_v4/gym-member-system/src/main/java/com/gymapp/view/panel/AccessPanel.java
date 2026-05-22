package com.gymapp.view.panel;

import com.gymapp.model.AccessLog;
import com.gymapp.model.Branch;
import com.gymapp.model.Role;
import com.gymapp.model.User;
import com.gymapp.service.AccessService;
import com.gymapp.service.BranchService;
import com.gymapp.util.DateTimeUtil;
import com.gymapp.util.UiUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AccessPanel extends BasePanel {
    private final User user;
    private final AccessService accessService = new AccessService();
    private final BranchService branchService = new BranchService();
    private final DefaultTableModel branchModel = UiUtil.readOnlyModel(new String[]{"場館ID", "名稱", "目前人數", "上限", "剩餘"});
    private final DefaultTableModel logModel = UiUtil.readOnlyModel(new String[]{"紀錄ID", "會員ID", "場館ID", "動作", "刷卡時間"});
    private final JTable branchTable = new JTable(branchModel);
    private final JTable logTable = new JTable(logModel);
    private final JTextField memberIdField = new JTextField(8);

    public AccessPanel(User user) {
        this.user = user;
        buildUi();
        refreshData();
    }

    private void buildUi() {
        if (user.hasRole(Role.MEMBER)) {
            memberIdField.setText(String.valueOf(user.getId()));
            memberIdField.setEditable(false);
        }
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.45);
        JPanel branches = new JPanel(new BorderLayout());
        branches.setBorder(BorderFactory.createTitledBorder("場館容留狀態"));
        branches.add(scroll(branchTable), BorderLayout.CENTER);
        JPanel logs = new JPanel(new BorderLayout());
        logs.setBorder(BorderFactory.createTitledBorder(user.hasRole(Role.MEMBER) ? "我的進出場紀錄" : "進出場紀錄"));
        logs.add(scroll(logTable), BorderLayout.CENTER);
        split.setTopComponent(branches);
        split.setBottomComponent(logs);
        add(split, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setBorder(BorderFactory.createTitledBorder("Check I/O 動態進出場驗證"));
        controls.add(new JLabel("會員ID"));
        controls.add(memberIdField);
        JButton in = new JButton("選取場館進場 / 產生 QR 驗證");
        JButton out = new JButton("出場");
        JButton refresh = new JButton("刷新");
        controls.add(in); controls.add(out); controls.add(refresh);
        add(controls, BorderLayout.NORTH);
        in.addActionListener(e -> checkIn());
        out.addActionListener(e -> checkOut());
        refresh.addActionListener(e -> refreshData());
    }

    private int memberId() {
        return UiUtil.intValue(memberIdField.getText(), "會員ID");
    }

    private void checkIn() {
        try {
            int branchId = selectedId(branchTable, 0);
            String msg = accessService.checkIn(memberId(), branchId);
            UiUtil.info(this, msg);
            refreshData();
        } catch (Exception e) { showError(e); }
    }

    private void checkOut() {
        try {
            String msg = accessService.checkOut(memberId());
            UiUtil.info(this, msg);
            refreshData();
        } catch (Exception e) { showError(e); }
    }

    @Override
    public void refreshData() {
        try {
            List<Object[]> branchRows = new ArrayList<>();
            for (Branch b : branchService.findAllWithDynamicCapacity()) {
                branchRows.add(new Object[]{b.getBranchId(), b.getBranchName(), b.getCurrentCapacity(), b.getMaxCapacity(), b.getRemainingCapacity()});
            }
            setRows(branchModel, branchRows);
            Integer memberFilter = user.hasRole(Role.MEMBER) ? user.getId() : null;
            if (!user.hasRole(Role.MEMBER) && memberIdField.getText() != null && !memberIdField.getText().isBlank()) {
                memberFilter = UiUtil.intValue(memberIdField.getText(), "會員ID");
            }
            List<Object[]> logRows = new ArrayList<>();
            for (AccessLog l : accessService.getLogs(memberFilter)) {
                logRows.add(new Object[]{l.getLogId(), l.getMemberId(), l.getBranchId(), l.getAction(), DateTimeUtil.format(l.getTimestamp())});
            }
            setRows(logModel, logRows);
        } catch (Exception e) { showError(e); }
    }
}
