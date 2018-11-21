package com.gc.nfc.common;

public class NetUrlConstant {

	//public static final String BASEURL = "http://www.yunnanbaijiang.com";//系统基地址
	public static final String BASEURL = "http://47.106.71.160";//系统基地址
	public static final String LOGINURL = BASEURL+"/api/sysusers/login";//登录接口
	public static final String LOGINOUTURL = BASEURL+"/api/sysusers/logout";//登出接口
	public static final String TASKORDERSURL = BASEURL+"/api/TaskOrders";//任务订单接口
	public static final String TASKORDERDEALURL = BASEURL+"/api/TaskOrders/Process";//任务订单处理接口
	public static final String ORDERURL = BASEURL+"/api/Orders";  //订单接口
	public static final String GASCYLINDERURL = BASEURL+"/api/GasCylinder";  //钢瓶接口

	public static final String PAYQRCODEURL = BASEURL+"/api/pay/scan";  //微信支付二维码接口

	public static final String TICKETURL = BASEURL+"/api/Ticket";  //气票查询接口
	public static final String COUPONURL = BASEURL+"/api/Coupon";  //优惠券查询接口

	public static final String TICKETPAYURL = BASEURL+"/api/TicketOrders";  //气票用户支付
	public static final String TGETDEPLEADERURL = BASEURL+"/api/sysusers/GetDepLeader";  //查询用户所属部门责任人

	public static final String POSITIONURL = BASEURL+"/api/sysusers/position";  //用户（派送员）经纬度位置信息变更

	public static final String BOTTLETAKEOVERURL = BASEURL+"/api/GasCylinder/TakeOver";  //钢瓶责任交接

	public static final String CUSTOMERCREDITURL = BASEURL+"/api/CustomerCredit";  //用户欠款查询

	public static final String QRCODEURL = BASEURL+"/api/pay/QRCode";  //二维码接口

	public static final String SYSUSERQUERYURL = BASEURL+"/api/sysusers/FindByUserId";  //系统用户信息查询

	public static final String UNINTERRUPTORDERSURL = BASEURL+"/api/UninterruptOrders";  //不间断供气订单信息查询
	public static final String UNINTERRUPTORDERSCACULATEURL = BASEURL+"/api/UninterruptOrders/Caculate";  //不间断定气订单价格计算
	public static final String UNINTERRUPTORDERSPAYURL = BASEURL+"/api/UninterruptOrders/Pay";  //7.10.	不间断定气订单支付

	public static final String GASCYNFACTORYURL = BASEURL+"/api/GasCynFactory";  //6.5.	钢瓶厂家查询
	public static final String SYSUSERKEEPALIVEURL = BASEURL+"/api/sysusers/KeepAlive";  //2.4.	心跳信息


	public static final String ORDERCACULATEURL = BASEURL+"/api/Orders/Caculate";  //订单残气计算
	public static final String USERCARDURL = BASEURL+"/api/UserCard";  //用户卡
	public static final String ElectDepositURL = BASEURL+"/api/ElectDeposit";  //电子押金单

	public static final String OrderBindGascynnumberURL = BASEURL+"/api/Orders/Bind/GasCynNumber";  //5.8.	订单关联钢瓶号

}
