package com.gc.nfc.domain;

public class RefoundDetail {
	public String code;//钢瓶编号
	public String kp_weight;//空瓶称重
	public String tare_weight;//钢瓶皮重
	public String canqi_weight;//残气重量
	public String chongzhuang_weight;//充装重量
	public String original_price;//原购买价格
	public String gas_price;//气价元/公斤
	public String canqi_price;//残气金额
	public boolean isOK;//是否异常
	public boolean isFirst;//是否第一次计算
	public boolean forceCaculate;//是否通过强制计算
	public String note;//备注

}
