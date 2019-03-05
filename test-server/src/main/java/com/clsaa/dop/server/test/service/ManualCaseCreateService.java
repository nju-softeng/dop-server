package com.clsaa.dop.server.test.service;

import com.clsaa.dop.server.test.mapper.ManualCaseServiceMapper;
import com.clsaa.dop.server.test.model.dto.ManualCaseDto;
import com.clsaa.dop.server.test.model.po.ManualCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
public class ManualCaseCreateService extends CreateServiceImpl<ManualCase, ManualCaseDto, Long> {

    @Autowired
    public ManualCaseCreateService(JpaRepository<ManualCase, Long> repository) {
        super(ManualCaseServiceMapper.MAPPER, repository);
    }
}
