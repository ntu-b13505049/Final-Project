package com.gymapp.dao;

import com.gymapp.database.Db;
import com.gymapp.model.Reservation;
import com.gymapp.util.DateTimeUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ReservationDAO {
    public List<Reservation> findAll() throws SQLException {
        return Db.query("SELECT * FROM reservation_history ORDER BY created_time DESC", null, this::map);
    }

    public List<Reservation> findByMember(int memberId) throws SQLException {
        return Db.query("SELECT * FROM reservation_history WHERE member_id=? ORDER BY created_time DESC", ps -> ps.setInt(1, memberId), this::map);
    }

    public Optional<Reservation> findById(int reservationId) throws SQLException {
        return Db.queryOne("SELECT * FROM reservation_history WHERE reservation_id=?", ps -> ps.setInt(1, reservationId), this::map);
    }

    public boolean hasActiveReservation(int memberId, int courseId) throws SQLException {
        return Db.queryOne("SELECT COUNT(*) c FROM reservation_history WHERE member_id=? AND course_id=? AND status='已預約'", ps -> {
            ps.setInt(1, memberId);
            ps.setInt(2, courseId);
        }, rs -> rs.getInt("c")).orElse(0) > 0;
    }

    public int insert(Reservation r) throws SQLException {
        return Db.insertAndReturnKey("INSERT INTO reservation_history (member_id, course_id, status, points_deducted, created_time) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)", ps -> {
            ps.setInt(1, r.getMemberId());
            ps.setInt(2, r.getCourseId());
            ps.setString(3, r.getStatus());
            ps.setInt(4, r.getPointsDeducted());
        });
    }

    public void updateStatus(int reservationId, String status) throws SQLException {
        Db.update("UPDATE reservation_history SET status=? WHERE reservation_id=?", ps -> {
            ps.setString(1, status);
            ps.setInt(2, reservationId);
        });
    }

    private Reservation map(ResultSet rs) throws SQLException {
        return new Reservation(rs.getInt("reservation_id"), rs.getInt("member_id"), rs.getInt("course_id"),
                rs.getString("status"), rs.getInt("points_deducted"), DateTimeUtil.fromDbTimestamp(rs, "created_time"));
    }
}
