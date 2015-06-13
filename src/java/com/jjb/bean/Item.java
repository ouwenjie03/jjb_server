package com.jjb.bean;

import java.util.Date;

/**
 * 数据库中表Item的bean类
 * @author Robert Peng
 */
public class Item {
	
	private int itemId;
	private int userId;
	private String name;
	private double price;
	private boolean isOut;
	private int classify;
	private Date time;
	
	/**
	 * 返回该Item的JSON表示
	 * @return 该Item的JSON表示
	 */
	public String toString() {
		return new StringBuilder("{\"userId\":\"").append(userId)
				.append("\", \"name\": \"").append(name)
				.append("\", \"price\": \"").append(price)
				.append("\", \"isOut\": \"").append(isOut)
				.append("\", \"classify\": \"").append(classify)
				.append("\", \"time\": \"").append(time)
				.append("\"}").toString();
	}
	
	public int getItemId() {
		return itemId;
	}
	public void setItemId(int itemId) {
		this.itemId = itemId;
	}
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}
	public boolean getOut() {
		return isOut;
	}
	public void setOut(boolean isOut) {
		this.isOut = isOut;
	}
	public int getClassify() {
		return classify;
	}
	public void setClassify(int classify) {
		this.classify = classify;
	}
	public Date getTime() {
		return time;
	}
	public void setTime(Date time) {
		this.time = time;
	}
	
	
}
