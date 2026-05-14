package com.javaee.user.controller;

import com.javaee.common.model.Result;
import com.javaee.user.dto.LoginDTO;
import com.javaee.user.dto.RegisterDTO;
import com.javaee.user.service.UserService;
import com.javaee.user.vo.LoginVO;
import com.javaee.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author qxk
 * @description: 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户登录、注册、信息管理等接口")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "根据用户名和密码进行登录，返回访问令牌和刷新令牌")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        log.info("用户登录: {}", loginDTO.getUsername());
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }

    /**
     * 用户注册
     * @param registerDTO 注册信息
     * @return 注册结果
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建新用户，返回用户信息")
    public Result<UserVO> register(@RequestBody RegisterDTO registerDTO) {
        log.info("用户注册: {}", registerDTO.getUsername());
        UserVO userVO = userService.register(registerDTO);
        return Result.success(userVO);
    }

    /**
     * 获取用户信息
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户信息", description = "根据用户ID获取用户详细信息")
    public Result<UserVO> getUserById(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("获取用户信息: {}", id);
        UserVO userVO = userService.getUserById(id);
        return Result.success(userVO);
    }

    /**
     * 刷新令牌
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public Result<String> refreshToken(@Parameter(description = "刷新令牌") @RequestParam String refreshToken) {
        log.info("刷新令牌");
        String accessToken = userService.refreshToken(refreshToken);
        return Result.success(accessToken);
    }

    /**
     * 登出
     * @return 登出结果
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出操作")
    public Result<Void> logout() {
        log.info("用户登出");
        // 前端清除本地存储的令牌
        return Result.success();
    }
}
