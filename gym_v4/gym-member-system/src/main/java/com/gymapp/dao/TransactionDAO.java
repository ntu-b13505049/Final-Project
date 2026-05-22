package com.gymapp.dao;

import com.gymapp.database.Db;
import com.gymapp.model.TransactionRecord;
import com.gymapp.util.DateTimeUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TransactionDAO {
    public List<TransactionRecord> findAll() throws SQLException {
        return Db.query("SELECT * FROM transaction_history ORDER BY timestamp DESC", null, this::map);
    }

    public List<TransactionRecord> findByMember(int memberId) throws SQLException {
        return Db.query("SELECT * FROM transaction_history WHERE member_id=? ORDER BY timestamp DESC", ps -> ps.setInt(1, memberId), this::map);
    }

    public int insert(TransactionRecord record) throws SQLException {
        return Db.insertAndReturnKey("INSERT INTO transaction_history (member_id, type, amount, timestamp, note) VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)", ps -> {
            ps.setInt(1, record.getMemberId());
            ps.setString(2, record.getType());
            ps.setInt(3, record.getAmount());
            ps.setString(4, record.getNote());
        });
    }

    private TransactionRecord map(ResultSet rs) throws SQLException {
        return new TransactionRecord(rs.getInt("transaction_id"), rs.getInt("member_id"), rs.getString("type"), rs.getInt("amount"),
                DateTimeUtil.fromDbTimestamp(rs, "timestamp"), rs.getString("note"));
    }
}
