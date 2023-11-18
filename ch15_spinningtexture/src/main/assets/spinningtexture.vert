#version 300 es

in vec3 in_coords;
in vec2 in_texcoords;
out vec2 new_texcoords;

// MVP matrix
uniform ubo {
  mat4 mvp_matrix;
};

void main(void) {
  vec4 new_coords = vec4(in_coords, 1.0);
  gl_Position = mvp_matrix * new_coords;
  new_texcoords = in_texcoords;
}
