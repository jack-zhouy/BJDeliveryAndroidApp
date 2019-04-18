package com.gc.nfc.ui;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class BottleRecycleActivity extends BaseActivity implements OnClickListener  {
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

	private  RadioGroup  radioGroup_nfc=null;
	private  RadioButton  radioButton_kp_recyle,radioButton_zp_recyle,radioButton_zp_reput;//nfc空瓶/重瓶录入

	private RelativeLayout m_relativeLayout_san;//用户卡扫码窗口
	private TextView m_textViewUserInfo;// 客户信息
	private TextView m_textViewTotalCountKP;// 空瓶数量
	private Button m_buttonNext;//下一步
	private ListView m_listView_kp;// 空瓶号列表
	private ImageView m_imageViewKPEye; //有效空瓶查看
	private ImageView m_imageViewSanUserCard; //用户卡扫码窗口
	private AppContext appContext;
	private User m_deliveryUser;//配送工

	private Map<String, String> m_BottlesMapKP;//空瓶表

	private boolean m_orderServiceQualityShowFlag;//是否是用户评价阶段

	AlertDialog m_dialog;//用户卡弹窗
	private String m_curUserId = null;
	private String m_curUserInfo = null;

	private Toast toast = null;
	TextView tv;//toast--view
	/**
	 * 暂停Activity，界面获取焦点，按钮可以点击
	 */

	private int m_selected_nfc_model;//0--空瓶回收 1--重瓶回收 2--重瓶落户


	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}
	@Override
	void init() {

		setContentView(R.layout.activity_bottle_recycle);

		//控件初始化
		m_buttonNext = (Button) findViewById(R.id.button_next);//下一步按钮
		m_listView_kp = (ListView) findViewById(R.id.listview_kp);
		m_imageViewKPEye = (ImageView) findViewById(R.id.imageView_KPEYE);
		m_imageViewSanUserCard = (ImageView) findViewById(R.id.imageView_Scan);
		m_textViewTotalCountKP = (TextView) findViewById(R.id.items_totalCountKP);
		m_textViewUserInfo = (TextView) findViewById(R.id.textView_userInfo);
		m_relativeLayout_san = (RelativeLayout) findViewById(R.id.RelativeLayout_san);

		radioGroup_nfc=(RadioGroup)findViewById(R.id.radioGroup_nfc_id);
		radioButton_kp_recyle=(RadioButton)findViewById(R.id.radioButton_kp_recyle);
		radioButton_zp_recyle=(RadioButton)findViewById(R.id.radioButton_zp_recyle);
		radioButton_zp_reput=(RadioButton)findViewById(R.id.radioButton_zp_reput);

		m_imageViewKPEye.setOnClickListener(this);
		m_buttonNext.setOnClickListener(this);
		m_imageViewSanUserCard.setOnClickListener(this);
		m_relativeLayout_san.setOnClickListener(this);

		//数据结构初始化

		m_BottlesMapKP = new HashMap<String,String>();



		//获取当前配送工
		appContext = (AppContext) getApplicationContext();
		m_deliveryUser = appContext.getUser();
		if (m_deliveryUser == null) {
			Toast.makeText(BottleRecycleActivity.this, "登陆会话失效", Toast.LENGTH_LONG).show();
			Intent intent = new Intent(BottleRecycleActivity.this, LoginActivity.class);
			startActivity(intent);
			finish();
		}
		//蓝牙设备初始化
		blueDeviceInitial();
		//默认刚开始是录钢瓶阶段
		m_orderServiceQualityShowFlag = false;


		radioGroup_nfc.setOnCheckedChangeListener(listen);
		radioGroup_nfc.check(radioButton_kp_recyle.getId());//默认是空瓶
	}

	private OnCheckedChangeListener  listen=new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			int id= group.getCheckedRadioButtonId();
			switch (group.getCheckedRadioButtonId()) {
				case R.id.radioButton_kp_recyle:
					m_selected_nfc_model = 0;
					break;
				case R.id.radioButton_zp_recyle:
					m_selected_nfc_model = 1;
					break;
				case R.id.radioButton_zp_reput:
					m_selected_nfc_model = 2;
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
				finish();
				break;
			case R.id.imageView_KPEYE:// 用户的气瓶
				if(m_curUserId==null){
					showToast("请扫描用户卡！");
					return;
				}
				Intent intentKP = new Intent();
				Bundle bundleKP = new Bundle();
				bundleKP.putString("userId", m_curUserId);
				intentKP.setClass(BottleRecycleActivity.this, MybottlesActivity.class);
				intentKP.putExtras(bundleKP);
				startActivity(intentKP);
				break;
			case R.id.imageView_Scan:// 扫用户卡弹窗
			case  R.id.RelativeLayout_san:
				orderServiceQualityShow();
				break;
			default:
				break;
		}

	}


	//NFC更新空瓶表
	private void refleshBottlesListKP(){
		m_textViewTotalCountKP.setText(Integer.toString(m_BottlesMapKP.size()));
		List<Map<String,Object>> list_map = new ArrayList<Map<String,Object>>(); //定义一个适配器对象
		for (Map.Entry<String, String> entry : m_BottlesMapKP.entrySet()) {
			Map<String,Object> bottleInfo = new HashMap<String, Object>(); //创建一个键值对的Map集合，用来存放名字和头像

			bottleInfo.put("bottleCode", entry.getKey());
			bottleInfo.put("bottleWeight", entry.getValue());
			list_map.add(bottleInfo);   //把这个存放好数据的Map集合放入到list中，这就完成类数据源的准备工作
		}

		//2、创建适配器（可以使用外部类的方式、内部类方式等均可）
		SimpleAdapter simpleAdapter = new SimpleAdapter(
				BottleRecycleActivity.this,/*传入一个上下文作为参数*/
				list_map,         /*传入相对应的数据源，这个数据源不仅仅是数据而且还是和界面相耦合的混合体。*/
				R.layout.bottle_list_simple_items, /*设置具体某个items的布局，需要是新的布局，而不是ListView控件的布局*/
				new String[]{"bottleCode","bottleWeight"}, /*传入上面定义的键值对的键名称,会自动根据传入的键找到对应的值*/
				new int[]{R.id.items_number,R.id.items_weight}) ;

		m_listView_kp.setAdapter(simpleAdapter);
		setListViewHeightBasedOnChildren(m_listView_kp);
	}


	//单个钢瓶交接
	public void bottleTakeOverUnit(final String bottleCode, final String srcUserId, final String targetUserId, final String serviceStatus, final String note, final boolean enableForce, final boolean isKP, final boolean isChangeFillingStatus,final String takeReason) {


		//如果存在交接记录表里，就提示已经存在了
		boolean contained = false;

		if(m_BottlesMapKP.containsKey(bottleCode)){
			contained = true;
		}


		if(contained){//已经存在了
			Toast.makeText(BottleRecycleActivity.this, "钢瓶号："+bottleCode+"    请勿重复提交！",
					Toast.LENGTH_LONG).show();
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
							MediaPlayer music = MediaPlayer.create(BottleRecycleActivity.this, R.raw.nfcok);
							music.start();
							addKP(bottleCode, takeReason);

						}else{
							MediaPlayer music = MediaPlayer.create(BottleRecycleActivity.this, R.raw.alarm);
							music.start();
							//409才允许强制交接
							if(response.getStatusLine().getStatusCode()==409){
								new AlertDialog.Builder(BottleRecycleActivity.this).setTitle("钢瓶异常流转！")
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
														bottleTakeOverUnit(bottleCode, srcUserId, targetUserId, serviceStatus, note, true, isKP, isChangeFillingStatus,takeReason);
													}
												})
										.setNegativeButton("取消",
												new DialogInterface.OnClickListener()
												{
													@Override
													public void onClick(DialogInterface dialog,
																		int which) {}
												})
										.show();
							}else{
								new AlertDialog.Builder(BottleRecycleActivity.this).setTitle("钢瓶异常流转！")
										.setMessage("钢瓶号 :"+bottleCode+"\r\n"+"错误原因:"+getResponseMessage(response))
										.setIcon(R.drawable.icon_logo)
										.setPositiveButton("确定",
												new DialogInterface.OnClickListener()
												{
													@Override
													public void onClick(DialogInterface dialog,
																		int which) {
													}
												}).show();
							}
						}
					}else {
						Toast.makeText(BottleRecycleActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(BottleRecycleActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
	}

	private void addKP(final String bottleCode,final String takeReason ){

		if(!m_BottlesMapKP.containsKey(bottleCode)){//第一次扫
			m_BottlesMapKP.put(bottleCode, takeReason);
			refleshBottlesListKP();
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
			Toast.makeText(BottleRecycleActivity.this, "未知错误，异常！" + e.getMessage(),
					Toast.LENGTH_LONG).show();
			return null;
		} catch (JSONException e) {
			Toast.makeText(BottleRecycleActivity.this, "未知错误，异常！" + e.getMessage(),
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
						String textRead = ntag21x.NdefTextRead();
						Message msg = new Message();
						msg.obj = textRead;
						if(m_orderServiceQualityShowFlag){
							msg.what = 0x89;//评价阶段
						}else{
							msg.what = 0x88;//扫码钢瓶阶段
						}

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
				case 0x88://钢瓶扫描
					if(m_curUserId==null){
						showToast("请先扫描用户卡");
						return;
					}
					String bottleCode = msg.obj.toString();
					String textArrayBottle[] = bottleCode.split(":");
					if(textArrayBottle.length==2) {
						showToast("无效钢瓶码格式！");
						return;
					}
					switch (m_selected_nfc_model)
					{
						case 0:
							bottleTakeOverUnit(bottleCode, m_curUserId, m_deliveryUser.getUsername(), "6", "退换货流程"+"|空瓶回收", false, true, true, "空瓶回收");//空瓶回收
							break;
						case 1:
							bottleTakeOverUnit(bottleCode, m_curUserId, m_deliveryUser.getUsername(), "6", "退换货流程"+"|重瓶回收", false, true, false, "重瓶回收");//重瓶回收
							break;
						case 2:
							bottleTakeOverUnit(bottleCode, m_deliveryUser.getUsername(), m_curUserId, "5", "退换货流程"+"|重瓶落户", false, true, true, "重瓶落户");//重瓶落户
							break;
							default:
								break;
					}
					break;
				case 0x89://扫描用户卡
					String readText = msg.obj.toString();
					String textArray[] = readText.split(":");
					if(textArray.length!=2){
						showToast("无效卡格式！");
						return;
					}else {
						String userCardIndex = textArray[1];
						GetUserCardInfo(userCardIndex);
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



	//用户卡评价弹窗
	private void orderServiceQualityShow(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		//builder.setCancelable(false);
		View view = View.inflate(this, R.layout.user_evaluate, null);   // 账号、密码的布局文件，自定义
		builder.setIcon(R.drawable.icon_logo);//设置对话框icon
		builder.setTitle("用户卡确认");
		m_dialog = builder.create();
		m_dialog.setView(view);
		m_dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				//处理监听事件
				m_orderServiceQualityShowFlag = false;
			}
		});
		m_dialog.show();
		Window dialogWindow = m_dialog.getWindow();//获取window对象
		dialogWindow.setGravity(Gravity.CENTER);//设置对话框位置
		dialogWindow.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);//设置横向全屏
		m_orderServiceQualityShowFlag = true;

	}


	private void showToast(String info){
		if(toast ==null){
			toast = Toast.makeText(BottleRecycleActivity.this, null, Toast.LENGTH_SHORT);
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

	//查询用户的用户卡号对应的用户名称
	private void GetUserCardInfo(String userCardNumber) {
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.USERCARDURL;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();


		params.put("number",userCardNumber);
		//params.put("status", 1);//1 使用中
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag) {
					HttpResponse response = (HttpResponse) res;
					if (response != null) {
						if (response.getStatusLine().getStatusCode() == 200) {
							try {
								JSONObject userCardsJson = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
								JSONArray userCardsListJson = userCardsJson.getJSONArray("items");
								if(userCardsListJson.length()==1){
									JSONObject userJson =  userCardsListJson.getJSONObject(0).getJSONObject("user");
									m_curUserId = userJson.getString("userId");
									m_curUserInfo = userJson.getString("name");
									m_textViewUserInfo.setText(m_curUserId+"  |  "+m_curUserInfo);
									m_dialog.dismiss();
								}else{
									m_curUserId = null;
									m_curUserInfo = null;
									showToast("未绑定用户卡");
									m_textViewUserInfo.setText("");
								}

							} catch (JSONException e) {
								Toast.makeText(BottleRecycleActivity.this, "未知错误，异常！" + e.getMessage(),
										Toast.LENGTH_LONG).show();
							} catch (IOException e) {
								Toast.makeText(BottleRecycleActivity.this, "未知错误，异常！" + e.getMessage(),
										Toast.LENGTH_LONG).show();
							}
						} else {
							Toast.makeText(BottleRecycleActivity.this, "用户卡查询失败",
									Toast.LENGTH_LONG).show();
						}
					} else {
						Toast.makeText(BottleRecycleActivity.this, "网络未连接！",
								Toast.LENGTH_LONG).show();
					}
				}}}, nrc);
	}


}
