module com.oel {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.oel to javafx.fxml;
    exports com.oel;
}