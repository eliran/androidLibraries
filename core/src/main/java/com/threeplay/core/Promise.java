package com.threeplay.core;

import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by eliranbe on 5/24/16.
 */
public class Promise<T> {
    private final static int SUCCESS_HANDLER = 0;
    private final static int FAILURE_HANDLER = 1;

    private volatile int triggered = -1;
    private final Handler<T>[] handlers = new Handler[2];
    private Promise<T> nextPromise;
    private Promise<T> intermediatePromise;

    private int forwardTrigger;
    private T result, forwardResult;
    private Exception exception, forwardException;

    private Promise() {
    }

    public static <T> Defer<T> defer() {
        return new Defer<>();
    }

    public static <T> Promise<T> promise() {
        return new Promise<>();
    }

    public static<T, U> Promise<List<T>> fromList(List<U> list, ListDefer<U, T> deferBlock){
        List<Promise<T>> promises = new LinkedList<>();
        for (U entry: list) {
            Defer<T> defer = new Promise.Defer<>();
            try {
                deferBlock.trigger(defer, entry);
            } catch ( Exception e ) {
                defer.rejectWithException(e);
            }
            promises.add(defer.promise);
        }
        return all(promises);
    }

    public static <T> Promise<T> withHandler(Handler<T> handler) {
        return withHandler(null, handler);
    }

    public static <T> Promise<T> withHandler(T result, Handler<T> handler) {
        return withResult(result).then(handler);
    }

    public static <T> Promise<T> withException(Exception e) {
        Defer<T> defer = defer();
        defer.rejectWithException(e);
        return defer.promise;
    }

    public static <T> Promise<T> withResult(T result){
        Defer<T> defer = defer();
        defer.resolveWithResult(result);
        return defer.promise;
    }

    public static <T> Promise<List<T>> collect(int count, CollectBlock<T> block){
        List<Promise<T>> promises = new LinkedList<>();
        for ( int i = 0; i < count; i++ ) {
            promises.add(block.promiseOfIndex(i));
        }
        return Promise.all(promises);
    }

    public Promise<T> defer(final DeferBlock<T> deferBlock){
        final Defer<T> defer = defer();
        this.any(new Handler<T>() {
            @Override
            public void trigger(Triggered<T> p) throws Exception {
                try {
                    deferBlock.trigger(defer, p);
                } catch ( Exception e ) {
                    defer.rejectWithException(e);
                }
            }
        });
        return defer.promise;
    }

    public static <T> Promise<List<T>> all(final Promise<T>... promises){
        return all(Arrays.asList(promises));
    }

    public static <T> Promise<List<T>> all(final List<Promise<T>> promises){
        final Defer<List<T>> defer = defer();
        final T[] results = (T[])new Object[promises.size()];
        int index = 0;
        final AtomicInteger promises_resolved = new AtomicInteger(0);
        final AtomicBoolean any_promise_rejected = new AtomicBoolean(false);
        for(final Promise<T> promise: promises){
            final int idx = index;
            promise.then(new Handler<T>() {
                @Override
                public void trigger(Triggered<T> p) throws Exception {
                    if (!any_promise_rejected.get()) {
                        results[idx] = p.getResult();
                        if (promises_resolved.addAndGet(1) == promises.size()) {
                            defer.resolveWithResult(Arrays.asList(results));
                        }
                    }
                }
            }, new Handler<T>() {
                @Override
                public void trigger(Triggered<T> p) throws Exception {
                    if (!any_promise_rejected.getAndSet(true)) {
                        defer.rejectWithException(p.getException());
                    }
                }
            });
            ++index;
        }
        return defer.promise;
    }

    @SuppressWarnings("unchecked")
    public static <T> Promise<T[]> allAs(final Class<T> type, final Promise<T>... promises){
        return all(promises).then(new Handler() {
            @Override
            public void trigger(Triggered p) throws Exception {
                List<T> list = (List<T>)p.getResult();
                T[] array = (T[])Array.newInstance(type, promises.length);
                p.successfulWithResult(list.toArray(array));
            }
        });
    }

    public boolean wasTriggered() {
        return triggered != -1;
    }

    public boolean wasSuccessful() {
        return triggered == SUCCESS_HANDLER;
    }

    protected T getResult() {
        return result;
    }

    protected Exception getException() {
        return exception;
    }

    public Promise<T> then(Handler<T> thenHandler, Handler<T> failHandler){
        return setHandlers(thenHandler, failHandler);
    }

    public Promise<T> then(Handler<T> handler) {
        return setHandlers(handler, null);
    }

    public Promise<T> fail(Handler<T> handler) {
        return setHandlers(null, handler);
    }

    public Promise<T> any(Handler<T> handler) {
        return setHandlers(handler, handler);
    }

    public Promise<T> any(final Runnable runnable) {
        return this.any(new Handler<T>() {
            @Override
            public void trigger(Triggered<T> p) throws Exception {
                runnable.run();
            }
        });
    }

    public Promise<T> then(final Defer<T> defer) {
        return any(new Handler<T>() {
            @Override
            public void trigger(Triggered<T> p) throws Exception {
                p.continueAfterPromise(defer.promise);
                if ( p.wasSuccessful() ) {
                    defer.resolveWithResult(p.getResult());
                }
                else {
                    defer.rejectWithException(p.getException());
                }
            }
        });
    }

    public Promise<T> then(final Sync sync){
        final Promise.Defer<T> defer = new Defer<>();
        try {
            sync.sync(new Sync.Done() {
                @Override
                public void synced() {
                    defer.resolveWithResult(result);
                }
            });
        } catch ( Exception e ) {
            defer.rejectWithException(e);
        }
        return defer.promise;
    }

    public T join() throws Exception {
        final AtomicReference<Exception> exception = new AtomicReference<>(null);
        final AtomicReference<T> result = new AtomicReference<>(null);
        final AtomicBoolean completed = new AtomicBoolean(false);
        any(new Handler<T>() {
            @Override
            public void trigger(Triggered<T> p) throws Exception {
                result.set(p.getResult());
                exception.set(p.getException());
                completed.set(true);
            }
        });
        while ( !completed.get() ) {
        }
        if ( exception.get() != null ) {
            throw exception.get();
        }
        return result.get();
    }



    public <U> Promise<U> then(final Promise<U> nextPromise){
        return nextPromise;
    }

    public <U>Promise<U> then(final Convert<T, U> convert){
        final Defer<U> defer = new Defer<>();
        then(new Handler<T>() {
            @Override
            public void trigger(Triggered<T> p) throws Exception {
                convert.convert(defer, p.getResult());
            }
        }).fail(new Handler<T>() {
            @Override
            public void trigger(Triggered<T> p) throws Exception {
                defer.rejectWithException(p.getException());
            }
        });
        return defer.promise;
    }

    public Promise<T> then(final PromiseRunnable<T> runnable){
        return this.then(new Handler<T>() {
            @Override
            public void trigger(Triggered<T> p) throws Exception {
                if ( p.wasSuccessful() ) {
                    runnable.run(p.getResult());
                }
            }
        });
    }

    public Promise<T> then(final Runnable runnable){
        return this.then(new Handler<T>() {
            @Override
            public void trigger(Triggered<T> p) throws Exception {
                runnable.run();
            }
        });
    }

    public <U>Promise<U> filter(final Filter<T, U> filter) {
        final Defer<U> defer = defer();
        any(new Handler<T>() {
            @Override
            public void trigger(Triggered<T> p) throws Exception {
                filter.filter(defer, p.wasSuccessful(), p.getResult(), p.getException());
            }
        });
        return defer.promise;
    }

    public Promise<T> logException(final Logger logger){
        return fail(new Handler<T>() {
            @Override
            public void trigger(Triggered<T> p) throws Exception {
                PrintWriter printWriter = new PrintWriter(logger.writer(Logger.ERROR, null));
                //noinspection ThrowableResultOfMethodCallIgnored
                p.getException().printStackTrace(printWriter);
                printWriter.close();
            }
        });
    }

    private void forward(int triggerType, T result, Exception exception){
        forwardTrigger = triggerType;
        forwardResult = result;
        forwardException = exception;
    }

    private Promise<T> setHandlers(Handler<T> success, Handler<T> failure) {
        if ( !allocateNextPromiseIfNeeded() ) return nextPromise.setHandlers(success, failure);

        handlers[SUCCESS_HANDLER] = success;
        handlers[FAILURE_HANDLER] = failure;

        triggerHandler();
        return nextPromise;
    }

    private synchronized boolean allocateNextPromiseIfNeeded(){
        if ( nextPromise != null ) return false;
        nextPromise = new Promise<>();
        return true;
    }

    private synchronized void trigger(int handlerIndex, T result, Exception exception) {
        this.result = this.forwardResult = result;
        this.exception = this.forwardException = exception;
        this.forwardTrigger = handlerIndex;
        this.triggered = handlerIndex;
        triggerHandler();
    }

    private void triggerHandler(){
        if ( triggered != -1 && nextPromise != null ) {
            Handler<T> handler = handlers[triggered];
            if ( handler != null ) {
                Triggered<T> triggered = new Triggered<>(this);
                try {
                    handler.trigger(triggered);
                } catch (Exception e) {
                    forward(FAILURE_HANDLER, result, e);
                }
                intermediatePromise = triggered.nextPromise;
            }
            nullifyInternals();
            if ( intermediatePromise != null ) {
                nullifyForward();
                intermediatePromise.any(new Handler<T>() {
                    @Override
                    public void trigger(Triggered<T> p) throws Exception {
                        nextPromise.trigger(p.wasSuccessful() ? SUCCESS_HANDLER : FAILURE_HANDLER, p.getResult(), p.getException());
                    }
                });
            }
            else {
                nextPromise.trigger(forwardTrigger, forwardResult, forwardException);
                nullifyForward();
            }
        }
    }

    private void nullifyInternals(){
        result = null; exception = null;
        handlers[0] = handlers[1] = null;
    }

    private void nullifyForward(){
        forwardResult = null; forwardException = null;
    }

    public static <U> Filter<U, String> toStringFilter() {
        return new Filter<U, String>() {
            @Override
            public void filter(Defer<String> defer, boolean successful, U result, Exception exception) {
                defer.resolveWithResult(result.toString());
            }
        };
    }

    public interface PromiseRunnable<T> {
        void run(T result);
    }

    public interface Filter<T, U> {
        void filter(Defer<U> defer, boolean successful, T result, Exception exception);
    }

    public static class Defer<T> {
        private final AtomicBoolean triggered = new AtomicBoolean();
        public final Promise<T> promise = new Promise<>();

        public Promise<T> resolveWithResult(T result) {
            if ( !verifyNotTriggered() ) return null;
            promise.trigger(SUCCESS_HANDLER, result, null);
            return promise;
        }

        public Promise<T> rejectWithException(Exception exception) {
            if ( !verifyNotTriggered() ) return null;
            promise.trigger(FAILURE_HANDLER, null, exception);
            return promise;
        }

        public Promise<T> then(Handler<T> thenHandler, Handler<T> failHandler) {
            return promise.then(thenHandler, failHandler);
        }

        public Promise<T> then(Handler<T> handler) {
            return promise.then(handler);
        }

        public Promise<T> fail(Handler<T> handler) {
            return promise.fail(handler);
        }

        public Promise<T> any(Handler<T> handler) {
            return promise.any(handler);
        }

        private boolean verifyNotTriggered() {
            return !triggered.getAndSet(true);
        }
    }

    public interface Convert<From, To> {
        void convert(Defer<To> defer, From result) throws Exception;
    }

    public interface DeferBlock<T> {
        void trigger(Defer<T> defer, Triggered<T> p) throws Exception;
    }

    public interface ListDefer<T, U> {
        void trigger(Defer<U> defer, T value) throws Exception;
    }

    public static abstract class Handler<T> {
        public abstract void trigger(Triggered<T> p) throws Exception;
    }

    public interface CollectBlock<T> {
        Promise<T> promiseOfIndex(final int index);
    }

    public interface Sync {
        interface Done {
            void synced();
        }
        void sync(Done done) throws Exception;
    }


    public static class Triggered<T> {
        private final Promise<T> promise;
        private final T result;
        private final Exception exception;
        private Promise<T> nextPromise;
        protected Triggered(Promise<T> promise){
            this.promise = promise;
            this.result = promise.result;
            this.exception = promise.exception;
        }

        public boolean wasSuccessful() {
            return promise.wasSuccessful();
        }

        public T getResult() {
            return result;
        }

        public Exception getException() {
            return exception;
        }

        public void successfulWithResult(T result) {
            promise.forward(SUCCESS_HANDLER, result, promise.forwardException);
        }

        public void failureWithException(Exception exception) {
            promise.forward(FAILURE_HANDLER, promise.forwardResult, exception);
        }

        public void trigger(Defer<T> defer){
            if ( promise.wasSuccessful() ) {
                defer.resolveWithResult(result);
            }
            else {
                defer.rejectWithException(exception);
            }
        }

        public void continueAfterPromise(Promise<T> nextPromise) {
            this.nextPromise = nextPromise;
        }

        public Defer<T> continueAfterDefer() {
            Defer<T> defer = Promise.defer();
            continueAfterPromise(defer.promise);
            return defer;
        }

    }
}
