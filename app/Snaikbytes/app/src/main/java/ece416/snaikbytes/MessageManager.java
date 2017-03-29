package ece416.snaikbytes;

import android.app.Activity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by mcoppola on 24/03/17.
 */

public final class MessageManager implements Serializable {

    //Activity is used to allow threads to write to the UI
    static Activity mActivity;
    static WebSocketClient mWebSocketClient;
    static String userID;
    static String currentGroupID;
    static Vector<String> mActiveGroups;
    static boolean mShowStatus;

    static MessageManager self = null;

    //NOTE Need to initialize our singleton via Setters
    static MessageManager GetInstance() {
        if (self == null) {
            self = new MessageManager();
        }
        return self;
    }

    private MessageManager()
    {
        mActivity = null;
        ConnectWebSocket();
        this.userID = "";
        this.currentGroupID = "";
        StartStatusThread();
    }

    //Getters and Setters
    public static void SetActivity(Activity act) {
        mActivity = act;
    }

    public  static void SetUserId(String id) {
        userID = id;
    }

    public  static void SetGroupId(String id) {
        currentGroupID = id;
    }

    public  static void SetCheckStatus(Boolean checkStatus) {
        mShowStatus = checkStatus;
    }


    private void ConnectWebSocket() {
        URI uri;
        try {
            uri = new URI("ws://ece416chat.herokuapp.com/");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                if (mActivity.toString().contains("MainActivity")) {
                    GetGroupList();
                }
                UpdateUIText("Status Up", R.id.statusText);
                Register();
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
                Log.i("Websocket", "Message Recieved " + s);

                try {
                    ParseJSon(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
                UpdateUIText("Status Down", R.id.statusText);
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
            default:
                Log.i("Websocket", "Ack received");
        }
    }

    private void Register() {
        JSONObject request = new JSONObject();
        try {
            request.put("action", "register");
            request.put("user_id", userID);
            SendJSON(request);
            Log.i("Websocket", "Registering user " + userID);
        } catch (JSONException e) {
            Log.d("Exceptions", "JSON Error " + e);
        }
    }

    private void SendJSON(JSONObject json)
    {
        if (mWebSocketClient.getReadyState() == WebSocket.READYSTATE.OPEN) {
            mWebSocketClient.send(json.toString().getBytes(StandardCharsets.UTF_8));
        } else {
            //TODO implement message buffering
        }
    }

    public void JoinGroup() {
        JSONObject jsonMessage = new JSONObject();
        try {
            jsonMessage.put("action", "join_group");
            jsonMessage.put("user_id", userID);
            jsonMessage.put("group_id", currentGroupID);
            SendJSON(jsonMessage);
        } catch (JSONException e) {
            Log.d("Exceptions", "JSON Error " + e);
        }
    }

    public void SendMessage(String message)
    {
        JSONObject jsonMessage = new JSONObject();
        try {
            jsonMessage.put("action", "message");
            jsonMessage.put("user_id", userID);
            jsonMessage.put("group_id", currentGroupID);
            jsonMessage.put("message", message);
            SendJSON(jsonMessage);
        } catch (JSONException e) {
            Log.d("Exceptions", "JSON Error " + e);
        }
    }

    public void GetGroupList()
    {
        JSONObject request = new JSONObject();
        try {
            request.put("action", "list_groups");
            SendJSON(request);
        } catch (JSONException e) {
            Log.d("Exceptions", "JSON Error " + e);
        }
    }

    public void GetGroupInfo()
    {
        JSONObject request = new JSONObject();
        try {
            request.put("action", "list_group_users");
            request.put("group_id", currentGroupID);
            SendJSON(request);
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
            request.put("group_id", currentGroupID);
            SendJSON(request);
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
            request.put("group_id", currentGroupID);
            SendJSON(request);
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

    private void StatusTask()
    {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            Log.d("Exceptions", "Thread Interrupt Exception, not sleeping " + e);
        }

        while(true)
        {
            Log.i("Websocket", "Socket State " + mWebSocketClient.getReadyState());
            if (mShowStatus) {
                if (mWebSocketClient.getReadyState().equals(WebSocket.READYSTATE.OPEN)) {
                    UpdateUIText("Status Up", R.id.statusText);
                } else {
                    UpdateUIText("Status Down", R.id.statusText);
                    ConnectWebSocket();
                }
            }

            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                Log.d("Exceptions", "Thread Interrupt Exception, not sleeping " + e);
            }
        }
    }

}





