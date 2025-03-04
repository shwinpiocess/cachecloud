package com.sohu.cache.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sohu.cache.constant.AppStatusEnum;
import com.sohu.cache.entity.*;
import com.sohu.cache.machine.MachineCenter;
import com.sohu.cache.stats.app.AppStatsCenter;
import com.sohu.cache.stats.instance.InstanceStatsCenter;
import com.sohu.cache.web.core.Result;
import com.sohu.cache.web.core.ResultGenerator;
import com.sohu.cache.web.vo.AppDetailVO;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.sohu.cache.web.enums.SuccessEnum;

/**
 * 全局统计
 *
 * @author leifu
 * @Time 2014年10月14日
 */
@Controller
@RequestMapping("manage/total")
public class TotalManageController extends BaseController {

    @Resource(name = "appStatsCenter")
    private AppStatsCenter appStatsCenter;

    @Resource
    private MachineCenter machineCenter;

    @Resource
    private InstanceStatsCenter instanceStatsCenter;

    @RequestMapping(value = "/list")
    public Result doTotalList(HttpServletRequest request,
                              HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        AppUser currentUser = getUserInfo(request);
        List<AppDesc> apps = appService.getAppDescList(currentUser, new AppSearch());
        List<AppDetailVO> appDetailList = new ArrayList<AppDetailVO>();

        long totalApplyMem = 0;
        long totalUsedMem = 0;
        long totalApps = 0;
        long totalRunningApps = 0;
        if (apps != null && apps.size() > 0) {
            for (AppDesc appDesc : apps) {
                AppDetailVO appDetail = appStatsCenter.getAppDetail(appDesc.getAppId());
                appDetailList.add(appDetail);
                totalApplyMem += appDetail.getMem();
                totalUsedMem += appDetail.getMemUsePercent() * appDetail.getMem() / 100.0;
                if (appDesc.getStatus() == AppStatusEnum.STATUS_PUBLISHED.getStatus()) {
                    totalRunningApps++;
                }
                totalApps++;
            }
        }

        long totalMachineMem = 0;
        long totalFreeMachineMem = 0;
        List<MachineStats> allMachineStats = machineCenter.getAllMachineStats();
        for (MachineStats machineStats : allMachineStats) {
            totalMachineMem += NumberUtils.toLong(machineStats.getMemoryTotal(), 0l);
            totalFreeMachineMem += NumberUtils.toLong(machineStats.getMemoryFree(), 0l);
        }

        long totalInstanceMem = 0;
        long totalUseInstanceMem = 0;
        List<InstanceStats> instanceStats = instanceStatsCenter.getInstanceStats();
        for (InstanceStats instanceStat : instanceStats) {
            totalInstanceMem += instanceStat.getMaxMemory();
            totalUseInstanceMem += instanceStat.getUsedMemory();
        }

        data.put("totalApps", totalApps);
        data.put("totalApplyMem", totalApplyMem);
        data.put("totalUsedMem", totalUsedMem);
        data.put("totalRunningApps", totalRunningApps);

        data.put("totalMachineMem", totalMachineMem);
        data.put("totalFreeMachineMem", totalFreeMachineMem);

        data.put("totalInstanceMem", totalInstanceMem);
        data.put("totalUseInstanceMem", totalUseInstanceMem);

        data.put("apps", apps);
        data.put("appDetailList", appDetailList);
        data.put("list", apps);
        data.put("totalActive", SuccessEnum.SUCCESS.value());
        return ResultGenerator.genSuccessResult(data);
    }

}
