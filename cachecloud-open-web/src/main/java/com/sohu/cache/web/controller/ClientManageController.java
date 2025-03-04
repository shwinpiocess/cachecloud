package com.sohu.cache.web.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sohu.cache.web.core.Result;
import com.sohu.cache.web.core.ResultGenerator;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.sohu.cache.client.service.ClientReportExceptionService;
import com.sohu.cache.client.service.ClientVersionService;
import com.sohu.cache.entity.AppClientVersion;
import com.sohu.cache.entity.AppDesc;
import com.sohu.cache.entity.ClientInstanceException;
import com.sohu.cache.web.enums.SuccessEnum;

/**
 * 客户端管理
 * 
 * @author leifu
 * @Date 2016年2月18日
 * @Time 下午4:55:32
 */
@Controller
@RequestMapping("manage/client")
public class ClientManageController extends BaseController {

    /**
     * 客户端异常服务
     */
    @Resource(name = "clientReportExceptionService")
    private ClientReportExceptionService clientReportExceptionService;
    
    /**
     * 客户端版本服务
     */
    @Resource(name = "clientVersionService")
    private ClientVersionService clientVersionService;

    /**
     * /manage/client/exception
     * @param request
     * @param response
     * @param model
     * @return
     */
    @RequestMapping(value = "/exception")
    public Result doClientExceptionStat(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        String ip = request.getParameter("ip");
        data.put("ip", ip);
        //近一个月
        long collectTime = NumberUtils.toLong(new SimpleDateFormat("yyyyMMdd000000").format(DateUtils.addMonths(new Date(), -1)));
        
        // 一段时间内客户端异常
        List<ClientInstanceException> clientInstanceExceptionList = clientReportExceptionService.getInstanceExceptionStat(ip, collectTime);
        data.put("clientInstanceExceptionList", clientInstanceExceptionList);
        
        // 应用相关map
        fillAppInfoMap(model);

        data.put("clientExceptionActive", SuccessEnum.SUCCESS.value());
        return ResultGenerator.genSuccessResult(data);
    }

    private void fillAppInfoMap(Model model) {
        List<AppDesc> appDescList = appService.getAllAppDesc();
        
        // 所有应用id和负责人对应关系
        Map<Long, String> appIdOwnerMap = new HashMap<Long, String>();
        for (AppDesc appDesc : appDescList) {
            appIdOwnerMap.put(appDesc.getAppId(), appDesc.getOfficer());
        }
        model.addAttribute("appIdOwnerMap", appIdOwnerMap);
        
        // 所有应用id和应用名对应关系
        Map<Long, String> appIdNameMap = new HashMap<Long, String>();
        for (AppDesc appDesc : appDescList) {
            appIdNameMap.put(appDesc.getAppId(), appDesc.getName());
        }
        model.addAttribute("appIdNameMap", appIdNameMap);
    }

    /**
     * /manage/client/version
     * @param request
     * @param response
     * @param model
     * @return
     */
    @RequestMapping(value = "/version")
    public Result doVersionStat(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        long appId = NumberUtils.toLong(request.getParameter("appId"),-1);
        List<AppClientVersion> appClientVersionList =  clientVersionService.getAll(appId);
        
        // 应用相关map
        fillAppInfoMap(model);
        
        data.put("appClientVersionList", appClientVersionList);
        data.put("clientVersionActive", SuccessEnum.SUCCESS.value());
        data.put("appId", request.getParameter("appId"));
        
        return ResultGenerator.genSuccessResult(data);
    }
}
