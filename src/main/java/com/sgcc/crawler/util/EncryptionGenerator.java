package com.sgcc.crawler.util;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

/**
 * 配置加密工具类 - 运行生成密文
 * 运行此类的 main 方法可以生成加密后的配置密文
 */
public class EncryptionGenerator {
    
    private static final String PASSWORD = "123456";
    
    private static PooledPBEStringEncryptor createEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(PASSWORD);
        config.setAlgorithm("PBEWithMD5AndDES");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }
    
    public static String encrypt(String plainText) {
        return createEncryptor().encrypt(plainText);
    }
    
    public static void main(String[] args) {
        System.out.println("=== 配置加密密文生成（密钥：123456）===\n");
        
        if (args.length > 0) {
            // 如果有命令行参数，加密参数
            String plainText = args[0];
            String encrypted = encrypt(plainText);
            System.out.println("明文: " + plainText);
            System.out.println("密文: ENC(" + encrypted + ")");
            System.out.println("验证解密: " + createEncryptor().decrypt(encrypted));
        } else {
            // 否则显示使用说明
            System.out.println("用法：");
            System.out.println("1. 在 IDE 中右键运行此类的 main 方法");
            System.out.println("2. 或使用命令行：java EncryptionGenerator <要加密的文本>");
            System.out.println();
            System.out.println("示例：加密一个测试字符串");
            String testText = "test123";
            String encrypted = encrypt(testText);
            System.out.println("测试明文: " + testText);
            System.out.println("测试密文: ENC(" + encrypted + ")");
            System.out.println("验证解密: " + createEncryptor().decrypt(encrypted));
            System.out.println();
            System.out.println("=== 将生成的 ENC(...) 密文复制到配置文件中即可 ===");
        }
    }
}
