package com.mirror.hojbackendquestionservice.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mirror.hojbackendcommon.common.ErrorCode;
import com.mirror.hojbackendcommon.constant.CommonConstant;
import com.mirror.hojbackendcommon.constant.RedisConstant;
import com.mirror.hojbackendcommon.exception.BusinessException;
import com.mirror.hojbackendcommon.utils.SeqUtil;
import com.mirror.hojbackendcommon.utils.SqlUtils;
import com.mirror.hojbackendmodel.model.dto.questionSubmit.QuestionSubmitAddRequest;
import com.mirror.hojbackendmodel.model.dto.questionSubmit.QuestionSubmitQueryRequest;
import com.mirror.hojbackendmodel.model.entity.Question;
import com.mirror.hojbackendmodel.model.entity.QuestionSubmit;
import com.mirror.hojbackendmodel.model.entity.User;
import com.mirror.hojbackendmodel.model.enums.BaseSequenceEnum;
import com.mirror.hojbackendmodel.model.enums.QuestionSubmitLanguageEnum;
import com.mirror.hojbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.mirror.hojbackendmodel.model.vo.QuestionSubmitVO;
import com.mirror.hojbackendquestionservice.mapper.QuestionSubmitMapper;
import com.mirror.hojbackendquestionservice.rabbitmq.MyMessageProducer;
import com.mirror.hojbackendquestionservice.service.QuestionService;
import com.mirror.hojbackendquestionservice.service.QuestionSubmitService;
import com.mirror.hojbackendserverclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Mirror
 * @description 针对表【question_submit(题目提交)】的数据库操作Service实现
 * @createDate 2024-06-13 10:35:37
 */
@Slf4j
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
        implements QuestionSubmitService {
    @Resource
    private QuestionService questionService;

    @Resource
    private UserFeignClient userService;

    @Resource
    private MyMessageProducer myMessageProducer;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 题目提交
     *
     * @param questionSubmitAddRequest
     * @param loginUser
     * @return 题目提交id
     */
    @Override
    @Transactional
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        // 判断实体是否存在，根据类别获取实体
        Question question = questionService.getById(questionSubmitAddRequest.getQuestionId());
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        Question questionUpdate = new Question();
        questionUpdate.setId(question.getId());
        questionUpdate.setSubmitNum(question.getSubmitNum() + 1);
        questionService.updateById(questionUpdate);
        //TODO 判断编程语言是否合法
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }
        // 是否已题目提交
        long userId = loginUser.getId();
        // 每个用户串行题目提交
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setId(SeqUtil.next(BaseSequenceEnum.QUESTION_SUBMIT_ID.getName()));
        questionSubmit.setLanguage(language);
        questionSubmit.setCode(questionSubmitAddRequest.getCode());
        questionSubmit.setJudgeInfo("[]");
        // 设置初始状态
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        questionSubmit.setQuestionId(questionSubmitAddRequest.getQuestionId());
        questionSubmit.setUserId(userId);
        boolean save = this.save(questionSubmit);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目提交失败");
        }
        redisTemplate.opsForValue().set(RedisConstant.QUESTION_SUBMIT_PREFIX + questionSubmit.getId(), questionSubmit, 3, TimeUnit.MINUTES);
        Long questionSubmitId = questionSubmit.getId();
        myMessageProducer.sendMessage("code_exchange", "my_routingKey", String.valueOf(questionSubmitId));
//        CompletableFuture.runAsync(() -> {
//             异步执行判题
//            judgeService.doJudge(questionSubmitId);
//        });
        return questionSubmitId;
    }

    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到mybatis 框架支持的查询QueryWrapper 类）
     *
     * @param questionSubmitSubmitQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitSubmitQueryRequest == null) {
            return queryWrapper;
        }
        String language = questionSubmitSubmitQueryRequest.getLanguage();
        Integer status = questionSubmitSubmitQueryRequest.getStatus();
        Long questionId = questionSubmitSubmitQueryRequest.getQuestionId();
        Long userId = questionSubmitSubmitQueryRequest.getUserId();
        int current = questionSubmitSubmitQueryRequest.getCurrent();
        int pageSize = questionSubmitSubmitQueryRequest.getPageSize();
        String sortField = questionSubmitSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitSubmitQueryRequest.getSortOrder();


        // 拼接查询条件
        queryWrapper.eq(StringUtils.isNotBlank(language), "language", language);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(QuestionSubmitStatusEnum.getEnumByValue(status) != null, "status", status);
//        queryWrapper.eq("isDelete", false);
        // 排序
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);

        return queryWrapper;
    }


    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser, boolean isValidate, boolean withRelatedData) {
        QuestionSubmitVO questionSubmitSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        boolean result = true;
        if (isValidate) {
            result = isAuthorizedToViewDetail(questionSubmit, loginUser);
        }
        //脱敏：仅本人和管理员能看见自己（提交userId和登录用户id不同）提交的代码等敏感信息
        // 2024/12/14 新增 当前用户已通过该题目的话也能看到
        if (!result) {
            // 需要脱敏
            questionSubmitSubmitVO.setCode(null);
        }
        if (withRelatedData) {
            User user = userService.getById(questionSubmit.getUserId());
            questionSubmitSubmitVO.setUserVO(userService.getUserVO(user));

            Question question = questionService.getById(questionSubmit.getQuestionId());
            questionSubmitSubmitVO.setQuestionVO(questionService.getQuestionVO(question, null));
        }
        return questionSubmitSubmitVO;
    }

    /**
     * 根据查询条件获取问题提交的分页数据。
     *
     * @param questionSubmitSubmitPage 问题提交的分页对象，包含查询结果集。
     * @param loginUser                当前登录用户。
     * @return 返回处理后的分页数据对象，包含用户信息丰富的问题提交数据。
     */
    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitSubmitPage, User loginUser) {
        // 获取当前页的问题提交列表
        List<QuestionSubmit> questionSubmitSubmitList = questionSubmitSubmitPage.getRecords();

        // 初始化问题提交的VO分页对象
        Page<QuestionSubmitVO> questionSubmitSubmitVOPage = new Page<>(questionSubmitSubmitPage.getCurrent(), questionSubmitSubmitPage.getSize(), questionSubmitSubmitPage.getTotal());

        // 如果当前页的问题提交列表为空，则直接返回空的分页对象
        if (CollUtil.isEmpty(questionSubmitSubmitList)) {
            return questionSubmitSubmitVOPage;
        }
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitSubmitList.stream()
                .map(questionSubmitSubmit -> getQuestionSubmitVO(questionSubmitSubmit, loginUser, true, false))
                .collect(Collectors.toList());
        questionSubmitSubmitVOPage.setRecords(questionSubmitVOList);
//        return questionSubmitSubmitVOPage;
//        // 统一获取所有问题提交的用户ID，用于后续批量查询用户信息
        // 1. 关联查询用户信息和题目信息
        Set<Long> userIdSet = questionSubmitSubmitList
                .stream()
                .map(QuestionSubmit::getUserId)
                .collect(Collectors.toSet());

        Set<Long> questionIdSet = questionSubmitSubmitList
                .stream()
                .map(QuestionSubmit::getQuestionId)
                .collect(Collectors.toSet());

        // 根据用户ID集合查询用户信息，并按用户ID分组
        Map<Long, List<User>> userListMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        // 根据题目ID集合查询用户信息，并按题目ID分组
        Map<Long, List<Question>> questionListMap = questionService.listByIds(questionIdSet)
                .stream()
                .collect(Collectors.groupingBy(Question::getId));

        // 将问题提交实体转换为VO对象，并填充用户信息
        // 填充信息
        List<QuestionSubmitVO> questionSubmitSubmitVOList = questionSubmitSubmitList.stream().map(questionSubmitSubmit -> {
            QuestionSubmitVO questionSubmitSubmitVO = QuestionSubmitVO.objToVo(questionSubmitSubmit);
            Long userId = questionSubmitSubmit.getUserId();
            Long questionId = questionSubmitSubmit.getQuestionId();
            User user = null;
            Question question = null;
            // 如果用户ID对应的用户列表不为空，则取第一个用户（因为用户ID应该是唯一的，这里简化了处理）
            if (userListMap.containsKey(userId)) {
                user = userListMap.get(userId).get(0);
            }
            if (questionListMap.containsKey(questionId)) {
                question = questionListMap.get(questionId).get(0);
            }
            // 通过用户服务将用户实体转换为VO对象，并设置到问题提交VO中
            questionSubmitSubmitVO.setUserVO(userService.getUserVO(user));
            questionSubmitSubmitVO.setQuestionVO(questionService.getQuestionVO(question, null));
            return questionSubmitSubmitVO;
        }).collect(Collectors.toList());

        // 设置处理后的VO列表到分页对象中
        questionSubmitSubmitVOPage.setRecords(questionSubmitSubmitVOList);

        // 返回处理后的分页数据对象
        return questionSubmitSubmitVOPage;
    }

    /**
     * 校验当前登录用户有无权限查看判题记录详情信息
     * 仅限记录提交者、管理员、已经通过当前题目的用户（TODO）
     *
     * @param questionSubmit
     * @param loginUser
     * @return
     */
    @Override
    public Boolean isAuthorizedToViewDetail(QuestionSubmit questionSubmit, User loginUser) {
        Long userId = questionSubmit.getUserId();
        if (!userService.isAdmin(loginUser) && !Objects.equals(userId, loginUser.getId())) {
            return false;
        }
        // TODO 判断当前用户是否通过该题
        return true;
    }

    @Override
    public Map<String, Object> getQuestionScoreData(Long questionId) {
        // 获取分数分布

        List<Map<String, Object>> scoreDistribution = this.getBaseMapper().getScoreDistribution(questionId);
        log.info("scoreDistribution: {}", scoreDistribution);
        // 获取其他统计数据
        Map<String, Object> statistics = this.getBaseMapper().getOtherStatistics(questionId);
        log.info("statistics: {}", statistics);
        Map<String, Long> scoreMap = new LinkedHashMap<>();
        if(CollectionUtil.isEmpty(statistics) || CollectionUtil.isEmpty(scoreDistribution)){
            return  Collections.emptyMap();
        }
        for(Map<String,Object> subMap: scoreDistribution){
            String scoreRange = String.valueOf(subMap.get("score_range"));
            long count = (Long)subMap.get("count");
            scoreMap.put(scoreRange,count);
        }
        log.info("scoreMap: {}",scoreMap);
        // 最大段
        String maxSegment = scoreMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("NA"); // 默认值

        // 最小段
        String minSegment = scoreMap.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("NA"); // 默认值

        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("scoreDistribution", scoreMap);
        resultMap.put("maxSegment", maxSegment);
        resultMap.put("minSegment", minSegment);
        resultMap.put("averageScore", statistics.getOrDefault("averageScore", "0.00"));
        resultMap.put("mostUsedLanguage", statistics.getOrDefault("mostUsedLanguage", "java"));
        return resultMap;
    }


}




