package com.gymapp.dao;

import com.gymapp.database.Db;
import com.gymapp.model.WaitlistEntry;
import com.gymapp.util.DateTimeUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class WaitlistDAO {
    public void add(int courseId, int memberId) throws SQLException {
        Db.update("INSERT OR IGNORE INTO waitlist (course_id, member_id, status, created_time) VALUES (?, ?, '候補中', CURRENT_TIMESTAMP)", ps -> {
            ps.setInt(1, courseId);
            ps.setInt(2, memberId);
        });
    }

    public Optional<WaitlistEntry> findFirstWaiting(int courseId) throws SQLException {
        return Db.queryOne("SELECT * FROM waitlist WHERE course_id=? AND status='候補中' ORDER BY created_time LIMIT 1", ps -> ps.setInt(1, courseId), this::map);
    }

    public List<WaitlistEntry> findByCourse(int courseId) throws SQLException {
        return Db.query("SELECT * FROM waitlist WHERE course_id=? ORDER BY created_time", ps -> ps.setInt(1, courseId), this::map);
    }

    public void updateStatus(int waitlistId, String status) throws SQLException {
        Db.update("UPDATE waitlist SET status=? WHERE waitlist_id=?", ps -> {
            ps.setString(1, status);
            ps.setInt(2, waitlistId);
        });
    }

    private WaitlistEntry map(ResultSet rs) throws SQLException {
        return new WaitlistEntry(rs.getInt("waitlist_id"), rs.getInt("course_id"), rs.getInt("member_id"),
                rs.getString("status"), DateTimeUtil.fromDbTimestamp(rs, "created_time"));
    }
}
