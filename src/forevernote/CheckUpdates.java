package forevernote;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Class that handles updates
 */
public class CheckUpdates {
    private static Stage window;
    private static Label label;
    private static VBox vLayout;
    private static final String version = "1.3";

    /**
     * Checks github repo for an update
     * @return whether update found or not
     */
    public static boolean isUpdateAvailable() {
        String urlString = "https://github.com/milan102/ForeverNote";

        try {
            URL url = new URL(urlString);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

            String line;
            String html = "";
            while ((line = reader.readLine()) != null) {
                html += line;
            }
            reader.close();

            return checkHTML(html);

        } catch (Exception e) {}

        return false;
    }

    /**
     * Checks HTML of github for version information
     * @param html is the content of the html page
     * @return whether the current version was found
     */
    public static boolean checkHTML(String html) {
        if (!html.contains("Current Version: " + version)) {
            initializeWindow();
            showUpdateAvailableDialog();
            showWindow();
            return true;
        }
        return false;
    }

    /**
     * Set the conditions of the popup window
     */
    public static void initializeWindow() {
        window = new Stage();

        window.initModality((Modality.APPLICATION_MODAL));
        window.setTitle("ForeverNote Updater");
        window.setMinWidth(500);

        label = new Label();

        vLayout = new VBox(10);
        vLayout.setAlignment(Pos.CENTER);
    }

    /**
     * Show that an update is available
     */
    public static void showUpdateAvailableDialog() {
        label.setText("An update is available...you should download it!");

        Button downloadButton = new Button("Download update");
        Button ignoreButton = new Button("Ignore update");

        downloadButton.setOnAction(e -> {
            Controller.openLinkInUsersDefaultBrowser(
                    "https://sourceforge.net/projects/forevernote/files/");
            window.close();
        });

        ignoreButton.setOnAction(e -> window.close());

        HBox hLayout = new HBox(10);
        hLayout.getChildren().addAll(downloadButton, ignoreButton);
        hLayout.setAlignment(Pos.CENTER);

        vLayout.getChildren().addAll(label, hLayout);
    }

    /**
     * Show that an update is unavailable
     */
    public static void showUpdateUnavailableDialog() {
        initializeWindow();
        label.setText("No update available. You are using the most current version, " + version);

        Button okButton = new Button("Ok");
        okButton.setOnAction(e -> window.close());

        vLayout.getChildren().addAll(label, okButton);
        showWindow();
    }

    /**
     * Display the window
     */
    public static void showWindow() {
        Scene scene = new Scene(vLayout, 500, 75);
        window.setScene(scene);
        window.showAndWait();
    }
}
