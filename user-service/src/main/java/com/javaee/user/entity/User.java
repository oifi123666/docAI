package com.javaee.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * @author qxk
 * @description: 用户实体类
 */
@Data
@TableName("user")
public class User {
    // 用户ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户名
    private String username;

    // 密码
    private String password;

    // 邮箱
    private String email;

    // 手机号
    private String phone;

    // 角色
    private String role;

    // 状态（0:禁用, 1:启用）
    private Integer status;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}
