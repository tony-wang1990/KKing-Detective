<div align="center">
  <h1>👑 King-Detective v4.1.2</h1>
  <p><strong>专业的 Oracle Cloud (OCI) 实例管理集成系统</strong></p>
  <p><i>Telegram Bot 极速响应终端 + 全新玻璃拟态 (Glassmorphism) Web UI</i></p>
  
  [![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
  [![Version](https://img.shields.io/badge/Version-v4.1.2-green.svg)]()
  [![Java](https://img.shields.io/badge/Java-21-orange.svg)]()
  [![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2+-brightgreen.svg)]()
</div>

---

## 📖 项目简介

`King-Detective` 是一款为甲骨文云 (OCI) 玩家与管理员量身定制的深度架构管理工具。通过底层的 **OCI Java SDK** 直接调用真实物理机器资源流，它不仅支持极致便捷的 Telegram Bot 移动端随时发包交互，更在 v4.1.2 版本中迎来了 **革命性的 Web 端全图形化重构**。这无异于搭建属于您个人的 OCI 随身控制台。

---

## ✨ 核心特性矩阵

### 🌐 全新 Web 图形化仪表盘 
- 💠 **玻璃拟态设计（Glassmorphism）**：摒弃传统丑陋的面板元素，使用全面响应式的现代极简美学，适配 PC 与移动端浏览器。
- 📊 **全局洞察雷达（Global Insights）**：内置并深度集成 `ECharts` 数据计算引擎。通过动态雷达图与环形图清晰勾勒全球区域分布、实例存活健康率以及您挂载的多个子账户的资产水位分布。
- 🖥️ **无假数据、纯净直连面板**：实时同步 OCI 官方的在线网卡状态、公网/内网 IP 以及 CPU/内存 Shape 参数。

### 🤖 Telegram Bot 随身全能移动端
- 🚀 **一键移动极速控制**：随时随地通过 Telegram 发送按钮回调，实现对服务器的 **开/关机、软重启、上限带宽动态推拉调节** 甚至账单配额的一键获取。
- 💡 **VPS 节点雷达**：内部寄宿了持久化运行的探针进程节点，全天候 24H 帮您紧盯主机可用性和网络失联警报。

### 🚑 “傻瓜式”重构与救砖工程
- 💿 **Netboot.xyz 网络装机模式**：通过 API 自动化拦截并篡改挂载的 iPXE 引导脚本，一键强刷进入远端网络 PE 界面装机。
- 🔄 **Re-image 镜像翻新 (保留数据卷) 模式**：业内极其独特的 `保留原数据卷重建实例` 功能。在保留原硬盘底座数据的前提下，为您翻新计算节点、突破黑洞封锁。

### 🔐 进阶运维与合规拓展
- **快照/回滚流水线**：支持 Web / TG 双端直接打快照 (Boot Volume Snapshot)，为折腾系统留存最硬的悔棋。
- **免密码指纹云管理**：无需通过跳板机连接服务器，直接从网页侧边栏进行安全分叉 —— 后台全自动注入或吊销实例绑定的 SSH 公钥，杜绝暴力破解。
- **自动化定时计划**：精准调度 OCI API，随时为任意机器配置例如 “夜间自动关机、清晨自动唤醒” 的云节能任务。

---

## 🛠️ 部署与架构总览

本项目的架构极其轻量且现代化，数据持久化依托于自带的 SQLite，无需额外部署繁重的 MySQL 即可直接运行：
> 前端：HTML5 + Alpine.js + Tailwind CSS 风 (Vanilla CSS)  
> 后端：Java 21 + Spring Boot 3x + OCI API SDK + MyBatisPlus


## 🚀 极速安装指南

提供了自动化部署脚手架，适用于 Ubuntu / Debian / CentOS 等主流 VPS 基础环境。

### 方式 1：一键拉起（全新安装推荐）
复制并在终端内以 root 身份执行以下命令：
```bash
bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/scripts/install.sh)
```
> *注：脚本将全自动处理 JDK / Docker 探针的环境判定、私钥挂载与依赖编译环境服务。*

### 方式 2：平滑热更新 (保留所有配置与密钥)
如果您已经是 King-Detective 的老用户，只需输入以下内容执行无感热更：
```bash
BOT_TOKEN="老Token" ADMIN_USERNAME="您的用户名" ADMIN_PASSWORD="密码" bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/update.sh)
```

---

## ⚙️ 接入配置说明

启动成功后，您可以通过下述方式验证运行结果：
- **Web 面板入口**： `http://您的服务器IP:9527`
- **默认超级管理员**： `admin`
- **默认安全密码**： `admin123456`
- **核心数据隔离区**： 您的所有数据库（`data`）及您的 OCI `.pem` 密钥文件 (`keys`) 将集中安全地锚定在 `/root/king-detective/` 目录下。

---

## 🛡️ 诊断与安全性审计
项目的每一行底层操作均为调用 OCI Java SDK 的正规 API Token 交互，不包含任何恶意转发或硬编码 Stub 数据拦截。相关的验证说明可参考位于 `docs/` 目录下的额外架构报告。

---

## 📝 开发者与免责声明
- **Author:** Tony Wang
- **Repository:** [tony-wang1990/King-Detective](https://github.com/tony-wang1990/King-Detective)
- **License:** Apache License 2.0

> ⚠️ *该项目仅供网络构架技术交流及 OCI Restful API 学习调试之用。严禁将本平台应用于任何违反甲骨文 (Oracle Cloud) 用户使用条款或服务条例 (TOS) 的非法/滥用活动。使用本程序导致的任何封号、机器回收等经济及连带损失，由使用者自行承担。*