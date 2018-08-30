package com.gc.nfc.utils;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationClientOption.AMapLocationProtocol;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.AMapLocationQualityReport;
import com.amap.api.maps.model.LatLng;
import com.gc.nfc.R;
import com.gc.nfc.app.AppContext;
import com.gc.nfc.common.NetRequestConstant;
import com.gc.nfc.common.NetUrlConstant;
import com.gc.nfc.domain.User;
import com.gc.nfc.interfaces.Netcallback;
import com.gc.nfc.ui.AutoLoginActivity;
import com.gc.nfc.ui.MainlyActivity;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.content.ServiceConnection;
import android.content.ComponentName;

import android.os.RemoteException;

import app.project.IMyAidlInterface;
/**
 * 高精度定位模式功能演示
 *
 * @创建时间： 2015年11月24日 下午5:22:42
 * @项目名称： AMapLocationDemo2.x
 * @author hongming.wang
 * @文件名称: Hight_Accuracy_Activity.java
 * @类型名称: Hight_Accuracy_Activity
 */
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.NotificationManager;
import android.support.annotation.Nullable;
public class AmapLocationService extends Service {


	private static final String TAG = "LocationService";

	private AppContext m_appContext;

	private String m_userId;

	private PowerManager mPowerManager;

	private WakeLock mWakeLock;


	MyBinder binder;
	MyConn conn;


	//声明AMapLocationClient类对象
	AMapLocationClient mLocationClient = null;
	//声明AMapLocationClientOption对象
	public AMapLocationClientOption mLocationOption = null;

	MediaPlayer mMediaPlayer = null;
	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	@Override
	public void onDestroy() {
		//mWakeLock.release();
		//开启远程服务
		//AmapLocationService.this.startService(new Intent(AmapLocationService.this, RomoteService.class));
		//绑定远程服务
		//AmapLocationService.this.bindService(new Intent(AmapLocationService.this, RomoteService.class), conn, Context.BIND_IMPORTANT);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		binder = new MyBinder();
		conn = new MyConn();

		netThread.start();
		//初始化定位
		mLocationClient = new AMapLocationClient(getApplicationContext());
		//设置定位回调监听
		mLocationClient.setLocationListener(mLocationListener);
		//初始化AMapLocationClientOption对象
		mLocationOption = getDefaultOption();

		netHandler.sendEmptyMessageDelayed(0x99,10000);

		getPosition();
		mMediaPlayer = MediaPlayer.create(this, R.raw.report_location);

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		m_appContext = (AppContext) getApplicationContext();
		m_userId = (String)SharedPreferencesHelper.get("username", "default");
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		Notification.Builder builder = new Notification.Builder(this)
				.setContentTitle("百江燃气")
				.setContentText("百江配送持续运行中。")
				.setContentIntent(pendingIntent)
				.setSmallIcon(R.drawable.ic_launcher)
				.setWhen(System.currentTimeMillis())
				.setOngoing(true);
		Notification notification=builder.getNotification();
		NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		startForeground(1,notification);
		notifyManager.notify(1, notification);
		//this.bindService(new Intent(AmapLocationService.this, RomoteService.class), conn, Context.BIND_IMPORTANT);
		return Service.START_STICKY;

	}

	Handler netHandler = null;

	/**
	 * 收发网络数据的线程
	 */
	Thread netThread = new Thread(){
		@Override
		public void run() {
			Looper.prepare();
			netHandler = new Handler(){
				public void dispatchMessage(Message msg) {
					Bundle data = msg.getData();
					switch(msg.what){
						case 0x1: //发送位置
							Double longitude = data.getDouble("longitude");
							Double latitude = data.getDouble("latitude");
							String timestr = data.getString("timestr");
							upDatePosition(longitude, latitude, timestr);
							break;
						case 0x99: //获取电源锁
							mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
							mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, this.getClass().getName());
							if (mWakeLock != null) {
								mWakeLock.acquire();
							}
							break;
					}
				};
			};
			Looper.loop();
		}
	};

	public void getPosition(){
		//给定位客户端对象设置定位参数
		mLocationClient.setLocationOption(mLocationOption);
		//启动定位
		mLocationClient.startLocation();
	}
	//声明定位回调监听器
	public AMapLocationListener mLocationListener = new AMapLocationListener(){

		@Override
		public void onLocationChanged(AMapLocation amapLocation) {
			if(amapLocation==null){
				return;
			}
			if(amapLocation.getErrorCode()!=0){
				return;
			}

			Double longitude = amapLocation.getLongitude();//获取经度
			Double latitude = amapLocation.getLatitude();//获取纬度
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date(amapLocation.getTime());
			String timestr = df.format(date);

			Message msg = new Message();
			Bundle data = new Bundle();
			data.putDouble("longitude", longitude);
			data.putDouble("latitude", latitude);
			data.putString("timestr", timestr);
			msg.setData(data);
			msg.what = 0x1;
			netHandler.sendMessage(msg);

		}

	};






	public void upDatePosition(double longitude, double latitude, String timestr) {
		LatLng myLocation = new LatLng(latitude ,longitude);
		if(m_appContext!=null){
			m_appContext.setLocation(myLocation);
		}

		reportLocation(myLocation);//上报定位数据
		sysUserKeepAlive();//心跳信息

	}

	private AMapLocationClientOption getDefaultOption(){
		AMapLocationClientOption mOption = new AMapLocationClientOption();
		mOption.setLocationMode(AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
		mOption.setGpsFirst(true);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
		mOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
		mOption.setNeedAddress(false);//可选，设置是否返回逆地理地址信息。默认是true
		mOption.setOnceLocation(false);//可选，设置是否单次定位。默认是false
		mOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
		AMapLocationClientOption.setLocationProtocol(AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
		mOption.setSensorEnable(false);//可选，设置是否使用传感器。默认是false
		mOption.setWifiScan(true); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
		mOption.setLocationCacheEnable(true); //可选，设置是否使用缓存定位，默认为true

		mOption.setInterval(20000);//可选，设置定位间隔。默认为2秒

		return mOption;
	}


	private void reportLocation(LatLng location){

		String result=null;
		try {
			HttpParams httpParams=new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
			HttpConnectionParams.setSoTimeout(httpParams, 5000);

			//创建一个HttpClient实例
			DefaultHttpClient httpClient=new DefaultHttpClient(httpParams);
			//建立HttpPost对象
			String requestUrl = NetUrlConstant.POSITIONURL;

			requestUrl = requestUrl + "?" + "userId="+ m_userId;
			HttpPost httpRequest=new HttpPost(requestUrl);
			httpRequest.setHeader("Content-Type", "application/json");

			//发送请求的参数
			JSONObject bodyJson = new JSONObject();  ;
			bodyJson.put("longitude", location.longitude);
			bodyJson.put("latitude", location.latitude);
			StringEntity stringEntity = new StringEntity(bodyJson.toString());
			//stringEntity.setContentEncoding("UTF-8");
			stringEntity.setContentType("application/json");

			httpRequest.setEntity(stringEntity);

			//发送请求并等待响应
			HttpResponse httpResponse=httpClient.execute(httpRequest);
			if(httpResponse.getStatusLine().getStatusCode()!=200){
				mMediaPlayer.start();
			}else{

			}

			JSONObject bodyJsona = new JSONObject();  ;
			bodyJsona.put("longitude", location.longitude);

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//心跳
	private void sysUserKeepAlive() {
		try {
			HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
			HttpConnectionParams.setSoTimeout(httpParams, 5000);

			//创建一个HttpClient实例
			DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
			//建立HttpPost对象
			String requestUrl = NetUrlConstant.SYSUSERKEEPALIVEURL;

			requestUrl = requestUrl + "/" + m_userId;
			HttpGet httpRequest = new HttpGet(requestUrl);
			httpRequest.setHeader("Content-Type", "application/json");
			//发送请求并等待响应
			HttpResponse httpResponse = httpClient.execute(httpRequest);
			if(httpResponse.getStatusLine().getStatusCode()!=200){

			}


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}






	class MyBinder extends IMyAidlInterface.Stub {
		@Override
		public String getServiceName() throws RemoteException {
			return AmapLocationService.class.getSimpleName();
		}
	}



	class MyConn implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			//开启远程服务
			//AmapLocationService.this.startService(new Intent(AmapLocationService.this, RomoteService.class));
			//绑定远程服务
			//AmapLocationService.this.bindService(new Intent(AmapLocationService.this, RomoteService.class), conn, Context.BIND_IMPORTANT);
		}
	}



}
