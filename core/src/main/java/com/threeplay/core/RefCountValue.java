package com.threeplay.core;

/**
 * Created by eliranbe on 1/31/17.
 */

public class RefCountValue<T> {
    private T value;
    private int retainCount = 0;


    public synchronized boolean set(T value) {
        if ( !hasValue() ) {
            this.value = value;
            retainCount = 1;
            return true;
        }
        return false;
    }

    private boolean hasValue() {
        return retainCount != 0;
    }

    public synchronized T release() {
        if ( retainCount > 0 ) {
            retainCount -= 1;
            return retainCount == 0 ? value : null;
        }
        return null;
    }

    public synchronized T retain() {
        if ( hasValue() ) {
            retainCount += 1;
            return value;
        }
        return null;
    }

    public synchronized T get() {
        return hasValue() ? value : null;
    }

    public synchronized int retainCount() {
        return retainCount;
    }
}
