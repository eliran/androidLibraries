package com.threeplay.android.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Created by eliranbe on 1/28/17.
 */

public class SliderComponent extends CircleMotionTrackerComponent {

    public enum Orientation {
        VERTICAL{
            @Override
            void drawBaseLine(Canvas canvas, float centerX, float centerY, float length, Paint paint){
                canvas.drawLine(centerX, centerY-length/2, centerX, centerY+length/2, paint);
            }

            @Override
            void setConstraints(SliderComponent sliderComponent, float sliderLength) {
                sliderComponent.setYConstraint(MotionTracker.Constraint.range(-sliderLength/2, +sliderLength/2));
                sliderComponent.setXConstraint(MotionTracker.Constraint.Lock);
            }

            @Override
            public float trackerValue(MotionTracker motionTracker) {
                return motionTracker.getY();
            }

            @Override
            public void setTrackerValue(MotionTracker motionTracker, float offset) {
                motionTracker.setPosition(motionTracker.getX(), offset);
            }
        },

        HORIZONTAL{
            @Override
            void drawBaseLine(Canvas canvas, float centerX, float centerY, float length, Paint paint){
                canvas.drawLine(centerX-length/2, centerY, centerX+length/2, centerY, paint);
            }

            @Override
            void setConstraints(SliderComponent sliderComponent, float sliderLength) {
                sliderComponent.setXConstraint(MotionTracker.Constraint.range(-sliderLength/2, +sliderLength/2));
                sliderComponent.setYConstraint(MotionTracker.Constraint.Lock);
            }

            @Override
            public float trackerValue(MotionTracker motionTracker) {
                return motionTracker.getX();
            }

            @Override
            public void setTrackerValue(MotionTracker motionTracker, float offset) {
                motionTracker.setPosition(offset, motionTracker.getY());
            }
        };

        abstract void drawBaseLine(Canvas canvas, float centerX, float centerY, float length, Paint paint);

        abstract void setConstraints(SliderComponent sliderComponent, float sliderLength);

        public abstract float trackerValue(MotionTracker motionTracker);

        public abstract void setTrackerValue(MotionTracker motionTracker, float offset);
    }

    private final Orientation orientation;
    private final float centerX, centerY;
    private final float sliderLength;
    private final float knobRadius;
    private final Paint lineStyle = new Paint();

    public SliderComponent(Orientation orientation, float centerX, float centerY, float knobRadius, float sliderLength) {
        super(centerX, centerY, knobRadius);
        this.orientation = orientation;
        this.centerX = centerX;
        this.centerY = centerY;
        this.sliderLength = sliderLength;
        this.knobRadius = knobRadius;
        lineStyle.setStrokeWidth(knobRadius/4);
        orientation.setConstraints(this, sliderLength);
    }

    public float getNormalizedValueBetween(float lo, float hi) {
        return lo + (hi - lo) * (0.5f + (orientation.trackerValue(getMotionTracker()) / sliderLength));
    }

    public float getNormalizedValue(){
        return getNormalizedValueBetween(0,1);
    }

    public void setNormalizedValue(float value) {
        float offset = value * sliderLength - (sliderLength/2);
        orientation.setTrackerValue(getMotionTracker(), offset);
    }

    public void setLineStyle(int color){
        lineStyle.setColor(color);
    }

    @Override
    public void onDraw(Canvas canvas, float refX, float refY) {
        lineStyle.setStyle(Paint.Style.FILL_AND_STROKE);
        orientation.drawBaseLine(canvas, centerX + refX, centerY + refY, sliderLength, lineStyle);
        super.onDraw(canvas, refX, refY);
    }
}
