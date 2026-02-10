# 国网招标爬虫 - 生产级 Docker 镜像
# 基础镜像：Ubuntu 20.04 LTS
FROM ubuntu:20.04

# 镜像元数据
LABEL maintainer="SGCC Crawler Team"
LABEL version="1.0.0"
LABEL description="国网招标信息爬虫系统 - 基于 Spring Boot 3.2.5 + Selenium"

# 设置非交互式安装模式，避免交互式提示
ENV DEBIAN_FRONTEND=noninteractive

# 设置时区为上海
ENV TZ=Asia/Shanghai

# 设置 JAVA_HOME 环境变量
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# 设置应用相关环境变量
ENV APP_HOME=/app
ENV CHROME_DRIVER_VERSION=120.0.6099.109

# 创建应用目录
WORKDIR ${APP_HOME}

# 更新包索引并安装基础依赖
RUN apt-get update && apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
        # Java 环境
        openjdk-21-jdk \
        # 爬虫依赖
        wget \
        curl \
        gnupg \
        unzip \
        ca-certificates \
        # 中文字体支持（防止爬虫渲染乱码）
        fonts-wqy-zenhei \
        fonts-wqy-microhei \
        fontconfig \
        # 系统工具
        tzdata \
        supervisor \
        # 清理缓存
        && rm -rf /var/lib/apt/lists/*

# 设置时区
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    dpkg-reconfigure -f noninteractive tzdata

# 安装 Google Chrome 浏览器
RUN wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - && \
    echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list && \
    apt-get update && \
    apt-get install -y google-chrome-stable && \
    rm -rf /var/lib/apt/lists/*

# 下载并安装与 Chrome 版本匹配的 ChromeDriver
RUN CHROME_VERSION=$(google-chrome --version | grep -oP '\d+\.\d+\.\d+\.\d+') && \
    CHROME_MAJOR_VERSION=$(echo $CHROME_VERSION | cut -d. -f1) && \
    echo "Chrome version: $CHROME_VERSION" && \
    # 根据 Chrome 主版本号确定对应的 ChromeDriver 版本
    if [ "$CHROME_MAJOR_VERSION" -ge 115 ]; then \
        CHROMEDRIVER_VERSION="$CHROME_MAJOR_VERSION.0.6099.109"; \
    else \
        CHROMEDRIVER_VERSION="114.0.5735.90"; \
    fi && \
    echo "Installing ChromeDriver version: $CHROMEDRIVER_VERSION" && \
    wget -O /tmp/chromedriver.zip "https://chromedriver.storage.googleapis.com/$CHROMEDRIVER_VERSION/chromedriver_linux64.zip" && \
    unzip /tmp/chromedriver.zip -d /tmp/ && \
    mv /tmp/chromedriver /usr/local/bin/chromedriver && \
    chmod +x /usr/local/bin/chromedriver && \
    rm -f /tmp/chromedriver.zip && \
    # 验证安装
    chromedriver --version

# 验证 Java 安装
RUN java -version && javac -version

# 验证 Chrome 和 ChromeDriver 安装
RUN google-chrome --version && chromedriver --version

# 复制应用 Jar 包（构建时由 docker-compose 指定上下文）
COPY target/sgcc-bidding-crawler-1.0.0.jar app.jar

# 创建日志和数据目录
RUN mkdir -p ${APP_HOME}/logs ${APP_HOME}/data

# 设置权限
RUN chown -R root:root ${APP_HOME} && \
    chmod +x ${APP_HOME}/app.jar

# 暴露应用端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]