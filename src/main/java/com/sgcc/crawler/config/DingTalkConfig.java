package com.sgcc.crawler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 钉钉配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "dingtalk")
public class DingTalkConfig {

    /**
     * Webhook地址
     */
    private String webhook;

    /**
     * 签名密钥(可选)
     */
    private String secret;

    /**
     * 是否启用
     */
    private boolean enabled = true;
}
