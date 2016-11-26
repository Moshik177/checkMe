package com.sadna.app.checkme.utils;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.sadna.app.checkme.MyApplication;
import com.sadna.app.checkme.activities.ChatGroupActivity;
import com.sadna.app.webservice.WebService;

public class FirebaseInstanceIDService extends FirebaseInstanceIdService{

    private String name;
    private SharedPreferences mSharedPref;
    private final String kUSERNAME = "cm_login_username";

    @Override
    public void onTokenRefresh() {


        //String UserName = ((MyApplication) getApplication()).getUsername();
        //Intent i = new Intent(this, ChatGroupActivity.class);
        //name = i.getStringExtra("name");
        String token = FirebaseInstanceId.getInstance().getToken();
        registerToken(token);
    }

    private void registerToken(final String token){

       name =  SharedPreferenceUtils.getInstance(this).getStringValue("name", "");

        Thread registerTokenToServer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                        WebService wsHttpRequest = new WebService("setToken");
                        String tokenId = wsHttpRequest.execute(name,token);

                } catch (Exception exception) {
                    Log.e("ServiceToken", exception.getMessage());
                }
            }
        });

        registerTokenToServer.start();
        try {
            registerTokenToServer.join();
        } catch (InterruptedException exception) {
            Log.e("ServiceToken", exception.getMessage());
        }
    }


    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Bundle extras = intent.getExtras();
        if(extras == null)
            Log.d("Service","null");
        else
        {
            Log.d("Service","not null");
            name = (String) extras.get("name");
        }

    }
}
