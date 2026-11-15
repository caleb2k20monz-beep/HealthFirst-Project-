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

class AdminDashboard extends JFrame {
    private final AppUser user;

    AdminDashboard(AppUser user) {
        this.user = user;
        setTitle("HealthFirst PIMS - Administrator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 680);
        setLocationRelativeTo(null);

        JLabel welcome = new JLabel("  Administrator: " + user.fullName + " (" + user.username + ")");
        welcome.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 6));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Medicines", new MedicinePanel());
        tabs.addTab("Suppliers", new SupplierPanel());
        tabs.addTab("Users", new UserPanel(user));
        tabs.addTab("Reports", new ReportsPanel());

        JButton logout = UIUtil.button("Logout");
        logout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(logout);

        add(welcome, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }
}
