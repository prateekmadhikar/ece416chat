package ece416.snaikbytes;

import android.app.Activity;
import android.util.Log;
import android.widget.EditText;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by mcoppola on 24/03/17.
 */

public class MessageManager {

    //Activity is used to allow threads to write to the UI
    Activity mActivity;

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
                EditText msgText = (EditText) mActivity.findViewById(mViewId);
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

