#version 300 es

in  vec2 in_coords;
in  vec3 in_color;
out vec3 new_color;

void main(void) {
  gl_Position = vec4(in_coords, 0.0, 1.0);
  new_color = in_color;
}