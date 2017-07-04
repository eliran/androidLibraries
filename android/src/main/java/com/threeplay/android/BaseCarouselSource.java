package com.threeplay.android;

import com.threeplay.core.Promise;

/**
 * Created by eliranbe on 12/1/16.
 */

public abstract class BaseCarouselSource<T> implements CarouselSource<T> {
    private int offset;
    public final int length;

    public BaseCarouselSource(int length){
        this.length = length;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
        adjustOffset(0);
    }

    @Override
    public Promise<T[]> next(int count) {
        if ( length > 0 ) {
            Promise<T[]> result = retrieve(offset, count);
            offset = adjustOffset(count);
            return result;
        }
        return Promise.withException(new Exception("Empty source"));
    }

    @Override
    public Promise<T[]> prev(int count) {
        if ( length > 0 ) {
            offset = adjustOffset(-count);
            Promise<T[]> result = retrieve(offset, count);
            return result;
        }
        return Promise.withException(new Exception("Empty source"));
    }

    private int adjustOffset(int delta){
        if ( length > 0 ) {
            int newOffset = offset + delta;
            if (newOffset < 0) {
                newOffset = length - ((-newOffset - 1) % length) - 1;
            }
            return newOffset % length;
        }
        return 0;
    }

    protected abstract Promise<T[]> retrieve(int offset, int count);

}
