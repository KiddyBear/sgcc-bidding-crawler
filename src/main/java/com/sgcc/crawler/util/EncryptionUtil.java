package com.sgcc.crawler.util;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

/**
 * 配置加密工具类
 * 用于对敏感配置信息进行加密/解密
 */
public class EncryptionUtil {

    private static final String PASSWORD = "123456";
    private static final String ALGORITHM = "PBEWithMD5AndDES";

    /**
     * 加密明文
     */
    public static String encrypt(String plainText) {
        PooledPBEStringEncryptor encryptor = createEncryptor();
        return encryptor.encrypt(plainText);
    }

    /**
     * 解密密文
     */
    public static String decrypt(String encryptedText) {
        PooledPBEStringEncryptor encryptor = createEncryptor();
        return encryptor.decrypt(encryptedText);
    }

    /**
     * 创建加密器
     */
    private static PooledPBEStringEncryptor createEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(PASSWORD);
        config.setAlgorithm(ALGORITHM);
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }

    /**
     * 主方法：用于命令行快速加密敏感信息
     * 用法: java com.sgcc.crawler.util.EncryptionUtil "要加密的文本"
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("=== 配置加密工具 ===");
            System.out.println("用法: java EncryptionUtil <明文>");
            System.out.println();
            System.out.println("示例：");
            String[] testStrings = {
                    "测试密码",
                    "测试webhook地址",
                    "测试secret"
            };
            for (String test : testStrings) {
                String encrypted = encrypt(test);
                System.out.println("明文: " + test);
                System.out.println("密文: ENC(" + encrypted + ")");
                System.out.println("验证解密: " + decrypt(encrypted));
                System.out.println("---");
            }
        } else {
            String plainText = args[0];
            String encrypted = encrypt(plainText);
            System.out.println("明文: " + plainText);
            System.out.println("密文: ENC(" + encrypted + ")");
        }
    }
}
