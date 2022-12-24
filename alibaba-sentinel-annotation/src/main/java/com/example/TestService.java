package com.example;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    /**
     * 使用注解控制流量，配置 资源点 和 处理函数
     */
    @SentinelResource(value = "hello-service", blockHandler = "exceptionHandler", fallback = "fallbackHandler")
    public void hello(String str) {
        System.out.println("hello " + str);
        // throw new RuntimeException("runtime");
    }

    /**
     * 限流的异常处理，参数要与上面相同然后加上 BlockException
     */
    public void exceptionHandler(String str, BlockException ex) {
        System.out.println(str + " " + ex);
    }

    public void fallbackHandler(String str) {
        System.out.println("fallback");
    }
}
