package com.sohu.cache.web.controller;

import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sohu.cache.entity.TriggerInfo;
import com.sohu.cache.schedule.SchedulerCenter;
import com.sohu.cache.web.core.Result;
import com.sohu.cache.web.core.ResultGenerator;
import org.apache.commons.lang.StringUtils;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.sohu.cache.web.enums.SuccessEnum;

/**
 * quartz管理test
 * 
 * @author leifu
 * @Time 2014年7月4日
 */
@Controller
@RequestMapping("manage/quartz")
public class QuartzManageController extends BaseController {

    @Resource
    private SchedulerCenter schedulerCenter;

    @RequestMapping(value = "/list")
    public Result doQuartzList(HttpServletRequest request,
                               HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        String query = request.getParameter("query");
        List<TriggerInfo> triggerList;
        if (StringUtils.isBlank(query)) {
            triggerList = schedulerCenter.getAllTriggers();
            query = "";
        } else {
            triggerList = schedulerCenter.getTriggersByNameOrGroup(query);
        }
        data.put("triggerList", triggerList);
        data.put("quartzActive", SuccessEnum.SUCCESS.value());
        data.put("query", query);
        return ResultGenerator.genSuccessResult(data);
    }

    @RequestMapping(value = "/pause")
    public Result pause(HttpServletRequest request,
                                     HttpServletResponse response, Model model) {
        String name = request.getParameter("name");
        String group = request.getParameter("group");
        if (StringUtils.isNotBlank(name) || StringUtils.isNotBlank(group)) {
            schedulerCenter.pauseTrigger(new TriggerKey(name, group));
        }
        return ResultGenerator.genSuccessResult();
    }

    @RequestMapping(value = "/resume")
    public Result resume(HttpServletRequest request,
                        HttpServletResponse response, Model model) {
        String name = request.getParameter("name");
        String group = request.getParameter("group");
        if (StringUtils.isNotBlank(name) || StringUtils.isNotBlank(group)) {
            schedulerCenter.resumeTrigger(new TriggerKey(name, group));
        }
        return ResultGenerator.genSuccessResult();
    }

    @RequestMapping(value = "/remove")
    public Result remove(HttpServletRequest request,
                         HttpServletResponse response, Model model) {
        String name = request.getParameter("name");
        String group = request.getParameter("group");
        if (StringUtils.isNotBlank(name) || StringUtils.isNotBlank(group)) {
            schedulerCenter.unscheduleJob(new TriggerKey(name, group));
        }
        return ResultGenerator.genSuccessResult();
    }

}
