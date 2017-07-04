package com.threeplay.android.storage;

import com.threeplay.core.QUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by eliranbe on 6/28/17.
 */

public class MemoryDataStore implements DataStore {
    private final Map<String, byte[]> memoryStore = new HashMap<>();

    @Override
    public void store(String key, byte[] data) {
        memoryStore.put(key, data);
    }

    @Override
    public void store(String key, InputStream dataStream) {
        memoryStore.put(key, QUtils.bytesFromStream(dataStream));
    }

    @Override
    public byte[] get(String key) {
        return memoryStore.get(key);
    }

    @Override
    public InputStream getStream(String key) {
        return new ByteArrayInputStream(get(key));
    }

    @Override
    public void remove(String key) {
        memoryStore.remove(key);
    }

    @Override
    public boolean contains(String key) {
        return memoryStore.containsKey(key);
    }
}
