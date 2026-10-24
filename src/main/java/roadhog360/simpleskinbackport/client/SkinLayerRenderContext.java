package roadhog360.simpleskinbackport.client;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelRenderer;

/** Scoped model rendering state. Inventory and first-person draws are always close. */
public final class SkinLayerRenderContext {

    private static double distanceSquared;
    private static boolean near;
    private static Map<ModelRenderer, Integer> textures;

    private SkinLayerRenderContext() {}

    public static boolean within(double distance) {
        return near || distanceSquared <= distance * distance;
    }

    public static void at(double x, double y, double z, Runnable draw) {
        double previous = distanceSquared;
        Map<ModelRenderer, Integer> previousTextures = textures;
        distanceSquared = x * x + y * y + z * z;
        textures = new IdentityHashMap<>();
        try {
            draw.run();
        } finally {
            distanceSquared = previous;
            textures = previousTextures;
        }
    }

    public static int texture(ModelRenderer renderer, int boundTexture, boolean textured) {
        if (textures == null) return boundTexture;
        if (textured) {
            textures.put(renderer, boundTexture);
            return boundTexture;
        }
        // Equipment can leave another texture bound before the untextured damage redraw.
        return textures.getOrDefault(renderer, boundTexture);
    }

    public static void near(Runnable draw) {
        boolean previous = near;
        near = true;
        try {
            draw.run();
        } finally {
            near = previous;
        }
    }
}
