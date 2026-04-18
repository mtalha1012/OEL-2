package com.oel;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class GUI implements Initializable {

    // ── Product table ──────────────────────────────────────────────
    @FXML private TableView<Product>          productTable;
    @FXML private TableColumn<Product, Integer> colProdId;
    @FXML private TableColumn<Product, String>  colProdName;
    @FXML private TableColumn<Product, Double>  colProdPrice;
    @FXML private TableColumn<Product, Integer> colProdStock;

    // ── Cart table ─────────────────────────────────────────────────
    @FXML private TableView<CartItem>            cartTable;
    @FXML private TableColumn<CartItem, String>  colCartName;
    @FXML private TableColumn<CartItem, Integer> colCartQty;
    @FXML private TableColumn<CartItem, Double>  colCartPrice;
    @FXML private TableColumn<CartItem, Double>  colCartSub;

    // ── Input fields ───────────────────────────────────────────────
    @FXML private Spinner<Integer> quantityField;
    @FXML private Spinner<Integer> updateQtyField;

    // ── Labels ─────────────────────────────────────────────────────
    @FXML private Label totalLabel;
    @FXML private Label headerTotalLabel;
    @FXML private Label cartBadgeLabel;
    @FXML private Label statusLabel;

    // ── Backend objects ────────────────────────────────────────────
    private ProductCatalog catalog;
    private ShoppingCart   cart;
    private OrderProcessor processor;

    // Observable lists — bound to the TableViews
    private ObservableList<Product>  productObsList;
    private ObservableList<CartItem> cartObsList;

    //  Initialization

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        catalog   = new ProductCatalog();
        cart      = new ShoppingCart();
        processor = new OrderProcessor();

        quantityField.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        updateQtyField.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));

        seedCatalog();
        initProductTable();
        initCartTable();
    }

    /** Populate the catalog with demo products. */
    private void seedCatalog() {
        catalog.addProduct(new Product(1,  "Samsung Galaxy A55",  89999, 15));
        catalog.addProduct(new Product(2,  "Apple AirPods Pro",   45000,  8));
        catalog.addProduct(new Product(3,  "Nike Air Max 270",    18500, 20));
        catalog.addProduct(new Product(4,  "Levi's 511 Slim Fit",  7500, 30));
        catalog.addProduct(new Product(5,  "Nescafé Gold 200g",    2200, 50));
        catalog.addProduct(new Product(6,  "Harry Potter Box Set", 4800, 12));
        catalog.addProduct(new Product(7,  "PlayStation 5 Pad",   12000,  6));
        catalog.addProduct(new Product(8,  "Anker Power Bank",     5500, 18));
    }

    // ── Product TableView ──────────────────────────────────────────

    private void initProductTable() {
        colProdId.setCellValueFactory(
                cd -> new SimpleObjectProperty<>(cd.getValue().getProductId()));

        colProdName.setCellValueFactory(
                cd -> new SimpleStringProperty(cd.getValue().getName()));

        colProdPrice.setCellValueFactory(
                cd -> new SimpleObjectProperty<>(cd.getValue().getPrice()));

        colProdStock.setCellValueFactory(
                cd -> new SimpleObjectProperty<>(cd.getValue().getAvailableStock()));

        // Format price column
        colProdPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null
                        : String.format("PKR %,.0f", price));
            }
        });

        // Colour stock red if low (≤ 3)
        colProdStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) { setText(null); setStyle(""); return; }
                setText(String.valueOf(stock));
            }
        });

        productObsList = FXCollections.observableArrayList(catalog.getInventory());
        productTable.setItems(productObsList);
    }

    // ── Cart TableView ─────────────────────────────────────────────

    private void initCartTable() {
        colCartName.setCellValueFactory(
                cd -> new SimpleStringProperty(cd.getValue().getProduct().getName()));

        colCartQty.setCellValueFactory(
                cd -> new SimpleObjectProperty<>(cd.getValue().getQuantity()));

        colCartPrice.setCellValueFactory(
                cd -> new SimpleObjectProperty<>(cd.getValue().getProduct().getPrice()));

        colCartSub.setCellValueFactory(
                cd -> new SimpleObjectProperty<>(
                        cd.getValue().getProduct().getPrice() * cd.getValue().getQuantity()));

        colCartPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("PKR %,.0f", v));
            }
        });

        // Subtotal in amber
        colCartSub.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(String.format("PKR %,.0f", v));
            }
        });

        cartObsList = FXCollections.observableArrayList();
        cartTable.setItems(cartObsList);
    }

    // ══════════════════════════════════════════════════════════════
    //  Button Handlers
    // ══════════════════════════════════════════════════════════════

    /** Add selected product to cart. */
    @FXML
    private void handleAddToCart() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("⚠  Select a product from the catalog first.", true);
            return;
        }

        int qty = quantityField.getValue();

        try {
            cart.addProduct(selected, qty);
            refreshCart();
            productTable.refresh(); // reflect updated available stock
            setStatus("✅  Added " + qty + "×  " + selected.getName() + " to cart.", false);
            quantityField.getValueFactory().setValue(1);
        } catch (InvalidQuantityException | InsufficientStockException e) {
            setStatus("⚠  " + e.getMessage(), true);
        }
    }

    /** Remove selected cart item. */
    @FXML
    private void handleRemove() {
        CartItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("⚠  Select a cart item to remove.", true);
            return;
        }

        try {
            cart.removeProduct(selected.getProduct().getProductId());
            refreshCart();
            productTable.refresh();
            setStatus("🗑  Removed " + selected.getProduct().getName() + " from cart.", false);
        } catch (ProductNotFoundException e) {
            setStatus("⚠  " + e.getMessage(), true);
        }
    }

    /** Update quantity of selected cart item. */
    @FXML
    private void handleUpdate() {
        CartItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("⚠  Select a cart item to update.", true);
            return;
        }

        int qty = updateQtyField.getValue();

        try {
            cart.updateQuantity(selected.getProduct().getProductId(), qty);
            refreshCart();
            productTable.refresh();
            setStatus("↺  Updated " + selected.getProduct().getName() + " quantity to " + qty + ".", false);
            updateQtyField.getValueFactory().setValue(1);
        } catch (ProductNotFoundException | InvalidQuantityException | InsufficientStockException e) {
            setStatus("⚠  " + e.getMessage(), true);
        }
    }

    /** Validate cart and open payment dialog. */
    @FXML
    private void handleCheckout() {
        try {
            Order order = processor.checkout(cart);   // throws EmptyCartException
            showPaymentDialog(order);
        } catch (EmptyCartException e) {
            setStatus("⚠  " + e.getMessage(), true);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Payment Dialog
    // ══════════════════════════════════════════════════════════════

    private void showPaymentDialog(Order order) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(productTable.getScene().getWindow());
        dialog.setTitle("Payment Gateway — Order #" + order.getOrderId());
        dialog.setResizable(false);

        VBox root = new VBox(16);
        root.setStyle("payment-dialog");
        root.setPrefWidth(420);

        boolean needsAdvance = order.getTotalAmount() >= 5000;

        // ── Header labels ──
        Label titleLbl = new Label("Select Payment Method");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: ghostwhite;");

        Label amtLbl = new Label("Total:  PKR " + String.format("%,.2f", order.getTotalAmount()));
        amtLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: goldenrod;");

        Label noticeLbl = new Label(needsAdvance
                ? "⚠  Total ≥ PKR 5,000 — Cash on Delivery is not available."
                : "✅  Eligible for any payment method.");
        noticeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: "
                + (needsAdvance ? "orange" : "mediumseagreen") + ";");
        noticeLbl.setWrapText(true);

        Separator sep = new Separator();
        sep.setStyle(".separator *.line { -fx-border-color: dimgray; }");

        // ── Payment options ──
        List<PaymentGateway> gateways = Arrays.asList(
                new CashOnDelivery(),
                new CreditCard(),
                new EasyPaisa()
        );

        ToggleGroup tg = new ToggleGroup();
        VBox optBox = new VBox(8);

        for (PaymentGateway gw : gateways) {
            boolean isCOD    = gw instanceof CashOnDelivery;
            boolean disabled = isCOD && needsAdvance;

            RadioButton rb = new RadioButton(gw.getMethodName());
            rb.setToggleGroup(tg);
            rb.setDisable(disabled);
            rb.setMaxWidth(Double.MAX_VALUE);
            rb.setUserData(gw);
            rb.setStyle(
                    "-fx-text-fill: " + (disabled ? "slategray" : "ghostwhite") + ";"
                            + "-fx-font-size: 13px;"
                            + "-fx-padding: 12 16;"
                            + "-fx-background-color: darkslateblue;"
                            + "-fx-background-radius: 6;"
            );
            optBox.getChildren().add(rb);
        }

        // Auto-select first enabled option
        tg.getToggles().stream()
                .filter(t -> !((RadioButton) t).isDisable())
                .findFirst()
                .ifPresent(t -> t.setSelected(true));

        // ── Confirm button ──
        Button confirmBtn = new Button("✅   Confirm & Place Order");
        confirmBtn.setMaxWidth(Double.MAX_VALUE);
        confirmBtn.setStyle(
                "-fx-background-color: mediumseagreen;"
                        + "-fx-text-fill: black;"
                        + "-fx-font-weight: bold;"
                        + "-fx-font-size: 14px;"
                        + "-fx-padding: 12 0;"
                        + "-fx-background-radius: 8;"
                        + "-fx-cursor: hand;"
        );

        confirmBtn.setOnAction(e -> {
            Toggle selected = tg.getSelectedToggle();
            if (selected == null) { return; }

            PaymentGateway chosen = (PaymentGateway) selected.getUserData();
            boolean ok = chosen.processPayment(order.getTotalAmount());

            if (ok) {
                processor.finalize(order, cart);
                refreshCart();
                productTable.refresh();
                dialog.close();

                showAlert(
                        "🎉  Order #" + order.getOrderId() + " Placed!\n\n"
                                + "Payment via : " + chosen.getMethodName() + "\n"
                                + "Amount       : PKR " + String.format("%,.2f", order.getTotalAmount())
                                + "\n\nThank you for shopping with ShopEase!"
                );
                setStatus("🎉  Order #" + order.getOrderId()
                        + " placed via " + chosen.getMethodName() + ".", false);

            } else {
                // COD refused for large amount
                showAlert("❌  " + chosen.getMethodName()
                        + " is not available for this order total.\nPlease choose another method.");
            }
        });

        root.getChildren().addAll(titleLbl, amtLbl, noticeLbl, sep, optBox, confirmBtn);

        dialog.setScene(new Scene(root));
        dialog.show();
    }

    // ══════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════

    /** Sync the cart ObservableList and update all summary labels. */
    private void refreshCart() {
        cartObsList.setAll(cart.getItems());

        double total = cart.calculateTotal();
        totalLabel.setText(String.format("PKR %,.2f", total));
        headerTotalLabel.setText(String.format("PKR %,.0f", total));

        int n = cart.getItems().size();
        cartBadgeLabel.setText(n + " item" + (n != 1 ? "s" : ""));
    }

    /** Set the bottom status bar text and colour. */
    private void setStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: tomato; -fx-font-size: 11px;"
                : "-fx-text-fill: mediumseagreen; -fx-font-size: 11px;");
    }

    /** Simple information alert, dark-styled. */
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ShopEase");
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dp = alert.getDialogPane();
        dp.setStyle("-fx-background-color: midnightblue;");

        // Style the text node if accessible
        try {
            dp.lookup(".content.label")
                    .setStyle("-fx-text-fill: ghostwhite; -fx-font-size: 13px;");
        } catch (Exception ignored) {}

        alert.showAndWait();
    }
}
