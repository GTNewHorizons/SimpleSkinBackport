package roadhog360.simpleskinbackport.client;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;

/** Owns skin snapshots and geometry until they are deleted or become idle. */
final class SkinLayerCache {

    private final long idleSeconds;
    private final IntConsumer deleteList;
    private final Cache<Integer, Skin> skins;

    SkinLayerCache(long idleSeconds, IntConsumer deleteList) {
        this.idleSeconds = idleSeconds;
        this.deleteList = deleteList;
        skins = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterAccess(idleSeconds, TimeUnit.SECONDS)
            .removalListener(
                (RemovalNotification<Integer, Skin> removed) -> removed.getValue()
                    .clear())
            .build();
    }

    void put(int texture, BufferedImage image) {
        remove(texture);
        if (image.getWidth() != 64 || image.getHeight() != 64) return;
        BufferedImage snapshot = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        snapshot.setRGB(0, 0, 64, 64, image.getRGB(0, 0, 64, 64, null, 0, 64), 0, 64);
        skins.put(texture, new Skin(snapshot));
    }

    Layer get(int texture, float[][] faces, float thickness) {
        Skin skin = skins.getIfPresent(texture);
        if (skin == null) return null;
        Shape shape = new Shape(faces, thickness);
        Layer layer = skin.layers.getIfPresent(shape);
        if (layer == null) {
            float[][] snapshot = new float[faces.length][];
            for (int i = 0; i < faces.length; i++) snapshot[i] = faces[i].clone();
            layer = new Layer(SkinLayerMesh.create(skin.image, snapshot, thickness));
            skin.layers.put(new Shape(snapshot, thickness), layer);
        }
        return layer;
    }

    void remove(int texture) {
        skins.invalidate(texture);
        skins.cleanUp();
    }

    void clear() {
        skins.invalidateAll();
        skins.cleanUp();
    }

    void cleanUp() {
        skins.cleanUp();
        skins.asMap()
            .values()
            .forEach(skin -> skin.layers.cleanUp());
    }

    private void release(Layer layer) {
        if (layer.displayList != 0) deleteList.accept(layer.displayList);
    }

    static final class Layer {

        SkinLayerMesh mesh;
        final boolean empty;
        int displayList;

        Layer(SkinLayerMesh mesh) {
            this.mesh = mesh;
            empty = mesh.vertices.length == 0;
        }

        void compiled(int displayList) {
            this.displayList = displayList;
            mesh = null;
        }
    }

    private final class Skin {

        final BufferedImage image;
        final Cache<Shape, Layer> layers = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterAccess(idleSeconds, TimeUnit.SECONDS)
            .removalListener((RemovalNotification<Shape, Layer> removed) -> release(removed.getValue()))
            .build();

        Skin(BufferedImage image) {
            this.image = image;
        }

        void clear() {
            layers.invalidateAll();
            layers.cleanUp();
        }
    }

    private static final class Shape {

        final float[][] faces;
        final float thickness;

        Shape(float[][] faces, float thickness) {
            this.faces = faces;
            this.thickness = thickness;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Shape)) return false;
            Shape shape = (Shape) other;
            return thickness == shape.thickness && Arrays.deepEquals(faces, shape.faces);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.deepHashCode(faces) + Float.floatToIntBits(thickness);
        }
    }
}
