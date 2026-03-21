<div align="center">
  <h1>👑 King-Detective</h1>
  <p><strong>Enterprise OCI Instance Management Platform</strong></p>
  <p>
    <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk" />
    <img src="https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen?style=flat-square&logo=springboot" />
    <img src="https://img.shields.io/badge/OCI%20SDK-3.77.2-red?style=flat-square" />
    <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" />
  </p>
  <p>Manage all your Oracle Cloud Infrastructure accounts and instances from a stunning Web UI or via Telegram Bot — with real-time data, no mock, no fake.</p>
</div>

---

## ✨ Features

### 🖥️ Web Dashboard
| Feature | Description |
|---------|-------------|
| 🌐 Real-time Instance List | Live polling every 60s, displays name / state / IP / shape |
| ⚡ One-click Power Control | Start / Stop instances directly from dashboard |
| 🔧 Advanced Actions | Re-image, Netboot iPXE, Boot Volume Snapshot, IPv6, Tags, Scheduled Power |
| 📊 Global Insights | Instance distribution pie chart, region bar chart via ECharts |
| ⚙️ Settings Panel | Telegram Bot config, cache duration, alert email, password change |
| 💓 System Heartbeat | 30s polling of `/api/sys/glance`, green/red indicator |
| 🔐 Login Security | IP blacklist, 5-attempt auto-ban, MFA (TOTP) support |

### 🤖 Telegram Bot (63 Handlers)
| Category | Capabilities |
|----------|-------------|
| Instance Management | Start / Stop / Reboot / Terminate / Rename / Shape change |
| One-click Rescue | Auto-Rescue (10-step), Re-image, Netboot/iPXE |
| Storage | Boot Volume backup, snapshot, attach/detach, resize |
| Network | IPv6, Security rule release, 500M NLB bandwidth boost |
| Monitoring | CPU / RAM / Bandwidth usage, traffic history, daily report |
| SSH & Keys | Remote command execution, API Key management |
| Account Management | Add/remove OCI accounts, Check-alive, Quota query |
| System | Backup/Restore (encrypted zip), MFA, AI chat (SiliconFlow) |
| Smart Features | Auto IP change, Auto region expansion, Auto restart monitoring |

---

## 🚀 Quick Start

### Docker Compose (Recommended)

```bash
# 1. Clone
git clone https://github.com/tony-wang1990/King-Detective.git
cd King-Detective

# 2. Configure environment
cp .env.example .env
# Edit .env with your values

# 3. Start
docker compose up -d
```

Access WebUI at: `http://your-server-ip:9527`

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `ADMIN_USERNAME` | No | Web login username (default: `admin`) |
| `ADMIN_PASSWORD` | No | Web login password (default: `admin123456`) |
| `TELEGRAM_BOT_TOKEN` | **Yes** | Your Telegram Bot token from @BotFather |
| `TELEGRAM_BOT_USERNAME` | No | Bot username (default: `king_detective_bot`) |
| `OPENAI_API_KEY` | No | SiliconFlow API key for AI chat |

> ⚠️ **Change default password after first login!**

### Update

```bash
bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/update.sh)
```

---

## 📁 Project Structure

```
King-Detective/
├── src/main/
│   ├── java/com/tony/kingdetective/
│   │   ├── controller/        # REST API endpoints
│   │   ├── service/impl/      # Business logic (OCI SDK calls)
│   │   ├── telegram/          # Telegram Bot (63 handlers)
│   │   ├── config/            # OracleInstanceFetcher, VirtualThread
│   │   ├── bean/              # DTOs, entities, params, responses
│   │   └── utils/             # CommonUtils, OciUtils, InputValidator
│   └── resources/
│       ├── static/index.html  # Single-page Web UI (Alpine.js + ECharts)
│       ├── application.yml    # App configuration
│       └── *.xml              # MyBatis mapper XMLs
├── docs/                      # Documentation
│   ├── DEPLOYMENT.md          # Full deployment guide
│   ├── API.md                 # REST API reference
│   ├── FAQ.md                 # Frequently asked questions
│   ├── SECURITY.md            # Security policy
│   └── GOOGLE_LOGIN_GUIDE.md  # Google OAuth2 setup guide
├── scripts/                   # Helper scripts
├── sql/                       # Database schema & migrations
├── Dockerfile                 # Container build
├── docker-compose.yml         # Compose configuration
└── update.sh                  # One-click update script
```

---

## ⚙️ Configuration

### Add OCI Account

**Via Web UI:** Settings → OCI Account Profiles → Add  
**Via Telegram:** `/account` → Add Account → follow prompts

Required OCI credentials:
- Tenancy OCID
- User OCID  
- Fingerprint
- Private key (`.pem` file)
- Region

### Telegram Bot Setup

1. Create bot via [@BotFather](https://t.me/BotFather)
2. Set `TELEGRAM_BOT_TOKEN` in `.env`
3. After start, send `/start` to your bot
4. Configure Chat ID in Web UI → Settings

---

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21 + Spring Boot 3.4.1 |
| OCI SDK | `oci-java-sdk-shaded-full` 3.77.2 |
| Database | SQLite (via MyBatis-Plus) |
| Frontend | Alpine.js + ECharts + Tailwind CSS |
| Bot | TelegramBots 7.x (long-polling) |
| Threading | Java 21 Virtual Threads |
| Auth | SaToken (JWT-style, auto-renew) |

---

## 📚 Documentation

- [Deployment Guide](docs/DEPLOYMENT.md)
- [API Reference](docs/API.md)
- [FAQ](docs/FAQ.md)
- [Security Policy](docs/SECURITY.md)
- [Google Login Setup](docs/GOOGLE_LOGIN_GUIDE.md)

---

## 📄 License

[MIT](LICENSE) © Tony Wang