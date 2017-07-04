package com.threeplay.firebase;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.threeplay.android.App;
import com.threeplay.android.services.AppService;
import com.threeplay.android.services.ConfigService;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by eliranbe on 12/7/16.
 */
public class FirebaseRemoteConfigService implements AppService, ConfigService {
    private final static String TAG = "FBConfigService";
    private final boolean developerMode;

    private FirebaseRemoteConfig remoteConfig;
    private List<Listener> listeners = new LinkedList<>();
    private int defaultsResourceId = 0;

    public FirebaseRemoteConfigService() {
        this(0, com.threeplay.android.BuildConfig.DEBUG);
    }

    public FirebaseRemoteConfigService(int defaultsResourceId, boolean developerMode){
        this.defaultsResourceId = defaultsResourceId;
        this.developerMode = developerMode;
    }

    @Override
    public void onAppCreate(App app) {
        fetchRemoteConfiguration();
    }

    public synchronized void fetchRemoteConfiguration(){
        if ( remoteConfig == null ){
            Log.d(TAG, "Fetching Remote Config");
            remoteConfig = FirebaseRemoteConfig.getInstance();
            remoteConfig.setConfigSettings(createRemoteConfigSettings());
            if (defaultsResourceId != 0) {
                remoteConfig.setDefaults(defaultsResourceId);
            }

            long cacheExpiration = remoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled() ? 0 : 3600;
            remoteConfig.fetch(cacheExpiration)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "Failed", e);
                        }
                    })
                    .addOnCompleteListener(new OnCompleteListener<Void>() {

                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(TAG, "Config fetch completed: " + task.isSuccessful());
                            if (task.isSuccessful()) {
                                remoteConfig.activateFetched();
                                notifyConfigChanged();
                            }
                        }
                    });
        }
    }

    private FirebaseRemoteConfigSettings createRemoteConfigSettings(){
        return new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(developerMode)
                .build();
    }

    private void notifyConfigChanged() {
        for (ConfigService.Listener listener : listeners) {
            listener.onConfigChanged(this);
        }
    }

    @Override
    public void subscribe(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public String getString(String key) {
        return remoteConfig != null ? remoteConfig.getString(key) : null;
    }

    @Override
    public Long getLong(String key) {
        return remoteConfig != null ? remoteConfig.getLong(key) : null;
    }

    @Override
    public Integer getInt(String key) {
        return remoteConfig != null ? (int)remoteConfig.getLong(key) : null;
    }


}
