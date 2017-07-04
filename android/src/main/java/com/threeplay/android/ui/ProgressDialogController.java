package com.threeplay.android.ui;

import android.app.Activity;
import android.app.ProgressDialog;

import com.threeplay.android.events.EventSource;
import com.threeplay.android.events.ProgressEventSource;

/**
 * Created by eliranbe on 1/10/17.
 */

public class ProgressDialogController implements ProgressEventSource.Observer {
    private final Activity activity;
    private ProgressEventSource eventSource;
    private ProgressDialog progress;

    public ProgressDialogController(Activity activity){
        this.activity = activity;
    }

    public void start(int style, String title, ProgressEventSource eventSource){
        stop();
        this.eventSource = eventSource;
        progress = new ProgressDialog(this.activity);
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.setProgressStyle(style);
        progress.setProgress(0);
        progress.setTitle(title);
        progress.show();
        eventSource.subscribe(this);
    }

    public void stop(){
        if ( progress != null ) {
            eventSource.unsubscribe(this);
            progress.dismiss();
            progress = null;
            eventSource = null;
        }
    }

    @Override
    public void onObservableEvent(EventSource<ProgressEventSource.Event> source, final ProgressEventSource.Event event) {
        if ( progress != null ) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if ( progress != null ) {
                        String message = event.getMessage();
                        if ( message != null ) {
                            progress.setMessage(message);
                        }
                        switch ( event.getKind() ) {
                            case MESSAGE:
                                break;
                            case PROGRESS:
                                if ( event.getTotal() != -1 ) {
                                    progress.setIndeterminate(false);
                                    progress.setMax(event.getTotal());
                                    progress.setProgress(event.getCompleted());
                                }
                                else {
                                    progress.setIndeterminate(true);
                                }
                                break;
                            case COMPLETED:
                                stop();
                                break;
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void finalize() throws Throwable {
        stop();
        super.finalize();
    }
}
