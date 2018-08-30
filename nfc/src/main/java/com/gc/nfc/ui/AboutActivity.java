package com.gc.nfc.ui;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Intent;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.gc.nfc.R;
import android.content.Context;
import android.os.Bundle;


public class AboutActivity extends Activity {

	TextView tvShowVersion;
	AlertDialog loadingDialog;
	private String applicationVersion;

	public static void actionStart(Context context) {
		Intent intent = new Intent(context, AboutActivity.class);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);


//		Intent intent = getIntent();
//		if(intent!=null){
//			applicationVersion = intent.getStringExtra("applicationVersion");
//		}
//		tvShowVersion = (TextView) findViewById(R.id.tv_about_us_show_version);
//		loadingDialog = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).create();
//
//		tvShowVersion.append(applicationVersion);
	}

	public void update(View view) {
		//Toast.makeText(this, "已是最新版", Toast.LENGTH_SHORT).show();
	}

	public void boot(View view) {
	}
}
