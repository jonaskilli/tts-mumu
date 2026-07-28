# TTS Server 转发器 API 使用文档

本文档说明 TTS Server 转发器开启后，支持的三种语音合成请求方式。

---

## 一、通用配置

开启「系统转发器」后，手机会启动一个 HTTP 服务（默认端口 **3331**）。

| 配置项 | 说明 |
|--------|------|
| 服务器地址 | `http://手机IP:3331` |
| 默认端口 | `3331`（可在 App 设置中修改） |
| 支持的请求方式 | `GET`、`POST` |

---

## 二、三种请求方式总览

| 方式 | 路由 | 说明 |
|------|------|------|
| **方式一** | `GET /api/tts` | 通过 URL 查询参数传参，最简单 |
| **方式二** | `POST /api/tts` | 通过 JSON Body 传参，功能与 GET 相同 |
| **方式三** | `POST /v1/audio/speech` | **OpenAI 兼容格式**，支持标准 OpenAI 客户端直接对接 |

---

## 三、方式一：GET /api/tts

通过 URL 查询参数（Query Parameters）传参，适合浏览器直接访问或简单脚本调用。

### 请求格式

```
GET http://手机IP:3331/api/tts?text=要合成的文本&engine=引擎名称&voice=音色&rate=语速&pitch=音调
```

### 参数说明

| 参数 | 必填 | 说明 | 示例 |
|------|------|------|------|
| `text` | ✅ | 要合成的文本 | `你好，世界` |
| `engine` | ✅ | TTS 引擎包名 | `com.google.android.tts` |
| `voice` | ❌ | 音色名称 | `zh-CN-XiaoxiaoNeural` |
| `locale` | ❌ | 语言区域 | `zh-CN` |
| `rate` / `speed` | ❌ | 语速（0~100，默认50） | `50` |
| `pitch` | ❌ | 音调（0~100，默认100） | `100` |

### 示例

#### 示例 1：浏览器直接访问
```
http://192.168.1.100:3331/api/tts?text=今天天气真好&engine=com.google.android.tts&voice=zh-CN-XiaoxiaoNeural&rate=50
```

#### 示例 2：curl 命令
```bash
curl -G "http://192.168.1.100:3331/api/tts" \
  --data-urlencode "text=这是一段测试文本" \
  -d "engine=com.google.android.tts" \
  -d "voice=zh-CN-XiaoxiaoNeural" \
  -d "rate=60" \
  -d "pitch=100" \
  --output test.wav
```

#### 示例 3：Python requests
```python
import requests

url = "http://192.168.1.100:3331/api/tts"
params = {
    "text": "你好，这是TTS测试",
    "engine": "com.google.android.tts",
    "voice": "zh-CN-XiaoxiaoNeural",
    "rate": "50",
    "pitch": "100"
}
response = requests.get(url, params=params)
with open("output.wav", "wb") as f:
    f.write(response.content)
```

---

## 四、方式二：POST /api/tts

通过 JSON Body 传参，适合程序化调用，参数与 GET 方式一一对应。

### 请求格式

```http
POST http://手机IP:3331/api/tts
Content-Type: application/json

{
  "text": "要合成的文本",
  "engine": "引擎名称",
  "locale": "zh-CN",
  "voice": "音色名称",
  "speed": 50,
  "pitch": 100
}
```

### 参数说明

| 参数 | 必填 | 类型 | 默认值 | 说明 |
|------|------|------|--------|------|
| `text` | ✅ | string | - | 要合成的文本 |
| `engine` | ❌ | string | `""` | 引擎包名，空字符串使用默认引擎 |
| `locale` | ❌ | string | `""` | 语言区域 |
| `voice` | ❌ | string | `""` | 音色名称 |
| `speed` | ❌ | int | `50` | 语速（0~100） |
| `pitch` | ❌ | int | `100` | 音调（0~100） |

### 示例

#### 示例 1：curl 命令
```bash
curl -X POST "http://192.168.1.100:3331/api/tts" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "这是一段测试文本",
    "engine": "com.google.android.tts",
    "voice": "zh-CN-XiaoxiaoNeural",
    "speed": 60,
    "pitch": 100
  }' \
  --output test.wav
```

#### 示例 2：Python requests
```python
import requests

url = "http://192.168.1.100:3331/api/tts"
data = {
    "text": "你好，这是POST请求测试",
    "engine": "com.google.android.tts",
    "voice": "zh-CN-XiaoxiaoNeural",
    "speed": 50,
    "pitch": 100
}
response = requests.post(url, json=data)
with open("output.wav", "wb") as f:
    f.write(response.content)
```

#### 示例 3：JavaScript / Fetch
```javascript
fetch("http://192.168.1.100:3331/api/tts", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    text: "你好，这是JavaScript调用",
    engine: "com.google.android.tts",
    voice: "zh-CN-XiaoxiaoNeural",
    speed: 50,
    pitch: 100
  })
})
.then(res => res.blob())
.then(blob => {
  const url = URL.createObjectURL(blob);
  const audio = new Audio(url);
  audio.play();
});
```

---

## 五、方式三：POST /v1/audio/speech（OpenAI 兼容）

**OpenAI 官方语音合成 API 兼容格式**，可直接对接支持 OpenAI TTS 的第三方客户端、阅读软件、AI 工具等。

### 请求格式

```http
POST http://手机IP:3331/v1/audio/speech
Content-Type: application/json
Authorization: Bearer 任意值（当前不校验）

{
  "model": "引擎名称",
  "input": "要合成的文本",
  "voice": "音色名称",
  "response_format": "mp3",
  "speed": 1.0
}
```

### 参数说明

| 参数 | 必填 | 类型 | 默认值 | 说明 |
|------|------|------|--------|------|
| `input` | ✅ | string | - | 要合成的文本（对应 OpenAI 的 `input`） |
| `model` | ❌ | string | `""` | TTS 引擎包名（对应 OpenAI 的 `model`） |
| `voice` | ❌ | string | `""` | 音色名称（对应 OpenAI 的 `voice`） |
| `response_format` | ❌ | string | `"mp3"` | 响应格式：`mp3`/`opus`/`aac`/`flac`/`wav`/`pcm` |
| `speed` | ❌ | float | `1.0` | 语速（0.25 ~ 4.0） |

> **注意：** 当前音频输出实际格式仍为 WAV，但 HTTP `Content-Type` 会根据 `response_format` 做对应设置，以兼容客户端格式校验。

### 示例

#### 示例 1：curl 命令（OpenAI 格式）
```bash
curl -X POST "http://192.168.1.100:3331/v1/audio/speech" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer sk-any-key" \
  -d '{
    "model": "com.google.android.tts",
    "input": "你好，这是OpenAI兼容接口测试",
    "voice": "zh-CN-XiaoxiaoNeural",
    "response_format": "mp3",
    "speed": 1.0
  }' \
  --output speech.mp3
```

#### 示例 2：Python（OpenAI SDK 方式）
```python
from openai import OpenAI

client = OpenAI(
    api_key="sk-anything",  # 当前不校验，随便填
    base_url="http://192.168.1.100:3331"  # 注意：没有 /v1，SDK 会自动加
)

response = client.audio.speech.create(
    model="com.google.android.tts",  # 填引擎名称
    voice="zh-CN-XiaoxiaoNeural",    # 填音色名称
    input="你好，这是通过OpenAI SDK调用的语音合成",
    speed=1.0,
    response_format="mp3"
)

response.stream_to_file("output.mp3")
```

#### 示例 3：Python requests（原生）
```python
import requests

url = "http://192.168.1.100:3331/v1/audio/speech"
headers = {
    "Content-Type": "application/json",
    "Authorization": "Bearer sk-test-key"
}
data = {
    "model": "com.google.android.tts",
    "input": "你好，这是OpenAI兼容接口",
    "voice": "zh-CN-XiaoxiaoNeural",
    "response_format": "mp3",
    "speed": 1.0
}

response = requests.post(url, headers=headers, json=data)
with open("speech.mp3", "wb") as f:
    f.write(response.content)
```

#### 示例 4：在阅读软件中配置（如 Legado / 阅读）

| 配置项 | 填写内容 |
|--------|---------|
| TTS 接口地址 | `http://192.168.1.100:3331/v1/audio/speech` |
| 模型 (Model) | `com.google.android.tts` |
| 音色 (Voice) | `zh-CN-XiaoxiaoNeural` |
| API Key | `sk-anything`（随便填） |
| 请求格式 | `JSON` / `OpenAI` |

---

## 六、常见问题

### Q1：如何查看可用的引擎和音色？

访问以下接口：

```
GET http://手机IP:3331/api/engines      # 查看所有可用引擎
GET http://手机IP:3331/api/voices?engine=com.google.android.tts  # 查看某引擎的音色
```

### Q2：OpenAI 接口的 `model` 填什么？

`model` 对应的是 **Android TTS 引擎的包名**，不是真正的 AI 模型。常见引擎：

| 引擎 | 包名 |
|------|------|
| Google TTS | `com.google.android.tts` |
| 讯飞语记 | `com.iflytek.speechcloud` |
| 百度语音 | `com.baidu.duersdk.opensdk` |
| 系统默认 | 留空或 `""` |

### Q3：返回的音频格式到底是什么？

当前底层 Android TTS 合成的音频为 **WAV 格式**。`response_format` 仅影响 HTTP 响应头中的 `Content-Type`，实际音频编码仍为 WAV。如需严格 MP3，需额外配置音频转码。

### Q4：密钥/API Key 怎么填？

当前接口 **不做鉴权校验**，`Authorization` 头可以随便填，例如：
```
Authorization: Bearer sk-123456
```

---

## 七、快速对照表

| 需求场景 | 推荐方式 | 路由 |
|----------|----------|------|
| 浏览器/简单脚本 | GET | `/api/tts` |
| 程序化调用（自定义客户端） | POST JSON | `/api/tts` |
| 对接 OpenAI 兼容客户端 | POST OpenAI | `/v1/audio/speech` |
| 阅读软件（Legado/阅读等） | POST OpenAI | `/v1/audio/speech` |
