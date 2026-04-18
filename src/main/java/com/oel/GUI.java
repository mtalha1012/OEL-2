package com.oel;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class GUI implements Initializable {

    @FXML private TableView<Product>            productTable;
    @FXML private TableColumn<Product, Integer> colProdId;
    @FXML private TableColumn<Product, String>  colProdName;
    @FXML private TableColumn<Product, Double>  colProdPrice;
    @FXML private TableColumn<Product, Integer> colProdStock;

    @FXML private TableView<CartItem>            cartTable;
    @FXML private TableColumn<CartItem, String>  colCartName;
    @FXML private TableColumn<CartItem, Integer> colCartQty;
    @FXML private TableColumn<CartItem, Double>  colCartPrice;
    @FXML private TableColumn<CartItem, Double>  colCartSub;

    @FXML private Spinner<Integer> quantityField;
    @FXML private Spinner<Integer> updateQtyField;

    @FXML private Label totalLabel;
    @FXML private Label headerTotalLabel;
    @FXML private Label cartBadgeLabel;
    @FXML private Label statusLabel;

    private ProductCatalog catalog;
    private ShoppingCart   cart;
    private OrderProcessor processor;

    private ObservableList<Product>  productObsList;
    private ObservableList<CartItem> cartObsList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        catalog   = new ProductCatalog();
        cart      = new ShoppingCart();
        processor = new OrderProcessor();

        quantityField.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        updateQtyField.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));

        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        seedCatalog();
        initProductTable();
        initCartTable();
    }

    private void seedCatalog() {
        catalog.addProduct(new Product(1, "Samsung Galaxy A55",  89999, 15));
        catalog.addProduct(new Product(2, "Apple AirPods Pro",   45000,  8));
        catalog.addProduct(new Product(3, "Nike Air Max 270",    18500, 20));
        catalog.addProduct(new Product(4, "Levi's 511 Slim Fit",  7500, 30));
        catalog.addProduct(new Product(5, "Nescafe Gold 200g",    2200, 50));
        catalog.addProduct(new Product(6, "Harry Potter Box Set", 4800, 12));
        catalog.addProduct(new Product(7, "PlayStation 5 Pad",   12000,  6));
        catalog.addProduct(new Product(8, "Anker Power Bank",     5500, 18));
    }

    private void initProductTable() {
        colProdId.setCellValueFactory(
                cd -> new SimpleObjectProperty<>(cd.getValue().getProductId()));
        colProdName.setCellValueFactory(
                cd -> new SimpleStringProperty(cd.getValue().getName()));
        colProdPrice.setCellValueFactory(
                cd -> new SimpleObjectProperty<>(cd.getValue().getPrice()));
        colProdStock.setCellValueFactory(
                cd -> new SimpleObjectProperty<>(cd.getValue().getAvailableStock()));

        colProdPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("PKR %,.0f", price));
            }
        });

        // stock-low (red) if <= 3, stock-ok (green) otherwise
        colProdStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                getStyleClass().removeAll("stock-low", "stock-ok");
                if (empty || stock == null) { setText(null); return; }
                setText(String.valueOf(stock));
                getStyleClass().add(stock <= 3 ? "stock-low" : "stock-ok");
            }
        });

        productObsList = FXCollections.observableArrayList(catalog.getInventory());
        productTable.setItems(productObsList);
    }

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

        // subtotal column styled amber via .subtotal-cell
        colCartSub.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(String.format("PKR %,.0f", v));
                getStyleClass().add("subtotal-cell");
            }
        });

        cartObsList = FXCollections.observableArrayList();
        cartTable.setItems(cartObsList);
    }

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
            productTable.refresh();
            setStatus("✅  Added " + qty + "×  " + selected.getName() + " to cart.", false);
            quantityField.getValueFactory().setValue(1);
        } catch (InvalidQuantityException | InsufficientStockException e) {
            setStatus("⚠  " + e.getMessage(), true);
        }
    }

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

    @FXML
    private void handleCheckout() {
        try {
            Order order = processor.checkout(cart);
            showPaymentDialog(order);
        } catch (EmptyCartException e) {
            setStatus("⚠  " + e.getMessage(), true);
        }
    }

    private void showPaymentDialog(Order order) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(productTable.getScene().getWindow());
        dialog.setTitle("Payment — Order #" + order.getOrderId());
        dialog.setResizable(false);

        VBox root = new VBox(16);
        root.getStyleClass().add("payment-dialog");
        root.setPrefWidth(420);

        // load GUI.css into this new stage's scene
        root.getStylesheets().add(getClass().getResource("GUI.css").toExternalForm());

        boolean needsAdvance = order.getTotalAmount() >= 5000;

        Label titleLbl = new Label("Select Payment Method");
        titleLbl.getStyleClass().add("dialog-title");

        Label amtLbl = new Label("Total:  PKR " + String.format("%,.2f", order.getTotalAmount()));
        amtLbl.getStyleClass().add("dialog-amount");

        Label noticeLbl = new Label(needsAdvance
                ? "⚠  Total >= PKR 5,000 — Cash on Delivery is not available."
                : "✅  Eligible for any payment method.");
        noticeLbl.getStyleClass().add(needsAdvance ? "notice-warning" : "notice-success");
        noticeLbl.setWrapText(true);

        Separator sep = new Separator();

        List<PaymentGateway> gateways = Arrays.asList(
                new CashOnDelivery(),
                new CreditCard(),
                new EasyPaisa()
        );

        ToggleGroup tg = new ToggleGroup();
        VBox optBox = new VBox(8);

        for (PaymentGateway gw : gateways) {
            boolean disabled = (gw instanceof CashOnDelivery) && needsAdvance;

            RadioButton rb = new RadioButton(gw.getMethodName());
            rb.setToggleGroup(tg);
            rb.setDisable(disabled);   // .payment-option:disabled handles color in CSS
            rb.setMaxWidth(Double.MAX_VALUE);
            rb.setUserData(gw);
            rb.getStyleClass().add("payment-option");
            optBox.getChildren().add(rb);
        }

        // auto-select first enabled gateway
        tg.getToggles().stream()
                .filter(t -> !((RadioButton) t).isDisable())
                .findFirst()
                .ifPresent(t -> t.setSelected(true));

        Button confirmBtn = new Button("✅   Confirm & Place Order");
        confirmBtn.setMaxWidth(Double.MAX_VALUE);
        confirmBtn.getStyleClass().add("btn-confirm");

        confirmBtn.setOnAction(e -> {
            Toggle selected = tg.getSelectedToggle();
            if (selected == null) return;

            PaymentGateway chosen = (PaymentGateway) selected.getUserData();
            boolean ok = chosen.processPayment(order.getTotalAmount());

            if (ok) {
                processor.finalize(order, cart);
                refreshCart();
                productTable.refresh();
                dialog.close();
                showAlert("🎉  Order #" + order.getOrderId() + " Placed!\n\n"
                        + "Payment via : " + chosen.getMethodName() + "\n"
                        + "Amount       : PKR " + String.format("%,.2f", order.getTotalAmount())
                        + "\n\nThank you for shopping with ShopEase!");
                setStatus("🎉  Order #" + order.getOrderId()
                        + " placed via " + chosen.getMethodName() + ".", false);
            } else {
                // COD refused for orders >= 5000
                showAlert("❌  " + chosen.getMethodName()
                        + " is not available for this order total.\nPlease choose another method.");
            }
        });

        root.getChildren().addAll(titleLbl, amtLbl, noticeLbl, sep, optBox, confirmBtn);
        dialog.setScene(new Scene(root));
        dialog.show();
    }

    private void refreshCart() {
        cartObsList.setAll(cart.getItems());

        double total = cart.calculateTotal();
        totalLabel.setText(String.format("PKR %,.2f", total));
        headerTotalLabel.setText(String.format("PKR %,.0f", total));

        int n = cart.getItems().size();
        cartBadgeLabel.setText(n + " item" + (n != 1 ? "s" : ""));
    }

    // clears previous class before applying new state
    private void setStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().removeAll("status-error", "status-ok");
        statusLabel.getStyleClass().add(isError ? "status-error" : "status-ok");
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ShopEase");
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dp = alert.getDialogPane();
        dp.getStyleClass().add("dark-dialog");
        dp.getStylesheets().add(getClass().getResource("GUI.css").toExternalForm());

        alert.showAndWait();
    }
}