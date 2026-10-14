package com.zzx.campusorder.feature.menu.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点视图对象。目录与菜单会携带子节点，按钮作为子节点的叶子。
 */
@Data
public class MenuTreeVO implements Serializable {

    private Long id;
    private Long parentId;
    private String menuName;
    private Integer menuType;
    private String perms;
    private String path;
    private String component;
    private String icon;
    private Integer orderNum;

    /** 子节点，无子节点时为空列表而不是 null */
    private List<MenuTreeVO> children = new ArrayList<>();
}
