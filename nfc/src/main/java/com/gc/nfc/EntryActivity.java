package com.gc.nfc;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Button;
import com.gc.nfc.R;

/**
 * Created by gc on 2016/12/8.
 */
public class EntryActivity extends AppCompatActivity {
    private static final String[] strs = new String[]{
            "nfc标签的简单测试（百江燃气）"
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        Button startButton = (Button)findViewById(R.id.start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchActivity();
            }
        });
    }

    private void switchActivity() {
        startActivity(new Intent(this, MainActivity.class));

    }
}
