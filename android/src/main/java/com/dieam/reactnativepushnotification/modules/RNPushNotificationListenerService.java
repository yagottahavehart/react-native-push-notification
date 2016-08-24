package com.dieam.reactnativepushnotification.modules;

import android.content.Intent;
import android.os.Bundle;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;

import java.util.List;
import java.util.Map;

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
        Bundle bundle = new Bundle();
        try {
          Map<String, String> message = remoteMessage.getData();
          // Check if message contains a data payload.
          if (message.size() > 0) {
              System.out.println("GRAB Message: " + message);
              /*{
                payload={
                  "metas":[{"type":"campaignLaunched"}],
                  "timeline_uuid":"e9f2f2d3-8f71-4824-8ca4-b9a2e4a5812e",
                  "account_id":"18ce53vw9tp",
                  "entity_type":"campaign",
                  "lineitem_tweet_id":null,
                  "lineitem_id":null,
                  "funding_instrument_id":null,"campaign_id":"5t1bd"
                },
                body=Campaign norif 7 has launched.,
                icon=ic_notification,
                title=Flightly
              }*/
              bundle.putString("title", (String)message.get("title"));
              bundle.putString("message", (String)message.get("body"));
              bundle.putString("data", (String)message.get("payload"));
          }
        } catch (Exception e) {
          System.out.println("GRAB "+e.toString());
        }


        sendNotification(bundle);
    }

    private JSONObject getPushData(String dataString) {
        try {
            return new JSONObject(dataString);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendNotification(Bundle bundle) {
System.out.println("GRAB sendNotification"+bundle.toString());
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
