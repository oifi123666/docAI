package com.javaee.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.javaee.user.dto.LoginDTO;
import com.javaee.user.dto.RegisterDTO;
import com.javaee.user.entity.User;
import com.javaee.user.vo.LoginVO;
import com.javaee.user.vo.UserVO;

/**
 * @author qxk
 * @description: 用户服务
 */
public interface UserService extends IService<User> {
    /**
     * 用户登录
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 用户注册
     * @param registerDTO 注册信息
     * @return 用户信息
     */
    UserVO register(RegisterDTO registerDTO);

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);

    /**
     * 根据ID查询用户
     * @param id 用户ID
     * @return 用户信息
     */
    UserVO getUserById(Long id);

    /**
     * 刷新令牌
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     */
    String refreshToken(String refreshToken);
}
