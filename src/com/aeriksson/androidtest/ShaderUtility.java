package com.aeriksson.androidtest;

import android.opengl.GLES20;

/**
 * Utilities for loading and compiling shaders. Tries to catch and report any
 * errors.
 */
class ShaderUtility {

	public static int loadShaderProgram(String vertexShader,
			String fragmentShader) {
		int vertexShaderHandle = compileShader(vertexShader,
				GLES20.GL_VERTEX_SHADER);
		int fragmentShaderHandle = compileShader(fragmentShader,
				GLES20.GL_FRAGMENT_SHADER);

		int shaderProgram = GLES20.glCreateProgram();

		GLES20.glAttachShader(shaderProgram, vertexShaderHandle);
		GLES20.glAttachShader(shaderProgram, fragmentShaderHandle);

		GLES20.glLinkProgram(shaderProgram);

		int[] linked = { 0 };
		GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linked, 0);
		if (linked[0] == 0) {
			String errorMessage = GLES20.glGetShaderInfoLog(shaderProgram);
			throw new RuntimeException(String.format(
					"Could not link shader: %s", errorMessage));
		}

		return shaderProgram;
	}

	private static int compileShader(String source, int type) {
		int shader = GLES20.glCreateShader(type);
		GLES20.glShaderSource(shader, source);
		GLES20.glCompileShader(shader);

		int[] compiled = { 0 };
		GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0) {
			String errorMessage = GLES20.glGetShaderInfoLog(shader);
			throw new RuntimeException(String.format(
					"Could not compile shader. %s", errorMessage));
		}

		return shader;
	}
}