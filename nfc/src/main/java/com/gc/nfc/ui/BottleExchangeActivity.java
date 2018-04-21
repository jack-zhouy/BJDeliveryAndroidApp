package com.gc.nfc.ui;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.Settings;
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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BottleExchangeActivity extends BaseActivity implements OnClickListener  {
	private int m_takeOverCount = 0;//空瓶交接状态

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
	private String m_businessKey;//订单号


	private String m_curUserId;//该订单用户
	private JSONObject m_curUserSettlementType;//结算类型
	private User m_deliveryUser;//配送工
	private Map<String,JSONObject> m_userBottlesMap;//当前订单用户的钢瓶
	private Map<String,JSONObject> m_myBottlesMap;//当前配送工的钢瓶

	private List<String> m_BottlesListKP;//重瓶表
	private List<String> m_BottlesListZP;//空瓶表

	private int m_selected_nfc_model;//0--空瓶 1--重瓶

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
			setContentView(R.layout.activity_bottle_exchange);

			//获取传过来的任务订单参数
			Bundle bundle = new Bundle();
			bundle = this.getIntent().getExtras();
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

			m_imageViewZPEye.setOnClickListener(this);
			m_imageViewKPEye.setOnClickListener(this);

			m_buttonNext.setOnClickListener(this);
			radioGroup_nfc.setOnCheckedChangeListener(listen);
			radioGroup_nfc.check(radioButton_kp.getId());//默认是空瓶

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
			getUserBottles();//获取用户名下的钢瓶号
			getMyBottles();//获取配送工名下的钢瓶号


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

		}catch (JSONException e){
			Toast.makeText(BottleExchangeActivity.this, "未知错误，异常！"+e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}




	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button_next:
				bottleTakeOver();
				try {
					m_buttonNext.setText("正在提交...");
					Thread.currentThread().sleep(1000);//阻断2秒
					if(m_takeOverCount == (m_BottlesListKP.size()+m_BottlesListZP.size())){
						Intent intent = new Intent();
						//将传过来的任务订单参数传到下一个页面
						Bundle bundle = new Bundle();
						bundle = this.getIntent().getExtras();
						intent.setClass(BottleExchangeActivity.this, OrderDealActivity.class);
						intent.putExtras(bundle);
						startActivity(intent);
					}else{
						Toast.makeText(BottleExchangeActivity.this, "提交超时，请重新提交！",
								Toast.LENGTH_LONG).show();
					}
					m_buttonNext.setText("下一步");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

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

		AppContext appContext = (AppContext) getApplicationContext();
		User user = appContext.getUser();
		if (user == null) {
			Toast.makeText(BottleExchangeActivity.this, "请先登录!", Toast.LENGTH_LONG).show();
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

	//读标签
	public void onNewIntent(Intent intent) {
		Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		//2.获取Ndef的实例
		Ndef ndef = Ndef.get(detectedTag);
//		if (!haveMifareUltralight) {
//			Toast.makeText(this, "不支持MifareUltralight数据格式", Toast.LENGTH_SHORT).show();
//			return;
//		}
		String bottleCode = readNfcTag(intent);//NFC中的钢瓶编码


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
				Toast.makeText(BottleExchangeActivity.this, "空瓶录入：钢瓶号 "+bottleCode+"  非法！",
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
				}if(!contained){//第一次扫
					m_BottlesListZP.add(bottleCode);
					refleshBottlesListZP();
				}
			}else{//非法钢瓶
				Toast.makeText(BottleExchangeActivity.this, "重瓶录入：钢瓶号 "+bottleCode+"  非法！",
						Toast.LENGTH_LONG).show();
			}
		}
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
				BottleExchangeActivity.this,/*传入一个上下文作为参数*/
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
				BottleExchangeActivity.this,/*传入一个上下文作为参数*/
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

	/**
	 * 读取NFC标签文本数据
	 */
	private String readNfcTag(Intent intent) {
		String strText = null;
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
			Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
					NfcAdapter.EXTRA_NDEF_MESSAGES);
			NdefMessage msgs[] = null;
			int contentSize = 0;
			if (rawMsgs != null) {
				msgs = new NdefMessage[rawMsgs.length];
				for (int i = 0; i < rawMsgs.length; i++) {
					msgs[i] = (NdefMessage) rawMsgs[i];
					contentSize += msgs[i].toByteArray().length;
				}
			}
			try {
				if (msgs != null) {
					NdefRecord record = msgs[0].getRecords()[0];
					strText = parseTextRecord(record);
					return strText;
				}
			} catch (Exception e) {

			}
		}
		return null;
	}

	/**
	 * 解析NDEF文本数据，从第三个字节开始，后面的文本数据
	 *
	 * @param ndefRecord
	 * @return
	 */
	public static String parseTextRecord(NdefRecord ndefRecord) {
		/**
		 * 判断数据是否为NDEF格式
		 */
		//判断TNF
		if (ndefRecord.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
			return null;
		}
		//判断可变的长度的类型
		if (!Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
			return null;
		}
		try {
			//获得字节数组，然后进行分析
			byte[] payload = ndefRecord.getPayload();
			//下面开始NDEF文本数据第一个字节，状态字节
			//判断文本是基于UTF-8还是UTF-16的，取第一个字节"位与"上16进制的80，16进制的80也就是最高位是1，
			//其他位都是0，所以进行"位与"运算后就会保留最高位
			String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";
			//3f最高两位是0，第六位是1，所以进行"位与"运算后获得第六位
			int languageCodeLength = payload[0] & 0x3f;
			//下面开始NDEF文本数据第二个字节，语言编码
			//获得语言编码
			String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
			//下面开始NDEF文本数据后面的字节，解析出文本
			String textRecord = new String(payload, languageCodeLength + 1,
					payload.length - languageCodeLength - 1, textEncoding);
			return textRecord;
		} catch (Exception e) {
			throw new IllegalArgumentException();
		}
	}

	//钢瓶责任交接
	public void bottleTakeOver() {
		//交接空瓶
		for(int i=0; i<m_BottlesListKP.size(); i++){
			bottleTakeOverUnit(m_BottlesListKP.get(i), m_curUserId, m_deliveryUser.getUsername(), "6");//空瓶回收
		}
		//交接重瓶
		for(int i=0; i<m_BottlesListZP.size(); i++){
			bottleTakeOverUnit(m_BottlesListZP.get(i),  m_deliveryUser.getUsername(), m_curUserId,"5");//客户使用
		}
	}

	//单个钢瓶交接
	public void bottleTakeOverUnit(String bottleCode, String srcUserId, String targetUserId, String serviceStatus) {
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.PUT);
		nrc.requestUrl = NetUrlConstant.BOTTLETAKEOVERURL+"/"+bottleCode;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("srcUserId",srcUserId);//用户号
		params.put("targetUserId",targetUserId);//用户号
		params.put("serviceStatus",serviceStatus);//用户号
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==200){
							m_takeOverCount++;
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


}
