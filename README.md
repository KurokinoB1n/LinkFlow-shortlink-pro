[![build status](https://github.com/opengoofy/hippo4j/actions/workflows/ci.yml/badge.svg?event=push)](https://github.com/opengoofy/hippo4j)
[![codecov](https://codecov.io/gh/opengoofy/hippo4j/branch/develop/graph/badge.svg?token=WBUVJN107I)](https://codecov.io/gh/opengoofy/hippo4j)
![maven](https://img.shields.io/maven-central/v/com.alibaba.otter/canal.svg)
[![license](https://img.shields.io/badge/license-Apache--2.0-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
![](https://img.shields.io/github/contributors/opengoofy/hippo4j)
[![percentage of issues still open](http://isitmaintained.com/badge/open/opengoofy/hippo4j.svg)](http://isitmaintained.com/project/opengoofy/hippo4j "percentage of issues still open")

![GitHub last commit (branch)](https://img.shields.io/github/last-commit/opengoofy/hippo4j/develop?color=orange)

## 简介

![](https://oss.open8gu.com/image-20231115133642504.png)

短链接（Short Link）是指将一个原始的长 URL（Uniform Resource Locator）通过特定的算法或服务转化为一个更短、易于记忆的
URL。短链接通常只包含几个字符，而原始的长 URL 可能会非常长。

短链接的原理非常简单，通过一个原始链接生成个相对短的链接，然后通过访问短链接跳转到原始链接。

如果更细节一些的话，那就是：

1. **生成唯一标识符**：当用户输入或提交一个长 URL 时，短链接服务会生成一个唯一的标识符或者短码。
2. **将标识符与长 URL 关联**：短链接服务将这个唯一标识符与用户提供的长 URL 关联起来，并将其保存在数据库或者其他持久化存储中。
3. **创建短链接**：将生成的唯一标识符加上短链接服务的域名（例如：http://nurl.ink ）作为前缀，构成一个短链接。
4. **重定向**：当用户访问该短链接时，短链接服务接收到请求后会根据唯一标识符查找关联的长 URL，然后将用户重定向到这个长 URL。
5. **跟踪统计**：一些短链接服务还会提供访问统计和分析功能，记录访问量、来源、地理位置等信息。

短链接经常出现在咱们日常生活中，大家总是能在某些活动节日里收到各种营销短信，里边就会出现短链接。帮助企业在营销活动中，识别用户行为、点击率等关键信息监控。

![](https://oss.open8gu.com/IMG_9858-20231126.jpg)

主要作用包括但不限于以下几个方面：

- **提升用户体验**：用户更容易记忆和分享短链接，增强了用户的体验。
- **节省空间**：短链接相对于长 URL 更短，可以节省字符空间，特别是在一些限制字符数的场合，如微博、短信等。
- **美化**：短链接通常更美观、简洁，不会包含一大串字符。
- **统计和分析**：可以追踪短链接的访问情况，了解用户的行为和喜好。



---
# LinkFlow - 分布式企业级短链与流量追踪中台

---

## 📌 平台简介

**LinkFlow** 是一款面向 SaaS 多租户场景的高并发、高可用分布式链路分发与实时流量追踪中台。针对短链接系统天然具备的“读极端密集（跳转）、写高频并发（发号）、存储海量膨胀与细粒度画像分析”等业务特征，通过多级缓存架构、动态缓存抖动防雪崩、双布隆轮转自愈、分布式分库分表及消息异步削峰等设计，为大规模营销触达与公网流量分发提供底层支撑。

* **极速重定向路由**：基于 MurmurHash32 与 Base62 算法将长 URL 压缩为 6 位短码，支持算法自动生成与租户个性化自定义短码，并在毫秒级内完成寻址校验与 HTTP 302 重定向跳转。

* **多维实时画像分析**：实时采集访客的 UV、UIP、设备型号、操作系统、浏览器类型、网络环境及地理位置归属，驱动全渠道监控看板。

* **租户级资源隔离**：基于 GID 分组机制实现租户数据的逻辑与分片物理隔离，兼顾 B 端精细化运营与 C 端极速访问。


---



## 🚀 核心架构与技术演进

### 1. L1/L2 多级缓存与缓存雪崩防御（Jitter 机制）
* **读链路极速放行**：构建 **Caffeine（L1 进程内本地缓存）+ Redis（L2 分布式集中缓存）** 的二级缓存体系。高频热点短链直接命中 L1 内存返回，规避网络 I/O 损耗。
* **Cache-Aside 状态闭环**：在短链修改、失效延期或移入/移出回收站等写操作时，严格执行双级缓存同步失效逻辑，配合空值缓存（Cache Null Object）策略抵御穿透风险，消除分布式多节点脏读。
* **随机抖动防雪崩（TTL Jitter）**：重构缓存过期时间计算引擎。对指定有效期短链，动态计算离失效时间的毫秒差值以对齐数据库；对永久短链，通过 `ThreadLocalRandom` 在基准时长上叠加 $\pm 10\%$ 的离散随机浮动系数，彻底消除海量 Key 集中过期引发的缓存雪崩。

### 2. 自定义短链体系与并发冲突自愈
* **二元短码解析路由**：构建短码双路径生成策略，支持用户显式指定自定义短码（Custom URI）及算法自动计算回退。
* **差异化前置排重**：针对用户自定义短码单独执行前置冲突排查，与算法自动生成的布隆过滤器校验形成互补。
* **布隆漏网自愈与异常定性**：在双表写入捕获 `DuplicateKeyException` 时，主动补偿 `bloom.add()` 修复布隆过滤器指纹；同时依据短码来源的二元性，将自定义冲突定性为用户态错误（`ClientException`），将算法哈希碰撞定性为服务端系统异常（`ServiceException`）。

### 3. 双布隆轮转 + 凌晨定时自愈机制
* 针对同类项目中使用传统布隆的无法删除误判和容量上限问题--“无法物理删除”与“容量饱和后误判率单调递增”的缺陷，设计双缓冲轮转方案：
* **门面解耦模式**：业务层统一面向 `ShortUriBloomFilterHolder` 门面调用，内部利用 `AtomicReference` 维护活跃过滤器指针。
* **低峰无感重建**：每日凌晨低峰期通过 Redis 分布式锁控制单节点执行扫库，向后台独立的 Standby 备用过滤器回填全量有效数据（`del_flag = 0`），自然清洗已删除短码的历史脏指纹。
* **CAS 零停机切换**：回填校验通过后执行纳秒级指针替换，全链路在扩容与自愈期间保持业务 100% 连续可用。

### 4. RocketMQ 异步削峰流水线
* **主旁路彻底解耦**：将 9 张预聚合统计表与 1 张访问日志明细表的落库开销彻底剥离出跳转核心链路，跳转时仅需向 RocketMQ 发送轻量埋点消息即刻返回。
* **集群横向扩展与削峰填谷**：依赖 RocketMQ 磁盘 CommitLog 的海量堆积能力平抑营销突发洪峰，通过分布式消费组实现多实例多线程并行消费。
* **全链路精准幂等**：独立抽取 `ShortLinkStatsSaveService` 统筹落库逻辑；采用 RocketMQ 全局唯一 `msgId` 配合 Redis `SETNX` 状态机执行前置幂等拦截，底层结合 MySQL `ON DUPLICATE KEY UPDATE` 联合唯一键完成最终一致性 Upsert 累加。

### 5. 分布式存储与双写二级路由
* **ShardingSphere 16 分片**：短链主表 `t_link` 采用 `gid` 作为分片键拆分为 16 张物理表，平衡海量存储与单表 B+ 树深度。
* **双写二级路由地图**：针对跳转时不带 `gid` 导致的跨片全库广播问题，设计以短码为主键的路由表 `t_link_goto`，采用分布式双写机制实现短码到分片键的高效二次寻址。



---

## 🏗️ 架构拓扑图

```text
                               【 外部公网海量访问 】
                                         │
                                         ▼
                 ┌────────────────────────────────────────────────┐
                 │    API Gateway (8000) / 分布式令牌桶 IP 限流   │
                 └───────────────────────┬────────────────────────┘
                                         │ (动态路由分发)
                                         ▼
                 ┌────────────────────────────────────────────────┐
                 │          LinkFlow Project 核心服务 (8001)      │
                 │                                                │
                 │   [L1 本地缓存] Caffeine (命中直接 302 重定向) │
                 │              │ (未命中)                        │
                 │   [L2 分布式缓存] Redis Cluster                │
                 │              │ (未命中)                        │
                 │   [防穿透层] 动态双布隆过滤器 + GOTO 空值缓存  │
                 │              │ (透过滤网)                      │
                 │   [防击穿层] Redisson 分布式锁 + DCL 双重检查  │
                 └──────────────┬──────────────────┬──────────────┘
                                │ (异步埋点)       │ (回源查库)
                                ▼                  ▼
                   ┌──────────────────────┐  ┌──────────────────────┐
                   │  RocketMQ 消息集群   │  │ ShardingSphere-JDBC  │
                   │ (short-link-stats)   │  │  16 分片 MySQL 8.0   │
                   └──────────┬───────────┘  └──────────────────────┘
                              │ (多线程并发消费)
                              ▼
                   ┌────────────────────────────────────────┐
                   │       ShortLinkStatsSaveService        │
                   │  - 基于 msgId 的 Redis 状态机幂等拦截  │
                   │  - 9 张维度表增量预聚合 (Upsert)       │
                   │  - 1 张访问日志流水表插入              │
                   └────────────────────────────────────────┘
```

---

## 📊 性能压测与基准评估

在 **4 核 8G 内存、40G 云盘、100M 公网带宽** 的单机 Docker 全栈混部（App + MySQL + Redis + RocketMQ）严苛环境下，使用 JMeter 在本机跨公网模拟 500 客户端并发：

| 压测链路 | 压测场景与网络拓扑 | 吞吐量 (QPS/TPS) | 响应时间 (RT) | 核心瓶颈归因与分析 |
| --- | --- | --- | --- | --- |
| **短链跳转读链路** | 500 并发 / L1 本地缓存命中 (跨公网) | **4,647 QPS** | P99 ≈ 900ms<br><br>平均 RT ≈ 107ms |受限于客户端跨公网物理 RTT（~50-100ms），500 线程的发包能力已被完全打满 
| **短链跳转读链路** | 500 并发 / 完整监控统计异步链路 (跨公网) | **1,800 QPS** | 平均 RT ≈ 278ms | 包含 Cookie 提取、两次 Redis `SADD` 判重及 RocketMQ Producer 消息投递开销
| **短链跳转读链路** | 内网 VPC / 理论无网络损耗环境 (免带宽瓶颈) | **35,000+ QPS** | 平均 RT < 2ms | 消除公网 RTT 损耗，瓶颈释放至 CPU 协议解析与 Tomcat 线程上下文切换 |
| **短码创建写链路** | 500 并发 / 算法生成 + 双表物理分片落库 | **850+ TPS** | 平均 RT ≈ 580ms | 单机混部下承载每秒 1,700+ 次物理落库，触达 40G 云盘的 IOPS 上限
| **短码创建写链路** | 独立外置云 RDS 数据库环境 | **3,000+ TPS** | 平均 RT < 30ms | 排除容器磁盘 I/O 争抢后的稳定写入吞吐 |
> **网络带宽理论计算依据**：
> HTTP 302 重定向单次返回报文（包含 Location 与 Set-Cookie 头）体积约为 0.45KB。在 100Mbps（12.5MB/s）公网网卡下，出网带宽的纯理论吞吐天花板为：
> 
> 
> 
> $$12.5 \times 1024 \div 0.45 \approx 28,400\text{ QPS}$$
> 
> 

---

## 🛠️ 技术选型清单

| 维度 | 技术选型 | 说明 |
| --- | --- | --- |
| **核心框架** | Java 17 / Spring Boot 3.0.7 / Spring Cloud 2022.0.3 | 现代微服务基础设施与高效运行时 |
| **分布式存储** | MySQL 8.0 / MyBatis-Plus / ShardingSphere-JDBC 5.3.2 | 水平切分 16 分表，支撑亿级数据存储
| **缓存与并发** | Caffeine 3.1.8 / Spring Data Redis / Redisson 3.21.3 | 本地堆内存与分布式位图协同，防击穿/防穿透
| **消息中间件** | Apache RocketMQ 4.9.7 (`rocketmq-spring-boot-starter:2.3.1`) | 支撑高吞吐削峰、海量消息堆积与集群扩展 
| **防穿透与动态自愈** | 双布隆轮转架构 / Redisson RBloomFilter / Spring Scheduled| 针对同类项目中使用传统布隆的无法删除误判和容量上限问题, 基于双缓冲与 CAS 原子切换，实现低峰期全量扫库重建，清洗历史删除指纹并重置扩容误判率
| **流量防护** | Spring Cloud Gateway / Sentinel 规则引擎 | 提供令牌桶细粒度 IP 限流与系统过载保护 
| **工具组件** | Lombok / Hutool 5.8.20 / Fastjson2 2.0.36 | 高效编码与数据序列化工具 

---

## 📂 模块结构说明

```text
linkflow
├── linkflow-gateway          # 统一分布式微服务网关（动态路由、JWT 鉴权、限流风控）
├── linkflow-admin            # 中台管理服务（租户管理、用户认证、分组策略）
├── linkflow-project          # 核心业务引擎（短码生成、302 跳转、双布隆轮转、监控采集）
├── linkflow-aggregation      # 聚合启动模块（便于全栈容器化交付与轻量化部署）
└── deploy                    # 部署资产目录（docker-compose、RocketMQ broker.conf、SQL 初始化）

```

---

## ⚡ 快速开始

### 1. 环境准备

* 运行系统：Linux / macOS / Windows
* 依赖环境：JDK 17+、Maven 3.8+、Docker & Docker-Compose

### 2. 启动基础支撑容器

进入 `deploy` 目录，执行容器编排脚本启动 MySQL、Redis 及 RocketMQ：

```bash
cd deploy
docker-compose up -d

```

> **注意**：若在云服务器上通过 Docker 部署 RocketMQ，请在 `deploy/rocketmq/broker.conf` 中将 `brokerIP1` 修改为宿主机的真实局域网/公网 IP，避免客户端连接超时。

### 3. 初始化数据库分片表

执行 `deploy/resources/schema.sql` 脚本，完成 16 张短链主表（`t_link_0` ~ `t_link_15`）、路由表（`t_link_goto_0` ~ `t_link_goto_15`）及 9 张预聚合维度监控表的结构创建。

### 4. 核心配置文件核对

检查 `project` 与 `aggregation` 模块的 `application.yaml` 关键配置项：

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: short-link-stats-producer

short-link:
  mq-topic: short-link-stats
  bloom:
    expected-insertions: 100000000   # 预期容量：1 亿
    false-probability: 0.001        # 容忍误判率：0.1%
    rebuild:
      enabled: true                 # 开启低峰期定时全量自愈
      cron: "0 0 3 * * ?"           # 每日凌晨 3 点自动触发

```

### 5. 项目编译与运行

在项目根目录下执行构建，并以聚合模式启动：

```bash
mvn clean install -DskipTests
java -jar linkflow-aggregation/target/linkflow-aggregation.jar

```

---

## 📄 开源许可证

本项目遵循 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0) 开源授权协议。
