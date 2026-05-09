#version 150

uniform sampler2D Sampler0;
uniform vec2 uResolution;
uniform float uProgress; // 0.0 to 1.0 (0.0 = old theme full, 1.0 = new theme full)
uniform float uSeed;    // unique pattern per transition
uniform vec3 uPaint;    // tint color (theme primary)

in vec2 TexCoord;
out vec4 fragColor;

// Simple pseudo-random and noise functions for procedural edge
float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

// Fbm for more organic "liquid" feel
float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = rot * p * 2.0;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec4 oldColor = texture(Sampler0, TexCoord);
    if (oldColor.a == 0.0) {
        discard;
    }

    // Горизонтальная заливка слева→направо (раньше была диагональ)
    float gradient = TexCoord.x;

    // Уникальный паттерн по seed: сдвиг+поворот
    float a = uSeed * 6.2831853;
    mat2 rs = mat2(cos(a), sin(a), -sin(a), cos(a));
    vec2 pos = (TexCoord + vec2(uSeed * 13.37, uSeed * 7.91)) * vec2(uResolution.x / uResolution.y, 1.0) * 5.0;
    pos = rs * pos;
    float n = fbm(pos + vec2(-uProgress, uProgress) * 2.2);

    // Капельная волна — больше локального искажения по Y, чтобы стекало
    float drip = sin(TexCoord.y * (12.0 + uSeed * 9.0) + uSeed * 17.0) * 0.05;
    drip += fbm(vec2(TexCoord.y * 6.0 + uSeed * 3.0, uProgress * 2.0)) * 0.10;

    float wave = gradient + (n * 0.18) + drip;

    float adjustedProgress = (uProgress * 1.4) - 0.20;

    if (wave < adjustedProgress) {
        discard;
    }

    float edgeDist = wave - adjustedProgress;
    float edgeThickness = 0.14;

    if (edgeDist < edgeThickness) {
        float dotPattern = noise(TexCoord * (120.0 + uSeed * 60.0));
        vec3 paintBright = clamp(uPaint * 1.25, 0.0, 1.0);
        vec3 paintDark = uPaint * 0.55;
        vec3 finalPaint = mix(paintDark, paintBright, dotPattern);

        float alphaEdge = 1.0 - (edgeDist / edgeThickness);
        fragColor = vec4(mix(oldColor.rgb, finalPaint, alphaEdge), oldColor.a);
    } else {
        fragColor = oldColor;
    }
}
