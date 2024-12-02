package com.mirror.hojbackendquestionservice.controller.inner;

import com.mirror.hojbackendcommon.common.ErrorCode;
import com.mirror.hojbackendcommon.exception.BusinessException;
import com.mirror.hojbackendmodel.model.dto.file.JudgeCaseFileQueryRequest;
import com.mirror.hojbackendmodel.model.entity.Question;
import com.mirror.hojbackendmodel.model.entity.QuestionSubmit;
import com.mirror.hojbackendquestionservice.service.JudgeCaseFileService;
import com.mirror.hojbackendquestionservice.service.QuestionService;
import com.mirror.hojbackendquestionservice.service.QuestionSubmitService;
import com.mirror.hojbackendserverclient.service.QuestionFeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Mirror
 * @date 2024/9/18
 */
@RestController
@RequestMapping("/inner")
public class QuestionInnerController implements QuestionFeignClient {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private JudgeCaseFileService judgeCaseFileService;

    @Override
    @GetMapping("/get/id")
    public Question getQuestionById(Long questionId) {
        if (questionId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
       return question;
    }

    @Override
    @GetMapping("/question_submit/get/id")
    public QuestionSubmit getQuestionSubmitById(Long questionSubmitId) {
       return questionSubmitService.getById(questionSubmitId);
    }

    @Override
    @PostMapping("/question_submit/update")
    public boolean updateQuestionSubmitById(QuestionSubmit questionSubmit) {
        return questionSubmitService.updateById(questionSubmit);
    }

    @Override
    @PostMapping("/update")
    public boolean updateQuestion(@RequestBody Question question){
        return questionService.updateById(question);
    }

    @Override
    @PostMapping("/judgeCaseFile/list")
    public List<String> getJudgeCaseFileListWithType(@RequestBody JudgeCaseFileQueryRequest judgeCaseFileQueryRequest){
        return judgeCaseFileService.getJudgeCaseFileListWithType(judgeCaseFileQueryRequest);
    }
}
