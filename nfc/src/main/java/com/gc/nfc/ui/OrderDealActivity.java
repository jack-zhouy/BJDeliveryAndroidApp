package com.gc.nfc.ui;

import android.app.PendingIntent;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
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
import java.util.Arrays;
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


public class OrderDealActivity extends BaseActivity implements OnClickListener,AbsListView.OnScrollListener  {

	AlertDialog m_alertDialogTicketSelect;//气票选择窗口
	AlertDialog m_alertDialogTicketCoupon;//优惠券选择窗口

	private TextView m_textViewPayStatus;//支付状态
	private Button m_buttonNext;//下一步
	private ListView m_listView_ticket;// 气票列表
	private ListView m_listView_coupon;// 优惠券列表

	private Spinner m_spinnerPaytype; //支付类型
	private TextView m_textViewPaytype; //支付类型
	private TextView m_textViewTotalFee; //商品总价

	private ImageView m_imageViewTicketSelect; //气票查看
	private ImageView m_imageViewCouponSelect; //优惠券查看



	private SwipeRefreshLayout swipeRefreshLayout;

	private AppContext appContext;
	private JSONObject m_OrderJson;//订单详情
	private String m_orderId;//订单号
	private String m_taskId;//任务订单详情
	private int m_orderStatus;//订单状态
	private LatLng m_recvLocation;//收货地址经纬度
	private String m_businessKey;//订单号
	private String m_recvAddr;//收获地址
	private String m_totalFee;//商品总价
	private JSONObject m_curUserSettlementType;//结算类型

	private String m_curUserId;//该订单用户
	private User m_deliveryUser;//配送工
	public static String m_orderPayStatus;//支付状态

	private List<JSONObject> m_TicketList;//气票链表
	private List<JSONObject> m_CouponList;//优惠券链表

	private JSONArray m_ValidTicketJsonArray;//有效的气票json数组
	private JSONArray m_ValidCouponJsonArray;//有效的优惠券json数组


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



	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}
	@Override
	void init() {
		try {
			setContentView(R.layout.activity_order_deal);
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
			m_listView_ticket = (ListView) findViewById(R.id.listview_ticket);
			m_listView_coupon = (ListView) findViewById(R.id.listview_coupon);

			m_spinnerPaytype = (Spinner) findViewById(R.id.spinner_payType);
			m_textViewPaytype = (TextView) findViewById(R.id.textview_payType);
			m_textViewTotalFee = (TextView) findViewById(R.id.textview_totalFee);
			m_imageViewTicketSelect = (ImageView) findViewById(R.id.imageView_ticketSelect);
			m_imageViewCouponSelect = (ImageView) findViewById(R.id.imageView_couponSelect);

			m_imageViewTicketSelect.setOnClickListener(this);
			m_imageViewCouponSelect.setOnClickListener(this);

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

			//获取当前配送工
			appContext = (AppContext) getApplicationContext();
			m_deliveryUser = appContext.getUser();

			//初始化两个LISTVIEW的点击事件
			m_listView_ticket.setOnItemLongClickListener(new OnItemLongClickListener() {

				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					deleteTicket(position);
					return true;
				}
			});
			m_listView_coupon.setOnItemLongClickListener(new OnItemLongClickListener() {

				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					deleteCoupon(position);
					return true;
				}
			});




			//数据初始化
			setOrderHeadInfo();
			setOrderDetailsInfo();
			setOrderAppendInfo();
			getRecvLocation();

			//如果结算类型不是普通用户，就隐藏支付方式选择
			if(!m_curUserSettlementType.get("code").toString().equals("00001")){
				m_spinnerPaytype.setVisibility(View.INVISIBLE);
				m_textViewPaytype.setVisibility(View.VISIBLE);
				m_textViewPaytype.setText(m_curUserSettlementType.get("name").toString());
			}
			//获取用户有效的气票
			getCustomerTickets();


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
				break;
			case R.id.imageView_ticketSelect:// 用户的气票
				showTicketSelectAlertDialog(v);
				break;
			case R.id.imageView_couponSelect://用户的优惠券

				break;
			default:
				break;
		}

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

	//更新气票列表
	private void refleshTicketList(){
		List<Map<String,Object>> list_map = new ArrayList<Map<String,Object>>(); //定义一个适配器对象

		for(int i=0;i<m_TicketList.size(); i++){
			Map<String,Object> bottleInfo = new HashMap<String, Object>(); //创建一个键值对的Map集合，用来存放名字和头像
			bottleInfo.put("bottleCode", m_TicketList.get(i));
			list_map.add(bottleInfo);   //把这个存放好数据的Map集合放入到list中，这就完成类数据源的准备工作
		}
		//2、创建适配器（可以使用外部类的方式、内部类方式等均可）
		SimpleAdapter simpleAdapter = new SimpleAdapter(
				OrderDealActivity.this,/*传入一个上下文作为参数*/
				list_map,         /*传入相对应的数据源，这个数据源不仅仅是数据而且还是和界面相耦合的混合体。*/
				R.layout.bottle_list_simple_items, /*设置具体某个items的布局，需要是新的布局，而不是ListView控件的布局*/
				new String[]{"bottleCode"}, /*传入上面定义的键值对的键名称,会自动根据传入的键找到对应的值*/
				new int[]{R.id.items_number}) ;

		m_listView_ticket.setAdapter(simpleAdapter);
		setListViewHeightBasedOnChildren(m_listView_ticket);
	}

	//更新优惠券列表
	private void refleshCouponList(){
		List<Map<String,Object>> list_map = new ArrayList<Map<String,Object>>(); //定义一个适配器对象

		for(int i=0;i<m_CouponList.size(); i++){
			Map<String,Object> bottleInfo = new HashMap<String, Object>(); //创建一个键值对的Map集合，用来存放名字和头像
			bottleInfo.put("bottleCode", m_CouponList.get(i));
			list_map.add(bottleInfo);   //把这个存放好数据的Map集合放入到list中，这就完成类数据源的准备工作
		}
		//2、创建适配器（可以使用外部类的方式、内部类方式等均可）
		SimpleAdapter simpleAdapter = new SimpleAdapter(
				OrderDealActivity.this,/*传入一个上下文作为参数*/
				list_map,         /*传入相对应的数据源，这个数据源不仅仅是数据而且还是和界面相耦合的混合体。*/
				R.layout.bottle_list_simple_items, /*设置具体某个items的布局，需要是新的布局，而不是ListView控件的布局*/
				new String[]{"bottleCode"}, /*传入上面定义的键值对的键名称,会自动根据传入的键找到对应的值*/
				new int[]{R.id.items_number}) ;
		m_listView_coupon.setAdapter(simpleAdapter);
		setListViewHeightBasedOnChildren(m_listView_coupon);
	}

	//气票删除函数
	private void deleteTicket(final int position){
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setMessage("取消使用气票:  "+m_TicketList.get(position)+"   ?");
		dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				m_TicketList.remove(position);
				refleshTicketList();
			}
		});
		dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		dialog.show();
	}

	//优惠券删除函数
	private void deleteCoupon(final int position){
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setMessage("取消使用优惠券:  "+m_CouponList.get(position)+"   ?");
		dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				m_CouponList.remove(position);
				refleshCouponList();
			}
		});
		dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		dialog.show();
	}


	//气票选择窗口
	public void showTicketSelectAlertDialog(View view){

		final String[] items = {"Struts2","Spring","Hibernate","Mybatis","Spring MVC"};
		// 创建一个AlertDialog建造者
		AlertDialog.Builder alertDialogBuilder= new AlertDialog.Builder(this);
		// 设置标题
		alertDialogBuilder.setTitle("气票选择窗口");
		// 参数介绍
		// 第一个参数：弹出框的信息集合，一般为字符串集合
		// 第二个参数：被默认选中的，一个布尔类型的数组
		// 第三个参数：勾选事件监听
		alertDialogBuilder.setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {

			}
		});
		alertDialogBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				//TODO 业务逻辑代码

				// 关闭提示框
				m_alertDialogTicketSelect.dismiss();
			}
		});
		alertDialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// TODO 业务逻辑代码
				// 关闭提示框
				m_alertDialogTicketSelect.dismiss();
			}
		});
		m_alertDialogTicketSelect = alertDialogBuilder.create();
		m_alertDialogTicketSelect.show();
	}

	//获取用户名下的气票
	public void getCustomerTickets() {
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.TICKETURL;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("customerUserId",m_curUserId);//当前客户
		params.put("useStatus",0);//未使用
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==200){
							try {
								JSONObject TicketJson = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
								m_ValidTicketJsonArray = TicketJson.getJSONArray("items");

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

}
