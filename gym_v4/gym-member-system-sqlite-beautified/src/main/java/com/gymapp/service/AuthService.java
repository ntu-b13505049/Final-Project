package com.gymapp.service;

import com.gymapp.dao.AuthDAO;
import com.gymapp.model.Member;
import com.gymapp.model.Role;
import com.gymapp.model.User;
import com.gymapp.util.AppException;

import java.sql.SQLException;

public class AuthService {
    private final AuthDAO authDAO = new AuthDAO();

    public User login(String account, String password, Role role) throws AppException {
        if (account == null || account.isBlank()) {
            throw new AppException("請輸入帳號");
        }
        if (password == null || password.isBlank()) {
            throw new AppException("請輸入密碼");
        }
        try {
            User user = authDAO.login(account.trim(), password, role)
                    .orElseThrow(() -> new AppException("帳號、密碼或角色錯誤"));
            if (user instanceof Member member && isSuspended(member.getStatus())) {
                throw new AppException("會員狀態為 Suspended，無法登入，請聯絡管理員恢復 Active 後再登入");
            }
            return user;
        } catch (SQLException e) {
            throw new AppException("登入時發生資料庫錯誤", e);
        }
    }

    private boolean isSuspended(String status) {
        return status != null && ("Suspended".equalsIgnoreCase(status.trim()) || "停權".equals(status.trim()));
    }
}
