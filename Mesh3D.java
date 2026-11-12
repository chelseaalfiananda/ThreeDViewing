
package com.grafkom.viewing3d;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Mesh3D {

    public static final int SPHERE = 0;
    public static final int TEAPOT = 1;
    public static final int HOUSE = 2;

    private static class MeshData {
        int vao;
        int vbo;
        int ebo;
        int indexCount;
    }

    private static class Builder {

        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        void vertex(
                float x, float y, float z,
                float nx, float ny, float nz,
                float u, float v
        ) {
            vertices.add(x);
            vertices.add(y);
            vertices.add(z);

            vertices.add(nx);
            vertices.add(ny);
            vertices.add(nz);

            vertices.add(u);
            vertices.add(v);

            indices.add(indices.size());
        }

        void triangle(
                float[] a,
                float[] b,
                float[] c,
                float[] na,
                float[] nb,
                float[] nc,
                float[] uva,
                float[] uvb,
                float[] uvc
        ) {
            vertex(
                    a[0], a[1], a[2],
                    na[0], na[1], na[2],
                    uva[0], uva[1]
            );

            vertex(
                    b[0], b[1], b[2],
                    nb[0], nb[1], nb[2],
                    uvb[0], uvb[1]
            );

            vertex(
                    c[0], c[1], c[2],
                    nc[0], nc[1], nc[2],
                    uvc[0], uvc[1]
            );
        }

        void flatTriangle(
                float[] a,
                float[] b,
                float[] c,
                float[] uva,
                float[] uvb,
                float[] uvc
        ) {
            float[] ab = {
                b[0] - a[0],
                b[1] - a[1],
                b[2] - a[2]
            };

            float[] ac = {
                c[0] - a[0],
                c[1] - a[1],
                c[2] - a[2]
            };

            float[] n = normalize(cross(ab, ac));

            triangle(
                    a, b, c,
                    n, n, n,
                    uva, uvb, uvc
            );
        }

        void quad(
                float[] a,
                float[] b,
                float[] c,
                float[] d
        ) {
            flatTriangle(
                    a, b, c,
                    new float[]{0, 0},
                    new float[]{1, 0},
                    new float[]{1, 1}
            );

            flatTriangle(
                    a, c, d,
                    new float[]{0, 0},
                    new float[]{1, 1},
                    new float[]{0, 1}
            );
        }

        float[] toVertex(
                float x,
                float y,
                float z
        ) {
            return new float[]{x, y, z};
        }

        float[] toNormal(
                float x,
                float y,
                float z
        ) {
            return normalize(new float[]{x, y, z});
        }

        static float[] cross(float[] a, float[] b) {
            return new float[]{
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]
            };
        }

        static float[] normalize(float[] v) {
            float length = (float) Math.sqrt(
                    v[0] * v[0] +
                    v[1] * v[1] +
                    v[2] * v[2]
            );

            if (length < 0.00001f) {
                return new float[]{0, 1, 0};
            }

            return new float[]{
                v[0] / length,
                v[1] / length,
                v[2] / length
            };
        }

        float[] getVertices() {
            float[] result = new float[vertices.size()];

            for (int i = 0; i < vertices.size(); i++) {
                result[i] = vertices.get(i);
            }

            return result;
        }

        int[] getIndices() {
            int[] result = new int[indices.size()];

            for (int i = 0; i < indices.size(); i++) {
                result[i] = indices.get(i);
            }

            return result;
        }
    }

    private final MeshData[] models = new MeshData[3];

    private int currentModel = SPHERE;

    public Mesh3D(int sectors, int stacks) {

        if (sectors < 3 || stacks < 2) {
            throw new IllegalArgumentException(
                    "Sectors minimal 3 dan stacks minimal 2."
            );
        }

        models[SPHERE] = upload(createSphere(sectors, stacks));
        models[TEAPOT] = upload(createTeapot(sectors, stacks));
        models[HOUSE] = upload(createHouse());

        System.out.println("Mesh Sphere, Teapot, dan House berhasil dibuat.");
    }

    // =========================================
    // PEMBUATAN SPHERE
    // =========================================

    private Builder createSphere(
            int sectors,
            int stacks
    ) {

        Builder builder = new Builder();

        for (int i = 0; i < stacks; i++) {

            float lat1 =
                    (float) Math.PI / 2.0f -
                    (float) Math.PI * i / stacks;

            float lat2 =
                    (float) Math.PI / 2.0f -
                    (float) Math.PI * (i + 1) / stacks;

            for (int j = 0; j < sectors; j++) {

                float lon1 =
                        (float) (2.0 * Math.PI * j / sectors);

                float lon2 =
                        (float) (2.0 * Math.PI * (j + 1) / sectors);

                float[] a = spherePoint(lat1, lon1);
                float[] b = spherePoint(lat2, lon1);
                float[] c = spherePoint(lat2, lon2);
                float[] d = spherePoint(lat1, lon2);

                float u1 = (float) j / sectors;
                float u2 = (float) (j + 1) / sectors;

                float v1 = (float) i / stacks;
                float v2 = (float) (i + 1) / stacks;

                builder.triangle(
                        a, b, c,
                        a, b, c,
                        new float[]{u1, v1},
                        new float[]{u1, v2},
                        new float[]{u2, v2}
                );

                builder.triangle(
                        a, c, d,
                        a, c, d,
                        new float[]{u1, v1},
                        new float[]{u2, v2},
                        new float[]{u2, v1}
                );
            }
        }

        return builder;
    }

    private float[] spherePoint(
            float latitude,
            float longitude
    ) {

        float x =
                (float) (Math.cos(latitude) * Math.cos(longitude));

        float y = (float) Math.sin(latitude);

        float z =
                (float) (Math.cos(latitude) * Math.sin(longitude));

        return new float[]{x, y, z};
    }

    // =========================================
    // PEMBUATAN TEAPOT
    // =========================================

    private Builder createTeapot(
            int sectors,
            int stacks
    ) {

        Builder builder = new Builder();

        // Badan teko.
        float[][] bodyProfile = {
            {0.25f, -0.65f},
            {0.48f, -0.55f},
            {0.62f, -0.25f},
            {0.65f, 0.05f},
            {0.55f, 0.38f},
            {0.30f, 0.52f},
            {0.00f, 0.52f}
        };

        addLathe(
                builder,
                bodyProfile,
                sectors
        );

        // Tutup teko.
        float[][] lidProfile = {
            {0.00f, 0.53f},
            {0.30f, 0.53f},
            {0.32f, 0.59f},
            {0.20f, 0.67f},
            {0.08f, 0.70f},
            {0.00f, 0.72f}
        };

        addLathe(
                builder,
                lidProfile,
                sectors
        );

        // Pegangan teko.
        addTorus(
                builder,
                0.39f,
                0.08f,
                -0.70f,
                0.05f,
                0.0f,
                sectors,
                Math.max(12, stacks)
        );

        // Cerat teko.
        addSpout(
                builder,
                sectors
        );

        return builder;
    }

    private void addLathe(
            Builder builder,
            float[][] profile,
            int sectors
    ) {

        for (int i = 0; i < profile.length - 1; i++) {

            float r1 = profile[i][0];
            float y1 = profile[i][1];

            float r2 = profile[i + 1][0];
            float y2 = profile[i + 1][1];

            for (int j = 0; j < sectors; j++) {

                float a1 =
                        (float) (2.0 * Math.PI * j / sectors);

                float a2 =
                        (float) (2.0 * Math.PI * (j + 1) / sectors);

                float[] p1 = lathePoint(r1, y1, a1);
                float[] p2 = lathePoint(r2, y2, a1);
                float[] p3 = lathePoint(r2, y2, a2);
                float[] p4 = lathePoint(r1, y1, a2);

                float[] n1 = latheNormal(a1, r2 - r1, y2 - y1);
                float[] n2 = latheNormal(a2, r2 - r1, y2 - y1);

                float u1 = (float) j / sectors;
                float u2 = (float) (j + 1) / sectors;

                builder.triangle(
                        p1, p2, p3,
                        n1, n1, n2,
                        new float[]{u1, 0},
                        new float[]{u1, 1},
                        new float[]{u2, 1}
                );

                builder.triangle(
                        p1, p3, p4,
                        n1, n2, n2,
                        new float[]{u1, 0},
                        new float[]{u2, 1},
                        new float[]{u2, 0}
                );
            }
        }
    }

    private float[] lathePoint(
            float radius,
            float y,
            float angle
    ) {
        return new float[]{
            radius * (float) Math.cos(angle),
            y,
            radius * (float) Math.sin(angle)
        };
    }

    private float[] latheNormal(
            float angle,
            float radiusChange,
            float heightChange
    ) {

        float[] normal = {
            heightChange * (float) Math.cos(angle),
            -radiusChange,
            heightChange * (float) Math.sin(angle)
        };

        return Builder.normalize(normal);
    }

    private void addTorus(
            Builder builder,
            float majorRadius,
            float minorRadius,
            float centerX,
            float centerY,
            float centerZ,
            int segments,
            int sides
    ) {

        for (int i = 0; i < segments; i++) {

            float u1 =
                    (float) (2.0 * Math.PI * i / segments);

            float u2 =
                    (float) (2.0 * Math.PI * (i + 1) / segments);

            for (int j = 0; j < sides; j++) {

                float v1 =
                        (float) (2.0 * Math.PI * j / sides);

                float v2 =
                        (float) (2.0 * Math.PI * (j + 1) / sides);

                float[] p1 = torusPoint(
                        majorRadius, minorRadius,
                        centerX, centerY, centerZ,
                        u1, v1
                );

                float[] p2 = torusPoint(
                        majorRadius, minorRadius,
                        centerX, centerY, centerZ,
                        u2, v1
                );

                float[] p3 = torusPoint(
                        majorRadius, minorRadius,
                        centerX, centerY, centerZ,
                        u2, v2
                );

                float[] p4 = torusPoint(
                        majorRadius, minorRadius,
                        centerX, centerY, centerZ,
                        u1, v2
                );

                float[] n1 = torusNormal(u1, v1);
                float[] n2 = torusNormal(u2, v1);
                float[] n3 = torusNormal(u2, v2);
                float[] n4 = torusNormal(u1, v2);

                builder.triangle(
                        p1, p2, p3,
                        n1, n2, n3,
                        new float[]{0, 0},
                        new float[]{1, 0},
                        new float[]{1, 1}
                );

                builder.triangle(
                        p1, p3, p4,
                        n1, n3, n4,
                        new float[]{0, 0},
                        new float[]{1, 1},
                        new float[]{0, 1}
                );
            }
        }
    }

    private float[] torusPoint(
            float major,
            float minor,
            float cx,
            float cy,
            float cz,
            float u,
            float v
    ) {

        float ring =
                major + minor * (float) Math.cos(v);

        return new float[]{
            cx + ring * (float) Math.cos(u),
            cy + minor * (float) Math.sin(v),
            cz + ring * (float) Math.sin(u)
        };
    }

    private float[] torusNormal(
            float u,
            float v
    ) {

        return Builder.normalize(new float[]{
            (float) (Math.cos(v) * Math.cos(u)),
            (float) Math.sin(v),
            (float) (Math.cos(v) * Math.sin(u))
        });
    }

    private void addSpout(
            Builder builder,
            int sectors
    ) {

        int rings = 12;
        float radius = 0.13f;

        for (int i = 0; i < rings; i++) {

            float t1 = (float) i / rings;
            float t2 = (float) (i + 1) / rings;

            float x1 = 0.48f + t1 * 0.95f;
            float x2 = 0.48f + t2 * 0.95f;

            float y1 = 0.05f + 0.40f * t1 * t1;
            float y2 = 0.05f + 0.40f * t2 * t2;

            float r1 = radius * (1.0f - 0.35f * t1);
            float r2 = radius * (1.0f - 0.35f * t2);

            for (int j = 0; j < sectors; j++) {

                float a1 =
                        (float) (2.0 * Math.PI * j / sectors);

                float a2 =
                        (float) (2.0 * Math.PI * (j + 1) / sectors);

                float[] p1 = new float[]{
                    x1, y1 + r1 * (float) Math.cos(a1),
                    r1 * (float) Math.sin(a1)
                };

                float[] p2 = new float[]{
                    x2, y2 + r2 * (float) Math.cos(a1),
                    r2 * (float) Math.sin(a1)
                };

                float[] p3 = new float[]{
                    x2, y2 + r2 * (float) Math.cos(a2),
                    r2 * (float) Math.sin(a2)
                };

                float[] p4 = new float[]{
                    x1, y1 + r1 * (float) Math.cos(a2),
                    r1 * (float) Math.sin(a2)
                };

                float[] n1 = new float[]{
                    0,
                    (float) Math.cos(a1),
                    (float) Math.sin(a1)
                };

                float[] n2 = new float[]{
                    0,
                    (float) Math.cos(a2),
                    (float) Math.sin(a2)
                };

                builder.triangle(
                        p1, p2, p3,
                        n1, n1, n2,
                        new float[]{t1, 0},
                        new float[]{t2, 0},
                        new float[]{t2, 1}
                );

                builder.triangle(
                        p1, p3, p4,
                        n1, n2, n2,
                        new float[]{t1, 0},
                        new float[]{t2, 1},
                        new float[]{t1, 1}
                );
            }
        }
    }

    // =========================================
    // PEMBUATAN RUMAH 3D
    // =========================================

    private Builder createHouse() {

        Builder builder = new Builder();

        float x = 1.0f;
        float yBottom = -0.75f;
        float yTop = 0.35f;
        float z = 0.85f;

        // Dinding depan.
        builder.quad(
                new float[]{-x, yBottom, z},
                new float[]{x, yBottom, z},
                new float[]{x, yTop, z},
                new float[]{-x, yTop, z}
        );

        // Dinding belakang.
        builder.quad(
                new float[]{x, yBottom, -z},
                new float[]{-x, yBottom, -z},
                new float[]{-x, yTop, -z},
                new float[]{x, yTop, -z}
        );

        // Dinding kiri.
        builder.quad(
                new float[]{-x, yBottom, -z},
                new float[]{-x, yBottom, z},
                new float[]{-x, yTop, z},
                new float[]{-x, yTop, -z}
        );

        // Dinding kanan.
        builder.quad(
                new float[]{x, yBottom, z},
                new float[]{x, yBottom, -z},
                new float[]{x, yTop, -z},
                new float[]{x, yTop, z}
        );

        // Atap depan dan belakang.
        float roofY = 1.05f;

        builder.quad(
                new float[]{-x, yTop, z},
                new float[]{0, roofY, z},
                new float[]{x, yTop, z},
                new float[]{x, yTop, z - 0.001f}
        );

        builder.quad(
                new float[]{x, yTop, -z},
                new float[]{0, roofY, -z},
                new float[]{-x, yTop, -z},
                new float[]{-x, yTop, -z + 0.001f}
        );

        // Bidang atap kiri.
        builder.quad(
                new float[]{-x, yTop, -z},
                new float[]{0, roofY, -z},
                new float[]{0, roofY, z},
                new float[]{-x, yTop, z}
        );

        // Bidang atap kanan.
        builder.quad(
                new float[]{0, roofY, -z},
                new float[]{x, yTop, -z},
                new float[]{x, yTop, z},
                new float[]{0, roofY, z}
        );

        // Pintu depan.
        builder.quad(
                new float[]{-0.22f, yBottom, z + 0.005f},
                new float[]{0.22f, yBottom, z + 0.005f},
                new float[]{0.22f, 0.15f, z + 0.005f},
                new float[]{-0.22f, 0.15f, z + 0.005f}
        );

        // Jendela depan kiri.
        addWindow(builder, -0.68f, -0.05f, z + 0.01f);

        // Jendela depan kanan.
        addWindow(builder, 0.68f, -0.05f, z + 0.01f);

        return builder;
    }

    private void addWindow(
            Builder builder,
            float centerX,
            float centerY,
            float frontZ
    ) {

        float w = 0.16f;
        float h = 0.18f;

        builder.quad(
                new float[]{
                    centerX - w,
                    centerY - h,
                    frontZ
                },
                new float[]{
                    centerX + w,
                    centerY - h,
                    frontZ
                },
                new float[]{
                    centerX + w,
                    centerY + h,
                    frontZ
                },
                new float[]{
                    centerX - w,
                    centerY + h,
                    frontZ
                }
        );
    }

    // =========================================
    // UPLOAD DATA KE GPU
    // =========================================

    private MeshData upload(Builder builder) {

        float[] vertexArray = builder.getVertices();
        int[] indexArray = builder.getIndices();

        FloatBuffer vertices =
                BufferUtils.createFloatBuffer(vertexArray.length);

        vertices.put(vertexArray);
        vertices.flip();

        IntBuffer indices =
                BufferUtils.createIntBuffer(indexArray.length);

        indices.put(indexArray);
        indices.flip();

        MeshData data = new MeshData();

        data.indexCount = indexArray.length;

        data.vao = glGenVertexArrays();
        data.vbo = glGenBuffers();
        data.ebo = glGenBuffers();

        glBindVertexArray(data.vao);

        glBindBuffer(GL_ARRAY_BUFFER, data.vbo);

        glBufferData(
                GL_ARRAY_BUFFER,
                vertices,
                GL_STATIC_DRAW
        );

        glBindBuffer(
                GL_ELEMENT_ARRAY_BUFFER,
                data.ebo
        );

        glBufferData(
                GL_ELEMENT_ARRAY_BUFFER,
                indices,
                GL_STATIC_DRAW
        );

        int stride = 8 * Float.BYTES;

        glVertexAttribPointer(
                0, 3, GL_FLOAT, false,
                stride, 0
        );

        glEnableVertexAttribArray(0);

        glVertexAttribPointer(
                1, 3, GL_FLOAT, false,
                stride, 3 * Float.BYTES
        );

        glEnableVertexAttribArray(1);

        glVertexAttribPointer(
                2, 2, GL_FLOAT, false,
                stride, 6 * Float.BYTES
        );

        glEnableVertexAttribArray(2);

        glBindVertexArray(0);

        return data;
    }

    // =========================================
    // MEMILIH MODEL
    // =========================================

    public void setModel(int model) {

        if (model < SPHERE || model > HOUSE) {
            throw new IllegalArgumentException(
                    "Model harus 0 (Sphere), 1 (Teapot), atau 2 (House)."
            );
        }

        currentModel = model;

        System.out.println(
                "Model aktif: " + getModelName()
        );
    }

    public int getCurrentModel() {
        return currentModel;
    }

    public String getModelName() {

        switch (currentModel) {

            case SPHERE:
                return "Sphere";

            case TEAPOT:
                return "Teapot";

            case HOUSE:
                return "House";

            default:
                return "Unknown";
        }
    }

    // =========================================
    // RENDER MODEL AKTIF
    // =========================================

    public void render() {

        MeshData data = models[currentModel];

        glBindVertexArray(data.vao);

        glDrawElements(
                GL_TRIANGLES,
                data.indexCount,
                GL_UNSIGNED_INT,
                0
        );

        glBindVertexArray(0);
    }

    // =========================================
    // MEMBERSIHKAN RESOURCE OPENGL
    // =========================================

    public void cleanup() {

        for (MeshData data : models) {

            if (data != null) {

                glDeleteBuffers(data.vbo);
                glDeleteBuffers(data.ebo);
                glDeleteVertexArrays(data.vao);
            }
        }
    }
}
