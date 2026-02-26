package org.example.springadminv2.domain.property.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.example.springadminv2.domain.property.dto.PropertyBackupRequest;
import org.example.springadminv2.domain.property.dto.PropertyGroupCreateRequest;
import org.example.springadminv2.domain.property.dto.PropertyGroupResponse;
import org.example.springadminv2.domain.property.dto.PropertyGroupSearchRequest;
import org.example.springadminv2.domain.property.dto.PropertyHistoryResponse;
import org.example.springadminv2.domain.property.dto.PropertyHistoryVersionResponse;
import org.example.springadminv2.domain.property.dto.PropertyItemRequest;
import org.example.springadminv2.domain.property.dto.PropertyResponse;
import org.example.springadminv2.domain.property.dto.PropertySaveRequest;
import org.example.springadminv2.domain.property.mapper.PropertyMapper;
import org.example.springadminv2.domain.wasproperty.mapper.WasPropertyMapper;
import org.example.springadminv2.global.dto.ExcelColumnDefinition;
import org.example.springadminv2.global.dto.PageResponse;
import org.example.springadminv2.global.exception.BaseException;
import org.example.springadminv2.global.exception.ErrorType;
import org.example.springadminv2.global.util.ExcelExportUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PropertyMapper propertyMapper;
    private final WasPropertyMapper wasPropertyMapper;

    // ── 그룹 CRUD ──

    @Transactional(readOnly = true)
    public PageResponse<PropertyGroupResponse> getPropertyGroups(PropertyGroupSearchRequest req) {
        long total = propertyMapper.countPropertyGroups(req);
        List<PropertyGroupResponse> content = propertyMapper.selectPropertyGroups(req);
        return PageResponse.of(content, total, req.page(), req.size());
    }

    @Transactional(readOnly = true)
    public List<PropertyGroupResponse> getAllPropertyGroups() {
        return propertyMapper.selectAllPropertyGroups();
    }

    @Transactional(readOnly = true)
    public boolean existsPropertyGroup(String groupId) {
        return propertyMapper.existsPropertyGroup(groupId) > 0;
    }

    @Transactional
    public void createPropertyGroup(PropertyGroupCreateRequest request, String userId) {
        if (propertyMapper.existsPropertyGroup(request.groupId()) > 0) {
            throw new BaseException(ErrorType.DUPLICATE_RESOURCE, "groupId=" + request.groupId());
        }
        String now = LocalDateTime.now().format(TIMESTAMP_FMT);
        if (request.properties() != null) {
            for (PropertyItemRequest item : request.properties()) {
                propertyMapper.insertProperty(item, request.groupId(), userId, now);
            }
        }
    }

    @Transactional
    public void deletePropertyGroup(String groupId) {
        if (propertyMapper.existsPropertyGroup(groupId) == 0) {
            throw new BaseException(ErrorType.RESOURCE_NOT_FOUND, "groupId=" + groupId);
        }
        wasPropertyMapper.deleteWasPropertiesByGroupId(groupId);
        propertyMapper.deletePropertiesByGroupId(groupId);
    }

    // ── 속성 CRUD ──

    @Transactional(readOnly = true)
    public List<PropertyResponse> getPropertiesByGroup(String groupId, String searchType, String keyword) {
        return propertyMapper.selectPropertiesByGroupId(groupId, searchType, keyword);
    }

    @Transactional
    public void saveProperties(PropertySaveRequest request, String userId) {
        String now = LocalDateTime.now().format(TIMESTAMP_FMT);
        if (request.items() == null) return;

        for (PropertyItemRequest item : request.items()) {
            switch (item.crud() != null ? item.crud() : "") {
                case "C" -> propertyMapper.insertProperty(item, request.groupId(), userId, now);
                case "U" -> propertyMapper.updateProperty(item, request.groupId(), userId, now);
                case "D" -> {
                    wasPropertyMapper.deleteWasPropertiesByInstanceAndGroup(request.groupId(), null);
                    propertyMapper.deleteProperty(request.groupId(), item.propertyId());
                }
                default -> {}
            }
        }
    }

    // ── 히스토리/백업 ──

    @Transactional
    public void backupPropertyGroup(String groupId, PropertyBackupRequest request, String userId) {
        String now = LocalDateTime.now().format(TIMESTAMP_FMT);
        int maxVersion = propertyMapper.selectMaxVersion(groupId);
        propertyMapper.insertBatchHistory(groupId, maxVersion + 1, request.reason(), userId, now);
    }

    @Transactional(readOnly = true)
    public List<PropertyHistoryVersionResponse> getHistoryVersions(String groupId) {
        return propertyMapper.selectHistoryVersions(groupId);
    }

    @Transactional(readOnly = true)
    public List<PropertyHistoryResponse> getHistoryByVersion(String groupId, int version) {
        return propertyMapper.selectHistoryByVersion(groupId, version);
    }

    @Transactional
    public void restorePropertyGroup(String groupId, int version, String userId) {
        String now = LocalDateTime.now().format(TIMESTAMP_FMT);
        propertyMapper.deletePropertiesByGroupId(groupId);
        propertyMapper.restoreFromHistory(groupId, version, userId, now);
    }

    // ── 엑셀 ──

    @Transactional(readOnly = true)
    public byte[] exportExcel(String groupId) throws IOException {
        List<PropertyResponse> data = propertyMapper.selectAllPropertiesForExcel(groupId);

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("그룹ID", "groupId", 20),
                new ExcelColumnDefinition("속성ID", "propertyId", 30),
                new ExcelColumnDefinition("속성명", "propertyName", 25),
                new ExcelColumnDefinition("설명", "propertyDesc", 40),
                new ExcelColumnDefinition("데이터타입", "dataType", 12),
                new ExcelColumnDefinition("유효값", "validData", 30),
                new ExcelColumnDefinition("기본값", "defaultValue", 30));

        List<Map<String, Object>> dataList = data.stream()
                .map(p -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("groupId", p.groupId());
                    row.put("propertyId", p.propertyId());
                    row.put("propertyName", p.propertyName());
                    row.put("propertyDesc", p.propertyDesc());
                    row.put("dataType", p.dataType());
                    row.put("validData", p.validData());
                    row.put("defaultValue", p.defaultValue());
                    return row;
                })
                .toList();

        return ExcelExportUtil.createWorkbook("Properties", columns, dataList);
    }

    @Transactional(readOnly = true)
    public String exportFile(String groupId, String format) {
        List<PropertyResponse> data = propertyMapper.selectAllPropertiesForExcel(groupId);
        StringBuilder sb = new StringBuilder();

        if ("yaml".equalsIgnoreCase(format)) {
            String currentGroup = null;
            for (PropertyResponse p : data) {
                if (!p.groupId().equals(currentGroup)) {
                    if (currentGroup != null) sb.append("\n");
                    sb.append("# ").append(p.groupId()).append("\n");
                    currentGroup = p.groupId();
                }
                sb.append(p.propertyId())
                        .append(": ")
                        .append(p.defaultValue() != null ? p.defaultValue() : "")
                        .append("\n");
            }
        } else {
            for (PropertyResponse p : data) {
                sb.append(p.groupId())
                        .append(".")
                        .append(p.propertyId())
                        .append("=")
                        .append(p.defaultValue() != null ? p.defaultValue() : "")
                        .append("\n");
            }
        }
        return sb.toString();
    }

    public String generateExcelFileName() {
        return ExcelExportUtil.generateFileName("property", LocalDate.now());
    }
}
