package com.aeriksson.metaballs;

import java.util.Arrays;

public class MetaballVisualizer extends IsosurfaceVisualizer {

	/** The default isosurface threshold for the metaballs. */
	private final static float DEFAULT_THRESHOLD = 1.0f;

	/** Current isosurface threshold value for the metaballs. */
	private float thresholdValue = DEFAULT_THRESHOLD;

	private float[][] particlePositions;

	public void setIsosurfaceThreshold(float thresholdFactor) {
		thresholdValue = thresholdFactor * 2.0f;
	}

	/**
	 * Updates If Marching Cubes is not running, a new iteration is started.
	 * 
	 * @param particlePositions
	 *            A list of x/y/z positions for the metaballs.
	 */
	public void setParticlePositions(float[][] particlePositions) {
		this.particlePositions = particlePositions;
	}

	/**
	 * Scalar field for the metaballs.
	 */
	protected ScalarField getScalarField() {
		// copy threshold value to avoid graphical artifacts when it is
		// changed mid-computation.
		final float lastThresholdValue = thresholdValue;

		final float[][] lastParticlePositions = getParticlesCopy();

		// The metaballs field
		ScalarField scalarField = new ScalarField() {
			@Override
			public float evaluate(float x, float y, float z) {

				float value = 0;

				for (int i = 0; i < particlePositions.length; i++) {
					final float deltaX = lastParticlePositions[i][0] - x;
					final float deltaY = lastParticlePositions[i][1] - y;
					final float deltaZ = lastParticlePositions[i][2] - z;

					value += 0.5 / (deltaX * deltaX + deltaY * deltaY + deltaZ
							* deltaZ);
				}

				// clip to boundaries of scanned area without affecting
				// normals
				final float delta = 0.1f;
				if (x < CLIPPING_BOX[0] + delta
						|| x > CLIPPING_BOX[1] - delta
						|| y < CLIPPING_BOX[2] + delta
						|| y > CLIPPING_BOX[3] - delta
						|| z < CLIPPING_BOX[4] + delta
						|| z > CLIPPING_BOX[5] - delta) {
					value -= 100;
				}

				return lastThresholdValue - value;
			}
		};
		return scalarField;
	}

	private float[][] getParticlesCopy() {
		float[][] copy = new float[particlePositions.length][];
		for (int i = 0; i < particlePositions.length; i++)
			copy[i] = Arrays.copyOf(particlePositions[i],
					particlePositions[i].length);
		return copy;
	}

	/**
	 * Color field. Each metaball is assigned a color. Vertices are colored
	 * based on their distances to the metaballs.
	 */
	protected VectorField getColorField() {

		final float[][] lastParticlePositions = getParticlesCopy();

		VectorField colorField = new VectorField() {
			@Override
			public float[] evaluate(float x, float y, float z) {
				float[] color = { 0, 0, 0 };
				for (int i = 0; i < particlePositions.length; i++) {
					float value = 0;
					final float deltaX = lastParticlePositions[i][0] - x;
					final float deltaY = lastParticlePositions[i][1] - y;
					final float deltaZ = lastParticlePositions[i][2] - z;

					value += 1 / (deltaX * deltaX + deltaY * deltaY + deltaZ
							* deltaZ);

					if (i % 3 == 0) {
						color[0] = +value;
					} else if (i % 3 == 1) {
						color[1] += value;
					} else if (i % 3 == 2) {
						color[2] += value;
					}
				}
				return color;
			}
		};
		return colorField;
	}
}
