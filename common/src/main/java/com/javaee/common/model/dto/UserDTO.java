package com.javaee.common.model.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author qxk
 * @description: 用户DTO（数据传输对象）
 */
public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    // 用户ID
    private Long id;

    // 用户名
    private String username;

    // 密码
    private String password;

    // 确认密码
    private String confirmPassword;

    // 邮箱
    private String email;

    // 手机号
    private String phone;

    // 真实姓名
    private String realName;

    // 性别（0-未知，1-男，2-女）
    private Integer gender;

    // 年龄
    private Integer age;

    // 头像
    private String avatar;

    // 角色ID列表
    private List<Long> roleIds;

    // 权限ID列表
    private List<Long> permissionIds;

    // 创建时间
    private Date createTime;

    // 更新时间
    private Date updateTime;

    // 最后登录时间
    private Date lastLoginTime;

    // 状态（0-禁用，1-启用）
    private Integer status;

    // 备注
    private String remark;

    // getter and setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }

    public List<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<Long> permissionIds) {
        this.permissionIds = permissionIds;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Date getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(Date lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    // toString
    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", realName='" + realName + '\'' +
                ", gender=" + gender +
                ", age=" + age +
                ", avatar='" + avatar + '\'' +
                ", roleIds=" + roleIds +
                ", permissionIds=" + permissionIds +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", lastLoginTime=" + lastLoginTime +
                ", status=" + status +
                ", remark='" + remark + '\'' +
                '}';
    }
}

