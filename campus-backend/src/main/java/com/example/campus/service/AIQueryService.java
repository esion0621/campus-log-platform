package com.example.campus.service;

import com.example.campus.dto.LibraryRankDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class AIQueryService {

    private final RealtimeService realtimeService;
    private final ReportService reportService;

    public AIQueryService(RealtimeService realtimeService, ReportService reportService) {
        this.realtimeService = realtimeService;
        this.reportService = reportService;
    }

    public String fetchContext(String question) {
        StringBuilder context = new StringBuilder();
        String lowerQuestion = question.toLowerCase();

        // ========== 图书馆人数 ==========
        if (containsAny(lowerQuestion, "图书馆", "在馆", "自习室", "阅览室", "人数", "多少人", "拥挤")) {
            String count = realtimeService.getLibraryCount();
            if (isValid(count)) {
                context.append("当前图书馆在馆人数：").append(count).append(" 人。\n");
                // 可附带趋势信息
                List<String> trend = realtimeService.getLibraryTrend();
                if (trend != null && !trend.isEmpty()) {
                    context.append("最近5次趋势：").append(String.join(" → ", trend)).append("\n");
                }
            } else {
                context.append("图书馆人数数据暂未获取到。\n");
            }
        }

        // ========== 教务系统 ==========
        if (containsAny(lowerQuestion, "教务", "选课", "成绩", "QPS", "并发", "访问量", "在线用户")) {
            String qps = realtimeService.getEduQps();
            String online = realtimeService.getEduOnlineUsers();
            if (isValid(qps)) {
                context.append("教务系统当前 QPS：").append(qps).append("，");
            }
            if (isValid(online)) {
                context.append("在线用户数：").append(online).append("\n");
            }
        }

        // ========== 消费数据 ==========
        if (containsAny(lowerQuestion, "消费", "食堂", "饭钱", "金额", "花了多少钱", "营业额", "销售额")) {
            String total = realtimeService.getCanteenTotal10s();
            if (isValid(total)) {
                context.append("今日校园消费总额：").append(total).append(" 元。\n");
            }

            Map<Object, Object> share = realtimeService.getCanteenDailyShare();
            if (share != null && !share.isEmpty()) {
                context.append("各食堂今日消费占比：\n");
                share.forEach((k, v) -> context.append(k).append("：").append(v).append("元\n"));
            }

            Map<Object, Object> items = realtimeService.getItemDailyCount();
            if (items != null && !items.isEmpty()) {
                context.append("热门消费项目（今日销量）：\n");
                items.entrySet().stream()
                        .sorted((a, b) -> Long.compare(
                                Long.parseLong(b.getValue().toString()),
                                Long.parseLong(a.getValue().toString())))
                        .limit(3)
                        .forEach(e -> context.append(e.getKey()).append("：").append(e.getValue()).append("次\n"));
            }
        }

        // ========== 设备状态 ==========
        if (containsAny(lowerQuestion, "设备", "在线", "离线", "故障", "闸机", "服务器", "终端")) {
            Map<String, String> summary = realtimeService.getDeviceOnlineSummary();
            if (summary != null) {
                context.append("设备在线率：").append(summary.get("online")).append("/")
                        .append(summary.get("total")).append(" 台在线。\n");
            }

            Map<Object, Object> status = realtimeService.getDeviceStatus();
            if (status != null && !status.isEmpty()) {
                long offlineCount = status.values().stream().filter("offline"::equals).count();
                if (offlineCount > 0) {
                    context.append("离线设备：");
                    status.entrySet().stream()
                            .filter(e -> "offline".equals(e.getValue()))
                            .limit(5)
                            .forEach(e -> context.append(e.getKey()).append(" "));
                    context.append("\n");
                }
            }
        }

        // ========== 周报排行 ==========
        if (containsAny(lowerQuestion, "排行", "最多", "最高", "哪个学院", "top", "冠军", "榜首", "入馆", "消费排行")) {
            String week = getCurrentWeek();
            List<LibraryRankDTO> libraryRanks = reportService.getLibraryWeeklyRank(week);
            if (libraryRanks != null && !libraryRanks.isEmpty()) {
                context.append("本周学院入馆排行（前五）：\n");
                libraryRanks.stream().limit(5).forEach(r ->
                        context.append(r.getCollegeName()).append("：").append(r.getAccessCount()).append(" 人次\n")
                );
            }
        }

        // ========== 借阅数据 ==========
        if (containsAny(lowerQuestion, "借阅", "图书", "书", "热门书籍", "分类")) {
            Map<Object, Object> borrow = realtimeService.getBorrowCategoryCount();
            if (borrow != null && !borrow.isEmpty()) {
                context.append("各类图书借阅次数：\n");
                borrow.forEach((k, v) -> context.append(k).append("：").append(v).append("次\n"));
            }
        }

        return context.toString();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private boolean isValid(String value) {
        return value != null && !value.isEmpty() && !"null".equalsIgnoreCase(value);
    }

    private String getCurrentWeek() {
        LocalDate now = LocalDate.now();
        int week = now.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
        return now.getYear() + "-W" + String.format("%02d", week);
    }
}
