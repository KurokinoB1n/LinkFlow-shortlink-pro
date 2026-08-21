package com.nageoffer.shortlink.project.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.nageoffer.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.nageoffer.shortlink.project.mq.idempotent.MessageQueueIdempotentHandler;
import com.nageoffer.shortlink.project.service.ShortLinkStatsSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 短链接监控统计 RocketMQ 消费者（v2）
 *
 * <p>与 v1 Stream 版本的差异：幂等唯一标识从 Stream RecordId 换成 RocketMQ msgId；
 * 消费成功默认提交，异常抛出后由 RocketMQ 按重试策略重新投递。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "${short-link.mq-topic}",
        consumerGroup = "short-link-stats-consumer-group"
)
public class ShortLinkStatsRocketMQConsumer implements RocketMQListener<MessageExt> {

    private final ShortLinkStatsSaveService shortLinkStatsSaveService;
    private final MessageQueueIdempotentHandler messageQueueIdempotentHandler;

    @Override
    public void onMessage(MessageExt message) {
        String messageId = message.getMsgId();
        // v2 临时诊断日志：确认消费者是否收到消息
        log.info("[RocketMQ-Consumer] receive msgId={}, body={}", messageId, new String(message.getBody(), StandardCharsets.UTF_8));
        // 与 v1 同一套幂等逻辑：setIfAbsent("0") → 消费完成置 "1" → 异常删除标记
        if (messageQueueIdempotentHandler.isMessageBeingConsumed(messageId)) {
            if (messageQueueIdempotentHandler.isAccomplish(messageId)) {
                return;
            }
            throw new RuntimeException("消息未完成流程，需要消息队列重试");
        }
        try {
            Map<String, String> producerMap = JSON.parseObject(new String(message.getBody(), StandardCharsets.UTF_8), Map.class);
            ShortLinkStatsRecordDTO statsRecord = JSON.parseObject(producerMap.get("statsRecord"), ShortLinkStatsRecordDTO.class);
            shortLinkStatsSaveService.actualSaveShortLinkStats(statsRecord);
        } catch (Throwable ex) {
            messageQueueIdempotentHandler.delMessageProcessed(messageId);
            log.error("记录短链接监控消费异常", ex);
            throw ex;   // 抛出后 RocketMQ 按重试策略重新投递
        }
        messageQueueIdempotentHandler.setAccomplish(messageId);
    }
}
