package com.mirror.hojbackendserverclient.service;

import com.mirror.hojbackendcommon.common.ErrorCode;
import com.mirror.hojbackendcommon.constant.UserConstant;
import com.mirror.hojbackendcommon.exception.BusinessException;
import com.mirror.hojbackendmodel.model.dto.userQuestion.UserQuestionAddRequest;
import com.mirror.hojbackendmodel.model.dto.userQuestion.UserQuestionQueryRequest;
import com.mirror.hojbackendmodel.model.entity.User;
import com.mirror.hojbackendmodel.model.enums.UserRoleEnum;
import com.mirror.hojbackendmodel.model.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户服务
 * @author Mirror.
 */
@FeignClient(name = "hoj-backend-user-service", path = "/api/user/inner")
public interface UserFeignClient {

    /**
     * 根据 id 获取用户
     * @param userId
     * @return
     */
    @GetMapping("/get/id")
    User getById(@RequestParam("userId") Long userId);

    /**
     * 根据 id 获取用户列表
     * @param idList
     * @return
     */
    @GetMapping("/get/ids")
    List<User> listByIds(@RequestParam("idList") Collection<Long> idList);

    /**
     * 添加用户通过题目记录（DTO版本）
     */
    @PostMapping("/addAccepted")
    Long addAcceptedRecord(@RequestBody UserQuestionAddRequest request);

    /**
     * 判断用户是否通过某题
     */
    @PostMapping("/isAccepted")
    Boolean isAcceptedQuestion(@RequestBody UserQuestionQueryRequest request);

    /**
     * 批量获取用户指定题目的最高分
     * @param userId
     * @param questionIds
     * @return
     */
    @GetMapping("/get/questionScores")
    Map<Long, Integer> getUserHighestScores(@RequestParam("userId") Long userId, @RequestParam("questionIds") List<Long> questionIds);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    default User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    default boolean isAdmin(User user) {
        return user != null && (UserRoleEnum.ADMIN.getValue().equals(user.getUserRole()) || UserRoleEnum.ROOT.getValue().equals(user.getUserRole()));
    }

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    default UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

}

