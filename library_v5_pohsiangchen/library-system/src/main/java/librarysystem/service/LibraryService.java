package librarysystem.service;

import librarysystem.Database;
import librarysystem.model.Book;
import librarysystem.model.BorrowRecord;
import librarysystem.model.Reservation;
import librarysystem.model.Review;
import librarysystem.model.RoleChangeRequest;
import librarysystem.model.RolePolicy;
import librarysystem.model.User;
import librarysystem.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LibraryService {

    public User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
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
        } catch (SQLException e) {
            throw new RuntimeException("查詢使用者失敗", e);
        }
    }

    public List<Book> searchBooks(String title, String author, String subject, String publisher, String isbn) {
        String sql = """
                SELECT b.book_id,
                       b.title,
                       b.authors,
                       b.subjects,
                       b.publisher,
                       b.publish_year,
                       b.edition,
                       b.format_desc,
                       b.source,
                       b.note,
                       b.active,
                       COALESCE((SELECT GROUP_CONCAT(isbn, ', ') FROM book_isbns WHERE book_id = b.book_id), '') AS isbn,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM borrow_records br
                           WHERE br.book_id = b.book_id AND br.return_date IS NULL
                       ) THEN 1 ELSE 0 END AS borrowed
                FROM books b
                WHERE (? = '' OR LOWER(b.title) LIKE ?)
                  AND (? = '' OR LOWER(b.authors) LIKE ?)
                  AND (? = '' OR LOWER(COALESCE(b.subjects, '')) LIKE ?)
                  AND (? = '' OR LOWER(COALESCE(b.publisher, '')) LIKE ?)
                  AND (? = '' OR EXISTS (
                        SELECT 1 FROM book_isbns bi
                        WHERE bi.book_id = b.book_id AND LOWER(bi.isbn) LIKE ?
                  ))
                ORDER BY b.active DESC, b.title ASC
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            bindSearchParameters(ps, title, author, subject, publisher, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                List<Book> books = new ArrayList<>();
                while (rs.next()) {
                    books.add(mapBook(rs));
                }
                return books;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查詢書籍失敗", e);
        }
    }

    public Book getBookById(int bookId) {
        String sql = """
                SELECT b.book_id,
                       b.title,
                       b.authors,
                       b.subjects,
                       b.publisher,
                       b.publish_year,
                       b.edition,
                       b.format_desc,
                       b.source,
                       b.note,
                       b.active,
                       COALESCE((SELECT GROUP_CONCAT(isbn, ', ') FROM book_isbns WHERE book_id = b.book_id), '') AS isbn,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM borrow_records br
                           WHERE br.book_id = b.book_id AND br.return_date IS NULL
                       ) THEN 1 ELSE 0 END AS borrowed
                FROM books b
                WHERE b.book_id = ?
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapBook(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("查詢書籍失敗", e);
        }
    }

    public int getBorrowLimit(User user) {
        return RolePolicy.borrowLimit(user.getRoleLevel());
    }

    public int getReservationLimit(User user) {
        return RolePolicy.reservationLimit(user.getRoleLevel());
    }

    public int getFinePerDay(User user) {
        return RolePolicy.finePerDay(user.getRoleLevel());
    }

    public int[] getAllowedDurations(User user) {
        return RolePolicy.allowedDurations(user.getRoleLevel());
    }

    public int getCurrentBorrowCount(int userId) {
        String sql = "SELECT COUNT(*) FROM borrow_records WHERE user_id = ? AND return_date IS NULL";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查詢借閱數量失敗", e);
        }
    }

    public void borrowBook(User user, int bookId, int borrowDays) {
        if (!user.isActive()) {
            throw new IllegalArgumentException("帳號已停權，無法借書。");
        }
        if (!RolePolicy.durationAllowed(user.getRoleLevel(), borrowDays)) {
            throw new IllegalArgumentException(RolePolicy.displayName(user.getRoleLevel()) + " 可選借閱期限：" + RolePolicy.durationText(user.getRoleLevel()) + "。");
        }
        if (getCurrentBorrowCount(user.getUserId()) >= getBorrowLimit(user)) {
            throw new IllegalArgumentException("已達同時借閱上限：" + getBorrowLimit(user) + " 本。");
        }

        try (Connection connection = Database.getConnection()) {
            if (!isUserStillActive(connection, user.getUserId())) {
                throw new IllegalArgumentException("帳號目前已停權，無法借書。");
            }
            Book book = getBookById(bookId);
            if (book == null) {
                throw new IllegalArgumentException("找不到書籍。");
            }
            if (!book.isActive()) {
                throw new IllegalArgumentException("此書已下架，無法借閱。");
            }
            if (book.isBorrowed()) {
                throw new IllegalArgumentException("此書目前已借出，可改用預約功能。");
            }
            if (hasNotifiedReservationForOther(connection, bookId, user.getUserId())) {
                throw new IllegalArgumentException("此書已通知預約者保留，暫時不能借閱。");
            }

            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO borrow_records(user_id, book_id, borrow_date, due_date, return_date, borrow_days, created_at)
                    VALUES (?, ?, ?, ?, NULL, ?, ?)
                    """)) {
                LocalDateTime now = LocalDateTime.now();
                ps.setInt(1, user.getUserId());
                ps.setInt(2, bookId);
                ps.setString(3, DateUtil.format(now));
                ps.setString(4, DateUtil.format(now.plusDays(borrowDays)));
                ps.setInt(5, borrowDays);
                ps.setString(6, DateUtil.format(now));
                ps.executeUpdate();
            }

            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE reservations
                    SET status = 'FULFILLED'
                    WHERE user_id = ? AND book_id = ? AND status IN ('WAITING', 'NOTIFIED')
                    """)) {
                ps.setInt(1, user.getUserId());
                ps.setInt(2, bookId);
                ps.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException("借書失敗", e);
        }
    }

    public String returnBook(int userId, int recordId) {
        String selectSql = "SELECT * FROM borrow_records WHERE record_id = ? AND user_id = ? AND return_date IS NULL";
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            BorrowRecord record;
            try (PreparedStatement selectPs = connection.prepareStatement(selectSql)) {
                selectPs.setInt(1, recordId);
                selectPs.setInt(2, userId);
                try (ResultSet rs = selectPs.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("找不到可歸還的借閱紀錄。");
                    }
                    record = new BorrowRecord();
                    record.setRecordId(rs.getInt("record_id"));
                    record.setBookId(rs.getInt("book_id"));
                    record.setDueDate(DateUtil.parse(rs.getString("due_date")));
                }
            }

            LocalDateTime now = LocalDateTime.now();
            try (PreparedStatement updatePs = connection.prepareStatement("UPDATE borrow_records SET return_date = ? WHERE record_id = ?")) {
                updatePs.setString(1, DateUtil.format(now));
                updatePs.setInt(2, recordId);
                updatePs.executeUpdate();
            }

            boolean notified = notifyNextReservationIfPossible(connection, record.getBookId());
            connection.commit();

            User user = getUserById(userId);
            long overdueDays = DateUtil.overdueDays(record.getDueDate(), now);
            int finePerDay = user == null ? RolePolicy.finePerDay(RolePolicy.NORMAL) : RolePolicy.finePerDay(user.getRoleLevel());
            long fineAmount = overdueDays * finePerDay;
            StringBuilder message = new StringBuilder("還書成功。");
            if (overdueDays > 0) {
                message.append(" 本次逾期 ").append(overdueDays).append(" 天，模擬罰款 ").append(fineAmount).append(" 元（").append(finePerDay).append(" 元/天）。");
            }
            if (notified) {
                message.append(" 已通知下一位預約者。");
            }
            return message.toString();
        } catch (SQLException e) {
            throw new RuntimeException("還書失敗", e);
        }
    }

    public List<BorrowRecord> getCurrentBorrowedRecords(int userId) {
        String sql = baseBorrowRecordSql() + " WHERE br.user_id = ? AND br.return_date IS NULL ORDER BY br.due_date ASC";
        return queryBorrowRecords(sql, ps -> ps.setInt(1, userId));
    }

    public List<BorrowRecord> getBorrowHistoryForUser(int userId) {
        String sql = baseBorrowRecordSql() + " WHERE br.user_id = ? ORDER BY br.borrow_date DESC";
        return queryBorrowRecords(sql, ps -> ps.setInt(1, userId));
    }

    public List<BorrowRecord> getBorrowHistoryForBook(int bookId) {
        String sql = baseBorrowRecordSql() + " WHERE br.book_id = ? ORDER BY br.borrow_date DESC";
        return queryBorrowRecords(sql, ps -> ps.setInt(1, bookId));
    }

    public List<BorrowRecord> getDueSoonRecords(int userId, int reminderDays) {
        List<BorrowRecord> current = getCurrentBorrowedRecords(userId);
        List<BorrowRecord> result = new ArrayList<>();
        for (BorrowRecord record : current) {
            long days = DateUtil.daysUntil(record.getDueDate());
            if (days <= reminderDays) {
                record.setOverdue(days < 0);
                record.setOverdueDays(Math.max(-days, 0));
                result.add(record);
            }
        }
        result.sort(Comparator.comparing(BorrowRecord::getDueDate));
        return result;
    }

    public void reserveBook(int userId, int bookId) {
        try (Connection connection = Database.getConnection()) {
            User user = getUserById(userId);
            if (user == null || !user.isActive()) {
                throw new IllegalArgumentException("帳號不可用，無法預約。");
            }
            if (getActiveReservationCount(connection, userId) >= RolePolicy.reservationLimit(user.getRoleLevel())) {
                throw new IllegalArgumentException("已達同時預約上限：" + RolePolicy.reservationLimit(user.getRoleLevel()) + " 本。");
            }
            Book book = getBookById(bookId);
            if (book == null) {
                throw new IllegalArgumentException("找不到書籍。");
            }
            if (!book.isActive()) {
                throw new IllegalArgumentException("此書已下架，無法預約。");
            }
            if (!book.isBorrowed()) {
                throw new IllegalArgumentException("此書目前可借，請直接借閱，不需預約。");
            }
            if (userAlreadyBorrowingBook(connection, userId, bookId)) {
                throw new IllegalArgumentException("你已經借了這本書。");
            }
            if (userHasActiveReservation(connection, userId, bookId)) {
                throw new IllegalArgumentException("你已經預約過這本書。");
            }

            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO reservations(user_id, book_id, status, created_at, notified_at)
                    VALUES (?, ?, 'WAITING', ?, NULL)
                    """)) {
                ps.setInt(1, userId);
                ps.setInt(2, bookId);
                ps.setString(3, DateUtil.nowString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("預約失敗", e);
        }
    }

    public String cancelReservation(int userId, int reservationId) {
        String selectSql = "SELECT book_id, status FROM reservations WHERE reservation_id = ? AND user_id = ?";
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            int bookId;
            String status;
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setInt(1, reservationId);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("找不到預約紀錄。");
                    }
                    bookId = rs.getInt("book_id");
                    status = rs.getString("status");
                }
            }

            if ("FULFILLED".equals(status) || "CANCELLED".equals(status)) {
                throw new IllegalArgumentException("此預約已結束，無法取消。");
            }

            try (PreparedStatement ps = connection.prepareStatement("UPDATE reservations SET status = 'CANCELLED' WHERE reservation_id = ?")) {
                ps.setInt(1, reservationId);
                ps.executeUpdate();
            }

            boolean notified = false;
            if ("NOTIFIED".equals(status)) {
                notified = notifyNextReservationIfPossible(connection, bookId);
            }
            connection.commit();
            return notified ? "已取消預約，並通知下一位預約者。" : "已取消預約。";
        } catch (SQLException e) {
            throw new RuntimeException("取消預約失敗", e);
        }
    }

    public List<Reservation> getUserReservations(int userId) {
        String sql = """
                SELECT r.reservation_id, r.user_id, r.book_id, r.status, r.created_at, r.notified_at,
                       b.title AS book_title, u.name AS user_name
                FROM reservations r
                JOIN books b ON r.book_id = b.book_id
                JOIN users u ON r.user_id = u.user_id
                WHERE r.user_id = ?
                ORDER BY r.created_at DESC
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> reservations = new ArrayList<>();
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
                return reservations;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查詢預約紀錄失敗", e);
        }
    }

    public List<Reservation> getReservationNotifications(int userId) {
        String sql = """
                SELECT r.reservation_id, r.user_id, r.book_id, r.status, r.created_at, r.notified_at,
                       b.title AS book_title, u.name AS user_name
                FROM reservations r
                JOIN books b ON r.book_id = b.book_id
                JOIN users u ON r.user_id = u.user_id
                WHERE r.user_id = ? AND r.status = 'NOTIFIED'
                ORDER BY r.notified_at DESC
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> reservations = new ArrayList<>();
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
                return reservations;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查詢通知失敗", e);
        }
    }

    public void addFavorite(User user, int bookId) {
        if (!RolePolicy.canUseFavorites(user.getRoleLevel())) {
            throw new IllegalArgumentException("收藏功能需 VIP / GOLD / PLATINUM 等級才能使用，請先送出等級申請。");
        }
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement("INSERT INTO favorites(user_id, book_id, created_at) VALUES (?, ?, ?)")) {
            ps.setInt(1, user.getUserId());
            ps.setInt(2, bookId);
            ps.setString(3, DateUtil.nowString());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique")) {
                throw new IllegalArgumentException("此書已在收藏清單中。");
            }
            throw new RuntimeException("加入收藏失敗", e);
        }
    }

    public void removeFavorite(int userId, int bookId) {
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM favorites WHERE user_id = ? AND book_id = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, bookId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("移除收藏失敗", e);
        }
    }

    public List<Book> getFavorites(int userId) {
        String sql = """
                SELECT b.book_id,
                       b.title,
                       b.authors,
                       b.subjects,
                       b.publisher,
                       b.publish_year,
                       b.edition,
                       b.format_desc,
                       b.source,
                       b.note,
                       b.active,
                       COALESCE((SELECT GROUP_CONCAT(isbn, ', ') FROM book_isbns WHERE book_id = b.book_id), '') AS isbn,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM borrow_records br
                           WHERE br.book_id = b.book_id AND br.return_date IS NULL
                       ) THEN 1 ELSE 0 END AS borrowed
                FROM favorites f
                JOIN books b ON f.book_id = b.book_id
                WHERE f.user_id = ?
                ORDER BY f.created_at DESC
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Book> books = new ArrayList<>();
                while (rs.next()) {
                    books.add(mapBook(rs));
                }
                return books;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查詢收藏清單失敗", e);
        }
    }

    public void submitReview(int userId, int bookId, int rating, String content) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("評分必須介於 1 到 5。");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("書評內容不可空白。");
        }

        try (Connection connection = Database.getConnection()) {
            if (!hasReturnedBorrowRecord(connection, userId, bookId)) {
                throw new IllegalArgumentException("必須歸還過這本書後才能寫書評。");
            }
            String sql = """
                    INSERT INTO reviews(user_id, book_id, rating, content, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(user_id, book_id) DO UPDATE SET
                        rating = excluded.rating,
                        content = excluded.content,
                        created_at = excluded.created_at
                    """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, bookId);
                ps.setInt(3, rating);
                ps.setString(4, content.trim());
                ps.setString(5, DateUtil.nowString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("送出書評失敗", e);
        }
    }

    public List<Review> getReviewsForBook(int bookId) {
        String sql = """
                SELECT r.review_id, r.user_id, r.book_id, r.rating, r.content, r.created_at,
                       u.name AS user_name, b.title AS book_title
                FROM reviews r
                JOIN users u ON r.user_id = u.user_id
                JOIN books b ON r.book_id = b.book_id
                WHERE r.book_id = ?
                ORDER BY r.created_at DESC
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Review> reviews = new ArrayList<>();
                while (rs.next()) {
                    reviews.add(mapReview(rs));
                }
                return reviews;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查詢書評失敗", e);
        }
    }


    public List<RoleChangeRequest> getRoleChangeRequestsForUser(int userId) {
        String sql = """
                SELECT r.request_id, r.user_id, r.target_level, r.reason, r.status, r.created_at,
                       r.handled_at, r.handled_by, r.admin_note,
                       u.student_no, u.name AS user_name, u.role_level AS current_level
                FROM role_change_requests r
                JOIN users u ON r.user_id = u.user_id
                WHERE r.user_id = ?
                ORDER BY r.created_at DESC
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<RoleChangeRequest> requests = new ArrayList<>();
                while (rs.next()) {
                    requests.add(mapRoleChangeRequest(rs));
                }
                return requests;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查詢等級申請失敗", e);
        }
    }

    public void submitRoleChangeRequest(int userId, String targetLevel, String reason) {
        String normalizedTarget = RolePolicy.normalize(targetLevel);
        if (!RolePolicy.canUpgradeTo(normalizedTarget)) {
            throw new IllegalArgumentException("可申請的目標等級為 VIP / GOLD / PLATINUM。");
        }
        User user = getUserById(userId);
        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException("帳號不可用，無法送出申請。");
        }
        if (RolePolicy.normalize(user.getRoleLevel()).equals(normalizedTarget)) {
            throw new IllegalArgumentException("你目前已經是 " + normalizedTarget + " 等級。");
        }

        try (Connection connection = Database.getConnection()) {
            if (hasPendingRoleChangeRequest(connection, userId)) {
                throw new IllegalArgumentException("你已有待審核的等級申請，請先等待管理者處理。");
            }
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO role_change_requests(user_id, target_level, reason, status, created_at)
                    VALUES (?, ?, ?, 'PENDING', ?)
                    """)) {
                ps.setInt(1, userId);
                ps.setString(2, normalizedTarget);
                ps.setString(3, reason == null ? "" : reason.trim());
                ps.setString(4, DateUtil.nowString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("送出等級申請失敗", e);
        }
    }

    public void cancelRoleChangeRequest(int userId, int requestId) {
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     UPDATE role_change_requests
                     SET status = 'CANCELLED', handled_at = ?, admin_note = '使用者自行取消'
                     WHERE request_id = ? AND user_id = ? AND status = 'PENDING'
                     """)) {
            ps.setString(1, DateUtil.nowString());
            ps.setInt(2, requestId);
            ps.setInt(3, userId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("只有待審核的申請可以取消。");
            }
        } catch (SQLException e) {
            throw new RuntimeException("取消等級申請失敗", e);
        }
    }

    private void bindSearchParameters(PreparedStatement ps, String title, String author, String subject,
                                      String publisher, String isbn) throws SQLException {
        int index = 1;
        for (String value : List.of(safeLower(title), safeLower(title), safeLower(author), safeLower(author),
                safeLower(subject), safeLower(subject), safeLower(publisher), safeLower(publisher),
                safeLower(isbn), safeLike(isbn))) {
            ps.setString(index++, value);
        }
        ps.setString(2, safeLike(title));
        ps.setString(4, safeLike(author));
        ps.setString(6, safeLike(subject));
        ps.setString(8, safeLike(publisher));
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String safeLike(String value) {
        String normalized = safeLower(value);
        return normalized.isBlank() ? "" : "%" + normalized + "%";
    }

    private boolean isUserStillActive(Connection connection, int userId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT status FROM users WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && "ACTIVE".equals(rs.getString(1));
            }
        }
    }

    private boolean hasNotifiedReservationForOther(Connection connection, int bookId, int currentUserId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservations WHERE book_id = ? AND status = 'NOTIFIED' AND user_id <> ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            ps.setInt(2, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean userAlreadyBorrowingBook(Connection connection, int userId, int bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM borrow_records WHERE user_id = ? AND book_id = ? AND return_date IS NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean userHasActiveReservation(Connection connection, int userId, int bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservations WHERE user_id = ? AND book_id = ? AND status IN ('WAITING', 'NOTIFIED')";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }


    private int getActiveReservationCount(Connection connection, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservations WHERE user_id = ? AND status IN ('WAITING', 'NOTIFIED')";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private boolean hasReturnedBorrowRecord(Connection connection, int userId, int bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM borrow_records WHERE user_id = ? AND book_id = ? AND return_date IS NOT NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean notifyNextReservationIfPossible(Connection connection, int bookId) throws SQLException {
        if (isBookBorrowed(connection, bookId)) {
            return false;
        }
        if (hasExistingNotifiedReservation(connection, bookId)) {
            return false;
        }
        String query = "SELECT reservation_id FROM reservations WHERE book_id = ? AND status = 'WAITING' ORDER BY created_at ASC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                int reservationId = rs.getInt("reservation_id");
                try (PreparedStatement updatePs = connection.prepareStatement(
                        "UPDATE reservations SET status = 'NOTIFIED', notified_at = ? WHERE reservation_id = ?")) {
                    updatePs.setString(1, DateUtil.nowString());
                    updatePs.setInt(2, reservationId);
                    updatePs.executeUpdate();
                    return true;
                }
            }
        }
    }

    private boolean hasExistingNotifiedReservation(Connection connection, int bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservations WHERE book_id = ? AND status = 'NOTIFIED'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean isBookBorrowed(Connection connection, int bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM borrow_records WHERE book_id = ? AND return_date IS NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private String baseBorrowRecordSql() {
        return """
                SELECT br.record_id, br.user_id, br.book_id, br.borrow_date, br.due_date, br.return_date, br.borrow_days,
                       u.student_no, u.name AS borrower_name, u.role_level AS user_role_level, b.title AS book_title
                FROM borrow_records br
                JOIN users u ON br.user_id = u.user_id
                JOIN books b ON br.book_id = b.book_id
                """;
    }

    private List<BorrowRecord> queryBorrowRecords(String sql, SqlConsumer<PreparedStatement> binder) {
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.accept(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<BorrowRecord> records = new ArrayList<>();
                while (rs.next()) {
                    records.add(mapBorrowRecord(rs));
                }
                return records;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查詢借閱紀錄失敗", e);
        }
    }

    private Book mapBook(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setBookId(rs.getInt("book_id"));
        book.setTitle(rs.getString("title"));
        book.setAuthors(rs.getString("authors"));
        book.setSubjects(rs.getString("subjects"));
        book.setPublisher(rs.getString("publisher"));
        book.setPublishYear(rs.getString("publish_year"));
        book.setEdition(rs.getString("edition"));
        book.setFormatDesc(rs.getString("format_desc"));
        book.setSource(rs.getString("source"));
        book.setNote(rs.getString("note"));
        book.setIsbn(rs.getString("isbn"));
        book.setActive(rs.getInt("active") == 1);
        book.setBorrowed(rs.getInt("borrowed") == 1);
        return book;
    }

    private BorrowRecord mapBorrowRecord(ResultSet rs) throws SQLException {
        BorrowRecord record = new BorrowRecord();
        record.setRecordId(rs.getInt("record_id"));
        record.setUserId(rs.getInt("user_id"));
        record.setBookId(rs.getInt("book_id"));
        record.setStudentNo(rs.getString("student_no"));
        record.setBorrowerName(rs.getString("borrower_name"));
        record.setUserRoleLevel(rs.getString("user_role_level"));
        record.setBookTitle(rs.getString("book_title"));
        record.setBorrowDate(DateUtil.parse(rs.getString("borrow_date")));
        record.setDueDate(DateUtil.parse(rs.getString("due_date")));
        record.setReturnDate(DateUtil.parse(rs.getString("return_date")));
        record.setBorrowDays(rs.getInt("borrow_days"));

        LocalDateTime due = record.getDueDate();
        LocalDateTime returned = record.getReturnDate();
        boolean overdue = returned == null ? LocalDateTime.now().isAfter(due) : returned.isAfter(due);
        long overdueDays = overdue ? DateUtil.overdueDays(due, returned) : 0;
        int fineRate = RolePolicy.finePerDay(record.getUserRoleLevel());
        record.setOverdue(overdue);
        record.setOverdueDays(overdueDays);
        record.setFineRatePerDay(fineRate);
        record.setFineAmount(overdueDays * fineRate);
        return record;
    }

    private boolean hasPendingRoleChangeRequest(Connection connection, int userId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM role_change_requests WHERE user_id = ? AND status = 'PENDING'")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private RoleChangeRequest mapRoleChangeRequest(ResultSet rs) throws SQLException {
        RoleChangeRequest request = new RoleChangeRequest();
        request.setRequestId(rs.getInt("request_id"));
        request.setUserId(rs.getInt("user_id"));
        request.setStudentNo(rs.getString("student_no"));
        request.setUserName(rs.getString("user_name"));
        request.setCurrentLevel(rs.getString("current_level"));
        request.setTargetLevel(rs.getString("target_level"));
        request.setReason(rs.getString("reason"));
        request.setStatus(rs.getString("status"));
        request.setCreatedAt(rs.getString("created_at"));
        request.setHandledAt(rs.getString("handled_at"));
        request.setHandledBy(rs.getString("handled_by"));
        request.setAdminNote(rs.getString("admin_note"));
        return request;
    }

    private Review mapReview(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setReviewId(rs.getInt("review_id"));
        review.setUserId(rs.getInt("user_id"));
        review.setBookId(rs.getInt("book_id"));
        review.setUserName(rs.getString("user_name"));
        review.setBookTitle(rs.getString("book_title"));
        review.setRating(rs.getInt("rating"));
        review.setContent(rs.getString("content"));
        review.setCreatedAt(rs.getString("created_at"));
        return review;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setReservationId(rs.getInt("reservation_id"));
        reservation.setUserId(rs.getInt("user_id"));
        reservation.setBookId(rs.getInt("book_id"));
        reservation.setBookTitle(rs.getString("book_title"));
        reservation.setUserName(rs.getString("user_name"));
        reservation.setStatus(rs.getString("status"));
        reservation.setCreatedAt(rs.getString("created_at"));
        reservation.setNotifiedAt(rs.getString("notified_at"));
        return reservation;
    }

    @FunctionalInterface
    private interface SqlConsumer<T> {
        void accept(T value) throws SQLException;
    }
}
