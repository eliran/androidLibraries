package com.threeplay.android.services;

/**
 * Created by eliranbe on 12/7/16.
 */

public interface ConfigService {
    String getString(String key);
    Long getLong(String key);
    Integer getInt(String key);

    void subscribe(Listener listener);
    void unsubscribe(Listener listener);

    interface Listener {
        void onConfigChanged(ConfigService service);
    }
}
