package com.zzx.campusorder.feature.window.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 食堂窗口实体，对应表 canteen_window。
 */
@Data
@TableName("canteen_window")
public class CanteenWindow implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String windowName;

    private String location;

    /** 1营业中，0已打烊；打烊后不允许提交新订单，不影响已有订单履约 */
    private Integer businessStatus;

    /** 1有效，0停用；存在历史订单时采用停用而不是物理删除 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
