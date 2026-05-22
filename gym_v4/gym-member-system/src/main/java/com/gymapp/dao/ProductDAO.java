package com.gymapp.dao;

import com.gymapp.database.Db;
import com.gymapp.model.Product;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ProductDAO {
    public List<Product> findAll() throws SQLException {
        return Db.query("SELECT * FROM products ORDER BY product_id", null, this::map);
    }

    public Optional<Product> findById(int id) throws SQLException {
        return Db.queryOne("SELECT * FROM products WHERE product_id=?", ps -> ps.setInt(1, id), this::map);
    }

    public int insert(Product p) throws SQLException {
        return Db.insertAndReturnKey("INSERT INTO products (product_name, category, unit_price, stock) VALUES (?, ?, ?, ?)", ps -> {
            ps.setString(1, p.getProductName());
            ps.setString(2, p.getCategory());
            ps.setInt(3, p.getUnitPrice());
            ps.setInt(4, p.getStock());
        });
    }

    public void update(Product p) throws SQLException {
        Db.update("UPDATE products SET product_name=?, category=?, unit_price=?, stock=? WHERE product_id=?", ps -> {
            ps.setString(1, p.getProductName());
            ps.setString(2, p.getCategory());
            ps.setInt(3, p.getUnitPrice());
            ps.setInt(4, p.getStock());
            ps.setInt(5, p.getProductId());
        });
    }

    public void delete(int id) throws SQLException {
        Db.update("DELETE FROM products WHERE product_id=?", ps -> ps.setInt(1, id));
    }

    private Product map(ResultSet rs) throws SQLException {
        return new Product(rs.getInt("product_id"), rs.getString("product_name"), rs.getString("category"), rs.getInt("unit_price"), rs.getInt("stock"));
    }
}
