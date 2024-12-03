package com.mirror.hojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 测试用例组
 *
 * @author Mirror
 * @TableName judgeCaseGroup
 * @date 2024/11/20
 */
@TableName(value = "judge_case_group")
@Data
public class JudgeCaseGroup implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 创建用户 id
     */
    private Long userId;

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
