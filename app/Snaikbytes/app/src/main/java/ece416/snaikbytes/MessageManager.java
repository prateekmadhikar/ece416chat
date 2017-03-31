package ece416.snaikbytes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.ArrayMap;
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
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by mcoppola on 24/03/17.
 */

public final class MessageManager implements Serializable {

    //Activity is used to allow threads to write to the UI
    static Activity mActivity;
    static WebSocketClient mWebSocketClient;
    static String userID;
    static String currentGroupID;
    static boolean mShowStatus;
    static  boolean mShowMessages;
    static ArrayBlockingQueue<JSONObject> mMessageQueue;
    static ArrayMap<String, ChatGroup> mGroupChats;
    static boolean mMessageWaitingAck;
    static Vector<String> mActiveGroups;

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
        mMessageQueue = new ArrayBlockingQueue<JSONObject>(100);
        mGroupChats = new ArrayMap<>();
        mActiveGroups = new Vector<String>();
        mMessageWaitingAck = false;
        StartStatusThread();
    }

    public static String GetGroupID()
    {
        return currentGroupID;
    }

    //Getters and Setters
    public static void SetActivity(Activity act) {
        mActivity = act;
    }

    public static void SetUserId(String id) {
        userID = id;
    }

    public static void SetGroupId(String id) {
        currentGroupID = id;
    }

    public static void SetCheckStatus(Boolean checkStatus) {
        mShowStatus = checkStatus;
    }

    public static void SetShowMessage(Boolean show) {
        mShowMessages = show;
    }

    private void AddToActiveGroups(String group)
    {
        for (String x : mActiveGroups) {
            if (x.equals(group))
            {
                return;
            }
        }
        mActiveGroups.add(group);
    }

    private void RemoveFromActiveGroups(String group)
    {
        int index = 0;
        for (String x : mActiveGroups) {
            if(x.equals(group))
            {
                mActiveGroups.remove(index);
                return;
            }
            index++;
        }
    }

    private void JoinActiveGroups()
    {
        for (String group : mActiveGroups) {
            JoinGroup(group);
        }
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
                JoinActiveGroups();
                FlushMessageQueue();
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

    private void SendJSON(JSONObject json)
    {
        if (mWebSocketClient.getReadyState() == WebSocket.READYSTATE.OPEN) {
            mWebSocketClient.send(json.toString().getBytes(StandardCharsets.UTF_8));
        } else {
            Log.i("Websocket", "Buffering Messages");
            mMessageQueue.add(json);
        }
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
                AlertNewMessage(jsonData);
                break;
            case "error":
                Log.i("Websocket", "Error from server ");
                UpdateMessageStatus(type);
                break;
            default:
                Log.i("Websocket", "Ack received");
                UpdateMessageStatus(type);
        }
    }

    private void UpdateMessageStatus(String type)
    {
        if(mShowMessages && mMessageWaitingAck)
        {
            mMessageWaitingAck = false;
            String status = "Failure";
            if(type.equals("ack"))
            {
                status = "Success";
                UpdateMessageUI();
            }
            UpdateUIText("Message Status: " + status, R.id.messageStatusText);
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

    public void JoinGroup(String group) {
        JSONObject jsonMessage = new JSONObject();
        try {
            jsonMessage.put("action", "join_group");
            jsonMessage.put("user_id", userID);
            jsonMessage.put("group_id", group);
            SendJSON(jsonMessage);
            AddToActiveGroups(group);
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

        RemoveFromActiveGroups(currentGroupID);

        if(mGroupChats.containsKey(currentGroupID))
        {
            mGroupChats.remove(currentGroupID);
        }
        Log.i("Websocket", "Leaving group " + currentGroupID);
    }

    private void FlushMessageQueue()
    {
        while(mMessageQueue.size() > 0)
        {
            SendJSON(mMessageQueue.remove());
            //TODO Consider a delay if constant sending is no good
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

    public void GetGroupUsers()
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

    private void ShowNotification(String user, String group, String message)
    {
        class threadStatusMessenger implements Runnable {
            Activity mActivity;
            String mUser;
            String mGroup;
            String mMsg;

            threadStatusMessenger(String user, String msg, String group, Activity activity) {
                mActivity = activity;
                mUser = user;
                mGroup = group;
                mMsg = msg;
            }

            public void run() {
                AlertDialog alertDialog = new AlertDialog.Builder(mActivity).create();
                alertDialog.setTitle(mUser + " message group " + mGroup);
                alertDialog.setMessage(mUser + ": " + mMsg);
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        }
        mActivity.runOnUiThread(new threadStatusMessenger(user, group, message, mActivity));
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

    private void AddToChatMap(String group, String user, String message)
    {
        //Store chat in group chat
        if(!mGroupChats.containsKey(group))
        {
            mGroupChats.put(group, new ChatGroup(group));
        }
        mGroupChats.get(group).AddChat(user, message);
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
            AddToChatMap(currentGroupID, userID, message);
            mMessageWaitingAck = true;
        } catch (JSONException e) {
            Log.d("Exceptions", "JSON Error " + e);
        }
    }

    public void AlertNewMessage(JSONObject json)
    {
        try {
            String group = json.getString("group_id");
            String user = json.getString("from");
            String message = json.getString("message");

            AddToChatMap(group, user, message);

            if(!currentGroupID.equals(group)) {
                ShowNotification(user, group, message);
            }

            if(currentGroupID.equals(group) && mShowMessages)
            {
                UpdateMessageUI();
            }

        } catch(JSONException e) {
            Log.i("Websocket", "JSON Error parsing new message " + e);
        }
    }

    public  void UpdateMessageUI()
    {
        String messageString = "";
        if (mGroupChats.containsKey(currentGroupID)) {
            Vector<Message> messages = mGroupChats.get(currentGroupID).GetMessages();
            for (Message msg : messages) {
                messageString += msg.GetUser() + ": " + msg.GetMessage() + "\n\n";
            }
        }
        UpdateUIText(messageString, R.id.message);
    }

    private void StartStatusThread()
    {
        new Thread(new Runnable() {
            public void run() {
                StatusTask();
            }
        }).start();
    }

    //There is a lag updating the status between activities,
    //This method allows us to manually trigger an update from the main (UI) thread
    public void CheckStatus()
    {
        TextView msgText = (TextView) mActivity.findViewById(R.id.statusText);
        if (mWebSocketClient.getReadyState().equals(WebSocket.READYSTATE.OPEN)) {
            msgText.setText("Status Up");
        } else {
            msgText.setText("Status Down");
            ConnectWebSocket();
        }
    }

    private void SendKeepAlive()
    {
        JSONObject jsonMessage = new JSONObject();
        try {
            jsonMessage.put("action", "keep_alive");
            SendJSON(jsonMessage);
        } catch (JSONException e) {
            Log.d("Exceptions", "JSON Error " + e);
        }
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
            Log.i("Status", "Socket State " + mWebSocketClient.getReadyState());
            if (mShowStatus) {
                if (mWebSocketClient.getReadyState().equals(WebSocket.READYSTATE.OPEN)) {
                    UpdateUIText("Status Up", R.id.statusText);
                    SendKeepAlive();
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





