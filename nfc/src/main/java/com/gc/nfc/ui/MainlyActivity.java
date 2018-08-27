package com.gc.nfc.ui;

import com.amap.api.maps.model.LatLng;
import com.gc.nfc.R;
import com.gc.nfc.app.AppContext;
import com.gc.nfc.common.NetRequestConstant;
import com.gc.nfc.common.NetUrlConstant;
import com.gc.nfc.domain.User;
import com.gc.nfc.interfaces.Netcallback;
import com.gc.nfc.receiver.LocationReceiver;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.app.TabActivity;
import android.content.Intent;
import android.provider.Settings;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.nfc.utils.LocationUtils;
import com.gc.nfc.utils.AmapLocationService;
import com.gc.nfc.utils.NetUtil;
import com.gc.nfc.utils.SharedPreferencesHelper;

import android.view.KeyEvent;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import android.os.Handler;
import android.app.Notification;



public class MainlyActivity extends TabActivity implements OnClickListener {

	private TabHost host;
	private final static String VALIDORDERS_STRING = "VALIDORDERS_STRING";//待抢订单
	private final static String MYORDERS_STRING = "MYORDERS_STRING";//我的订单
	private final static String MYSELF_STRING = "MYSELF_STRING";//我的
	private final static String MORE_STRING = "MORE_STRING";//更多
	private ImageView img_validorders;
	private ImageView img_myorders;
	private ImageView img_mine;
	private ImageView img_more;
	private TextView  text_validorders;
	private TextView  text_myorders;
	private TextView  text_mine;
	private TextView  text_more;
	private LinearLayout linearlayout_validorders;
	private LinearLayout linearlayout_myorders;
	private LinearLayout linearlayout_mine;
	private LinearLayout linearlayout_more;

	// 用来计算返回键的点击间隔时间
	private long exitTime = 0;


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mainly);
		this.getScreenDisplay();

		this.initView();
		host = getTabHost();
		host.setup();
		setMyselfTab();
		setValidOrdersTab();
		setMyOrdersTab();

//		setMoreTab();
		Bundle bundle = this.getIntent().getExtras();
		if(bundle==null){
			host.setCurrentTabByTag(MYSELF_STRING);//默认我的界面
		}else {
			int  switchTab = bundle.getInt("switchTab");
			if(switchTab==1){
				host.setCurrentTabByTag(MYORDERS_STRING);//我的订单
			}else{
				host.setCurrentTabByTag(MYSELF_STRING);//默认我的界面
			}
		}
		//开启定位任务
		isOpenGPS();
		//开启定位


		final Intent intentService = new Intent(this,AmapLocationService.class);
		startService(intentService);
		final Intent intentServiceWatch = new Intent(this,com.gc.nfc.utils.RomoteService.class);
		startService(intentServiceWatch);


//		LocationUtils.setInstance(this);
//		LocationUtils.isOpenGPS();
//		LocationUtils.getStatusListener();
//		LocationReceiver.startLocation(this);

	}


	public void initView(){
		img_validorders=(ImageView) findViewById(R.id.img_vaildorders);
		img_myorders=(ImageView) findViewById(R.id.img_myorders);
		img_mine=(ImageView) findViewById(R.id.img_mine);
//		img_more=(ImageView) findViewById(R.id.img_more);

		img_validorders.setOnClickListener(this);
		img_myorders.setOnClickListener(this);
		img_mine.setOnClickListener(this);
//		img_more.setOnClickListener(this);

		text_validorders=(TextView) findViewById(R.id.text_vaildorders);
		text_myorders=(TextView) findViewById(R.id.text_myorders);
		text_mine=(TextView) findViewById(R.id.text_mine);
//		text_more=(TextView) findViewById(R.id.text_more);

		linearlayout_validorders=(LinearLayout) findViewById(R.id.linearlayout_vaildorders);
		linearlayout_myorders=(LinearLayout) findViewById(R.id.linearlayout_myorders);
		linearlayout_mine=(LinearLayout) findViewById(R.id.linearlayout_mine);
//		linearlayout_more=(LinearLayout) findViewById(R.id.linearlayout_more);

		linearlayout_validorders.setOnClickListener(this);
		linearlayout_myorders.setOnClickListener(this);
		linearlayout_mine.setOnClickListener(this);
//		linearlayout_more.setOnClickListener(this);
	}

	private void setValidOrdersTab() {
		TabSpec tabSpec = host.newTabSpec(VALIDORDERS_STRING);
		tabSpec.setIndicator(VALIDORDERS_STRING);
		Intent intent = new Intent(MainlyActivity.this,ValidOrdersActivity.class);
		tabSpec.setContent(intent);
		host.addTab(tabSpec);
	}

	private void setMyOrdersTab() {
		TabSpec tabSpec = host.newTabSpec(MYORDERS_STRING);
		tabSpec.setIndicator(MYORDERS_STRING);
		Intent intent = new Intent(MainlyActivity.this,MyOrdersActivity.class);
		tabSpec.setContent(intent);
		host.addTab(tabSpec);
	}

	private void setMyselfTab() {
		TabSpec tabSpec = host.newTabSpec(MYSELF_STRING);
		tabSpec.setIndicator(MYSELF_STRING);
		Intent intent = new Intent(MainlyActivity.this, MineActivity.class);
		tabSpec.setContent(intent);
		host.addTab(tabSpec);
	}

//	private void setMoreTab() {
//		TabSpec tabSpec = host.newTabSpec(MORE_STRING);
//		tabSpec.setIndicator(MORE_STRING);
//		Intent intent = new Intent(MainActivity.this, MoreActivity.class);
//		tabSpec.setContent(intent);
//		host.addTab(tabSpec);
//
//	}

	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.linearlayout_vaildorders:
			case R.id.img_vaildorders:
				host.setCurrentTabByTag(VALIDORDERS_STRING);
				img_validorders.setBackgroundResource(R.drawable.ic_menu_deal_on);
				text_validorders.setTextColor(getResources().getColor(R.color.green));
				img_myorders.setBackgroundResource(R.drawable.ic_menu_poi_off);
				text_myorders.setTextColor(getResources().getColor(R.color.textgray));
				img_mine.setBackgroundResource(R.drawable.ic_menu_user_off);
				text_mine.setTextColor(getResources().getColor(R.color.textgray));
//				img_more.setBackgroundResource(R.drawable.ic_menu_more_off);
//				text_more.setTextColor(getResources().getColor(R.color.textgray));
				break;
//
			case R.id.linearlayout_myorders:
			case R.id.img_myorders:
				host.setCurrentTabByTag(MYORDERS_STRING);
				img_validorders.setBackgroundResource(R.drawable.ic_menu_deal_off);
				text_validorders.setTextColor(getResources().getColor(R.color.textgray));
				img_myorders.setBackgroundResource(R.drawable.ic_menu_poi_on);
				text_myorders.setTextColor(getResources().getColor(R.color.green));
				img_mine.setBackgroundResource(R.drawable.ic_menu_user_off);
				text_mine.setTextColor(getResources().getColor(R.color.textgray));
//				img_more.setBackgroundResource(R.drawable.ic_menu_more_off);
//				text_more.setTextColor(getResources().getColor(R.color.textgray));
				break;

			case R.id.linearlayout_mine:
			case R.id.img_mine:
				host.setCurrentTabByTag(MYSELF_STRING);
				img_validorders.setBackgroundResource(R.drawable.ic_menu_deal_off);
				text_validorders.setTextColor(getResources().getColor(R.color.textgray));
				img_myorders.setBackgroundResource(R.drawable.ic_menu_poi_off);
				text_myorders.setTextColor(getResources().getColor(R.color.textgray));
				img_mine.setBackgroundResource(R.drawable.ic_menu_user_on);
				text_mine.setTextColor(getResources().getColor(R.color.green));
//				img_more.setBackgroundResource(R.drawable.ic_menu_more_off);
//				text_more.setTextColor(getResources().getColor(R.color.textgray));
				break;

//		case R.id.linearlayout_more:
//		case R.id.img_more:
//			host.setCurrentTabByTag(MORE_STRING);
//			img_groupbuy.setBackgroundResource(R.drawable.ic_menu_deal_off);
//			text_groupbuy.setTextColor(getResources().getColor(R.color.textgray));
//			img_merchant.setBackgroundResource(R.drawable.ic_menu_poi_off);
//			text_merchant.setTextColor(getResources().getColor(R.color.textgray));
//			img_mine.setBackgroundResource(R.drawable.ic_menu_user_off);
//			text_mine.setTextColor(getResources().getColor(R.color.textgray));
//			img_more.setBackgroundResource(R.drawable.ic_menu_more_on);
//			text_more.setTextColor(getResources().getColor(R.color.green));
//			break;

			default:
				break;
		}
	}

	private void getScreenDisplay(){
		Display display=this.getWindowManager().getDefaultDisplay();
		int screenWidth = display.getWidth();
		int screenHeight=display.getHeight();

		AppContext appContext=(AppContext) getApplicationContext();
		appContext.setScreenWidth(screenWidth);
		appContext.setScreenHeight(screenHeight);
	}

	public  void isOpenGPS(){
		LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)){
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setMessage("GPS未打开，本配送程序必须打开!");
			dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					// 设置完成后返回到原来的界面
					startActivityForResult(intent,0);
				}
			});
			dialog.show();
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// RESULT_OK，判断另外一个activity已经结束数据输入功能，Standard activity result:
		// operation succeeded. 默认值是-1
		if (requestCode == 0) {
			isOpenGPS();
		}
	}



	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 &&event.getAction() == KeyEvent.ACTION_DOWN)        {
			new AlertDialog.Builder(MainlyActivity.this).setTitle("提示")
					.setMessage("确认退出吗？")
					.setIcon(R.drawable.icon_logo)
					.setPositiveButton("确定",
							new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog,
													int which)
								{
									android.os.Process.killProcess(android.os.Process.myPid()); // 结束进程
								}
							})
					.setNegativeButton("取消",
							new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog,
													int which)
								{

								}
							})
					.show();
			return false;
		}
		else
		{
			return super.dispatchKeyEvent(event);
		}
	}

	public void exit() {
		if ((System.currentTimeMillis() - exitTime) > 2000) {
			Toast.makeText(getApplicationContext(), "再按一次退出程序",
					Toast.LENGTH_SHORT).show();
			exitTime = System.currentTimeMillis();
		} else {
			finish();
			System.exit(0);
		}
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

}
