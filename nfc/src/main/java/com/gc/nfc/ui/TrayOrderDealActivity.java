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
import android.view.LayoutInflater;
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
import java.text.NumberFormat;
import com.gc.nfc.domain.RefoundDetail;
import java.math.RoundingMode;

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
	private Map<String, RefoundDetail> m_BottlesMapKP;//获取所有需要进行抵扣的空瓶相关详情
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
			m_BottlesMapKP = new HashMap<String,RefoundDetail>();
			Bundle bundle = new Bundle();
			bundle = this.getIntent().getExtras();
			String  strOrder = bundle.getString("order");
			m_OrderJson = new JSONObject(strOrder);
			m_taskId = bundle.getString("taskId");
			m_orderId = bundle.getString("businessKey");
			m_totalFee = "";

			//获取订单用户
			JSONObject customerJson = m_OrderJson.getJSONObject("customer");
			m_curUserId = customerJson.get("userId").toString();


			//获取所有需要进行抵扣的空瓶号
			Set keySet = bundle.keySet();   // 得到bundle中所有的key
			Iterator iter = keySet.iterator();
			while(iter.hasNext()) {
				String key = (String)iter.next();
				if(key.contains("KMA")){
					RefoundDetail refoundDetail = new RefoundDetail();
					refoundDetail.kp_weight = bundle.getString(key);//空瓶称重
					refoundDetail.forceCaculate = false;
					refoundDetail.isOK = false;//是否计算成功
					refoundDetail.isFirst = true;//是否为第一次计算
					refoundDetail.code = key;//钢瓶编号
					m_BottlesMapKP.put(key, refoundDetail);
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
					if(m_BottlesMapKP.size()!=0){
						//计算订单价格
						OrderCalculate();
					}else{//没有退残,以当前订单的支付金额为准
						refleshPayStatus();
					}

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
			GetDepLeader();//获取配送工的门店领导

			if(m_BottlesMapKP.size()!=0){
				//计算订单价格
				OrderCalculate();
			}else{//没有退残,以当前订单的支付金额为准
				setOrderAppendInfo(m_OrderJson);
			}

			//绑定空瓶原始价格输入事件


			//初始化两个LISTVIEW的点击事件，目前没有实现交接的回撤
			m_listView_kp.setOnItemLongClickListener(new OnItemLongClickListener() {

				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					//录入钢瓶原始价格及充装量
					TextView bottleCodeTextView = (TextView)view.findViewById(R.id.items_number);
					String bottleCode = bottleCodeTextView.getText().toString();
					RefoundDetail stRefoundDetail = m_BottlesMapKP.get(bottleCode);
					if(stRefoundDetail.isOK){
						Toast.makeText(TrayOrderDealActivity.this, "计算完成，无需干预！",
								Toast.LENGTH_LONG).show();
					}else{
						getRefoundNeededParams(bottleCode);
					}

					//deleteKP(position);
					return true;
				}
			});
		}catch (JSONException e){
			Toast.makeText(TrayOrderDealActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}
	private void PayOnScan(){
		//残气计算失败
		if(!isPayStatus()){
			Toast.makeText(TrayOrderDealActivity.this, "残气计算还未完成！无法扫码支付",
					Toast.LENGTH_LONG).show();
			return;
		}
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
		String  strUri = NetUrlConstant.PAYQRCODEURL+"?totalFee="+iPayMount+"&orderIndex="+m_orderId+"&userId="+m_deliveryUser.getUsername();

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



	//更新订单的支付状态及相关金额
	private void setOrderAppendInfo(JSONObject orderJson) {
		try {
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
									JSONObject OrderJson = OrdersListJson.getJSONObject(0);
									//更新控件
									setOrderAppendInfo(OrderJson);
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
		String payTypes[] = {"PTOnLine","PTCash","PTDebtCredit","PTWxOffLine"};//扫码，现金，赊销,微信线下扫码
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
			if(!isPayStatus()){
				Toast.makeText(TrayOrderDealActivity.this, "残气计算还未完成！无法支付",
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


	@Override
	public void onPointerCaptureChanged(boolean hasCapture) {

	}

	//NFC更新空瓶表
	private void refleshBottlesMapKP(){
		List<Map<String,Object>> list_map = new ArrayList<Map<String,Object>>(); //定义一个适配器对象
		for (Map.Entry<String, RefoundDetail> entry : m_BottlesMapKP.entrySet()) {
			Map<String,Object> bottleInfo = new HashMap<String, Object>(); //创建一个键值对的Map集合，用来存放名字和头像
			RefoundDetail stRefoundDetail = entry.getValue();
			if(stRefoundDetail.forceCaculate){
				bottleInfo.put("note", "（干预瓶）:"+stRefoundDetail.note);//备注

			}else{
				bottleInfo.put("note", "(正常瓶）:"+stRefoundDetail.note);//备注

			}
			bottleInfo.put("code", stRefoundDetail.code);//钢瓶编号
			bottleInfo.put("kp_weight", "空瓶重:"+stRefoundDetail.kp_weight);//空瓶称重
			bottleInfo.put("tare_weight", "瓶皮重:"+stRefoundDetail.tare_weight);//钢瓶皮重
			bottleInfo.put("canqi_weight", "残气量:"+stRefoundDetail.canqi_weight);//残气重量
			bottleInfo.put("chongzhuang_weight", "充装量:"+stRefoundDetail.chongzhuang_weight);//充装重量
			bottleInfo.put("original_price", "购买价:"+stRefoundDetail.original_price);//原购买价格
			bottleInfo.put("gas_price", "均气价:"+stRefoundDetail.gas_price);//气价元/公斤
			bottleInfo.put("canqi_price", "退残额:"+stRefoundDetail.canqi_price);//残气金额

			if(stRefoundDetail.isOK){//是否异常
				bottleInfo.put("icon", R.drawable.icon_bottle);
			}else{
				bottleInfo.put("icon", R.drawable.warning);
			}

			list_map.add(bottleInfo);   //把这个存放好数据的Map集合放入到list中，这就完成类数据源的准备工作
		}

		//2、创建适配器（可以使用外部类的方式、内部类方式等均可）
		SimpleAdapter simpleAdapter = new SimpleAdapter(
				TrayOrderDealActivity.this,/*传入一个上下文作为参数*/
				list_map,         /*传入相对应的数据源，这个数据源不仅仅是数据而且还是和界面相耦合的混合体。*/
				R.layout.bottle_list_tray_pay_items, /*设置具体某个items的布局，需要是新的布局，而不是ListView控件的布局*/
				new String[]{"note","icon", "code","kp_weight","tare_weight","canqi_weight","chongzhuang_weight","original_price","gas_price","canqi_price"}, /*传入上面定义的键值对的键名称,会自动根据传入的键找到对应的值*/
				new int[]{R.id.items_note,R.id.items_imageViewIsOK,R.id.items_number,R.id.items_kpweight,R.id.items_tareweight,R.id.items_canqiweight,
						R.id.items_chongweight,R.id.items_originalPrice,R.id.items_gasPrice,R.id.items_canqiPrice}) ;

		m_listView_kp.setAdapter(simpleAdapter);
		setListViewHeightBasedOnChildren(m_listView_kp);
	}
	//订单残气计算
	private void OrderCalculate() {
		NetRequestConstant nrc = new NetRequestConstant();
		try {
			// get请求
			nrc.setType(HttpRequestType.PUT);
			nrc.requestUrl = NetUrlConstant.ORDERCACULATEURL + "/" + m_orderId;
			nrc.context = this;
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("customerId", m_curUserId);
			nrc.setParams(params);
//		//构建body
			Map<String, Object> body = new HashMap<String, Object>();
			JSONArray refoundDetailJSONArray = new JSONArray();
			for (Map.Entry<String, RefoundDetail> entry : m_BottlesMapKP.entrySet()) {
				RefoundDetail refoundDetail = entry.getValue();
				JSONObject refoundDetailJSONObject = new JSONObject();
				refoundDetailJSONObject.put("gasCynNumber", refoundDetail.code);//钢瓶号
				refoundDetailJSONObject.put("refoundWeight", refoundDetail.kp_weight);//回瓶重量
				if(refoundDetail.isFirst){//第一次不需要强制
					refoundDetailJSONObject.put("forceCaculate", false);//是否强制计算
					refoundDetail.isFirst = false;
				}else{
					if(refoundDetail.isOK){
						if(refoundDetail.forceCaculate){//强制完成的还需要强制
							refoundDetailJSONObject.put("forceCaculate", true);//是否强制计算
							refoundDetailJSONObject.put("standWeight", refoundDetail.chongzhuang_weight);//钢瓶液化气重量
							refoundDetailJSONObject.put("dealPrice", refoundDetail.original_price);//该钢瓶上一次成交价格
						}else{
							refoundDetailJSONObject.put("forceCaculate", false);//是否强制计算
						}

					}else{//计算不成功，需要强制
						refoundDetailJSONObject.put("forceCaculate", true);//是否强制计算
						refoundDetailJSONObject.put("standWeight", refoundDetail.chongzhuang_weight);//钢瓶液化气重量
						refoundDetailJSONObject.put("dealPrice", refoundDetail.original_price);//该钢瓶上一次成交价格
					}

				}
				refoundDetailJSONArray.put(refoundDetailJSONObject);
			}
			body.put("jsonArray", refoundDetailJSONArray);
			nrc.setBody(body);
			nrc.isBodyJsonArray = true;
		}catch (JSONException e){
			Toast.makeText(TrayOrderDealActivity.this, "订单残气计算请求构建失败！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
			return;
		}
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag) {
					HttpResponse response = (HttpResponse) res;
					if (response != null) {
						if (response.getStatusLine().getStatusCode() == 200) {
							try {
								JSONObject calDetailsJson = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
								JSONArray calDetailsJSONArray = calDetailsJson.getJSONArray("items");
								for(int i=0; i<calDetailsJSONArray.length();i++){
									JSONObject calDetailJSONObject = calDetailsJSONArray.getJSONObject(i);
									boolean success = calDetailJSONObject.getBoolean("success");
									String gasCynNumber = calDetailJSONObject.getString("gasCynNumber");//钢瓶号
									RefoundDetail temp_RefoundDetail = m_BottlesMapKP.get(gasCynNumber);
									temp_RefoundDetail.note = calDetailJSONObject.getString("note");//备注

									if(success){//计算成功后获取详情
										JSONObject gasRefoundDetail = calDetailJSONObject.getJSONObject("gasRefoundDetail");
										String orderSn = gasRefoundDetail.getString("orderSn");//订单号
										String dealPrice = DoubleCast(gasRefoundDetail.getDouble("dealPrice"));//成交价格（优惠后）
										String prevOrder = gasRefoundDetail.getString("prevOrder");//关联订单号
										String prevGoodsCode = gasRefoundDetail.getString("prevGoodsCode");//关联商品编码
										temp_RefoundDetail.isOK = true;//是否异常
										temp_RefoundDetail.kp_weight = DoubleCast(gasRefoundDetail.getDouble("refoundWeight"));//空瓶称重
										temp_RefoundDetail.tare_weight = DoubleCast(gasRefoundDetail.getDouble("tareWeight"));//钢瓶皮重
										temp_RefoundDetail.canqi_weight = DoubleCast(gasRefoundDetail.getDouble("remainGas"));//残气重量
										temp_RefoundDetail.chongzhuang_weight = DoubleCast(gasRefoundDetail.getDouble("standWeight"));//充装重量
										temp_RefoundDetail.original_price = DoubleCast(gasRefoundDetail.getDouble("dealPrice"));//原购买价格
										temp_RefoundDetail.gas_price = DoubleCast(gasRefoundDetail.getDouble("unitPrice"));//气价元/公斤
										temp_RefoundDetail.canqi_price = DoubleCast(gasRefoundDetail.getDouble("refoundSum"));//残气金额
										temp_RefoundDetail.forceCaculate = gasRefoundDetail.getBoolean("forceCaculate");//是否通过强制计算
									}else{//计算不成功，将flag置为false
										temp_RefoundDetail.isOK = false;
									}
									m_BottlesMapKP.put(gasCynNumber, temp_RefoundDetail);
								}

								//TODO
								refleshBottlesMapKP();//刷新空瓶表
								refleshPayStatus();//刷新订单状态
							} catch (JSONException e) {
								Toast.makeText(TrayOrderDealActivity.this, "未知错误，异常！" + e.getMessage(),
										Toast.LENGTH_LONG).show();
							} catch (IOException e) {
								Toast.makeText(TrayOrderDealActivity.this, "未知错误，异常！" + e.getMessage(),
										Toast.LENGTH_LONG).show();
							}
						} else {
							refleshBottlesMapKP();//刷新空瓶表
							refleshPayStatus();//刷新订单状态
							Toast.makeText(TrayOrderDealActivity.this, getResponseMessage(response),
									Toast.LENGTH_LONG).show();
						}
					} else {
						Toast.makeText(TrayOrderDealActivity.this, "网络未连接！",
								Toast.LENGTH_LONG).show();
					}
				}}}, nrc);
	}


	//输入计算残气需要的参数，原购买金额及充装量
	private void getRefoundNeededParams(String bottleCode){
		final String bottleCodeTemp = bottleCode;
		LayoutInflater inflater = getLayoutInflater();
		final View layout = inflater.inflate(R.layout.upload_refound_params,
				null);
		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("请输入").setIcon(
				R.drawable.icon_app).setView(
				layout).setPositiveButton("确定",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						EditText editTextOriginalPrice = (EditText)layout.findViewById(R.id.input_originalPrice);
						EditText editTextChongZhuangWeight = (EditText)layout.findViewById(R.id.input_chongzhuangWeight);
						String OriginalPrice = editTextOriginalPrice.getText().toString();
						String ChongZhuangWeight = editTextChongZhuangWeight.getText().toString();
						if(OriginalPrice.length()==0||ChongZhuangWeight.length()==0){
							Toast.makeText(TrayOrderDealActivity.this, "输入有误，请重新输入！",
									Toast.LENGTH_LONG).show();
						}else{
							RefoundDetail stRefoundDetail = m_BottlesMapKP.get(bottleCodeTemp);
							stRefoundDetail.original_price = OriginalPrice;
							stRefoundDetail.chongzhuang_weight = ChongZhuangWeight;
							m_BottlesMapKP.put(bottleCodeTemp, stRefoundDetail);
							refleshBottlesMapKP();
						}
					}
				});
		builder.setCancelable(false);
		builder.show();
	}

	//是否可以进行支付
	private boolean isPayStatus(){
		boolean isisPayStatusOK = true;
		for (Map.Entry<String, RefoundDetail> entry : m_BottlesMapKP.entrySet()) {
			RefoundDetail stRefoundDetail = entry.getValue();
			if(!stRefoundDetail.isOK){
				isisPayStatusOK = false;
				break;
			}
		}
		return isisPayStatusOK;
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
			Toast.makeText(TrayOrderDealActivity.this, "未知错误，异常！" + e.getMessage(),
					Toast.LENGTH_LONG).show();
			return null;
		} catch (JSONException e) {
			Toast.makeText(TrayOrderDealActivity.this, "未知错误，异常！" + e.getMessage(),
					Toast.LENGTH_LONG).show();
			return null;
		}
	}

	//保留两位小数，并且四舍五入
	private String DoubleCast(Double data){
		NumberFormat nf = NumberFormat.getNumberInstance();
		// 保留两位小数
		nf.setMaximumFractionDigits(2);
		// 如果不需要四舍五入，可以使用RoundingMode.DOWN
		nf.setRoundingMode(RoundingMode.HALF_UP);
		return nf.format(data);
	}
}
