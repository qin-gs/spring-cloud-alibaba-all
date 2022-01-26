#### SpringCloud Alibaba 学习



##### Nacos 实现服务注册与发现

##### 几种消费方式：RestTemplate, WebClient, Fiegn

##### 使用 Nacos 作为配置中心

##### 在 Nacos 中添加规则 与 spring cloud 客户端配置的对应关系

- spring.cloud.nacos.config.prefix

- spring.cloud.nacos.config.file-extension

- spring.cloud.nacos.config.group

  `${spring.cloud.nacos.config.prefix}-${spring.profile.active}.${spring.cloud.nacos.config.file-extension}`



##### Nacos 多环境配置

- data id：以Spring Cloud应用命名的properties文件

  通过在 spring cloud 配置文件中添加 `spring.profiles.active=DEV`，选择 nacos 中配置的数据

- group：在 spring cloud 配置文件中指定 group `spring.cloud.nacos.config.group=DEV_GROUP`

- namespace：在 spring cloud 配置文件中指定 namespace `spring.cloud.nacos.config.namespace=83eed625-d166-4619-b923-93df2088883a`



##### Nacos 多文件加载 与 共享配置

多文件加载

`spring.cloud.nacos.config.ext-config` 加载多个配置文件内容，是一个数组List类型。每个配置中包含三个参数：`data-id`、`group`，`refresh`；前两个不做赘述，与Nacos中创建的配置相互对应，`refresh`参数控制这个配置文件中的内容时候支持自动刷新，默认情况下，只有默认加载的配置才会自动刷新，对于这些扩展的配置加载内容需要配置该设置时候才会实现自动刷新。

共享配置

- `spring.cloud.nacos.config.shared-dataids`参数用来配置多个共享配置的`Data Id`，多个的时候用用逗号分隔
- `spring.cloud.nacos.config.refreshable-dataids`参数用来定义哪些共享配置的`Data Id`在配置变化时，应用中可以动态刷新，多个`Data Id`之间用逗号隔开。如果没有明确配置，默认情况下所有共享配置都不支持动态刷新

配置的优先级

 通过 preifx +  extention + group 拼接  >  ext-config  >  shared-tataids



##### Nacos 数据持久化

搭建 Nacos 集群之前，需要修改 Nacos 的数据持久化配置为 Mysql 存储



##### Nacos 集群部署

mysql 数据库配置

多个 Nacos ip 配置 /conf/cluster.conf

修改参数，启动多个实例 /bin/startup.sh

开启负载均衡 nginx



##### Sentinel 接口限流

类似于 Hystrix

下载运行 Sentinel Dashboard

在 簇点链路 中添加限流规则



##### Sentinel 使用 Nacos 持久化规则

- 文件配置
- Nacos 配置
- Zookeeper 配置
- Apollo 配置

启动 Nocos 和 Sentinel

引入 sentinel-datasource-nacos

在 Nacos 中创建限流规则

- Sentinel控制台中修改规则：仅存在于服务的内存中，不会修改Nacos中的配置值，重启后恢复原来的值。
- Nacos控制台中修改规则：服务的内存中规则会更新，Nacos中持久化规则也会更新，重启后依然保持。



##### Sentinel 使用 Apollo 持久化规则



##### Sentinel 修改规则同步到 Nacos/Apollo



##### @SentinelResource 注解

自定义资源点

- **服务降级**是在服务器压力陡增的情况下，利用有限资源，根据当前业务情况，关闭某些服务接口或者页面，以此释放服务器资源以保证核心任务的正常运行

- **服务熔断**一般是指软件系统中，由于某些原因使得服务出现了过载现象，为防止造成整个系统故障，从而采用的一种保护措施，所以很多地方把熔断亦称为过载保护

**限流 和 异常处理**

```java
// 定义资源点 和 限流异常处理函数，两个函数前面的参数要相同
@SentinelResource(value = "hello", blockHandler = "exceptionHandler")
public void hello(String str)

public void exceptionHandler(String str, BlockException ex)
```

**熔断降级 和 异常处理**

Sentinel 中配置 降级

当前请求异常率超过配置值后，后续指定时间内的调用直接发出 熔断降级，抛出 DegradeException 异常

```java
// 定义资源点 和 熔断异常处理函数，两个函数的参数要完全相同
@SentinelResource(value = "hello", fallback = "fallbackHandler")
public void hello(String str)

public void fallbackHandler
```



##### 整合 Dubbo (服务治理 与 rpc)

