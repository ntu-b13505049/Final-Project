package com.gymapp.dao;

import com.gymapp.database.Db;
import com.gymapp.model.FollowUpRecord;
import com.gymapp.util.DateTimeUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class FollowUpDAO {
    public List<FollowUpRecord> findAll() throws SQLException {
        return Db.query("SELECT * FROM follow_up_records ORDER BY next_follow_date, created_at DESC", null, this::map);
    }

    public List<FollowUpRecord> findByMember(int memberId) throws SQLException {
        return Db.query("SELECT * FROM follow_up_records WHERE member_id=? ORDER BY next_follow_date, created_at DESC", ps -> ps.setInt(1, memberId), this::map);
    }

    public int insert(FollowUpRecord r) throws SQLException {
        return Db.insertAndReturnKey("INSERT INTO follow_up_records (member_id, trainer_id, goal, current_status, next_follow_date, suggestion, created_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", ps -> {
            ps.setInt(1, r.getMemberId());
            if (r.getTrainerId() == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, r.getTrainerId());
            ps.setString(3, r.getGoal());
            ps.setString(4, r.getCurrentStatus());
            ps.setDate(5, DateTimeUtil.toSqlDate(r.getNextFollowDate()));
            ps.setString(6, r.getSuggestion());
        });
    }

    public void update(FollowUpRecord r) throws SQLException {
        Db.update("UPDATE follow_up_records SET member_id=?, trainer_id=?, goal=?, current_status=?, next_follow_date=?, suggestion=? WHERE follow_id=?", ps -> {
            ps.setInt(1, r.getMemberId());
            if (r.getTrainerId() == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, r.getTrainerId());
            ps.setString(3, r.getGoal());
            ps.setString(4, r.getCurrentStatus());
            ps.setDate(5, DateTimeUtil.toSqlDate(r.getNextFollowDate()));
            ps.setString(6, r.getSuggestion());
            ps.setInt(7, r.getFollowId());
        });
    }

    public void delete(int id) throws SQLException {
        Db.update("DELETE FROM follow_up_records WHERE follow_id=?", ps -> ps.setInt(1, id));
    }

    private FollowUpRecord map(ResultSet rs) throws SQLException {
        return new FollowUpRecord(rs.getInt("follow_id"), rs.getInt("member_id"),
                rs.getObject("trainer_id") == null ? null : rs.getInt("trainer_id"),
                rs.getString("goal"), rs.getString("current_status"), DateTimeUtil.fromDbDate(rs, "next_follow_date"),
                rs.getString("suggestion"), DateTimeUtil.fromDbTimestamp(rs, "created_at"));
    }
}
