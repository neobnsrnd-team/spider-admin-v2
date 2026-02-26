package org.example.springadminv2.domain.wasinstance.service;

import java.util.List;

import org.example.springadminv2.domain.wasinstance.dto.WasInstanceResponse;
import org.example.springadminv2.domain.wasinstance.mapper.WasInstanceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WasInstanceService {

    private final WasInstanceMapper wasInstanceMapper;

    @Transactional(readOnly = true)
    public List<WasInstanceResponse> getAllInstances() {
        return wasInstanceMapper.selectAllInstances();
    }
}
