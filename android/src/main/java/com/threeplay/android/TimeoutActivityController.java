package com.threeplay.android;

import android.app.Activity;
import android.util.Log;

import com.threeplay.core.Promise;

import java.lang.ref.WeakReference;

/**
 * Created by eliranbe on 2/3/17.
 */

public class TimeoutActivityController extends ContinuousTask {
    private final static int TIME_RESOLUTION_IN_MS = 250;
    private final int defaultExpiration;
    private int currentExpiration = 0;
    private long lastActivityTime;
    private WeakReference<Listener> listener = new WeakReference<>(null);

    public TimeoutActivityController(Activity activity, int defaultExpiration){
        super(TIME_RESOLUTION_IN_MS);
        this.defaultExpiration = defaultExpiration;
        reset();
        if ( activity instanceof Listener ) {
            setListener((Listener) activity);
        }
    }

    public void reset(){
        resetWithExpiration(-1);
    }

    public void resetWithExpiration(int expiration){
        currentExpiration = expiration < 0 ? defaultExpiration : expiration;
        lastActivityTime = getCurrentTime();
        if ( !isRunning() && currentExpiration > 0 ) {
            start(0);
        }
    }

    public boolean isExpired(){
        return this.currentExpiration > 0 && elapsedTimeInSeconds() >= this.currentExpiration;
    }

    public int elapsedTimeInSeconds(){
        return (int)((getCurrentTime() - lastActivityTime) / 1000);
    }

    private long getCurrentTime(){
        return System.currentTimeMillis();
    }

    public void setListener(Listener listener) {
        this.listener = new WeakReference<>(listener);
    }

    public void updateExpiration(int expiration) {
        currentExpiration = expiration < 0 ? defaultExpiration : expiration;
        if ( currentExpiration > 0 ) {
          if ( !isRunning() ) {
              start(0);
          }
        }
        else {
            cancel();
        }
    }

    public interface Listener {
        void activityTimedOut(int elapsedTime);
    }

    @Override
    public Promise<Integer> task(long elapsedTime) {
        if ( isExpired() ) {
            Listener l = listener.get();
            if ( l != null ) {
                l.activityTimedOut(elapsedTimeInSeconds());
            }
            return Promise.withResult(STOP);
        }
        return Promise.withResult(DEFAULT_INTERVAL);
    }
}
