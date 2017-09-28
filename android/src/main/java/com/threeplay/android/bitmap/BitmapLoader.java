package com.threeplay.android.bitmap;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;

//import com.android.mms.exif.ExifInterface;
import com.threeplay.android.fetcher.ResourceFetcher;
import com.threeplay.core.Logger;
import com.threeplay.core.Promise;
import com.threeplay.core.QUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by eliranbe on 12/18/16.
 */

public abstract class BitmapLoader {

    private class Size {
        final int width, height;

        Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    public static Request fromResource(Resources resources, int resId){
        return new Request(new BitmapResourceLoader(resources, resId));
    }

    public static Request fromUri(ContentResolver resolver, Uri uri){
        return new Request(new BitmapUriLoader(resolver, uri));
    }

    public static Request fromBytes(byte[] content){
        return new Request(new BitmapBytesLoader(content, 0, content.length));
    }

    public static Request fromFile(File file){
        return new Request(new BitmapFileLoader(file));
    }

    public static Request fromBitmap(Bitmap bitmap) {
        return new Request(new BitmapBitmapLoader(bitmap));
    }

    public static Request fromBitmapFetcher(ResourceFetcher<Bitmap> fetcher, String key) {
        return new Request(new BitmapFetcherLoader(fetcher, key));
    }

    public static Request fromFetcher(ResourceFetcher<InputStream> fetcher, String key){
        return new Request(new InputStreamFetcherLoader(fetcher, key));
    }

    Promise<Bitmap> fetch(final Request request){
        final Promise.Defer<Bitmap> defer = Promise.defer();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap result = immediateFetch(request);
                    defer.resolveWithResult(result);
                }
                catch ( OutOfMemoryError e ) {
                    defer.rejectWithException(new Exception("Out Of Memory while loading bitmap"));
                }
            }
        }).start();
        return defer.promise;
    }

    Bitmap immediateFetch(Request request){
        return decodeWithOptions(request.decodeOptionsWithBounds(decodeBitmapSize()));
    }

    private Size decodeBitmapSize() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inJustDecodeBounds = true;
        decodeWithOptions(options);
        return new Size(options.outWidth, options.outHeight);
    }

    abstract Bitmap decodeWithOptions(BitmapFactory.Options options);

    int decodeBitmapRotation() {
        return 0;
    }

    private interface Transform {
        Bitmap transform(Bitmap bitmap);
    }

    private static class ScaleTransform implements Transform {
        private final boolean uniform;
        private final int maxWidth, maxHeight;

        ScaleTransform(){
            this(0,0,true);
        }

        ScaleTransform(int maxWidth, int maxHeight, boolean uniform){
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
            this.uniform = uniform;
        }

        public ScaleTransform switchDimensions(){
            return new ScaleTransform(maxHeight, maxWidth, uniform);
        }

        public ScaleTransform smallerLimit(int width, int height){
            width = maxWidth == 0 ? width : Math.min(width, maxWidth);
            height = maxHeight == 0 ? height : Math.min(height, maxHeight);
            if ( width != maxWidth || height != maxHeight ) {
                return new ScaleTransform(width, height, uniform);
            }
            return this;
        }

        @Override
        public Bitmap transform(Bitmap bitmap) {
            int w = bitmap.getWidth(), h = bitmap.getHeight();
            int scaledW = w, scaledH = h;
            if ( uniform ) {
                double scale = 1.0 / unifiedScale(w, h);
                scaledW = (int) (w * scale);
                scaledH = (int) (h * scale);
            }
            if ( scaledW > 0 && scaledH > 0 && (scaledW < w || scaledH < h) ) {
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        scaledW,
                        scaledH,
                        true);
                bitmap.recycle();
                return scaledBitmap;
            }
            return bitmap;
        }

        double unifiedScale(int width, int height){
            return Math.max(scale(width, maxWidth), scale(height, maxHeight));
        }

        double scale(int dim, int max){
            if ( max > 0 && dim > max ) {
                return (double)dim / max;
            }
            return 1;
        }
    }

    private static class RotateTransform implements Transform {
        private final int angle;

        RotateTransform(int angle){
            this.angle = angle;
        }

        @Override
        public Bitmap transform(Bitmap bitmap) {
            if ( angle != 0 ) {
                int w = bitmap.getWidth(), h = bitmap.getHeight();
                Logger.i("RotateTransform: Rotating image by " + angle + " degrees");
                Matrix M = new Matrix();
                M.postRotate(angle, w/2, h/2);
                return Bitmap.createBitmap(bitmap, 0,0,w,h,M,false);
            }
            return bitmap;
        }
    }

    private static class PaddingTransform implements Transform {
        private final int left, right, top, bottom;

        PaddingTransform(int left, int top, int right, int bottom){
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        @Override
        public Bitmap transform(Bitmap bitmap) {
            if ( left != 0 || right != 0 || top != 0 || bottom != 0 ) {
                Bitmap result = Bitmap.createBitmap(bitmap.getWidth() + left + right, bitmap.getHeight() + top + bottom, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(result);
                canvas.drawBitmap(bitmap, left, top, null);
                return result;
            }
            return bitmap;
        }
    }

    private static class ResizeTransform implements Transform {
        private final int width, height;

        ResizeTransform(int width, int height){
            this.width = width;
            this.height = height;
        }

        @Override
        public Bitmap transform(Bitmap bitmap) {
            Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            int x = (width - bitmap.getWidth()) / 2, y = (height - bitmap.getHeight()) / 2;
            canvas.drawBitmap(bitmap, x, y, null);
            return result;
        }
    }

    public static class Request {
        private static final String TAG = "Request";
        private final BitmapLoader loader;
        private final ScaleTransform scaleTransform;
        private final List<Transform> transforms;

        private Request(BitmapLoader loader){
            this(loader, new ScaleTransform(), null);
        }

        private Request(BitmapLoader loader, ScaleTransform scaleTransform, List<Transform> transforms){
            this.loader = loader;
            this.scaleTransform = scaleTransform;
            this.transforms = transforms != null ? transforms : Collections.<Transform>emptyList();
        }

        public Request limit(int maxWidth, int maxHeight){
            return new Request(loader, new ScaleTransform(maxWidth, maxHeight, true), transforms);
        }

        public Request rotate(int rotation){
            return new Request(
                    loader,
                    rotation == 90 || rotation == 270 ? scaleTransform.switchDimensions() : scaleTransform,
                    appendTransform(new RotateTransform(rotation))
            );
        }

        public Request padding(int left, int top, int right, int bottom){
            return new Request(
                    loader,
                    scaleTransform,
                    appendTransform(new PaddingTransform(left, top, right, bottom))
            );
        }

        public Request fixOrientation(){
            int rotation = loader.decodeBitmapRotation();
            if ( rotation != 0 ) {
                return rotate(rotation);
            }
            return this;
        }

        public Request resize(int width, int height){
            return new Request(loader, scaleTransform.smallerLimit(width, height), appendTransform(new ResizeTransform(width, height)));
        }

        private List<Transform> appendTransform(Transform transform){
            if ( transform != null ) {
                List<Transform> result = new LinkedList<>(transforms);
                result.add(transform);
                return result;
            }
            return transforms;
        }

        public Bitmap immediateFetch(){
            return postProcessing(loader.immediateFetch(this));
        }

        public Promise<Bitmap> fetch(){
            return loader.fetch(this).then(new Promise.Handler<Bitmap>() {
                @Override
                public void trigger(Promise.Triggered<Bitmap> p) throws Exception {
                    p.successfulWithResult(postProcessing(p.getResult()));
                }
            });
        }

        private Bitmap postProcessing(Bitmap bitmap){
            if ( bitmap == null ) { return null; }
            bitmap = scaleTransform.transform(bitmap);
            for ( Transform transform: transforms ) {
                bitmap = transform.transform(bitmap);
            }
            return bitmap;
        }

        BitmapFactory.Options decodeOptionsWithBounds(int width, int height){
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            int scale = (int)scaleTransform.unifiedScale(width, height);
            options.inSampleSize = 1;
            while ( scale >= 2 ) {
                scale /= 2;
                options.inSampleSize *= 2;
            }
            //if ( options.inSampleSize > 1 ) {
            //    Logger.i(TAG + ": Downscaling by " + options.inSampleSize + " while loading image of size " + width + "x" + height );
            //}
            return options;
        }

        BitmapFactory.Options decodeOptionsWithBounds(Size size){
            return decodeOptionsWithBounds(size.width, size.height);
        }
    }

    private static class BitmapResourceLoader extends BitmapLoader {
        private final Resources resources;
        private final int resId;

        BitmapResourceLoader(Resources resources, int resId){
            this.resources = resources;
            this.resId = resId;
        }

        @Override
        Bitmap decodeWithOptions(BitmapFactory.Options options){
            return BitmapFactory.decodeResource(resources, resId, options);
        }
    }

    private static class BitmapUriLoader extends BitmapLoader {
        private final ContentResolver resolver;
        private final Uri uri;

        BitmapUriLoader(ContentResolver resolver, Uri uri){
            this.resolver = resolver;
            this.uri = uri;
        }

        @Override
        Bitmap decodeWithOptions(BitmapFactory.Options options) {
            try {
                InputStream is = resolver.openInputStream(uri);
                Bitmap result = BitmapFactory.decodeStream(is, null, options);
                is.close();
                return result;
            } catch ( IOException ok ) {
                return null;
            }
        }

        @Override
        int decodeBitmapRotation() {
            return 0;
            //return ExifInterface.getRotationForOrientationValue((short)getOrientation());
        }

//        private int getOrientation(){
//            try {
//                InputStream is = resolver.openInputStream(uri);
//                if ( is != null ) {
//                    ExifInterface exif = new ExifInterface();
//                    exif.readExif(is);
//                    is.close();
//                    return exif.getTagIntValue(ExifInterface.TAG_ORIENTATION);
//                }
//            } catch ( IOException ok ) {
//            }
//            return ExifInterface.Orientation.TOP_LEFT;
//        }
    }

    private static class BitmapBytesLoader extends BitmapLoader {
        private final byte[] content;
        private final int offset, length;

        public BitmapBytesLoader(byte[] content, int offset, int length) {
            this.content = content;
            this.offset = offset;
            this.length = length;
        }

        @Override
        Bitmap decodeWithOptions(BitmapFactory.Options options) {
            return BitmapFactory.decodeByteArray(content, offset, length, options);
        }
    }

    private static class BitmapBitmapLoader extends BitmapLoader {

        private final Bitmap source;

        public BitmapBitmapLoader(Bitmap source){
            this.source = source;
        }

        @Override
        Bitmap decodeWithOptions(BitmapFactory.Options options) {
            if ( options.inJustDecodeBounds ) {
                options.outWidth = source.getWidth();
                options.outHeight = source.getHeight();
            }
            return source;
        }
    }

    private static class BitmapFileLoader extends BitmapLoader {
        private final File file;
        public BitmapFileLoader(File file) {
            this.file = file;
        }

        @Override
        Bitmap decodeWithOptions(BitmapFactory.Options options) {
            try {
                InputStream is = new FileInputStream(file);
                Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
                is.close();
                return bitmap;
            } catch ( IOException ok ) {
                return null;
            }
        }
    }

    private static class BitmapFetcherLoader extends BitmapLoader {
        private final ResourceFetcher<Bitmap> fetcher;
        private final String key;

        BitmapFetcherLoader(ResourceFetcher<Bitmap> fetcher, String key){
            this.fetcher = fetcher;
            this.key = key;
        }

        @Override
        Bitmap decodeWithOptions(BitmapFactory.Options options) {
            try {
                return fetcher.fetch(key).join();
            } catch ( Exception e ) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private static class InputStreamFetcherLoader extends BitmapLoader {
        private final ResourceFetcher<InputStream> fetcher;
        private final String key;
        private byte[] content;

        InputStreamFetcherLoader(ResourceFetcher<InputStream> fetcher, String key){
            this.fetcher = fetcher;
            this.key = key;
        }

        @Override
        Bitmap decodeWithOptions(BitmapFactory.Options options) {
            if ( content == null ) {
                try {
                    InputStream is = fetcher.fetch(key).join();
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    QUtils.copyStreamTo(is, os);
                    is.close();
                    content = os.toByteArray();
                } catch ( Exception e ) {
                    e.printStackTrace();
                    return null;
                }
            }
            return BitmapFactory.decodeByteArray(content, 0, content.length, options);
        }
    }
}