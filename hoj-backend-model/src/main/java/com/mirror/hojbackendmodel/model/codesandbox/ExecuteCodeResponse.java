package com.mirror.hojbackendmodel.model.codesandbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Mirror
 * @date 2024/7/23
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteCodeResponse {

    private List<String> outputList;

    private List<String> outputFilePathList;
    /**
     * 执行信息
     */
    private String message;

    /**
     * 执行状态
     */
    private Integer status;

    /**
     * 对于每一个输入的判题信息
     */
    private List<JudgeInfo> judgeInfoList;

}
