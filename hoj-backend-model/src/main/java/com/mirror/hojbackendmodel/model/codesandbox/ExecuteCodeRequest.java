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
public class ExecuteCodeRequest {

    private List<String> inputList;

    private List<String> inputFilePathList;

    private String code;

    private String language;

    /**
     * 时间限制
     * 单位ms
     */
    private Long timeLimit = 30000L;//没传时间限制的话就默认30s超时

    /**
     * 内存限制
     * 单位MB
     */
    private Long memoryLimit = 5120L;//没传内存限制的话就默认512M内存
}
