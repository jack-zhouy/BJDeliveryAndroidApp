package com.gc.nfc.common;

public class NetUrlConstant {
	public static final String LOGINURL = "http://120.78.241.67/api/sysusers/login";//登录接口
	public static final String TASKORDERSURL = "http://120.78.241.67/api/TaskOrders";//任务订单接口
	public static final String TASKORDERDEALURL = "http://120.78.241.67/api/TaskOrders/Process";//任务订单处理接口
	public static final String ORDERURL = "http://120.78.241.67/api/Orders";  //订单接口
	public static final String GASCYLINDERURL = "http://120.78.241.67/api/GasCylinder";  //钢瓶接口

	public static final String PAYQRCODEURL = "http://120.78.241.67/api/pay/scan";  //微信支付二维码接口

	public static final String TICKETURL = "http://120.78.241.67//api/Ticket";  //气票查询接口
	public static final String COUPONURL = "http://120.78.241.67//api/Coupon";  //优惠券查询接口

	public static final String TICKETPAYURL = "http://120.78.241.67/api/TicketOrders";  //气票用户支付
	public static final String TGETDEPLEADERURL = "http://120.78.241.67/api/sysusers/GetDepLeader";  //查询用户所属部门责任人

	public static final String POSITIONURL = "http://120.78.241.67/api/sysusers/position";  //用户（派送员）经纬度位置信息变更

	public static final String BOTTLETAKEOVERURL = "http://120.78.241.67/api/GasCylinder/TakeOver";  //钢瓶责任交接

}
