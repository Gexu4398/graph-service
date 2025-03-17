# 项目结构说明文档

## 项目概述

该项目是一个基于Spring Boot框架的图数据服务系统，采用多模块结构设计，主要用于图数据的处理、存储和管理。

## 项目模块结构

```
graph-service/
├── biz-model            // 业务数据模型层
├── biz-service          // 核心业务服务层
├── biz-keycloak-model   // Keycloak认证相关模型
├── biz-shell            // 命令行工具模块
├── biz-batch-service    // 批处理服务模块
├── test-environments    // 测试环境配置
├── testcontainers-minio // MinIO测试容器
├── biz-graph            // 图数据处理模块
```

### 模块功能说明

1. **biz-model**：定义了系统的核心数据模型，包括本体（Ontology）及其属性等JPA实体。

2. **biz-service**：提供核心业务功能，包括API接口、业务逻辑处理等。

3. **biz-keycloak-model**：集成Keycloak身份认证相关的数据模型。

4. **biz-shell**：基于Spring Shell的命令行工具，用于系统管理和交互式操作。

5. **biz-batch-service**：提供批量数据处理功能，基于Spring Batch实现。

6. **test-environments**：包含测试环境配置和测试用例。

7. **testcontainers-minio**：使用Testcontainers实现的MinIO测试容器。

8. **biz-graph**：图数据处理相关功能模块。

## 技术栈

### 核心框架
- Java 21
- Spring Boot 3.4.2
- Spring Cloud 2024.0.0
- Spring Shell 3.4.0

### 数据存储
- PostgreSQL (关系型数据库)
- Neo4j (图数据库)
- Redis (缓存)
- Elasticsearch (全文搜索)
- MinIO (对象存储)

### 安全认证
- Keycloak 26.1.3 (身份认证与授权)

### 批处理
- Spring Batch (批量数据处理)

### 搜索引擎
- Hibernate Search 7.2.2
- Elasticsearch (全文搜索引擎)

### API文档
- SpringDoc OpenAPI 2.8.5

### 辅助工具
- Hutool 5.8.36 (工具库)
- Apache POI 5.4.0 (办公文档处理)
- PDFBox 3.0.4 (PDF处理)
- Docx4j 11.5.2 (Word文档处理)
- JSoup 1.19.1 (HTML解析)

### 测试框架
- JUnit Jupiter
- Testcontainers 1.20.5

### 其他
- Lombok (代码简化)
- JodaTime (日期时间处理)
- Spring AI 1.0.0-M6 (AI集成)

## 部署环境

项目支持两种环境配置：
- 开发环境（dev）- 默认
- 生产环境（prod）

系统的各种外部服务（如PostgreSQL、Redis、Elasticsearch等）均可以通过环境变量自定义配置，默认使用localhost。