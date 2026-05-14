#!/bin/bash

# 进入项目目录
cd /mnt/d/code/work_project/docAI/user-service

# 构建Docker镜像
docker build -t user-service:1.0.0 .

# 查看构建结果
docker images | grep user-service