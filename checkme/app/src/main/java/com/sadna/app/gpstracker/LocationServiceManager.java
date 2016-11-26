package com.sadna.app.gpstracker;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.sadna.app.checkme.MyApplication;
import com.sadna.app.webservice.WebService;

public class LocationServiceManager extends Service implements
        LocationListener, ConnectionCallbacks, OnConnectionFailedListener {

    public static final String TAG = LocationServiceManager.class.getSimpleName();

    private Location mLastLocation;

    private GoogleApiClient mGoogleApiClient;

    private boolean mRequestingLocationUpdates = false;

    private LocationRequest mLocationRequest;

    private static int UPDATE_INTERVAL = 5000; // 5 sec
    private static int FATEST_INTERVAL = 2500; // 2.5 sec
    private static int DISPLACEMENT = 1; // in meters

    private final Handler handler = new Handler();
    private Intent i;

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (checkPlayServices()) {
            buildGoogleApiClient();
            createLocationRequest();
            i = new Intent(TAG);
            if (mGoogleApiClient != null) {
                mGoogleApiClient.connect();
                sendLocation(true);
            }
        }
        return START_STICKY;
    }

    private void sendLocation(boolean usedLastKnownLocation) {
        if (usedLastKnownLocation) {
            mLastLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
        }

        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            SendLocationAsyncTask sendLocationTask = new SendLocationAsyncTask(((MyApplication) getApplication()).getUsername(), String.valueOf(latitude), String.valueOf(longitude));
            sendLocationTask.execute((Void) null);
        } else {
            Log.e(TAG, "Failed get last known location");
        }
        togglePeriodicLocationUpdates();
    }

    private void togglePeriodicLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            if (!mRequestingLocationUpdates) {
                mRequestingLocationUpdates = true;
                startLocationUpdates();
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                return false;
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                return false;
            }
        }
        return true;
    }

    protected void startLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle bundle) {

        sendLocation(true);

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int status) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        sendLocation(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }

    }

    protected boolean sendLocationToWebService(String username, String latitude, String longitude) {
        WebService wsHttpRequest = new WebService("setUserLocation");

        try {
            wsHttpRequest.execute(username, latitude, longitude);
        } catch (Throwable e) {
            e.printStackTrace();
            //Log.e(TAG, e.getMessage());
            return false;
        }

        return true;
    }

    public class SendLocationAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private String mUsername;
        private String mLatitude;
        private String mLongitude;

        SendLocationAsyncTask(String username, String latitude, String longitude) {
            mUsername = username;
            mLatitude = latitude;
            mLongitude = longitude;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return sendLocationToWebService(mUsername, mLatitude, mLongitude);
        }

        @Override
        protected void onPostExecute(final Boolean success) {
        }

        @Override
        protected void onCancelled() {
        }
    }
}