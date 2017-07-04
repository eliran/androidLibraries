package com.threeplay.android;

import android.app.Activity;

import com.threeplay.core.Promise;

/**
 * Created by eliranbe on 1/17/17.
 */

public class Callback {
    public static <T> Promise.Handler<T> fromPromise(final OnComplete onComplete) {
        return new Promise.Handler<T>() {
            @Override
            public void trigger(Promise.Triggered<T> p) throws Exception {
                if ( onComplete != null ) onComplete.completed();
            }
        };
    }

    public static <T> Promise.Handler<T> fromPromise(final OnCompleteWithResult<T> onCompleteWithResult) {
        return new Promise.Handler<T>() {
            @Override
            public void trigger(Promise.Triggered<T> p) throws Exception {
                if( onCompleteWithResult != null ) onCompleteWithResult.completedWithResult(p.getResult());
            }
        };
    }

    public interface OnCompleteWithResult<T> {
        void completedWithResult(T result);
    }

    public interface OnMap<T, U> {
        U elementWithValue(T value);
    }

    public interface OnFilter<T> extends OnMap<T, Boolean>{
    }

    public interface OnComplete {
        void completed();
    }

    public interface OnProgressWithValue<T> {
        void progressWithValue(T value);
    }

    // TODO: Move OnCompleteInActivity & runOnActivity to their own Android based class and keep the Callback class pure Java so it could move to the Core module
    public abstract static class OnCompleteInActivity implements OnComplete {
        private final Activity activity;

        public OnCompleteInActivity(Activity activity){
            this.activity = activity;
        }

        @Override
        public void completed() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    completedOnUiThread();
                }
            });
        }

        public abstract void completedOnUiThread();
    }

    public static <T> Promise.Handler<T> runOnActivity(final Activity activity, final OnCompleteWithResult<T> onCompleteWithResult){
        return new Promise.Handler<T>() {
            @Override
            public void trigger(final Promise.Triggered<T> p) throws Exception {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if ( onCompleteWithResult != null ) { onCompleteWithResult.completedWithResult(p.getResult()); }
                    }
                });
            }
        };
    }

}

