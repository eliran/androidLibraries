package com.threeplay.android.state;

import com.threeplay.android.Callback;

/**
 * Created by eliranbe on 2/2/17.
 */

public interface PersistableState {
    void saveState(StateStorage storage);
    void restoreState(StateStorage storage, Callback.OnComplete onComplete);
}
