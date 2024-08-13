package com.vnicy_ty.service;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


@Service
public class IpRateService {

    private final Map<String, AtomicInteger> ipCounters = new ConcurrentHashMap<>();
    private final ThreadPoolTaskScheduler taskScheduler;

    public IpRateService(ThreadPoolTaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
        scheduleDailyCleanup();
    }

    public boolean isIpAllowed(String ip) {
        AtomicInteger counter = ipCounters.computeIfAbsent(ip, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();

        System.out.println("-----");
        System.out.println("Request from IP "+ ip+": Count is now "+ count);
        System.out.println("-----");

        if (count > 20) {
            return false; // 已达到限制次数
        }

        return true;
    }

    private void scheduleDailyCleanup() {
        Runnable cleanupTask = this::cleanupExpiredRecords;
        taskScheduler.schedule(cleanupTask, new CronTrigger("0 0 0 * * ?")); // 每天凌晨0点运行
    }

    private void cleanupExpiredRecords() {
        ipCounters.clear(); // 每天清空一次
    }
}