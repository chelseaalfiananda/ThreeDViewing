
package com.grafkom.viewing3d;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;

public class ShadowMapFBO {

    private final int width;
    private final int height;

    private int framebufferID;
    private int depthTextureID;

    public ShadowMapFBO(int width, int height) {

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Ukuran Shadow Map harus lebih dari 0."
            );
        }

        this.width = width;
        this.height = height;

        createFramebuffer();
    }

    private void createFramebuffer() {

        framebufferID = glGenFramebuffers();
        depthTextureID = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, depthTextureID);

        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_DEPTH_COMPONENT24,
                width,
                height,
                0,
                GL_DEPTH_COMPONENT,
                GL_FLOAT,
                (java.nio.ByteBuffer) null
        );

        glTexParameteri(
                GL_TEXTURE_2D,
                GL_TEXTURE_MIN_FILTER,
                GL_NEAREST
        );

        glTexParameteri(
                GL_TEXTURE_2D,
                GL_TEXTURE_MAG_FILTER,
                GL_NEAREST
        );

        glTexParameteri(
                GL_TEXTURE_2D,
                GL_TEXTURE_WRAP_S,
                GL_CLAMP_TO_BORDER
        );

        glTexParameteri(
                GL_TEXTURE_2D,
                GL_TEXTURE_WRAP_T,
                GL_CLAMP_TO_BORDER
        );

        glTexParameteri(
                GL_TEXTURE_2D,
                GL_TEXTURE_COMPARE_MODE,
                GL_NONE
        );

        FloatBuffer borderColor =
                BufferUtils.createFloatBuffer(4);

        borderColor.put(
                new float[]{1f, 1f, 1f, 1f}
        ).flip();

        glTexParameterfv(
                GL_TEXTURE_2D,
                GL_TEXTURE_BORDER_COLOR,
                borderColor
        );

        glBindFramebuffer(
                GL_FRAMEBUFFER,
                framebufferID
        );

        glFramebufferTexture2D(
                GL_FRAMEBUFFER,
                GL_DEPTH_ATTACHMENT,
                GL_TEXTURE_2D,
                depthTextureID,
                0
        );

        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        int status =
                glCheckFramebufferStatus(GL_FRAMEBUFFER);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);

        if (status != GL_FRAMEBUFFER_COMPLETE) {

            cleanup();

            throw new RuntimeException(
                    "Shadow Framebuffer tidak lengkap: " + status
            );
        }
    }

    public void bindFramebuffer() {

        glBindFramebuffer(
                GL_FRAMEBUFFER,
                framebufferID
        );

        glViewport(0, 0, width, height);

        glClear(GL_DEPTH_BUFFER_BIT);
    }

    public void unbindFramebuffer(
            int screenWidth,
            int screenHeight
    ) {

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        glViewport(
                0,
                0,
                screenWidth,
                screenHeight
        );
    }

    public void bindDepthTexture(int textureUnit) {

        glActiveTexture(GL_TEXTURE0 + textureUnit);

        glBindTexture(
                GL_TEXTURE_2D,
                depthTextureID
        );
    }

    public int getDepthTextureID() {
        return depthTextureID;
    }

    public int getFramebufferID() {
        return framebufferID;
    }

    public void cleanup() {

        if (framebufferID != 0) {
            glDeleteFramebuffers(framebufferID);
            framebufferID = 0;
        }

        if (depthTextureID != 0) {
            glDeleteTextures(depthTextureID);
            depthTextureID = 0;
        }
    }
}
