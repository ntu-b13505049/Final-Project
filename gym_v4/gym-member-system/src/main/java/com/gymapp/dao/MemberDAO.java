package com.gymapp.dao;

import com.gymapp.database.Db;
import com.gymapp.model.Member;
import com.gymapp.util.PasswordUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class MemberDAO {
    public List<Member> findAll() throws SQLException {
        return Db.query("SELECT * FROM member_info ORDER BY id", null, this::map);
    }

    public Optional<Member> findById(int id) throws SQLException {
        return Db.queryOne("SELECT * FROM member_info WHERE id = ?", ps -> ps.setInt(1, id), this::map);
    }

    public int insert(Member member, String rawPassword) throws SQLException {
        String password = (rawPassword == null || rawPassword.isBlank()) ? PasswordUtil.sha256("member123") : PasswordUtil.sha256(rawPassword);
        return Db.insertAndReturnKey(
                "INSERT INTO member_info (name, account, password, phone, email, status, wallet_balance) VALUES (?, ?, ?, ?, ?, ?, ?)",
                ps -> {
                    ps.setString(1, member.getName());
                    ps.setString(2, member.getAccount());
                    ps.setString(3, password);
                    ps.setString(4, member.getPhone());
                    ps.setString(5, member.getEmail());
                    ps.setString(6, member.getStatus());
                    ps.setInt(7, member.getWallet().getBalance());
                });
    }

    public void update(Member member, String rawPasswordOrBlank) throws SQLException {
        if (rawPasswordOrBlank != null && !rawPasswordOrBlank.isBlank()) {
            Db.update("UPDATE member_info SET name=?, account=?, password=?, phone=?, email=?, status=?, wallet_balance=? WHERE id=?",
                    ps -> {
                        ps.setString(1, member.getName());
                        ps.setString(2, member.getAccount());
                        ps.setString(3, PasswordUtil.sha256(rawPasswordOrBlank));
                        ps.setString(4, member.getPhone());
                        ps.setString(5, member.getEmail());
                        ps.setString(6, member.getStatus());
                        ps.setInt(7, member.getWallet().getBalance());
                        ps.setInt(8, member.getId());
                    });
        } else {
            Db.update("UPDATE member_info SET name=?, account=?, phone=?, email=?, status=?, wallet_balance=? WHERE id=?",
                    ps -> {
                        ps.setString(1, member.getName());
                        ps.setString(2, member.getAccount());
                        ps.setString(3, member.getPhone());
                        ps.setString(4, member.getEmail());
                        ps.setString(5, member.getStatus());
                        ps.setInt(6, member.getWallet().getBalance());
                        ps.setInt(7, member.getId());
                    });
        }
    }

    public void delete(int id) throws SQLException {
        Db.update("DELETE FROM member_info WHERE id=?", ps -> ps.setInt(1, id));
    }

    public void updateWalletBalance(int id, int balance) throws SQLException {
        Db.update("UPDATE member_info SET wallet_balance=? WHERE id=?", ps -> {
            ps.setInt(1, balance);
            ps.setInt(2, id);
        });
    }

    private Member map(ResultSet rs) throws SQLException {
        return new Member(rs.getInt("id"), rs.getString("name"), rs.getString("account"), rs.getString("password"),
                rs.getString("phone"), rs.getString("email"), rs.getString("status"), rs.getInt("wallet_balance"));
    }
}
