package com.gymapp.dao;

import com.gymapp.database.Db;
import com.gymapp.model.AccessLog;
import com.gymapp.util.DateTimeUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AccessLogDAO {
    public List<AccessLog> findAll() throws SQLException {
        return Db.query("SELECT * FROM access_log ORDER BY timestamp DESC LIMIT 300", null, this::map);
    }

    public List<AccessLog> findByMember(int memberId) throws SQLException {
        return Db.query("SELECT * FROM access_log WHERE member_id=? ORDER BY timestamp DESC LIMIT 100", ps -> ps.setInt(1, memberId), this::map);
    }

    public Optional<AccessLog> findLatestByMember(int memberId) throws SQLException {
        return Db.queryOne("SELECT * FROM access_log WHERE member_id=? ORDER BY timestamp DESC, log_id DESC LIMIT 1", ps -> ps.setInt(1, memberId), this::map);
    }

    public int insert(AccessLog log) throws SQLException {
        return Db.insertAndReturnKey("INSERT INTO access_log (member_id, branch_id, action, timestamp) VALUES (?, ?, ?, CURRENT_TIMESTAMP)", ps -> {
            ps.setInt(1, log.getMemberId());
            ps.setInt(2, log.getBranchId());
            ps.setString(3, log.getAction());
        });
    }

    private AccessLog map(ResultSet rs) throws SQLException {
        return new AccessLog(rs.getInt("log_id"), rs.getInt("member_id"), rs.getInt("branch_id"), rs.getString("action"),
                DateTimeUtil.fromDbTimestamp(rs, "timestamp"));
    }
}
