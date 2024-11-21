package com.mirror.hojbackendquestionservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mirror.hojbackendmodel.model.dto.file.JudgeCaseFileAddRequest;
import com.mirror.hojbackendmodel.model.entity.JudgeCaseFile;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;


/**
* @author Mirror
* @description 针对表【judge_case_file(判题用例表)】的数据库操作Service
* @createDate 2024-11-20 17:33:08
* @Entity com.mirror.hoj.model.entity.JudgeCaseFile
*/
public interface JudgeCaseFileService extends IService<JudgeCaseFile> {

    /**
     * 保存或更新文件
     * @param file
     * @param judgeCaseFileAddRequest
     * @param request
     * @return
     */
    Long saveOrUpdateFile(MultipartFile file, JudgeCaseFileAddRequest judgeCaseFileAddRequest, HttpServletRequest request);

    Boolean deleteJudgeCaseFile(Long fileId);
}
