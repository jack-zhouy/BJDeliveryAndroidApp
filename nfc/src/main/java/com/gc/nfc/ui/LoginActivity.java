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
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.gc.nfc.common.NetRequestConstant;
import com.gc.nfc.interfaces.Netcallback;
import com.gc.nfc.ui.MainlyActivity;
import com.gc.nfc.utils.NetUtil;

import android.content.SharedPreferences;
import android.os.StrictMode;
import android.media.MediaPlayer;
import com.gc.nfc.utils.JellyInterpolator;
import android.animation.*;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.animation.Animator.AnimatorListener;

import com.gc.nfc.utils.SharedPreferencesHelper;


public class LoginActivity  extends BaseActivity implements OnClickListener {

	private TextView mBtnLogin;

	private View progress;

	private View mInputLayout;

	private float mWidth, mHeight;

	private LinearLayout mName, mPsw;

	private EditText m_userIdEditText, m_passwordEditText;

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
		setContentView(R.layout.activity_login);

		mBtnLogin = (TextView) findViewById(R.id.main_btn_login);
		progress = findViewById(R.id.layout_progress);
		mInputLayout = findViewById(R.id.input_layout);
		mName = (LinearLayout) findViewById(R.id.input_layout_name);
		mPsw = (LinearLayout) findViewById(R.id.input_layout_psw);
		m_userIdEditText = (EditText) findViewById(R.id.input_userId);
		m_passwordEditText = (EditText) findViewById(R.id.input_password);

		mBtnLogin.setOnClickListener(this);
	}
	private void inputAnimator(final View view, float w, float h) {

		progress.setVisibility(View.VISIBLE);
		progressAnimator(progress);
		mInputLayout.setVisibility(View.INVISIBLE);

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
		m_animator.addListener(new AnimatorListener() {

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
				login();
				progress.setVisibility(View.INVISIBLE);
				mInputLayout.setVisibility(View.VISIBLE);

			}

			@Override
			public void onAnimationCancel(Animator animation) {

			}
		});


	}


	public void onClick(View v) {
		// 计算出控件的高与宽
		mWidth = mBtnLogin.getMeasuredWidth();
		mHeight = mBtnLogin.getMeasuredHeight();
		// 隐藏输入框
		//mName.setVisibility(View.INVISIBLE);
		//mPsw.setVisibility(View.INVISIBLE);

		inputAnimator(mInputLayout, mWidth, mHeight);


	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		if(data!=null){
//			String username = data.getStringExtra("username");
//			String password = data.getStringExtra("password");
//			edittext_username.setText(username);
//			edittext_password.setText(password);
//		}
	}

	private void login(){
		// get请求
		NetRequestConstant nrc = new NetRequestConstant();
		nrc.setType(HttpRequestType.GET);
		final String username = m_userIdEditText.getText().toString();
		final String password = m_passwordEditText.getText().toString();
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
								user.setGroupName(groupName);
								appContext.setUser(user);
								setResult(12, data);
								SharedPreferencesHelper.put("username", username);
								SharedPreferencesHelper.put("password", password);

								MediaPlayer music = MediaPlayer.create(LoginActivity.this, R.raw.start_working);
								music.start();
								if(groupCode.equals("00005")||groupCode.equals("00006")){
									Toast.makeText(LoginActivity.this, "登陆成功！", Toast.LENGTH_LONG).show();
									Intent intent = new Intent(getApplicationContext() , StockManagerActivity.class);
									startActivity(intent);
									finish();
								}else if(groupCode.equals("00003")){
									Toast.makeText(LoginActivity.this, "登陆成功！", Toast.LENGTH_LONG).show();
									Intent intent = new Intent(getApplicationContext() , MainlyActivity.class);
									startActivity(intent);
									finish();
								}else if(groupCode.equals("00007")){//调拨员
									Toast.makeText(LoginActivity.this, "登陆成功！", Toast.LENGTH_LONG).show();
									Intent intent = new Intent(getApplicationContext() , DiaoBoActivity.class);
									startActivity(intent);
									finish();
								}
								else{
									Toast.makeText(LoginActivity.this, "非有效账户，请更换！",
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
	}

}
