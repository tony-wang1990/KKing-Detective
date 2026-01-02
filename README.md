# King-Detective

## 一键部署

```bash
bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/install.sh)
```

## 手动部署

```bash
mkdir -p /app/king-detective/keys && cd /app/king-detective
wget https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/docker-compose.yml
wget https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/src/main/resources/application.yml
docker-compose up -d
```

**访问**: `http://your-ip:9527`  
**账号**: `admin` / `admin123456`
