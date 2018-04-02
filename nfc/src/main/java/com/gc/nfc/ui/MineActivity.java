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
//		switch (v.getId()) {
//		case R.id.iV_mine1:// 美团通知
//			Intent intent = new Intent(MineActivity.this, NotificationActivity.class);
//			startActivity(intent);
//
//			break;
//		case R.id.lL_mine:
//		case R.id.button_register:// 登录
//			Intent intent1 = new Intent(MineActivity.this, LoginActivity.class);
//			startActivityForResult(intent1, 12);
//
//			break;
//		case R.id.rL_mine:// 我的美团卷
//			if (user != null) {
//				Intent intent2 = new Intent(MineActivity.this, TicketActivity.class);
//				startActivity(intent2);
//			} else {
//				Toast.makeText(this, "亲，请先登录", Toast.LENGTH_SHORT).show();
//			}
//
//			break;
//		case R.id.rL_mine1:
//			if (user != null) {
//				Intent intent3 = new Intent(MineActivity.this,
//						CollectActivity.class);
//				startActivity(intent3);
//			} else {
//				Toast.makeText(this, "亲，请先登录", Toast.LENGTH_SHORT).show();
//			}
//
//			break;
//		case R.id.lL_mine3:// 每日推荐
//			Intent intent4 = new Intent(MineActivity.this,
//					RecommendedActivity.class);
//			startActivity(intent4);
//			break;
//		case R.id.lL_mine4:
//			if (user != null) {
//				Intent intent5 = new Intent(MineActivity.this,
//						ObligationActivity.class);
//				startActivity(intent5);
//			} else {
//				Toast.makeText(this, "亲，请先登录", Toast.LENGTH_SHORT).show();
//			}
//
//			break;
//		case R.id.lL_mine5:
//			if (user != null) {
//				Intent intent6 = new Intent(MineActivity.this, PaidActivity.class);
//				startActivity(intent6);
//			} else {
//				Toast.makeText(this, "亲，请先登录", Toast.LENGTH_SHORT).show();
//			}
//			break;
//		case R.id.lL_mine6:// 抽奖单
//			if (user != null) {
//				Intent intent7 = new Intent(MineActivity.this,
//						LotteryActivity.class);
//				startActivity(intent7);
//			} else {
//				Toast.makeText(this, "亲，请先登录", Toast.LENGTH_SHORT).show();
//			}
//			break;
//		case R.id.lL_mine7:
//			Intent intent8 = new Intent(MineActivity.this,
//					VoucherActivity.class);
//			startActivity(intent8);
//			break;
//		case R.id.lL_mine2:
//			startActivityForResult(new Intent(this, AccountActivity.class), 11);
//			break;
//		default:
//			break;
//		}

	}

	@Override
	void init() {
		setContentView(R.layout.activity_mine);
		iv_mine1 = (ImageView) findViewById(R.id.iV_mine1);

		lL_myBottle = (LinearLayout) findViewById(R.id.lL_myBottle);// 我的气瓶
		lL_myHistoryOrders = (LinearLayout) findViewById(R.id.lL_myOrder);//历史订单
		textview_username = (TextView) findViewById(R.id.textview_username);

		iv_mine1.setOnClickListener(this);
		lL_myBottle.setOnClickListener(this);
		lL_myBottle.setOnClickListener(this);

	}
}
