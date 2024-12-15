package com.mirror.hojbackendmodel.model.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 判题信息消息枚举
 * @author Mirror.
 */
public enum JudgeInfoMessageEnum {

    WAITING("Waiting", 0),
    COMPILE_ERROR("Compile Error", -1),
    ACCEPTED("Accepted", 1),
    WRONG_ANSWER("Wrong Answer", 2),
    MEMORY_LIMIT_EXCEEDED("Memory Limit Exceeded", 3),
    TIME_LIMIT_EXCEEDED("Time Limit Exceeded", 4),
    RUNTIME_ERROR("Runtime Error", 5),
    SYSTEM_ERROR("System Error", 6),
    PRESENTATION_ERROR("Presentation Error", 7),
    OUTPUT_LIMIT_EXCEEDED("Output Limit Exceeded", 8),
    DANGEROUS_OPERATION("Dangerous Operation", 9);



    private final String text;

    private final int priority; // 优先级字段

    JudgeInfoMessageEnum(String text, int priority) {
        this.text = text;
        this.priority = priority;
    }

    /**
     * 获取优先级列表
     *
     * @return List<Integer>
     */
    public static List<Integer> getPriorities() {
        return Arrays.stream(values()).map(item -> item.priority).collect(Collectors.toList());
    }

    /**
     * 根据优先级获取枚举
     *
     * @param priority
     * @return JudgeInfoMessageEnum
     */
    public static JudgeInfoMessageEnum getEnumByPriority(int priority) {
        for (JudgeInfoMessageEnum anEnum : JudgeInfoMessageEnum.values()) {
            if (anEnum.priority == priority) {
                return anEnum;
            }
        }
        return null;
    }
    public int getPriority() {
        return priority;
    }

    public String getText() {
        return text;
    }
    /**
     * 根据 text 获取优先级
     *
     * @param text 消息文本
     * @return 优先级，如果未找到对应枚举则返回 -1
     */
    public static int getPriorityByText(String text) {
        if (ObjectUtils.isEmpty(text)) {
            return -1; // 表示未找到或输入为空
        }
        for (JudgeInfoMessageEnum anEnum : JudgeInfoMessageEnum.values()) {
            if (anEnum.text.equals(text)) {
                return anEnum.priority;
            }
        }
        return -1; // 未匹配到的情况
    }



}