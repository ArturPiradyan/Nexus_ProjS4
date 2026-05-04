import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class CampusUI extends Application {
    private final SmartCampusEngine engine = new SmartCampusEngine();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Nexus: Smart Campus Dashboard");

        TextField startField = new TextField();
        startField.setPromptText("Enter Start Node (e.g., ENTRANCE_A)");

        TextField endField = new TextField();
        endField.setPromptText("Enter Destination (e.g., LAB_302)");

        CheckBox adaToggle = new CheckBox("Require Accessible Route (ADA)");
        Button findPathBtn = new Button("Calculate Smart Path");

        TextArea resultArea = new TextArea();
        resultArea.setEditable(false);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(
                new Label("Campus Navigation"),
                startField,
                endField,
                adaToggle,
                findPathBtn,
                new Label("Directions:"),
                resultArea
        );

        findPathBtn.setOnAction(e -> {
            try {
                engine.loadMapFromDB();
                List<String> path = engine.findPath(
                        startField.getText().trim().toUpperCase(),
                        endField.getText().trim().toUpperCase(),
                        adaToggle.isSelected()
                );
                resultArea.setText("Path: " + String.join(" -> ", path));
            } catch (Exception ex) {
                resultArea.setText("Error: " + ex.getMessage());
            }
        });

        Scene scene = new Scene(layout, 400, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
