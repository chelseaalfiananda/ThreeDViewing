package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

public class HSRManager {

    private boolean depthTestEnabled = true;
    private boolean cullingEnabled = false;

    public HSRManager() {
        applySettings();
    }

    public void toggleDepthTest() {

        depthTestEnabled = !depthTestEnabled;

        applySettings();

        System.out.println(
                "Z-Buffer: " +
                (depthTestEnabled ? "ON" : "OFF")
        );
    }

    public void toggleCulling() {

        cullingEnabled = !cullingEnabled;

        applySettings();

        System.out.println(
                "Back-Face Culling: " +
                (cullingEnabled ? "ON" : "OFF")
        );
    }

    public void applySettings() {

        if (depthTestEnabled) {

            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);

        } else {

            glDisable(GL_DEPTH_TEST);
        }

        if (cullingEnabled) {

            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
            glFrontFace(GL_CCW);

        } else {

            glDisable(GL_CULL_FACE);
        }
    }

    public boolean isDepthTestEnabled() {
        return depthTestEnabled;
    }

    public boolean isCullingEnabled() {
        return cullingEnabled;
    }
}
