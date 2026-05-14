package com.javaee.common.model.vo;

import java.io.Serializable;
import java.util.List;

/**
 * @author qxk
 * @description: 分页响应VO（统一）
 */
public class PageVO<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    // 数据列表
    private List<T> list;

    // 总记录数
    private Long total;

    // 页码
    private Integer pageNum;

    // 每页大小
    private Integer pageSize;

    // 总页数
    private Integer pages;

    // 是否有上一页
    private Boolean hasPreviousPage;

    // 是否有下一页
    private Boolean hasNextPage;

    // 构造方法
    public PageVO() {
    }

    public PageVO(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = (int) Math.ceil((double) total / pageSize);
        this.hasPreviousPage = pageNum > 1;
        this.hasNextPage = pageNum < this.pages;
    }

    // getter and setter
    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
        if (pageSize != null && pageSize > 0) {
            this.pages = (int) Math.ceil((double) total / pageSize);
        }
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
        if (pages != null) {
            this.hasPreviousPage = pageNum > 1;
            this.hasNextPage = pageNum < this.pages;
        }
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        if (total != null && total > 0) {
            this.pages = (int) Math.ceil((double) total / pageSize);
        }
        if (pageNum != null && pages != null) {
            this.hasPreviousPage = pageNum > 1;
            this.hasNextPage = pageNum < this.pages;
        }
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
        if (pageNum != null) {
            this.hasPreviousPage = pageNum > 1;
            this.hasNextPage = pageNum < pages;
        }
    }

    public Boolean getHasPreviousPage() {
        return hasPreviousPage;
    }

    public void setHasPreviousPage(Boolean hasPreviousPage) {
        this.hasPreviousPage = hasPreviousPage;
    }

    public Boolean getHasNextPage() {
        return hasNextPage;
    }

    public void setHasNextPage(Boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }

    // toString
    @Override
    public String toString() {
        return "PageVO{" +
                "list=" + list +
                ", total=" + total +
                ", pageNum=" + pageNum +
                ", pageSize=" + pageSize +
                ", pages=" + pages +
                ", hasPreviousPage=" + hasPreviousPage +
                ", hasNextPage=" + hasNextPage +
                '}';
    }

    // 静态方法
    /**
     * 构建分页响应
     * @param list 数据列表
     * @param total 总记录数
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param <T> 数据类型
     * @return PageVO
     */
    public static <T> PageVO<T> build(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        return new PageVO<>(list, total, pageNum, pageSize);
    }
}

