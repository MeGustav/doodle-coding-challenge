package com.megustav.health.impl;

import com.megustav.health.DatabaseHealthRepository;
import com.megustav.health.DatabaseHealthService;
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
