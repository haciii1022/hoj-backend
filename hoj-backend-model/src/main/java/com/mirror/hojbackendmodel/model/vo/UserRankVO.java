package com.mirror.hojbackendmodel.model.vo;


import lombok.Builder;
import lombok.Data;

/**
 * 用户排行榜数据
 * @author Mirror.
 * @date 2025/3/5
 */
@Data
@Builder
public class UserRankVO {
    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 通过题目数量
     */
    private Integer solvedNum;

    /**
     * 总AC数
     */
    private Integer acceptedNum;

    /**
     * 提交数
     */
    private Integer submitNum;
}
