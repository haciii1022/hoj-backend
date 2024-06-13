package com.mirror.hoj.model.dto.questionSubmit;

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
     * 消耗时间（KB）
     */
    private Long time;

    /**
     * 消耗内存（KB）
     */
    private Long memory;
}
