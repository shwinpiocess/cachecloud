package com.sohu.cache.web.controller;

import com.sohu.cache.alert.InstanceAlertService;
import com.sohu.cache.entity.*;
import com.sohu.cache.redis.RedisCenter;
import com.sohu.cache.stats.app.AppStatsCenter;
import com.sohu.cache.stats.instance.InstanceStatsCenter;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.web.core.Result;
import com.sohu.cache.web.core.ResultGenerator;
import com.sohu.cache.web.vo.RedisSlowLog;
import com.sohu.cache.web.chart.key.ChartKeysUtil;
import com.sohu.cache.web.chart.model.SplineChartEntity;
import com.sohu.cache.web.util.DateUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.util.*;

/**
 * Created by hym on 14-7-27.
 */
@Controller
@RequestMapping("/admin/instance")
public class InstanceController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource(name = "instanceStatsCenter")
    private InstanceStatsCenter instanceStatsCenter;

    @Resource(name = "appStatsCenter")
    private AppStatsCenter appStatsCenter;

    @Resource(name = "redisCenter")
    private RedisCenter redisCenter;

    @Resource
    private InstanceAlertService instanceAlertService;

    @RequestMapping("/index")
    public Result index(HttpServletRequest request, HttpServletResponse response, Model model, Integer admin, Long instanceId, Long appId, String tabTag) {
        HashMap<String, Object> data = new HashMap<>(0);
        String startDateParam = request.getParameter("startDate");
        String endDateParam = request.getParameter("endDate");

        if (StringUtils.isBlank(startDateParam) || StringUtils.isBlank(endDateParam)) {
            Date endDate = new Date();
            Date startDate = DateUtils.addDays(endDate, -1);
            startDateParam = DateUtil.formatDate(startDate, "yyyyMMdd");
            endDateParam = DateUtil.formatDate(endDate, "yyyyMMdd");
        }
        data.put("startDate", startDateParam);
        data.put("endDate", endDateParam);

        if (instanceId != null && instanceId > 0) {
            data.put("instanceId", instanceId);
            InstanceInfo instanceInfo = instanceStatsCenter.getInstanceInfo(instanceId);

            if (instanceInfo == null) {
                data.put("type", -1);
            } else {
                if (appId != null && appId > 0) {
                    data.put("appId", appId);
                } else {
                    data.put("appId", instanceInfo.getAppId());
                }
                data.put("type", instanceInfo.getType());
            }
        } else {

        }
        if (tabTag != null) {
            data.put("tabTag", tabTag);
        }
        return ResultGenerator.genSuccessResult(data);
    }

    @RequestMapping("/stat")
    public Result stat(HttpServletRequest request, HttpServletResponse response, Model model, Integer admin, Long instanceId) {
        HashMap<String, Object> data = new HashMap<>(0);
        String startDateParam = request.getParameter("startDate");
        String endDateParam = request.getParameter("endDate");

        if (StringUtils.isBlank(startDateParam) || StringUtils.isBlank(endDateParam)) {
            Date endDate = new Date();
            Date startDate = DateUtils.addDays(endDate, -1);
            startDateParam = DateUtil.formatDate(startDate, "yyyyMMdd");
            endDateParam = DateUtil.formatDate(endDate, "yyyyMMdd");
        }
        data.put("startDate", startDateParam);
        data.put("endDate", endDateParam);

        if (instanceId != null && instanceId > 0) {
            data.put("instanceId", instanceId);
            InstanceInfo instanceInfo = instanceStatsCenter.getInstanceInfo(instanceId);
            data.put("instanceInfo", instanceInfo);
            data.put("appId", instanceInfo.getAppId());
            data.put("appDetail", appStatsCenter.getAppDetail(instanceInfo.getAppId()));
            InstanceStats instanceStats = instanceStatsCenter.getInstanceStats(instanceId);
            data.put("instanceStats", instanceStats);
            List<AppCommandStats> topLimitAppCommandStatsList = appStatsCenter.getTopLimitAppCommandStatsList(instanceInfo.getAppId(), Long.parseLong(startDateParam) * 10000, Long.parseLong(endDateParam) * 10000, 5);
            data.put("appCommandStats", topLimitAppCommandStatsList);
        }
        return ResultGenerator.genSuccessResult(data);
    }

    @RequestMapping("/advancedAnalysis")
    public Result advancedAnalysis(HttpServletRequest request, HttpServletResponse response, Model model, Integer admin, Long instanceId) {
        HashMap<String, Object> data = new HashMap<>(0);
        String startDateParam = request.getParameter("startDate");
        String endDateParam = request.getParameter("endDate");

        if (StringUtils.isBlank(startDateParam) || StringUtils.isBlank(endDateParam)) {
            Date endDate = new Date();
            Date startDate = DateUtils.addDays(endDate, -1);
            startDateParam = DateUtil.formatDate(startDate, "yyyyMMdd");
            endDateParam = DateUtil.formatDate(endDate, "yyyyMMdd");
        }
        data.put("startDate", startDateParam);
        data.put("endDate", endDateParam);

        if (instanceId != null && instanceId > 0) {
            data.put("instanceId", instanceId);
            InstanceInfo instanceInfo = instanceStatsCenter.getInstanceInfo(instanceId);
            data.put("instanceInfo", instanceInfo);
            data.put("appId", instanceInfo.getAppId());
            List<AppCommandStats> topLimitAppCommandStatsList = appStatsCenter.getTopLimitAppCommandStatsList(instanceInfo.getAppId(), Long.parseLong(startDateParam) * 10000, Long.parseLong(endDateParam) * 10000, 5);
            data.put("appCommandStats", topLimitAppCommandStatsList);
        } else {

        }
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 获取某个命令时间分布图
     *
     * @param instanceId  实例id
     * @param commandName 命令名称
     * @throws java.text.ParseException
     */
    @RequestMapping("/getCommandStats")
    public Result getCommandStats(HttpServletRequest request,
                                        HttpServletResponse response, Model model, Long instanceId,
                                        String commandName) throws ParseException {
        HashMap<String, Object> data = new HashMap<>(0);
        String startDateParam = request.getParameter("startDate");
        String endDateParam = request.getParameter("endDate");

        if (StringUtils.isBlank(startDateParam) || StringUtils.isBlank(endDateParam)) {
            Date endDate = new Date();
            Date startDate = DateUtils.addDays(endDate, -1);
            startDateParam = DateUtil.formatDate(startDate, "yyyyMMdd");
            endDateParam = DateUtil.formatDate(endDate, "yyyyMMdd");
        }
        data.put("startDate", startDateParam);
        data.put("endDate", endDateParam);

        Date startDate = DateUtil.parseYYYYMMdd(startDateParam);
        Date endDate = DateUtil.parseYYYYMMdd(endDateParam);
        if (instanceId != null) {
            long firstDayBegin = NumberUtils.toLong(DateUtil.formatYYYYMMdd(startDate) + "0000");
            long firstDayEnd = NumberUtils.toLong(DateUtil.formatYYYYMMdd(startDate) + "2359");
            long secondDayBegin = NumberUtils.toLong(DateUtil.formatYYYYMMdd(endDate) + "0000");
            long secondDayEnd = NumberUtils.toLong(DateUtil.formatYYYYMMdd(endDate) + "2359");
            long bt = System.currentTimeMillis();
            List<InstanceCommandStats> instanceCommandStatsListFirst = instanceStatsCenter
                    .getCommandStatsList(instanceId, firstDayBegin, firstDayEnd, commandName);
            List<InstanceCommandStats> instanceCommandStatsListSecond = instanceStatsCenter
                    .getCommandStatsList(instanceId, secondDayBegin, secondDayEnd, commandName);
            long et = System.currentTimeMillis() - bt;
            Map<String, InstanceCommandStats> cmdStatsFirst = new HashMap<String, InstanceCommandStats>();
            Map<String, InstanceCommandStats> cmdStatsSecond = new HashMap<String, InstanceCommandStats>();

            for (InstanceCommandStats first : instanceCommandStatsListFirst) {
                cmdStatsFirst.put(first.getCollectTime() + "", first);
            }
            for (InstanceCommandStats second : instanceCommandStatsListSecond) {
                cmdStatsSecond.put(second.getCollectTime() + "", second);
            }

            SplineChartEntity splineChartEntity = new SplineChartEntity();
            String container = request.getParameter("container");
            if (container != null) {
                splineChartEntity.renderTo(container);
            }
            data.put("chart", splineChartEntity);
            splineChartEntity.putTitle(ChartKeysUtil.TitleKey.TEXT.getKey(), "命令:" + commandName + " 的比较曲线【" + startDateParam + "】-【" + endDateParam + "】");
            splineChartEntity.setYAxisTitle("y");
            List<Long> data1 = new ArrayList<Long>();
            List<Long> data2 = new ArrayList<Long>();
            Map<String, Object> serie1 = new HashMap<String, Object>();
            serie1.put("name", startDateParam);
            serie1.put("data", data1);
//            serie1.put("type", "area");
            Map<String, Object> serie2 = new HashMap<String, Object>();
            serie2.put("name", endDateParam);
            serie2.put("data", data2);
//            serie2.put("type", "area");
            splineChartEntity.putSeries(serie1);
            splineChartEntity.putSeries(serie2);
            List<Object> x = new LinkedList<Object>();
            for (int i = 0; i < 1440; i += 1) {
                Date date = DateUtils.addMinutes(startDate, i);
                String s = DateUtil.formatHHMM(date);
                if (cmdStatsFirst.containsKey(startDateParam + s)) {
                    data1.add(cmdStatsFirst.get(startDateParam + s).getCommandCount());
                } else {
                    data1.add(0l);
                }
                if (cmdStatsSecond.containsKey(endDateParam + s)) {
                    data2.add(cmdStatsSecond.get(endDateParam + s).getCommandCount());
                } else {
                    data2.add(0l);
                }

                x.add(s);
            }
            splineChartEntity.setXAxisCategories(x);
        }
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 获取某个命令时间分布图
     *
     * @param instanceId  实例id
     * @param commandName 命令名称
     * @throws java.text.ParseException
     */
    @RequestMapping("/getCommandStatsV2")
    public Result getCommandStatsV2(HttpServletRequest request,
                                          HttpServletResponse response, Model model, Long instanceId,
                                          String commandName) throws ParseException {
        HashMap<String, Object> data = new HashMap<>(0);
        String startDateParam = request.getParameter("startDate");
        String endDateParam = request.getParameter("endDate");

        if (StringUtils.isBlank(startDateParam) || StringUtils.isBlank(endDateParam)) {
            Date endDate = new Date();
            Date startDate = DateUtils.addDays(endDate, -1);
            startDateParam = DateUtil.formatDate(startDate, "yyyyMMdd");
            endDateParam = DateUtil.formatDate(endDate, "yyyyMMdd");
        }
        data.put("startDate", startDateParam);
        data.put("endDate", endDateParam);

        Date startDate = DateUtil.parseYYYYMMdd(startDateParam);
        Date endDate = DateUtil.parseYYYYMMdd(endDateParam);
        if (instanceId != null) {
            long firstDayBegin = NumberUtils.toLong(DateUtil.formatYYYYMMdd(startDate) + "0000");
            long firstDayEnd = NumberUtils.toLong(DateUtil.formatYYYYMMdd(startDate) + "2359");
            long secondDayBegin = NumberUtils.toLong(DateUtil.formatYYYYMMdd(endDate) + "0000");
            long secondDayEnd = NumberUtils.toLong(DateUtil.formatYYYYMMdd(endDate) + "2359");
            long bt = System.currentTimeMillis();
            List<InstanceCommandStats> instanceCommandStatsListFirst = instanceStatsCenter
                    .getCommandStatsList(instanceId, firstDayBegin, firstDayEnd, commandName);
            List<InstanceCommandStats> instanceCommandStatsListSecond = instanceStatsCenter
                    .getCommandStatsList(instanceId, secondDayBegin, secondDayEnd, commandName);
            long et = System.currentTimeMillis() - bt;
            Map<String, InstanceCommandStats> cmdStatsFirst = new HashMap<String, InstanceCommandStats>();
            Map<String, InstanceCommandStats> cmdStatsSecond = new HashMap<String, InstanceCommandStats>();

            for (InstanceCommandStats first : instanceCommandStatsListFirst) {
                cmdStatsFirst.put(first.getCollectTime() + "", first);
            }
            for (InstanceCommandStats second : instanceCommandStatsListSecond) {
                cmdStatsSecond.put(second.getCollectTime() + "", second);
            }

            SplineChartEntity splineChartEntity = new SplineChartEntity();
            String container = request.getParameter("container");
            if (container != null) {
                splineChartEntity.renderTo(container);
            }
            data.put("chart", splineChartEntity);
            splineChartEntity.putTitle(ChartKeysUtil.TitleKey.TEXT.getKey(), "命令:" + commandName + " 的比较曲线【" + startDateParam + "】-【" + endDateParam + "】");
            splineChartEntity.setYAxisTitle("y");
            List<Long> data1 = new ArrayList<Long>();
            List<Long> data2 = new ArrayList<Long>();
            Map<String, Object> marker = new HashMap<String, Object>();
            marker.put("radius", 1);
            Map<String, Object> serie1 = new HashMap<String, Object>();
            serie1.put("name", startDateParam);
            serie1.put("data", data1);
            serie1.put("marker", marker);
            Map<String, Object> serie2 = new HashMap<String, Object>();
            serie2.put("name", endDateParam);
            serie2.put("data", data2);
            serie2.put("marker", marker);
            splineChartEntity.putSeries(serie1);
            splineChartEntity.putSeries(serie2);
            List<Object> x = new LinkedList<Object>();
            for (int i = 0; i < 1440; i += 1) {
                Date date = DateUtils.addMinutes(startDate, i);
                String s = DateUtil.formatHHMM(date);
                if (cmdStatsFirst.containsKey(startDateParam + s)) {
                    data1.add(cmdStatsFirst.get(startDateParam + s).getCommandCount());
                } else {
                    data1.add(0l);
                }
                if (cmdStatsSecond.containsKey(endDateParam + s)) {
                    data2.add(cmdStatsSecond.get(endDateParam + s).getCommandCount());
                } else {
                    data2.add(0l);
                }

                x.add(s);
            }
            splineChartEntity.setXAxisCategories(x);
        }
        return ResultGenerator.genSuccessResult(data);
    }

    @RequestMapping("/fault")
    public Result fault(HttpServletRequest request, HttpServletResponse response, Model model, Integer admin, Integer instanceId, Long appId) {
        HashMap<String, Object> data = new HashMap<>(0);
        //String startDateParam = request.getParameter("startDate");
        //String endDateParam = request.getParameter("endDate");
        List<InstanceFault> list = null;
        try {
            list = instanceAlertService.getListByInstId(instanceId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        if (list == null) {
            list = new ArrayList<InstanceFault>();
        }
        data.put("list", list);
        return ResultGenerator.genSuccessResult(data);
    }

    @RequestMapping("/configSelect")
    public Result configSelect(HttpServletRequest request, HttpServletResponse response, Model model, Integer admin, Long instanceId, Long appId) {
        HashMap<String, Object> data = new HashMap<>(0);
        if (instanceId != null && instanceId > 0) {
            data.put("instanceId", instanceId);
            Map<String, String> redisConfigList = redisCenter.getRedisConfigList(instanceId.intValue());
            data.put("redisConfigList", redisConfigList);
        }
        if (appId != null && appId > 0) {
            data.put("appId", appId);
        }
        return ResultGenerator.genSuccessResult(data);
    }

    @RequestMapping("/slowSelect")
    public Result slowSelect(HttpServletRequest request, HttpServletResponse response, Model model, Integer admin, Long instanceId) {
        HashMap<String, Object> data = new HashMap<>(0);
        if (instanceId != null && instanceId > 0) {
            data.put("instanceId", instanceId);
            List<RedisSlowLog> redisSlowLogs = redisCenter.getRedisSlowLogs(instanceId.intValue(), -1);
            data.put("redisSlowLogs", redisSlowLogs);
        }
        return ResultGenerator.genSuccessResult(data);
    }

    @RequestMapping("/clientList")
    public Result clientList(HttpServletRequest request, HttpServletResponse response, Model model, Integer admin, Long instanceId) {
        HashMap<String, Object> data = new HashMap<>(0);
        if (instanceId != null && instanceId > 0) {
            data.put("instanceId", instanceId);
            List<String> clientList = redisCenter.getClientList(instanceId.intValue());
            data.put("clientList", clientList);
        }
        return ResultGenerator.genSuccessResult(data);
    }

    @RequestMapping("/command")
    public Result command(HttpServletRequest request, HttpServletResponse response, Model model, Integer admin, Long instanceId, Long appId) {
        HashMap<String, Object> data = new HashMap<>(0);
        if (instanceId != null && instanceId > 0) {
            data.put("instanceId", instanceId);
        }
        return ResultGenerator.genSuccessResult(data);
    }

    @RequestMapping("/commandExecute")
    public Result commandExecute(HttpServletRequest request, HttpServletResponse response, Model model, Integer admin, Long instanceId, Long appId) {
        HashMap<String, Object> data = new HashMap<>(0);
        if (instanceId != null && instanceId > 0) {
            data.put("instanceId", instanceId);
            String command = request.getParameter("command");
            String result = instanceStatsCenter.executeCommand(instanceId, command);
            data.put("result", result);
        } else {
            data.put("result", "error");
        }
        return ResultGenerator.genSuccessResult(data);
    }

}