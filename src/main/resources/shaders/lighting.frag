
#version 330 core

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

out vec4 FragColor;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;

uniform sampler2D textureSampler;
uniform sampler2D shadowMap;

uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useFlatShading;

// Menghitung bayangan menggunakan Shadow Mapping.
float calculateShadow(
    vec4 lightSpacePosition,
    vec3 normal
) {

    vec3 projectedCoords =
        lightSpacePosition.xyz / lightSpacePosition.w;

    projectedCoords = projectedCoords * 0.5 + 0.5;

    // Fragmen di luar area shadow tidak menerima bayangan.
    if (
        projectedCoords.z > 1.0 ||
        projectedCoords.x < 0.0 ||
        projectedCoords.x > 1.0 ||
        projectedCoords.y < 0.0 ||
        projectedCoords.y > 1.0
    ) {
        return 0.0;
    }

    float closestDepth = texture(
        shadowMap,
        projectedCoords.xy
    ).r;

    float currentDepth = projectedCoords.z;

    float bias = max(
        0.005 * (
            1.0 - dot(
                normal,
                normalize(lightPos - FragPos)
            )
        ),
        0.0005
    );

    return currentDepth - bias > closestDepth
            ? 1.0
            : 0.0;
}

void main() {

    // Menghitung normal permukaan.
    vec3 normal = normalize(Normal);

    // Flat Shading.
    if (useFlatShading) {

        normal = normalize(
            cross(
                dFdx(FragPos),
                dFdy(FragPos)
            )
        );
    }

    // Arah cahaya dan kamera.
    vec3 lightDirection = normalize(
        lightPos - FragPos
    );

    vec3 viewDirection = normalize(
        viewPos - FragPos
    );

    // Ambient Lighting.
    vec3 ambient = 0.2 * lightColor;

    // Diffuse Lighting.
    float diffuseStrength = max(
        dot(normal, lightDirection),
        0.0
    );

    vec3 diffuse =
        diffuseStrength * lightColor;

    // Specular Lighting.
    float specularStrength;

    if (useBlinnPhong) {

        // Blinn-Phong.
        vec3 halfwayDirection = normalize(
            lightDirection + viewDirection
        );

        specularStrength = pow(
            max(
                dot(normal, halfwayDirection),
                0.0
            ),
            32.0
        );

    } else {

        // Phong.
        vec3 reflectDirection = reflect(
            -lightDirection,
            normal
        );

        specularStrength = pow(
            max(
                dot(viewDirection, reflectDirection),
                0.0
            ),
            16.0
        );
    }

    vec3 specular =
        0.5 * specularStrength * lightColor;

    // Shadow Mapping.
    float shadow = calculateShadow(
        FragPosLightSpace,
        normal
    );

    // Warna dasar objek.
    vec3 baseColor = objectColor;

    if (useTexture) {

        baseColor = texture(
            textureSampler,
            TexCoords
        ).rgb;
    }

    // Menggabungkan seluruh pencahayaan.
    vec3 lighting = ambient
        + (1.0 - shadow) * (diffuse + specular);

    vec3 result = lighting * baseColor;

    FragColor = vec4(result, 1.0);
}
