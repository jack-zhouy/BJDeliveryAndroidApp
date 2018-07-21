package com.gc.nfc.ui;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.nfc.R;
import com.gc.nfc.app.AppContext;
import com.gc.nfc.common.NetRequestConstant;
import com.gc.nfc.common.NetUrlConstant;
import com.gc.nfc.domain.User;
import com.gc.nfc.interfaces.Netcallback;
import com.gc.nfc.utils.JellyInterpolator;
import com.gc.nfc.utils.NetUtil;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.gc.nfc.utils.SharedPreferencesHelper;


public class AutoLoginActivity extends BaseActivity{
	private View progress;
	private ObjectAnimator m_animator;

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
		setContentView(R.layout.activity_auto_login);
		progress = findViewById(R.id.layout_progress);

		SharedPreferencesHelper.initial(this);
		inputAnimator();
	}


	private void autoLogin(){
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);

		SharedPreferencesHelper.get("username", "default");
		final String username = (String)SharedPreferencesHelper.get("username", "default");
		final String password = (String)SharedPreferencesHelper.get("password", "default");
		nrc.requestUrl = NetUrlConstant.LOGINURL;
		nrc.context = this;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("userId", username);
		params.put("password", password);
		nrc.setParams(params);
		getServer(new Netcallback() {
			public void preccess(Object res, boolean flag) {
				if(flag){
					HttpResponse response=(HttpResponse)res;
					if(response!=null){
						if(response.getStatusLine().getStatusCode()==200){
							try {
								//设置登录会话的cookies
								NetUtil.setLoginCookies();
								JSONObject userJson = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
								JSONObject groupJson = userJson.getJSONObject("userGroup");
								JSONObject departmentJson =  userJson.getJSONObject("department");

								String groupCode = groupJson.optString("code");
								String groupName = groupJson.optString("name");
								String departmentCode = departmentJson.optString("code");
								String departmentName = departmentJson.optString("name");
								Intent data = new Intent();
								data.putExtra("userId", username);
								AppContext appContext = (AppContext) getApplicationContext();
								User user = new User();
								user.setUsername(username);
								user.setPassword(password);
								user.setDepartmentCode(departmentCode);
								user.setDepartmentName(departmentName);
								user.setGroupCode(groupCode);
								user.setGroupName(groupName);				appContext.setUser(user);
								Toast.makeText(AutoLoginActivity.this, "登陆成功！", Toast.LENGTH_LONG).show();
								setResult(12, data);
								MediaPlayer music = MediaPlayer.create(AutoLoginActivity.this, R.raw.start_working);
								music.start();
								if(groupCode.equals("00005")||groupCode.equals("00006")){
									Intent intent = new Intent(getApplicationContext() , StockManagerActivity.class);
									startActivity(intent);
									finish();
								}else if(groupCode.equals("00003")){
									Intent intent = new Intent(getApplicationContext() , MainlyActivity.class);
									startActivity(intent);
									finish();
								}else if(groupCode.equals("00007")){//调拨员
									Toast.makeText(AutoLoginActivity.this, "登陆成功！", Toast.LENGTH_LONG).show();
									Intent intent = new Intent(getApplicationContext() , DiaoBoActivity.class);
									startActivity(intent);
									finish();
								}else{
									Toast.makeText(AutoLoginActivity.this, "非有效账户，请更换！",
											Toast.LENGTH_LONG).show();
									Intent intent = new Intent(getApplicationContext() , LoginActivity.class);
									startActivity(intent);
									finish();
								}


							}catch (IOException e){
								Toast.makeText(AutoLoginActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
								Intent intent = new Intent(getApplicationContext() , LoginActivity.class);
								startActivity(intent);
								finish();
							}catch (JSONException e) {
								Toast.makeText(AutoLoginActivity.this, "未知错误，异常！",
										Toast.LENGTH_LONG).show();
								Intent intent = new Intent(getApplicationContext() , LoginActivity.class);
								startActivity(intent);
								finish();
							}

						}else{
							Toast.makeText(AutoLoginActivity.this, "账号或密码不正确", Toast.LENGTH_LONG).show();
							Intent intent = new Intent(getApplicationContext() , LoginActivity.class);
							startActivity(intent);
							finish();
						}
					}else {
						Toast.makeText(AutoLoginActivity.this, "未知错误，异常！",
								Toast.LENGTH_LONG).show();
						Intent intent = new Intent(getApplicationContext() , LoginActivity.class);
						startActivity(intent);
						finish();
					}
				} else {
					Toast.makeText(AutoLoginActivity.this, "网络未连接！",
							Toast.LENGTH_LONG).show();
					Intent intent = new Intent(getApplicationContext() , LoginActivity.class);
					startActivity(intent);
					finish();
				}
			}
		}, nrc);
	}


	private void inputAnimator() {

		progress.setVisibility(View.VISIBLE);
		progressAnimator(progress);

	}

	/**
	 * 出现进度动画
	 *
	 * @param view
	 */
	private void progressAnimator(final View view) {
		PropertyValuesHolder animator = PropertyValuesHolder.ofFloat("scaleX",
				0.5f, 1f);
		PropertyValuesHolder animator2 = PropertyValuesHolder.ofFloat("scaleY",
				0.5f, 1f);
		m_animator = ObjectAnimator.ofPropertyValuesHolder(view,
				animator, animator2);
		m_animator.setDuration(1000);
		m_animator.setInterpolator(new JellyInterpolator());
		m_animator.start();
		m_animator.addListener(new Animator.AnimatorListener() {

			@Override
			public void onAnimationStart(Animator animation) {

			}

			@Override
			public void onAnimationRepeat(Animator animation) {

			}

			@Override
			public void onAnimationEnd(Animator animation) {
				/**
				 * 动画结束后，先显示加载的动画，然后再隐藏输入框
				 */
				//开始登陆
				autoLogin();
				progress.setVisibility(View.INVISIBLE);

			}

			@Override
			public void onAnimationCancel(Animator animation) {

			}
		});


	}

}
