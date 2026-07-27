# 推送源码到 GitHub 参考指南

本仓库源码整理在本地目录，已推送到 GitHub。本文档记录推送流程，供后续参考。

## 仓库信息

- **GitHub 远程仓库**：`https://github.com/jonaskilli/tts-mumu.git`
- **本地 Git 仓库根目录**：`TTS_Server_Android-source/TTS_Server_Android/`
  - 注意：仓库根直接是 Android 项目（`settings.gradle`、`gradlew` 等在此目录），不是外层 `TTS_Server_Android-source/`。
- **主分支**：`main`

## 首次推送（已完成）

源码已通过以下方式推送到 `main` 分支：

```bash
cd TTS_Server_Android-source/TTS_Server_Android

# 初始化并提交（首次）
git init
git add -A
git commit -m "Initial commit: TTS Server Android 整理源码"
git branch -M main

# 推送（用带 token 的 URL，避免明文写入远程配置）
git push -u "https://<TOKEN>@github.com/jonaskilli/tts-mumu.git" main
```

## 日常推送流程

后续修改代码后，按常规流程推送即可：

```bash
cd TTS_Server_Android-source/TTS_Server_Android

git add -A
git commit -m "描述本次改动"
git push origin main
```

> 若已配置好 `origin` 远程（见下文），直接 `git push` 即可。

## 关于 GitHub Actions 工作流（重要）

`.github/workflows/` 下的文件（如 `automerge.yml`、`debug.yml`、`release.yml`、`test.yml`）是 CI 配置。

**关键限制**：用 Personal Access Token (PAT) 推送 `.github/workflows/` 下的文件时，token **必须勾选 `workflow` 作用域**，否则 GitHub 会拒绝整个推送（`remote rejected: refusing to allow a Personal Access Token to create or update workflow ... without workflow scope`）。

生成带 `workflow` 权限的 PAT 步骤：
1. GitHub → 头像 → **Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)**
2. **Generate new token (classic)**
3. 勾选：
   - ✅ `repo`（全套仓库权限）
   - ✅ `workflow`（推送 Actions 工作流文件，必选）
4. 生成后复制 `ghp_...`，仅显示一次。

补充 workflow 文件的推送命令：

```bash
git add .github/workflows
git commit -m "ci: 补充 GitHub Actions 工作流"
git push "https://<TOKEN_WITH_WORKFLOW_SCOPE>@github.com/jonaskilli/tts-mumu.git" main
```

## 配置 origin 远程（可选）

如果想用 `git push` 而非每次带 token 的 URL，可配置远程并保存凭证：

```bash
git remote add origin https://github.com/jonaskilli/tts-mumu.git
git config credential.helper 'store --file=.git/credentials'
# 然后执行一次带 token 的推送，凭证会被保存
```

> 或用 SSH：`git remote set-url origin git@github.com:jonaskilli/tts-mumu.git`（需提前配置 SSH key，最安全，无明文 token）。

## 安全提醒

- **不要**把 token 明文写入 `.git/config` 或提交到仓库。
- 用完 token 后建议到 GitHub **Settings → Developer settings → PAT** 将其 **revoke**（撤销）。
- 优先使用 SSH key 或 GitHub CLI (`gh`) 认证，避免 token 泄露。
