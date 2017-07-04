package com.threeplay.android.cache;

import android.renderscript.ScriptGroup;

import com.threeplay.android.Hash;
import com.threeplay.core.QUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by eliranbe on 2/27/17.
 */
public class StoredCacheStore implements CacheStore {
    private final CacheStore fastCacheStore;
    private final File cacheDir;

    public StoredCacheStore(File cacheDir){
        this(cacheDir, null);
    }

    public StoredCacheStore(File cacheDir, CacheStore fastCacheStore){
        this.fastCacheStore = fastCacheStore != null ? fastCacheStore : NullCacheStore.instance;
        this.cacheDir = cacheDir;
        cacheDir.mkdirs();
    }

    @Override
    public InputStream get(String key) {
        key = prepareKey(key);
        InputStream inputStream = fastCacheStore.get(key);
        if ( inputStream != null ) {
            return inputStream;
        }
        try {
            FileInputStream fs = new FileInputStream(new File(cacheDir, key));
            InputStream is = fastCacheStore.put(key, fs);
            if (fs != is) {
                fs.close();
            }
            return is;
        } catch ( IOException ok ) {
            return null;
        }
    }

    @Override
    public InputStream put(String key, InputStream is) {
        String internalKey = prepareKey(key);
        is = fastCacheStore.put(internalKey, is);
        try {
            FileOutputStream fs = new FileOutputStream(new File(cacheDir, internalKey));
            QUtils.copyStreamTo(is, fs);
            fs.close();
        } catch ( IOException ok ) {
        }
        return get(key);
    }

    private String prepareKey(String key){
        return Hash.sha1(key);
    }
}
