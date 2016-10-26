package com.dieam.reactnativepushnotification.modules;


import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.HashMap;
import java.util.Vector;

public class RNPushNotificationHelper {
    private Context mContext;
    private static HashMap<String, Bundle> s_notifMap = new HashMap<String, Bundle>();
    private static HashMap<String, JSONObject> s_groupMap = new HashMap<String, JSONObject>();
    private static int s_id = 0;

    public RNPushNotificationHelper(Application context) {
        mContext = context;
    }

    public static void clearNotificationStack(String adAccountId) {
      s_groupMap.put(adAccountId, new JSONObject());
    }

    public Class getMainActivityClass() {
      String packageName = mContext.getPackageName();
      Intent launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(packageName);
      String className = launchIntent.getComponent().getClassName();
      try {
          return Class.forName(className);
      } catch (ClassNotFoundException e) {
          e.printStackTrace();
          return null;
      }
    }

    private AlarmManager getAlarmManager() {
        return (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
    }

    private PendingIntent getScheduleNotificationIntent(Bundle bundle) {
        int notificationID;
        String notificationIDString = bundle.getString("id");

        if ( notificationIDString != null ) {
            notificationID = Integer.parseInt(notificationIDString);
        } else {
            notificationID = (int) System.currentTimeMillis();
        }

        Intent notificationIntent = new Intent(mContext, RNPushNotificationPublisher.class);
        notificationIntent.putExtra(RNPushNotificationPublisher.NOTIFICATION_ID, notificationID);
        notificationIntent.putExtras(bundle);

        return PendingIntent.getBroadcast(mContext, notificationID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void sendNotificationScheduled(Bundle bundle) {
        Class intentClass = getMainActivityClass();
        if (intentClass == null) {
            return;
        }

        Double fireDateDouble = bundle.getDouble("fireDate", 0);
        if (fireDateDouble == 0) {
            return;
        }

        long fireDate = Math.round(fireDateDouble);
        long currentTime = System.currentTimeMillis();

        Log.i("ReactSystemNotification", "fireDate: " + fireDate + ", Now Time: " + currentTime);
        PendingIntent pendingIntent = getScheduleNotificationIntent(bundle);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getAlarmManager().setExact(AlarmManager.RTC_WAKEUP, fireDate, pendingIntent);
        } else {
            getAlarmManager().set(AlarmManager.RTC_WAKEUP, fireDate, pendingIntent);
        }
    }

    public void sendNotification(Bundle bundle) {
        Class intentClass = getMainActivityClass();
        String dataStr = bundle.getString("data");
        String msgStr = bundle.getString("message");

        if (intentClass == null) {
            return;
        }

        if (msgStr == null) {
            return;
        }

        if (dataStr == null) {
            return;
        }

        //disallow duplicates
        if (s_notifMap.get(dataStr) != null) {
          return;
        }

        s_notifMap.put(dataStr, bundle);

System.out.println("GRAB Helper.sendNotification "+dataStr);
        Resources res = mContext.getResources();
        String packageName = mContext.getPackageName();

        String title = bundle.getString("title");
        if (title == null) {
            ApplicationInfo appInfo = mContext.getApplicationInfo();
            title = mContext.getPackageManager().getApplicationLabel(appInfo).toString();
        }
        //find group based on account or campaign id;
        JSONObject dataObj = new JSONObject();
        JSONArray groupMsgs = null;
        String groupStr = "";
        String campaignString = "";
        int notificationID = 0;
        NotificationManager notificationManager =
          (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notification = null;

        try {
          dataObj = new JSONObject(dataStr);
          groupStr = dataObj.getString("account_id");
System.out.println("GRAB account id "+groupStr);

          JSONObject groupObj = s_groupMap.get(groupStr);

          if (groupObj == null) {
            groupObj = new JSONObject();
            groupObj.put("id", s_id++);
          }

          groupMsgs = groupObj.optJSONArray("messages");

          if (groupMsgs == null) {
            groupMsgs = new JSONArray();
          }

          groupMsgs.put(msgStr);
          groupObj.put("messages", groupMsgs);

          notification = new NotificationCompat.Builder(mContext)
                    .setContentTitle(title)
                    .setTicker(bundle.getString("ticker"))
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(bundle.getBoolean("autoCancel", true));
                    //.setGroup(groupStr);

          notificationID = groupObj.getInt("id");


  System.out.println("GRAB "+groupMsgs.length()+" messages for group "+groupStr+" with id "+notificationID);
          if (groupMsgs.length() == 1) {
            notification.setContentText(msgStr);
          } else {
            //cancel the previous notification with this id and create a new one
            //notificationManager.cancel(notificationID);
            notification.setContentText("You have "+groupMsgs.length()+" new messages.");
            NotificationCompat.InboxStyle ibs = new NotificationCompat.InboxStyle();

            for (int i = 0; i < groupMsgs.length(); ++i) {
              ibs = ibs.addLine(groupMsgs.getString(i));
            }

            notification.setStyle(ibs)
              .setGroup(groupStr)
              .setGroupSummary(true);
              //need to modify data so that deeplink goes to account, not campaign

                dataObj.put("campaign_id", null);
                dataStr = dataObj.toString();

  System.out.println("GRAB stacked with deeplink "+dataStr);
          }

          s_groupMap.put(groupStr, groupObj);
        } catch (Exception e) {
  System.out.println("GRAB error "+e.toString());
        }
        String largeIcon = bundle.getString("largeIcon");

        String subText = bundle.getString("subText");

        if ( subText != null ) {
            notification.setSubText(subText);
        }

        //String number = bundle.getString("number");

        //if ( number != null ) {
            //notification.setNumber(++ s_numMessages);//  Integer.parseInt(number));
        //}

        int smallIconResId;
        int largeIconResId;

        String smallIcon = bundle.getString("smallIcon");

        if ( smallIcon != null ) {
            smallIconResId = res.getIdentifier(smallIcon, "mipmap", packageName);
        } else {
            smallIconResId = res.getIdentifier("ic_notification", "mipmap", packageName);
        }

        if ( smallIconResId == 0 ) {
            smallIconResId = res.getIdentifier("ic_launcher", "mipmap", packageName);

            if ( smallIconResId == 0 ) {
                smallIconResId  = android.R.drawable.ic_dialog_info;
            }
        }

        if ( largeIcon != null ) {
            largeIconResId = res.getIdentifier(largeIcon, "mipmap", packageName);
        } else {
            largeIconResId = res.getIdentifier("ic_launcher", "mipmap", packageName);
        }

        Bitmap largeIconBitmap = BitmapFactory.decodeResource(res, largeIconResId);

        if ( largeIconResId != 0 && ( largeIcon != null || android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP ) ) {
            notification.setLargeIcon(largeIconBitmap);
        }

        notification.setSmallIcon(smallIconResId);
        String bigText = bundle.getString("bigText");

        if (bigText == null ) {
            bigText = bundle.getString("message");
        }

        if (groupMsgs.length() == 1) {
          notification.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
        }

        Intent deeplinkIntent = new Intent(Intent.ACTION_VIEW);
        String deeplinkURL = "blackbird://deeplink?data="+dataStr;
        deeplinkIntent.setData(Uri.parse(deeplinkURL));

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notification.setSound(defaultSoundUri);

        if ( android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            notification.setCategory(NotificationCompat.CATEGORY_CALL);

            String color = bundle.getString("color");
            if (color != null) {
                notification.setColor(Color.parseColor(color));
            }
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, notificationID, deeplinkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        notification.setContentIntent(pendingIntent);

        Notification info = notification.build();
        info.defaults |= Notification.DEFAULT_VIBRATE;
        info.defaults |= Notification.DEFAULT_SOUND;
        info.defaults |= Notification.DEFAULT_LIGHTS;

        notificationManager.notify(notificationID, info);
    }

    public void cancelAll() {
        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancelAll();

        Bundle b = new Bundle();
        b.putString("id", "0");
        getAlarmManager().cancel(getScheduleNotificationIntent(b));
    }
}
