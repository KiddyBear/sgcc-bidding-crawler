package com.sgcc.crawler.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jasypt 加密配置类
 * 在配置项加载到容器前自动解密 ENC(...) 格式的密文
 */
@Configuration
public class JasyptConfig {

    /**
     * 注册 Jasypt 解密器 Bean
     * Bean 名称必须为 jasyptStringEncryptor，Jasypt 会自动识别
     */
    @Bean("jasyptStringEncryptor")
    public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        
        // 加密密钥（从环境变量或启动参数读取，增强安全性）
        String password = System.getProperty("jasypt.encryptor.password", "123456");
        config.setPassword(password);
        
        // 加密算法
        config.setAlgorithm("PBEWithMD5AndDES");
        
        // 密钥获取迭代次数
        config.setKeyObtentionIterations("1000");
        
        // 加密器池大小
        config.setPoolSize("1");
        
        // 加密提供者
        config.setProviderName("SunJCE");
        
        // 盐值生成器
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        
        // 密文输出格式
        config.setStringOutputType("base64");
        
        encryptor.setConfig(config);
        return encryptor;
    }
}
