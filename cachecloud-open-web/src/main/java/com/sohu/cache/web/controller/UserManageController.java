package com.sohu.cache.web.controller;

import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sohu.cache.web.core.Result;
import com.sohu.cache.web.core.ResultGenerator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.sohu.cache.constant.AppCheckEnum;
import com.sohu.cache.constant.AppUserTypeEnum;
import com.sohu.cache.entity.AppAudit;
import com.sohu.cache.entity.AppUser;
import com.sohu.cache.web.enums.SuccessEnum;
import com.sohu.cache.web.util.AppEmailUtil;

/**
 * 用户信息管理
 * @author leifu
 * @Time 2014年6月6日
 */
@Controller
@RequestMapping("manage/user")
public class UserManageController extends BaseController {
    
    @Resource(name = "appEmailUtil")
    private AppEmailUtil appEmailUtil;

    /**
     * 用户初始化
     * @param id 用户id
     * @return
     */
    @RequestMapping(value = "/init")
    public Result doUserInit(HttpServletRequest request,
                             HttpServletResponse response, Model model, Long id) {
        HashMap<String, Object> data = new HashMap<>(0);
        if (id != null) {
            AppUser user = userService.get(id);
            data.put("user", user);
            data.put("modify", true);
        }
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 更新用户
     * @param name
     * @param chName
     * @param email
     * @param mobile
     * @param type
     * @param userId
     * @return
     */
    @RequestMapping(value = "/add")
    public Result doAddUser(HttpServletRequest request,
            HttpServletResponse response, Model model, String name, String chName, String email, String mobile,
            Integer type, Long userId) {
        // 后台暂时不对参数进行验证
        AppUser appUser = AppUser.buildFrom(userId, name, chName, email, mobile, type);
        try {
			if (userId == null) {
			    userService.save(appUser);
			} else {
			    userService.update(appUser);
			}
	        return ResultGenerator.genSuccessResult();
		} catch (Exception e) {
	        logger.error(e.getMessage(), e);
            return ResultGenerator.genFailResult(String.valueOf(SuccessEnum.FAIL.value()));
		}
    }

    /**
     * 删除用户
     * @param userId
     * @return
     */
    @RequestMapping(value = "/delete")
    public Result doDeleteUser(HttpServletRequest request,
            HttpServletResponse response, Model model, Long userId) {
        userService.delete(userId);
        return ResultGenerator.genSuccessResult();
    }

    /**
     * 用户列表
     * @return
     */
    @RequestMapping(value = "/list")
    public Result doUserList(HttpServletRequest request,
            HttpServletResponse response, Model model, String searchChName) {
        HashMap<String, Object> data = new HashMap<>(0);
        List<AppUser> users = userService.getUserList(searchChName);
        data.put("users", users);
        data.put("searchChName", searchChName);
        data.put("userActive", SuccessEnum.SUCCESS.value());
        return ResultGenerator.genSuccessResult(data);
    }
    
    @RequestMapping(value = "/addAuditStatus")
    public Result doAddAuditStatus(HttpServletRequest request,
            HttpServletResponse response, Model model, Integer status,
            Long appAuditId, String refuseReason) {
        AppAudit appAudit = appService.getAppAuditById(appAuditId);
        AppUser appUser = userService.get(appAudit.getUserId());
        // 通过或者驳回并记录日志
        appService.updateUserAuditStatus(appAuditId, status);

        // 记录驳回原因
        if (AppCheckEnum.APP_REJECT.value().equals(status)) {
            appAudit.setRefuseReason(refuseReason);
            appService.updateRefuseReason(appAudit, getUserInfo(request));
            userService.delete(appUser.getId());
        }

        // 发邮件统计
        if (AppCheckEnum.APP_PASS.value().equals(status)
                || AppCheckEnum.APP_REJECT.value().equals(status)) {
            appUser.setType(AppUserTypeEnum.REGULAR_USER.value());
            appAudit.setStatus(status);
            userService.update(appUser);
            appEmailUtil.noticeUserResult(appUser, appAudit);
        }

        // 批准成功直接跳转
        if (AppCheckEnum.APP_PASS.value().equals(status)) {
            return ResultGenerator.genSuccessResult();
        }

        return ResultGenerator.genSuccessResult();
    }

}
