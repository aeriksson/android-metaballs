package com.aeriksson.metaballs;

import android.os.AsyncTask;

/**
 * Renders a set of metaballs using the Marching Cubes algorithm.
 * 
 * The computation is handled on a separate thread to keep it from affecting the
 * framerate.
 */
public abstract class IsosurfaceVisualizer {

	/** Defines the volume in which Marching Cubes is run. */
	static float[] CLIPPING_BOX = { -2.5f, 2.5f, -2.5f, 2.5f,
			-2.5f, 2.5f };

	/** Number of elements per side in Marching Cubes. */
	private final static int ELEMENTS_PER_SIDE = 30;

	/**
	 * Buffers for the Marching Cubes output. Double buffering is used to
	 * prevent synchronization issues.
	 */
	private MeshData frontBuffer, backBuffer;

	private MarchingCubes marchingCubes = new MarchingCubes(CLIPPING_BOX,
			ELEMENTS_PER_SIDE);

	private MarchingCubesTask marchingCubesTask;

	/**
	 * Updates If Marching Cubes is not running, a new iteration is started.
	 * 
	 * @param particlePositions
	 *            A list of x/y/z positions for the metaballs.
	 */
	public void update() {

		if (marchingCubesTask == null
				|| marchingCubesTask.getStatus() == AsyncTask.Status.FINISHED) {

			ScalarField scalarField = getScalarField();
			VectorField colorField = getColorField();
			
			
			marchingCubes.setFunction(scalarField);
			marchingCubes.setColorField(colorField);

			marchingCubesTask = new MarchingCubesTask();
			marchingCubesTask.execute(marchingCubes);
		}
	}
	
	protected abstract ScalarField getScalarField();
	
	protected abstract VectorField getColorField();

	/**
	 * @return The vertices/normals/ from the last Marching Cubes iteration.
	 */
	public MeshData getMesh() {
		return frontBuffer;
	}

	/**
	 * Asynchronous task that calls the Marching Cubes algorithm and saves the
	 * output.
	 */
	class MarchingCubesTask extends AsyncTask<MarchingCubes, Void, Void> {

		@Override
		protected Void doInBackground(MarchingCubes... marchingCubes) {

			marchingCubes[0].compute();

			updateMesh(marchingCubes[0]);

			return null;
		}

		private void updateMesh(MarchingCubes mc) {

			int triangleCount = mc.getTriangleCount();
			float[] vertices = mc.getVertices();
			float[] normals = mc.getNormals();
			float[] colors = mc.getColors();

			backBuffer = new MeshData(triangleCount, vertices, normals, colors);

			frontBuffer = backBuffer;
		}
	}
}
