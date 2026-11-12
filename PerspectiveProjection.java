package com.grafkom.viewing3d;

import org.joml.Matrix4f;

public class PerspectiveProjection {

    private float fovDegree;
    private float aspectRatio;
    private float zNear;
    private float zFar;

    public PerspectiveProjection(
            float fovDegree,
            float aspectRatio,
            float zNear,
            float zFar
    ) {
        setFovDegree(fovDegree);
        setAspectRatio(aspectRatio);
        setClippingPlanes(zNear, zFar);
    }

    // Menghitung matriks proyeksi perspektif
    public Matrix4f calculateProjectionMatrix() {
        return new Matrix4f().perspective(
                (float) Math.toRadians(fovDegree),
                aspectRatio,
                zNear,
                zFar
        );
    }

    // Mengatur Field of View
    public void setFovDegree(float fovDegree) {
        if (fovDegree <= 1.0f || fovDegree >= 179.0f) {
            throw new IllegalArgumentException(
                    "FOV harus antara 1 dan 179 derajat"
            );
        }

        this.fovDegree = fovDegree;
    }

    // Mengatur rasio layar
    public void setAspectRatio(float aspectRatio) {
        if (aspectRatio <= 0.0f) {
            throw new IllegalArgumentException(
                    "Aspect ratio harus lebih besar dari 0"
            );
        }

        this.aspectRatio = aspectRatio;
    }

    // Mengatur jarak Near dan Far
    public void setClippingPlanes(float zNear, float zFar) {
        if (zNear <= 0.0f || zFar <= zNear) {
            throw new IllegalArgumentException(
                    "Nilai clipping plane tidak valid"
            );
        }

        this.zNear = zNear;
        this.zFar = zFar;
    }

    public float getFovDegree() {
        return fovDegree;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public float getZNear() {
        return zNear;
    }

    public float getZFar() {
        return zFar;
    }
}
