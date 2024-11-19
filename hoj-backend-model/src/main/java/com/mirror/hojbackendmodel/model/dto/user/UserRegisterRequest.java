package com.mirror.hojbackendmodel.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 * @author Mirror.
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private String userAccount;

    private String userName;

    private String userPassword;

    private String checkPassword;
}
