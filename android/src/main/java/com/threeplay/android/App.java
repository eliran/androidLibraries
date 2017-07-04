package com.threeplay.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.util.AttributeSet;

import com.threeplay.android.services.AppService;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by eliranbe on 12/7/16.
 */

public class App extends Application {
    private static final String TAG = "TPApp";

    private List<AppService> services = new LinkedList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        for (AppService service : services) service.onAppCreate(this);
    }

    public void addAppService(AppService service) {
        services.add(service);
    }

    public static AlertDialog messageAlertDialog(Activity activity, String message) {
        return messageAlertDialog(activity, null, message, "OK");
    }

    public static AlertDialog messageAlertDialog(Activity activity, String title, String message, String cancelTitle) {
        return messageAlertDialog(activity, title, message, cancelTitle, null);
    }

    public static AlertDialog messageAlertDialog(Activity activity, String title, String message, String cancelTitle, final Callback.OnCompleteWithResult<Boolean> onComplete) {
        return new AlertDialogHolder(activity, title, message, cancelTitle, onComplete).show();
    }

    private static class AlertDialogHolder {
        private final Callback.OnCompleteWithResult<Boolean> onComplete;
        private AlertDialog dialog;
        private boolean notified = false;

        AlertDialogHolder(Activity activity, String title, String message, String cancelTitle, final Callback.OnCompleteWithResult<Boolean> onComplete){
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            this.onComplete = onComplete;
            if (title != null) {
                builder.setTitle(title);
            }
            if (message != null) {
                builder.setMessage(message);
            }
            if (cancelTitle != null) {
                builder.setPositiveButton(cancelTitle, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        notifySelection(true);
                        dialogInterface.cancel();
                    }
                });
            } else {
                builder.setCancelable(false);
            }
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    notifySelection(false);
                }
            });
            dialog = builder.create();
        }

        AlertDialog show(){
            dialog.show();
            return dialog;
        }

        private void notifySelection(boolean result){
            if ( !notified && onComplete != null ) {
                notified = true;
                onComplete.completedWithResult(result);
            }
        }
    }
}