package com.example;

import com.example.service.HelloService;
import org.apache.dubbo.config.annotation.Service;

/**
 * 服务提供者，这个 @Service 是 dubbo 的
 */
@Service
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String str) {
        return "hello " + str;
    }
}
