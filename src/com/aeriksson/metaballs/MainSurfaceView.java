package com.aeriksson.metaballs;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;

/**
 * Simple GLSurfaceView. Dispatches scroll and scale events to the camera.
 */
public class MainSurfaceView extends GLSurfaceView implements
		OnGestureListener, OnScaleGestureListener {

	private Camera camera;

	private float prevSpan;

	GestureDetector gestureDetector;
	ScaleGestureDetector scaleGestureDetector;

	public MainSurfaceView(Context context, AttributeSet attributes) {
		super(context, attributes);
		setEGLContextClientVersion(2);

		gestureDetector = new GestureDetector(context, this);
		scaleGestureDetector = new ScaleGestureDetector(context, this);

		gestureDetector.setIsLongpressEnabled(true);
	}

	public void setCamera(Camera camera) {
		this.camera = camera;
	}

	@Override
	public boolean onTouchEvent(final MotionEvent e) {
		// Forward touch events to the gesture detectors.
		gestureDetector.onTouchEvent(e);
		scaleGestureDetector.onTouchEvent(e);
		return true;
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		float currentSpan = detector.getCurrentSpan();

		final float delta = prevSpan - currentSpan;

		queueEvent(new Runnable() {
			public void run() {
				camera.zoom(delta);
			}
		});

		prevSpan = currentSpan;

		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		prevSpan = detector.getCurrentSpan();
		return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2,
			final float distanceX, final float distanceY) {

		queueEvent(new Runnable() {
			public void run() {
				camera.rotate(distanceX, distanceY);
			}
		});

		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}
}