<div align="center">
  <h1>👑 King-Detective</h1>
  <p><strong>企业级 Oracle 云实例一站式管理平台</strong></p>
  <p>
    <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk" />
    <img src="https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen?style=flat-square&logo=springboot" />
    <img src="https://img.shields.io/badge/OCI%20SDK-3.77.2-red?style=flat-square" />
    <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" />
  </p>
  <p>通过精美的 Web 控制台或 Telegram Bot，统一管理所有甲骨文云账号和实例 —— 全部真实实时数据，零虚假接口。</p>
</div>

---

## ✨ 功能特性

### 🖥️ Web 控制台

| 功能 | 说明 |
|------|------|
| 🌐 实例实时看板 | 每 60 秒自动刷新，展示名称 / 状态 / IP / 规格 |
| ⚡ 一键开关机 | 直接在控制台启动 / 停止实例 |
| 🔧 高级操作 | 重装系统、Netboot iPXE 救援、启动卷快照、IPv6、标签、定时开关机 |
| 📊 全局统计 | 实例分布饼图、区域条形图（ECharts 驱动） |
| ⚙️ 设置面板 | Telegram Bot 配置、缓存时长、告警邮件、修改密码 |
| 💓 系统心跳 | 每 30 秒检测后端状态，绿/红指示灯实时显示 |
| 🔐 登录安全 | IP 黑名单、5 次失败自动封禁、支持 MFA（TOTP）二步验证 |

### 🤖 Telegram Bot（63 个功能模块）

| 类别 | 功能列表 |
|------|---------|
| 实例管理 | 开机 / 关机 / 重启 / 销毁 / 改名 / 变更规格 |
| 一键救援 | Auto-Rescue（10步全自动）、重装系统、Netboot/iPXE |
| 存储管理 | 启动卷备份、快照、挂载/分离、扩容 |
| 网络管理 | IPv6 配置、安全规则一键开放、500M NLB 带宽调整 |
| 监控告警 | CPU / 内存 / 带宽用量、流量历史、每日日报 |
| SSH & 密钥 | 远程命令执行、API Key 管理 |
| 账户管理 | 添加/删除 OCI 账户、存活检测、配额查询 |
| 系统工具 | 加密备份/恢复、MFA 管理、AI 聊天（SiliconFlow） |
| 智能运维 | 自动换 IP、自动区域扩容、自动重启监控 |

---

## 🚀 快速开始

### Docker Compose 部署（推荐）

```bash
# 1. 克隆仓库
git clone https://github.com/tony-wang1990/King-Detective.git
cd King-Detective

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env，填入你的配置

# 3. 启动
docker compose up -d
```

访问地址：`http://服务器IP:9527`

### 环境变量说明

| 变量名 | 是否必填 | 说明 |
|--------|---------|------|
| `ADMIN_USERNAME` | 否 | Web 登录用户名（默认：`admin`） |
| `ADMIN_PASSWORD` | 否 | Web 登录密码（默认：`admin123456`） |
| `TELEGRAM_BOT_TOKEN` | **必填** | 从 @BotFather 获取的 Bot Token |
| `TELEGRAM_BOT_USERNAME` | 否 | Bot 用户名（默认：`king_detective_bot`） |
| `OPENAI_API_KEY` | 否 | SiliconFlow API Key（AI 聊天功能） |

> ⚠️ **首次登录后请立即在设置中修改默认密码！**

### 一键更新

```bash
bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/update.sh)
```

---

## 📁 项目结构

```
King-Detective/
├── src/main/
│   ├── java/com/tony/kingdetective/
│   │   ├── controller/        # REST API 控制器
│   │   ├── service/impl/      # 业务逻辑（OCI SDK 真实调用）
│   │   ├── telegram/          # Telegram Bot（63 个 Handler）
│   │   ├── config/            # 实例拉取器、虚拟线程配置
│   │   ├── bean/              # DTO、实体、请求/响应体
│   │   └── utils/             # 工具类
│   └── resources/
│       ├── static/index.html  # 单页 Web UI（Alpine.js + ECharts）
│       ├── application.yml    # 应用配置
│       └── *.xml              # MyBatis Mapper XML
├── docs/                      # 文档目录
│   ├── DEPLOYMENT.md          # 完整部署指南
│   ├── API.md                 # REST API 文档
│   ├── FAQ.md                 # 常见问题解答
│   ├── SECURITY.md            # 安全策略
│   └── GOOGLE_LOGIN_GUIDE.md  # Google OAuth2 接入指南
├── scripts/                   # 辅助脚本
├── sql/                       # 数据库建表与迁移脚本
├── Dockerfile                 # 容器构建文件
├── docker-compose.yml         # Compose 配置
└── update.sh                  # 一键更新脚本
```

---

## ⚙️ 添加 OCI 账户

**Web UI 方式：** 设置 → OCI 账户 → 添加  
**Telegram 方式：** 发送 `/account` → 添加账户 → 按提示操作

所需 OCI 凭证：
- 租户 OCID（Tenancy OCID）
- 用户 OCID（User OCID）
- 指纹（Fingerprint）
- 私钥文件（`.pem` 格式）
- 区域（Region）

---

## 🏗️ 技术栈

| 层级 | 技术选型 |
|------|---------|
| 后端 | Java 21 + Spring Boot 3.4.1 |
| OCI SDK | `oci-java-sdk-shaded-full` 3.77.2 |
| 数据库 | SQLite（通过 MyBatis-Plus 操作） |
| 前端 | Alpine.js + ECharts + Tailwind CSS |
| 机器人 | TelegramBots 7.x（长轮询模式） |
| 并发 | Java 21 虚拟线程（Virtual Threads） |
| 认证 | SaToken（JWT 风格，请求自动续期） |

---

## 📚 文档

- [完整部署指南](docs/DEPLOYMENT.md)
- [API 接口文档](docs/API.md)
- [常见问题 FAQ](docs/FAQ.md)
- [安全政策](docs/SECURITY.md)
- [Google 登录接入指南](docs/GOOGLE_LOGIN_GUIDE.md)

---

## 📄 开源协议

[MIT](LICENSE) © Tony Wang