
package com.grafkom.viewing3d;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {

    private final int programID;
    private final Map<String, Integer> uniforms = new HashMap<>();

    public ShaderProgram(
            String vertexPath,
            String fragmentPath
    ) {

        int vertexShader = 0;
        int fragmentShader = 0;

        programID = glCreateProgram();

        try {

            vertexShader = createShader(
                    vertexPath,
                    GL_VERTEX_SHADER
            );

            fragmentShader = createShader(
                    fragmentPath,
                    GL_FRAGMENT_SHADER
            );

            glAttachShader(programID, vertexShader);
            glAttachShader(programID, fragmentShader);

            glLinkProgram(programID);

            if (glGetProgrami(
                    programID,
                    GL_LINK_STATUS
            ) == GL_FALSE) {

                throw new RuntimeException(
                        "Shader linking gagal: "
                        + glGetProgramInfoLog(programID)
                );
            }

        } catch (RuntimeException e) {

            glDeleteProgram(programID);

            throw e;

        } finally {

            if (vertexShader != 0) {
                glDeleteShader(vertexShader);
            }

            if (fragmentShader != 0) {
                glDeleteShader(fragmentShader);
            }
        }
    }

    private int createShader(String path, int type) {

        String source = readShader(path);

        int shaderID = glCreateShader(type);

        glShaderSource(shaderID, source);
        glCompileShader(shaderID);

        if (glGetShaderi(
                shaderID,
                GL_COMPILE_STATUS
        ) == GL_FALSE) {

            String error = glGetShaderInfoLog(shaderID);

            glDeleteShader(shaderID);

            throw new RuntimeException(
                    "Shader compilation gagal pada "
                    + path + ": " + error
            );
        }

        return shaderID;
    }

    private String readShader(String path) {

        try (InputStream input = getClass()
                .getClassLoader()
                .getResourceAsStream(path)) {

            if (input == null) {
                throw new RuntimeException(
                        "Shader tidak ditemukan: " + path
                );
            }

            try (BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(input)
                    )) {

                return reader.lines()
                        .collect(Collectors.joining("\n"));
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Gagal membaca shader: " + path,
                    e
            );
        }
    }

    public void createUniform(String name) {

        int location = glGetUniformLocation(
                programID,
                name
        );

        if (location < 0) {
            throw new RuntimeException(
                    "Uniform tidak ditemukan: " + name
            );
        }

        uniforms.put(name, location);
    }

    private int getUniformLocation(String name) {

        Integer location = uniforms.get(name);

        if (location == null) {
            throw new RuntimeException(
                    "Uniform belum dibuat: " + name
            );
        }

        return location;
    }

    public void use() {
        glUseProgram(programID);
    }

    public void stop() {
        glUseProgram(0);
    }

    public void setBoolean(String name, boolean value) {

        glUniform1i(
                getUniformLocation(name),
                value ? 1 : 0
        );
    }

    public void setInteger(String name, int value) {

        glUniform1i(
                getUniformLocation(name),
                value
        );
    }

    public void setFloat(String name, float value) {

        glUniform1f(
                getUniformLocation(name),
                value
        );
    }

    public void setVector3f(
            String name,
            Vector3f value
    ) {

        glUniform3f(
                getUniformLocation(name),
                value.x,
                value.y,
                value.z
        );
    }

    public void setMatrix4f(
            String name,
            Matrix4f value
    ) {

        float[] matrix = new float[16];

        value.get(matrix);

        glUniformMatrix4fv(
                getUniformLocation(name),
                false,
                matrix
        );
    }

    public int getProgramID() {
        return programID;
    }

    public void cleanup() {
        glDeleteProgram(programID);
    }
}
