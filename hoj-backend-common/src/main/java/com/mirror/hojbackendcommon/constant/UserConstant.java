package com.mirror.hojbackendcommon.constant;

/**
 * 用户常量
 * @author Mirror.
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 超级管理员角色
     */
    String ROOT = "root";

    /**
     * 被封号
     */
    String BAN_ROLE = "ban";

    /**
     * 默认初始密码
     */
    String DEFAULT_PASSWORD = "12345678";

}
