package com.sadna.app.checkme.activities;


import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sadna.app.checkme.BaseActivity;
import com.sadna.app.checkme.MyApplication;
import com.sadna.app.checkme.R;
import com.sadna.app.checkme.entities.UserId;
import com.sadna.app.checkme.entities.UserPhone;
import com.sadna.app.webservice.WebService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class AddUsersToGroupActivity extends BaseActivity {

    private ArrayList<String> Phones = new ArrayList<>();
    private Map<String, String> map = new HashMap<>();
    private Map<String,String> mapAfterFindingMatches =  new HashMap<>();
    private Vector<String> namesOfContacts = new Vector<>();
    private ArrayList<UserPhone> PhonesThatAreConnectedWithApp = new ArrayList<>();
    private Gson gson = new Gson();
    private List<UserId> groupMembers = new ArrayList<>();
    private static Cursor pCur;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_users_to_group);

        getGroupMembers();
        getAllContactsFromUser();
        GetPhonesFromDataBase();

        final ListView resultListView = (ListView) findViewById(R.id.ListOfContacts);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.group_row, namesOfContacts);
        resultListView.setAdapter(adapter);
        resultListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // ListView Clicked item value
                final String itemValue = (String) resultListView.getItemAtPosition(position);
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(AddUsersToGroupActivity.this);
                mBuilder.setMessage("אתה בטוח שאתה רוצה להוסיף אותו לקבוצה?")
                        .setNegativeButton("ביטול", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // if this button is clicked, just close
                                // the dialog box and do nothing
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("אישור", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        adapter.remove(itemValue);
                                        setUserOnGroupDataBase(itemValue);
                                    }
                                }
                        );
                AlertDialog alert = mBuilder.create();
                alert.show();

            }
        });

        final Button finishButton = (Button) findViewById(R.id.finish_button);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), GroupsMainActivity.class));
            }
        });
    }


    private void getAllContactsFromUser() {
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cur != null && cur.getCount() > 0) {
            while (cur.moveToNext()) {

                if ((Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)))) == 1) {
                    String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                    int i = 0;

                    while (pCur != null && pCur.moveToNext()) {
                        int pCount = pCur.getCount();
                        String[] phoneNum = new String[pCount];
                        String[] phoneType = new String[pCount];
                        phoneNum[i] = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        phoneType[i] = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                        String newStringOrder = orderPhoneNumbers(phoneNum[i]);
                        if (newStringOrder != null) {
                            Phones.add(newStringOrder);
                            map.put(newStringOrder, name);
                        }
                        i++;

                    }
                    pCur.close();
                }

            }
        }

        cur.close();
    }

    private String orderPhoneNumbers(String unOrderedPhoneNumber) {
        StringBuilder newOrderString = new StringBuilder(unOrderedPhoneNumber);
        String stringToReturn;

        if (newOrderString.length() < 10 || newOrderString.substring(0, 3).compareTo("077") == 0 || newOrderString.substring(0, 3).compareTo("072") == 0) {
            stringToReturn = null;
        } else {
            newOrderString = new StringBuilder(newOrderString.toString().replaceAll("\\s", "").replaceAll("-", "").replaceAll("^?972", "0").replace("+", ""));
            stringToReturn = newOrderString.toString();
        }
        return stringToReturn;
    }

    private void setUserOnGroupDataBase(final String userName) {
        Thread addUserToGroupThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    WebService wsHttpRequest = new WebService("addUserToGroup");

                    try {
                        wsHttpRequest.execute(((MyApplication) getApplication()).getSelectedGroupId(), userName);
                    } catch (Throwable exception) {
                        Log.e("AddUsersToGroupActivity", exception.getMessage());
                    }
                } catch (Exception e) {
                    Log.e("AddUsersToGroupActivity", e.getMessage());
                }
            }
        });

        addUserToGroupThread.start();
        try {
            addUserToGroupThread.join();
        } catch (InterruptedException exception) {
            Log.e("AddUsersToGroupActivity", exception.getMessage());
        }
    }

    private void GetPhonesFromDataBase() {
        Thread getUserPhonesThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    WebService wsHttpRequest = new WebService("getPhonesFromContacts");
                    String result = null;

                    try {
                        result = wsHttpRequest.executeToArray(Phones);
                    } catch (Throwable exception) {
                        Log.e("AddUsersToGroupActivity", exception.getMessage());
                    }

                    PhonesThatAreConnectedWithApp = gson.fromJson(result, new TypeToken<ArrayList<UserPhone>>() {
                    }.getType());

                    compareMapWithPhoneContacts();
                } catch (Exception e) {
                    Log.e("AddUsersToGroupActivity", e.getMessage());
                }
            }
        });

        getUserPhonesThread.start();
        try {
            getUserPhonesThread.join();
        } catch (InterruptedException exception) {
            Log.e("AddUsersToGroupActivity", exception.getMessage());
        }
    }

    private void getGroupMembers() {
        Thread getGroupMembersThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    WebService wsHttpRequest = new WebService("getGroupMembers");
                    String result = null;

                    try {
                        result = wsHttpRequest.execute(((MyApplication) getApplication()).getSelectedGroupId());
                    } catch (Throwable exception) {
                        Log.e("RemoveUsersGroupActvt", exception.getMessage());
                    }

                    groupMembers = gson.fromJson(result, new TypeToken<ArrayList<UserId>>() {
                    }.getType());
                } catch (Exception e) {
                    Log.e("RemoveUsersGroupActvt", e.getMessage());
                }
            }
        });

        getGroupMembersThread.start();
        try {
            getGroupMembersThread.join();
        } catch (InterruptedException exception) {
            Log.e("RemoveUsersGroupActvt", exception.getMessage());
        }
    }


    private void compareMapWithPhoneContacts() {
        for (UserPhone userPhone : PhonesThatAreConnectedWithApp) {
            for (Map.Entry<String, String> contact : map.entrySet()) {
                String phoneToCompare = contact.getKey();
                String PhoneOfUser = userPhone.getPhone();
                if (PhoneOfUser.equals(phoneToCompare)) {
                    if (!userAlreadyExistsOnGroup(userPhone.getUsername())) {
                        mapAfterFindingMatches.put(phoneToCompare, userPhone.getUsername());
                        namesOfContacts.add(userPhone.getUsername());
                        break;
                    }
                }
            }
        }

    }

    private boolean userAlreadyExistsOnGroup(String username) {
        for (UserId member : groupMembers) {
            if (member.getUsername().equals(username)) {
                return true;
            }
        }

        return false;
    }

}