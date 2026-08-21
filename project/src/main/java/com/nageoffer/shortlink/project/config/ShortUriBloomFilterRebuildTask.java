package com.nageoffer.shortlink.project.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 短链布隆过滤器定时重建任务
 *
 * <p>要点：</p>
 * <ul>
 *     <li>@Scheduled 定时触发（cron 可配置）</li>
 *     <li>分布式锁保证多实例下只有一份重建在跑</li>
 *     <li>重建期间旧过滤器继续服务，切换是原子的（见 ShortUriBloomFilterHolder.rebuild）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortUriBloomFilterRebuildTask {

    private static final String REBUILD_LOCK_KEY = "short-link:lock:bloom-rebuild";

    private final RedissonClient redissonClient;
    private final ShortUriBloomFilterHolder shortUriBloomFilterHolder;
    private final BloomFilterProperties bloomFilterProperties;

    @Scheduled(cron = "${short-link.bloom.rebuild.cron}")
    public void rebuild() {
        if (!bloomFilterProperties.getRebuild().getEnabled()) {
            return;
        }
        RLock lock = redissonClient.getLock(REBUILD_LOCK_KEY);
        if (!lock.tryLock()) {
            log.info("短链布隆重建任务已在其他实例执行，本次跳过");
            return;
        }
        try {
            long count = shortUriBloomFilterHolder.rebuild();
            log.info("短链布隆过滤器定时重建完成，回填数量：{}", count);
        } finally {
            lock.unlock();
        }
    }
}