package com.mirror.hojbackendcommon.utils;

import cn.hutool.extra.spring.SpringUtil;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import java.sql.Types;
import java.util.Collections;
import java.util.Map;

public final class SeqUtil {

    private SeqUtil() {
    }

    private static final SimpleJdbcCall curValueCall = new SimpleJdbcCall(SpringUtil.getBean(org.springframework.jdbc.core.JdbcTemplate.class));
    private static final SimpleJdbcCall nextValueCall = new SimpleJdbcCall(SpringUtil.getBean(org.springframework.jdbc.core.JdbcTemplate.class));

    /**
     * 获取下一个序列值
     * @param seqName
     * @return
     */
    public static Long getNextValue(String seqName) {
        curValueCall.setProcedureName("curval");
        curValueCall.declareParameters(
                new SqlParameter("seq_name", Types.VARCHAR),
                new SqlOutParameter("cur_value", Types.BIGINT)
        );
        Map<String, Object> params = Collections.singletonMap("seq_name", seqName.toLowerCase());
        Map<String, Object> result = curValueCall.execute(params);
        return (Long) result.get("cur_value");
    }

    /**
     * 获取下一个序列值,并更新下一个值
     * @param seqName
     * @return
     */
    public static Long next(String seqName) {
        nextValueCall.setProcedureName("nextval");
        nextValueCall.declareParameters(
                new SqlParameter("seq_name", Types.VARCHAR),
                new SqlOutParameter("next_value", Types.BIGINT)
        );
        Map<String, Object> params = Collections.singletonMap("seq_name", seqName.toLowerCase());
        Map<String, Object> result = nextValueCall.execute(params);
        return (Long) result.get("next_value");
    }
}
