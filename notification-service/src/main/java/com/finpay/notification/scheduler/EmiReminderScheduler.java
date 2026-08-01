package com.finpay.notification.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmiReminderScheduler {

    @Scheduled(cron = "0 0 9 * * *")
    public void checkEmiDueReminders() {
        log.info("EMI reminder scheduler running - TODO: fetch due EMIs from loan-service");
    }

    @Scheduled(cron = "0 0 10 * * *")
    public void checkOverdueEmis() {
        log.info("Overdue EMI checker running - TODO: fetch overdue EMIs from loan-service");
    }
}
