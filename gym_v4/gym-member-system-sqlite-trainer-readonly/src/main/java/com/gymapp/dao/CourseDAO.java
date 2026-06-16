package com.gymapp.dao;

import com.gymapp.database.Db;
import com.gymapp.model.GymClass;
import com.gymapp.model.GymClassFactory;
import com.gymapp.util.DateTimeUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CourseDAO {
    public List<GymClass> findAll() throws SQLException {
        return Db.query("SELECT * FROM course_info ORDER BY schedule_time", null, this::map);
    }

    public List<GymClass> findByTrainer(int trainerId) throws SQLException {
        return Db.query("SELECT * FROM course_info WHERE trainer_id=? ORDER BY schedule_time", ps -> ps.setInt(1, trainerId), this::map);
    }

    public Optional<GymClass> findById(int courseId) throws SQLException {
        return Db.queryOne("SELECT * FROM course_info WHERE course_id=?", ps -> ps.setInt(1, courseId), this::map);
    }

    public int insert(GymClass course) throws SQLException {
        return Db.insertAndReturnKey("INSERT INTO course_info (course_name, course_type, trainer_id, branch_id, schedule_time, max_capacity, enrolled_count, points_required) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", ps -> {
            ps.setString(1, course.getCourseName());
            ps.setString(2, course.getCourseType());
            if (course.getTrainerId() == null) ps.setNull(3, java.sql.Types.INTEGER); else ps.setInt(3, course.getTrainerId());
            if (course.getBranchId() == null) ps.setNull(4, java.sql.Types.INTEGER); else ps.setInt(4, course.getBranchId());
            ps.setTimestamp(5, DateTimeUtil.toTimestamp(course.getScheduleTime()));
            ps.setInt(6, course.getMaxCapacity());
            ps.setInt(7, course.getEnrolledCount());
            ps.setInt(8, course.getPointsRequired());
        });
    }

    public void update(GymClass course) throws SQLException {
        Db.update("UPDATE course_info SET course_name=?, course_type=?, trainer_id=?, branch_id=?, schedule_time=?, max_capacity=?, enrolled_count=?, points_required=? WHERE course_id=?", ps -> {
            ps.setString(1, course.getCourseName());
            ps.setString(2, course.getCourseType());
            if (course.getTrainerId() == null) ps.setNull(3, java.sql.Types.INTEGER); else ps.setInt(3, course.getTrainerId());
            if (course.getBranchId() == null) ps.setNull(4, java.sql.Types.INTEGER); else ps.setInt(4, course.getBranchId());
            ps.setTimestamp(5, DateTimeUtil.toTimestamp(course.getScheduleTime()));
            ps.setInt(6, course.getMaxCapacity());
            ps.setInt(7, course.getEnrolledCount());
            ps.setInt(8, course.getPointsRequired());
            ps.setInt(9, course.getCourseId());
        });
    }

    public void delete(int courseId) throws SQLException {
        Db.update("DELETE FROM course_info WHERE course_id=?", ps -> ps.setInt(1, courseId));
    }

    public void refreshEnrolledCount(int courseId) throws SQLException {
        Db.update("UPDATE course_info SET enrolled_count=(SELECT COUNT(*) FROM reservation_history WHERE course_id=? AND status='已預約') WHERE course_id=?", ps -> {
            ps.setInt(1, courseId);
            ps.setInt(2, courseId);
        });
    }

    private GymClass map(ResultSet rs) throws SQLException {
        return GymClassFactory.create(
                rs.getInt("course_id"),
                rs.getString("course_name"),
                rs.getString("course_type"),
                rs.getObject("trainer_id") == null ? null : rs.getInt("trainer_id"),
                rs.getObject("branch_id") == null ? null : rs.getInt("branch_id"),
                DateTimeUtil.fromDbTimestamp(rs, "schedule_time"),
                rs.getInt("max_capacity"),
                rs.getInt("enrolled_count"),
                rs.getInt("points_required"));
    }
}
