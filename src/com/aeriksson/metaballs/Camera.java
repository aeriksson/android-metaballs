package com.aeriksson.metaballs;

import android.opengl.Matrix;

/**
 * Simple camera focused on the origin.
 * Uses standard spherical coordinates.
 */
class Camera {
	private final static float AZIMUTH_SCALE_FACTOR = 0.01f;
	private final static float INCLINATION_SCALE_FACTOR = 0.01f;
	private final static float RADIUS_SCALE_FACTOR = 0.01f;
	
	private final static float MIN_RADIUS = 1.0f;
	private final static float MAX_RADIUS = 20.0f;

	private float radius = 10.0f;
	private float inclination = (float) Math.PI / 2.0f;
	private float azimuth = 0;

	private float[] viewMatrix = new float[16];

	Camera() {
		updateViewMatrix();
	}

	public void rotate(float dx, float dy) {
		azimuth += AZIMUTH_SCALE_FACTOR * dx;
		inclination += INCLINATION_SCALE_FACTOR * dy;

		updateViewMatrix();
	}

	public void zoom(double delta) {

		radius += RADIUS_SCALE_FACTOR * delta;

		if (radius < MIN_RADIUS) {
			radius = MIN_RADIUS;
		} else if (radius > MAX_RADIUS) {
			radius = MAX_RADIUS;
		}

		updateViewMatrix();
	}

	public float[] getViewMatrix() {
		return viewMatrix;
	}

	private void updateViewMatrix() {

		final float eyeX = (float) (radius * Math.sin(inclination) * Math
				.cos(azimuth));
		final float eyeY = (float) (radius * Math.sin(inclination) * Math
				.sin(azimuth));
		final float eyeZ = (float) (radius * Math.cos(inclination));
		final float upX = (float) (- Math.cos(inclination) * Math.cos(azimuth));
		final float upY = (float) (- Math.cos(inclination) * Math.sin(azimuth));
		final float upZ = (float) Math.sin(inclination);

		Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, 0, 0, 0, upX, upY,
				upZ);
	}
}