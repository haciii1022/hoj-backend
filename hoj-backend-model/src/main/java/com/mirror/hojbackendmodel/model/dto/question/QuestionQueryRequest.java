package com.mirror.hojbackendmodel.model.dto.question;

import com.mirror.hojbackendcommon.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 查询请求
 * @author Mirror.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表（json 数组）
     */
    private List<String> tags;

    /**
     * 答案
     */
    private String answer;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 是否需要获取另外关联数据
     * 如 该用户在此题的最高得分
     */
    private Boolean isWithRelatedData;

    /**
     * 是否包含隐藏题目
     */
    private Boolean includeHiddenQuestions;


    private static final long serialVersionUID = 1L;
}