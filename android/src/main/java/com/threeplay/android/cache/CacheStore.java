package com.threeplay.android.cache;

import java.io.InputStream;

/**
 * Created by eliranbe on 2/14/17.
 */
public interface CacheStore {
    InputStream get(String key);

    InputStream put(String key, InputStream is);

    class NullCacheStore implements CacheStore {
        public final static CacheStore instance = new NullCacheStore();
        @Override
        public InputStream get(String key) {
            return null;
        }

        @Override
        public InputStream put(String key, InputStream is) {
            return is;
        }
    }
}
