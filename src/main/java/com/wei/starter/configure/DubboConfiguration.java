package com.wei.starter.configure;


import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableDubbo(scanBasePackages = "com.wei.rpc")
@PropertySource("classpath:/dubbo.properties")
public class DubboConfiguration {

}
