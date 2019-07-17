package com.sohu.cache.web.controller;

import java.util.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sohu.cache.web.core.Result;
import com.sohu.cache.web.core.ResultGenerator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.sohu.cache.entity.AppAudit;
import com.sohu.cache.entity.AppUser;
import com.sohu.cache.entity.InstanceInfo;
import com.sohu.cache.redis.RedisCenter;
import com.sohu.cache.redis.RedisDeployCenter;
import com.sohu.cache.stats.instance.InstanceDeployCenter;
import com.sohu.cache.stats.instance.InstanceStatsCenter;
import com.sohu.cache.web.enums.SuccessEnum;

/**
 * 应用后台管理
 * 
 * @author leifu
 * @Time 2014年7月3日
 */
@Controller
@RequestMapping("manage/instance")
public class InstanceManageController extends BaseController {

    private Logger logger = LoggerFactory.getLogger(InstanceManageController.class);

    @Resource(name = "instanceDeployCenter")
    private InstanceDeployCenter instanceDeployCenter;
    
    @Resource(name = "redisCenter")
    private RedisCenter redisCenter;
    
    @Resource(name = "instanceStatsCenter")
    private InstanceStatsCenter instanceStatsCenter;
    
    /**
     * 上线(和下线分开)
     * 
     * @param instanceId
     */
    @RequestMapping(value = "/startInstance")
    public Result doStartInstance(HttpServletRequest request, HttpServletResponse response, Model model, long appId, int instanceId) {
        HashMap<String, Object> data = new HashMap<>(0);
        AppUser appUser = getUserInfo(request);
        logger.warn("user {} startInstance {} ", appUser.getName(), instanceId);
        boolean result = false;
        if (instanceId > 0) {
            try {
                result = instanceDeployCenter.startExistInstance(appId, instanceId);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                data.put("message", e.getMessage());
            }
        } else {
            logger.error("doStartInstance instanceId:{}", instanceId);
            data.put("message", "wrong param");
        }
        logger.warn("user {} startInstance {} result is {}", appUser.getName(), instanceId, result);
        if (result) {
            data.put("success", SuccessEnum.SUCCESS.value());
        } else {
            data.put("success", SuccessEnum.FAIL.value());
        }
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 下线实例
     * 
     * @param instanceId
     */
    @RequestMapping(value = "/shutdownInstance")
    public Result doShutdownInstance(HttpServletRequest request, HttpServletResponse response, Model model, long appId, int instanceId) {
        HashMap<String, Object> data = new HashMap<>(0);
        AppUser appUser = getUserInfo(request);
        logger.warn("user {} shutdownInstance {} ", appUser.getName(), instanceId);
        boolean result = false;
        if (instanceId > 0) {
            try {
                result = instanceDeployCenter.shutdownExistInstance(appId, instanceId);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                data.put("message", e.getMessage());
            }
        } else {
            logger.error("doShutdownInstance instanceId:{}", instanceId);
            data.put("message", "wrong param");
        }
        logger.warn("user {} shutdownInstance {}, result is {}", appUser.getName(), instanceId, result);
        if (result) {
            data.put("success", SuccessEnum.SUCCESS.value());
        } else {
            data.put("success", SuccessEnum.FAIL.value());
        }
        return ResultGenerator.genSuccessResult(data);
    }
    
    /**
     * 查看redis节点日志
     */
    @RequestMapping("/log")
    public Result doShowLog(HttpServletRequest request, HttpServletResponse response, Model model, int instanceId) {
        HashMap<String, Object> data = new HashMap<>(0);
        int pageSize = NumberUtils.toInt(request.getParameter("pageSize"), 0);
        if (pageSize == 0) {
            pageSize = 100;
        }
        String instanceLogStr = instanceDeployCenter.showInstanceRecentLog(instanceId, pageSize);
        data.put("instanceLogList", StringUtils.isBlank(instanceLogStr) ? Collections.emptyList() : Arrays.asList(instanceLogStr.split("\n")));
        return ResultGenerator.genSuccessResult(data);
    }
    
    /**
     * 处理实例配置修改
     * 
     * @param appAuditId 审批id
     */
    @RequestMapping(value = "/initInstanceConfigChange")
    public Result doInitInstanceConfigChange(HttpServletRequest request,
            HttpServletResponse response, Model model, Long appAuditId) {
        HashMap<String, Object> data = new HashMap<>(0);
        // 申请原因
        AppAudit appAudit = appService.getAppAuditById(appAuditId);
        data.put("appAudit", appAudit);

        // 用第一个参数存实例id
        Long instanceId = NumberUtils.toLong(appAudit.getParam1());
        Map<String, String> redisConfigList = redisCenter.getRedisConfigList(instanceId.intValue());
        data.put("redisConfigList", redisConfigList);

        // 实例
        InstanceInfo instanceInfo = instanceStatsCenter.getInstanceInfo(instanceId);
        data.put("instanceInfo", instanceInfo);
        data.put("appId", appAudit.getAppId());
        data.put("appAuditId", appAuditId);

        // 修改配置的键值对
        data.put("instanceConfigKey", appAudit.getParam2());
        data.put("instanceConfigValue", appAudit.getParam3());

        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 
     * @param appId 应用id
     * @param host 实例ip
     * @param port 实例端口
     * @param instanceConfigKey 实例配置key
     * @param instanceConfigValue 实例配置value
     * @param appAuditId 审批id
     * @return
     */
    @RequestMapping(value = "/addInstanceConfigChange")
    public Result doAddAppConfigChange(HttpServletRequest request,
            HttpServletResponse response, Model model, Long appId, String host, int port,
            String instanceConfigKey, String instanceConfigValue, Long appAuditId) {
        AppUser appUser = getUserInfo(request);
        logger.warn("user {} change instanceConfig:appId={},{}:{};key={};value={},appAuditId:{}", appUser.getName(), appId, host, port, instanceConfigKey, instanceConfigValue, appAuditId);
        boolean isModify = false;
        if (StringUtils.isNotBlank(host) && port > 0 && appAuditId != null && StringUtils.isNotBlank(instanceConfigKey) && StringUtils.isNotBlank(instanceConfigValue)) {
            try {
                isModify = instanceDeployCenter.modifyInstanceConfig(appId, appAuditId, host, port, instanceConfigKey, instanceConfigValue);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return ResultGenerator.genFailResult(e.getMessage());
            }
        }
        logger.warn("user {} change instanceConfig:appId={},{}:{};key={};value={},appAuditId:{},result is:{}", appUser.getName(), appId, host, port, instanceConfigKey, instanceConfigValue, appAuditId, isModify);
        return ResultGenerator.genSuccessResult();
    }
    
    
    
    
    
    
}
