package com.mirror.hojbackendmodel.model.dto.file;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Mirror
 * @date 2024/11/20
 */
@Data
public class JudgeCaseFileBatchImportRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long questionId;

}
