package com.mirror.hojbackendquestionservice.controller;

import cn.hutool.core.lang.Assert;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mirror.hojbackendcommon.annotation.AuthCheck;
import com.mirror.hojbackendcommon.common.BaseResponse;
import com.mirror.hojbackendcommon.common.DeleteRequest;
import com.mirror.hojbackendcommon.common.ErrorCode;
import com.mirror.hojbackendcommon.common.ResultUtils;
import com.mirror.hojbackendcommon.constant.FileConstant;
import com.mirror.hojbackendcommon.constant.RedisConstant;
import com.mirror.hojbackendcommon.constant.UserConstant;
import com.mirror.hojbackendcommon.exception.BusinessException;
import com.mirror.hojbackendcommon.exception.ThrowUtils;
import com.mirror.hojbackendcommon.utils.FileUtil;
import com.mirror.hojbackendcommon.utils.SeqUtil;
import com.mirror.hojbackendmodel.model.dto.file.JudgeCaseFileAddRequest;
import com.mirror.hojbackendmodel.model.dto.file.JudgeCaseGroupAddRequest;
import com.mirror.hojbackendmodel.model.dto.question.JudgeCase;
import com.mirror.hojbackendmodel.model.dto.question.JudgeConfig;
import com.mirror.hojbackendmodel.model.dto.question.QuestionAddRequest;
import com.mirror.hojbackendmodel.model.dto.question.QuestionEditRequest;
import com.mirror.hojbackendmodel.model.dto.question.QuestionQueryRequest;
import com.mirror.hojbackendmodel.model.dto.question.QuestionUpdateRequest;
import com.mirror.hojbackendmodel.model.dto.questionSubmit.QuestionSubmitAddRequest;
import com.mirror.hojbackendmodel.model.dto.questionSubmit.QuestionSubmitQueryRequest;
import com.mirror.hojbackendmodel.model.entity.JudgeCaseFile;
import com.mirror.hojbackendmodel.model.entity.JudgeCaseGroup;
import com.mirror.hojbackendmodel.model.entity.Question;
import com.mirror.hojbackendmodel.model.entity.QuestionSubmit;
import com.mirror.hojbackendmodel.model.entity.User;
import com.mirror.hojbackendmodel.model.enums.BaseSequenceEnum;
import com.mirror.hojbackendmodel.model.vo.JudgeCaseGroupVO;
import com.mirror.hojbackendmodel.model.vo.QuestionSubmitVO;
import com.mirror.hojbackendmodel.model.vo.QuestionVO;
import com.mirror.hojbackendquestionservice.service.JudgeCaseFileService;
import com.mirror.hojbackendquestionservice.service.JudgeCaseGroupService;
import com.mirror.hojbackendquestionservice.service.QuestionService;
import com.mirror.hojbackendquestionservice.service.QuestionSubmitService;
import com.mirror.hojbackendserverclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 题目接口
 *
 * @author Mirror.
 */
@RestController
@RequestMapping("/")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private JudgeCaseGroupService judgeCaseGroupService;

    @Resource
    private JudgeCaseFileService judgeCaseFileService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 创建
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        if (questionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<String> tags = questionAddRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        List<JudgeCase> judgeCase = questionAddRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCase));
        }
        JudgeConfig judgeConfig = questionAddRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        questionService.validQuestion(question, true);
        judgeCaseGroupService.validJudgeCaseGroup(question.getId());
        User loginUser = userFeignClient.getLoginUser(request);
        question.setId(SeqUtil.next(BaseSequenceEnum.QUESTION_ID.getName()));
        question.setUserId(loginUser.getId());
        question.setFavourNum(0);
        question.setThumbNum(0);

        log.info("addQuestion: {}", question);
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userFeignClient.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userFeignClient.isAdmin(user)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = questionService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        List<String> tags = questionUpdateRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        List<JudgeCase> judgeCase = questionUpdateRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCase));
        }
        JudgeConfig judgeConfig = questionUpdateRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        // 参数校验
        questionService.validQuestion(question, false);
        judgeCaseGroupService.validJudgeCaseGroup(question.getId());
        long id = questionUpdateRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = questionService.updateById(question);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Question> getQuestionById(Long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request);
        if (question.getIsHidden() == 1 && !userFeignClient.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "题目已经删除或被隐藏");
        }
        if (!question.getUserId().equals(loginUser.getId()) && !userFeignClient.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return ResultUtils.success(question);
    }

    /**
     * 根据 id 获取（脱敏）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(Long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request);
        if (question.getIsHidden() == 1 && !userFeignClient.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "题目已经删除或被隐藏");
        }
        return ResultUtils.success(questionService.getQuestionVO(question, request));
    }

    /**
     * 分页获取题目列表（仅管理员）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        Boolean isWithRelatedData = questionQueryRequest.getIsWithRelatedData();
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, isWithRelatedData, request));
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        if (questionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        Boolean isWithRelatedData = questionQueryRequest.getIsWithRelatedData();
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, isWithRelatedData, request));
    }


    /**
     * 编辑（用户）
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/edit")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        List<String> tags = questionEditRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        List<JudgeCase> judgeCase = questionEditRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCase));
        }
        JudgeConfig judgeConfig = questionEditRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        // 参数校验
        questionService.validQuestion(question, false);
        judgeCaseGroupService.validJudgeCaseGroup(question.getId());
        User loginUser = userFeignClient.getLoginUser(request);
        long id = questionEditRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !userFeignClient.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = questionService.updateById(question);
        return ResultUtils.success(result);
    }

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param request
     */
    @PostMapping("/question_submit/do")
    public BaseResponse<Long> doQuestionSubmit(@RequestBody QuestionSubmitAddRequest questionSubmitAddRequest, HttpServletRequest request) {
        if (questionSubmitAddRequest == null || questionSubmitAddRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能提交
        final User loginUser = userFeignClient.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        long questionId = questionSubmitAddRequest.getQuestionId();
        long result = questionSubmitService.doQuestionSubmit(questionSubmitAddRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 分页获取题目提交列表（除管理员外，普通用户只能看到非答案、提交代码等公开信息）
     *
     * @param questionSubmitQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/question_submit/list/page")
    public BaseResponse<Page<QuestionSubmitVO>> listQuestionSubmitByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest, HttpServletRequest request) {

        long current = questionSubmitQueryRequest.getCurrent();
        long size = questionSubmitQueryRequest.getPageSize();
        log.info("listQuestionSubmitByPage入参: {}", questionSubmitQueryRequest);
        //从数据库中得到了原始的题目提交分页信息
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(current, size),
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        User loginUser = userFeignClient.getLoginUser(request);
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVOPage(questionSubmitPage, loginUser));
    }


    /**
     * 根据 id 获取题目判题信息
     * 2、用户在记录列表中点击某一行跳转触发此接口
     * @param questionSubmitId
     * @return
     */
    @GetMapping("/question_submit/detail")
    public BaseResponse<QuestionSubmitVO> getQuestionSubmitDetailById(@RequestParam("questionSubmitId") Long questionSubmitId, HttpServletRequest request) {
        Assert.notNull(questionSubmitId, "提交id不能为空");
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        User loginUser = userFeignClient.getLoginUser(request);
        Boolean b = questionSubmitService.isAuthorizedToViewDetail(questionSubmit, loginUser);
        if (!b) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "无权限查看判题记录详情");
        }
        QuestionSubmitVO submitVO = questionSubmitService.getQuestionSubmitVO(questionSubmit, loginUser, false, true);
        return ResultUtils.success(submitVO);
    }

    /**
     * 根据 id 获取题目判题信息
     * 1、用户提交代码在轮询判题结果
     * @param questionSubmitId
     * @return
     */
    @GetMapping("/question_submit")
    public BaseResponse<QuestionSubmitVO> getQuestionSubmitVoById(@RequestParam("questionSubmitId") Long questionSubmitId, HttpServletRequest request) {
        Assert.notNull(questionSubmitId, "提交id不能为空");
        String key = RedisConstant.QUESTION_SUBMIT_PREFIX + questionSubmitId;
        QuestionSubmit questionSubmit = (QuestionSubmit) redisTemplate.opsForValue().get(key);
        User loginUser = userFeignClient.getLoginUser(request);
        if (questionSubmit == null) {
            questionSubmit = questionSubmitService.getById(questionSubmitId);
            redisTemplate.opsForValue().set(RedisConstant.QUESTION_SUBMIT_PREFIX + questionSubmit.getId(), questionSubmit, 3, TimeUnit.MINUTES);
        }
        QuestionSubmitVO submitVO = questionSubmitService.getQuestionSubmitVO(questionSubmit, loginUser, false, false);
        return ResultUtils.success(submitVO);
    }

    @GetMapping("/judgeCaseGroup/get")
    public BaseResponse<List<JudgeCaseGroupVO>> getJudgeCaseGroupListByQuestionId(@RequestParam("questionId") Long questionId, HttpServletRequest request) {
        if (questionId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(judgeCaseGroupService.getJudgeCaseGroupList(question, request));
    }

    /**
     * 新增判题用例组
     *
     * @param judgeCaseGroupAddRequest
     * @param request
     * @return
     */
    @PostMapping("/judgeCaseGroup/add")
    public BaseResponse<Long> addJudgeCaseGroup(@RequestBody JudgeCaseGroupAddRequest judgeCaseGroupAddRequest, HttpServletRequest request) {
        User loginUser = userFeignClient.getLoginUser(request);
        JudgeCaseGroup judgeCaseGroup = new JudgeCaseGroup();
        BeanUtils.copyProperties(judgeCaseGroupAddRequest, judgeCaseGroup);
        judgeCaseGroup.setId(SeqUtil.next(BaseSequenceEnum.JUDGE_CASE_GROUP_ID.getName()));
        judgeCaseGroup.setUserId(loginUser.getId());
        log.info("addJudgeCaseGroup: {}", judgeCaseGroup);
        boolean result = judgeCaseGroupService.save(judgeCaseGroup);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(judgeCaseGroup.getId());
    }

    /**
     * 新增/更新判题用例文件
     *
     * @param file
     * @param jsonData
     * @param request
     * @return
     */
    @PostMapping("/judgeCaseFile/add")
    public BaseResponse<Long> addJudgeCaseFile(@RequestPart("file") MultipartFile file,
                                               @RequestPart("jsonData") String jsonData, HttpServletRequest request) {
        JudgeCaseFileAddRequest judgeCaseFileAddRequest = JSONUtil.toBean(jsonData, JudgeCaseFileAddRequest.class);
        ThrowUtils.throwIf(file.isEmpty(), ErrorCode.UNPROCESSABLE_ENTITY);
        return ResultUtils.success(judgeCaseFileService.saveOrUpdateFile(file, judgeCaseFileAddRequest, request));
    }

    @GetMapping("/judgeCaseGroup/delete")
    public BaseResponse<Boolean> deleteJudgeCaseGroup(@RequestParam("groupId") Long groupId, HttpServletRequest request) {
        ThrowUtils.throwIf(groupId <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(judgeCaseGroupService.deleteJudgeCaseGroup(groupId));
    }

    @GetMapping("/judgeCaseFile/delete")
    public BaseResponse<Boolean> deleteJudgeCaseFile(@RequestParam("fileId") Long fileId, HttpServletRequest request) {
        ThrowUtils.throwIf(fileId <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(judgeCaseFileService.deleteJudgeCaseFile(fileId));
    }

    @GetMapping("/judgeCaseFile/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadJudgeCaseFile(@RequestParam("fileId") Long fileId, HttpServletRequest request) {
        ThrowUtils.throwIf(fileId <= 0, ErrorCode.PARAMS_ERROR);
        JudgeCaseFile judgeCaseFile = judgeCaseFileService.getById(fileId);
        Integer type = judgeCaseFile.getType();
        String extension = type == 0 ? ".in" : ".out";
        String fileName = judgeCaseFile.getFileFolder() + File.separator + judgeCaseFile.getFileName() + extension;
        String fullFileName = FileConstant.ROOT_PATH + fileName;
        org.springframework.core.io.Resource resource = FileUtil.downloadFileViaSFTP(fullFileName);
        // 设置下载文件的响应头
        String contentDisposition = "attachment; filename=\"" + fileName + "\"";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)  // 设置下载方式
                .contentType(MediaType.APPLICATION_OCTET_STREAM)  // 让浏览器知道文件是二进制流
                .body(resource);
    }

    @GetMapping("/next")
    public BaseResponse<Long> getNextQuestionId(HttpServletRequest request) {
        return ResultUtils.success(SeqUtil.getNextValue(BaseSequenceEnum.QUESTION_ID.getName()));
    }

//    @GetMapping("/test")
//    public BaseResponse<Boolean> test(HttpServletRequest request) {
//        String filePath = "/home/ubuntu/hoj/question/1801181035134369793/4_1.out";
//        String filePath2 = "/home/ubuntu/hoj/question/1801181035134369793/4_1.ans";
//        boolean b = FileUtil.compareFilesIgnoringLastLineEnding(filePath, filePath2);
//        return ResultUtils.success(b);
//    }
}
