package com.dieam.reactnativepushnotification.modules;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.json.*;

import android.content.Context;
import com.localytics.android.Localytics;

public class RNPushNotification extends ReactContextBaseJavaModule {
    private ReactContext mReactContext;
    private RNPushNotificationHelper mRNPushNotificationHelper;

    public RNPushNotification(ReactApplicationContext reactContext) {
        super(reactContext);

        mReactContext = reactContext;
        mRNPushNotificationHelper = new RNPushNotificationHelper((Application) reactContext.getApplicationContext());
        registerNotificationsRegistration();
        registerNotificationsReceiveNotification();
    }

    @Override
    public String getName() {
        return "RNPushNotification";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();

        Activity activity = getCurrentActivity();
        if (activity != null) {
            Intent intent = activity.getIntent();
            Bundle bundle = intent.getBundleExtra("notification");
            if ( bundle != null ) {
                bundle.putBoolean("foreground", false);
                String bundleString = convertJSON(bundle);
                constants.put("initialNotification", bundleString);
            }
        }
        return constants;
    }

    private void sendEvent(String eventName, Object params) {
        //System.out.println("GRAB sendEvent "+eventName+" "+params.toString());
        if ( mReactContext.hasActiveCatalystInstance() ) {
            mReactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        }
    }

    public void newIntent(Intent intent) {
        System.out.println("GRAB newIntent "+intent.toString());
        if ( intent.hasExtra("notification") ) {
            Bundle bundle = intent.getBundleExtra("notification");
            bundle.putBoolean("foreground", false);
            intent.putExtra("notification", bundle);
            notifyNotification(bundle);
        }
    }

    private void registerNotificationsRegistration() {
        IntentFilter intentFilter = new IntentFilter("RNPushNotificationRegisteredToken");

        mReactContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //System.out.println("GRAB registration onReceive "+intent.toString());
                String token = intent.getStringExtra("token");
                WritableMap params = Arguments.createMap();
                params.putString("deviceToken", token);

                sendEvent("remoteNotificationsRegistered", params);
            }
        }, intentFilter);
    }

    private void registerNotificationsReceiveNotification() {
        IntentFilter intentFilter = new IntentFilter("RNPushNotificationReceiveNotification");
        mReactContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
              System.out.println("GRAB notification onReceive "+intent.toString());
                notifyNotification(intent.getBundleExtra("notification"));
            }
        }, intentFilter);
    }

    private void notifyNotification(Bundle bundle) {
      //This is the case when app is open
        System.out.println("GRAB RNPushNotification.notifyNotification "+bundle.toString());
        mRNPushNotificationHelper.sendNotification(bundle);
    }

    private String convertJSON(Bundle bundle) {
        JSONObject json = new JSONObject();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    json.put(key, JSONObject.wrap(bundle.get(key)));
                } else {
                    json.put(key, bundle.get(key));
                }
            } catch(JSONException e) {
                return null;
            }
        }
        return json.toString();
    }

    @ReactMethod
    public void requestPermissions(String token) {
      boolean validToken = token != null && !token.equalsIgnoreCase("");

      if (!RNPushNotificationRegistrationService.s_token.equalsIgnoreCase("")
        &&  RNPushNotificationRegistrationService.s_instance != null) {
          RNPushNotificationRegistrationService.s_instance.sendRegistrationToken(RNPushNotificationRegistrationService.s_token);
      } else if (validToken) {
        Localytics.setPushRegistrationId(token);
      }
    }

    @ReactMethod
    public void cancelAllLocalNotifications() {
        mRNPushNotificationHelper.cancelAll();
    }

    @ReactMethod
    public void presentLocalNotification(ReadableMap details) {
        Bundle bundle = Arguments.toBundle(details);
        mRNPushNotificationHelper.sendNotification(bundle);
    }

    @ReactMethod
    public void scheduleLocalNotification(ReadableMap details) {
        Bundle bundle = Arguments.toBundle(details);
        mRNPushNotificationHelper.sendNotificationScheduled(bundle);
    }

    @ReactMethod
    public void clearNotificationStack(String adAccountId) {
      RNPushNotificationHelper.clearNotificationStack(adAccountId);
    }

}