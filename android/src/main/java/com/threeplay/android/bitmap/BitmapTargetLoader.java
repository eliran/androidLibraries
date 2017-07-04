package com.threeplay.android.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import com.threeplay.core.Promise;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by eliranbe on 2/9/17.
 */

public class BitmapTargetLoader {
    private static final String TAG = "TargetLoader";
    private static final BitmapTargetLoaderCoordinator coordinator = new BitmapTargetLoaderCoordinator();

    private final Context context;

    private BitmapLoader.Request loadRequest;
    private ImageResourceHolder loadingResource = new ImageResourceHolder();
    private ImageResourceHolder failureResource = new ImageResourceHolder();

    public static BitmapTargetLoader with(BitmapLoader.Request request){
        return new BitmapTargetLoader(request, null);
    }

    public static BitmapTargetLoader with(BitmapLoader.Request request, Context context){
        return new BitmapTargetLoader(request, context);
    }

    public BitmapTargetLoader(BitmapLoader.Request loadRequest, Context context){
        this.loadRequest = loadRequest;
        this.context = context;
    }

    public BitmapTargetLoader setLoading(Bitmap bitmap){
        loadingResource = new BitmapImageResourceHolder(bitmap);
        return this;
    }

    public BitmapTargetLoader setLoading(int resId){
        loadingResource = new ResourceImageResourceHolder(resId);
        return this;
    }

    public BitmapTargetLoader setFailure(Bitmap bitmap){
        failureResource = new BitmapImageResourceHolder(bitmap);
        return this;
    }

    public BitmapTargetLoader setFailure(int resId){
        failureResource = new ResourceImageResourceHolder(resId);
        return this;
    }

    public BitmapTargetLoader setSize(int width, int height){
        loadRequest = loadRequest.resize(width, height);
        return this;
    }

    public void into(final ImageView imageView){
        coordinator.start(this, imageView);
        loadingResource.into(imageView);
        loadRequest.fetch().any(new Promise.Handler<Bitmap>() {
            @Override
            public void trigger(final Promise.Triggered<Bitmap> p) throws Exception {
                coordinator.complete(BitmapTargetLoader.this, imageView, new Runnable() {
                    @Override
                    public void run() {
                        if ( p.wasSuccessful() ) {
                            imageView.setImageBitmap(p.getResult());
                        }
                        else {
                            failureResource.into(imageView);
                        }
                    }
                });
            }
        });
    }


    private static class ImageResourceHolder {
        void into(ImageView imageView){
        }
    }

    private static class BitmapImageResourceHolder extends ImageResourceHolder {
        private final Bitmap bitmap;

        BitmapImageResourceHolder(Bitmap bitmap){
            this.bitmap = bitmap;
        }

        @Override
        void into(ImageView imageView) {
            imageView.setImageBitmap(bitmap);
        }
    }

    private class ResourceImageResourceHolder extends ImageResourceHolder {
        private final int resId;

        public ResourceImageResourceHolder(int resId) {
            this.resId = resId;
        }

        @Override
        void into(ImageView imageView) {
            imageView.setImageResource(resId);
            Drawable drawable = imageView.getDrawable();
            if ( drawable instanceof AnimationDrawable ) {
                ((AnimationDrawable)drawable).start();
            }
        }
    }

    private static class BitmapTargetLoaderCoordinator {
        private ConcurrentHashMap<ImageView, BitmapTargetLoader> activeLoader = new ConcurrentHashMap<>();

        public void start(BitmapTargetLoader bitmapTargetLoader, ImageView imageView) {
            activeLoader.put(imageView, bitmapTargetLoader);
        }

        private void complete(final BitmapTargetLoader bitmapTargetLoader, final ImageView imageView, final Runnable runnable){
            if ( activeLoader.get(imageView) == bitmapTargetLoader ) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if ( activeLoader.remove(imageView, bitmapTargetLoader) ) {
                            runnable.run();
                        }
                        else {
                            Log.d(TAG, "(1) Operation canceled on " + imageView);
                        }
                    }
                });
            }
        }
    }
}
