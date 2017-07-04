package com.threeplay.android;

import android.util.Log;

import com.threeplay.core.Promise;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by eliranbe on 1/25/17.
 */
public abstract class ContinuousTask {
    public final static int DEFAULT_INTERVAL = -2;
    public final static int STOP = -1;
    public final static int NO_DELAY = 0;

    private Timer timer;
    private int interval;
    private long lastRequestedTime;
    private long expectedRequestTime;
    private boolean cancelTask = false;

    public static int fromSeconds(int seconds){
        return seconds * 1000;
    }

    private class InternalTask extends TimerTask {
        @Override
        public void run() {
            cancelTimer();
            long elapsesTime = System.currentTimeMillis() - lastRequestedTime;
             Promise<Integer> promise = task(elapsesTime);
            if ( promise != null ) {
                promise.then(processTaskResult(), processTaskFailure());
            }
            else {
                taskFailed();
            }
        }
    }

    private Promise.Handler<Integer> processTaskResult(){
        return new Promise.Handler<Integer>() {
            @Override
            public void trigger(Promise.Triggered<Integer> p) throws Exception {
                int continueTask = p.getResult();
                if ( !cancelTask && continueTask != STOP ) {
                    startNextWithInterval(continueTask >= 0 ? continueTask : interval);
                 }
            }
        };
    }

    private Promise.Handler<Integer> processTaskFailure(){
        return new Promise.Handler<Integer>() {
            @Override
            public void trigger(Promise.Triggered<Integer> p) throws Exception {
                taskFailed();
            }
        };
    }

    private void startNextWithInterval(int interval){
        start((int) (interval - Math.min(System.currentTimeMillis() - expectedRequestTime, interval)));
    }

    private void taskFailed(){
        startNextWithInterval(interval);
    }

    public ContinuousTask(int interval) {
        this.interval = interval;
    }

    public void setInterval(int interval){
        this.interval = interval;
    }

    public boolean isRunning() {
        return timer != null;
    }

    public synchronized void start(int delay) {
        cancelTimer();
        cancelTask = false;
        timer = new Timer();
        lastRequestedTime = System.currentTimeMillis();
        expectedRequestTime = lastRequestedTime + delay;
        timer.schedule(new InternalTask(), delay);
    }

    public abstract Promise<Integer> task(long elapsedTime);

    public void cancel() {
        cancelTask = true;
        cancelTimer();
    }

    private synchronized void cancelTimer(){
        if (timer != null) {
            timer.cancel();
        }
        timer = null;
    }
}
