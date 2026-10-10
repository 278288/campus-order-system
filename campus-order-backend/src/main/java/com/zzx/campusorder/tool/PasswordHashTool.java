package com.zzx.campusorder.tool;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * 生成测试账号密码的 BCrypt 哈希，用于填充建表脚本中的测试数据。
 * 运行方式：mvn -q compile exec:java -Dexec.mainClass=com.zzx.campusorder.tool.PasswordHashTool -Dexec.args=123456
 */
public final class PasswordHashTool {

    public static void main(String[] args) {
        String raw = args.length > 0 ? args[0] : "123456";
        String hash = BCrypt.withDefaults().hashToString(10, raw.toCharArray());
        boolean ok = BCrypt.verifyer().verify(raw.toCharArray(), hash).verified;
        System.out.println("raw  = " + raw);
        System.out.println("hash = " + hash);
        System.out.println("verify = " + ok);
    }
}
