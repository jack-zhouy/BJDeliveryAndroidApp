package com.gc.nfc.ui;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.dk.bleNfc.BleManager.BleManager;
import com.dk.bleNfc.BleManager.Scanner;
import com.dk.bleNfc.BleManager.ScannerCallback;
import com.dk.bleNfc.BleNfcDeviceService;
import com.dk.bleNfc.DeviceManager.BleNfcDevice;
import com.dk.bleNfc.DeviceManager.ComByteManager;
import com.dk.bleNfc.DeviceManager.DeviceManager;
import com.dk.bleNfc.DeviceManager.DeviceManagerCallback;
import com.dk.bleNfc.Exception.CardNoResponseException;
import com.dk.bleNfc.Exception.DeviceNoResponseException;
import com.dk.bleNfc.Tool.StringTool;
import com.dk.bleNfc.card.Ntag21x;
import com.gc.nfc.R;
import com.gc.nfc.app.AppContext;
import com.gc.nfc.common.NetRequestConstant;
import com.gc.nfc.common.NetUrlConstant;
import com.gc.nfc.domain.User;
import com.gc.nfc.interfaces.Netcallback;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.gc.nfc.utils.AmapLocationService;
import com.gc.nfc.utils.SharedPreferencesHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.util.DisplayMetrics;
public class StockManagerActivity extends BaseActivity implements OnClickListener  {
	BleNfcDeviceService mBleNfcDeviceService;
	private BleNfcDevice bleNfcDevice;
	private Scanner mScanner;
	private ImageView m_imageViewSearchBlue = null;
	private EditText msgText = null;
	private ProgressDialog readWriteDialog = null;

	private StringBuffer msgBuffer;
	private BluetoothDevice mNearestBle = null;
	private Lock mNearestBleLock = new ReentrantLock();// 锁对象
	private int lastRssi = -100;
	//=========================
	private TextView m_textViewTotalCountZP;// 重瓶数量
	private Button m_buttonNext;//下一步
	private ListView m_listView_zp;// 重瓶号列表
	private  RadioGroup  radioGroup_nfc=null;
	private  RadioButton  radioButton_kp,radioButton_zp;//nfc空瓶/重瓶录入
	private SwipeRefreshLayout swipeRefreshLayout;
	private AppContext appContext;


	private User m_curLoginUserId;


	private List<String> m_BottlesListZP;//瓶表

	private int m_selected_nfc_model;//0--入库 1--出库

	Bundle m_bundle;//上个activity传过来的参数

	private EditText  m_bottleIdZPEditText;//手动输入空重瓶号
	private ImageView m_imageAddZPManual; //手动输入重瓶号
	private TextView m_editTextTakeOverUserId;//交接人
	private ImageView m_imageViewScan; //扫码识别用户
	private TextView m_textView_Department; //责任部门
	private String m_takerOverUserId;//出库为dest人员·入库为src人员
	private String m_takeOverGroupCode;//交接人角色信息

	private Toast toast = null;
	TextView tv;//toast--view
	private Intent m_IntentAmapServeice;

	/*
	 * 暂停Activity，界面获取焦点，按钮可以点击
	 */


	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}
	@Override
	void init() {

		setContentView(R.layout.activity_stock_manage);

		//控件初始化
		m_buttonNext = (Button) findViewById(R.id.button_next);//下一步按钮
		m_listView_zp = (ListView) findViewById(R.id.listview_zp);
		radioGroup_nfc=(RadioGroup)findViewById(R.id.radioGroup_nfc_id);
		radioButton_kp=(RadioButton)findViewById(R.id.radioButton_kp_id);
		radioButton_zp=(RadioButton)findViewById(R.id.radioButton_zp_id);

		m_bottleIdZPEditText = (EditText) findViewById(R.id.input_bottleIdZP);
		m_imageAddZPManual = (ImageView) findViewById(R.id.imageView_addZPManual);
		m_imageViewScan = (ImageView) findViewById(R.id.imageView_Scan);
		m_textViewTotalCountZP =(TextView) findViewById(R.id.items_totalCountZP);
		m_imageAddZPManual.setOnClickListener(this);
		m_imageViewScan.setOnClickListener(this);

		m_buttonNext.setOnClickListener(this);
		radioGroup_nfc.setOnCheckedChangeListener(listen);
		radioGroup_nfc.check(radioButton_kp.getId());//默认是空瓶

		//数据结构初始化
		m_BottlesListZP = new ArrayList<String>();

		m_imageViewScan = (ImageView) findViewById(R.id.imageView_Scan);
		m_editTextTakeOverUserId = (TextView) findViewById(R.id.input_TakeOverUserId);
		m_textView_Department = (TextView) findViewById(R.id.textView_Department);

		//获取当前
		appContext = (AppContext) getApplicationContext();
		m_curLoginUserId = appContext.getUser();
		if (m_curLoginUserId == null) {
			Toast.makeText(StockManagerActivity.this, "登陆会话失效", Toast.LENGTH_LONG).show();
			Intent intent = new Intent(StockManagerActivity.this, LoginActivity.class);
			startActivity(intent);
			finish();
		}

		//初始化两个LISTVIEW的点击事件，目前没有实现交接的回撤
		m_listView_zp.setOnItemLongClickListener(new OnItemLongClickListener() {

			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				return true;
			}
		});
		m_takerOverUserId = null;
		//获取责任部门
		String username = m_curLoginUserId.getUsername();
		m_textView_Department.setText(username+"("+m_curLoginUserId.getGroupName()+"|"+m_curLoginUserId.getDepartmentName()+")");

		//蓝牙设备初始化
		blueDeviceInitial();

//		//开启定位任务
//		isOpenGPS();
//		//开启定位
//		m_IntentAmapServeice = new Intent(this,AmapLocationService.class);
//		startService(m_IntentAmapServeice);
//		final Intent intentServiceWatch = new Intent(this,com.gc.nfc.utils.RomoteService.class);
//		startService(intentServiceWatch);

	}

	private OnCheckedChangeListener  listen=new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			int id= group.getCheckedRadioButtonId();
			switch (group.getCheckedRadioButtonId()) {
				case R.id.radioButton_kp_id:
					m_selected_nfc_model = 0;
					break;
				case R.id.radioButton_zp_id:
					m_selected_nfc_model = 1;
					break;
				default:
					break;
			}
		}};

	//动态设置ListView的高度
	private void setListViewHeightBasedOnChildren(ListView listView) {
		if (listView == null) {
			return;
		}
		ListAdapter listAdapter = listView.getAdapter();
		if (listAdapter == null) {
			return;
		}
		int totalHeight = 0;
		for (int i = 0; i < listAdapter.getCount(); i++) {
			View listItem = listAdapter.getView(i, null, listView);
			listItem.measure(0, 0);
			totalHeight += listItem.getMeasuredHeight();
		}
		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
		listView.setLayoutParams(params);
	}


	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button_next:
				cleanAll();
				break;
			case R.id.imageView_addZPManual://手动添加钢瓶号
				//后面讨论如何处理第一次入库
				if(m_takerOverUserId == null){
					showToast("请扫码获取交接人信息！");
					return;
				}
				String bottleCode = m_bottleIdZPEditText.getText().toString();
				String bottleStatus = new String();
				if(m_selected_nfc_model==0){//ruku
					if(m_curLoginUserId.getGroupCode().equals("00005")){//门店
						bottleStatus = "2";//门店库存
					}else if(m_curLoginUserId.getGroupCode().equals("00006")){//充气站
						bottleStatus = "1";//充气站库存
					}else{
						Toast.makeText(StockManagerActivity.this, "非充气站或门店账户，请退出！",	Toast.LENGTH_LONG).show();
						return;
					}
					bottleTakeOverUnit(bottleCode, m_takerOverUserId, m_curLoginUserId.getUsername(), bottleStatus, m_curLoginUserId.getDepartmentName()+"|钢瓶入库", false, true);//钢瓶入库
				}else if(m_selected_nfc_model==1){//chuku
					if(m_curLoginUserId.getGroupCode().equals("00005")){//门店
						if(m_takeOverGroupCode.equals("00007")){//配送车
							bottleStatus = "3";//在途运输
						}else if(m_takeOverGroupCode.equals("00003")){//配送工
							bottleStatus = "4";//在途派送
						}else {
							Toast.makeText(StockManagerActivity.this, "非配送工或调拨车账户，请更换！",	Toast.LENGTH_LONG).show();
							return;
						}
					}else if(m_curLoginUserId.getGroupCode().equals("00006")){//充气站
						bottleStatus = "3";//在途运输
					}else{
						Toast.makeText(StockManagerActivity.this, "非充气站或门店账户，请退出！",	Toast.LENGTH_LONG).show();
						return;
					}
					bottleTakeOverUnit(bottleCode, m_curLoginUserId.getUsername(), m_takerOverUserId, bottleStatus, m_curLoginUserId.getDepartmentName()+"|钢瓶出库", false, false);//钢瓶出库
				}

				break;
			case R.id.imageView_Scan://扫描用户二维码
				startScan();
				break;
			default:
				break;
		}

	}
	private void startScan() {
		new IntentIntegrator(this)
				.setOrientationLocked(false)
				.setCaptureActivity(CustomScanActivity.class) // 设置自定义的activity是CustomActivity
				.initiateScan(); // 初始化扫描
	}

	@Override
// 通过 onActivityResult的方法获取 扫描回来的 值
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode,resultCode,data);
		if(intentResult != null) {
			if(intentResult.getContents() == null) {
				Toast.makeText(this,"内容为空",Toast.LENGTH_LONG).show();
			} else {
				//Toast.makeText(this,"扫描成功",Toast.LENGTH_LONG).show();
				// ScanResult 为 获取到的字符串
				String takerOverUserId = intentResult.getContents();

				//查询交接人信息
				sysUserInfoQuery(takerOverUserId);
			}
		} else {
			super.onActivityResult(requestCode,resultCode,data);
		}
	}
	private void sysUserInfoQuery(String userId){
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);
		nrc.requestUrl = NetUrlConstant.SYSUSERQUERYURL;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("userId", userId);

		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==200){
							try {
								JSONObject responseJson = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));


								JSONObject userJson = responseJson;
								JSONObject groupJson = userJson.getJSONObject("userGroup");
								String groupCode = groupJson.optString("code");
								String userId = userJson.optString("userId");
								String userName	 = userJson.optString("name");
								String groupName	 = groupJson.optString("name");
								m_takeOverGroupCode = groupCode;
								if(groupCode.equals("00007")){//配送车
									m_takerOverUserId = userId;
									m_editTextTakeOverUserId.setText(userId+" ("+"姓名:"+userName+" | 工作组:"+groupName+")");
								}else if(groupCode.equals("00003")){//配送工
									m_takerOverUserId = userId;
									m_editTextTakeOverUserId.setText(userId+" ("+"姓名:"+userName+" | 工作组:"+groupName+")");
								}else {
									Toast.makeText(StockManagerActivity.this, "非配送工或调拨车账户，请更换！",	Toast.LENGTH_LONG).show();
								}


							}catch (IOException e){
								Toast.makeText(StockManagerActivity.this, "无效账户！",
										Toast.LENGTH_LONG).show();
								Intent intent = new Intent(getApplicationContext() , LoginActivity.class);
								startActivity(intent);
								finish();
							}catch (JSONException e) {
								Toast.makeText(StockManagerActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
								Intent intent = new Intent(getApplicationContext() , LoginActivity.class);
								startActivity(intent);
								finish();
							}

						}else{
							Toast.makeText(StockManagerActivity.this, "请求交接人信息失败", Toast.LENGTH_LONG).show();
//							Intent intent = new Intent(getApplicationContext() , LoginActivity.class);
//							startActivity(intent);
//							finish();
						}
					}else {
						Toast.makeText(StockManagerActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
						Intent intent = new Intent(getApplicationContext() , LoginActivity.class);
						startActivity(intent);
						finish();
					}
				} else {
					Toast.makeText(StockManagerActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
					Intent intent = new Intent(getApplicationContext() , LoginActivity.class);
					startActivity(intent);
					finish();
				}
			}
		}, nrc);
	}

	//NFC更新重瓶表
	private void refleshBottlesListZP(){
		m_textViewTotalCountZP.setText(Integer.toString(m_BottlesListZP.size()));
		List<Map<String,Object>> list_map = new ArrayList<Map<String,Object>>(); //定义一个适配器对象

		for(int i=0;i<m_BottlesListZP.size(); i++){
			Map<String,Object> bottleInfo = new HashMap<String, Object>(); //创建一个键值对的Map集合，用来存放名字和头像
			bottleInfo.put("bottleCode", m_BottlesListZP.get(i));
			list_map.add(bottleInfo);   //把这个存放好数据的Map集合放入到list中，这就完成类数据源的准备工作
		}
		//2、创建适配器（可以使用外部类的方式、内部类方式等均可）
		SimpleAdapter simpleAdapter = new SimpleAdapter(
				StockManagerActivity.this,/*传入一个上下文作为参数*/
				list_map,         /*传入相对应的数据源，这个数据源不仅仅是数据而且还是和界面相耦合的混合体。*/
				R.layout.bottle_list_simple_items, /*设置具体某个items的布局，需要是新的布局，而不是ListView控件的布局*/
				new String[]{"bottleCode"}, /*传入上面定义的键值对的键名称,会自动根据传入的键找到对应的值*/
				new int[]{R.id.items_number}) ;
		m_listView_zp.setAdapter(simpleAdapter);
		setListViewHeightBasedOnChildren(m_listView_zp);
	}

	//单个钢瓶交接
	public void bottleTakeOverUnit(final String bottleCode, final String srcUserId, final String targetUserId, final String serviceStatus, final String note, final boolean enableForce, final boolean isRuKu) {
		//后面讨论如何处理第一次入库
		if(m_takerOverUserId == null){
			showToast("请扫码获取交接人信息！");
			return;
		}
		//如果存在交接记录表里，就提示已经存在了

		boolean contained = false;

		for(int i=0; i<m_BottlesListZP.size();i++){
			if(m_BottlesListZP.get(i).equals(bottleCode)){
				contained = true;
				break;
			}
		}


		if(contained){//已经存在了
			showToast("钢瓶号："+bottleCode+"    请勿重复提交！");

			return;
		}

		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.PUT);
		nrc.requestUrl = NetUrlConstant.BOTTLETAKEOVERURL+"/"+bottleCode;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("srcUserId",srcUserId);//用户号
		params.put("targetUserId",targetUserId);
		params.put("serviceStatus",serviceStatus);
		params.put("enableForce",enableForce);
		params.put("note",note);//详细描述
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==200){
							MediaPlayer music = MediaPlayer.create(StockManagerActivity.this, R.raw.nfcok);
							music.start();
							if(isRuKu){
							}else {
								if(m_curLoginUserId.getGroupCode().equals("00006")) {//充气站
									bottlesStatusKPtoZP(bottleCode);//将空瓶状态转换为重瓶
								}
							}
							addZP(bottleCode);
						}else{
							MediaPlayer music = MediaPlayer.create(StockManagerActivity.this, R.raw.alarm);
							music.start();
							new AlertDialog.Builder(StockManagerActivity.this).setTitle("钢瓶异常流转！")
									.setMessage("钢瓶号 :"+bottleCode+"\r\n"+"错误原因:"+getResponseMessage(response)+"\r\n确认强制交接吗？")
									.setIcon(R.drawable.icon_logo)
									.setPositiveButton("确定",
											new DialogInterface.OnClickListener()
											{
												@Override
												public void onClick(DialogInterface dialog,
																	int which)
												{
													//强制交接
													bottleTakeOverUnit(bottleCode, srcUserId, targetUserId, serviceStatus, note, true, isRuKu);
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
						}
					}else {
						Toast.makeText(StockManagerActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(StockManagerActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
	}


	//单个钢瓶状态由重瓶转为空瓶
	public void bottlesStatusKPtoZP(String bottleCode) {
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.PUT);
		nrc.requestUrl = NetUrlConstant.GASCYLINDERURL+"/"+bottleCode;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		Map<String, Object> body = new HashMap<String, Object>();
		body.put("loadStatus","LSHeavy");//重瓶
		nrc.setParams(params);
		nrc.setBody(body);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==200){
						}
					}else {
						Toast.makeText(StockManagerActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(StockManagerActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
	}

	private void addZP(String bottleCode) {

		boolean contained = false;
		for (int i = 0; i < m_BottlesListZP.size(); i++) {
			if (m_BottlesListZP.get(i).equals(bottleCode)) {
				contained = true;
				break;
			}
		}
		if (!contained) {//第一次扫
			showToast(bottleCode);
			m_BottlesListZP.add(bottleCode);
			refleshBottlesListZP();
		}

	}

	private String getResponseMessage(HttpResponse response) {
		try {

			BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
					.getContent()));
			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			in.close();
			String responseBody = sb.toString();

			if (responseBody.equals("")) {
				responseBody = "{\"message\":\"no value\"}";
			}

			JSONObject errorDetailJson = new JSONObject(responseBody);
			String errorDetail = errorDetailJson.get("message").toString();
			return errorDetail;
		} catch (IOException e) {
			Toast.makeText(StockManagerActivity.this, "未知错误，异常！" + e.getMessage(),
					Toast.LENGTH_LONG).show();
			return null;
		} catch (JSONException e) {
			Toast.makeText(StockManagerActivity.this, "未知错误，异常！" + e.getMessage(),
					Toast.LENGTH_LONG).show();
			return null;
		}
	}

	private void blueDeviceInitial(){
		msgText = (EditText)findViewById(R.id.msgText);
		m_imageViewSearchBlue= (ImageView) findViewById(R.id.imageView_search);
		m_imageViewSearchBlue.setOnClickListener(new StartSearchButtonListener());
		msgBuffer = new StringBuffer();
		//ble_nfc服务初始化
		Intent gattServiceIntent = new Intent(this, BleNfcDeviceService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}
	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			BleNfcDeviceService mBleNfcDeviceService = ((BleNfcDeviceService.LocalBinder) service).getService();
			bleNfcDevice = mBleNfcDeviceService.bleNfcDevice;
			mScanner = mBleNfcDeviceService.scanner;
			mBleNfcDeviceService.setDeviceManagerCallback(deviceManagerCallback);
			mBleNfcDeviceService.setScannerCallback(scannerCallback);
			//开始搜索设备
			searchNearestBleDevice();
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBleNfcDeviceService = null;
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		if (mBleNfcDeviceService != null) {
			mBleNfcDeviceService.setScannerCallback(scannerCallback);
			mBleNfcDeviceService.setDeviceManagerCallback(deviceManagerCallback);
		}
	}
	@Override
	public void onDestroy() {
		super.onDestroy();

		if (readWriteDialog != null) {
			readWriteDialog.dismiss();
		}

		unbindService(mServiceConnection);
	}
	//Scanner 回调
	private ScannerCallback scannerCallback = new ScannerCallback() {
		@Override
		public void onReceiveScanDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
			super.onReceiveScanDevice(device, rssi, scanRecord);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //StringTool.byteHexToSting(scanRecord.getBytes())
				System.out.println("Activity搜到设备：" + device.getName()
						+ " 信号强度：" + rssi
						+ " scanRecord：" + StringTool.byteHexToSting(scanRecord));
			}

			//搜索蓝牙设备并记录信号强度最强的设备
			if ( (scanRecord != null) && (StringTool.byteHexToSting(scanRecord).contains("017f5450"))) {  //从广播数据中过滤掉其它蓝牙设备
				if (rssi < -55) {
					return;
				}
				//msgBuffer.append("搜到设备：").append(device.getName()).append(" 信号强度：").append(rssi).append("\r\n");
				handler.sendEmptyMessage(0);
				if (mNearestBle != null) {
					if (rssi > lastRssi) {
						mNearestBleLock.lock();
						try {
							mNearestBle = device;
						}finally {
							mNearestBleLock.unlock();
						}
					}
				}
				else {
					mNearestBleLock.lock();
					try {
						mNearestBle = device;
					}finally {
						mNearestBleLock.unlock();
					}
					lastRssi = rssi;
				}
			}
		}

		@Override
		public void onScanDeviceStopped() {
			super.onScanDeviceStopped();
		}
	};

	//设备操作类回调
	private DeviceManagerCallback deviceManagerCallback = new DeviceManagerCallback() {
		@Override
		public void onReceiveConnectBtDevice(boolean blnIsConnectSuc) {
			super.onReceiveConnectBtDevice(blnIsConnectSuc);
			if (blnIsConnectSuc) {
				System.out.println("Activity设备连接成功");
				msgBuffer.delete(0, msgBuffer.length());
				msgBuffer.append("设备连接成功!");
				if (mNearestBle != null) {
					//msgBuffer.append("设备名称：").append(bleNfcDevice.getDeviceName()).append("\r\n");
				}
				//msgBuffer.append("信号强度：").append(lastRssi).append("dB\r\n");
				//msgBuffer.append("SDK版本：" + BleNfcDevice.SDK_VERSIONS + "\r\n");

				//连接上后延时500ms后再开始发指令
				try {
					Thread.sleep(500L);
					handler.sendEmptyMessage(3);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void onReceiveDisConnectDevice(boolean blnIsDisConnectDevice) {
			super.onReceiveDisConnectDevice(blnIsDisConnectDevice);
			System.out.println("Activity设备断开链接");
			msgBuffer.delete(0, msgBuffer.length());
			msgBuffer.append("设备断开链接!");
			handler.sendEmptyMessage(0);
		}

		@Override
		public void onReceiveConnectionStatus(boolean blnIsConnection) {
			super.onReceiveConnectionStatus(blnIsConnection);
			System.out.println("Activity设备链接状态回调");
		}

		@Override
		public void onReceiveInitCiphy(boolean blnIsInitSuc) {
			super.onReceiveInitCiphy(blnIsInitSuc);
		}

		@Override
		public void onReceiveDeviceAuth(byte[] authData) {
			super.onReceiveDeviceAuth(authData);
		}

		@Override
		//寻到卡片回调
		public void onReceiveRfnSearchCard(boolean blnIsSus, int cardType, byte[] bytCardSn, byte[] bytCarATS) {
			super.onReceiveRfnSearchCard(blnIsSus, cardType, bytCardSn, bytCarATS);
			if (!blnIsSus || cardType == BleNfcDevice.CARD_TYPE_NO_DEFINE) {
				return;
			}
			System.out.println("Activity接收到激活卡片回调：UID->" + StringTool.byteHexToSting(bytCardSn) + " ATS->" + StringTool.byteHexToSting(bytCarATS));

			final int cardTypeTemp = cardType;
			new Thread(new Runnable() {
				@Override
				public void run() {
					boolean isReadWriteCardSuc;
					try {
						if (bleNfcDevice.isAutoSearchCard()) {
							//如果是自动寻卡的，寻到卡后，先关闭自动寻卡
							bleNfcDevice.stoptAutoSearchCard();
							isReadWriteCardSuc = readWriteCardDemo(cardTypeTemp);

							//读卡结束，重新打开自动寻卡
							startAutoSearchCard();
						}
						else {
							isReadWriteCardSuc = readWriteCardDemo(cardTypeTemp);

							//如果不是自动寻卡，读卡结束,关闭天线
							bleNfcDevice.closeRf();
						}

						//打开蜂鸣器提示读卡完成
						if (isReadWriteCardSuc) {
							bleNfcDevice.openBeep(50, 50, 3);  //读写卡成功快响3声
						}
						else {
							bleNfcDevice.openBeep(100, 100, 2); //读写卡失败慢响2声
						}
					} catch (DeviceNoResponseException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}

		@Override
		public void onReceiveRfmSentApduCmd(byte[] bytApduRtnData) {
			super.onReceiveRfmSentApduCmd(bytApduRtnData);
			System.out.println("Activity接收到APDU回调：" + StringTool.byteHexToSting(bytApduRtnData));
		}

		@Override
		public void onReceiveRfmClose(boolean blnIsCloseSuc) {
			super.onReceiveRfmClose(blnIsCloseSuc);
		}
		@Override
		//按键返回回调
		public void onReceiveButtonEnter(byte keyValue) {}
	};
	//读写卡Demo
	private boolean readWriteCardDemo(int cardType) {
		switch (cardType) {
			case DeviceManager.CARD_TYPE_ULTRALIGHT: //寻到Ultralight卡
				final Ntag21x ntag21x = (Ntag21x) bleNfcDevice.getCard();
				if (ntag21x != null) {
					try {
						//ntag21x.NdefTextWrite("KMA123456789");
						String bottleCode = ntag21x.NdefTextRead();
						Message msg = new Message();
						msg.obj = bottleCode;
						msg.what = 0x88;//标签读取事件
						handler.sendMessage(msg);



					} catch (CardNoResponseException e) {
						e.printStackTrace();
						return false;
					}
				}
				break;
			default:
				break;
		}
		return true;
	}

	//搜索最近的设备并连接
	private void searchNearestBleDevice() {
		msgBuffer.delete(0, msgBuffer.length());
		msgBuffer.append("正在搜索设备...");
		handler.sendEmptyMessage(0);
		if (!mScanner.isScanning() && (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					synchronized (this) {
						mScanner.startScan(0);
						mNearestBleLock.lock();
						try {
							mNearestBle = null;
						}finally {
							mNearestBleLock.unlock();
						}
						lastRssi = -100;

						int searchCnt = 0;
						while ((mNearestBle == null)
								&& (searchCnt < 20000)
								&& (mScanner.isScanning())
								&& (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
							searchCnt++;
							try {
								Thread.sleep(1);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}

						if (mScanner.isScanning() && (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							mScanner.stopScan();
							mNearestBleLock.lock();
							try {
								if (mNearestBle != null) {
									mScanner.stopScan();
									msgBuffer.delete(0, msgBuffer.length());
									msgBuffer.append("正在连接设备...");
									handler.sendEmptyMessage(0);
									bleNfcDevice.requestConnectBleDevice(mNearestBle.getAddress());
								} else {
									msgBuffer.delete(0, msgBuffer.length());
									msgBuffer.append("未找到设备！");
									handler.sendEmptyMessage(0);
								}
							}finally {
								mNearestBleLock.unlock();
							}
						} else {
							mScanner.stopScan();
						}
					}
				}
			}).start();
		}
	}

	//发送读写进度条显示Handler
	private void showReadWriteDialog(String msg, int rate) {
		Message message = new Message();
		message.what = 4;
		message.arg1 = rate;
		message.obj = msg;
		handler.sendMessage(message);
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			msgText.setText(msgBuffer);

			if ( (bleNfcDevice.isConnection() == BleManager.STATE_CONNECTED) || ((bleNfcDevice.isConnection() == BleManager.STATE_CONNECTING)) ) {
				//searchButton.setText("断开连接");
			}
			else {
				//searchButton.setText("搜索设备");
			}

			switch (msg.what) {
				case 1:
					break;
				case 2:
					break;
				case 3:
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								byte versions = bleNfcDevice.getDeviceVersions();
								//msgBuffer.append("设备版本:").append(String.format("%02x", versions)).append("\r\n");
								handler.sendEmptyMessage(0);
								double voltage = bleNfcDevice.getDeviceBatteryVoltage();
								//msgBuffer.append("设备电池电压:").append(String.format("%.2f", voltage)).append("\r\n");
								if (voltage < 3.61) {
									msgBuffer.append("(电量低)");
								} else {
									msgBuffer.append("(电量充足)");
								}

								handler.sendEmptyMessage(0);
								boolean isSuc = bleNfcDevice.androidFastParams(true);
								if (isSuc) {
									//msgBuffer.append("\r\n蓝牙快速传输参数设置成功!");
								}
								else {
									//msgBuffer.append("\n不支持快速传输参数设置!");
								}
								handler.sendEmptyMessage(0);

								//msgBuffer.append("\n开启自动寻卡...\r\n");
								handler.sendEmptyMessage(0);
								//开始自动寻卡
								startAutoSearchCard();
							} catch (DeviceNoResponseException e) {
								e.printStackTrace();
							}
						}
					}).start();
					break;
				case 0x88:
					//后面讨论如何处理第一次入库
					if(m_takerOverUserId == null){//
						showToast("请扫码获取交接人信息！");
						return;
					}
					String bottleCode = msg.obj.toString();
					String bottleStatus = new String();
					if(m_selected_nfc_model==0){//ruku
						if(m_curLoginUserId.getGroupCode().equals("00005")){//门店
							bottleStatus = "2";//门店库存
						}else if(m_curLoginUserId.getGroupCode().equals("00006")){//充气站
							bottleStatus = "1";//充气站库存
						}else{
							Toast.makeText(StockManagerActivity.this, "非充气站或门店账户，请退出！",	Toast.LENGTH_LONG).show();
							return;
						}
						bottleTakeOverUnit(bottleCode, m_takerOverUserId, m_curLoginUserId.getUsername(), bottleStatus, m_curLoginUserId.getDepartmentName()+"|钢瓶入库", false, true);//钢瓶入库
					}else if(m_selected_nfc_model==1){//chuku
						if(m_curLoginUserId.getGroupCode().equals("00005")){//门店
							if(m_takeOverGroupCode.equals("00007")){//配送车
								bottleStatus = "3";//在途运输
							}else if(m_takeOverGroupCode.equals("00003")){//配送工
								bottleStatus = "4";//在途派送
							}else {
								Toast.makeText(StockManagerActivity.this, "非配送工或调拨车账户，请更换！",	Toast.LENGTH_LONG).show();
								return;
							}
						}else if(m_curLoginUserId.getGroupCode().equals("00006")){//充气站
							bottleStatus = "3";//在途运输
						}else{
							Toast.makeText(StockManagerActivity.this, "非充气站或门店账户，请退出！",	Toast.LENGTH_LONG).show();
							return;
						}
						bottleTakeOverUnit(bottleCode, m_curLoginUserId.getUsername(), m_takerOverUserId, bottleStatus, m_curLoginUserId.getDepartmentName()+"|钢瓶出库", false, false);//钢瓶出库
					}
					break;

			}
		}
	};
	//开始自动寻卡
	private boolean startAutoSearchCard() throws DeviceNoResponseException {
		//打开自动寻卡，200ms间隔，寻M1/UL卡
		boolean isSuc = false;
		int falseCnt = 0;
		do {
			isSuc = bleNfcDevice.startAutoSearchCard((byte) 20, ComByteManager.ISO14443_P4);
		}while (!isSuc && (falseCnt++ < 10));
		if (!isSuc){
			//msgBuffer.delete(0, msgBuffer.length());
			msgBuffer.append("不支持自动寻卡！\r\n");
			handler.sendEmptyMessage(0);
		}
		return isSuc;
	}
	//搜索按键监听
	private class StartSearchButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			if ( (bleNfcDevice.isConnection() == BleManager.STATE_CONNECTED) ) {
				bleNfcDevice.requestDisConnectDevice();
				return;
			}
			searchNearestBleDevice();
		}
	}

	private void cleanAll(){
		m_takerOverUserId = null;
		m_BottlesListZP.clear();
		refleshBottlesListZP();
		m_editTextTakeOverUserId.setText("");


	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 &&event.getAction() == KeyEvent.ACTION_DOWN)        {
			new AlertDialog.Builder(StockManagerActivity.this).setTitle("提示")
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
					.setNegativeButton("退出登录",
							new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog,
													int which)
								{
									loginOut();//重新登陆
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
	private void loginOut(){
		SharedPreferencesHelper.put("username", "");
		SharedPreferencesHelper.put("password", "");
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);
		final String username = m_curLoginUserId.getUsername();
		final String password = m_curLoginUserId.getPassword();
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
							Toast.makeText(StockManagerActivity.this, "退出登录失败", Toast.LENGTH_LONG).show();
						}
					}else {
						Toast.makeText(StockManagerActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(StockManagerActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
	}

	private void showToast(String info){
		if(toast ==null){
			toast = Toast.makeText(StockManagerActivity.this, null, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			LinearLayout toastView = (LinearLayout)toast.getView();
			WindowManager wm = (WindowManager)this.getSystemService(this.WINDOW_SERVICE);
			DisplayMetrics outMetrics = new DisplayMetrics();
			wm.getDefaultDisplay().getMetrics(outMetrics);
			tv=new TextView(this);
			toastView.getBackground().setAlpha(0);//0~255透明度值
			//toastView.setBackgroundResource(R.drawable.ic_menu_deal_on);
			tv.setTextSize(40);
			tv.setTextColor(getResources().getColor(R.color.colorAccent));
			toastView.setGravity(Gravity.CENTER);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			params.setMargins(0, 0, 0, 180);
			tv.setLayoutParams(params);
			toast.setView(toastView);
			toastView.addView(tv);
		}
		tv.setText(info);
		toast.show();
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

}
