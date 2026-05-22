package com.gymapp.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Db {
    private Db() {}

    @FunctionalInterface
    public interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public static int update(String sql, StatementBinder binder) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(ps);
            }
            return ps.executeUpdate();
        }
    }

    public static int insertAndReturnKey(String sql, StatementBinder binder) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (binder != null) {
                binder.bind(ps);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public static <T> List<T> query(String sql, StatementBinder binder, RowMapper<T> mapper) throws SQLException {
        List<T> rows = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
            }
        }
        return rows;
    }

    public static <T> Optional<T> queryOne(String sql, StatementBinder binder, RowMapper<T> mapper) throws SQLException {
        List<T> rows = query(sql, binder, mapper);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
