package com.aeriksson.metaballs;

import java.nio.FloatBuffer;

import android.opengl.GLES20;

/**
 * Handles rendering of the metaballs, including updating the mesh.
 */
public class ParticleRenderer implements Renderable {

	final String VERTEX_SHADER = "                                    "
			+ "uniform mat4 MVPMatrix;                                     \n"
			+ "uniform mat4 MVMatrix;                                      \n"
			+ "uniform vec3 lightPosition;                                 \n"
			+ "attribute vec3 position;                                    \n"
			+ "attribute vec3 normal;                                      \n"
			+ "attribute vec3 color;                                       \n"
			+ "varying vec3 vPosition;                                     \n"
			+ "varying vec4 vColor;                                        \n"
			+ "varying vec3 vNormal;                                       \n"
			+ "varying float vLightDistance;                               \n"
			+ "varying vec3 vLightDirection;                               \n"
			+ "void main()                                                 \n"
			+ "{                                                           \n"
			+ "    vPosition = vec3(MVMatrix * vec4(position, 1.0));       \n"
			+ "    vLightDistance = length(lightPosition - vPosition);     \n"
			+ "    vLightDirection = normalize(lightPosition - vPosition); \n"
			+ "    vNormal = vec3(MVMatrix * vec4(normal, 0.0));           \n"
			+ "    vColor = vec4(normalize(color + vec3(.6, .6, .6)), 1.0);\n"
			+ "    gl_Position = MVPMatrix * vec4(position, 1.0);          \n"
			+ "}                                                           \n";

	final String FRAGMENT_SHADER = "           "
			+ "precision mediump float;                                                          \n"
			+ "varying vec3 vPosition;                                                           \n"
			+ "varying vec4 vColor;                                                              \n"
			+ "varying vec3 vNormal;                                                             \n"
			+ "varying float vLightDistance;                                                     \n"
			+ "varying vec3 vLightDirection;                                                     \n"
			+ "void main()                                                                       \n"
			+ "{                                                                                 \n"
			+ "    float diffuse = max(dot(vNormal, vLightDirection), 0.2);                      \n"
			+ "    gl_FragColor = vColor * diffuse + vec4(vec3(0.4, 0.4, 0.4) * diffuse, 1.0);   \n"
			+ "}                                                                                 \n";

	/**
	 * Constants used to describe to the shader how the triangles are stored.
	 */
	private final static int VERTEX_SIZE = 3;
	private final static int NORMAL_SIZE = 3;
	private final static int COLOR_SIZE = 3;
	private final static int BYTES_PER_FLOAT = 4;

	/** Shader program handle */
	private int shader = -1;

	/** Shader uniform and attribute handles. */
	private int shaderMVPMatrixIndex;
	private int shaderMVMatrixIndex;
	private int shaderPositionIndex;
	private int shaderNormalIndex;
	private int shaderColorIndex;
	private int shaderLightPositionIndex;

	private final static float[] LIGHT_POSITION = { 1.0f, 1.0f, 1.0f };

	private Particles particles;
	private MetaballVisualizer metaballVisualizer;

	public ParticleRenderer(Particles particles) {
		this.particles = particles;

		metaballVisualizer = new MetaballVisualizer();
	}

	private void initShader() {
		shader = ShaderUtility
				.loadShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);

		GLES20.glUseProgram(shader);

		shaderMVPMatrixIndex = GLES20.glGetUniformLocation(shader, "MVPMatrix");
		shaderMVMatrixIndex = GLES20.glGetUniformLocation(shader, "MVMatrix");
		shaderLightPositionIndex = GLES20.glGetUniformLocation(shader,
				"lightPosition");
		shaderPositionIndex = GLES20.glGetAttribLocation(shader, "position");
		shaderNormalIndex = GLES20.glGetAttribLocation(shader, "normal");
		shaderColorIndex = GLES20.glGetAttribLocation(shader, "color");
	}

	public void setIsosurfaceThresholdFactor(float thresholdFactor) {
		metaballVisualizer.setIsosurfaceThreshold(thresholdFactor);
	}

	@Override
	public void render(float[] viewMatrix, float[] viewProjectionMatrix) {
		float[][] particlePositions = particles.getPositions();
		metaballVisualizer.setParticlePositions(particlePositions);
		metaballVisualizer.update();

		// The shader has to be instantiated in the correct thread; i.e. here
		// and not in the constructor.
		if (shader == -1) {
			initShader();
		}

		renderTriangles(viewMatrix, viewProjectionMatrix);
	}

	private void renderTriangles(float[] viewMatrix,
			float[] viewProjectionMatrix) {

		MeshData mesh = metaballVisualizer.getMesh();

		if (mesh == null) {
			return;
		}

		FloatBuffer vertices = mesh.getVertices();
		FloatBuffer normals = mesh.getNormals();
		FloatBuffer colors = mesh.getColors();
		int triangleCount = mesh.getTriangleCount();

		GLES20.glUniformMatrix4fv(shaderMVPMatrixIndex, 1, false,
				viewProjectionMatrix, 0);
		GLES20.glUniformMatrix4fv(shaderMVMatrixIndex, 1, false, viewMatrix, 0);
		GLES20.glUniform3fv(shaderLightPositionIndex, 1, LIGHT_POSITION, 0);

		vertices.position(0);
		GLES20.glVertexAttribPointer(shaderPositionIndex, VERTEX_SIZE,
				GLES20.GL_FLOAT, false, VERTEX_SIZE * BYTES_PER_FLOAT, vertices);
		GLES20.glEnableVertexAttribArray(shaderPositionIndex);

		normals.position(0);
		GLES20.glVertexAttribPointer(shaderNormalIndex, NORMAL_SIZE,
				GLES20.GL_FLOAT, false, NORMAL_SIZE * BYTES_PER_FLOAT, normals);
		GLES20.glEnableVertexAttribArray(shaderNormalIndex);

		colors.position(0);
		GLES20.glVertexAttribPointer(shaderColorIndex, COLOR_SIZE,
				GLES20.GL_FLOAT, false, COLOR_SIZE * BYTES_PER_FLOAT, colors);
		GLES20.glEnableVertexAttribArray(shaderColorIndex);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, triangleCount);
	}
}