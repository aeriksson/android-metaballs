package com.aeriksson.metaballs;

import android.os.AsyncTask;

/**
 * Renders a set of metaballs using the Marching Cubes algorithm.
 * 
 * The computation is handled on a separate thread to keep it from affecting the
 * framerate.
 */
public class MetaballVisualizer {

	/** The default isosurface threshold for the metaballs. */
	private final static float DEFAULT_THRESHOLD = 1.0f;

	/** Defines the volume in which Marching Cubes is run. */
	private final static float[] CLIPPING_BOX = { -2.5f, 2.5f, -2.5f, 2.5f,
			-2.5f, 2.5f };

	/** Number of elements per side in Marching Cubes. */
	private final static int ELEMENTS_PER_SIDE = 30;

	/** Buffer containing the output from the last Marching Cubes iteration. */
	private TriangleBuffer triangleBuffer;

	private MarchingCubes marchingCubes = new MarchingCubes(CLIPPING_BOX,
			ELEMENTS_PER_SIDE);

	private MarchingCubesTask marchingCubesTask;

	/** Current isosurface threshold value for the metaballs. */
	private float thresholdValue = DEFAULT_THRESHOLD;

	/**
	 * Updates If Marching Cubes is not running, a new iteration is started.
	 * 
	 * @param particlePositions
	 *            A list of x/y/z positions for the metaballs.
	 */
	public void update(final float[][] particlePositions) {

		if (marchingCubesTask == null
				|| marchingCubesTask.getStatus() == AsyncTask.Status.FINISHED) {

			// The metaballs field
			ScalarField scalarField = new ScalarField() {
				@Override
				public float evaluate(float x, float y, float z) {

					float value = 0;

					for (int i = 0; i < particlePositions.length; i++) {
						final float deltaX = particlePositions[i][0] - x;
						final float deltaY = particlePositions[i][1] - y;
						final float deltaZ = particlePositions[i][2] - z;

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

					return thresholdValue - value;
				}
			};

			// Color field. Each metaball is assigned a color.
			// Vertices are colored based on their distances to the
			// metaballs.
			VectorField colorField = new VectorField() {
				@Override
				public float[] evaluate(float x, float y, float z) {
					float[] color = { 0, 0, 0 };
					for (int i = 0; i < particlePositions.length; i++) {
						float value = 0;
						final float deltaX = particlePositions[i][0] - x;
						final float deltaY = particlePositions[i][1] - y;
						final float deltaZ = particlePositions[i][2] - z;

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

			marchingCubes.setFunction(scalarField);
			marchingCubes.setColorField(colorField);

			marchingCubesTask = new MarchingCubesTask();
			marchingCubesTask.execute(marchingCubes);
		}
	}

	public void setIsosurfaceThreshold(float thresholdFactor) {
		thresholdValue = thresholdFactor * 2.0f;
	}

	/**
	 * @return The triangles from the last Marching Cubes iteration.
	 */
	public TriangleBuffer getTriangleBuffer() {
		return triangleBuffer;
	}

	/**
	 * Asynchronous task that calls the Marching Cubes algorithm and saves the
	 * output.
	 */
	class MarchingCubesTask extends
			AsyncTask<MarchingCubes, Void, TriangleBuffer> {

		@Override
		protected TriangleBuffer doInBackground(MarchingCubes... marchingCubes) {

			marchingCubes[0].compute();

			int triangleCount = marchingCubes[0].getTriangleCount();
			float[] triangles = marchingCubes[0].getTriangles();

			TriangleBuffer triangleBuffer = new TriangleBuffer(
					triangleCount * 27, 27);
			triangleBuffer.add(triangles, triangleCount);

			return triangleBuffer;
		}

		@Override
		protected void onPostExecute(TriangleBuffer result) {
			triangleBuffer = result;
		}
	}
}
