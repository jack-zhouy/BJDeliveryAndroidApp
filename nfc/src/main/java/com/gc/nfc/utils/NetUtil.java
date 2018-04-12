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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.gc.nfc.common.NetRequestConstant;
public class NetUtil {
	
	    //用get方式请求网络，返回响应的结果
		public static HttpResponse httpGet(NetRequestConstant nrc) {
			try {
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
				//创建HttpParams对象，用来设置HTTP参数
				HttpParams httpParams=new BasicHttpParams();
				//创建一个HttpClient实例
				HttpClient httpClient=new DefaultHttpClient(httpParams);
				//发送请求并等待
				HttpResponse httpResponse = httpClient.execute(httpRequest);
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


				//创建HttpParams对象，用来设置HTTP参数
				HttpParams httpParams=new BasicHttpParams();
				HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
				HttpConnectionParams.setSoTimeout(httpParams, 5000);

				//创建一个HttpClient实例
				HttpClient httpClient=new DefaultHttpClient(httpParams);

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

				//发送请求的参数
	            Map<String, Object> body = nrc.body;

				List<NameValuePair> parameters = new ArrayList<NameValuePair>();

				for(Map.Entry<String, Object> entry : body.entrySet()){
					NameValuePair pair = new BasicNameValuePair(entry.getKey(), (String) entry.getValue());
					parameters.add(pair);
				}

				//添加请求参数到请求对象
				httpRequest.setEntity(new UrlEncodedFormEntity(parameters, HTTP.UTF_8));

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
