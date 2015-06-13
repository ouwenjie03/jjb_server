package com.jjb.util;

import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.jjb.bean.Item;

/**
 * JSON格式数据包的工具类
 * @author Robert Peng
 */
public class JSONResponse {

	public static String signInSuccess(int userId, String accessKey, Date expiresTime) {
		JSONObject result = new JSONObject();
		result.put("userId", userId);
		result.put("accessKey", accessKey);
		result.put("expiresTime", expiresTime.toString());
		return result.toString();
	}
	
	public static String signUpSuccess(int userId, String accessKey, Date expiresTime) {
		return signInSuccess(userId, accessKey, expiresTime);
	}
	
	public static String success() {
		JSONObject result = new JSONObject();
		result.put("failed", false);
		return result.toString();
	}
	
	public static String fail() {
		JSONObject result = new JSONObject();
		result.put("failed", true);
		return result.toString();
	}
	
	public static String syncData(List<Item> items) {
		JSONObject result = new JSONObject();
		JSONArray itemArr = new JSONArray();
		for (Item item : items) {
			itemArr.put(item.toString());
		}
		result.put("items", itemArr.toString());
		return result.toString();
	}
	
	public static String syncOK(int syncCount, Date modifiedDate) {
		JSONObject result = new JSONObject();
		result.put("syncCount", syncCount);
		result.put("modifiedDate", modifiedDate.toString());
		return result.toString();
	}
	
}
