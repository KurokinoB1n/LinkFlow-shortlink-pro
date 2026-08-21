package com.nageoffer.shortlink.project.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 布隆过滤器参数配置（v2 新增）
 * 默认值与原硬编码一致：1 亿预期插入、0.1% 误判率
 */
@Data
@Component
@ConfigurationProperties(prefix = "short-link.bloom")
public class BloomFilterProperties {

    /**
     * 预期插入数量
     */
    private Long expectedInsertions = 100_000_000L;

    /**
     * 误判率
     */
    private Double falseProbability = 0.001;

    /**
     * 定时重建配置
     */
    private Rebuild rebuild = new Rebuild();

    @Data
    public static class Rebuild {

        /**
         * 是否开启定时重建
         */
        private Boolean enabled = true;

        /**
         * 定时重建 cron（默认每天凌晨 3 点）
         */
        private String cron = "0 0 3 * * ?";
    }
}