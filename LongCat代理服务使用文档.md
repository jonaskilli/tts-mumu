# LongCat API 中转服务使用文档

> 将美团 LongCat（长猫助手）私有 API 包装为 OpenAI 兼容格式的中转服务。
> 每次请求自动新建会话，兼容 `OpenAI SDK`、`NextChat`、`LobeChat` 等客户端。

---

## 目录

1. [服务概述](#1-服务概述)
2. [启动与停止](#2-启动与停止)
3. [API 端点总览](#3-api-端点总览)
4. [核心接口：聊天补全（POST /v1/chat/completions）](#4-核心接口聊天补全-post-v1chatcompletions)
5. [手动创建会话（POST /v1/longcat/session）](#5-手动创建会话-post-v1longcatsession)
6. [更新认证信息（POST /v1/longcat/config）](#6-更新认证信息-post-v1longcatconfig)
7. [查询模型列表（GET /v1/models）](#7-查询模型列表-get-v1models)
8. [健康检查（GET /health）](#8-健康检查-get-health)
9. [响应格式说明](#9-响应格式说明)
10. [身份认证详解](#10-身份认证详解)
11. [常见问题与排查](#11-常见问题与排查)

---

## 1. 服务概述

### 1.1 作用

将 LongCat 的私有 API 包装成标准的 **OpenAI 兼容 API**，这样任何支持 OpenAI API 格式的客户端（如 NextChat、LobeChat、OpenAI Python SDK、curl 等）都可以直接接入使用。

### 1.2 核心特性

| 特性 | 说明 |
|------|------|
| **每次请求自动新建会话** | 每次调用 `/v1/chat/completions` 时自动创建新会话，无需手动管理 conversationId |
| **OpenAI 兼容格式** | 请求/响应格式完全兼容 OpenAI API |
| **支持流式输出** | 支持 `stream: true` 参数，使用 SSE 格式 |
| **支持 system prompt** | 支持 system 角色，用于设置人设/角色 |
| **支持搜索增强** | 使用 `model: "longcat-search"` 可启用搜索能力 |
| **支持动态更新认证** | 认证过期后可通过 API 在线更新，无需重启服务 |

### 1.3 前提条件

- 有效的 LongCat 登录凭证（Cookie 中的 `passport_token_key`）
- 有效的 mtgsig 签名

---

## 2. 启动与停止

### 2.1 启动服务

```bash
nohup python3 /storage/emulated/0/0000/longcat_proxy.py 9092 > /tmp/longcat_proxy.log 2>&1 &
```

- `9092` 为端口号，可修改为任意未占用端口
- 日志输出到 `/tmp/longcat_proxy.log`

### 2.2 停止服务

```bash
kill $(ps aux | grep longcat_proxy | grep -v grep | awk '{print $2}')
```

### 2.3 查看日志

```bash
tail -f /tmp/longcat_proxy.log
```

### 2.4 检查服务是否运行

```bash
ps aux | grep longcat_proxy | grep -v grep
```

---

## 3. API 端点总览

| 方法 | 端点 | 功能 |
|------|------|------|
| `GET` | `/health` | 健康检查 |
| `GET` | `/v1/models` | 获取可用模型列表 |
| `POST` | `/v1/chat/completions` | **核心接口**：发送聊天消息（自动新建会话） |
| `POST` | `/v1/longcat/session` | 手动创建新会话（调试用） |
| `POST` | `/v1/longcat/config` | 更新认证信息（mtgsig、Cookie 等） |

---

## 4. 核心接口：聊天补全（POST /v1/chat/completions）

### 4.1 请求格式

**请求地址：** `http://127.0.0.1:9092/v1/chat/completions`

**请求头：**
```
Content-Type: application/json
```

**请求体（JSON）：**

```json
{
  "model": "longcat",
  "messages": [
    {"role": "system", "content": "你是一只可爱的猫娘，说话要喵~结尾"},
    {"role": "user", "content": "你好"}
  ],
  "stream": false
}
```

### 4.2 参数说明

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `model` | string | 否 | `"longcat"` | 模型名称。`"longcat"`（普通）、`"longcat-search"`（搜索增强） |
| `messages` | array | **是** | - | 消息列表，兼容 OpenAI 格式 |
| `stream` | boolean | 否 | `false` | 是否启用流式输出 |

**messages 数组说明：**

支持三种角色：
- `system`：系统设定/人设（如"你是一只猫娘"）
- `user`：用户消息
- `assistant`：AI 历史回复（用于多轮对话时携带上下文，但当前每次请求都新建会话，assistant 消息实际不会生效）

### 4.3 示例请求（非流式）

```bash
curl -s -X POST http://127.0.0.1:9092/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "longcat",
    "messages": [
      {"role": "system", "content": "你是一只可爱的猫娘，说话要喵~结尾"},
      {"role": "user", "content": "今天天气怎么样？"}
    ],
    "stream": false
  }'
```

### 4.4 示例请求（流式）

```bash
curl -s -X POST http://127.0.0.1:9092/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "longcat",
    "messages": [
      {"role": "user", "content": "讲个笑话"}
    ],
    "stream": true
  }'
```

### 4.5 非流式响应格式

```json
{
  "id": "chatcmpl-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "object": "chat.completion",
  "created": 1234567890,
  "model": "longcat",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "今天天气不错喵~ 适合出去晒太阳呢！"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 0,
    "completion_tokens": 0,
    "total_tokens": 0
  }
}
```

### 4.6 流式响应格式

标准的 SSE（Server-Sent Events）格式，每个事件以 `data: ` 开头，以 `\n\n` 结尾：

```
data: {"id":"chatcmpl-xxx","object":"chat.completion.chunk","created":1234567890,"model":"longcat","choices":[{"index":0,"delta":{"content":"今天"},"finish_reason":null}]}

data: {"id":"chatcmpl-xxx","object":"chat.completion.chunk","created":1234567890,"model":"longcat","choices":[{"index":0,"delta":{"content":"天气"},"finish_reason":null}]}

data: {"id":"chatcmpl-xxx","object":"chat.completion.chunk","created":1234567890,"model":"longcat","choices":[{"index":0,"delta":{"content":"不错"},"finish_reason":null}]}

data: {"id":"chatcmpl-xxx","object":"chat.completion.chunk","created":1234567890,"model":"longcat","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

data: [DONE]
```

### 4.7 使用搜索增强

将 `model` 设置为 `"longcat-search"` 即可启用搜索能力：

```json
{
  "model": "longcat-search",
  "messages": [
    {"role": "user", "content": "今天的热点新闻是什么？"}
  ]
}
```

---

## 5. 手动创建会话（POST /v1/longcat/session）

正常情况下不需要手动调用此接口，因为 `/v1/chat/completions` 会自动新建会话。此接口主要用于调试和验证。

### 5.1 请求格式

```json
{
  "model": "",
  "agentId": "1"
}
```

### 5.2 参数说明

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `model` | string | 否 | `""` | 模型名称，传空字符串使用默认模型 |
| `agentId` | string | 否 | `"1"` | 智能体 ID，`"1"` 为默认智能体 |

### 5.3 示例

```bash
curl -s -X POST http://127.0.0.1:9092/v1/longcat/session \
  -H "Content-Type: application/json" \
  -d '{"model":"","agentId":"1"}'
```

### 5.4 响应格式

```json
{
  "conversation_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "session": {
    "conversationId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "model": "LongCat",
    "agent": "1",
    "title": "新对话",
    "titleType": "SYSTEM",
    "currentMessageId": 0,
    "capabilities": ["search", "think", "file", "voiceCall"],
    "label": "今天",
    "createAt": 1234567890123,
    "updateAt": 1234567890123
  },
  "model": "LongCat",
  "title": "新对话"
}
```

---

## 6. 更新认证信息（POST /v1/longcat/config）

### 6.1 使用场景

当 mtgsig 签名或 Cookie 过期（返回 401/403 错误）时，需要更新认证信息。需要重新从 LongCat 客户端抓包获取最新的请求头数据。

### 6.2 请求格式

```json
{
  "mtgsig": "{\"a1\":\"1.2\",\"a2\":1784356859522,...}",
  "m_traceid": "6226950002705196576",
  "cookies": "com.sankuai.friday.longcat.platform_strategy=; passport_token_key=...; ...",
  "agent_id": "1",
  "base_url": "https://longcat.chat/api/v1/chat-completion-V2?yodaReady=h5&csecplatform=4&csecversion=4.2.4",
  "default_conv_id": "429d5e50-3c06-48a5-a873-d27aa91ba240"
}
```

### 6.3 参数说明

所有字段均为可选，只需传入需要更新的字段即可。

| 参数 | 类型 | 说明 |
|------|------|------|
| `mtgsig` | string | JSON 字符串格式的 mtgsig 签名，**必须转义为字符串** |
| `m_traceid` | string | 请求追踪 ID |
| `cookies` | string | Cookie 字符串，包含 `passport_token_key` 等 |
| `agent_id` | string | 智能体 ID |
| `base_url` | string | LongCat API 地址（通常无需修改） |
| `default_conv_id` | string | 默认会话 ID（当前版本未使用，保留兼容） |

### 6.4 示例

```bash
curl -s -X POST http://127.0.0.1:9092/v1/longcat/config \
  -H "Content-Type: application/json" \
  -d '{
    "mtgsig": "{\"a1\":\"1.2\",\"a2\":1784356859522,\"a3\":\"...\"}",
    "m_traceid": "6226950002705196576",
    "cookies": "passport_token_key=...; ..."
  }'
```

### 6.5 响应格式

```json
{
  "status": "ok",
  "config_keys": ["base_url", "mtgsig", "m_traceid", "cookies", ...]
}
```

---

## 7. 查询模型列表（GET /v1/models）

兼容 OpenAI 格式，用于客户端自动发现可用模型。

### 7.1 示例

```bash
curl -s http://127.0.0.1:9092/v1/models
```

### 7.2 响应格式

```json
{
  "object": "list",
  "data": [
    {"id": "longcat", "object": "model", "owned_by": "longcat", "created": 0},
    {"id": "longcat-search", "object": "model", "owned_by": "longcat", "created": 0}
  ]
}
```

---

## 8. 健康检查（GET /health）

### 8.1 示例

```bash
curl -s http://127.0.0.1:9092/health
```

### 8.2 响应格式

```json
{
  "status": "ok",
  "service": "longcat-proxy"
}
```

---

## 9. 响应格式说明

### 9.1 正常响应

所有正常响应均遵循 OpenAI 兼容格式，详见 [4.5 非流式响应](#45-非流式响应) 和 [4.6 流式响应](#46-流式响应)。

### 9.2 错误响应

| HTTP 状态码 | 含义 | 常见原因 |
|-------------|------|----------|
| `400` | 请求参数错误 | 缺少 `messages` 字段，或消息内容为空 |
| `502` | 上游 API 错误 | LongCat 服务器不可用、认证过期、网络问题 |

错误响应体示例：

```json
{
  "error": "messages is required"
}
```

```json
{
  "error": "create session failed: {'status': 401, 'body': '...'}"
}
```

```json
{
  "error": "LongCat API error: {'status': 500, 'body': '...'}"
}
```

---

## 10. 身份认证详解

### 10.1 必需的认证信息

在与 LongCat 上游 API 通信时，需要以下三个关键认证要素：

| 要素 | 说明 | 来源 |
|------|------|------|
| **mtgsig** | 美团系的签名算法，基于时间戳、设备信息等生成的加密签名 | 从 LongCat 客户端抓包获取 |
| **m-traceid** | 请求追踪 ID，一个数字字符串 | 从 LongCat 客户端抓包获取 |
| **Cookie** | 包含 `passport_token_key` 等登录凭证 | 从 LongCat 客户端抓包获取 |

### 10.2 如何获取认证信息

需要使用抓包工具（如 Reqable、Charles、Fiddler）抓取 LongCat 客户端的 API 请求，从中提取以下请求头：

```
mtgsig: {"a1":"1.2","a2":1784356859522,...}
m-traceid: 6226950002705196576
Cookie: com.sankuai.friday.longcat.platform_strategy=; passport_token_key=...; ...
```

**推荐抓包目标：** 抓取 `chat-completion-V2` 或 `session-create` 请求的请求头。

### 10.3 认证有效期

- mtgsig 签名有时效性，过期后需重新抓包获取
- Cookie 中的 `passport_token_key` 登录令牌也有有效期
- 两者过期后，API 会返回 401 或 403 错误，此时需要通过 `POST /v1/longcat/config` 更新

### 10.4 更新认证信息的步骤

1. 使用抓包工具重新抓取 LongCat 客户端的最新请求
2. 从请求头中提取 `mtgsig`、`m-traceid`、`Cookie`
3. 调用 `POST /v1/longcat/config` 更新这些值
4. 验证：再次发送聊天请求，确认正常返回

---

## 11. 常见问题与排查

### 11.1 返回 502 错误

**原因：** 上游 LongCat API 调用失败。
**排查步骤：**
1. 检查认证信息是否过期（查看日志中的具体错误）
2. 调用 `POST /v1/longcat/config` 更新 mtgsig 和 Cookie
3. 检查网络连接是否正常

### 11.2 返回空内容或部分内容

**原因：** LongCat 响应解析异常。
**排查步骤：**
1. 查看 `/tmp/longcat_proxy.log` 中的原始响应
2. 检查 `_parse_sse_domestic` 函数是否正确解析

### 11.3 服务启动失败

**原因：** 端口被占用或 Python 依赖缺失。
**排查步骤：**
```bash
# 检查端口占用
lsof -i :9092

# 检查 Python 依赖
pip list | grep -E "fastapi|uvicorn"
```

### 11.4 多轮对话问题

**注意：** 当前版本**每次请求都会新建会话**，这意味着：
- 每次请求都是全新的对话
- AI 无法记住之前对话的上下文
- 如果需要多轮对话，需要在请求中携带所有历史消息（通过 `messages` 数组）

### 11.5 如何验证服务正常

```bash
# 1. 健康检查
curl -s http://127.0.0.1:9092/health

# 2. 模型列表
curl -s http://127.0.0.1:9092/v1/models

# 3. 发送聊天请求
curl -s -X POST http://127.0.0.1:9092/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model":"longcat","messages":[{"role":"user","content":"你好"}]}'
```

---

## 附录：代码文件说明

| 文件 | 路径 | 说明 |
|------|------|------|
| 主程序 | `/storage/emulated/0/0000/longcat_proxy.py` | 服务代码 |
| 日志文件 | `/tmp/longcat_proxy.log` | 运行日志 |

---

*最后更新：2026-07-18*
*如有问题，请提供抓包数据以便排查。*