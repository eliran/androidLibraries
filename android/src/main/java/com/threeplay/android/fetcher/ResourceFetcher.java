package com.threeplay.android.fetcher;

import com.threeplay.core.Promise;

/**
 * Created by eliranbe on 11/30/16.
 */

public interface ResourceFetcher<T> {
    Promise<T> fetch(String key);
}
