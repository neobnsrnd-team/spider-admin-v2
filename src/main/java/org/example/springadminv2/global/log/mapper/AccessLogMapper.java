package org.example.springadminv2.global.log.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.springadminv2.global.log.event.AccessLogEvent;

@Mapper
public interface AccessLogMapper {
    void insertAccessLog(AccessLogEvent event);
}
