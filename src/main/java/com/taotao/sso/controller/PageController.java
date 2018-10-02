package com.taotao.sso.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/page")
public class PageController {
	/**
	 * 用户注册界面的跳转
	 * @return
	 */
	@RequestMapping("/register")
	public String showRegister(){
		return "register";
	}
	/**
	 * 用户登录界面的跳转
	 * @param redirect//需要请求登录服务的url,登录服务结束后jsp页面会重新发起请求，返回到原来的页面
	 * @param model
	 * @return
	 */
	@RequestMapping("/login")
	public String showLogin(String redirect,Model model){
		model.addAttribute("redirect",redirect);
		return "login";
	}
}
