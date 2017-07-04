package com.threeplay.firebase;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StreamDownloadTask;
import com.threeplay.android.fetcher.ResourceFetcher;
import com.threeplay.core.Promise;

import java.io.InputStream;

/**
 * Created by eliranbe on 12/1/16.
 */
public class FirebaseResourceFetcher implements ResourceFetcher<InputStream> {
    private final static String TAG = "FB:Fetcher";
    private StorageReference storageRef;

    public FirebaseResourceFetcher(StorageReference storageRef){
        this.storageRef = storageRef;
    }

    @Override
    public Promise<InputStream> fetch(final String key) {
        final Promise.Defer<InputStream> defer = Promise.defer();
        final StorageReference storage = key != null ? storageRef.child(key) : storageRef;
        try {
            storage.getStream().addOnSuccessListener(new OnSuccessListener<StreamDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(StreamDownloadTask.TaskSnapshot taskSnapshot) {
                    InputStream is = taskSnapshot.getStream();
                    Log.d(TAG, storage.toString() + " => " + taskSnapshot.getTotalByteCount());
                    defer.resolveWithResult(is);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "failed to download: " + storage.toString());
                    defer.rejectWithException(e);
                }
            });
        } catch ( Exception e ) {
            defer.rejectWithException(e);
        }
        return defer.promise;
    }
}
