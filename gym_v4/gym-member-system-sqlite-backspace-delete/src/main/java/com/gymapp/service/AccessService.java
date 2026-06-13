package com.gymapp.service;

import com.gymapp.dao.AccessLogDAO;
import com.gymapp.dao.BranchDAO;
import com.gymapp.dao.MemberDAO;
import com.gymapp.model.AccessLog;
import com.gymapp.model.Branch;
import com.gymapp.model.Member;
import com.gymapp.util.AppException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AccessService {
    private final AccessLogDAO accessLogDAO = new AccessLogDAO();
    private final BranchDAO branchDAO = new BranchDAO();
    private final MemberDAO memberDAO = new MemberDAO();

    public String checkIn(int memberId, int branchId) throws AppException {
        try {
            Member member = memberDAO.findById(memberId).orElseThrow(() -> new AppException("找不到會員 ID：" + memberId));
            if (!member.isActive()) {
                throw new AppException("會員狀態不是 Active，無法進場");
            }
            Optional<AccessLog> latest = accessLogDAO.findLatestByMember(memberId);
            if (latest.isPresent() && "進場".equals(latest.get().getAction())) {
                throw new AppException("此會員尚未出場，不能重複進場");
            }
            Branch branch = branchDAO.findById(branchId).orElseThrow(() -> new AppException("找不到場館 ID：" + branchId));
            int dynamic = branchDAO.calculateCurrentCapacity(branchId);
            if (dynamic >= branch.getMaxCapacity()) {
                throw new AppException("場館已達最大容留人數，暫時無法進場");
            }
            accessLogDAO.insert(new AccessLog(0, memberId, branchId, "進場", null));
            branchDAO.recalculateAndUpdate(branchId);
            return "進場成功，QR Code 驗證序號：GYM-IN-" + memberId + "-" + System.currentTimeMillis();
        } catch (SQLException e) {
            throw new AppException("進場失敗", e);
        }
    }

    public String checkOut(int memberId) throws AppException {
        try {
            Optional<AccessLog> latest = accessLogDAO.findLatestByMember(memberId);
            if (latest.isEmpty() || !"進場".equals(latest.get().getAction())) {
                throw new AppException("此會員目前不在場內，無法出場");
            }
            int branchId = latest.get().getBranchId();
            accessLogDAO.insert(new AccessLog(0, memberId, branchId, "出場", null));
            branchDAO.recalculateAndUpdate(branchId);
            return "出場成功，已更新場館即時人數。";
        } catch (SQLException e) {
            throw new AppException("出場失敗", e);
        }
    }

    public List<AccessLog> getLogs(Integer memberId) throws SQLException {
        return memberId == null ? accessLogDAO.findAll() : accessLogDAO.findByMember(memberId);
    }
}
