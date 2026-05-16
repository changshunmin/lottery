#!/bin/bash

# 构建并启动 Docker 容器

echo "正在构建应用..."
mvn clean package -DskipTests

echo "正在构建 Docker 镜像..."
docker-compose build

echo "正在启动服务..."
docker-compose up -d

echo ""
echo "服务已启动！"
echo "应用地址: http://localhost:8080"
echo "大转盘页面: http://localhost:8080/index.html"
echo "管理页面: http://localhost:8080/lottery.html"
echo ""
echo "查看日志: docker-compose logs -f"
echo "停止服务: docker-compose down"