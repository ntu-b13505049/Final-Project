package librarysystem.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import librarysystem.model.BorrowRecord;
import librarysystem.model.DashboardStats;
import librarysystem.util.DateUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class WebDashboardServer {
    private final AdminService adminService = new AdminService();
    private HttpServer server;
    private int port;

    public synchronized String start() {
        if (server != null) {
            return url();
        }

        IOException lastException = null;
        for (int candidatePort : new int[]{8080, 8081, 8082, 8083}) {
            try {
                HttpServer created = HttpServer.create(new InetSocketAddress("127.0.0.1", candidatePort), 0);
                created.createContext("/", this::handleHome);
                created.setExecutor(null);
                created.start();
                server = created;
                port = candidatePort;
                return url();
            } catch (IOException e) {
                lastException = e;
            }
        }
        throw new RuntimeException("無法啟動 Web 報表服務，8080~8083 連接埠皆不可用。", lastException);
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            port = 0;
        }
    }

    public synchronized boolean isRunning() {
        return server != null;
    }

    public synchronized String url() {
        return server == null ? "" : "http://127.0.0.1:" + port + "/";
    }

    private void handleHome(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        String html = renderDashboardHtml();
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String renderDashboardHtml() {
        DashboardStats stats = adminService.getDashboardStats();
        Map<String, Integer> subjectStats = adminService.getSubjectBorrowStats();
        StringBuilder html = new StringBuilder();
        html.append("""
                <!doctype html>
                <html lang="zh-Hant">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>圖書館系統 Web 報表</title>
                  <style>
                    body { font-family: Arial, 'Microsoft JhengHei', sans-serif; margin: 0; background: #f4f6f8; color: #1f2937; }
                    header { background: #1f2937; color: white; padding: 24px 36px; }
                    main { padding: 24px 36px; }
                    .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 16px; }
                    .card { background: white; border-radius: 14px; padding: 18px; box-shadow: 0 4px 12px rgba(0,0,0,.08); }
                    .card .value { font-size: 32px; font-weight: bold; margin-top: 8px; }
                    section { background: white; border-radius: 14px; padding: 18px; margin-top: 22px; box-shadow: 0 4px 12px rgba(0,0,0,.08); }
                    table { width: 100%; border-collapse: collapse; }
                    th, td { border-bottom: 1px solid #e5e7eb; padding: 10px; text-align: left; }
                    th { background: #f9fafb; }
                    .bar { display: inline-block; height: 14px; background: #4b5563; border-radius: 999px; vertical-align: middle; }
                    .muted { color: #6b7280; }
                  </style>
                </head>
                <body>
                <header>
                  <h1>圖書館借還書系統 Web 報表</h1>
                  <div class="muted">本頁由 Java HttpServer 即時讀取 SQLite 資料庫產生，可用於 Demo 展示。</div>
                </header>
                <main>
                """);
        html.append("<div class=\"cards\">");
        appendCard(html, "總書數", stats.getTotalBooks());
        appendCard(html, "啟用使用者", stats.getActiveUsers());
        appendCard(html, "目前借閱中", stats.getCurrentBorrows());
        appendCard(html, "逾期借閱", stats.getOverdueBorrows());
        appendCard(html, "書評總數", stats.getTotalReviews());
        appendCard(html, "待處理預約", stats.getWaitingReservations());
        appendCard(html, "待審核等級申請", stats.getPendingRoleRequests());
        html.append("</div>");

        html.append("<section><h2>主題借閱熱度 Top 10</h2><table><tr><th>主題</th><th>次數</th><th>圖示</th></tr>");
        int max = subjectStats.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        for (Map.Entry<String, Integer> entry : subjectStats.entrySet()) {
            int width = Math.max(8, (int) Math.round(entry.getValue() * 220.0 / max));
            html.append("<tr><td>").append(escape(entry.getKey())).append("</td><td>").append(entry.getValue()).append("</td><td><span class=\"bar\" style=\"width:").append(width).append("px\"></span></td></tr>");
        }
        html.append("</table></section>");

        html.append("<section><h2>目前逾期未還紀錄</h2><table><tr><th>學號</th><th>借閱者</th><th>書名</th><th>到期時間</th><th>逾期天數</th><th>模擬罰款</th></tr>");
        for (BorrowRecord record : adminService.getOpenOverdueRecords()) {
            html.append("<tr><td>").append(escape(record.getStudentNo()))
                    .append("</td><td>").append(escape(record.getBorrowerName()))
                    .append("</td><td>").append(escape(record.getBookTitle()))
                    .append("</td><td>").append(DateUtil.formatDisplay(record.getDueDate()))
                    .append("</td><td>").append(record.getOverdueDays())
                    .append("</td><td>").append(record.getFineAmount()).append(" 元</td></tr>");
        }
        html.append("</table></section>");
        html.append("</main></body></html>");
        return html.toString();
    }

    private void appendCard(StringBuilder html, String title, int value) {
        html.append("<div class=\"card\"><div>").append(escape(title)).append("</div><div class=\"value\">").append(value).append("</div></div>");
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
