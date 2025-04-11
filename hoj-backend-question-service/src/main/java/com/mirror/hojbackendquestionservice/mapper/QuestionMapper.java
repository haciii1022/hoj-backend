package com.mirror.hojbackendquestionservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mirror.hojbackendmodel.model.entity.Question;
import org.apache.ibatis.annotations.Select;


/**
* @author Mirror
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2024-06-13 10:33:08
* @Entity com.mirror.hoj.model.entity.Question
*/
public interface QuestionMapper extends BaseMapper<Question> {
    @Select("SELECT MAX(submitNum) FROM question")
    Integer selectMaxSubmitNum();
}




