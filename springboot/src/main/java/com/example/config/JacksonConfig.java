package com.example.config;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("null")
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 将 Long 类型序列化为 String，解决前端精度丢失问题
            JsonSerializer<?> serializer = ToStringSerializer.instance;
            builder.serializerByType(Long.class, serializer);
            builder.serializerByType(Long.TYPE, serializer);
        };
    }
}