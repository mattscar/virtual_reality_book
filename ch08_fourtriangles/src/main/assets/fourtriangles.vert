#version 300 es

// Attribute data
in  vec2 in_coords;
in  vec3 in_color;
out vec3 new_color;

// Uniform variable - rotation matrix
uniform mat4 rot_matrix;

vec2 offset[4] = vec2[4] (
  vec2(-0.5, -0.5), vec2(-0.5, 0.5),
  vec2(0.5, -0.5), vec2(0.5, 0.5));

void main(void) {

  // Rotate the incoming vertex
  vec4 new_coords = rot_matrix * vec4(in_coords, 0.0, 1.0);

  // Apply the offset based on the instance
  new_coords.xy += offset[gl_InstanceID];

  // Set the output coordinates
  gl_Position = new_coords;

  // Set the outgoing color
  new_color = in_color;
}