package forevernote;


import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that retrieves and stores notebook data
 */
public class Notebooks {

    private String leftString;
    private String rightString;
    private Pattern pattern;
    private Matcher matcher;
    private String trimmedHTML;
    private static List<String> notebookHTML;
    private static List<String> notebookNames;
    private static List<String> notebookParents;
    private static HashMap<String,String> notebookStacks;

    /**
     * Constructor that creates new data structures
     */
    public Notebooks() {
        notebookHTML = new ArrayList<>();
        notebookNames = new ArrayList<>();
        notebookParents = new ArrayList<>();
        notebookStacks = new HashMap<>();
    }

    /**
     * Starts process of loading notebooks from HTML
     * @param fullHTML is the raw HTML of the web page
     */
    public void loadFromHTML(String fullHTML) {
        fullHTML = fullHTML.replaceAll("[\n\r]", "");
        leftString = "listNotebooks";
        rightString = "listSearches";
        pattern = setPattern(leftString, rightString);
        matcher = pattern.matcher(fullHTML);

        if (matcher.find()) {
            trimmedHTML = matcher.group(1);
        }
        addEachNotebookHTML();
    }

    /**
     * Extract and add each specific notebook's full HTML code to a list
     */
    private void addEachNotebookHTML() {
        leftString = "\"{\\\"1\\\":{\\\"str\\\":\\\"";
        rightString = "}}}}";
        pattern = setPattern(leftString, rightString);
        matcher = pattern.matcher(trimmedHTML);

        while (matcher.find()) {
            notebookHTML.add(matcher.group(1));
        }

        addEachNotebookName();
    }

    /**
     * Extract and add each notebook name to a list
     * If the notebook contains HTML code that identifies as a stack, builds the stack
     * Else build notebooks normally
     * Sort notebook names in alphabetical order, and ensure that case of first letter is not an issue
     * (i.e. prevent an issue causing A, B,....Z sort and then lowercase 'a' coming after uppercase 'Z'
     * so implementation looks like A, a, B, b....Z)
     */
    private void addEachNotebookName() {
        String notebookIdentifier = "\\\"2\\\":{\\\"str\\\":\\\"";
        String stackIdentifier = "\\\"12\\\":{\\\"str\\\":\\\"";
        rightString = "\\\"}";
        pattern = setPattern(notebookIdentifier, rightString);
        Pattern pattern2 = setPattern(stackIdentifier, rightString);

        for (int i = 0; i < notebookHTML.size(); i++) {
            matcher = pattern.matcher(notebookHTML.get(i));
            Matcher matcher2 = pattern2.matcher(notebookHTML.get(i));

            if (notebookHTML.get(i).contains(stackIdentifier)) {
                buildStackHierarchy(matcher, matcher2);
            } else {
                buildLeaves(matcher);
            }
        }
        Collections.sort(notebookNames, new SortIgnoreCase());
    }

    /**
     * Builds tree of parent and child relationship
     * If parent already exists, then get all children of the parent as a string
     * then add child separated by a comma, and finally add the full child list
     * Else add parent to a list of notebooks and list of parents
     * then add child under parent
     * @param child is to be placed under parent
     * @param parent is to be placed above child
     */
    private void buildStackHierarchy (Matcher child, Matcher parent) {
        while (child.find() && parent.find()) {
            if (notebookStacks.containsKey(parent.group(1))) {
                String oldValue = notebookStacks.get(parent.group(1));
                String newValue = oldValue + ", " + child.group(1);
                notebookStacks.put(parent.group(1), newValue);
            } else {
                notebookNames.add(parent.group(1));
                notebookParents.add(parent.group(1));
                notebookStacks.put(parent.group(1), child.group(1));
            }
        }
    }

    /**
     * Add notebook to a list of notebook names
     * @param leaf is a notebook without a child, directly under the root
     */
    private void buildLeaves(Matcher leaf) {
        while (leaf.find()) {
            notebookNames.add(leaf.group(1));
        }
    }

    /**
     * Set the pattern for
     * @param pattern1 is the string before the item you want
     * @param pattern2 is the string after the item you want
     * @return the pattern
     */
    private Pattern setPattern(String pattern1, String pattern2) {
        return Pattern.compile(Pattern.quote(pattern1) + "(.*?)" + Pattern.quote(pattern2));
    }

    /**
     * Ignores case when sorting notebook list
     */
    public class SortIgnoreCase implements Comparator<Object> {
        public int compare(Object o1, Object o2) {
            String s1 = (String) o1;
            String s2 = (String) o2;
            return s1.toLowerCase().compareTo(s2.toLowerCase());
        }
    }

    /**
     * Get all notebook html
     * @return list of notebook html
     */
    public static List<String> getNotebookHTMLList() {
        return notebookHTML;
    }

    /**
     * Get all notebook names
     * @return list of notebook names
     */
    public static List<String> getNotebookNamesList() {
        return notebookNames;
    }

    /**
     * Get all parent names
     * @return list of parent names
     */
    public static List<String> getNotebookParentsList() {
        return notebookParents;
    }

    /**
     * Get stacks containing parent and children
     * @return hashmap of parent and children
     */
    public static HashMap<String,String> getNotebookStacks() {
        return notebookStacks;
    }

}
