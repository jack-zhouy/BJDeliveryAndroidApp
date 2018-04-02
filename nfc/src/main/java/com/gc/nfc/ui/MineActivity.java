package com.gc.nfc.ui;

import com.gc.nfc.R;
import com.gc.nfc.app.AppContext;
import com.gc.nfc.domain.User;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MineActivity extends BaseActivity implements OnClickListener {

	private ImageView iv_mine1;// 通知
	private LinearLayout lL_myBottle;// 我的气瓶
	private LinearLayout lL_myHistoryOrders;//历史订单

	private TextView textview_username;
	private AppContext appContext;
	private User user;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		appContext = (AppContext) getApplicationContext();
		user = appContext.getUser();
		if (user != null) {
			String username = user.getUsername();
			textview_username.setText(username);
		}
	}

	public void onClick(View v) {
		// TODO Auto-generated method stub
		Intent intent;
		switch (v.getId()) {
			case R.id.lL_myHistoryOrders:// 历史订单
				intent = new Intent(MineActivity.this, HistoryOrdersActivity.class);
				startActivity(intent);
				break;
			case R.id.lL_myBottle:// 我的气瓶
				intent = new Intent(MineActivity.this, MybottlesActivity.class);
				startActivity(intent);
				break;
			default:
				break;
		}

	}

	@Override
	void init() {
		setContentView(R.layout.activity_mine);
		iv_mine1 = (ImageView) findViewById(R.id.iV_mine1);

		lL_myBottle = (LinearLayout) findViewById(R.id.lL_myBottle);// 我的气瓶
		lL_myHistoryOrders = (LinearLayout) findViewById(R.id.lL_myHistoryOrders);//历史订单
		textview_username = (TextView) findViewById(R.id.textview_username);


		lL_myBottle.setOnClickListener(this);
		lL_myHistoryOrders.setOnClickListener(this);

	}
}
