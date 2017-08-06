package forevernote;

import javafx.animation.PauseTransition;
import javafx.scene.web.WebEngine;
import javafx.util.Duration;

import java.io.*;
import java.util.Scanner;

/**
 * Class that organizes execution of javascript through the web engine
 */
public class JavascriptCode {

    private static PauseTransition pause;

    /**
     * Click on an element by id
     * @param webEngine is the browser object that the element is loaded in
     * @param htmlId is the element id as defined inside the HTML div tag
     */
    public static void clickById(WebEngine webEngine, String htmlId) {
        webEngine.executeScript("document.getElementById(\"" + htmlId + "\").click()");
    }

    /**
     * Click on an element by class
     * @param webEngine is the browser object that the element is loaded in
     * @param htmlClass is the element class as defined inside the div tag
     */
    public static void clickByClass(WebEngine webEngine, String htmlClass) {
        webEngine.executeScript("document.querySelector(\"." + htmlClass + "\").click()");
    }

     /**
     * Applies CSS changes by id
     * @param webEngine is the browser object that the element is loaded in
     * @param htmlId is the element id as defined inside the HTML div tag
     * @param property is the CSS property to modify
     * @param value is the value of the CSS property
     */
    public static void setCSSPropertyById(WebEngine webEngine, String htmlId, String property, String value) {
        webEngine.executeScript("document.getElementById(\"" + htmlId + "\").style.setProperty(" +
                "\"" + property + "\", \"" + value + "\", \"important\")");
    }

    /**
     * Applies CSS changes by class
     * @param webEngine is the browser object that the element is loaded in
     * @param htmlClass is the element class as defined inside the div tag
     * @param property is the CSS property to modify
     * @param value is the value of the CSS property
     */
    public static void setCSSPropertyByClass(WebEngine webEngine, String htmlClass, String property, String value) {
        webEngine.executeScript("document.querySelector(\"." + htmlClass + "\").style.setProperty(" +
                "\"" + property + "\", \"" + value + "\", \"important\")");
    }

    /**
     * Pause for a certain amount of time and then runs the methods passed through
     * Allows for certain components to load before performing further actions
     * (i.e. click on one javascript element that loads a panel, cannot immediately click on the elements
     * on the panel because we have to wait till the panel loads)
     * @param secondsDuration is the time in seconds to wait
     * @param function includes all Java methods to run after waiting
     */
    public static void pauseJavascriptExecution(double secondsDuration, Runnable... function) {
        pause = new PauseTransition(Duration.seconds(secondsDuration));

        pause.setOnFinished(event -> {
            for (int i = 0; i < function.length; i++) {
                function[i].run();
            }
        });

        pause.play();
    }

    /**
     * Clicks on the tag button, thus showing the tag view
     */
    public static void clickCreateNewTagButton() {
        clickByClass(Controller.getWebEngine(), "focus-drawer-TagsDrawer-TagsDrawer-create-tag-icon");
    }

    /**
     * Clicks on the create new notebook button
     */
    public static void clickCreateNewNotebookButton() {
        Controller.getWebEngine().executeScript("document.querySelectorAll(\"[id*=createNotebookButton]\")[0].click()");
    }

    /**
     * Loads sign in page
     */
    public static void loadSignInPage() {
        Controller.getWebEngine().load("https://www.evernote.com/Login.action");
    }

    /**
     * Clicks on the copy share url button
     */
    public static void clickCopyURLButton() {
        clickByClass(Controller.getWebEngine(), "GOKB433CCD.GOKB433CFF.GOKB433CME");
    }

    /**
     * Clicks on the done button, normally executed after copying the share url
     */
    public static void clickCopyDoneButton() {
        clickByClass(Controller.getWebEngine(), "GOKB433CGF.GOKB433CFF.GOKB433CBD");
    }

    /**
     * Clicks on the copy note link button
     */
    public static void clickCopyNoteLink() {
        clickByClass(Controller.getWebEngine(), "gwt-Label.GOKB433CCK.GOKB433CCD");
    }

    public static void clickOverflowButton() {
        clickById(Controller.getWebEngine(), "gwt-debug-NoteAttributes-overflowButton");
    }

    /**
     * Clicks on account settings
     */
    public static void clickAccountSettings() {
        clickById(Controller.getWebEngine(), "gwt-debug-AccountMenu-settings");
    }

    /**
     * Hides all other components in the web view pop-up window, such that only the note is visible
     */
    public static void showNoteOnly() {
        JavascriptCode.setCSSPropertyById(Controller.getWebView2().getEngine(), "gwt-debug-sidebar", "visibility", "hidden");
        JavascriptCode.setCSSPropertyById(Controller.getWebView2().getEngine(), "gwt-debug-stage", "margin-left", "-350px");
    }

    /**
     * Defines a variable to hold the element containing the confirmation button
     * Defines a function that reloads all notebooks once a new notebook is created
     * Adds event listener to the create notebook confirmation button, runs function once clicked
     */
    public static void addCreateNotebookEventListener() {
        Controller.getWebEngine().executeScript("var createNotebookButton = document.getElementById(\"gwt-debug-CreateNotebookDialog-confirm\");"
                + "function refresh(){java.reloadNotebooks();}"
                + "createNotebookButton.addEventListener(\"click\", refresh);"
        );
    }

    /**
     * Defines a variable to hold the element containing the username text field
     * Defines a function that prints the the value typed in the text field
     * Adds event listener to the login button, runs function once clicked
     */
    public static void addLoginListener() {
        Controller.getWebEngine().executeScript("var username = document.getElementById(\"username\");"
                + "function print(){java.print(username.value);}"
                + "document.getElementById(\"loginButton\").addEventListener(\"click\", print);"
        );
    }

    /**
     * If the username file exists, gets value and sets to the username text field
     */
    public static void setLoginUsername() {
        String username;
        File file = new File("ForeverNoteUsername.ini");

        try {
            if (file.exists()) {

                Scanner reader = new Scanner(new FileInputStream("ForeverNoteUsername.ini"));
                username = reader.nextLine();
                Controller.getWebEngine().executeScript("document.getElementById(\"" + "username"
                        + "\").value = \"" + username + "\" ");
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

    }
}
