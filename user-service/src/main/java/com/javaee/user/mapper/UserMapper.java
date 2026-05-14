package com.javaee.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javaee.user.entity.User;
import org.apache.ibatis.annotations.Select;

/**
 * @author qxk
 * @description: 用户Mapper
 */
public interface UserMapper extends BaseMapper<User> {
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User selectByUsername(String username);

    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户信息
     */
    @Select("SELECT * FROM user WHERE email = #{email}")
    User selectByEmail(String email);

    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户信息
     */
    @Select("SELECT * FROM user WHERE phone = #{phone}")
    User selectByPhone(String phone);
}
