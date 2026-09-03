package com.megustav.health;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DatabaseHealthRepository {

    private final JdbcTemplate jdbcTemplate;

    public String getVersion() {
        return jdbcTemplate.queryForObject("SELECT version()", String.class);
    }
}