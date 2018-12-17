package com.gc.nfc.ui;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.dk.bleNfc.BleManager.BleManager;
import com.dk.bleNfc.BleManager.ScannerCallback;
import com.dk.bleNfc.BleNfcDeviceService;
import com.dk.bleNfc.DeviceManager.BleNfcDevice;
import com.dk.bleNfc.DeviceManager.ComByteManager;
import com.dk.bleNfc.DeviceManager.DeviceManager;
import com.dk.bleNfc.DeviceManager.DeviceManagerCallback;
import com.dk.bleNfc.Exception.CardNoResponseException;
import com.dk.bleNfc.Exception.DeviceNoResponseException;
import com.dk.bleNfc.Tool.StringTool;
import com.dk.bleNfc.card.CpuCard;
import com.dk.bleNfc.card.FeliCa;
import com.dk.bleNfc.card.Iso14443bCard;
import com.dk.bleNfc.card.Iso15693Card;
import com.dk.bleNfc.card.Mifare;
import com.dk.bleNfc.card.Ntag21x;
import com.dk.bleNfc.BleManager.*;
import android.bluetooth.BluetoothDevice;
import com.gc.nfc.utils.*;
import android.view.LayoutInflater;
import java.util.Set;
import java.util.Iterator;
import android.content.DialogInterface.OnDismissListener;
import android.text.TextWatcher;
import android.text.Editable;


public class BottleExchangeActivity extends BaseActivity implements OnClickListener  {
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
	private int m_takeOverCount = 0;//空瓶交接状态

	private TextView m_textViewTotalCountKP;// 空瓶数量
	private TextView m_textViewTotalCountZP;// 重瓶数量

	private NfcAdapter mNfcAdapter;
	private PendingIntent mPendingIntent;

	private Button m_buttonNext;//下一步
	private ListView m_listView_kp;// 空瓶号列表
	private ListView m_listView_zp;// 重瓶号列表
	private  RadioGroup  radioGroup_nfc=null;
	private  RadioButton  radioButton_kp,radioButton_zp;//nfc空瓶/重瓶录入

	private ImageView m_imageViewKPEye; //有效空瓶查看
	private ImageView m_imageViewZPEye; //有效重瓶查看
	private SwipeRefreshLayout swipeRefreshLayout;
	private AppContext appContext;
	private JSONObject m_OrderJson;//订单详情
	private String m_orderId;//订单号

	private String m_customerAddress;//用户地址


	private String m_curUserId;//该订单用户
	private JSONObject m_curUserSettlementType;//结算类型
	private User m_deliveryUser;//配送工
	private Map<String,JSONObject> m_userBottlesMap;//当前订单用户的钢瓶
	private Map<String,JSONObject> m_myBottlesMap;//当前配送工的钢瓶

	private Map<String, String> m_BottlesMapKP;//重瓶表
	private Map<String, String> m_BottlesMapZP;//空瓶表

	private int m_selected_nfc_model;//0--空瓶 1--重瓶

	Bundle m_bundle;//上个activity传过来的参数

	private EditText m_bottleIdKPEditText, m_bottleIdZPEditText;//手动输入空重瓶号
	private ImageView m_imageAddKPManual; //手动输入空瓶号
	private ImageView m_imageAddZPManual; //手动输入重瓶号

	private boolean isSpecialOrder;//是否是托盘订单


	private boolean m_orderServiceQualityShowFlag;//是否是用户评价阶段

	private String m_handedUserCard = null;//当前用户的用户卡


	private Toast toast = null;
	TextView tv;//toast--view
	/**
	 * 暂停Activity，界面获取焦点，按钮可以点击
	 */
	private int m_ptp_quantity_5kg;//瓶换瓶数量
	private int m_ptp_quantity_15kg;//瓶换瓶数量
	private int m_ptp_quantity_50kg;//瓶换瓶数量
	private int m_yjp_quantity_5kg;//押金瓶数量
	private int m_yjp_quantity_15kg;//押金瓶数量
	private int m_yjp_quantity_50kg;//押金瓶数量
	private String m_yjp_ys_total;//押金瓶应收金额total
	private String m_yjp_ss_total;//押金瓶实收金额total


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
			m_listView_kp = (ListView) findViewById(R.id.listview_kp);
			m_listView_zp = (ListView) findViewById(R.id.listview_zp);
			radioGroup_nfc=(RadioGroup)findViewById(R.id.radioGroup_nfc_id);
			radioButton_kp=(RadioButton)findViewById(R.id.radioButton_kp_id);
			radioButton_zp=(RadioButton)findViewById(R.id.radioButton_zp_id);
			m_imageViewKPEye = (ImageView) findViewById(R.id.imageView_KPEYE);
			m_imageViewZPEye = (ImageView) findViewById(R.id.imageView_ZPEYE);
			m_textViewTotalCountKP = (TextView) findViewById(R.id.items_totalCountKP);
			m_textViewTotalCountZP = (TextView) findViewById(R.id.items_totalCountZP);

			m_bottleIdKPEditText = (EditText) findViewById(R.id.input_bottleIdKP);
			m_bottleIdZPEditText = (EditText) findViewById(R.id.input_bottleIdZP);
			m_imageAddKPManual = (ImageView) findViewById(R.id.imageView_addKPManual);
			m_imageAddZPManual = (ImageView) findViewById(R.id.imageView_addZPManual);


			m_imageViewZPEye.setOnClickListener(this);
			m_imageViewKPEye.setOnClickListener(this);
			m_imageAddKPManual.setOnClickListener(this);
			m_imageAddZPManual.setOnClickListener(this);

			m_buttonNext.setOnClickListener(this);
			radioGroup_nfc.setOnCheckedChangeListener(listen);
			radioGroup_nfc.check(radioButton_kp.getId());//默认是空瓶
			//数据结构初始化
			m_userBottlesMap = new HashMap<String, JSONObject>();
			m_myBottlesMap = new HashMap<String, JSONObject>();
			m_BottlesMapKP = new HashMap<String,String>();
			m_BottlesMapZP = new HashMap<String,String>();
			//获取当前配送工
			appContext = (AppContext) getApplicationContext();
			m_deliveryUser = appContext.getUser();
			if (m_deliveryUser == null) {
				Toast.makeText(BottleExchangeActivity.this, "登陆会话失效", Toast.LENGTH_LONG).show();
				Intent intent = new Intent(BottleExchangeActivity.this, LoginActivity.class);
				startActivity(intent);
				finish();
			}

			//初始化两个LISTVIEW的点击事件，目前没有实现交接的回撤
			m_listView_kp.setOnItemLongClickListener(new OnItemLongClickListener() {

				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					//录入钢瓶重量
					TextView bottleCodeTextView = (TextView)view.findViewById(R.id.items_number);
					String bottleCode = bottleCodeTextView.getText().toString();
					getBottleWeight(bottleCode);
					//deleteKP(position);
					return true;
				}
			});
			//重瓶现在不需要称重
//			m_listView_zp.setOnItemLongClickListener(new OnItemLongClickListener() {
//
//				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//					//录入钢瓶重量
//					TextView bottleCodeTextView = (TextView)view.findViewById(R.id.items_number);
//					String bottleCode = bottleCodeTextView.getText().toString();
//					getBottleWeight(bottleCode, true);
//					//deleteZP(position);
//					return true;
//				}
//			});




			//数据初始化
			setOrderHeadInfo();
			getUserBottles();//获取用户名下的钢瓶号
			getMyBottles();//获取配送工名下的钢瓶号
			//蓝牙设备初始化
			blueDeviceInitial();
			//默认刚开始是录钢瓶阶段
			m_orderServiceQualityShowFlag = false;
			//用户已有用户卡查询
			GetUserCard();

			//电子押金单初始化
			m_ptp_quantity_5kg = 0;//瓶换瓶数量
			m_ptp_quantity_15kg = 0;//瓶换瓶数量
			m_ptp_quantity_50kg = 0;//瓶换瓶数量
			m_yjp_quantity_5kg = 0;//押金瓶数量
			m_yjp_quantity_15kg = 0;//押金瓶数量
			m_yjp_quantity_50kg = 0;//押金瓶数量
			m_yjp_ys_total= "";//押金瓶应收金额total
			m_yjp_ss_total= "";//押金瓶实收金额total


		}catch (JSONException e){
			Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
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

	//设置订单概要信息
	private void setOrderHeadInfo() {
		try {
			//获取订单用户
			JSONObject customerJson = m_OrderJson.getJSONObject("customer");
			m_curUserId = customerJson.get("userId").toString();

			m_orderId = m_OrderJson.get("orderSn").toString();

			JSONObject addressJson = m_OrderJson.getJSONObject("recvAddr");
			m_customerAddress = addressJson.get("city").toString()+addressJson.get("county").toString()+addressJson.get("detail").toString();

//判断是不是托盘订单
			JSONObject orderTriggerTypeJson = m_OrderJson.getJSONObject("orderTriggerType");
			isSpecialOrder = false;
			if(orderTriggerTypeJson!=null&&(orderTriggerTypeJson.get("index").toString().equals("1"))){
				isSpecialOrder = true;
			}



		}catch (JSONException e){
			Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}




	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button_next:
				if(!isBottlesQuantityOK()){//空重瓶交接不符合
					return;
				}
				if(isSpecialOrder){//不间断供气订单，所有空瓶必须称重
					for (Map.Entry<String, String> entry : m_BottlesMapKP.entrySet()) {
						String weight_temp = entry.getValue();
						if(weight_temp.length()==0||Double.parseDouble(weight_temp)<4){
							Toast.makeText(BottleExchangeActivity.this, "所有空瓶必须称重，重量错误!",
 						Toast.LENGTH_LONG).show();
							return;
						}
					}
				}
//				Toast.makeText(BottleExchangeActivity.this, "正在提交，请稍等。。。",
//						Toast.LENGTH_LONG).show();
//				m_buttonNext.setText("正在提交...");
//				m_buttonNext.setBackgroundColor(getResources().getColor(R.color.transparent_background));
//				m_buttonNext.setEnabled(false);
				//订单上传瓶号
				upLoadGasCylinder();
				//订单绑定重瓶号
				OrdersBindGasCynNumber();

				//评价成功，跳转支付,,测试需要，等会删除
				//handler_old.sendEmptyMessageDelayed(0,1000);
				//用户评价阶段
				orderServiceQualityShow();


				break;
			case R.id.imageView_KPEYE:// 用户的气瓶
				Intent intentKP = new Intent();
				Bundle bundleKP = new Bundle();
				bundleKP.putString("userId", m_curUserId);
				intentKP.setClass(BottleExchangeActivity.this, MybottlesActivity.class);
				intentKP.putExtras(bundleKP);
				startActivity(intentKP);
				break;
			case R.id.imageView_ZPEYE://配送工的气瓶
				Intent intentZP = new Intent();
				Bundle bundleZP = new Bundle();
				bundleZP.putString("userId", m_deliveryUser.getUsername());
				intentZP.setClass(BottleExchangeActivity.this, MybottlesActivity.class);
				intentZP.putExtras(bundleZP);
				startActivity(intentZP);
				break;

			case R.id.imageView_addKPManual://手动添加空瓶号
				String bottleCodeKP = m_bottleIdKPEditText.getText().toString();
				bottleTakeOverUnit(bottleCodeKP, m_curUserId, m_deliveryUser.getUsername(), "6", m_customerAddress+"|空瓶回收", false, true);//空瓶回收
				break;
			case R.id.imageView_addZPManual://手动添加重瓶号
				String bottleCodeZP = m_bottleIdZPEditText.getText().toString();
				bottleTakeOverUnit(bottleCodeZP,  m_deliveryUser.getUsername(), m_curUserId,"5", m_customerAddress+"|重瓶落户",false, false);//客户使用
				break;
			default:
				break;
		}

	}

	//获取该用户名下的瓶子
	public void getUserBottles() {

		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.GASCYLINDERURL;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("liableUserId",m_curUserId );//用户号
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==200){
							try {
								m_userBottlesMap.clear();
								JSONObject bottlesJson = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
								JSONArray bottlesListJson = bottlesJson.getJSONArray("items");

								for(int i=0;i<bottlesListJson.length(); i++){
									JSONObject bottleJson = bottlesListJson.getJSONObject(i);  // 遍历 jsonarray 数组，把每一个对象转成 json 对象
									String bottleCode = bottleJson.get("number").toString();//钢瓶编号
									m_userBottlesMap.put(bottleCode, bottleJson);
								}

							}catch (IOException e){
								Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
							}catch (JSONException e) {
								Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
							}
						}
					}else {
						Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(BottleExchangeActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
	}

	//获取配送工名下的瓶子
	public void getMyBottles() {


		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.GASCYLINDERURL;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("liableUserId",m_deliveryUser.getUsername());//责任人是当前用户
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==200){
							try {
								m_myBottlesMap.clear();
								JSONObject bottlesJson = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
								JSONArray bottlesListJson = bottlesJson.getJSONArray("items");

								for(int i=0;i<bottlesListJson.length(); i++){
									JSONObject bottleJson = bottlesListJson.getJSONObject(i);  // 遍历 jsonarray 数组，把每一个对象转成 json 对象
									String bottleCode = bottleJson.get("number").toString();//钢瓶编号
									m_myBottlesMap.put(bottleCode, bottleJson);
								}

							}catch (IOException e){
								Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
							}catch (JSONException e) {
								Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
							}
						}
					}else {
						Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(BottleExchangeActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
	}

	//NFC更新空瓶表
	private void refleshBottlesListKP(){
		m_textViewTotalCountKP.setText(Integer.toString(m_BottlesMapKP.size()));
		List<Map<String,Object>> list_map = new ArrayList<Map<String,Object>>(); //定义一个适配器对象
		for (Map.Entry<String, String> entry : m_BottlesMapKP.entrySet()) {
			Map<String,Object> bottleInfo = new HashMap<String, Object>(); //创建一个键值对的Map集合，用来存放名字和头像

			bottleInfo.put("bottleCode", entry.getKey());
			bottleInfo.put("bottleWeight", entry.getValue()+"公斤");
			list_map.add(bottleInfo);   //把这个存放好数据的Map集合放入到list中，这就完成类数据源的准备工作
		}

		//2、创建适配器（可以使用外部类的方式、内部类方式等均可）
		SimpleAdapter simpleAdapter = new SimpleAdapter(
				BottleExchangeActivity.this,/*传入一个上下文作为参数*/
				list_map,         /*传入相对应的数据源，这个数据源不仅仅是数据而且还是和界面相耦合的混合体。*/
				R.layout.bottle_list_simple_items, /*设置具体某个items的布局，需要是新的布局，而不是ListView控件的布局*/
				new String[]{"bottleCode","bottleWeight"}, /*传入上面定义的键值对的键名称,会自动根据传入的键找到对应的值*/
				new int[]{R.id.items_number,R.id.items_weight}) ;

		m_listView_kp.setAdapter(simpleAdapter);
		setListViewHeightBasedOnChildren(m_listView_kp);
	}

	//NFC更新重瓶表
	private void refleshBottlesListZP(){
		m_textViewTotalCountZP.setText(Integer.toString(m_BottlesMapZP.size()));
		List<Map<String,Object>> list_map = new ArrayList<Map<String,Object>>(); //定义一个适配器对象

		for (Map.Entry<String, String> entry : m_BottlesMapZP.entrySet()) {
			Map<String,Object> bottleInfo = new HashMap<String, Object>(); //创建一个键值对的Map集合，用来存放名字和头像

			bottleInfo.put("bottleCode", entry.getKey());
			bottleInfo.put("bottleWeight", entry.getValue());
			list_map.add(bottleInfo);   //把这个存放好数据的Map集合放入到list中，这就完成类数据源的准备工作
		}

		//2、创建适配器（可以使用外部类的方式、内部类方式等均可）
		SimpleAdapter simpleAdapter = new SimpleAdapter(
				BottleExchangeActivity.this,/*传入一个上下文作为参数*/
				list_map,         /*传入相对应的数据源，这个数据源不仅仅是数据而且还是和界面相耦合的混合体。*/
				R.layout.bottle_list_simple_items, /*设置具体某个items的布局，需要是新的布局，而不是ListView控件的布局*/
				new String[]{"bottleCode","bottleWeight"}, /*传入上面定义的键值对的键名称,会自动根据传入的键找到对应的值*/
				new int[]{R.id.items_number,R.id.items_weight}) ;
		m_listView_zp.setAdapter(simpleAdapter);
		setListViewHeightBasedOnChildren(m_listView_zp);
	}


	//单个钢瓶交接
	public void bottleTakeOverUnit(final String bottleCode, final String srcUserId, final String targetUserId, final String serviceStatus, final String note, final boolean enableForce, final boolean isKP) {
		//如果存在交接记录表里，就提示已经存在了
		boolean contained = false;
		if (isKP){
			if(m_BottlesMapKP.containsKey(bottleCode)){
				contained = true;
			}
		}else{
			if(m_BottlesMapZP.containsKey(bottleCode)){
				contained = true;
			}
		}

		if(contained){//已经存在了
			Toast.makeText(BottleExchangeActivity.this, "钢瓶号："+bottleCode+"    请勿重复提交！",
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
							MediaPlayer music = MediaPlayer.create(BottleExchangeActivity.this, R.raw.nfcok);
							music.start();
							if(isKP){
								addKP(bottleCode);
							}else {
								addZP(bottleCode);
							}
						}else{
							MediaPlayer music = MediaPlayer.create(BottleExchangeActivity.this, R.raw.alarm);
							music.start();
							//409才允许强制交接
							if(response.getStatusLine().getStatusCode()==409){
								new AlertDialog.Builder(BottleExchangeActivity.this).setTitle("钢瓶异常流转！")
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
														bottleTakeOverUnit(bottleCode, srcUserId, targetUserId, serviceStatus, note, true, isKP);
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
								new AlertDialog.Builder(BottleExchangeActivity.this).setTitle("钢瓶异常流转！")
										.setMessage("钢瓶号 :"+bottleCode+"\r\n"+"错误原因:"+getResponseMessage(response))
										.setIcon(R.drawable.icon_logo)
										.setPositiveButton("确定",
												new DialogInterface.OnClickListener()
												{
													@Override
													public void onClick(DialogInterface dialog,
																		int which)
													{

													}
												}).show();
							}
						}
					}else {
						Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(BottleExchangeActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
	}


	private Handler handler_old = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//如果是托盘订单，需要检查空重瓶数量
//			m_buttonNext.setText("下一步");
//			m_buttonNext.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
//			m_buttonNext.setEnabled(true);
			super.handleMessage(msg);
			//不间断供气的订单
			if(isSpecialOrder){
				//将空瓶号传到下一个页面，用于不间断供气计费
				Set keySet = m_BottlesMapKP.keySet();
				Iterator iter = keySet.iterator();
				while(iter.hasNext())
				{
					String key = (String)iter.next();
					m_bundle.putString(key, m_BottlesMapKP.get(key));
				}
				//提交电子押金单
				createElectDep();


			}else{
				//提交电子押金单
				createElectDep();
			}
		}
	};



	private void addKP(final String bottleCode){

		if(!m_BottlesMapKP.containsKey(bottleCode)){//第一次扫
			m_BottlesMapKP.put(bottleCode, "0");
			refleshBottlesListKP();
		}
	}
	private void addZP(String bottleCode) {

		if(!m_BottlesMapZP.containsKey(bottleCode)){//第一次扫
			m_BottlesMapZP.put(bottleCode, "");
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
			Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！" + e.getMessage(),
					Toast.LENGTH_LONG).show();
			return null;
		} catch (JSONException e) {
			Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！" + e.getMessage(),
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
					String bottleCode = msg.obj.toString();
					if(m_selected_nfc_model==0){//空瓶录入模式
						bottleTakeOverUnit(bottleCode, m_curUserId, m_deliveryUser.getUsername(), "6", m_customerAddress+"|空瓶回收", false, true);//空瓶回收
					}else if(m_selected_nfc_model==1){//重瓶录入模式
						bottleTakeOverUnit(bottleCode,  m_deliveryUser.getUsername(), m_curUserId,"5", m_customerAddress+"|重瓶落户",false, false);//客户使用
					}
					break;
				case 0x89://扫描用户卡
					String readText = msg.obj.toString();
					String textArray[] = readText.split(":");
					if(textArray.length!=2){
						showToast("无效卡格式！");
						return;
					}else {
						String evaluate = textArray[0];
						String userCardIndex = textArray[1];

						if(m_handedUserCard==null){
							showToast("该用户未绑定用户卡");
						}
						if(!userCardIndex.equals(m_handedUserCard)){
							showToast("非本人卡号！");
							return;
						}
						if(evaluate.equals("Y")){
							showToast("满意！");
							orderServiceQualityUpload(true);
						}else if(evaluate.equals("N")){
							showToast("不满意！");
							orderServiceQualityUpload(false);
						} else {
							showToast("无效卡格式！");
							return;
						}
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
	private class StartSearchButtonListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if ( (bleNfcDevice.isConnection() == BleManager.STATE_CONNECTED) ) {
				bleNfcDevice.requestDisConnectDevice();
				return;
			}
			searchNearestBleDevice();
		}
	}

	//上传回收瓶号
	private boolean upLoadGasCylinder() {

		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.PUT);

		nrc.requestUrl = NetUrlConstant.ORDERURL+"/"+m_orderId;
		nrc.context = this;
		Map<String, Object> body = new HashMap<String, Object>();

		body.put("recycleGasCylinder",m_BottlesMapKP.toString());//空瓶号上传
		body.put("deliveryGasCylinder",m_BottlesMapZP.toString());//重瓶号上传　
		//如果没有交接就不上传
		if(m_BottlesMapKP.size()==0&&m_BottlesMapZP.size()==0){
			return false;
		}

		nrc.setBody(body);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==200){

						}else if(response.getStatusLine().getStatusCode()==404){
							Toast.makeText(BottleExchangeActivity.this, "订单不存在",
									Toast.LENGTH_LONG).show();
						} else{
							Toast.makeText(BottleExchangeActivity.this, "瓶号上传失败"+response.getStatusLine().getStatusCode(),
									Toast.LENGTH_LONG).show();
						}
					}else {
						Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(BottleExchangeActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
		return true;
	}


	private void getBottleWeight(String bottleCode){
		final String bottleCodeTemp = bottleCode;
		LayoutInflater inflater = getLayoutInflater();
		final View layout = inflater.inflate(R.layout.upload_weight,
				null);
		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("请输入").setIcon(
				R.drawable.icon_app).setView(
				layout).setPositiveButton("确定",
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog,
										int which)
					{
						EditText et = (EditText)layout.findViewById(R.id.input_bottleWeight);
						String weight = et.getText().toString();
						if(weight.length()==0){
							Toast.makeText(BottleExchangeActivity.this, "重量输入有误，请重新输入！",
									Toast.LENGTH_LONG).show();
						}else{
							m_BottlesMapKP.put(bottleCodeTemp, weight);
							refleshBottlesListKP();
						}


					}
				});
		builder.setCancelable(false);
		builder.show();
	}


	//用户卡评价弹窗
	private void orderServiceQualityShow(){
		//用户已有用户卡查询
		GetUserCard();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		//builder.setCancelable(false);
		View view = View.inflate(this, R.layout.user_evaluate, null);   // 账号、密码的布局文件，自定义
		builder.setIcon(R.drawable.icon_logo);//设置对话框icon
		builder.setTitle("用户卡评价("+m_handedUserCard+")");
		AlertDialog dialog = builder.create();
		dialog.setView(view);
		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				//处理监听事件
				m_orderServiceQualityShowFlag = false;
			}
		});
		dialog.show();
		Window dialogWindow = dialog.getWindow();//获取window对象
		dialogWindow.setGravity(Gravity.CENTER);//设置对话框位置
		dialogWindow.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);//设置横向全屏
		m_orderServiceQualityShowFlag = true;

	}


	//用户卡评价上传
	private void orderServiceQualityUpload(boolean orderServiceValue){
			// get请求
			NetRequestConstant nrc = new NetRequestConstant();
			nrc.setType(HttpRequestType.PUT);

			nrc.requestUrl = NetUrlConstant.ORDERURL+"/"+m_orderId;
			nrc.context = this;
			Map<String, Object> body = new HashMap<String, Object>();

			if(orderServiceValue){//满意
				body.put("orderServiceQuality","OSQNegative");
			}else{
				body.put("orderServiceQuality","OSQPositive");
			}
			nrc.setBody(body);
			getServer(new Netcallback() {
				public void preccess(Object res, boolean flag) {
					if(flag){
						HttpResponse response=(HttpResponse)res;
						if(response!=null){
							if(response.getStatusLine().getStatusCode()==200){
								//评价成功，跳转支付
								handler_old.sendEmptyMessageDelayed(0,3000);


							}else if(response.getStatusLine().getStatusCode()==404){
								Toast.makeText(BottleExchangeActivity.this, "订单不存在",
										Toast.LENGTH_LONG).show();
							} else if(response.getStatusLine().getStatusCode()==401){
								Toast.makeText(BottleExchangeActivity.this, "鉴权失败，请重新登录"+response.getStatusLine().getStatusCode(),
										Toast.LENGTH_LONG).show();
							}else{
								Toast.makeText(BottleExchangeActivity.this, "支付失败" + response.getStatusLine().getStatusCode(),
										Toast.LENGTH_LONG).show();
							}
						}else {
							Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！",
									Toast.LENGTH_LONG).show();
						}
					} else {
						Toast.makeText(BottleExchangeActivity.this, "网络未连接！",
								Toast.LENGTH_LONG).show();
					}
				}
			}, nrc);
	}

	private void showToast(String info){
		if(toast ==null){
			toast = Toast.makeText(BottleExchangeActivity.this, null, Toast.LENGTH_SHORT);
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

	//查询用户的用户卡号
	private void GetUserCard() {
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.USERCARDURL;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();


		params.put("userId",m_curUserId);
		params.put("status", 1);//1 使用中
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
									m_handedUserCard = userCardsListJson.getJSONObject(0).getString("number");
								}else{
									m_handedUserCard = null;
									showToast("该用户未绑定用户卡");
								}
							} catch (JSONException e) {
								Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！" + e.getMessage(),
										Toast.LENGTH_LONG).show();
							} catch (IOException e) {
								Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！" + e.getMessage(),
										Toast.LENGTH_LONG).show();
							}
						} else {
							Toast.makeText(BottleExchangeActivity.this, "用户卡查询失败",
									Toast.LENGTH_LONG).show();
						}
					} else {
						Toast.makeText(BottleExchangeActivity.this, "网络未连接！",
								Toast.LENGTH_LONG).show();
					}
				}}}, nrc);
	}

	//应收金额自动计算
private void addEditViewChanged(EditText eEditText, final int unitPrice, final TextView eTextView, final View layout){
	eEditText.addTextChangedListener(new TextWatcher(){

		public void afterTextChanged(Editable s) {
			int quantity = 0;
			if(s.toString().equals("")){
			}else{
				quantity = Integer.parseInt(s.toString());
			}
			int ysPrice = quantity*unitPrice;
			eTextView.setText(String.valueOf(ysPrice));
			//计算应收总价
			TextView textView_5kg_ys_price = (TextView) layout.findViewById(R.id.textView_yjp_5kg_ys);
			TextView textView_15kg_ys_price = (TextView) layout.findViewById(R.id.textView_yjp_15kg_ys);
			TextView textView_50kg_ys_price = (TextView) layout.findViewById(R.id.textView_yjp_50kg_ys);
			int totalYsPrice = Integer.parseInt(getTextViewToString(textView_5kg_ys_price))+Integer.parseInt(getTextViewToString(textView_15kg_ys_price))+
					Integer.parseInt(getTextViewToString(textView_50kg_ys_price));
			TextView textView_ys_temp = (TextView) layout.findViewById(R.id.textView_ys);
			textView_ys_temp.setText(String.valueOf(totalYsPrice));


		}
		public void beforeTextChanged(CharSequence s, int start, int count,
									  int after) {}
		public void onTextChanged(CharSequence s, int start, int before,
								  int count) {}
	});
}

	private void show_deposit_slip() {

		LayoutInflater inflater = getLayoutInflater();
		final View layout = inflater.inflate(R.layout.show_deposit_slip, null);
		EditText textView_5kg_quantity = (EditText) layout.findViewById(R.id.textView_yjp_5kg_quantity);
		EditText textView_15kg_quantity = (EditText) layout.findViewById(R.id.textView_yjp_15kg_quantity);
		EditText textView_50kg_quantity = (EditText) layout.findViewById(R.id.textView_yjp_50kg_quantity);

		TextView textView_5kg_ys_price = (TextView) layout.findViewById(R.id.textView_yjp_5kg_ys);
		TextView textView_15kg_ys_price = (TextView) layout.findViewById(R.id.textView_yjp_15kg_ys);
		TextView textView_50kg_ys_price = (TextView) layout.findViewById(R.id.textView_yjp_50kg_ys);
		addEditViewChanged(textView_5kg_quantity,85, textView_5kg_ys_price, layout);
		addEditViewChanged(textView_15kg_quantity,160, textView_15kg_ys_price, layout);
		addEditViewChanged(textView_50kg_quantity,500, textView_50kg_ys_price, layout);

		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("电子押金单").setIcon(
				R.drawable.icon_app).setView(
				layout).setPositiveButton("确定",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,
										int which) {
						m_ptp_quantity_5kg = getEditTextToInt((EditText) layout.findViewById(R.id.textView_ptp_5kg_quantity));//瓶换瓶数量
						m_ptp_quantity_15kg = getEditTextToInt((EditText) layout.findViewById(R.id.textView_ptp_15kg_quantity));//瓶换瓶数量
						m_ptp_quantity_50kg = getEditTextToInt((EditText) layout.findViewById(R.id.textView_ptp_50kg_quantity));//瓶换瓶数量
						m_yjp_quantity_5kg = getEditTextToInt((EditText) layout.findViewById(R.id.textView_yjp_5kg_quantity));//押金瓶数量
						m_yjp_quantity_15kg = getEditTextToInt((EditText) layout.findViewById(R.id.textView_yjp_15kg_quantity));//押金瓶数量
						m_yjp_quantity_50kg = getEditTextToInt((EditText) layout.findViewById(R.id.textView_yjp_50kg_quantity));//押金瓶数量
						m_yjp_ys_total = getTextViewToString((TextView) layout.findViewById(R.id.textView_ys));//押金瓶应收金额total
						m_yjp_ss_total = getTextViewToString((EditText) layout.findViewById(R.id.editView_ss));//押金瓶实收金额total
					}
				});
		builder.setCancelable(false);
		builder.show();

	}
	private int getEditTextToInt(EditText eEditText){
		if(eEditText.getText().toString().equals("")){
			return 0;
		}else{
			return Integer.parseInt(eEditText.getText().toString());
		}
	}
	private String getTextViewToString(TextView textView){
		if(textView.getText().toString().equals("")){
			return "0";
		}else{
			return textView.getText().toString();
		}
	}

	//校验已经交接以及电子押金单的钢瓶数量型号与订单内容是否一致
	private boolean isBottlesQuantityOK() {
		try {
			//获取订单的商品详情
			JSONArray orderDetailList = m_OrderJson.getJSONArray("orderDetailList");
			Map<String, Integer> goodsMapQuantity = new HashMap<String, Integer>(); //统计每个规格的数量
			for (int i = 0; i < orderDetailList.length(); i++) {
				//找出商品规格
				JSONObject orderDetail = orderDetailList.getJSONObject(i);  // 订单详情单条记录
				JSONObject goodDetail = orderDetail.getJSONObject("goods");  // 商品详情
				String goodCode = goodDetail.get("code").toString();
				int tempCount = Integer.parseInt(orderDetail.get("quantity").toString());
				if(goodsMapQuantity.containsKey(goodCode)){
					int totalCount = goodsMapQuantity.get(goodCode);
					totalCount += tempCount;
					goodsMapQuantity.remove(goodCode);
					goodsMapQuantity.put(goodCode,totalCount);
				}else{
					goodsMapQuantity.put(goodCode,tempCount);
				}
			}


			//订单中的钢瓶数量
			int iQuantityOrder_total = 0;
			for (String key : goodsMapQuantity.keySet()) {
				Integer quantity_temp = goodsMapQuantity.get(key);
				iQuantityOrder_total += quantity_temp;
			}
			if(iQuantityOrder_total!=m_BottlesMapZP.size()){
				Toast.makeText(BottleExchangeActivity.this, "重瓶交接数量与订单不符合！",
						Toast.LENGTH_LONG).show();
				return false;
			}
			//判断空的交接是否匹配
			int iQuantityKpJJ_total = m_BottlesMapKP.size()+m_ptp_quantity_5kg+m_ptp_quantity_15kg+m_ptp_quantity_50kg+
					m_yjp_quantity_5kg+m_yjp_quantity_15kg+m_yjp_quantity_50kg;
			if(iQuantityKpJJ_total!=iQuantityOrder_total){
				Toast.makeText(BottleExchangeActivity.this, "空瓶回收与重瓶数量不符，请重新填写电子押金单！",
						Toast.LENGTH_LONG).show();
				show_deposit_slip();
				return false;
			}

		}catch (JSONException e){
			Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}


	//上传电子押金单
	private void createElectDep() {
		// POST
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.POST);

		nrc.requestUrl = NetUrlConstant.ElectDepositURL;
		nrc.context = this;
		Map<String, Object> body = new HashMap<String, Object>();
		body.put("customerId",m_curUserId);
		body.put("operId",m_deliveryUser.getUsername());
		body.put("amountReceivable",m_yjp_ys_total);
		body.put("actualAmount",m_yjp_ss_total);

		JSONArray detail = createElectDepDetails();
		if(detail==null){
			//无电子押金单，直接跳转
			jumpToOrderDeal();
			return;
		}
		body.put("electDepositDetails",detail);
		nrc.setBody(body);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==201){
							//电子押金单上传后跳转
							jumpToOrderDeal();

						} else{
							Toast.makeText(BottleExchangeActivity.this, "电子押金单上传失败，"+getResponseMessage(response)+response.getStatusLine().getStatusCode(),
									Toast.LENGTH_LONG).show();
						}
					}else {
						Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(BottleExchangeActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
		return ;
	}

	private JSONArray createElectDepDetails(){
		try {
			JSONArray electDepDetails = new JSONArray();
			JSONObject spec_5kg = new JSONObject();
			JSONObject spec_15kg = new JSONObject();
			JSONObject spec_50kg = new JSONObject();
			spec_5kg.put("code", "0001");
			spec_15kg.put("code", "0002");
			spec_50kg.put("code", "0003");

			if(m_ptp_quantity_5kg!=0){//5kg瓶换瓶数量
				JSONObject electDepDetail_temp = new JSONObject();
				electDepDetail_temp.put("electDepositType", "EBottleChanging");
				electDepDetail_temp.put("gasCylinderSpec", spec_5kg);
				electDepDetail_temp.put("quantity", m_ptp_quantity_5kg);
				electDepDetails.put(electDepDetail_temp);
			}
			if(m_ptp_quantity_15kg!=0){//15kg瓶换瓶数量
				JSONObject electDepDetail_temp = new JSONObject();
				electDepDetail_temp.put("electDepositType", "EBottleChanging");
				electDepDetail_temp.put("gasCylinderSpec", spec_15kg);
				electDepDetail_temp.put("quantity", m_ptp_quantity_15kg);
				electDepDetails.put(electDepDetail_temp);
			}
			if(m_ptp_quantity_50kg!=0){//50kg瓶换瓶数量
				JSONObject electDepDetail_temp = new JSONObject();
				electDepDetail_temp.put("electDepositType", "EBottleChanging");
				electDepDetail_temp.put("gasCylinderSpec", spec_50kg);
				electDepDetail_temp.put("quantity", m_ptp_quantity_50kg);
				electDepDetails.put(electDepDetail_temp);
			}

			if(m_yjp_quantity_5kg!=0){//5kg押金瓶数量
				JSONObject electDepDetail_temp = new JSONObject();
				electDepDetail_temp.put("electDepositType", "EDepositBottle");
				electDepDetail_temp.put("gasCylinderSpec", spec_5kg);
				electDepDetail_temp.put("quantity", m_yjp_quantity_5kg);
				electDepDetails.put(electDepDetail_temp);
			}
			if(m_yjp_quantity_15kg!=0){//15kg押数量
				JSONObject electDepDetail_temp = new JSONObject();
				electDepDetail_temp.put("electDepositType", "EDepositBottle");
				electDepDetail_temp.put("gasCylinderSpec", spec_15kg);
				electDepDetail_temp.put("quantity", m_yjp_quantity_15kg);
				electDepDetails.put(electDepDetail_temp);
			}
			if(m_yjp_quantity_50kg!=0){//50kg押金瓶数量
				JSONObject electDepDetail_temp = new JSONObject();
				electDepDetail_temp.put("electDepositType", "EDepositBottle");
				electDepDetail_temp.put("gasCylinderSpec", spec_50kg);
				electDepDetail_temp.put("quantity", m_yjp_quantity_50kg);
				electDepDetails.put(electDepDetail_temp);
			}
			if(electDepDetails.length()==0){
				return null;
			}else{
				return electDepDetails;
			}


		}catch (JSONException e){
			Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
			return null;
		}
	}

	//跳转至订单支付
	private void jumpToOrderDeal(){
		//跳转之前上传重瓶号
		//订单绑定重瓶号
		Intent intent = new Intent();
		if(isSpecialOrder){//不间断供气订单
			intent.setClass(BottleExchangeActivity.this, TrayOrderDealActivity.class);

		}else{//瓶结算订单O
			intent.setClass(BottleExchangeActivity.this, OrderDealActivity .class);
		}
		intent.putExtras(m_bundle);
		startActivity(intent);
	}

	//订单关联重瓶号
	private void OrdersBindGasCynNumber() {
		//如果没有交接就不上传
		if(m_BottlesMapZP.size()==0){
			return ;
		}
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.PUT);

		nrc.requestUrl = NetUrlConstant.OrderBindGascynnumberURL;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();

		params.put("orderSn",m_orderId);
		String strGasCynNumbers = "";
		boolean isFirstOne = true;
		for (Map.Entry<String, String> entry : m_BottlesMapZP.entrySet()) {
			if (isFirstOne) {
				isFirstOne = false;
			}else{
				strGasCynNumbers+=",";
			}
			strGasCynNumbers += entry.getKey();

		}
		params.put("gasCynNumbers",strGasCynNumbers);//重瓶号上传　


		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==200){
							Toast.makeText(BottleExchangeActivity.this, "重瓶绑定订单成功"+response.getStatusLine().getStatusCode(),
									Toast.LENGTH_LONG).show();
						}else{
							Toast.makeText(BottleExchangeActivity.this, "重瓶绑定订单失败"+response.getStatusLine().getStatusCode(),
									Toast.LENGTH_LONG).show();
						}
					}else {
						Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(BottleExchangeActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
		return ;
	}


}
