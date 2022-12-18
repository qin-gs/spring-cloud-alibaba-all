package com.example.sentinel.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.example.sentinel.service.SentinelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SentinelController {

    @Autowired
    private SentinelService sentinelService;

    @GetMapping("hello")
    public String helloWorld() {
        return sentinelService.helloWorld();
    }
}
