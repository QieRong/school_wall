# README.md 部署章节补充

## 在README.md的"配置说明"章节后添加以下内容：

---

## 🚀 生产环境部署

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.6+
- Nginx（可选，用于反向代理）

### 环境变量配置

#### 必须配置的环境变量

**1. JWT密钥（必须）**

⚠️ **安全警告**：生产环境必须修改JWT密钥，不要使用默认值！

```bash
export JWT_SECRET="your-strong-secret-key-at-least-32-characters-long"
```

建议使用以下命令生成强密钥：

```bash
# Linux/Mac
openssl rand -base64 32

# 或使用在线工具生成
```

**2. 数据库配置（必须）**

```bash
export DB_HOST="your-database-host"
export DB_PORT="3306"
export DB_NAME="biaobaiqiang"
export DB_USERNAME="your-database-username"
export DB_PASSWORD="your-database-password"
```

**3. 文件上传配置（推荐）**

```bash
# 文件上传路径（建议使用绝对路径）
export FILE_UPLOAD_PATH="/data/files/"

# 文件访问基础URL（改为你的域名）
export FILE_BASE_URL="http://your-domain.com"
```

**4. DeepSeek AI配置（可选）**

如果使用AI助手功能：

```bash
export DEEPSEEK_API_KEY="your-deepseek-api-key"
```

#### 可选配置

```bash
# 服务器端口（默认19090）
export SERVER_PORT="19090"

# JWT过期时间（默认7天，单位：毫秒）
export JWT_EXPIRATION="604800000"

# AI每日调用限制（默认10次）
export DEEPSEEK_DAILY_LIMIT="10"
```

### 后端部署

#### 方式1：使用Maven打包

```bash
cd springboot

# 1. 清理并打包（跳过测试）
mvn clean package -DskipTests

# 2. 设置环境变量（示例）
export JWT_SECRET="your-production-secret-key"
export DB_HOST="localhost"
export DB_USERNAME="root"
export DB_PASSWORD="your-password"

# 3. 启动应用
java -jar target/springboot-0.0.1-SNAPSHOT.jar
```

#### 方式2：使用systemd服务（推荐）

创建服务文件 `/etc/systemd/system/confession-wall.service`：

```ini
[Unit]
Description=Confession Wall Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/confession-wall
ExecStart=/usr/bin/java -jar /opt/confession-wall/springboot-0.0.1-SNAPSHOT.jar

# 环境变量
Environment="JWT_SECRET=your-production-secret-key"
Environment="DB_HOST=localhost"
Environment="DB_USERNAME=root"
Environment="DB_PASSWORD=your-password"
Environment="FILE_UPLOAD_PATH=/data/files/"
Environment="FILE_BASE_URL=http://your-domain.com"

# 日志
StandardOutput=journal
StandardError=journal

# 重启策略
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
sudo systemctl daemon-reload
sudo systemctl start confession-wall
sudo systemctl enable confession-wall
sudo systemctl status confession-wall
```

### 前端部署

#### 1. 配置生产环境变量

编辑 `vue-frontend/.env.production`：

```env
# 修改为你的后端地址
VITE_API_URL=http://your-domain.com
```

#### 2. 构建前端

```bash
cd vue-frontend

# 安装依赖
npm install

# 构建生产版本
npm run build
```

构建完成后，`dist` 目录包含所有静态文件。

#### 3. 部署到Nginx

**方式1：直接部署**

```bash
# 复制文件到Nginx目录
sudo cp -r dist/* /var/www/html/confession-wall/
```

**方式2：使用Nginx配置**

创建Nginx配置文件 `/etc/nginx/sites-available/confession-wall`：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /var/www/html/confession-wall;
        try_files $uri $uri/ /index.html;

        # 缓存静态资源
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }

    # 后端API代理
    location /api {
        proxy_pass http://localhost:19090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 文件访问代理
    location /files {
        proxy_pass http://localhost:19090;
        proxy_set_header Host $host;

        # 文件缓存
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # WebSocket代理
    location /ws {
        proxy_pass http://localhost:19090;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;

        # WebSocket超时设置
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
    }

    # Gzip压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    gzip_min_length 1000;
}
```

启用配置：

```bash
sudo ln -s /etc/nginx/sites-available/confession-wall /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### HTTPS配置（推荐）

使用Let's Encrypt免费SSL证书：

```bash
# 安装certbot
sudo apt install certbot python3-certbot-nginx

# 获取证书并自动配置Nginx
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo certbot renew --dry-run
```

### 数据库初始化

```bash
# 登录MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE biaobaiqiang CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入数据
mysql -u root -p biaobaiqiang < biaobaiqiang.sql

# 创建专用用户（推荐）
CREATE USER 'confession_user'@'localhost' IDENTIFIED BY 'strong_password';
GRANT ALL PRIVILEGES ON biaobaiqiang.* TO 'confession_user'@'localhost';
FLUSH PRIVILEGES;
```

### 文件上传目录

```bash
# 创建文件上传目录
sudo mkdir -p /data/files
sudo chown -R www-data:www-data /data/files
sudo chmod -R 755 /data/files
```

### 防火墙配置

```bash
# 开放必要端口
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 19090/tcp  # 如果需要直接访问后端
sudo ufw enable
```

### 监控和日志

#### 查看后端日志

```bash
# 如果使用systemd
sudo journalctl -u confession-wall -f

# 如果直接运行
tail -f logs/spring.log
```

#### 查看Nginx日志

```bash
# 访问日志
tail -f /var/log/nginx/access.log

# 错误日志
tail -f /var/log/nginx/error.log
```

### 性能优化建议

1. **数据库优化**：
   - 定期备份数据库
   - 添加必要的索引
   - 配置MySQL慢查询日志

2. **文件存储优化**：
   - 考虑使用对象存储（如阿里云OSS）
   - 配置CDN加速静态资源

3. **应用优化**：
   - 配置JVM参数（堆内存、GC策略）
   - 使用Redis缓存热点数据
   - 配置连接池参数

### 故障排查

#### 后端无法启动

```bash
# 检查端口占用
sudo lsof -i :19090

# 检查Java版本
java -version

# 检查数据库连接
mysql -h localhost -u root -p
```

#### 前端无法访问

```bash
# 检查Nginx状态
sudo systemctl status nginx

# 检查Nginx配置
sudo nginx -t

# 检查文件权限
ls -la /var/www/html/confession-wall
```

#### 文件上传失败

```bash
# 检查目录权限
ls -la /data/files

# 检查磁盘空间
df -h

# 检查Nginx上传大小限制
grep client_max_body_size /etc/nginx/nginx.conf
```

### 安全检查清单

- [ ] 修改了默认JWT密钥
- [ ] 数据库使用强密码
- [ ] 配置了防火墙规则
- [ ] 启用了HTTPS
- [ ] 定期备份数据库
- [ ] 配置了日志监控
- [ ] 限制了文件上传大小
- [ ] 配置了Nginx安全头

### 备份策略

#### 数据库备份

```bash
# 创建备份脚本 /opt/backup/backup-db.sh
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/opt/backup/mysql"
mkdir -p $BACKUP_DIR

mysqldump -u root -p'your-password' biaobaiqiang > $BACKUP_DIR/biaobaiqiang_$DATE.sql
gzip $BACKUP_DIR/biaobaiqiang_$DATE.sql

# 保留最近7天的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete
```

添加到crontab：

```bash
# 每天凌晨2点备份
0 2 * * * /opt/backup/backup-db.sh
```

#### 文件备份

```bash
# 备份上传的文件
tar -czf /opt/backup/files_$(date +%Y%m%d).tar.gz /data/files
```

---

## 📞 技术支持

如果在部署过程中遇到问题，请检查：

1. **环境变量是否正确配置**
2. **数据库连接是否正常**
3. **端口是否被占用**
4. **文件权限是否正确**
5. **防火墙规则是否配置**

**常见问题**：

- 登录失败：检查JWT密钥配置
- 文件上传失败：检查文件目录权限
- 跨域问题：检查Nginx代理配置
- WebSocket连接失败：检查Nginx WebSocket配置
