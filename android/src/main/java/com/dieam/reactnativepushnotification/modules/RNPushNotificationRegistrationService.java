package com.dieam.reactnativepushnotification.modules;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import android.content.Intent;

public class RNPushNotificationRegistrationService extends FirebaseInstanceIdService {

    //private static final String TAG = "RNPushNotification";

    //public RNPushNotificationRegistrationService() {super(TAG);}

    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        System.out.println("GRAB onTokenRefresh "+refreshedToken);
        sendRegistrationToken(refreshedToken);
    }

    private void sendRegistrationToken(String token) {
        Intent intent = new Intent("RNPushNotificationRegisteredToken");
        intent.putExtra("token", token);
        System.out.println("GRAB sendRegistrationToken "+intent.toString());
        sendBroadcast(intent);
    }

}