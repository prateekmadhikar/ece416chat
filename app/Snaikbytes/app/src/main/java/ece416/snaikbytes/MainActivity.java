package ece416.snaikbytes;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    MessageManager mMessageManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMessageManager = new MessageManager(this);
    }

    public void startChatActivity(View view) {
        Intent intent = new Intent(this, ChatActivity.class);
        startActivity(intent);
    }

    public void ListGroups(View view)
    {
        new Thread(new Runnable() {
            public void run() {
                mMessageManager.GetGroupList();
            }
        }).start();
    }
}
