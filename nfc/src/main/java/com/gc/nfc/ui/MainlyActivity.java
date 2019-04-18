package com.gc.nfc.ui;

import com.amap.api.maps.model.LatLng;
import com.gc.nfc.R;
import com.gc.nfc.app.AppContext;
import com.gc.nfc.common.NetRequestConstant;
import com.gc.nfc.common.NetUrlConstant;
import com.gc.nfc.domain.User;
import com.gc.nfc.interfaces.Netcallback;
import com.gc.nfc.receiver.LocationReceiver;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.app.TabActivity;
import android.content.Intent;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.nfc.utils.AmapLocationService;
import android.os.PersistableBundle;

import android.view.KeyEvent;

import com.gc.nfc.utils.OnePixelReceiver;
import android.content.IntentFilter;
import android.app.job.JobScheduler;
import android.app.job.JobInfo;
import android.content.ComponentName;
import android.os.Build;
import com.gc.nfc.service.MyJobService;
import com.gc.nfc.utils.SharedPreferencesHelper;
import com.alibaba.sdk.android.push.CloudPushService;
import com.alibaba.sdk.android.push.CommonCallback;
import com.alibaba.sdk.android.push.noonesdk.PushServiceFactory;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import com.alibaba.sdk.android.push.CloudPushService;
import com.alibaba.sdk.android.push.CommonCallback;
import com.alibaba.sdk.android.push.noonesdk.PushServiceFactory;
import com.alibaba.sdk.android.push.register.GcmRegister;
import com.alibaba.sdk.android.push.register.HuaWeiRegister;
import com.alibaba.sdk.android.push.register.MiPushRegister;

public class MainlyActivity extends TabActivity implements OnClickListener {
	private final static String PROCESS_NAME = "com.gc.nfc";
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

	private Intent m_IntentAmapServeice;

	private JobScheduler mJobScheduler;
	private String m_userId;

	//监听屏幕状态的广播
	private OnePixelReceiver mOnepxReceiver;

	// 用来计算返回键的点击间隔时间
	private long exitTime = 0;

	private CloudPushService mPushService;//推送服务


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mainly);
		this.getScreenDisplay();




//		//判断是不是主进程
//		if(!isAppMainProcess()){
//			Toast.makeText(getApplicationContext(), "ooooooooooooooo",
//					Toast.LENGTH_SHORT).show();
//		}
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

		m_userId= (String) SharedPreferencesHelper.get("username", "default");


		//开启定位任务
		isOpenGPS();


//		m_IntentAmapServeice = new Intent(this,AmapLocationService.class);
//		startService(m_IntentAmapServeice);

		mOnepxReceiver = new OnePixelReceiver();
		IntentFilter intentFilter2 = new IntentFilter();
		intentFilter2.addAction("android.intent.action.SCREEN_OFF");
		intentFilter2.addAction("android.intent.action.SCREEN_ON");
		intentFilter2.addAction("android.intent.action.USER_PRESENT");
		registerReceiver(mOnepxReceiver, intentFilter2);

		startJobScheduler(m_userId);

		initialCloudPushService();//初始化推送服务

//		final Intent intentServiceWatch = new Intent(this,com.gc.nfc.utils.RomoteService.class);
//		startService(intentServiceWatch);

//		LocationUtils.setInstance(this);
//		LocationUtils.isOpenGPS();
//		LocationUtils.getStatusListener();
//		LocationReceiver.startLocation(this);

//API大于24



	}
	public void onDestroy(){
		//stopService(m_IntentAmapServeice);
		super.onDestroy();
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
									stopService(m_IntentAmapServeice);
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

	@RequiresApi(21)
	public void startJobScheduler(String userId) {
		mJobScheduler = (JobScheduler)
				getSystemService( Context.JOB_SCHEDULER_SERVICE );

		int id = 55;

		mJobScheduler.cancel(id);
		JobInfo.Builder builder = new JobInfo.Builder(id, new ComponentName(this, MyJobService.class));
		if (Build.VERSION.SDK_INT >= 21) {
			builder.setMinimumLatency(5000); //执行的最小延迟时间
			//builder.setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS); //执行的最小延迟时间
			builder.setOverrideDeadline(6000);  //执行的最长延时时间
			builder.setBackoffCriteria(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS, JobInfo.BACKOFF_POLICY_LINEAR);//线性重试方案
		} else {
			builder.setPeriodic(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);
		}
		builder.setPersisted(true);  // 设置设备重启时，执行该任务
		builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
		builder.setRequiresCharging(true); // 当插入充电器，执行该任务
		PersistableBundle persiBundle = new PersistableBundle();
		persiBundle.putString("servicename", AmapLocationService.class.getName());
		persiBundle.putString("userId", userId);
		builder.setExtras(persiBundle);
		JobInfo info = builder.build();

		mJobScheduler.schedule(info); //开始定时执行该系统任务
	}
	/**
	 * 判断是不是UI主进程，因为有些东西只能在UI主进程初始化
	 */
	public  boolean isAppMainProcess() {
		try {
			int pid = android.os.Process.myPid();
			String process = getAppNameByPID(getApplicationContext(), pid);
			if (TextUtils.isEmpty(process)) {
				//第一次创建,系统中还不存在该process,所以一定是主进程
				return true;
			} else if (PROCESS_NAME.equalsIgnoreCase(process)) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return true;
		}
	}

	/**
	 * 根据Pid得到进程名
	 */
	public  String getAppNameByPID(Context context, int pid) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (android.app.ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
			if (processInfo.pid == pid) {
				//主进程的pid是否和当前的pid相同,若相同,则对应的包名就是app的包名
				return processInfo.processName;
			}
		}
		return "";
	}


	/**
	 * 打开推送通道接口:CloudPushService.turnOnPushChannel调用示例
	 * 1. 推送通道默认是打开的,如果没有调用turnOffPushChannel接口关闭推送通道,无法调用该接口
	 */
	private void turnOnPush() {
		mPushService.turnOnPushChannel(new CommonCallback() {
			@Override
			public void onSuccess(String s) {
				System.out.print("打开推送通道：ok");
			}

			@Override
			public void onFailed(String errorCode, String errorMsg) {
				System.out.print("打开推送通道：failed");
			}
		});
	}

	/**关闭推送通道接口:CloudPushService.turnOffPushChannel调用示例
	 * 1. 调用该接口后,移动推送服务端将不再向该设备推送消息
	 * 2. 关闭推送通道,必须通过调用turnOnPushChannel才能让服务端重新向该设备推送消息
	 */
	private void turnOffPush() {
		mPushService.turnOffPushChannel(new CommonCallback() {
			@Override
			public void onSuccess(String s) {

			}

			@Override
			public void onFailed(String errorCode, String errorMsg) {

			}
		});
	}
	/**
	 * 添加别名接口:CloudPushService.addAlias调用示例
	 * 1. 调用接口后,可以通过别名进行推送
	 */
	private void addAlias(String aliasName) {
		mPushService.addAlias(aliasName, new CommonCallback() {
			@Override
			public void onSuccess(String s) {
				System.out.print("添加别名：ok");
			}
			@Override
			public void onFailed(String errorCode, String errorMsg) {
				System.out.print("添加别名：failed");
			}
		});
	}


	private void initialCloudPushService() {
		mPushService = PushServiceFactory.getCloudPushService();
		turnOnPush();//打开推送通道
		mPushService.removeAlias(null,new CommonCallback() {
			@Override
			public void onSuccess(String s) {
				System.out.print("删除别名：ok");
				addAlias(m_userId);
			}
			@Override
			public void onFailed(String errorCode, String errorMsg) {
				System.out.print("删除别名：failed");
				addAlias(m_userId);
			}
		});
		setCusNotifSound();
	}



	/**
	 * 指定通知声音文件示例
	 * 1. 此处指定资源Id为R.raw.alicloud_notification_sound_assgin的声音文件
	 */
	private void setCusNotifSound() {
		final String DEFAULT_RES_PATH_FREFIX = "android.resource://";
		final String DEFAULT_RES_SOUND_TYPE = "raw";
		final String ASSIGN_NOTIFCE_SOUND = "new_order_notify";
		final String SETTING_NOTICE = "setting_notification";
		int assignSoundId = getResources().getIdentifier(ASSIGN_NOTIFCE_SOUND, DEFAULT_RES_SOUND_TYPE, getPackageName());
		if (assignSoundId != 0) {
			String defaultSoundPath = DEFAULT_RES_PATH_FREFIX + getPackageName() + "/" + assignSoundId;
			mPushService.setNotificationSoundFilePath(defaultSoundPath);
			Log.i(SETTING_NOTICE, "Set notification sound res id to R." + DEFAULT_RES_SOUND_TYPE + "." + ASSIGN_NOTIFCE_SOUND);

		} else {
			Log.e(SETTING_NOTICE, "Set notification sound path error, R."
					+ DEFAULT_RES_SOUND_TYPE + "." + ASSIGN_NOTIFCE_SOUND + " not found.");

		}
	}

//	/**
//	 * 设置默认通知图标方法示例
//	 * 1. 如果用户未调用过setNotificationLargeIcon()接口，默认取R.raw.alicloud_notification_largeicon图标资源
//	 * 则无需使用以下方式进行设置
//	 */
//	private void setDefNotifIcon() {
//		final String DEFAULT_NOTICE_LARGE_ICON = "alicloud_notification_largeicon";
//		final String ASSIGN_NOTIFCE_LARGE_ICON = "alicloud_notification_largeicon_assign";
//		int defaultLargeIconId = getResources().getIdentifier(DEFAULT_NOTICE_LARGE_ICON, DEFAULT_RES_ICON_TYPE, PackageName);
//		if (defaultLargeIconId != 0) {
//			Drawable drawable = getApplicationContext().getResources().getDrawable(defaultLargeIconId);
//			if (drawable != null) {
//				Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
//				mPushService.setNotificationLargeIcon(bitmap);
//			}
//		} else {
//
//		}
//
//	}



}
