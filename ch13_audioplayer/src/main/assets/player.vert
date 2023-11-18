#version 300 es

// Attribute data
in  vec2 in_coords;
in  vec3 in_color;
out vec3 new_color;

// Uniform buffer object
uniform ubo {
  mat4 trans_matrix;
  vec2 target;
  vec2 offset[3];
  int selected_index;
};

void main(void) {

  // Set the output coordinates
  gl_Position = trans_matrix * vec4(in_coords, -5.0, 1.0);

  // Set the outgoing color
  new_color = in_color;
}