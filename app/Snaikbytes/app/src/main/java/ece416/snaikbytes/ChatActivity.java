package ece416.snaikbytes;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.io.Serializable;

public class ChatActivity extends AppCompatActivity implements Serializable {

    MessageManager mMessageManager;
    String groupID;
    String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();

        if (bundle != null) {
            if (bundle.containsKey("userID")) {
                userID = (String) bundle.getSerializable("userID");
            }
            if (bundle.containsKey("groupID")) {
                groupID = (String) bundle.getSerializable("groupID");
            }
        }

        mMessageManager = new MessageManager(this, userID, groupID);
    }

    public void showGroup(View view) {
        mMessageManager.GetGroupUsers();
    }

    public void joinGroup(View view) {
        mMessageManager.JoinGroup();
    }

    public void quitGroup(View view) {
        mMessageManager.LeaveGroup();
    }

    public void sendMessage(View view) {
        // TODO: Send the message
    }


}
