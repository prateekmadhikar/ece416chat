package ece416.snaikbytes;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.Serializable;

public class MainActivity extends AppCompatActivity implements Serializable {

    MessageManager mMessageManager;
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

        mMessageManager = new MessageManager(this, userID, null);

        lv = (ListView) this.findViewById(R.id.listOfGroups);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Object o = lv.getItemAtPosition(i);
                String groupID = o.toString();
                startChatActivity(groupID);
            }
        });
    }

    public void startChatActivity(String groupID) {
        Intent intent = new Intent(this, ChatActivity.class);

        Bundle bundle = new Bundle();

        bundle.putSerializable("userID", userID);
        intent.putExtras(bundle);

        bundle.putSerializable("groupID", groupID);
        intent.putExtras(bundle);

        startActivity(intent);
    }
}
