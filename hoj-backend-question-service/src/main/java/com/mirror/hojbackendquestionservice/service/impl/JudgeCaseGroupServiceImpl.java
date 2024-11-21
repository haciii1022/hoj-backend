package com.mirror.hojbackendquestionservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    }

    @Override
    public List<JudgeCaseGroupVO> getJudgeCaseGroupList(Question question, HttpServletRequest request) {
        // 查询 JudgeCaseGroup 数据
        List<JudgeCaseGroup> groups = lambdaQuery()
                .eq(JudgeCaseGroup::getQuestionId, question.getId())
                .orderByAsc(JudgeCaseGroup::getId)
                .list();
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
}
