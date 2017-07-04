package com.threeplay.android.cache;

import android.util.Log;

import com.threeplay.android.cache.MemoryCacheStore;
import com.threeplay.android.fetcher.ResourceFetcher;
import com.threeplay.core.Promise;

import java.io.InputStream;

/**
 * Created by eliranbe on 2/14/17.
 */

public class ResourceFetchCache implements ResourceFetcher<InputStream> {
    private static final String TAG = "CacheFetcher";
    private final ResourceFetcher<InputStream> fetcher;
    private final String cacheKey;
    private final CacheStore cacheStore;

    public ResourceFetchCache(CacheStore cacheStore, String cacheKey, ResourceFetcher<InputStream> fetcher){
        this.fetcher = fetcher;
        this.cacheStore = cacheStore;
        this.cacheKey = cacheKey;
    }

    @Override
    public Promise<InputStream> fetch(String key) {
        String uniqueKey = createUniqueKey(key);
        Promise<InputStream> promise = fetchFromCache(uniqueKey);
        if ( promise != null ) {
            Log.d(TAG, "Loaded from cache: " + key);
        }
        return promise == null ? fetcher.fetch(key).then(storeInCache(uniqueKey)) : promise;
    }

    private String createUniqueKey(String key) {
        return key == null ? cacheKey : cacheKey + '$' + key;
    }

    private Promise<InputStream> fetchFromCache(String uniqueKey){
        InputStream is = cacheStore.get(uniqueKey);
        return is != null ? Promise.withResult(is) : null;
    }

    private Promise.Convert<InputStream, InputStream> storeInCache(final String uniqueKey){
        return new Promise.Convert<InputStream, InputStream>() {
            @Override
            public void convert(Promise.Defer<InputStream> defer, InputStream result) throws Exception {
                defer.resolveWithResult(cacheStore.put(uniqueKey, result));
            }
        };
    }
}
