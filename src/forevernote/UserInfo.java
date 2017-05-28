package forevernote;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that retrieves and stores user information
 */
public class UserInfo {

    private String username;
    private String email;
    private String name;
    private String leftString;
    private String rightString;
    private Pattern pattern;
    private Matcher matcher;
    private String trimmedHTML;

    /**
     * Starts process of loading user information from HTML
     * @param fullHTML is the raw HTML of the web page
     */
    public void loadFromHTML(String fullHTML) {
        fullHTML = fullHTML.replaceAll("[\n\r]", "");
        leftString = "getUser";
        rightString = "listNotebooks";
        pattern = setPattern(leftString, rightString);
        matcher = pattern.matcher(fullHTML);

        if (matcher.find()) {
            trimmedHTML = matcher.group(1);
        }
        parseUsername();
        parseEmail();
        parseName();
    }

    /**
     * Parses username from HTML
     */
    private void parseUsername() {
        leftString = "\\\"2\\\":{\\\"str\\\":\\\"";
        rightString = "\\\"},";
        pattern = setPattern(leftString, rightString);
        matcher = pattern.matcher(trimmedHTML);

        while (matcher.find()) {
            username = matcher.group(1);
        }
    }

    /**
     * Parses email from HTML
     */
    private void parseEmail() {
        leftString = "\\\"3\\\":{\\\"str\\\":\\\"";
        rightString = "\\\"},";
        pattern = setPattern(leftString, rightString);
        matcher = pattern.matcher(trimmedHTML);

        while (matcher.find()) {
            email = matcher.group(1);
        }
    }

    /**
     * Parses name from HTML
     */
    private void parseName() {
        leftString = "\\\"4\\\":{\\\"str\\\":\\\"";
        rightString = "\\\"},";
        pattern = setPattern(leftString, rightString);
        matcher = pattern.matcher(trimmedHTML);

        while (matcher.find()) {
            name = matcher.group(1);
        }
    }

    /**
     * Gets the username
     * @return a string containing the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the email
     * @return a string containing the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Gets the name
     * @return a string containing the name
     */
    public String getName() {
        return name;
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

}
