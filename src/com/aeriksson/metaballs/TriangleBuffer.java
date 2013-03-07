package com.aeriksson.metaballs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Stores triangles for rendering. Wraps a FloatBuffer stored as a ByteBuffer.
 */
public class TriangleBuffer {

	final String INVALID_TRIANGLE_DIMENSION_ERROR = "Invalid triangle dimension: %s "
			+ "(should be divisible by %s).";

	private final static int BYTES_PER_FLOAT = 4;

	private final int floatsPerTriangle;

	private FloatBuffer buffer = null;

	private int triangleCount = 0;

	TriangleBuffer(int capacity, int floatsPerTriangle) {
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(capacity
				* BYTES_PER_FLOAT);
		this.floatsPerTriangle = floatsPerTriangle;
		byteBuffer.order(ByteOrder.nativeOrder());
		buffer = byteBuffer.asFloatBuffer();

		buffer.position(0);
	}

	public void clear() {
		triangleCount = 0;
		buffer.clear();
	}

	public void add(float[] triangles) {
		if (triangles.length % floatsPerTriangle != 0) {
			String errorString = String.format(
					INVALID_TRIANGLE_DIMENSION_ERROR, triangles.length,
					floatsPerTriangle);
			throw new IllegalArgumentException(errorString);
		}

		// TODO: handle boundary case here
		if (!isFull()) {
			triangleCount += triangles.length / floatsPerTriangle;
			buffer.put(triangles);
		}
	}

	public void add(float[] triangles, int count) {
		// TODO: add error check here

		triangleCount += count;
		buffer.put(triangles, 0, count * floatsPerTriangle);
	}

	public int getTriangleCount() {
		return triangleCount;
	}

	public FloatBuffer getFloatBuffer() {
		return buffer.asReadOnlyBuffer();
	}

	private boolean isFull() {
		return buffer.remaining() < floatsPerTriangle;
	}
}