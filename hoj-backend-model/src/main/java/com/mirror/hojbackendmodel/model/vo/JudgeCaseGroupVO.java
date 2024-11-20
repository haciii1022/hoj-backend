package com.mirror.hojbackendmodel.model.vo;

import com.mirror.hojbackendcommon.constant.FileConstant;
import com.mirror.hojbackendmodel.model.entity.JudgeCaseFile;
import com.mirror.hojbackendmodel.model.entity.JudgeCaseGroup;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.List;

/**
 * @author Mirror
 * @date 2024/11/20
 */
@Data
public class JudgeCaseGroupVO {

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
    private Integer isDelete;

    /**
     * 输入文件
     */
    private JudgeCaseFile inputFile;

    /**
     * 输出文件
     */
    private JudgeCaseFile outputFile;

    public static JudgeCaseGroupVO objToVo(
            JudgeCaseGroup judgeCaseGroup,
            List<JudgeCaseFile> judgeCaseFiles) {
        JudgeCaseGroupVO judgeCaseGroupVO = new JudgeCaseGroupVO();
        BeanUtils.copyProperties(judgeCaseGroup, judgeCaseGroupVO);

        // 查找 inputFile 和 outputFile
        for (JudgeCaseFile file : judgeCaseFiles) {
            if (file.getGroupId().equals(judgeCaseGroup.getId())) {
                if (FileConstant.FILE_TYPE_IN.equals(file.getType())) {
                    judgeCaseGroupVO.setInputFile(file);
                }
                else if (FileConstant.FILE_TYPE_OUT.equals(file.getType())) {
                    judgeCaseGroupVO.setOutputFile(file);
                }
            }
        }

        return judgeCaseGroupVO;
    }

}
