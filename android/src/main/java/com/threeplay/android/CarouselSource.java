package com.threeplay.android;

import com.threeplay.core.Promise;

/**
 * Created by eliranbe on 11/29/16.
 */

public interface CarouselSource<T> {
    void setOffset(int offset);
    Promise<T[]> next(int count);
    Promise<T[]> prev(int count);
}
