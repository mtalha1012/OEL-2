package com.oel;

import java.io.IOException;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.stage.*;

public class OEL extends Application {
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/oel/GUI.fxml"));
        Scene scene = new Scene(loader.load());

        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}

//Product Class
class Product {
    private int productId;
    private String name;
    private double price;
    private int stock;
    private int reservedStock;

    public Product(int productId, String name, double price, int stock) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.reservedStock = 0;
    }

    public int getProductId() { return productId; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getAvailableStock() { return stock - reservedStock; }

    public void reserveStock(int q) { reservedStock += q; }
    public void releaseStock(int q) { reservedStock -= q; }

    public void confirmSale(int q) {
        stock -= q;
        reservedStock -= q;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s - PKR %.2f (Available: %d)", productId, name, price, getAvailableStock());
    }
}

//Cart Item
class CartItem {
    private Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}

//Product Catalog
class ProductCatalog {
    private List<Product> inventory = new ArrayList<>();

    public void addProduct(Product product) {
        inventory.add(product);
    }

    public Product getProduct(int id) throws ProductNotFoundException {
        for (Product p : inventory) {
            if (p.getProductId() == id) {
                return p;
            }
        }

        throw new ProductNotFoundException("Error: Product ID " + id + " does not exist in our catalog.");
    }

    public void displayCatalog() {
        System.out.println("\n Available Products: ");
        for (Product p : inventory) {
            System.out.println(p);
        }
    }

    public List<Product> getInventory() {
        return inventory;
    }
}

// Shopping Cart will only manange items to maintain the Single Responsibility Principle (SRP)
class ShoppingCart {
    private List<CartItem> items = new ArrayList<>();

    public void addProduct(Product product, int quantity) throws InvalidQuantityException, InsufficientStockException {
        if (quantity <= 0) throw new InvalidQuantityException("Quantity must be greater than zero.");

        // AVAILABLE STOCK (Not total)
        if (quantity > product.getAvailableStock())
            throw new InsufficientStockException("Only " + product.getAvailableStock() + " units available.");

        CartItem item = findItemById(product.getProductId());

        if (item != null)
            item.setQuantity(item.getQuantity() + quantity);
        else
            items.add(new CartItem(product, quantity));

        product.reserveStock(quantity);
    }

    public void removeProduct(int productId) throws ProductNotFoundException {
        CartItem item = findItemById(productId);
        if (item == null) throw new ProductNotFoundException("Product ID " + productId + " is not in your cart.");

        item.getProduct().releaseStock(item.getQuantity());
        items.remove(item);
    }

    public void updateQuantity(int productId, int newQuantity) throws ProductNotFoundException, InvalidQuantityException, InsufficientStockException {
        if (newQuantity <= 0) throw new InvalidQuantityException("Quantity must be greater than zero.");

        CartItem item = findItemById(productId);
        if (item == null) throw new ProductNotFoundException("Product ID " + productId + " is not in your cart.");

        int difference = newQuantity - item.getQuantity();

        if (difference > 0) {
            if (difference > item.getProduct().getAvailableStock())
                throw new InsufficientStockException("Not enough stock available to add " + difference + " more.");
            item.getProduct().reserveStock(difference);
        } else if (difference < 0)
            item.getProduct().releaseStock(-(difference));

        item.setQuantity(newQuantity);
    }

    public double calculateTotal() {
        double total = 0;
        for (CartItem item : items) {
            total += item.getProduct().getPrice() * item.getQuantity();
        }
        return total;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void clearCart() {
        items.clear();
    }

    private CartItem findItemById(int productId) {
        for (CartItem item : items) {
            if (item.getProduct().getProductId() == productId) return item;
        }
        return null;
    }
}

class OrderProcessor {

    public double calculateTotal(ShoppingCart cart) {
        double total = 0;
        for (CartItem item : cart.getItems()) {
            total += item.getProduct().getPrice() * item.getQuantity();
        }
        return total;
    }

    public Order checkout(ShoppingCart cart) throws EmptyCartException {
        if (cart.getItems().isEmpty())
            throw new EmptyCartException("Your shopping cart is empty.");

        double bill = calculateTotal(cart);

        Order newOrder = new Order(cart.getItems(), bill);

        return newOrder;
    }

    public void finalize(Order order, ShoppingCart cart) {
        for (CartItem item : order.getItems()) {
            item.getProduct().confirmSale(item.getQuantity());
        }
        cart.clearCart(); // Empty the physical basket
    }
}

class Order {
    private static int orderCounter = 1000;
    private int orderId;
    private List<CartItem> items;
    private double totalAmount;

    public Order(List<CartItem> items, double totalAmount) {
        this.orderId = ++orderCounter;
        this.items = new ArrayList<>(items); // Snapshot of items
        this.totalAmount = totalAmount;
    }

    public int getOrderId() { return orderId; }
    public List<CartItem> getItems() { return items; }
    public double getTotalAmount() { return totalAmount; }
}

interface PaymentGateway {
    boolean processPayment(double amount);
    String getMethodName();
}

class CashOnDelivery implements PaymentGateway {
    @Override
    public boolean processPayment(double amount) {
        if (amount >= 5000) {
            System.out.println("Cash On Delivery is not available for orders of PKR 5000 or more. Please use advance payment.");
            return false;
        }
        System.out.println("Order placed via Cash On Delivery. Please pay PKR " + amount + " upon arrival.");
        return true;
    }
    @Override
    public String getMethodName() { return "Cash On Delivery (COD)"; }
}

class CreditCard implements PaymentGateway {
    @Override
    public boolean processPayment(double amount) {
        System.out.println("Order placed via Credit Card. PKR " + amount + " paid.");
        return true;
    }
    @Override
    public String getMethodName() { return "Credit Card"; }
}

class EasyPaisa implements PaymentGateway {
    @Override
    public boolean processPayment(double amount) {
        System.out.println("Order placed via EasyPaisa. PKR " + amount + " paid.!");
        return true;
    }
    @Override
    public String getMethodName() { return "EasyPaisa"; }
}



class InputReader {
    private static InputReader instance;
    private Scanner sc;

    private InputReader() {
        sc = new Scanner(System.in);
    }

    public static InputReader getInstance() {
        if (instance == null) instance = new InputReader();
        return instance;
    }

    public int getInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                int input = sc.nextInt();
                sc.nextLine();
                return input;
            } catch (InputMismatchException e) {
                System.out.println("Please enter an integer.");
                sc.nextLine();
            }
        }
    }

    public int getInt(String prompt, int min, int max) {
        while (true) {
            int val = getInt(prompt);
            if (val >= min && val <= max) return val;
            System.out.println("Please enter an integer between " + min + " and " + max + ".");
        }
    }

    public double getDouble(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                double input = sc.nextDouble();
                sc.nextLine();
                return input;
            } catch (InputMismatchException e) {
                System.out.println("Please enter a numeric value.");
                sc.nextLine();
            }
        }
    }

    public double getDouble(String prompt, double min, double max) {
        while (true) {
            double val = getDouble(prompt);
            if (val >= min && val <= max) return val;
            System.out.println("Please enter a numeric value between " + min + " and " + max + ".");
        }
    }

    public String getString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine();
            if (!input.trim().isEmpty()) return input;
            System.out.println("Please enter a valid string.");
        }
    }

    public Date getDate(String prompt) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);
        while (true) {
            System.out.print(prompt);
            String dateStr = sc.nextLine();

            if (!dateStr.matches("\\d{2}/\\d{2}/\\d{4}")) {
                System.out.println("Incorrect format. Use dd/MM/yyyy (e.g., 17/04/2026).");
                continue;
            }
            try {
                return sdf.parse(dateStr);
            } catch (ParseException e) {
                System.out.println("Invalid date. Use a real calendar date.");
            }
        }
    }
}

// Custom Execption Classes
class ProductNotFoundException extends Exception {
    public ProductNotFoundException(String message) { super(message); }
}
class InsufficientStockException extends Exception {
    public InsufficientStockException(String message) { super(message); }
}
class InvalidQuantityException extends Exception {
    public InvalidQuantityException(String message) { super(message); }
}
class EmptyCartException extends Exception {
    public EmptyCartException(String message) { super(message); }
}
