package com.threeplay.android.state;

/**
 * Created by eliranbe on 2/2/17.
 */

public interface StateStorage {
    StateStorage nameSpace(String ns);
    String keyStoreName(String key);

    // Key value write/read
    void putInt(String key, Integer value);
    void putFloat(String key, Float value);
    void putString(String key, String value);
    void putBoolean(String key, Boolean value);
    void commit();

    Float getFloat(String key, float defaultValue);
    Integer getInt(String key, int defaultValue);
    String getString(String key, String defaultValue);
    Boolean getBoolean(String key, boolean defaultValue);

    // Large objects
    void putBytes(String key, byte[] bytes);
    byte[] getBytes(String key);

    class StateStorageNameSpace implements StateStorage {
        private final String ns;
        private final StateStorage stateStorage;

        StateStorageNameSpace(String ns, StateStorage stateStorage){
            this.ns = ns;
            this.stateStorage = stateStorage;
        }

        @Override
        public String keyStoreName(String key) {
            return ns + "_" + key;
        }

        @Override
        public StateStorage nameSpace(String ns) {
            return new StateStorageNameSpace(ns, this);
        }

        @Override
        public void putInt(String key, Integer value) {
            stateStorage.putInt(keyStoreName(key), value);
        }

        @Override
        public void putFloat(String key, Float value) {
            stateStorage.putFloat(keyStoreName(key), value);
        }

        @Override
        public void putString(String key, String value) {
            stateStorage.putString(keyStoreName(key), value);
        }

        @Override
        public void putBoolean(String key, Boolean value) {
            stateStorage.putBoolean(keyStoreName(key), value);
        }

        @Override
        public void commit() {
            stateStorage.commit();
        }

        @Override
        public Float getFloat(String key, float defaultValue) {
            return stateStorage.getFloat(keyStoreName(key), defaultValue);
        }

        @Override
        public Integer getInt(String key, int defaultValue) {
            return stateStorage.getInt(keyStoreName(key), defaultValue);
        }

        @Override
        public String getString(String key, String defaultValue) {
            return stateStorage.getString(keyStoreName(key), defaultValue);
        }

        @Override
        public Boolean getBoolean(String key, boolean defaultValue) {
            return stateStorage.getBoolean(keyStoreName(key), defaultValue);
        }

        @Override
        public void putBytes(String key, byte[] bytes) {
            stateStorage.putBytes(keyStoreName(key), bytes);
        }

        @Override
        public byte[] getBytes(String key) {
            return stateStorage.getBytes(keyStoreName(key));
        }
    }
}
