module com.github.tom29.regiomeisterschaft {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;


    opens com.github.tom29.regiomeisterschaft to javafx.fxml;
    exports com.github.tom29.regiomeisterschaft;
}