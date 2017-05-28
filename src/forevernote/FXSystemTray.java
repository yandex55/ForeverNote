package forevernote;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Class that creates and handles the SystemTray
 */
public class FXSystemTray {

    /**
     * Creates system tray along with menu items, icons, and action listeners
     * @param primaryStage is the main application's JavaFX stage
     * @throws Exception if issue with adding tray icon
     */
    public FXSystemTray(Stage primaryStage) throws Exception {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            javafx.scene.image.Image image = new javafx.scene.image.Image(getClass().getResourceAsStream("forevernoteicon_tray.png"));
            PopupMenu popup = new PopupMenu();
            MenuItem openForeverNote = new MenuItem("Open ForeverNote");
            MenuItem hideForeverNote = new MenuItem("Hide ForeverNote");
            MenuItem quitForeverNote = new MenuItem("Quit ForeverNote");

            popup.add(openForeverNote);
            popup.add(hideForeverNote);
            popup.add(quitForeverNote);

            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
            TrayIcon trayIcon = new TrayIcon(bufferedImage, "Open ForeverNote", popup);

            trayIcon.addActionListener(e -> {
                Platform.runLater(() -> {
                    primaryStage.show();
                });
            });

            openForeverNote.addActionListener(e -> {
                Platform.runLater(() -> {
                    primaryStage.show();
                });
            });

            hideForeverNote.addActionListener(e -> {
                Platform.runLater(() -> {
                    primaryStage.hide();
                });
            });

            quitForeverNote.addActionListener(e -> {
                Platform.runLater(() -> {
                    Platform.exit();
                    System.exit(0);
                });
            });

            tray.add(trayIcon);
        }
    }
}
