#version 300 es

in vec3 new_color;
in vec3 new_normal;
in vec3 new_viewer_direction;
in vec3 new_light_direction;

/* Uniform data */
uniform vec3 light_params[3];
uniform float shininess;

/* Output color */
out vec4 out_color;

void main() {

  /* Step 1: Compute N . L */
  float n_dot_l = dot(new_normal, new_light_direction);

  /* Step 2: Compute H, the vector between L and V */
  vec3 half_vector =
    normalize(new_light_direction + new_viewer_direction);

  /* Step 3: Compute (N . H)^n' */
  float blinn = dot(new_normal, half_vector);
  blinn = clamp(blinn, 0.0f, 1.0f);
  blinn = pow(blinn, shininess);

  /* Step 4: Compute sum of light components */
  vec3 light_color = light_params[0] +
    light_params[1] * n_dot_l +
    light_params[2] * blinn;
  light_color = clamp(light_color, 0.0f, 1.0f);

  /* Step 5: Blend light color and original color */
  vec3 color_sum = 
    clamp((light_color + new_color)/2.0f, 0.0f, 1.0f);
  out_color = vec4(color_sum, 1.0);
}