package com.zzx.campusorder.feature.dish.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 菜品视图对象。浏览菜品时一并返回所属窗口名称与营业状态，减少前端请求次数。
 */
@Data
public class DishVO implements Serializable {

    private Long id;
    private Long windowId;
    private String windowName;
    private Integer windowBusinessStatus;
    private String dishName;
    private String category;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
    private String description;
    private Integer status;

    /** 是否可购买：在售、库存大于 0 且所属窗口营业中 */
    private Boolean purchasable;
}
