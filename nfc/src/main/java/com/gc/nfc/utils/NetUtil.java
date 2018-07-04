package com.gc.nfc.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import org.apache.http.client.CookieStore;

import com.gc.nfc.common.NetRequestConstant;
public class NetUtil {
	private static CookieStore m_tempCookies = null;//临时cookies
	private static CookieStore m_loginCookies = null;//登陆会话的cookies


	public static void setLoginCookies(){
		m_loginCookies = m_tempCookies;
	}
	//用get方式请求网络，返回响应的结果
	public static HttpResponse httpGet(NetRequestConstant nrc) {
		try {
			HttpParams httpParams=new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
			HttpConnectionParams.setSoTimeout(httpParams, 5000);

			//创建一个HttpClient实例
			DefaultHttpClient httpClient=new DefaultHttpClient(httpParams);

			String requestUrl = nrc.requestUrl;
			if (nrc.params != null && !nrc.params.isEmpty()) {
				List<NameValuePair> pairs = new ArrayList<NameValuePair>(nrc.params.size());
				for (String key : nrc.params.keySet()) {
					pairs.add(new BasicNameValuePair(key, nrc.params.get(key).toString()));
				}
				requestUrl += "?" + EntityUtils.toString(new UrlEncodedFormEntity(pairs, "UTF-8"));
			}
			//建立HttpGet对象
			HttpGet httpRequest=new HttpGet(requestUrl);
			//添加cookies
			if(m_loginCookies!=null){
				httpClient.setCookieStore(m_loginCookies);
			}

			HttpResponse httpResponse = httpClient.execute(httpRequest);

			m_tempCookies =httpClient.getCookieStore();
			return  httpResponse;
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	//post请求方式
	public static HttpResponse httpPost(NetRequestConstant nrc){
		String result=null;
		try {
			HttpParams httpParams=new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
			HttpConnectionParams.setSoTimeout(httpParams, 5000);

			//创建一个HttpClient实例
			DefaultHttpClient httpClient=new DefaultHttpClient(httpParams);
			//添加cookies
			if(m_loginCookies!=null){
				httpClient.setCookieStore(m_loginCookies);
			}
			//建立HttpPost对象
			String requestUrl = nrc.requestUrl;
			if (nrc.params != null && !nrc.params.isEmpty()) {
				List<NameValuePair> pairs = new ArrayList<NameValuePair>(nrc.params.size());
				for (String key : nrc.params.keySet()) {
					pairs.add(new BasicNameValuePair(key, nrc.params.get(key).toString()));
				}
				requestUrl += "?" + EntityUtils.toString(new UrlEncodedFormEntity(pairs, "UTF-8"));
			}
			HttpPost httpRequest=new HttpPost(requestUrl);
			httpRequest.setHeader("Content-Type", "application/json");

			//发送请求的参数
			Map<String, Object> body = nrc.body;
			JSONObject bodyJson = new JSONObject();  ;
			for(Map.Entry<String, Object> entry : body.entrySet()){
				bodyJson.put(entry.getKey(), entry.getValue());
			}

			StringEntity stringEntity = new StringEntity(bodyJson.toString());
			//stringEntity.setContentEncoding("UTF-8");
			stringEntity.setContentType("application/json");

			httpRequest.setEntity(stringEntity);

			//发送请求并等待响应
			HttpResponse httpResponse=httpClient.execute(httpRequest);

			Logger.e( "NetUtil Code ：" + nrc.requestUrl);
			return httpResponse;

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	//put请求方式
	public static HttpResponse httpPut(NetRequestConstant nrc){
		String result=null;
		try {

			HttpParams httpParams=new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
			HttpConnectionParams.setSoTimeout(httpParams, 5000);

			//创建一个HttpClient实例
			DefaultHttpClient httpClient=new DefaultHttpClient(httpParams);
			//添加cookies
			if(m_loginCookies!=null){
				httpClient.setCookieStore(m_loginCookies);
			}

			//建立HttpPost对象
			String requestUrl = nrc.requestUrl;
			if (nrc.params != null && !nrc.params.isEmpty()) {
				List<NameValuePair> pairs = new ArrayList<NameValuePair>(nrc.params.size());
				for (String key : nrc.params.keySet()) {
					pairs.add(new BasicNameValuePair(key, nrc.params.get(key).toString()));
				}
				requestUrl += "?" + EntityUtils.toString(new UrlEncodedFormEntity(pairs, "UTF-8"));
			}
			HttpPut httpRequest=new HttpPut(requestUrl);
			httpRequest.setHeader("Content-Type", "application/json");

			//发送请求的参数
			if(nrc.body!=null){
				Map<String, Object> body = nrc.body;
				JSONObject bodyJson = new JSONObject();  ;
				for(Map.Entry<String, Object> entry : body.entrySet()){
					bodyJson.put(entry.getKey(), (String) entry.getValue());
				}

				StringEntity stringEntity = new StringEntity(bodyJson.toString());
				stringEntity.setContentType("application/json");
				httpRequest.setEntity(stringEntity);
			}

			//stringEntity.setContentEncoding("UTF-8");

			//发送请求并等待响应
			HttpResponse httpResponse=httpClient.execute(httpRequest);

			Logger.e( "NetUtil Code ：" + nrc.requestUrl);
			return httpResponse;

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static boolean isCheckNet(Context context){
		ConnectivityManager cm=(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info=cm.getActiveNetworkInfo();
		if(info==null){
			//没有联网
			return false;
		}else{
			//联网类型
			//String type=info.getTypeName();
			return true;
		}
	}

}
