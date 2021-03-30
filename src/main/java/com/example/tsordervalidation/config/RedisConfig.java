package com.example.tsordervalidation.config;

import com.example.tsordervalidation.component.OrderPub;
import com.example.tsordervalidation.component.OrderPublisher;
import com.example.tsordervalidation.order.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

@Configuration
public class RedisConfig {
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;



    @Bean
    ChannelTopic topic() {
        return new ChannelTopic("validated-order");
    }

    @Bean
    RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<Order>(Order.class));
        return redisTemplate;
    }

    @Bean
    OrderPub orderPub() {
        return new OrderPublisher(redisTemplate(redisConnectionFactory), topic());
    }
}
