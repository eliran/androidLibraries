package com.threeplay.android;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.threeplay.core.Promise;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;

/**
 * Created by eliranbe on 10/6/16.
 */

public class UI {

    public interface ViewLayoutChanged {
        void viewLayoutChanged(View view);
    }

    public enum GradientDirection {
        LEFT_TO_RIGHT(0,0,1,0);


        public final float startX, startY, endX, endY;

        GradientDirection(float startX, float startY, float endX, float endY){
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }
    }

    public static Promise<Surface> surfaceOfSurfaceView(SurfaceView surfaceView){
        final Promise.Defer<Surface> defer = Promise.defer();
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                defer.resolveWithResult(surfaceHolder.getSurface());
            }

            @Override public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {}
        });
        return defer.promise;
    }

    /**
     * Based/taken from: http://stackoverflow.com/a/8799344
     * @param text Text to create bitmap for
     * @param textSize Size in points
     * @param textColor Color of text
     * @return Bitmap of text
     */
    public static Bitmap centeredTextBitmap(String text, float textSize, int textColor) {
        Paint paint = new Paint(ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.CENTER);
        float baseline = -paint.ascent(); // ascent() is negative
        int width = (int) (paint.measureText(text) + 0.5f); // round
        int height = (int) (baseline + paint.descent() + 0.5f);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawText(text, 0, baseline, paint);
        return image;
    }

    public static Bitmap bitmapFromDrawable(Drawable drawable, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static int lightenColor(int color, float a){
        return mixColors(Color.rgb(255, 255, 255), color, a);
    }

    public static int darkenColor(int color, float a){
        return mixColors(Color.rgb(0, 0, 0), color, a);
    }

    public static int mixColors(int colorA, int colorB, float a){
        float rA = Color.red(colorA) * a, gA = Color.green(colorA) * a, bA = Color.blue(colorA) * a;
        a = 1.0f - a;
        float rB = Color.red(colorB) * a, gB = Color.green(colorB) * a, bB = Color.blue(colorB) * a;

        return Color.rgb(
                Math.min(255, (int)(rA + rB)),
                Math.min(255, (int)(gA + gB)),
                Math.min(255, (int)(bA + bB))
        );
    }

    public static Rect bitmapRect(Bitmap bitmap){
        return new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
    }

    public static ShapeDrawable.ShaderFactory resizableLinearGradient(final float startX, final float startY, final float endX, final float endY, final int[] colors, final float[] positions, final Shader.TileMode tileMode){
        return new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                return new LinearGradient(startX * width, startY * height, endX * width, endY * height, colors, positions, tileMode);
            }
        };
    }

    public static Drawable linearGradientDrawable(float startX, float startY, float endX, float endY, final int[] colors, final float[] positions, final Shader.TileMode tileMode){
        PaintDrawable drawable = new PaintDrawable();
        drawable.setShape(new RectShape());
        drawable.setShaderFactory(resizableLinearGradient(startX, startY, endX, endY, colors, positions, tileMode));
        return drawable;
    }

    public static Drawable linearGradientDrawable(GradientDirection direction, final int[] colors, final float[] positions, final Shader.TileMode tileMode){
        return linearGradientDrawable(direction.startX, direction.startY, direction.endX, direction.endY, colors, positions, tileMode);
    }

    public static Drawable linearGradientDrawable(GradientDirection direction, final int[] colors, final float[] positions){
        return linearGradientDrawable(direction, colors, positions, Shader.TileMode.CLAMP);
    }

    public static float[] colorsToFloats(int[] colors, boolean withAlpha) {
        float[] fcolors = new float[colors.length * (withAlpha ? 4 : 3)];
        int findex = 0;
        for (int color : colors) {
            fcolors[findex++] = Color.red(color) / 255.0f;
            fcolors[findex++] = Color.green(color) / 255.0f;
            fcolors[findex++] = Color.blue(color) / 255.0f;
            if (withAlpha) {
                fcolors[findex++] = Color.alpha(color) / 255.0f;
            }
        }
        return fcolors;
    }

    public static void onViewLayoutChange(final View view, final ViewLayoutChanged delegate, final boolean always){
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(final View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRIght, int oldBottom) {
                if ( !always ) {
                    view.removeOnLayoutChangeListener(this);
                }
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        delegate.viewLayoutChanged(view);
                    }
                });
            }
        });
    }

    public static abstract class ActivityThreadRunnable implements Runnable {
        private Activity activity;
        public ActivityThreadRunnable(Activity activity){
            this.activity = activity;
        }

        @Override
        public void run() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onMainThread();
                }
            });
        }

        public abstract void onMainThread();
    }
}
