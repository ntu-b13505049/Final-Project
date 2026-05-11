
package librarysystem.ui;

import librarysystem.model.Admin;
import librarysystem.model.Book;
import librarysystem.model.BorrowRecord;
import librarysystem.model.DashboardStats;
import librarysystem.model.Reservation;
import librarysystem.model.Review;
import librarysystem.model.RoleChangeRequest;
import librarysystem.model.RolePolicy;
import librarysystem.model.User;
import librarysystem.service.AdminService;
import librarysystem.service.LibraryService;
import librarysystem.service.WebDashboardServer;
import librarysystem.util.DateUtil;
import librarysystem.util.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class AdminDashboardFrame extends JFrame {
    private final Admin admin;
    private final AdminService adminService = new AdminService();
    private final LibraryService libraryService = new LibraryService();
    private final WebDashboardServer webDashboardServer = new WebDashboardServer();

    private JLabel adminInfoLabel;
    private JLabel totalBooksLabel;
    private JLabel activeUsersLabel;
    private JLabel currentBorrowsLabel;
    private JLabel overdueBorrowsLabel;
    private JLabel totalReviewsLabel;
    private JLabel waitingReservationsLabel;
    private JLabel pendingRoleRequestsLabel;

    private JTable subjectStatsTable;
    private JTable borrowRecordsTable;
    private JTable usersTable;
    private JTable booksTable;
    private JTable reviewsTable;
    private JTable reservationsTable;
    private JTable roleRequestsTable;

    private DefaultTableModel subjectStatsModel;
    private DefaultTableModel borrowRecordsModel;
    private DefaultTableModel usersModel;
    private DefaultTableModel booksModel;
    private DefaultTableModel reviewsModel;
    private DefaultTableModel reservationsModel;
    private DefaultTableModel roleRequestsModel;

    private JTextField recordStudentNoField;
    private JTextField recordBorrowerNameField;
    private JTextField recordBookTitleField;
    private JComboBox<String> recordStatusBox;

    private JTextField userSearchKeywordField;
    private JComboBox<String> userRoleFilterBox;
    private JComboBox<String> userStatusFilterBox;

    private JTextField bookSearchKeywordField;
    private JComboBox<String> bookStatusFilterBox;

    private JTextField reviewSearchKeywordField;
    private JComboBox<String> reviewRatingFilterBox;
    private JTextField reservationSearchKeywordField;
    private JComboBox<String> reservationStatusFilterBox;

    private JTextField roleRequestSearchKeywordField;
    private JComboBox<String> roleRequestStatusFilterBox;

    private JTextField bookTitleField;
    private JTextField bookAuthorsField;
    private JTextField bookSubjectsField;
    private JTextField bookPublisherField;
    private JTextField bookYearField;
    private JTextField bookEditionField;
    private JTextField bookFormatField;
    private JTextField bookSourceField;
    private JTextArea bookNoteArea;
    private JTextField bookIsbnField;
    private JLabel bookFormModeLabel;
    private Integer editingBookId;
    private JComboBox<String> roleLevelBox;
    private SubjectStatsChartPanel subjectStatsChartPanel;

    public AdminDashboardFrame(Admin admin) {
        this.admin = admin;
        setTitle("圖書館系統 - 管理者介面");
        setSize(1400, 860);
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

        JLabel titleLabel = new JLabel("管理者後台");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        adminInfoLabel = new JLabel(" ");

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(titleLabel);
        left.add(adminInfoLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("重新整理後台資料");
        JButton webButton = new JButton("啟動 Web 報表");
        JButton logoutButton = new JButton("登出");
        refreshButton.addActionListener(e -> refreshAllData());
        webButton.addActionListener(e -> startWebDashboard());
        logoutButton.addActionListener(e -> logout());
        right.add(refreshButton);
        right.add(webButton);
        right.add(logoutButton);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private JTabbedPane createTabs() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        tabbedPane.addTab("總覽", createOverviewTab());
        tabbedPane.addTab("借還紀錄", createBorrowRecordTab());
        tabbedPane.addTab("使用者管理", createUsersTab());
        tabbedPane.addTab("書籍管理", createBooksTab());
        tabbedPane.addTab("書評與預約", createReviewsAndReservationsTab());
        tabbedPane.addTab("等級申請", createRoleRequestsTab());
        return tabbedPane;
    }

    private JPanel createOverviewTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel cards = new JPanel(new GridLayout(2, 4, 10, 10));
        totalBooksLabel = statCard(cards, "總書數");
        activeUsersLabel = statCard(cards, "啟用使用者");
        currentBorrowsLabel = statCard(cards, "目前借閱中");
        overdueBorrowsLabel = statCard(cards, "逾期借閱");
        totalReviewsLabel = statCard(cards, "書評總數");
        waitingReservationsLabel = statCard(cards, "待處理預約");
        pendingRoleRequestsLabel = statCard(cards, "待審核等級申請");

        subjectStatsModel = modelOf("主題", "借閱次數");
        subjectStatsTable = new JTable(subjectStatsModel);
        UiUtil.applyDefaultTableStyle(subjectStatsTable);
        subjectStatsChartPanel = new SubjectStatsChartPanel();

        JPanel chartActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton barButton = new JButton("柱狀圖");
        JButton pieButton = new JButton("圓餅圖");
        barButton.addActionListener(e -> { subjectStatsChartPanel.setMode("BAR"); subjectStatsChartPanel.repaint(); });
        pieButton.addActionListener(e -> { subjectStatsChartPanel.setMode("PIE"); subjectStatsChartPanel.repaint(); });
        chartActions.add(barButton);
        chartActions.add(pieButton);
        JPanel chartPanel = new JPanel(new BorderLayout());
        chartPanel.add(chartActions, BorderLayout.NORTH);
        chartPanel.add(subjectStatsChartPanel, BorderLayout.CENTER);

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildTitledPanel("主題借閱熱度 Top 10", new JScrollPane(subjectStatsTable)),
                buildTitledPanel("主題統計視覺化", chartPanel));
        centerSplit.setResizeWeight(0.45);

        panel.add(cards, BorderLayout.NORTH);
        panel.add(centerSplit, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBorrowRecordTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBorder(BorderFactory.createTitledBorder("查詢條件"));
        recordStudentNoField = new JTextField(12);
        recordBorrowerNameField = new JTextField(12);
        recordBookTitleField = new JTextField(14);
        recordStatusBox = new JComboBox<>(new String[]{"全部", "借閱中", "已歸還", "逾期", "未逾期"});
        addFilterField(filterPanel, 0, "學號", recordStudentNoField);
        addFilterField(filterPanel, 1, "姓名", recordBorrowerNameField);
        addFilterField(filterPanel, 2, "書名 / 書籍ID", recordBookTitleField);
        addFilterCombo(filterPanel, 3, "狀態", recordStatusBox);
        JButton searchButton = new JButton("查詢紀錄");
        JButton resetButton = new JButton("清空條件");
        searchButton.addActionListener(e -> refreshBorrowRecordTable());
        resetButton.addActionListener(e -> {
            recordStudentNoField.setText("");
            recordBorrowerNameField.setText("");
            recordBookTitleField.setText("");
            recordStatusBox.setSelectedIndex(0);
            refreshBorrowRecordTable();
        });
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 8;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.insets = new Insets(6, 6, 6, 6);
        JPanel action = new JPanel(new GridLayout(2, 1, 6, 6));
        action.add(searchButton);
        action.add(resetButton);
        filterPanel.add(action, gbc);

        borrowRecordsModel = modelOf("紀錄ID", "學號", "借閱者", "等級", "書籍ID", "書名", "借出時間", "到期時間", "歸還時間", "逾期", "逾期天數", "罰款");
        borrowRecordsTable = new JTable(borrowRecordsModel);
        borrowRecordsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtil.applyDefaultTableStyle(borrowRecordsTable);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton viewBookHistoryButton = new JButton("查看選取書籍完整紀錄");
        JButton refreshButton = new JButton("重新整理");
        buttons.add(viewBookHistoryButton);
        buttons.add(refreshButton);
        viewBookHistoryButton.addActionListener(e -> showSelectedBookHistory());
        refreshButton.addActionListener(e -> refreshBorrowRecordTable());

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(borrowRecordsTable), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createUsersTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBorder(BorderFactory.createTitledBorder("使用者搜尋"));
        userSearchKeywordField = new JTextField(18);
        userRoleFilterBox = new JComboBox<>(withAll(RolePolicy.allLevelsArray()));
        userStatusFilterBox = new JComboBox<>(new String[]{"全部", "ACTIVE", "SUSPENDED"});
        addFilterField(filterPanel, 0, "學號 / 姓名 / ID", userSearchKeywordField);
        addFilterCombo(filterPanel, 1, "權限", userRoleFilterBox);
        addFilterCombo(filterPanel, 2, "狀態", userStatusFilterBox);
        JButton userSearchButton = new JButton("搜尋使用者");
        JButton userResetButton = new JButton("清空條件");
        userSearchButton.addActionListener(e -> refreshUsersTable());
        userResetButton.addActionListener(e -> {
            userSearchKeywordField.setText("");
            userRoleFilterBox.setSelectedIndex(0);
            userStatusFilterBox.setSelectedIndex(0);
            refreshUsersTable();
        });
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 0;
        gbc.insets = new Insets(6, 6, 6, 6);
        JPanel filterActions = new JPanel(new GridLayout(2, 1, 6, 6));
        filterActions.add(userSearchButton);
        filterActions.add(userResetButton);
        filterPanel.add(filterActions, gbc);

        usersModel = modelOf("使用者ID", "學號", "姓名", "權限", "狀態", "建立時間");
        usersTable = new JTable(usersModel);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtil.applyDefaultTableStyle(usersTable);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton suspendButton = new JButton("停權選取使用者");
        JButton restoreButton = new JButton("復權選取使用者");
        roleLevelBox = new JComboBox<>(RolePolicy.allLevelsArray());
        JButton updateRoleButton = new JButton("設定選取使用者等級");
        JButton refreshButton = new JButton("重新整理");
        buttons.add(suspendButton);
        buttons.add(restoreButton);
        buttons.add(new JLabel("新等級："));
        buttons.add(roleLevelBox);
        buttons.add(updateRoleButton);
        buttons.add(refreshButton);

        suspendButton.addActionListener(e -> changeSelectedUserStatus("SUSPENDED"));
        restoreButton.addActionListener(e -> changeSelectedUserStatus("ACTIVE"));
        updateRoleButton.addActionListener(e -> changeSelectedUserRoleLevel());
        refreshButton.addActionListener(e -> refreshUsersTable());

        JPanel north = new JPanel(new BorderLayout(6, 6));
        north.add(filterPanel, BorderLayout.NORTH);
        north.add(buttons, BorderLayout.SOUTH);
        panel.add(north, BorderLayout.NORTH);
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBooksTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBorder(BorderFactory.createTitledBorder("書籍搜尋"));
        bookSearchKeywordField = new JTextField(24);
        bookStatusFilterBox = new JComboBox<>(new String[]{"全部", "上架中", "已下架", "已借出", "可借"});
        addFilterField(searchPanel, 0, "關鍵字 / ISBN / 書籍ID", bookSearchKeywordField);
        addFilterCombo(searchPanel, 1, "狀態", bookStatusFilterBox);
        JButton bookSearchButton = new JButton("搜尋書籍");
        JButton bookResetButton = new JButton("清空條件");
        bookSearchButton.addActionListener(e -> refreshBooksTable());
        bookResetButton.addActionListener(e -> {
            bookSearchKeywordField.setText("");
            bookStatusFilterBox.setSelectedIndex(0);
            refreshBooksTable();
        });
        GridBagConstraints searchGbc = new GridBagConstraints();
        searchGbc.gridx = 4;
        searchGbc.gridy = 0;
        searchGbc.insets = new Insets(6, 6, 6, 6);
        JPanel searchActions = new JPanel(new GridLayout(2, 1, 6, 6));
        searchActions.add(bookSearchButton);
        searchActions.add(bookResetButton);
        searchPanel.add(searchActions, searchGbc);

        JPanel topPanel = new JPanel(new BorderLayout(6, 6));
        topPanel.add(searchPanel, BorderLayout.NORTH);
        topPanel.add(createBookFormPanel(), BorderLayout.CENTER);
        panel.add(topPanel, BorderLayout.NORTH);

        booksModel = modelOf("書籍ID", "題名", "作者", "主題", "出版者", "ISBN", "狀態");
        booksTable = new JTable(booksModel);
        booksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtil.applyDefaultTableStyle(booksTable);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBookButton = new JButton("新增書籍");
        JButton loadBookButton = new JButton("載入選取書籍到表單");
        JButton updateBookButton = new JButton("修改書籍");
        JButton clearFormButton = new JButton("清空表單");
        JButton toggleActiveButton = new JButton("切換上架 / 下架");
        JButton historyButton = new JButton("查看本書借閱紀錄");
        JButton refreshButton = new JButton("重新整理");
        buttons.add(addBookButton);
        buttons.add(loadBookButton);
        buttons.add(updateBookButton);
        buttons.add(clearFormButton);
        buttons.add(toggleActiveButton);
        buttons.add(historyButton);
        buttons.add(refreshButton);

        addBookButton.addActionListener(e -> addBook());
        loadBookButton.addActionListener(e -> loadSelectedBookIntoForm());
        updateBookButton.addActionListener(e -> updateSelectedBook());
        clearFormButton.addActionListener(e -> clearBookForm());
        toggleActiveButton.addActionListener(e -> toggleSelectedBookActive());
        historyButton.addActionListener(e -> showSelectedBookHistoryFromBookTab());
        refreshButton.addActionListener(e -> refreshBooksTable());

        panel.add(new JScrollPane(booksTable), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createReviewsAndReservationsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        reviewsModel = modelOf("書評ID", "使用者", "書名", "評分", "內容", "建立時間");
        reviewsTable = new JTable(reviewsModel);
        UiUtil.applyDefaultTableStyle(reviewsTable);

        JPanel reviewPanel = new JPanel(new BorderLayout(6, 6));
        JPanel reviewFilterPanel = new JPanel(new GridBagLayout());
        reviewFilterPanel.setBorder(BorderFactory.createTitledBorder("書評搜尋"));
        reviewSearchKeywordField = new JTextField(24);
        reviewRatingFilterBox = new JComboBox<>(new String[]{"全部", "5", "4", "3", "2", "1"});
        addFilterField(reviewFilterPanel, 0, "使用者 / 書名 / 內容", reviewSearchKeywordField);
        addFilterCombo(reviewFilterPanel, 1, "評分", reviewRatingFilterBox);
        JButton reviewSearchButton = new JButton("搜尋書評");
        JButton reviewResetButton = new JButton("清空條件");
        reviewSearchButton.addActionListener(e -> refreshReviewsTable());
        reviewResetButton.addActionListener(e -> {
            reviewSearchKeywordField.setText("");
            reviewRatingFilterBox.setSelectedIndex(0);
            refreshReviewsTable();
        });
        GridBagConstraints reviewGbc = new GridBagConstraints();
        reviewGbc.gridx = 4;
        reviewGbc.gridy = 0;
        reviewGbc.insets = new Insets(6, 6, 6, 6);
        JPanel reviewActions = new JPanel(new GridLayout(2, 1, 6, 6));
        reviewActions.add(reviewSearchButton);
        reviewActions.add(reviewResetButton);
        reviewFilterPanel.add(reviewActions, reviewGbc);
        reviewPanel.add(reviewFilterPanel, BorderLayout.NORTH);
        reviewPanel.add(new JScrollPane(reviewsTable), BorderLayout.CENTER);

        reservationsModel = modelOf("預約ID", "使用者", "書名", "狀態", "建立時間", "通知時間");
        reservationsTable = new JTable(reservationsModel);
        UiUtil.applyDefaultTableStyle(reservationsTable);

        JPanel reservationPanel = new JPanel(new BorderLayout(6, 6));
        JPanel reservationFilterPanel = new JPanel(new GridBagLayout());
        reservationFilterPanel.setBorder(BorderFactory.createTitledBorder("預約搜尋"));
        reservationSearchKeywordField = new JTextField(24);
        reservationStatusFilterBox = new JComboBox<>(new String[]{"全部", "WAITING", "NOTIFIED", "FULFILLED", "CANCELLED"});
        addFilterField(reservationFilterPanel, 0, "使用者 / 書名 / 預約ID", reservationSearchKeywordField);
        addFilterCombo(reservationFilterPanel, 1, "狀態", reservationStatusFilterBox);
        JButton reservationSearchButton = new JButton("搜尋預約");
        JButton reservationResetButton = new JButton("清空條件");
        reservationSearchButton.addActionListener(e -> refreshReservationsTable());
        reservationResetButton.addActionListener(e -> {
            reservationSearchKeywordField.setText("");
            reservationStatusFilterBox.setSelectedIndex(0);
            refreshReservationsTable();
        });
        GridBagConstraints reservationGbc = new GridBagConstraints();
        reservationGbc.gridx = 4;
        reservationGbc.gridy = 0;
        reservationGbc.insets = new Insets(6, 6, 6, 6);
        JPanel reservationActions = new JPanel(new GridLayout(2, 1, 6, 6));
        reservationActions.add(reservationSearchButton);
        reservationActions.add(reservationResetButton);
        reservationFilterPanel.add(reservationActions, reservationGbc);
        reservationPanel.add(reservationFilterPanel, BorderLayout.NORTH);
        reservationPanel.add(new JScrollPane(reservationsTable), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildTitledPanel("所有書評", reviewPanel),
                buildTitledPanel("所有預約紀錄", reservationPanel));
        splitPane.setResizeWeight(0.5);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRoleRequestsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBorder(BorderFactory.createTitledBorder("等級申請搜尋"));
        roleRequestSearchKeywordField = new JTextField(24);
        roleRequestStatusFilterBox = new JComboBox<>(new String[]{"全部", "PENDING", "APPROVED", "REJECTED"});
        addFilterField(filterPanel, 0, "學號 / 姓名 / 理由", roleRequestSearchKeywordField);
        addFilterCombo(filterPanel, 1, "狀態", roleRequestStatusFilterBox);
        JButton searchButton = new JButton("搜尋申請");
        JButton resetButton = new JButton("清空條件");
        searchButton.addActionListener(e -> refreshRoleRequestsTable());
        resetButton.addActionListener(e -> {
            roleRequestSearchKeywordField.setText("");
            roleRequestStatusFilterBox.setSelectedIndex(0);
            refreshRoleRequestsTable();
        });
        GridBagConstraints filterGbc = new GridBagConstraints();
        filterGbc.gridx = 4;
        filterGbc.gridy = 0;
        filterGbc.insets = new Insets(6, 6, 6, 6);
        JPanel filterActions = new JPanel(new GridLayout(2, 1, 6, 6));
        filterActions.add(searchButton);
        filterActions.add(resetButton);
        filterPanel.add(filterActions, filterGbc);

        roleRequestsModel = modelOf("申請ID", "使用者ID", "學號", "姓名", "目前等級", "申請等級", "理由", "狀態", "建立時間", "處理時間", "管理者", "備註");
        roleRequestsTable = new JTable(roleRequestsModel);
        roleRequestsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtil.applyDefaultTableStyle(roleRequestsTable);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton approveButton = new JButton("核准選取申請");
        JButton rejectButton = new JButton("退回選取申請");
        JButton refreshButton = new JButton("重新整理");
        buttons.add(approveButton);
        buttons.add(rejectButton);
        buttons.add(refreshButton);

        approveButton.addActionListener(e -> handleSelectedRoleRequest(true));
        rejectButton.addActionListener(e -> handleSelectedRoleRequest(false));
        refreshButton.addActionListener(e -> refreshRoleRequestsTable());

        JPanel north = new JPanel(new BorderLayout(6, 6));
        north.add(filterPanel, BorderLayout.NORTH);
        north.add(buttons, BorderLayout.SOUTH);
        panel.add(north, BorderLayout.NORTH);
        panel.add(new JScrollPane(roleRequestsTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBookFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("書籍資料（新增 / 修改共用）"));

        bookFormModeLabel = new JLabel("目前模式：新增書籍");
        bookTitleField = new JTextField(12);
        bookAuthorsField = new JTextField(12);
        bookSubjectsField = new JTextField(12);
        bookPublisherField = new JTextField(12);
        bookYearField = new JTextField(12);
        bookEditionField = new JTextField(12);
        bookFormatField = new JTextField(12);
        bookSourceField = new JTextField(12);
        bookIsbnField = new JTextField(12);
        bookNoteArea = new JTextArea(3, 20);
        bookNoteArea.setLineWrap(true);
        bookNoteArea.setWrapStyleWord(true);

        GridBagConstraints modeGbc = new GridBagConstraints();
        modeGbc.gridx = 0;
        modeGbc.gridy = 0;
        modeGbc.gridwidth = 6;
        modeGbc.anchor = GridBagConstraints.WEST;
        modeGbc.insets = new Insets(6, 6, 2, 6);
        panel.add(bookFormModeLabel, modeGbc);

        addBookField(panel, 0, "題名", bookTitleField);
        addBookField(panel, 1, "作者", bookAuthorsField);
        addBookField(panel, 2, "主題", bookSubjectsField);
        addBookField(panel, 3, "出版者", bookPublisherField);
        addBookField(panel, 4, "出版年", bookYearField);
        addBookField(panel, 5, "版本", bookEditionField);
        addBookField(panel, 6, "格式", bookFormatField);
        addBookField(panel, 7, "資料來源", bookSourceField);
        addBookField(panel, 8, "ISBN", bookIsbnField);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("附註："), gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(6, 6, 6, 6);
        panel.add(new JScrollPane(bookNoteArea), gbc);

        return panel;
    }

    private void addFilterField(JPanel panel, int index, String labelText, JTextField field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = index * 2;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(6, 6, 6, 6);
        panel.add(new JLabel(labelText + "："), gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = index * 2 + 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(6, 6, 6, 6);
        panel.add(field, gbc);
    }

    private void addFilterCombo(JPanel panel, int index, String labelText, JComboBox<String> comboBox) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = index * 2;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(6, 6, 6, 6);
        panel.add(new JLabel(labelText + "："), gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = index * 2 + 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(6, 6, 6, 6);
        panel.add(comboBox, gbc);
    }

    private void addBookField(JPanel panel, int index, String labelText, JTextField field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = (index % 3) * 2;
        gbc.gridy = index / 3 + 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(6, 6, 6, 6);
        panel.add(new JLabel(labelText + "："), gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = (index % 3) * 2 + 1;
        gbc.gridy = index / 3 + 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(6, 6, 6, 6);
        panel.add(field, gbc);
    }

    private JLabel statCard(JPanel container, String title) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(card.getBackground().darker()),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        JLabel valueLabel = new JLabel("0", SwingConstants.CENTER);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        container.add(card);
        return valueLabel;
    }

    private JPanel buildTitledPanel(String title, java.awt.Component child) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(child, BorderLayout.CENTER);
        return panel;
    }

    private String[] withAll(String[] values) {
        String[] result = new String[values.length + 1];
        result[0] = "全部";
        System.arraycopy(values, 0, result, 1, values.length);
        return result;
    }

    private String textOf(JTextField field) {
        return field == null ? "" : field.getText();
    }

    private String statusFilterValue(JComboBox<String> comboBox) {
        if (comboBox == null || comboBox.getSelectedItem() == null) {
            return "";
        }
        String selected = comboBox.getSelectedItem().toString();
        return switch (selected) {
            case "借閱中" -> "CURRENT";
            case "已歸還" -> "RETURNED";
            case "逾期" -> "OVERDUE";
            case "未逾期" -> "NOT_OVERDUE";
            case "上架中" -> "ACTIVE";
            case "已下架" -> "INACTIVE";
            case "已借出" -> "BORROWED";
            case "可借" -> "AVAILABLE";
            case "全部" -> "";
            default -> selected;
        };
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
        adminInfoLabel.setText("已登入管理者：" + admin.getUsername() + " ｜ 可管理使用者、書籍、借還紀錄、書評、預約、等級申請與 Web 報表");
        refreshOverview();
        refreshBorrowRecordTable();
        refreshUsersTable();
        refreshBooksTable();
        refreshReviewsTable();
        refreshReservationsTable();
        refreshRoleRequestsTable();
    }

    private void refreshOverview() {
        DashboardStats stats = adminService.getDashboardStats();
        totalBooksLabel.setText(String.valueOf(stats.getTotalBooks()));
        activeUsersLabel.setText(String.valueOf(stats.getActiveUsers()));
        currentBorrowsLabel.setText(String.valueOf(stats.getCurrentBorrows()));
        overdueBorrowsLabel.setText(String.valueOf(stats.getOverdueBorrows()));
        totalReviewsLabel.setText(String.valueOf(stats.getTotalReviews()));
        waitingReservationsLabel.setText(String.valueOf(stats.getWaitingReservations()));
        pendingRoleRequestsLabel.setText(String.valueOf(stats.getPendingRoleRequests()));

        clearRows(subjectStatsModel);
        Map<String, Integer> subjectStats = adminService.getSubjectBorrowStats();
        for (Map.Entry<String, Integer> entry : subjectStats.entrySet()) {
            subjectStatsModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
        if (subjectStatsChartPanel != null) {
            subjectStatsChartPanel.setData(subjectStats);
        }
    }

    private void refreshBorrowRecordTable() {
        clearRows(borrowRecordsModel);
        List<BorrowRecord> records = adminService.getBorrowRecords(
                textOf(recordStudentNoField),
                textOf(recordBorrowerNameField),
                textOf(recordBookTitleField),
                statusFilterValue(recordStatusBox)
        );
        for (BorrowRecord record : records) {
            borrowRecordsModel.addRow(new Object[]{
                    record.getRecordId(),
                    record.getStudentNo(),
                    record.getBorrowerName(),
                    record.getUserRoleLevel(),
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

    private void refreshUsersTable() {
        clearRows(usersModel);
        for (User user : adminService.searchUsers(
                textOf(userSearchKeywordField),
                statusFilterValue(userRoleFilterBox),
                statusFilterValue(userStatusFilterBox)
        )) {
            usersModel.addRow(new Object[]{
                    user.getUserId(),
                    user.getStudentNo(),
                    user.getName(),
                    user.getRoleLevel(),
                    user.getStatus(),
                    user.getCreatedAt()
            });
        }
    }

    private void refreshBooksTable() {
        clearRows(booksModel);
        for (Book book : adminService.searchBooks(
                textOf(bookSearchKeywordField),
                statusFilterValue(bookStatusFilterBox)
        )) {
            String statusText = !book.isActive() ? "已下架" : (book.isBorrowed() ? "上架中 / 已借出" : "上架中 / 可借");
            booksModel.addRow(new Object[]{
                    book.getBookId(),
                    book.getTitle(),
                    book.getAuthors(),
                    book.getSubjects(),
                    book.getPublisher(),
                    book.getIsbn(),
                    statusText
            });
        }
    }

    private void refreshReviewsTable() {
        clearRows(reviewsModel);
        for (Review review : adminService.searchReviews(
                textOf(reviewSearchKeywordField),
                statusFilterValue(reviewRatingFilterBox)
        )) {
            reviewsModel.addRow(new Object[]{
                    review.getReviewId(),
                    review.getUserName(),
                    review.getBookTitle(),
                    review.getRating(),
                    review.getContent(),
                    review.getCreatedAt()
            });
        }
    }

    private void refreshReservationsTable() {
        clearRows(reservationsModel);
        for (Reservation reservation : adminService.searchReservations(
                textOf(reservationSearchKeywordField),
                statusFilterValue(reservationStatusFilterBox)
        )) {
            reservationsModel.addRow(new Object[]{
                    reservation.getReservationId(),
                    reservation.getUserName(),
                    reservation.getBookTitle(),
                    reservation.getStatus(),
                    reservation.getCreatedAt(),
                    reservation.getNotifiedAt() == null ? "-" : reservation.getNotifiedAt()
            });
        }
    }

    private void refreshRoleRequestsTable() {
        if (roleRequestsModel == null) {
            return;
        }
        clearRows(roleRequestsModel);
        for (RoleChangeRequest request : adminService.getRoleChangeRequests(
                statusFilterValue(roleRequestStatusFilterBox),
                textOf(roleRequestSearchKeywordField)
        )) {
            roleRequestsModel.addRow(new Object[]{
                    request.getRequestId(),
                    request.getUserId(),
                    request.getStudentNo(),
                    request.getUserName(),
                    request.getCurrentLevel(),
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

    private void changeSelectedUserStatus(String status) {
        Integer userId = getSelectedInt(usersTable, 0);
        if (userId == null) {
            showWarn("請先選擇一位使用者。");
            return;
        }
        String currentStatus = getSelectedString(usersTable, 4);
        if (status.equals(currentStatus)) {
            showWarn("該使用者目前已是此狀態。");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                ("ACTIVE".equals(status) ? "確定要復權此使用者嗎？" : "確定要停權此使用者嗎？"),
                "確認操作", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            adminService.updateUserStatus(userId, status);
            showInfo("使用者狀態已更新。");
            refreshUsersTable();
            refreshOverview();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void changeSelectedUserRoleLevel() {
        Integer userId = getSelectedInt(usersTable, 0);
        if (userId == null) {
            showWarn("請先選擇一位使用者。");
            return;
        }
        String targetLevel = (String) roleLevelBox.getSelectedItem();
        int confirm = JOptionPane.showConfirmDialog(this,
                "確定要將選取使用者等級改為 " + targetLevel + " 嗎？",
                "確認變更等級", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            adminService.updateUserRoleLevel(userId, targetLevel);
            showInfo("使用者等級已更新。");
            refreshUsersTable();
            refreshOverview();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void handleSelectedRoleRequest(boolean approve) {
        Integer requestId = getSelectedInt(roleRequestsTable, 0);
        String status = getSelectedString(roleRequestsTable, 7);
        if (requestId == null) {
            showWarn("請先選擇一筆等級申請。");
            return;
        }
        if (!"PENDING".equals(status)) {
            showWarn("只有 PENDING 申請可以審核。");
            return;
        }
        String note = JOptionPane.showInputDialog(this,
                approve ? "請輸入核准備註（可留空）：" : "請輸入退回原因（可留空）：",
                approve ? "核准申請" : "退回申請",
                JOptionPane.PLAIN_MESSAGE);
        if (note == null) {
            return;
        }
        try {
            adminService.handleRoleChangeRequest(requestId, approve, admin.getUsername(), note);
            showInfo(approve ? "已核准等級申請。" : "已退回等級申請。");
            refreshAllData();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void startWebDashboard() {
        try {
            String url = webDashboardServer.start();
            showInfo("Web 報表已啟動：" + url + "\n瀏覽器若未自動開啟，請手動貼上此網址。");
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void addBook() {
        try {
            Book book = buildBookFromForm();
            adminService.addBook(book);
            showInfo("書籍新增成功。");
            clearBookForm();
            refreshBooksTable();
            refreshOverview();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void loadSelectedBookIntoForm() {
        Integer bookId = getSelectedInt(booksTable, 0);
        if (bookId == null) {
            showWarn("請先選擇一本書，再載入表單。");
            return;
        }
        Book book = libraryService.getBookById(bookId);
        if (book == null) {
            showWarn("找不到選取的書籍。");
            return;
        }
        editingBookId = book.getBookId();
        bookTitleField.setText(nullToEmpty(book.getTitle()));
        bookAuthorsField.setText(nullToEmpty(book.getAuthors()));
        bookSubjectsField.setText(nullToEmpty(book.getSubjects()));
        bookPublisherField.setText(nullToEmpty(book.getPublisher()));
        bookYearField.setText(nullToEmpty(book.getPublishYear()));
        bookEditionField.setText(nullToEmpty(book.getEdition()));
        bookFormatField.setText(nullToEmpty(book.getFormatDesc()));
        bookSourceField.setText(nullToEmpty(book.getSource()));
        bookIsbnField.setText(nullToEmpty(book.getIsbn()));
        bookNoteArea.setText(nullToEmpty(book.getNote()));
        bookFormModeLabel.setText("目前模式：修改書籍 ID " + editingBookId);
    }

    private void updateSelectedBook() {
        if (editingBookId == null) {
            showWarn("請先選擇一本書，並按「載入選取書籍到表單」後再修改。");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "確定要修改書籍 ID " + editingBookId + " 的資料嗎？",
                "確認修改書籍", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            Book book = buildBookFromForm();
            book.setBookId(editingBookId);
            adminService.updateBook(book);
            showInfo("書籍資料修改成功。");
            clearBookForm();
            refreshBooksTable();
            refreshOverview();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private Book buildBookFromForm() {
        Book book = new Book();
        book.setTitle(bookTitleField.getText());
        book.setAuthors(bookAuthorsField.getText());
        book.setSubjects(bookSubjectsField.getText());
        book.setPublisher(bookPublisherField.getText());
        book.setPublishYear(bookYearField.getText());
        book.setEdition(bookEditionField.getText());
        book.setFormatDesc(bookFormatField.getText());
        book.setSource(bookSourceField.getText());
        book.setNote(bookNoteArea.getText());
        book.setIsbn(bookIsbnField.getText());
        return book;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void clearBookForm() {
        bookTitleField.setText("");
        bookAuthorsField.setText("");
        bookSubjectsField.setText("");
        bookPublisherField.setText("");
        bookYearField.setText("");
        bookEditionField.setText("");
        bookFormatField.setText("");
        bookSourceField.setText("");
        bookIsbnField.setText("");
        bookNoteArea.setText("");
        editingBookId = null;
        if (bookFormModeLabel != null) {
            bookFormModeLabel.setText("目前模式：新增書籍");
        }
    }

    private void toggleSelectedBookActive() {
        Integer bookId = getSelectedInt(booksTable, 0);
        String statusText = getSelectedString(booksTable, 6);
        if (bookId == null || statusText == null) {
            showWarn("請先選擇一本書。");
            return;
        }
        boolean currentlyActive = statusText.startsWith("上架中");
        try {
            adminService.toggleBookActive(bookId, !currentlyActive);
            showInfo(currentlyActive ? "已下架該書籍。" : "已重新上架該書籍。");
            refreshBooksTable();
            refreshOverview();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void showSelectedBookHistory() {
        Integer bookId = getSelectedInt(borrowRecordsTable, 4);
        if (bookId == null) {
            showWarn("請先選擇一筆借閱紀錄。");
            return;
        }
        showBookHistoryDialog(bookId);
    }

    private void showSelectedBookHistoryFromBookTab() {
        Integer bookId = getSelectedInt(booksTable, 0);
        if (bookId == null) {
            showWarn("請先選擇一本書。");
            return;
        }
        showBookHistoryDialog(bookId);
    }

    private void showBookHistoryDialog(int bookId) {
        List<BorrowRecord> records = libraryService.getBorrowHistoryForBook(bookId);
        DefaultTableModel model = modelOf("紀錄ID", "借閱者", "學號", "等級", "借出時間", "到期時間", "歸還時間", "是否逾期", "逾期天數", "罰款");
        for (BorrowRecord record : records) {
            model.addRow(new Object[]{
                    record.getRecordId(),
                    record.getBorrowerName(),
                    record.getStudentNo(),
                    record.getUserRoleLevel(),
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
        JOptionPane.showMessageDialog(this, new JScrollPane(table), "本書完整借閱紀錄", JOptionPane.PLAIN_MESSAGE);
    }

    private Integer getSelectedInt(JTable table, int columnIndex) {
        if (table == null || table.getSelectedRow() < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
        Object value = table.getModel().getValueAt(modelRow, columnIndex);
        return value == null ? null : Integer.parseInt(value.toString());
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

    private static class SubjectStatsChartPanel extends JPanel {
        private Map<String, Integer> data = Map.of();
        private String mode = "BAR";

        SubjectStatsChartPanel() {
            setPreferredSize(new java.awt.Dimension(460, 360));
            setBackground(Color.WHITE);
        }

        void setData(Map<String, Integer> data) {
            this.data = data == null ? Map.of() : data;
            repaint();
        }

        void setMode(String mode) {
            this.mode = mode == null ? "BAR" : mode;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            try {
                if (data.isEmpty()) {
                    g2.drawString("尚無主題借閱資料", 24, 36);
                    return;
                }
                if ("PIE".equals(mode)) {
                    paintPie(g2);
                } else {
                    paintBar(g2);
                }
            } finally {
                g2.dispose();
            }
        }

        private void paintBar(Graphics2D g2) {
            int width = getWidth();
            int x = 130;
            int y = 28;
            int barMax = Math.max(80, width - x - 70);
            int rowHeight = 28;
            int max = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("主題借閱柱狀圖", 18, 18);
            int i = 0;
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                int yy = y + i * rowHeight;
                if (yy > getHeight() - 28) {
                    break;
                }
                int barWidth = Math.max(6, (int) Math.round(entry.getValue() * 1.0 / max * barMax));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(shorten(entry.getKey(), 12), 16, yy + 15);
                g2.fillRoundRect(x, yy, barWidth, 18, 8, 8);
                g2.drawString(String.valueOf(entry.getValue()), x + barWidth + 8, yy + 15);
                i++;
            }
        }

        private void paintPie(Graphics2D g2) {
            int size = Math.min(getWidth() - 180, getHeight() - 60);
            size = Math.max(160, size);
            int x = 28;
            int y = 42;
            int total = data.values().stream().mapToInt(Integer::intValue).sum();
            int start = 0;
            int i = 0;
            Color[] palette = new Color[]{
                    new Color(80, 80, 80), new Color(120, 120, 120), new Color(160, 160, 160),
                    new Color(100, 130, 160), new Color(150, 120, 100), new Color(120, 150, 120),
                    new Color(160, 100, 120), new Color(100, 160, 160), new Color(170, 150, 90), new Color(130, 100, 160)
            };
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("主題借閱圓餅圖", 18, 18);
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                int angle = (int) Math.round(entry.getValue() * 360.0 / total);
                g2.setColor(palette[i % palette.length]);
                g2.fillArc(x, y, size, size, start, angle);
                start += angle;
                i++;
            }
            int legendX = x + size + 24;
            int legendY = y + 12;
            i = 0;
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                int yy = legendY + i * 24;
                if (yy > getHeight() - 20) {
                    break;
                }
                g2.setColor(palette[i % palette.length]);
                g2.fillRect(legendX, yy - 10, 14, 14);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(shorten(entry.getKey(), 14) + "：" + entry.getValue(), legendX + 20, yy + 2);
                i++;
            }
        }

        private String shorten(String value, int maxLength) {
            if (value == null) {
                return "";
            }
            return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
        }
    }

}
