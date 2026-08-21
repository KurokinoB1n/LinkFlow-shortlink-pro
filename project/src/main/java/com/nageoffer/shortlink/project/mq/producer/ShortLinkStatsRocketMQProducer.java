package com.nageoffer.shortlink.project.mq.producer;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 短链接监控统计 RocketMQ 生产者（v2，替换 v1 Redis Stream 生产者）
 * 方法签名与 v1 保持一致：send(Map&lt;String, String&gt;)，调用点无感替换
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkStatsRocketMQProducer {

    private final RocketMQTemplate rocketMQTemplate;

    @Value("${short-link.mq-topic}")
    private String topic;

    public void send(Map<String, String> producerMap) {
        // v2 临时诊断日志：确认生产者是否被调用
        log.info("[RocketMQ-Producer] send topic={}, msg={}", topic, producerMap.get("statsRecord"));
        rocketMQTemplate.convertAndSend(topic, producerMap);
    }
}
