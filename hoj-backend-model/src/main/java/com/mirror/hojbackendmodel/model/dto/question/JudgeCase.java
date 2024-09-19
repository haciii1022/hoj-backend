package com.mirror.hojbackendmodel.model.dto.question;

import lombok.Data;

/**
 * 题目用例
 * @author Mirror
 * @date 2024/6/13
 */
@Data
public class JudgeCase {
    /**
     * 输入用例
     */
    private String input;

    /**
     * 输出用例
     */
    private String output;
}
