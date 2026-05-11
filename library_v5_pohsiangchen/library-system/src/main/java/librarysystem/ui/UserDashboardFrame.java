package librarysystem.ui;

import librarysystem.model.Book;
import librarysystem.model.BorrowRecord;
import librarysystem.model.Reservation;
import librarysystem.model.Review;
import librarysystem.model.RoleChangeRequest;
import librarysystem.model.RolePolicy;
import librarysystem.model.User;
import librarysystem.service.LibraryService;
import librarysystem.util.DateUtil;
import librarysystem.util.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;

public class UserDashboardFrame extends JFrame {
    private final LibraryService libraryService = new LibraryService();
    private User currentUser;

    private JLabel userInfoLabel;
    private JLabel favoriteHintLabel;

    private JTextField titleField;
    private JTextField authorField;
    private JTextField subjectField;
    private JTextField publisherField;
    private JTextField isbnField;
    private JTextArea bookDetailArea;

    private JTable reminderTable;
    private JTable dashboardBorrowTable;
    private JTable notificationTable;
    private JTable searchTable;
    private JTable myBorrowTable;
    private JTable historyTable;
    private JTable favoritesTable;
    private JTable reservationTable;
    private JTable rolePolicyTable;
    private JTable roleRequestTable;

    private DefaultTableModel reminderModel;
    private DefaultTableModel dashboardBorrowModel;
    private DefaultTableModel notificationModel;
    private DefaultTableModel searchModel;
    private DefaultTableModel myBorrowModel;
    private DefaultTableModel historyModel;
    private DefaultTableModel favoritesModel;
    private DefaultTableModel reservationModel;
    private DefaultTableModel rolePolicyModel;
    private DefaultTableModel roleRequestModel;
    private JComboBox<String> roleTargetBox;
    private JTextArea roleReasonArea;

    public UserDashboardFrame(User user) {
        this.currentUser = user;
        setTitle("圖書館系統 - 使用者介面");
        setSize(1320, 820);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        add(createHeader(), BorderLayout.NORTH);
        add(createTabs(), BorderLayout.CENTER);

        refreshAllData();
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));

        JLabel titleLabel = new JLabel("學生使用者中心");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));

        userInfoLabel = new JLabel(" ");
        userInfoLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(titleLabel);
        left.add(Box.createVerticalStrut(4));
        left.add(userInfoLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("重新整理全部資料");
        JButton logoutButton = new JButton("登出");
        refreshButton.addActionListener(e -> refreshAllData());
        logoutButton.addActionListener(e -> logout());
        right.add(refreshButton);
        right.add(logoutButton);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private JTabbedPane createTabs() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        tabbedPane.addTab("首頁與提醒", createHomeTab());
        tabbedPane.addTab("查詢與借書", createSearchTab());
        tabbedPane.addTab("目前借閱", createCurrentBorrowTab());
        tabbedPane.addTab("借還歷史", createHistoryTab());
        tabbedPane.addTab("收藏清單", createFavoritesTab());
        tabbedPane.addTab("預約清單", createReservationsTab());
        tabbedPane.addTab("等級申請", createRoleRequestTab());
        return tabbedPane;
    }

    private JPanel createHomeTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        dashboardBorrowModel = modelOf("紀錄ID", "書名", "借出時間", "到期時間", "借閱天數", "狀態", "預估罰款");
        dashboardBorrowTable = new JTable(dashboardBorrowModel);
        dashboardBorrowTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtil.applyDefaultTableStyle(dashboardBorrowTable);

        reminderModel = modelOf("紀錄ID", "書名", "到期時間", "狀態", "剩餘/逾期天數", "預估罰款");
        reminderTable = new JTable(reminderModel);
        UiUtil.applyDefaultTableStyle(reminderTable);

        notificationModel = modelOf("預約ID", "書名", "狀態", "通知時間");
        notificationTable = new JTable(notificationModel);
        UiUtil.applyDefaultTableStyle(notificationTable);

        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.add(buildTitledPanel("目前借閱中的書", new JScrollPane(dashboardBorrowTable)), BorderLayout.CENTER);

        JPanel right = new JPanel(new GridLayout(2, 1, 8, 8));
        right.add(buildTitledPanel("到期提醒 / 逾期提醒", new JScrollPane(reminderTable)));
        right.add(buildTitledPanel("預約通知", new JScrollPane(notificationTable)));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        splitPane.setDividerLocation(700);
        panel.add(splitPane, BorderLayout.CENTER);

        JLabel tipLabel = new JLabel("提醒規則：距離到期 3 天內會顯示於提醒區；逾期會自動計算天數與模擬罰款；不同等級有不同借閱/預約上限與期限。", SwingConstants.LEFT);
        panel.add(tipLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSearchTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        panel.add(createSearchConditionPanel(), BorderLayout.NORTH);

        searchModel = modelOf("書籍ID", "題名", "作者", "主題", "出版者", "ISBN", "狀態");
        searchTable = new JTable(searchModel);
        searchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtil.applyDefaultTableStyle(searchTable);
        searchTable.getSelectionModel().addListSelectionListener(e -> updateSelectedBookDetail());

        JPanel tablePanel = buildTitledPanel("查詢結果", new JScrollPane(searchTable));

        bookDetailArea = new JTextArea();
        bookDetailArea.setEditable(false);
        bookDetailArea.setLineWrap(true);
        bookDetailArea.setWrapStyleWord(true);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton borrowButton = new JButton("借閱選取書籍");
        JButton reserveButton = new JButton("預約選取書籍");
        JButton historyButton = new JButton("查看本書借閱紀錄");
        JButton reviewsButton = new JButton("查看本書書評");
        JButton favoriteButton = new JButton("加入收藏");
        JButton refreshButton = new JButton("刷新查詢結果");
        actionPanel.add(borrowButton);
        actionPanel.add(reserveButton);
        actionPanel.add(historyButton);
        actionPanel.add(reviewsButton);
        actionPanel.add(favoriteButton);
        actionPanel.add(refreshButton);

        borrowButton.addActionListener(e -> borrowSelectedBookFromSearch());
        reserveButton.addActionListener(e -> reserveSelectedBook());
        historyButton.addActionListener(e -> showSelectedBookHistory());
        reviewsButton.addActionListener(e -> showSelectedBookReviews());
        favoriteButton.addActionListener(e -> addSelectedBookToFavorite());
        refreshButton.addActionListener(e -> refreshSearchResults());

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(buildTitledPanel("書籍詳細資訊", new JScrollPane(bookDetailArea)), BorderLayout.CENTER);
        bottom.add(actionPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, bottom);
        splitPane.setResizeWeight(0.65);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCurrentBorrowTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        myBorrowModel = modelOf("紀錄ID", "書名", "借出時間", "到期時間", "借閱天數", "是否逾期", "逾期天數", "預估罰款");
        myBorrowTable = new JTable(myBorrowModel);
        myBorrowTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtil.applyDefaultTableStyle(myBorrowTable);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton returnButton = new JButton("歸還選取書籍");
        JButton refreshButton = new JButton("重新整理");
        JButton historyButton = new JButton("查看本書借閱紀錄");
        btnPanel.add(returnButton);
        btnPanel.add(historyButton);
        btnPanel.add(refreshButton);

        returnButton.addActionListener(e -> returnSelectedBook());
        historyButton.addActionListener(e -> showSelectedBookHistoryFromCurrentBorrow());
        refreshButton.addActionListener(e -> refreshCurrentBorrowTable());

        panel.add(btnPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(myBorrowTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        historyModel = modelOf("紀錄ID", "書籍ID", "書名", "借出時間", "到期時間", "歸還時間", "是否逾期", "逾期天數", "罰款");
        historyTable = new JTable(historyModel);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtil.applyDefaultTableStyle(historyTable);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton reviewButton = new JButton("對選取紀錄寫 / 改書評");
        JButton reviewsButton = new JButton("查看該書所有書評");
        JButton refreshButton = new JButton("重新整理");
        btnPanel.add(reviewButton);
        btnPanel.add(reviewsButton);
        btnPanel.add(refreshButton);

        reviewButton.addActionListener(e -> reviewSelectedBook());
        reviewsButton.addActionListener(e -> showSelectedBookReviewsFromHistory());
        refreshButton.addActionListener(e -> refreshHistoryTable());

        panel.add(btnPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFavoritesTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        favoriteHintLabel = new JLabel(" ");
        panel.add(favoriteHintLabel, BorderLayout.NORTH);

        favoritesModel = modelOf("書籍ID", "題名", "作者", "狀態", "ISBN");
        favoritesTable = new JTable(favoritesModel);
        favoritesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtil.applyDefaultTableStyle(favoritesTable);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton borrowButton = new JButton("借閱選取收藏書籍");
        JButton removeButton = new JButton("移出收藏");
        JButton reviewsButton = new JButton("查看本書書評");
        JButton refreshButton = new JButton("重新整理");
        btnPanel.add(borrowButton);
        btnPanel.add(removeButton);
        btnPanel.add(reviewsButton);
        btnPanel.add(refreshButton);

        borrowButton.addActionListener(e -> borrowSelectedBookFromFavorites());
        removeButton.addActionListener(e -> removeSelectedFavorite());
        reviewsButton.addActionListener(e -> showSelectedBookReviewsFromFavorites());
        refreshButton.addActionListener(e -> refreshFavoritesTable());

        panel.add(btnPanel, BorderLayout.SOUTH);
        panel.add(new JScrollPane(favoritesTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createReservationsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        reservationModel = modelOf("預約ID", "書籍ID", "書名", "狀態", "建立時間", "通知時間");
        reservationTable = new JTable(reservationModel);
        reservationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtil.applyDefaultTableStyle(reservationTable);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton cancelButton = new JButton("取消選取預約");
        JButton borrowButton = new JButton("借閱通知中的書");
        JButton refreshButton = new JButton("重新整理");
        btnPanel.add(cancelButton);
        btnPanel.add(borrowButton);
        btnPanel.add(refreshButton);

        cancelButton.addActionListener(e -> cancelSelectedReservation());
        borrowButton.addActionListener(e -> borrowSelectedReservationBook());
        refreshButton.addActionListener(e -> refreshReservationTable());

        panel.add(btnPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(reservationTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRoleRequestTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        rolePolicyModel = modelOf("等級", "借閱上限", "可選期限", "預約上限", "逾期罰款", "收藏功能");
        rolePolicyTable = new JTable(rolePolicyModel);
        UiUtil.applyDefaultTableStyle(rolePolicyTable);

        roleRequestModel = modelOf("申請ID", "目標等級", "理由", "狀態", "建立時間", "處理時間", "管理者", "備註");
        roleRequestTable = new JTable(roleRequestModel);
        roleRequestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtil.applyDefaultTableStyle(roleRequestTable);

        roleTargetBox = new JComboBox<>(RolePolicy.upgradeTargets());
        roleReasonArea = new JTextArea(4, 36);
        roleReasonArea.setLineWrap(true);
        roleReasonArea.setWrapStyleWord(true);

        JPanel requestForm = new JPanel(new BorderLayout(8, 8));
        JPanel topLine = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topLine.add(new JLabel("申請目標等級："));
        topLine.add(roleTargetBox);
        JButton submitButton = new JButton("送出等級申請");
        JButton cancelButton = new JButton("取消選取待審申請");
        JButton refreshButton = new JButton("重新整理");
        topLine.add(submitButton);
        topLine.add(cancelButton);
        topLine.add(refreshButton);
        requestForm.add(topLine, BorderLayout.NORTH);
        requestForm.add(buildTitledPanel("申請理由", new JScrollPane(roleReasonArea)), BorderLayout.CENTER);

        submitButton.addActionListener(e -> submitRoleRequest());
        cancelButton.addActionListener(e -> cancelSelectedRoleRequest());
        refreshButton.addActionListener(e -> refreshRoleRequestTab());

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildTitledPanel("等級規則", new JScrollPane(rolePolicyTable)),
                buildTitledPanel("我的等級申請紀錄", new JScrollPane(roleRequestTable)));
        splitPane.setResizeWeight(0.45);

        panel.add(requestForm, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSearchConditionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("查詢條件"));

        titleField = new JTextField(12);
        authorField = new JTextField(12);
        subjectField = new JTextField(12);
        publisherField = new JTextField(12);
        isbnField = new JTextField(12);

        addSearchField(panel, 0, "題名", titleField);
        addSearchField(panel, 1, "作者", authorField);
        addSearchField(panel, 2, "主題", subjectField);
        addSearchField(panel, 3, "出版者", publisherField);
        addSearchField(panel, 4, "ISBN", isbnField);

        JButton searchButton = new JButton("查詢");
        JButton resetButton = new JButton("清空條件");
        searchButton.addActionListener(e -> refreshSearchResults());
        resetButton.addActionListener(e -> {
            titleField.setText("");
            authorField.setText("");
            subjectField.setText("");
            publisherField.setText("");
            isbnField.setText("");
            refreshSearchResults();
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.BOTH;
        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 6, 6));
        btnPanel.add(searchButton);
        btnPanel.add(resetButton);
        panel.add(btnPanel, gbc);
        return panel;
    }

    private void addSearchField(JPanel panel, int index, String labelText, JTextField field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = (index % 3) * 2;
        gbc.gridy = index / 3;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(6, 6, 6, 6);
        panel.add(new JLabel(labelText + "："), gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = (index % 3) * 2 + 1;
        gbc.gridy = index / 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(6, 6, 6, 6);
        panel.add(field, gbc);
    }

    private JPanel buildTitledPanel(String title, java.awt.Component child) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(child, BorderLayout.CENTER);
        return panel;
    }

    private DefaultTableModel modelOf(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void refreshAllData() {
        reloadCurrentUserOrLogout();
        updateHeaderInfo();
        refreshHomeTables();
        refreshSearchResults();
        refreshCurrentBorrowTable();
        refreshHistoryTable();
        refreshFavoritesTable();
        refreshReservationTable();
        refreshRoleRequestTab();
    }

    private void reloadCurrentUserOrLogout() {
        User latest = libraryService.getUserById(currentUser.getUserId());
        if (latest == null || !latest.isActive()) {
            JOptionPane.showMessageDialog(this, "你的帳號已被停權或刪除，系統將自動登出。", "帳號不可用", JOptionPane.WARNING_MESSAGE);
            logout();
            return;
        }
        this.currentUser = latest;
    }

    private void updateHeaderInfo() {
        int currentBorrowCount = libraryService.getCurrentBorrowCount(currentUser.getUserId());
        int remindCount = libraryService.getDueSoonRecords(currentUser.getUserId(), 3).size();
        int notifiedCount = libraryService.getReservationNotifications(currentUser.getUserId()).size();
        int activeReservations = (int) libraryService.getUserReservations(currentUser.getUserId()).stream()
                .filter(r -> "WAITING".equals(r.getStatus()) || "NOTIFIED".equals(r.getStatus()))
                .count();
        userInfoLabel.setText(String.format("使用者：%s (%s)｜身分：%s｜狀態：%s｜目前借閱：%d / %d｜預約：%d / %d｜提醒：%d｜預約通知：%d｜%s",
                currentUser.getName(), currentUser.getStudentNo(), currentUser.getRoleLevel(), currentUser.getStatus(),
                currentBorrowCount, libraryService.getBorrowLimit(currentUser), activeReservations, libraryService.getReservationLimit(currentUser),
                remindCount, notifiedCount, RolePolicy.description(currentUser.getRoleLevel())));
    }

    private void refreshHomeTables() {
        clearRows(dashboardBorrowModel);
        for (BorrowRecord record : libraryService.getCurrentBorrowedRecords(currentUser.getUserId())) {
            dashboardBorrowModel.addRow(new Object[]{
                    record.getRecordId(),
                    record.getBookTitle(),
                    DateUtil.formatDisplay(record.getBorrowDate()),
                    DateUtil.formatDisplay(record.getDueDate()),
                    record.getBorrowDays(),
                    record.isOverdue() ? "逾期中" : "借閱中",
                    record.getFineAmount() + " 元"
            });
        }

        clearRows(reminderModel);
        for (BorrowRecord record : libraryService.getDueSoonRecords(currentUser.getUserId(), 3)) {
            long daysLeft = DateUtil.daysUntil(record.getDueDate());
            String status = daysLeft < 0 ? "逾期" : (daysLeft == 0 ? "今天到期" : "即將到期");
            String dayText = daysLeft < 0 ? ("逾期 " + Math.abs(daysLeft) + " 天") : ("剩餘 " + daysLeft + " 天");
            reminderModel.addRow(new Object[]{
                    record.getRecordId(),
                    record.getBookTitle(),
                    DateUtil.formatDisplay(record.getDueDate()),
                    status,
                    dayText,
                    record.getFineAmount() + " 元"
            });
        }

        clearRows(notificationModel);
        for (Reservation reservation : libraryService.getReservationNotifications(currentUser.getUserId())) {
            notificationModel.addRow(new Object[]{
                    reservation.getReservationId(),
                    reservation.getBookTitle(),
                    reservation.getStatus(),
                    reservation.getNotifiedAt() == null ? "-" : reservation.getNotifiedAt()
            });
        }
    }

    private void refreshSearchResults() {
        clearRows(searchModel);
        List<Book> books = libraryService.searchBooks(
                titleField == null ? "" : titleField.getText(),
                authorField == null ? "" : authorField.getText(),
                subjectField == null ? "" : subjectField.getText(),
                publisherField == null ? "" : publisherField.getText(),
                isbnField == null ? "" : isbnField.getText()
        );
        for (Book book : books) {
            searchModel.addRow(new Object[]{
                    book.getBookId(),
                    book.getTitle(),
                    book.getAuthors(),
                    book.getSubjects(),
                    book.getPublisher(),
                    book.getIsbn(),
                    book.getAvailabilityText()
            });
        }
        if (!books.isEmpty()) {
            searchTable.setRowSelectionInterval(0, 0);
        } else if (bookDetailArea != null) {
            bookDetailArea.setText("查無符合條件的書籍。");
        }
    }

    private void updateSelectedBookDetail() {
        Integer bookId = getSelectedInt(searchTable, 0);
        if (bookId == null) {
            bookDetailArea.setText("請先從上方表格選取一本書。\n\n可操作：借閱 / 預約 / 查看本書歷史借閱紀錄 / 查看書評 / 加入收藏。\n");
            return;
        }
        Book book = libraryService.getBookById(bookId);
        if (book == null) {
            bookDetailArea.setText("找不到書籍資訊。");
            return;
        }

        List<Review> reviews = libraryService.getReviewsForBook(bookId);
        StringBuilder sb = new StringBuilder();
        sb.append("書籍 ID：").append(book.getBookId()).append("\n");
        sb.append("題名：").append(book.getTitle()).append("\n");
        sb.append("作者：").append(book.getAuthors()).append("\n");
        sb.append("主題：").append(empty(book.getSubjects())).append("\n");
        sb.append("出版者：").append(empty(book.getPublisher())).append("\n");
        sb.append("出版年：").append(empty(book.getPublishYear())).append("\n");
        sb.append("版本：").append(empty(book.getEdition())).append("\n");
        sb.append("格式：").append(empty(book.getFormatDesc())).append("\n");
        sb.append("ISBN：").append(empty(book.getIsbn())).append("\n");
        sb.append("資料來源：").append(empty(book.getSource())).append("\n");
        sb.append("附註：").append(empty(book.getNote())).append("\n");
        sb.append("借閱狀態：").append(book.getAvailabilityText()).append("\n");
        sb.append("書評數量：").append(reviews.size()).append("\n");
        bookDetailArea.setText(sb.toString());
        bookDetailArea.setCaretPosition(0);
    }

    private void refreshCurrentBorrowTable() {
        clearRows(myBorrowModel);
        for (BorrowRecord record : libraryService.getCurrentBorrowedRecords(currentUser.getUserId())) {
            myBorrowModel.addRow(new Object[]{
                    record.getRecordId(),
                    record.getBookTitle(),
                    DateUtil.formatDisplay(record.getBorrowDate()),
                    DateUtil.formatDisplay(record.getDueDate()),
                    record.getBorrowDays(),
                    record.isOverdue() ? "是" : "否",
                    record.getOverdueDays(),
                    record.getFineAmount() + " 元"
            });
        }
    }

    private void refreshHistoryTable() {
        clearRows(historyModel);
        for (BorrowRecord record : libraryService.getBorrowHistoryForUser(currentUser.getUserId())) {
            historyModel.addRow(new Object[]{
                    record.getRecordId(),
                    record.getBookId(),
                    record.getBookTitle(),
                    DateUtil.formatDisplay(record.getBorrowDate()),
                    DateUtil.formatDisplay(record.getDueDate()),
                    DateUtil.formatDisplay(record.getReturnDate()),
                    record.isOverdue() ? "是" : "否",
                    record.getOverdueDays(),
                    record.getFineAmount() + " 元"
            });
        }
    }

    private void refreshFavoritesTable() {
        clearRows(favoritesModel);
        if (!RolePolicy.canUseFavorites(currentUser.getRoleLevel())) {
            favoriteHintLabel.setText("收藏功能需 VIP / GOLD / PLATINUM 等級。你目前是 NORMAL，可到「等級申請」送出升級申請。\n");
            return;
        }
        favoriteHintLabel.setText(RolePolicy.displayName(currentUser.getRoleLevel()) + " 收藏清單：可快速查看、借閱與移除收藏。\n");
        for (Book book : libraryService.getFavorites(currentUser.getUserId())) {
            favoritesModel.addRow(new Object[]{
                    book.getBookId(),
                    book.getTitle(),
                    book.getAuthors(),
                    book.getAvailabilityText(),
                    book.getIsbn()
            });
        }
    }

    private void refreshReservationTable() {
        clearRows(reservationModel);
        for (Reservation reservation : libraryService.getUserReservations(currentUser.getUserId())) {
            reservationModel.addRow(new Object[]{
                    reservation.getReservationId(),
                    reservation.getBookId(),
                    reservation.getBookTitle(),
                    reservation.getStatus(),
                    reservation.getCreatedAt(),
                    reservation.getNotifiedAt() == null ? "-" : reservation.getNotifiedAt()
            });
        }
    }

    private void refreshRoleRequestTab() {
        if (rolePolicyModel == null || roleRequestModel == null) {
            return;
        }
        clearRows(rolePolicyModel);
        for (String level : RolePolicy.supportedLevels()) {
            rolePolicyModel.addRow(new Object[]{
                    RolePolicy.displayName(level),
                    RolePolicy.borrowLimit(level) + " 本",
                    RolePolicy.durationText(level),
                    RolePolicy.reservationLimit(level) + " 本",
                    RolePolicy.finePerDay(level) + " 元/天",
                    RolePolicy.canUseFavorites(level) ? "可用" : "不可用"
            });
        }

        clearRows(roleRequestModel);
        for (RoleChangeRequest request : libraryService.getRoleChangeRequestsForUser(currentUser.getUserId())) {
            roleRequestModel.addRow(new Object[]{
                    request.getRequestId(),
                    request.getTargetLevel(),
                    request.getReason(),
                    request.getStatus(),
                    request.getCreatedAt(),
                    request.getHandledAt() == null ? "-" : request.getHandledAt(),
                    request.getHandledBy() == null ? "-" : request.getHandledBy(),
                    request.getAdminNote() == null ? "" : request.getAdminNote()
            });
        }
    }

    private void borrowSelectedBookFromSearch() {
        Integer bookId = getSelectedInt(searchTable, 0);
        if (bookId == null) {
            showWarn("請先選擇一本書。");
            return;
        }
        openBorrowDialog(bookId);
    }

    private void borrowSelectedBookFromFavorites() {
        Integer bookId = getSelectedInt(favoritesTable, 0);
        if (bookId == null) {
            showWarn("請先從收藏清單選擇一本書。");
            return;
        }
        openBorrowDialog(bookId);
    }

    private void borrowSelectedReservationBook() {
        Integer reservationId = getSelectedInt(reservationTable, 0);
        Integer bookId = getSelectedInt(reservationTable, 1);
        String status = getSelectedString(reservationTable, 3);
        if (reservationId == null || bookId == null) {
            showWarn("請先選擇預約紀錄。");
            return;
        }
        if (!"NOTIFIED".equals(status)) {
            showWarn("只有被通知可借的預約紀錄才能直接借書。");
            return;
        }
        openBorrowDialog(bookId);
    }

    private void openBorrowDialog(int bookId) {
        int[] durations = libraryService.getAllowedDurations(currentUser);
        String[] options = new String[durations.length];
        for (int i = 0; i < durations.length; i++) {
            options[i] = durations[i] + " 天";
        }
        JComboBox<String> comboBox = new JComboBox<>(options);
        int result = JOptionPane.showConfirmDialog(this, comboBox, "選擇借閱天數", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        int borrowDays = durations[comboBox.getSelectedIndex()];
        try {
            libraryService.borrowBook(currentUser, bookId, borrowDays);
            showInfo("借書成功。借閱期限：" + borrowDays + " 天。");
            refreshAllData();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void reserveSelectedBook() {
        Integer bookId = getSelectedInt(searchTable, 0);
        if (bookId == null) {
            showWarn("請先選擇一本書。");
            return;
        }
        try {
            libraryService.reserveBook(currentUser.getUserId(), bookId);
            showInfo("預約成功。待書籍歸還後，系統會通知你。");
            refreshAllData();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void returnSelectedBook() {
        Integer recordId = getSelectedInt(myBorrowTable, 0);
        if (recordId == null) {
            showWarn("請先選擇要歸還的借閱紀錄。");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "確認要歸還選取的書籍嗎？", "確認還書", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            String message = libraryService.returnBook(currentUser.getUserId(), recordId);
            showInfo(message);
            refreshAllData();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void reviewSelectedBook() {
        Integer bookId = getSelectedInt(historyTable, 1);
        String returnDate = getSelectedString(historyTable, 5);
        String title = getSelectedString(historyTable, 2);
        if (bookId == null) {
            showWarn("請先選取一筆歷史借閱紀錄。");
            return;
        }
        if (returnDate == null || "未歸還".equals(returnDate)) {
            showWarn("必須先歸還後才能寫書評。");
            return;
        }
        openReviewDialog(bookId, title);
    }

    private void openReviewDialog(int bookId, String bookTitle) {
        JDialog dialog = new JDialog(this, "撰寫 / 更新書評", true);
        dialog.setSize(460, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JComboBox<Integer> ratingBox = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        JTextArea reviewArea = new JTextArea(8, 28);
        reviewArea.setLineWrap(true);
        reviewArea.setWrapStyleWord(true);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        form.add(new JLabel("書名：" + bookTitle), gbc);

        gbc.gridy = 1;
        form.add(new JLabel("評分："), gbc);

        gbc.gridx = 1;
        form.add(ratingBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        form.add(new JScrollPane(reviewArea), gbc);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("儲存書評");
        JButton cancelButton = new JButton("取消");
        actions.add(saveButton);
        actions.add(cancelButton);

        saveButton.addActionListener(e -> {
            try {
                libraryService.submitReview(currentUser.getUserId(), bookId, (Integer) ratingBox.getSelectedItem(), reviewArea.getText());
                dialog.dispose();
                showInfo("書評已儲存。");
                refreshAllData();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void addSelectedBookToFavorite() {
        Integer bookId = getSelectedInt(searchTable, 0);
        if (bookId == null) {
            showWarn("請先選擇一本書。");
            return;
        }
        try {
            libraryService.addFavorite(currentUser, bookId);
            showInfo("已加入收藏。");
            refreshFavoritesTable();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void removeSelectedFavorite() {
        Integer bookId = getSelectedInt(favoritesTable, 0);
        if (bookId == null) {
            showWarn("請先從收藏清單選取一本書。");
            return;
        }
        try {
            libraryService.removeFavorite(currentUser.getUserId(), bookId);
            showInfo("已移出收藏。");
            refreshFavoritesTable();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void cancelSelectedReservation() {
        Integer reservationId = getSelectedInt(reservationTable, 0);
        if (reservationId == null) {
            showWarn("請先選擇預約紀錄。");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "確定要取消此預約嗎？", "確認取消", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            String message = libraryService.cancelReservation(currentUser.getUserId(), reservationId);
            showInfo(message);
            refreshAllData();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void submitRoleRequest() {
        if (roleTargetBox == null) {
            return;
        }
        String targetLevel = (String) roleTargetBox.getSelectedItem();
        try {
            libraryService.submitRoleChangeRequest(currentUser.getUserId(), targetLevel, roleReasonArea.getText());
            roleReasonArea.setText("");
            showInfo("已送出等級申請，請等待管理者審核。");
            refreshRoleRequestTab();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void cancelSelectedRoleRequest() {
        Integer requestId = getSelectedInt(roleRequestTable, 0);
        String status = getSelectedString(roleRequestTable, 3);
        if (requestId == null) {
            showWarn("請先選擇一筆申請紀錄。");
            return;
        }
        if (!"PENDING".equals(status)) {
            showWarn("只有 PENDING 申請可以取消。");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "確定要取消這筆等級申請嗎？", "確認取消", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            libraryService.cancelRoleChangeRequest(currentUser.getUserId(), requestId);
            showInfo("已取消等級申請。");
            refreshRoleRequestTab();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void showSelectedBookHistory() {
        Integer bookId = getSelectedInt(searchTable, 0);
        if (bookId == null) {
            showWarn("請先選擇一本書。");
            return;
        }
        showBookHistoryDialog(bookId);
    }

    private void showSelectedBookHistoryFromCurrentBorrow() {
        Integer recordId = getSelectedInt(myBorrowTable, 0);
        if (recordId == null) {
            showWarn("請先選擇目前借閱紀錄。");
            return;
        }
        List<BorrowRecord> currentRecords = libraryService.getCurrentBorrowedRecords(currentUser.getUserId());
        for (BorrowRecord record : currentRecords) {
            if (record.getRecordId() == recordId) {
                showBookHistoryDialog(record.getBookId());
                return;
            }
        }
    }

    private void showSelectedBookReviews() {
        Integer bookId = getSelectedInt(searchTable, 0);
        if (bookId == null) {
            showWarn("請先選擇一本書。");
            return;
        }
        showBookReviewDialog(bookId);
    }

    private void showSelectedBookReviewsFromHistory() {
        Integer bookId = getSelectedInt(historyTable, 1);
        if (bookId == null) {
            showWarn("請先選取歷史紀錄。");
            return;
        }
        showBookReviewDialog(bookId);
    }

    private void showSelectedBookReviewsFromFavorites() {
        Integer bookId = getSelectedInt(favoritesTable, 0);
        if (bookId == null) {
            showWarn("請先從收藏清單選擇一本書。");
            return;
        }
        showBookReviewDialog(bookId);
    }

    private void showBookHistoryDialog(int bookId) {
        List<BorrowRecord> records = libraryService.getBorrowHistoryForBook(bookId);
        DefaultTableModel model = modelOf("紀錄ID", "借閱者", "學號", "借出時間", "到期時間", "歸還時間", "是否逾期", "逾期天數", "罰款");
        for (BorrowRecord record : records) {
            model.addRow(new Object[]{
                    record.getRecordId(),
                    record.getBorrowerName(),
                    record.getStudentNo(),
                    DateUtil.formatDisplay(record.getBorrowDate()),
                    DateUtil.formatDisplay(record.getDueDate()),
                    DateUtil.formatDisplay(record.getReturnDate()),
                    record.isOverdue() ? "是" : "否",
                    record.getOverdueDays(),
                    record.getFineAmount() + " 元"
            });
        }
        JTable table = new JTable(model);
        UiUtil.applyDefaultTableStyle(table);
        JOptionPane.showMessageDialog(this, new JScrollPane(table), "本書近期借閱紀錄", JOptionPane.PLAIN_MESSAGE);
    }

    private void showBookReviewDialog(int bookId) {
        List<Review> reviews = libraryService.getReviewsForBook(bookId);
        DefaultTableModel model = modelOf("評分", "使用者", "書名", "內容", "時間");
        for (Review review : reviews) {
            model.addRow(new Object[]{
                    review.getRating(),
                    review.getUserName(),
                    review.getBookTitle(),
                    review.getContent(),
                    review.getCreatedAt()
            });
        }
        JTable table = new JTable(model);
        UiUtil.applyDefaultTableStyle(table);
        JOptionPane.showMessageDialog(this, new JScrollPane(table), "本書所有書評", JOptionPane.PLAIN_MESSAGE);
    }

    private Integer getSelectedInt(JTable table, int columnIndex) {
        if (table == null || table.getSelectedRow() < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
        Object value = table.getModel().getValueAt(modelRow, columnIndex);
        if (value == null) {
            return null;
        }
        return Integer.parseInt(value.toString());
    }

    private String getSelectedString(JTable table, int columnIndex) {
        if (table == null || table.getSelectedRow() < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
        Object value = table.getModel().getValueAt(modelRow, columnIndex);
        return value == null ? null : value.toString();
    }

    private void clearRows(DefaultTableModel model) {
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }
    }

    private String empty(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "完成", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showWarn(String message) {
        JOptionPane.showMessageDialog(this, message, "提醒", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "錯誤", JOptionPane.ERROR_MESSAGE);
    }

    private void logout() {
        new LoginFrame().setVisible(true);
        dispose();
    }
}
