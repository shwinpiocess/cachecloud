package com.sohu.cache.web.controller;


import java.util.*;

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

import com.sohu.cache.constant.CommandResult;
import com.sohu.cache.constant.MachineInfoEnum;
import com.sohu.cache.constant.AppDataMigrateEnum;
import com.sohu.cache.constant.AppDataMigrateResult;
import com.sohu.cache.constant.RedisMigrateToolConstant;
import com.sohu.cache.entity.AppDataMigrateSearch;
import com.sohu.cache.entity.AppDataMigrateStatus;
import com.sohu.cache.entity.AppDesc;
import com.sohu.cache.entity.AppUser;
import com.sohu.cache.entity.InstanceInfo;
import com.sohu.cache.entity.MachineInfo;
import com.sohu.cache.machine.MachineCenter;
import com.sohu.cache.redis.RedisCenter;
import com.sohu.cache.stats.app.AppDataMigrateCenter;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.util.TypeUtil;
import com.sohu.cache.web.service.AppService;

/**
 * 应用数据迁移入口
 * 
 * @author leifu
 * @Date 2016-6-8
 * @Time 下午11:10:34
 */
@Controller
@RequestMapping("/data/migrate")
public class AppDataMigrateController extends BaseController {

    @Resource(name = "appDataMigrateCenter")
    private AppDataMigrateCenter appDataMigrateCenter;
    
    @Resource(name = "appService")
    private AppService appService;
    
    @Resource(name = "machineCenter")
    private MachineCenter machineCenter;
    
    @Resource(name = "redisCenter")
    private RedisCenter redisCenter;
    
    private static Set<String> MIGRATE_SAMPLE_USEFUL_LINES = new HashSet<String>();
    static {
        MIGRATE_SAMPLE_USEFUL_LINES.add("Checked keys");
        MIGRATE_SAMPLE_USEFUL_LINES.add("Inconsistent value keys");
        MIGRATE_SAMPLE_USEFUL_LINES.add("Inconsistent expire keys");
        MIGRATE_SAMPLE_USEFUL_LINES.add("Other check error keys");
        MIGRATE_SAMPLE_USEFUL_LINES.add("Checked OK keys");
    }
    

    /**
     * 初始化界面
     * @return
     */
    @RequestMapping(value = "/init")
    public Result init(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        List<MachineInfo> machineInfoList = machineCenter.getMachineInfoByType(MachineInfoEnum.TypeEnum.REDIS_MIGRATE_TOOL);
        data.put("machineInfoList", machineInfoList);
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 检查配置
     * @return
     */
    @RequestMapping(value = "/check")
    public Result check(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        //相关参数
        String migrateMachineIp = request.getParameter("migrateMachineIp");
        String sourceRedisMigrateIndex = request.getParameter("sourceRedisMigrateIndex");
        AppDataMigrateEnum sourceRedisMigrateEnum = AppDataMigrateEnum.getByIndex(NumberUtils.toInt(sourceRedisMigrateIndex, -1));
        String sourceServers = request.getParameter("sourceServers");
        String targetRedisMigrateIndex = request.getParameter("targetRedisMigrateIndex");
        AppDataMigrateEnum targetRedisMigrateEnum = AppDataMigrateEnum.getByIndex(NumberUtils.toInt(targetRedisMigrateIndex, -1));
        String targetServers = request.getParameter("targetServers");
        String redisSourcePass = request.getParameter("redisSourcePass");
        String redisTargetPass = request.getParameter("redisTargetPass");

        //检查返回结果
        AppDataMigrateResult redisMigrateResult = appDataMigrateCenter.check(migrateMachineIp, sourceRedisMigrateEnum, sourceServers, targetRedisMigrateEnum, targetServers, redisSourcePass, redisTargetPass);
        data.put("status", redisMigrateResult.getStatus());
        data.put("message", redisMigrateResult.getMessage());
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 开始迁移
     * @return
     */
    @RequestMapping(value = "/start")
    public Result start(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        //相关参数
        String migrateMachineIp = request.getParameter("migrateMachineIp");
        String sourceRedisMigrateIndex = request.getParameter("sourceRedisMigrateIndex");
        AppDataMigrateEnum sourceRedisMigrateEnum = AppDataMigrateEnum.getByIndex(NumberUtils.toInt(sourceRedisMigrateIndex, -1));
        String sourceServers = request.getParameter("sourceServers");
        String targetRedisMigrateIndex = request.getParameter("targetRedisMigrateIndex");
        AppDataMigrateEnum targetRedisMigrateEnum = AppDataMigrateEnum.getByIndex(NumberUtils.toInt(targetRedisMigrateIndex, -1));
        String targetServers = request.getParameter("targetServers");
        long sourceAppId = NumberUtils.toLong(request.getParameter("sourceAppId"));
        long targetAppId = NumberUtils.toLong(request.getParameter("targetAppId"));
        String redisSourcePass = request.getParameter("redisSourcePass");
        String redisTargetPass = request.getParameter("redisTargetPass");

        AppUser appUser = getUserInfo(request);
        long userId = appUser == null ? 0 : appUser.getId();

        // 不需要对格式进行检验,check已经做过了，开始迁移
        boolean isSuccess = appDataMigrateCenter.migrate(migrateMachineIp, sourceRedisMigrateEnum, sourceServers,
                targetRedisMigrateEnum, targetServers, sourceAppId, targetAppId, redisSourcePass, redisTargetPass, userId);

        data.put("status", isSuccess ? 1 : 0);
        return ResultGenerator.genSuccessResult(data);
    }
    
    /**
     * 停掉迁移任务
     * @return
     */
    @RequestMapping(value = "/stop")
    public Result stop(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        //任务id：查到任务相关信息
        long id = NumberUtils.toLong(request.getParameter("id"));
        AppDataMigrateResult stopMigrateResult = appDataMigrateCenter.stopMigrate(id);
        data.put("status", stopMigrateResult.getStatus());
        data.put("message", stopMigrateResult.getMessage());
        return ResultGenerator.genSuccessResult(data);
    }
    
    /**
     * 查看迁移日志
     * @return
     */
    @RequestMapping(value = "/log")
    public Result log(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        //任务id：查到任务相关信息
        long id = NumberUtils.toLong(request.getParameter("id"));
        int pageSize = NumberUtils.toInt(request.getParameter("pageSize"), 0);
        if (pageSize == 0) {
            pageSize = 100;
        }
        
        String log = appDataMigrateCenter.showDataMigrateLog(id, pageSize);
        data.put("logList", Arrays.asList(log.split(ConstUtils.NEXT_LINE)));
        return ResultGenerator.genSuccessResult(data);
    }
    
    /**
     * 查看迁移日志
     * @return
     */
    @RequestMapping(value = "/config")
    public Result config(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        //任务id：查到任务相关信息
        long id = NumberUtils.toLong(request.getParameter("id"));
        String config = appDataMigrateCenter.showDataMigrateConf(id);
        data.put("configList", Arrays.asList(config.split(ConstUtils.NEXT_LINE)));
        return ResultGenerator.genSuccessResult(data);
    }
    
    /**
     * 查看迁移进度
     * @return
     */
    @RequestMapping(value = "/process")
    public Result showProcess(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        long id = NumberUtils.toLong(request.getParameter("id"));
        Map<RedisMigrateToolConstant, Map<String, Object>> migrateToolStatMap = appDataMigrateCenter.showMiragteToolProcess(id);
        data.put("migrateToolStatMap", migrateToolStatMap);
        return ResultGenerator.genSuccessResult(data);
    }
    
    /**
     * 查看迁移进度
     * @return
     */
    @RequestMapping(value = "/checkData")
    public Result checkData(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        long id = NumberUtils.toLong(request.getParameter("id"));
        int nums = 1000 + new Random().nextInt(2000);
        //为了方便，直接传入命令
        CommandResult commandResult = appDataMigrateCenter.sampleCheckData(id, nums);
        String message = commandResult.getResult();
        List<String> checkDataResultList = new ArrayList<String>();
        checkDataResultList.add("一共随机检验了" + nums + "个key" + ",检查结果如下:");
        String[] lineArr = message.split(ConstUtils.NEXT_LINE);
        for (String line : lineArr) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            // 行数太多显示会有问题
            if (lineArr.length > 100 && !isUsefulLine(line)) {
                continue;
            }
            //message格式显示有点问题
            line = line.replace("[0m", "");
            line = line.replace("[31m", "");
            line = line.replace("[33m", "");
            checkDataResultList.add(line.trim());
        }
        data.put("checkDataResultList", checkDataResultList);
        data.put("checkDataCommand", commandResult.getCommand());
        return ResultGenerator.genSuccessResult(data);
    }

    private boolean isUsefulLine(String line) {
        for (String usefulLine : MIGRATE_SAMPLE_USEFUL_LINES) {
            if (line.contains(usefulLine)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查看迁移列表(包含历史)
     * @return
     */
    @RequestMapping(value = "/list")
    public Result list(HttpServletRequest request, HttpServletResponse response, Model model, AppDataMigrateSearch appDataMigrateSearch) {
        HashMap<String, Object> data = new HashMap<>(0);
        List<AppDataMigrateStatus> appDataMigrateStatusList = appDataMigrateCenter.search(appDataMigrateSearch);
        data.put("appDataMigrateStatusList", appDataMigrateStatusList);
        data.put("appDataMigrateSearch", appDataMigrateSearch);
        return ResultGenerator.genSuccessResult(data);
    }
    
    /**
     * 通过应用id获取可用的Redis实例信息
     * @return
     */
    @RequestMapping(value = "/appInstanceList")
    public Result appInstanceList(HttpServletRequest request, HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        String appIdStr = request.getParameter("appId");
        long appId = NumberUtils.toLong(appIdStr);
        AppDesc appDesc = appService.getByAppId(appId);
        StringBuffer instances = new StringBuffer();
        List<InstanceInfo> instanceList = appService.getAppInstanceInfo(appId);
        if (CollectionUtils.isNotEmpty(instanceList)) {
            for (int i = 0; i < instanceList.size(); i++) {
                InstanceInfo instanceInfo = instanceList.get(i);
                if (instanceInfo == null) {
                    continue;
                }
                if (instanceInfo.isOffline()) {
                    continue;
                }
                // 如果是sentinel类型的应用只出master
                if (TypeUtil.isRedisSentinel(appDesc.getType())) {
                    if (TypeUtil.isRedisSentinel(instanceInfo.getType())) {
                        continue;
                    }
                    if (!redisCenter.isMaster(appId, instanceInfo.getIp(), instanceInfo.getPort())) {
                        continue;
                    }
                }
                instances.append(instanceInfo.getIp() + ":" + instanceInfo.getPort());
                if (i != instanceList.size() - 1) {
                    instances.append(ConstUtils.NEXT_LINE);
                }
            }
        }
        data.put("instances", instances.toString());
        data.put("appType", appDesc == null ? -1 : appDesc.getType());
        return ResultGenerator.genSuccessResult(data);
    }
    
}
