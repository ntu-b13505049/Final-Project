package com.gymapp.dao;

import com.gymapp.database.Db;
import com.gymapp.model.Equipment;
import com.gymapp.util.DateTimeUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class EquipmentDAO {
    public List<Equipment> findAll() throws SQLException {
        return Db.query("SELECT * FROM equipment_info ORDER BY equipment_id", null, this::map);
    }

    public int insert(Equipment e) throws SQLException {
        return Db.insertAndReturnKey("INSERT INTO equipment_info (equipment_name, type, status, purchase_date, last_maintenance_date, next_maintenance_date, branch_id, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", ps -> {
            ps.setString(1, e.getEquipmentName());
            ps.setString(2, e.getType());
            ps.setString(3, e.getStatus());
            ps.setDate(4, DateTimeUtil.toSqlDate(e.getPurchaseDate()));
            ps.setDate(5, DateTimeUtil.toSqlDate(e.getLastMaintenanceDate()));
            ps.setDate(6, DateTimeUtil.toSqlDate(e.getNextMaintenanceDate()));
            if (e.getBranchId() == null) ps.setNull(7, java.sql.Types.INTEGER); else ps.setInt(7, e.getBranchId());
            ps.setString(8, e.getNotes());
        });
    }

    public void update(Equipment e) throws SQLException {
        Db.update("UPDATE equipment_info SET equipment_name=?, type=?, status=?, purchase_date=?, last_maintenance_date=?, next_maintenance_date=?, branch_id=?, notes=? WHERE equipment_id=?", ps -> {
            ps.setString(1, e.getEquipmentName());
            ps.setString(2, e.getType());
            ps.setString(3, e.getStatus());
            ps.setDate(4, DateTimeUtil.toSqlDate(e.getPurchaseDate()));
            ps.setDate(5, DateTimeUtil.toSqlDate(e.getLastMaintenanceDate()));
            ps.setDate(6, DateTimeUtil.toSqlDate(e.getNextMaintenanceDate()));
            if (e.getBranchId() == null) ps.setNull(7, java.sql.Types.INTEGER); else ps.setInt(7, e.getBranchId());
            ps.setString(8, e.getNotes());
            ps.setInt(9, e.getEquipmentId());
        });
    }

    public void delete(int id) throws SQLException {
        Db.update("DELETE FROM equipment_info WHERE equipment_id=?", ps -> ps.setInt(1, id));
    }

    private Equipment map(ResultSet rs) throws SQLException {
        return new Equipment(rs.getInt("equipment_id"), rs.getString("equipment_name"), rs.getString("type"), rs.getString("status"),
                DateTimeUtil.fromDbDate(rs, "purchase_date"),
                DateTimeUtil.fromDbDate(rs, "last_maintenance_date"),
                DateTimeUtil.fromDbDate(rs, "next_maintenance_date"),
                rs.getObject("branch_id") == null ? null : rs.getInt("branch_id"),
                rs.getString("notes"));
    }
}
