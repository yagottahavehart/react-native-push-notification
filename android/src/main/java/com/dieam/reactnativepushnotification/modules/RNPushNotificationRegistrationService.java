package com.dieam.reactnativepushnotification.modules;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import android.content.Intent;

public class RNPushNotificationRegistrationService extends FirebaseInstanceIdService {
    public static String s_token = "";
    public static RNPushNotificationRegistrationService s_instance;
    //private static final String TAG = "RNPushNotification";

    public RNPushNotificationRegistrationService() {
      super();
      s_instance = this;
    }

    @Override
    public void onTokenRefresh() {
        String refreshedToken = s_token = FirebaseInstanceId.getInstance().getToken();
        System.out.println("GRAB onTokenRefresh "+refreshedToken);
        sendRegistrationToken(refreshedToken);
    }

    public void sendRegistrationToken(String token) {
        Intent intent = new Intent("RNPushNotificationRegisteredToken");
        intent.putExtra("token", token);
        //System.out.println("GRAB sendRegistrationToken "+intent.toString());
        sendBroadcast(intent);
    }

}