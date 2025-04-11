package com.mirror.hojbackenduserservice.controller.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mirror.hojbackendmodel.model.dto.userQuestion.UserQuestionAddRequest;
import com.mirror.hojbackendmodel.model.dto.userQuestion.UserQuestionQueryRequest;
import com.mirror.hojbackendmodel.model.entity.User;
import com.mirror.hojbackendmodel.model.entity.UserQuestionStatistics;
import com.mirror.hojbackendserverclient.service.UserFeignClient;
import com.mirror.hojbackenduserservice.service.UserQuestionStatisticsService;
import com.mirror.hojbackenduserservice.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户内部调用接口
 * @author Mirror.
 */
@RestController
@RequestMapping("/inner")
public class UserInnerController implements UserFeignClient {

    @Resource
    private UserService userService;

    @Resource
    private UserQuestionStatisticsService userQuestionStatisticsService;

    /**
     * 根据 id 获取用户
     * @param userId
     * @return
     */
    @Override
    @GetMapping("/get/id")
    public User getById(@RequestParam("userId") Long userId) {
        return userService.getById(userId);
    }

    /**
     * 根据 id 获取用户列表
     * @param idList
     * @return
     */
    @Override
    @GetMapping("/get/ids")
    public List<User> listByIds(@RequestParam("idList") Collection<Long> idList) {
        return userService.listByIds(idList);
    }

    /**
     * 添加用户通过题目记录（DTO版本）
     */
    @Override
    @PostMapping("/addAccepted")
    public Long addAcceptedRecord(@RequestBody UserQuestionAddRequest request) {
        return userQuestionStatisticsService.addAcceptedRecord(request);
    }

    /**
     * 判断用户是否通过某题
     */
    @Override
    @PostMapping("/isAccepted")
    public Boolean isAcceptedQuestion(@RequestBody UserQuestionQueryRequest request) {
        Long userId = request.getUserId();
        Long questionId = request.getQuestionId();
        long count = userQuestionStatisticsService.getBaseMapper().selectCount(new QueryWrapper<UserQuestionStatistics>()
                .eq(userId != null, "userId", userId)
                .eq(questionId != null, "questionId", questionId)
                .eq("highestScore", 100));
        return count > 0;
    }

    /**
     * 批量获取用户指定题目的最高分
     * @param userId
     * @param questionIds
     * @return
     */
    @Override
    @GetMapping("/get/questionScores")
    public Map<Long, Integer> getUserHighestScores(@RequestParam("userId") Long userId, @RequestParam("questionIds") List<Long> questionIds){
        return userQuestionStatisticsService.getUserHighestScores(userId, questionIds);
    }
}

