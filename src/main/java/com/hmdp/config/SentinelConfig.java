package com.hmdp.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
public class SentinelConfig {

    @Value("${sentinel.flow.concurrency-threshold:1600}")
    private int concurrencyThreshold;

    @Value("${sentinel.system.cpu-usage:0.75}")
    private double cpuUsageThreshold;

    @PostConstruct
    public void init() {
        initFlowRules();
        initDegradeRules();
        initSystemRules();
        log.info("Sentinel规则初始化完成，接口限流阈值: {}, CPU使用率阈值: {}",
                concurrencyThreshold, cpuUsageThreshold);
    }

    /**
     * 初始化流控规则
     * 特别针对高并发接口和1600并发塌陷点
     */
    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 1. 秒杀接口流控规则 - 基于QPS
        FlowRule seckillRule = new FlowRule();
        seckillRule.setResource("seckillVoucher");
        seckillRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        seckillRule.setCount(concurrencyThreshold); // 阈值设置为1600
        seckillRule.setLimitApp("default");
        seckillRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_WARM_UP);
        seckillRule.setWarmUpPeriodSec(10);
        rules.add(seckillRule);

        // 2. 下单接口并发数流控规则 - 基于并发线程数
        FlowRule orderRule = new FlowRule();
        orderRule.setResource("createVoucherOrder");
        orderRule.setGrade(RuleConstant.FLOW_GRADE_THREAD);
        orderRule.setCount(concurrencyThreshold * 0.8); // 设置为阈值的80%，提前预防
        orderRule.setLimitApp("default");
        orderRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        rules.add(orderRule);

        // 3. 查询店铺接口限流规则
        FlowRule shopRule = new FlowRule();
        shopRule.setResource("queryShopById");
        shopRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        shopRule.setCount(concurrencyThreshold * 1.2); // 查询可以稍宽松
        shopRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        rules.add(shopRule);

        FlowRuleManager.loadRules(rules);
    }

    /**
     * 初始化熔断降级规则
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // 秒杀接口熔断规则
        DegradeRule seckillRule = new DegradeRule();
        seckillRule.setResource("seckillVoucher");
        seckillRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);  // 响应时间
        seckillRule.setCount(100);  // 阈值100ms
        seckillRule.setTimeWindow(10);  // 熔断时长10s
        seckillRule.setMinRequestAmount(5); // 最小请求数
        rules.add(seckillRule);

        // 下单接口异常熔断规则
        DegradeRule orderRule = new DegradeRule();
        orderRule.setResource("createVoucherOrder");
        orderRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        orderRule.setCount(0.2); // 异常比例阈值0.2
        orderRule.setTimeWindow(30); // 熔断时长30s
        orderRule.setMinRequestAmount(5); // 最小请求数
        rules.add(orderRule);

        DegradeRuleManager.loadRules(rules);
    }

    /**
     * 初始化系统保护规则
     * 特别针对1600线程塌陷点
     */
    private void initSystemRules() {
        List<SystemRule> rules = new ArrayList<>();

        // CPU使用率限制
        SystemRule cpuRule = new SystemRule();
        cpuRule.setHighestSystemLoad(cpuUsageThreshold); // CPU使用率阈值
        rules.add(cpuRule);

        // 线程数限制 - 防止线程数过高导致系统崩溃
        SystemRule threadRule = new SystemRule();
        threadRule.setHighestCpuUsage(cpuUsageThreshold);
        threadRule.setMaxThread(concurrencyThreshold); // 设置最大线程数为塌陷点
        rules.add(threadRule);

        // QPS限制
        SystemRule qpsRule = new SystemRule();
        qpsRule.setQps(concurrencyThreshold * 100); // QPS限制
        rules.add(qpsRule);

        SystemRuleManager.loadRules(rules);
    }
}