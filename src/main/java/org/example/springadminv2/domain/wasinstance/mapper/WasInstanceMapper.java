package org.example.springadminv2.domain.wasinstance.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.example.springadminv2.domain.wasinstance.dto.WasInstanceResponse;

@Mapper
public interface WasInstanceMapper {

    List<WasInstanceResponse> selectAllInstances();
}
