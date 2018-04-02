package com.gc.nfc.common;

import java.util.Map;

import com.gc.nfc.ui.BaseActivity.HttpRequestType;

import android.content.Context;

public class NetRequestConstant {

	public static Context context;
	public static String requestUrl;
	public static Map<String, Object> body;
	public static Map<String, Object> params;

	private HttpRequestType type;

	public HttpRequestType getType() {
		return type;
	}

	public void setType(HttpRequestType type) {
		this.type = type;
	}
	
	public static void setBody(Map body){
		NetRequestConstant.body = body;
	}

	public static void setParams(Map params){
		NetRequestConstant.params = params;
	}

	

}
