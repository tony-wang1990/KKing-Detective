
##  一键部署 (VPS)

### 方式 1：自动安装脚本（推荐）

直接复制以下命令到服务器终端运行即可：

```bash
bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/scripts/install.sh)
```

脚本会自动完成：

- ✅ 环境检测（Docker/Docker Compose）
- ✅ 拉取最新Docker镜像
- ✅ 启动服务（自动重启）
- ✅ 支持一键更新（拉取最新版本）


## 💻 访问应用

- **Web面板：** `http://your-ip:9527`
- **默认账号：** `admin`
- **默认密码：** `admin123456`

---

## 🔄 一键更新（保留所有数据）

### ⚠️ 重要说明

**更新不会丢失任何数据！**
- ✅ 保留账户密码配置
- ✅ 保留OCI私钥文件
- ✅ 保留所有历史记录
- ✅ 自动备份数据库

### 更新步骤

在服务器上执行以下命令：

```bash
# 设置你的配置信息（替换为你的实际值）
export BOT_TOKEN="你的Telegram_Bot_Token"
export ADMIN_USERNAME="你的管理员用户名"
export ADMIN_PASSWORD="你的管理员密码"

# 下载并执行更新脚本
wget https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/update.sh
bash update.sh
```

**或者一行命令：**

```bash
BOT_TOKEN="你的Token" ADMIN_USERNAME="你的用户名" ADMIN_PASSWORD="你的密码" bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/update.sh)
```

### 更新流程

脚本会自动：
1. 📋 备份现有数据库
2. 🛑 停止旧容器
3. ⬇️ 拉取最新Docker镜像
4. 🚀 启动新容器（挂载原有数据卷）
5. ✅ 验证服务状态

### 数据保存位置

- **数据库：** `/root/king-detective/data/king-detective.db`
- **私钥：** `/root/king-detective/keys/`
- **备份：** `/root/king-detective/data/king-detective.db.backup.*`

---

## 🔄 版本管理

### 查看版本信息

#### Telegram Bot
1. 发送 `/start`
2. 点击 **🛡️ 版本信息**
3. 查看当前版本和最新版本

#### Web面板
- 登录后台即可查看版本信息

---

## ⚙️ 修改账号密码

如果需要修改登录账号和密码：

```bash
# 编辑配置文件
nano /app/king-detective/application.yml

# 修改以下内容
web:
  account: "您的新账号"
  password: "您的新密码"

# 重启服务生效
cd /app/king-detective
docker-compose restart king-detective
```

---

## 📝 更多文档

- **详细更新说明：** [UPDATE.md](./UPDATE.md)
- **问题排查：** 查看容器日志 `docker logs -f king-detective`

