package com.threeplay.android.cache;

import android.util.Log;

import com.threeplay.core.QUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by eliranbe on 2/14/17.
 */

public class MemoryCacheStore implements CacheStore {
    private static final String TAG = "MemoryCache";
    private final int maximumCacheMemory;
    private final int maximumTimeInCache;

    static class CacheEntry {
        private final String key;
        private byte[] content;
        private long   lastUsed;

        CacheEntry(String key, byte[] content){
            this.key = key;
            this.content = content;
            this.lastUsed = System.currentTimeMillis();
        }

        String getKey(){
            return key;
        }

        byte[] get(){
            this.lastUsed = System.currentTimeMillis();
            return content;
        }

        public int contentLength() {
            return content.length;
        }
    }

    private Map<String, CacheEntry> storage = new HashMap<>();
    private LinkedList<CacheEntry> entryList = new LinkedList<>();
    private int totalMemory = 0;
    private long oldestEntry = Long.MAX_VALUE;

    public MemoryCacheStore(int maximumCacheMemory, int maximumTimeInCache){
        this.maximumCacheMemory = maximumCacheMemory;
        this.maximumTimeInCache = maximumTimeInCache;
    }

    @Override
    public synchronized InputStream get(String key) {
        CacheEntry entry = storage.get(key);
        if ( entry != null ) {
            byte[] content = entry.get();
            updateRecentlyUsed(entry);
            trimCacheByAge();
            return toStream(content);
        }
        return null;
    }

    private CacheEntry updateRecentlyUsed(CacheEntry entry){
        if ( entry != null ) {
            Iterator<CacheEntry> iterator = entryList.iterator();
            while (iterator.hasNext()) {
                CacheEntry cur = iterator.next();
                if (cur == entry) {
                    iterator.remove();
                    break;
                }
            }
            entryList.addFirst(entry);
        }
        return entry;
    }

    @Override
    public synchronized InputStream put(String key, InputStream is){
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            QUtils.copyStreamTo(is, os);
            byte[] bytes = os.toByteArray();
            newEntry(key, bytes);
            is.close();
            os.close();
            return toStream(bytes);
        } catch ( IOException ok ) {
        }
        return null;
    }

    private InputStream toStream(byte[] bytes){
        return bytes != null ? new ByteArrayInputStream(bytes) : null;
    }

    private CacheEntry newEntry(String key, byte[] bytes){
        if ( maximumCacheMemory > 0 && totalMemory + bytes.length >= maximumCacheMemory ) {
            trimCacheMemory(maximumCacheMemory - bytes.length);
        }
        CacheEntry entry = new CacheEntry(key, bytes);
        storage.put(key, entry);
        totalMemory += bytes.length;
        if ( oldestEntry > entry.lastUsed ) {
            oldestEntry = entry.lastUsed;
        }
        return updateRecentlyUsed(entry);
    }

    private synchronized void trimCacheMemory(int maxMemory) {
        Iterator<CacheEntry> iterator = entryList.iterator();
        int usedMemory = 0;
        while ( iterator.hasNext() ) {
            CacheEntry entry = iterator.next();
            if ( entry.contentLength() + usedMemory > maxMemory ) {
                removeEntry(entry);
                iterator.remove();
            }
            else {
                usedMemory += entry.contentLength();
            }
        }
        totalMemory = usedMemory;
    }

    private void trimCacheByAge(){
        long current = System.currentTimeMillis();
        long minTimeAllowed = current - maximumTimeInCache;
        if ( oldestEntry < minTimeAllowed ) {
            Iterator<CacheEntry> iterator = entryList.descendingIterator();
            while ( iterator.hasNext() ) {
                CacheEntry entry = iterator.next();
                if ( entry.lastUsed < minTimeAllowed ) {
                    removeEntry(entry);
                    iterator.remove();
                }
                else {
                    oldestEntry = entry.lastUsed;
                    break;
                }
            }
        }
    }

    private void removeEntry(CacheEntry entry){
        storage.remove(entry.key);
        totalMemory -= entry.contentLength();
    }
}
