package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class DiscoveryClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryClientApplication.class, args);
    }

    @RestController
    static class TestController {

        public static final String URL = "http://alibaba-nacos-discovery-server/hello?name=world";

        @Autowired
        private RestTemplate restTemplate;

        @Autowired
        private WebClient.Builder builder;

        /**
         * 实现负载均衡
         */
        @Autowired
        private LoadBalancerClient loadBalancerClient;


        @GetMapping("/test-restTemplate")
        public String testRestTemplate() {
            ServiceInstance instance = loadBalancerClient.choose("alibaba-nacos-discovery-server");
            String result = restTemplate.getForObject(URL, String.class);
            return "invoke: " + URL + ", return: " + result;
        }

        @GetMapping("/test-Builder")
        public Mono<String> testBuilder() {
            return builder.build()
                    .get()
                    .uri(URL)
                    .retrieve()
                    .bodyToMono(String.class);
        }
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }


}
