package com.example.requestmanagement.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PublicationNumberGenerator {
    private final JdbcTemplate jdbcTemplate;

    public PublicationNumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long next() {
        Number value = jdbcTemplate.queryForObject("SELECT PUBLICATION_SEQ.NEXTVAL FROM DUAL", Number.class);
        if (value == null) {
            throw new IllegalStateException("Publication sequence did not return a value");
        }
        return value.longValue();
    }
}
