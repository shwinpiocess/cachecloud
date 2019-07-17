package com.sohu.cache.web.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sohu.cache.web.core.Result;
import com.sohu.cache.web.core.ResultGenerator;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.web.enums.SuccessEnum;
import com.sohu.cache.web.util.AppEmailUtil;

@Controller
@RequestMapping("manage/notice")
public class NoticeManageController extends BaseController {
    
    @Resource(name = "appEmailUtil")
    private AppEmailUtil appEmailUtil;

    /**
     * 初始化系统通知
     * 
     * @return
     */
    @RequestMapping(value = "/initNotice")
    public Result init(HttpServletRequest request,
                       HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        String notice = "";
        data.put("notice", notice);
        data.put("success", request.getParameter("success"));
        data.put("noticeActive", SuccessEnum.SUCCESS.value());
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 发送邮件通知
     */
    @RequestMapping(value = "/add")
    public Result addNotice(HttpServletRequest request,
            HttpServletResponse response, Model model) {

        String notice = request.getParameter("notice");
        boolean result = appEmailUtil.noticeAllUser(notice);
        SuccessEnum successEnum = result ? SuccessEnum.SUCCESS : SuccessEnum.FAIL;
        if (successEnum == SuccessEnum.SUCCESS) {
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult(String.valueOf(SuccessEnum.FAIL.value()));
        }
    }

    /**
     * 获取系统通知
     * 
     * @return
     */
    @RequestMapping(value = "/get")
    public Result getNotice(HttpServletRequest request,
            HttpServletResponse response, Model model) {
        HashMap<String, Object> data = new HashMap<>(0);
        String notice = "";
        List<String> list = null;
        if (StringUtils.isNotBlank(notice)) {
            list = Arrays.asList(notice.split(ConstUtils.NEXT_LINE));
            data.put("status", SuccessEnum.SUCCESS.value());
        } else {
            list = new ArrayList<String>();
            data.put("status", SuccessEnum.FAIL.value());
        }
        data.put("data", list);
        return ResultGenerator.genSuccessResult(data);
    }

}
