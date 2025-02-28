package com.mirror.hojbackendquestionservice.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jcraft.jsch.SftpException;
import com.mirror.hojbackendcommon.common.ErrorCode;
import com.mirror.hojbackendcommon.constant.FileConstant;
import com.mirror.hojbackendcommon.exception.BusinessException;
import com.mirror.hojbackendcommon.exception.ThrowUtils;
import com.mirror.hojbackendcommon.utils.FileUtil;
import com.mirror.hojbackendcommon.utils.SeqUtil;
import com.mirror.hojbackendcommon.utils.ZipUtil;
import com.mirror.hojbackendmodel.model.dto.file.JudgeCaseFileAddRequest;
import com.mirror.hojbackendmodel.model.dto.file.JudgeCaseFileBatchImportRequest;
import com.mirror.hojbackendmodel.model.dto.file.JudgeCaseFileQueryRequest;
import com.mirror.hojbackendmodel.model.entity.JudgeCaseFile;
import com.mirror.hojbackendmodel.model.entity.JudgeCaseGroup;
import com.mirror.hojbackendmodel.model.entity.User;
import com.mirror.hojbackendmodel.model.enums.BaseSequenceEnum;
import com.mirror.hojbackendquestionservice.mapper.JudgeCaseFileMapper;
import com.mirror.hojbackendquestionservice.service.JudgeCaseFileService;
import com.mirror.hojbackendquestionservice.service.JudgeCaseGroupService;
import com.mirror.hojbackendserverclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
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
    @Transactional
    public Boolean batchImportFile(MultipartFile file, JudgeCaseFileBatchImportRequest judgeCaseFileBatchImportRequest, HttpServletRequest request) {
        String tempZipPath = FileConstant.ROOT_PATH + FileConstant.ZIP_PREFIX + FileConstant.SEPARATOR + System.currentTimeMillis();
        String extractedPath = tempZipPath + "-extracted";
        tempZipPath += ".zip";
        long questionId = judgeCaseFileBatchImportRequest.getQuestionId();
        List<File> inFiles = new ArrayList<>();
        List<File> outFiles = new ArrayList<>();
        User loginUser = userFeignClient.getLoginUser(request);
        JudgeCaseFileAddRequest judgeCaseFileAddRequest = new JudgeCaseFileAddRequest();
        judgeCaseFileAddRequest.setQuestionId(questionId);
        try {
            //先把zip文件保存下来
            FileUtil.saveFileToLocal(file,tempZipPath);
            //解压zip文件，并获取有序的inFiles和outFiles
            ZipUtil.extractAndProcessZip(tempZipPath, extractedPath, inFiles, outFiles);
            int inFileSize = inFiles.size();
            int outFileSize = outFiles.size();
            int groupCount = Math.max(inFileSize, outFileSize);
            log.info("本次批量导入共有 {} 组数据,包含 {} 个输入用例, {} 个输出用例",groupCount, inFileSize, outFileSize);
            for(int idx = 0; idx < groupCount; idx++){
                Long groupId = judgeCaseGroupService.addJudgeCaseGroup(questionId, loginUser);
                judgeCaseFileAddRequest.setGroupId(groupId);
                if(idx < inFileSize){
                    judgeCaseFileAddRequest.setType(FileConstant.FILE_TYPE_IN);
                    this.innerSaveFile(inFiles.get(idx),judgeCaseFileAddRequest,loginUser);
                }
                if(idx < outFileSize){
                    judgeCaseFileAddRequest.setType(FileConstant.FILE_TYPE_OUT);
                    this.innerSaveFile(outFiles.get(idx),judgeCaseFileAddRequest,loginUser);
                }
            }
            //保存完毕，删除临时文件
            FileUtil.deleteLocalPath(tempZipPath);
            FileUtil.deleteLocalPath(extractedPath);
        } catch (Exception e) {
            log.error("解压失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解压失败");
        }
        return true;
    }

    private void innerSaveFile(File file, JudgeCaseFileAddRequest judgeCaseFileAddRequest, User loginUser){
        long questionId = judgeCaseFileAddRequest.getQuestionId();
        long groupId = judgeCaseFileAddRequest.getGroupId();
        int type = judgeCaseFileAddRequest.getType();
        String pathPrefix = FileConstant.QUESTION_PREFIX + FileConstant.SEPARATOR + questionId;
        String fileName = groupId + "_" + type;
        String extension = type == 0 ? ".in" : ".out";
        try {
            FileUtil.saveFileViaSFTP(file, FileConstant.ROOT_PATH + pathPrefix + "/" + fileName + extension);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UPLOAD_FILE_ERROR);
        }
        JudgeCaseFile judgeCaseFile = new JudgeCaseFile();
        judgeCaseFile.setGroupId(groupId);
//        judgeCaseFile.setUrl(result);
        judgeCaseFile.setType(type);
        judgeCaseFile.setFileName(fileName);
        judgeCaseFile.setFileFolder(pathPrefix);
        judgeCaseFile.setUserId(loginUser.getId());
        judgeCaseFile.setId(SeqUtil.next(BaseSequenceEnum.JUDGE_CASE_FILE_ID.getName()));
        log.info("addJudgeCaseFile: {}", judgeCaseFile);
        boolean b = this.save(judgeCaseFile);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR);
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
                                + FileConstant.SEPARATOR
                                + Optional.ofNullable(judgeCaseFile.getFileName()).orElse("")
                                + extension
                )
                .collect(Collectors.toList());

        return fullFilePathList;
    }
}
