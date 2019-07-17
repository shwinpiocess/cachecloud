package com.sohu.cache.web.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sohu.cache.web.core.Result;
import com.sohu.cache.web.core.ResultGenerator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.sohu.cache.constant.ErrorMessageEnum;
import com.sohu.cache.dao.InstanceDao;
import com.sohu.cache.entity.AppUser;
import com.sohu.cache.entity.InstanceAlertConfig;
import com.sohu.cache.entity.InstanceInfo;
import com.sohu.cache.redis.enums.InstanceAlertCheckCycleEnum;
import com.sohu.cache.redis.enums.InstanceAlertCompareTypeEnum;
import com.sohu.cache.redis.enums.InstanceAlertStatusEnum;
import com.sohu.cache.redis.enums.InstanceAlertTypeEnum;
import com.sohu.cache.redis.enums.RedisAlertConfigEnum;
import com.sohu.cache.stats.instance.InstanceAlertConfigService;
import com.sohu.cache.web.enums.SuccessEnum;

/**
 * 实例报警阀值
 * @author leifu
 * @Date 2016年8月24日
 * @Time 下午1:24:25
 */
@Controller
@RequestMapping("manage/instanceAlert")
public class InstanceAlertValueController extends BaseController {
    
    @Resource(name = "instanceAlertConfigService")
    private InstanceAlertConfigService instanceAlertConfigService;
    
    @Resource(name = "instanceDao")
    private InstanceDao instanceDao;
    
    /**
     * 初始化配置
     */
    @RequestMapping(value = "/init")
    public Result init(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        data.put("instanceAlertCheckCycleEnumList", InstanceAlertCheckCycleEnum.getInstanceAlertCheckCycleEnumList());
        data.put("instanceAlertCompareTypeEnumList", InstanceAlertCompareTypeEnum.getInstanceAlertCompareTypeEnumList());
        data.put("redisAlertConfigEnumList", RedisAlertConfigEnum.getRedisAlertConfigEnumList());
        data.put("instanceAlertAllList", instanceAlertConfigService.getByType(InstanceAlertTypeEnum.ALL_ALERT.getValue()));
        data.put("instanceAlertList", instanceAlertConfigService.getAll());
        data.put("success", request.getParameter("success"));
        data.put("instanceAlertValueActive", SuccessEnum.SUCCESS.value());
        List<InstanceAlertConfig> instanceAlertSpecialList = instanceAlertConfigService.getByType(InstanceAlertTypeEnum.INSTANCE_ALERT.getValue());
        fillinstanceHostPort(instanceAlertSpecialList);
        data.put("instanceAlertSpecialList", instanceAlertSpecialList);
        return ResultGenerator.genSuccessResult(data);
    }
    
    /**
     * 填充hostport
     * @param instanceAlertSpecialList
     */
    private void fillinstanceHostPort(List<InstanceAlertConfig> instanceAlertSpecialList) {
        if (CollectionUtils.isEmpty(instanceAlertSpecialList)) {
            return;
        }
        for (InstanceAlertConfig instanceAlertConfig : instanceAlertSpecialList) {
            long instanceId = instanceAlertConfig.getInstanceId();
            InstanceInfo instanceInfo = instanceDao.getInstanceInfoById(instanceId);
            if (instanceInfo == null) {
                continue;
            }
            instanceAlertConfig.setInstanceInfo(instanceInfo);
        }
    }

    /**
     * 添加配置
     */
    @RequestMapping(value = "/add")
    public Result add(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        AppUser appUser = getUserInfo(request);
        InstanceAlertConfig instanceAlertConfig = getInstanceAlertConfig(request);
        SuccessEnum successEnum;
        try {
            logger.warn("user {} want to add instanceAlertConfig {}, result is {}", appUser.getName(), instanceAlertConfig);
            instanceAlertConfigService.save(instanceAlertConfig);
            successEnum = SuccessEnum.SUCCESS;
        } catch (Exception e) {
            successEnum = SuccessEnum.FAIL;
            data.put("message", ErrorMessageEnum.INNER_ERROR_MSG.getMessage());
            logger.error(e.getMessage(), e);
        }
        logger.warn("user {} add instanceAlertConfig {}, result is {}", appUser.getName(),instanceAlertConfig,successEnum.value());
        data.put("status", successEnum.value());
        return ResultGenerator.genSuccessResult(data);
    }
    
    /**
     * 检查hostPort是否存在
     */
    @RequestMapping(value = "/checkInstanceHostPort")
    public Result checkInstanceHostPort(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        String hostPort = request.getParameter("instanceHostPort");
        if (StringUtils.isBlank(hostPort)) {
            data.put("status", SuccessEnum.FAIL.value());
            data.put("message","参数为空");
            return ResultGenerator.genSuccessResult(data);
        }
        String[] hostPortArr = hostPort.split(":");
        if (hostPortArr.length != 2) {
            data.put("status", SuccessEnum.FAIL.value());
            data.put("message","hostPort:" + hostPort + "格式错误");
            return ResultGenerator.genSuccessResult(data);
        }
        String host = hostPortArr[0];
        int port = NumberUtils.toInt(hostPortArr[1]);
        InstanceInfo instanceInfo = instanceDao.getAllInstByIpAndPort(host, port);
        if (instanceInfo == null) {
            data.put("status", SuccessEnum.FAIL.value());
            data.put("message","hostPort:" + hostPort + "不存在");
        } else {
            data.put("status", SuccessEnum.SUCCESS.value());
        }
        return ResultGenerator.genSuccessResult(data);
    }
    
    /**
     * 初始化配置
     */
    @RequestMapping(value = "/monitor")
    public ModelAndView monitor(HttpServletRequest request, HttpServletResponse response, Model model) {
        instanceAlertConfigService.monitorLastMinuteAllInstanceInfo();
        return null;
    }

    /**
     * 修改配置
     */
    @RequestMapping(value = "/update")
    public Result update(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        AppUser appUser = getUserInfo(request);
        int id = NumberUtils.toInt(request.getParameter("id"));
        String alertValue = request.getParameter("alertValue");
        int checkCycle = NumberUtils.toInt(request.getParameter("checkCycle"));
        logger.warn("user {} want to change instance alert id={}, alertValue={}, checkCycle={}", appUser.getName(), alertValue, checkCycle);
        SuccessEnum successEnum;
        try {
            instanceAlertConfigService.update(id, alertValue, checkCycle);
            successEnum = SuccessEnum.SUCCESS;
        } catch (Exception e) {
            successEnum = SuccessEnum.FAIL;
            data.put("message", ErrorMessageEnum.INNER_ERROR_MSG.getMessage());
            logger.error(e.getMessage(), e);
        }
        logger.warn("user {} change instance alert id={}, alertValue={}, checkCycle={}, result is {}", appUser.getName(), alertValue, checkCycle, successEnum.info());
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
        int id = NumberUtils.toInt(request.getParameter("id"));
        InstanceAlertConfig instanceAlertConfig = instanceAlertConfigService.get(id);
        logger.warn("user {} want to delete config id {}, instanceAlertConfig {}", appUser.getName(), id, instanceAlertConfig);
        SuccessEnum successEnum;
        try {
            instanceAlertConfigService.remove(id);
            successEnum = SuccessEnum.SUCCESS;
        } catch (Exception e) {
            successEnum = SuccessEnum.FAIL;
            data.put("message", ErrorMessageEnum.INNER_ERROR_MSG.getMessage());
            logger.error(e.getMessage(), e);
        }
        logger.warn("user {} want to delete config id {}, instanceAlertConfig {}, result is {}", appUser.getName(), id, instanceAlertConfig, successEnum.info());
        data.put("status", successEnum.value());
        return ResultGenerator.genSuccessResult(data);

    }

    
    private InstanceInfo getInstanceInfo (String hostPort) {
        String[] hostPortArr = hostPort.split(":");
        String host = hostPortArr[0];
        int port = NumberUtils.toInt(hostPortArr[1]);
        return instanceDao.getAllInstByIpAndPort(host, port);
    }


    private InstanceAlertConfig getInstanceAlertConfig(HttpServletRequest request) {
        // 相关参数
        Date now = new Date();
        String alertConfig = request.getParameter("alertConfig");
        String alertValue = request.getParameter("alertValue");
        RedisAlertConfigEnum redisAlertConfigEnum = RedisAlertConfigEnum.getRedisAlertConfig(alertConfig);
        String configInfo = redisAlertConfigEnum == null ? "" : redisAlertConfigEnum.getInfo();
        int compareType = NumberUtils.toInt(request.getParameter("compareType"));
        int checkCycle = NumberUtils.toInt(request.getParameter("checkCycle"));
        int instanceId = 0;
        int type = NumberUtils.toInt(request.getParameter("type"));
        if (InstanceAlertTypeEnum.INSTANCE_ALERT.getValue() == type) {
            String hostPort = request.getParameter("instanceHostPort");
            InstanceInfo instanceInfo = getInstanceInfo(hostPort);
            instanceId = instanceInfo.getId();
        }
        // 生成对象
        InstanceAlertConfig instanceAlertConfig = new InstanceAlertConfig();
        instanceAlertConfig.setAlertConfig(alertConfig);
        instanceAlertConfig.setAlertValue(alertValue);
        instanceAlertConfig.setConfigInfo(configInfo);
        instanceAlertConfig.setCompareType(compareType);
        instanceAlertConfig.setInstanceId(instanceId);
        instanceAlertConfig.setCheckCycle(checkCycle);
        instanceAlertConfig.setLastCheckTime(now);
        instanceAlertConfig.setType(type);
        instanceAlertConfig.setUpdateTime(now);
        instanceAlertConfig.setStatus(InstanceAlertStatusEnum.YES.getValue());
        return instanceAlertConfig;
    }
    
}
