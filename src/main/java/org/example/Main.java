package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/chess.fxml"));
        StackPane root = loader.load();

        Scene scene = new Scene(root, 1200, 800);
        stage.setTitle("Chess");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(700);
        stage.setResizable(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
