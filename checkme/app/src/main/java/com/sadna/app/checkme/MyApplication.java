package com.sadna.app.checkme;

import android.app.Application;


public class MyApplication extends Application {

    private String mUsername;
    private String mUserId;
    private String mSelectedGroupName;
    private String mSelectedGroupId;
    private static boolean activityVisible = false;
    private boolean mIsSelf;

    public boolean getIsSelf() { return mIsSelf;}

    public void setIsSelf(boolean mIsSelf) { this.mIsSelf  = mIsSelf;}

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        this.mUsername = username;
    }

    public String getUserId() {
        return mUserId;
    }

    public void setUserId(String userId) {
        this.mUserId = userId;
    }

    public String getSelectedGroupName() {
        return mSelectedGroupName;
    }

    public void setSelectedGroupName(String selectedGroupName) {
        this.mSelectedGroupName = selectedGroupName;
    }

    public String getSelectedGroupId() {
        return mSelectedGroupId;
    }

    public void setSelectedGroupId(String selectedGroupId) {
        this.mSelectedGroupId = selectedGroupId;
    }

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }
}