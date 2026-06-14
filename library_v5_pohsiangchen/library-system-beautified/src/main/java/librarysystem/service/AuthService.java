package librarysystem.service;

import librarysystem.Database;
import librarysystem.model.Admin;
import librarysystem.model.RolePolicy;
import librarysystem.model.User;
import librarysystem.util.DateUtil;
import librarysystem.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {

    public User loginUser(String studentNo, String password) {
        String sql = "SELECT * FROM users WHERE student_no = ? AND password = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, studentNo.trim());
            ps.setString(2, SecurityUtil.sha256(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("學號或密碼錯誤。");
                }
                User user = mapUser(rs);
                if (!user.isActive()) {
                    throw new IllegalArgumentException("此帳號已被停權，請聯絡管理員。");
                }
                return user;
            }
        } catch (SQLException e) {
            throw new RuntimeException("使用者登入失敗", e);
        }
    }

    public Admin loginAdmin(String username, String password) {
        String sql = "SELECT * FROM admins WHERE username = ? AND password = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, SecurityUtil.sha256(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("管理者帳號或密碼錯誤。");
                }
                return mapAdmin(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("管理者登入失敗", e);
        }
    }

    public void registerUser(String studentNo, String name, String password) {
        registerUser(studentNo, name, password, RolePolicy.NORMAL);
    }

    public void registerUser(String studentNo, String name, String password, String roleLevel) {
        if (studentNo == null || studentNo.isBlank() || name == null || name.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("學號、姓名、密碼皆不可空白。");
        }

        // 進階功能：註冊時一律建立 NORMAL 帳號，VIP/GOLD/PLATINUM 需登入後送出申請，由管理者審核。
        String normalizedRoleLevel = RolePolicy.NORMAL;

        String checkSql = "SELECT COUNT(*) FROM users WHERE student_no = ?";
        String insertSql = "INSERT INTO users(student_no, name, password, role_level, created_at, status) VALUES (?, ?, ?, ?, ?, 'ACTIVE')";

        try (Connection connection = Database.getConnection()) {
            try (PreparedStatement checkPs = connection.prepareStatement(checkSql)) {
                checkPs.setString(1, studentNo.trim());
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new IllegalArgumentException("此學號已註冊。");
                    }
                }
            }

            try (PreparedStatement insertPs = connection.prepareStatement(insertSql)) {
                insertPs.setString(1, studentNo.trim());
                insertPs.setString(2, name.trim());
                insertPs.setString(3, SecurityUtil.sha256(password));
                insertPs.setString(4, normalizedRoleLevel);
                insertPs.setString(5, DateUtil.nowString());
                insertPs.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("註冊失敗", e);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("student_no"),
                rs.getString("name"),
                rs.getString("password"),
                rs.getString("role_level"),
                rs.getString("created_at"),
                rs.getString("status")
        );
    }

    private Admin mapAdmin(ResultSet rs) throws SQLException {
        return new Admin(
                rs.getInt("admin_id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("created_at")
        );
    }
}
