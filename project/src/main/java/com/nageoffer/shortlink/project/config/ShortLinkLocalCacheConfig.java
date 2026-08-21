package com.nageoffer.shortlink.project.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 短链跳转本地一级缓存（L1）配置
 *
 * <p>要点：</p>
 * <ul>
 *     <li>expireAfterWrite(30s)：短 TTL，靠过期收敛多实例间的短暂不一致</li>
 *     <li>maximumSize(10000)：必须有界，防止热点短链无限占用堆内存</li>
 *     <li>淘汰算法：Caffeine 默认 W-TinyLFU（频率 + 新鲜度）</li>
 * </ul>
 */
@Configuration
public class ShortLinkLocalCacheConfig {

    @Bean
    public Cache<String, String> shortLinkLocalCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)   // 短 TTL 收敛一致性
                .maximumSize(10_000)
                .build();
    }
}