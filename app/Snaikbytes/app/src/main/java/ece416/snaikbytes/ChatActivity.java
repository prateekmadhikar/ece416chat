package ece416.snaikbytes;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class ChatActivity extends AppCompatActivity {
    private MessageManager mMessageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mMessageManager = new MessageManager(this);
    }

    public void showGroup(View view) {
        // TODO: Show the group members
    }

    public void joinGroup(View view) {
        // TODO: Join the chat group
    }

    public void quitGroup(View view) {
        // TODO: Quit the chat group
    }

    public void sendMessage(View view) {
        // TODO: Send the message
    }


}
