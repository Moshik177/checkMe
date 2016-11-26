package com.sadna.app.checkme.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.RemoteMessage;
import com.sadna.app.checkme.MyApplication;
import com.sadna.app.checkme.R;
import com.sadna.app.checkme.activities.ChatGroupActivity;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        if(((MyApplication) getApplication()).getIsSelf() && ((MyApplication) getApplication()).isActivityVisible()) {
            showNotification(remoteMessage.getData().get("message"));
        }
    }

    private void showNotification(String message) {

        Intent i = new Intent(this, ChatGroupActivity.class);
        String mTitle = "CheckMe";
        int mNotificationId = 1;
        int MAX_NOTIFICATIONS = 5;
        NotificationManager mNotifyMgr;
        NotificationCompat.Builder mBuilder;
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_ONE_SHOT);
        Uri sound= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
       // Uri sound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.chat_ringtone_pop);
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
//            mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.mipmap.check_me_logo);
//        } else {

            mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.mipmap.check_me_logo)
                    .setContentTitle(mTitle)
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setSound(sound)
                    .setContentIntent(pendingIntent);

        mNotificationId = (mNotificationId == MAX_NOTIFICATIONS) ? 1 : mNotificationId + 1;
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, mNotificationId, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_CANCEL_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mNotifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.notify(0, mBuilder.build());

        //MediaPlayer player = MediaPlayer.create(this, R.raw.message_incoming);
        //player.setLooping(false);
        //player.start();

    }

}
