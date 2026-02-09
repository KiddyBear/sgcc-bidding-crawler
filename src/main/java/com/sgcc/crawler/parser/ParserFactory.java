package com.sgcc.crawler.parser;

import com.sgcc.crawler.entity.AnnouncementType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析器工厂 - 工厂模式
 */
@Slf4j
@Component
public class ParserFactory {

    @Autowired(required = false)
    private List<AnnouncementParser> parsers;

    private final Map<AnnouncementType, AnnouncementParser> parserMap = new HashMap<>();

    @PostConstruct
    public void init() {
        if (parsers != null) {
            for (AnnouncementParser parser : parsers) {
                parserMap.put(parser.getSupportedType(), parser);
                log.info("注册解析器: {} -> {}", parser.getSupportedType(), parser.getClass().getSimpleName());
            }
        }
    }

    /**
     * 获取指定类型的解析器
     */
    public AnnouncementParser getParser(AnnouncementType type) {
        AnnouncementParser parser = parserMap.get(type);
        if (parser == null) {
            log.warn("未找到类型 {} 的解析器", type);
        }
        return parser;
    }

    /**
     * 获取所有已注册的解析器
     */
    public Map<AnnouncementType, AnnouncementParser> getAllParsers() {
        return new HashMap<>(parserMap);
    }

    /**
     * 检查是否支持指定类型
     */
    public boolean supports(AnnouncementType type) {
        return parserMap.containsKey(type);
    }
}
