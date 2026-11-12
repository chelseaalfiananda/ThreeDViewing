
package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

public class StereoAnaglyphRenderer {

    private float eyeSeparation = 0.20f;

    public void setEyeSeparation(float eyeSeparation) {
        this.eyeSeparation = Math.max(
                0.0f,
                Math.min(0.5f, eyeSeparation)
        );
    }

    public float getEyeSeparation() {
        return eyeSeparation;
    }

    public void renderAnaglyph(EyeRenderer renderer) {

        if (renderer == null) {
            return;
        }

        // Bersihkan layar sebelum menggambar kedua mata.
        glColorMask(true, true, true, true);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        try {

            // Mata kiri: merah.
            glColorMask(true, false, false, true);
            glClear(GL_DEPTH_BUFFER_BIT);

            renderer.render(-eyeSeparation / 2.0f);

            // Mata kanan: cyan.
            glColorMask(false, true, true, true);
            glClear(GL_DEPTH_BUFFER_BIT);

            renderer.render(eyeSeparation / 2.0f);

        } finally {

            // Kembalikan seluruh kanal warna.
            glColorMask(true, true, true, true);
        }
    }

    @FunctionalInterface
    public interface EyeRenderer {
        void render(float eyeOffset);
    }
}
