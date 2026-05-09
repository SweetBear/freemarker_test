markdown
# Freemarker 静态页面生成项目

基于 Spring Boot + Freemarker + MongoDB GridFS 实现的**博客静态页面自动生成服务**，项目启动时自动生成静态 HTML 并上传到 MongoDB 存储。

---

## ✨ 核心功能

- ✅ **项目启动自动生成静态页面**  
  实现 `CommandLineRunner` 接口，Spring Boot 启动完成后自动执行静态化逻辑。

- ✅ **Freemarker 模板渲染**  
  支持博客列表页、详情页模板静态化，自动生成 HTML 字符串。

- ✅ **MongoDB GridFS 存储静态页面**  
  生成的 HTML 文件直接上传到 GridFS，支持覆盖、删除、查询。

- ✅ **JSON 数据源驱动**  
  从本地 `blogs.json` 读取博客数据，支持外部修改文件自动重新生成页面。

- ✅ **数据变更自动同步**
  - 新增/修改博客 → 自动更新对应详情页 + 列表页
  - 删除博客 → 自动删除 GridFS 中的静态页面
  - JSON 文件外部修改 → 全量重新生成

- ✅ **阅读次数分离**  
  静态页面不包含 `readCount`，由前端 Vue 异步获取，保证静态页稳定。

---

## 🛠 技术栈

- Spring Boot 2.x
- Freemarker
- MongoDB + GridFS
- Java 8+
- Maven

---

## 📁 项目结构
com.billfang├── entity/ # 实体类（Blog）├── service/ # 静态页面生成服务├── util/ # 工具类│ ├── FreemarkerUtil # 模板渲染│ ├── GridFsUtil # GridFS 上传 / 删除│ └── JsonUtil # JSON 文件读取 + 监听└── resources/└── templates/ # Freemarker 模板├── blogList.ftl└── blogDetail.ftl
plaintext

---

## 🚀 核心执行流程

1. 项目启动
2. 执行 `CommandLineRunner.run()`
3. 读取 `blogs.json` 数据
4. Freemarker 渲染列表页 + 所有详情页
5. 上传 HTML 到 MongoDB GridFS
6. 监听 JSON 文件变化，自动重新生成
7. 提供增删改查同步接口

---

## 📌 启动说明

1. 配置 MongoDB 连接（`application.yml`）
2. 准备 `blogs.json` 数据文件
3. 启动 Spring Boot 项目
4. 查看日志：
=== 静态页面初始化完成，已上传至 MongoDB GridFS ===
plaintext

---

## 📄 静态文件说明

- 列表页：`blogList.html`
- 详情页：`blog{id}.html`
- 存储位置：MongoDB GridFS
- 访问方式：通过文件名从 GridFS 读取并返回前端

---

## 👤 作者

SweetBear
