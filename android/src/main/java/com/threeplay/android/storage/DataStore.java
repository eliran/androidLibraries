package com.threeplay.android.storage;

import java.io.InputStream;

/**
 * Created by eliranbe on 6/28/17.
 */

public interface DataStore {
    boolean contains(String key);
    void store(String key, byte[] data);
    void store(String key, InputStream dataStream);
    byte[] get(String key);
    InputStream getStream(String key);
    void remove(String key);
}
