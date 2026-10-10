package com.zzx.campusorder.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装。
 *
 * @param <T> 记录类型
 */
@Data
public class PageResult<T> implements Serializable {

    /** 满足条件的总记录数 */
    private long total;

    /** 当前页码，从 1 开始 */
    private long pageNum;

    /** 每页记录数 */
    private long pageSize;

    /** 当前页记录，可能为空列表 */
    private List<T> records;

    public PageResult(long total, long pageNum, long pageSize, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.records = records == null ? List.of() : records;
    }
}
