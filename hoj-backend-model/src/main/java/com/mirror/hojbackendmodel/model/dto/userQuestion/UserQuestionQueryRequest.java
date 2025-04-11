package com.mirror.hojbackendmodel.model.dto.userQuestion;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserQuestionQueryRequest implements Serializable {

    private Long userId;

    private Long questionId;

    private Long submitId;

    private static final long serialVersionUID = 1L;
}