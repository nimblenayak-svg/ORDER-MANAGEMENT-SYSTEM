import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class OrderManagementSystem {
    // Database credentials
    private static final String URL = "jdbc:mysql://localhost:3306/ecommerce_db";
    private static final String USER = "root";
    private static final String PASSWORD = "root"; // Change this

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            System.out.println("Connected to ecommerce_db successfully!");

            while (true) {
                System.out.println("\n--- E-COMMERCE ORDER MANAGEMENT SYSTEM ---");
                System.out.println("1. Insert Orders (Min 3 records)");
                System.out.println("2. View Orders by Status (Qty > 2, Sorted by Price DESC)");
                System.out.println("3. Increase Order Quantity");
                System.out.println("4. Update Order Status");
                System.out.println("5. Display Total Order Value & Delete Cancelled Order");
                System.out.println("6. Exit");
                System.out.print("Enter your choice: ");

                int choice = sc.nextInt();
                sc.nextLine(); // Consume newline

                switch (choice) {
                    case 1 -> insertOrders(conn);
                    case 2 -> viewFilteredOrders(conn);
                    case 3 -> increaseQuantity(conn);
                    case 4 -> updateOrderStatus(conn);
                    case 5 -> totalValueAndDeleteCancelled(conn);
                    case 6 -> {
                        System.out.println("Exiting application...");
                        return;
                    }
                    default -> System.out.println("Invalid choice! Try again.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database Connection Error: " + e.getMessage());
        }
    }

    // Operation (a): Insert at least 3 records using PreparedStatement & handle duplicate IDs
    private static void insertOrders(Connection conn) {
        System.out.print("How many orders do you want to insert (minimum 3)? ");
        int count = sc.nextInt();
        sc.nextLine();

        String sql = "INSERT INTO Orders (order_id, customer_name, product_name, quantity, unit_price, status) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 1; i <= count; i++) {
                System.out.println("\nEnter details for Order #" + i + ":");
                System.out.print("Order ID: ");
                int id = sc.nextInt();
                sc.nextLine();

                System.out.print("Customer Name: ");
                String customer = sc.nextLine();

                System.out.print("Product Name: ");
                String product = sc.nextLine();

                System.out.print("Quantity: ");
                int qty = sc.nextInt();

                System.out.print("Unit Price: ");
                double price = sc.nextDouble();
                sc.nextLine();

                System.out.print("Status (Pending / Shipped / Delivered / Cancelled): ");
                String status = sc.nextLine();

                pstmt.setInt(1, id);
                pstmt.setString(2, customer);
                pstmt.setString(3, product);
                pstmt.setInt(4, qty);
                pstmt.setDouble(5, price);
                pstmt.setString(6, status);

                try {
                    pstmt.executeUpdate();
                    System.out.println("Order ID " + id + " inserted successfully.");
                } catch (SQLIntegrityConstraintViolationException e) {
                    System.out.println("Error: Duplicate Order ID (" + id + "). Record skipped!");
                }
            }
        } catch (SQLException e) {
            System.out.println("Insert Error: " + e.getMessage());
        }
    }

    // Operation (b): Filter by status & quantity > 2, sorted by unit_price DESC
    private static void viewFilteredOrders(Connection conn) {
        System.out.print("Enter order status to filter: ");
        String status = sc.nextLine();

        String sql = "SELECT * FROM Orders WHERE status = ? AND quantity > 2 ORDER BY unit_price DESC";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();

            boolean found = false;
            System.out.println("\nID | Customer | Product | Qty | Unit Price | Status");
            System.out.println("------------------------------------------------------------");
            while (rs.next()) {
                found = true;
                System.out.printf("%d | %s | %s | %d | %.2f | %s%n",
                        rs.getInt("order_id"),
                        rs.getString("customer_name"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getDouble("unit_price"),
                        rs.getString("status"));
            }
            if (!found) {
                System.out.println("No matching orders found.");
            }
        } catch (SQLException e) {
            System.out.println("Query Error: " + e.getMessage());
        }
    }

    // Operation (c): Increase existing quantity for an order ID
    private static void increaseQuantity(Connection conn) {
        System.out.print("Enter Order ID: ");
        int id = sc.nextInt();
        System.out.print("Enter additional quantity to add: ");
        int addQty = sc.nextInt();
        sc.nextLine();

        String sql = "UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, addQty);
            pstmt.setInt(2, id);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Quantity updated successfully for Order ID " + id + ".");
            } else {
                System.out.println("Order ID " + id + " does not exist.");
            }
        } catch (SQLException e) {
            System.out.println("Update Error: " + e.getMessage());
        }
    }

    // Operation (d): Retrieve order, validate status, update & display updated details
    private static void updateOrderStatus(Connection conn) {
        System.out.print("Enter Order ID: ");
        int id = sc.nextInt();
        sc.nextLine();

        String checkSql = "SELECT * FROM Orders WHERE order_id = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("Order ID " + id + " does not exist.");
                return;
            }

            System.out.println("Current Order -> Customer: " + rs.getString("customer_name") +
                    ", Product: " + rs.getString("product_name") +
                    ", Status: " + rs.getString("status"));

            System.out.print("Enter new status (Pending / Shipped / Delivered): ");
            String newStatus = sc.nextLine();

            List<String> validStatuses = Arrays.asList("Pending", "Shipped", "Delivered");
            // Case-insensitive validation
            String matchedStatus = validStatuses.stream()
                    .filter(s -> s.equalsIgnoreCase(newStatus.trim()))
                    .findFirst()
                    .orElse(null);

            if (matchedStatus == null) {
                System.out.println("Invalid status! Allowed values: Pending, Shipped, Delivered.");
                return;
            }

            String updateSql = "UPDATE Orders SET status = ? WHERE order_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, matchedStatus);
                updateStmt.setInt(2, id);
                updateStmt.executeUpdate();
            }

            // Retrieve and display updated order details
            ResultSet updatedRs = checkStmt.executeQuery();
            if (updatedRs.next()) {
                System.out.println("\nUpdated Order Details:");
                System.out.printf("ID: %d | Customer: %s | Product: %s | Qty: %d | Price: %.2f | Status: %s%n",
                        updatedRs.getInt("order_id"),
                        updatedRs.getString("customer_name"),
                        updatedRs.getString("product_name"),
                        updatedRs.getInt("quantity"),
                        updatedRs.getDouble("unit_price"),
                        updatedRs.getString("status"));
            }
        } catch (SQLException e) {
            System.out.println("Status Update Error: " + e.getMessage());
        }
    }

    // Operation (e): Display SUM(quantity * unit_price) & delete if status is 'Cancelled'
    private static void totalValueAndDeleteCancelled(Connection conn) {
        String sumSql = "SELECT SUM(quantity * unit_price) AS total_value FROM Orders";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sumSql)) {
            if (rs.next()) {
                System.out.printf("Total Order Value across all orders: %.2f%n", rs.getDouble("total_value"));
            }
        } catch (SQLException e) {
            System.out.println("Calculation Error: " + e.getMessage());
        }

        System.out.print("Enter Order ID to delete: ");
        int id = sc.nextInt();
        sc.nextLine();

        String checkSql = "SELECT status FROM Orders WHERE order_id = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("Cannot delete: Order ID " + id + " does not exist.");
                return;
            }

            String currentStatus = rs.getString("status");
            if (!"Cancelled".equalsIgnoreCase(currentStatus)) {
                System.out.println("Cannot delete: Order status is '" + currentStatus + "'. Only 'Cancelled' orders can be deleted.");
                return;
            }

            String deleteSql = "DELETE FROM Orders WHERE order_id = ? AND status = 'Cancelled'";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, id);
                deleteStmt.executeUpdate();
                System.out.println("Cancelled Order ID " + id + " deleted successfully.");
            }
        } catch (SQLException e) {
            System.out.println("Delete Error: " + e.getMessage());
        }
    }
}