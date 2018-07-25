package com.gc.nfc.ui;

import com.gc.nfc.R;
import com.gc.nfc.app.AppContext;
import com.gc.nfc.common.NetRequestConstant;
import com.gc.nfc.common.NetUrlConstant;
import com.gc.nfc.domain.User;
import com.gc.nfc.interfaces.Netcallback;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.ListAdapter;
import android.view.ViewGroup;
import android.view.Gravity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;


import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Poi;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AmapNaviPage;
import com.amap.api.navi.AmapNaviParams;
import com.amap.api.navi.AmapNaviTheme;
import com.amap.api.navi.AmapNaviType;
import com.amap.api.navi.AmapPageType;
import com.amap.api.navi.INaviInfoCallback;
import com.amap.api.navi.model.AMapNaviLocation;



import java.util.Date;
import java.text.SimpleDateFormat;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.provider.Settings;

import android.Manifest;
import android.content.pm.PackageManager;
public class OrderDetailActivity extends BaseActivity implements OnClickListener, INaviInfoCallback {

	private TextView m_textViewOrderSn;//订单号
	private TextView m_textViewUserId;//联系人
	private TextView m_textViewUserPhone;//联系人电话
	private TextView m_textViewCreateTime;//创建时间
	private TextView m_textViewAddress;//地址
	private TextView m_textViewPayTypeInfo;//用户欠款
	private TextView m_textViewPassedTime;//经过的时间



	private ImageView m_imageViewUserIcon;//用户头像

	private TextView m_textViewTotalQuantity;//合计数量
	private TextView m_textViewTotalMountOrignal;//原价合计金额
	private TextView m_textViewTotalMountDeal;//成交价合计金额


	private ListView m_listView;// 商品详情

	private TextView m_textViewPayStatus;//支付状态
	private TextView m_textViewOrderStatus;//订单状态
	private TextView m_textViewReserveTime;//预约时间
	private TextView m_textViewPs;//备注

	private boolean m_isTicketUser;//是否是气票用户



	private Button m_buttonNext;//下一步
	private ImageView m_imageViewNav;//导航
	private ImageView m_imageViewCall;//电话

	private AppContext appContext;


	private JSONObject m_OrderJson;//订单详情
	private String m_taskId;//任务订单详情
	private int m_orderStatus;//订单状态
	private User m_user;//当前登录用户

	private String m_currentCustomerId;//当前订单用户

	private LatLng m_recvLocation;//收货地址经纬度

	private String m_businessKey;//订单号

	private String m_recvAddr;//收获地址

	private String m_orderCreateTime;//订单创建时间

	private java.util.Timer timer;

	private String m_customerPhone;//客户电话

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mHandler.sendEmptyMessage(1);
	}
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			calculatePassedTime();
			mHandler.sendEmptyMessageDelayed(1, 1000);
		}

	};


	private  String getDatePoor(Date endDate, Date nowDate) {

		long nd = 1000 * 24 * 60 * 60;
		long nh = 1000 * 60 * 60;
		long nm = 1000 * 60;
		long ns = 1000;
		// 获得两个时间的毫秒时间差异
		long diff = endDate.getTime() - nowDate.getTime();
		// 计算差多少天
		long day = diff / nd;
		// 计算差多少小时
		long hour = diff % nd / nh+day*24;
		// 计算差多少分钟
		long min = diff % nd % nh / nm;
		// 计算差多少秒//输出结果
		long sec = diff % nd % nh % nm / ns;
		return hour + ":" + min + ":" + sec;
	}
	private void calculatePassedTime(){
		try {
			Date now = new Date();
			SimpleDateFormat simFormat = new SimpleDateFormat("yyyy-MM-d HH:mm:ss");
			Date before = simFormat.parse(m_orderCreateTime);
			m_textViewPassedTime.setText("已过 "+getDatePoor(now, before));
		}catch (ParseException e){
			Toast.makeText(OrderDetailActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	};
	@Override
	void init() {
		try {
			setContentView(R.layout.activity_order_detail);

			//当前登录用户
			appContext = (AppContext) getApplicationContext();
			m_user = appContext.getUser();
			if (m_user == null) {
				Toast.makeText(OrderDetailActivity.this, "登陆会话失效", Toast.LENGTH_LONG).show();
				Intent intent = new Intent(OrderDetailActivity.this, AutoLoginActivity.class);
				startActivity(intent);
				finish();
				return;
			}

			//获取传过来的任务订单参数
			Bundle bundle = new Bundle();
			bundle = this.getIntent().getExtras();
			String  strOrder = bundle.getString("order");
			m_OrderJson = new JSONObject(strOrder);
			m_taskId = bundle.getString("taskId");
			m_businessKey = bundle.getString("businessKey");
			m_orderStatus = bundle.getInt("orderStatus");


			//控件初始化
			m_buttonNext = (Button) findViewById(R.id.button_next);//下一步按钮
			m_textViewOrderSn = (TextView) findViewById(R.id.items_orderSn);
			m_textViewUserId = (TextView) findViewById(R.id.items_userId);
			m_textViewUserPhone = (TextView) findViewById(R.id.items_userPhone);
			m_textViewCreateTime = (TextView) findViewById(R.id.items_creatTime);
			m_textViewAddress = (TextView) findViewById(R.id.items_address);
			m_imageViewUserIcon= (ImageView) findViewById(R.id.items_imageUserIcon);
			m_textViewPayTypeInfo= (TextView) findViewById(R.id.items_userCredit);
			m_listView = (ListView) findViewById(R.id.listview);
			m_imageViewNav = (ImageView) findViewById(R.id.imageView_nav);
			m_imageViewCall = (ImageView) findViewById(R.id.imageView_call);


			m_textViewPayStatus = (TextView) findViewById(R.id.textview_payStatus);
			m_textViewOrderStatus = (TextView) findViewById(R.id.textview_orderStatus);
			m_textViewReserveTime = (TextView) findViewById(R.id.textview_reserveTime);
			m_textViewPs = (TextView) findViewById(R.id.textview_ps);
			m_textViewTotalQuantity = (TextView) findViewById(R.id.items_totalQuantity);
			m_textViewTotalMountOrignal = (TextView) findViewById(R.id.items_totalMountOrignal);
			m_textViewTotalMountDeal = (TextView) findViewById(R.id.items_totalMountDeal);
			m_textViewPassedTime = (TextView) findViewById(R.id.items_passedTime);




			m_buttonNext.setOnClickListener(this);
			m_imageViewNav.setOnClickListener(this);

			m_imageViewCall.setOnClickListener(this);


			//数据初始化
			setOrderHeadInfo();
			setOrderDetailsInfo();
			setOrderAppendInfo();
			getRecvLocation();
			//查询用户欠款
			getCustomerCredit();

			//初始化按钮显示
			switch (m_orderStatus){
				case 0://待派送
					m_buttonNext.setText("立即抢单");
					break;
				case 1://待配送
					m_buttonNext.setText("下一步");
					break;
				case -1://历史订单，不需要下一步操作按钮
					m_buttonNext.setVisibility(View.INVISIBLE);
					break;
				default:
					m_buttonNext.setVisibility(View.INVISIBLE);
					break;
			}

		}catch (JSONException e){
			Toast.makeText(OrderDetailActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

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
			String strOrderSn = "订单编号："+orderJson.get("orderSn").toString();
			m_textViewOrderSn.setText(strOrderSn);

			String strCreateTime = "下单时间："+orderJson.get("createTime").toString();
			m_textViewCreateTime.setText(strCreateTime);
			m_orderCreateTime = orderJson.get("createTime").toString();


			//获取订单用户
			JSONObject customerJson = orderJson.getJSONObject("customer");
			String strUserId = orderJson.get("recvName").toString();
			String strUserPhone = "电话："+orderJson.get("recvPhone").toString();
			m_textViewUserId.setText(strUserId);
			m_textViewUserPhone.setText(strUserPhone);
			m_currentCustomerId = customerJson.get("userId").toString();
			m_customerPhone = strUserPhone;


			//获取地址

			JSONObject addressJson = orderJson.getJSONObject("recvAddr");
			String strAddress = addressJson.get("city").toString()+addressJson.get("county").toString()+addressJson.get("detail").toString();
			m_textViewAddress.setText(strAddress);
			m_recvAddr = strAddress;

			//判断是不是气票用户
			//获取结算类型
			JSONObject curUserSettlementType = customerJson.getJSONObject("settlementType");
			if(curUserSettlementType.get("code").toString().equals("00003")) {//气票
				m_imageViewUserIcon.setImageResource(R.drawable.icon_ticket_user_white);
				m_isTicketUser = true;
			}else if(curUserSettlementType.get("code").toString().equals("00002")) {//月结
				m_imageViewUserIcon.setImageResource(R.drawable.icon_month_user_white);
				m_isTicketUser = false;
			} else{
				m_imageViewUserIcon.setImageResource(R.drawable.icon_common_user_white);
				m_isTicketUser = false;
			}

		}catch (JSONException e){
			Toast.makeText(OrderDetailActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

	//设置商品详情
	private void setOrderDetailsInfo() {
		try {
			int iTotalQuantity = 0;
			//订单总价
			m_textViewTotalMountOrignal.setText("￥"+m_OrderJson.getString("originalAmount"));
			m_textViewTotalMountDeal.setText("￥"+m_OrderJson.getString("orderAmount"));
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
				orderInfo.put("orignalPrice", "￥"+orderDetail.get("originalPrice").toString());  //原价
				orderInfo.put("dealPrice", "￥"+orderDetail.get("dealPrice").toString());  //成交价

				int iTempQuantity = Integer.parseInt(orderDetail.get("quantity").toString());
				iTotalQuantity+=iTempQuantity;

				list_map.add(orderInfo);   //把这个存放好数据的Map集合放入到list中，这就完成类数据源的准备工作
			}
			//订单商品总数量
			m_textViewTotalQuantity.setText("X"+Integer.toString(iTotalQuantity));
			//2、创建适配器（可以使用外部类的方式、内部类方式等均可）
			SimpleAdapter simpleAdapter = new SimpleAdapter(
					OrderDetailActivity.this,/*传入一个上下文作为参数*/
					list_map,         /*传入相对应的数据源，这个数据源不仅仅是数据而且还是和界面相耦合的混合体。*/
					R.layout.order_detail_items, /*设置具体某个items的布局，需要是新的布局，而不是ListView控件的布局*/
					new String[]{"goodName", "goodQuantity", "orignalPrice","dealPrice"}, /*传入上面定义的键值对的键名称,会自动根据传入的键找到对应的值*/
					new int[]{R.id.items_goodName, R.id.items_goodQuantity,  R.id.items_orignalPrice,R.id.items_dealPrice});
			//3、为listView加入适配器
			m_listView.setAdapter(simpleAdapter);
			setListViewHeightBasedOnChildren(m_listView);
		}catch (JSONException e){
			Toast.makeText(OrderDetailActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}
	//获取订单的收货地址经纬度
	private void getRecvLocation(){
		try {
			JSONObject orderJson = m_OrderJson;
			m_recvLocation = new LatLng(orderJson.getDouble("recvLatitude"), orderJson.getDouble("recvLongitude"));
		} catch (JSONException e){
			Toast.makeText(OrderDetailActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

	//设置订单附加信息
	private void setOrderAppendInfo() {
		try {
			m_textViewOrderStatus = (TextView) findViewById(R.id.textview_orderStatus);
			m_textViewReserveTime = (TextView) findViewById(R.id.textview_reserveTime);
			m_textViewPs = (TextView) findViewById(R.id.textview_ps);

			//获取传过来的任务订单参数
			JSONObject orderJson = m_OrderJson;

			//支付状态
			JSONObject payStatusJson = orderJson.getJSONObject("payStatus");
			String strPayStatus = payStatusJson.get("name").toString();
			m_textViewPayStatus.setText(strPayStatus);

			//订单状态
			String orderStatusDesc[] = {"待派送","派送中","已签收","订单结束","作废"};
			int iOrderStatus = Integer.parseInt(orderJson.get("orderStatus").toString());
			String strOrderStatus = orderStatusDesc[iOrderStatus];
			m_textViewOrderStatus.setText(strOrderStatus);

			//预约时间
			String strReserveTime = orderJson.get("reserveTime").toString();
			if(strReserveTime.equals("null")){
				m_textViewReserveTime.setText("无");
			}else{
				m_textViewReserveTime.setText(strReserveTime);
			}


			//备注
			String strComment = orderJson.get("comment").toString();
			if(strComment.equals("null")){
				m_textViewPs.setText("无");
			}else{
				m_textViewPs.setText(strComment);
			}


		}catch (JSONException e){
			Toast.makeText(OrderDetailActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}


	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button_next:
				if(m_orderStatus==0){//抢单
					getOrderOps();
				}else if(m_orderStatus==1){//配送
					dealOrderOps();
				}else{
					return;
				}

				break;
			case R.id.imageView_nav://导航
				switchNavBar();
				break;
			case R.id.imageView_call://打电话
				callPhone(m_customerPhone);
				break;
			default:
				break;
		}

	}

	//配送
	private void dealOrderOps(){
		Intent intent = new Intent();
		Bundle bundle = new Bundle();
		bundle.putString("taskId", m_taskId);
		bundle.putString("order", m_OrderJson.toString());
		bundle.putString("businessKey", m_businessKey);//订单号
		intent.setClass(OrderDetailActivity.this, BottleExchangeActivity.class);
		intent.putExtras(bundle);
		startActivity(intent);
	}


	//抢单
	private void getOrderOps(){
		if(m_user==null){
			Toast.makeText(OrderDetailActivity.this, "未登录！", Toast.LENGTH_LONG).show();
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
							Toast toast = Toast.makeText(OrderDetailActivity.this, "抢单成功！", Toast.LENGTH_LONG);
							toast.setGravity(Gravity.CENTER, 0, 0);
							toast.show();

							MediaPlayer music = MediaPlayer.create(OrderDetailActivity.this, R.raw.get_order);
							music.start();
							Intent intent = new Intent(getApplicationContext() , MainlyActivity.class);
							Bundle bundle = new Bundle();

							bundle.putInt("switchTab", 1);//tab跳转到我的订单
							intent.putExtras(bundle);

							startActivity(intent);
							finish();


						}else{
							Toast.makeText(OrderDetailActivity.this, "抢单失败", Toast.LENGTH_LONG).show();
						}
					}else {
						Toast.makeText(OrderDetailActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(OrderDetailActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);

	}

	//导航
	private void switchNavBar(){
		//获取当前位置
		AppContext appContext = (AppContext) getApplicationContext();
		LatLng startP = appContext.getLocation();

		LatLng endP = m_recvLocation;

		AmapNaviPage.getInstance().showRouteActivity(getApplicationContext(), new AmapNaviParams(new Poi("当前位置", startP, ""), null, new Poi(m_recvAddr, endP, ""), AmapNaviType.DRIVER), OrderDetailActivity.this);
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

	//查询用户欠款
	private boolean getCustomerCredit() {
		if(m_isTicketUser){
			String payTypeInfo = "欠款：￥0";
			m_textViewPayTypeInfo.setText(payTypeInfo);
			return true;
		}

		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.CUSTOMERCREDITURL;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("userId",m_currentCustomerId);
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==200){
							try {
								JSONObject creditJsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
								JSONArray m_creditJsonArray = creditJsonObject.getJSONArray("items");
								if(m_creditJsonArray.length()==1){
									JSONObject creditJson = m_creditJsonArray.getJSONObject(0);
									JSONObject creditType = creditJson.getJSONObject("creditType");

									String payTypeInfo = "欠款：￥"+creditJson.get("amount").toString();
									m_textViewPayTypeInfo.setText(payTypeInfo);
								}else{
									String payTypeInfo = "欠款：￥0";
									m_textViewPayTypeInfo.setText(payTypeInfo);
								}
							}catch (IOException e){
								Toast.makeText(OrderDetailActivity.this, "查询用户欠款失败！，异常IOException",
										Toast.LENGTH_LONG).show();
							}catch (JSONException e) {
								Toast.makeText(OrderDetailActivity.this, "查询用户欠款失败！，异常JSONException",
										Toast.LENGTH_LONG).show();
							}
						}else{
							Toast.makeText(OrderDetailActivity.this, "查询用户欠款失败！，错误"+response.getStatusLine().getStatusCode(),
									Toast.LENGTH_LONG).show();
						}
					}else {
						Toast.makeText(OrderDetailActivity.this, "查询用户欠款失败！，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(OrderDetailActivity.this, "网络未连接！", Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
		return true;
	}

	/**
	 * 拨打电话（直接拨打电话）
	 *
	 * @param phoneNum 电话号码
	 */
	public void callPhone(String phoneNum) {
		// 检查是否获得了权限（Android6.0运行时权限）
		if (ActivityCompat.checkSelfPermission(OrderDetailActivity.this,Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
// 没有获得授权，申请授权
			if (ActivityCompat.shouldShowRequestPermissionRationale(OrderDetailActivity.this,Manifest.permission.CALL_PHONE)) {
// 返回值：
//如果app之前请求过该权限,被用户拒绝, 这个方法就会返回true.
//如果用户之前拒绝权限的时候勾选了对话框中”Don’t ask again”的选项,那么这个方法会返回false.
//如果设备策略禁止应用拥有这条权限, 这个方法也返回false.
// 弹窗需要解释为何需要该权限，再次请求授权
				Toast.makeText(OrderDetailActivity.this, "请授权！", Toast.LENGTH_LONG).show();
// 帮跳转到该应用的设置界面，让用户手动授权
				Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
				Uri uri = Uri.fromParts("package", getPackageName(), null);
				intent.setData(uri);
				startActivity(intent);
			}else{
// 不需要解释为何需要该权限，直接请求授权
				ActivityCompat.requestPermissions(OrderDetailActivity.this,new String[]{Manifest.permission.CALL_PHONE},1);
			}
		}else {
// 已经获得授权，可以打电话
			Intent intent = new Intent(Intent.ACTION_CALL);
			Uri data = Uri.parse("tel:" + phoneNum);
			intent.setData(data);
			startActivity(intent);
		}


	}

}
