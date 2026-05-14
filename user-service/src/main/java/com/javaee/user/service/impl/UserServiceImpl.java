package com.javaee.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.javaee.common.constant.ErrorCodeEnum;
import com.javaee.common.exception.BusinessException;
import com.javaee.common.utils.JwtUtils;
import com.javaee.common.utils.ValidateUtils;
import com.javaee.user.dto.LoginDTO;
import com.javaee.user.dto.RegisterDTO;
import com.javaee.user.entity.User;
import com.javaee.user.mapper.UserMapper;
import com.javaee.user.service.UserService;
import com.javaee.user.vo.LoginVO;
import com.javaee.user.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/**
 * @author qxk
 * @description: 用户服务实现
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 校验参数
        ValidateUtils.notEmpty(loginDTO.getUsername(), "用户名不能为空");
        ValidateUtils.notEmpty(loginDTO.getPassword(), "密码不能为空");

        // 查询用户
        User user = userMapper.selectByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        // 校验状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_ERROR);
        }

        // 校验密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCodeEnum.PASSWORD_ERROR);
        }

        // 生成令牌
        String accessToken = JwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = JwtUtils.generateRefreshToken(user.getId());

        // 构建响应
        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(accessToken);
        loginVO.setRefreshToken(refreshToken);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        loginVO.setUser(userVO);

        return loginVO;
    }

    @Override
    public UserVO register(RegisterDTO registerDTO) {
        // 校验参数
        ValidateUtils.notEmpty(registerDTO.getUsername(), "用户名不能为空");
        ValidateUtils.notEmpty(registerDTO.getPassword(), "密码不能为空");
        ValidateUtils.email(registerDTO.getEmail(), "邮箱格式不正确");
        ValidateUtils.phone(registerDTO.getPhone(), "手机号格式不正确");

        // 校验用户是否已存在
        if (userMapper.selectByUsername(registerDTO.getUsername()) != null) {
            throw new BusinessException(ErrorCodeEnum.USER_EXISTED);
        }
        if (userMapper.selectByEmail(registerDTO.getEmail()) != null) {
            throw new BusinessException("邮箱已被注册");
        }
        if (userMapper.selectByPhone(registerDTO.getPhone()) != null) {
            throw new BusinessException("手机号已被注册");
        }

        // 创建用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setPhone(registerDTO.getPhone());
        user.setRole("USER"); // 默认角色
        user.setStatus(1); // 默认启用
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 保存用户，直接调用 Mybatis-Plus 的 save 方法
        save(user);

        // 构建响应
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public User getUserByUsername(String username) {
        ValidateUtils.notEmpty(username, "用户名不能为空");
        return userMapper.selectByUsername(username);
    }

    @Override
    public UserVO getUserById(Long id) {
        ValidateUtils.notNull(id, "用户ID不能为空");
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public String refreshToken(String refreshToken) {
        ValidateUtils.notEmpty(refreshToken, "刷新令牌不能为空");
        try {
            // 解析刷新令牌
            Long userId = JwtUtils.getUserId(refreshToken);
            User user = getById(userId);
            if (user == null) {
                throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
            }
            // 生成新的访问令牌
            return JwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.TOKEN_ERROR);
        }
    }
}
