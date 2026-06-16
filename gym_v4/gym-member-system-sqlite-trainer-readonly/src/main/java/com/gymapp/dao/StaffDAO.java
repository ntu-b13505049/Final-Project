package com.gymapp.dao;

import com.gymapp.database.Db;
import com.gymapp.model.*;
import com.gymapp.util.PasswordUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class StaffDAO {
    public List<Staff> findAll() throws SQLException {
        return Db.query("SELECT * FROM staff ORDER BY id", null, this::map);
    }

    public List<Staff> findTrainers() throws SQLException {
        return Db.query("SELECT * FROM staff WHERE role='教練' ORDER BY id", null, this::map);
    }

    public Optional<Staff> findById(int id) throws SQLException {
        return Db.queryOne("SELECT * FROM staff WHERE id=?", ps -> ps.setInt(1, id), this::map);
    }

    public int insert(Staff staff, String rawPassword) throws SQLException {
        String password = (rawPassword == null || rawPassword.isBlank()) ? PasswordUtil.sha256("trainer123") : PasswordUtil.sha256(rawPassword);
        return Db.insertAndReturnKey("INSERT INTO staff (role, name, account, password, phone, specialty, branch_id) VALUES (?, ?, ?, ?, ?, ?, ?)", ps -> {
            ps.setString(1, staff.getRole().displayName());
            ps.setString(2, staff.getName());
            ps.setString(3, staff.getAccount());
            ps.setString(4, password);
            ps.setString(5, staff.getPhone());
            ps.setString(6, staff.getSpecialty());
            if (staff.getBranchId() == null) ps.setNull(7, java.sql.Types.INTEGER); else ps.setInt(7, staff.getBranchId());
        });
    }

    public void update(Staff staff, String rawPasswordOrBlank) throws SQLException {
        if (rawPasswordOrBlank != null && !rawPasswordOrBlank.isBlank()) {
            Db.update("UPDATE staff SET role=?, name=?, account=?, password=?, phone=?, specialty=?, branch_id=? WHERE id=?", ps -> {
                ps.setString(1, staff.getRole().displayName());
                ps.setString(2, staff.getName());
                ps.setString(3, staff.getAccount());
                ps.setString(4, PasswordUtil.sha256(rawPasswordOrBlank));
                ps.setString(5, staff.getPhone());
                ps.setString(6, staff.getSpecialty());
                if (staff.getBranchId() == null) ps.setNull(7, java.sql.Types.INTEGER); else ps.setInt(7, staff.getBranchId());
                ps.setInt(8, staff.getId());
            });
        } else {
            Db.update("UPDATE staff SET role=?, name=?, account=?, phone=?, specialty=?, branch_id=? WHERE id=?", ps -> {
                ps.setString(1, staff.getRole().displayName());
                ps.setString(2, staff.getName());
                ps.setString(3, staff.getAccount());
                ps.setString(4, staff.getPhone());
                ps.setString(5, staff.getSpecialty());
                if (staff.getBranchId() == null) ps.setNull(6, java.sql.Types.INTEGER); else ps.setInt(6, staff.getBranchId());
                ps.setInt(7, staff.getId());
            });
        }
    }

    public void delete(int id) throws SQLException {
        Db.update("DELETE FROM staff WHERE id=?", ps -> ps.setInt(1, id));
    }

    private Staff map(ResultSet rs) throws SQLException {
        Role role = Role.fromDisplayName(rs.getString("role"));
        Integer branchId = rs.getObject("branch_id") == null ? null : rs.getInt("branch_id");
        if (role == Role.ADMIN) {
            return new Admin(rs.getInt("id"), rs.getString("name"), rs.getString("account"), rs.getString("password"),
                    rs.getString("phone"), rs.getString("specialty"), branchId);
        }
        return new Trainer(rs.getInt("id"), rs.getString("name"), rs.getString("account"), rs.getString("password"),
                rs.getString("phone"), rs.getString("specialty"), branchId);
    }
}
