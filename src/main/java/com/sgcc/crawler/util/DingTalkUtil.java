package com.sgcc.crawler.util;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 钉钉机器人工具类
 */
@Slf4j
public class DingTalkUtil {

    /**
     * 发送文本消息
     */
    public static boolean sendText(String webhook, String secret, String content) {
        JSONObject message = new JSONObject();
        message.set("msgtype", "text");

        JSONObject text = new JSONObject();
        text.set("content", content);
        message.set("text", text);

        return send(webhook, secret, message);
    }

    /**
     * 发送Markdown消息
     */
    public static boolean sendMarkdown(String webhook, String secret, String title, String content) {
        JSONObject message = new JSONObject();
        message.set("msgtype", "markdown");

        JSONObject markdown = new JSONObject();
        markdown.set("title", title);
        markdown.set("text", content);
        message.set("markdown", markdown);

        return send(webhook, secret, message);
    }

    /**
     * 发送ActionCard消息(带按钮)
     */
    public static boolean sendActionCard(String webhook, String secret, String title, String content, String btnTitle, String btnUrl) {
        JSONObject message = new JSONObject();
        message.set("msgtype", "actionCard");

        JSONObject actionCard = new JSONObject();
        actionCard.set("title", title);
        actionCard.set("text", content);
        actionCard.set("btnOrientation", "0");
        actionCard.set("singleTitle", btnTitle);
        actionCard.set("singleURL", btnUrl);
        message.set("actionCard", actionCard);

        return send(webhook, secret, message);
    }

    /**
     * 发送消息到钉钉
     */
    private static boolean send(String webhook, String secret, JSONObject message) {
        try {
            String url = buildUrl(webhook, secret);

            HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(message.toString())
                    .timeout(10000)
                    .execute();

            String body = response.body();
            JSONObject result = JSONUtil.parseObj(body);

            int errcode = result.getInt("errcode", -1);
            if (errcode == 0) {
                log.info("钉钉消息发送成功");
                return true;
            } else {
                log.error("钉钉消息发送失败: {}", body);
                return false;
            }
        } catch (Exception e) {
            log.error("钉钉消息发送异常", e);
            return false;
        }
    }

    /**
     * 构建带签名的URL
     */
    private static String buildUrl(String webhook, String secret) {
        if (secret == null || secret.isEmpty()) {
            return webhook;
        }

        try {
            long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secret;

            HMac hmac = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes(StandardCharsets.UTF_8));
            byte[] signData = hmac.digest(stringToSign);
            String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);

            return webhook + "&timestamp=" + timestamp + "&sign=" + sign;
        } catch (Exception e) {
            log.error("签名计算失败", e);
            return webhook;
        }
    }
}
