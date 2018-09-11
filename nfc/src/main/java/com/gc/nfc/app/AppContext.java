package com.gc.nfc.app;



import com.alibaba.sdk.android.push.CloudPushService;
import com.alibaba.sdk.android.push.CommonCallback;
import com.alibaba.sdk.android.push.noonesdk.PushServiceFactory;
import com.alibaba.sdk.android.push.register.GcmRegister;
import com.alibaba.sdk.android.push.register.HuaWeiRegister;
import com.alibaba.sdk.android.push.register.MiPushRegister;
import com.amap.api.maps.model.LatLng;

import com.gc.nfc.R;
import com.gc.nfc.domain.User;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.app.Notification;

public class AppContext extends Application {
	private User user;
	private String groupCode;
	private String groupName;
	private LatLng location;
	private int screenWidth;
	private int screenHeight;
	private SharedPreferences preferences;


	public String getGroupCode() {
		return groupCode;
	}

	public void setGroupCode(String groupCode) {
		this.groupCode = groupCode;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public int getScreenWidth() {
		return screenWidth;
	}

	public void setScreenWidth(int screenWidth) {
		this.screenWidth = screenWidth;
	}

	public int getScreenHeight() {
		return screenHeight;
	}

	public void setScreenHeight(int screenHeight) {
		this.screenHeight = screenHeight;
	}

	public SharedPreferences getPreferences() {
		return preferences;
	}

	public void setPreferences(SharedPreferences preferences) {
		this.preferences = preferences;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		//初始化推送服务
		initCloudChannel(this);
	}



	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
		Editor editor = preferences.edit();
		editor.putString("username", user.getUsername());
		editor.putString("password", user.getPassword());
		editor.commit();
	}

	public LatLng getLocation() {
		return location;
	}

	public void setLocation(LatLng location) {
		this.location = location;

	}

	/**
	 * 初始化云推送通道
	 * @param applicationContext
	 */
	private void initCloudChannel(final Context applicationContext) {
		// 创建notificaiton channel
		this.createNotificationChannel();
		PushServiceFactory.init(applicationContext);
		final CloudPushService pushService = PushServiceFactory.getCloudPushService();
		pushService.register(applicationContext, new CommonCallback() {
			@Override
			public void onSuccess(String response) {
				System.out.print("初始化云推送通道：ok");
			}

			@Override
			public void onFailed(String errorCode, String errorMessage) {
				System.out.print("初始化云推送通道：failed");
			}
		});

		MiPushRegister.register(applicationContext, "XIAOMI_ID", "XIAOMI_KEY"); // 初始化小米辅助推送
		HuaWeiRegister.register(applicationContext); // 接入华为辅助推送
		GcmRegister.register(applicationContext, "send_id", "application_id"); // 接入FCM/GCM初始化推送
	}

	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			// 通知渠道的id
			String id = "1";
			// 用户可以看到的通知渠道的名字.
			CharSequence name = "notification channel";
			// 用户可以看到的通知渠道的描述
			String description = "notification description";
			int importance = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel mChannel = new NotificationChannel(id, name, importance);
			// 配置通知渠道的属性
			mChannel.setDescription(description);
			// 设置通知出现时的闪灯（如果 android 设备支持的话）
			mChannel.enableLights(true);
			mChannel.setLightColor(Color.RED);
			// 设置通知出现时的震动（如果 android 设备支持的话）
			mChannel.enableVibration(true);
			mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

			final String DEFAULT_RES_PATH_FREFIX = "android.resource://";
			String defaultSoundPath = DEFAULT_RES_PATH_FREFIX + getPackageName() + "/" + R.raw.new_order_notify;
			mChannel.setSound(Uri.parse(defaultSoundPath), Notification.AUDIO_ATTRIBUTES_DEFAULT);

			//最后在notificationmanager中创建该通知渠道
			mNotificationManager.createNotificationChannel(mChannel);
		}
	}
}
