package com.mirror.hoj.judge;

import cn.hutool.json.JSONUtil;
import com.mirror.hoj.common.ErrorCode;
import com.mirror.hoj.exception.BusinessException;
import com.mirror.hoj.judge.codesandbox.CodeSandbox;
import com.mirror.hoj.judge.codesandbox.CodeSandboxFactory;
import com.mirror.hoj.judge.codesandbox.CodeSandboxProxy;
import com.mirror.hoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.mirror.hoj.judge.codesandbox.model.ExecuteCodeResponse;
import com.mirror.hoj.judge.strategy.JudgeContext;
import com.mirror.hoj.model.dto.question.JudgeCase;
import com.mirror.hoj.model.dto.question.JudgeConfig;
import com.mirror.hoj.model.dto.questionSubmit.JudgeInfo;
import com.mirror.hoj.model.entity.Question;
import com.mirror.hoj.model.entity.QuestionSubmit;
import com.mirror.hoj.model.enums.JudgeInfoMessageEnum;
import com.mirror.hoj.model.enums.QuestionSubmitStatusEnum;
import com.mirror.hoj.service.QuestionService;
import com.mirror.hoj.service.QuestionSubmitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Mirror
 * @date 2024/8/5
 */
@Service
public class JudgeServiceImpl implements JudgeService {
    @Resource
    QuestionService questionService;

    @Resource
    QuestionSubmitService questionSubmitService;

    @Value("${codesandbox.type:remote}")
    private String type;

    @Resource
    private JudgeManager judgeManager;

    @Override
    public QuestionSubmit doJudge(Long questionSubmitId) {
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目提交不存在");
        }

        Long questionId = questionSubmit.getQuestionId();
        Question question = questionService.getById(questionId);
        if (question == null || question.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        //只有WAITING状态的判题记录才会执行，防止重复判题
        if (!Objects.equals(questionSubmit.getStatus(), QuestionSubmitStatusEnum.WAITING.getValue())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目正在判题中");
        }
        //更新判题状态为RUNNING
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        boolean update = questionSubmitService.updateById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        //调用代码沙箱执行判题服务
        CodeSandbox codeSandbox = CodeSandboxFactory.getCodeSandbox(type);
        codeSandbox = new CodeSandboxProxy(codeSandbox);
        String code = questionSubmit.getCode();
        String language = questionSubmit.getLanguage();
        String judgeCaseStr = question.getJudgeCase();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        String judgeConfigStr = question.getJudgeConfig();
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .language(language)
                .inputList(inputList)
                .timeLimit(judgeConfig.getTimeLimit())
                .memoryLimit(judgeConfig.getMemoryLimit())
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        //根据沙箱的执行结果，设置题目的判题状态和信息
        JudgeContext judgeContext = new JudgeContext();
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        judgeContext.setQuestion(question);
        judgeContext.setQuestionSubmit(questionSubmit);
        judgeContext.setOutputList(executeCodeResponse.getOutputList());
        judgeContext.setJudgeCaseList(judgeCaseList);
        judgeContext.setInputList(inputList);
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);
        //在数据库中更新判题状态
        if (Objects.equals(judgeInfo.getMessage(), JudgeInfoMessageEnum.ACCEPTED.getValue())) {
            questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        } else {
            questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
        }
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        System.out.println("judgeInfo = " + judgeInfo);
        System.out.println("questionSubmitUpdate = " + questionSubmitUpdate);
        System.out.println(JSONUtil.toJsonStr(judgeInfo));
        update = questionSubmitService.updateById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        return questionSubmitService.getById(questionSubmitId);
    }
}
