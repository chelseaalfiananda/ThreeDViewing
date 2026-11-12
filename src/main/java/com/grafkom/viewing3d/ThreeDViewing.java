
package com.grafkom.viewing3d;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ThreeDViewing {

    private static long window;

    private static int windowWidth = 1000;
    private static int windowHeight = 700;

    private static final int SHADOW_WIDTH = 2048;
    private static final int SHADOW_HEIGHT = 2048;

    private static boolean glInitialized = false;

    private static Camera3D camera;
    private static StereoAnaglyphRenderer stereo;
    private static Mesh3D mesh;

    private static ShaderProgram lightingShader;
    private static ShaderProgram shadowShader;

    private static ShadowMapFBO shadowMap;
    private static HSRManager hsrManager;
    private static Texture2D texture;

    private static int groundVAO;
    private static int groundVBO;

    private static int gridVAO;
    private static int gridVBO;
    private static int gridVertexCount;

    private static ProjectionTaxonomyManager.ProjectionType projectionType =
            ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_1POINT;

    private static boolean stereoMode = false;
    private static boolean useBlinnPhong = false;
    private static boolean useFlatShading = false;
    private static boolean useTexture = false;

    private static float cameraRadius = 8.0f;
    private static float cameraAzimuth = 0.0f;
    private static float cameraElevation = 0.0f;

    private static final Vector3f lightPosition =
            new Vector3f(-4.0f, 6.0f, 4.0f);

    private static final Vector3f lightColor =
            new Vector3f(1.0f, 1.0f, 1.0f);

    private static final Vector3f objectColor =
            new Vector3f(0.25f, 0.55f, 0.95f);

    private static double lastMouseX;
    private static double lastMouseY;

    private static boolean mouseDragging = false;

    public static void main(String[] args) {

        try {
            run();

        } catch (Exception e) {
            e.printStackTrace();

        } finally {

            cleanup();

            if (window != 0L) {
                glfwDestroyWindow(window);
            }

            glfwTerminate();
        }
    }

    private static void run() {
        init();
        loop();
    }

    private static void init() {

        if (!glfwInit()) {
            throw new IllegalStateException(
                    "Gagal menginisialisasi GLFW."
            );
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);

        glfwWindowHint(
                GLFW_OPENGL_PROFILE,
                GLFW_OPENGL_CORE_PROFILE
        );

        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        window = glfwCreateWindow(
                windowWidth,
                windowHeight,
                "3D Viewing Interactive Visualizer",
                0L,
                0L
        );

        if (window == 0L) {
            throw new IllegalStateException(
                    "Gagal membuat window GLFW."
            );
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);

        GL.createCapabilities();
        glInitialized = true;

        glViewport(
                0,
                0,
                windowWidth,
                windowHeight
        );

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        glEnable(GL_MULTISAMPLE);
        glDisable(GL_CULL_FACE);

        glClearColor(
                0.07f,
                0.08f,
                0.12f,
                1.0f
        );

        camera = new Camera3D(
                new Vector3f(0.0f, 0.0f, 8.0f),
                new Vector3f(0.0f, 0.0f, 0.0f),
                new Vector3f(0.0f, 1.0f, 0.0f)
        );

        stereo = new StereoAnaglyphRenderer();
        stereo.setEyeSeparation(0.20f);

        mesh = new Mesh3D(48, 32);

        hsrManager = new HSRManager();

        lightingShader = new ShaderProgram(
                "shaders/lighting.vert",
                "shaders/lighting.frag"
        );

        shadowShader = new ShaderProgram(
                "shaders/shadow.vert",
                "shaders/shadow.frag"
        );

        setupLightingUniforms();
        setupShadowUniforms();

        shadowMap = new ShadowMapFBO(
                SHADOW_WIDTH,
                SHADOW_HEIGHT
        );

        createGround();
        createGrid();

        loadTexture();

        createCallbacks();

        updateWindowTitle();

        System.out.println("================================");
        System.out.println("3D VIEWING BERHASIL DIINISIALISASI");
        System.out.println("================================");
        System.out.println("7 : Sphere");
        System.out.println("8 : Teapot");
        System.out.println("9 : House");
        System.out.println("--------------------------------");
        System.out.println("B : Phong / Blinn-Phong");
        System.out.println("F : Flat / Smooth Shading");
        System.out.println("Z : Depth Test ON/OFF");
        System.out.println("C : Back-Face Culling ON/OFF");
        System.out.println("T : Texture Filtering");
        System.out.println("S : Stereo Anaglyph ON/OFF");
        System.out.println("1-6 : Jenis Proyeksi");
        System.out.println("Panah / Mouse : Rotasi Kamera");
        System.out.println("+ / - : Zoom");
        System.out.println("================================");
    }

    // Mengatur posisi objek agar berada di atas lantai.
    // Matriks ini digunakan pada Shadow Pass dan Lighting Pass.

    private static Matrix4f createObjectModelMatrix() {

        Matrix4f model = new Matrix4f();

        switch (mesh.getCurrentModel()) {

            case Mesh3D.SPHERE:

                model.translate(0.0f, 0.0f, 0.0f);
                break;

            case Mesh3D.TEAPOT:

                model.translate(0.0f, -0.45f, 0.0f);
                break;

            case Mesh3D.HOUSE:

                model.translate(0.0f, -0.35f, 0.0f);
                break;

            default:

                model.identity();
                break;
        }

        return model;
    }

    // Memperbarui seluruh informasi pada title bar.

    private static void updateWindowTitle() {

        if (window == 0L || hsrManager == null || mesh == null) {
            return;
        }

        String shading =
                useBlinnPhong ? "Blinn-Phong" : "Phong";

        String flatShading =
                useFlatShading ? "Flat" : "Smooth";

        String depth =
                hsrManager.isDepthTestEnabled() ? "ON" : "OFF";

        String culling =
                hsrManager.isCullingEnabled() ? "ON" : "OFF";

        String textureStatus =
                useTexture && texture != null ? "ON" : "OFF";

        String filtering = "Tidak tersedia";

        if (texture != null) {
            filtering = texture.isBilinearFiltering()
                    ? "Bilinear Mipmapped"
                    : "Nearest";
        }

        String stereoStatus =
                stereoMode ? "ON" : "OFF";

        String title =
                "3D VIEWING"
                + " | Model: " + mesh.getModelName()
                + " | B: " + shading
                + " | F: " + flatShading
                + " | Z: " + depth
                + " | C: " + culling
                + " | T: " + textureStatus
                + " (" + filtering + ")"
                + " | S: " + stereoStatus
                + " | Projection: " + projectionType;

        glfwSetWindowTitle(window, title);
    }

    private static void setupLightingUniforms() {

        lightingShader.createUniform("model");
        lightingShader.createUniform("view");
        lightingShader.createUniform("projection");
        lightingShader.createUniform("lightSpaceMatrix");

        lightingShader.createUniform("lightPos");
        lightingShader.createUniform("viewPos");
        lightingShader.createUniform("lightColor");
        lightingShader.createUniform("objectColor");

        lightingShader.createUniform("textureSampler");
        lightingShader.createUniform("shadowMap");

        lightingShader.createUniform("useTexture");
        lightingShader.createUniform("useBlinnPhong");
        lightingShader.createUniform("useFlatShading");
    }

    private static void setupShadowUniforms() {

        shadowShader.createUniform("model");
        shadowShader.createUniform("lightSpaceMatrix");
    }

    private static void loadTexture() {

        try {

            texture = new Texture2D(
                    "textures/texture.png"
            );

            useTexture = true;

            System.out.println(
                    "Texture berhasil dimuat."
            );

        } catch (Exception e) {

            texture = null;
            useTexture = false;

            System.out.println(
                    "Texture tidak ditemukan. Menggunakan warna objek."
            );
        }
    }

    private static void createCallbacks() {

        glfwSetFramebufferSizeCallback(
                window,
                (win, width, height) -> {

                    windowWidth = Math.max(width, 1);
                    windowHeight = Math.max(height, 1);

                    glViewport(
                            0,
                            0,
                            windowWidth,
                            windowHeight
                    );
                }
        );

        glfwSetKeyCallback(
                window,
                (win, key, scancode, action, mods) -> {

                    if (action != GLFW_PRESS) {
                        return;
                    }

                    switch (key) {

                        case GLFW_KEY_ESCAPE:

                            glfwSetWindowShouldClose(
                                    window,
                                    true
                            );

                            break;

                        // MEMILIH MODEL 3D

                        case GLFW_KEY_7:

                            mesh.setModel(Mesh3D.SPHERE);

                            System.out.println("Model aktif: Sphere");

                            updateWindowTitle();

                            break;

                        case GLFW_KEY_8:

                            mesh.setModel(Mesh3D.TEAPOT);

                            System.out.println("Model aktif: Teapot");

                            updateWindowTitle();

                            break;

                        case GLFW_KEY_9:

                            mesh.setModel(Mesh3D.HOUSE);

                            System.out.println("Model aktif: House");

                            updateWindowTitle();

                            break;

                        // MEMILIH PROYEKSI

                        case GLFW_KEY_1:

                            changeProjection(
                                    ProjectionTaxonomyManager.ProjectionType.ORTHOGRAPHIC
                            );

                            break;

                        case GLFW_KEY_2:

                            changeProjection(
                                    ProjectionTaxonomyManager.ProjectionType.ISOMETRIC
                            );

                            break;

                        case GLFW_KEY_3:

                            changeProjection(
                                    ProjectionTaxonomyManager.ProjectionType.OBLIQUE_CAVALIER
                            );

                            break;

                        case GLFW_KEY_4:

                            changeProjection(
                                    ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_1POINT
                            );

                            break;

                        case GLFW_KEY_5:

                            changeProjection(
                                    ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_2POINT
                            );

                            break;

                        case GLFW_KEY_6:

                            changeProjection(
                                    ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_3POINT
                            );

                            break;

                        // PHONG / BLINN-PHONG

                        case GLFW_KEY_B:

                            useBlinnPhong = !useBlinnPhong;

                            System.out.println(
                                    "Shading: "
                                    + (useBlinnPhong
                                            ? "Blinn-Phong"
                                            : "Phong")
                            );

                            updateWindowTitle();

                            break;

                        // FLAT / SMOOTH SHADING

                        case GLFW_KEY_F:

                            useFlatShading = !useFlatShading;

                            System.out.println(
                                    "Shading: "
                                    + (useFlatShading
                                            ? "Flat"
                                            : "Smooth")
                            );

                            updateWindowTitle();

                            break;

                        // DEPTH TEST

                        case GLFW_KEY_Z:

                            hsrManager.toggleDepthTest();

                            updateWindowTitle();

                            break;

                        // BACK-FACE CULLING

                        case GLFW_KEY_C:

                            hsrManager.toggleCulling();

                            updateWindowTitle();

                            break;

                        // TEXTURE FILTERING

                        case GLFW_KEY_T:

                            if (texture != null) {

                                texture.toggleFiltering();

                                System.out.println(
                                        "Texture Filtering: "
                                        + (texture.isBilinearFiltering()
                                                ? "Bilinear Mipmapped"
                                                : "Nearest")
                                );

                            } else {

                                System.out.println(
                                        "Texture tidak tersedia."
                                );
                            }

                            updateWindowTitle();

                            break;

                        // STEREO ANAGLYPH

                        case GLFW_KEY_S:

                            stereoMode = !stereoMode;

                            System.out.println(
                                    "Stereo Mode: "
                                    + (stereoMode
                                            ? "ON"
                                            : "OFF")
                            );

                            updateWindowTitle();

                            break;

                        // ROTASI KAMERA

                        case GLFW_KEY_LEFT:

                            cameraAzimuth -= 5.0f;

                            break;

                        case GLFW_KEY_RIGHT:

                            cameraAzimuth += 5.0f;

                            break;

                        case GLFW_KEY_UP:

                            cameraElevation += 5.0f;

                            break;

                        case GLFW_KEY_DOWN:

                            cameraElevation -= 5.0f;

                            break;

                        // ZOOM

                        case GLFW_KEY_EQUAL:
                        case GLFW_KEY_KP_ADD:

                            cameraRadius -= 0.5f;

                            break;

                        case GLFW_KEY_MINUS:
                        case GLFW_KEY_KP_SUBTRACT:

                            cameraRadius += 0.5f;

                            break;
                    }

                    cameraRadius = clamp(
                            cameraRadius,
                            2.0f,
                            20.0f
                    );

                    cameraElevation = clamp(
                            cameraElevation,
                            -85.0f,
                            85.0f
                    );
                }
        );

        glfwSetMouseButtonCallback(
                window,
                (win, button, action, mods) -> {

                    if (button == GLFW_MOUSE_BUTTON_LEFT) {

                        mouseDragging = action == GLFW_PRESS;

                        if (mouseDragging) {

                            double[] xpos = new double[1];
                            double[] ypos = new double[1];

                            glfwGetCursorPos(
                                    window,
                                    xpos,
                                    ypos
                            );

                            lastMouseX = xpos[0];
                            lastMouseY = ypos[0];
                        }
                    }
                }
        );

        glfwSetCursorPosCallback(
                window,
                (win, xpos, ypos) -> {

                    if (mouseDragging) {

                        double dx = xpos - lastMouseX;
                        double dy = ypos - lastMouseY;

                        cameraAzimuth += (float) dx * 0.4f;

                        cameraElevation -= (float) dy * 0.4f;

                        cameraElevation = clamp(
                                cameraElevation,
                                -85.0f,
                                85.0f
                        );

                        lastMouseX = xpos;
                        lastMouseY = ypos;
                    }
                }
        );

        glfwSetScrollCallback(
                window,
                (win, xoffset, yoffset) -> {

                    cameraRadius -= (float) yoffset * 0.5f;

                    cameraRadius = clamp(
                            cameraRadius,
                            2.0f,
                            20.0f
                    );
                }
        );
    }

    private static void changeProjection(
            ProjectionTaxonomyManager.ProjectionType type
    ) {

        projectionType = type;

        ProjectionTaxonomyManager.setCurrentType(type);

        switch (type) {

            case ORTHOGRAPHIC:

                cameraAzimuth = 0.0f;
                cameraElevation = 0.0f;

                break;

            case ISOMETRIC:

                cameraAzimuth = 45.0f;
                cameraElevation = 35.264f;

                break;

            case OBLIQUE_CAVALIER:

                cameraAzimuth = 0.0f;
                cameraElevation = 0.0f;
                cameraRadius = 8.0f;

                break;

            case PERSPECTIVE_1POINT:

                cameraAzimuth = 0.0f;
                cameraElevation = 0.0f;

                break;

            case PERSPECTIVE_2POINT:

                cameraAzimuth = 35.0f;
                cameraElevation = 0.0f;

                break;

            case PERSPECTIVE_3POINT:

                cameraAzimuth = 35.0f;
                cameraElevation = 25.0f;

                break;
        }

        System.out.println(
                "Proyeksi aktif: " + type
        );

        updateWindowTitle();
    }

    private static void loop() {

        while (!glfwWindowShouldClose(window)) {

            float aspect =
                    (float) windowWidth / (float) windowHeight;

            camera.updateOrbitalPosition(
                    cameraRadius,
                    cameraAzimuth,
                    cameraElevation
            );

            Matrix4f projection =
                    ProjectionTaxonomyManager.createProjection(
                            projectionType,
                            aspect
                    );

            if (projectionType ==
                    ProjectionTaxonomyManager.ProjectionType.OBLIQUE_CAVALIER) {

                float shear = 0.7071067f;

                projection.m20(shear);
                projection.m21(shear);
            }

            Matrix4f lightProjection =
                    new Matrix4f().ortho(
                            -10.0f,
                            10.0f,
                            -10.0f,
                            10.0f,
                            1.0f,
                            25.0f
                    );

            Matrix4f lightView =
                    new Matrix4f().lookAt(
                            lightPosition,
                            new Vector3f(0.0f, 0.0f, 0.0f),
                            new Vector3f(0.0f, 1.0f, 0.0f)
                    );

            Matrix4f lightSpaceMatrix =
                    lightProjection.mul(lightView);

            renderShadowPass(lightSpaceMatrix);

            if (stereoMode) {

                stereo.renderAnaglyph(eyeOffset -> {

                    Matrix4f eyeView =
                            camera.calculateLookAtMatrix(eyeOffset);

                    renderLightingPass(
                            projection,
                            eyeView,
                            lightSpaceMatrix
                    );
                });

            } else {

                glColorMask(
                        true,
                        true,
                        true,
                        true
                );

                glClear(
                        GL_COLOR_BUFFER_BIT |
                        GL_DEPTH_BUFFER_BIT
                );

                Matrix4f view =
                        camera.calculateLookAtMatrix(0.0f);

                renderLightingPass(
                        projection,
                        view,
                        lightSpaceMatrix
                );
            }

            glColorMask(
                    true,
                    true,
                    true,
                    true
            );

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private static void renderShadowPass(
            Matrix4f lightSpaceMatrix
    ) {

        boolean depthWasEnabled =
                hsrManager.isDepthTestEnabled();

        glEnable(GL_DEPTH_TEST);

        shadowMap.bindFramebuffer();

        glViewport(
                0,
                0,
                SHADOW_WIDTH,
                SHADOW_HEIGHT
        );

        glClear(GL_DEPTH_BUFFER_BIT);

        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(2.0f, 4.0f);

        shadowShader.use();

        shadowShader.setMatrix4f(
                "lightSpaceMatrix",
                lightSpaceMatrix
        );

        // Model objek utama.

        Matrix4f model = createObjectModelMatrix();

        shadowShader.setMatrix4f(
                "model",
                model
        );

        mesh.render();

        // Model lantai.

        Matrix4f groundModel =
                new Matrix4f()
                        .translate(0.0f, -1.2f, 0.0f)
                        .scale(5.0f, 0.1f, 5.0f);

        shadowShader.setMatrix4f(
                "model",
                groundModel
        );

        glBindVertexArray(groundVAO);

        glDrawArrays(
                GL_TRIANGLES,
                0,
                6
        );

        glBindVertexArray(0);

        shadowShader.stop();

        glDisable(GL_POLYGON_OFFSET_FILL);

        shadowMap.unbindFramebuffer(
                windowWidth,
                windowHeight
        );

        if (!depthWasEnabled) {
            glDisable(GL_DEPTH_TEST);
        }
    }

    private static void renderLightingPass(
            Matrix4f projection,
            Matrix4f view,
            Matrix4f lightSpaceMatrix
    ) {

        lightingShader.use();

        lightingShader.setMatrix4f(
                "projection",
                projection
        );

        lightingShader.setMatrix4f(
                "view",
                view
        );

        lightingShader.setMatrix4f(
                "lightSpaceMatrix",
                lightSpaceMatrix
        );

        lightingShader.setVector3f(
                "lightPos",
                lightPosition
        );

        Vector3f cameraPosition =
                new Matrix4f(view)
                        .invert()
                        .getTranslation(new Vector3f());

        lightingShader.setVector3f(
                "viewPos",
                cameraPosition
        );

        lightingShader.setVector3f(
                "lightColor",
                lightColor
        );

        lightingShader.setBoolean(
                "useTexture",
                useTexture && texture != null
        );

        lightingShader.setBoolean(
                "useBlinnPhong",
                useBlinnPhong
        );

        lightingShader.setBoolean(
                "useFlatShading",
                useFlatShading
        );

        lightingShader.setInteger(
                "textureSampler",
                0
        );

        lightingShader.setInteger(
                "shadowMap",
                1
        );

        if (texture != null && useTexture) {
            texture.bind(0);
        }

        shadowMap.bindDepthTexture(1);

        // OBJEK UTAMA.

        Matrix4f model = createObjectModelMatrix();

        lightingShader.setMatrix4f(
                "model",
                model
        );

        lightingShader.setVector3f(
                "objectColor",
                objectColor
        );

        mesh.render();

        // LANTAI.

        lightingShader.setBoolean(
                "useTexture",
                false
        );

        lightingShader.setVector3f(
                "objectColor",
                new Vector3f(0.30f, 0.32f, 0.38f)
        );

        Matrix4f groundModel =
                new Matrix4f()
                        .translate(0.0f, -1.2f, 0.0f)
                        .scale(5.0f, 0.1f, 5.0f);

        lightingShader.setMatrix4f(
                "model",
                groundModel
        );

        glBindVertexArray(groundVAO);

        glDrawArrays(
                GL_TRIANGLES,
                0,
                6
        );

        glBindVertexArray(0);

        // GRID LANTAI.

        lightingShader.setVector3f(
                "objectColor",
                new Vector3f(0.55f, 0.58f, 0.65f)
        );

        Matrix4f gridModel =
                new Matrix4f()
                        .translate(0.0f, -1.09f, 0.0f);

        lightingShader.setMatrix4f(
                "model",
                gridModel
        );

        glBindVertexArray(gridVAO);

        glDrawArrays(
                GL_LINES,
                0,
                gridVertexCount
        );

        glBindVertexArray(0);

        // LEPASKAN TEKSTUR DARI UNIT 0.

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);

        // LEPASKAN SHADOW MAP DARI UNIT 1.

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, 0);

        glActiveTexture(GL_TEXTURE0);

        lightingShader.stop();
    }

    private static void createGround() {

        float[] vertices = {

            -1, 0, -1,     0, 1, 0,     0, 0,
             1, 0,  1,     0, 1, 0,     1, 1,
             1, 0, -1,     0, 1, 0,     1, 0,

            -1, 0, -1,     0, 1, 0,     0, 0,
            -1, 0,  1,     0, 1, 0,     0, 1,
             1, 0,  1,     0, 1, 0,     1, 1
        };

        groundVAO = glGenVertexArrays();
        groundVBO = glGenBuffers();

        glBindVertexArray(groundVAO);

        glBindBuffer(
                GL_ARRAY_BUFFER,
                groundVBO
        );

        glBufferData(
                GL_ARRAY_BUFFER,
                vertices,
                GL_STATIC_DRAW
        );

        int stride = 8 * Float.BYTES;

        glVertexAttribPointer(
                0,
                3,
                GL_FLOAT,
                false,
                stride,
                0
        );

        glEnableVertexAttribArray(0);

        glVertexAttribPointer(
                1,
                3,
                GL_FLOAT,
                false,
                stride,
                3 * Float.BYTES
        );

        glEnableVertexAttribArray(1);

        glVertexAttribPointer(
                2,
                2,
                GL_FLOAT,
                false,
                stride,
                6 * Float.BYTES
        );

        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
    }

    private static void createGrid() {

        List<Float> data = new ArrayList<>();

        for (int i = -5; i <= 5; i++) {

            addGridVertex(data, i, 0.0f, -5.0f);
            addGridVertex(data, i, 0.0f, 5.0f);

            addGridVertex(data, -5.0f, 0.0f, i);
            addGridVertex(data, 5.0f, 0.0f, i);
        }

        gridVertexCount = data.size() / 8;

        float[] vertices = new float[data.size()];

        for (int i = 0; i < data.size(); i++) {
            vertices[i] = data.get(i);
        }

        gridVAO = glGenVertexArrays();
        gridVBO = glGenBuffers();

        glBindVertexArray(gridVAO);

        glBindBuffer(
                GL_ARRAY_BUFFER,
                gridVBO
        );

        glBufferData(
                GL_ARRAY_BUFFER,
                vertices,
                GL_STATIC_DRAW
        );

        int stride = 8 * Float.BYTES;

        glVertexAttribPointer(
                0,
                3,
                GL_FLOAT,
                false,
                stride,
                0
        );

        glEnableVertexAttribArray(0);

        glVertexAttribPointer(
                1,
                3,
                GL_FLOAT,
                false,
                stride,
                3 * Float.BYTES
        );

        glEnableVertexAttribArray(1);

        glVertexAttribPointer(
                2,
                2,
                GL_FLOAT,
                false,
                stride,
                6 * Float.BYTES
        );

        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
    }

    private static void addGridVertex(
            List<Float> data,
            float x,
            float y,
            float z
    ) {

        data.add(x);
        data.add(y);
        data.add(z);

        data.add(0.0f);
        data.add(1.0f);
        data.add(0.0f);

        data.add(0.0f);
        data.add(0.0f);
    }

    private static float clamp(
            float value,
            float min,
            float max
    ) {

        return Math.max(
                min,
                Math.min(max, value)
        );
    }

    private static void cleanup() {

        if (!glInitialized) {
            return;
        }

        if (mesh != null) {
            mesh.cleanup();
        }

        if (lightingShader != null) {
            lightingShader.cleanup();
        }

        if (shadowShader != null) {
            shadowShader.cleanup();
        }

        if (shadowMap != null) {
            shadowMap.cleanup();
        }

        if (texture != null) {
            texture.cleanup();
        }

        if (groundVBO != 0) {
            glDeleteBuffers(groundVBO);
        }

        if (groundVAO != 0) {
            glDeleteVertexArrays(groundVAO);
        }

        if (gridVBO != 0) {
            glDeleteBuffers(gridVBO);
        }

        if (gridVAO != 0) {
            glDeleteVertexArrays(gridVAO);
        }

        glInitialized = false;
    }
}
