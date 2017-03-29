package ece416.snaikbytes;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.Serializable;

public class MainActivity extends AppCompatActivity implements Serializable {

    String userID;
    ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();

        if (bundle != null) {
            if (bundle.containsKey("userID")) {
                userID = (String) bundle.getSerializable("userID");
            }
        }

        MessageManager.GetInstance().SetActivity(this);
        MessageManager.GetInstance().SetUserId(userID);
        MessageManager.GetInstance().SetCheckStatus(true);

        lv = (ListView) this.findViewById(R.id.listOfGroups);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Object o = lv.getItemAtPosition(i);
                String groupID = o.toString();
                MessageManager.GetInstance().SetGroupId(groupID);
                startChatActivity(groupID);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        MessageManager.GetInstance().SetActivity(this);
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        MessageManager.GetInstance().SetCheckStatus(false);
    }

    public void startChatActivity(String groupID) {
        Intent intent = new Intent(this, ChatActivity.class);

        Bundle bundle = new Bundle();

        bundle.putSerializable("userID", userID);
        intent.putExtras(bundle);

        bundle.putSerializable("currentGroupID", groupID);
        intent.putExtras(bundle);

        startActivity(intent);
    }
}
