package com.gymapp.dao;

import com.gymapp.database.Db;
import com.gymapp.model.SaleRecord;
import com.gymapp.util.DateTimeUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SaleDAO {
    public List<SaleRecord> findAll() throws SQLException {
        return Db.query("SELECT * FROM sales_history ORDER BY timestamp DESC LIMIT 300", null, this::map);
    }

    public int insert(SaleRecord s) throws SQLException {
        return Db.insertAndReturnKey("INSERT INTO sales_history (member_id, product_id, quantity, total_amount, sold_by, timestamp) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", ps -> {
            if (s.getMemberId() == null) ps.setNull(1, java.sql.Types.INTEGER); else ps.setInt(1, s.getMemberId());
            ps.setInt(2, s.getProductId());
            ps.setInt(3, s.getQuantity());
            ps.setInt(4, s.getTotalAmount());
            if (s.getSoldBy() == null) ps.setNull(5, java.sql.Types.INTEGER); else ps.setInt(5, s.getSoldBy());
        });
    }

    private SaleRecord map(ResultSet rs) throws SQLException {
        return new SaleRecord(rs.getInt("sale_id"),
                rs.getObject("member_id") == null ? null : rs.getInt("member_id"),
                rs.getInt("product_id"), rs.getInt("quantity"), rs.getInt("total_amount"),
                rs.getObject("sold_by") == null ? null : rs.getInt("sold_by"),
                DateTimeUtil.fromDbTimestamp(rs, "timestamp"));
    }
}
