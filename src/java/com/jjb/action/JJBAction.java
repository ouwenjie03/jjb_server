package com.jjb.action;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.orm.hibernate4.support.HibernateDaoSupport;

import com.jjb.bean.AccessKey;
import com.jjb.bean.Item;
import com.jjb.bean.Money;
import com.jjb.bean.User;
import com.jjb.dao.AccessKeyDao;
import com.jjb.dao.ItemDao;
import com.jjb.dao.MoneyDao;
import com.jjb.dao.UserDao;
import com.jjb.util.JSONResponse;

/**
 * JJB Jersey Servlet类
 * @author Robert Peng
 */
@Path("/")
@Produces("application/json")
public class JJBAction extends HibernateDaoSupport {
	private Session session;
	
	private ItemDao itemDao;
	private UserDao userDao;
	private MoneyDao moneyDao;
	private AccessKeyDao accessKeyDao;
	
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	
	/**
	 * 用户登录, 有效期一个月
	 * @param username 用户名
	 * @param password hash以后的32位密码
	 * @return
	 * 		登录成功则返回JSONResponse.signInSuccess，携带userId、accessKey与expiresTime
	 * 		登录失败则返回JSONResponse.fail
	 */
	@POST
	@Path("signIn")
	@Consumes("application/x-www-form-urlencoded")
	public String userSignIn(
			@FormParam("username") @DefaultValue("") String username,
			@FormParam("password") @DefaultValue("") String password) {
		if (username.isEmpty() || password.isEmpty() || !password.matches("[0-9a-f]{32}"))
			return JSONResponse.fail();
		User user = userDao.queryUser(username);
		if (user.getPassword().equals(password)) {
			// 有效期一个月
			Date expiresTime = new Date();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(expiresTime);
			calendar.add(Calendar.MONTH, 1);
			expiresTime = calendar.getTime();

			// 保存accessKey
			String accessKeyStr = UUID.randomUUID().toString().replace("-", "");
			AccessKey accessKey = new AccessKey();
			accessKey.setAccessKey(accessKeyStr);
			accessKey.setUserId(user.getUserId());
			accessKey.setExpiresTime(expiresTime);
			accessKeyDao.setAccessKey(accessKey);

			return JSONResponse.signInSuccess(user.getUserId(), accessKeyStr,
					expiresTime);
		} else {
			return JSONResponse.fail();
		}
	}
	
	/**
	 * 用户注册
	 * @param username 用户名
	 * @param password hash以后的32位密码
	 * @return
	 * 		注册成功则返回JSONResponse.signUpSuccess，携带userId、accessKey与expiresTime
	 * 		注册失败则返回JSONResponse.fail
	 */
	@POST
	@Path("signUp")
	@Consumes("application/x-www-form-urlencoded")
	public String userSignUp(
			@FormParam("username") @DefaultValue("") String username,
			@FormParam("password") @DefaultValue("") String password) {
		if (username.isEmpty() || password.isEmpty() || !password.matches("[0-9a-f]{32}"))
			return JSONResponse.fail();
		User user = userDao.queryUser(username);
		if (user != null) {
			// 用户存在
			return JSONResponse.fail();
		} else {
			user = new User();
			user.setUsername(username);
			user.setPassword(password);
			userDao.insertUser(user);
			user = userDao.queryUser(username);
			
			// 有效期一个月
			Date expiresTime = new Date();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(expiresTime);
			calendar.add(Calendar.MONTH, 1);
			expiresTime = calendar.getTime();

			// 保存accessKey
			String accessKeyStr = UUID.randomUUID().toString().replace("-", "");
			AccessKey accessKey = new AccessKey();
			accessKey.setAccessKey(accessKeyStr);
			accessKey.setUserId(user.getUserId());
			accessKey.setExpiresTime(expiresTime);
			accessKeyDao.setAccessKey(accessKey);

			return JSONResponse.signInSuccess(user.getUserId(), accessKeyStr,
					expiresTime);
		}
	}
	
	/**
	 * 设定可用总额
	 * @param userId 用户id
	 * @param accessKey accessKey
	 * @param totalMoney 可用总额
	 * @return
	 * 		设定成功则返回JSONResponse.success
	 * 		设定失败则返回JSONResponse.fail
	 */
	@POST
	@Path("setting")
	@Consumes("application/x-www-form-urlencoded")
	public String setting(
			@FormParam("userId") int userId,
			@FormParam("accessKey") @DefaultValue("") String accessKey,
			@FormParam("totalMoney") double totalMoney) {
		// 获取到由OpenSessionInViewFilter绑定到当前线程的Hibernate Session
		moneyDao.setSession(currentSession());
		
		boolean result = userRoleAuth(userId, accessKey);
		if (!result)
			return JSONResponse.needSignIn();
		
		session.beginTransaction();
		result = moneyDao.setMoney(new Money(userId, totalMoney));
		if (result) {
			session.flush();
			session.getTransaction().commit();
			return JSONResponse.success();
		} else {
			session.getTransaction().rollback();
			return JSONResponse.fail();
		}
	}
	
	/**
	 * 从服务器下载指定日期后修改过的消费记录，不指定日期则下载全部消费记录
	 * @param userId 用户id
	 * @param accessKey access key
	 * @param fromDateTime 指定日期，不指定（为空）则下载全部消费记录
	 * @return
	 * 		返回JSONResponse.syncData，携带items字段，值为Item组成的数组
	 */
	@POST
	@Path("sync")
	public String syncFromServer(
			@FormParam("userId") int userId,
			@FormParam("accessKey") @DefaultValue("") String accessKey,
			@FormParam("fromDateTime") @DefaultValue("") String rawFromDate) {
		
		boolean result = userRoleAuth(userId, accessKey);
		if (!result)
			return JSONResponse.needSignIn();
		
		itemDao.setSession(currentSession());
		
		Date fromDateTime = null;
		if (!rawFromDate.isEmpty()) {
			try {
				fromDateTime = DATE_FORMAT.parse(rawFromDate);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return JSONResponse.syncData(itemDao.bulkQueryByTime(userId, fromDateTime));
	}
	
	/**
	 * 向服务器上传消费记录
	 * @param items 由消费记录Item组成的JSON数组
	 * @return
	 * 		返回JSONResponse.syncOK，携带syncCount字段(成功上传的Item数)
	 */
	@POST
	@Path("sync")
	@Consumes("application/x-www-form-urlencoded")
	public String syncToServer(
			@FormParam("userId") int userId,
			@FormParam("accessKey") @DefaultValue("") String accessKey,
			@FormParam("items") @DefaultValue("") String items) {
		boolean result = userRoleAuth(userId, accessKey);
		if (!result)
			return JSONResponse.needSignIn();
		
		JSONArray itemArr = null;
		try {
			itemArr = new JSONArray(items);
		} catch (Exception e) {
			e.printStackTrace();
			return JSONResponse.fail();
		}
		
		List<Item> itemsToAdd = new LinkedList<Item>();
		JSONObject currentObject = null;
		Item currentItem = null;
		for (int i = 0; i < itemArr.length(); i++) {
			currentObject = itemArr.getJSONObject(i);
			currentItem = new Item();
			try {
				currentItem.setUserId(currentObject.getInt("userId"));
				currentItem.setItemId(String.valueOf(currentObject.getInt("userId"))
									+ "#" + String.valueOf(currentObject.getInt("itemId")));
				currentItem.setName(currentObject.getString("name"));
				currentItem.setIsOut(currentObject.getBoolean("isOut"));
				currentItem.setPrice(currentObject.getDouble("price"));
				currentItem.setClassify(currentObject.getInt("classify"));
				currentItem.setOccurredTime(DATE_FORMAT.parse(currentObject.getString("occurredTime")));
				currentItem.setModifiedTime(DATE_FORMAT.parse(currentObject.getString("modifiedTime")));
				itemsToAdd.add(currentItem);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		session = currentSession();
		itemDao.setSession(session);
		session.beginTransaction();
		int resultCount = itemDao.insertItems(itemsToAdd);
		session.flush();
		session.getTransaction().commit();
		return JSONResponse.syncOK(resultCount);
	}
	
	/**
	 * 验证用户的accessKey
	 * @param userId 用户id
	 * @param accessKey 用户传来的access key
	 * @return 验证通过返回true，否则返回false
	 */
	private boolean userRoleAuth(int userId, String accessKey) {	
		accessKeyDao.setSession(currentSession());
		AccessKey keyInDB = accessKeyDao.queryAccessKey(userId);
		if (!keyInDB.equals(accessKey) || keyInDB.getExpiresTime().before(new Date()))
			return false;
		return true;
	}
	
	public ItemDao getItemDao() {
		return itemDao;
	}
	
	public void setItemDao(ItemDao itemDao) {
		this.itemDao = itemDao;
	}
	
	public UserDao getUserDao() {
		return userDao;
	}
	
	public void setUserDao(UserDao userDao) {
		this.userDao = userDao;
	}
	
	public MoneyDao getMoneyDao() {
		return moneyDao;
	}
	
	public void setMoneyDao(MoneyDao moneyDao) {
		this.moneyDao = moneyDao;
	}

	public AccessKeyDao getAccessKeyDao() {
		return accessKeyDao;
	}

	public void setAccessKeyDao(AccessKeyDao accessKeyDao) {
		this.accessKeyDao = accessKeyDao;
	}

}
