# 面向工程教育认证的教学档案管理系统

一套面向高校工程教育认证场景的教学档案管理系统，覆盖教学档案的采集、分类、审核、归档与工程教育认证指标（毕业要求、指标点、课程目标）的关联管理。前后端分离，含完整的用户权限体系（RBAC + JWT）。

> 本科毕业设计项目，独立完成前后端设计与开发。

## 技术栈

**后端**：Java · Spring Boot · Spring Security · JWT · MyBatis-Plus · MySQL
**前端**：Vue 3 · Vite · Vue Router · Pinia · Axios
**其他**：BCrypt 密码加密、RBAC 角色权限、文件上传

## 核心功能

- **用户与权限**：登录鉴权（Spring Security + JWT）、RBAC 角色/菜单权限控制、密码 BCrypt 加密
- **教学档案管理**：档案信息、档案分类、档案文件上传、多级审核流程、销毁日志
- **工程教育认证**：毕业要求、指标点、课程目标及其映射关系维护，支撑认证指标达成度管理
- **报表与系统**：数据报表、系统配置、操作日志与登录日志

## 项目结构

```
管理系统/
├── backend/                     # Spring Boot 后端
│   └── src/main/java/com/edu/archive/
│       ├── controller/          # 接口层（档案/认证/指标/报表/权限/用户…）
│       ├── service/             # 业务层
│       ├── entity/              # 实体
│       ├── security/            # JWT + Spring Security
│       └── config/              # 配置
├── frontend/                    # Vue 3 前端
│   └── src/
│       ├── api/                 # 接口封装
│       ├── views/               # 页面（archive/certification/indicator/report/system/user）
│       ├── router/  stores/  components/
├── sql/schema.sql               # 建表 + 初始化数据
└── run.bat
```

## 快速开始

### 1. 数据库
```sql
CREATE DATABASE edu_archive DEFAULT CHARACTER SET utf8mb4;
-- 导入建表与初始化数据
SOURCE sql/schema.sql;
```

### 2. 后端
先在 `backend/src/main/resources/application.yml` 配置数据库密码（或设置环境变量 `DB_PASSWORD`），然后：
```bash
cd backend
mvn spring-boot:run          # 默认端口 8080
```

### 3. 前端
```bash
cd frontend
npm install
npm run dev                  # 默认 Vite 端口 5173
```

### 默认账号
初始管理员用户名 `admin`（密码见 `sql/schema.sql` 中的初始化数据，首次登录后请及时修改）。

## 说明

本科毕业设计，用于实践 Spring Boot + Vue 前后端分离开发、Spring Security/JWT 鉴权、RBAC 权限设计与工程教育认证业务建模。配置中的账号密码、JWT 密钥已做占位处理，运行前请替换为自己的值。
