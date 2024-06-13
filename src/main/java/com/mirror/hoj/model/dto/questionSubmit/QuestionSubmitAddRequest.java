package com.mirror.hoj.model.dto.questionSubmit;

import lombok.Data;

import java.io.Serializable;

/**
 * 帖子点赞请求
 * @author Mirror.
 */
@Data
public class QuestionSubmitAddRequest implements Serializable {

    /**
     * 编程语言
     */
    private String language;

    /**
     * 用户代码
     */
    private String code;

    /**
     * 题目 id
     */
    private Long questionId;

    private static final long serialVersionUID = 1L;
}