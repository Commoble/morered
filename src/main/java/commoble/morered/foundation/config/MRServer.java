package commoble.morered.foundation.config;

public class MRServer extends ConfigBase {

    public final ConfigGroup server = group(0, "server", Comments.server);
    public final ConfigDouble maxWirePostConnectionRange = d(32D, 0D, Double.MAX_VALUE, "maxWirePostConnectionRange",
            Comments.maxWirePostConnectionRange);

    @Override
    public String getName() {
        return "server";
    }

    private static class Comments {
        static String server = "server";
        static String maxWirePostConnectionRange = "max wire post connection range";
    }
}
