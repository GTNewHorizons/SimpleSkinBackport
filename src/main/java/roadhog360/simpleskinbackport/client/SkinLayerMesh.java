package roadhog360.simpleskinbackport.client;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/** Pixel prisms built from the model's existing textured faces. */
final class SkinLayerMesh {

    // Four vertices per quad, each with position, texture coordinates and normal.
    final float[] vertices;

    private SkinLayerMesh(float[] vertices) {
        this.vertices = vertices;
    }

    static SkinLayerMesh create(BufferedImage image, float[][] faces, float thickness) {
        Builder output = new Builder();
        for (float[] face : faces) {
            int columns = Math.round(Math.abs(face[8] - face[3]) * image.getWidth());
            int rows = Math.round(Math.abs(face[19] - face[4]) * image.getHeight());
            if (columns == 0 || rows == 0) continue;
            output.faceNormal(face);
            int[] alpha = new int[rows * columns];
            boolean[] merged = new boolean[alpha.length];
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < columns; x++) {
                    float column = (x + 0.5F) / columns;
                    float row = (y + 0.5F) / rows;
                    int pixelX = Math.min(
                        image.getWidth() - 1,
                        Math.max(0, (int) (coordinate(face, 3, column, row) * image.getWidth())));
                    int pixelY = Math.min(
                        image.getHeight() - 1,
                        Math.max(0, (int) (coordinate(face, 4, column, row) * image.getHeight())));
                    alpha[y * columns + x] = image.getRGB(pixelX, pixelY) >>> 24;
                }
            }
            // The original affine UV mapping can span occupied cells with different colors and alpha.
            // Empty cells must remain absent even when a damage pass disables texturing.
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < columns; x++) {
                    int cell = y * columns + x;
                    if (alpha[cell] == 0 || merged[cell]) continue;
                    int width = 1;
                    while (x + width < columns && alpha[cell + width] != 0 && !merged[cell + width]) width++;
                    int height = 1;
                    rectangle: while (y + height < rows) {
                        for (int dx = 0; dx < width; dx++) {
                            int next = (y + height) * columns + x + dx;
                            if (alpha[next] == 0 || merged[next]) break rectangle;
                        }
                        height++;
                    }
                    for (int dy = 0; dy < height; dy++) {
                        int start = (y + dy) * columns + x;
                        Arrays.fill(merged, start, start + width, true);
                    }
                    output.surface(
                        face,
                        (float) x / columns,
                        (float) y / rows,
                        (float) (x + width) / columns,
                        (float) (y + height) / rows,
                        thickness);
                }
            }
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < columns; x++) {
                    int cell = y * columns + x;
                    int opacity = alpha[cell];
                    if (opacity == 0) continue;
                    float x0 = (float) x / columns;
                    float y0 = (float) y / rows;
                    float x1 = (float) (x + 1) / columns;
                    float y1 = (float) (y + 1) / rows;
                    float u = (coordinate(face, 3, x0, y0) + coordinate(face, 3, x1, y1)) * 0.5F;
                    float v = (coordinate(face, 4, x0, y0) + coordinate(face, 4, x1, y1)) * 0.5F;
                    // Sample each wall at its pixel's center, including with filtering or mipmaps.
                    if (y == 0 || alpha[cell - columns] < opacity) output.wall(face, x0, y0, x1, y0, u, v, thickness);
                    if (x == columns - 1 || alpha[cell + 1] < opacity)
                        output.wall(face, x1, y0, x1, y1, u, v, thickness);
                    if (y == rows - 1 || alpha[cell + columns] < opacity)
                        output.wall(face, x1, y1, x0, y1, u, v, thickness);
                    if (x == 0 || alpha[cell - 1] < opacity) output.wall(face, x0, y1, x0, y0, u, v, thickness);
                }
            }
        }
        return new SkinLayerMesh(Arrays.copyOf(output.data, output.size));
    }

    private static float coordinate(float[] face, int axis, float column, float row) {
        return face[axis] + (face[5 + axis] - face[axis]) * column + (face[15 + axis] - face[axis]) * row;
    }

    private static final class Builder {

        private float[] data = new float[1024];
        private int size;
        private final float[] a = new float[5];
        private final float[] b = new float[5];
        private final float[] c = new float[5];
        private final float[] d = new float[5];
        private float nx;
        private float ny;
        private float nz;

        private void faceNormal(float[] face) {
            float ax = face[5] - face[0], ay = face[6] - face[1], az = face[7] - face[2];
            float bx = face[15] - face[0], by = face[16] - face[1], bz = face[17] - face[2];
            nx = ay * bz - az * by;
            ny = az * bx - ax * bz;
            nz = ax * by - ay * bx;
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            nx /= length;
            ny /= length;
            nz /= length;
        }

        private void point(float[] target, float[] face, float column, float row, float depth) {
            for (int i = 0; i < 5; i++) target[i] = coordinate(face, i, column, row);
            target[0] += nx * depth;
            target[1] += ny * depth;
            target[2] += nz * depth;
        }

        private void surface(float[] face, float x0, float y0, float x1, float y1, float thickness) {
            point(a, face, x0, y0, thickness);
            point(b, face, x1, y0, thickness);
            point(c, face, x1, y1, thickness);
            point(d, face, x0, y1, thickness);
            quad(a, b, c, d, nx, ny, nz);
            point(a, face, x0, y0, 0);
            point(b, face, x1, y0, 0);
            point(c, face, x1, y1, 0);
            point(d, face, x0, y1, 0);
            quad(d, c, b, a, -nx, -ny, -nz);
        }

        private void wall(float[] face, float x0, float y0, float x1, float y1, float u, float v, float thickness) {
            point(a, face, x0, y0, 0);
            point(b, face, x1, y1, 0);
            point(c, face, x1, y1, thickness);
            point(d, face, x0, y0, thickness);
            a[3] = b[3] = c[3] = d[3] = u;
            a[4] = b[4] = c[4] = d[4] = v;
            float ax = b[0] - a[0], ay = b[1] - a[1], az = b[2] - a[2];
            float bx = c[0] - a[0], by = c[1] - a[1], bz = c[2] - a[2];
            float normalX = ay * bz - az * by;
            float normalY = az * bx - ax * bz;
            float normalZ = ax * by - ay * bx;
            float length = (float) Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
            quad(a, b, c, d, normalX / length, normalY / length, normalZ / length);
        }

        private void quad(float[] a, float[] b, float[] c, float[] d, float nx, float ny, float nz) {
            if (size + 32 > data.length) data = Arrays.copyOf(data, data.length * 2);
            vertex(a, nx, ny, nz);
            vertex(b, nx, ny, nz);
            vertex(c, nx, ny, nz);
            vertex(d, nx, ny, nz);
        }

        private void vertex(float[] point, float nx, float ny, float nz) {
            System.arraycopy(point, 0, data, size, 5);
            data[size + 5] = nx;
            data[size + 6] = ny;
            data[size + 7] = nz;
            size += 8;
        }
    }
}
