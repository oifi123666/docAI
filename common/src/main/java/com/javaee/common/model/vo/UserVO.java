package com.javaee.common.model.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author qxk
 * @description: 用户VO（展示）
 */
public class UserVO implements Serializable {
    private static final long serialVersionUID = 1L;

    // 用户ID
    private Long id;

    // 用户名
    private String username;

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

    // 角色列表
    private List<String> roles;

    // 权限列表
    private List<String> permissions;

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

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
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
        return "UserVO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", realName='" + realName + '\'' +
                ", gender=" + gender +
                ", age=" + age +
                ", avatar='" + avatar + '\'' +
                ", roles=" + roles +
                ", permissions=" + permissions +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", lastLoginTime=" + lastLoginTime +
                ", status=" + status +
                ", remark='" + remark + '\'' +
                '}';
    }
}

