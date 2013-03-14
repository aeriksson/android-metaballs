package com.aeriksson.metaballs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Stores buffers and other relevant information representing a triangle mesh.
 * 
 * For now, the mesh data is read-only, which means that the mesh 
 */
public class MeshData {
	private static final int BYTES_PER_FLOAT = 4;
	private static final int FLOATS_PER_TRIANGLE = 9;

	private final int triangleCount;
	private final FloatBuffer vertices;
	private final FloatBuffer normals;
	private final FloatBuffer colors;

	public MeshData(int triangleCount, float[] vertices,
			float[] normals, float[] colors) {
		this.triangleCount = triangleCount;
		this.vertices = getEmptyBuffer(triangleCount);
		this.normals = getEmptyBuffer(triangleCount);
		this.colors = getEmptyBuffer(triangleCount);
		
		this.vertices.put(vertices, 0, triangleCount * FLOATS_PER_TRIANGLE);
		this.normals.put(normals, 0, triangleCount * FLOATS_PER_TRIANGLE);
		this.colors.put(colors, 0, triangleCount * FLOATS_PER_TRIANGLE);
	}
	
	/**
	 * Allocates a float buffer with room for the given number of triangles
	 * in the native byte format.
	 */
	private static FloatBuffer getEmptyBuffer(int triangleCount) {
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(triangleCount
				* FLOATS_PER_TRIANGLE * BYTES_PER_FLOAT);
		byteBuffer.order(ByteOrder.nativeOrder());
		return byteBuffer.asFloatBuffer();
	}

	public int getTriangleCount() {
		return triangleCount;
	}

	public FloatBuffer getVertices() {
		return vertices.asReadOnlyBuffer();
	}

	public FloatBuffer getNormals() {
		return normals.asReadOnlyBuffer();
	}

	public FloatBuffer getColors() {
		return colors.asReadOnlyBuffer();
	}
}