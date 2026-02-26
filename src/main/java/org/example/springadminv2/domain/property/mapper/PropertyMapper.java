package org.example.springadminv2.domain.property.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.springadminv2.domain.property.dto.PropertyGroupResponse;
import org.example.springadminv2.domain.property.dto.PropertyGroupSearchRequest;
import org.example.springadminv2.domain.property.dto.PropertyHistoryResponse;
import org.example.springadminv2.domain.property.dto.PropertyHistoryVersionResponse;
import org.example.springadminv2.domain.property.dto.PropertyItemRequest;
import org.example.springadminv2.domain.property.dto.PropertyResponse;

@Mapper
public interface PropertyMapper {

    // ── 그룹 CRUD ──

    List<PropertyGroupResponse> selectPropertyGroups(@Param("req") PropertyGroupSearchRequest req);

    long countPropertyGroups(@Param("req") PropertyGroupSearchRequest req);

    List<PropertyGroupResponse> selectAllPropertyGroups();

    int existsPropertyGroup(@Param("groupId") String groupId);

    void insertPropertyGroup(
            @Param("groupId") String groupId,
            @Param("groupName") String groupName,
            @Param("lastUpdateUserId") String userId,
            @Param("lastUpdateDtime") String dtime);

    void deletePropertyGroup(@Param("groupId") String groupId);

    // ── 속성 CRUD ──

    List<PropertyResponse> selectPropertiesByGroupId(
            @Param("groupId") String groupId, @Param("searchType") String searchType, @Param("keyword") String keyword);

    void insertProperty(
            @Param("req") PropertyItemRequest req,
            @Param("groupId") String groupId,
            @Param("lastUpdateUserId") String userId,
            @Param("lastUpdateDtime") String dtime);

    void updateProperty(
            @Param("req") PropertyItemRequest req,
            @Param("groupId") String groupId,
            @Param("lastUpdateUserId") String userId,
            @Param("lastUpdateDtime") String dtime);

    void deleteProperty(@Param("groupId") String groupId, @Param("propertyId") String propertyId);

    void deletePropertiesByGroupId(@Param("groupId") String groupId);

    int existsProperty(@Param("groupId") String groupId, @Param("propertyId") String propertyId);

    // ── 히스토리/백업 ──

    int selectMaxVersion(@Param("groupId") String groupId);

    void insertBatchHistory(
            @Param("groupId") String groupId,
            @Param("version") int version,
            @Param("reason") String reason,
            @Param("lastUpdateUserId") String userId,
            @Param("lastUpdateDtime") String dtime);

    List<PropertyHistoryVersionResponse> selectHistoryVersions(@Param("groupId") String groupId);

    List<PropertyHistoryResponse> selectHistoryByVersion(
            @Param("groupId") String groupId, @Param("version") int version);

    void restoreFromHistory(
            @Param("groupId") String groupId,
            @Param("version") int version,
            @Param("lastUpdateUserId") String userId,
            @Param("lastUpdateDtime") String dtime);

    // ── 엑셀 ──

    List<PropertyResponse> selectAllPropertiesForExcel(@Param("groupId") String groupId);
}
