package com.sohu.cache.web.controller;

import java.util.Date;
import java.util.HashMap;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sohu.cache.web.core.Result;
import com.sohu.cache.web.core.ResultGenerator;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.sohu.cache.constant.ImportAppResult;
import com.sohu.cache.entity.AppDesc;
import com.sohu.cache.entity.AppUser;
import com.sohu.cache.stats.app.ImportAppCenter;

/**
 * 已经存在Redis导入
 * 
 * @author leifu
 * @Date 2016-4-16
 * @Time 下午2:31:14
 */
@Controller
@RequestMapping("/import/app")
public class ImportAppController extends BaseController {

    @Resource(name = "importAppCenter")
    private ImportAppCenter importAppCenter;

    @RequestMapping(value = "/init")
    public ModelAndView init(HttpServletRequest request, HttpServletResponse response, Model model) {
        return new ModelAndView("import/init");
    }

    @RequestMapping(value = "/check")
    public Result check(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        AppDesc appDesc = genAppDesc(request);
        String appInstanceInfo = request.getParameter("appInstanceInfo");
        ImportAppResult importAppResult = importAppCenter.check(appDesc, appInstanceInfo);
        data.put("status", importAppResult.getStatus());
        data.put("message", importAppResult.getMessage());
        return ResultGenerator.genSuccessResult(data);
    }

    @RequestMapping(value = "/add")
    public Result add(HttpServletRequest request,
            HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        AppDesc appDesc = genAppDesc(request);
        String appInstanceInfo = request.getParameter("appInstanceInfo");
        logger.warn("appDesc:" + appDesc);
        logger.warn("appInstanceInfo: " + appInstanceInfo);

        // 不需要对格式进行检验,check已经做过了。
        boolean isSuccess = importAppCenter.importAppAndInstance(appDesc, appInstanceInfo);
        logger.warn("import app result is {}", isSuccess);

        data.put("status", isSuccess ? 1 : 0);
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 生成AppDesc
     * 
     * @param request
     * @return
     */
    private AppDesc genAppDesc(HttpServletRequest request) {
        // 当前用户
        AppUser currentUser = getUserInfo(request);
        // 当前时间
        Date date = new Date();
        // 组装Appdesc
        AppDesc appDesc = new AppDesc();
        appDesc.setName(request.getParameter("name"));
        appDesc.setIntro(request.getParameter("intro"));
        appDesc.setOfficer(request.getParameter("officer"));
        appDesc.setType(NumberUtils.toInt(request.getParameter("type")));
        appDesc.setIsTest(NumberUtils.toInt(request.getParameter("isTest")));
        appDesc.setMemAlertValue(NumberUtils.toInt(request.getParameter("memAlertValue")));
        appDesc.setPassword(request.getParameter("password"));
        appDesc.setUserId(currentUser.getId());
        appDesc.setStatus(2);
        appDesc.setCreateTime(date);
        appDesc.setPassedTime(date);
        appDesc.setVerId(1);

        return appDesc;
    }

}
