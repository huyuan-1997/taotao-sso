package com.taotao.sso.dao;

public interface JedisClient {
	String get(String key);
	String set(String key, String value);
	String hget(String hkey, String key);
	long hset(String hkey, String key, String value);
	long incr(String key);
	long expire(String key, int second);//设置key的存活时间
	long ttl(String key);
	long del(String key);
	long hdel(String hkey,String key);
}
