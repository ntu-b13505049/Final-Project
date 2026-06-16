package librarysystem.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import librarysystem.model.Book;
import librarysystem.model.BorrowRecord;
import librarysystem.model.DashboardStats;
import librarysystem.model.Reservation;
import librarysystem.model.Review;
import librarysystem.model.RoleChangeRequest;
import librarysystem.model.User;
import librarysystem.util.DateUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
                created.createContext("/", this::handleRequest);
                created.setExecutor(null);
                created.start();
                server = created;
                port = candidatePort;
                return url();
            } catch (IOException e) {
                lastException = e;
            }
        }
        throw new RuntimeException("無法啟動 Web 管理頁，8080~8083 連接埠皆不可用。", lastException);
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

    private void handleRequest(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed", "text/plain; charset=UTF-8");
            return;
        }

        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        Map<String, String> query = parseQuery(uri.getRawQuery());

        try {
            switch (path) {
                case "/", "/index.html" -> sendText(exchange, 200, renderDashboardHtml(), "text/html; charset=UTF-8");
                case "/api/stats" -> sendJson(exchange, renderStatsJson());
                case "/api/subjects" -> sendJson(exchange, renderSubjectStatsJson());
                case "/api/overdue" -> sendJson(exchange, borrowRecordsToJson(adminService.getOpenOverdueRecords()));
                case "/api/books" -> sendJson(exchange, booksToJson(adminService.searchBooks(query.getOrDefault("q", ""), query.getOrDefault("status", ""))));
                case "/api/users" -> sendJson(exchange, usersToJson(adminService.searchUsers(query.getOrDefault("q", ""), query.getOrDefault("role", ""), query.getOrDefault("status", ""))));
                case "/api/borrows" -> sendJson(exchange, borrowRecordsToJson(adminService.getBorrowRecords(
                        query.getOrDefault("student", ""),
                        query.getOrDefault("name", ""),
                        query.getOrDefault("book", ""),
                        query.getOrDefault("status", "")
                )));
                case "/api/reviews" -> sendJson(exchange, reviewsToJson(adminService.searchReviews(query.getOrDefault("q", ""), query.getOrDefault("rating", ""))));
                case "/api/reservations" -> sendJson(exchange, reservationsToJson(adminService.searchReservations(query.getOrDefault("q", ""), query.getOrDefault("status", ""))));
                case "/api/role-requests" -> sendJson(exchange, roleRequestsToJson(adminService.getRoleChangeRequests(query.getOrDefault("status", ""), query.getOrDefault("q", ""))));
                default -> sendJson(exchange, 404, "{\"error\":\"找不到路由\"}");
            }
        } catch (RuntimeException e) {
            sendJson(exchange, 500, "{\"error\":" + jsonString(e.getMessage()) + "}");
        }
    }

    private String renderDashboardHtml() {
        return """
                <!doctype html>
                <html lang="zh-Hant">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>圖書館借還書系統 Web 管理頁</title>
                  <style>
                    :root {
                      --bg: #f5f7fb;
                      --panel: #ffffff;
                      --ink: #19213a;
                      --muted: #64748b;
                      --line: #e2e8f0;
                      --brand: #635bff;
                      --brand-2: #7c3aed;
                      --good: #047857;
                      --warn: #b45309;
                      --bad: #b91c1c;
                      --soft-red: #fff1f2;
                      --shadow: 0 18px 45px rgba(31, 41, 55, .08);
                    }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      font-family: Arial, "Microsoft JhengHei", "Noto Sans TC", sans-serif;
                      background:
                        radial-gradient(circle at top left, rgba(99, 91, 255, .18), transparent 36rem),
                        linear-gradient(180deg, #fbfbff 0%, var(--bg) 45%, #eef2ff 100%);
                      color: var(--ink);
                      font-size: 17px;
                    }
                    header {
                      padding: 32px 42px 18px;
                      color: white;
                      background: linear-gradient(135deg, #312e81 0%, #4f46e5 50%, #7c3aed 100%);
                      border-bottom-left-radius: 32px;
                      border-bottom-right-radius: 32px;
                      box-shadow: var(--shadow);
                    }
                    header h1 { margin: 0; font-size: 34px; letter-spacing: .04em; }
                    header p { margin: 10px 0 0; color: rgba(255,255,255,.82); line-height: 1.6; }
                    .toolbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-top: 20px; flex-wrap: wrap; }
                    .pill {
                      display: inline-flex; align-items: center; gap: 8px;
                      padding: 9px 14px; border-radius: 999px;
                      background: rgba(255,255,255,.15); color: white;
                      border: 1px solid rgba(255,255,255,.24);
                    }
                    .refresh {
                      border: 0; border-radius: 999px; padding: 11px 18px;
                      font-weight: 800; cursor: pointer; color: #312e81; background: white;
                      box-shadow: 0 8px 20px rgba(0,0,0,.14);
                    }
                    main { padding: 24px 42px 42px; max-width: 1500px; margin: 0 auto; }
                    nav {
                      display: flex; gap: 10px; flex-wrap: wrap; margin: 8px 0 22px;
                      position: sticky; top: 0; z-index: 5; padding: 10px 0; backdrop-filter: blur(10px);
                    }
                    .tab-button {
                      border: 1px solid var(--line); background: rgba(255,255,255,.78);
                      color: var(--ink); border-radius: 999px; padding: 10px 16px;
                      cursor: pointer; font-weight: 800; font-size: 16px;
                    }
                    .tab-button.active { background: var(--brand); color: white; border-color: var(--brand); }
                    .tab { display: none; }
                    .tab.active { display: block; }
                    .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 16px; }
                    .card {
                      background: rgba(255,255,255,.88); border: 1px solid rgba(226,232,240,.8);
                      border-radius: 22px; padding: 20px; box-shadow: var(--shadow);
                    }
                    .card .label { color: var(--muted); font-weight: 800; }
                    .card .value { font-size: 34px; font-weight: 900; margin-top: 8px; }
                    .grid-2 { display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(360px, .9fr); gap: 18px; align-items: start; }
                    .panel {
                      background: rgba(255,255,255,.92); border: 1px solid rgba(226,232,240,.85);
                      border-radius: 24px; box-shadow: var(--shadow); padding: 20px; margin-bottom: 18px;
                    }
                    .panel h2 { margin: 0 0 14px; font-size: 24px; }
                    .panel h3 { margin: 0 0 12px; font-size: 20px; }
                    .hint { color: var(--muted); line-height: 1.6; margin: 6px 0 16px; }
                    .filters { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 12px; margin: 10px 0 16px; }
                    label { display: block; color: var(--muted); font-weight: 800; font-size: 14px; margin-bottom: 6px; }
                    input, select {
                      width: 100%; border: 1px solid var(--line); border-radius: 14px;
                      padding: 11px 12px; font-size: 16px; background: white; color: var(--ink);
                    }
                    .table-wrap { overflow: auto; border: 1px solid var(--line); border-radius: 18px; background: white; }
                    table { width: 100%; border-collapse: collapse; min-width: 860px; }
                    th, td { border-bottom: 1px solid var(--line); padding: 11px 12px; text-align: left; vertical-align: top; }
                    th { background: #f8fafc; position: sticky; top: 0; z-index: 2; white-space: nowrap; }
                    tr:hover td { background: #fafaff; }
                    tr.overdue td { background: var(--soft-red); color: #7f1d1d; font-weight: 800; }
                    .badge { display: inline-flex; border-radius: 999px; padding: 5px 10px; font-size: 14px; font-weight: 900; white-space: nowrap; }
                    .badge.good { background: #dcfce7; color: var(--good); }
                    .badge.warn { background: #fef3c7; color: var(--warn); }
                    .badge.bad { background: #fee2e2; color: var(--bad); }
                    .badge.gray { background: #e2e8f0; color: #334155; }
                    .badge.brand { background: #ede9fe; color: var(--brand-2); }
                    .count-line { color: var(--muted); font-weight: 800; margin: 4px 0 12px; }
                    canvas { width: 100%; max-height: 280px; }
                    .api-note { font-family: Consolas, monospace; background: #0f172a; color: #e0e7ff; border-radius: 16px; padding: 14px; overflow: auto; }
                    @media (max-width: 920px) {
                      header, main { padding-left: 18px; padding-right: 18px; }
                      .grid-2 { grid-template-columns: 1fr; }
                      table { min-width: 760px; }
                    }
                  </style>
                </head>
                <body>
                  <header>
                    <h1>圖書館借還書系統 Web 管理頁</h1>
                    <p>使用 Java 內建 HttpServer 提供 HTML / CSS / JavaScript 頁面，前端透過 API 即時讀取 SQLite 資料庫。</p>
                    <div class="toolbar">
                      <span class="pill">本機網址：<strong id="current-url"></strong></span>
                      <span class="pill">最後更新：<strong id="updated-at">載入中</strong></span>
                      <button class="refresh" id="refresh-all">重新整理全部資料</button>
                    </div>
                  </header>
                  <main>
                    <nav id="tabs">
                      <button class="tab-button active" data-tab="overview">總覽</button>
                      <button class="tab-button" data-tab="books">書籍查詢</button>
                      <button class="tab-button" data-tab="users">使用者</button>
                      <button class="tab-button" data-tab="borrows">借還紀錄</button>
                      <button class="tab-button" data-tab="reviews">書評</button>
                      <button class="tab-button" data-tab="reservations">預約</button>
                      <button class="tab-button" data-tab="roles">等級申請</button>
                    </nav>

                    <section class="tab active" id="tab-overview">
                      <div class="cards" id="stats-cards"></div>
                      <div class="grid-2" style="margin-top:18px;">
                        <div class="panel">
                          <h2>主題借閱熱度</h2>
                          <p class="hint">依借閱紀錄統計各主題出現次數，使用 Canvas 畫出柱狀圖。</p>
                          <canvas id="subject-bar" height="260"></canvas>
                        </div>
                        <div class="panel">
                          <h2>主題分布比例</h2>
                          <p class="hint">同一份資料也會畫成圓餅圖，Demo 時可展示 Web 視覺化。</p>
                          <canvas id="subject-pie" height="260"></canvas>
                        </div>
                      </div>
                      <div class="panel">
                        <h2>目前逾期未還紀錄</h2>
                        <p class="hint">逾期資料會用紅色醒目標示，並顯示逾期天數與模擬罰款。</p>
                        <div class="count-line" id="overdue-count"></div>
                        <div class="table-wrap"><table id="overdue-table"></table></div>
                      </div>
                    </section>

                    <section class="tab" id="tab-books">
                      <div class="panel">
                        <h2>書籍查詢</h2>
                        <p class="hint">可查題名、作者、主題、出版者、出版年、ISBN 等欄位；輸入後會自動搜尋。</p>
                        <div class="filters">
                          <div><label>關鍵字 / ISBN</label><input id="book-q" placeholder="例如：Python、Pearson、978..." autocomplete="off"></div>
                          <div><label>狀態</label><select id="book-status"><option value="">全部</option><option value="ACTIVE">上架中</option><option value="INACTIVE">已下架</option><option value="BORROWED">已借出</option><option value="RESERVED">已被預約</option><option value="AVAILABLE">可借</option></select></div>
                        </div>
                        <div class="count-line" id="books-count"></div>
                        <div class="table-wrap"><table id="books-table"></table></div>
                      </div>
                    </section>

                    <section class="tab" id="tab-users">
                      <div class="panel">
                        <h2>使用者查詢</h2>
                        <div class="filters">
                          <div><label>學號 / 姓名 / ID</label><input id="user-q" placeholder="例如：A12345678、張家豪" autocomplete="off"></div>
                          <div><label>等級</label><select id="user-role"><option value="">全部</option><option>NORMAL</option><option>VIP</option><option>GOLD</option><option>PLATINUM</option></select></div>
                          <div><label>狀態</label><select id="user-status"><option value="">全部</option><option>ACTIVE</option><option>SUSPENDED</option></select></div>
                        </div>
                        <div class="count-line" id="users-count"></div>
                        <div class="table-wrap"><table id="users-table"></table></div>
                      </div>
                    </section>

                    <section class="tab" id="tab-borrows">
                      <div class="panel">
                        <h2>借還紀錄查詢</h2>
                        <p class="hint">可依學號、姓名、書名 / 書籍 ID 查詢；逾期列會自動標紅。</p>
                        <div class="filters">
                          <div><label>學號</label><input id="borrow-student" placeholder="例如：A12345678" autocomplete="off"></div>
                          <div><label>姓名</label><input id="borrow-name" placeholder="例如：張家豪" autocomplete="off"></div>
                          <div><label>書名 / 書籍 ID</label><input id="borrow-book" placeholder="例如：Python 或 109" autocomplete="off"></div>
                          <div><label>狀態</label><select id="borrow-status"><option value="">全部</option><option value="CURRENT">借閱中</option><option value="RETURNED">已歸還</option><option value="OVERDUE">逾期</option><option value="NOT_OVERDUE">未逾期</option></select></div>
                        </div>
                        <div class="count-line" id="borrows-count"></div>
                        <div class="table-wrap"><table id="borrows-table"></table></div>
                      </div>
                    </section>

                    <section class="tab" id="tab-reviews">
                      <div class="panel">
                        <h2>書評查詢</h2>
                        <div class="filters">
                          <div><label>使用者 / 書名 / 內容</label><input id="review-q" placeholder="例如：書名、學生、評論內容" autocomplete="off"></div>
                          <div><label>評分</label><select id="review-rating"><option value="">全部</option><option>5</option><option>4</option><option>3</option><option>2</option><option>1</option></select></div>
                        </div>
                        <div class="count-line" id="reviews-count"></div>
                        <div class="table-wrap"><table id="reviews-table"></table></div>
                      </div>
                    </section>

                    <section class="tab" id="tab-reservations">
                      <div class="panel">
                        <h2>預約查詢</h2>
                        <div class="filters">
                          <div><label>預約 ID / 使用者 / 書名</label><input id="reservation-q" placeholder="例如：WAITING、張家豪、書名" autocomplete="off"></div>
                          <div><label>狀態</label><select id="reservation-status"><option value="">全部</option><option>WAITING</option><option>NOTIFIED</option><option>FULFILLED</option><option>CANCELLED</option></select></div>
                        </div>
                        <div class="count-line" id="reservations-count"></div>
                        <div class="table-wrap"><table id="reservations-table"></table></div>
                      </div>
                    </section>

                    <section class="tab" id="tab-roles">
                      <div class="panel">
                        <h2>等級申請查詢</h2>
                        <div class="filters">
                          <div><label>學號 / 姓名 / 理由</label><input id="role-q" placeholder="例如：VIP、申請理由、學號" autocomplete="off"></div>
                          <div><label>狀態</label><select id="role-status"><option value="">全部</option><option>PENDING</option><option>APPROVED</option><option>REJECTED</option></select></div>
                        </div>
                        <div class="count-line" id="roles-count"></div>
                        <div class="table-wrap"><table id="roles-table"></table></div>
                      </div>
                      <div class="panel">
                        <h3>API 測試</h3>
                        <p class="hint">這些 API 都是由 Java HttpServer 提供，前端 JavaScript 會自動呼叫。</p>
                        <div class="api-note">/api/stats、/api/subjects、/api/books?q=Python、/api/users?q=A123、/api/borrows?student=A12345678、/api/reviews、/api/reservations、/api/role-requests</div>
                      </div>
                    </section>
                  </main>
                  <script>
                    const $ = (id) => document.getElementById(id);
                    const palette = ["#635bff", "#7c3aed", "#06b6d4", "#10b981", "#f59e0b", "#ef4444", "#ec4899", "#64748b", "#0ea5e9", "#84cc16"];
                    const debounce = (fn, wait = 260) => { let t; return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), wait); }; };
                    const htmlEscapes = {"&":"&amp;", "<":"&lt;", ">":"&gt;", '"':"&quot;", "'":"&#39;"};
                    const esc = (v) => String(v ?? "").replace(/[&<>"']/g, (m) => htmlEscapes[m]);
                    const params = (obj) => {
                      const p = new URLSearchParams();
                      Object.entries(obj).forEach(([k,v]) => { if (v !== undefined && v !== null && String(v).trim() !== "") p.set(k, v); });
                      const s = p.toString();
                      return s ? "?" + s : "";
                    };
                    async function getJson(url) {
                      const res = await fetch(url, { headers: { "Accept": "application/json" } });
                      if (!res.ok) throw new Error(await res.text());
                      return await res.json();
                    }
                    function badge(text, type = "gray") { return `<span class="badge ${type}">${esc(text)}</span>`; }
                    function bookStatus(row) {
                      const availability = row.availability || "";
                      if (!row.active || availability === "下架") return badge("已下架", "gray");
                      if (availability.includes("已被預約")) return badge(availability, "brand");
                      if (availability.includes("已借出")) return badge(availability, "warn");
                      return badge("可借", "good");
                    }
                    function userStatus(v) { return v === "ACTIVE" ? badge(v, "good") : badge(v, "bad"); }
                    function roleBadge(v) { return badge(v || "NORMAL", v === "NORMAL" ? "gray" : "brand"); }
                    function borrowStatus(row) {
                      if (row.overdue) return badge("逾期 " + row.overdueDays + " 天", "bad");
                      return row.returnDate ? badge("已歸還", "gray") : badge("借閱中", "good");
                    }
                    function simpleTable(tableId, rows, columns, options = {}) {
                      const table = $(tableId);
                      if (!rows || rows.length === 0) {
                        table.innerHTML = `<tr><td style="padding:22px; color:#64748b;">查無資料</td></tr>`;
                        return;
                      }
                      const head = columns.map(c => `<th>${esc(c.title)}</th>`).join("");
                      const body = rows.map(row => {
                        const cls = options.rowClass ? options.rowClass(row) : "";
                        const cells = columns.map(c => `<td>${c.html ? c.html(row) : esc(row[c.key])}</td>`).join("");
                        return `<tr class="${cls}">${cells}</tr>`;
                      }).join("");
                      table.innerHTML = `<thead><tr>${head}</tr></thead><tbody>${body}</tbody>`;
                    }
                    function setCount(id, rows, label) { $(id).textContent = `共 ${rows.length} 筆${label || "資料"}`; }

                    async function loadStats() {
                      const [stats, subjects, overdue] = await Promise.all([
                        getJson("/api/stats"), getJson("/api/subjects"), getJson("/api/overdue")
                      ]);
                      const cards = [
                        ["總書數", stats.totalBooks], ["啟用使用者", stats.activeUsers], ["目前借閱中", stats.currentBorrows],
                        ["逾期借閱", stats.overdueBorrows], ["書評總數", stats.totalReviews], ["待處理預約", stats.waitingReservations],
                        ["待審核等級申請", stats.pendingRoleRequests]
                      ];
                      $("stats-cards").innerHTML = cards.map(([k,v]) => `<div class="card"><div class="label">${esc(k)}</div><div class="value">${esc(v)}</div></div>`).join("");
                      drawBarChart($("subject-bar"), subjects);
                      drawPieChart($("subject-pie"), subjects);
                      setCount("overdue-count", overdue, "逾期未還紀錄");
                      simpleTable("overdue-table", overdue, borrowColumns(), { rowClass: () => "overdue" });
                      updateTime();
                    }
                    function drawBarChart(canvas, data) {
                      const ctx = canvas.getContext("2d");
                      const dpr = window.devicePixelRatio || 1;
                      const rect = canvas.getBoundingClientRect();
                      canvas.width = Math.max(600, rect.width * dpr);
                      canvas.height = 280 * dpr;
                      ctx.scale(dpr, dpr);
                      const w = canvas.width / dpr, h = canvas.height / dpr;
                      ctx.clearRect(0,0,w,h);
                      const max = Math.max(1, ...data.map(d => d.count));
                      const left = 118, right = 16, top = 18, gap = 8;
                      const barH = Math.max(14, (h - top - 16) / Math.max(data.length, 1) - gap);
                      ctx.font = "14px Microsoft JhengHei, Arial";
                      data.forEach((d, i) => {
                        const y = top + i * (barH + gap);
                        const barW = (w - left - right - 52) * d.count / max;
                        ctx.fillStyle = palette[i % palette.length];
                        roundRect(ctx, left, y, barW, barH, 8);
                        ctx.fill();
                        ctx.fillStyle = "#334155";
                        ctx.textAlign = "right";
                        ctx.fillText(d.subject.slice(0, 9), left - 10, y + barH - 3);
                        ctx.textAlign = "left";
                        ctx.fillText(d.count, left + barW + 8, y + barH - 3);
                      });
                    }
                    function drawPieChart(canvas, data) {
                      const ctx = canvas.getContext("2d");
                      const dpr = window.devicePixelRatio || 1;
                      const rect = canvas.getBoundingClientRect();
                      canvas.width = Math.max(360, rect.width * dpr);
                      canvas.height = 280 * dpr;
                      ctx.scale(dpr, dpr);
                      const w = canvas.width / dpr, h = canvas.height / dpr;
                      ctx.clearRect(0,0,w,h);
                      const total = Math.max(1, data.reduce((s,d) => s + d.count, 0));
                      const cx = Math.min(w * .35, 150), cy = h / 2, r = Math.min(110, h / 2 - 18);
                      let start = -Math.PI / 2;
                      data.forEach((d, i) => {
                        const angle = Math.PI * 2 * d.count / total;
                        ctx.beginPath(); ctx.moveTo(cx, cy); ctx.arc(cx, cy, r, start, start + angle); ctx.closePath();
                        ctx.fillStyle = palette[i % palette.length]; ctx.fill();
                        start += angle;
                      });
                      ctx.font = "14px Microsoft JhengHei, Arial";
                      data.slice(0, 8).forEach((d, i) => {
                        const x = Math.min(w * .62, cx + r + 42), y = 28 + i * 28;
                        ctx.fillStyle = palette[i % palette.length]; ctx.fillRect(x, y - 12, 14, 14);
                        ctx.fillStyle = "#334155"; ctx.fillText(`${d.subject} (${d.count})`, x + 22, y);
                      });
                    }
                    function roundRect(ctx, x, y, w, h, r) {
                      const rr = Math.min(r, w / 2, h / 2);
                      ctx.beginPath(); ctx.moveTo(x + rr, y); ctx.arcTo(x + w, y, x + w, y + h, rr); ctx.arcTo(x + w, y + h, x, y + h, rr); ctx.arcTo(x, y + h, x, y, rr); ctx.arcTo(x, y, x + w, y, rr); ctx.closePath();
                    }

                    const bookColumns = () => [
                      {title:"ID", key:"bookId"}, {title:"題名", key:"title"}, {title:"作者", key:"authors"}, {title:"主題", key:"subjects"},
                      {title:"出版者", key:"publisher"}, {title:"年份", key:"publishYear"}, {title:"ISBN", key:"isbn"}, {title:"狀態", html: bookStatus}
                    ];
                    const userColumns = () => [
                      {title:"ID", key:"userId"}, {title:"學號", key:"studentNo"}, {title:"姓名", key:"name"}, {title:"等級", html:r=>roleBadge(r.roleLevel)},
                      {title:"狀態", html:r=>userStatus(r.status)}, {title:"建立時間", key:"createdAt"}
                    ];
                    const borrowColumns = () => [
                      {title:"紀錄ID", key:"recordId"}, {title:"學號", key:"studentNo"}, {title:"借閱者", key:"borrowerName"}, {title:"書名", key:"bookTitle"},
                      {title:"借出", key:"borrowDate"}, {title:"到期", key:"dueDate"}, {title:"歸還", key:"returnDate"}, {title:"狀態", html: borrowStatus}, {title:"罰款", html:r=>esc(r.fineAmount) + " 元"}
                    ];
                    async function loadBooks() {
                      const rows = await getJson("/api/books" + params({q: $("book-q").value, status: $("book-status").value}));
                      setCount("books-count", rows, "書籍"); simpleTable("books-table", rows, bookColumns());
                    }
                    async function loadUsers() {
                      const rows = await getJson("/api/users" + params({q: $("user-q").value, role: $("user-role").value, status: $("user-status").value}));
                      setCount("users-count", rows, "使用者"); simpleTable("users-table", rows, userColumns());
                    }
                    async function loadBorrows() {
                      const rows = await getJson("/api/borrows" + params({student: $("borrow-student").value, name: $("borrow-name").value, book: $("borrow-book").value, status: $("borrow-status").value}));
                      setCount("borrows-count", rows, "借還紀錄"); simpleTable("borrows-table", rows, borrowColumns(), {rowClass:r=>r.overdue ? "overdue" : ""});
                    }
                    async function loadReviews() {
                      const rows = await getJson("/api/reviews" + params({q: $("review-q").value, rating: $("review-rating").value}));
                      setCount("reviews-count", rows, "書評");
                      simpleTable("reviews-table", rows, [
                        {title:"ID", key:"reviewId"}, {title:"使用者", key:"userName"}, {title:"書名", key:"bookTitle"}, {title:"評分", html:r=>"★".repeat(r.rating)}, {title:"內容", key:"content"}, {title:"時間", key:"createdAt"}
                      ]);
                    }
                    async function loadReservations() {
                      const rows = await getJson("/api/reservations" + params({q: $("reservation-q").value, status: $("reservation-status").value}));
                      setCount("reservations-count", rows, "預約");
                      simpleTable("reservations-table", rows, [
                        {title:"ID", key:"reservationId"}, {title:"使用者", key:"userName"}, {title:"書名", key:"bookTitle"}, {title:"狀態", html:r=>badge(r.status, r.status === "WAITING" || r.status === "NOTIFIED" ? "warn" : "gray")}, {title:"建立", key:"createdAt"}, {title:"通知", key:"notifiedAt"}
                      ]);
                    }
                    async function loadRoles() {
                      const rows = await getJson("/api/role-requests" + params({q: $("role-q").value, status: $("role-status").value}));
                      setCount("roles-count", rows, "等級申請");
                      simpleTable("roles-table", rows, [
                        {title:"ID", key:"requestId"}, {title:"學號", key:"studentNo"}, {title:"姓名", key:"userName"}, {title:"目前", html:r=>roleBadge(r.currentLevel)}, {title:"申請", html:r=>roleBadge(r.targetLevel)}, {title:"狀態", html:r=>badge(r.status, r.status === "PENDING" ? "warn" : (r.status === "APPROVED" ? "good" : "bad"))}, {title:"理由", key:"reason"}, {title:"時間", key:"createdAt"}
                      ]);
                    }
                    function updateTime() { $("updated-at").textContent = new Date().toLocaleString("zh-TW"); }
                    function wireAuto(ids, loader) {
                      const run = debounce(loader);
                      ids.forEach(id => {
                        const el = $(id);
                        el.addEventListener("input", run);
                        el.addEventListener("change", loader);
                      });
                    }
                    async function refreshAll() {
                      try {
                        await Promise.all([loadStats(), loadBooks(), loadUsers(), loadBorrows(), loadReviews(), loadReservations(), loadRoles()]);
                      } catch (e) {
                        alert("讀取資料失敗：" + e.message);
                      }
                    }
                    function setupTabs() {
                      document.querySelectorAll(".tab-button").forEach(btn => {
                        btn.addEventListener("click", () => {
                          document.querySelectorAll(".tab-button").forEach(b => b.classList.remove("active"));
                          document.querySelectorAll(".tab").forEach(t => t.classList.remove("active"));
                          btn.classList.add("active");
                          $("tab-" + btn.dataset.tab).classList.add("active");
                          history.replaceState(null, "", "#" + btn.dataset.tab);
                        });
                      });
                      const hash = location.hash.replace("#", "");
                      if (hash) document.querySelector(`[data-tab="${hash}"]`)?.click();
                    }
                    document.addEventListener("DOMContentLoaded", () => {
                      $("current-url").textContent = location.href;
                      setupTabs();
                      wireAuto(["book-q", "book-status"], loadBooks);
                      wireAuto(["user-q", "user-role", "user-status"], loadUsers);
                      wireAuto(["borrow-student", "borrow-name", "borrow-book", "borrow-status"], loadBorrows);
                      wireAuto(["review-q", "review-rating"], loadReviews);
                      wireAuto(["reservation-q", "reservation-status"], loadReservations);
                      wireAuto(["role-q", "role-status"], loadRoles);
                      $("refresh-all").addEventListener("click", refreshAll);
                      window.addEventListener("resize", debounce(loadStats, 400));
                      refreshAll();
                    });
                  </script>
                </body>
                </html>
                """;
    }

    private String renderStatsJson() {
        DashboardStats stats = adminService.getDashboardStats();
        StringBuilder json = new StringBuilder();
        json.append("{");
        appendJsonField(json, "totalBooks", stats.getTotalBooks()).append(',');
        appendJsonField(json, "activeUsers", stats.getActiveUsers()).append(',');
        appendJsonField(json, "currentBorrows", stats.getCurrentBorrows()).append(',');
        appendJsonField(json, "overdueBorrows", stats.getOverdueBorrows()).append(',');
        appendJsonField(json, "totalReviews", stats.getTotalReviews()).append(',');
        appendJsonField(json, "waitingReservations", stats.getWaitingReservations()).append(',');
        appendJsonField(json, "pendingRoleRequests", stats.getPendingRoleRequests());
        json.append("}");
        return json.toString();
    }

    private String renderSubjectStatsJson() {
        Map<String, Integer> subjectStats = adminService.getSubjectBorrowStats();
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : subjectStats.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('{');
            appendJsonField(json, "subject", entry.getKey()).append(',');
            appendJsonField(json, "count", entry.getValue());
            json.append('}');
        }
        json.append(']');
        return json.toString();
    }

    private String booksToJson(List<Book> books) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < books.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            Book book = books.get(i);
            json.append('{');
            appendJsonField(json, "bookId", book.getBookId()).append(',');
            appendJsonField(json, "title", book.getTitle()).append(',');
            appendJsonField(json, "authors", book.getAuthors()).append(',');
            appendJsonField(json, "subjects", book.getSubjects()).append(',');
            appendJsonField(json, "publisher", book.getPublisher()).append(',');
            appendJsonField(json, "publishYear", book.getPublishYear()).append(',');
            appendJsonField(json, "edition", book.getEdition()).append(',');
            appendJsonField(json, "formatDesc", book.getFormatDesc()).append(',');
            appendJsonField(json, "source", book.getSource()).append(',');
            appendJsonField(json, "note", book.getNote()).append(',');
            appendJsonField(json, "isbn", book.getIsbn()).append(',');
            appendJsonField(json, "active", book.isActive()).append(',');
            appendJsonField(json, "borrowed", book.isBorrowed()).append(',');
            appendJsonField(json, "reserved", book.isReserved()).append(',');
            appendJsonField(json, "availability", book.getAvailabilityText());
            json.append('}');
        }
        json.append(']');
        return json.toString();
    }

    private String usersToJson(List<User> users) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            User user = users.get(i);
            json.append('{');
            appendJsonField(json, "userId", user.getUserId()).append(',');
            appendJsonField(json, "studentNo", user.getStudentNo()).append(',');
            appendJsonField(json, "name", user.getName()).append(',');
            appendJsonField(json, "roleLevel", user.getRoleLevel()).append(',');
            appendJsonField(json, "roleDescription", user.getRoleDescription()).append(',');
            appendJsonField(json, "status", user.getStatus()).append(',');
            appendJsonField(json, "createdAt", user.getCreatedAt());
            json.append('}');
        }
        json.append(']');
        return json.toString();
    }

    private String borrowRecordsToJson(List<BorrowRecord> records) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < records.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            BorrowRecord record = records.get(i);
            json.append('{');
            appendJsonField(json, "recordId", record.getRecordId()).append(',');
            appendJsonField(json, "userId", record.getUserId()).append(',');
            appendJsonField(json, "bookId", record.getBookId()).append(',');
            appendJsonField(json, "studentNo", record.getStudentNo()).append(',');
            appendJsonField(json, "borrowerName", record.getBorrowerName()).append(',');
            appendJsonField(json, "userRoleLevel", record.getUserRoleLevel()).append(',');
            appendJsonField(json, "bookTitle", record.getBookTitle()).append(',');
            appendJsonField(json, "borrowDate", DateUtil.formatDisplay(record.getBorrowDate())).append(',');
            appendJsonField(json, "dueDate", DateUtil.formatDisplay(record.getDueDate())).append(',');
            appendJsonField(json, "returnDate", record.getReturnDate() == null ? "" : DateUtil.formatDisplay(record.getReturnDate())).append(',');
            appendJsonField(json, "borrowDays", record.getBorrowDays()).append(',');
            appendJsonField(json, "overdue", record.isOverdue()).append(',');
            appendJsonField(json, "overdueDays", record.getOverdueDays()).append(',');
            appendJsonField(json, "fineRatePerDay", record.getFineRatePerDay()).append(',');
            appendJsonField(json, "fineAmount", record.getFineAmount());
            json.append('}');
        }
        json.append(']');
        return json.toString();
    }

    private String reviewsToJson(List<Review> reviews) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < reviews.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            Review review = reviews.get(i);
            json.append('{');
            appendJsonField(json, "reviewId", review.getReviewId()).append(',');
            appendJsonField(json, "userId", review.getUserId()).append(',');
            appendJsonField(json, "bookId", review.getBookId()).append(',');
            appendJsonField(json, "userName", review.getUserName()).append(',');
            appendJsonField(json, "bookTitle", review.getBookTitle()).append(',');
            appendJsonField(json, "rating", review.getRating()).append(',');
            appendJsonField(json, "content", review.getContent()).append(',');
            appendJsonField(json, "createdAt", review.getCreatedAt());
            json.append('}');
        }
        json.append(']');
        return json.toString();
    }

    private String reservationsToJson(List<Reservation> reservations) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < reservations.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            Reservation reservation = reservations.get(i);
            json.append('{');
            appendJsonField(json, "reservationId", reservation.getReservationId()).append(',');
            appendJsonField(json, "userId", reservation.getUserId()).append(',');
            appendJsonField(json, "bookId", reservation.getBookId()).append(',');
            appendJsonField(json, "bookTitle", reservation.getBookTitle()).append(',');
            appendJsonField(json, "userName", reservation.getUserName()).append(',');
            appendJsonField(json, "status", reservation.getStatus()).append(',');
            appendJsonField(json, "createdAt", reservation.getCreatedAt()).append(',');
            appendJsonField(json, "notifiedAt", reservation.getNotifiedAt());
            json.append('}');
        }
        json.append(']');
        return json.toString();
    }

    private String roleRequestsToJson(List<RoleChangeRequest> requests) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < requests.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            RoleChangeRequest request = requests.get(i);
            json.append('{');
            appendJsonField(json, "requestId", request.getRequestId()).append(',');
            appendJsonField(json, "userId", request.getUserId()).append(',');
            appendJsonField(json, "studentNo", request.getStudentNo()).append(',');
            appendJsonField(json, "userName", request.getUserName()).append(',');
            appendJsonField(json, "currentLevel", request.getCurrentLevel()).append(',');
            appendJsonField(json, "targetLevel", request.getTargetLevel()).append(',');
            appendJsonField(json, "reason", request.getReason()).append(',');
            appendJsonField(json, "status", request.getStatus()).append(',');
            appendJsonField(json, "createdAt", request.getCreatedAt()).append(',');
            appendJsonField(json, "handledAt", request.getHandledAt()).append(',');
            appendJsonField(json, "handledBy", request.getHandledBy()).append(',');
            appendJsonField(json, "adminNote", request.getAdminNote());
            json.append('}');
        }
        json.append(']');
        return json.toString();
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> query = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return query;
        }
        for (String part : rawQuery.split("&")) {
            if (part.isBlank()) {
                continue;
            }
            int eq = part.indexOf('=');
            String key = eq >= 0 ? part.substring(0, eq) : part;
            String value = eq >= 0 ? part.substring(eq + 1) : "";
            query.put(decode(key), decode(value));
        }
        return query;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void sendJson(HttpExchange exchange, String json) throws IOException {
        sendJson(exchange, 200, json);
    }

    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        sendText(exchange, statusCode, json, "application/json; charset=UTF-8");
    }

    private void sendText(HttpExchange exchange, int statusCode, String text, String contentType) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private StringBuilder appendJsonField(StringBuilder json, String key, String value) {
        return json.append(jsonString(key)).append(':').append(jsonString(value));
    }

    private StringBuilder appendJsonField(StringBuilder json, String key, int value) {
        return json.append(jsonString(key)).append(':').append(value);
    }

    private StringBuilder appendJsonField(StringBuilder json, String key, long value) {
        return json.append(jsonString(key)).append(':').append(value);
    }

    private StringBuilder appendJsonField(StringBuilder json, String key, boolean value) {
        return json.append(jsonString(key)).append(':').append(value);
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        escaped.append('"');
        return escaped.toString();
    }
}
