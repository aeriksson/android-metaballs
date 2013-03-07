package com.aeriksson.androidtest;

import java.util.LinkedList;

import android.os.SystemClock;

/**
 * Keeps track of and updates objects.
 */
public class Animator {
	long previousTime = SystemClock.elapsedRealtime();
	
	LinkedList<Animatable> animatables = new LinkedList<Animatable>();
	
	public void addAnimatable(Animatable animatable) {
		animatables.add(animatable);
	}
	
	public void update() {
		long currentTime = SystemClock.elapsedRealtime();
		long timeDelta = currentTime - previousTime;
		for (Animatable animatable: animatables) {
			animatable.update(timeDelta);
		}
		previousTime = currentTime;
	}
}
