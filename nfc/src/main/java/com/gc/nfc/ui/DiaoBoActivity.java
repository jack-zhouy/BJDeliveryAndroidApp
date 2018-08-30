package com.gc.nfc.ui;

import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.nfc.R;
import com.gc.nfc.app.AppContext;
import com.gc.nfc.common.NetRequestConstant;
import com.gc.nfc.common.NetUrlConstant;
import com.gc.nfc.domain.User;
import com.gc.nfc.interfaces.Netcallback;
import com.gc.nfc.service.MyJobService;
import com.gc.nfc.utils.AmapLocationService;
import com.gc.nfc.utils.OnePixelReceiver;
import com.gc.nfc.utils.SharedPreferencesHelper;

import org.apache.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class DiaoBoActivity extends BaseActivity implements OnClickListener {

	private LinearLayout lL_myBottle;// 我的气瓶
	private LinearLayout lL_myLogout;//退出登录
	private LinearLayout lL_myAppVersion;// 系统版本

	private TextView textview_username;

	private ImageView imageView_userQRcode;

	private AppContext appContext;
	private User user;
	private Intent m_IntentAmapServeice;
	//监听屏幕状态的广播
	private OnePixelReceiver mOnepxReceiver;

	private JobScheduler mJobScheduler;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		appContext = (AppContext) getApplicationContext();
		user = appContext.getUser();
		if (user == null) {
			Toast.makeText(DiaoBoActivity.this, "登陆会话失效", Toast.LENGTH_LONG).show();
			Intent intent = new Intent(DiaoBoActivity.this, AutoLoginActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		if (user != null) {
			String username = user.getUsername();
			textview_username.setText(username+"\n"+"("+user.getGroupName()+"|"+user.getDepartmentName()+")");
			String  strUri = NetUrlConstant.QRCODEURL+"?text="+user.getUsername();
			try {
				URL link = new URL(strUri);
				InputStream is = link.openStream();
				Bitmap bitmap = BitmapFactory.decodeStream(is);
				imageView_userQRcode.setImageBitmap(bitmap);
			}catch (MalformedURLException e){
			}
			catch (IOException e){
			}
		}
		//开启定位任务
		isOpenGPS();
		//开启定位
		m_IntentAmapServeice = new Intent(this,AmapLocationService.class);
		startService(m_IntentAmapServeice);
		mOnepxReceiver = new OnePixelReceiver();
		IntentFilter intentFilter2 = new IntentFilter();
		intentFilter2.addAction("android.intent.action.SCREEN_OFF");
		intentFilter2.addAction("android.intent.action.SCREEN_ON");
		intentFilter2.addAction("android.intent.action.USER_PRESENT");
		registerReceiver(mOnepxReceiver, intentFilter2);

		startJobScheduler();

//		final Intent intentServiceWatch = new Intent(this,com.gc.nfc.utils.RomoteService.class);
//		startService(intentServiceWatch);
	}

	public void onClick(View v) {
		// TODO Auto-generated method stub
		Intent intent;
		switch (v.getId()) {
			case R.id.lL_myBottle:// 我的气瓶
				intent = new Intent(DiaoBoActivity.this, MybottlesActivity.class);
				Bundle bundle = new Bundle();
				bundle.putString("userId",user.getUsername());
				intent.putExtras(bundle);
				startActivity(intent);
				break;
			case R.id.lL_myLogout:// 退出登录
				loginOut();
				break;
			case R.id.lL_mySetting:// 系统版本
				intent = new Intent(DiaoBoActivity.this, AboutActivity.class);
				startActivity(intent);
				break;

			case R.id.imageView_userQRcode:// 显示用户二维码
				showIdentification();
				break;
			default:
				break;
		}

	}

	@Override
	void init() {
		setContentView(R.layout.activity_diaobo);
		lL_myBottle = (LinearLayout) findViewById(R.id.lL_myBottle);// 我的气瓶
		lL_myAppVersion = (LinearLayout) findViewById(R.id.lL_mySetting);// 我的版本
		lL_myLogout = (LinearLayout) findViewById(R.id.lL_myLogout);//退出登录
		imageView_userQRcode  = (ImageView) findViewById(R.id.imageView_userQRcode);//二维码用户身份



		textview_username = (TextView) findViewById(R.id.textview_username);


		lL_myBottle.setOnClickListener(this);
		lL_myLogout.setOnClickListener(this);
		lL_myAppVersion.setOnClickListener(this);
		imageView_userQRcode.setOnClickListener(this);



	}

	private void loginOut(){
		SharedPreferencesHelper.put("username", "");
		SharedPreferencesHelper.put("password", "");

		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);
		final String username = user.getUsername();
		final String password = user.getPassword();
		nrc.requestUrl = NetUrlConstant.LOGINOUTURL+"/"+username;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==200){
								//设置退出登录
									Intent intent = new Intent(getApplicationContext() , LoginActivity.class);
									startActivity(intent);
									finish();
						}else{
							Toast.makeText(DiaoBoActivity.this, "退出登录失败", Toast.LENGTH_LONG).show();
						}
					}else {
						Toast.makeText(DiaoBoActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(DiaoBoActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
	}

	private void showIdentification(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		View view = View.inflate(this, R.layout.pay_on_scan, null);   // 账号、密码的布局文件，自定义
		ImageView QRcode = (ImageView)view.findViewById(R.id.items_imageViewScanCode);
		String  strUri = NetUrlConstant.QRCODEURL+"?text="+user.getUsername();



		try {
			URL link = new URL(strUri);
			InputStream is = link.openStream();
			Bitmap bitmap = BitmapFactory.decodeStream( is );
			QRcode.setImageBitmap(bitmap);
			builder.setIcon(R.drawable.icon_logo);//设置对话框icon

			builder.setTitle("身份码");

			AlertDialog dialog = builder.create();
			dialog.setView(view);
			dialog.setButton(DialogInterface.BUTTON_POSITIVE,"确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();//关闭对话框
				}
			});

			dialog.show();
			Window dialogWindow = dialog.getWindow();//获取window对象
			dialogWindow.setGravity(Gravity.CENTER);//设置对话框位置
			dialogWindow.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);//设置横向全屏
		}
		catch (MalformedURLException e){
		}
		catch (IOException e){
		}
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
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 &&event.getAction() == KeyEvent.ACTION_DOWN)        {
			new AlertDialog.Builder(DiaoBoActivity.this).setTitle("提示")
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

	public void onDestroy(){
		stopService(m_IntentAmapServeice);
		super.onDestroy();
	}

	@RequiresApi(21)
	public void startJobScheduler() {
		mJobScheduler = (JobScheduler)
				getSystemService( Context.JOB_SCHEDULER_SERVICE );

		int id = 55;

		mJobScheduler.cancel(id);
		JobInfo.Builder builder = new JobInfo.Builder(id, new ComponentName(this, MyJobService.class));
		if (Build.VERSION.SDK_INT >= 21) {
			builder.setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS); //执行的最小延迟时间
			builder.setOverrideDeadline(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);  //执行的最长延时时间
			builder.setBackoffCriteria(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS, JobInfo.BACKOFF_POLICY_LINEAR);//线性重试方案
		} else {
			builder.setPeriodic(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);
		}
		builder.setPersisted(true);  // 设置设备重启时，执行该任务
		builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
		builder.setRequiresCharging(true); // 当插入充电器，执行该任务
		PersistableBundle persiBundle = new PersistableBundle();
		persiBundle.putString("servicename", AmapLocationService.class.getName());
		builder.setExtras(persiBundle);
		JobInfo info = builder.build();

		mJobScheduler.schedule(info); //开始定时执行该系统任务
	}
}
