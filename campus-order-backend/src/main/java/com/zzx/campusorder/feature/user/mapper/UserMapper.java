package com.zzx.campusorder.feature.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzx.campusorder.feature.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户持久层。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
