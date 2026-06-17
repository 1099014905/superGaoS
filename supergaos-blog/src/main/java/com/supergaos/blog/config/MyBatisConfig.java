package com.supergaos.blog.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.supergaos.blog.mapper")
public class MyBatisConfig {
}
