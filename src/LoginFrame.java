import javax.swing.*;
import java.awt.*;
import java.sql.*;

class LoginFrame extends JFrame {
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);

    LoginFrame() {
        setTitle("HealthFirst PIMS - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(430, 300);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel outer = new JPanel(new BorderLayout(12, 12));
        outer.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));
        JLabel title = new JLabel("HealthFirst Pharmacy", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel subtitle = new JLabel("Inventory Management System", SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel heading = new JPanel(new GridLayout(2, 1, 3, 3));
        heading.add(title);
        heading.add(subtitle);

        JPanel fields = new JPanel(new GridLayout(2, 2, 10, 12));
        fields.add(new JLabel("Username:"));
        fields.add(usernameField);
        fields.add(new JLabel("Password:"));
        fields.add(passwordField);

        JButton loginButton = new JButton("Login");
        JButton exitButton = new JButton("Exit");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(exitButton);
        buttons.add(loginButton);

        outer.add(heading, BorderLayout.NORTH);
        outer.add(fields, BorderLayout.CENTER);
        outer.add(buttons, BorderLayout.SOUTH);
        setContentPane(outer);

        loginButton.addActionListener(e -> login());
        passwordField.addActionListener(e -> login());
        exitButton.addActionListener(e -> dispose());
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter both username and password.",
                    "Missing details", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "SELECT user_id, username, full_name, role FROM users WHERE username=? AND password=?";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, PasswordUtil.sha256(password));
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "Invalid username or password.",
                            "Login failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                AppUser user = new AppUser(rs.getInt("user_id"), rs.getString("username"),
                        rs.getString("full_name"), rs.getString("role"));
                dispose();
                if ("Admin".equalsIgnoreCase(user.role)) {
                    new AdminDashboard(user).setVisible(true);
                } else {
                    new CashierDashboard(user).setVisible(true);
                }
            }
        } catch (SQLException ex) {
            showDatabaseError(this, ex);
        }
    }

    static void showDatabaseError(Component parent, Exception ex) {
        JOptionPane.showMessageDialog(parent,
                "Could not complete the database operation.\n" + ex.getMessage()
                        + "\nCheck that MySQL is running and the database settings are correct.",
                "Database error", JOptionPane.ERROR_MESSAGE);
    }
}
