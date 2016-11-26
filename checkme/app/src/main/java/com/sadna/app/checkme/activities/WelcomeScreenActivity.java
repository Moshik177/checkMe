package com.sadna.app.checkme.activities;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;
import android.util.Log;
import com.sadna.app.checkme.BaseActivity;
import com.sadna.app.checkme.R;



public class WelcomeScreenActivity extends BaseActivity {

    private SharedPreferences mSharedPref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mSharedPref = getApplicationContext().getSharedPreferences("CheckMePref", 0); // 0 - for private mode;
        Typeface face=Typeface.createFromAsset(getAssets(),"fonts/ufonts.com_segoe-script-bold.ttf");
        TextView logoText = (TextView) findViewById(R.id.logoText);
        logoText.setTypeface(face);

        /** set time to splash out */
        final int welcomeScreenDisplay = 3000;
        /** create a thread to show splash up to splash time */
        Thread welcomeThread = new Thread() {

            int wait = 0;

            @Override
            public void run() {
                try {
                    super.run();
                    /**
                        * use while to get the splash time. Use sleep() to increase
                        * the wait variable for every 100L.
                    */
                    while (wait < welcomeScreenDisplay) {
                        sleep(100);
                        wait += 100;
                    }

                } catch (Exception e) {
                    Log.e("WelcomeScreen", e.getMessage());
                } finally {
                    /**
                        * Called after splash times up. Do some action after splash
                        * times up. Here we moved to another main activity class
                    */
                    if (!mSharedPref.getBoolean("phone_activated", false)) {
                        startActivity(new Intent(getApplicationContext(), PhoneVerificationSendActivity.class));
                    }
                    else {
                        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                    }
                    finish();
                }
            }
        };
        welcomeThread.start();
    }


}
