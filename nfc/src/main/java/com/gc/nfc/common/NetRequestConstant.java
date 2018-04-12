package com.gc.nfc.common;

import java.util.Map;

import com.gc.nfc.ui.BaseActivity.HttpRequestType;

import android.content.Context;

public class NetRequestConstant {

	public  Context context;
	public  String requestUrl;
	public  Map<String, Object> body;
	public  Map<String, Object> params;

	private HttpRequestType type;

	public HttpRequestType getType() {
		return type;
	}

	public void setType(HttpRequestType type) {
		this.type = type;
	}
	
	public  void setBody(Map body){
		this.body = body;
	}

	public  void setParams(Map params){
		this.params = params;
	}

	

}
