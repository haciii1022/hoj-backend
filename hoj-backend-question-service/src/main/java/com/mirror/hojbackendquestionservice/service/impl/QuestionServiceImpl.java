package com.mirror.hojbackendquestionservice.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mirror.hojbackendcommon.common.ErrorCode;
import com.mirror.hojbackendcommon.constant.CommonConstant;
import com.mirror.hojbackendcommon.constant.RedisConstant;
import com.mirror.hojbackendcommon.exception.BusinessException;
import com.mirror.hojbackendcommon.exception.ThrowUtils;
import com.mirror.hojbackendcommon.utils.SqlUtils;
import com.mirror.hojbackendmodel.model.dto.question.QuestionQueryRequest;
import com.mirror.hojbackendmodel.model.entity.Question;
import com.mirror.hojbackendmodel.model.entity.QuestionSubmit;
import com.mirror.hojbackendmodel.model.entity.User;
import com.mirror.hojbackendmodel.model.vo.QuestionVO;
import com.mirror.hojbackendmodel.model.vo.UserVO;
import com.mirror.hojbackendquestionservice.mapper.QuestionMapper;
import com.mirror.hojbackendquestionservice.scheduler.RankSyncScheduler;
import com.mirror.hojbackendquestionservice.service.QuestionService;
import com.mirror.hojbackendquestionservice.service.QuestionSubmitService;
import com.mirror.hojbackendserverclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mirror.hojbackendquestionservice.scheduler.RankSyncScheduler.possibleSet;

/**
 * @author Mirror
 * @description 针对表【question(题目)】的数据库操作Service实现
 * @createDate 2024-06-13 10:33:08
 */
@Slf4j
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question>
        implements QuestionService {

    @Resource
    private UserFeignClient userFeignClient;

    @Lazy
    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    /**
     * 校验题目是否合法
     *
     * @param question
     * @param add
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        if (question == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = question.getTitle();
        String content = question.getContent();
        String tags = question.getTags();
        String answer = question.getAnswer();
        String judgeCase = question.getJudgeCase();
        String judgeConfig = question.getJudgeConfig();

        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(title, content, tags), ErrorCode.PARAMS_ERROR);
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(title) && title.length() > 80) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StringUtils.isNotBlank(content) && content.length() > 8192 * 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容过长");
        }
        // 未使用
//        if (StringUtils.isNotBlank(answer) && answer.length() > 8192) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答案过长");
//        }
//        if (StringUtils.isNotBlank(judgeCase) && judgeCase.length() > 8192) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题用例过长");
//        }
//        if (StringUtils.isNotBlank(judgeConfig) && judgeConfig.length() > 8192) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题配置过长");
//        }
    }

    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到mybatis 框架支持的查询QueryWrapper 类）
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        Long id = questionQueryRequest.getId();
        Boolean includeHiddenQuestions = questionQueryRequest.getIncludeHiddenQuestions();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        List<String> tags = questionQueryRequest.getTags();
        String answer = questionQueryRequest.getAnswer();
        Long userId = questionQueryRequest.getUserId();
        int current = questionQueryRequest.getCurrent();
        int pageSize = questionQueryRequest.getPageSize();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(answer), "answer", answer);
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.eq(Objects.equals(includeHiddenQuestions,false), "isHidden", 0);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
//        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        QuestionVO questionVO = QuestionVO.objToVo(question);
        long questionId = question.getId();
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userFeignClient.getById(userId);
        }
        UserVO userVO = userFeignClient.getUserVO(user);
        questionVO.setUserVO(userVO);

        return questionVO;
    }

    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, Boolean isWithRelatedData, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollUtil.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userFeignClient.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充信息
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            QuestionVO questionVO = QuestionVO.objToVo(question);
            Long userId = question.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUserVO(userFeignClient.getUserVO(user));
            return questionVO;
        }).collect(Collectors.toList());

        if (isWithRelatedData) {
            User loginUser = userFeignClient.getLoginUser(request);
            if (loginUser == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }
            List<Long> questionIds = questionList.stream().map(Question::getId).collect(Collectors.toList());
            Map<Long, Integer> highestScoreMap = userFeignClient.getUserHighestScores(loginUser.getId(), questionIds);
//            QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
//            queryWrapper.select("questionId", "MAX(score) AS score")
//                    .in("questionId", questionIds)
//                    .eq("userId", loginUser.getId())
//                    .groupBy("questionId");

//            List<Map<String, Object>> result = questionSubmitService.listMaps(queryWrapper);
//            Map<Long, Integer> highestScoreMap = result.stream()
//                    .collect(Collectors.toMap(
//                            map -> (Long) map.get("questionId"),
//                            map -> (Integer) map.get("score")== null ? -1 : (Integer) map.get("score") // 默认值 -1,表示未提交过
//                    ));
            questionVOList.forEach(item ->{
                item.setHistoricalScore(highestScoreMap.getOrDefault(item.getId(), -1));
            });
        }

        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    @Override
    public void fullSyncRedisRank() {
        //1.获取数据库中最高提交量的题目
        Integer maxSubmitNum = this.baseMapper.selectMaxSubmitNum();
        if (maxSubmitNum == null || maxSubmitNum == 0) return;
        //2.根据当日提交量和最高提交量生成筛选条
        int todaySubmitCount = (int) Optional.ofNullable(redisTemplate.opsForValue().get(RedisConstant.TODAY_SUBMIT_COUNT))
                .orElse(RankSyncScheduler.minCount);
        todaySubmitCount = Math.max(todaySubmitCount,RankSyncScheduler.minCount);
        //3.获得备选序列列表
        int threshold = maxSubmitNum - todaySubmitCount;
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Question::getId, Question::getSubmitNum) // 只查询必要字段
                .ge(Question::getSubmitNum, threshold)          // submit_num >= threshold
                .orderByDesc(Question::getSubmitNum);           // 按提交量降序
        List<Map<String, Object>> result = this.listMaps(wrapper);
        //4.写入redis的ZSet
//        ZSetOperations<String, Object> zSetOperations = redisTemplate.opsForZSet();
        // 1. 准备ZSet操作对象
        ZSetOperations<String, Object> zSet = redisTemplate.opsForZSet();
        String redisKey = RedisConstant.HOT_QUESTION_RANK;

        // 2. 创建批量操作集合（推荐批量操作提升性能）
        Set<ZSetOperations.TypedTuple<Object>> tuples = new HashSet<>();
        possibleSet.clear();
        // 3. 遍历处理结果集
        result.forEach(item -> {
            // 获取数据库字段值（注意字段名是下划线格式）
            Object idObj = item.get("id");
            Object submitNumObj = item.get("submitNum");
            // 空值校验
            if (idObj == null || submitNumObj == null) {
                log.warn("无效数据记录: {}", item);
                return;
            }
            try {
                // 类型转换
                String member = idObj.toString();
                possibleSet.add((Long) idObj);
                double score = Double.parseDouble(submitNumObj.toString());
                // 构建写入对象
                tuples.add(new DefaultTypedTuple<>(member, score));
            } catch (NumberFormatException e) {
                log.error("数值转换失败: ID={}, SubmitNum={}", idObj, submitNumObj);
            }
        });
        if (!tuples.isEmpty()) {
            // 先清空旧数据
            redisTemplate.delete(redisKey);
            // 批量写入新数据
            zSet.add(redisKey, tuples);
            // 设置24小时过期时间
            redisTemplate.expire(redisKey, 24, TimeUnit.HOURS);
            log.info("成功同步热门题目数据 {} 条", tuples.size());
        } else {
            log.warn("未找到符合条件的候选题目");
        }
        redisTemplate.opsForValue().set(RedisConstant.TODAY_SUBMIT_COUNT,0);
    }
}




