package com.gymapp.service;

import com.gymapp.dao.FitnessRecordDAO;
import com.gymapp.dao.WorkoutLogDAO;
import com.gymapp.model.FitnessRecord;
import com.gymapp.model.WorkoutLog;

import java.sql.SQLException;
import java.util.List;

public class FitnessService {
    private final FitnessRecordDAO fitnessRecordDAO = new FitnessRecordDAO();
    private final WorkoutLogDAO workoutLogDAO = new WorkoutLogDAO();

    public List<FitnessRecord> findRecords(Integer memberId) throws SQLException {
        return memberId == null ? fitnessRecordDAO.findAll() : fitnessRecordDAO.findByMember(memberId);
    }

    public FitnessRecordDAO getFitnessRecordDAO() {
        return fitnessRecordDAO;
    }

    public WorkoutLogDAO getWorkoutLogDAO() {
        return workoutLogDAO;
    }

    public List<WorkoutLog> findWorkoutLogs(Integer memberId) throws SQLException {
        return memberId == null ? workoutLogDAO.findAll() : workoutLogDAO.findByMember(memberId);
    }
}
