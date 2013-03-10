package com.aeriksson.metaballs;

import android.app.Activity;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity implements OnSeekBarChangeListener {

	Camera camera;
	Animator animator;
	Particles particles;
	Renderer renderer;
	ParticleRenderer particleRenderer;
	MainSurfaceView view;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		SeekBar sb = (SeekBar) findViewById(R.id.slider);
		sb.setOnSeekBarChangeListener(this);

		camera = new Camera();

		animator = new Animator();
		renderer = new Renderer(camera, animator);

		view = (MainSurfaceView) findViewById(R.id.surface_view);
		view.setCamera(camera);
		view.setRenderer(renderer);

		particles = new Particles();
		particleRenderer = new ParticleRenderer(particles);

		animator.addAnimatable(particles);
		renderer.addRenderable(particleRenderer);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean unused) {
		int maximum = seekBar.getMax();
		particleRenderer.setIsosurfaceThresholdFactor(progress
				/ (float) maximum);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

}
