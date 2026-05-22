package com.gymapp.view.panel;

import com.gymapp.model.*;
import com.gymapp.service.ProductService;
import com.gymapp.util.DateTimeUtil;
import com.gymapp.util.UiUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ProductPanel extends BasePanel {
    private final User user;
    private final ProductService productService = new ProductService();
    private final DefaultTableModel productModel = UiUtil.readOnlyModel(new String[]{"商品ID", "品名", "類別", "單價/點數", "庫存"});
    private final DefaultTableModel saleModel = UiUtil.readOnlyModel(new String[]{"銷售ID", "會員ID", "商品ID", "數量", "總額", "銷售員ID", "時間"});
    private final JTable productTable = new JTable(productModel);
    private final JTable saleTable = new JTable(saleModel);
    private final JTextField idField = new JTextField(8);
    private final JTextField nameField = new JTextField(14);
    private final JTextField categoryField = new JTextField(12);
    private final JTextField priceField = new JTextField("100", 8);
    private final JTextField stockField = new JTextField("10", 8);
    private final JTextField memberIdField = new JTextField(8);
    private final JTextField quantityField = new JTextField("1", 5);
    private final JCheckBox useWalletBox = new JCheckBox("使用會員錢包扣點", true);

    public ProductPanel(User user) {
        this.user = user;
        buildUi();
        refreshData();
    }

    private void buildUi() {
        if (user.hasRole(Role.MEMBER)) {
            memberIdField.setText(String.valueOf(user.getId()));
            memberIdField.setEditable(false);
            useWalletBox.setSelected(true);
            useWalletBox.setEnabled(false);
        }
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.62);
        JPanel p1 = new JPanel(new BorderLayout());
        p1.setBorder(BorderFactory.createTitledBorder("商品清單"));
        p1.add(scroll(productTable), BorderLayout.CENTER);
        JPanel p2 = new JPanel(new BorderLayout());
        p2.setBorder(BorderFactory.createTitledBorder("銷售紀錄"));
        p2.add(scroll(saleTable), BorderLayout.CENTER);
        split.setTopComponent(p1);
        split.setBottomComponent(p2);
        add(split, BorderLayout.CENTER);

        JPanel form = UiUtil.formPanel();
        idField.setEditable(false);
        UiUtil.addField(form, 0, "商品ID", idField);
        UiUtil.addField(form, 1, "品名", nameField);
        UiUtil.addField(form, 2, "類別", categoryField);
        UiUtil.addField(form, 3, "單價/點數", priceField);
        UiUtil.addField(form, 4, "庫存", stockField);
        UiUtil.addField(form, 5, "購買會員ID", memberIdField);
        UiUtil.addField(form, 6, "數量", quantityField);
        UiUtil.addField(form, 7, "付款", useWalletBox);
        JButton add = new JButton("新增商品");
        JButton update = new JButton("修改商品");
        JButton delete = new JButton("刪除商品");
        JButton sell = new JButton(user.hasRole(Role.MEMBER) ? "購買選取商品" : "銷售選取商品");
        JButton refresh = new JButton("刷新");
        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        if (!user.hasRole(Role.MEMBER)) {
            buttons.add(add); buttons.add(update); buttons.add(delete);
        }
        buttons.add(sell); buttons.add(refresh);
        JPanel east = new JPanel(new BorderLayout());
        east.setBorder(BorderFactory.createTitledBorder("商品販售"));
        east.add(form, BorderLayout.CENTER);
        east.add(buttons, BorderLayout.SOUTH);
        add(east, BorderLayout.EAST);

        productTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && productTable.getSelectedRow() >= 0) fillFromSelected(); });
        add.addActionListener(e -> addProduct());
        update.addActionListener(e -> updateProduct());
        delete.addActionListener(e -> deleteProduct());
        sell.addActionListener(e -> sellProduct());
        refresh.addActionListener(e -> refreshData());
    }

    private void fillFromSelected() {
        int r = productTable.convertRowIndexToModel(productTable.getSelectedRow());
        idField.setText(String.valueOf(productModel.getValueAt(r, 0)));
        nameField.setText(String.valueOf(productModel.getValueAt(r, 1)));
        categoryField.setText(String.valueOf(productModel.getValueAt(r, 2)));
        priceField.setText(String.valueOf(productModel.getValueAt(r, 3)));
        stockField.setText(String.valueOf(productModel.getValueAt(r, 4)));
    }

    private Product readProduct(boolean needId) {
        Product p = new Product();
        if (needId) p.setProductId(UiUtil.intValue(idField.getText(), "商品ID"));
        p.setProductName(nameField.getText().trim());
        p.setCategory(categoryField.getText().trim());
        p.setUnitPrice(UiUtil.intValue(priceField.getText(), "單價"));
        p.setStock(UiUtil.intValue(stockField.getText(), "庫存"));
        if (p.getProductName().isBlank()) throw new IllegalArgumentException("品名不可空白");
        return p;
    }

    private void addProduct() { try { productService.getProductDAO().insert(readProduct(false)); refreshData(); } catch (Exception e) { showError(e); } }
    private void updateProduct() { try { productService.getProductDAO().update(readProduct(true)); refreshData(); } catch (Exception e) { showError(e); } }
    private void deleteProduct() { try { int id = selectedId(productTable, 0); if (UiUtil.confirm(this, "確定刪除商品 ID " + id + "？")) { productService.getProductDAO().delete(id); refreshData(); } } catch (Exception e) { showError(e); } }

    private void sellProduct() {
        try {
            int productId = selectedId(productTable, 0);
            int qty = UiUtil.intValue(quantityField.getText(), "數量");
            Integer memberId = UiUtil.nullableInt(memberIdField.getText());
            Integer soldBy = user.hasRole(Role.MEMBER) ? null : user.getId();
            String msg = productService.sell(productId, qty, memberId, soldBy, useWalletBox.isSelected());
            UiUtil.info(this, msg);
            refreshData();
        } catch (Exception e) { showError(e); }
    }

    @Override
    public void refreshData() {
        try {
            List<Object[]> products = new ArrayList<>();
            for (Product p : productService.findProducts()) {
                products.add(new Object[]{p.getProductId(), p.getProductName(), p.getCategory(), p.getUnitPrice(), p.getStock()});
            }
            setRows(productModel, products);
            List<Object[]> sales = new ArrayList<>();
            for (SaleRecord s : productService.getSaleDAO().findAll()) {
                sales.add(new Object[]{s.getSaleId(), s.getMemberId() == null ? "" : s.getMemberId(), s.getProductId(), s.getQuantity(), s.getTotalAmount(), s.getSoldBy() == null ? "" : s.getSoldBy(), DateTimeUtil.format(s.getTimestamp())});
            }
            setRows(saleModel, sales);
        } catch (Exception e) { showError(e); }
    }
}
