package roadhog360.simpleskinbackport.client;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import roadhog360.simpleskinbackport.configuration.configs.ConfigMain;
import roadhog360.simpleskinbackport.core.Utils;
import roadhog360.simpleskinbackport.ducks.IBoxSizeGetter;
import roadhog360.simpleskinbackport.ducks.ISkinLayer;

/** Draws skin layers with cached geometry and scoped state. All calls run on the render thread. */
public final class SkinLayers {

    private static final SkinLayerCache CACHE = new SkinLayerCache(30, GLAllocation::deleteDisplayLists);
    private static float[][] modelFaces = new float[0][];

    private SkinLayers() {}

    public enum Part {
        HAT,
        JACKET,
        LEFT_SLEEVE,
        RIGHT_SLEEVE,
        LEFT_TROUSER,
        RIGHT_TROUSER
    }

    public static void mark(ModelRenderer renderer, Part part) {
        ((ISkinLayer) renderer).ssb$setSkinLayer(part);
    }

    public static void upload(int texture, BufferedImage image, int x, int y) {
        CACHE.remove(texture);
        if (x == 0 && y == 0
            && image.getWidth() == 64
            && image.getHeight() == 64
            && GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH) == 64
            && GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT) == 64) {
            CACHE.put(texture, image);
        }
    }

    public static void invalidate(int texture) {
        CACHE.remove(texture);
    }

    public static void clear() {
        CACHE.clear();
    }

    public static void cleanUp() {
        CACHE.cleanUp();
    }

    public static boolean render(ModelRenderer renderer, Part part, float scale) {
        if (!enabled(part) || !SkinLayerRenderContext.within(ConfigMain.extrudedLayerDistance)) return false;
        int texture = SkinLayerRenderContext
            .texture(renderer, GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D), GL11.glIsEnabled(GL11.GL_TEXTURE_2D));
        if (!CACHE.contains(texture) && !readSkin(texture)) return false;
        int faceCount = 0;
        for (int i = 0; i < renderer.cubeList.size(); i++) {
            faceCount += ((IBoxSizeGetter) renderer.cubeList.get(i)).ssb$getQuads().length;
        }
        if (modelFaces.length != faceCount) modelFaces = new float[faceCount][21];
        float baseInset = baseInset(renderer, part);
        int faceIndex = 0;
        for (int boxIndex = 0; boxIndex < renderer.cubeList.size(); boxIndex++) {
            ModelBox box = renderer.cubeList.get(boxIndex);
            for (TexturedQuad quad : ((IBoxSizeGetter) box).ssb$getQuads()) {
                float[] face = modelFaces[faceIndex++];
                face[20] = baseInset;
                for (int i = 0; i < 4; i++) {
                    PositionTextureVertex vertex = quad.vertexPositions[i];
                    face[i * 5] = (float) vertex.vector3D.xCoord;
                    face[i * 5 + 1] = (float) vertex.vector3D.yCoord;
                    face[i * 5 + 2] = (float) vertex.vector3D.zCoord;
                    face[i * 5 + 3] = vertex.texturePositionX;
                    face[i * 5 + 4] = vertex.texturePositionY;
                }
            }
        }
        SkinLayerCache.Layer layer = CACHE.get(texture, modelFaces, ConfigMain.extrudedLayerThickness);
        if (layer == null) return false;
        if (layer.empty) return true;
        if (layer.displayList == 0) {
            int displayList = GLAllocation.generateDisplayLists(1);
            GL11.glNewList(displayList, GL11.GL_COMPILE);
            draw(layer.mesh);
            GL11.glEndList();
            layer.compiled(displayList);
        }
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glScalef(scale, scale, scale);
            GL11.glCallList(layer.displayList);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
        return true;
    }

    private static float baseInset(ModelRenderer renderer, Part part) {
        if (part != Part.HAT) return Utils.BODY_LAYER_INFLATION;
        return ((ISkinLayer) renderer).ssb$isBipedPart() ? 0.5F : 0.25F;
    }

    static void draw(SkinLayerMesh mesh) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        for (int i = 0; i < mesh.vertices.length; i += 8) {
            float[] data = mesh.vertices;
            tessellator.setNormal(data[i + 5], data[i + 6], data[i + 7]);
            tessellator.addVertexWithUV(data[i], data[i + 1], data[i + 2], data[i + 3], data[i + 4]);
        }
        tessellator.draw();
    }

    private static boolean enabled(Part part) {
        if (!ConfigMain.extrudedLayers) return false;
        return switch (part) {
            case HAT -> ConfigMain.extrudedHat;
            case JACKET -> ConfigMain.extrudedJacket;
            case LEFT_SLEEVE -> ConfigMain.extrudedLeftSleeve;
            case RIGHT_SLEEVE -> ConfigMain.extrudedRightSleeve;
            case LEFT_TROUSER -> ConfigMain.extrudedLeftTrouser;
            case RIGHT_TROUSER -> ConfigMain.extrudedRightTrouser;
        };
    }

    private static boolean readSkin(int texture) {
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        try {
            if (GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH) != 64
                || GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT) != 64) return false;
            // Recover missing snapshots from the loaded texture without retaining every downloaded image.
            ByteBuffer pixels = BufferUtils.createByteBuffer(64 * 64 * 4);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    int i = (y * 64 + x) * 4;
                    image.setRGB(
                        x,
                        y,
                        (pixels.get(i + 3) & 255) << 24 | (pixels.get(i) & 255) << 16
                            | (pixels.get(i + 1) & 255) << 8
                            | pixels.get(i + 2) & 255);
                }
            }
            CACHE.put(texture, image);
            return true;
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
        }
    }
}
