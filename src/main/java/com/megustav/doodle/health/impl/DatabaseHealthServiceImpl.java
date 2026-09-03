package com.megustav.doodle.health.impl;

import com.megustav.doodle.health.DatabaseHealthRepository;
import com.megustav.doodle.health.DatabaseHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseHealthServiceImpl implements DatabaseHealthService {

    private final DatabaseHealthRepository repository;

    @Override
    public boolean isDatabaseHealthy() {
        return repository.getVersion() != null;
    }

}
