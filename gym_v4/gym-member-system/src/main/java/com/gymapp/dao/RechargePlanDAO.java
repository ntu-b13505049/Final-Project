package com.gymapp.dao;

import com.gymapp.database.Db;
import com.gymapp.model.RechargePlan;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class RechargePlanDAO {
    public List<RechargePlan> findAll() throws SQLException {
        return Db.query("SELECT * FROM recharge_plans ORDER BY plan_id", null, this::map);
    }

    public Optional<RechargePlan> findById(int id) throws SQLException {
        return Db.queryOne("SELECT * FROM recharge_plans WHERE plan_id=?", ps -> ps.setInt(1, id), this::map);
    }

    private RechargePlan map(ResultSet rs) throws SQLException {
        return new RechargePlan(rs.getInt("plan_id"), rs.getString("plan_name"), rs.getInt("pay_amount"), rs.getInt("points"), rs.getString("description"));
    }
}
