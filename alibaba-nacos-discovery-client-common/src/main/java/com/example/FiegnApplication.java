package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class FiegnApplication {

    public static void main(String[] args) {
        SpringApplication.run(FiegnApplication.class, args);
    }

    @RestController
    static class FeignController {

        @Autowired
        private Client client;

        @GetMapping("test-feign")
        public String testFeign() {
            String result = client.hello("fiegn");
            return "return: " + result;
        }

    }

    @FeignClient("alibaba-nacos-discovery-server")
    interface Client {

        @GetMapping("hello")
        String hello(@RequestParam("name") String name);
    }
}
