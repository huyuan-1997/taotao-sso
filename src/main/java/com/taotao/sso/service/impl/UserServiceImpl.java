package com.taotao.sso.service.impl;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import com.taotao.common.pojo.TaotaoResult;
import com.taotao.common.utils.CookieUtils;
import com.taotao.common.utils.JsonUtils;
import com.taotao.mapper.TbUserMapper;
import com.taotao.pojo.TbUser;
import com.taotao.pojo.TbUserExample;
import com.taotao.pojo.TbUserExample.Criteria;
import com.taotao.sso.dao.JedisClient;
import com.taotao.sso.service.UserService;

@Service
public class UserServiceImpl implements UserService{
	@Autowired
	private TbUserMapper userMapper;
	// 数据库层
	@Autowired
	private JedisClient jedisClient;
	@Value("${REDIS_USER_SESSION_KEY}")
	private String REDIS_USER_SESSION_KEY;
	@Value("${SSO_SESSION_EXPIRE}")
	private Integer SSO_SESSION_EXPIRE;

	/**
	 * 数据校验，查看是否是新用户 查看用户名，手机号，邮箱是否有重复
	 */
	public TaotaoResult checkData(String content, Integer type) {
		// 创建查询条件
		TbUserExample example = new TbUserExample();
		Criteria criteria = example.createCriteria();
		// 对数据进行校验
		// 用户名校验
		if (1 == type) {
			criteria.andUsernameEqualTo(content);
			// 电话校验
		} else if (2 == type) {
			criteria.andPhoneEqualTo(content);
			// email校验
		} else {
			criteria.andEmailEqualTo(content);
		}
		//执行查询
		List<TbUser> list = userMapper.selectByExample(example);
		if(list==null || list.size()==0){
			return TaotaoResult.ok(true);
		}
		return TaotaoResult.ok(false);
	}
	/**
	 * 注册用户名，将用户信息保存到数据库
	 */
	public TaotaoResult createUser(TbUser user){
		user.setUpdated(new Date());
		user.setCreated(new Date());
		//md5加密
		user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));;
		userMapper.insert(user);
		return TaotaoResult.ok();
	}
	/**
	 * 用户登录
	 */
	public TaotaoResult userLogin(String username,String password,HttpServletRequest request,HttpServletResponse response){
		//根据用户名查询用户
		TbUserExample example = new TbUserExample();
		Criteria  criteria = example.createCriteria();
		criteria.andUsernameEqualTo(username);
		//执行查询
		List<TbUser> list = userMapper.selectByExample(example);
		//没有用户
		if(null==list||list.size()==0){
			return TaotaoResult.build(400, "用户名或密码错误");
		}
		TbUser user = list.get(0);
		//对比密码
		if(!DigestUtils.md5DigestAsHex(password.getBytes()).equals(user.getPassword())){
			return TaotaoResult.build(400, "用户名或者密码错误");
		}
		//登录信息都正确
		//生成token
		String token = UUID.randomUUID().toString();
		//保存用户对象之前需要将密码清空，实际开发中必须要知道的
		user.setPassword(null);
		//将用户信息写入到redis中
		jedisClient.set(REDIS_USER_SESSION_KEY+":"+token,JsonUtils.objectToJson(user));
		//设置登录信息的过期时间
		//将用户信息保存到Cookie中
		CookieUtils.setCookie(request, response, "TT_TOKEN", token);
		//返回token
		return TaotaoResult.ok(token);		
	}
	/**
	 * 根据token查询用户信息
	 * 
	 */
	public TaotaoResult getUserByToken(String token){
		//根据token从redis中查询用户信息
		String json = jedisClient.get(REDIS_USER_SESSION_KEY+":"+token);
		//判断是否是空
		if(StringUtils.isBlank(json)){
			return TaotaoResult.build(400, "登录超时，请重新登录");			
		}
		//不是空值，说明还在操作，更新token的存储时间
		jedisClient.expire(REDIS_USER_SESSION_KEY + ":" + token, SSO_SESSION_EXPIRE);		
		//返回用户信息
		return TaotaoResult.ok(JsonUtils.jsonToPojo(json, TbUser.class));
	}
	

}
