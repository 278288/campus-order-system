package com.zzx.campusorder.feature.dish.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品实体，对应表 dish。
 */
@Data
@TableName("dish")
public class Dish implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long windowId;

    private String dishName;

    /** 分类：主食、荤菜、素菜、汤品、饮品、其他 */
    private String category;

    /** 单价，单位元，必须大于 0 */
    private BigDecimal price;

    /** 可售库存，非负整数 */
    private Integer stock;

    private String imageUrl;

    private String description;

    /** 1在售，0已下架 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
