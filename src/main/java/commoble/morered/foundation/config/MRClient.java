package commoble.morered.foundation.config;

public class MRClient extends ConfigBase {

    public final ConfigGroup client = group(0, "client", Comments.client);


    public final ConfigGroup rendering = group(1, "rendering", Comments.rendering);
    public final ConfigBool showPlacementPreview = b(true, "showPlacementPreview",
            Comments.showPlacementPreview);
    public final ConfigDouble previewPlacementOpacity = d(0.4D, 0, 1D, "previewPlacementOpacity",
            Comments.previewPlacementOpacity);

    @Override
    public String getName() {
        return "client";
    }

    private static class Comments {
        static String client = "Client only settings";
        static String rendering = "Rendering";
        static String showPlacementPreview = "Render preview of plate blocks before placing them";
        static String previewPlacementOpacity = "";
    }
}
