package com.finpay.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSummary {

    private long totalCount;
    private long unreadCount;
    private List<NotificationResponse> recent;
}
