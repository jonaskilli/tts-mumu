# CNB云原生开发配置说明

本项目已配置CNB云原生开发环境，支持在云端构建Android应用，并已优化国内访问速度。

## 配置文件说明

### 1. `.ide/Dockerfile`
- **基础镜像**: Ubuntu 22.04 + 阿里云APT镜像
- **Java环境**: OpenJDK 17
- **Android SDK**: API 34 + 命令行工具（使用清华大学镜像）
- **开发工具**: code-server + 常用VSCode插件
- **其他**: Git、OpenSSH等必备工具

### 2. `.cnb.yml`
- **资源规格**: 8核CPU（16GB内存）
- **启动流程**:
  - 配置国内镜像加速（自动）
  - 环境检查
  - 签名验证
- **支持服务**: VSCode远程开发
- **销毁前清理**: 恢复原始配置

### 3. `gradle.properties.cnb`
- **Gradle仓库**: 阿里云镜像加速
- **Maven仓库**: 阿里云Google、Public镜像
- **构建优化**: 启用缓存、并行构建

## 国内镜像加速详情

### 镜像源列表

#### 1. **APT软件包**（Ubuntu）
- **镜像源**: 阿里云
- **配置文件**: `/etc/apt/sources.list`
- **加速效果**: `apt-get install` 安装基础工具

#### 2. **Android SDK**
- **镜像源**: 清华大学 TUNA 镜像
- **地址**: `https://mirrors.tuna.tsinghua.edu.cn/android/android-repository/`
- **备用**: 大连东软镜像 `mirrors.neusoft.edu.cn`
- **加速效果**: SDK组件下载速度提升

#### 3. **Gradle仓库**
- **Google仓库**: 阿里云 `https://maven.aliyun.com/repository/google`
- **Public仓库**: 阿里云 `https://maven.aliyun.com/repository/public`
- **JCenter**: 阿里云 `https://maven.aliyun.com/repository/jcenter`
- **Gradle插件**: 阿里云 `https://maven.aliyun.com/repository/gradle-plugin`
- **加速效果**: 依赖下载速度大幅提升

#### 4. **Maven仓库**
- **Google**: 阿里云镜像
- **Maven Central**: 阿里云镜像
- **JCenter**: 阿里云镜像（已废弃但仍提供镜像）

### 镜像配置机制

启动云原生开发环境时，系统会自动：
1. 备份原 `gradle.properties` → `gradle.properties.backup`
2. 复制 `gradle.properties.cnb` → `gradle.properties`
3. 配置国内镜像源生效

关闭环境时自动恢复原配置。

## 使用方法

### 本地使用

1. **配置签名**
   在项目根目录创建 `local.properties` 文件：
   ```properties
   KEY_PATH=/workspace/release.jks
   KEY_PASSWORD=你的密钥库密码
   ALIAS_NAME=TTSServer
   ALIAS_PASSWORD=你的密钥密码
   ```

2. **启动开发环境**
   - 访问仓库分支页面
   - 点击右上角"云原生开发"按钮
   - 等待环境启动（首次构建Docker镜像约需5-10分钟）

3. **构建APK**
   进入开发环境后，在终端执行：
   ```bash
   # 国内镜像已自动配置，直接构建即可
   ./gradlew assembleAppRelease

   # 或者清理后构建
   ./gradlew clean assembleAppRelease
   ```

4. **下载APK**
   构建产物位于：`/workspace/app/build/outputs/apk/app/release/`

### GitHub Actions自动构建

项目已配置GitHub Actions工作流，支持自动构建：

#### 触发条件
- `test.yml`：推送到master或compose分支时
- `release.yml`：推送CHANGELOG.md到compose分支时

#### 配置GitHub Secrets
在仓库Settings > Secrets and variables > Actions中添加：

1. `ALIAS_NAME` = TTSServer
2. `ALIAS_PASSWORD` = 你的密钥密码
3. `KEY_PASSWORD` = 你的密钥库密码
4. `KEY_STORE` = Base64编码的签名文件内容

生成Base64编码：
```bash
# 在本地执行
openssl base64 < release.jks | tr -d '\r\n' > release.jks.base64.txt
# 将release.jks.base64.txt的内容复制到KEY_STORE
```

## 注意事项

1. **首次启动较慢**
   - 首次启动需要构建Docker镜像，预计需要5-10分钟
   - 后续启动会复用缓存，速度更快
   - 使用国内镜像可加速首次构建

2. **资源限制**
   - 开发环境配置为8核CPU（内存自动分配16GB）
   - 如需调整，可修改 `.cnb.yml` 中的 `runner.cpus` 值

3. **签名安全**
   - `local.properties` 文件不会提交到Git仓库
   - GitHub Secrets用于CI/CD环境
   - 请勿将签名信息泄露到公开仓库

4. **构建优化**
   - 使用 `--build-cache` 参数利用Gradle缓存加速构建
   - 使用 `--parallel` 参数并行构建加快速度
   - 国内镜像已自动配置，无需手动设置

5. **镜像切换**
   - 如需使用官方源，可修改 `gradle.properties` 手动切换
   - 关闭环境时会自动恢复原配置

## 故障排除

### 签名错误
- 检查 `local.properties` 文件路径是否正确
- 确认签名文件 `release.jks` 存在于指定路径
- 验证密码和别名是否正确

### 下载速度慢
- 检查网络连接
- 验证国内镜像是否生效（查看 `gradle.properties`）
- 尝试手动配置其他镜像源

### Android SDK下载失败
- 查看启动日志中的SDK安装步骤
- 检查镜像源是否可访问
- 手动执行 `sdkmanager` 命令测试

### Gradle依赖下载失败
- 确认 `gradle.properties.cnb` 已正确复制
- 检查阿里云镜像服务是否正常
- 尝试临时切换到官方源排查问题

### 环境启动失败
- 查看 `.ide/Dockerfile` 构建日志
- 检查是否有依赖安装错误
- 确认Dockerfile语法正确
- 验证国内镜像配置是否有效

## 相关文档

- [CNB云原生开发介绍](https://docs.cnb.cool/zh/workspaces/intro.html)
- [自定义开发环境](https://docs.cnb.cool/zh/workspaces/custom-dev-env.html)
- [自定义构建流水线](https://docs.cnb.cool/zh/workspaces/custom-dev-pipeline.html)
- [清华大学TUNA镜像](https://mirrors.tuna.tsinghua.edu.cn/)
- [阿里云Maven镜像](https://developer.aliyun.com/mvn/guide)
