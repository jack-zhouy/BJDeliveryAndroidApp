package com.gc.nfc.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMap.OnCameraChangeListener;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.MapsInitializer;
import com.amap.api.maps2d.UiSettings;


import com.amap.api.maps2d.model.*;
import com.gc.nfc.R;
import com.gc.nfc.common.NetRequestConstant;
import com.gc.nfc.common.NetUrlConstant;
import com.gc.nfc.interfaces.Netcallback;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.graphics.BitmapFactory;
import android.os.Handler;

/**
 * AMapV1地图中介绍如何显示一个基本地图
 */
public class MapViewActivity extends BaseActivity {
	private AMap aMap;
	private MapView mapView;
	private UiSettings mUiSettings;//定义一个UiSettings对象
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map_view);
		mapView = (MapView) findViewById(R.id.map);
		mapView.onCreate(savedInstanceState);// 此方法必须重写
		mapinit();
		MapsInitializer.loadWorldGridMap(true);
	}

	public void mapinit() {
		if (aMap == null) {
			aMap = mapView.getMap();
			setUpMap();

			MyLocationStyle myLocationStyle;
			myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
			myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
			aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
//aMap.getUiSettings().setMyLocationButtonEnabled(true);设置默认定位按钮是否显示，非必需设置。
			aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。

			mHandler.postDelayed(r, 1000);//延时100毫秒



			mUiSettings = aMap.getUiSettings();//实例化UiSettings类对象
			mUiSettings.setMyLocationButtonEnabled(true); //显示默认的定位按钮

			aMap.setMyLocationEnabled(true);// 可触发定位并显示当前位置
		}
	}

	/**
	 * 初始化AMap对象
	 */
	public void init() {

	}

	/**
	 * 对地图添加onMapIsAbroadListener
	 */
	private void setUpMap() {
		aMap.setOnCameraChangeListener(new OnCameraChangeListener() {

			@Override
			public void onCameraChangeFinish(CameraPosition cameraPosition) {

			}

			@Override
			public void onCameraChange(CameraPosition cameraPosition) {
			}
		});
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onResume() {
		super.onResume();
		mapView.onResume();
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onPause() {
		super.onPause();
		mapView.onPause();
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
	}





	protected void clean_marksers(){
		aMap.clear();

	}


	protected void reflesh_makers(String userId, String name, Double longitude, Double latitude){

		MarkerOptions markerOption = new MarkerOptions();
		LatLng latLng = new LatLng(latitude,longitude);
		markerOption.position(latLng);
		markerOption.title(userId).snippet(name);
		markerOption.draggable(false);//设置Marker可拖动
		markerOption.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
				.decodeResource(getResources(),R.drawable.worker)));

		Marker marker = aMap.addMarker(markerOption);
		marker.setVisible(true);
	}

	//获取所有在线配送工的位置
	private void get_ps_location() {
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.GETSYSUSERINFOURL;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();


		params.put("aliveStatus","1");
		params.put("groupCode", "00003");//配送工
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag) {
					HttpResponse response = (HttpResponse) res;
					if (response != null) {
						if (response.getStatusLine().getStatusCode() == 200) {
							clean_marksers();
							try {
								JSONObject usersJson = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
								JSONArray usersListJson = usersJson.getJSONArray("items");
								for(int i=0; i<usersListJson.length(); i++){
									String userId = usersListJson.getJSONObject(i).getString("userId");
									String userName = usersListJson.getJSONObject(i).getString("name");
									JSONObject userPositionJson = usersListJson.getJSONObject(i).getJSONObject("userPosition");
									Double longitude = userPositionJson.getDouble("longitude");
									Double latitude = userPositionJson.getDouble("latitude");
									reflesh_makers(userId, userName, longitude, latitude);
								}
							} catch (JSONException e) {
								Toast.makeText(MapViewActivity.this, "未知错误，异常！" + e.getMessage(),
										Toast.LENGTH_LONG).show();
							} catch (IOException e) {
								Toast.makeText(MapViewActivity.this, "未知错误，异常！" + e.getMessage(),
										Toast.LENGTH_LONG).show();
							}
						} else {
							Toast.makeText(MapViewActivity.this, "查询配送工位置失败",
									Toast.LENGTH_LONG).show();
						}
					} else {
						Toast.makeText(MapViewActivity.this, "网络未连接！",
								Toast.LENGTH_LONG).show();
					}
				}}}, nrc);
	}
	Handler mHandler = new Handler();
	Runnable r = new Runnable() {

		@Override
		public void run() {
			//do something
			get_ps_location();
			//每隔1s循环执行run方法
			mHandler.postDelayed(this, 10000);
		}
	};




}
