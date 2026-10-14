package com.zzx.campusorder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 校园点餐系统后端启动类。
 *
 * @author 郑志雄 2520145325
 */
@SpringBootApplication
@MapperScan({
        "com.zzx.campusorder.feature.user.mapper",
        "com.zzx.campusorder.feature.window.mapper",
        "com.zzx.campusorder.feature.dish.mapper",
        "com.zzx.campusorder.feature.order.mapper",
        "com.zzx.campusorder.feature.stats.mapper",
        "com.zzx.campusorder.feature.menu.mapper"
})
public class CampusOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusOrderApplication.class, args);
    }
}
