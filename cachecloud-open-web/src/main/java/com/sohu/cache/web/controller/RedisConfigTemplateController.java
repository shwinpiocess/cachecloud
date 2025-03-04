package com.sohu.cache.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sohu.cache.web.core.Result;
import com.sohu.cache.web.core.ResultGenerator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.sohu.cache.constant.ErrorMessageEnum;
import com.sohu.cache.constant.RedisConfigTemplateChangeEnum;
import com.sohu.cache.entity.AppUser;
import com.sohu.cache.entity.InstanceConfig;
import com.sohu.cache.redis.RedisConfigTemplateService;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.web.enums.SuccessEnum;
import com.sohu.cache.web.util.AppEmailUtil;

/**
 * Redis配置模板管理
 * 
 * @author leifu
 * @Date 2016-6-25
 * @Time 下午2:48:25
 */
@Controller
@RequestMapping("manage/redisConfig")
public class RedisConfigTemplateController extends BaseController {

    @Resource(name = "redisConfigTemplateService")
    private RedisConfigTemplateService redisConfigTemplateService;

    @Resource(name = "appEmailUtil")
    private AppEmailUtil appEmailUtil;

    /**
     * 初始化配置
     */
    @RequestMapping(value = "/init")
    public Result init(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        // 默认是Redis普通节点配置
        int type = NumberUtils.toInt(request.getParameter("type"), ConstUtils.CACHE_REDIS_STANDALONE);
        data.put("redisConfigList", redisConfigTemplateService.getByType(type));
        data.put("success", request.getParameter("success"));
        data.put("redisConfigActive", SuccessEnum.SUCCESS.value());
        data.put("type", type);
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 修改配置
     */
    @RequestMapping(value = "/update")
    public Result update(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        AppUser appUser = getUserInfo(request);
        String id = request.getParameter("id");
        String configKey = request.getParameter("configKey");
        String configValue = request.getParameter("configValue");
        String info = request.getParameter("info");
        int status = NumberUtils.toInt(request.getParameter("status"), -1);
        if (StringUtils.isBlank(id) || !NumberUtils.isDigits(id) || StringUtils.isBlank(configKey) || status > 1
                || status < 0) {
            data.put("status", SuccessEnum.FAIL.value());
            data.put("message", ErrorMessageEnum.PARAM_ERROR_MSG.getMessage() + "id=" + id + ",configKey="
                    + configKey + ",configValue=" + configValue + ",status=" + status);
            return ResultGenerator.genSuccessResult(data);
        }
        //开始修改
        logger.warn("user {} want to change id={}'s configKey={}, configValue={}, info={}, status={}", appUser.getName(),
                id, configKey, configValue, info, status);
        SuccessEnum successEnum;
        InstanceConfig instanceConfig = redisConfigTemplateService.getById(NumberUtils.toLong(id));
        try {
            instanceConfig.setConfigValue(configValue);
            instanceConfig.setInfo(info);
            instanceConfig.setStatus(status);
            redisConfigTemplateService.saveOrUpdate(instanceConfig);
            successEnum = SuccessEnum.SUCCESS;
        } catch (Exception e) {
            successEnum = SuccessEnum.FAIL;
            data.put("message", ErrorMessageEnum.INNER_ERROR_MSG.getMessage());
            logger.error(e.getMessage(), e);
        }
        logger.warn("user {} want to change id={}'s configKey={}, configValue={}, info={}, status={}, result is {}", appUser.getName(),
                id, configKey, configValue, info, status, successEnum.value());
        //发送邮件通知
        appEmailUtil.sendRedisConfigTemplateChangeEmail(appUser, instanceConfig, successEnum, RedisConfigTemplateChangeEnum.UPDATE);
        data.put("status", successEnum.value());
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 删除配置
     */
    @RequestMapping(value = "/remove")
    public Result remove(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        AppUser appUser = getUserInfo(request);
        String idParam = request.getParameter("id");
        long id = NumberUtils.toLong(idParam);
        if (id <= 0) {
            data.put("status", SuccessEnum.FAIL.value());
            data.put("message", ErrorMessageEnum.PARAM_ERROR_MSG.getMessage() + "id=" + idParam);
            return ResultGenerator.genSuccessResult(data);
        }
        logger.warn("user {} want to delete id={}'s config", appUser.getName(), id);
        SuccessEnum successEnum;
        InstanceConfig instanceConfig = redisConfigTemplateService.getById(id);
        try {
            redisConfigTemplateService.remove(id);
            successEnum = SuccessEnum.SUCCESS;
        } catch (Exception e) {
            successEnum = SuccessEnum.FAIL;
            data.put("message", ErrorMessageEnum.INNER_ERROR_MSG.getMessage());
            logger.error(e.getMessage(), e);
        }
        logger.warn("user {} want to delete id={}'s config, result is {}", appUser.getName(), id, successEnum.value());
        //发送邮件通知
        appEmailUtil.sendRedisConfigTemplateChangeEmail(appUser, instanceConfig, successEnum, RedisConfigTemplateChangeEnum.DELETE);
        data.put("status", successEnum.value());
        return ResultGenerator.genSuccessResult(data);

    }

    /**
     * 添加配置
     */
    @RequestMapping(value = "/add")
    public Result add(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        AppUser appUser = getUserInfo(request);
        InstanceConfig instanceConfig = getInstanceConfig(request);
        if (StringUtils.isBlank(instanceConfig.getConfigKey())) {
            data.put("status", SuccessEnum.FAIL.value());
            data.put("message", ErrorMessageEnum.PARAM_ERROR_MSG.getMessage() + "configKey=" + instanceConfig.getConfigKey());
            return ResultGenerator.genSuccessResult(data);
        }
        logger.warn("user {} want to add config, configKey is {}, configValue is {}, type is {}", appUser.getName(),
                instanceConfig.getConfigKey(), instanceConfig.getType());
        SuccessEnum successEnum;
        try {
            redisConfigTemplateService.saveOrUpdate(instanceConfig);
            successEnum = SuccessEnum.SUCCESS;
        } catch (Exception e) {
            successEnum = SuccessEnum.FAIL;
            data.put("message", ErrorMessageEnum.INNER_ERROR_MSG.getMessage());
            logger.error(e.getMessage(), e);
        }
        logger.warn("user {} want to add config, configKey is {}, configValue is {}, type is {}, result is {}",
                appUser.getName(),
                instanceConfig.getConfigKey(), instanceConfig.getConfigValue(), instanceConfig.getType(), successEnum.value());
        data.put("status", successEnum.value());
        //发送邮件通知
        appEmailUtil.sendRedisConfigTemplateChangeEmail(appUser, instanceConfig, successEnum, RedisConfigTemplateChangeEnum.ADD);
        return ResultGenerator.genSuccessResult(data);

    }

    /**
     * 预览配置
     */
    @RequestMapping(value = "/preview")
    public Result preview(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        //默认配置
        int type = NumberUtils.toInt(request.getParameter("type"), -1);
        String host = StringUtils.isBlank(request.getParameter("host")) ? "127.0.0.1" : request.getParameter("host");
        int port = NumberUtils.toInt(request.getParameter("port"), 6379);
        int maxMemory = NumberUtils.toInt(request.getParameter("maxMemory"), 2048);
        int sentinelPort = NumberUtils.toInt(request.getParameter("sentinelPort"), 26379);
        String masterName = StringUtils.isBlank(request.getParameter("masterName")) ? "myMaster" : request
                .getParameter("masterName");
        int quorum = NumberUtils.toInt(request.getParameter("quorum"), 2);

        // 根据类型生成配置模板
        List<String> configList = new ArrayList<String>();
        if (ConstUtils.CACHE_REDIS_STANDALONE == type) {
            configList = redisConfigTemplateService.handleCommonConfig(port, maxMemory);
        } else if (ConstUtils.CACHE_REDIS_SENTINEL == type) {
            configList = redisConfigTemplateService.handleSentinelConfig(masterName, host, port, sentinelPort);
        } else if (ConstUtils.CACHE_TYPE_REDIS_CLUSTER == type) {
            configList = redisConfigTemplateService.handleClusterConfig(port);
        }
        data.put("type", type);
        data.put("host", host);
        data.put("port", port);
        data.put("maxMemory", maxMemory);
        data.put("sentinelPort", sentinelPort);
        data.put("masterName", masterName);
        data.put("configList", configList);
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 使用最简单的request生成InstanceConfig对象
     * 
     * @return
     */
    private InstanceConfig getInstanceConfig(HttpServletRequest request) {
        String configKey = request.getParameter("configKey");
        String configValue = request.getParameter("configValue");
        String info = request.getParameter("info");
        String type = request.getParameter("type");
        InstanceConfig instanceConfig = new InstanceConfig();
        instanceConfig.setConfigKey(configKey);
        instanceConfig.setConfigValue(configValue);
        instanceConfig.setInfo(info);
        instanceConfig.setType(NumberUtils.toInt(type));
        instanceConfig.setStatus(1);
        return instanceConfig;
    }

}
