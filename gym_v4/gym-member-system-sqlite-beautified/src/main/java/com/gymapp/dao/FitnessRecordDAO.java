package com.gymapp.dao;

import com.gymapp.database.Db;
import com.gymapp.model.FitnessRecord;
import com.gymapp.util.DateTimeUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class FitnessRecordDAO {
    public List<FitnessRecord> findAll() throws SQLException {
        return Db.query("SELECT * FROM fitness_records ORDER BY recorded_at DESC", null, this::map);
    }

    public List<FitnessRecord> findByMember(int memberId) throws SQLException {
        return Db.query("SELECT * FROM fitness_records WHERE member_id=? ORDER BY recorded_at DESC", ps -> ps.setInt(1, memberId), this::map);
    }

    public int insert(FitnessRecord r) throws SQLException {
        return Db.insertAndReturnKey("INSERT INTO fitness_records (member_id, trainer_id, weight_kg, body_fat, muscle_mass, training_content, suggestion, recorded_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", ps -> {
            ps.setInt(1, r.getMemberId());
            if (r.getTrainerId() == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, r.getTrainerId());
            ps.setFloat(3, r.getWeightKg());
            ps.setFloat(4, r.getBodyFat());
            ps.setFloat(5, r.getMuscleMass());
            ps.setString(6, r.getTrainingContent());
            ps.setString(7, r.getSuggestion());
        });
    }

    public void update(FitnessRecord r) throws SQLException {
        Db.update("UPDATE fitness_records SET member_id=?, trainer_id=?, weight_kg=?, body_fat=?, muscle_mass=?, training_content=?, suggestion=? WHERE record_id=?", ps -> {
            ps.setInt(1, r.getMemberId());
            if (r.getTrainerId() == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, r.getTrainerId());
            ps.setFloat(3, r.getWeightKg());
            ps.setFloat(4, r.getBodyFat());
            ps.setFloat(5, r.getMuscleMass());
            ps.setString(6, r.getTrainingContent());
            ps.setString(7, r.getSuggestion());
            ps.setInt(8, r.getRecordId());
        });
    }

    public void delete(int id) throws SQLException {
        Db.update("DELETE FROM fitness_records WHERE record_id=?", ps -> ps.setInt(1, id));
    }

    private FitnessRecord map(ResultSet rs) throws SQLException {
        return new FitnessRecord(rs.getInt("record_id"), rs.getInt("member_id"),
                rs.getObject("trainer_id") == null ? null : rs.getInt("trainer_id"),
                rs.getFloat("weight_kg"), rs.getFloat("body_fat"), rs.getFloat("muscle_mass"),
                rs.getString("training_content"), rs.getString("suggestion"), DateTimeUtil.fromDbTimestamp(rs, "recorded_at"));
    }
}
