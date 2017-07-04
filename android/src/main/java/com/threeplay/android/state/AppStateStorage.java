package com.threeplay.android.state;

import android.content.SharedPreferences;
import android.util.Log;

import com.threeplay.android.Hash;
import com.threeplay.core.QUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by eliranbe on 2/2/17.
 */

public class AppStateStorage implements StateStorage {
    private final SharedPreferences sharedPreferences;
    private SharedPreferences.Editor preferencesEditor;
    private File filesDir;

    public AppStateStorage(SharedPreferences sharedPreferences, File filesDir){
        this.sharedPreferences = sharedPreferences;
        this.filesDir = filesDir;
    }

    @Override
    public StateStorage nameSpace(String ns) {
        return new StateStorageNameSpace(ns, this);
    }

    @Override
    public String keyStoreName(String key) {
        return key;
    }

    @Override
    public void putInt(String key, Integer value) {
        edit().putInt(key, value);
    }

    @Override
    public void putFloat(String key, Float value) {
        edit().putFloat(key, value);
    }

    @Override
    public void putString(String key, String value) {
        edit().putString(key, value);
    }

    @Override
    public void putBoolean(String key, Boolean value) {
        edit().putBoolean(key, value);
    }

    @Override
    public Float getFloat(String key, float defaultValue) {
        return sharedPreferences.getFloat(key, defaultValue);
    }

    @Override
    public Integer getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    @Override
    public String getString(String key, String defaultString) {
        return sharedPreferences.getString(key, defaultString);
    }

    @Override
    public Boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    @Override
    public void putBytes(String key, byte[] bytes) {
        try {
            FileOutputStream os = new FileOutputStream(fileForKey(key));
            os.write(bytes);
            os.flush();
            os.close();
        } catch ( IOException ok ) {
            ok.printStackTrace();
        }
    }

    @Override
    public byte[] getBytes(String key) {
        try {
            FileInputStream is = new FileInputStream(fileForKey(key));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            QUtils.copyStreamTo(is, os);
            is.close();
            byte[] content = os.toByteArray();
            os.close();
            return content;
        } catch ( IOException ok ) {
            ok.printStackTrace();
        }
        return null;
    }

    private File fileForKey(String key){
        Log.d("FileStore", "Storing key: " + key);
        return new File(filesDir, "state_" + Hash.sha1(key));
    }

    private SharedPreferences.Editor edit(){
        if ( preferencesEditor == null ) {
            preferencesEditor = sharedPreferences.edit();
        }
        return preferencesEditor;
    }

    @Override
    public void commit() {
        if ( preferencesEditor != null ) {
            preferencesEditor.commit();
            preferencesEditor = null;
        }
    }
}
