package com.mirror.hojbackendquestionservice.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jcraft.jsch.SftpException;
import com.mirror.hojbackendcommon.common.ErrorCode;
import com.mirror.hojbackendcommon.constant.FileConstant;
import com.mirror.hojbackendcommon.exception.BusinessException;
import com.mirror.hojbackendcommon.exception.ThrowUtils;
import com.mirror.hojbackendcommon.utils.FileUtil;
import com.mirror.hojbackendcommon.utils.OssUtil;
import com.mirror.hojbackendcommon.utils.SeqUtil;
import com.mirror.hojbackendmodel.model.dto.file.JudgeCaseFileAddRequest;
import com.mirror.hojbackendmodel.model.dto.file.JudgeCaseFileQueryRequest;
import com.mirror.hojbackendmodel.model.entity.JudgeCaseFile;
import com.mirror.hojbackendmodel.model.entity.JudgeCaseGroup;
import com.mirror.hojbackendmodel.model.entity.Question;
import com.mirror.hojbackendmodel.model.entity.User;
import com.mirror.hojbackendmodel.model.enums.BaseSequenceEnum;
import com.mirror.hojbackendmodel.model.vo.JudgeCaseGroupVO;
import com.mirror.hojbackendquestionservice.mapper.JudgeCaseFileMapper;
import com.mirror.hojbackendquestionservice.service.JudgeCaseFileService;
import com.mirror.hojbackendquestionservice.service.JudgeCaseGroupService;
import com.mirror.hojbackendserverclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jni.File;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Mirror
 * @date 2024/11/20
 */
@Slf4j
@Service
public class JudgeCaseFileServiceImpl extends ServiceImpl<JudgeCaseFileMapper, JudgeCaseFile>
        implements JudgeCaseFileService {

    @Resource
    private UserFeignClient userFeignClient;

    @Lazy
    @Resource
    private JudgeCaseGroupService judgeCaseGroupService;

    @Override
    @Transactional
    public Long saveOrUpdateFile(MultipartFile file, JudgeCaseFileAddRequest judgeCaseFileAddRequest, HttpServletRequest request) {
        User loginUser = userFeignClient.getLoginUser(request);
        long questionId = judgeCaseFileAddRequest.getQuestionId();
        long groupId = judgeCaseFileAddRequest.getGroupId();
        int type = judgeCaseFileAddRequest.getType();
        String pathPrefix = FileConstant.QUESTION_PREFIX + FileConstant.SEPARATOR + questionId;
        String fileName = groupId + "_" + type;
        String extension = type == 0 ? ".in" : ".out";
//        String result = OssUtil.uploadFile(file, fileName, pathPrefix);
        try {
            FileUtil.saveFileViaSFTP(file, FileConstant.ROOT_PATH + pathPrefix + "/" + fileName + extension);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UPLOAD_FILE_ERROR);
        }
        // 原先OSS的上传逻辑
//        if (FileConstant.UPLOAD_FAIL.equals(result)) {
//            throw new BusinessException(ErrorCode.UPLOAD_FILE_ERROR);
//        }
        // 判断数据库中是否已经有该数据
        List<JudgeCaseFile> fileList = lambdaQuery()
                .eq(JudgeCaseFile::getGroupId, groupId)
                .eq(JudgeCaseFile::getType, type)
                .list();
        JudgeCaseFile judgeCaseFile = new JudgeCaseFile();
        judgeCaseFile.setGroupId(groupId);
//        judgeCaseFile.setUrl(result);
        judgeCaseFile.setType(type);
        judgeCaseFile.setFileName(fileName);
        judgeCaseFile.setFileFolder(pathPrefix);
        judgeCaseFile.setUserId(loginUser.getId());
        if (CollectionUtil.isNotEmpty(fileList)) {
            if (fileList.size() != 1) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            judgeCaseFile.setId(fileList.get(0).getId());
            log.info("updateJudgeCaseFile: {}", judgeCaseFile);
        } else {
            judgeCaseFile.setId(SeqUtil.next(BaseSequenceEnum.JUDGE_CASE_FILE_ID.getName()));
            log.info("addJudgeCaseFile: {}", judgeCaseFile);
        }
        boolean b = this.saveOrUpdate(judgeCaseFile);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR);
        return judgeCaseFile.getId();
    }

    @Override
    public Boolean deleteJudgeCaseFile(Long fileId) {
        JudgeCaseFile judgeCaseFile = this.getById(fileId);
        String fileName = judgeCaseFile.getFileFolder()
                + FileConstant.SEPARATOR
                + judgeCaseFile.getFileName()
                + (Objects.equals(judgeCaseFile.getType(), FileConstant.FILE_TYPE_IN) ? ".in" : ".out");
        String fullFilePath = FileConstant.ROOT_PATH + fileName;
        try {
            FileUtil.deleteFileViaSFTP(fullFilePath);
        } catch (SftpException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        // 原先OSS的删除逻辑
//        Boolean result = OssUtil.deleteFile(filePath);
//        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return this.removeById(fileId);
    }

    @Override
    public List<String> getJudgeCaseFileListWithType(JudgeCaseFileQueryRequest judgeCaseFileQueryRequest) {
        Long questionId = judgeCaseFileQueryRequest.getQuestionId();
        List<Long> groupIdList = judgeCaseGroupService.lambdaQuery()
                .eq(JudgeCaseGroup::getQuestionId, questionId)
                .orderByAsc(JudgeCaseGroup::getId)
                .list()
                .stream()
                .map(JudgeCaseGroup::getId)
                .collect(Collectors.toList());

        Integer type = judgeCaseFileQueryRequest.getType();
        String extension = type == 0 ? ".in" : ".out";

        List<String> fullFilePathList = lambdaQuery()
                .in(JudgeCaseFile::getGroupId, groupIdList)
                .eq(JudgeCaseFile::getType, type)
                .list()
                .stream()
                .map(judgeCaseFile ->
                        FileConstant.ROOT_PATH
                                + Optional.ofNullable(judgeCaseFile.getFileFolder()).orElse("")
                                + Optional.ofNullable(judgeCaseFile.getFileName()).orElse("")
                                + extension
                )
                .collect(Collectors.toList());

        return fullFilePathList;
    }
}
