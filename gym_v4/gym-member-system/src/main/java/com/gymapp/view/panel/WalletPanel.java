package com.gymapp.view.panel;

import com.gymapp.dao.MemberDAO;
import com.gymapp.model.Member;
import com.gymapp.model.RechargePlan;
import com.gymapp.model.Role;
import com.gymapp.model.TransactionRecord;
import com.gymapp.model.User;
import com.gymapp.service.WalletService;
import com.gymapp.util.DateTimeUtil;
import com.gymapp.util.UiUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WalletPanel extends BasePanel {
    private final User user;
    private final WalletService walletService = new WalletService();
    private final MemberDAO memberDAO = new MemberDAO();
    private final DefaultTableModel model = UiUtil.readOnlyModel(new String[]{"交易ID", "會員ID", "類別", "變動點數", "時間", "備註"});
    private final JTable table = new JTable(model);
    private final JTextField memberIdField = new JTextField(8);
    private final JLabel balanceLabel = new JLabel("餘額：--");
    private final JComboBox<RechargePlan> planBox = new JComboBox<>();
    private final JTextField customPointsField = new JTextField("500", 8);

    public WalletPanel(User user) {
        this.user = user;
        buildUi();
        refreshData();
    }

    private void buildUi() {
        if (user.hasRole(Role.MEMBER)) {
            memberIdField.setText(String.valueOf(user.getId()));
            memberIdField.setEditable(false);
        }
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBorder(BorderFactory.createTitledBorder("會員儲值方案 / 電子錢包"));
        top.add(new JLabel("會員ID"));
        top.add(memberIdField);
        top.add(balanceLabel);
        top.add(new JLabel("方案"));
        top.add(planBox);
        JButton depositPlan = new JButton("依方案儲值");
        top.add(depositPlan);
        top.add(new JLabel("自訂點數"));
        top.add(customPointsField);
        JButton depositCustom = new JButton("自訂儲值");
        JButton refresh = new JButton("刷新");
        top.add(depositCustom);
        top.add(refresh);
        add(top, BorderLayout.NORTH);
        add(scroll(table), BorderLayout.CENTER);
        depositPlan.addActionListener(e -> depositByPlan());
        depositCustom.addActionListener(e -> depositCustom());
        refresh.addActionListener(e -> refreshData());
    }

    private Integer currentMemberIdOrNullForList() {
        if (user.hasRole(Role.MEMBER)) return user.getId();
        if (memberIdField.getText() == null || memberIdField.getText().isBlank()) return null;
        return UiUtil.intValue(memberIdField.getText(), "會員ID");
    }

    private int requiredMemberId() {
        return UiUtil.intValue(memberIdField.getText(), "會員ID");
    }

    private void depositByPlan() {
        try {
            RechargePlan plan = (RechargePlan) planBox.getSelectedItem();
            if (plan == null) throw new IllegalArgumentException("請選擇儲值方案");
            int newBalance = walletService.depositByPlan(requiredMemberId(), plan.getPlanId());
            UiUtil.info(this, "儲值完成，新餘額：" + newBalance);
            refreshData();
        } catch (Exception e) { showError(e); }
    }

    private void depositCustom() {
        try {
            int points = UiUtil.intValue(customPointsField.getText(), "自訂點數");
            int newBalance = walletService.deposit(requiredMemberId(), points, "儲值", "自訂儲值");
            UiUtil.info(this, "儲值完成，新餘額：" + newBalance);
            refreshData();
        } catch (Exception e) { showError(e); }
    }

    @Override
    public void refreshData() {
        try {
            planBox.removeAllItems();
            for (RechargePlan p : walletService.getPlans()) planBox.addItem(p);
            Integer memberId = currentMemberIdOrNullForList();
            if (memberId != null) {
                Member m = memberDAO.findById(memberId).orElse(null);
                balanceLabel.setText(m == null ? "餘額：找不到會員" : "餘額：" + m.getWallet().getBalance());
            } else {
                balanceLabel.setText("餘額：請輸入會員ID查看");
            }
            List<Object[]> rows = new ArrayList<>();
            for (TransactionRecord t : walletService.getTransactions(memberId)) {
                rows.add(new Object[]{t.getTransactionId(), t.getMemberId(), t.getType(), t.getAmount(), DateTimeUtil.format(t.getTimestamp()), t.getNote()});
            }
            setRows(model, rows);
        } catch (Exception e) { showError(e); }
    }
}
