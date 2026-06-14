package com.gymapp.service;

import com.gymapp.dao.FollowUpDAO;
import com.gymapp.model.FollowUpRecord;

import java.sql.SQLException;
import java.util.List;

public class FollowUpService {
    private final FollowUpDAO followUpDAO = new FollowUpDAO();

    public List<FollowUpRecord> findRecords(Integer memberId) throws SQLException {
        return memberId == null ? followUpDAO.findAll() : followUpDAO.findByMember(memberId);
    }

    public FollowUpDAO getFollowUpDAO() {
        return followUpDAO;
    }
}
