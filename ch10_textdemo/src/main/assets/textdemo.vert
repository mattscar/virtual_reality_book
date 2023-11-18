#version 300 es

in  vec2 in_coords;
in  vec2 in_texcoords;

uniform float vertscale;

out vec2 new_texcoords;

void main(void) {
  gl_Position = vec4(in_coords * vec2(1.0f, vertscale), 0.0, 1.0);
  new_texcoords = in_texcoords;
}