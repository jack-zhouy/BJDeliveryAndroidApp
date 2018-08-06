package com.gc.nfc.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.model.LatLng;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;


public class TrayOrderDealActivity extends BaseActivity implements OnClickListener,AbsListView.OnScrollListener  {


	private TextView m_textViewPayStatus;//支付状态
	private Button m_buttonNext;//下一步
	private Spinner m_spinnerPaytype; //支付类型
	private TextView m_textViewPaytype; //支付类型
	private TextView m_textViewTotalFee; //商品总价
	private TextView m_textViewOrginalFee; //商品原价
	private TextView m_textViewResidualGasFee; //残气价格



	private ListView m_listView_kp;// 空瓶号列表


	private SwipeRefreshLayout swipeRefreshLayout;

	private String m_depLeader;//配送工所属门店责任人
	private AppContext appContext;
	private JSONObject m_OrderJson;//订单详情
	private String m_orderId;//订单号
	private String m_taskId;//任务订单ID
	private String m_totalFee;//商品总价
	private String m_curUserId;//该订单用户
	private User m_deliveryUser;//配送工
	public static String m_orderPayStatus;//支付状态
	private Map<String, String> m_BottlesMapKP;//空瓶表
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
	public void onScrollStateChanged(AbsListView view, int scrollState) {return;}
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {return;}
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}
	@Override
	void init() {
		try {
			setContentView(R.layout.activity_tray_order_deal);
			//获取传过来的任务订单参数
			m_BottlesMapKP = new HashMap<String,String>();
			Bundle bundle = new Bundle();
			bundle = this.getIntent().getExtras();
			String  strOrder = bundle.getString("order");
			m_OrderJson = new JSONObject(strOrder);
			m_taskId = bundle.getString("taskId");
			m_orderId = bundle.getString("businessKey");
			m_totalFee = "";

			Set keySet = bundle.keySet();   // 得到bundle中所有的key
			Iterator iter = keySet.iterator();
			while(iter.hasNext())
			{
				String key = (String)iter.next();
				if(key.contains("KMA")){
					m_BottlesMapKP.put(key, bundle.getString(key));
				}
			}


			//控件初始化
			m_buttonNext = (Button) findViewById(R.id.button_next);//下一步按钮
			m_textViewPayStatus = (TextView) findViewById(R.id.textview_payStatus);
			m_spinnerPaytype = (Spinner) findViewById(R.id.spinner_payType);
			m_textViewPaytype = (TextView) findViewById(R.id.textview_payType);
			m_textViewTotalFee = (TextView) findViewById(R.id.textview_totalFee);
			m_textViewOrginalFee = (TextView) findViewById(R.id.textview_orginalFee);
			m_textViewResidualGasFee = (TextView) findViewById(R.id.textview_residualGasFee);
			m_listView_kp = (ListView) findViewById(R.id.listview_kp);
			//获取支付状态
			JSONObject payStatusJson = m_OrderJson.getJSONObject("payStatus");
			m_orderPayStatus = payStatusJson.get("name").toString();
			m_textViewPayStatus.setText(m_orderPayStatus);




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
			m_spinnerPaytype.setSelection(1,true);
			m_spinnerPaytype.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
										   int pos, long id) {

					switch(pos){
						case 0://扫码
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

			//数据初始化
			setOrderAppendInfo();//设置支付状态
			GetDepLeader();//获取配送工的门店领导


			if(m_BottlesMapKP.size()!=0){
				refleshBottlesMapKP();//刷新空瓶表
				//计算订单价格
				OrderCalculate();
			}



		}catch (JSONException e){
			Toast.makeText(TrayOrderDealActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}
	private void PayOnScan(){
		if(m_totalFee.length()==0){
			Toast.makeText(TrayOrderDealActivity.this, "计价失败！无法扫码支付",
					Toast.LENGTH_LONG).show();
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		View view = View.inflate(this, R.layout.pay_on_scan, null);   // 账号、密码的布局文件，自定义
		ImageView QRcode = (ImageView)view.findViewById(R.id.items_imageViewScanCode);
		double dPayMount = Double.parseDouble(m_totalFee)*100;
		int iPayMount = (int) dPayMount;
		String  strUri = NetUrlConstant.PAYQRCODEURL+"?totalFee="+iPayMount+"&orderIndex="+m_orderId;

		try {
			URL link = new URL(strUri);
			InputStream is = link.openStream();
			Bitmap bitmap = BitmapFactory.decodeStream( is );
			QRcode.setImageBitmap(bitmap);
			builder.setIcon(R.drawable.icon_logo);//设置对话框icon

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



	//设置订单的支付状态
	private void setOrderAppendInfo() {
		try {
			//获取传过来的任务订单参数
			JSONObject orderJson = m_OrderJson;

			//支付状态
			JSONObject payStatusJson = orderJson.getJSONObject("payStatus");
			String strPayStatus = payStatusJson.get("name").toString();
			m_textViewPayStatus.setText(strPayStatus);

			String refoundSum = orderJson.getString("refoundSum");
			String orderAmount = orderJson.getString("orderAmount");
			String originalAmount = orderJson.getString("originalAmount");

			m_textViewTotalFee.setText("￥"+orderAmount);
			m_textViewOrginalFee.setText("￥"+originalAmount);
			m_textViewResidualGasFee.setText("￥"+refoundSum);
			m_totalFee = orderAmount;

		}catch (JSONException e){
			Toast.makeText(TrayOrderDealActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}


	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button_next:
			{
				if(m_depLeader==null){
					Toast.makeText(TrayOrderDealActivity.this, "所属店长查询失败！请联系客服予以解决！",
							Toast.LENGTH_LONG).show();
					return;
				}else{
					Toast.makeText(TrayOrderDealActivity.this, "正在提交，请稍等。。。",
							Toast.LENGTH_LONG).show();
					m_buttonNext.setText("正在提交...");
					m_buttonNext.setBackgroundColor(getResources().getColor(R.color.transparent_background));
					m_buttonNext.setEnabled(false);
					handlerDelayCommit.sendEmptyMessageDelayed(0,1000);
				}


			}
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
									TrayOrderDealActivity.m_orderPayStatus = OrdersListJson.getJSONObject(0).getJSONObject("payStatus").getString("name");//获取订单状态
									m_textViewPayStatus.setText(TrayOrderDealActivity.m_orderPayStatus);
								}else{
									return;
								}

							}catch (IOException e){
								Toast.makeText(TrayOrderDealActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
							}catch (JSONException e) {
								Toast.makeText(TrayOrderDealActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
							}
						}
					}else {
						Toast.makeText(TrayOrderDealActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(TrayOrderDealActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
		handler.sendEmptyMessage(0x101);//通过handler发送一个更新数据的标记
	}


	//非气票用户支付
	private boolean commonUserPay() {
			// get请求
			NetRequestConstant nrc = new NetRequestConstant();
			nrc.setType(HttpRequestType.PUT);

			nrc.requestUrl = NetUrlConstant.ORDERURL+"/"+m_orderId;
			nrc.context = this;
			Map<String, Object> body = new HashMap<String, Object>();

			//如果结算类型是普通用户，就需要提交支付方式参数
			String payTypes[] = {"PTOnLine","PTCash","PTDebtCredit"};//扫码，现金，赊销
			body.put("payType",payTypes[m_spinnerPaytype.getSelectedItemPosition()]);//支付方式
			//如果是扫码，支付状态没有支付就不允许提交订单
			if(m_spinnerPaytype.getSelectedItemPosition()==0){
				if(!m_orderPayStatus.equals("已支付")){
					Toast.makeText(TrayOrderDealActivity.this, "扫码支付结果未返回！",
							Toast.LENGTH_LONG).show();
					return false;
				}
			}

			//如果是已支付，支付方式只能为扫码
			if(m_orderPayStatus.equals("已支付")){
				if(m_spinnerPaytype.getSelectedItemPosition()!=0) {
					Toast.makeText(TrayOrderDealActivity.this, "订单已经支付，确认支付类型为扫码？",
							Toast.LENGTH_LONG).show();
					return false;
				}
			}
			body.put("payStatus","PSPaied");// 修改订单状态为已经支付
			nrc.setBody(body);
			getServer(new Netcallback() {
				public void preccess(Object res, boolean flag) {
					if(flag){
						HttpResponse response=(HttpResponse)res;
						if(response!=null){
							if(response.getStatusLine().getStatusCode()==200){
								//完成配送
								deliverOver();
							}else if(response.getStatusLine().getStatusCode()==404){
								Toast.makeText(TrayOrderDealActivity.this, "订单不存在",
										Toast.LENGTH_LONG).show();
							} else if(response.getStatusLine().getStatusCode()==401){
								Toast.makeText(TrayOrderDealActivity.this, "鉴权失败，请重新登录"+response.getStatusLine().getStatusCode(),
										Toast.LENGTH_LONG).show();
							}else{
								Toast.makeText(TrayOrderDealActivity.this, "支付失败" + response.getStatusLine().getStatusCode(),
										Toast.LENGTH_LONG).show();
							}
						}else {
							Toast.makeText(TrayOrderDealActivity.this, "未知错误，异常！",
									Toast.LENGTH_LONG).show();
						}
					} else {
						Toast.makeText(TrayOrderDealActivity.this, "网络未连接！",
								Toast.LENGTH_LONG).show();
					}
				}
			}, nrc);
			return true;


	}

	//配送完成
	private boolean deliverOver() {

		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.TASKORDERDEALURL+"/"+m_taskId;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();


		params.put("businessKey",m_orderId);
		if(m_depLeader==null){
			Toast.makeText(TrayOrderDealActivity.this, "所属店长查询失败！",
					Toast.LENGTH_LONG).show();
			return false;
		}
		params.put("candiUser", m_depLeader);
		params.put("orderStatus", 2);//待核单
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==200){
							Toast.makeText(TrayOrderDealActivity.this, "订单配送成功",
									Toast.LENGTH_LONG).show();
							MediaPlayer music = MediaPlayer.create(TrayOrderDealActivity.this, R.raw.finish_order);
							music.start();
							Intent intent = new Intent(getApplicationContext() , MainlyActivity.class);
							Bundle bundle = new Bundle();

							bundle.putInt("switchTab", 1);//tab跳转到我的订单
							intent.putExtras(bundle);

							startActivity(intent);
							finish();
						} else{
							Toast.makeText(TrayOrderDealActivity.this, "订单配送失败",
									Toast.LENGTH_LONG).show();
						}
					}else {
						Toast.makeText(TrayOrderDealActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(TrayOrderDealActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
		return true;
	}

	//获取本配送工的店长
	private void GetDepLeader() {
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.TGETDEPLEADERURL;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();


		params.put("userId",m_deliveryUser.getUsername());
		//params.put("candiUser", m_user.getUsername());
		params.put("groupCode", "00005");//门店店长
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag) {
					HttpResponse response = (HttpResponse) res;
					if (response != null) {
						if (response.getStatusLine().getStatusCode() == 200) {
							try {
								JSONObject usersJson = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
								JSONArray usersListJson = usersJson.getJSONArray("items");
								for(int i=0; i<usersListJson.length(); i++){
									if(i == 0){
										m_depLeader = usersListJson.getJSONObject(i).getString("userId");
									}else{
										m_depLeader = m_depLeader+","+usersListJson.getJSONObject(i).getString("userId");
									}
								}
							} catch (JSONException e) {
								Toast.makeText(TrayOrderDealActivity.this, "未知错误，异常！" + e.getMessage(),
										Toast.LENGTH_LONG).show();
							} catch (IOException e) {
								Toast.makeText(TrayOrderDealActivity.this, "未知错误，异常！" + e.getMessage(),
										Toast.LENGTH_LONG).show();
							}
						} else {
							Toast.makeText(TrayOrderDealActivity.this, "订单配送失败",
									Toast.LENGTH_LONG).show();
						}
					} else {
						Toast.makeText(TrayOrderDealActivity.this, "网络未连接！",
								Toast.LENGTH_LONG).show();
					}
				}}}, nrc);
	}

	//延时提交，防止乱点
	private Handler handlerDelayCommit = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if(m_depLeader==null){
				Toast.makeText(TrayOrderDealActivity.this, "所属店长查询失败！",
						Toast.LENGTH_LONG).show();
				return;
			}
			if(m_orderPayStatus.equals("已支付")){
				//完成配送
				deliverOver();
			}else{
					commonUserPay();
			}
			m_buttonNext.setText("下一步");
			m_buttonNext.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
			m_buttonNext.setEnabled(true);
			super.handleMessage(msg);
		}
	};


	//NFC更新空瓶表
	private void refleshBottlesMapKP(){
		List<Map<String,Object>> list_map = new ArrayList<Map<String,Object>>(); //定义一个适配器对象
		for (Map.Entry<String, String> entry : m_BottlesMapKP.entrySet()) {
			Map<String,Object> bottleInfo = new HashMap<String, Object>(); //创建一个键值对的Map集合，用来存放名字和头像

			bottleInfo.put("bottleCode", entry.getKey());
			bottleInfo.put("bottleWeight", entry.getValue());
			list_map.add(bottleInfo);   //把这个存放好数据的Map集合放入到list中，这就完成类数据源的准备工作
		}

		//2、创建适配器（可以使用外部类的方式、内部类方式等均可）
		SimpleAdapter simpleAdapter = new SimpleAdapter(
				TrayOrderDealActivity.this,/*传入一个上下文作为参数*/
				list_map,         /*传入相对应的数据源，这个数据源不仅仅是数据而且还是和界面相耦合的混合体。*/
				R.layout.bottle_list_tray_pay_items, /*设置具体某个items的布局，需要是新的布局，而不是ListView控件的布局*/
				new String[]{"bottleCode","bottleWeight"}, /*传入上面定义的键值对的键名称,会自动根据传入的键找到对应的值*/
				new int[]{R.id.items_number,R.id.items_weight}) ;

		m_listView_kp.setAdapter(simpleAdapter);
		setListViewHeightBasedOnChildren(m_listView_kp);
	}
	//订单残气计算
	private void OrderCalculate() {
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.ORDERCACULATEURL;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		String bottleNumbers = "";
		int tempCount = 0;
		for (Map.Entry<String, String> entry : m_BottlesMapKP.entrySet()) {
			if(tempCount!=0){
				bottleNumbers +=",";
			}
			bottleNumbers += entry.getKey();
			tempCount++;
		}

		params.put("orderSn",m_orderId);
		params.put("gasCynNumbers", bottleNumbers);
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag) {
					HttpResponse response = (HttpResponse) res;
					if (response != null) {
						if (response.getStatusLine().getStatusCode() == 200) {
							try {
								JSONObject orderJson = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
								String refoundSum = orderJson.getString("refoundSum");
								String orderAmount = orderJson.getString("orderAmount");
								String originalAmount = orderJson.getString("originalAmount");
								JSONObject payStatusJson = orderJson.getJSONObject("payStatus");

								m_textViewTotalFee.setText("￥"+orderAmount);
								m_textViewOrginalFee.setText("￥"+originalAmount);
								m_textViewResidualGasFee.setText("￥"+refoundSum);
								m_totalFee = orderAmount;
							} catch (JSONException e) {
								Toast.makeText(TrayOrderDealActivity.this, "未知错误，异常！" + e.getMessage(),
										Toast.LENGTH_LONG).show();
							} catch (IOException e) {
								Toast.makeText(TrayOrderDealActivity.this, "未知错误，异常！" + e.getMessage(),
										Toast.LENGTH_LONG).show();
							}
						} else {
							Toast.makeText(TrayOrderDealActivity.this, "订单残气计算失败!",
									Toast.LENGTH_LONG).show();
						}
					} else {
						Toast.makeText(TrayOrderDealActivity.this, "网络未连接！",
								Toast.LENGTH_LONG).show();
					}
				}}}, nrc);
	}


}
