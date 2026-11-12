
package com.grafkom.viewing3d;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengl.GL13.*;

public class Texture2D {

    private final int textureID;
    private boolean bilinearFiltering = true;

    public Texture2D(String resourcePath) {

        textureID = glGenTextures();

        ByteBuffer imageBuffer = null;
        ByteBuffer imageData = null;

        try (InputStream input = getClass()
                .getClassLoader()
                .getResourceAsStream(resourcePath)) {

            if (input == null) {
                throw new RuntimeException(
                        "File tekstur tidak ditemukan: " + resourcePath
                );
            }

            byte[] imageBytes = input.readAllBytes();

            imageBuffer = MemoryUtil.memAlloc(imageBytes.length);
            imageBuffer.put(imageBytes).flip();

            STBImage.stbi_set_flip_vertically_on_load(true);

            try (MemoryStack stack = MemoryStack.stackPush()) {

                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                imageData = STBImage.stbi_load_from_memory(
                        imageBuffer,
                        width,
                        height,
                        channels,
                        0
                );

                if (imageData == null) {
                    throw new RuntimeException(
                            "Gagal memuat tekstur: "
                            + STBImage.stbi_failure_reason()
                    );
                }

                int format;

                if (channels.get(0) == 4) {
                    format = GL_RGBA;
                } else if (channels.get(0) == 3) {
                    format = GL_RGB;
                } else if (channels.get(0) == 1) {
                    format = GL_RED;
                } else {
                    throw new RuntimeException(
                            "Format gambar tidak didukung."
                    );
                }

                glBindTexture(GL_TEXTURE_2D, textureID);

                glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

                glTexImage2D(
                        GL_TEXTURE_2D,
                        0,
                        format,
                        width.get(0),
                        height.get(0),
                        0,
                        format,
                        GL_UNSIGNED_BYTE,
                        imageData
                );

                glGenerateMipmap(GL_TEXTURE_2D);

                glTexParameteri(
                        GL_TEXTURE_2D,
                        GL_TEXTURE_WRAP_S,
                        GL_REPEAT
                );

                glTexParameteri(
                        GL_TEXTURE_2D,
                        GL_TEXTURE_WRAP_T,
                        GL_REPEAT
                );

                setFiltering(true);

                glBindTexture(GL_TEXTURE_2D, 0);
            }

        } catch (IOException | RuntimeException e) {

            glBindTexture(GL_TEXTURE_2D, 0);
            glDeleteTextures(textureID);

            throw new RuntimeException(
                    "Gagal memuat tekstur: " + resourcePath,
                    e
            );

        } finally {

            if (imageData != null) {
                STBImage.stbi_image_free(imageData);
            }

            if (imageBuffer != null) {
                MemoryUtil.memFree(imageBuffer);
            }
        }
    }

    public void setFiltering(boolean bilinear) {

        bilinearFiltering = bilinear;

        glBindTexture(GL_TEXTURE_2D, textureID);

        if (bilinearFiltering) {

            glTexParameteri(
                    GL_TEXTURE_2D,
                    GL_TEXTURE_MIN_FILTER,
                    GL_LINEAR_MIPMAP_LINEAR
            );

            glTexParameteri(
                    GL_TEXTURE_2D,
                    GL_TEXTURE_MAG_FILTER,
                    GL_LINEAR
            );

        } else {

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
        }

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void toggleFiltering() {
        setFiltering(!bilinearFiltering);
    }

    public void bind(int textureUnit) {

        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void cleanup() {
        glDeleteTextures(textureID);
    }

    public boolean isBilinearFiltering() {
        return bilinearFiltering;
    }

    public int getTextureID() {
        return textureID;
    }
}
