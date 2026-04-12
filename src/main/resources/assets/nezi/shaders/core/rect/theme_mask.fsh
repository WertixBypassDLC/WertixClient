#version 150

uniform sampler2D Sampler0;
uniform vec2 uResolution;
uniform float uProgress; // 0.0 to 1.0 (0.0 = old theme full, 1.0 = new theme full)

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

    // Normalised from 0 to 1
    // Bottom-left is TexCoord (0,0), top-right is (1,1).
    float gradient = (TexCoord.x + TexCoord.y) * 0.5; 
    
    // Add organic wave/noise displacement
    vec2 pos = TexCoord * vec2(uResolution.x / uResolution.y, 1.0) * 5.0;
    float n = fbm(pos + vec2(-uProgress, uProgress) * 2.0);
    
    // The wave shape
    float wave = gradient + (n * 0.15) + (sin(TexCoord.x * 20.0 + TexCoord.y * 20.0) * 0.02);
    
    float adjustedProgress = (uProgress * 1.5) - 0.25; 
    
    if (wave < adjustedProgress) {
        discard;
    }

    float edgeDist = wave - adjustedProgress;
    float edgeThickness = 0.12;
    
    if (edgeDist < edgeThickness) {
        float dotPattern = noise(TexCoord * 150.0);
        vec3 redColor = vec3(1.0, 0.1, 0.1);
        vec3 darkRed = vec3(0.6, 0.0, 0.0);
        vec3 finalRed = mix(darkRed, redColor, dotPattern);
        
        float alphaEdge = 1.0 - (edgeDist / edgeThickness);
        fragColor = vec4(mix(oldColor.rgb, finalRed, alphaEdge), oldColor.a);
    } else {
        fragColor = oldColor;
    }
}
