package ece416.snaikbytes;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

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
        StartStatusThread();
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

    private void StatusTask()
    {
        URI uri;
        try {
            uri = new URI("ws://ece416chat.herokuapp.com/status");
        } catch(Exception e) {
            Log.d("Exceptions", "Error Malformed URL " + e);
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
            }

            @Override
            public void onMessage(String message) {
                Log.i("Websocket", message);

                if (message.equals("Success"))
                {
                    UpdateUIText("Status Up", R.id.statusText);
                } else {
                    UpdateUIText("Status Down", R.id.statusText);
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


}

