package com.mirror.hojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户
 * @author Mirror.
 */
@TableName(value = "user_question_statistics")
@Data
public class UserQuestionStatistics implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 题目id
     */
    private Long questionId;

    /**
     * 首次通过的提交id
     */
    private Long firstAcceptedId;

    /**
     * 历史最高分
     */
    private Integer highestScore;

    /**
     * 通过次数
     */
    private Integer acceptedNum;

    /**
     * 提交次数
     */
    private Integer submitNum;
    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}