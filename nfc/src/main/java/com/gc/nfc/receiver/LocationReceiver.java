package com.gc.nfc.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.os.Build;
import android.widget.Toast;

import com.gc.nfc.utils.LocationUtils;
import android.app.AlarmManager;


public class LocationReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		// TODO Auto-generated method stub
		String msg = intent.getStringExtra("msg");
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
		//getBestLocation(context);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			getBestLocation(context);
		}
	}

	public  static void getBestLocation(Context context) {
		Criteria c = new Criteria();//Criteria类是设置定位的标准信息（系统会根据你的要求，匹配最适合你的定位供应商），一个定位的辅助信息的类
		c.setPowerRequirement(Criteria.POWER_LOW);//设置低耗电
		c.setAltitudeRequired(true);//设置需要海拔
		c.setBearingAccuracy(Criteria.ACCURACY_COARSE);//设置COARSE精度标准
		c.setAccuracy(Criteria.ACCURACY_LOW);//设置低精度
		//... Criteria 还有其他属性，就不一一介绍了
		Location best = LocationUtils.getBestLocation(context, c);
		if (best == null) {
			Toast.makeText(context, " best location is null", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(context, "best location: lat==" + best.getLatitude() + " lng==" + best.getLongitude(), Toast.LENGTH_SHORT).show();
		}
		startLocation(context);
	}

	public  static void startLocation(Context context){
//创建Intent对象，action为ELITOR_CLOCK，附加信息为字符串“你该打酱油了”
		Intent intent = new Intent("LOCATION_CLOCK");
		intent.putExtra("msg","你该打酱油了");

//定义一个PendingIntent对象，PendingIntent.getBroadcast包含了sendBroadcast的动作。
//也就是发送了action 为"ELITOR_CLOCK"的intent
		PendingIntent pi = PendingIntent.getBroadcast(context,0,intent,0);

//AlarmManager对象,注意这里并不是new一个对象，Alarmmanager为系统级服务
		AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

//设置闹钟从当前时间开始，每隔5s执行一次PendingIntent对象pi，注意第一个参数与第二个参数的关系
// 5秒后通过PendingIntent pi对象发送广播
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			//参数2是开始时间、参数3是允许系统延迟的时间
			am.setWindow(AlarmManager.RTC, System.currentTimeMillis(), 5000, pi);
		} else {
			am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 5000, pi);
		}

	}



}
