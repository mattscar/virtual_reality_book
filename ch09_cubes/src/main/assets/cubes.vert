#version 300 es

/* Attribute data */
in vec3 in_coords;
in vec3 in_color;
in vec3 in_normal;

/* Uniform data */
uniform mat4 mvp_matrices[3];
uniform vec3 viewer_pos;
uniform vec3 light_pos;

/* Passed to the fragment shader */
out vec3 new_color;
out vec3 new_normal;
out vec3 new_viewer_direction;
out vec3 new_light_direction;

void main() {

  /* Apply the model transformation */
  vec4 world_coords =
    mvp_matrices[0] * vec4(in_coords, 1.0);

  /* Translate the second and third instances */
  world_coords.x = world_coords.x + (float(gl_InstanceID) * 10.0f);  
  world_coords.z = world_coords.z - (float(gl_InstanceID) * 10.0f);

  /* Apply the view transformation */
  vec4 eye_coords =
    mvp_matrices[1] * world_coords;

  /* Determine the direction to the viewer */
  new_viewer_direction =
    normalize(viewer_pos - eye_coords.xyz);

  /* Apply the projection transformation */
  vec4 clip_coords =
    mvp_matrices[2] * eye_coords;

  /* Determine the direction to the light source */
  new_light_direction =
    normalize(light_pos - clip_coords.xyz);

  /* Set output variables */
  new_color = in_color;
  new_normal = in_normal;
  gl_Position = clip_coords;
}