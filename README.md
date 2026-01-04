
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

## 🔄 版本管理

### 查看版本信息

#### Telegram Bot
1. 发送 `/start`
2. 点击 **🛡️ 版本信息**
3. 查看当前版本和最新版本

#### Web面板
- 登录后台即可查看版本信息

### 一键更新

#### 方式1：Bot一键更新
1. 点击 **🛡️ 版本信息**
2. 如果有新版本，点击 **🔄 点击更新至最新版本**
3. 等待1-2分钟自动完成

#### 方式2：命令行更新
在VPS上重新执行安装脚本即可：
```bash
bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/scripts/install.sh)
```

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

