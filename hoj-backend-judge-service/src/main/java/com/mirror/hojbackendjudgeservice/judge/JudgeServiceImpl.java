package com.mirror.hojbackendjudgeservice.judge;

import cn.hutool.json.JSONUtil;
import com.mirror.hojbackendcommon.common.ErrorCode;
import com.mirror.hojbackendcommon.constant.FileConstant;
import com.mirror.hojbackendcommon.constant.RedisConstant;
import com.mirror.hojbackendcommon.exception.BusinessException;
import com.mirror.hojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.mirror.hojbackendjudgeservice.judge.codesandbox.CodeSandboxFactory;
import com.mirror.hojbackendjudgeservice.judge.codesandbox.CodeSandboxProxy;
import com.mirror.hojbackendjudgeservice.judge.strategy.JudgeContext;
import com.mirror.hojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.mirror.hojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.mirror.hojbackendmodel.model.codesandbox.JudgeInfo;
import com.mirror.hojbackendmodel.model.dto.file.JudgeCaseFileQueryRequest;
import com.mirror.hojbackendmodel.model.dto.question.JudgeConfig;
import com.mirror.hojbackendmodel.model.dto.userQuestion.UserQuestionAddRequest;
import com.mirror.hojbackendmodel.model.entity.Question;
import com.mirror.hojbackendmodel.model.entity.QuestionSubmit;
import com.mirror.hojbackendmodel.model.enums.JudgeInfoMessageEnum;
import com.mirror.hojbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.mirror.hojbackendserverclient.service.QuestionFeignClient;
import com.mirror.hojbackendserverclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Mirror
 * @date 2024/8/5
 */
@Slf4j
@Service
public class JudgeServiceImpl implements JudgeService {

    @Resource
    QuestionFeignClient questionFeignClient;

    @Resource
    UserFeignClient userFeignClient;

    @Value("${codesandbox.type:remote}")
    private String type;

    @Resource
    private JudgeManager judgeManager;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
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
//        String judgeCaseStr = question.getJudgeCase();
//        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
//        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        String judgeConfigStr = question.getJudgeConfig();
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);

        //  获取inputFilePathList
        JudgeCaseFileQueryRequest request = new JudgeCaseFileQueryRequest();
        request.setQuestionId(questionId);
        request.setType(FileConstant.FILE_TYPE_IN);
        List<String> judgeCaseInputFilePathList = questionFeignClient.getJudgeCaseFileListWithType(request);

        request.setType(FileConstant.FILE_TYPE_OUT);
        List<String> judgeCaseOutputFilePathList = questionFeignClient.getJudgeCaseFileListWithType(request);
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .language(language)
                .identifier(String.valueOf(questionSubmitId))
//                .inputList(inputList)
                .inputFilePathList(judgeCaseInputFilePathList)
                .timeLimit(judgeConfig.getTimeLimit())
                .memoryLimit(judgeConfig.getMemoryLimit())
                .build();
        //FIXME 这里的ExecuteCodeResponse要修改一下，和沙箱的对齐
        JudgeInfo judgeInfo;
        JudgeContext judgeContext = new JudgeContext();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        UserQuestionAddRequest userQuestionAddRequest = new UserQuestionAddRequest();
        userQuestionAddRequest.setUserId(questionSubmit.getUserId());
        userQuestionAddRequest.setQuestionId(questionId);
        userQuestionAddRequest.setSubmitId(questionSubmitId);
        //如果沙箱执行失败，直接返回
        if (Objects.equals(executeCodeResponse.getStatus(), QuestionSubmitStatusEnum.FAILED.getValue())) {
            judgeInfo = new JudgeInfo();
            judgeInfo.setScore(0);
            userQuestionAddRequest.setScore(0);
            judgeInfo.setDetail(executeCodeResponse.getDetail());
            if (executeCodeResponse.getMessage().equals(JudgeInfoMessageEnum.COMPILE_ERROR.getText())) {
                questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
                judgeInfo.setMessage(JudgeInfoMessageEnum.COMPILE_ERROR.getText());
            } else {
                questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
                judgeInfo.setMessage(JudgeInfoMessageEnum.SYSTEM_ERROR.getText());
            }
        } else {
            //根据沙箱的执行结果，设置题目的判题状态和信息
            judgeContext.setJudgeInfoList(executeCodeResponse.getJudgeInfoList());
            judgeContext.setJudgeCaseinputFilePathList(judgeCaseInputFilePathList);
            judgeContext.setOutputFilePathList(executeCodeResponse.getOutputFilePathList());
            judgeContext.setJudgeCaseOutputFilePathList(judgeCaseOutputFilePathList);
            judgeContext.setQuestion(question);
            judgeContext.setQuestionSubmit(questionSubmit);
//            judgeContext.setOutputList(executeCodeResponse.getOutputList());
//            judgeContext.setJudgeCaseList(judgeCaseList);
//            judgeContext.setInputList(inputList);
            // FIXME 总判题信息，后续需要拼接个判题子信息
            judgeInfo = judgeManager.doJudge(judgeContext);
            Integer score = judgeContext.getQuestionSubmit().getScore();
            questionSubmitUpdate.setScore(score);
            userQuestionAddRequest.setScore(score);
            //在数据库中更新判题状态
            if (Objects.equals(judgeInfo.getMessage(), JudgeInfoMessageEnum.ACCEPTED.getText())) {
                questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
                Question questionUpdate = new Question();
                questionUpdate.setId(questionId);
                questionUpdate.setAcceptedNum(question.getAcceptedNum() + 1);
                boolean b = questionFeignClient.updateQuestion(questionUpdate);
                if (!b) {
                    log.error("题目通过数更新失败");
//                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新题目信息错误");
                }
            } else {
                questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
            }
        }

        // FIXME questionSubmit.judgeInfo设置成返回的 judgeInfo+judgeInfoList,第一个为总judgeInfo
        List<JudgeInfo> judgeInfoList = new ArrayList<>();
        judgeInfoList.add(judgeInfo);
        judgeInfoList.addAll(Optional.ofNullable(judgeContext.getJudgeInfoList()).orElse(Collections.emptyList()));
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfoList));
        update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        Long result = userFeignClient.addAcceptedRecord(userQuestionAddRequest);
        if (result < 0) {
            log.error("用户通过题目数据插入失败，请查看日志");
        } else if (result == 0) {
            log.info("该用户非首次尝试该题，更新user_question_statistics数据成功");
        }else {
            log.info("该用户首次尝试该题，新增user_question_statistics数据成功");
        }
        QuestionSubmit submit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        redisTemplate.opsForValue().set(RedisConstant.QUESTION_SUBMIT_PREFIX + questionSubmitId,
                submit, 3, TimeUnit.MINUTES);
        return submit;
    }

    @Override
    public QuestionSubmit handleErrorJudge(Long questionSubmitId) {
        QuestionSubmit questionSubmit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        List<JudgeInfo> judgeInfoList = new ArrayList<>();
        JudgeInfo info = new JudgeInfo();
        info.setMessage(JudgeInfoMessageEnum.SYSTEM_ERROR.getText());
        judgeInfoList.add(info);
        questionSubmit.setJudgeInfo(JSONUtil.toJsonStr(judgeInfoList));
        questionSubmit.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
        boolean update = questionFeignClient.updateQuestionSubmitById(questionSubmit);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        QuestionSubmit submit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        redisTemplate.opsForValue().set(RedisConstant.QUESTION_SUBMIT_PREFIX + questionSubmitId,
                submit, 3, TimeUnit.MINUTES);
        return submit;
    }
}
