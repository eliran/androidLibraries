package com.threeplay.android.storage;

import com.threeplay.core.Logger;
import com.threeplay.core.QUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by eliranbe on 7/2/17.
 */

public class LocalDataStore implements DataStore {
    private final File rootDir;

    public LocalDataStore(File rootDir){
        this.rootDir = rootDir;
    }

    @Override
    public boolean contains(String key) {
        File f = fileForKey(key);
        return f.exists() && !f.isDirectory();
    }

    @Override
    public void store(String key, byte[] data) {
        store(key, new ByteArrayInputStream(data));
    }

    @Override
    public void store(String key, InputStream dataStream) {
        File f = fileForKey(key);
        try {
            prepareFile(f);
            Logger.d("Storing key '%s': exists: %b dir: %b", f.getAbsoluteFile(), f.exists(), f.isDirectory());
            OutputStream os = new FileOutputStream(f);
            QUtils.copyStreamTo(dataStream, os);
            os.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    private void prepareFile(File f){
        if (f.exists()) {
            if ( f.isDirectory() ) {
                f.delete();
            }
        } else {
            f.getParentFile().mkdirs();
        }
    }

    @Override
    public byte[] get(String key) {
        return QUtils.bytesFromStream(getStream(key));
    }

    @Override
    public InputStream getStream(String key) {
        try {
            File f = fileForKey(key);
            return f.exists() && !f.isDirectory() ? new FileInputStream(f) : null;
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void remove(String key) {
        if ( !fileForKey(key).delete() ) {
            Logger.d("Failed removing file '%s'", key);
        }
    }

    private File fileForKey(String key){
        return new File(rootDir, key);
    }
}
