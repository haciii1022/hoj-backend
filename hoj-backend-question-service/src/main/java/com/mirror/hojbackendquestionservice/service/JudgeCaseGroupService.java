package com.mirror.hojbackendquestionservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mirror.hojbackendmodel.model.entity.JudgeCaseGroup;
import com.mirror.hojbackendmodel.model.entity.Question;
import com.mirror.hojbackendmodel.model.vo.JudgeCaseGroupVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
* @author Mirror
* @description 针对表【judge_case_group(判题用例组)】的数据库操作Service
* @createDate 2024-11-20 17:33:08
* @Entity com.mirror.hoj.model.entity.JudgeCaseGroup
*/
public interface JudgeCaseGroupService extends IService<JudgeCaseGroup> {

    /**
     * 校验判题用例组合法性：每组有且只有一个in文件和out文件
     * @param questionId
     */
    void validJudgeCaseGroup(Long questionId);

    List<JudgeCaseGroupVO> getJudgeCaseGroupList(Question qustion, HttpServletRequest request);


}
