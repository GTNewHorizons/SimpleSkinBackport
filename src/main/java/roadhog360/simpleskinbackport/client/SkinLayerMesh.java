package roadhog360.simpleskinbackport.client;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/** Joined pixel shells built from six consecutive textured faces per model box. */
final class SkinLayerMesh {

    // Four vertices per quad, each with position, texture coordinates and normal.
    final float[] vertices;

    private SkinLayerMesh(float[] vertices) {
        this.vertices = vertices;
    }

    static SkinLayerMesh create(BufferedImage image, float[][] faces, float thickness) {
        Builder output = new Builder();
        // The cache owns a contiguous, non-premultiplied ARGB snapshot.
        int[] pixels = ((DataBufferInt) image.getRaster()
            .getDataBuffer()).getData();
        int imageWidth = image.getWidth(), imageHeight = image.getHeight();
        for (int boxStart = 0; boxStart < faces.length; boxStart += 6) {
            Face[] box = new Face[Math.min(6, faces.length - boxStart)];
            for (int i = 0; i < box.length; i++)
                box[i] = new Face(pixels, imageWidth, imageHeight, faces[boxStart + i]);
            joinEdges(box);
            for (Face surface : box) {
                float[] face = surface.vertices;
                int columns = surface.columns;
                int rows = surface.rows;
                if (columns == 0 || rows == 0) continue;
                output.face(face, thickness);
                int[] alpha = surface.alpha;
                int[] joined = new int[alpha.length];
                for (int y = 0; y < rows; y++) {
                    for (int x = 0; x < columns; x++) {
                        if (alpha[y * columns + x] != 0) joined[y * columns + x] = surface.joinedEdges(box, x, y);
                    }
                }
                boolean[] merged = new boolean[alpha.length];
                // The original affine UV mapping can span occupied cells with different colors and alpha.
                // Empty cells must remain absent even when a damage pass disables texturing.
                for (int y = 0; y < rows; y++) {
                    for (int x = 0; x < columns; x++) {
                        int cell = y * columns + x;
                        if (alpha[cell] == 0 || merged[cell]) continue;
                        int width = 1;
                        while (x + width < columns && alpha[cell + width] != 0
                            && !merged[cell + width]
                            && joined[cell + width] == joined[cell]) width++;
                        int height = 1;
                        rectangle: while (y + height < rows) {
                            for (int dx = 0; dx < width; dx++) {
                                int next = (y + height) * columns + x + dx;
                                if (alpha[next] == 0 || merged[next] || joined[next] != joined[cell]) break rectangle;
                            }
                            height++;
                        }
                        for (int dy = 0; dy < height; dy++) {
                            int start = (y + dy) * columns + x;
                            Arrays.fill(merged, start, start + width, true);
                        }
                        output.joinedEdges = joined[cell];
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
                        boolean top = y == 0 ? surface.neighbor(box, 0, x) == 0 : alpha[cell - columns] < opacity;
                        boolean right = x == columns - 1 ? surface.neighbor(box, 1, y) == 0 : alpha[cell + 1] < opacity;
                        boolean bottom = y == rows - 1 ? surface.neighbor(box, 2, columns - 1 - x) == 0
                            : alpha[cell + columns] < opacity;
                        boolean left = x == 0 ? surface.neighbor(box, 3, rows - 1 - y) == 0 : alpha[cell - 1] < opacity;
                        if (!top && !right && !bottom && !left) continue;
                        output.joinedEdges = joined[cell];
                        float x0 = (float) x / columns;
                        float y0 = (float) y / rows;
                        float x1 = (float) (x + 1) / columns;
                        float y1 = (float) (y + 1) / rows;
                        float u = (coordinate(face, 3, x0, y0) + coordinate(face, 3, x1, y1)) * 0.5F;
                        float v = (coordinate(face, 4, x0, y0) + coordinate(face, 4, x1, y1)) * 0.5F;
                        // Sample each wall at its pixel's center, including with filtering or mipmaps.
                        if (top) output.wall(face, x0, y0, x1, y0, u, v, thickness);
                        if (right) output.wall(face, x1, y0, x1, y1, u, v, thickness);
                        if (bottom) output.wall(face, x1, y1, x0, y1, u, v, thickness);
                        if (left) output.wall(face, x0, y1, x0, y0, u, v, thickness);
                    }
                }
            }
        }
        return new SkinLayerMesh(Arrays.copyOf(output.data, output.size));
    }

    private static float coordinate(float[] face, int axis, float column, float row) {
        return face[axis] + (face[5 + axis] - face[axis]) * column + (face[15 + axis] - face[axis]) * row;
    }

    // Pair original cuboid edges geometrically so mirrored UVs don't affect adjacency.
    private static void joinEdges(Face[] box) {
        for (int i = 0; i < box.length; i++) {
            for (int j = i + 1; j < box.length; j++) {
                for (int a = 0; a < 4; a++) {
                    for (int b = 0; b < 4; b++) {
                        boolean reversed = samePoint(box[i].vertices, a, box[j].vertices, (b + 1) % 4)
                            && samePoint(box[i].vertices, (a + 1) % 4, box[j].vertices, b);
                        if (reversed || (samePoint(box[i].vertices, a, box[j].vertices, b)
                            && samePoint(box[i].vertices, (a + 1) % 4, box[j].vertices, (b + 1) % 4))) {
                            box[i].edges[a] = j * 8 + b * 2 + (reversed ? 1 : 0);
                            box[j].edges[b] = i * 8 + a * 2 + (reversed ? 1 : 0);
                        }
                    }
                }
            }
        }
    }

    private static boolean samePoint(float[] a, int ai, float[] b, int bi) {
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(a[ai * 5 + axis] - b[bi * 5 + axis]) > 0.00001F) return false;
        }
        return true;
    }

    private static final class Face {

        private final float[] vertices;
        private final int columns;
        private final int rows;
        private final int[] alpha;
        // Each edge stores its neighbor face, edge and direction in one integer.
        private final int[] edges = { -1, -1, -1, -1 };

        private Face(int[] pixels, int imageWidth, int imageHeight, float[] vertices) {
            this.vertices = vertices.clone();
            // Start on the base cuboid, including its normal plane and tangent footprint.
            for (int axis = 0; axis < 3; axis++) {
                float center = (vertices[axis] + vertices[10 + axis]) * 0.5F;
                int j = (axis + 1) % 3, k = (axis + 2) % 3;
                float normal = Math.signum(
                    (vertices[5 + j] - vertices[j]) * (vertices[15 + k] - vertices[k])
                        - (vertices[5 + k] - vertices[k]) * (vertices[15 + j] - vertices[j]));
                for (int vertex = 0; vertex < 4; vertex++) {
                    int position = vertex * 5 + axis;
                    float tangent = Math.signum(vertices[position] - center);
                    this.vertices[position] -= (tangent == 0 ? normal : tangent) * vertices[20];
                }
            }
            this.vertices[20] = 0;
            columns = Math.round(Math.abs(vertices[8] - vertices[3]) * imageWidth);
            rows = Math.round(Math.abs(vertices[19] - vertices[4]) * imageHeight);
            alpha = new int[rows * columns];
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < columns; x++) {
                    float column = (x + 0.5F) / columns;
                    float row = (y + 0.5F) / rows;
                    int pixelX = Math
                        .min(imageWidth - 1, Math.max(0, (int) (coordinate(vertices, 3, column, row) * imageWidth)));
                    int pixelY = Math
                        .min(imageHeight - 1, Math.max(0, (int) (coordinate(vertices, 4, column, row) * imageHeight)));
                    alpha[y * columns + x] = pixels[pixelY * imageWidth + pixelX] >>> 24;
                }
            }
        }

        private int joinedEdges(Face[] box, int x, int y) {
            // Bits follow the quad's top, right, bottom and left edges.
            int joined = 0;
            if (y == 0 && neighbor(box, 0, x) != 0) joined |= 1;
            if (x == columns - 1 && neighbor(box, 1, y) != 0) joined |= 2;
            if (y == rows - 1 && neighbor(box, 2, columns - 1 - x) != 0) joined |= 4;
            if (x == 0 && neighbor(box, 3, rows - 1 - y) != 0) joined |= 8;
            return joined;
        }

        private int neighbor(Face[] box, int edge, int pixel) {
            int link = edges[edge];
            if (link < 0) return 0;
            Face other = box[link / 8];
            int otherEdge = (link % 8) / 2;
            int count = edge % 2 == 0 ? columns : rows;
            int otherCount = otherEdge % 2 == 0 ? other.columns : other.rows;
            if (otherCount == 0 || other.columns == 0 || other.rows == 0) return 0;
            float position = (pixel + 0.5F) / count;
            if (link % 2 != 0) position = 1 - position;
            int next = Math.min(otherCount - 1, (int) (position * otherCount));
            switch (otherEdge) {
                case 0:
                    return other.alpha[next];
                case 1:
                    return other.alpha[next * other.columns + other.columns - 1];
                case 2:
                    return other.alpha[other.rows * other.columns - 1 - next];
                default:
                    return other.alpha[(other.rows - 1 - next) * other.columns];
            }
        }
    }

    private static final class Builder {

        private float[] data = new float[1024];
        private int size;
        private final float[] a = new float[5];
        private final float[] b = new float[5];
        private final float[] c = new float[5];
        private final float[] d = new float[5];
        private float thickness;
        private int joinedEdges;
        private float nx;
        private float ny;
        private float nz;

        private void face(float[] face, float thickness) {
            this.thickness = thickness;
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
            // Boundary texels extend only toward a painted neighbor on the adjacent face.
            // Front and rear use the same footprint so exposed walls remain parallel.
            for (int axis = 0; axis < 3; axis++) {
                float alongColumn = face[5 + axis] - face[axis];
                float alongRow = face[15 + axis] - face[axis];
                if (alongColumn != 0
                    && ((column == 0 && (joinedEdges & 8) != 0) || (column == 1 && (joinedEdges & 2) != 0)))
                    target[axis] += Math.signum(alongColumn) * (column == 0 ? -thickness : thickness);
                if (alongRow != 0 && ((row == 0 && (joinedEdges & 1) != 0) || (row == 1 && (joinedEdges & 4) != 0)))
                    target[axis] += Math.signum(alongRow) * (row == 0 ? -thickness : thickness);
            }
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
