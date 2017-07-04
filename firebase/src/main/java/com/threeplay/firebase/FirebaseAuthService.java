package com.threeplay.firebase;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.threeplay.android.App;
import com.threeplay.android.events.ProgressEventSource;
import com.threeplay.android.services.AppService;
import com.threeplay.android.services.AuthService;

/**
 * Created by eliranbe on 1/26/17.
 */

public class FirebaseAuthService implements AppService, AuthService {
    private FirebaseAuth auth;

    @Override
    public void onAppCreate(App app) {
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public ProgressEventSource signInAnonymously() {
        final ProgressEventSource eventSource = new ProgressEventSource.Base();
        eventSource.notify(ProgressEventSource.Events.message("Logging in"));
        auth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                eventSource.notify(ProgressEventSource.Events.complete(task.isSuccessful()));
            }
        });
        return eventSource;
    }

    @Override
    public ProgressEventSource signIn(String email, String password) {
        final ProgressEventSource eventSource = new ProgressEventSource.Base();
        eventSource.notify(ProgressEventSource.Events.message("Logging in"));
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                eventSource.notify(ProgressEventSource.Events.complete(task.isSuccessful()));
            }
        });
        return eventSource;
    }

    @Override
    public boolean isSignedIn() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null && !user.isAnonymous();
    }
}
