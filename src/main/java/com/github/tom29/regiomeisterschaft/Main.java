package com.github.tom29.regiomeisterschaft;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        VBox root = new VBox();
        root.setSpacing(10);
        root.setPadding(new Insets(10));

        // TableView für vorhandene Daten
        TableView<ObservableList<String>> tableView = createTableView();
        root.getChildren().add(tableView);

        // Button für die neue Seite
        Button addButton = new Button("Add Criterion");
        addButton.setOnAction(event -> {
            Stage newStage = createNewCriterionStage(primaryStage);
            newStage.show();
        });

        root.getChildren().add(addButton);

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Prioritize");
        primaryStage.show();
    }

    private TableView<ObservableList<String>> createTableView() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("database.json");
        JSONObject json = new JSONObject(new String(inputStream.readAllBytes()));
        JSONObject criteria = json.getJSONObject("criterion");

        TableView<ObservableList<String>> tableView = new TableView<>();

        // Spalte für die Kriterien hinzufügen
        TableColumn<ObservableList<String>, String> criterionColumn = new TableColumn<>("Criterion");
        criterionColumn.setCellValueFactory(cellData -> {
            ObservableList<String> row = cellData.getValue();
            return new SimpleStringProperty(row.get(0));
        });
        tableView.getColumns().add(criterionColumn);

        // Spalte für die Gewichtung hinzufügen
        TableColumn<ObservableList<String>, String> weightColumn = new TableColumn<>("Weight");
        weightColumn.setCellValueFactory(cellData -> {
            ObservableList<String> row = cellData.getValue();
            return new SimpleStringProperty(row.get(1));
        });
        tableView.getColumns().add(weightColumn);

        JSONArray devices = json.names();
        for (int i = 0; i < devices.length(); i++) {
            String deviceName = devices.getString(i);
            if (Objects.equals(deviceName, "criterion")) {
                continue;
            }
            TableColumn<ObservableList<String>, String> deviceNameColumn = new TableColumn<>(deviceName);
            tableView.getColumns().add(deviceNameColumn);

            TableColumn<ObservableList<String>, String> valueColumn = new TableColumn<>("Value");
            TableColumn<ObservableList<String>, String> scoreColumn = new TableColumn<>("Score");
            deviceNameColumn.getColumns().addAll(valueColumn, scoreColumn);

            int startIndex = 2 + i * 2; // Startindex für Wert und Score für das aktuelle Gerät
            deviceNameColumn.setCellValueFactory(cellData -> {
                ObservableList<String> row = cellData.getValue();
                return new SimpleStringProperty(row.get(startIndex)); // Index für Wert
            });

            valueColumn.setCellValueFactory(cellData -> {
                ObservableList<String> row = cellData.getValue();
                return new SimpleStringProperty(row.get(startIndex)); // Index für Wert
            });

            scoreColumn.setCellValueFactory(cellData -> {
                ObservableList<String> row = cellData.getValue();
                return new SimpleStringProperty(row.get(startIndex + 1)); // Index für Score
            });
        }

        for (String criterionName : criteria.keySet()) {
            ObservableList<String> row = FXCollections.observableArrayList();
            row.add(criterionName);
            row.add(String.valueOf(criteria.getJSONObject(criterionName).optInt("weight", 0))); // Gewichtung hinzufügen
            for (int i = 0; i < devices.length(); i++) {
                String deviceName = devices.getString(i);
                JSONObject device = json.getJSONObject(deviceName);
                String value = device.optString(criterionName, "");
                String score = ""; // Score ist zunächst leer
                row.addAll(value, score); // Wert und Score zur Zeile hinzufügen
            }
            tableView.getItems().add(row);
        }

        return tableView;
    }



    private Stage createNewCriterionStage(Stage primaryStage) {
        VBox root = new VBox();
        root.setSpacing(10);
        root.setPadding(new Insets(10));

        TextField criterionField = new TextField();
        criterionField.setPromptText("Criterion");

        TextField weightField = new TextField();
        weightField.setPromptText("Weight");

        Button addButton = new Button("Add Criterion");
        addButton.setOnAction(event -> {
            String criterionName = criterionField.getText();
            String weight = weightField.getText();

            if (!criterionName.isEmpty() && !weight.isEmpty()) {
                try {
                    updateDatabase(criterionName, weight, primaryStage);
                    criterionField.clear();
                    weightField.clear();
                    System.out.println("Criterion added successfully.");
                    Stage stage = (Stage) addButton.getScene().getWindow();
                    stage.close();

                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Error occurred while adding criterion.");
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("Please enter criterion name and weight.");
            }
        });

        root.getChildren().addAll(
                new Label("Add New Criterion:"),
                new HBox(new Label("Criterion Name:"), criterionField),
                new HBox(new Label("Weight:"), weightField),
                addButton
        );

        Scene scene = new Scene(root, 300, 150);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Add Criterion");
        return stage;
    }

    private void updateDatabase(String criterionName, String weightText, Stage primaryStage) throws IOException, URISyntaxException {
        // Überprüfen, ob das Gewicht ein gültiger Integer ist
        int weight;
        try {
            weight = Integer.parseInt(weightText);
        } catch (NumberFormatException e) {
            System.out.println("Invalid weight. Please enter a valid integer.");
            return;
        }

        // Pfad zur JSON-Datei
        String jsonFilePath = getClass().getResource("database.json").toURI().getPath();

        // JSON-Daten aus der Datei lesen
        InputStream inputStream = new FileInputStream(jsonFilePath);
        String jsonData = new String(inputStream.readAllBytes());
        JSONObject json = new JSONObject(jsonData);

        // Überprüfen, ob das Kriterium bereits vorhanden ist
        if (json.getJSONObject("criterion").has(criterionName)) {
            System.out.println("Criterion already exists.");
            return;
        }

        // Neues Kriterium hinzufügen
        json.getJSONObject("criterion").put(criterionName, new JSONObject().put("weight", weight));

        // JSON-Daten in die Datei schreiben
        try (OutputStream outputStream = new FileOutputStream(jsonFilePath)) {
            outputStream.write(json.toString().getBytes());
        }

        // TableView aktualisieren
        refreshTableView(primaryStage);
    }

    private void refreshTableView(Stage primaryStage) throws IOException {
        // Altes TableView löschen
        VBox root = (VBox) primaryStage.getScene().getRoot();
        root.getChildren().remove(0);

        // Neues TableView erstellen und hinzufügen
        TableView<ObservableList<String>> tableView = createTableView();
        root.getChildren().add(0, tableView);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
