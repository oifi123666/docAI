package com.javaee.common.model.dto;

import com.javaee.common.constant.CommonConstant;

import java.io.Serializable;

/**
 * @author qxk
 * @description: 分页请求DTO（统一）
 */
public class PageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    // 页码
    private Integer pageNum;

    // 每页大小
    private Integer pageSize;

    // 排序字段
    private String sortField;

    // 排序方式（asc/desc）
    private String sortOrder;

    // 构造方法
    public PageDTO() {
        this.pageNum = CommonConstant.PAGE_NUM;
        this.pageSize = CommonConstant.PAGE_SIZE;
        this.sortOrder = "asc";
    }

    public PageDTO(Integer pageNum, Integer pageSize) {
        this.pageNum = pageNum != null && pageNum > 0 ? pageNum : CommonConstant.PAGE_NUM;
        this.pageSize = pageSize != null && pageSize > 0 ? pageSize : CommonConstant.PAGE_SIZE;
        // 限制最大页面大小
        if (this.pageSize > CommonConstant.MAX_PAGE_SIZE) {
            this.pageSize = CommonConstant.MAX_PAGE_SIZE;
        }
        this.sortOrder = "asc";
    }

    public PageDTO(Integer pageNum, Integer pageSize, String sortField, String sortOrder) {
        this(pageNum, pageSize);
        this.sortField = sortField;
        this.sortOrder = sortOrder != null ? sortOrder : "asc";
    }

    // getter and setter
    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum != null && pageNum > 0 ? pageNum : CommonConstant.PAGE_NUM;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize != null && pageSize > 0 ? pageSize : CommonConstant.PAGE_SIZE;
        // 限制最大页面大小
        if (this.pageSize > CommonConstant.MAX_PAGE_SIZE) {
            this.pageSize = CommonConstant.MAX_PAGE_SIZE;
        }
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder != null ? sortOrder : "asc";
    }

    // toString
    @Override
    public String toString() {
        return "PageDTO{" +
                "pageNum=" + pageNum +
                ", pageSize=" + pageSize +
                ", sortField='" + sortField + '\'' +
                ", sortOrder='" + sortOrder + '\'' +
                '}';
    }

    // 计算偏移量
    public Integer getOffset() {
        return (this.pageNum - 1) * this.pageSize;
    }
}
