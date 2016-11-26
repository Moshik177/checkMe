package com.sadna.app.checkme.activities;


import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.codebutler.android_websockets.WebSocketClient;

import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sadna.app.checkme.BaseActivity;
import com.sadna.app.checkme.MyApplication;
import com.sadna.app.checkme.R;
import com.sadna.app.checkme.entities.Group;
import com.sadna.app.checkme.entities.UserId;
import com.sadna.app.checkme.entities.UserLocation;
import com.sadna.app.checkme.utils.ChatUtils;
import com.sadna.app.checkme.utils.FirebaseInstanceIDService;
import com.sadna.app.checkme.utils.Message;
import com.sadna.app.checkme.utils.MessagesListAdapter;
import com.sadna.app.checkme.utils.SharedPreferenceUtils;
import com.sadna.app.checkme.utils.encoder;
import com.sadna.app.webservice.WebService;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.google.android.gms.analytics.internal.zzy.v;
import static com.google.android.gms.wearable.DataMap.TAG;

public class ChatGroupActivity extends BaseActivity {

    // LogCat tag
    private static final String TAG = "ChatGroupActivity";

    private static final String CHAT_SOCKET_URL = "vmedu103.mtacloud.co.il";

    public static final String URL_WEBSOCKET = "ws://" + CHAT_SOCKET_URL + ":8081/sadna.chat-server/chat?name=%s&room_name=%s";

    // JSON flags to identify the kind of JSON response
    private static final String TAG_SELF = "self", TAG_NEW = "new",
            TAG_MESSAGE = "message", TAG_EXIT = "exit";

    private boolean isOpened = false;
    private EditText inputMsg;
    private WebSocketClient client;
    private List<UserId> usersGroup;
    // Chat messages list adapter
    private MessagesListAdapter adapter;
    private List<Message> listMessages;
    private ListView listViewMessages;
    private String tokens;
    private ChatUtils utils;

    // Client name
    private String name = null;

    // Client room
    private String room = null;
    private Toolbar toolbar;
    private String mTitle = "CheckMe";
    private int mNotificationId = 1;
    private static final int MAX_NOTIFICATIONS = 5;
    NotificationManager mNotifyMgr;
    private String ChatMessages;
    private List<String> UserNames = new ArrayList<String>();
    private String users;

    @Override
    protected void onResume() {
        super.onResume();
        mNotifyMgr.cancelAll();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ((MyApplication) getApplication()).activityPaused();
    }

    @Override
    protected void onStop() {
        super.onStop();
        ((MyApplication) getApplication()).activityPaused();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_group);
        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        ((TextView) findViewById(R.id.groupNameTitle)).setText(((MyApplication) getApplication()).getSelectedGroupName());

        inputMsg = (EditText) findViewById(R.id.inputMsg);
        listViewMessages = (ListView) findViewById(R.id.list_view_messages);

        utils = new ChatUtils(getApplicationContext());

        // Getting the person name from previous screen
        Intent i = getIntent();
        name = i.getStringExtra("name");
        room = i.getStringExtra("room_name");

        SharedPreferenceUtils.getInstance(this).setValue("name", name);
        listMessages = new ArrayList<>();
        toolbar = new Toolbar(this);
        //this will read line after line and build the right conv
        //addMessagesToArray(listMessages);
        String groupId = ((MyApplication) getApplication()).getSelectedGroupId();
        verifyThatHaveTheToken();
        getChat(groupId);
        adapter = new MessagesListAdapter(this, listMessages);
        listViewMessages.setAdapter(adapter);
        getGroupMemmbers(room);
        getTokens();

            /**
             * Creating web socket client. This will have callback methods
             * */
        try {
            client = new WebSocketClient(URI.create(String.format(URL_WEBSOCKET, encoder.encode(name,"utf-8"), URLEncoder.encode(room))), new WebSocketClient.Listener() {
                @Override
                public void onConnect() {

                }

                /**
                 * On receiving the message from web socket server
                 */
                @Override
                public void onMessage(String message) {
                    Log.d(TAG, String.format("Got string message! %s", message));

                    parseMessage(message);

                }

                @Override
                public void onMessage(byte[] data) {
                    Log.d(TAG, String.format("Got binary message! %s",
                            bytesToHex(data)));


                    // Message will be in JSON format
                    parseMessage(bytesToHex(data));
                }

                /**
                 * Called when the connection is terminated
                 */
                @Override
                public void onDisconnect(int code, String reason) {

                    String message = String.format(Locale.US,
                            "Disconnected! Code: %d Reason: %s", code, reason);

                    showToast(message);

                    // clear the session id from shared preferences
                    utils.storeSessionId(null);
                }

                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "Error! : " + error);

                    //showToast("Error! : " + error);
                }

            }, null);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        client.connect();


    }

        public void setListenerToRootView(View v) {

                        if (isOpened == false) {
                            Toolbar.LayoutParams toolBarParams = new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT,
                                    R.attr.actionBarSize);
                            toolbar.setLayoutParams(toolBarParams);
                            toolbar.setBackgroundColor(getResources().getColor(R.color.holo_purple_new));
                            toolbar.setTitleTextColor(Color.WHITE);
                            toolbar.setTitle(((MyApplication) getApplication()).getSelectedGroupName());
                            //toolbar.setPopupTheme(R.style.AppTheme_PopupOverlay);
                            toolbar.setVisibility(View.VISIBLE);
                            isOpened = true;
                        }

                     else if (isOpened == true) {
                        toolbar.setVisibility(View.INVISIBLE);
                        isOpened = false;
                    }
                }


    public void onButtonSendClick(View v) {

        if (!inputMsg.getText().toString().trim().isEmpty()) {
            // Sending message to web srocket server
            sendMessageToServer(utils.getSendMessageJSON(inputMsg.getText().toString(),tokens,name));
        
            // Clearing the input filed once message was sent
            inputMsg.setText("");
        }
    }

    /**
     * Method to send message to web socket server
     */
    private void sendMessageToServer(String message) {
        if (client != null && client.isConnected()) {
            client.send(message);
        }
    }


    /**
     * Parsing the JSON message received from server The intent of message will
     * be identified by JSON node 'flag'. flag = self, message belongs to the
     * person. flag = new, a new person joined the conversation. flag = message,
     * a new message received from server. flag = exit, somebody left the
     * conversation.
     */
    private void parseMessage(final String msg) {

        try {
            JSONObject jObj = new JSONObject(msg);

            // JSON node 'flag'
            String flag = jObj.getString("flag");

            // if flag is 'self', this JSON contains session id
            if (flag.equalsIgnoreCase(TAG_SELF)) {

                String sessionId = jObj.getString("sessionId");

                // Save the session id in shared preferences
                utils.storeSessionId(sessionId);

                Log.e(TAG, "Your session id: " + utils.getSessionId());

            } else if (flag.equalsIgnoreCase(TAG_NEW)) {
                // If the flag is 'new', new person joined the room
                String name = jObj.getString("name");
                String message = jObj.getString("message");

                // number of people online
                String onlineCount = jObj.getString("onlineCount");

                showToast(name + message + ". כרגע " + onlineCount
                        + " אנשים מחוברים!");

            } else if (flag.equalsIgnoreCase(TAG_MESSAGE)) {
                // if the flag is 'message', new message received
                String fromName = SharedPreferenceUtils.getInstance(this).getStringValue("name", "");
                String message = jObj.getString("message");
                String sessionId = jObj.getString("sessionId");
                boolean isSelf = true;

                // Checking if the message was sent by you
                if (!sessionId.equals(utils.getSessionId())) {
                    fromName = jObj.getString("name");
                    isSelf = false;
                }
               ((MyApplication) getApplication()).setIsSelf(isSelf);
                Message m = new Message(fromName, message, isSelf,tokens);

                // Appending the message to chat list
                appendMessage(m);

            } else if (flag.equalsIgnoreCase(TAG_EXIT)) {
                // If the flag is 'exit', somebody left the conversation
                String name = jObj.getString("name");
                String message = jObj.getString("message");

                showToast(name + message);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ((MyApplication) getApplication()).activityPaused();
        if (client != null && client.isConnected()) {
            client.disconnect();
        }
    }

    /**
     * Appending message to list view
     */
    private void appendMessage(final Message m) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                listMessages.add(m);
                outputToFile(m.toJsonString());
                Log.e(TAG, "massage: " +m.toJsonString());
                adapter.notifyDataSetChanged();
                // Playing device's notification
                playBeep(m.isSelf());
                if (!((MyApplication) getApplication()).isActivityVisible()) {
                    notifySendingMessage(m);

                }

            }
        });
    }

    private boolean outputToFile(String msgStr) {
        boolean isOk = true;
        File f = getFilesDir();
        String FileName = ((MyApplication) getApplication()).getSelectedGroupId() + ".txt";
        FileOutputStream fos;
        String seperation = "\n";
        String yourFilePath = null;
        try {
            yourFilePath = FileName;
            fos = openFileOutput(FileName, Context.MODE_APPEND);

            try {
                fos.write(msgStr.getBytes());
                fos.write(seperation.getBytes());
            } catch (IOException e) {
                isOk = false;
                e.printStackTrace();
            }
            try {
                fos.close();
            } catch (IOException e) {
                isOk = false;
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            isOk = false;
            e.printStackTrace();
        }

        return isOk;
    }

    private void addMessagesToArray(String ChatMessage , List<Message> listMessages) {

        try {
            String[] lines = ChatMessage.split(System.getProperty("line.separator"));
            StringBuilder sb = new StringBuilder();

            for (String line :lines) {
                sb.append(line);
                Gson gson = new Gson();
                Message msg = gson.fromJson(line, Message.class);
               String username = ((MyApplication) getApplication()).getUsername();
                if(msg.getFromName().equals(username))
                    msg.setSelf(true);
                listMessages.add(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void getChat(final String Room) {
        Thread getChatThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    WebService wsHttpRequest = new WebService("getChat");
                    String ChatMessages = wsHttpRequest.execute(Room);
                    addMessagesToArray(ChatMessages, listMessages);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        getChatThread.start();
        try {
            getChatThread.join();
        } catch (InterruptedException exception) {
            Log.e("MapsActivity", exception.getMessage());
        }
    }

    private void getTokens() {
        Thread getTokens = new Thread(new Runnable() {
            @Override
            public void run() {
                try {


                    WebService wsHttpRequest = new WebService("getGroupTokensWithOutTheSender");
                    tokens = wsHttpRequest.execute(users);
                    Log.e("MapsActivity","tokens:" + tokens);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        getTokens.start();
        try {
            getTokens.join();
        } catch (InterruptedException exception) {
            Log.e("MapsActivity", exception.getMessage());
        }
    }


    private void verifyThatHaveTheToken() {
        Thread verifyThatHaveTheToken = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    FirebaseInstanceIDService mfs= new FirebaseInstanceIDService();
                    mfs.onTokenRefresh();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        verifyThatHaveTheToken.start();
        try {
            verifyThatHaveTheToken.join();
        } catch (InterruptedException exception) {
            Log.e("MapsActivity", exception.getMessage());
        }
    }


    private void getGroupMemmbers(final String Room) {
        Thread getGroupMemmbers = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    WebService wsHttpRequest = new WebService("getGroupMembers");
                    String result = null;
                    result = wsHttpRequest.execute(((MyApplication) getApplication()).getSelectedGroupId());
                    Gson gson = new Gson();
                    usersGroup = gson.fromJson(result, new TypeToken<ArrayList<UserId>>() {
                    }.getType());
                    Log.e(TAG, "UsersGroup:" +  usersGroup);
                    RemoveCurrentUserFromUserIdGroup();
                    getUserNames();
                    Log.e(TAG, "Users:" + users);
                } catch (Exception e) {
                    Log.e("MapsActivity", e.getMessage());
                }
            }
        });

        getGroupMemmbers.start();
        try {
            getGroupMemmbers.join();
        } catch (InterruptedException exception) {
            Log.e("MapsActivity", exception.getMessage());
        }
    }

    private void RemoveCurrentUserFromUserIdGroup(){
       for(UserId user : usersGroup) {
           if(name.equals(user.getUsername())){
               usersGroup.remove(user);
               break;
           }
       }

    }

    private void getUserNames(){

        for(UserId user : usersGroup) {
            UserNames.add(user.getUsername());
        }
        Gson gson = new Gson();
        users = gson.toJson(UserNames);
    }

    private void showToast(final String message) {
        if (((MyApplication) getApplication()).isActivityVisible()) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), message,
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }


    /**
     * Plays device's default notification sound
     */
    public void playBeep(boolean isSelf) {

        try {
            Uri notification;
            if (isSelf) {
                MediaPlayer player = MediaPlayer.create(this, R.raw.chat_ringtone_pop);
                player.setLooping(false);
                player.start();
            } else {
                if (((MyApplication) getApplication()).isActivityVisible()) {
                    MediaPlayer player = MediaPlayer.create(this, R.raw.message_incoming);
                    player.setLooping(false);
                    player.start();
                } else {
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    MediaPlayer player = MediaPlayer.create(this, notification);
                    player.setLooping(false);
                    player.start();
                }
            }
        } catch (Exception e) {
            Log.e("ChatGroupActivity", e.getMessage());
        }
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    private void notifySendingMessage(Message message) {
        Intent i = getIntent();
        String name = message.getFromName();
        String text = message.getMessage();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.check_me_logo)
                .setContentTitle(mTitle)
                .setContentText(name + ": " + text).setAutoCancel(true);

        mNotificationId = (mNotificationId == MAX_NOTIFICATIONS) ? 1 : mNotificationId + 1;
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, mNotificationId, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_CANCEL_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }
}