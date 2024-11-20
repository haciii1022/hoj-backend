package com.mirror.hojbackendmodel.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Mirror
 * @date 2024/11/19
 */
@RequiredArgsConstructor
@Getter
public enum BaseSequenceEnum {
    /**
     * 定时任务
     */
    USER_ID("USER", "ID", "USER_ID"),
    QUESTION_ID("QUESTION", "ID", "QUESTION_ID"),
    QUESTION_SUBMIT_ID("QUESTION_SUBMIT", "ID", "QUESTION_SUBMIT_ID"),
    JUDGE_CASE_GROUP_ID("JUDGE_CASE_GROUP", "ID", "JUDGE_CASE_GROUP_ID"),
    JUDGE_CASE_FILE_ID("JUDGE_CASE_FILE", "ID", "JUDGE_CASE_FILE_ID");
    /**
     * 表名
     */
    private final String table;

    /**
     * 列名
     */
    private final String column;

    /**
     * 序列名
     */
    private final String name;
}
