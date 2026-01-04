# King-Detective

基于 Oracle OCI SDK 开发的 Web 端可视化甲骨文云助手，支持多租户管理、实例监控、自动抢机等功能。

## 🚀 一键部署教程 (VPS)

### 方式 1：自动安装脚本（推荐）

直接复制以下命令到服务器终端运行即可：

```bash
bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/scripts/install.sh)
```

脚本会自动完成：

- 环境检测 (Docker/Compose)
- 拉取最新镜像
- 启动服务

### 方式 2：使用部署脚本

如果您已经克隆了项目：

```bash
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

---

## 💻 访问应用

- **地址：** `http://your-ip:9527`
- **默认账号：** `admin`
- **默认密码：** `admin123456`

---

## 🔄 如何更新

重新运行只要运行 **方式 1** 的命令，或者在系统设置中触发自动更新。
