package org.example.springadminv2.domain.wasproperty.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertyHistoryResponse;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertyHistoryVersionResponse;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertyResponse;

@Mapper
public interface WasPropertyMapper {

    List<WasPropertyResponse> selectWasProperties(
            @Param("groupId") String groupId, @Param("propertyId") String propertyId);

    void mergeWasProperty(
            @Param("instanceId") String instanceId,
            @Param("groupId") String groupId,
            @Param("propertyId") String propertyId,
            @Param("value") String value,
            @Param("desc") String desc);

    int selectWasMaxVersion(@Param("groupId") String groupId, @Param("instanceId") String instanceId);

    void insertBatchWasHistory(
            @Param("groupId") String groupId,
            @Param("instanceId") String instanceId,
            @Param("version") int version,
            @Param("reason") String reason,
            @Param("lastUpdateUserId") String userId,
            @Param("lastUpdateDtime") String dtime);

    List<WasPropertyHistoryVersionResponse> selectWasHistoryVersions(
            @Param("groupId") String groupId, @Param("instanceId") String instanceId);

    List<WasPropertyHistoryResponse> selectWasHistoryByVersion(
            @Param("groupId") String groupId, @Param("instanceId") String instanceId, @Param("version") int version);

    List<WasPropertyResponse> selectCurrentWasProperties(
            @Param("groupId") String groupId, @Param("instanceId") String instanceId);

    void restoreWasFromHistory(
            @Param("groupId") String groupId,
            @Param("instanceId") String instanceId,
            @Param("version") int version,
            @Param("lastUpdateUserId") String userId,
            @Param("lastUpdateDtime") String dtime);

    void deleteWasPropertiesByGroupId(@Param("groupId") String groupId);

    void deleteWasPropertiesByInstanceAndGroup(
            @Param("groupId") String groupId, @Param("instanceId") String instanceId);
}
