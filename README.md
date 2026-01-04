# King-Detective 🐢

**王探长** - 基于 Oracle OCI SDK 开发的 Web 端可视化甲骨文云助手

## ⚙ 核心功能

1. 支持同时批量添加多个租户配置信息
2. 实例管理、引导卷配置、一键附加IPv6、安全列表管理
3. **一键自动更新** ⭐ - 点击按钮即可自动more新最新版本
4. **Cloud Shell 控制台** ⭐ - VNC连接，方便netboot救砖
5. 多租户同时批量抢机，支持断点续抢
6. 根据 CIDR 网段更换公共IP，支持自动更新 Cloudflare DNS
7. Telegram 机器人操作支持
8. 实时流量统计（分钟级别）
9. 前端页面实时查看后端日志
10. 加密备份恢复，实现无缝数据迁移
11. MFA登录验证功能
12. Google 一键登录支持
13. AI 聊天助手（基于硅基流动API）

## 💻 一键部署

```bash
bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/install.sh)
```

安装完成后访问 `http://your-ip:9527`

默认账号：`admin` / `admin123456`

## 📁 部署后目录结构

```
/app/king-detective
├── keys/                          # PEM 密钥文件目录
├── application.yml                # 项目配置文件
├── docker-compose.yml             # Docker Compose 配置
├── king-detective.db              # SQLite 数据库
└── update_version_trigger.flag   # 版本更新触发文件
```

## 🔄 一键更新

系统支持一键自动更新功能：

1. 登录后进入系统设置
2. 点击"一键更新"按钮
3. 系统will自动拉取最新镜像并重启

**注意：** 更新过程中配置和数据不会丢失

## 🌏 Nginx 反向代理（可选）

如需配置 HTTPS 和 Cloud Shell VNC 访问：

```nginx
location /myvnc/ {
    proxy_pass http://127.0.0.1:6080/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}

location / {
    proxy_pass http://127.0.0.1:9527;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection 'upgrade';
}
```

## 📝 更新日志

### v2.1.0 (2026-01-04)

- ✨ 新增一键自动更新功能
- ✨ 新增 Cloud Shell VNC 控制台
- ✨ 集成 watcher 容器自动监控版本
- 🐛 修复编译警告（@Builder.Default）
- 🐛 修复unchecked警告

### v2.0.0 (2026-01-04)

- ✅ 修复 MyBatis XML 属性大小写错误
- ✅ 修复 pom.xml XML 编码问题
- ✅ 修复 Spring AI API 兼容性

## 🔒 注意事项

- ⚠️ 开机频率过高可能导致封号，请合理使用
- 🔐 建议使用密钥登录服务器，防止API数据泄露
- 📃 记得定时清理 Docker 日志

## ©️ 版权信息

**King-Detective** © 2026 Tony Wang. All Rights Reserved.

基于 [oci-helper](https://github.com/Yohann0617/oci-helper) 项目改进

## 📞 反馈与支持

- GitHub Issues: <https://github.com/tony-wang1990/King-Detective/issues>
- Release: <https://github.com/tony-wang1990/King-Detective/releases>
