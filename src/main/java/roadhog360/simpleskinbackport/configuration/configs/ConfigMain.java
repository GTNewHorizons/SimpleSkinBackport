package roadhog360.simpleskinbackport.configuration.configs;

import java.io.File;

import roadhog360.simpleskinbackport.configuration.ConfigBase;
import roadhog360.simpleskinbackport.core.DefaultPlayerSkin;

public class ConfigMain extends ConfigBase {

    public static DefaultPlayerSkin.Set defaultSkinSet = DefaultPlayerSkin.Set.ALL_DEFAULTS;
    public static boolean oldSlimArms = false;
    public static boolean extrudedLayers = true;
    public static float extrudedLayerThickness = 0.2F;
    public static float extrudedLayerDistance = 12;
    public static boolean extrudedHat = true;
    public static boolean extrudedJacket = true;
    public static boolean extrudedLeftSleeve = true;
    public static boolean extrudedRightSleeve = true;
    public static boolean extrudedLeftTrouser = true;
    public static boolean extrudedRightTrouser = true;

    static final String catSkins = "skins";
    static final String catHeads = "heads";
    static final String catLayers = "client.extrudedLayers";

    public ConfigMain(File file) {
        super(file);

        getCategory(catSkins).setComment("Settings for mod-specific patches.");

        configCats.add(getCategory(catSkins));
        configCats.add(getCategory(catHeads));
        getCategory(catLayers).setComment("Optional per-pixel thickness for outer skin layers.");
        configCats.add(getCategory(catLayers));
    }

    @Override
    protected void syncConfigOptions() {
        extrudedLayers = getBoolean("enabled", catLayers, true, "Render occupied outer skin pixels with thickness.");
        extrudedLayerThickness = getFloat(
            "thickness",
            catLayers,
            0.2F,
            0.05F,
            1F,
            "Thickness in model pixels from the base skin. Larger values can intersect equipment.");
        extrudedLayerDistance = getFloat(
            "distance",
            catLayers,
            12,
            1,
            64,
            "Maximum render distance in blocks. Farther players and placed heads use flat layers.");
        extrudedHat = getBoolean(
            "hat",
            catLayers,
            true,
            "Extrude the hat layer, including player head items and blocks.");
        extrudedJacket = getBoolean("jacket", catLayers, true, "Extrude the jacket layer.");
        extrudedLeftSleeve = getBoolean("leftSleeve", catLayers, true, "Extrude the left sleeve.");
        extrudedRightSleeve = getBoolean(
            "rightSleeve",
            catLayers,
            true,
            "Extrude the right sleeve, including first person.");
        extrudedLeftTrouser = getBoolean("leftTrouser", catLayers, true, "Extrude the left trouser layer.");
        extrudedRightTrouser = getBoolean("rightTrouser", catLayers, true, "Extrude the right trouser layer.");
        defaultSkinSet = DefaultPlayerSkin.Set.valueOf(
            getString(
                "defaultSkinSet",
                catSkins,
                DefaultPlayerSkin.Set.ALL_DEFAULTS.name(),
                """
                    What default skin set should we use for players? This only affects players whose skins are not loaded, or do not have a custom skin.
                    Note that players may also set their skin to one of the new defaults in their Minecraft launcher, and that will take precedent over this option.
                    Each set contains the following skins:
                    """
                    + ConfigBase.getSkinReplacementDescriptions(),
                ConfigBase.getSkinReplacementModes(false)));
        oldSlimArms = getBoolean(
            "oldSlimArms",
            catSkins,
            false,
            """
                Before 1.15, slim-armed skins were a half-pixel lower than the player's torso. Set this to true to re-enable that behavior.
                """);
    }
}
