package com.threeplay.android.events;

import com.threeplay.core.Promise;

/**
 * Created by eliranbe on 1/11/17.
 */

public interface ProgressEventSource extends EventSource<ProgressEventSource.Event> {

    enum EventKind { MESSAGE, PROGRESS, COMPLETED }

    interface Event {
        EventKind getKind();
        int getTotal();
        int getCompleted();
        int getFailed();
        int getPending();
        String getMessage();
    }

    interface Observer extends EventSource.Observer<Event> {
    }

    boolean isCompleted();
    Promise<Boolean> onCompletePromise();

    class Events {
        public static Event message(String message){
            return new BaseEvent(EventKind.MESSAGE,0,0,0,0,message);
        }

        public static Event complete(boolean successful){
            return new BaseEvent(EventKind.COMPLETED,100,100,successful ? 0 : 100, 0,null);
        }

        public static Event progress(int total, int completed, int failed, int pending) {
            if ( total == completed ) {
                return new BaseEvent(EventKind.COMPLETED, total, completed, failed, pending, null);
            }
            return new BaseEvent(EventKind.PROGRESS, total, completed, failed, pending, null);
        }
    }

    class Base extends EventSource.Base<Event> implements ProgressEventSource, EventSource.Observer<Event> {
        public boolean isCompleted(){
            Event event = getLastEvent();
            return event != null && event.getCompleted() == event.getTotal();
        }

        @Override
        public Promise<Boolean> onCompletePromise() {
            final Promise.Defer<Boolean> defer = Promise.defer();
            EventSource.Observer<Event> observer = new EventSource.Observer<Event>() {
                @Override
                public void onObservableEvent(EventSource<Event> source, Event event) {
                    if ( event.getKind() == EventKind.COMPLETED ) {
                        defer.resolveWithResult(event.getFailed() == 0);
                    }
                }
            };
            subscribe(observer);
            return defer.promise;
        }
    }

    class BaseEvent implements Event {
        private final int total, completed, failed, pending;
        private final String message;
        private final EventKind kind;

        public BaseEvent(int total, int completed, int failed, int pending) {
            this(EventKind.PROGRESS, total, completed, failed, pending, null);
        }

        public BaseEvent(EventKind kind, int total, int completed, int failed, int pending, String message){
            this.kind = kind;
            this.total = total;
            this.completed = completed;
            this.failed = failed;
            this.pending = pending;
            this.message = message;
        }

        @Override
        public EventKind getKind() {
            return kind;
        }

        @Override
        public int getTotal() {
            return total;
        }

        @Override
        public int getCompleted() {
            return completed;
        }

        @Override
        public int getFailed() {
            return failed;
        }

        @Override
        public int getPending() {
            return pending;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
