package com.taskmanager.dto.response;

import java.util.Map;

public record ProjectReportResponse(
        Map<String, Long> byStatus,
        Map<String, Long> byPriority
) {}
