package com.mirror.hojbackendjudgeservice.judge;

import cn.hutool.json.JSONUtil;
import com.mirror.hojbackendcommon.common.ErrorCode;
import com.mirror.hojbackendcommon.exception.BusinessException;
import com.mirror.hojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.mirror.hojbackendjudgeservice.judge.codesandbox.CodeSandboxFactory;
import com.mirror.hojbackendjudgeservice.judge.codesandbox.CodeSandboxProxy;
import com.mirror.hojbackendjudgeservice.judge.strategy.JudgeContext;
import com.mirror.hojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.mirror.hojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.mirror.hojbackendmodel.model.codesandbox.JudgeInfo;
import com.mirror.hojbackendmodel.model.dto.question.JudgeCase;
import com.mirror.hojbackendmodel.model.dto.question.JudgeConfig;
import com.mirror.hojbackendmodel.model.entity.Question;
import com.mirror.hojbackendmodel.model.entity.QuestionSubmit;
import com.mirror.hojbackendmodel.model.enums.JudgeInfoMessageEnum;
import com.mirror.hojbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.mirror.hojbackendserverclient.service.QuestionFeignClient;
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
    QuestionFeignClient questionFeignClient;

    @Value("${codesandbox.type:remote}")
    private String type;

    @Resource
    private JudgeManager judgeManager;

    @Override
    public QuestionSubmit doJudge(Long questionSubmitId) {
        QuestionSubmit questionSubmit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目提交不存在");
        }

        Long questionId = questionSubmit.getQuestionId();
        Question question = questionFeignClient.getQuestionById(questionId);
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
        boolean update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
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
        //TODO 这里的ExecuteCodeResponse要修改一下，和沙箱的对齐
        JudgeInfo judgeInfo;
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        //如果沙箱执行失败，直接返回
        if(Objects.equals(executeCodeResponse.getStatus(), QuestionSubmitStatusEnum.FAILED.getValue())) {
            judgeInfo = new JudgeInfo();
            if (executeCodeResponse.getMessage().equals(JudgeInfoMessageEnum.COMPILE_ERROR.getText())) {
                questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
                judgeInfo.setMessage(JudgeInfoMessageEnum.COMPILE_ERROR.getText());
            }
        }else{
            //根据沙箱的执行结果，设置题目的判题状态和信息
            JudgeContext judgeContext = new JudgeContext();
            judgeContext.setJudgeInfoList(executeCodeResponse.getJudgeInfoList());
            judgeContext.setQuestion(question);
            judgeContext.setQuestionSubmit(questionSubmit);
            judgeContext.setOutputList(executeCodeResponse.getOutputList());
            judgeContext.setJudgeCaseList(judgeCaseList);
            judgeContext.setInputList(inputList);
            judgeInfo = judgeManager.doJudge(judgeContext);
            //在数据库中更新判题状态
            if (Objects.equals(judgeInfo.getMessage(), JudgeInfoMessageEnum.ACCEPTED.getText())) {
                questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
            } else {
                questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
            }
        }

        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        System.out.println("judgeInfo = " + judgeInfo);
        System.out.println("questionSubmitUpdate = " + questionSubmitUpdate);
        System.out.println(JSONUtil.toJsonStr(judgeInfo));
        update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        return questionFeignClient.getQuestionSubmitById(questionSubmitId);
    }
}
