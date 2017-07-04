package com.threeplay.android.fetcher;

import com.threeplay.android.events.ProgressEventSource;
import com.threeplay.core.Promise;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by eliranbe on 1/10/17.
 */

public class ResourceFetchManager<T> implements Runnable {

    public interface Fetch<T> {
        int getTotal();
        Promise<T> next(ResourceFetcher<T> defaultFetcher);
    }

    public interface FetchHandler<T> {
        void onBeginFetch(String key);

        void onContent(String key, T content, Promise.Defer<T> defer) throws Exception;

        void onEndFetch(String key, boolean successful);

        void onComplete(int total, int failed, int canceled);
    }

    public abstract static class FetchContentHandler<T> implements FetchHandler<T> {
        @Override
        public void onBeginFetch(String key) {}

        @Override
        public abstract void onContent(String key, T content, Promise.Defer<T> defer) throws Exception;

        @Override
        public void onEndFetch(String key, boolean successful) {}

        @Override
        public void onComplete(int total, int failed, int canceled) {}
    }

    public static class FetchGroup<T> implements Fetch<T> {
        private final Queue<String> keys;
        private final int total;
        private final ResourceFetcher<T> fetcher;
        private final FetchHandler<T> handler;

        private AtomicInteger completed = new AtomicInteger(0);
        private AtomicInteger failed = new AtomicInteger(0);

        public FetchGroup(String key, FetchHandler<T> handler){
            this(null, Collections.singletonList(key), handler);
        }

        public FetchGroup(List<String> keys, FetchHandler<T> handler) {
            this(null, keys, handler);
        }

        public FetchGroup(ResourceFetcher<T> fetcher, List<String> keys, FetchHandler<T> handler){
            this.total = keys.size();
            this.keys = new ConcurrentLinkedQueue<>(keys);
            this.handler = handler;
            this.fetcher = fetcher;
        }

        @Override
        public int getTotal() {
            return total;
        }

        public Promise<T> next(final ResourceFetcher<T> defaultFetcher){
            final String key = keys.poll();
            if ( key == null ) { return null; }
            handler.onBeginFetch(key);
            Promise<T> promise = (fetcher != null ? fetcher : defaultFetcher).fetch(key);
            promise = promise.defer(new Promise.DeferBlock<T>() {
                @Override
                public void trigger(Promise.Defer<T> defer, Promise.Triggered<T> p) throws Exception {
                    if ( p.wasSuccessful() ) {
                        handler.onContent(key, p.getResult(), defer);
                    }
                    else {
                        defer.rejectWithException(p.getException());
                    }
                }
            });
            return promise.any(new Promise.Handler<T>() {
                @Override
                public void trigger(Promise.Triggered<T> p) throws Exception {
                    if ( !p.wasSuccessful() ) { failed.incrementAndGet(); }
                    handler.onEndFetch(key, p.wasSuccessful());
                    if ( completed.incrementAndGet() == total ) {
                        handler.onComplete(total, failed.get(), 0);
                    }
                }
            });
        }
    }

    private final ResourceFetcher<T> defaultFetcher;
    private final ProgressEventSource progressEventSource = new ProgressEventSource.Base();

    private Queue<Fetch<T>> fetchQueue = new ConcurrentLinkedQueue<>();
    private AtomicInteger completed = new AtomicInteger(0);
    private AtomicInteger failed = new AtomicInteger(0);
    private AtomicInteger total = new AtomicInteger(0);
    private AtomicInteger pending = new AtomicInteger(0);

    private Boolean close = null;

    private final int maxConcurrent;

    public ResourceFetchManager(int maxConcurrent, ResourceFetcher<T> defaultFetcher){
        this.maxConcurrent = maxConcurrent;
        this.defaultFetcher = defaultFetcher;
        start();
    }

    public synchronized void start(){
        if ( close == null ) {
            close = false;
            new Thread(this).start();
        }
    }

    public synchronized void stop(){
        if ( close != null ) {
            close = true;
        }
    }

    public synchronized void closeIfEmpty(){
        if ( total.get() == 0 ) {
            stop();
        }
    }

    public synchronized void add(Fetch<T> fetch){
        total.addAndGet(fetch.getTotal());
        fetchQueue.add(fetch);
        notifyProgressChanged();
    }

    @Override
    public void run() {
        Fetch<T> fetch = null;
        while ( !close ) {
            if ( fetch == null ) { fetch = fetchQueue.poll(); }
            if ( fetch == null || pending.get() >= maxConcurrent ) {
                sleep();
            }
            else if ( !fetchResource(fetch) ) {
                fetch = null;
            }
        }
        notifyProgressChanged();
    }

    private void sleep(){
        try { Thread.sleep(1); } catch ( InterruptedException ok ) {}
    }

    private synchronized boolean fetchResource(final Fetch<T> fetch){
        if ( fetch != null ) {
            pending.incrementAndGet();
            Promise<T> promise = fetch.next(defaultFetcher);
            if ( promise == null ) {
                pending.decrementAndGet();
                return false;
            }
            notifyProgressChanged();
            promise.any(new Promise.Handler<T>() {
                @Override
                public void trigger(Promise.Triggered<T> p) throws Exception {
                    completed.incrementAndGet();
                    if ( !p.wasSuccessful() ) { failed.incrementAndGet(); }
                    pending.decrementAndGet();
                    notifyProgressChanged();
                }
            });
            return true;
        }
        return false;
    }

    public ProgressEventSource getProgressEventSource(){
        return progressEventSource;
    }

    private void notifyProgressChanged(){
        progressEventSource.notify(ProgressEventSource.Events.progress(total.get(), completed.get(), failed.get(), pending.get()));
    }

    @Override
    protected void finalize() throws Throwable {
        stop();
        super.finalize();
    }
}
