package com.gymapp.dao;

import com.gymapp.database.Db;
import com.gymapp.model.*;
import com.gymapp.util.PasswordUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class AuthDAO {
    public Optional<User> login(String account, String rawPassword, Role role) throws SQLException {
        if (role == Role.MEMBER) {
            return loginMember(account, rawPassword).map(m -> (User) m);
        }
        return loginStaff(account, rawPassword, role).map(s -> (User) s);
    }

    private Optional<Member> loginMember(String account, String rawPassword) throws SQLException {
        Optional<Member> member = Db.queryOne(
                "SELECT * FROM member_info WHERE account = ?",
                ps -> ps.setString(1, account),
                this::mapMember);
        return member.filter(m -> PasswordUtil.matches(rawPassword, m.getPassword()));
    }

    private Optional<Staff> loginStaff(String account, String rawPassword, Role role) throws SQLException {
        Optional<Staff> staff = Db.queryOne(
                "SELECT * FROM staff WHERE account = ? AND role = ?",
                ps -> {
                    ps.setString(1, account);
                    ps.setString(2, role.displayName());
                },
                this::mapStaff);
        return staff.filter(s -> PasswordUtil.matches(rawPassword, s.getPassword()));
    }

    private Member mapMember(ResultSet rs) throws SQLException {
        return new Member(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("account"),
                rs.getString("password"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("status"),
                rs.getInt("wallet_balance"));
    }

    private Staff mapStaff(ResultSet rs) throws SQLException {
        Role role = Role.fromDisplayName(rs.getString("role"));
        Integer branchId = rs.getObject("branch_id") == null ? null : rs.getInt("branch_id");
        if (role == Role.ADMIN) {
            return new Admin(rs.getInt("id"), rs.getString("name"), rs.getString("account"), rs.getString("password"),
                    rs.getString("phone"), rs.getString("specialty"), branchId);
        }
        return new Trainer(rs.getInt("id"), rs.getString("name"), rs.getString("account"), rs.getString("password"),
                rs.getString("phone"), rs.getString("specialty"), branchId);
    }
}
