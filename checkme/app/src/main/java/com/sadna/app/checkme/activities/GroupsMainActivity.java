package com.sadna.app.checkme.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sadna.app.checkme.BaseActivity;
import com.sadna.app.checkme.MyApplication;
import com.sadna.app.checkme.R;
import com.sadna.app.checkme.entities.Group;
import com.sadna.app.checkme.entities.GroupAdapter;
import com.sadna.app.webservice.WebService;
import android.app.AlertDialog;
import android.content.DialogInterface;
import java.util.ArrayList;


/**
 * A login screen that offers login via username and password.
 */
public class GroupsMainActivity extends BaseActivity {

    private List<Group> userGroups = new ArrayList<>();
    private Gson gson = new Gson();
    private ListView userGroupsListView;
    private ArrayAdapter<Group> listViewAdapter;
    private final String[] menuItems = {"עזוב קבוצה", "מחק קבוצה","נהל משתמשים", "פטפט","החלף תמונה"};
    private Map<String, List<Object>> menuItemsPropertiesMap = new HashMap<>();
    private static final int RSUALT_LOAD_IMAGE = 1;
    private static String base64Picture;
    private GroupAdapter groupAdapter;
    private String groupid;
    private Bitmap bitmap;
    private int position;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        menuItemsPropertiesMap.put(menuItems[0], Arrays.asList(true, (Object) "האם אתה בטוח שאתה רוצה לעזוב את הקבוצה"));
        menuItemsPropertiesMap.put(menuItems[1], Arrays.asList(true, (Object) "האם אתה בטוח שאתה רוצה למחוק את הקבוצה"));
        menuItemsPropertiesMap.put(menuItems[2], Arrays.asList(false, (Object) null));
        menuItemsPropertiesMap.put(menuItems[3], Arrays.asList(false, (Object) null));
        menuItemsPropertiesMap.put(menuItems[4], Arrays.asList(false, (Object) null));

        setContentView(R.layout.activity_group_main);
        getUserGroups();

        // Find the ListView resource.
        userGroupsListView = (ListView) findViewById(R.id.groupsListView);

        // On item clicked set selected group name and id in myapp
        userGroupsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Group currentSelectedGroup = (Group) userGroupsListView.getItemAtPosition(position);
                ((MyApplication)getApplication()).setSelectedGroupId(currentSelectedGroup.getId());
                ((MyApplication)getApplication()).setSelectedGroupName(currentSelectedGroup.getName());
                startActivity(new Intent(getApplicationContext(), MapsActivity.class));
            }
        });

        // get data from the table by the ListAdapter
        groupAdapter = new GroupAdapter(this, userGroups);

        userGroupsListView .setAdapter(groupAdapter);
        registerForContextMenu(userGroupsListView);
        SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshActivity();
            }
        });

    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.groupsListView) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(userGroupsListView.getItemAtPosition(info.position).toString());
            menu.add(Menu.NONE, 0, 0, menuItems[0]);
            if (((Group) userGroupsListView.getItemAtPosition(info.position)).getOwnerId() == Integer.parseInt(((MyApplication) getApplication()).getUserId())) {
                for (int i = 1; i < menuItems.length - 2; i++) {
                    menu.add(Menu.NONE, i, i, menuItems[i]);
                }
            }
            menu.add(Menu.NONE, 3, 3, menuItems[3]);
            menu.add(Menu.NONE, 4, 4, menuItems[4]);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        final String menuItemName = menuItems[menuItemIndex];
        String listItemName = userGroupsListView.getItemAtPosition(info.position).toString();

        ((MyApplication) getApplication()).setSelectedGroupId(((Group) userGroupsListView.getItemAtPosition(info.position)).getId());
        ((MyApplication) getApplication()).setSelectedGroupName(((Group) userGroupsListView.getItemAtPosition(info.position)).getName());
         groupid = ((Group) userGroupsListView.getItemAtPosition(info.position)).getId();
        if ((boolean) menuItemsPropertiesMap.get(menuItemName).get(0)) {
            AlertDialog.Builder mBuilder = new AlertDialog.Builder(GroupsMainActivity.this);
            mBuilder.setMessage(menuItemsPropertiesMap.get(menuItemName).get(1) + " \"" + listItemName + "\"?")
                    .setNegativeButton("ביטול", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // if this button is clicked, just close
                            // the dialog box and do nothing
                            dialog.cancel();
                        }
                    })
                    .setPositiveButton("אישור", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    executeContextMenuAction(menuItemName, ((Group) userGroupsListView.getItemAtPosition(info.position)).getId(),info.position);
                                }
                            }
                    );
            AlertDialog alert = mBuilder.create();
            alert.show();
        } else {
            executeContextMenuAction(menuItemName, null,info.position);
        }

        return true;
    }

    private void getUserGroups() {
        Thread getUserGroupsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<String> userGroupsStrings;
                    WebService wsHttpRequest = new WebService("getUserGroups");
                    String result = null;

                    try {
                        result = wsHttpRequest.execute(((MyApplication) getApplication()).getUserId());
                    } catch (Throwable exception) {
                        Log.e("GroupsMainActivity" ,exception.getMessage());
                    }

                    userGroupsStrings = gson.fromJson(result, new TypeToken<ArrayList<String>>() {
                    }.getType());
                    int i = 0 ;
                    for (String group : userGroupsStrings) {
                        userGroups.add(gson.fromJson(group, Group.class));
                        int groupId = Integer.parseInt(userGroups.get(i).getId());
                        getUserPhotoFromTheServer(groupId) ;
                        userGroups.get(i).setPhoto(bitmap);
                        bitmap = null;
                        i++;
                    }
                } catch (Exception e) {
                    Log.e("GroupsMainActivity", e.getMessage());
                }
            }
        });

        getUserGroupsThread.start();
        try {
            getUserGroupsThread.join();
        } catch (InterruptedException exception) {
            Log.e("GroupsMainActivity", exception.getMessage());
        }
    }

    public void addGroup(View view) {
        moveToCreateNewGroupActivity();
    }

    private void moveToCreateNewGroupActivity() {
        startActivity(new Intent(getApplicationContext(), CreateNewGroupActivity.class));
    }

    private void executeContextMenuAction(String actionName, String groupId,int position) {
        if (actionName.equals(menuItems[0])) {
            leaveGroup(groupId);
            refreshActivity();
        } else if (actionName.equals(menuItems[1])) {
            deleteGroup(groupId);
            refreshActivity();
        } else if (actionName.equals(menuItems[2])) {
            startActivity(new Intent(getApplicationContext(), ManageUsersActivity.class));
        } else if (actionName.equals(menuItems[3])) {
            String name = ((MyApplication)getApplication()).getUsername();
            String room_name = ((MyApplication)getApplication()).getSelectedGroupId();
            Intent intent = new Intent(getApplicationContext(), ChatGroupActivity.class);
            intent.putExtra("name", name);
            intent.putExtra("room_name", room_name);
            startActivity(intent);
        } else if (actionName.equals(menuItems[4])) {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent,RSUALT_LOAD_IMAGE);
            this.position = position;
        }
    }

    private void refreshActivity() {
        finish();
        startActivity(getIntent());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, requestCode, data);
        /// / Check which request we're responding to
        if (requestCode == RSUALT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {
            try {
                // get uri from Intent
                Uri uri = data.getData();
                // get bitmap from uri
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                // store bitmap to file
                File filename = new File(Environment.getExternalStorageDirectory(), "imageName.jpg");
                FileOutputStream out = new FileOutputStream(filename);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out);
                out.flush();
                out.close();
                // get base64 string from file
                base64Picture = getStringImage(filename);
                SendingTheGroupPhotoToServer(groupid);
                bitmap =groupAdapter.getCroppedBitmap(bitmap,150,150);
                View view = userGroupsListView.getChildAt(position);
                ImageView imageView = (ImageView)view.findViewById(R.id.group_empty_picture);
                imageView.setImageBitmap(bitmap);

                // use base64 for your next step.
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void SendingTheGroupPhotoToServer(final String groupId) {
        Thread SendingTheGroupPhotoToServer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    uploadPhotoToServer(base64Picture,RSUALT_LOAD_IMAGE,groupId);
                } catch (Exception exception) {
                    Log.e("GroupsMainActivity", exception.getMessage());
                }
            }
        });

        SendingTheGroupPhotoToServer.start();
        try {
            SendingTheGroupPhotoToServer.join();
        } catch (InterruptedException exception) {
            Log.e("GroupsMainActivity", exception.getMessage());
        }
    }

    private String getStringImage(File file){
        try {
            FileInputStream fin = new FileInputStream(file);
            byte[] imageBytes = new byte[(int)file.length()];
            fin.read(imageBytes, 0, imageBytes.length);
            fin.close();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static boolean uploadPhotoToServer(String base64Picture, int number,String id){

        WebService wsHttpRequest = new WebService("setPhoto");
        String result = null;
        try {
            result = wsHttpRequest.execute(base64Picture,Integer.toString(number),id);
        } catch (Throwable exception) {
            exception.printStackTrace();
            return false;
        }
        return true;
    }

    private void leaveGroup(final String groupId) {
        Thread leaveGroupThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    WebService wsHttpRequest = new WebService("leaveGroup");

                    try {
                        wsHttpRequest.execute(((MyApplication) getApplication()).getUserId(), groupId);
                    } catch (Throwable exception) {
                        Log.e("GroupsMainActivity", exception.getMessage());
                    }

                } catch (Exception exception) {
                    Log.e("GroupsMainActivity", exception.getMessage());
                }
            }
        });

        leaveGroupThread.start();
        try {
            leaveGroupThread.join();
        } catch (InterruptedException exception) {
            Log.e("GroupsMainActivity", exception.getMessage());
        }
    }

    private void deleteGroup(final String groupId) {
        Thread deleteGroupThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    WebService wsHttpRequest = new WebService("deleteGroup");

                    try {
                        wsHttpRequest.execute(groupId);
                    } catch (Throwable exception) {
                        Log.e("GroupsMainActivity", exception.getMessage());
                    }

                } catch (Exception exception) {
                    Log.e("GroupsMainActivity", exception.getMessage());
                }
            }
        });

        deleteGroupThread.start();
        try {
            deleteGroupThread.join();
        } catch (InterruptedException exception) {
            Log.e("GroupsMainActivity", exception.getMessage());
        }
    }

    private void getUserPhotoFromTheServer(final int groupId) {
        Thread getUserPhotoFromTheServer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    WebService wsHttpRequest = new WebService("getPhoto");
                    String base64String = wsHttpRequest.execute(Integer.toString(RSUALT_LOAD_IMAGE),Integer.toString(groupId));
                    byte[] bytes = Base64.decode(base64String.getBytes(), Base64.DEFAULT);
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

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
}