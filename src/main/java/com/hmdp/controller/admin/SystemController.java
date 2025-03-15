package com.hmdp.controller.admin;

import com.alibaba.csp.sentinel.node.Node;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import com.alibaba.csp.sentinel.util.TimeUtil;
import com.hmdp.dto.Result;
import com.hmdp.utils.BloomFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统控制器
 */
@RestController
@RequestMapping("/system")
public class SystemController {

    @Resource
    private BloomFilter bloomFilter;

    @GetMapping("/bloom/health")
    public Result checkBloomFilterHealth() {
        Map<String, Object> health = bloomFilter.bloomFilterHealthCheck();
        return Result.ok(health);
    }

    @GetMapping("/metrics")
    public Result getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // 系统CPU信息
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = osBean.getSystemLoadAverage() / osBean.getAvailableProcessors();

        metrics.put("cpuUsage", cpuLoad >= 0 ? String.format("%.2f", cpuLoad) : "N/A");
        metrics.put("processors", osBean.getAvailableProcessors());
        metrics.put("threads", Thread.activeCount());
        metrics.put("memory", Runtime.getRuntime().freeMemory() / 1024 / 1024 + "MB / "
                + Runtime.getRuntime().totalMemory() / 1024 / 1024 + "MB");

        // Sentinel统计信息 - 使用正确的API
        Map<String, Object> sentinelMetrics = new HashMap<>();

        // 获取流控规则
        List<FlowRule> flowRules = FlowRuleManager.getRules();

        // 获取资源监控信息
        List<String> resourceList = new ArrayList<>();
        resourceList.add("seckillVoucher");
        resourceList.add("createVoucherOrder");
        resourceList.add("queryShopById");

        for (String resource : resourceList) {
            Node node = ClusterBuilderSlot.getClusterNode(resource);
            if (node != null) {
                Map<String, Object> resourceMetric = new HashMap<>();
                resourceMetric.put("passQps", node.passQps());
                resourceMetric.put("blockQps", node.blockQps());
                resourceMetric.put("totalQps", node.totalQps());
                resourceMetric.put("avgRt", node.avgRt());
                resourceMetric.put("curThreadNum", node.curThreadNum());

                // 获取对应的流控规则阈值
                for (FlowRule rule : flowRules) {
                    if (resource.equals(rule.getResource())) {
                        resourceMetric.put("threshold", rule.getCount());
                        resourceMetric.put("grade", rule.getGrade() == 0 ? "QPS" : "Thread");
                        break;
                    }
                }

                sentinelMetrics.put(resource, resourceMetric);
            }
        }

        // 系统规则信息
        sentinelMetrics.put("systemRules", SystemRuleManager.getRules());
        sentinelMetrics.put("timestamp", TimeUtil.currentTimeMillis());

        metrics.put("sentinel", sentinelMetrics);

        return Result.ok(metrics);
    }

    @GetMapping("/status")
    public Result getStatus() {
        Map<String, Object> status = new HashMap<>();

        // 获取资源监控信息
        List<String> resourceList = new ArrayList<>();
        resourceList.add("seckillVoucher");
        resourceList.add("createVoucherOrder");
        resourceList.add("queryShopById");

        Map<String, Map<String, Object>> resourceStatus = new HashMap<>();

        for (String resource : resourceList) {
            Node node = ClusterBuilderSlot.getClusterNode(resource);
            if (node != null) {
                Map<String, Object> metrics = new HashMap<>();
                metrics.put("passQps", node.passQps());
                metrics.put("blockQps", node.blockQps());
                metrics.put("concurrency", node.curThreadNum());
                resourceStatus.put(resource, metrics);
            }
        }

        status.put("resources", resourceStatus);
        status.put("threadCount", Thread.activeCount());

        return Result.ok(status);
    }
}
