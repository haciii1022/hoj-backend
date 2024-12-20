package com.mirror.hojbackendmodel.model.codesandbox;

import lombok.Data;

/**
 * 判题信息
 * @author Mirror
 * @date 2024/6/13
 */
@Data
public class JudgeInfo {
    /**
     * 程序执行信息
     */
    private String message;

    /**
     * 2024/12/9 新增
     * 信息详情
     */
    private String detail;

    /**
     * 2024/12/18新增 得分
     */
    private Integer score;

    /**
     * 消耗时间（KB）
     */
    private Long time;

    /**
     * 消耗内存（KB）
     */
    private Long memory;
}
