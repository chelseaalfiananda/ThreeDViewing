
package com.grafkom.viewing3d;

import org.joml.Matrix4f;

public class ProjectionTaxonomyManager {

    public enum ProjectionType {
        ORTHOGRAPHIC,
        ISOMETRIC,
        OBLIQUE_CAVALIER,
        PERSPECTIVE_1POINT,
        PERSPECTIVE_2POINT,
        PERSPECTIVE_3POINT
    }

    private static ProjectionType currentType =
            ProjectionType.PERSPECTIVE_1POINT;

    public static void setCurrentType(ProjectionType type) {
        if (type != null) {
            currentType = type;
        }
    }

    public static ProjectionType getCurrentType() {
        return currentType;
    }

    public static Matrix4f createProjection(
            ProjectionType type,
            float aspect
    ) {

        if (type == null) {
            type = currentType;
        }

        if (aspect <= 0.0f) {
            aspect = 1.0f;
        }

        Matrix4f projection = new Matrix4f();

        switch (type) {

            case ORTHOGRAPHIC:
            case ISOMETRIC:

                projection.ortho(
                        -3.0f * aspect,
                        3.0f * aspect,
                        -3.0f,
                        3.0f,
                        0.1f,
                        50.0f
                );
                break;

            case OBLIQUE_CAVALIER:

                projection.ortho(
                        -3.0f * aspect,
                        3.0f * aspect,
                        -3.0f,
                        3.0f,
                        0.1f,
                        50.0f
                );
                break;

            case PERSPECTIVE_1POINT:
            case PERSPECTIVE_2POINT:
            case PERSPECTIVE_3POINT:
            default:

                projection.perspective(
                        (float) Math.toRadians(45.0f),
                        aspect,
                        0.1f,
                        50.0f
                );
                break;
        }

        return projection;
    }
}
