package com.mirror.hojbackendmodel.model.dto.userQuestion;

import lombok.Data;
import java.io.Serializable;

@Data
public class UserQuestionAddRequest implements Serializable {

    private Long userId;

    private Long questionId;
    
    private Long submitId;

    private Integer score;

    private static final long serialVersionUID = 1L;
}