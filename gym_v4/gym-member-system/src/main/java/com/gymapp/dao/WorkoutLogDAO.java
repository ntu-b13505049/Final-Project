package com.gymapp.dao;

import com.gymapp.database.Db;
import com.gymapp.model.WorkoutLog;
import com.gymapp.util.DateTimeUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class WorkoutLogDAO {
    public List<WorkoutLog> findAll() throws SQLException {
        return Db.query("SELECT * FROM workout_logs ORDER BY workout_time DESC", null, this::map);
    }

    public List<WorkoutLog> findByMember(int memberId) throws SQLException {
        return Db.query("SELECT * FROM workout_logs WHERE member_id=? ORDER BY workout_time DESC", ps -> ps.setInt(1, memberId), this::map);
    }

    public int insert(WorkoutLog log) throws SQLException {
        return Db.insertAndReturnKey("INSERT INTO workout_logs (member_id, exercise_name, weight, reps, workout_time) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)", ps -> {
            ps.setInt(1, log.getMemberId());
            ps.setString(2, log.getExerciseName());
            ps.setFloat(3, log.getWeight());
            ps.setInt(4, log.getReps());
        });
    }

    private WorkoutLog map(ResultSet rs) throws SQLException {
        return new WorkoutLog(rs.getInt("log_id"), rs.getInt("member_id"), rs.getString("exercise_name"),
                rs.getFloat("weight"), rs.getInt("reps"), DateTimeUtil.fromDbTimestamp(rs, "workout_time"));
    }
}
