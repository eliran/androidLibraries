package com.threeplay.android;

import android.app.Dialog;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by eliranbe on 2/9/17.
 */

public class Listeners {

    public interface OnDialogSelectListener {
        void onSelect(Dialog dialog, int optionIndex);
    }

    public interface OnChange<T> {
        void onChange(T source);
    }

    public static class Holder<T> {
        private List<T> listeners = new LinkedList<>();

        public void addListener(T listener){
            this.listeners.add(listener);
        }

        public void removeListener(T listener){
            this.listeners.remove(listener);
        }

        public void removeAllListeners(){
            this.listeners = new LinkedList<>();
        }

        public Iterable<T> getListeners(){
            return listeners;
        }
    }
}
