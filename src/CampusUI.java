import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CampusUI extends Application {
    private static final String APP_STYLE =
            "-fx-background-color: linear-gradient(to bottom, #f4f7fb, #e6edf6);";
    private static final String PANEL_STYLE =
            "-fx-background-color: white;" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;" +
            "-fx-border-color: #d6dfeb;";
    private static final String CARD_STYLE =
            "-fx-background-color: #f8fbff;" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;" +
            "-fx-border-color: #dbe6f2;" +
            "-fx-padding: 14;";
    private static final String PRIMARY_BUTTON_STYLE =
            "-fx-background-color: #1f6feb;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: 700;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 18;";
    private static final String SECONDARY_BUTTON_STYLE =
            "-fx-background-color: #edf3fb;" +
            "-fx-text-fill: #1f2937;" +
            "-fx-font-weight: 600;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 14;" +
            "-fx-border-color: #d5e0ee;" +
            "-fx-border-radius: 8;";
    private static final String INPUT_STYLE =
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;" +
            "-fx-border-color: #c9d7e6;" +
            "-fx-background-color: white;" +
            "-fx-padding: 10 12;";

    private final SmartCampusEngine engine = new SmartCampusEngine();
    private final List<String> recentSearches = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Nexus: Smart Campus Dashboard");

        TextField startField = createTextField("Enter Start Node (e.g., ENTRANCE_A)");
        TextField endField = createTextField("Enter Destination (e.g., LAB_302)");
        CheckBox adaToggle = new CheckBox("Require accessible route");

        Button swapButton = new Button("Swap");
        swapButton.setStyle(SECONDARY_BUTTON_STYLE);
        swapButton.setMaxWidth(Double.MAX_VALUE);

        Button exampleRouteButton = new Button("Load Example");
        exampleRouteButton.setStyle(SECONDARY_BUTTON_STYLE);
        exampleRouteButton.setMaxWidth(Double.MAX_VALUE);

        Button refreshConditionsButton = new Button("Refresh Conditions");
        refreshConditionsButton.setStyle(SECONDARY_BUTTON_STYLE);
        refreshConditionsButton.setMaxWidth(Double.MAX_VALUE);

        Button findPathBtn = new Button("Calculate Route");
        findPathBtn.setStyle(PRIMARY_BUTTON_STYLE);
        findPathBtn.setMaxWidth(Double.MAX_VALUE);

        Label statusBadge = new Label("Ready");
        statusBadge.setStyle(createBadgeStyle("#eef4ff", "#1f4ea3"));

        Label routeHeadline = new Label("No route loaded");
        routeHeadline.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #142033;");

        Label routeSubline = new Label("Enter a start node and destination to calculate a path.");
        routeSubline.setStyle("-fx-text-fill: #5b6b80;");
        routeSubline.setWrapText(true);

        Label modeValue = createMetricValue("Standard");
        Label stopCountValue = createMetricValue("0");
        Label segmentCountValue = createMetricValue("0");
        Label routeOccupancyValue = createMetricValue("0%");
        Label routeCongestionValue = createMetricValue("0.00x");

        Label campusAverageOccupancyValue = createMetricValue("0%");
        Label campusAverageCongestionValue = createMetricValue("0.00x");
        Label busiestAreaValue = createMetricValue("None");
        busiestAreaValue.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #142033;");

        FlowPane routeTimeline = new FlowPane();
        routeTimeline.setHgap(8);
        routeTimeline.setVgap(8);
        routeTimeline.setStyle("-fx-padding: 4 0 0 0;");

        ListView<String> stepList = createDirectionsList();
        ListView<String> recentList = new ListView<>();
        recentList.setPlaceholder(new Label("Recent routes will appear here."));
        recentList.setPrefHeight(180);

        ListView<AreaStatus> routeConditionList = createAreaStatusList("Route area conditions will appear here.");
        routeConditionList.setPrefHeight(220);

        ListView<AreaStatus> campusConditionList = createAreaStatusList("Campus occupancy and congestion will appear here.");
        campusConditionList.setPrefHeight(420);

        swapButton.setOnAction(e -> {
            String currentStart = startField.getText();
            startField.setText(endField.getText());
            endField.setText(currentStart);
        });

        exampleRouteButton.setOnAction(e -> {
            startField.setText("ENTRANCE_A");
            endField.setText("LAB_302");
        });

        recentList.setOnMouseClicked(e -> {
            String selected = recentList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }

            String[] parts = selected.split(" \\| ");
            if (parts.length >= 3) {
                String[] route = parts[0].split(" -> ");
                if (route.length == 2) {
                    startField.setText(route[0]);
                    endField.setText(route[1]);
                }
                adaToggle.setSelected("Accessible".equals(parts[1]));
            }
        });

        refreshConditionsButton.setOnAction(e -> {
            try {
                statusBadge.setText("Refreshing");
                statusBadge.setStyle(createBadgeStyle("#fff7e8", "#9a6700"));
                engine.loadMapFromDB();
                updateCampusConditions(campusConditionList, campusAverageOccupancyValue, campusAverageCongestionValue, busiestAreaValue);
                routeSubline.setText("Campus conditions refreshed from current graph congestion data.");
                statusBadge.setText("Conditions Live");
                statusBadge.setStyle(createBadgeStyle("#ebfff1", "#166534"));
            } catch (Exception ex) {
                applyErrorState(statusBadge, routeHeadline, routeSubline, stepList, routeTimeline, routeConditionList, ex);
            }
        });

        findPathBtn.setOnAction(e -> {
            try {
                statusBadge.setText("Loading");
                statusBadge.setStyle(createBadgeStyle("#fff7e8", "#9a6700"));

                engine.loadMapFromDB();
                String start = normalizeNodeId(startField.getText());
                String end = normalizeNodeId(endField.getText());
                List<String> path = engine.findPath(start, end, adaToggle.isSelected());
                List<AreaStatus> routeStatuses = engine.getAreaStatusesForPath(path);

                routeHeadline.setText(start + " -> " + end);
                routeSubline.setText("Route calculated successfully. Occupancy and congestion are shown for each area on the path.");
                modeValue.setText(adaToggle.isSelected() ? "Accessible" : "Standard");
                stopCountValue.setText(Integer.toString(path.size()));
                segmentCountValue.setText(Integer.toString(Math.max(path.size() - 1, 0)));
                routeOccupancyValue.setText(formatPercent(engine.calculateAverageOccupancy(routeStatuses)));
                routeCongestionValue.setText(formatFactor(engine.calculateAverageCongestion(routeStatuses)));

                updateTimeline(routeTimeline, path);
                stepList.getItems().setAll(buildDirections(path, routeStatuses));
                routeConditionList.setItems(FXCollections.observableArrayList(routeStatuses));
                updateCampusConditions(campusConditionList, campusAverageOccupancyValue, campusAverageCongestionValue, busiestAreaValue);

                statusBadge.setText("Route Ready");
                statusBadge.setStyle(createBadgeStyle("#ebfff1", "#166534"));

                String recentEntry = start + " -> " + end + " | "
                        + (adaToggle.isSelected() ? "Accessible" : "Standard")
                        + " | " + path.size() + " stops";
                recentSearches.remove(recentEntry);
                recentSearches.add(0, recentEntry);
                if (recentSearches.size() > 6) {
                    recentSearches.remove(recentSearches.size() - 1);
                }
                recentList.getItems().setAll(recentSearches);
            } catch (Exception ex) {
                routeOccupancyValue.setText("0%");
                routeCongestionValue.setText("0.00x");
                stopCountValue.setText("0");
                segmentCountValue.setText("0");
                routeConditionList.getItems().clear();
                applyErrorState(statusBadge, routeHeadline, routeSubline, stepList, routeTimeline, routeConditionList, ex);
            }
        });

        VBox controlPanel = new VBox(16,
                createSectionTitle("Route Controls", "Search the campus graph and compare route modes."),
                labeledInput("Start Node", startField),
                labeledInput("Destination", endField),
                adaToggle,
                createButtonRow(swapButton, exampleRouteButton),
                refreshConditionsButton,
                findPathBtn,
                new Separator(),
                createSectionTitle("Recent Searches", "Click a previous route to reload it."),
                recentList
        );
        controlPanel.setPadding(new Insets(20));
        controlPanel.setPrefWidth(320);
        controlPanel.setStyle(PANEL_STYLE);

        GridPane routeMetricsGrid = new GridPane();
        routeMetricsGrid.setHgap(12);
        routeMetricsGrid.setVgap(12);
        routeMetricsGrid.add(createMetricCard("Route Mode", modeValue), 0, 0);
        routeMetricsGrid.add(createMetricCard("Stops", stopCountValue), 1, 0);
        routeMetricsGrid.add(createMetricCard("Segments", segmentCountValue), 2, 0);
        routeMetricsGrid.add(createMetricCard("Avg Occupancy", routeOccupancyValue), 0, 1);
        routeMetricsGrid.add(createMetricCard("Avg Congestion", routeCongestionValue), 1, 1);

        GridPane campusMetricsGrid = new GridPane();
        campusMetricsGrid.setHgap(12);
        campusMetricsGrid.setVgap(12);
        campusMetricsGrid.add(createMetricCard("Campus Occupancy", campusAverageOccupancyValue), 0, 0);
        campusMetricsGrid.add(createMetricCard("Campus Congestion", campusAverageCongestionValue), 1, 0);
        campusMetricsGrid.add(createMetricCard("Busiest Area", busiestAreaValue), 0, 1);

        VBox routeSummary = new VBox(8, statusBadge, routeHeadline, routeSubline);

        VBox timelinePanel = new VBox(10,
                createSectionTitle("Route Overview", "Visual sequence of stops on the current path."),
                routeTimeline
        );
        timelinePanel.setPadding(new Insets(16));
        timelinePanel.setStyle(PANEL_STYLE);

        VBox directionsPanel = new VBox(10,
                createSectionTitle("Step-by-Step Directions", "Detailed progression with availability notes."),
                stepList
        );
        directionsPanel.setPadding(new Insets(16));
        directionsPanel.setStyle(PANEL_STYLE);

        VBox routeConditionPanel = new VBox(10,
                createSectionTitle("Route Conditions", "Occupancy, congestion, and availability for each area on the path."),
                routeConditionList
        );
        routeConditionPanel.setPadding(new Insets(16));
        routeConditionPanel.setStyle(PANEL_STYLE);

        VBox resultsColumn = new VBox(16, routeSummary, routeMetricsGrid, timelinePanel, directionsPanel, routeConditionPanel);
        resultsColumn.setPrefWidth(540);
        VBox.setVgrow(directionsPanel, Priority.ALWAYS);

        VBox campusConditionPanel = new VBox(16,
                createSectionTitle("Campus Conditions", "Current area occupancy and congestion across the university graph."),
                campusMetricsGrid,
                campusConditionList
        );
        campusConditionPanel.setPadding(new Insets(20));
        campusConditionPanel.setPrefWidth(360);
        campusConditionPanel.setStyle(PANEL_STYLE);
        VBox.setVgrow(campusConditionList, Priority.ALWAYS);

        HBox content = new HBox(18, controlPanel, resultsColumn, campusConditionPanel);
        content.setPadding(new Insets(20));
        content.setStyle(APP_STYLE);
        HBox.setHgrow(resultsColumn, Priority.ALWAYS);

        BorderPane root = new BorderPane(content);
        root.setStyle(APP_STYLE);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Scene scene = new Scene(scrollPane, 1320, 820);
        primaryStage.setMinWidth(1180);
        primaryStage.setMinHeight(760);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void updateCampusConditions(
            ListView<AreaStatus> campusConditionList,
            Label campusAverageOccupancyValue,
            Label campusAverageCongestionValue,
            Label busiestAreaValue
    ) {
        List<AreaStatus> statuses = engine.getAreaStatuses();
        campusConditionList.setItems(FXCollections.observableArrayList(statuses));

        campusAverageOccupancyValue.setText(formatPercent(engine.calculateAverageOccupancy(statuses)));
        campusAverageCongestionValue.setText(formatFactor(engine.calculateAverageCongestion(statuses)));

        AreaStatus busiest = statuses.stream()
                .max(Comparator.comparingDouble(AreaStatus::getOccupancyPercent))
                .orElse(null);
        busiestAreaValue.setText(busiest == null ? "None" : busiest.getNodeId());
    }

    private void applyErrorState(
            Label statusBadge,
            Label routeHeadline,
            Label routeSubline,
            ListView<String> stepList,
            FlowPane routeTimeline,
            ListView<AreaStatus> routeConditionList,
            Exception ex
    ) {
        routeHeadline.setText("Route unavailable");
        routeSubline.setText(ex.getMessage());
        routeTimeline.getChildren().clear();
        stepList.getItems().setAll("Check node names, database content, and accessibility constraints.");
        routeConditionList.getItems().clear();
        statusBadge.setText("Error");
        statusBadge.setStyle(createBadgeStyle("#fff1f2", "#b42318"));
    }

    private TextField createTextField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.setStyle(INPUT_STYLE);
        return field;
    }

    private VBox labeledInput(String labelText, TextField field) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: 600; -fx-text-fill: #243247;");
        return new VBox(6, label, field);
    }

    private HBox createButtonRow(Button leftButton, Button rightButton) {
        HBox row = new HBox(10, leftButton, rightButton);
        HBox.setHgrow(leftButton, Priority.ALWAYS);
        HBox.setHgrow(rightButton, Priority.ALWAYS);
        return row;
    }

    private VBox createSectionTitle(String titleText, String subtitleText) {
        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #142033;");

        Label subtitle = new Label(subtitleText);
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #607086;");

        return new VBox(4, title, subtitle);
    }

    private VBox createMetricCard(String titleText, Label valueLabel) {
        Label title = new Label(titleText);
        title.setStyle("-fx-text-fill: #5f6f85; -fx-font-size: 12px; -fx-font-weight: 600;");

        VBox card = new VBox(8, title, valueLabel);
        card.setMinWidth(160);
        card.setStyle(CARD_STYLE);
        return card;
    }

    private Label createMetricValue(String valueText) {
        Label label = new Label(valueText);
        label.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #142033;");
        return label;
    }

    private String createBadgeStyle(String background, String textColor) {
        return "-fx-background-color: " + background + ";" +
                "-fx-text-fill: " + textColor + ";" +
                "-fx-font-weight: 700;" +
                "-fx-background-radius: 999;" +
                "-fx-padding: 6 12;";
    }

    private void updateTimeline(FlowPane routeTimeline, List<String> path) {
        routeTimeline.getChildren().clear();

        for (int i = 0; i < path.size(); i++) {
            Label stopChip = new Label(path.get(i));
            stopChip.setStyle(
                    "-fx-background-color: #eaf2ff;" +
                    "-fx-background-radius: 999;" +
                    "-fx-padding: 8 12;" +
                    "-fx-text-fill: #18407f;" +
                    "-fx-font-weight: 600;"
            );
            routeTimeline.getChildren().add(stopChip);

            if (i < path.size() - 1) {
                Label arrow = new Label("->");
                arrow.setStyle("-fx-text-fill: #7890ad; -fx-padding: 8 2;");
                routeTimeline.getChildren().add(arrow);
            }
        }
    }

    private List<String> buildDirections(List<String> path, List<AreaStatus> routeStatuses) {
        List<String> directions = new ArrayList<>();
        if (path.isEmpty()) {
            return directions;
        }

        directions.add("Start at " + path.get(0) + ".");
        for (int i = 1; i < path.size(); i++) {
            AreaStatus status = i < routeStatuses.size() ? routeStatuses.get(i) : null;
            String conditionNote = status == null
                    ? ""
                    : " Area status: " + status.getAvailabilityLabel()
                    + ", " + status.getCongestionLabel().toLowerCase(Locale.ROOT)
                    + " congestion.";
            directions.add("Continue from " + path.get(i - 1) + " to " + path.get(i) + "." + conditionNote);
        }
        directions.add("Arrive at " + path.get(path.size() - 1) + ".");
        return directions;
    }

    private ListView<String> createDirectionsList() {
        ListView<String> stepList = new ListView<>();
        stepList.setPlaceholder(new Label("Route steps will appear here."));
        stepList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label stepLabel = new Label(item);
                stepLabel.setWrapText(true);
                stepLabel.setStyle("-fx-text-fill: #1f2937; -fx-font-size: 13px;");

                Label indexLabel = new Label(Integer.toString(getIndex() + 1));
                indexLabel.setMinSize(28, 28);
                indexLabel.setAlignment(Pos.CENTER);
                indexLabel.setStyle(
                        "-fx-background-color: #dbeafe;" +
                        "-fx-text-fill: #1d4ed8;" +
                        "-fx-font-weight: 700;" +
                        "-fx-background-radius: 14;"
                );

                HBox row = new HBox(12, indexLabel, stepLabel);
                row.setAlignment(Pos.TOP_LEFT);
                row.setPadding(new Insets(8, 6, 8, 6));
                setGraphic(row);
            }
        });
        stepList.setPrefHeight(260);
        return stepList;
    }

    private ListView<AreaStatus> createAreaStatusList(String placeholderText) {
        ListView<AreaStatus> listView = new ListView<>();
        listView.setPlaceholder(new Label(placeholderText));
        listView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AreaStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label nodeLabel = new Label(status.getNodeId());
                nodeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #142033;");

                Label availabilityBadge = new Label(status.getAvailabilityLabel());
                availabilityBadge.setStyle(createAvailabilityStyle(status.getAvailabilityLabel()));

                Label congestionBadge = new Label(status.getCongestionLabel());
                congestionBadge.setStyle(createCongestionStyle(status.getCongestionLabel()));

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox header = new HBox(8, nodeLabel, spacer, availabilityBadge, congestionBadge);
                header.setAlignment(Pos.CENTER_LEFT);

                ProgressBar occupancyBar = new ProgressBar(status.getOccupancyPercent() / 100.0);
                occupancyBar.setPrefWidth(220);

                Label occupancyLabel = new Label("Occupancy " + formatPercent(status.getOccupancyPercent()));
                occupancyLabel.setStyle("-fx-text-fill: #243247; -fx-font-weight: 600;");

                Label congestionLabel = new Label("Congestion " + formatFactor(status.getAverageCongestionFactor()));
                congestionLabel.setStyle("-fx-text-fill: #5f6f85;");

                Label connectionLabel = new Label(status.getConnectionCount() + " linked segments");
                connectionLabel.setStyle("-fx-text-fill: #5f6f85;");

                HBox statsRow = new HBox(14, occupancyLabel, congestionLabel, connectionLabel);
                statsRow.setAlignment(Pos.CENTER_LEFT);

                VBox card = new VBox(10, header, occupancyBar, statsRow);
                card.setPadding(new Insets(12));
                card.setStyle(CARD_STYLE);
                setGraphic(card);
            }
        });
        return listView;
    }

    private String createAvailabilityStyle(String availability) {
        return switch (availability) {
            case "Open" -> createBadgeStyle("#ebfff1", "#166534");
            case "Available" -> createBadgeStyle("#eef4ff", "#1f4ea3");
            case "Limited" -> createBadgeStyle("#fff7e8", "#9a6700");
            default -> createBadgeStyle("#fff1f2", "#b42318");
        };
    }

    private String createCongestionStyle(String congestion) {
        return switch (congestion) {
            case "Low" -> createBadgeStyle("#ebfff1", "#166534");
            case "Moderate" -> createBadgeStyle("#eef4ff", "#1f4ea3");
            case "High" -> createBadgeStyle("#fff7e8", "#9a6700");
            default -> createBadgeStyle("#fff1f2", "#b42318");
        };
    }

    private String normalizeNodeId(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String formatPercent(double value) {
        return String.format(Locale.US, "%.0f%%", value);
    }

    private String formatFactor(double value) {
        return String.format(Locale.US, "%.2fx", value);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
