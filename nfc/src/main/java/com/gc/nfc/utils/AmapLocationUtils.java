package com.gc.nfc.utils;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationClientOption.AMapLocationProtocol;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.AMapLocationQualityReport;
import com.amap.api.maps.model.LatLng;
import com.gc.nfc.app.AppContext;
import com.gc.nfc.common.NetUrlConstant;
import com.gc.nfc.domain.User;

import android.content.Context;
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
import java.util.List;
import java.util.Map;

/**
 * 高精度定位模式功能演示
 *
 * @创建时间： 2015年11月24日 下午5:22:42
 * @项目名称： AMapLocationDemo2.x
 * @author hongming.wang
 * @文件名称: Hight_Accuracy_Activity.java
 * @类型名称: Hight_Accuracy_Activity
 */
public class AmapLocationUtils{


	private AMapLocationClient locationClient = null;
	private AMapLocationClientOption locationOption = null;

	private Context mcontext;

	public  AmapLocationUtils(Context context){
		initLocation(context);
		startLocation();

	}

	/**
	 * 初始化定位
	 *
	 * @since 2.8.0
	 * @author hongming.wang
	 *
	 */
	private void initLocation(Context context){
		//初始化client
		locationClient = new AMapLocationClient(context.getApplicationContext());
		locationOption = getDefaultOption();
		//设置定位参数
		locationClient.setLocationOption(locationOption);
		// 设置定位监听
		locationClient.setLocationListener(locationListener);

		mcontext = context.getApplicationContext();
	}

	/**
	 * 默认的定位参数
	 * @since 2.8.0
	 * @author hongming.wang
	 *
	 */
	private AMapLocationClientOption getDefaultOption(){
		AMapLocationClientOption mOption = new AMapLocationClientOption();
		mOption.setLocationMode(AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
		mOption.setGpsFirst(true);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
		mOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
		mOption.setInterval(5000);//可选，设置定位间隔。默认为2秒
		mOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是true
		mOption.setOnceLocation(false);//可选，设置是否单次定位。默认是false
		mOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
		AMapLocationClientOption.setLocationProtocol(AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
		mOption.setSensorEnable(false);//可选，设置是否使用传感器。默认是false
		mOption.setWifiScan(true); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
		mOption.setLocationCacheEnable(true); //可选，设置是否使用缓存定位，默认为true
		return mOption;
	}

	/**
	 * 定位监听
	 */
	AMapLocationListener locationListener = new AMapLocationListener() {
		@Override
		public void onLocationChanged(AMapLocation location) {
			if (null != location) {

				StringBuffer sb = new StringBuffer();
				//errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
				if(location.getErrorCode() == 0){
					sb.append("定位成功" + "\n");
					sb.append("定位类型: " + location.getLocationType() + "\n");
					sb.append("经    度    : " + location.getLongitude() + "\n");
					sb.append("纬    度    : " + location.getLatitude() + "\n");
					sb.append("精    度    : " + location.getAccuracy() + "米" + "\n");
					sb.append("提供者    : " + location.getProvider() + "\n");

					sb.append("速    度    : " + location.getSpeed() + "米/秒" + "\n");
					sb.append("角    度    : " + location.getBearing() + "\n");
					// 获取当前提供定位服务的卫星个数
					sb.append("星    数    : " + location.getSatellites() + "\n");
					sb.append("国    家    : " + location.getCountry() + "\n");
					sb.append("省            : " + location.getProvince() + "\n");
					sb.append("市            : " + location.getCity() + "\n");
					sb.append("城市编码 : " + location.getCityCode() + "\n");
					sb.append("区            : " + location.getDistrict() + "\n");
					sb.append("区域 码   : " + location.getAdCode() + "\n");
					sb.append("地    址    : " + location.getAddress() + "\n");
					sb.append("兴趣点    : " + location.getPoiName() + "\n");
					//定位完成的时间
					//sb.append("定位时间: " + Utils.formatUTC(location.getTime(), "yyyy-MM-dd HH:mm:ss") + "\n");
				} else {
					//定位失败
					sb.append("定位失败" + "\n");
					sb.append("错误码:" + location.getErrorCode() + "\n");
					sb.append("错误信息:" + location.getErrorInfo() + "\n");
					sb.append("错误描述:" + location.getLocationDetail() + "\n");
				}
				sb.append("***定位质量报告***").append("\n");
				sb.append("* WIFI开关：").append(location.getLocationQualityReport().isWifiAble() ? "开启":"关闭").append("\n");
				sb.append("* GPS状态：").append(getGPSStatusString(location.getLocationQualityReport().getGPSStatus())).append("\n");
				sb.append("* GPS星数：").append(location.getLocationQualityReport().getGPSSatellites()).append("\n");
				sb.append("****************").append("\n");
				//定位之后的回调时间
				//sb.append("回调时间: " + Utils.formatUTC(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss") + "\n");

				//解析定位结果，
				String result = sb.toString();
				//Toast.makeText(mcontext, result,Toast.LENGTH_SHORT).show();
				//存储当前位置
				AppContext appContext = (AppContext) mcontext;
				LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
				appContext.setLocation(myLocation);

				reportLocation(myLocation);//上报定位数据

			} else {
				//Toast.makeText(mcontext, "定位失败",Toast.LENGTH_SHORT).show();

			}
		}
	};


	/**
	 * 获取GPS状态的字符串
	 * @param statusCode GPS状态码
	 * @return
	 */
	private String getGPSStatusString(int statusCode){
		String str = "";
		switch (statusCode){
			case AMapLocationQualityReport.GPS_STATUS_OK:
				str = "GPS状态正常";
				break;
			case AMapLocationQualityReport.GPS_STATUS_NOGPSPROVIDER:
				str = "手机中没有GPS Provider，无法进行GPS定位";
				break;
			case AMapLocationQualityReport.GPS_STATUS_OFF:
				str = "GPS关闭，建议开启GPS，提高定位质量";
				break;
			case AMapLocationQualityReport.GPS_STATUS_MODE_SAVING:
				str = "选择的定位模式中不包含GPS定位，建议选择包含GPS定位的模式，提高定位质量";
				break;
			case AMapLocationQualityReport.GPS_STATUS_NOGPSPERMISSION:
				str = "没有GPS定位权限，建议开启gps定位权限";
				break;
		}
		return str;
	}


	/**
	 * 开始定位
	 *
	 * @since 2.8.0
	 * @author hongming.wang
	 *
	 */
	private void startLocation(){
		// 设置定位参数
		locationClient.setLocationOption(locationOption);
		// 启动定位
		locationClient.startLocation();
	}

	/**
	 * 停止定位
	 *
	 * @since 2.8.0
	 * @author hongming.wang
	 *
	 */
	private void stopLocation(){
		// 停止定位
		locationClient.stopLocation();
	}

	/**
	 * 销毁定位
	 *
	 * @since 2.8.0
	 * @author hongming.wang
	 *
	 */
	private void destroyLocation(){
		if (null != locationClient) {
			/**
			 * 如果AMapLocationClient是在当前Activity实例化的，
			 * 在Activity的onDestroy中一定要执行AMapLocationClient的onDestroy
			 */
			locationClient.onDestroy();
			locationClient = null;
			locationOption = null;
		}
	}


	private void reportLocation(LatLng location){
		AppContext appContext = (AppContext) mcontext;
		String result=null;
		try {
			HttpParams httpParams=new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
			HttpConnectionParams.setSoTimeout(httpParams, 5000);

			//创建一个HttpClient实例
			DefaultHttpClient httpClient=new DefaultHttpClient(httpParams);
			//建立HttpPost对象
			String requestUrl = NetUrlConstant.POSITIONURL;

			requestUrl = requestUrl + "?" + "userId="+ appContext.getUser().getUsername();

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

			HttpResponse httpResponse1 = httpResponse;

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
}
