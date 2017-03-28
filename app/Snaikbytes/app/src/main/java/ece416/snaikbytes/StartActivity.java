package ece416.snaikbytes;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import java.io.Serializable;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
    }

    public void startMainActivity(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        Bundle bundle = new Bundle();

        EditText name = (EditText) findViewById(R.id.name);
        String userID = name.getText().toString();

        bundle.putSerializable("userID", userID);
        intent.putExtras(bundle);

        startActivity(intent);
    }
}
