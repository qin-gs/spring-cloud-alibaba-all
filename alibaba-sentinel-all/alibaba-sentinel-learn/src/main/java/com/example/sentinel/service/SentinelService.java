package com.example.sentinel.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import org.springframework.stereotype.Service;

@Service
public class SentinelService {

    @SentinelResource("HelloWorld")
    public String helloWorld() {
        // 资源中的逻辑
        System.out.println("hello world");
        return "Hello world";
    }
}
