#version 330 core

out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;

uniform sampler2D textureSampler;

uniform bool useTexture;
uniform bool useBlinnPhong;

uniform bool useFlatShading;

void main()
{
    // Normal permukaan
    vec3 norm = normalize(Normal);

    // Flat Shading menggunakan turunan posisi fragment
    if (useFlatShading)
    {
        norm = normalize(
            cross(
                dFdx(FragPos),
                dFdy(FragPos)
            )
        );
    }

    // Ambient Lighting
    float ambientStrength = 0.2;

    vec3 ambient =
        ambientStrength * lightColor;

    // Diffuse Lighting
    vec3 lightDir =
        normalize(lightPos - FragPos);

    float diff =
        max(dot(norm, lightDir), 0.0);

    vec3 diffuse =
        diff * lightColor;

    // Specular Lighting
    float specularStrength = 0.8;

    vec3 viewDir =
        normalize(viewPos - FragPos);

    float spec = 0.0;

    if (useBlinnPhong)
    {
        // Blinn-Phong
        vec3 halfwayDir =
            normalize(lightDir + viewDir);

        spec = pow(
            max(dot(norm, halfwayDir), 0.0),
            32.0
        );
    }
    else
    {
        // Phong
        vec3 reflectDir =
            reflect(-lightDir, norm);

        spec = pow(
            max(dot(viewDir, reflectDir), 0.0),
            16.0
        );
    }

    vec3 specular =
        specularStrength * spec * lightColor;

    // Texture Mapping
    vec3 baseColor = objectColor;

    if (useTexture)
    {
        baseColor =
            texture(textureSampler, TexCoords).rgb;
    }

    // Hasil pencahayaan
    vec3 result =
        (ambient + diffuse + specular) * baseColor;

    FragColor = vec4(result, 1.0);
}

