package com.mirror.hojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 测试用例文件
 *
 * @author Mirror
 * @TableName judgeCaseFile
 * @date 2024/11/20
 */
@TableName(value = "judge_case_file")
@Data
public class JudgeCaseFile implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 测试用例组 id
     */
    private Long groupId;

    /**
     * 文件 url
     */
    private String url;

    /**
     * 文件类型 0:in 1:out
     */
    private Integer type;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件夹位置
     */
    private String fileFolder;

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
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
