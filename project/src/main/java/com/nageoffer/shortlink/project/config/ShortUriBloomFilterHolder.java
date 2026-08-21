package com.nageoffer.shortlink.project.config;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shortlink.project.dao.mapper.ShortLinkMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 短链布隆过滤器 Holder：双布隆轮转 + 原子切换
 *
 * <p>作用：</p>
 * <ul>
 *     <li>contains/add 委托给当前活跃过滤器，业务侧无感知</li>
 *     <li>rebuild：先建好 standby 过滤器并全量回填，再原子切换，重建期间旧过滤器继续服务</li>
 *     <li>多实例并发重建由调用方（ShortUriBloomFilterRebuildTask）用分布式锁保证</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortUriBloomFilterHolder {

    private final RedissonClient redissonClient;
    private final BloomFilterProperties bloomFilterProperties;
    private final ShortLinkMapper shortLinkMapper;

    /**
     * v1 过滤器（RBloomFilterConfiguration 注册，名字保持不变）
     */
    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;

    /**
     * 版本号：重建时生成新名字过滤器，保证新旧共存、原子切换
     */
    private final AtomicLong version = new AtomicLong(0);

    /**
     * 当前活跃过滤器
     */
    private final AtomicReference<RBloomFilter<String>> activeFilter = new AtomicReference<>();

    /**
     * 重建时每批扫描条数
     */
    private static final long PAGE_SIZE = 1000;

    @PostConstruct
    public void init() {
        activeFilter.set(shortUriCreateCachePenetrationBloomFilter);
        log.info("短链布隆过滤器初始化完成，指向 v1：{}", shortUriCreateCachePenetrationBloomFilter.getName());
    }

    public boolean contains(String fullShortUrl) {
        return activeFilter.get().contains(fullShortUrl);
    }

    public void add(String fullShortUrl) {
        activeFilter.get().add(fullShortUrl);
    }

    /**
     * 全量重建：standby 先建好 → 分页扫库回填 → 原子切换
     *
     * @return 回填的短链数量
     */
    public long rebuild() {
        long newVersion = version.incrementAndGet();
        String standbyName = "shortUriCreateCachePenetrationBloomFilter_v" + newVersion;
        RBloomFilter<String> standby = redissonClient.getBloomFilter(standbyName);
        standby.tryInit(bloomFilterProperties.getExpectedInsertions(), bloomFilterProperties.getFalseProbability());

        long count = loadAllValidShortUri(standby::add);
        // 先建好再切：切换前的窗口期，旧过滤器继续对外服务
        activeFilter.set(standby);
        log.info("短链布隆过滤器重建完成，standby={}，回填数量={}", standbyName, count);
        return count;
    }

    /**
     * 分页扫描 t_link 有效记录（del_flag=0, del_time=0），全量回填
     * 注意：不带 gid 的查询会广播到 16 片后合并，属于低频任务，可接受
     */
    private long loadAllValidShortUri(Consumer<String> sink) {
        long total = 0;
        long current = 1;
        while (true) {
            IPage<ShortLinkDO> page = shortLinkMapper.selectPage(
                    new Page<>(current, PAGE_SIZE),
                    Wrappers.<ShortLinkDO>lambdaQuery()
                            .select(ShortLinkDO::getFullShortUrl)
                            .eq(ShortLinkDO::getDelFlag, 0)
                            .eq(ShortLinkDO::getDelTime, 0L));
            List<ShortLinkDO> records = page.getRecords();
            if (records == null || records.isEmpty()) {
                break;
            }
            records.forEach(each -> sink.accept(each.getFullShortUrl()));
            total += records.size();
            if (records.size() < PAGE_SIZE || current >= page.getPages()) {
                break;
            }
            current++;
        }
        return total;
    }
}