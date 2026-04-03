package com.example.campus.service;

import com.example.campus.dto.RealtimeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RealtimeService {

    private final RedisService redisService;

    public RealtimeDTO getRealtimeMetrics() {
        RealtimeDTO dto = new RealtimeDTO();

        // 核心指标
        dto.setLibraryCount(redisService.get("library:current_count"));
        dto.setEduQps(redisService.get("edu:qps"));
        dto.setCanteenTotal10s(redisService.get("canteen:total_10s"));
        dto.setEduOnlineUsers(redisService.get("edu:online_users"));

        // 设备在线数
        dto.setDeviceOnlineCount(redisService.get("device:online_count"));
        dto.setDeviceTotalCount(redisService.get("device:total_count"));

        // 图书馆趋势（最近12次）
        List<String> trend = redisService.lrange("library:trend", 0, 11);
        dto.setLibraryTrend(trend);

        // 各食堂当日消费总额
        Map<Object, Object> dailyTotal = redisService.hgetAll("canteen:daily_total");
        Map<String, String> dailyTotalMap = new HashMap<>();
        dailyTotal.forEach((k, v) -> dailyTotalMap.put(k.toString(), v.toString()));
        dto.setCanteenDailyTotal(dailyTotalMap);

        // 食堂消费占比
        Map<Object, Object> dailyShare = redisService.hgetAll("canteen:daily_share");
        Map<String, String> dailyShareMap = new HashMap<>();
        dailyShare.forEach((k, v) -> dailyShareMap.put(k.toString(), v.toString()));
        dto.setCanteenDailyShare(dailyShareMap);

        // 热门项目销量/销售额
        Map<Object, Object> itemCount = redisService.hgetAll("item:daily_count");
        Map<String, String> itemCountMap = new HashMap<>();
        itemCount.forEach((k, v) -> itemCountMap.put(k.toString(), v.toString()));
        dto.setItemDailyCount(itemCountMap);

        Map<Object, Object> itemTotal = redisService.hgetAll("item:daily_total");
        Map<String, String> itemTotalMap = new HashMap<>();
        itemTotal.forEach((k, v) -> itemTotalMap.put(k.toString(), v.toString()));
        dto.setItemDailyTotal(itemTotalMap);

        // 设备状态
        Map<Object, Object> deviceStatus = redisService.hgetAll("device:status");
        Map<String, String> deviceStatusMap = new HashMap<>();
        deviceStatus.forEach((k, v) -> deviceStatusMap.put(k.toString(), v.toString()));
        dto.setDeviceStatus(deviceStatusMap);

        // 借阅分类计数
        Map<Object, Object> borrowCategory = redisService.hgetAll("borrow:category_count");
        Map<String, String> borrowCategoryMap = new HashMap<>();
        borrowCategory.forEach((k, v) -> borrowCategoryMap.put(k.toString(), v.toString()));
        dto.setBorrowCategoryCount(borrowCategoryMap);

        return dto;
    }

    public String getLibraryCount() {
        return redisService.get("library:current_count");
    }

    public List<String> getLibraryTrend() {
        return redisService.lrange("library:trend", 0, 11);
    }

    public String getEduQps() {
        return redisService.get("edu:qps");
    }

    public String getCanteenTotal10s() {
        return redisService.get("canteen:total_10s");
    }

    public String getEduOnlineUsers() {
        return redisService.get("edu:online_users");
    }

    public Map<Object, Object> getDeviceStatus() {
        return redisService.hgetAll("device:status");
    }

    public Map<String, String> getDeviceOnlineSummary() {
        Map<String, String> summary = new HashMap<>();
        summary.put("online", redisService.get("device:online_count"));
        summary.put("total", redisService.get("device:total_count"));
        return summary;
    }
    
    public Map<Object, Object> getCanteenDailyShare() {
        return redisService.hgetAll("canteen:daily_share");
    }

    public Map<Object, Object> getItemDailyCount() {
        return redisService.hgetAll("item:daily_count");
    }

    public Map<Object, Object> getItemDailyTotal() {
        return redisService.hgetAll("item:daily_total");
    }

    public Map<Object, Object> getBorrowCategoryCount() {
        return redisService.hgetAll("borrow:category_count");
    }
}
