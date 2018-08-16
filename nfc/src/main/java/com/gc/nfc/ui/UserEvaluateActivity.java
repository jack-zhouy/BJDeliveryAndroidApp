package com.gc.nfc.ui;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
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
import android.view.LayoutInflater;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class UserEvaluateActivity extends BaseActivity implements OnClickListener  {
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
	private Button m_buttonNext;//下一步
	private AppContext appContext;
	private JSONObject m_OrderJson;//订单详情

	Bundle m_bundle;//上个activity传过来的参数

	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}
	@Override
	void init() {
		try {
			setContentView(R.layout.activity_bottle_exchange);
			//获取传过来的任务订单参数
			Bundle bundle = new Bundle();
			bundle = this.getIntent().getExtras();
			m_bundle = bundle;
			String  strOrder = bundle.getString("order");
			m_OrderJson = new JSONObject(strOrder);

			//控件初始化
			m_buttonNext = (Button) findViewById(R.id.button_next);//下一步按钮
			m_buttonNext.setOnClickListener(this);
			//数据初始化
			setOrderHeadInfo();
			//蓝牙设备初始化
			blueDeviceInitial();


		}catch (JSONException e){
			Toast.makeText(UserEvaluateActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

	//设置订单概要信息
	private void setOrderHeadInfo() {
		try {
			//获取订单用户
			JSONObject customerJson = m_OrderJson.getJSONObject("customer");
		}catch (JSONException e){
			Toast.makeText(UserEvaluateActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}




	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button_next:
				Toast.makeText(UserEvaluateActivity.this, "正在提交，请稍等。。。",
						Toast.LENGTH_LONG).show();
				m_buttonNext.setText("正在提交...");
				m_buttonNext.setBackgroundColor(getResources().getColor(R.color.transparent_background));
				m_buttonNext.setEnabled(false);
				handler_old.sendEmptyMessageDelayed(0,3000);
				break;
			default:
				break;
		}

	}


	private Handler handler_old = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//如果是托盘订单，需要检查空重瓶数量
			m_buttonNext.setText("下一步");
			m_buttonNext.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
			m_buttonNext.setEnabled(true);
			super.handleMessage(msg);
			//

		}
	};


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
			Toast.makeText(UserEvaluateActivity.this, "未知错误，异常！" + e.getMessage(),
					Toast.LENGTH_LONG).show();
			return null;
		} catch (JSONException e) {
			Toast.makeText(UserEvaluateActivity.this, "未知错误，异常！" + e.getMessage(),
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
					String userEvaluateCode = msg.obj.toString();
					//用户卡数据处理
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


	//上传评价结果
	private void upLoadGasCylinder() {

	}



}
