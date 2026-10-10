package com.zzx.campusorder.feature.user.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录成功返回对象：访问令牌 + 用户信息。
 */
@Data
public class LoginVO implements Serializable {

    private String token;

    private String tokenType = "Bearer";

    /** 令牌有效期，单位分钟 */
    private Long expireMinutes;

    private UserVO user;
}
