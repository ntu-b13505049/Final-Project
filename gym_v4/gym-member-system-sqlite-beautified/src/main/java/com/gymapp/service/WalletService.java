package com.gymapp.service;

import com.gymapp.dao.RechargePlanDAO;
import com.gymapp.dao.TransactionDAO;
import com.gymapp.database.DBConnection;
import com.gymapp.model.RechargePlan;
import com.gymapp.model.TransactionRecord;
import com.gymapp.util.AppException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class WalletService {
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final RechargePlanDAO rechargePlanDAO = new RechargePlanDAO();

    public List<TransactionRecord> getTransactions(Integer memberId) throws SQLException {
        return memberId == null ? transactionDAO.findAll() : transactionDAO.findByMember(memberId);
    }

    public List<RechargePlan> getPlans() throws SQLException {
        return rechargePlanDAO.findAll();
    }

    public int deposit(int memberId, int points, String type, String note) throws AppException {
        if (points <= 0) {
            throw new AppException("儲值點數必須大於 0");
        }
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int newBalance = changeBalance(con, memberId, points);
                insertTransaction(con, memberId, type == null ? "儲值" : type, points, note);
                con.commit();
                return newBalance;
            } catch (Exception e) {
                con.rollback();
                if (e instanceof AppException) throw (AppException) e;
                throw e;
            }
        } catch (SQLException e) {
            throw new AppException("儲值失敗", e);
        }
    }

    public int depositByPlan(int memberId, int planId) throws AppException {
        try {
            RechargePlan plan = rechargePlanDAO.findById(planId).orElseThrow(() -> new AppException("找不到儲值方案"));
            return deposit(memberId, plan.getPoints(), "儲值", plan.getPlanName() + "：付款 " + plan.getPayAmount() + " 元");
        } catch (SQLException e) {
            throw new AppException("讀取儲值方案失敗", e);
        }
    }

    public int deduct(int memberId, int points, String type, String note) throws AppException {
        if (points <= 0) {
            throw new AppException("扣點點數必須大於 0");
        }
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int current = lockBalance(con, memberId);
                if (current < points) {
                    throw new AppException("會員點數不足，目前餘額：" + current);
                }
                int newBalance = current - points;
                updateBalance(con, memberId, newBalance);
                insertTransaction(con, memberId, type == null ? "扣點" : type, -points, note);
                con.commit();
                return newBalance;
            } catch (Exception e) {
                con.rollback();
                if (e instanceof AppException) throw (AppException) e;
                throw e;
            }
        } catch (SQLException e) {
            throw new AppException("扣點失敗", e);
        }
    }

    static int lockBalance(Connection con, int memberId) throws SQLException, AppException {
        try (PreparedStatement ps = con.prepareStatement("SELECT wallet_balance FROM member_info WHERE id=?")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new AppException("找不到會員 ID：" + memberId);
                }
                return rs.getInt("wallet_balance");
            }
        }
    }

    static int changeBalance(Connection con, int memberId, int delta) throws SQLException, AppException {
        int current = lockBalance(con, memberId);
        int newBalance = current + delta;
        if (newBalance < 0) {
            throw new AppException("會員點數不足，目前餘額：" + current);
        }
        updateBalance(con, memberId, newBalance);
        return newBalance;
    }

    static void updateBalance(Connection con, int memberId, int newBalance) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE member_info SET wallet_balance=? WHERE id=?")) {
            ps.setInt(1, newBalance);
            ps.setInt(2, memberId);
            ps.executeUpdate();
        }
    }

    static void insertTransaction(Connection con, int memberId, String type, int amount, String note) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO transaction_history (member_id, type, amount, timestamp, note) VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)")) {
            ps.setInt(1, memberId);
            ps.setString(2, type);
            ps.setInt(3, amount);
            ps.setString(4, note);
            ps.executeUpdate();
        }
    }
}
