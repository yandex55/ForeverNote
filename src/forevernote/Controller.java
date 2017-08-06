package forevernote;

import com.sun.glass.events.KeyEvent;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.scene.image.Image;
import netscape.javascript.JSObject;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Class that handles the logic of UI elements
 */
public class Controller {
    @FXML private WebView browser;
    @FXML private TreeView<String> treeView;
    @FXML private MenuItem signOut;
    @FXML private Button syncButton;
    @FXML private Button allNotesButton;
    @FXML private Button newNoteButton;
    @FXML private BorderPane borderPane;
    @FXML private CheckMenuItem checkMenuSignedInUser;
    @FXML private CheckMenuItem checkMenuNoteList;
    @FXML private CheckMenuItem checkMenuNotePanel;
    @FXML private CheckMenuItem checkMenuEditingToolbar;
    @FXML private CheckMenuItem checkMenuSelectAll;
    @FXML private CheckMenuItem checkMenuMoveToNotebook;
    @FXML private CheckMenuItem checkMenuShareNote;
    @FXML private CheckMenuItem checkMenuPostToTwitter;
    @FXML private CheckMenuItem checkMenuInsertTable;
    @FXML private CheckMenuItem checkMenuColor;
    @FXML private CheckMenuItem checkMenuLeft;
    @FXML private CheckMenuItem checkMenuBold;
    @FXML private CheckMenuItem checkMenuEditHyperlink;
    @FXML private CheckMenuItem checkMenuSync;
    @FXML private CheckMenuItem checkMenuHelpAndLearning;
    private static WebEngine webEngine;
    private static WebView webView2;
    private Notebooks notebooks;
    private NotebookStructure notebookStructure;
    private CheckUpdates checkUpdates;
    private int numberOfSuccessfulRuns;

    /**
     * Runs immediately on program start
     */
    public void initialize() {
        setStartingConditions();
        addWebEngineListener();
        setTextFieldUsername();
    }

    /**
     * Hides unnecessary components on loading screen
     * Disable the buggy right-click context menu because cut/copy/paste doesn't work
     * Handles links opened in a new tab
     * Loads sign-in page
     */
    private void setStartingConditions() {
        webEngine = browser.getEngine();
        browser.setContextMenuEnabled(false);
        setVisibleTopAndLeftBorderPane(false);
        handlePopupLinks();
        webEngine.load("https://www.evernote.com/Login.action");
    }

    /**
     * Detect changes to WebEngine
     * Performs actions once logged in
     */
    private void addWebEngineListener() {
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (Worker.State.SUCCEEDED.equals(newValue)) {

                boolean loggedIn = webEngine.getLocation().toLowerCase().contains("login=true");

                if (loggedIn) {
                    loadUserInformation();
                    setButtonActions();
                }
            }
        });
    }

    /**
     * Sets username text field after login page has fully loaded
     */
    public void setTextFieldUsername() {
        numberOfSuccessfulRuns = 0;
        runJavascriptAfterPageLoad(webEngine, JavascriptCode::setLoginUsername);
    }

    /**
     * Hide border top and left, thus ensuring components don't get shown before login
     * @param visibility is whether the border pane is shown or not
     */
    private void setVisibleTopAndLeftBorderPane(boolean visibility) {
        borderPane.getLeft().setManaged(visibility);
        borderPane.getLeft().setVisible(visibility);
        borderPane.getTop().setManaged(visibility);
        borderPane.getTop().setVisible(visibility);
    }

    /**
     * When links are opened in a tab, they are instead opened in a new WebView popup window
     */
    private void handlePopupLinks() {
        webEngine.setCreatePopupHandler(new Callback<PopupFeatures, WebEngine>() {
            @Override
            public WebEngine call(PopupFeatures p) {
                Stage stage = new Stage(StageStyle.UTILITY);
                webView2 = new WebView();
                stage.setScene(new Scene(webView2));
                stage.show();
                return webView2.getEngine();
            }
        });
    }

    /**
     * Runs methods to load user information, and shows top and left border panes
     */
    private void loadUserInformation() {
        setVisibleTopAndLeftBorderPane(true);
        addTreeViewListener();
        loadNotebooks();

        UserInfo userInfo = new UserInfo();
        userInfo.loadFromHTML(getWebPageHTML());
        setMenuUsername(userInfo);
        saveUsername(userInfo);
    }

    /**
     * Since buttons were styled with CSS, their actions had to be defined here
     */
    private void setButtonActions() {
        newNoteButton.setOnAction(e -> actionNewNote());
        allNotesButton.setOnAction(e -> actionAllNotes());
        syncButton.setOnAction(e -> actionSync());
    }

    /**
     * Adds a listener to the tree view containing the list of notebooks
     * If root is clicked, loads all notes
     * If notebook isn't a stack, then runs methods to find and click
     * @throws NullPointerException if a tree view item no longer exists
     */
    private void addTreeViewListener() {
        treeView.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {

            String notebookName = newValue.getValue();
            boolean clickedRoot = notebookName.equals("Notebooks");
            boolean notebookIsAParent = Notebooks.getNotebookParentsList().contains(notebookName);

            if (clickedRoot) {
                JavascriptCode.clickById(webEngine, "gwt-debug-Sidebar-notesButton");
            } else if (!notebookIsAParent) {
                searchForNotebook(notebookName);
                clickOnNotebookInResults(notebookName);
            }
        });
    }

    /**
     * Uses page's HTML code to get notebook names and create their structure
     * Displays information on tree view, and sets to visible
     */
    private void loadNotebooks() {
        notebooks = new Notebooks();
        notebooks.loadFromHTML(getWebPageHTML());

        Node trashGraphic = new ImageView(new Image("http://i.imgur.com/u8fNhvm.png"));

        notebookStructure = new NotebookStructure();
        notebookStructure.createNotebookStructure();
        notebookStructure.makeBranchWithImage("Trash", trashGraphic, notebookStructure.getNotebookStructure());

        treeView.setRoot(notebookStructure.getNotebookStructure());
        treeView.setShowRoot(true);
    }

    /**
     * Runs Javascript code once page is fully loaded, to ensure that web page elements are present before
     * attempting to manipulate them
     * If number of successful runs is less than 2, wait for two seconds then run the check again
     * Functions will be ran twice to ensure functionality
     * @param webEngine is the web browser window
     * @param function includes all methods to run once page has fully loaded
     */
    private void runJavascriptAfterPageLoad(WebEngine webEngine, Runnable... function) {
        if (webEngine.getLoadWorker().stateProperty().getValue().equals(Worker.State.SUCCEEDED)) {
            for (int i = 0; i < function.length; i++) {
                function[i].run();
            }
            numberOfSuccessfulRuns++;
        }

        if (numberOfSuccessfulRuns < 2) {
            JavascriptCode.pauseJavascriptExecution(2, () -> runJavascriptAfterPageLoad(webEngine, function));
        }
    }

    /**
     * Clicks notebooks button on web page, and sets the search query to the name
     * Clicks on the notebooks button twice after that:
     * 1st time conceals the notebook list panel so that the search query initiates
     * 2nd time shows the notebook list panel so that the search results can be seen
     * @param notebookName
     */
    private void searchForNotebook(String notebookName) {
        webEngine.executeScript("document.querySelectorAll(\"[id*=notebooksButton]\")[0].click()");
        webEngine.executeScript("document.getElementById(\"gwt-debug-NotebooksDrawer-drawerFilter-textBox\").value = \"" + notebookName + "\";");
        webEngine.executeScript("document.querySelectorAll(\"[id*=notebooksButton]\")[0].click()");
        webEngine.executeScript("document.querySelectorAll(\"[id*=notebooksButton]\")[0].click()");
    }

    /**
     * If the notebook name is a special case, like Trash, then clicks on that specific HTML element
     * Else selects all search results, and if the search result is equal to the name of the notebook, clicks
     * Needs to find the exact notebook because a search results can include notebooks with similar names
     * @param notebookName is the name of the notebook
     */
    private void clickOnNotebookInResults(String notebookName) {
        if (notebookName.equals("Trash")) {
            webEngine.executeScript("document.querySelectorAll(\"[class*=qa-trash]\")[0].click()");
        }
        else {
            webEngine.executeScript("var drawer = document.querySelectorAll(\"[id*=DrawerView]\")[0];" +
                    "var notebooks = drawer.querySelectorAll(\"[class*=qa-name]\");" +
                    "for (var i = 0; i < notebooks.length; i++) {" +
                    "if (notebooks[i].innerHTML == \"" + notebookName + "\") {" +
                    "notebooks[i].click();" + "}}"
            );
        }
    }

    /**
     * Gets user's email from HTML, then sets the sign-out menu item's text to include the info
     * @param userInfo is the object containing user information gathered from the HTML of the page
     */
    private void setMenuUsername(UserInfo userInfo) {
        checkMenuSignedInUser.setText(userInfo.getEmail());
        signOut.setText("Sign _out " + userInfo.getEmail());
    }

    /**
     *  Creates file to store username in, and then prints username to an ini file
     * @param userInfo is the object containing user information gathered from the HTML of the page
     */
    private void saveUsername(UserInfo userInfo) {
        File file = new File("ForeverNoteUsername.ini");

        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fileWriter = new FileWriter(new File("ForeverNoteUsername.ini"));
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(userInfo.getEmail());
            printWriter.close();

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve page's HTML
     * @return HTML contents
     */
    private String getWebPageHTML() {
        return (String) webEngine.executeScript("document.documentElement.outerHTML");
    }

    /**
     * Clicks new note button
     */
    public void actionNewNote() {
        JavascriptCode.clickById(webEngine, "gwt-debug-Sidebar-newNoteButton");
    }

    /**
     * Clicks on the work chat button, thus showing the work chat panel
     * Clicks on the work chat start button
     */
    public void actionNewChat() {
        JavascriptCode.clickById(webEngine, "gwt-debug-Sidebar-workChatButton");
        JavascriptCode.clickById(webEngine, "gwt-debug-WorkChatDrawer-startChatButton");
    }

    /**
     * Clicks on tags button, thus showing tag panel
     * Pauses to ensure button has loaded, then clicks on create new tag button
     */
    public void actionNewTag() {
        JavascriptCode.clickById(webEngine, "gwt-debug-Sidebar-tagsButton");
        JavascriptCode.pauseJavascriptExecution(0.1, JavascriptCode::clickCreateNewTagButton);
    }

    /**
     * Clicks on notebooks button, thus showing notebooks panel
     * Pauses to ensure button has loaded, then clicks on create new notebook button
     * Pauses to ensure button has loaded, then adds a Javascript event listener to the create notebook confirmation button
     */
    public void actionNewNotebook() {
        JavascriptCode.clickById(webEngine, "gwt-debug-Sidebar-notebooksButton");
        JavascriptCode.pauseJavascriptExecution(0.1, JavascriptCode::clickCreateNewNotebookButton);

        JSObject jsobj = (JSObject) webEngine.executeScript("window");
        jsobj.setMember("java", this);
        JavascriptCode.pauseJavascriptExecution(2, JavascriptCode::addCreateNotebookEventListener);
    }

    /**
     * Pauses to ensure new notebook is made, then reloads notebook list
     */
    public void reloadNotebooks() {
        JavascriptCode.pauseJavascriptExecution(2, webEngine::reload, this::loadNotebooks);
    }

    /**
     * Clicks on attach files button
     */
    public void actionAttachFiles() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-attachmentButton");
    }

    /**
     * Clicks on avatar to initiate viewing of logout button
     * Clicks logout button twice to ensure proper sign out
     * Pauses to ensure fully logged out, then initializes from the beginning to allow a new user to sign in
     */
    public void actionSignOut() {
        JavascriptCode.clickById(webEngine, "gwt-debug-AccountMenu-avatar");
        JavascriptCode.clickById(webEngine, "gwt-debug-AccountMenu-logout");
        JavascriptCode.clickById(webEngine, "gwt-debug-AccountMenu-logout");
        JavascriptCode.pauseJavascriptExecution(1, this::initialize);
    }

    /**
     * Exit the application
     */
    public void actionExit() {
        Platform.exit();
        System.exit(0);
    }

    /**
     * Undo shortcut initiated only if manually clicked in the menu
     */
    public void actionUndo() {
        KeyboardInput.robotDoubleKeyPress(KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
    }

    /**
     * Redo shortcut initiated only if manually clicked in the menu
     */
    public void actionRedo() {
        KeyboardInput.robotDoubleKeyPress(KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
    }

    /**
     * Cut shortcut initiated only if manually clicked in the menu
     */
    public void actionCut() {
        KeyboardInput.robotDoubleKeyPress(KeyEvent.VK_CONTROL, KeyEvent.VK_X);
    }

    /**
     * Copy shortcut initiated only if manually clicked in the menu
     */
    public void actionCopy() {
        KeyboardInput.robotDoubleKeyPress(KeyEvent.VK_CONTROL, KeyEvent.VK_C);
    }

    /**
     * Paste shortcut initiated only if manually clicked in the menu
     */
    public void actionPaste() {
        //KeyboardInput.robotDoubleKeyPress(KeyEvent.VK_CONTROL, KeyEvent.VK_V);
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        System.out.println(clipboard.getString());
    }

    /**
     * Delete shortcut initiated only if manually clicked in the menu
     */
    public void actionDelete() {
        KeyboardInput.robotSingleKeyPress(KeyEvent.VK_DELETE);
    }

    /**
     * Select all shortcut initiated only if manually clicked in the menu
     */
    public void actionSelectAll() {
        KeyboardInput.robotDoubleKeyPress(KeyEvent.VK_CONTROL, KeyEvent.VK_A);
        checkMenuSelectAll.setSelected(false);
    }

    /**
     * Clicks on shortcuts button to show shortcut panel
     */
    public void actionShortcuts() {
        JavascriptCode.clickById(webEngine, "gwt-debug-Sidebar-shortcutsButton");
    }

    /**
     * Sets left panel visibility
     */
    public void actionLeftPanel() {
        boolean leftPanelIsVisible = borderPane.getLeft().isVisible();

        if (leftPanelIsVisible) {
            leftPanelSetVisible(false);
        } else {
            leftPanelSetVisible(true);
        }
    }

    /**
     * Controls left panel visibility
     * @param visibility is whether the left panel is shown or not
     */
    private void leftPanelSetVisible(boolean visibility) {
        treeView.setVisible(visibility);
        borderPane.getLeft().setManaged(visibility);
    }

    /**
     * Uses CSS properties to change visibility of note list
     */
    public void actionNoteList() {
        if (!checkMenuNoteList.isSelected()) {
            JavascriptCode.setCSSPropertyByClass(webEngine, "focus-NotesView-NotesView", "visibility", "hidden");
        } else {
            JavascriptCode.setCSSPropertyByClass(webEngine, "focus-NotesView-NotesView", "visibility", "visible");
        }
    }

    /**
     * Uses CSS properties to change visibility of note panel
     */
    public void actionNotePanel() {
        if (!checkMenuNotePanel.isSelected()) {
            JavascriptCode.setCSSPropertyById(webEngine, "gwt-debug-NoteView-root", "visibility", "hidden");
        } else {
            JavascriptCode.setCSSPropertyById(webEngine, "gwt-debug-NoteView-root", "visibility", "visible");
        }
    }

    /**
     * Clicks on tags button, thus showing the tag panel
     */
    public void actionTagsView() {
        JavascriptCode.clickById(webEngine, "gwt-debug-Sidebar-tagsButton");
    }

    /**
     * Clicks on quick search button, thus showing the quick search container
     */
    public void actionQuickSearch() {
        JavascriptCode.clickById(webEngine, "gwt-debug-Sidebar-searchButton-container");
    }

    /**
     * Clicks on work chat button, thus showing the work chat panel
     */
    public void actionWorkChat() {
        JavascriptCode.clickById(webEngine, "gwt-debug-Sidebar-workChatButton");
    }

    /**
     * Uses CSS properties to change visibility of note's editing toolbar
     */
    public void actionEditingToolbar() {
        if (!checkMenuEditingToolbar.isSelected()) {
            JavascriptCode.setCSSPropertyByClass(webEngine, "GBVXONSAGB", "visibility", "hidden");
        } else {
            JavascriptCode.setCSSPropertyByClass(webEngine, "GBVXONSAGB", "visibility", "visible");
        }
    }

    /**
     * Set number of successful runs to zero
     * Open current web page in a new window
     * Run method to apply CSS once note is loaded
     */
    public void actionOpenInANewWindow() {
        numberOfSuccessfulRuns = 0;
        webEngine.executeScript("window.open(document.baseURI, \"blank\");");
        runJavascriptAfterPageLoad(webView2.getEngine(), JavascriptCode::showNoteOnly);
    }

    /**
     * Clicks share button
     * Sets check menu to false once clicked, so that check menu never shows,
     * because this allows proper spacing in the file menu without ever showing the checkbox
     */
    public void actionShareNote() {
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteSharing-shareButton");
        checkMenuShareNote.setSelected(false);
    }

    /**
     * Click on share dropdown
     * Click on share link menu option
     * Pauses to ensure button is loaded, then: 1)clicks copy url button, 2)performs copy command, 3)clicks done button
     */
    public void actionCopyShareURL() {
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteSharing-dropdown");
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteSharingMenu-root-LINK");
        JavascriptCode.pauseJavascriptExecution(1, JavascriptCode::clickCopyURLButton,
                this::actionCopy, JavascriptCode::clickCopyDoneButton);
    }

    /**
     * Click on share dropdown
     * Click on share to facebook menu option
     * Pauses to ensure popup is loaded, then loads URL in user's default web browser
     */
    public void actionPostToFacebook() {
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteSharing-dropdown");
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteSharingMenu-root-FACEBOOK");
        JavascriptCode.pauseJavascriptExecution(1, Controller::getWebViewLinkAndOpenInUsersDefaultBrowser);
    }

    /**
     * Click on share dropdown
     * Click on share to twitter menu option
     * Pauses to ensure popup is loaded, then loads URL in user's default web browser
     */
    public void actionPostToTwitter() {
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteSharing-dropdown");
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteSharingMenu-root-TWITTER");
        JavascriptCode.pauseJavascriptExecution(1, Controller::getWebViewLinkAndOpenInUsersDefaultBrowser);
        checkMenuPostToTwitter.setSelected(false);
    }

    /**
     * Click on share dropdown
     * Click on share to LinkedIn menu option
     * Does not implement opening in user's default web browser because further manipulation of link needs to occur
     */
    public void actionPostToLinkedIn() {
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteSharing-dropdown");
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteSharingMenu-root-LINKEDIN");
    }

    /**
     * Open URI in a popup web view and get the link
     * Open link in users default browser
     * Click on share dropdown so that the menu is no longer showing
     */
    private static void getWebViewLinkAndOpenInUsersDefaultBrowser() {
        try {
            Desktop.getDesktop().browse(new URI(webView2.getEngine().getLocation()));
            Stage webView2Stage = (Stage) webView2.getScene().getWindow();
            webView2Stage.close();
            JavascriptCode.clickById(webEngine, "gwt-debug-NoteSharing-dropdown");
        } catch (IOException | URISyntaxException  e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Click on share dropdown
     * Click on email menu option
     */
    public void actionSendACopy() {
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteSharing-dropdown");
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteSharingMenu-root-EMAIL");
    }

    /**
     * Click on share dropdown
     * Click on link menu option to show sharing options
     */
    public void actionModifySharing() {
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteSharing-dropdown");
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteSharingMenu-root-LINK");
    }

    /**
     * Click on overflow button
     * Pauses to ensure dropdown loaded, then clicks option and performs copy command
     * Pauses to ensure previous commands are fully completed, then clicks overflow button to no longer show
     */
    public void actionCopyNoteLink() {
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteAttributes-overflowButton");
        JavascriptCode.pauseJavascriptExecution(1, JavascriptCode::clickCopyNoteLink, this::actionCopy);
        JavascriptCode.pauseJavascriptExecution(3, JavascriptCode::clickOverflowButton);
    }

    /**
     * Click on note's current notebook name to initiate a move
     * Sets check menu to false once clicked, so that check menu never shows,
     * because this allows proper spacing in the file menu without ever showing the checkbox
     */
    public void actionMoveToNotebook() {
        JavascriptCode.clickById(webEngine, "gwt-debug-NotebookSelectMenu-notebookName");
        checkMenuMoveToNotebook.setSelected(false);
    }

    /**
     * Clicks on trash button
     * Confirms move to trash
     */
    public void actionMoveNoteToTrash() {
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteAttributes-trashButton");
        JavascriptCode.clickById(webEngine, "gwt-debug-ConfirmationDialog-confirm");
    }

    /**
     * Clicks on reminder button
     */
    public void actionSetReminder() {
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteAttributes-reminderButton");
    }

    /**
     * Clicks on add shortcut button
     */
    public void actionAddShortcut() {
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteAttributes-shortcutButton");
    }

    /**
     * Click note information button
     */
    public void actionNoteInformation() {
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteAttributes-infoButton");
    }

    /**
     * Click note information button, then clicks on the view history link
     */
    public void actionNoteHistory() {
        JavascriptCode.clickById(webEngine, "gwt-debug-NoteAttributes-infoButton");
        webEngine.executeScript("var historyContainer = document.querySelectorAll(\"[id*=historyContainer]\")[0];" +
                "var historyItems = historyContainer.querySelectorAll(\"[class*=G]\");" +
                "for (var i = 0; i < historyItems.length; i++) {" +
                "if (historyItems[i].innerHTML == \"View history\") {" +
                "historyItems[i].click();" + "}}"
        );

        webEngine.executeScript("var dialogContainer = document.querySelectorAll(\"[id*=GlassModalDialog-footer]\")[0];" +
                "var dialogItems = dialogContainer.querySelectorAll(\"[class*=G]\");" +
                "for (var i = 0; i < dialogItems.length; i++) {" +
                "if (dialogItems[i].innerHTML == \"Cancel\") {" +
                "dialogItems[i].click();" + "}}"
        );
    }

    /**
     * Clicks font button
     */
    public void actionFontStyle() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-fontButton");
    }

    /**
     * Clicks font size button
     */
    public void actionFontSize() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-fontSizeButton");
    }

    /**
     * Clicks font color button
     */
    public void actionFontColor() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-colorButton");
        checkMenuColor.setSelected(false);
    }

    /**
     * Clicks align paragraph left button
     */
    public void actionParagraphLeft() {
        JavascriptCode.clickById(webEngine, "gwt-debug-EditorAlignDropdown-left");
        checkMenuLeft.setSelected(false);
    }

    /**
     * Clicks align paragraph center button
     */
    public void actionParagraphCenter() {
        JavascriptCode.clickById(webEngine, "gwt-debug-EditorAlignDropdown-center");
    }

    /**
     * Clicks align paragraph right button
     */
    public void actionParagraphRight() {
        JavascriptCode.clickById(webEngine, "gwt-debug-EditorAlignDropdown-right");
    }

    /**
     * Clicks increase indentation button
     */
    public void actionParagraphIncreaseIndentation() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-indentButton");
    }

    /**
     * Clicks decrease indentation button
     */
    public void actionParagraphDecreaseIndentation() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-outdentButton");
    }

    /**
     * Clicks bulleted list button
     */
    public void actionParagraphBulletedList() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-bulletButton");
    }

    /**
     * Clicks numbered  list button
     */
    public void actionParagraphNumberedList() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-listButton");
    }

    /**
     * Clicks bold button
     */
    public void actionStyleBold() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-boldButton");
        checkMenuBold.setSelected(false);
    }

    /**
     * Clicks italic button
     */
    public void actionStyleItalic() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-italicButton");
    }

    /**
     * Clicks underline button
     */
    public void actionStyleUnderline() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-underlineButton");
    }

    /**
     * Clicks strikethrough button
     */
    public void actionStyleStrikethrough() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-strikeButton");
    }

    /**
     * Clicks code block button
     */
    public void actionStyleCodeBlock() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-codeBlockButton");
    }

    /**
     * Clicks superscript button
     */
    public void actionStyleSuperscript() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-superscriptButton");
    }

    /**
     * Clicks subscript button
     */
    public void actionStyleSubscript() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-subscriptButton");
    }

    /**
     * Clicks insert table button
     */
    public void actionInsertTable() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-tableButton");
        checkMenuInsertTable.setSelected(false);
    }

    /**
     * Clicks horizontal rule button
     */
    public void actionInsertHorizontalRule() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-horizontalRuleButton");
    }

    /**
     * Clicks insert checkbox button
     */
    public void actionInsertCheckBox() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-checkboxButton");
    }

    /**
     * Clicks add hyperlink button
     */
    public void actionHyperlinkAdd() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-linkButton");
    }

    /**
     * Clicks edit hyperlink if a hyperlink is selected
     * Sets check menu to false once clicked, so that check menu never shows,
     * because this allows proper spacing in the file menu without ever showing the checkbox
     */
    public void actionHyperlinkEdit() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FloatingLinkBar-edit");
        checkMenuEditHyperlink.setSelected(false);
    }

    /**
     * Clicks remove hyperlink if a hyperlink is selected
     */
    public void actionHyperLinkRemove() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FloatingLinkBar-remove");
    }

    /**
     * Clicks remove formatting button
     */
    public void actionRemoveFormatting() {
        JavascriptCode.clickById(webEngine, "gwt-debug-FormattingBar-noFormatButton");
    }

    /**
     * Syncs notebooks and notes by reloading the page
     * If the web page is fully loaded, initiate the load notebook method
     * Sets check menu to false once clicked, so that check menu never shows,
     * because this allows proper spacing in the file menu without ever showing the checkbox
     */
    public void actionSync() {
        webEngine.reload();
        if (webEngine.getLoadWorker().stateProperty().getValue().equals(Worker.State.SUCCEEDED)) {
            loadNotebooks();
        }
        checkMenuSync.setSelected(false);
    }

    /**
     * Clicks on avatar to initiate viewing of account settings button
     * Pauses to ensure menu has loaded, then clicks on account settings button
     */
    public void actionAccountInfo() {
        JavascriptCode.clickById(webEngine, "gwt-debug-AccountMenu-avatar");
        JavascriptCode.pauseJavascriptExecution(1, JavascriptCode::clickAccountSettings);
    }

    /**
     * Loads help and learning url in user's default browser
     * Sets check menu to false once clicked, so that check menu never shows,
     * because this allows proper spacing in the file menu without ever showing the checkbox
     */
    public void actionHelpAndLearning() {
        openLinkInUsersDefaultBrowser("https://help.evernote.com/hc/en-us");
        checkMenuHelpAndLearning.setSelected(false);
    }

    /**
     * Loads getting started url in user's default browser
     */
    public void actionGettingStartedGuide() {
        openLinkInUsersDefaultBrowser("https://help.evernote.com/hc/en-us/articles/209006027-Welcome-to-the-new-Evernote-Web");
    }

    /**
     * Clicks on avatar to initiate viewing of feedback button
     * Clicks on feedback option twice to ensure proper loading
     */
    public void actionRateEvernote() {
        JavascriptCode.clickById(webEngine, "gwt-debug-AccountMenu-avatar");
        JavascriptCode.clickById(webEngine, "gwt-debug-AccountMenu-feedback");
        JavascriptCode.clickById(webEngine, "gwt-debug-AccountMenu-feedback");
    }

    /**
     * Opens settings url in user's default browser
     */
    public void actionGoToMyAccountPage() {
        openLinkInUsersDefaultBrowser("https://www.evernote.com/Settings.action");
    }

    /**
     * Opens github repo url in user's default browser
     */
    public void actionCheckForUpdates() {
        if (!checkUpdates.isUpdateAvailable()) {
            checkUpdates.showUpdateUnavailableDialog();
        }
    }

    /**
     * Opens release notes url in user's default browser
     */
    public void actionReleaseNotes() {
        openLinkInUsersDefaultBrowser("https://github.com/milan102/ForeverNote#new-release-notes");
    }

    /**
     * Opens contact developer url in user's default browser
     */
    public void actionContactDeveloper() {
        openLinkInUsersDefaultBrowser("https://milanisaweso.me/index.php/contact/");
    }

    /**
     * Loads main website url in user's default browser
     */
    public void actionAbout() {
        openLinkInUsersDefaultBrowser("https://evernote.com/");
    }

    /**
     * Opens link user's default browser
     * @param uri is the page to load
     */
    public static void openLinkInUsersDefaultBrowser(String uri) {
        if (Desktop.isDesktopSupported())
        {
            new Thread(() -> {
                try {
                    Desktop.getDesktop().browse(new URI(uri));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Clicks all notes button
     */
    public void actionAllNotes() {
        JavascriptCode.clickById(webEngine, "gwt-debug-Sidebar-notesButton" );
    }

    /**
     * Prints HTML of current page to the console
     */
    public void printHTML() {
        String html = (String) webEngine.executeScript("document.documentElement.outerHTML");
        System.out.println(html);
    }

    /**
     * Adds a listener to the login button
     */
    public void addLoginListener() {
        JSObject jsobj = (JSObject) webEngine.executeScript("window");
        jsobj.setMember("java", this);
        JavascriptCode.pauseJavascriptExecution(5, JavascriptCode::addLoginListener);
    }

    /**
     * Prints text to console
     * @param s is the string to print
     */
    public void print(String s) {
        System.out.println(s);
    }

    /**
     * Gets the main web engine
     * @return the web engine object
     */
    public static WebEngine getWebEngine() {
        return webEngine;
    }

    /**
     * Gets the secondary web view
     * @return the web view object
     */
    public static WebView getWebView2() {
        return webView2;
    }
}
