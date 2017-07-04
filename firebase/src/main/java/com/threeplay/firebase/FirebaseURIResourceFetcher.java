package com.threeplay.firebase;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.StorageReference;
import com.threeplay.android.fetcher.ResourceFetcher;
import com.threeplay.core.Promise;

/**
 * Created by eliranbe on 12/1/16.
 */
public class FirebaseURIResourceFetcher implements ResourceFetcher<Uri> {
    private final static String TAG = "FB:URIFetcher";
    private StorageReference storageRef;

    public FirebaseURIResourceFetcher(StorageReference storageRef){
        this.storageRef = storageRef;
    }

    @Override
    public Promise<Uri> fetch(final String key) {
        final Promise.Defer<Uri> defer = Promise.defer();
        final StorageReference storage = key != null ? storageRef.child(key) : storageRef;
        storage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Log.d(TAG, key + " uri => " + uri);
                defer.resolveWithResult(uri);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "failed to download: " + storage.toString());
                defer.rejectWithException(e);
            }
        });
        return defer.promise;
    }
}
