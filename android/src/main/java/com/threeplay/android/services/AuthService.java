package com.threeplay.android.services;

import com.threeplay.android.events.ProgressEventSource;

/**
 * Created by eliranbe on 1/26/17.
 */

public interface AuthService {
    boolean isSignedIn();

    ProgressEventSource signInAnonymously();
    ProgressEventSource signIn(String email, String password);
}
