package com.nageoffer.shortlink.project.config;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 布隆过滤器配置
 * v2：tryInit 参数从硬编码改为读取 BloomFilterProperties（默认值与原值一致）
 */
@Configuration
@RequiredArgsConstructor
public class RBloomFilterConfiguration {

    private final BloomFilterProperties bloomFilterProperties;

    /**
     * 防止短链接创建查询数据库的布隆过滤器（v1，名字保持不变）
     */
    @Bean
    public RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> cachePenetrationBloomFilter = redissonClient.getBloomFilter("shortUriCreateCachePenetrationBloomFilter");
        cachePenetrationBloomFilter.tryInit(bloomFilterProperties.getExpectedInsertions(), bloomFilterProperties.getFalseProbability());
        return cachePenetrationBloomFilter;
    }
}