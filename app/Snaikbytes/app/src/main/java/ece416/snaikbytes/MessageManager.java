package ece416.snaikbytes;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Created by mcoppola on 24/03/17.
 */

public class MessageManager {

    //Activity is used to allow threads to write to the UI
    Activity mActivity;
    WebSocketClient mWebSocketClient;

    public MessageManager(Activity activity)
    {
        mActivity = activity;
        ConnectWebSocket();
        //StartStatusThread();
    }

    private void ConnectWebSocket() {
        URI uri;
        try {
            uri = new URI("ws://ece416chat.herokuapp.com"); //could try :5000
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                //TODO handle
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
                Log.i("Websocket", "Message Recieved " + s);
                //TODO Implement
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





