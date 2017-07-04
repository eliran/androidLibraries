package com.threeplay.android.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

public class CircleMotionTrackerComponent extends MotionTracker.MotionTrackerComponent {
    private Paint paint = new Paint();
    private float radius;
    private int fillColor, strokeColor, strokeWidth;

    public CircleMotionTrackerComponent(float x, float y, float radius) {
        super(new CircleMotionTracker(x, y, radius * 2));
        this.radius = radius;
    }

    public void setStyle(int fillColor, int strokeColor, int strokeWidth) {
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
    }

    public void onDraw(Canvas canvas, float refX, float refY) {
        float x = refX + motionTracker.getX(), y = refY + motionTracker.getY();
        paint.setColor(fillColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x, y, radius, paint);
        if (strokeWidth > 0) {
            paint.setColor(strokeColor);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            canvas.drawCircle(x, y, radius, paint);
        }
    }
}
