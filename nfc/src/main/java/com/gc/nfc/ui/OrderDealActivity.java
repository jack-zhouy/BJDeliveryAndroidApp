package com.gc.nfc.ui;

import android.app.PendingIntent;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Poi;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AmapNaviPage;
import com.amap.api.navi.AmapNaviParams;
import com.amap.api.navi.AmapNaviType;
import com.amap.api.navi.INaviInfoCallback;
import com.amap.api.navi.model.AMapNaviLocation;
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import android.widget.AdapterView.OnItemLongClickListener;
import android.app.AlertDialog;
import android.view.Window;

import android.view.WindowManager;

import android.content.DialogInterface;
import android.graphics.Color;
import java.io.InputStream;
import android.net.Uri;

import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import java.net.URL;
import java.util.Set;

import java.util.Random;


public class OrderDealActivity extends BaseActivity implements OnClickListener, INaviInfoCallback,AbsListView.OnScrollListener  {
	private final String m_testValidKP[] = {"05","06"};
	private final String m_testValidZP[] = {"08","11"};
	private int testValidKPIndex = 0;
	private int testValidZPIndex = 0;


	private NfcAdapter mNfcAdapter;
	private PendingIntent mPendingIntent;

	private TextView m_textViewPayStatus;//支付状态
	private Button m_buttonNext;//下一步
	private ListView m_listView_kp;// 空瓶号列表
	private ListView m_listView_zp;// 重瓶号列表
	private  RadioGroup  radioGroup_nfc=null;
	private  RadioButton  radioButton_kp,radioButton_zp;//nfc空瓶/重瓶录入
	private Spinner m_spinnerPaytype; //支付类型
	private TextView m_textViewPaytype; //支付类型
	private TextView m_textViewTotalFee; //商品总价

	private ImageView m_imageViewKPEye; //有效空瓶查看
	private ImageView m_imageViewZPEye; //有效重瓶查看



	private SwipeRefreshLayout swipeRefreshLayout;

	private AppContext appContext;


	private JSONObject m_OrderJson;//订单详情

	private String m_orderId;//订单号
	private String m_taskId;//任务订单详情
	private int m_orderStatus;//订单状态
	private User m_user;//当前登录用户
	private LatLng m_recvLocation;//收货地址经纬度
	private String m_businessKey;//订单号
	private String m_recvAddr;//收获地址
	private String m_totalFee;//商品总价

	private String m_curUserId;//该订单用户
	private JSONObject m_curUserSettlementType;//结算类型
	private User m_deliveryUser;//配送工
	private Map<String,JSONObject> m_userBottlesMap;//当前订单用户的钢瓶
	private Map<String,JSONObject> m_myBottlesMap;//当前配送工的钢瓶

	private List<String> m_BottlesListKP;//重瓶表
	private List<String> m_BottlesListZP;//空瓶表


	public static String m_orderPayStatus;//支付状态


	private int m_selected_nfc_model;//0--空瓶 1--重瓶

	private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what){
				case 0x101:
					if (swipeRefreshLayout.isRefreshing()){
						swipeRefreshLayout.setRefreshing(false);//设置不刷新
					}
					break;
			}
		}
	};
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		return;

	}



	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		return;

	}

	@Override
	protected void onStart() {
		super.onStart();
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if(mNfcAdapter!=null&& mNfcAdapter.isEnabled()){

		}else{

			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setMessage("NFC 初始化失败！，本配送程序必须打开!");
			dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
					// 设置完成后返回到原来的界面
					startActivityForResult(intent,0);
				}
			});
			dialog.show();
		}
		//一旦截获NFC消息，就会通过PendingIntent调用窗口
		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()), 0);
	}

	/**
	 * 获得焦点，按钮可以点击
	 */
	@Override
	public void onResume() {
		super.onResume();
		//设置处理优于所有其他NFC的处理
		if (mNfcAdapter != null)
			mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
	}

	/**
	 * 暂停Activity，界面获取焦点，按钮可以点击
	 */
	@Override
	public void onPause() {
		super.onPause();
		//恢复默认状态
		if (mNfcAdapter != null)
			mNfcAdapter.disableForegroundDispatch(this);
	}


	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}
	@Override
	void init() {
		try {
			setContentView(R.layout.activity_order_deal);

			//当前登录用户
			appContext = (AppContext) getApplicationContext();
			m_user = appContext.getUser();

			//获取传过来的任务订单参数
			Bundle bundle = new Bundle();
			bundle = this.getIntent().getExtras();
			String  strOrder = bundle.getString("order");
			m_OrderJson = new JSONObject(strOrder);
			m_taskId = bundle.getString("taskId");
			m_businessKey = bundle.getString("businessKey");

			//控件初始化
			m_buttonNext = (Button) findViewById(R.id.button_next);//下一步按钮
			m_textViewPayStatus = (TextView) findViewById(R.id.textview_payStatus);
			m_listView_kp = (ListView) findViewById(R.id.listview_kp);
			m_listView_zp = (ListView) findViewById(R.id.listview_zp);
			radioGroup_nfc=(RadioGroup)findViewById(R.id.radioGroup_nfc_id);
			radioButton_kp=(RadioButton)findViewById(R.id.radioButton_kp_id);
			radioButton_zp=(RadioButton)findViewById(R.id.radioButton_zp_id);
			m_spinnerPaytype = (Spinner) findViewById(R.id.spinner_payType);
			m_textViewPaytype = (TextView) findViewById(R.id.textview_payType);
			m_textViewTotalFee = (TextView) findViewById(R.id.textview_totalFee);
			m_imageViewKPEye = (ImageView) findViewById(R.id.imageView_KPEYE);
			m_imageViewZPEye = (ImageView) findViewById(R.id.imageView_ZPEYE);

			m_imageViewZPEye.setOnClickListener(this);
			m_imageViewKPEye.setOnClickListener(this);

			//获取支付状态
			JSONObject payStatusJson = m_OrderJson.getJSONObject("payStatus");
			m_orderPayStatus = payStatusJson.get("name").toString();
			m_textViewPayStatus.setText(m_orderPayStatus);

			//获取订单号
			m_orderId = m_businessKey;

			//获取商品总价
			m_totalFee = m_OrderJson.get("orderAmount").toString();
			m_textViewTotalFee.setText(m_totalFee);


			swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.main_srl);
			swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light,
					android.R.color.holo_orange_light, android.R.color.holo_red_light);
			swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
				@Override
				public void onRefresh() {
					refleshPayStatus();
				}
			});



			m_buttonNext.setOnClickListener(this);
			radioGroup_nfc.setOnCheckedChangeListener(listen);

			radioGroup_nfc.check(radioButton_kp.getId());//默认是空瓶
			m_selected_nfc_model = 0;

			m_textViewPaytype.setVisibility(View.INVISIBLE);


			m_spinnerPaytype.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
										   int pos, long id) {

					switch(pos){
						case 1:
							PayOnScan();
							break;
						default:
							break;
					}
				}
				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					// Another interface callback
				}
			});

			//数据结构初始化
			m_userBottlesMap = new HashMap<String, JSONObject>();
			m_myBottlesMap = new HashMap<String, JSONObject>();
			m_BottlesListKP = new ArrayList<String>();
			m_BottlesListZP = new ArrayList<String>();

			//获取当前配送工
			appContext = (AppContext) getApplicationContext();
			m_deliveryUser = appContext.getUser();

			//初始化两个LISTVIEW的点击事件
			m_listView_kp.setOnItemLongClickListener(new OnItemLongClickListener() {

				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					deleteKP(position);
					return true;
				}
			});
			m_listView_zp.setOnItemLongClickListener(new OnItemLongClickListener() {

				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					deleteZP(position);
					return true;
				}
			});




			//数据初始化
			setOrderHeadInfo();
			setOrderDetailsInfo();
			setOrderAppendInfo();
			getRecvLocation();
			getUserBottles();//获取用户名下的钢瓶号
			getMyBottles();//获取配送工名下的钢瓶号

			//如果结算类型不是普通用户，就隐藏支付方式选择
			if(!m_curUserSettlementType.get("code").toString().equals("00001")){
				m_spinnerPaytype.setVisibility(View.INVISIBLE);
				m_textViewPaytype.setVisibility(View.VISIBLE);
				m_textViewPaytype.setText(m_curUserSettlementType.get("name").toString());
			}



		}catch (JSONException e){
			Toast.makeText(OrderDealActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}
	private void PayOnScan(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		View view = View.inflate(this, R.layout.pay_on_scan, null);   // 账号、密码的布局文件，自定义
		ImageView QRcode = (ImageView)view.findViewById(R.id.items_imageViewScanCode);
		String  strUri = NetUrlConstant.PAYQRCODEURL+"?totalFee="+"1"+"&orderIndex="+m_businessKey;



		try {
			URL link = new URL(strUri);
			InputStream is = link.openStream();
			Bitmap bitmap = BitmapFactory.decodeStream( is );
			QRcode.setImageBitmap(bitmap);
			builder.setIcon(R.mipmap.ic_launcher);//设置对话框icon

			builder.setTitle("支付码");

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
			//获取传过来的订单参数
			JSONObject orderJson = m_OrderJson;
			m_orderId = orderJson.get("orderSn").toString();
			JSONObject addressJson = orderJson.getJSONObject("recvAddr");
			String strAddress = "地址："+addressJson.get("city").toString()+addressJson.get("county").toString()+addressJson.get("detail").toString();
			m_recvAddr = strAddress;

			//获取订单用户
			JSONObject customerJson = orderJson.getJSONObject("customer");
			m_curUserId = customerJson.get("userId").toString();
			//获取结算类型
			m_curUserSettlementType = customerJson.getJSONObject("settlementType");



		}catch (JSONException e){
			Toast.makeText(OrderDealActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

	//设置商品详情
	private void setOrderDetailsInfo() {
		try {
			//获取传过来的任务订单参数
			JSONObject orderJson = m_OrderJson;
			JSONArray orderDetailList = orderJson.getJSONArray("orderDetailList");
			List<Map<String,Object>> list_map = new ArrayList<Map<String,Object>>(); //定义一个适配器对象
			for (int i = 0; i < orderDetailList.length(); i++) {
				JSONObject orderDetail = orderDetailList.getJSONObject(i);  // 订单详情单条记录
				JSONObject goodDetail = orderDetail.getJSONObject("goods");  // 商品详情

				Map<String, Object> orderInfo = new HashMap<String, Object>(); //创建一个键值对的Map集合，用来存放名字和头像
				orderInfo.put("goodName",  goodDetail.get("name").toString());  //商品名称
				orderInfo.put("goodQuantity", "X"+orderDetail.get("quantity").toString());  //收货地址
				orderInfo.put("dealPrice", "￥"+orderDetail.get("dealPrice").toString());  //用户信息

				list_map.add(orderInfo);   //把这个存放好数据的Map集合放入到list中，这就完成类数据源的准备工作
			}
			//2、创建适配器（可以使用外部类的方式、内部类方式等均可）
			SimpleAdapter simpleAdapter = new SimpleAdapter(
					OrderDealActivity.this,/*传入一个上下文作为参数*/
					list_map,         /*传入相对应的数据源，这个数据源不仅仅是数据而且还是和界面相耦合的混合体。*/
					R.layout.order_detail_items, /*设置具体某个items的布局，需要是新的布局，而不是ListView控件的布局*/
					new String[]{"goodName", "goodQuantity", "dealPrice"}, /*传入上面定义的键值对的键名称,会自动根据传入的键找到对应的值*/
					new int[]{R.id.items_goodName, R.id.items_goodQuantity, R.id.items_dealPrice});
			//3、为listView加入适配器
			//m_listView.setAdapter(simpleAdapter);
			//setListViewHeightBasedOnChildren(m_listView);
		}catch (JSONException e){
			Toast.makeText(OrderDealActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}
	//获取订单的收货地址经纬度
	private void getRecvLocation(){
		try {
			JSONObject orderJson = m_OrderJson;
			m_recvLocation = new LatLng(orderJson.getDouble("recvLatitude"), orderJson.getDouble("recvLongitude"));
		} catch (JSONException e){
			Toast.makeText(OrderDealActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

	//设置订单附加信息
	private void setOrderAppendInfo() {
		try {
			//获取传过来的任务订单参数
			JSONObject orderJson = m_OrderJson;

			//支付状态
			JSONObject payStatusJson = orderJson.getJSONObject("payStatus");
			String strPayStatus = payStatusJson.get("name").toString();
			m_textViewPayStatus.setText(strPayStatus);

		}catch (JSONException e){
			Toast.makeText(OrderDealActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}


	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button_next:
				if(m_user==null){
					Toast.makeText(OrderDealActivity.this, "未登录！", Toast.LENGTH_LONG).show();
					return;
				}
				// get请求
				NetRequestConstant nrc = new NetRequestConstant();
				nrc.setType(HttpRequestType.GET);
				nrc.requestUrl = NetUrlConstant.TASKORDERDEALURL+"/"+m_taskId;
				nrc.context = this;
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("businessKey", m_businessKey);
				params.put("candiUser", m_user.getUsername());
				params.put("orderStatus", 1);
				nrc.setParams(params);

				getServer(new Netcallback() {
					public void preccess(Object res, boolean flag) {
						if(flag){
							HttpResponse response=(HttpResponse)res;
							if(response!=null){
								if(response.getStatusLine().getStatusCode()==200){
									Toast toast = Toast.makeText(OrderDealActivity.this, "抢单成功！", Toast.LENGTH_LONG);
									toast.setGravity(Gravity.CENTER, 0, 0);
									toast.show();

									MediaPlayer music = MediaPlayer.create(OrderDealActivity.this, R.raw.get_order);
									music.start();
									Intent intent = new Intent(getApplicationContext() , MainlyActivity.class);
									Bundle bundle = new Bundle();

									bundle.putInt("switchTab", 1);//tab跳转到我的订单
									intent.putExtras(bundle);

									startActivity(intent);
									finish();


								}else{
									Toast.makeText(OrderDealActivity.this, "抢单失败", Toast.LENGTH_LONG).show();
								}
							}else {
								Toast.makeText(OrderDealActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
							}
						} else {
							Toast.makeText(OrderDealActivity.this, "网络未连接！",
									Toast.LENGTH_LONG).show();
						}
					}
				}, nrc);

				break;
			case R.id.imageView_KPEYE:// 用户的气瓶
				Intent intentKP = new Intent();
				Bundle bundleKP = new Bundle();
				bundleKP.putString("userId", m_curUserId);
				intentKP.setClass(OrderDealActivity.this, MybottlesActivity.class);
				intentKP.putExtras(bundleKP);
				startActivity(intentKP);
				break;
			case R.id.imageView_ZPEYE://配送工的气瓶
				Intent intentZP = new Intent();
				Bundle bundleZP = new Bundle();
				bundleZP.putString("userId", m_deliveryUser.getUsername());
				intentZP.setClass(OrderDealActivity.this, MybottlesActivity.class);
				intentZP.putExtras(bundleZP);
				startActivity(intentZP);
				break;
			default:
				break;
		}

	}
	private void switchNavBar(){
		//获取当前位置
		AppContext appContext = (AppContext) getApplicationContext();
		LatLng startP = appContext.getLocation();

		LatLng endP = m_recvLocation;

		AmapNaviPage.getInstance().showRouteActivity(getApplicationContext(), new AmapNaviParams(new Poi("当前位置", startP, ""), null, new Poi(m_recvAddr, endP, ""), AmapNaviType.DRIVER), OrderDealActivity.this);
		AMapNavi mAMapNavi = null;
		mAMapNavi = AMapNavi.getInstance(this);
		mAMapNavi.setUseInnerVoice(true);
	}

	@Override
	public void onInitNaviFailure() {

	}

	@Override
	public void onLocationChange(AMapNaviLocation aMapNaviLocation) {

	}

	@Override
	public void onArriveDestination(boolean b) {

	}

	@Override
	public void onStartNavi(int i) {

	}

	@Override
	public void onCalculateRouteSuccess(int[] ints) {

	}

	@Override
	public void onCalculateRouteFailure(int i) {

	}

	@Override
	public void onGetNavigationText(String s) {

	}

	@Override
	public void onStopSpeaking() {

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

	}

	@Override
	public void onReCalculateRoute(int i) {

	}

	@Override
	public void onExitPage(int i) {

	}

	//刷新支付状态
	public void refleshPayStatus() {
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.ORDERURL;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("orderSn",m_orderId );//订单号
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==200){
							try {
								List<Map<String,Object>> list_map = new ArrayList<Map<String,Object>>(); //定义一个适配器对象
								JSONObject OrdersJson = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
								JSONArray OrdersListJson = OrdersJson.getJSONArray("items");
								if(OrdersListJson.length()==1){
									OrderDealActivity.m_orderPayStatus = OrdersListJson.getJSONObject(0).getJSONObject("payStatus").getString("name");//获取订单状态
									m_textViewPayStatus.setText(OrderDealActivity.m_orderPayStatus);
								}else{
									return;
								}

							}catch (IOException e){
								Toast.makeText(OrderDealActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
							}catch (JSONException e) {
								Toast.makeText(OrderDealActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
							}
						}
					}else {
						Toast.makeText(OrderDealActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(OrderDealActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
		handler.sendEmptyMessage(0x101);//通过handler发送一个更新数据的标记
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
								Toast.makeText(OrderDealActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
							}catch (JSONException e) {
								Toast.makeText(OrderDealActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
							}
						}
					}else {
						Toast.makeText(OrderDealActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(OrderDealActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
	}

	//获取配送工名下的瓶子
	public void getMyBottles() {

		AppContext appContext = (AppContext) getApplicationContext();
		User user = appContext.getUser();
		if (user == null) {
			Toast.makeText(OrderDealActivity.this, "请先登录!", Toast.LENGTH_LONG).show();
			return;
		}
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.GASCYLINDERURL;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("liableUserId",user.getUsername() );//责任人是当前用户
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
								Toast.makeText(OrderDealActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
							}catch (JSONException e) {
								Toast.makeText(OrderDealActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
							}
						}
					}else {
						Toast.makeText(OrderDealActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(OrderDealActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
	}

	//读标签
	public void onNewIntent(Intent intent) {
		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		String[] techList = tag.getTechList();
		boolean haveMifareUltralight = false;
		for (String tech : techList) {
			if (tech.indexOf("MifareUltralight") >= 0) {
				haveMifareUltralight = true;
				break;
			}
		}
//		if (!haveMifareUltralight) {
//			Toast.makeText(this, "不支持MifareUltralight数据格式", Toast.LENGTH_SHORT).show();
//			return;
//		}
		//String bottleCode = readTag(tag);//NFC中的钢瓶编码
		//测试代码
		String bottleCode;
		if (!haveMifareUltralight) {
			Toast.makeText(this, "不支持MifareUltralight数据格式", Toast.LENGTH_SHORT).show();
			if(m_selected_nfc_model==0){
				if(testValidKPIndex==2){
					bottleCode = m_testValidKP[1];
				}else{
					bottleCode = m_testValidKP[testValidKPIndex++];
				}


			}else{
				if(testValidZPIndex==2){
					bottleCode = m_testValidZP[1];
				}else{
					bottleCode = m_testValidZP[testValidZPIndex++];
				}
			}

		}else{
			bottleCode = readTag(tag);//NFC中的钢瓶编码
		}
		//////////////////////////////////////

		if(m_selected_nfc_model==0){//空瓶录入模式
			if(m_userBottlesMap.containsKey(bottleCode)){
				boolean contained = false;
				for(int i=0; i<m_BottlesListKP.size();i++){
					if(m_BottlesListKP.get(i).equals(bottleCode)){
						contained = true;
						break;
					}
				}
				if(!contained){//第一次扫
					m_BottlesListKP.add(bottleCode);
					refleshBottlesListKP();
				}
			}else{//非法钢瓶
				Toast.makeText(OrderDealActivity.this, "空瓶录入：钢瓶号 "+bottleCode+"  非法！",
						Toast.LENGTH_LONG).show();
			}

		}else if(m_selected_nfc_model==1){//重瓶录入模式
			if(m_myBottlesMap.containsKey(bottleCode)){
				boolean contained = false;
				for(int i=0; i<m_BottlesListZP.size();i++){
					if(m_BottlesListZP.get(i).equals(bottleCode)){
						contained = true;
						break;
					}
				}
				if(!contained){//第一次扫
					m_BottlesListZP.add(bottleCode);
					refleshBottlesListZP();
				}
			}else{//非法钢瓶
				Toast.makeText(OrderDealActivity.this, "重瓶录入：钢瓶号 "+bottleCode+"  非法！",
						Toast.LENGTH_LONG).show();
			}
		}


	}

	//读标签
	public String readTag(Tag tag) {
		MifareUltralight ultralight = MifareUltralight.get(tag);
		try {
			ultralight.connect();
			byte[] data = ultralight.readPages(4);
			return new String(data, Charset.forName("GB2312"));
		} catch (Exception e) {
		} finally {
			try {
				ultralight.close();
			} catch (Exception e) {
			}
		}
		return null;
	}

	//NFC更新空瓶表
	private void refleshBottlesListKP(){
		List<Map<String,Object>> list_map = new ArrayList<Map<String,Object>>(); //定义一个适配器对象

		for(int i=0;i<m_BottlesListKP.size(); i++){
			Map<String,Object> bottleInfo = new HashMap<String, Object>(); //创建一个键值对的Map集合，用来存放名字和头像
			bottleInfo.put("bottleCode", m_BottlesListKP.get(i));
			list_map.add(bottleInfo);   //把这个存放好数据的Map集合放入到list中，这就完成类数据源的准备工作
		}
		//2、创建适配器（可以使用外部类的方式、内部类方式等均可）
		SimpleAdapter simpleAdapter = new SimpleAdapter(
				OrderDealActivity.this,/*传入一个上下文作为参数*/
				list_map,         /*传入相对应的数据源，这个数据源不仅仅是数据而且还是和界面相耦合的混合体。*/
				R.layout.bottle_list_simple_items, /*设置具体某个items的布局，需要是新的布局，而不是ListView控件的布局*/
				new String[]{"bottleCode"}, /*传入上面定义的键值对的键名称,会自动根据传入的键找到对应的值*/
				new int[]{R.id.items_number}) ;

		m_listView_kp.setAdapter(simpleAdapter);
		setListViewHeightBasedOnChildren(m_listView_kp);
	}

	//NFC更新重瓶表
	private void refleshBottlesListZP(){
		List<Map<String,Object>> list_map = new ArrayList<Map<String,Object>>(); //定义一个适配器对象

		for(int i=0;i<m_BottlesListZP.size(); i++){
			Map<String,Object> bottleInfo = new HashMap<String, Object>(); //创建一个键值对的Map集合，用来存放名字和头像
			bottleInfo.put("bottleCode", m_BottlesListZP.get(i));
			list_map.add(bottleInfo);   //把这个存放好数据的Map集合放入到list中，这就完成类数据源的准备工作
		}
		//2、创建适配器（可以使用外部类的方式、内部类方式等均可）
		SimpleAdapter simpleAdapter = new SimpleAdapter(
				OrderDealActivity.this,/*传入一个上下文作为参数*/
				list_map,         /*传入相对应的数据源，这个数据源不仅仅是数据而且还是和界面相耦合的混合体。*/
				R.layout.bottle_list_simple_items, /*设置具体某个items的布局，需要是新的布局，而不是ListView控件的布局*/
				new String[]{"bottleCode"}, /*传入上面定义的键值对的键名称,会自动根据传入的键找到对应的值*/
				new int[]{R.id.items_number}) ;

		m_listView_zp.setAdapter(simpleAdapter);
		setListViewHeightBasedOnChildren(m_listView_zp);
	}

	//空瓶删除函数
	private void deleteKP(final int position){
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setMessage("删除钢瓶:  "+m_BottlesListKP.get(position)+"   ?");
		dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				m_BottlesListKP.remove(position);
				refleshBottlesListKP();
			}
		});
		dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		dialog.show();
	}

	//空瓶删除函数
	private void deleteZP(final int position){
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setMessage("删除钢瓶:  "+m_BottlesListZP.get(position)+"   ?");
		dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				m_BottlesListZP.remove(position);
				refleshBottlesListZP();
			}
		});
		dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		dialog.show();
	}



}
