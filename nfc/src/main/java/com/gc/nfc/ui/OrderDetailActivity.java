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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.ListAdapter;
import android.view.ViewGroup;
import android.view.Gravity;

public class OrderDetailActivity extends BaseActivity implements OnClickListener {

	private TextView m_textViewOrderSn;//订单号
	private TextView m_textViewUserInfo;//用户信息
	private TextView m_textViewCreateTime;//创建时间
	private TextView m_textViewAddress;//地址
	private ListView m_listView;// 商品详情

	private TextView m_textViewPayStatus;//支付状态
	private TextView m_textViewOrderStatus;//订单状态
	private TextView m_textViewReserveTime;//预约时间
	private TextView m_textViewPs;//备注


	private Button m_buttonNext;//下一步

	private AppContext appContext;


	private JSONObject m_OrderJson;//订单详情
	private String m_taskId;//任务订单详情
	private int m_orderStatus;//订单状态
	private User m_user;//当前登录用户

	private String m_businessKey;//订单号

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}
	@Override
	void init() {
		try {
			setContentView(R.layout.activity_order_detail);

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
			m_orderStatus = bundle.getInt("orderStatus");

			//控件初始化
			m_buttonNext = (Button) findViewById(R.id.button_next);//下一步按钮
			m_textViewOrderSn = (TextView) findViewById(R.id.textview_orderSn);
			m_textViewUserInfo = (TextView) findViewById(R.id.textview_userInfo);
			m_textViewCreateTime = (TextView) findViewById(R.id.textview_createTime);
			m_textViewAddress = (TextView) findViewById(R.id.textview_address);
			m_listView = (ListView) findViewById(R.id.listview);

			m_textViewPayStatus = (TextView) findViewById(R.id.textview_payStatus);
			m_textViewOrderStatus = (TextView) findViewById(R.id.textview_orderStatus);
			m_textViewReserveTime = (TextView) findViewById(R.id.textview_reserveTime);
			m_textViewPs = (TextView) findViewById(R.id.textview_ps);

			m_buttonNext.setOnClickListener(this);


			//数据初始化
			setOrderHeadInfo();
			setOrderDetailsInfo();
			setOrderAppendInfo();

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
			String strOrderSn = "订单号："+orderJson.get("orderSn").toString();
			m_textViewOrderSn.setText(strOrderSn);

			String strUserInfo = "用户："+orderJson.get("recvName").toString()+"  |  电话："+orderJson.get("recvPhone").toString();
			m_textViewUserInfo.setText(strUserInfo);

			String strCreateTime = "创建时间："+orderJson.get("createTime").toString();
			m_textViewCreateTime.setText(strCreateTime);

			JSONObject addressJson = orderJson.getJSONObject("recvAddr");
			String strAddress = "地址："+addressJson.get("city").toString()+addressJson.get("county").toString()+addressJson.get("detail").toString();
			m_textViewAddress.setText(strAddress);
		}catch (JSONException e){
			Toast.makeText(OrderDetailActivity.this, "未知错误，异常！"+e.getMessage(),
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
					OrderDetailActivity.this,/*传入一个上下文作为参数*/
					list_map,         /*传入相对应的数据源，这个数据源不仅仅是数据而且还是和界面相耦合的混合体。*/
					R.layout.order_detail_items, /*设置具体某个items的布局，需要是新的布局，而不是ListView控件的布局*/
					new String[]{"goodName", "goodQuantity", "dealPrice"}, /*传入上面定义的键值对的键名称,会自动根据传入的键找到对应的值*/
					new int[]{R.id.items_goodName, R.id.items_goodQuantity, R.id.items_dealPrice});
			//3、为listView加入适配器
			m_listView.setAdapter(simpleAdapter);
			setListViewHeightBasedOnChildren(m_listView);
		}catch (JSONException e){
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
			m_textViewReserveTime.setText(strReserveTime);

			//备注
			String strComment = orderJson.get("comment").toString();
			m_textViewPs.setText(strComment);

		}catch (JSONException e){
			Toast.makeText(OrderDetailActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}


	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button_next:
				if(m_user==null){
					Toast.makeText(OrderDetailActivity.this, "未登录！", Toast.LENGTH_LONG).show();
					return;
				}
				// get请求
				NetRequestConstant nrc = new NetRequestConstant();
				nrc.setType(HttpRequestType.GET);
				NetRequestConstant.requestUrl = NetUrlConstant.TASKORDERDEALURL+"/"+m_taskId;
				NetRequestConstant.context = this;
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("businessKey", m_businessKey);
				params.put("candiUser", m_user.getUsername());
				params.put("orderStatus", 1);
				NetRequestConstant.setParams(params);

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

				break;
			default:
				break;
		}

	}

}
