package com.gc.nfc.ui;

import android.app.Activity;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.view.Window;
import android.view.WindowManager;
import android.view.Gravity;
import android.os.PowerManager;
import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;


public class OnePiexlActivity extends Activity {

	private BroadcastReceiver endReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//设置1像素
		Window window = getWindow();
		window.setGravity(Gravity.LEFT | Gravity.TOP);
		WindowManager.LayoutParams params = window.getAttributes();
		params.x = 0;
		params.y = 0;
		params.height = 1;
		params.width = 1;
		window.setAttributes(params);

		//结束该页面的广播
		endReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				finish();
			}
		};
		registerReceiver(endReceiver, new IntentFilter("finish"));
		//检查屏幕状态
		checkScreen();
	}

	@Override
	protected void onResume() {
		super.onResume();
		checkScreen();
	}

	/**
	 * 检查屏幕状态  isScreenOn为true  屏幕“亮”结束该Activity
	 */
	private void checkScreen() {

		PowerManager pm = (PowerManager) OnePiexlActivity.this.getSystemService(Context.POWER_SERVICE);
		boolean isScreenOn = pm.isScreenOn();
		if (isScreenOn) {
			finish();
		}
	}
}
