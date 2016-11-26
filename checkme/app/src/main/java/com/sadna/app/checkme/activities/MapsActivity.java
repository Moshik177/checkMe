package com.sadna.app.checkme.activities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sadna.app.checkme.MyApplication;
import com.sadna.app.checkme.R;
import com.sadna.app.checkme.entities.GroupAdapter;
import com.sadna.app.checkme.entities.UserLocation;
import com.sadna.app.webservice.WebService;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    final int THIRTY_SECONDS_IN_MILLISECONDS = 30000;

    private List<UserLocation> usersLocations = new ArrayList<>();
    private Gson gson = new Gson();
    private ArrayList<Marker> mMarkers = new ArrayList<>();
    private static final int CHOOSENNUMBER = 2;
    private Handler mHandler;
    public static GoogleMap mMap;
    public Bitmap mUserDynamicallyGeneratedPictureResource;
    private int UserId;
    private Bitmap bitPhoto;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ((TextView) findViewById(R.id.groupNameTitle)).setText(((MyApplication) getApplication()).getSelectedGroupName());
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.mMap = map;

        // Map settings
        map.setBuildingsEnabled(true);
        map.setIndoorEnabled(true);
        map.setTrafficEnabled(true);

        // UI Settings
        map.getUiSettings().setAllGesturesEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setIndoorLevelPickerEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);

        showUpdatedLocationsOnMapEveryXSeconds(THIRTY_SECONDS_IN_MILLISECONDS);
    }

    @Override
    public void onBackPressed() {
        // stop Handler
        mHandler.removeCallbacksAndMessages(null);
        finish();
    }

    private void showUpdatedLocationsOnMapEveryXSeconds(int millisecondsToDelay) {
        mHandler = new Handler();
        final int delay = millisecondsToDelay; // milliseconds

        mHandler.postDelayed(new Runnable() {
            public void run() {
                showLocationsOnMap(MapsActivity.mMap);
                mHandler.postDelayed(this, delay);
            }
        }, 0);
    }

    private void showLocationsOnMap(GoogleMap map) {
        getGroupMembersLocations();

        // Clears any past markers
        for (Marker marker : mMarkers) {
            marker.remove();
        }
        mMarkers.clear();
        map.clear();

        // For each location, generate marker custom image and add marker to the map
        for (UserLocation userLocation : usersLocations) {
            LatLng location = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
            getUserId(userLocation.getUsername());
            getUserPhotoFromTheServer(UserId);
            //String userGeneratedPictureURL = "https://chart.googleapis.com/chart?chst=d_bubble_icon_text_big_withshadow&chld=location|bb|" + userLocation.getUsername() + "|00CCFF|000000";
            //downloadDynamicallyGeneratedPicture(userGeneratedPictureURL, Integer.toString(userLocation.getUsername().hashCode()));
            Marker marker = map.addMarker(new MarkerOptions().position(location).icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(bitPhoto,userLocation.getUsername()))));
            mMarkers.add(marker);
            bitPhoto = null;
        }

        // Sets the optimal zoom of the map to include all users
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : mMarkers) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();
        int padding = 100; // offset from edges of the mMap in pixels
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        map.animateCamera(cameraUpdate);
    }

    private void downloadDynamicallyGeneratedPicture(final String fileUrl, final String name) {
        Thread downloadDynamicallyGeneratedPictureThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream is = (InputStream) new URL(fileUrl).getContent();
                    Drawable d = Drawable.createFromStream(is, name);
                    mUserDynamicallyGeneratedPictureResource = ((BitmapDrawable) d).getBitmap();
                } catch (Exception exception) {
                    Log.e("MapsActivity", exception.getMessage());
                }
            }
        });

        downloadDynamicallyGeneratedPictureThread.start();
        try {
            downloadDynamicallyGeneratedPictureThread.join();
        } catch (InterruptedException exception) {
            Log.e("MapsActivity", exception.getMessage());
        }
    }

    private Bitmap getMarkerBitmapFromView(Bitmap photo,String username) {


        View customMarkerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.view_custom_mark, null);
        ImageView markerImageView = (ImageView) customMarkerView.findViewById(R.id.profile_image);
        TextView user = (TextView) customMarkerView.findViewById(R.id.name);
        if(photo == null) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);
            bm = GroupAdapter.getCroppedBitmap(bm,50,50);
            markerImageView.setImageBitmap(bm);
        }

        else {
            photo =GroupAdapter.getCroppedBitmap(photo,50,50);
            markerImageView.setImageBitmap(photo);
        }
        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        customMarkerView.layout(0, 0,customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight());
        customMarkerView.buildDrawingCache();
        Bitmap returnedBitmap = Bitmap.createBitmap(customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        user.setText(username);
        Paint color = new Paint();
        color.setColor(Color.BLACK);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable drawable = customMarkerView.getBackground();
        if (drawable != null)
            drawable.draw(canvas);
        customMarkerView.draw(canvas);
        return returnedBitmap;
    }
    private void getUserId(final String CurrUsername) {
        Thread getUsersIdThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    WebService wsHttpRequest = new WebService("getUser");
                    String userDetails = wsHttpRequest.execute(CurrUsername);
                    JSONObject userDetailsJsonObject = new JSONObject(userDetails);
                    UserId =  userDetailsJsonObject.getInt("id");

                } catch (Exception e) {
                    Log.e("MapsActivity", e.getMessage());
                }
            }
        });

        getUsersIdThread.start();
        try {
            getUsersIdThread.join();
        } catch (InterruptedException exception) {
            Log.e("MapsActivity", exception.getMessage());
        }
    }

    private void getUserPhotoFromTheServer(final int Userid) {
        Thread getUserPhotoFromTheServer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    WebService wsHttpRequest = new WebService("getPhoto");
                    String base64String = wsHttpRequest.execute(Integer.toString(CHOOSENNUMBER),Integer.toString(Userid));
                    byte[] bytes = Base64.decode(base64String.getBytes(), Base64.DEFAULT);
                    bitPhoto = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                } catch (Exception e) {
                    e.getMessage();
                    Log.e("MapsActivity", e.getMessage());
                }
            }
        });

        getUserPhotoFromTheServer.start();
        try {
            getUserPhotoFromTheServer.join();
        } catch (InterruptedException exception) {
            Log.e("MapsActivity", exception.getMessage());
        }
    }


    private void getGroupMembersLocations() {
        Thread getGroupMembersLocationsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    WebService wsHttpRequest = new WebService("getGroupMembersLocations");
                    String result = null;

                    try {
                        result = wsHttpRequest.execute(((MyApplication) getApplication()).getSelectedGroupId());
                    } catch (Throwable exception) {
                        Log.e("MapsActivity", exception.getMessage());
                    }

                    usersLocations = gson.fromJson(result, new TypeToken<ArrayList<UserLocation>>() {
                    }.getType());
                } catch (Exception e) {
                    Log.e("MapsActivity", e.getMessage());
                }
            }
        });

        getGroupMembersLocationsThread.start();
        try {
            getGroupMembersLocationsThread.join();
        } catch (InterruptedException exception) {
            Log.e("MapsActivity", exception.getMessage());
        }
    }
}