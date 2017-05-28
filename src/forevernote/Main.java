package forevernote;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Class that starts JavaFX Stage, main application
 */
public class Main extends Application {

    /**
     * Loads fxml file and sets stage parameters before showing
     * @param primaryStage is the main stage
     * @throws Exception if fxml file not found
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("forevernote.fxml"));
        primaryStage.setTitle("ForeverNote");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("forevernoteicon.png")));
        primaryStage.setScene(new Scene(root, 1366, 768));
        Platform.setImplicitExit(false);
        new FXSystemTray(primaryStage);
        primaryStage.show();
    }

    /**
     * Console entry point
     */
    public static void main(String[] args) {
        launch(args);
    }
}
