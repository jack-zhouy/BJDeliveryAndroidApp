package com.gc.nfc.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HistoryOrdersActivity extends BaseActivity implements OnClickListener,AbsListView.OnScrollListener {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    private SwipeRefreshLayout swipeRefreshLayout;
    public static JSONArray m_taskOrderListJson; //任务订单列表=====注意 ：这里的任务订单就是订单，两个接口不一样

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

    void init() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 取消标题栏
        setContentView(R.layout.activity_history_orders);
        ListView listView = (ListView) findViewById(R.id.listview);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switchActivity(position);
            }
        });

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.main_srl);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light,
                android.R.color.holo_orange_light, android.R.color.holo_red_light);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refleshVaildOrders();
            }
        });
        refleshVaildOrders();
    }


    private void switchActivity(int position) {
        try {
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            JSONObject taskOrderJson = m_taskOrderListJson.getJSONObject(position);
            JSONObject orderJson = taskOrderJson;//任务订单就是订单，没有object
            bundle.putString("order", orderJson.toString());
            bundle.putInt("orderStatus", -1);

            intent.setClass(HistoryOrdersActivity.this, OrderDetailActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
        }catch (JSONException e){
            Toast.makeText(HistoryOrdersActivity.this, "未知错误，异常！"+e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
    public void onClick(View v) {
        return;
    }

    public void refleshVaildOrders() {
        AppContext appContext = (AppContext) getApplicationContext();
        User user = appContext.getUser();
        if (user == null) {
            Toast.makeText(HistoryOrdersActivity.this, "登陆会话失效", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(HistoryOrdersActivity.this, AutoLoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        // get请求
        NetRequestConstant nrc = new NetRequestConstant();
        nrc.setType(HttpRequestType.GET);

        nrc.requestUrl = NetUrlConstant.ORDERURL;
        nrc.context = this;
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("dispatcherId",user.getUsername() );//待派送
        params.put("orderBy","id desc" );//按照时间逆序排列
        params.put("pageNo","1" );//按照时间逆序排列
        params.put("pageSize","10" );//按照时间逆序排列
        nrc.setParams(params);
        getServer(new Netcallback() {
            public void preccess(Object res, boolean flag) {
                if(flag){
                    HttpResponse response=(HttpResponse)res;
                    if(response!=null){
                        if(response.getStatusLine().getStatusCode()==200){
                            try {
                                List<Map<String,Object>> list_map = new ArrayList<Map<String,Object>>(); //定义一个适配器对象
                                JSONObject taskOrdersJson = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
                                JSONArray taskOrdersListJson = taskOrdersJson.getJSONArray("items");

                                HistoryOrdersActivity.m_taskOrderListJson = taskOrdersListJson;//获取任务订单列表

                                for(int i=0;i<taskOrdersListJson.length(); i++){
                                    JSONObject taskorder = taskOrdersListJson.getJSONObject(i);  // 遍历 jsonarray 数组，把每一个对象转成 json 对象
                                    JSONObject order = taskorder;  // 遍历 jsonarray 数组，把每一个对象转成 json 对象
                                    Map<String,Object> orderInfo = new HashMap<String, Object>(); //创建一个键值对的Map集合，用来存放名字和头像
                                    orderInfo.put("orderSn", "订单编号："+order.get("orderSn").toString());  //订单编号
                                    orderInfo.put("createTime", "下单时间："+order.get("createTime").toString());  //创建时间
                                    JSONObject addressJson = order.getJSONObject("recvAddr");
                                    orderInfo.put("address", addressJson.get("city").toString()+addressJson.get("county").toString()+addressJson.get("detail").toString());  //收货地址
                                    orderInfo.put("userId", "联系人："+order.get("recvName"));  //用户信息
                                    orderInfo.put("userPhone", "电话："+order.get("recvPhone").toString());  //用户信息
                                    String orderStatusDesc[] = {"待接单","派送中","待核单","已结束","已作废"};
                                    orderInfo.put("orderStatus", orderStatusDesc[Integer.parseInt(order.get("orderStatus").toString())]);

                                    //获取结算类型
                                    JSONObject customerJson = order.getJSONObject("customer");
                                    JSONObject curUserSettlementType = customerJson.getJSONObject("settlementType");
                                    if(curUserSettlementType.get("code").toString().equals("00003")) {//气票
                                        orderInfo.put("userIcon", R.drawable.icon_ticket_user);
                                    }else if(curUserSettlementType.get("code").toString().equals("00002")) {//月结
                                        orderInfo.put("userIcon", R.drawable.icon_month_user);
                                    } else{
                                        orderInfo.put("userIcon", R.drawable.icon_common_user);
                                    }

                                    boolean urgent = order.getBoolean("urgent");
                                    if(urgent){
                                        orderInfo.put("urgent", "加急");
                                    }else{
                                        orderInfo.put("urgent", "");
                                    }


                                    list_map.add(orderInfo);   //把这个存放好数据的Map集合放入到list中，这就完成类数据源的准备工作
                                }



                                //2、创建适配器（可以使用外部类的方式、内部类方式等均可）
                                SimpleAdapter simpleAdapter = new SimpleAdapter(
                                        HistoryOrdersActivity.this,/*传入一个上下文作为参数*/
                                        list_map,         /*传入相对应的数据源，这个数据源不仅仅是数据而且还是和界面相耦合的混合体。*/
                                        R.layout.order_list_items, /*设置具体某个items的布局，需要是新的布局，而不是ListView控件的布局*/
                                        new String[]{"orderSn", "createTime",  "userId",  "userPhone", "userIcon", "address","orderStatus","urgent"}, /*传入上面定义的键值对的键名称,会自动根据传入的键找到对应的值*/
                                        new int[]{R.id.items_orderSn,R.id.items_creatTime,R.id.items_userId,R.id.items_userPhone, R.id.items_imageUserIcon,  R.id.items_address, R.id.items_orderStatus,
                                                R.id.items_urgent}) ;
//                                {
//                                    @Override
//                                    public View getView(int position, View convertView, ViewGroup parent) {
//                                        View v = super.getView(position,convertView,parent);
//                                        Button button = null;
//                                        if (convertView == null) {
//                                            convertView = View.inflate(ValidOrdersActivity.this, R.layout.order_list_items, null);
//                                            button = (Button) v.findViewById(R.id.button_get);
//                                            convertView.setTag(button);
//                                        } else {
//                                            button = (Button) v.getTag();
//                                        }
//                                        button.setOnClickListener(new View.OnClickListener() {
//                                            @Override
//                                            public void onClick(View view) {
//                                                Toast.makeText(ValidOrdersActivity.this, "listview的内部的按钮被点击了！，位置是-->" + (Integer) view.getTag(), Toast.LENGTH_SHORT).show();
//                                            }
//                                        });
//                                        button.setTag(position);
//                                        return convertView;
//                                    }
//                                };/*传入items布局文件中需要指定传入的控件，这里直接上id即可*/
                                ListView listView = (ListView) findViewById(R.id.listview);
                                //3、为listView加入适配器
                                listView.setAdapter(simpleAdapter);
                            }catch (IOException e){
                                Toast.makeText(HistoryOrdersActivity.this, "未知错误，异常！",
                                        Toast.LENGTH_LONG).show();
                            }catch (JSONException e) {
                                Toast.makeText(HistoryOrdersActivity.this, "未知错误，异常！",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }else {
                        Toast.makeText(HistoryOrdersActivity.this, "未知错误，异常！",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(HistoryOrdersActivity.this, "网络未连接！",
                            Toast.LENGTH_LONG).show();
                }
            }
        }, nrc);
        handler.sendEmptyMessage(0x101);//通过handler发送一个更新数据的标记
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
