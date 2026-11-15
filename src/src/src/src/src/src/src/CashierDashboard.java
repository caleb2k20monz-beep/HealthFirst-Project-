import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

class CashierDashboard extends JFrame {
    private final AppUser user;
    private final JTextField search = new JTextField(18);
    private final DefaultTableModel stockModel = UIUtil.model(new String[]{"ID", "Medicine", "Price", "Available", "Expiry"});
    private final JTable stockTable = UIUtil.table(stockModel);
    private final DefaultTableModel cartModel = UIUtil.model(new String[]{"Medicine ID", "Medicine", "Qty", "Unit price", "Line total"});
    private final JTable cartTable = UIUtil.table(cartModel);
    private final Map<Integer, CartItem> cart = new LinkedHashMap<>();
    private final JLabel totalLabel = new JLabel("Total: R 0.00");

    CashierDashboard(AppUser user) {
        this.user = user;
        setTitle("HealthFirst PIMS - Cashier POS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650); setLocationRelativeTo(null);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Cashier: " + user.fullName)); top.add(new JLabel("Search medicine:")); top.add(search);
        JButton find = UIUtil.button("Search"); JButton showAll = UIUtil.button("Show all");
        top.add(find); top.add(showAll);

        JPanel left = new JPanel(new BorderLayout(5, 5));
        left.setBorder(BorderFactory.createTitledBorder("Stock check / select medicine"));
        left.add(new JScrollPane(stockTable), BorderLayout.CENTER);
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = UIUtil.button("Add selected to cart");
        addPanel.add(add); left.add(addPanel, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout(5, 5));
        right.setBorder(BorderFactory.createTitledBorder("Current sale"));
        right.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        JPanel cartButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton remove = UIUtil.button("Remove selected"); JButton clear = UIUtil.button("Clear cart");
        JButton checkout = UIUtil.button("Checkout / Generate bill");
        cartButtons.add(remove); cartButtons.add(clear); cartButtons.add(checkout);
        JPanel cartSouth = new JPanel(new BorderLayout());
        cartSouth.add(totalLabel, BorderLayout.NORTH); cartSouth.add(cartButtons, BorderLayout.CENTER);
        right.add(cartSouth, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.5);
        JButton logout = UIUtil.button("Logout");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT)); bottom.add(logout);

        add(top, BorderLayout.NORTH); add(split, BorderLayout.CENTER); add(bottom, BorderLayout.SOUTH);
        find.addActionListener(e -> loadStock(search.getText().trim()));
        search.addActionListener(e -> loadStock(search.getText().trim()));
        showAll.addActionListener(e -> { search.setText(""); loadStock(""); });
        add.addActionListener(e -> addSelected());
        remove.addActionListener(e -> removeSelected());
        clear.addActionListener(e -> clearCart());
        checkout.addActionListener(e -> checkout());
        logout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        loadStock("");
    }

    private void loadStock(String term) {
        stockModel.setRowCount(0);
        String sql = "SELECT medicine_id,name,price,quantity_in_stock,expiry_date FROM medicines " +
                "WHERE name LIKE ? ORDER BY name";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + term + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) stockModel.addRow(new Object[]{rs.getInt(1), rs.getString(2),
                        rs.getBigDecimal(3), rs.getInt(4), rs.getDate(5)});
            }
        } catch (SQLException ex) { LoginFrame.showDatabaseError(this, ex); }
    }

    private void addSelected() {
        Integer id = UIUtil.selectedId(stockTable, stockModel);
        if (id == null) { UIUtil.error(this, "Select a medicine first."); return; }
        int row = stockTable.convertRowIndexToModel(stockTable.getSelectedRow());
        String name = stockModel.getValueAt(row, 1).toString();
        BigDecimal price = new BigDecimal(stockModel.getValueAt(row, 2).toString());
        int available = Integer.parseInt(stockModel.getValueAt(row, 3).toString());
        if (available <= 0) { UIUtil.error(this, "This medicine is out of stock."); return; }
        String input = JOptionPane.showInputDialog(this, "Quantity to add (available: " + available + "):", "1");
        if (input == null) return;
        try {
            int qty = Integer.parseInt(input.trim());
            if (qty <= 0) throw new NumberFormatException();
            CartItem old = cart.get(id);
            int newQty = qty + (old == null ? 0 : old.quantity);
            if (newQty > available) { UIUtil.error(this, "Requested quantity exceeds stock available."); return; }
            if (old == null) cart.put(id, new CartItem(id, name, qty, price));
            else old.quantity = newQty;
            refreshCart();
        } catch (NumberFormatException ex) { UIUtil.error(this, "Quantity must be a positive whole number."); }
    }

    private void removeSelected() {
        Integer id = UIUtil.selectedId(cartTable, cartModel);
        if (id == null) { UIUtil.error(this, "Select a cart item to remove."); return; }
        cart.remove(id); refreshCart();
    }

    private void clearCart() { cart.clear(); refreshCart(); }

    private void refreshCart() {
        cartModel.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.values()) {
            BigDecimal line = item.price.multiply(BigDecimal.valueOf(item.quantity));
            total = total.add(line);
            cartModel.addRow(new Object[]{item.id, item.name, item.quantity, item.price, line});
        }
        totalLabel.setText("Total: R " + total.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    private void checkout() {
        if (cart.isEmpty()) { UIUtil.error(this, "The cart is empty."); return; }
        StringBuilder bill = new StringBuilder();
        BigDecimal total = BigDecimal.ZERO;
        bill.append("HEALTHFIRST PHARMACY\n");
        bill.append("Sale date: ").append(new java.util.Date()).append("\n");
        bill.append("Cashier: ").append(user.fullName).append("\n");
        bill.append("--------------------------------------------------\n");
        bill.append(String.format("%-22s %5s %10s %10s%n", "Medicine", "Qty", "Price", "Total"));
        for (CartItem item : cart.values()) {
            BigDecimal line = item.price.multiply(BigDecimal.valueOf(item.quantity));
            total = total.add(line);
            bill.append(String.format("%-22s %5d %10s %10s%n",
                    truncate(item.name, 22), item.quantity, item.price.toPlainString(), line.toPlainString()));
        }
        bill.append("--------------------------------------------------\n");
        bill.append("TOTAL: R ").append(total.setScale(2, java.math.RoundingMode.HALF_UP)).append("\n");
        bill.append("Thank you for shopping at HealthFirst!\n");

        int confirm = JOptionPane.showConfirmDialog(this, "Complete sale for R " + total + "?",
                "Confirm checkout", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection c = DB.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Validate current stock inside the transaction to prevent selling unavailable quantities.
                for (CartItem item : cart.values()) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT quantity_in_stock FROM medicines WHERE medicine_id=? FOR UPDATE")) {
                        ps.setInt(1, item.id);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next() || rs.getInt(1) < item.quantity) {
                                throw new SQLException("Insufficient stock for " + item.name + ". Refresh stock and retry.");
                            }
                        }
                    }
                }
                int saleId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO sales(total_amount,user_id) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setBigDecimal(1, total); ps.setInt(2, user.id); ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Could not create sale record.");
                        saleId = keys.getInt(1);
                    }
                }
                for (CartItem item : cart.values()) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO sale_items(sale_id,medicine_id,quantity_sold,price_at_sale) VALUES(?,?,?,?)")) {
                        ps.setInt(1, saleId); ps.setInt(2, item.id); ps.setInt(3, item.quantity); ps.setBigDecimal(4, item.price);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE medicines SET quantity_in_stock=quantity_in_stock-? WHERE medicine_id=? AND quantity_in_stock>=?")) {
                        ps.setInt(1, item.quantity); ps.setInt(2, item.id); ps.setInt(3, item.quantity);
                        if (ps.executeUpdate() != 1) throw new SQLException("Stock changed during checkout. Please retry.");
                    }
                }
                c.commit();
                showBill(bill.toString(), saleId);
                clearCart();
                loadStock(search.getText().trim());
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (Exception ex) {
            LoginFrame.showDatabaseError(this, ex);
        }
    }

    private void showBill(String text, int saleId) {
        JTextArea area = new JTextArea(text, 22, 58);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setEditable(false);
        JButton save = UIUtil.button("Save bill to text file");
        JButton print = UIUtil.button("Print bill");
        JButton close = UIUtil.button("Close");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(save); buttons.add(print); buttons.add(close);
        JDialog dialog = new JDialog(this, "Bill - Sale #" + saleId, true);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.add(new JScrollPane(area), BorderLayout.CENTER); dialog.add(buttons, BorderLayout.SOUTH);
        save.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("HealthFirst_Bill_" + saleId + ".txt"));
            if (chooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                try (Writer writer = new FileWriter(chooser.getSelectedFile())) {
                    writer.write(text); UIUtil.info(dialog, "Bill saved.");
                } catch (IOException ex) { UIUtil.error(dialog, "Could not save bill: " + ex.getMessage()); }
            }
        });
        print.addActionListener(e -> {
            try { area.print(); } catch (Exception ex) { UIUtil.error(dialog, "Could not print bill: " + ex.getMessage()); }
        });
        close.addActionListener(e -> dialog.dispose());
        dialog.pack(); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
    }

    private static String truncate(String text, int length) {
        return text.length() <= length ? text : text.substring(0, length - 1);
    }

    private static class CartItem {
        final int id; final String name; int quantity; final BigDecimal price;
        CartItem(int id, String name, int quantity, BigDecimal price) {
            this.id = id; this.name = name; this.quantity = quantity; this.price = price;
        }
    }
}
