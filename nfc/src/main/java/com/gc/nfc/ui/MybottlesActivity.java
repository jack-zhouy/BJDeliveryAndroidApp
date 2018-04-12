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


public class MybottlesActivity extends BaseActivity implements OnClickListener,AbsListView.OnScrollListener {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    private SwipeRefreshLayout swipeRefreshLayout;
    public static JSONArray m_bottlesListJson; //我的钢瓶列表

    private String m_userId;

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
        setContentView(R.layout.activity_my_bottles);
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
                refleshVaildBottles();
            }
        });

        Bundle bundle = new Bundle();
        bundle = this.getIntent().getExtras();
        m_userId = bundle.getString("userId");


        refleshVaildBottles();
    }


    private void switchActivity(int position) {
//TODO
    }
    public void onClick(View v) {
        return;
    }

    public void refleshVaildBottles() {
        // get请求
        NetRequestConstant nrc = new NetRequestConstant();
        nrc.setType(HttpRequestType.GET);

        nrc.requestUrl = NetUrlConstant.GASCYLINDERURL;
        nrc.context = this;
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("liableUserId",m_userId);//责任人是当前用户
        nrc.setParams(params);
        getServer(new Netcallback() {
            public void preccess(Object res, boolean flag) {
                if(flag){
                    HttpResponse response=(HttpResponse)res;
                    if(response!=null){
                        if(response.getStatusLine().getStatusCode()==200){
                            try {
                                List<Map<String,Object>> list_map = new ArrayList<Map<String,Object>>(); //定义一个适配器对象
                                JSONObject bottlesJson = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
                                JSONArray bottlesListJson = bottlesJson.getJSONArray("items");

                                MybottlesActivity.m_bottlesListJson = bottlesListJson;//获取钢瓶列表

                                for(int i=0;i<bottlesListJson.length(); i++){
                                    JSONObject bottleJson = bottlesListJson.getJSONObject(i);  // 遍历 jsonarray 数组，把每一个对象转成 json 对象
                                    Map<String,Object> bottleInfo = new HashMap<String, Object>(); //创建一个键值对的Map集合，用来存放名字和头像
                                    bottleInfo.put("number", "钢瓶编号："+bottleJson.get("number").toString());  //钢瓶编号
                                    JSONObject specJson = bottleJson.getJSONObject("spec");//钢瓶规格
                                    bottleInfo.put("spec", "钢瓶规格："+specJson.get("name").toString());  //钢瓶规格

                                    JSONObject serviceStatusJson = bottleJson.getJSONObject("serviceStatus");//钢瓶规格
                                    bottleInfo.put("serviceStatus", "钢瓶状态："+serviceStatusJson.get("name").toString());  //钢瓶状态
                                    list_map.add(bottleInfo);   //把这个存放好数据的Map集合放入到list中，这就完成类数据源的准备工作
                                }
                                //2、创建适配器（可以使用外部类的方式、内部类方式等均可）
                                SimpleAdapter simpleAdapter = new SimpleAdapter(
                                        MybottlesActivity.this,/*传入一个上下文作为参数*/
                                        list_map,         /*传入相对应的数据源，这个数据源不仅仅是数据而且还是和界面相耦合的混合体。*/
                                        R.layout.bottle_list_items, /*设置具体某个items的布局，需要是新的布局，而不是ListView控件的布局*/
                                        new String[]{"number", "spec", "serviceStatus"}, /*传入上面定义的键值对的键名称,会自动根据传入的键找到对应的值*/
                                        new int[]{R.id.items_number,R.id.items_spec, R.id.items_serviceStatus}) ;
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
                                Toast.makeText(MybottlesActivity.this, "未知错误，异常！",
                                        Toast.LENGTH_LONG).show();
                            }catch (JSONException e) {
                                Toast.makeText(MybottlesActivity.this, "未知错误，异常！",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }else {
                        Toast.makeText(MybottlesActivity.this, "未知错误，异常！",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MybottlesActivity.this, "网络未连接！",
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
