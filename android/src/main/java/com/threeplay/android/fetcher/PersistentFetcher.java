package com.threeplay.android.fetcher;

import android.util.Log;

import com.threeplay.android.ContinuousTask;
import com.threeplay.core.Promise;

/**
 * Created by eliranbe on 6/30/17.
 */

public class PersistentFetcher<T> {
    private final static String TAG = "PersistentFetch";

    private final ResourceFetcher<T> fetcher;
    private final String key;
    private Listener<T> listener;
    private RetryStrategy retryStrategy;
    private FetchTask fetchTask;

    interface RetryStrategy {
        int NO_MORE_RETRIES = -1;
        void reset();
        int nextRetryInMillis();
    }

    public interface Listener<T> {
        void fetcherRetrieved(String key, T resource);
    }

    public PersistentFetcher(ResourceFetcher<T> fetcher, String key){
        this(fetcher, key, new DefaultRetryStrategy());
    }

    public PersistentFetcher(ResourceFetcher<T> fetcher, String key, RetryStrategy retryStrategy){
        this.fetcher = fetcher;
        this.key = key;
        this.retryStrategy = retryStrategy;
        this.fetchTask = new FetchTask();
    }

    public PersistentFetcher<T> setListener(Listener<T> listener){
        this.listener = listener;
        return this;
    }

    public PersistentFetcher<T> start(){
        fetchTask.startFetch();
        return this;
    }

    public void cancel(){
        fetchTask.cancel();
    }

    private Promise<T> fetch(){
        return fetcher.fetch(key).then(resourceReceivedHandler());
    }

    private Promise.Handler<T> resourceReceivedHandler() {
        return new Promise.Handler<T>() {
            @Override
            public void trigger(Promise.Triggered<T> p) throws Exception {
                if ( listener != null ) {
                    listener.fetcherRetrieved(key, p.getResult());
                }
            }
        };
    }

    public static class DefaultRetryStrategy implements RetryStrategy {
        private int nextRetryTime;
        private final int minRetryTime;
        private final int maxRetryTime;

        public DefaultRetryStrategy(){
            this(1000, 30000);
        }

        public DefaultRetryStrategy(int minTime, int maxTime){
            this.minRetryTime = minTime;
            this.maxRetryTime = maxTime;
            reset();
        }

        @Override
        public int nextRetryInMillis() {
            int time = nextRetryTime;
            nextRetryTime = Math.min(nextRetryTime * 2, maxRetryTime);
            return  time;
        }

        @Override
        public void reset() {
            nextRetryTime = minRetryTime;
        }
    }

    private class FetchTask extends ContinuousTask {
        private int timeFromLastRequest = 0;
        private int nextRequestTime = 0;
        public FetchTask() {
            super(100);
        }

        public void startFetch(){
            timeFromLastRequest = 0;
            nextRequestTime = 0;
            retryStrategy.reset();
            start(0);
        }

        @Override
        public Promise<Integer> task(long elapsedTime) {
            timeFromLastRequest += elapsedTime;
            if ( timeFromLastRequest >= nextRequestTime ) {
                timeFromLastRequest = 0;
                nextRequestTime = retryStrategy.nextRetryInMillis();
                return fetch().filter(new Promise.Filter<T, Integer>() {
                    @Override
                    public void filter(Promise.Defer<Integer> defer, boolean successful, T result, Exception exception) {
                        defer.resolveWithResult(successful ? STOP : DEFAULT_INTERVAL);
                    }
                });
            }
            return null;
        }
    }
}
