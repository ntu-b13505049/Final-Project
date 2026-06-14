package com.gymapp.service;

import com.gymapp.dao.CourseDAO;
import com.gymapp.model.GymClass;

import java.sql.SQLException;
import java.util.List;

public class CourseService {
    private final CourseDAO courseDAO = new CourseDAO();

    public List<GymClass> findCourses(Integer trainerId) throws SQLException {
        return trainerId == null ? courseDAO.findAll() : courseDAO.findByTrainer(trainerId);
    }

    public CourseDAO getCourseDAO() {
        return courseDAO;
    }
}
