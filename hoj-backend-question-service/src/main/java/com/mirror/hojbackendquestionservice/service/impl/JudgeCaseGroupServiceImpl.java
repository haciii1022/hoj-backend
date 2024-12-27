package com.mirror.hojbackendquestionservice.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mirror.hojbackendcommon.common.ErrorCode;
import com.mirror.hojbackendcommon.constant.FileConstant;
import com.mirror.hojbackendcommon.exception.BusinessException;
import com.mirror.hojbackendmodel.model.entity.JudgeCaseFile;
import com.mirror.hojbackendmodel.model.entity.JudgeCaseGroup;
import com.mirror.hojbackendmodel.model.entity.Question;
import com.mirror.hojbackendmodel.model.vo.JudgeCaseGroupVO;
import com.mirror.hojbackendquestionservice.mapper.JudgeCaseGroupMapper;
import com.mirror.hojbackendquestionservice.service.JudgeCaseFileService;
import com.mirror.hojbackendquestionservice.service.JudgeCaseGroupService;
import com.mirror.hojbackendserverclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Mirror
 * @date 2024/11/20
 */
@Slf4j
@Service
public class JudgeCaseGroupServiceImpl extends ServiceImpl<JudgeCaseGroupMapper, JudgeCaseGroup>
        implements JudgeCaseGroupService {

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private JudgeCaseFileService judgeCaseFileService;

    @Override
    public void validJudgeCaseGroup(Long questionId) {
        // Step 1: 查询所有的 JudgeGroup 的 ID
        Set<Long> groupIdSet = this.lambdaQuery()
                .eq(JudgeCaseGroup::getQuestionId, questionId)
                .list()
                .stream()
                .map(JudgeCaseGroup::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollectionUtil.isEmpty(groupIdSet)) {
            return;
        }
        // Step 2 & 3: 查询所有与这些 groupId 相关的 JudgeCaseFile 并按 groupId 分组
        Map<Long, List<JudgeCaseFile>> groupedFiles = judgeCaseFileService.lambdaQuery()
                .in(JudgeCaseFile::getGroupId, groupIdSet)
                .list()
                .stream()
                .collect(Collectors.groupingBy(JudgeCaseFile::getGroupId));

        // Step 4: 校验每个组是否有且只有一个 in 和一个 out 文件
        int index = 0;
        for (Long groupId : groupIdSet) {
            index++;
            List<JudgeCaseFile> files =  groupedFiles.get(groupId);
            long inCount = 0;
            long outCount = 0;
            if (CollectionUtil.isNotEmpty(files)) {
                inCount = files.stream().filter(file -> Objects.equals(file.getType(), FileConstant.FILE_TYPE_IN)).count();
                outCount = files.stream().filter(file -> Objects.equals(file.getType(), FileConstant.FILE_TYPE_OUT)).count();
            }
            if (inCount != 1 || outCount != 1) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "第" + index + "组数据校验失败！每组数据必须保证有一个in和out文件。");
            }
        }
    }

    @Override
    public List<JudgeCaseGroupVO> getJudgeCaseGroupList(Question question, HttpServletRequest request) {
        // 查询 JudgeCaseGroup 数据
        List<JudgeCaseGroup> groups = this.lambdaQuery()
                .eq(JudgeCaseGroup::getQuestionId, question.getId())
                .orderByAsc(JudgeCaseGroup::getId)
                .list();
        if (ObjectUtil.isEmpty(groups)) {
            return Collections.emptyList();
        }
        // 获取 groupIds
        List<Long> groupIds = groups.stream()
                .map(JudgeCaseGroup::getId)
                .collect(Collectors.toList());
        // 查询相关的 JudgeCaseFile 数据
        List<JudgeCaseFile> files = judgeCaseFileService.lambdaQuery()
                .in(JudgeCaseFile::getGroupId, groupIds)
                .list();
        // 按 groupId 分组
        Map<Long, List<JudgeCaseFile>> filesGroupedByGroupId = Optional.ofNullable(files)
                .orElse(Collections.emptyList()).stream()
                .collect(Collectors.groupingBy(JudgeCaseFile::getGroupId));

        return groups.stream()
                .map(group -> {
                    return JudgeCaseGroupVO.objToVo(group, filesGroupedByGroupId.getOrDefault(group.getId(), null));
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Boolean deleteJudgeCaseGroup(Long groupId) {
        List<JudgeCaseFile> fileList = judgeCaseFileService.lambdaQuery()
                .eq(JudgeCaseFile::getGroupId, groupId)
                .list();
        if (CollectionUtil.isNotEmpty(fileList)) {
            fileList.forEach(item -> judgeCaseFileService.deleteJudgeCaseFile(item.getId()));
        }
        return this.removeById(groupId);
    }
}
