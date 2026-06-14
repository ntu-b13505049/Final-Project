package com.gymapp.service;

import com.gymapp.dao.CourseDAO;
import com.gymapp.dao.ReservationDAO;
import com.gymapp.dao.WaitlistDAO;
import com.gymapp.database.DBConnection;
import com.gymapp.model.GymClass;
import com.gymapp.model.WaitlistEntry;
import com.gymapp.observer.CourseSeatSubject;
import com.gymapp.observer.WaitlistPromotionObserver;
import com.gymapp.util.AppException;

import java.sql.*;
import java.util.Optional;

public class ReservationService {
    private final CourseDAO courseDAO = new CourseDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final WaitlistDAO waitlistDAO = new WaitlistDAO();
    private final CourseSeatSubject seatSubject = new CourseSeatSubject();

    public ReservationService() {
        seatSubject.addObserver(new WaitlistPromotionObserver(this));
    }

    public String reserve(int memberId, int courseId) throws AppException {
        return reserveInternal(memberId, courseId, false, null);
    }

    public String cancelReservation(int reservationId) throws AppException {
        int courseId;
        int points;
        int memberId;
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM reservation_history WHERE reservation_id=?")) {
                    ps.setInt(1, reservationId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new AppException("找不到預約單號：" + reservationId);
                        }
                        if (!"已預約".equals(rs.getString("status"))) {
                            throw new AppException("只有已預約狀態可以取消");
                        }
                        courseId = rs.getInt("course_id");
                        memberId = rs.getInt("member_id");
                        points = rs.getInt("points_deducted");
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE reservation_history SET status='已取消' WHERE reservation_id=?")) {
                    ps.setInt(1, reservationId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE course_info SET enrolled_count=CASE WHEN enrolled_count > 0 THEN enrolled_count - 1 ELSE 0 END WHERE course_id=?")) {
                    ps.setInt(1, courseId);
                    ps.executeUpdate();
                }
                if (points > 0) {
                    WalletService.changeBalance(con, memberId, points);
                    WalletService.insertTransaction(con, memberId, "預約退點", points, "取消預約單號 " + reservationId);
                }
                con.commit();
            } catch (Exception e) {
                con.rollback();
                if (e instanceof AppException) throw (AppException) e;
                throw e;
            }
        } catch (SQLException e) {
            throw new AppException("取消預約失敗", e);
        }

        try {
            Optional<GymClass> course = courseDAO.findById(courseId);
            course.ifPresent(seatSubject::notifySeatAvailable);
        } catch (SQLException e) {
            // 候補通知失敗不影響取消預約結果，保留於訊息內。
            return "已取消並退點，但候補遞補檢查失敗：" + e.getMessage();
        }
        return "已取消預約並退回點數，系統已檢查候補名單。";
    }

    public void promoteNextWaitlistedMember(int courseId) {
        try {
            Optional<WaitlistEntry> first = waitlistDAO.findFirstWaiting(courseId);
            if (first.isPresent()) {
                WaitlistEntry entry = first.get();
                try {
                    reserveInternal(entry.getMemberId(), courseId, true, entry.getWaitlistId());
                } catch (AppException e) {
                    waitlistDAO.updateStatus(entry.getWaitlistId(), "遞補失敗");
                }
            }
        } catch (SQLException ignored) {
            // GUI 專題不需要背景通知；實務可改為日誌記錄。
        }
    }

    private String reserveInternal(int memberId, int courseId, boolean fromWaitlist, Integer waitlistId) throws AppException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                MemberSnapshot member = lockMember(con, memberId);
                if (!member.active) {
                    throw new AppException("會員狀態不是 Active，不能預約課程");
                }
                if (hasActiveReservation(con, memberId, courseId)) {
                    throw new AppException("此會員已預約該課程");
                }
                CourseSnapshot course = lockCourse(con, courseId);
                if (course.enrolledCount >= course.maxCapacity) {
                    if (!fromWaitlist) {
                        addWaitlist(con, courseId, memberId);
                        con.commit();
                        return "課程已額滿，已加入候補名單。";
                    }
                    throw new AppException("課程仍額滿，暫時無法遞補");
                }
                if (member.walletBalance < course.pointsRequired) {
                    throw new AppException("會員點數不足，目前餘額：" + member.walletBalance + "，需扣點：" + course.pointsRequired);
                }
                int newBalance = member.walletBalance - course.pointsRequired;
                WalletService.updateBalance(con, memberId, newBalance);
                WalletService.insertTransaction(con, memberId, fromWaitlist ? "候補扣點" : "預約扣點", -course.pointsRequired, "預約課程：" + course.courseName);
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO reservation_history (member_id, course_id, status, points_deducted, created_time) VALUES (?, ?, '已預約', ?, CURRENT_TIMESTAMP)")) {
                    ps.setInt(1, memberId);
                    ps.setInt(2, courseId);
                    ps.setInt(3, course.pointsRequired);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE course_info SET enrolled_count=enrolled_count+1 WHERE course_id=?")) {
                    ps.setInt(1, courseId);
                    ps.executeUpdate();
                }
                if (fromWaitlist && waitlistId != null) {
                    try (PreparedStatement ps = con.prepareStatement("UPDATE waitlist SET status='已遞補' WHERE waitlist_id=?")) {
                        ps.setInt(1, waitlistId);
                        ps.executeUpdate();
                    }
                }
                con.commit();
                return fromWaitlist ? "候補已遞補成功並完成扣點。" : "預約成功，已扣除 " + course.pointsRequired + " 點。";
            } catch (Exception e) {
                con.rollback();
                if (e instanceof AppException) throw (AppException) e;
                throw e;
            }
        } catch (SQLException e) {
            throw new AppException("預約失敗", e);
        }
    }

    private MemberSnapshot lockMember(Connection con, int memberId) throws SQLException, AppException {
        try (PreparedStatement ps = con.prepareStatement("SELECT status, wallet_balance FROM member_info WHERE id=?")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new AppException("找不到會員 ID：" + memberId);
                String status = rs.getString("status");
                int wallet = rs.getInt("wallet_balance");
                return new MemberSnapshot("Active".equalsIgnoreCase(status) || "啟用".equals(status), wallet);
            }
        }
    }

    private CourseSnapshot lockCourse(Connection con, int courseId) throws SQLException, AppException {
        try (PreparedStatement ps = con.prepareStatement("SELECT course_name, max_capacity, enrolled_count, points_required FROM course_info WHERE course_id=?")) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new AppException("找不到課程 ID：" + courseId);
                return new CourseSnapshot(rs.getString("course_name"), rs.getInt("max_capacity"), rs.getInt("enrolled_count"), rs.getInt("points_required"));
            }
        }
    }

    private boolean hasActiveReservation(Connection con, int memberId, int courseId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) c FROM reservation_history WHERE member_id=? AND course_id=? AND status='已預約'")) {
            ps.setInt(1, memberId);
            ps.setInt(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("c") > 0;
            }
        }
    }

    private void addWaitlist(Connection con, int courseId, int memberId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT OR IGNORE INTO waitlist (course_id, member_id, status, created_time) VALUES (?, ?, '候補中', CURRENT_TIMESTAMP)")) {
            ps.setInt(1, courseId);
            ps.setInt(2, memberId);
            ps.executeUpdate();
        }
    }

    public ReservationDAO getReservationDAO() {
        return reservationDAO;
    }

    public CourseDAO getCourseDAO() {
        return courseDAO;
    }

    public WaitlistDAO getWaitlistDAO() {
        return waitlistDAO;
    }

    private static class MemberSnapshot {
        final boolean active;
        final int walletBalance;
        MemberSnapshot(boolean active, int walletBalance) {
            this.active = active;
            this.walletBalance = walletBalance;
        }
    }

    private static class CourseSnapshot {
        final String courseName;
        final int maxCapacity;
        final int enrolledCount;
        final int pointsRequired;
        CourseSnapshot(String courseName, int maxCapacity, int enrolledCount, int pointsRequired) {
            this.courseName = courseName;
            this.maxCapacity = maxCapacity;
            this.enrolledCount = enrolledCount;
            this.pointsRequired = pointsRequired;
        }
    }
}
