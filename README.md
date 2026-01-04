

##  一键部署 (VPS)

### 方式 1：自动安装脚本（推荐）

直接复制以下命令到服务器终端运行即可：

```bash
bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/scripts/install.sh)
```

脚本会自动完成：

- 环境检测 (Docker/Compose)
- 拉取最新镜像
- 启动服务


修改账号密码
如果需要修改登录账号和密码，请在服务器上执行以下步骤：

修改配置文件：
bash
nano /app/king-detective/application.yml
如果是手动部署，文件路径可能为 
./application.yml
找到并修改以下内容：
yaml
web:
  account: "您的新账号"
  password: "您的新密码"
重启服务生效：
bash
docker-compose restart king-detective



## 💻 访问应用

- **地址：** `http://your-ip:9527`
- **默认账号：** `admin`
- **默认密码：** `admin123456`


