package com.threeplay.android.events;

import com.threeplay.android.Callback;
import com.threeplay.core.Promise;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by eliranbe on 1/11/17.
 */

public interface EventSource<T> {
    interface Observer<T> {
        void onObservableEvent(EventSource<T> source, T event);
    }

    T getLastEvent();
    void notify(T event);
    void subscribe(Observer<T> observer);
    void subscribe(Observer<T> observer, boolean notifyLastEvent);
    void unsubscribe(Observer<T> observer);

    interface ToPromiseBlock<T, U> {
        void onEvent(T event, Callback.OnCompleteWithResult<U> callback);
    }


    interface Extended<T> extends EventSource<T> {
        void waitForNextEvent(final Callback.OnCompleteWithResult<T> callback);
        EventSource<T> filter(final Callback.OnFilter<T> filter);
        <U> Promise<U> toPromise(final ToPromiseBlock<T, U> callback);
    }

    class Base<T> implements EventSource.Extended<T>, Observer<T> {
        private final AtomicReference<LinkedList<Observer<T>>> observers = new AtomicReference<>(new LinkedList<Observer<T>>());
        private T lastEvent = null;

        // TODO: Verify if observers can be weakly referenced and so I can de allocate filters when no longer needed
        public EventSource.Extended<T> filter(final Callback.OnFilter<T> filter){
            final EventSource.Extended<T> eventSink = new EventSource.Base<>();
            if ( filter != null ) {
                subscribe(new Observer<T>() {
                    @Override
                    public void onObservableEvent(EventSource<T> source, T event) {
                        if (filter.elementWithValue(event)) {
                            eventSink.notify(event);
                        }
                    }
                });
            }
            return eventSink;
        }

        @Override
        public void waitForNextEvent(final Callback.OnCompleteWithResult<T> callback){
            subscribe(new Observer<T>() {
                @Override
                public void onObservableEvent(EventSource<T> source, T event) {
                    source.unsubscribe(this);
                    if ( callback != null ) { callback.completedWithResult(event); }
                }
            }, false);
        }

        @Override
        public <U> Promise<U> toPromise(final ToPromiseBlock<T, U> callback) {
            final Promise.Defer<U> defer = Promise.defer();
            subscribe(new Observer<T>() {
                @Override
                public void onObservableEvent(EventSource<T> source, T event) {
                    final Observer<T> thisObserver = this;
                    callback.onEvent(event, new Callback.OnCompleteWithResult<U>() {
                        @Override
                        public void completedWithResult(U result) {
                            unsubscribe(thisObserver);
                            defer.resolveWithResult(result);
                        }
                    });
                }
            });
            return defer.promise;
        }

        @Override
        public synchronized void subscribe(Observer<T> observer) {
            subscribe(observer, true);
        }

        @Override
        public void subscribe(Observer<T> observer, boolean notifyLastEvent) {
            LinkedList<Observer<T>> newObserversList = duplicateObservers();
            newObserversList.add(observer);
            if ( notifyLastEvent && lastEvent != null ) { observer.onObservableEvent(this, lastEvent); }
            observers.set(newObserversList);
        }

        @Override
        public synchronized void unsubscribe(Observer<T> observer) {
            LinkedList<Observer<T>> newObserversList = duplicateObservers();
            newObserversList.remove(observer);
            observers.set(newObserversList);
        }

        private LinkedList<Observer<T>> duplicateObservers(){
            return new LinkedList<>(observers.get());
        }

        @Override
        public synchronized T getLastEvent() {
            return lastEvent;
        }

        @Override
        public void notify(T event) {
            synchronized (this) { lastEvent = event; }
            for (Observer<T> observer: observers.get()) {
                observer.onObservableEvent(this, event);
            }
        }

        @Override
        public void onObservableEvent(EventSource<T> source, T event) {
            notify(event);
        }
    }
}
