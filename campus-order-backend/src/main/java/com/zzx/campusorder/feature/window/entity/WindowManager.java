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
 * 窗口管理员实体，对应表 window_manager（用户与窗口的多对多中间表）。
 */
@Data
@TableName("window_manager")
public class WindowManager implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long windowId;

    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
