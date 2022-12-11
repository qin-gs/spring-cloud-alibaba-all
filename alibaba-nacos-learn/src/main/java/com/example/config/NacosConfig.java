package com.example.config;

import com.alibaba.nacos.spring.context.annotation.config.NacosPropertySource;
import org.springframework.context.annotation.Configuration;

/**
 * 启动 配置管理 + 服务发现 两个功能
 */
@Configuration
@NacosPropertySource(dataId = "example", autoRefreshed = true)
public class NacosConfig {

}
