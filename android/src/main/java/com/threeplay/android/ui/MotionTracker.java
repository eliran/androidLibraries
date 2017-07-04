package com.threeplay.android.ui;

import android.graphics.Canvas;
import android.view.MotionEvent;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by eliranbe on 1/27/17.
 */
public abstract class MotionTracker {
    public interface Listener {
        void motionTrackerEvent(MotionTracker motionTracker);
    }

    public static class Constraint {
        public final static Constraint None = new Constraint(){
            @Override
            float limitValue(float value, float previous, float other) {
                return value;
            }
        };

        public final static Constraint Lock = new Constraint(){
            @Override
            float limitValue(float value, float previous, float other) {
                return previous;
            }
        };

        public static Constraint range(float min, float max) {
            return new Constraint(min, max);
        }

        private final float min, max;

        public Constraint(){
            this(0,0);
        }

        public Constraint (float min, float max){
            this.min = min;
            this.max = max;
        }

        float limitValue(float value, float previous, float other){
            if ( value < min ) return min;
            if ( value > max ) return max;
            return value;
        }
    }

    private List<Listener> listeners = new LinkedList<>();
    private float x, y, lastX, lastY;
    private Constraint xConstraint = Constraint.None;
    private Constraint yConstraint = Constraint.None;

    public MotionTracker(float x, float y) {
        this.x = this.lastX = x;
        this.y = this.lastY = y;
    }

    public void setXConstraint(Constraint constraint){
        xConstraint = constraint == null ? Constraint.None : constraint;
    }

    public void setYConstraint(Constraint constraint){
        yConstraint = constraint == null ? Constraint.None : constraint;
    }

    public float getX() {
        return x;
    }

    public float getLastX() {
        return lastX;
    }

    public float getDeltaX() {
        return x - lastX;
    }

    public float getY() {
        return y;
    }

    public float getLastY() {
        return lastY;
    }

    public float getDeltaY() {
        return y - lastY;
    }

    public void setPosition(float x, float y){
        this.lastX = this.x; this.x = xConstraint.limitValue(x, this.x, this.y);
        this.lastY = this.y; this.y = yConstraint.limitValue(y, this.y, this.x);
    }

    public void addPosition(float dx, float dy){
        setPosition(x + dx, y + dy);
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener){
        this.listeners.remove(listener);
    }

    public abstract boolean pointInside(float x, float y);

    public abstract void onDrag(float dx, float dy);

    public abstract void onActivate();

    public abstract void onDeactivate();

    protected void notifyEvent() {
        for ( Listener listener : listeners ) {
            listener.motionTrackerEvent(this);
        }
    }

    public static class Controller {
        private LinkedList<MotionTracker> motionTrackers = new LinkedList<>();
        private LinkedList<MotionTrackerComponent> motionTrackerComponents = new LinkedList<>();
        private MotionTracker activeTracker = null;
        private float lastX, lastY;
        private float refX, refY;
        private boolean enabled = true;

        public Controller(){
            this(0, 0);
        }

        public Controller(float refX, float refY){
            this.refX = refX;
            this.refY = refY;
        }

        public void setEnabled(boolean enabled){
            this.enabled = enabled;
        }

        public void setReferencePoint(float refX, float refY){
            this.refX = refX;
            this.refY = refY;
        }

        public void addReferencePoint(float dx, float dy) {
            this.refX += dx;
            this.refY += dy;
        }

        public float getRefX(){
            return refX;
        }

        public float getRefY(){
            return refY;
        }

        public boolean onTouch(MotionEvent motionEvent){
            if ( enabled ) {
                int action = motionEvent.getAction();
                float x = motionEvent.getX(), y = motionEvent.getY();
                if (action == MotionEvent.ACTION_DOWN) {
                    for (MotionTracker tracker : motionTrackers) {
                        if (tracker.pointInside(x - refX, y - refY)) {
                            lastX = x;
                            lastY = y;
                            activeTracker = tracker;
                            tracker.onActivate();
                            return true;
                        }
                    }
                } else if (action == MotionEvent.ACTION_UP) {
                    if (activeTracker != null) {
                        activeTracker.onDeactivate();
                        activeTracker = null;
                    }
                    return true;
                } else if (action == MotionEvent.ACTION_MOVE && activeTracker != null) {
                    float dx = x - lastX, dy = y - lastY;
                    activeTracker.onDrag(dx, dy);
                    lastX = x;
                    lastY = y;
                    return true;
                }
                return false;
            }
            return true;
        }

        public void onDraw(Canvas canvas){
            for ( MotionTrackerComponent component: motionTrackerComponents ) {
                component.onDraw(canvas, refX, refY);
            }
        }

        public void addTracker(MotionTracker tracker) {
            motionTrackers.add(tracker);
        }

        public void addTrackerComponent(MotionTrackerComponent trackerComponent){
            motionTrackerComponents.add(trackerComponent);
            motionTrackers.add(trackerComponent.getMotionTracker());
        }

    }

    public static class DragMotionTracker extends MotionTracker {
        public DragMotionTracker(float x, float y) {
            super(x, y);
        }

        @Override
        public boolean pointInside(float x, float y) {
            return true;
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }

        @Override
        public void onDrag(float dx, float dy) {
            addPosition(dx, dy);
            notifyEvent();
        }
    }

    public static class ButtonMotionTracker extends MotionTracker {
        private final float width;
        private final float height;

        public ButtonMotionTracker(float x, float y, float width, float height) {
            super(x, y);
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean pointInside(float x, float y) {
            return Math.abs(x - getX()) < width/2 && Math.abs(y - getY()) < height/2;
        }

        @Override
        public void onActivate() {
            notifyEvent();
        }

        @Override
        public void onDeactivate() {
        }

        @Override
        public void onDrag(float dx, float dy) {
        }
    }

    public abstract static class MotionTrackerComponent {
        protected final MotionTracker motionTracker;

        public MotionTrackerComponent(MotionTracker motionTracker){
            this.motionTracker = motionTracker;
        }

        public void setXConstraint(Constraint constraint){
            motionTracker.setXConstraint(constraint);
        }

        public void setYConstraint(Constraint constraint){
            motionTracker.setYConstraint(constraint);
        }

        public MotionTracker getMotionTracker() {
            return motionTracker;
        }

        public void addListener(Listener listener) {
            motionTracker.addListener(listener);
        }

        public abstract void onDraw(Canvas canvas, float refX, float refY);
    }
}
