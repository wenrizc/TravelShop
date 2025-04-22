package com.travelshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelshop.dto.LoginFormDTO;
import com.travelshop.dto.Result;
import com.travelshop.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IUserService extends IService<User> {

    /**
     * 发送验证码
     *
     * @param phone   手机号码
     * @param session 会话
     * @return {@link Result}
     */
    Result sendCode(String phone, HttpSession session);

    /**
     * 登录
     *
     * @param loginForm 登录表单
     * @param session   会话
     * @return {@link Result}
     */
    Result login(LoginFormDTO loginForm, HttpSession session);

    /**
     * 签到
     *
     * @return {@link Result}
     */
    Result sign();

    /**
     * 统计连续签到
     *
     * @return {@link Result}
     */
    Result signCount();

    /**
     * 登出
     *
     * @return {@link Result}
     */
    Result logout(HttpServletRequest request);

    /**
     * 刷新访问令牌
     * @param refreshToken 刷新令牌
     * @return Result
     */
    Result refreshToken(String refreshToken);
}
