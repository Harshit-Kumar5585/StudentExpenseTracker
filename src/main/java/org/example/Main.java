
package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Main {
    private static final String URL = "jdbc:mysql://localhost:3306/student_expense_tracker";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "Syntel@123";

    private static JFrame frame;
    private static JTextField amountField;
    private static JTextField descriptionField;
    private static JTextField dateField;
    private static JComboBox<String> categoryBox;
    private static JTable expenseTable;
    private static DefaultTableModel tableModel;
    private static JLabel totalLabel;
    private static int selectedExpenseId = -1;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            createGUI();
            loadExpenses();
        });
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    private static void createGUI() {
        frame = new JFrame("Student Expense Tracker");
        frame.setSize(1000, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

        JLabel heading = new JLabel("STUDENT EXPENSE TRACKER", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 28));
        heading.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 12, 12));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

        amountField = new JTextField();

        String[] categories = {
                "Food",
                "Transport",
                "Shopping",
                "College Supplies",
                "Entertainment",
                "Hostel",
                "Other"
        };
        categoryBox = new JComboBox<>(categories);

        descriptionField = new JTextField();
        dateField = new JTextField();

        JButton addButton = new JButton("Add Expense");
        JButton updateButton = new JButton("Update Expense");
        JButton deleteButton = new JButton("Delete Expense");
        JButton clearButton = new JButton("Clear");

        formPanel.add(new JLabel("Amount:"));
        formPanel.add(amountField);
        formPanel.add(new JLabel("Category:"));
        formPanel.add(categoryBox);
        formPanel.add(new JLabel("Description:"));
        formPanel.add(descriptionField);
        formPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        formPanel.add(dateField);
        formPanel.add(addButton);
        formPanel.add(updateButton);

        String[] columns = {"ID", "Amount", "Category", "Description", "Date"};

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        expenseTable = new JTable(tableModel);
        expenseTable.setRowHeight(28);

        JScrollPane scrollPane = new JScrollPane(expenseTable);

        totalLabel = new JLabel("Total Expenses: ₹0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 20));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomButtons.add(deleteButton);
        bottomButtons.add(clearButton);

        bottomPanel.add(totalLabel, BorderLayout.WEST);
        bottomPanel.add(bottomButtons, BorderLayout.EAST);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(heading, BorderLayout.NORTH);
        topPanel.add(formPanel, BorderLayout.CENTER);

        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);

        addButton.addActionListener(e -> addExpense());
        updateButton.addActionListener(e -> updateExpense());
        deleteButton.addActionListener(e -> deleteExpense());
        clearButton.addActionListener(e -> clearForm());

        expenseTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedExpense();
            }
        });

        frame.setVisible(true);
    }

    private static void addExpense() {
        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            if (amount <= 0) {
                JOptionPane.showMessageDialog(frame, "Amount must be greater than zero.");
                return;
            }
            String category = categoryBox.getSelectedItem().toString();
            String description = descriptionField.getText().trim();
            Date expenseDate = Date.valueOf(dateField.getText().trim());
            String sql = "INSERT INTO expenses (amount, category, description, expense_date) VALUES (?, ?, ?, ?)";
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setDouble(1, amount);
                statement.setString(2, category);
                statement.setString(3, description);
                statement.setDate(4, expenseDate);
                statement.executeUpdate();
            }
            JOptionPane.showMessageDialog(frame, "Expense added successfully!");
            clearForm();
            loadExpenses();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid amount.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid date in YYYY-MM-DD format.");
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private static void loadExpenses() {
        tableModel.setRowCount(0);
        double total = 0;
        String sql = "SELECT * FROM expenses ORDER BY expense_date DESC, expense_id DESC";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int id = resultSet.getInt("expense_id");
                double amount = resultSet.getDouble("amount");
                String category = resultSet.getString("category");
                String description = resultSet.getString("description");
                Date date = resultSet.getDate("expense_date");
                Object[] row = {
                        id,
                        String.format("%.2f", amount),
                        category,
                        description,
                        date
                };
                tableModel.addRow(row);
                total += amount;
            }
            totalLabel.setText(String.format("Total Expenses: ₹%.2f", total));
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private static void loadSelectedExpense() {
        int selectedRow = expenseTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }
        selectedExpenseId = Integer.parseInt(tableModel.getValueAt(selectedRow, 0).toString());
        amountField.setText(tableModel.getValueAt(selectedRow, 1).toString());
        categoryBox.setSelectedItem(tableModel.getValueAt(selectedRow, 2).toString());
        descriptionField.setText(tableModel.getValueAt(selectedRow, 3).toString());
        dateField.setText(tableModel.getValueAt(selectedRow, 4).toString());
    }

    private static void updateExpense() {
        if (selectedExpenseId == -1) {
            JOptionPane.showMessageDialog(frame, "Please select an expense from the table first.");
            return;
        }
        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            if (amount <= 0) {
                JOptionPane.showMessageDialog(frame, "Amount must be greater than zero.");
                return;
            }
            String category = categoryBox.getSelectedItem().toString();
            String description = descriptionField.getText().trim();
            Date expenseDate = Date.valueOf(dateField.getText().trim());
            String sql = "UPDATE expenses SET amount = ?, category = ?, description = ?, expense_date = ? WHERE expense_id = ?";
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setDouble(1, amount);
                statement.setString(2, category);
                statement.setString(3, description);
                statement.setDate(4, expenseDate);
                statement.setInt(5, selectedExpenseId);
                statement.executeUpdate();
            }
            JOptionPane.showMessageDialog(frame, "Expense updated successfully!");
            clearForm();
            loadExpenses();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid amount.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid date in YYYY-MM-DD format.");
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private static void deleteExpense() {
        if (selectedExpenseId == -1) {
            JOptionPane.showMessageDialog(frame, "Please select an expense from the table first.");
            return;
        }
        int confirmation = JOptionPane.showConfirmDialog(
                frame,
                "Are you sure you want to delete this expense?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );
        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }
        String sql = "DELETE FROM expenses WHERE expense_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, selectedExpenseId);
            statement.executeUpdate();
            JOptionPane.showMessageDialog(frame, "Expense deleted successfully!");
            clearForm();
            loadExpenses();
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private static void clearForm() {
        amountField.setText("");
        descriptionField.setText("");
        dateField.setText("");
        categoryBox.setSelectedIndex(0);
        selectedExpenseId = -1;
        expenseTable.clearSelection();
    }

    private static void showDatabaseError(SQLException ex) {
        JOptionPane.showMessageDialog(
                frame,
                "Database Error:\n" + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
        );
        ex.printStackTrace();
    }
}