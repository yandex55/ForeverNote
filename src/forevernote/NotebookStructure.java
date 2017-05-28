package forevernote;

import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Class that creates the actual notebook structure that is shown on the tree view
 */
public class NotebookStructure {
    private List<String> notebookNames;
    private List<String> notebookParents;
    private HashMap<String,String> notebookStacks;
    private TreeItem<String> root;

    /**
     * Creates entire structure
     * Define an image that will be placed next to the root node
     * Get the data from the Notebook class
     * Creates tree items, if the notebook is a parent then creates a family
     */
    public void createNotebookStructure() {
        Node graphic  = new ImageView(new Image("http://i.imgur.com/mj0zd5L.png"));
        notebookNames = Notebooks.getNotebookNamesList();
        notebookParents = Notebooks.getNotebookParentsList();
        notebookStacks = Notebooks.getNotebookStacks();
        root = new TreeItem<>("Notebooks", graphic);
        root.setExpanded(true);

        for (String name : notebookNames) {
            TreeItem<String> treeItem = makeBranch(name, root);

            if (notebookParents.contains(name)) {
                makeFamily(name, treeItem);
            }
        }
    }

    /**
     * Creates a branch under the root node in the tree view
     * @param title is the name of the branch
     * @param root is the root where the branch will be placed under
     * @return the tree item to be placed on the tree view
     */
    public TreeItem<String> makeBranch(String title, TreeItem<String> root) {
        TreeItem<String> item = new TreeItem<>(title);
        item.setExpanded(true);
        root.getChildren().add(item);
        return item;
    }

    /**
     * Creates a branch under the root node in the tree view, with an image
     * @param title is the name of the branch
     * @param graphic is the image to place next to the tree item
     * @param root is the root where the branch will be placed under
     * @return the tree item to be placed on the tree view
     */
    public TreeItem<String> makeBranchWithImage(String title, Node graphic, TreeItem<String> root) {
        TreeItem<String> item = new TreeItem<>(title, graphic);
        item.setExpanded(true);
        root.getChildren().add(item);
        return item;
    }

    /**
     * Construct a tree view family
     * Gets the comma separated children from the hash map using the parent as the key
     * Splits children by comma to get the exact child name
     * Sort the children in alphabetical order
     * @param parentName the string containing the parent name
     * @param parent the tree item object of the parent that gets passed when making a branch
     */
    private void makeFamily(String parentName, TreeItem<String> parent) {
        String commaSeparatedChildren = notebookStacks.get(parentName);
        String[] children = commaSeparatedChildren.split(", ");
        Arrays.sort(children);

        for (String child : children) {
            makeBranch(child, parent);
        }
    }

    /**
     * Gets the notebook structure
     * @return the root of the tree view
     */
    public TreeItem<String> getNotebookStructure() {
        return root;
    }
}
