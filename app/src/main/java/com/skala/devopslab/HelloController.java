package com.skala.devopslab;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final String version;

    // 배포할 때 APP_VERSION 환경변수에 image tag 를 넣으면 어떤 버전이 떠 있는지 바로 보인다
    public HelloController(@Value("${APP_VERSION:local}") String version) {
        this.version = version;
    }

    @GetMapping("/api/hello")
    public Map<String, String> hello() {
        return Map.of("message", "hello, devops", "version", version);
    }
}
