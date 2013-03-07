package com.aeriksson.metaballs;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
	
	Camera camera;
	Animator animator;
	Particles particles;
	Renderer renderer;
	MainSurfaceView view;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		camera = new Camera();
		
		animator = new Animator();
		renderer = new Renderer(camera, animator);
		
	    view = new MainSurfaceView(this);
	    view.setCamera(camera);
	    view.setRenderer(renderer);
	    setContentView(view);
	    
		particles = new Particles();
		Renderable particleRenderer = new ParticleRenderer(particles);
		
		animator.addAnimatable(particles);
		renderer.addRenderable(particleRenderer);
	}
}
