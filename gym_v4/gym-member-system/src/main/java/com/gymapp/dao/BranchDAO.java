package com.gymapp.dao;

import com.gymapp.database.Db;
import com.gymapp.model.Branch;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class BranchDAO {
    public List<Branch> findAll() throws SQLException {
        return Db.query("SELECT * FROM branch_info ORDER BY branch_id", null, this::map);
    }

    public Optional<Branch> findById(int id) throws SQLException {
        return Db.queryOne("SELECT * FROM branch_info WHERE branch_id=?", ps -> ps.setInt(1, id), this::map);
    }

    public int insert(Branch branch) throws SQLException {
        return Db.insertAndReturnKey("INSERT INTO branch_info (branch_name, max_capacity, current_capacity) VALUES (?, ?, ?)", ps -> {
            ps.setString(1, branch.getBranchName());
            ps.setInt(2, branch.getMaxCapacity());
            ps.setInt(3, branch.getCurrentCapacity());
        });
    }

    public void update(Branch branch) throws SQLException {
        Db.update("UPDATE branch_info SET branch_name=?, max_capacity=?, current_capacity=? WHERE branch_id=?", ps -> {
            ps.setString(1, branch.getBranchName());
            ps.setInt(2, branch.getMaxCapacity());
            ps.setInt(3, branch.getCurrentCapacity());
            ps.setInt(4, branch.getBranchId());
        });
    }

    public void delete(int id) throws SQLException {
        Db.update("DELETE FROM branch_info WHERE branch_id=?", ps -> ps.setInt(1, id));
    }

    public int calculateCurrentCapacity(int branchId) throws SQLException {
        String sql = "SELECT COUNT(*) AS c " +
                "FROM access_log l " +
                "JOIN (SELECT member_id, MAX(log_id) AS max_id FROM access_log GROUP BY member_id) latest " +
                "ON latest.max_id = l.log_id " +
                "WHERE l.branch_id = ? AND l.action = '進場'";
        return Db.queryOne(sql, ps -> ps.setInt(1, branchId), rs -> rs.getInt("c")).orElse(0);
    }

    public void recalculateAndUpdate(int branchId) throws SQLException {
        int current = calculateCurrentCapacity(branchId);
        Db.update("UPDATE branch_info SET current_capacity=? WHERE branch_id=?", ps -> {
            ps.setInt(1, current);
            ps.setInt(2, branchId);
        });
    }

    private Branch map(ResultSet rs) throws SQLException {
        return new Branch(rs.getInt("branch_id"), rs.getString("branch_name"), rs.getInt("max_capacity"), rs.getInt("current_capacity"));
    }
}
