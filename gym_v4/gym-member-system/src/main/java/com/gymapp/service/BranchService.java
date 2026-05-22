package com.gymapp.service;

import com.gymapp.dao.BranchDAO;
import com.gymapp.model.Branch;

import java.sql.SQLException;
import java.util.List;

public class BranchService {
    private final BranchDAO branchDAO = new BranchDAO();

    public List<Branch> findAllWithDynamicCapacity() throws SQLException {
        List<Branch> branches = branchDAO.findAll();
        for (Branch branch : branches) {
            int current = branchDAO.calculateCurrentCapacity(branch.getBranchId());
            branch.setCurrentCapacity(current);
            branchDAO.recalculateAndUpdate(branch.getBranchId());
        }
        return branches;
    }

    public BranchDAO getBranchDAO() {
        return branchDAO;
    }
}
