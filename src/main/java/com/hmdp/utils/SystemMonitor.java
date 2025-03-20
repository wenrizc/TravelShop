package com.hmdp.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 系统资源监控工具
 * 用于监控CPU、内存使用率，避免在系统高负载时执行重量级任务
 */
@Slf4j
@Component
public class SystemMonitor {

    // 阈值配置，可从配置文件注入
    @Value("${system.monitor.cpu.threshold:80.0}")
    private double cpuThreshold;

    @Value("${system.monitor.memory.threshold:85.0}")
    private double memoryThreshold;

    // 采样间隔(秒)
    @Value("${system.monitor.sampling.interval:5}")
    private int samplingInterval;

    // 警告阈值百分比(与最大阈值的比例)
    @Value("${system.monitor.warning.ratio:0.8}")
    private double warningRatio;

    private final OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private final Runtime runtime = Runtime.getRuntime();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "system-monitor-thread");
        t.setDaemon(true);
        return t;
    });

    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");

    // 最近一次的系统指标 - 使用原子引用保证线程安全
    private final AtomicReference<SystemMetrics> lastMetrics = new AtomicReference<>();

    // 历史采样数据 - 用于评估趋势
    private final Map<String, Double> historicalData = new HashMap<>();

    /**
     * 系统指标数据类
     */
    @Data
    public static class SystemMetrics {
        private double cpuUsage;      // CPU使用率 (0-100%)
        private double memoryUsage;   // 内存使用率 (0-100%)
        private long totalMemory;     // 总内存 (bytes)
        private long usedMemory;      // 已用内存 (bytes)
        private long maxMemory;       // 最大可用内存 (bytes)
        private long freeMemory;      // 空闲内存 (bytes)
        private double systemLoad;    // 系统负载
        private int threadCount;      // 当前线程数
        private long timestamp;       // 采集时间戳

        // 检查是否超过指定阈值
        public boolean isOverloaded(double cpuThreshold, double memoryThreshold) {
            return cpuUsage > cpuThreshold || memoryUsage > memoryThreshold;
        }

        @Override
        public String toString() {
            return String.format("CPU: %.2f%%, Memory: %.2f%% (%dMB/%dMB), System Load: %.2f, Threads: %d",
                    cpuUsage, memoryUsage,
                    usedMemory / (1024 * 1024), maxMemory / (1024 * 1024),
                    systemLoad, threadCount);
        }
    }

    @PostConstruct
    public void init() {
        // 初始采集一次系统指标
        SystemMetrics metrics1 = collectMetrics();
        lastMetrics.set(metrics1);

        // 定期采集系统指标
        scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        SystemMetrics metrics = collectMetrics();
                        lastMetrics.set(metrics);

                        // 记录历史数据用于趋势分析
                        updateHistoricalData(metrics);

                        // 当系统接近阈值时输出警告日志
                        checkWarningThresholds(metrics);

                        // 定期输出系统资源情况（调试级别）
                        log.debug("系统资源使用情况: {}", metrics);
                    } catch (Exception e) {
                        log.error("监控系统资源时发生异常", e);
                    }
                },
                0, samplingInterval, TimeUnit.SECONDS
        );

        log.info("系统监控已启动，采样间隔: {}秒, CPU阈值: {}%, 内存阈值: {}%",
                samplingInterval, cpuThreshold, memoryThreshold);
    }

    /**
     * 收集系统指标
     */
    private SystemMetrics collectMetrics() {
        SystemMetrics metrics = new SystemMetrics();

        // 采集CPU使用率
        metrics.setCpuUsage(osBean.getSystemCpuLoad() * 100);

        // 采集内存使用情况
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        metrics.setMaxMemory(maxMemory);
        metrics.setTotalMemory(totalMemory);
        metrics.setUsedMemory(usedMemory);
        metrics.setFreeMemory(freeMemory);
        metrics.setMemoryUsage((double) usedMemory / maxMemory * 100);

        // 系统平均负载
        metrics.setSystemLoad(osBean.getSystemLoadAverage());

        // 当前线程数
        metrics.setThreadCount(Thread.activeCount());

        // 采集时间戳
        metrics.setTimestamp(System.currentTimeMillis());

        return metrics;
    }

    /**
     * 检查是否达到警告阈值
     */
    private void checkWarningThresholds(SystemMetrics metrics) {
        double cpuWarningThreshold = cpuThreshold * warningRatio;
        double memoryWarningThreshold = memoryThreshold * warningRatio;

        if (metrics.getCpuUsage() > cpuWarningThreshold && metrics.getCpuUsage() <= cpuThreshold) {
            log.warn("CPU使用率接近阈值: {}% (警告阈值: {}%, 最大阈值: {}%)",
                    decimalFormat.format(metrics.getCpuUsage()),
                    decimalFormat.format(cpuWarningThreshold),
                    decimalFormat.format(cpuThreshold));
        }

        if (metrics.getMemoryUsage() > memoryWarningThreshold && metrics.getMemoryUsage() <= memoryThreshold) {
            log.warn("内存使用率接近阈值: {}% (警告阈值: {}%, 最大阈值: {}%)",
                    decimalFormat.format(metrics.getMemoryUsage()),
                    decimalFormat.format(memoryWarningThreshold),
                    decimalFormat.format(memoryThreshold));
        }

        if (metrics.getCpuUsage() > cpuThreshold) {
            log.error("CPU使用率超过阈值: {}% (阈值: {}%)",
                    decimalFormat.format(metrics.getCpuUsage()),
                    decimalFormat.format(cpuThreshold));
        }

        if (metrics.getMemoryUsage() > memoryThreshold) {
            log.error("内存使用率超过阈值: {}% (阈值: {}%)",
                    decimalFormat.format(metrics.getMemoryUsage()),
                    decimalFormat.format(memoryThreshold));
        }
    }

    /**
     * 更新历史数据用于趋势分析
     */
    private void updateHistoricalData(SystemMetrics metrics) {
        // 保存过去10分钟的CPU使用率和内存使用率趋势
        String timeKey = String.valueOf(System.currentTimeMillis() / 60000); // 按分钟分组
        historicalData.put("cpu:" + timeKey, metrics.getCpuUsage());
        historicalData.put("memory:" + timeKey, metrics.getMemoryUsage());

        // 清理1小时前的数据
        long oneHourAgo = (System.currentTimeMillis() - 3600000) / 60000;
        historicalData.entrySet().removeIf(entry -> {
            String[] parts = entry.getKey().split(":");
            return parts.length > 1 && Long.parseLong(parts[1]) < oneHourAgo;
        });
    }

    /**
     * 获取当前系统指标
     */
    public SystemMetrics getCurrentMetrics() {
        return collectMetrics();
    }

    /**
     * 检查系统是否过载
     * @return 如果CPU或内存使用率超过阈值，返回true
     */
    public boolean isSystemOverloaded() {
        SystemMetrics metrics = collectMetrics();
        boolean overloaded = metrics.isOverloaded(cpuThreshold, memoryThreshold);

        if (overloaded) {
            log.warn("系统资源使用率过高: {}", metrics);
        }

        return overloaded;
    }

    /**
     * 检查系统负载是否处于增长趋势
     * @return 如果负载呈现上升趋势，返回true
     */
    public boolean isLoadIncreasing() {
        try {
            // 获取最近5分钟的CPU记录，计算趋势
            long now = System.currentTimeMillis() / 60000;
            double current = historicalData.getOrDefault("cpu:" + now, 0.0);
            double fiveMinAgo = historicalData.getOrDefault("cpu:" + (now - 5), 0.0);

            return (current - fiveMinAgo) > 10.0; // CPU在5分钟内上升了10%以上
        } catch (Exception e) {
            log.error("计算系统负载趋势时发生错误", e);
            return false;
        }
    }

    /**
     * 获取最后一次采样的系统指标
     */
    public SystemMetrics getLastMetrics() {
        SystemMetrics metrics = lastMetrics.get();
        return metrics != null ? metrics : collectMetrics();
    }

    /**
     * 获取格式化的系统状态描述
     */
    public String getSystemStatusDescription() {
        SystemMetrics metrics = getLastMetrics();
        StringBuilder sb = new StringBuilder();

        sb.append("系统状态:\n");
        sb.append("- CPU使用率: ").append(decimalFormat.format(metrics.getCpuUsage())).append("%\n");
        sb.append("- 内存使用率: ").append(decimalFormat.format(metrics.getMemoryUsage())).append("%\n");
        sb.append("- 已用内存: ").append(formatSize(metrics.getUsedMemory())).append("\n");
        sb.append("- 总内存: ").append(formatSize(metrics.getTotalMemory())).append("\n");
        sb.append("- 最大可用内存: ").append(formatSize(metrics.getMaxMemory())).append("\n");
        sb.append("- 系统负载: ").append(decimalFormat.format(metrics.getSystemLoad())).append("\n");
        sb.append("- 线程数: ").append(metrics.getThreadCount());

        return sb.toString();
    }

    /**
     * 获取所有监控数据，用于API返回
     */
    public Map<String, Object> getMonitorData() {
        SystemMetrics metrics = getLastMetrics();
        Map<String, Object> data = new HashMap<>();

        data.put("cpuUsage", metrics.getCpuUsage());
        data.put("memoryUsage", metrics.getMemoryUsage());
        data.put("usedMemory", metrics.getUsedMemory());
        data.put("totalMemory", metrics.getTotalMemory());
        data.put("freeMemory", metrics.getFreeMemory());
        data.put("maxMemory", metrics.getMaxMemory());
        data.put("systemLoad", metrics.getSystemLoad());
        data.put("threadCount", metrics.getThreadCount());
        data.put("timestamp", metrics.getTimestamp());
        data.put("overloaded", metrics.isOverloaded(cpuThreshold, memoryThreshold));
        data.put("cpuThreshold", cpuThreshold);
        data.put("memoryThreshold", memoryThreshold);
        data.put("loadIncreasing", isLoadIncreasing());

        return data;
    }

    /**
     * 格式化字节大小为人类可读形式
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * 关闭监控
     */
    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        log.info("系统监控已关闭");
    }
}