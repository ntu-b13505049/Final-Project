package librarysystem.service;

import librarysystem.Database;
import librarysystem.model.Book;
import librarysystem.model.BorrowRecord;
import librarysystem.model.DashboardStats;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminService {
    private final LibraryService libraryService = new LibraryService();

    public List<User> getAllUsers() {
        return searchUsers("", "", "");
    }

    public List<User> searchUsers(String keyword, String roleLevelFilter, String statusFilter) {
        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM users
                WHERE 1 = 1
                """);
        List<String> params = new ArrayList<>();
        String normalizedKeyword = normalize(keyword);
        if (!normalizedKeyword.isBlank()) {
            sql.append("""
                    AND (
                        LOWER(student_no) LIKE ?
                        OR LOWER(name) LIKE ?
                        OR CAST(user_id AS TEXT) LIKE ?
                    )
                    """);
            String pattern = like(keyword);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        String role = roleLevelFilter == null ? "" : roleLevelFilter.trim().toUpperCase();
        if (!role.isBlank() && !"全部".equals(role) && !"ALL".equals(role)) {
            sql.append(" AND role_level = ?\n");
            params.add(RolePolicy.normalize(role));
        }

        String status = statusFilter == null ? "" : statusFilter.trim().toUpperCase();
        if (!status.isBlank() && !"全部".equals(status) && !"ALL".equals(status)) {
            sql.append(" AND status = ?\n");
            params.add(status);
        }

        sql.append(" ORDER BY created_at DESC");

        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            bindStringParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) {
                    users.add(new User(
                            rs.getInt("user_id"),
                            rs.getString("student_no"),
                            rs.getString("name"),
                            rs.getString("password"),
                            rs.getString("role_level"),
                            rs.getString("created_at"),
                            rs.getString("status")
                    ));
                }
                return users;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查詢使用者失敗", e);
        }
    }

    public void updateUserStatus(int userId, String status) {
        if (!"ACTIVE".equals(status) && !"SUSPENDED".equals(status)) {
            throw new IllegalArgumentException("狀態必須是 ACTIVE 或 SUSPENDED。");
        }
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新帳號狀態失敗", e);
        }
    }

    public void updateUserRoleLevel(int userId, String roleLevel) {
        String normalized = RolePolicy.normalize(roleLevel);
        if (!RolePolicy.isSupported(normalized)) {
            throw new IllegalArgumentException("等級必須是：" + RolePolicy.supportedLevelsText());
        }
        String sql = "UPDATE users SET role_level = ? WHERE user_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, normalized);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新使用者等級失敗", e);
        }
    }

    public List<Book> getAllBooks() {
        return searchBooks("", "");
    }

    public List<Book> searchBooks(String keyword, String statusFilter) {
        StringBuilder sql = new StringBuilder("""
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
                WHERE 1 = 1
                """);
        List<String> params = new ArrayList<>();
        String normalizedKeyword = normalize(keyword);
        if (!normalizedKeyword.isBlank()) {
            sql.append("""
                    AND (
                        CAST(b.book_id AS TEXT) LIKE ?
                        OR LOWER(b.title) LIKE ?
                        OR LOWER(COALESCE(b.authors, '')) LIKE ?
                        OR LOWER(COALESCE(b.subjects, '')) LIKE ?
                        OR LOWER(COALESCE(b.publisher, '')) LIKE ?
                        OR LOWER(COALESCE(b.publish_year, '')) LIKE ?
                        OR LOWER(COALESCE(b.edition, '')) LIKE ?
                        OR LOWER(COALESCE(b.source, '')) LIKE ?
                        OR LOWER(COALESCE(b.note, '')) LIKE ?
                        OR EXISTS (
                            SELECT 1 FROM book_isbns bi
                            WHERE bi.book_id = b.book_id AND LOWER(bi.isbn) LIKE ?
                        )
                    )
                    """);
            String pattern = like(keyword);
            for (int i = 0; i < 10; i++) {
                params.add(pattern);
            }
        }

        String status = statusFilter == null ? "" : statusFilter.trim().toUpperCase();
        switch (status) {
            case "ACTIVE" -> sql.append(" AND b.active = 1\n");
            case "INACTIVE" -> sql.append(" AND b.active = 0\n");
            case "BORROWED" -> sql.append("""
                    AND b.active = 1
                    AND EXISTS (SELECT 1 FROM borrow_records br WHERE br.book_id = b.book_id AND br.return_date IS NULL)
                    """);
            case "AVAILABLE" -> sql.append("""
                    AND b.active = 1
                    AND NOT EXISTS (SELECT 1 FROM borrow_records br WHERE br.book_id = b.book_id AND br.return_date IS NULL)
                    """);
            default -> { }
        }
        sql.append(" ORDER BY b.active DESC, b.title ASC");

        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            bindStringParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Book> books = new ArrayList<>();
                while (rs.next()) {
                    books.add(mapBookForAdmin(rs));
                }
                return books;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查詢書籍失敗", e);
        }
    }

    public void addBook(Book book) {
        validateBookForSave(book, false);

        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            int bookId;
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO books(title, authors, subjects, publisher, publish_year, edition, format_desc, source, note, active)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                    """, Statement.RETURN_GENERATED_KEYS)) {
                bindBookFields(ps, book);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("新增書籍失敗，未取得主鍵。");
                    }
                    bookId = keys.getInt(1);
                }
            }
            insertBookIsbns(connection, bookId, book.getIsbn());
            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException("新增書籍失敗", e);
        }
    }

    public void updateBook(Book book) {
        validateBookForSave(book, true);

        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE books
                    SET title = ?,
                        authors = ?,
                        subjects = ?,
                        publisher = ?,
                        publish_year = ?,
                        edition = ?,
                        format_desc = ?,
                        source = ?,
                        note = ?
                    WHERE book_id = ?
                    """)) {
                bindBookFields(ps, book);
                ps.setInt(10, book.getBookId());
                int updated = ps.executeUpdate();
                if (updated == 0) {
                    throw new IllegalArgumentException("找不到要修改的書籍。");
                }
            }

            try (PreparedStatement deleteIsbns = connection.prepareStatement("DELETE FROM book_isbns WHERE book_id = ?")) {
                deleteIsbns.setInt(1, book.getBookId());
                deleteIsbns.executeUpdate();
            }
            insertBookIsbns(connection, book.getBookId(), book.getIsbn());
            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException("修改書籍失敗", e);
        }
    }

    public void toggleBookActive(int bookId, boolean active) {
        try (Connection connection = Database.getConnection()) {
            if (!active && hasOpenBorrow(connection, bookId)) {
                throw new IllegalArgumentException("此書目前仍在借閱中，建議先等待歸還再下架。");
            }
            try (PreparedStatement ps = connection.prepareStatement("UPDATE books SET active = ? WHERE book_id = ?")) {
                ps.setInt(1, active ? 1 : 0);
                ps.setInt(2, bookId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("更新書籍上下架狀態失敗", e);
        }
    }

    private void validateBookForSave(Book book, boolean requireExistingId) {
        if (book == null) {
            throw new IllegalArgumentException("書籍資料不可為空。");
        }
        if (requireExistingId && book.getBookId() <= 0) {
            throw new IllegalArgumentException("修改書籍時必須指定書籍 ID。");
        }
        if (book.getTitle() == null || book.getTitle().isBlank()) {
            throw new IllegalArgumentException("書名不可空白。");
        }
        if (book.getAuthors() == null || book.getAuthors().isBlank()) {
            throw new IllegalArgumentException("作者不可空白。");
        }
        if (book.getIsbn() == null || book.getIsbn().isBlank()) {
            throw new IllegalArgumentException("ISBN 不可空白。");
        }
    }

    private void bindBookFields(PreparedStatement ps, Book book) throws SQLException {
        ps.setString(1, clean(book.getTitle()));
        ps.setString(2, clean(book.getAuthors()));
        ps.setString(3, clean(book.getSubjects()));
        ps.setString(4, clean(book.getPublisher()));
        ps.setString(5, clean(book.getPublishYear()));
        ps.setString(6, clean(book.getEdition()));
        ps.setString(7, clean(book.getFormatDesc()));
        ps.setString(8, clean(book.getSource()));
        ps.setString(9, clean(book.getNote()));
    }

    private void insertBookIsbns(Connection connection, int bookId, String isbnText) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO book_isbns(book_id, isbn) VALUES (?, ?)")) {
            for (String isbn : splitIsbns(isbnText)) {
                ps.setInt(1, bookId);
                ps.setString(2, isbn);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private List<String> splitIsbns(String isbnText) {
        List<String> isbns = new ArrayList<>();
        if (isbnText == null) {
            return isbns;
        }
        for (String raw : isbnText.split("[,，;；\n\r]+")) {
            String isbn = raw.trim();
            if (!isbn.isBlank() && !isbns.contains(isbn)) {
                isbns.add(isbn);
            }
        }
        if (isbns.isEmpty()) {
            throw new IllegalArgumentException("ISBN 不可空白。");
        }
        return isbns;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public List<BorrowRecord> getBorrowRecords(String studentNoKeyword, String bookTitleKeyword) {
        return getBorrowRecords(studentNoKeyword, "", bookTitleKeyword, "");
    }

    public List<BorrowRecord> getBorrowRecords(String studentNoKeyword, String borrowerNameKeyword, String bookTitleKeyword, String statusFilter) {
        String sql = """
                SELECT br.record_id, br.user_id, br.book_id, br.borrow_date, br.due_date, br.return_date, br.borrow_days,
                       u.student_no, u.name AS borrower_name, u.role_level AS user_role_level, b.title AS book_title
                FROM borrow_records br
                JOIN users u ON br.user_id = u.user_id
                JOIN books b ON br.book_id = b.book_id
                WHERE (? = '' OR LOWER(u.student_no) LIKE ?)
                  AND (? = '' OR LOWER(u.name) LIKE ?)
                  AND (? = '' OR LOWER(b.title) LIKE ? OR CAST(b.book_id AS TEXT) LIKE ?)
                ORDER BY br.borrow_date DESC
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            String studentNo = normalize(studentNoKeyword);
            String studentLike = like(studentNoKeyword);
            String borrowerName = normalize(borrowerNameKeyword);
            String borrowerLike = like(borrowerNameKeyword);
            String bookTitle = normalize(bookTitleKeyword);
            String bookLike = like(bookTitleKeyword);
            ps.setString(1, studentNo);
            ps.setString(2, studentLike);
            ps.setString(3, borrowerName);
            ps.setString(4, borrowerLike);
            ps.setString(5, bookTitle);
            ps.setString(6, bookLike);
            ps.setString(7, bookLike);
            try (ResultSet rs = ps.executeQuery()) {
                List<BorrowRecord> list = new ArrayList<>();
                while (rs.next()) {
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
                    boolean overdue = record.getReturnDate() == null
                            ? LocalDateTime.now().isAfter(record.getDueDate())
                            : record.getReturnDate().isAfter(record.getDueDate());
                    long overdueDays = overdue ? DateUtil.overdueDays(record.getDueDate(), record.getReturnDate()) : 0;
                    int fineRate = RolePolicy.finePerDay(record.getUserRoleLevel());
                    record.setOverdue(overdue);
                    record.setOverdueDays(overdueDays);
                    record.setFineRatePerDay(fineRate);
                    record.setFineAmount(overdueDays * fineRate);
                    if (matchesBorrowRecordStatus(record, statusFilter)) {
                        list.add(record);
                    }
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查詢借閱紀錄失敗", e);
        }
    }

    private boolean matchesBorrowRecordStatus(BorrowRecord record, String statusFilter) {
        String status = statusFilter == null ? "" : statusFilter.trim().toUpperCase();
        return switch (status) {
            case "CURRENT" -> record.getReturnDate() == null;
            case "RETURNED" -> record.getReturnDate() != null;
            case "OVERDUE" -> record.isOverdue();
            case "NOT_OVERDUE" -> !record.isOverdue();
            default -> true;
        };
    }

    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        try (Connection connection = Database.getConnection()) {
            stats.setTotalBooks(count(connection, "SELECT COUNT(*) FROM books"));
            stats.setActiveUsers(count(connection, "SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'"));
            stats.setCurrentBorrows(count(connection, "SELECT COUNT(*) FROM borrow_records WHERE return_date IS NULL"));
            stats.setOverdueBorrows(count(connection, "SELECT COUNT(*) FROM borrow_records WHERE return_date IS NULL AND due_date < '" + DateUtil.nowString() + "'"));
            stats.setTotalReviews(count(connection, "SELECT COUNT(*) FROM reviews"));
            stats.setWaitingReservations(count(connection, "SELECT COUNT(*) FROM reservations WHERE status IN ('WAITING', 'NOTIFIED')"));
            stats.setPendingRoleRequests(count(connection, "SELECT COUNT(*) FROM role_change_requests WHERE status = 'PENDING'"));
            return stats;
        } catch (SQLException e) {
            throw new RuntimeException("查詢儀表板資料失敗", e);
        }
    }

    public Map<String, Integer> getSubjectBorrowStats() {
        String sql = """
                SELECT b.subjects
                FROM borrow_records br
                JOIN books b ON br.book_id = b.book_id
                """;
        Map<String, Integer> counter = new LinkedHashMap<>();
        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                String subjects = rs.getString("subjects");
                if (subjects == null || subjects.isBlank()) {
                    counter.merge("未分類", 1, Integer::sum);
                    continue;
                }
                for (String subject : subjects.split(",")) {
                    String key = subject.trim();
                    if (key.isBlank()) {
                        key = "未分類";
                    }
                    counter.merge(key, 1, Integer::sum);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查詢主題統計失敗", e);
        }

        return counter.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(10)
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        Map::putAll);
    }

    public List<Review> getAllReviews() {
        return searchReviews("", "");
    }

    public List<Review> searchReviews(String keyword, String ratingFilter) {
        StringBuilder sql = new StringBuilder("""
                SELECT r.review_id, r.user_id, r.book_id, r.rating, r.content, r.created_at,
                       u.name AS user_name, b.title AS book_title
                FROM reviews r
                JOIN users u ON r.user_id = u.user_id
                JOIN books b ON r.book_id = b.book_id
                WHERE 1 = 1
                """);
        List<String> params = new ArrayList<>();
        String normalizedKeyword = normalize(keyword);
        if (!normalizedKeyword.isBlank()) {
            sql.append("""
                    AND (
                        CAST(r.review_id AS TEXT) LIKE ?
                        OR LOWER(u.name) LIKE ?
                        OR LOWER(u.student_no) LIKE ?
                        OR LOWER(b.title) LIKE ?
                        OR CAST(b.book_id AS TEXT) LIKE ?
                        OR LOWER(r.content) LIKE ?
                    )
                    """);
            String pattern = like(keyword);
            for (int i = 0; i < 6; i++) {
                params.add(pattern);
            }
        }
        String rating = ratingFilter == null ? "" : ratingFilter.trim();
        if (!rating.isBlank() && !"全部".equals(rating) && !"ALL".equalsIgnoreCase(rating)) {
            sql.append(" AND r.rating = ?\n");
            params.add(rating);
        }
        sql.append(" ORDER BY r.created_at DESC");

        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            bindStringParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Review> reviews = new ArrayList<>();
                while (rs.next()) {
                    Review review = new Review();
                    review.setReviewId(rs.getInt("review_id"));
                    review.setUserId(rs.getInt("user_id"));
                    review.setBookId(rs.getInt("book_id"));
                    review.setUserName(rs.getString("user_name"));
                    review.setBookTitle(rs.getString("book_title"));
                    review.setRating(rs.getInt("rating"));
                    review.setContent(rs.getString("content"));
                    review.setCreatedAt(rs.getString("created_at"));
                    reviews.add(review);
                }
                return reviews;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查詢書評失敗", e);
        }
    }

    public List<Reservation> getAllReservations() {
        return searchReservations("", "");
    }

    public List<Reservation> searchReservations(String keyword, String statusFilter) {
        StringBuilder sql = new StringBuilder("""
                SELECT r.reservation_id, r.user_id, r.book_id, r.status, r.created_at, r.notified_at,
                       b.title AS book_title, u.name AS user_name
                FROM reservations r
                JOIN books b ON r.book_id = b.book_id
                JOIN users u ON r.user_id = u.user_id
                WHERE 1 = 1
                """);
        List<String> params = new ArrayList<>();
        String normalizedKeyword = normalize(keyword);
        if (!normalizedKeyword.isBlank()) {
            sql.append("""
                    AND (
                        CAST(r.reservation_id AS TEXT) LIKE ?
                        OR LOWER(u.name) LIKE ?
                        OR LOWER(u.student_no) LIKE ?
                        OR LOWER(b.title) LIKE ?
                        OR CAST(b.book_id AS TEXT) LIKE ?
                    )
                    """);
            String pattern = like(keyword);
            for (int i = 0; i < 5; i++) {
                params.add(pattern);
            }
        }
        String status = statusFilter == null ? "" : statusFilter.trim().toUpperCase();
        if (!status.isBlank() && !"全部".equals(status) && !"ALL".equals(status)) {
            sql.append(" AND r.status = ?\n");
            params.add(status);
        }
        sql.append(" ORDER BY r.created_at DESC");

        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            bindStringParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> reservations = new ArrayList<>();
                while (rs.next()) {
                    Reservation reservation = new Reservation();
                    reservation.setReservationId(rs.getInt("reservation_id"));
                    reservation.setUserId(rs.getInt("user_id"));
                    reservation.setBookId(rs.getInt("book_id"));
                    reservation.setBookTitle(rs.getString("book_title"));
                    reservation.setUserName(rs.getString("user_name"));
                    reservation.setStatus(rs.getString("status"));
                    reservation.setCreatedAt(rs.getString("created_at"));
                    reservation.setNotifiedAt(rs.getString("notified_at"));
                    reservations.add(reservation);
                }
                return reservations;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查詢預約紀錄失敗", e);
        }
    }

    public List<RoleChangeRequest> getRoleChangeRequests(String statusFilter) {
        return getRoleChangeRequests(statusFilter, "");
    }

    public List<RoleChangeRequest> getRoleChangeRequests(String statusFilter, String keyword) {
        String normalizedStatus = statusFilter == null ? "" : statusFilter.trim().toUpperCase();
        if ("全部".equals(normalizedStatus) || "ALL".equals(normalizedStatus)) {
            normalizedStatus = "";
        }
        StringBuilder sql = new StringBuilder("""
                SELECT r.request_id, r.user_id, r.target_level, r.reason, r.status, r.created_at,
                       r.handled_at, r.handled_by, r.admin_note,
                       u.student_no, u.name AS user_name, u.role_level AS current_level
                FROM role_change_requests r
                JOIN users u ON r.user_id = u.user_id
                WHERE (? = '' OR r.status = ?)
                """);
        List<String> params = new ArrayList<>();
        params.add(normalizedStatus);
        params.add(normalizedStatus);

        String normalizedKeyword = normalize(keyword);
        if (!normalizedKeyword.isBlank()) {
            sql.append("""
                    AND (
                        CAST(r.request_id AS TEXT) LIKE ?
                        OR LOWER(u.student_no) LIKE ?
                        OR LOWER(u.name) LIKE ?
                        OR LOWER(r.target_level) LIKE ?
                        OR LOWER(COALESCE(r.reason, '')) LIKE ?
                        OR LOWER(COALESCE(r.admin_note, '')) LIKE ?
                    )
                    """);
            String pattern = like(keyword);
            for (int i = 0; i < 6; i++) {
                params.add(pattern);
            }
        }
        sql.append(" ORDER BY CASE r.status WHEN 'PENDING' THEN 0 ELSE 1 END, r.created_at DESC");

        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            bindStringParams(ps, params);
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

    public void handleRoleChangeRequest(int requestId, boolean approve, String adminUsername, String adminNote) {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            int userId;
            String targetLevel;
            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT user_id, target_level, status
                    FROM role_change_requests
                    WHERE request_id = ?
                    """)) {
                ps.setInt(1, requestId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("找不到等級申請。");
                    }
                    if (!"PENDING".equals(rs.getString("status"))) {
                        throw new IllegalArgumentException("只有 PENDING 申請可以審核。");
                    }
                    userId = rs.getInt("user_id");
                    targetLevel = RolePolicy.normalize(rs.getString("target_level"));
                }
            }

            if (approve) {
                try (PreparedStatement ps = connection.prepareStatement("UPDATE users SET role_level = ? WHERE user_id = ?")) {
                    ps.setString(1, targetLevel);
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE role_change_requests
                    SET status = ?, handled_at = ?, handled_by = ?, admin_note = ?
                    WHERE request_id = ?
                    """)) {
                ps.setString(1, approve ? "APPROVED" : "REJECTED");
                ps.setString(2, DateUtil.nowString());
                ps.setString(3, adminUsername);
                ps.setString(4, adminNote == null ? "" : adminNote.trim());
                ps.setInt(5, requestId);
                ps.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException("審核等級申請失敗", e);
        }
    }

    public List<BorrowRecord> getOpenOverdueRecords() {
        return getBorrowRecords("", "").stream()
                .filter(record -> record.getReturnDate() == null && record.isOverdue())
                .toList();
    }

    private Book mapBookForAdmin(ResultSet rs) throws SQLException {
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

    private void bindStringParams(PreparedStatement ps, List<String> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setString(i + 1, params.get(i));
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

    private boolean hasOpenBorrow(Connection connection, int bookId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM borrow_records WHERE book_id = ? AND return_date IS NULL")) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private int count(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim().toLowerCase();
    }

    private String like(String input) {
        String normalized = normalize(input);
        return normalized.isBlank() ? "" : "%" + normalized + "%";
    }
}
