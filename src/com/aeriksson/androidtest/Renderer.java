package com.aeriksson.androidtest;

import java.util.LinkedList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

/**
 * Keeps track of and renders a set of renderable objects.
 * 
 * Since this is the entry point on new frames, animated objects
 * are also called from here (through an Animator object).
 */
public class Renderer implements GLSurfaceView.Renderer {
	private Camera camera;
	private Animator animator;
	
	private LinkedList<Renderable> renderables = new LinkedList<Renderable>();

	private float[] viewMatrix;
	private float[] projectionMatrix = new float[16];
	private float[] viewProjectionMatrix = new float[16];

	Renderer(Camera camera, Animator animator) {
		this.camera = camera;
		this.animator = animator;
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		setGLOptions();
	}

	private void setGLOptions() {
		GLES20.glClearColor(0, 0, 0, 1);
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
	}
	
	public void addRenderable(Renderable renderable) {
		renderables.add(renderable);
	}
	
	@Override
	public void onDrawFrame(GL10 unused) {
		animator.update();
		render();
	}

	private void render() {
		clearScreen();

		updateMatrices();

		for (Renderable renderable: renderables) {
			renderable.render(viewMatrix, viewProjectionMatrix);
		}
	}

	private void clearScreen() {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
	}

	private void updateMatrices() {
		viewMatrix = camera.getViewMatrix();
		Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		GLES20.glViewport(0, 0, width, height);

		float ratio = (float) width / height;

		Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1.0f, 1.0f, 1.0f,
				40.0f);
	}
}
