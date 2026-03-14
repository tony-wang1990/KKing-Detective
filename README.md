# 👑 King-Detective v4.1.1
> **专业的 Oracle Cloud (OCI) 实例管理集成系统 —— Telegram Bot + 全新玻璃拟态 Web UI**

`King-Detective` 是一款为甲骨文云 (OCI) 用户量身定制的深度管理工具。它不仅支持极致便捷的 Telegram Bot 移动端交互，更在 v4.1.1 版本中迎来了**革命性的 Web 端重构**，集成了先进的 OCI 资源审计、数据可视化以及“傻瓜式”一键救砖能力。

---

## ✨ 核心特性

### 🌐 全新 Web 仪表盘 (v4.1.1)
- **玻璃拟态设计 (Glassmorphism)：** 极简主义现代设计，适配 PC 与移动端浏览器。
- **Global Insights 洞察雷达：** 集成 `ECharts` 数据引擎，通过雷达图、饼图直观统计全球区域分布、实例存活率及账户资产分布。
- **高级实例看板：** 实时同步 OCI 真实 API 数据（IP、状态、Shape），无任何虚假/Mock 信息。

### 🤖 Telegram Bot 全能交互
- **移动端首选：** 支持一键开关机、重启、带宽动态调整、流量监控与账单查询。
- **VPS 监控：** 内置监控探针，实时反馈主机性能状态。

### 🚑 “傻瓜式”一键救砖系统
- **Netboot.xyz 模式：** 自动化篡改 iPXE 脚本并软重启，实现一键进入网络 PE/引导重装界面。
- **Re-image (Keep Disk) 模式：** 自动化实现 `保留启动卷终止并原地重建`，让您在保留数据的同时翻新计算节点并强制更换 IP。

### 🔐 进阶资源管理
- **快照管理：** 支持 Web/TG 双端一键创建 Boot Volume 快照及备份恢复。
- **标签管理：** 无缝管理 OCI Freeform Tags 标签。
- **SSH 密钥抽屉：** 无需连接服务器，直接通过网页侧边栏管理 API Key (SSH 公钥)，支持实时查阅指纹与删除上传。
- **定时任务：** 支持对具体实例设置每日固定时间的 “晚关早开” 自动化电源策略。

---

## 🚀 快速开始

### 方式 1：自动安装脚本（推荐）
适用于 Ubuntu/Debian/CentOS 等 VPS 环境。复制以下命令运行：

```bash
bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/scripts/install.sh)
```

脚本自动完成环境检测、Docker 镜像拉取与自启动配置。

### 方式 2：手动更新 (v4.1.1)
如果您已安装旧版本，请通过以下一键命令平滑升级（不丢失任何私钥与数据）：

```bash
BOT_TOKEN="你的Token" ADMIN_USERNAME="用户名" ADMIN_PASSWORD="密码" bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/update.sh)
```

---

## ⚙️ 访问配置
- **Web 面板：** `http://your-ip:9527`
- **默认账号：** `admin` / **默认密码：** `admin123456`
- **数据目录：** `/root/king-detective/` (包含 `data` 数据库与 `keys` 私钥)

---

## 🛡️ 诊断与审计
项目所有后端接口均通过 `OCI Java SDK` 真实调用。您可在后台控制台查看 `audit_report.md` 获取关于功能逻辑真实性与代码质量的完整诊断报告。

---

## 📝 开发者与声明
- **Author:** Tony Wang
- **Repo:** [tony-wang1990/King-Detective](https://github.com/tony-wang1990/King-Detective)
- **License:** Apache License 2.0

---
*本项目仅供技术交流使用，请勿用于违反甲骨文使用条款的任何活动。*
