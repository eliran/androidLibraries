package com.threeplay.android.ui;

import com.threeplay.android.ui.MotionTracker;

/**
 * Created by eliranbe on 1/27/17.
 */
public class CircleMotionTracker extends MotionTracker.DragMotionTracker {
    private float radius;

    public CircleMotionTracker(float initialX, float initialY, float radius) {
        super(initialX, initialY);
        this.radius = radius;
    }

    @Override
    public boolean pointInside(float x, float y) {
        float dx = x - getX(), dy = y - getY();
        return (dx * dx + dy * dy) < (radius * radius);
    }
}
