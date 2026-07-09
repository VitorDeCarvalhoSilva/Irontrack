package com.irontrack.api.services.impl;

import com.irontrack.api.services.PushSubscriptionCleanupService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class JdbcPushSubscriptionCleanupService implements PushSubscriptionCleanupService {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPushSubscriptionCleanupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void deleteAllForUser(String userId) {
        jdbcTemplate.update("DELETE FROM push_subscriptions WHERE user_id = ?", userId);
    }
}
