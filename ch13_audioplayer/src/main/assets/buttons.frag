#version 300 es

uniform sampler2D texSampler;

in vec2 new_texcoords;
out vec4 out_color;

void main() {
  vec3 color = vec3(texture(texSampler, new_texcoords));
  out_color = vec4(color, 1.0);
}