package com.dieam.reactnativepushnotification.modules;

import android.content.Intent;
import android.os.Bundle;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;

import java.util.List;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

public class RNPushNotificationListenerService extends FirebaseMessagingService {
    private static final String TAG = "RNPushNotification";

    public RNPushNotificationListenerService() {
      super();
      System.out.println("GRAB Starting FirebaseMessagingService");
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
      System.out.println("GRAB FirebaseMessagingService.onMessageReceived");
      //System.out.println("GRAB Service.onMessageReceived "+remoteMessage.toString());
        // JSONObject data = getPushData(bundle.getString("data"));
        // if (data != null) {
        //     if (!bundle.containsKey("message")) {
        //         bundle.putString("message", data.optString("alert", "Notification received"));
        //     }
        //     if (!bundle.containsKey("title")) {
        //         bundle.putString("title", data.optString("title", null));
        //     }
        // }
        // System.out.println("GRAB From: " + remoteMessage.getFrom());

        // // Check if message contains a data payload.
        // if (remoteMessage.getData().size() > 0) {
        //     System.out.println("GRAB Message data payload: " + remoteMessage.getData());
        // }

        // // Check if message contains a notification payload.
        // if (remoteMessage.getNotification() != null) {
        //     System.out.println("GRAB Message Notification Body: " + remoteMessage.getNotification().getBody());
        // }
        // Bundle bundle = new Bundle();
        // sendNotification(bundle);
    }

    private JSONObject getPushData(String dataString) {
        try {
            return new JSONObject(dataString);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendNotification(Bundle bundle) {

        Boolean isRunning = isApplicationRunning();

        Intent intent = new Intent("RNPushNotificationReceiveNotification");
        bundle.putBoolean("foreground", isRunning);
        intent.putExtra("notification", bundle);
        sendBroadcast(intent);

        if (!isRunning) {
            new RNPushNotificationHelper(getApplication(), this).sendNotification(bundle);
        }
    }

    private boolean isApplicationRunning() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
            if (processInfo.processName.equals(getApplication().getPackageName())) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String d: processInfo.pkgList) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
