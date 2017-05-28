package forevernote;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Class that performs key presses
 * Needed for if user manually selects an option that requires keyboard shortcuts
 * Example: Select Edit -> Copy, so Ctrl+C has to be performed
 */
public class KeyboardInput {

    private static Robot robot;

    /**
     * Presses and releases key
     * @param firstKey is the one key to press
     */
    public static void robotSingleKeyPress(int firstKey) {
        try
        {
            robot = new Robot();
            robot.keyPress(firstKey);
            robot.keyRelease(firstKey);
        }
        catch (Exception exp)
        {
            exp.printStackTrace();
        }
    }

    /**
     * Presses and releases both keys
     * @param firstKey is the first key to press
     * @param secondKey is the second key to press
     */
    public static void robotDoubleKeyPress(int firstKey, int secondKey) {
        try
        {
            robot = new Robot();
            robot.keyPress(firstKey);
            robot.keyPress(secondKey);
            robot.keyRelease(firstKey);
            robot.keyRelease(secondKey);
        }
        catch (Exception exp)
        {
            exp.printStackTrace();
        }
    }

    /**
     * Press Alt key
     */
    public static void pressAltKey() {
        robotSingleKeyPress(KeyEvent.VK_ALT);
    }
}
