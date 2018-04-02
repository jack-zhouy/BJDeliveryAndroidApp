package com.gc.nfc.ui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.gc.nfc.MainActivity;
import com.gc.nfc.R;
import com.gc.nfc.app.AppContext;
import com.gc.nfc.common.NetUrlConstant;
import com.gc.nfc.domain.User;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.gc.nfc.common.NetRequestConstant;
import com.gc.nfc.interfaces.Netcallback;
import com.gc.nfc.ui.MainlyActivity;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.media.MediaPlayer;


public class LoginActivity  extends BaseActivity implements OnClickListener {

	private ImageView imageview_back;
	private EditText edittext_username, edittext_password;
	private Button login;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
	}

	void init() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);// 取消标题栏
		AppContext appContext = (AppContext) getApplicationContext();
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		appContext.setPreferences(preferences);

		setContentView(R.layout.activity_login);

		imageview_back = (ImageView) findViewById(R.id.imageview_back);
		login = (Button) findViewById(R.id.button_login);
		edittext_username = (EditText) findViewById(R.id.login_userName);
		edittext_password = (EditText) findViewById(R.id.login_userPassword);
		imageview_back.setOnClickListener(this);
		login.setOnClickListener(this);

	}

	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.imageview_back:
				finish();
				break;
			case R.id.button_login:
				// get请求
				NetRequestConstant nrc = new NetRequestConstant();
				nrc.setType(HttpRequestType.GET);
				final String username = edittext_username.getText().toString();
				final String password = edittext_password.getText().toString();
				NetRequestConstant.requestUrl = NetUrlConstant.LOGINURL;
				NetRequestConstant.context = this;
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("userId", username);
				params.put("password", password);
				NetRequestConstant.setParams(params);
				getServer(new Netcallback() {
					public void preccess(Object res, boolean flag) {
						if(flag){
							HttpResponse response=(HttpResponse)res;
							if(response!=null){
								if(response.getStatusLine().getStatusCode()==200){
									try {
										JSONObject userJson = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
										JSONObject groupJson = userJson.getJSONObject("userGroup");
										String groupCode = groupJson.optString("code");
										if(groupCode.equals("00003")){
											Intent data = new Intent();
											data.putExtra("userId", username);
											AppContext appContext = (AppContext) getApplicationContext();
											User user = new User();
											user.setUsername(username);
											user.setPassword(password);
											appContext.setUser(user);
											Toast.makeText(LoginActivity.this, "登陆成功！", Toast.LENGTH_LONG).show();
											setResult(12, data);

											MediaPlayer music = MediaPlayer.create(LoginActivity.this, R.raw.start_working);
											music.start();
											Intent intent = new Intent(getApplicationContext() , MainlyActivity.class);

											startActivity(intent);
											finish();

										}else{
											Toast.makeText(LoginActivity.this, "非配送账户，请更换！",
													Toast.LENGTH_LONG).show();
										}

									}catch (IOException e){
										Toast.makeText(LoginActivity.this, "未知错误，异常！",
												Toast.LENGTH_LONG).show();
									}catch (JSONException e) {
										Toast.makeText(LoginActivity.this, "未知错误，异常！",
												Toast.LENGTH_LONG).show();
									}

								}else{
									Toast.makeText(LoginActivity.this, "账号或密码不正确", Toast.LENGTH_LONG).show();
								}
							}else {
								Toast.makeText(LoginActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
							}
						} else {
							Toast.makeText(LoginActivity.this, "网络未连接！",
									Toast.LENGTH_LONG).show();
						}
					}
				}, nrc);

				break;
			default:
				break;
		}

	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(data!=null){
			String username = data.getStringExtra("username");
			String password = data.getStringExtra("password");
			edittext_username.setText(username);
			edittext_password.setText(password);

		}
	}

}
