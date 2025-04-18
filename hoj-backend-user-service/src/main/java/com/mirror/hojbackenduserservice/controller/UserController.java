package com.mirror.hojbackenduserservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mirror.hojbackendcommon.annotation.AuthCheck;
import com.mirror.hojbackendcommon.common.BaseResponse;
import com.mirror.hojbackendcommon.common.DeleteRequest;
import com.mirror.hojbackendcommon.common.ErrorCode;
import com.mirror.hojbackendcommon.common.ResultUtils;
import com.mirror.hojbackendcommon.constant.FileConstant;
import com.mirror.hojbackendcommon.constant.UserConstant;
import com.mirror.hojbackendcommon.exception.BusinessException;
import com.mirror.hojbackendcommon.exception.ThrowUtils;
import com.mirror.hojbackendcommon.utils.FileUtil;
import com.mirror.hojbackendmodel.model.dto.user.UserAddRequest;
import com.mirror.hojbackendmodel.model.dto.user.UserLoginRequest;
import com.mirror.hojbackendmodel.model.dto.user.UserQueryRequest;
import com.mirror.hojbackendmodel.model.dto.user.UserRegisterRequest;
import com.mirror.hojbackendmodel.model.dto.user.UserUpdateMyRequest;
import com.mirror.hojbackendmodel.model.dto.user.UserUpdateRequest;
import com.mirror.hojbackendmodel.model.entity.User;
import com.mirror.hojbackendmodel.model.vo.LoginUserVO;
import com.mirror.hojbackendmodel.model.vo.UserRankVO;
import com.mirror.hojbackendmodel.model.vo.UserVO;
import com.mirror.hojbackenduserservice.service.UserQuestionStatisticsService;
import com.mirror.hojbackenduserservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.DigestUtils;
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
import java.util.*;
import java.util.stream.Collectors;

import static com.mirror.hojbackenduserservice.manager.RankRedisManager.RANK_KEY;
import static com.mirror.hojbackenduserservice.manager.RankRedisManager.USER_STATS_PREFIX;
import static com.mirror.hojbackenduserservice.service.impl.UserServiceImpl.SALT;


/**
 * 用户接口
 *
 * @author Mirror.
 */
@RestController
@RequestMapping("/")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private UserQuestionStatisticsService userQuestionStatisticsService;

//    @Resource
//    private OSS ossClient;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userName = userRegisterRequest.getUserName();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userName, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }


    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    // endregion

    // region 增删改查

    /**
     * 创建用户
     *
     * @param userAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        String defaultPassword = "12345678";
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + defaultPassword).getBytes());
        user.setUserPassword(encryptPassword);
        user.setUserProfile("");
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ROOT)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest,
                                            HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        String userPassword = user.getUserPassword();
        if (StringUtils.isNotBlank(userPassword)) {
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            user.setUserPassword(encryptPassword);
        }
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取用户（仅管理员）
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 session 获取当前用户信息
     *
     * @param request
     * @return
     */
    @GetMapping("/get/my")
    public BaseResponse<User> getUserByRequest(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id, HttpServletRequest request) {
        BaseResponse<User> response = getUserById(id, request);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 分页获取用户列表（仅管理员）
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ROOT)
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                   HttpServletRequest request) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        return ResultUtils.success(userPage);
    }

    /**
     * 分页获取用户封装列表
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                       HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userService.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return ResultUtils.success(userVOPage);
    }


    /**
     * 更新个人信息
     *
     * @param userUpdateMyRequest
     * @param request
     * @return
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
                                              HttpServletRequest request) {
        if (userUpdateMyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        User user = new User();
        BeanUtils.copyProperties(userUpdateMyRequest, user);
        user.setId(loginUser.getId());
        if (Objects.equals(userUpdateMyRequest.getIsResetPassword(),true)) {
            String userPassword = UserConstant.DEFAULT_PASSWORD;
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            user.setUserPassword(encryptPassword);
        }
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新头像(仅限当前登录用户更新)
     *
     * @param file
     * @param originalUrl
     * @param request
     * @return
     */
    @PostMapping("/updateAvatar")
    public BaseResponse<Boolean> updateUserAvatar(@RequestPart("file") MultipartFile file,
                                                  @RequestPart(value = "originalUrl", required = false) String originalUrl,
                                                  HttpServletRequest request) {
        // 检查文件是否为空
        ThrowUtils.throwIf(file.isEmpty(), ErrorCode.UNPROCESSABLE_ENTITY);
        //保存文件到指定文件夹
        User loginUser = userService.getLoginUser(request);
        String originalFilename = file.getOriginalFilename();
        String extension = null;
        if (originalFilename != null) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String userAvatarUrl = FileConstant.USER_AVATAR_PREFIX + "/user_" + loginUser.getId() + extension;
        String fullFilePath = FileConstant.ROOT_PATH + userAvatarUrl;
        try {
            FileUtil.saveFileViaSFTP(file, fullFilePath);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UPLOAD_FILE_ERROR);
        }
        // 原先的OSS上传逻辑
//        String originalFilename = null;
//        if (StringUtils.isNotBlank(originalUrl)) {
//            String[] split = originalUrl.split("/");
//            if (split.length > 0) {
//                originalFilename = split[split.length - 1];
//            }
//        }
//        String avatarResult = OssUtil.uploadFile(file, originalFilename, FileConstant.USER_AVATAR_PREFIX);
        loginUser.setUserAvatar(File.separator + userAvatarUrl);
        boolean result = userService.updateById(loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 获取用户通过的题目ID列表
     * @param userId 用户ID
     * @return 题目ID列表
     */
    @GetMapping("/list/accepted")
    public BaseResponse<List<Long>> getUserAcceptedQuestions(@RequestParam("userId") Long userId) {
        // 参数校验
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效用户ID");
        }

        // 查询服务
        List<Long> questionIds = userQuestionStatisticsService.getUserAcceptedQuestions(userId);
        return ResultUtils.success(questionIds);
    }

    /**
     * 获取用户过题排行榜前五名
     * 若过题量相同，按照通过率降序排序
     * @return
     */
    @GetMapping("/rank/top5")
    public BaseResponse<List<UserRankVO>> getTop5Rank() {
        // 1. 从ZSET获取前5用户ID
        Set<String> userIds = redisTemplate.opsForZSet()
                .reverseRange(RANK_KEY, 0, 4);

        if (userIds == null || userIds.isEmpty()) {
            return ResultUtils.success(Collections.emptyList());
        }
        // 2. 批量查询用户详情
        List<UserRankVO> result = userIds.stream()
                .map(userId -> {
                    Map<Object, Object> userStats = redisTemplate.opsForHash()
                            .entries(USER_STATS_PREFIX + userId);
                    User user = userService.getById(userId);
                    System.out.println(userStats.toString());
                    return UserRankVO.builder()
                            .userAccount(user.getUserAccount())
                            .userName(user.getUserName())
                            .userAvatar(user.getUserAvatar())
                            .solvedNum(Integer.parseInt(userStats.get("solvedNum").toString()))
                            .submitNum(Integer.parseInt(userStats.get("submitNum").toString()))
                            .acceptedNum(Integer.parseInt(userStats.get("acceptedNum").toString()))
                            .build();
                })
                .collect(Collectors.toList());
        System.out.println(result);
        return ResultUtils.success(result);
    }
}
