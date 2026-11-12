package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera3D {

    private final Vector3f eye;
    private final Vector3f target;
    private final Vector3f up;

    private float radius;
    private float azimuthDeg;
    private float elevationDeg;

    public Camera3D(
            Vector3f eye,
            Vector3f target,
            Vector3f up
    ) {
        this.eye = new Vector3f(eye);
        this.target = new Vector3f(target);
        this.up = new Vector3f(up).normalize();

        this.radius = Math.max(
                2.0f,
                Math.min(eye.distance(target), 20.0f)
        );

        this.azimuthDeg = (float) Math.toDegrees(
                Math.atan2(
                        eye.x - target.x,
                        eye.z - target.z
                )
        );

        this.elevationDeg = (float) Math.toDegrees(
                Math.asin(
                        Math.max(-1.0f, Math.min(
                                1.0f,
                                (eye.y - target.y) / eye.distance(target)
                        ))
                )
        );

        updateOrbitalPosition();
    }

    // Menghitung matriks View LookAt.
    public Matrix4f calculateLookAtMatrix() {
        return calculateLookAtMatrix(0.0f);
    }

    // Menghitung matriks kamera dengan pergeseran mata untuk stereo.
    public Matrix4f calculateLookAtMatrix(float eyeOffset) {
        Vector3f forward = new Vector3f(target)
                .sub(eye)
                .normalize();

        Vector3f right = new Vector3f(forward)
                .cross(up)
                .normalize();

        Vector3f shiftedEye = new Vector3f(eye)
                .fma(eyeOffset, right);

        Vector3f shiftedTarget = new Vector3f(target)
                .fma(eyeOffset, right);

        return new Matrix4f().lookAt(
                shiftedEye,
                shiftedTarget,
                up
        );
    }

    // Memperbarui posisi kamera berdasarkan orbit.
    private void updateOrbitalPosition() {
        float azRad = (float) Math.toRadians(azimuthDeg);
        float elRad = (float) Math.toRadians(elevationDeg);

        float cosElevation = (float) Math.cos(elRad);

        eye.set(
                target.x + radius * cosElevation
                        * (float) Math.sin(azRad),

                target.y + radius
                        * (float) Math.sin(elRad),

                target.z + radius * cosElevation
                        * (float) Math.cos(azRad)
        );
    }

    // Mengatur posisi orbital secara langsung.
    public void updateOrbitalPosition(
            float radius,
            float azimuthDeg,
            float elevationDeg
    ) {
        this.radius = clamp(radius, 2.0f, 20.0f);
        this.azimuthDeg = azimuthDeg;
        this.elevationDeg = clamp(elevationDeg, -85.0f, 85.0f);

        updateOrbitalPosition();
    }

    // Rotasi horizontal kamera.
    public void rotateHorizontal(float amount) {
        azimuthDeg += amount;
        updateOrbitalPosition();
    }

    // Rotasi vertikal kamera.
    public void rotateVertical(float amount) {
        elevationDeg = clamp(
                elevationDeg + amount,
                -85.0f,
                85.0f
        );

        updateOrbitalPosition();
    }

    // Zoom kamera.
    public void zoom(float amount) {
        radius = clamp(radius - amount, 2.0f, 20.0f);
        updateOrbitalPosition();
    }

    private static float clamp(
            float value,
            float min,
            float max
    ) {
        return Math.max(min, Math.min(value, max));
    }

    public Vector3f getEye() {
        return new Vector3f(eye);
    }

    public Vector3f getTarget() {
        return new Vector3f(target);
    }

    public float getRadius() {
        return radius;
    }

    public float getAzimuthDeg() {
        return azimuthDeg;
    }

    public float getElevationDeg() {
        return elevationDeg;
    }
}
