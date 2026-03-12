package roadhog360.simpleskinbackport.mixinplugin;

import static roadhog360.simpleskinbackport.SimpleSkinBackport.MOD_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.MixinEnvironment;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import roadhog360.simpleskinbackport.configuration.configs.ConfigModCompat;

@LateMixin
public class SimpleSkinBackportLateMixins implements ILateMixinLoader {

    public static final MixinEnvironment.Side SIDE = MixinEnvironment.getCurrentEnvironment()
        .getSide();

    @Override
    public String getMixinConfig() {
        return "mixins." + MOD_ID + ".late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        List<String> mixins = new ArrayList<>();
        if (SIDE == MixinEnvironment.Side.CLIENT) {
            if (loadedMods.contains("TwilightForest")) {
                if (ConfigModCompat.TFgiantSkinSet != null) {
                    mixins.add("twilightforest.MixinRenderTFGiant");
                }
            }
            if (loadedMods.contains("Botania")) {
                mixins.add("botania.MixinClientProxy");
                mixins.add("botania.MixinRenderTileSkullOverride");
            }
        }
        return mixins;
    }
}
