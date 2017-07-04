package com.threeplay.android.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;

/**
 * Created by eliranbe on 1/28/17.
 */

public class ArcSliderComponent extends CircleMotionTrackerComponent {

    public enum Orientation {
        VERTICAL{
            @Override
            void drawBaseLine(Canvas canvas, float centerX, float centerY, float length, Paint paint){
                canvas.drawLine(centerX, centerY-length/2, centerX, centerY+length/2, paint);
            }

            @Override
            void setConstraints(ArcSliderComponent sliderComponent, float sliderLength) {
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
            void setConstraints(ArcSliderComponent sliderComponent, float sliderLength) {
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

        abstract void setConstraints(ArcSliderComponent sliderComponent, float sliderLength);

        public abstract float trackerValue(MotionTracker motionTracker);

        public abstract void setTrackerValue(MotionTracker motionTracker, float offset);
    }

    private final float centerX, centerY;
    private final float angleRange;
    private final float adjustRadius;
    private final float width, height;
    private final float arcWidth, arcHeight;
    private final Paint lineStyle = new Paint();

    public ArcSliderComponent(float centerX, float centerY, final float angleRange, final float width, final float height, float knobRadius) {
        super(centerX, centerY, knobRadius);
        this.centerX = centerX;
        this.centerY = centerY;
        this.width = width;
        this.height = height;
        this.angleRange = angleRange;
        adjustRadius = knobRadius/2;
        lineStyle.setStrokeWidth(knobRadius/4);
        arcWidth = (width * (float)Math.cos(toRadians(angleRange/2)));
        arcHeight = ((height/2) * (float)Math.sin(toRadians(angleRange/2)));

        setXConstraint(MotionTracker.Constraint.range(-arcWidth/2, arcWidth/2));
        setYConstraint(new MotionTracker.Constraint(){
            @Override
            float limitValue(float value, float previous, float other) {
                float angle = 90 - (((0.5f+(other/arcWidth))*angleRange) - angleRange/2);
                return -arcHeight - (float)(Math.sin(Math.toRadians(angle))*(height+adjustRadius)/2);
            }
        });
    }

    public PointF getEdgePoint(boolean leftEdge){
        return new PointF(
                leftEdge ? centerX - arcWidth/2 : centerX + arcWidth/2,
                centerY - height/2 + arcHeight
        );
    }

    private float toRadians(float angle){
       return (float)Math.toRadians(90 - angle);
    }

    public float getNormalizedValueBetween(float lo, float hi) {
        return lo + (hi - lo) * (0.5f + (motionTracker.getX() / arcWidth));
    }

    public float getNormalizedValue(){
        return getNormalizedValueBetween(0,1);
    }

    public void setNormalizedValue(float value) {
        float offset = value*arcWidth - arcWidth/2;
        motionTracker.setPosition(offset, 0);
    }

    public void setLineStyle(int color){
        lineStyle.setColor(color);
    }

    @Override
    public void onDraw(Canvas canvas, float refX, float refY) {
        lineStyle.setStyle(Paint.Style.STROKE);
        float startAngle = 270 - angleRange/2;
        float top = refY + centerY - height/2;
        canvas.drawArc(refX + centerX - width/2, top, refX + centerX + width/2, refY + centerY + height/2, startAngle, angleRange, false, lineStyle);
        super.onDraw(canvas, refX, refY);
    }
}
