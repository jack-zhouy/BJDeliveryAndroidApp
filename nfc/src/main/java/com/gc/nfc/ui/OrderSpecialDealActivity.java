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
import android.widget.EditText;

public class OrderSpecialDealActivity extends BaseActivity implements OnClickListener,AbsListView.OnScrollListener  {


	private TextView m_textViewJifeiCode;//计费瓶信息
	private EditText m_editTextJifeiWeight;//计费瓶重量
	private TextView m_textViewGenghuanCode;//更换瓶信息
	private EditText m_editTextGenghuanWeight;//更换瓶重量

	private TextView m_textViewPayStatus;//支付状态
	private Button m_buttonNext;//下一步

	private Spinner m_spinnerPaytype; //支付类型
	private TextView m_textViewPaytype; //支付类型
	private TextView m_textViewTotalFee; //商品总价


	private SwipeRefreshLayout swipeRefreshLayout;

	private String m_depLeader;//配送工所属门店责任人
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



	private String m_strJifeiCode;//计费瓶信息
	private float m_fJifeiWeight;//计费瓶重量
	private String m_strGenghuanCode;//更换瓶信息
	private float m_fGenghuanWeight;//更换瓶重量
	private String m_jifeiOrderSn;//原计费订单



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
			setContentView(R.layout.activity_special_order_pay);
			//获取传过来的任务订单参数
			Bundle bundle = new Bundle();
			bundle = this.getIntent().getExtras();
			String  strOrder = bundle.getString("order");
			m_strJifeiCode = bundle.getString("kpCode");
			m_strGenghuanCode= bundle.getString("zpCode");
			m_OrderJson = new JSONObject(strOrder);
			m_taskId = bundle.getString("taskId");
			m_businessKey = bundle.getString("businessKey");

			//控件初始化
			m_buttonNext = (Button) findViewById(R.id.button_next);//下一步按钮
			m_textViewPayStatus = (TextView) findViewById(R.id.textview_payStatus);

			m_spinnerPaytype = (Spinner) findViewById(R.id.spinner_payType);
			m_textViewPaytype = (TextView) findViewById(R.id.textview_payType);
			m_textViewTotalFee = (TextView) findViewById(R.id.textview_totalFee);

			m_textViewJifeiCode = (TextView) findViewById(R.id.textview_jifeiBottleCode);//计费瓶信息
			m_editTextJifeiWeight = (EditText) findViewById(R.id.textview_jifeiBottleWeight);//计费瓶重量
			m_textViewGenghuanCode = (TextView) findViewById(R.id.textview_genghuanBottleCode);//更换瓶信息
			m_editTextGenghuanWeight = (EditText) findViewById(R.id.textview_genghuanBottleWeight);//更换瓶重量

			m_textViewGenghuanCode.setText(m_strGenghuanCode);
			m_textViewJifeiCode.setText(m_strJifeiCode);
			//获取订单号
			m_orderId = m_businessKey;

			swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.main_srl);
			swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light,
					android.R.color.holo_orange_light, android.R.color.holo_red_light);
			//下拉刷新
			swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
				@Override
				public void onRefresh() {
					//计费订单计算
					if((m_jifeiOrderSn!=null)){
						m_fJifeiWeight = Float.parseFloat(m_editTextJifeiWeight.getText().toString());
						caculateUninterruptOrders();
					}

				}
			});

			m_buttonNext.setOnClickListener(this);
			m_textViewPaytype.setVisibility(View.INVISIBLE);
			m_spinnerPaytype.setSelection(0,true);

			//获取当前配送工
			appContext = (AppContext) getApplicationContext();
			m_deliveryUser = appContext.getUser();

			//数据初始化
			setOrderHeadInfo();
			GetDepLeader();
			//查询计费订单
			if(m_strJifeiCode!=null){
				queryUninterruptOrders();
			}
		}catch (JSONException e){
			Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
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
			Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}





	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button_next:
			{
				if(m_depLeader==null){
					Toast.makeText(OrderSpecialDealActivity.this, "所属店长查询失败！请联系客服予以解决！",
							Toast.LENGTH_LONG).show();
					return;
				}else{
					Toast.makeText(OrderSpecialDealActivity.this, "正在提交，请稍等。。。",
							Toast.LENGTH_LONG).show();
					m_buttonNext.setText("正在提交...");
					m_buttonNext.setBackgroundColor(getResources().getColor(R.color.transparent_background));
					m_buttonNext.setEnabled(false);
					handlerDelayCommit.sendEmptyMessageDelayed(0,1000);
				}
			}
			break;
			default:
				break;
		}

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
			Toast.makeText(OrderSpecialDealActivity.this, "所属店长查询失败！",
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
							Toast.makeText(OrderSpecialDealActivity.this, "订单配送成功",
									Toast.LENGTH_LONG).show();
							MediaPlayer music = MediaPlayer.create(OrderSpecialDealActivity.this, R.raw.finish_order);
							music.start();
							Intent intent = new Intent(getApplicationContext() , MainlyActivity.class);
							Bundle bundle = new Bundle();

							bundle.putInt("switchTab", 1);//tab跳转到我的订单
							intent.putExtras(bundle);

							startActivity(intent);
							finish();
						} else{
							Toast.makeText(OrderSpecialDealActivity.this, "订单配送失败",
									Toast.LENGTH_LONG).show();
						}
					}else {
						Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(OrderSpecialDealActivity.this, "网络未连接！",
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
								Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！" + e.getMessage(),
										Toast.LENGTH_LONG).show();
							} catch (IOException e) {
								Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！" + e.getMessage(),
										Toast.LENGTH_LONG).show();
							}
						} else {
							Toast.makeText(OrderSpecialDealActivity.this, "订单配送失败",
									Toast.LENGTH_LONG).show();
						}
					} else {
						Toast.makeText(OrderSpecialDealActivity.this, "网络未连接！",
								Toast.LENGTH_LONG).show();
					}
				}}}, nrc);
	}

	//延时提交，防止乱点
	private Handler handlerDelayCommit = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if(m_depLeader==null){
				Toast.makeText(OrderSpecialDealActivity.this, "所属店长查询失败！",
						Toast.LENGTH_LONG).show();
				return;
			}
			if(m_totalFee!=null){
				//支付之前的不间断供气订单
				payUninterruptOrders();
			}

			//创建新的不间断供气订单
			createUninterruptOrders();



			m_buttonNext.setText("下一步");
			m_buttonNext.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
			m_buttonNext.setEnabled(true);
			super.handleMessage(msg);
		}
	};


	//计费订单查询
	private void queryUninterruptOrders() {

		if(m_strJifeiCode==null){
			return;
		}
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.UNINTERRUPTORDERSURL;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("gasCynNumber", m_strJifeiCode);
		params.put("payStatus", "0");//未支付
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				try {
					if(flag){
						HttpResponse response=(HttpResponse)res;
						if(response!=null){
							if(response.getStatusLine().getStatusCode()==200){
								//获取未支付订单
								JSONObject ordersJsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
								JSONArray jifeiOrdersListJson = ordersJsonObject.getJSONArray("items");
								if(jifeiOrdersListJson.length()!=1){
									Toast.makeText(OrderSpecialDealActivity.this, "错误未计费订单数量"+jifeiOrdersListJson.length(),
											Toast.LENGTH_LONG).show();
								}else{
									JSONObject jifeiOrder = jifeiOrdersListJson.getJSONObject(0);
									m_jifeiOrderSn = jifeiOrder.get("orderSn").toString();
									float fullWeight = Float.parseFloat(jifeiOrder.get("fullWeight").toString());
									//更新计费钢瓶信息
									m_textViewJifeiCode.setText(m_jifeiOrderSn+"（"+fullWeight+" kg)");
								}

							}else if(response.getStatusLine().getStatusCode()==404){
								Toast.makeText(OrderSpecialDealActivity.this, "订单不存在",
										Toast.LENGTH_LONG).show();
							} else{
								Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！"+response.getStatusLine().getStatusCode(),
										Toast.LENGTH_LONG).show();
							}
						}else {
							Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！",
									Toast.LENGTH_LONG).show();
						}
					} else {
						Toast.makeText(OrderSpecialDealActivity.this, "网络未连接！",
								Toast.LENGTH_LONG).show();
					}
				} catch (IOException e){
					Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！",
							Toast.LENGTH_LONG).show();
				}catch (JSONException e){
					Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！"+e.getMessage(),
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);
	}


	//计费订单创建
	private void createUninterruptOrders() {

		if(m_strGenghuanCode==null){
			return;
		}
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.POST);

		if(m_editTextGenghuanWeight.getText().toString().length()==0){
			Toast.makeText(OrderSpecialDealActivity.this, "请输入重瓶重量",
					Toast.LENGTH_LONG).show();
			return;
		}
		m_fGenghuanWeight = Float.parseFloat(m_editTextGenghuanWeight.getText().toString());

try {
	nrc.requestUrl = NetUrlConstant.UNINTERRUPTORDERSURL;
	nrc.context = this;
	Map<String, Object> params = new HashMap<String, Object>();
	Map<String, Object> body = new HashMap<String, Object>();
	JSONObject tempJsonObject = new JSONObject("{userId: "+m_curUserId+"}");
	body.put("customer", tempJsonObject);
	tempJsonObject = new JSONObject("{userId: "+m_deliveryUser.getUsername()+"}");
	body.put("dispatcher", tempJsonObject);
	tempJsonObject = new JSONObject("{number: "+m_strGenghuanCode+"}");
	body.put("gasCylinder", tempJsonObject);
	tempJsonObject = new JSONObject("{orderSn: "+m_orderId+"}");
	body.put("dispatchOrder", tempJsonObject);
	tempJsonObject = new JSONObject("{code: "+getGoodsCode()+"}");
	body.put("goods", tempJsonObject);
	body.put("fullWeight", m_fGenghuanWeight);

		nrc.setBody(body);
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {

				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==201) {
							Toast.makeText(OrderSpecialDealActivity.this, "提交更换瓶重量成功",
									Toast.LENGTH_LONG).show();
							//完成配送
							deliverOver();
						}
						else{
							Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！"+getResponseMessage(response),
									Toast.LENGTH_LONG).show();
						}
					}else {
						Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(OrderSpecialDealActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}

			}
		}, nrc);

}catch (JSONException e){

}
	}

	//设置商品详情
	private String getGoodsCode() {
		try {
			//获取传过来的任务订单参数
			JSONObject orderJson = m_OrderJson;
			JSONArray orderDetailList = orderJson.getJSONArray("orderDetailList");
			if (orderDetailList.length() >= 1) {
				JSONObject orderDetail = orderDetailList.getJSONObject(0);  // 订单详情单条记录
				JSONObject goodDetail = orderDetail.getJSONObject("goods");  // 商品详情
				String goodsCode = goodDetail.get("code").toString();
				return goodsCode;
			}
		} catch (JSONException e) {
			Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！",
					Toast.LENGTH_LONG).show();
			return null;
		}
		return null;
	}

	//计费价格计算
	private void caculateUninterruptOrders() {
		handler.sendEmptyMessage(0x101);//通过handler发送一个更新数据的标记
		if(m_jifeiOrderSn==null){
			Toast.makeText(OrderSpecialDealActivity.this, "计费订单不存在",
					Toast.LENGTH_LONG).show();
			return;
		}
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.UNINTERRUPTORDERSCACULATEURL+"/"+m_jifeiOrderSn;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("emptyWeight", m_fJifeiWeight);
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				try {
					if(flag){
						HttpResponse response=(HttpResponse)res;
						if(response!=null){
							if(response.getStatusLine().getStatusCode()==200){
								//获取未支付订单
								JSONObject ordersJsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));

									m_totalFee = ordersJsonObject.get("dealAmount").toString();
									m_textViewTotalFee.setText("￥"+m_totalFee);


//								if(jifeiOrdersListJson.length()!=1){
//									Toast.makeText(OrderSpecialDealActivity.this, "错误未计费订单数量"+jifeiOrdersListJson.length(),
//											Toast.LENGTH_LONG).show();
//								}else{
//									JSONObject jifeiOrder = jifeiOrdersListJson.getJSONObject(0);
//									m_jifeiOrderSn = jifeiOrder.get("orderSn").toString();
//								}

							}else if(response.getStatusLine().getStatusCode()==404){
								Toast.makeText(OrderSpecialDealActivity.this, "计费订单不存在",
										Toast.LENGTH_LONG).show();
							} else{
								Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！"+response.getStatusLine().getStatusCode(),
										Toast.LENGTH_LONG).show();
							}
						}else {
							Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！",
									Toast.LENGTH_LONG).show();
						}
					} else {
						Toast.makeText(OrderSpecialDealActivity.this, "网络未连接！",
								Toast.LENGTH_LONG).show();
					}
				} catch (IOException e){
					Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！",
							Toast.LENGTH_LONG).show();
				}catch (JSONException e){
					Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！"+e.getMessage(),
							Toast.LENGTH_LONG).show();
				}
			}
		}, nrc);

	}


	//计费订单支付
	private void payUninterruptOrders() {

		if(m_jifeiOrderSn==null){
			return;
		}
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		nrc.requestUrl = NetUrlConstant.UNINTERRUPTORDERSPAYURL+"/"+m_jifeiOrderSn;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();

		//如果结算类型是普通用户，就需要提交支付方式参数
		params.put("payType",1);//支付方式
		params.put("emptyWeight",m_fJifeiWeight);//空瓶时重量

		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {

				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==200){
							Toast.makeText(OrderSpecialDealActivity.this, "计费订单支付",
									Toast.LENGTH_LONG).show();

						}else if(response.getStatusLine().getStatusCode()==404){
							Toast.makeText(OrderSpecialDealActivity.this, "计费订单不存在",
									Toast.LENGTH_LONG).show();
						} else{
							Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！"+response.getStatusLine().getStatusCode(),
									Toast.LENGTH_LONG).show();
						}
					}else {
						Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(OrderSpecialDealActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
				}

			}
		}, nrc);
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
			Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！" + e.getMessage(),
					Toast.LENGTH_LONG).show();
			return null;
		} catch (JSONException e) {
			Toast.makeText(OrderSpecialDealActivity.this, "未知错误，异常！" + e.getMessage(),
					Toast.LENGTH_LONG).show();
			return null;
		}
	}

}
