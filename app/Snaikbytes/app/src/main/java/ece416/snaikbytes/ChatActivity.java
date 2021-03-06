package ece416.snaikbytes;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.Serializable;

public class ChatActivity extends AppCompatActivity implements Serializable {

    String groupID;
    String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        MessageManager.GetInstance().SetActivity(this);
        MessageManager.GetInstance().SetCheckStatus(true);
        MessageManager.GetInstance().SetShowMessage(true);

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();

        if (bundle != null) {
            if (bundle.containsKey("userID")) {
                userID = (String) bundle.getSerializable("userID");
            }
            if (bundle.containsKey("currentGroupID")) {
                groupID = (String) bundle.getSerializable("currentGroupID");
            }
        }

        MessageManager.GetInstance().CheckStatus();
        SetGroupIDText();
        MessageManager.GetInstance().UpdateMessageUI();
        MessageManager.GetInstance().JoinGroup(MessageManager.GetInstance().GetGroupID());
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        MessageManager.GetInstance().SetActivity(this);
        MessageManager.GetInstance().SetCheckStatus(true);
        MessageManager.GetInstance().SetShowMessage(true);
        SetGroupIDText();
        MessageManager.GetInstance().UpdateMessageUI();
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        MessageManager.GetInstance().SetCheckStatus(false);
        MessageManager.GetInstance().SetShowMessage(false);

    }

    public void SetGroupIDText()
    {
        TextView groupText = (TextView) findViewById(R.id.currentGroupID);
        groupText.setText("Group ID is: " + MessageManager.GetInstance().GetGroupID());
    }

    public void showGroup(View view) {
        MessageManager.GetInstance().GetGroupUsers();
    }

    public void joinGroup(View view) {
        MessageManager.GetInstance().JoinGroup(MessageManager.GetInstance().GetGroupID());
    }

    public void quitGroup(View view) {
        MessageManager.GetInstance().LeaveGroup();
    }

    public void sendMessage(View view) {
        EditText messageText = ((EditText) findViewById(R.id.newMessage));
        MessageManager.GetInstance().SendMessage(messageText.getText().toString());
        messageText.setText("");
    }


}
