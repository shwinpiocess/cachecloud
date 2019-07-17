package com.sohu.cache.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.sohu.cache.web.core.Result;
import com.sohu.cache.web.core.ResultCode;
import com.sohu.cache.web.core.ResultGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.sohu.cache.entity.AppUser;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.web.service.UserLoginStatusService;
import com.sohu.cache.web.service.UserService;

import java.io.IOException;

/**
 * 前台登陆验证
 *
 * @author leifu
 * @Time 2014年6月12日
 */
public class FrontUserLoginInterceptor extends HandlerInterceptorAdapter {
    private Logger logger = LoggerFactory.getLogger(FrontUserLoginInterceptor.class);
    
    private UserService userService;
    
    private UserLoginStatusService userLoginStatusService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) throws Exception {

        long userId = userLoginStatusService.getUserIdFromLoginStatus(request);
        AppUser user = userService.get(userId);
        
        if (user == null) {
            Result result = new Result();
            result.setCode(ResultCode.UNAUTHORIZED).setMessage("签名认证失败");
            responseResult(response, result);
            return false;
        }
        
        request.setAttribute("userInfo", user);
        request.setAttribute("uri", request.getRequestURI());
        
        return true;
    }

    private void responseResult(HttpServletResponse response, Result result) {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-type", "application/json;charset=UTF-8");
        response.setStatus(200);
        try {
            response.getWriter().write(JSON.toJSONString(result));
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setUserLoginStatusService(UserLoginStatusService userLoginStatusService) {
        this.userLoginStatusService = userLoginStatusService;
    }

}