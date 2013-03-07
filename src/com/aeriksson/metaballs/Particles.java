package com.aeriksson.metaballs;

import java.util.Random;

/**
 * Keeps track of and animates a set of particles orbiting the origin.
 * 
 * Velocities and positions are constant and randomly generated, and are stored
 * in spherical coordinates. The same constant radius is used for all particles.
 */
public class Particles implements Animatable {

	private static final int DEFAULT_PARTICLE_COUNT = 3;

	private static final float INITIAL_SPEED = 0.1e-2f;

	private static final float RADIUS = 1.2f;

	private float[][] positions;

	private float[][] parameterVelocities;

	private Random rng = new Random(5);

	Particles() {
		this(DEFAULT_PARTICLE_COUNT);
	}

	Particles(int count) {
		positions = new float[count][];
		parameterVelocities = new float[count][];

		for (int i = 0; i < count; i++) {
			positions[i] = getStartPosition();
			parameterVelocities[i] = getStartVelocity();
		}
	}

	private float[] getStartPosition() {
		return new float[] { rng.nextFloat() * 2 * (float) Math.PI,
				rng.nextFloat() * 2 * (float) Math.PI };
	}

	private float[] getStartVelocity() {
		float[] v = { rng.nextFloat() * 2 - 1, rng.nextFloat() * 2 - 1 };
		float amplitude = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1]);

		v[0] *= INITIAL_SPEED / amplitude;
		v[1] *= INITIAL_SPEED / amplitude;

		return v;
	}

	/**
	 * @return The particle positions as a list of x/y/z coordinates.
	 */
	public float[][] getPositions() {
		float[][] euclideanPositions = new float[positions.length][];
		for (int i = 0; i < positions.length; i++) {
			euclideanPositions[i] = new float[3];
			final float inclination = positions[i][0];
			final float azimuth = positions[i][0];
			euclideanPositions[i][0] = (float) (RADIUS * Math.sin(inclination) * Math
					.cos(azimuth));
			euclideanPositions[i][1] = (float) (RADIUS * Math.sin(inclination) * Math
					.sin(azimuth));
			euclideanPositions[i][2] = (float) (RADIUS * Math.cos(inclination));
		}
		return euclideanPositions;
	}

	public void update(long timeDelta) {
		for (int i = 0; i < positions.length; i++) {
			positions[i][0] += parameterVelocities[i][0] * timeDelta;
			positions[i][1] += parameterVelocities[i][1] * timeDelta;
		}
	}
}