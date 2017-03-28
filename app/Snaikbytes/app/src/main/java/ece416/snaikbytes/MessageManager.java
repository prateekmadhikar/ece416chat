package ece416.snaikbytes;

import android.app.Activity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Created by mcoppola on 24/03/17.
 */

public class MessageManager implements Serializable {

    //Activity is used to allow threads to write to the UI
    Activity mActivity;
    WebSocketClient mWebSocketClient;
    String userID;
    String groupID;

    public MessageManager(Activity activity, String userID, String groupID)
    {
        mActivity = activity;
        ConnectWebSocket();
        this.userID = userID;
        this.groupID = groupID;
        //StartStatusThread();
    }

    private void ConnectWebSocket() {
        URI uri;
        try {
            uri = new URI("ws://ece416chat.herokuapp.com/"); //could try :5000
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                //TODO handle
                Register();

                if (mActivity.toString().contains("MainActivity")) {
                    GetGroupList();
                }
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
                Log.i("Websocket", "Message Recieved " + s);
                //TODO Implement
                try {
                    ParseJSon(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
    }

    private void ParseJSon(String data) throws JSONException
    {
        if (data == null)
            return;

        JSONObject jsonData = new JSONObject(data);
        String type = jsonData.getString("type");

        switch(type) {
            case "list_groups":
                UpdateGroups(data);
                break;
            case "list_group_users":
                ListUsers();
                break;
            case "message":
                AlertNewMessage();
                break;
            case "error":
                Log.i("Websocket", "Error from server ");
                break;
            default :
                Log.i("Websocket", "Ack received");
        }
    }

    public void Register()
    {
        JSONObject request = new JSONObject();
        try {
            request.put("action", "register");
            request.put("user_id", userID);
            mWebSocketClient.send(request.toString().getBytes(StandardCharsets.UTF_8));
            Log.d("Websocket", "Registered");
        } catch (JSONException e) {
            Log.d("Exceptions", "JSON Error " + e);
        }
    }

    public void GetGroupList()
    {
        JSONObject request = new JSONObject();
        try {
            request.put("action", "list_groups");
            mWebSocketClient.send(request.toString().getBytes(StandardCharsets.UTF_8));
        } catch (JSONException e) {
            Log.d("Exceptions", "JSON Error " + e);
        }
    }

    public void GetGroupUsers()
    {
        JSONObject request = new JSONObject();
        try {
            request.put("action", "list_group_users");
            request.put("user_id", userID);
            request.put("group_id", groupID);
            mWebSocketClient.send(request.toString().getBytes(StandardCharsets.UTF_8));
        } catch (JSONException e) {
            Log.d("Exceptions", "JSON Error " + e);
        }
    }

    public void JoinGroup()
    {
        JSONObject request = new JSONObject();
        try {
            request.put("action", "join_group");
            request.put("user_id", userID);
            request.put("group_id", groupID);
            mWebSocketClient.send(request.toString().getBytes(StandardCharsets.UTF_8));
        } catch (JSONException e) {
            Log.d("Exceptions", "JSON Error " + e);
        }
    }

    public void LeaveGroup()
    {
        JSONObject request = new JSONObject();
        try {
            request.put("action", "leave_group");
            request.put("user_id", userID);
            request.put("group_id", groupID);
            mWebSocketClient.send(request.toString().getBytes(StandardCharsets.UTF_8));
        } catch (JSONException e) {
            Log.d("Exceptions", "JSON Error " + e);
        }
    }

    /*
    * We can't block the UI thread so we need a method to allow other threads to update the UI
    * Takes a string and a testview and posts a runable to update the ui.
    */
    public void UpdateUIText(String msg, final int viewId)
    {
        class threadStatusMessenger implements Runnable {
            String mMsg;
            Activity mActivity;
            int mViewId;

            threadStatusMessenger(String msg, Activity activity, int viewId) {
                mMsg = msg;
                mActivity = activity;
                mViewId = viewId;
            }

            public void run() {
                TextView msgText = (TextView) mActivity.findViewById(mViewId);
                msgText.setText(mMsg);
            }
        }
        mActivity.runOnUiThread(new threadStatusMessenger(msg, mActivity, viewId));
    }

    public void UpdateGroups(String data)
    {
        class threadStatusMessenger implements Runnable {
            String data;
            Activity mActivity;

            threadStatusMessenger(String data, Activity activity) {
                this.data = data;
                this.mActivity = activity;
            }

            public void run() {
                ArrayList<String> groups = new ArrayList<String>();

                try {
                    JSONObject jsonData = new JSONObject(data);
                    JSONArray jsonGroups = jsonData.getJSONArray("groups");

                    for (int i = 0; i < jsonGroups.length(); i++) {
                        JSONObject jsonGroup = jsonGroups.getJSONObject(i);
                        String groupId = jsonGroup.getString("group_id");
                        groups.add(groupId);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                        mActivity,
                        android.R.layout.simple_list_item_1,
                        groups );

                ListView lv = (ListView) mActivity.findViewById(R.id.listOfGroups);
                lv.setAdapter(arrayAdapter);
            }
        }

        mActivity.runOnUiThread(new threadStatusMessenger(data, mActivity));
    }

    public void ListUsers()
    {
        // TODO: Parse and display users
    }

    public void AlertNewMessage ()
    {
        // TODO: implement
    }

    private void StartStatusThread()
    {
        new Thread(new Runnable() {
            public void run() {
                StatusTask();
            }
        }).start();
    }

    private String ReadStream(InputStream stream)
    {
        try {
            ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
            int dataByte = stream.read();
            while(dataByte != -1) {
                outputBytes.write(dataByte);
                dataByte = stream.read();
            }
            return outputBytes.toString();
        } catch (IOException e) {
            return "";
        }
    }

    private void StatusTask()
    {
        URL url;
        try {
            url = new URL("https://ece416chat.herokuapp.com/status");
        } catch(Exception e) {
            Log.d("Exceptions", "Error Malformed URL " + e);
            return;
        }

        while(true)
        {
            String response = "";

            try {
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    response = ReadStream(in);
                } catch (Exception e) {
                    Log.d("Exceptions", "Error Reading Server Response " + e);
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.d("Exceptions", "Error Initiating Connection with Server " + e);
            }

            if (response.equals("Success"))
            {
                UpdateUIText("Status Up", R.id.statusText);
            } else {
                UpdateUIText("Status Down", R.id.statusText);
            }

            try {
                Thread.sleep(4500);
            } catch (Exception e) {
                Log.d("Exceptions", "Thread Interrupt Exception, not sleeping " + e);
            }
        }
    }

}





