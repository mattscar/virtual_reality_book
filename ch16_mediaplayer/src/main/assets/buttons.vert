#version 300 es

in vec2 in_coords;
in vec2 in_texcoords;
out vec2 new_texcoords;

// Uniform buffer object
uniform ubo {
  mat4 trans_matrix;
  vec2 target;
  vec2 offset[3];
  int selected_index;
};

void main(void) {

  // Rotate the incoming vertex
  vec4 new_coords = vec4(in_coords, -5.0, 1.0);

  // Apply the offset and the transformation
  new_coords.xy += offset[gl_InstanceID];
  new_coords = trans_matrix * new_coords;

  // Set the output coordinates
  gl_Position = new_coords;

  // Set texture coordinates
  new_texcoords.x = in_texcoords.x + 0.333 * float(gl_InstanceID);
  if(selected_index != gl_InstanceID) {
    new_texcoords.y = in_texcoords.y;
  } else {
    new_texcoords.y = in_texcoords.y - 0.5;
  }
}
