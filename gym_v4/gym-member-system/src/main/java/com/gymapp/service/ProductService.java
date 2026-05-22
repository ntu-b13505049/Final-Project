package com.gymapp.service;

import com.gymapp.dao.ProductDAO;
import com.gymapp.dao.SaleDAO;
import com.gymapp.database.DBConnection;
import com.gymapp.model.Product;
import com.gymapp.model.SaleRecord;
import com.gymapp.util.AppException;

import java.sql.*;
import java.util.List;

public class ProductService {
    private final ProductDAO productDAO = new ProductDAO();
    private final SaleDAO saleDAO = new SaleDAO();

    public ProductDAO getProductDAO() { return productDAO; }
    public SaleDAO getSaleDAO() { return saleDAO; }

    public List<Product> findProducts() throws SQLException {
        return productDAO.findAll();
    }

    public String sell(int productId, int quantity, Integer memberId, Integer soldBy, boolean useWallet) throws AppException {
        if (quantity <= 0) {
            throw new AppException("購買數量必須大於 0");
        }
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                ProductSnapshot product = lockProduct(con, productId);
                if (product.stock < quantity) {
                    throw new AppException("庫存不足，目前庫存：" + product.stock);
                }
                int total = product.unitPrice * quantity;
                if (useWallet) {
                    if (memberId == null) {
                        throw new AppException("使用會員錢包付款時必須輸入會員 ID");
                    }
                    int balance = WalletService.lockBalance(con, memberId);
                    if (balance < total) {
                        throw new AppException("會員錢包點數不足，目前餘額：" + balance + "，需付款：" + total);
                    }
                    WalletService.updateBalance(con, memberId, balance - total);
                    WalletService.insertTransaction(con, memberId, "商品購買", -total, "購買商品：" + product.name + " x " + quantity);
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE products SET stock=stock-? WHERE product_id=?")) {
                    ps.setInt(1, quantity);
                    ps.setInt(2, productId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO sales_history (member_id, product_id, quantity, total_amount, sold_by, timestamp) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)")) {
                    if (memberId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, memberId);
                    ps.setInt(2, productId);
                    ps.setInt(3, quantity);
                    ps.setInt(4, total);
                    if (soldBy == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, soldBy);
                    ps.executeUpdate();
                }
                con.commit();
                return "銷售完成，總額：" + total + (useWallet ? "（已由會員錢包扣點）" : "（現金/其他付款）");
            } catch (Exception e) {
                con.rollback();
                if (e instanceof AppException) throw (AppException) e;
                throw e;
            }
        } catch (SQLException e) {
            throw new AppException("商品銷售失敗", e);
        }
    }

    private ProductSnapshot lockProduct(Connection con, int productId) throws SQLException, AppException {
        try (PreparedStatement ps = con.prepareStatement("SELECT product_name, unit_price, stock FROM products WHERE product_id=?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new AppException("找不到商品 ID：" + productId);
                }
                return new ProductSnapshot(rs.getString("product_name"), rs.getInt("unit_price"), rs.getInt("stock"));
            }
        }
    }

    private static class ProductSnapshot {
        final String name;
        final int unitPrice;
        final int stock;
        ProductSnapshot(String name, int unitPrice, int stock) {
            this.name = name;
            this.unitPrice = unitPrice;
            this.stock = stock;
        }
    }
}
