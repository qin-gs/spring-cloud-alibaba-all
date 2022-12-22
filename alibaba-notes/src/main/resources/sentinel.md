# Sentinel 原理



## 使用

先把可能需要保护的资源定义好（埋点），之后再配置规则

1. 定义资源

   - 主流框架默认适配

   - 抛出异常的方式定义资源

     ```java
     // 1.5.0 版本开始可以利用 try-with-resources 特性（使用有限制）
     // 资源名可使用任意有业务语义的字符串，比如方法名、接口名或其它可唯一标识的字符串。
     try (Entry entry = SphU.entry("resourceName")) {
       // 被保护的业务逻辑
       // do something here...
     } catch (BlockException ex) {
       // 资源访问阻止，被限流或被降级
       // 在此处进行相应的处理操作
     }
     ```

   - 返回布尔值方式定义资源
   - 注解方式定义资源
   - 异步调用支持



2. 定义规则

   - 流量控制规则 (FlowRule)

     |      Field      | 说明                                                         | 默认值                        |
     | :-------------: | :----------------------------------------------------------- | :---------------------------- |
     |    resource     | 资源名，资源名是限流规则的作用对象                           |                               |
     |      count      | 限流阈值                                                     |                               |
     |      grade      | 限流阈值类型，QPS 模式（1）或并发线程数模式（0）             | QPS 模式                      |
     |    limitApp     | 流控针对的调用来源                                           | `default`，代表不区分调用来源 |
     |    strategy     | 调用关系限流策略：直接、链路、关联                           | 根据资源本身（直接）          |
     | controlBehavior | 流控效果（直接拒绝/WarmUp/匀速+排队等待），不支持按调用关系限流 | 直接拒绝                      |
     |   clusterMode   | 是否集群限流                                                 | 否                            |

   - 熔断降级规则 (DegradeRule)

     |       Field        | 说明                                                         | 默认值     |
     | :----------------: | :----------------------------------------------------------- | :--------- |
     |      resource      | 资源名，即规则的作用对象                                     |            |
     |       grade        | 熔断策略，支持慢调用比例/异常比例/异常数策略                 | 慢调用比例 |
     |       count        | 慢调用比例模式下为慢调用临界 RT（超出该值计为慢调用）；异常比例/异常数模式下为对应的阈值 |            |
     |     timeWindow     | 熔断时长，单位为 s                                           |            |
     |  minRequestAmount  | 熔断触发的最小请求数，请求数小于该值时即使异常比率超出阈值也不会熔断（1.7.0 引入） | 5          |
     |   statIntervalMs   | 统计时长（单位为 ms），如 60*1000 代表分钟级（1.8.0 引入）   | 1000 ms    |
     | slowRatioThreshold | 慢调用比例阈值，仅慢调用比例模式有效（1.8.0 引入）           |            |

   - 系统保护规则 (SystemRule)：系统自适应限流从整体维度对应用入口流量进行控制；通过自适应的流控策略，让系统的入口流量和系统的负载达到一个平衡，让系统尽可能跑在最大吞吐量的同时保证系统整体的稳定性

     |       Field       | 说明                                   | 默认值      |
     | :---------------: | :------------------------------------- | :---------- |
     | highestSystemLoad | `load1` 触发值，用于触发自适应控制阶段 | -1 (不生效) |
     |       avgRt       | 所有入口流量的平均响应时间             | -1 (不生效) |
     |     maxThread     | 入口流量的最大并发数                   | -1 (不生效) |
     |        qps        | 所有入口资源的 QPS                     | -1 (不生效) |
     |  highestCpuUsage  | 当前系统的 CPU 使用率（0.0-1.0）       | -1 (不生效) |

   - 来源访问控制规则 (AuthorityRule)：通过黑名单 或 白名单 限制资源是否通过

   - 热点参数规则 (ParamFlowRule)

3. 检验规则是否生效

   流控降级相关的异常都是异常类 `BlockException` 的子类

   ```java
   BlockException.isBlockException(Throwable t);
   ```





## 核心类

<img src="./assets/sentinel-slot-chain-architecture.png" alt="sentinel-slot-chain-architecture" style="zoom: 20%;" />



Sentinel 的核心骨架，将不同的 Slot 按照顺序串在一起（责任链模式），从而将不同的功能（限流、降级、系统保护）组合在一起。

- 统计数据构建部分
- 判断部分

系统为每个资源创建一套 SlotChain



### SPI 机制

sentinel 槽链中每个 slot 的执行顺序是固定好的，用 ProcessorSlot 作为 SPI 接口进行扩展







## 工作流程



在 Sentinel 里面，所有的资源都对应一个资源名称（`resourceName`），每次资源调用都会创建一个 `Entry` 对象。Entry 可以通过对主流框架的适配自动创建，也可以通过注解的方式或调用 `SphU` API 显式创建。Entry 创建的时候，同时也会创建一系列功能插槽（slot chain），这些插槽有不同的职责

- `NodeSelectorSlot` 负责收集**资源的路径**，并将这些资源的调用路径，以树状结构存储起来，用于根据调用路径来限流降级；
- `ClusterBuilderSlot` 则用于存储**资源的统计**信息以及**调用者**信息，例如该资源的 RT, QPS, thread count 等等，这些信息将用作为多维度限流，降级的依据；
- `StatisticSlot` 则用于记录、统计不同纬度的 runtime 指标监控信息；
- `FlowSlot` 则用于根据预设的限流规则以及前面 slot 统计的状态，来进行**流量控制**；
- `AuthoritySlot` 则根据配置的黑白名单和调用来源信息，来做黑白名单控制；
- `DegradeSlot` 则通过统计信息以及预设的规则，来做**熔断降级**；
- `SystemSlot` 则通过系统的状态，例如 load1 等，来控制总的入口流量；



<img src="./assets/slots.gif" alt="slots链" style="zoom:50%;" />

node 类型

<img src="./assets/node类型.png" alt="node 类型" style="zoom:30%;" />





​	自定义 slot

<img src="./assets/自定义slot.png" alt="自定义slot" style="zoom:50%;" />





## 流量控制

**qps / 并发数**

- `resource`：资源名，即限流规则的作用对象
- `count`: 限流阈值
- `grade`: 限流阈值类型（1-QPS 或 0-并发线程数）
- `limitApp`: 流控针对的调用来源，若为 `default` 则不区分调用来源
- `strategy`: 调用关系限流策略
- `controlBehavior`: 流量控制效果（直接拒绝、Warm Up (预热/冷启动)、匀速排队）



**基于调用关系的流量控制**

- 根据调用方限流
- 根据调用链路入口限流：链路限流
- 具有关系的资源流量控制：关联流量控制



## 网关流控

- GatewayFlowRule：网关限流规则
- ApiDefinition：用户自定义的 api 定义分组

网关流控默认的粒度是 route 维度以及自定义 API 分组维度，默认不支持 URL 粒度



**原理**

Sentinel 底层会将网关流控规则转化为热点参数规则（`ParamFlowRule`），存储在 `GatewayRuleManager` 中，与正常的热点参数规则相隔离

GatewayFlowSlot：专门用来做网关规则的检查，从 `GatewayRuleManager` 中提取生成的热点参数规则，根据传入的参数依次进行规则检查



## 熔断降级

- 慢调用比例：熔断、探测恢复、关闭
- 异常比例
- 异常数



熔断器事件监听



## 热点参数限流

统计某个热点数据中访问频次最高的 Top K 数据

```java
// paramA in index 0, paramB in index 1.
// 若需要配置例外项或者使用集群维度流控，则传入的参数只支持基本类型。
SphU.entry(resourceName, EntryType.IN, 1, paramA, paramB);
```

对于 `@SentinelResource` 注解方式定义的资源，若注解作用的方法上有参数，Sentinel 会将它们作为参数传入 `SphU.entry(res, args)`；比如以下的方法里面 `uid` 和 `type` 会分别作为第一个和第二个参数传入 Sentinel API，从而可以用于热点规则判断：

```java
@SentinelResource("myMethod")
public Result doSomething(String uid, int type) {
  // some logic here...
}
```



## 系统自适应限流





## 黑白名单控制

比如我们希望控制对资源 `test` 的访问设置白名单，只有来源为 `appA` 和 `appB` 的请求才可通过，则可以配置如下白名单规则：

```java
AuthorityRule rule = new AuthorityRule();
rule.setResource("test");
rule.setStrategy(RuleConstant.AUTHORITY_WHITE);
rule.setLimitApp("appA,appB");
AuthorityRuleManager.loadRules(Collections.singletonList(rule));
```



## 实时监控

提供对所有资源的实时监控



## 动态规则扩展

- 通过 api 直接修改

  只能放在内存中

  ```java
  // 修改流控规则
  FlowRuleManager.loadRules(List<FlowRule> rules);
  // 修改降级规则
  DegradeRuleManager.loadRules(List<DegradeRule> rules);
  ```

- 通过 DataSource 接口适配不同数据源修改

  通过控制台设置规则后将规则推送到统一的规则中心，客户端实现 ReadableDataSource 接口端监听规则中心实时获取变更

  - 拉：定时轮训 (文件，consul，eureka)

    `FileRefreshableDataSource`

  - 推：注册监听器 (zookeeper，redis，nacos，apollo)

    `NacosDataSource`



## Sentinel 控制台

`-Dcsp.sentinel.app.type=1` 启动参数会将服务标记为 API Gateway













### context

Context 代表调用链路上下文，贯穿一次调用链路中的所有 `Entry`。Context 维持着入口节点（`entranceNode`）、本次调用链路的 curNode、调用来源（`origin`）等信息。Context 名称即为调用链路入口名称。

Context 维持的方式：通过 ThreadLocal 传递，只有在入口 `enter` 的时候生效。由于 Context 是通过 ThreadLocal 传递的，因此对于异步调用链路，线程切换的时候会丢掉 Context，因此需要手动通过 `ContextUtil.runOnContext(context, f)` 来变换 context。



Node 之间的关系

- Node：数据统计接口
- StatisticNode：统计节点
- EntranceNode：入口节点，一个 Context 有一个入口节点，统计当前 Context 的总体流量数据
- DefaultNode：默认节点，统计一个资源在当前 Context 中的流量数据
- ClusterNode：集群节点，统计一个资源在所有 Context 中的流量数据，每个资源都有



<img src="./assets/node类型.png" alt="node类型" style="zoom:30%;" />

通过 aop 完成，SentinelResourceAspect

```java
SphU.entry(resourceName, resourceType, entryType, pjp.getArgs())
```

核心逻辑

`com.alibaba.csp.sentinel.CtSph#entryWithPriority(com.alibaba.csp.sentinel.slotchain.ResourceWrapper, int, boolean, java.lang.Object...)`

1. 从 ThreadLocal 中获取 Context
2. 如果 Context 是 NullContext，说明当前系统中的 Context 超出阈值 (`com.alibaba.csp.sentinel.context.ContextUtil#trueEnter`)
3. 如果当前线程没有 Context == null，创建一个默认的 (sentinel_default_context)
4. 查找 ProcessorSlotChain
5. 找到后创建一个资源操作对象，对资源进行操作 (chain.entry)



SlotChain 查找









### sentinel 与 hystrix 线程隔离的区别

- 线程池隔离 (hystrix)：支持主动超时、异步调用；线程的额外开销大
- 信号量隔离 (sentinel)：轻量级



## 限流算法

- 计数器算法

  - 固定窗口计数器

    将时间划分为多个窗口，窗口时间间隔 (interval)

    每个窗口维护一个计数器，每次请求加 1

    超出阈值的请求被丢弃

  - 滑动窗口计数器

    将一个窗口划分为多个更小的区间，窗口范围从 (currentTime - interval) 之后的那个时区开始

- 令牌桶算法

  以固定速率生成令牌，桶满了之后多余令牌丢弃

  请求到来后申请到令牌后才能被处理，拿不到令牌的需要等或丢弃

- 漏桶算法

  将所有请求全部放到桶中，以固定速率处理



sentinel

- 默认限流：滑动时间窗口
- 排队等待：漏桶
- 热点参数：令牌桶





## Sentinel 基本概念

- 统计数据：统计某个资源的访问数据
- 规则判断：限流规则、降级规则、熔断规则...



ProcessorSlotChain





