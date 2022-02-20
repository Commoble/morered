package commoble.morered.plate_blocks;

/**
 * Basic logic
 */
@FunctionalInterface
public interface LogicFunction {
    /**
     * @param a The first input for a gate, 90 degrees clockwise from the output
     * @param b The second input for a gate, 180 degrees clockwise from the output
     * @param c The third input for a gate, 270 degrees clockwise from the output
     * @return True if the output should be lit, false if the output should be unlit
     */
    boolean apply(boolean a, boolean b, boolean c);
}
