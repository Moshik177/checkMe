package com.sadna.app.checkme.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sadna.app.checkme.BaseActivity;
import com.sadna.app.checkme.MyApplication;
import com.sadna.app.checkme.R;
import com.sadna.app.checkme.entities.Group;
import com.sadna.app.webservice.WebService;

import org.json.JSONObject;

/**
 * A login screen that offers login via username and password.
 */


public class CreateNewGroupActivity extends BaseActivity {

  String groupName;
  String groupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_group);
        groupId = ((MyApplication) getApplication()).getUserId();
    }

    public void attemptGroupCreation(View view) {

        EditText groupEditText = ((EditText) findViewById(R.id.newGroupNameTextBox));
        groupName = groupEditText.getText().toString();

        if (!validateForm()) {
            return;
        }
        else signUpGroup();

    }

    private void resetErrors() {
        ((EditText) findViewById(R.id.newGroupNameTextBox)).setError(null);
    }

    private boolean validateForm() {
        boolean valid = true;

        resetErrors();

        if (groupName.isEmpty()) {
            ((EditText) findViewById(R.id.newGroupNameTextBox)).setError(getString(R.string.error_field_not_empty));
            valid = false;
        }

        return valid;
    }


    private void signUpGroup() {
        Thread signUpGroupThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    WebService wsHttpRequest = new WebService("addGroup");
                    String result;
                    Group CreateGroupByUser;
                    Gson gson = new Gson();
                    result = wsHttpRequest.execute(groupName, groupId);
                    CreateGroupByUser = gson.fromJson(result, new TypeToken<Group>() {
                    }.getType());
                    ((MyApplication) getApplication()).setSelectedGroupId(CreateGroupByUser.getId());
                    startActivity(new Intent(getApplicationContext(), AddUsersToGroupActivity.class));
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
        });

        signUpGroupThread.start();
        try {
            signUpGroupThread.join();
        } catch (InterruptedException exception) {
            Log.e("CreateGroupActivity", exception.getMessage());
        }
    }

}



