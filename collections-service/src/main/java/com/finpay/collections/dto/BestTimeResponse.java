package com.finpay.collections.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BestTimeResponse {

    private String bestDay;
    private String bestTimeSlot;
    private String preferredChannel;
    private String tip;
}
