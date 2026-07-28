let req = {}
var callback = null
let ws = null
// 状态管理
let audioState = {
    chunks: [],      // 存放所有音频片段
    totalLength: 0,  // 总字节数
    isFinished: false
}

// 缓存的音色列表
let voicesCache = []
let localesCache = ['zh-CN']

// 手动配置的克隆音色列表
let manualCloneVoices = []

// UI 编辑器引用
let cloneVoiceEditor = null
let cloneAudioTextEditor = null
let cloneAudioUrlEditor = null

function getWsUrl() {
    let url = ttsrv.userVars['wsUrl']
    if (!url || url.trim() === "") {
        throw "未设置变量 WebSocket URL"
    }
    return url.trim()
}

function check() {
    getWsUrl()
}

function generateId() {
    const chars = 'abcdef0123456789'
    let result = ''
    for (let i = 0; i < 32; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    return result
}

function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

// 判断是否是克隆音色
function isCloneVoice(voiceId) {
    return voiceId && voiceId.startsWith('create_voice_');
}

// 解析手动配置的克隆音色
// 格式: 代码@名称@audio_text@audio_url;...
// 例如: create_voice_123@元宝@参考音频文本@http://xxx.wav
function parseManualCloneVoices() {
    let config = ttsrv.userVars['manualCloneVoices'];
    if (!config || config.trim() === "") {
        return [];
    }
    
    let voices = [];
    let items = config.split(';');
    
    for (let item of items) {
        item = item.trim();
        if (!item) continue;
        
        let parts = item.split('@');
        if (parts.length >= 2) {
            let voiceId = parts[0].trim();
            let voiceName = parts[1].trim();
            let audioText = parts.length >= 3 ? parts[2].trim() : "";
            let audioUrl = parts.length >= 4 ? parts[3].trim() : "";
            
            if (voiceId && voiceName && isCloneVoice(voiceId)) {
                voices.push({
                    voice_id: voiceId,
                    voice_name: voiceName,
                    gender: "female",
                    is_cloned: true,
                    audio_text: audioText,
                    audio_url: audioUrl
                });
                logger.i("[手动配置] 添加克隆音色: " + voiceId + " -> " + voiceName);
            } else if (!isCloneVoice(voiceId)) {
                logger.w("[手动配置] 跳过无效的克隆音色ID: " + voiceId);
            }
        }
    }
    
    logger.i("[手动配置] 共解析 " + voices.length + " 个克隆音色");
    return voices;
}

// 从 wsUrl 中提取所有参数
function extractWsParams(wsUrl) {
    logger.i("[提取参数] 开始从 wsUrl 提取参数");
    logger.i("[提取参数] wsUrl 长度: " + wsUrl.length);
    
    let result = {};
    
    // 提取 ut
    let utMatch = wsUrl.match(/[?&]ut=([^&]+)/);
    if (utMatch) {
        result.ut = decodeURIComponent(utMatch[1]);
        logger.i("[提取参数] ut: " + result.ut.substring(0, 30) + "...");
    } else {
        logger.e("[提取参数] 未找到 ut 参数!");
    }
    
    // 提取 pr
    let prMatch = wsUrl.match(/[?&]pr=([^&]+)/);
    if (prMatch) {
        result.pr = prMatch[1];
        logger.i("[提取参数] pr: " + result.pr);
    }
    
    // 提取 fr
    let frMatch = wsUrl.match(/[?&]fr=([^&]+)/);
    if (frMatch) {
        result.fr = frMatch[1];
        logger.i("[提取参数] fr: " + result.fr);
    }
    
    // 提取 sign (用于 WebSocket，不是 API sign)
    let signMatch = wsUrl.match(/[?&]sign=([^&]+)/);
    if (signMatch) {
        result.wsSign = decodeURIComponent(signMatch[1]);
        logger.i("[提取参数] wsSign: " + result.wsSign.substring(0, 50) + "...");
    }
    
    logger.i("[提取参数] 参数提取完成");
    return result;
}

// --- 核心工具：Java流物理写入 ---

function writeString(stream, str) {
    for (let i = 0; i < str.length; i++) {
        stream.write(str.charCodeAt(i));
    }
}

function writeInt(stream, val) {
    stream.write(val & 0xFF);
    stream.write((val >> 8) & 0xFF);
    stream.write((val >> 16) & 0xFF);
    stream.write((val >> 24) & 0xFF);
}

function writeShort(stream, val) {
    stream.write(val & 0xFF);
    stream.write((val >> 8) & 0xFF);
}

function writeWavHeaderToStream(stream, dataLength) {
    let sampleRate = 24000;
    let channels = 1;
    let bitsPerSample = 16;
    let byteRate = sampleRate * channels * bitsPerSample / 8;
    let blockAlign = channels * bitsPerSample / 8;

    writeString(stream, 'RIFF');
    writeInt(stream, 36 + dataLength);
    writeString(stream, 'WAVE');
    writeString(stream, 'fmt ');
    writeInt(stream, 16); 
    writeShort(stream, 1); 
    writeShort(stream, channels);
    writeInt(stream, sampleRate);
    writeInt(stream, byteRate);
    writeShort(stream, blockAlign);
    writeShort(stream, bitsPerSample);
    writeString(stream, 'data');
    writeInt(stream, dataLength);
}

let PluginJS = {
    "name": "通义千问-支持克隆音色",
    "id": "qianwen.with.clone",
    "author": "TTS Server",
    "version": 4,
    'iconUrl': 'https://img.alicdn.com/imgextra/i1/O1CN01L9yG8u1oO6iA7Jz9e_!!6000000005214-55-tps-83-82.svg',
    'vars': {
        wsUrl: {
            label: "WebSocket URL (必须)",
            hint: "抓包获取的 wss://speech-tts.qianwen.com/api/v2/tts?... 完整URL"
        },
        manualCloneVoices: {
            label: "批量添加克隆音色",
            hint: "格式: 代码@名称@audio_text@audio_url 例如: create_voice_123@元宝@参考音频文本@http://xxx.wav。多个用分号分隔。"
        }
    },

    "isNeedDecode": function(locale, voice) {
        return true; 
    },

    "onStop": function () {
        if (ws != null) {
            try { ws.close(1000, "stop"); } catch(e) {}
            ws = null
        }
    },

    "getAudioV2": function (request, callback2) {
        check()

        let speed = 0.5 + (request.rate / 100) * 1.5
        let volume = request.volume / 50
        callback = callback2
        
        audioState = {
            chunks: [],
            totalLength: 0,
            isFinished: false
        }
        
        // ID 自动纠错和映射
        let rawId = String(request.voice || "").trim()
        let finalId = rawId;
        let voiceInfo = null;
        
        // 处理自定义克隆音色选项
        if (rawId === "__custom_clone__") {
            let customId = ttsrv.tts.data['customCloneVoiceId'];
            if (customId && isCloneVoice(customId)) {
                finalId = customId;
                logger.i("使用自定义克隆音色: " + finalId);
            } else {
                logger.e("未设置有效的克隆音色ID，请在UI中输入");
                callback2.error("请在下方输入克隆音色ID");
                return;
            }
        }
        
        // 查找音色信息
        let allVoices = getAllVoices();
        for (let v of allVoices) {
            if (v.voice_id === rawId) {
                voiceInfo = v;
                break;
            }
        }
        
        // 预设音色映射
        if (rawId === "" || rawId.indexOf("unknown1") > -1 || rawId === "longqiang") {
            finalId = "longqiang"
        } else if (rawId.indexOf("unknown2") > -1 || rawId === "longyan") {
            finalId = "longyan"
        } else if (rawId.indexOf("unknown_muyang") > -1) {
            finalId = "zh_female_quarkF531S0_ptts"
        } else if (isCloneVoice(rawId)) {
            // 克隆音色直接使用
            finalId = rawId;
        }
        
        // 重新查找音色信息（如果是自定义克隆音色，构建 voiceInfo）
        if (finalId === ttsrv.tts.data['customCloneVoiceId']) {
            voiceInfo = {
                voice_id: finalId,
                voice_name: "我的克隆音色",
                gender: "female",
                is_cloned: true,
                audio_text: ttsrv.tts.data['customCloneAudioText'] || "",
                audio_url: ttsrv.tts.data['customCloneAudioUrl'] || ""
            };
        } else {
            for (let v of allVoices) {
                if (v.voice_id === finalId) {
                    voiceInfo = v;
                    break;
                }
            }
        }

        req = {
            text: request.text,
            voice: finalId,
            voiceInfo: voiceInfo,
            speed: speed,
            volume: volume,
            reqid: generateId()
        }
        
        logger.i("使用音色: " + finalId + (voiceInfo && voiceInfo.is_cloned ? " [克隆音色]" : " [预设音色]"));
        
        if (ws != null) {
            try { ws.close(1000, "restart"); } catch(e) {}
            ws = null
        }

        getAudio()
    }
}

function getAudio() {
    if (ws == null) {
        let wsUrl = getWsUrl()
        wsUrl = String(wsUrl)
        
        if (!wsUrl.startsWith("wss://")) {
            callback.error("URL 格式错误")
            return
        }
        
        let encodedUrl = wsUrl.replace(/\*/g, '%2A').replace(/\$/g, '%24')
        let headers = {
            "Origin": "https://tongyi.aliyun.com",
            "User-Agent": "Mozilla/5.0 (Linux; Android 16; PJX110 Build/UKQ1.231108.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/123.0.6312.80 Mobile Safari/537.36 AliApp(tongyi/6.1.5.2780) TTID/36335934394984@TongYi_Android_6.1.5.2780"
        }
        
        try {
            ws = new Websocket(encodedUrl, headers)
        } catch (e) {
            callback.error("WebSocket 创建失败: " + e.message)
            return
        }

        const flushAllAudio = function(reason) {
            if (audioState.isFinished) return;
            audioState.isFinished = true;

            try {
                if (audioState.chunks.length > 0) {
                    
                    // 前后各 0.5秒静音
                    let silenceDurationMs = 500;
                    let silenceBytes = 24000 * 2 * (silenceDurationMs / 1000);
                    let finalTotalLength = silenceBytes + audioState.totalLength + silenceBytes;

                    logger.i("生成 WAV (前后各" + silenceDurationMs + "ms): 总长 " + finalTotalLength + " bytes");

                    // 1. 创建流
                    let stream = new java.io.ByteArrayOutputStream();
                    
                    // 2. 写入 WAV 头
                    writeWavHeaderToStream(stream, finalTotalLength);
                    
                    // 3. 写入头部静音
                    for (let i = 0; i < silenceBytes; i++) {
                        stream.write(0);
                    }

                    // 4. 写入真实音频
                    for (let chunk of audioState.chunks) {
                        stream.write(chunk);
                    }
                    
                    // 5. 写入尾部静音
                    for (let i = 0; i < silenceBytes; i++) {
                        stream.write(0);
                    }
                    
                    // 6. 发送
                    let finalBytes = stream.toByteArray();
                    callback.write(finalBytes);
                    stream.close();
                    
                    try { java.lang.Thread.sleep(200); } catch(e) {}
                    
                } else {
                    logger.w("无音频数据");
                }
            } catch (e) {
                logger.e("写入异常: " + e);
            } finally {
                callback.close();
                ws = null;
            }
        }

        ws.on('close', function (code, reason) {
            flushAllAudio("Close:" + code);
        })

        ws.on('error', function (err, resp) {
            flushAllAudio("Error");
        })

        ws.on('text', function (msg) {
            try {
                let data = JSON.parse(msg)
                
                if (data.code == 2000000 && data.data) {
                    if (ws && ws._lastDataTimeRef) ws._lastDataTimeRef.value = Date.now()
                    
                    if (data.data.audio) {
                        let audioBytes = ttsrv.base64DecodeToBytes(data.data.audio)
                        if (audioBytes) {
                            audioState.chunks.push(audioBytes);
                            audioState.totalLength += audioBytes.length;
                        }
                    }
                    
                    if (data.data.status == 2 || data.data.status == "2") {
                        ws.close(1000, "done");
                        flushAllAudio("Done");
                    }
                } else if (data.code && data.code != 2000000) {
                    logger.e("服务器错误: " + data.message);
                    flushAllAudio("Rescue");
                }
            } catch (e) {
                logger.e("解析异常: " + e);
            }
        })

        ws.on('open', function () {
            sendMessage()
            
            let lastDataTime = { value: Date.now() }
            let stopCheck = { value: false }
            let checkRunnable = new java.lang.Runnable({
                run: function() {
                    try {
                        java.lang.Thread.sleep(1500)
                        while (!stopCheck.value && ws != null) {
                            java.lang.Thread.sleep(500)
                            if (ws == null || stopCheck.value) break
                            let elapsed = Date.now() - lastDataTime.value
                            if (elapsed > 30000) {
                                stopCheck.value = true;
                                if (ws != null) try { ws.close(1000, "timeout"); } catch(e) {}
                                flushAllAudio("Timeout");
                                break
                            }
                        }
                    } catch (e) {}
                }
            })
            let checkThread = new java.lang.Thread(checkRunnable)
            checkThread.start()
            ws._lastDataTimeRef = lastDataTime
            ws._stopCheck = stopCheck
        })
        return
    }
    if (ws.readyState == Websocket.OPEN) {
        sendMessage()
    } else {
        ws = null
        return getAudio()
    }
}

function sendMessage() {
    let chatReqId = ""
    try {
        let wsUrl = getWsUrl()
        let match = wsUrl.match(/[?&]hid=([^&]+)/)
        if (match) chatReqId = match[1]
        else chatReqId = generateUUID()
    } catch(e) {}

    // 判断是否是克隆音色
    let isClone = isCloneVoice(req.voice);
    
    // 根据音色类型选择 model
    let targetModel = "QUARK_VOICE";
    if (req.voice === "longqiang" || req.voice === "longyan") {
        targetModel = "QWEN_NLS";
    }
    // 克隆音色使用 QUARK_VOICE

    // 构建消息1
    let msg1 = {
        reqid: req.reqid,
        text: req.text,
        model: targetModel,
        vcn: req.voice,
        type: "stream",
        speed: parseFloat(req.speed),
        volume: parseFloat(req.volume),
        format: "pcm",
        status: 1,
        sample_rate: 24000,
        extra_params: {chat_req_id: chatReqId}
    };
    
    // 克隆音色需要添加 audio_text 和 audio_url
    if (isClone && req.voiceInfo) {
        if (req.voiceInfo.audio_text) {
            msg1.audio_text = req.voiceInfo.audio_text;
        }
        if (req.voiceInfo.audio_url) {
            msg1.audio_url = req.voiceInfo.audio_url;
        }
        logger.i("克隆音色参数: audio_text=" + (req.voiceInfo.audio_text ? "有" : "无") + 
                 ", audio_url=" + (req.voiceInfo.audio_url ? "有" : "无"));
    }

    // 消息2 - 结束标记（克隆音色使用默认音色作为结束标记）
    let msg2 = {
        reqid: req.reqid,
        text: "",
        model: "QUARK_VOICE",
        vcn: isClone ? "zh_female_quarkF531S0_ptts" : req.voice,
        type: "stream",
        speed: parseFloat(req.speed),
        volume: parseFloat(req.volume),
        format: "pcm",
        status: 2,
        sample_rate: 24000,
        extra_params: {}
    }
    
    // 克隆音色的 msg2 也需要 audio_text 和 audio_url 字段（即使是空字符串）
    if (isClone) {
        msg2.audio_text = "";
        msg2.audio_url = "";
    }
    
    try {
        logger.i("发送消息1: " + JSON.stringify(msg1, null, 2));
        logger.i("发送消息2: " + JSON.stringify(msg2, null, 2));
        ws.send(JSON.stringify(msg1))
        
        let t = new java.lang.Thread(new java.lang.Runnable({
            run: function() {
                try {
                     java.lang.Thread.sleep(50) 
                     if (ws) ws.send(JSON.stringify(msg2))
                } catch(e) {}
            }
        }))
        t.start()
    } catch(e) {
        callback.error("发送异常: " + e)
    }
}

// 预设音色列表
const PRESET_VOICES = [
    {voice_id: "zh_female_quarkF531S0_ptts", voice_name: "沐阳", gender: "female"},
    {voice_id: "zh_female_quark_lulu", voice_name: "若初", gender: "female"},
    {voice_id: "zh_female_quark_ajiao", voice_name: "苏荷姐姐", gender: "female"},
    {voice_id: "zh_female_quark_luoying", voice_name: "元气草莓", gender: "female"},
    {voice_id: "zh_female_quark_jiabei", voice_name: "活力嘉蓓", gender: "female"},
    {voice_id: "zh_female_quark_xinshen", voice_name: "起司妹妹", gender: "female"},
    {voice_id: "zh_female_quark_xiaoning", voice_name: "电台华姐", gender: "female"},
    {voice_id: "zh_female_quark_f29", voice_name: "彩虹甜豆", gender: "female"},
    {voice_id: "zh_female_quark_xiaoxiao", voice_name: "念念", gender: "female"},
    {voice_id: "zh_female_quark_zheque", voice_name: "方晴师姐", gender: "female"},
    {voice_id: "longqiang", voice_name: "浅吻雾梨", gender: "female"},
    {voice_id: "longyan", voice_name: "午夜甜茶", gender: "female"},
    {voice_id: "zh_male_quark_bb01", voice_name: "皓东", gender: "male"},
    {voice_id: "zh_male_quark_m24", voice_name: "温屿哥哥", gender: "male"},
    {voice_id: "zh_male_chengfeng_ICL", voice_name: "阿辉", gender: "male"},
    {voice_id: "__custom_clone__", voice_name: "➕ 添加克隆音色", gender: "female", is_custom: true}
];

// 合并所有音色（预设 + 手动克隆 + 自定义）
function getAllVoices() {
    // 获取用户自定义的克隆音色
    let customCloneId = ttsrv.tts.data['customCloneVoiceId'];
    let customVoices = [];
    
    if (customCloneId && isCloneVoice(customCloneId)) {
        customVoices.push({
            voice_id: customCloneId,
            voice_name: "🎤 我的克隆音色",
            gender: "female",
            is_cloned: true,
            audio_text: ttsrv.tts.data['customCloneAudioText'] || "",
            audio_url: ttsrv.tts.data['customCloneAudioUrl'] || ""
        });
    }
    
    // 合并所有克隆音色并去重
    let allClonedVoices = customVoices.concat(manualCloneVoices);
    let seen = new Set();
    let uniqueClonedVoices = [];
    for (let v of allClonedVoices) {
        if (!seen.has(v.voice_id)) {
            seen.add(v.voice_id);
            uniqueClonedVoices.push(v);
        }
    }
    
    // 合并所有音色
    return PRESET_VOICES.concat(uniqueClonedVoices);
}

// ==================== EditorJS ====================
let EditorJS = {
    "getAudioSampleRate": function (locale, voice) { return 24000 },
    "getAudioFormat": function (locale, voice) { return "wav" },
    "getLocales": function () { return localesCache },
    "getVoices": function (locale) {
        let mm = {}
        let allVoices = getAllVoices();
        logger.i("[getVoices] 返回 " + allVoices.length + " 个音色 (预设:13+1, 克隆:" + manualCloneVoices.length + ")");
        
        allVoices.forEach(v => { 
            let prefix = v.is_cloned ? "🎤 " : "";
            mm[v.voice_id] = { 
                name: prefix + v.voice_name, 
                gender: v.gender 
            } 
        })
        return mm
    },

    // 加载本地或网络数据，运行在IO线程
    "onLoadData": function () {
        logger.i("[onLoadData] ====== 开始加载数据 ======");
        
        try {
            check();
        } catch(e) {
            logger.e("[onLoadData] 检查失败: " + e);
            return;
        }
        
        // 先加载预设音色（过滤掉自定义选项）
        voicesCache = PRESET_VOICES.filter(v => !v.is_custom);
        logger.i("[onLoadData] 预设音色加载完成: " + voicesCache.length + " 个");
        
        // 加载手动配置的克隆音色
        logger.i("[onLoadData] 开始加载手动配置的克隆音色...");
        manualCloneVoices = parseManualCloneVoices();
        if (manualCloneVoices.length > 0) {
            logger.i("[onLoadData] 手动配置加载完成: " + manualCloneVoices.length + " 个克隆音色");
        }
        
        let totalVoices = getAllVoices();
        logger.i("[onLoadData] ====== 加载数据结束 ======");
        logger.i("[onLoadData] 总计音色: " + totalVoices.length + " 个 (预设:13, 克隆:" + manualCloneVoices.length + ")");
    },

    "onLoadUI": function (ctx, linerLayout) {
        logger.i("[onLoadUI] 开始加载 UI");
        
        // 创建克隆音色 ID 输入框
        cloneVoiceEditor = JTextInput(ctx, "克隆音色ID (例如: create_voice_123456789)")
        cloneVoiceEditor.setVisibility(View.GONE)
        linerLayout.addView(cloneVoiceEditor)
        ttsrv.setMargins(cloneVoiceEditor, 0, 8, 0, 0)
        
        // 创建 audio_text 输入框
        cloneAudioTextEditor = JTextInput(ctx, "参考音频文本 (audio_text)")
        cloneAudioTextEditor.setVisibility(View.GONE)
        linerLayout.addView(cloneAudioTextEditor)
        ttsrv.setMargins(cloneAudioTextEditor, 0, 8, 0, 0)
        
        // 创建 audio_url 输入框
        cloneAudioUrlEditor = JTextInput(ctx, "参考音频URL (audio_url)")
        cloneAudioUrlEditor.setVisibility(View.GONE)
        linerLayout.addView(cloneAudioUrlEditor)
        ttsrv.setMargins(cloneAudioUrlEditor, 0, 8, 0, 0)
        
        // ID 输入框文本变化监听
        cloneVoiceEditor.setOnTextChangedListener(function (text) {
            if (text && text.trim() !== "") {
                let trimmed = text.trim();
                if (isCloneVoice(trimmed)) {
                    ttsrv.tts.data['customCloneVoiceId'] = trimmed;
                    logger.i("[onLoadUI] 设置克隆音色ID: " + trimmed);
                }
            } else {
                ttsrv.tts.data['customCloneVoiceId'] = "";
            }
        });
        
        // audio_text 输入框监听
        cloneAudioTextEditor.setOnTextChangedListener(function (text) {
            ttsrv.tts.data['customCloneAudioText'] = text ? text.trim() : "";
            if (text && text.trim()) {
                logger.i("[onLoadUI] 设置 audio_text: " + text.trim().substring(0, 50) + "...");
            }
        });
        
        // audio_url 输入框监听
        cloneAudioUrlEditor.setOnTextChangedListener(function (text) {
            ttsrv.tts.data['customCloneAudioUrl'] = text ? text.trim() : "";
            if (text && text.trim()) {
                logger.i("[onLoadUI] 设置 audio_url: " + text.trim().substring(0, 80) + "...");
            }
        });
        
        logger.i("[onLoadUI] UI 加载完成");
    },

    "onVoiceChanged": function (locale, voiceCode) {
        logger.i("[onVoiceChanged] 切换到音色: " + voiceCode);
        
        // 关闭现有 WebSocket 连接
        if (ws != null) { try { ws.close(1000, "change"); } catch(e) {} ws = null }
        
        // 控制自定义克隆音色输入框的显示/隐藏
        if (voiceCode === "__custom_clone__") {
            if (cloneVoiceEditor) {
                cloneVoiceEditor.setVisibility(View.VISIBLE);
                let savedId = ttsrv.tts.data['customCloneVoiceId'] || "";
                cloneVoiceEditor.text.set(savedId);
            }
            if (cloneAudioTextEditor) {
                cloneAudioTextEditor.setVisibility(View.VISIBLE);
                let savedText = ttsrv.tts.data['customCloneAudioText'] || "";
                cloneAudioTextEditor.text.set(savedText);
            }
            if (cloneAudioUrlEditor) {
                cloneAudioUrlEditor.setVisibility(View.VISIBLE);
                let savedUrl = ttsrv.tts.data['customCloneAudioUrl'] || "";
                cloneAudioUrlEditor.text.set(savedUrl);
            }
            logger.i("[onVoiceChanged] 显示自定义克隆音色输入框");
        } else {
            if (cloneVoiceEditor) cloneVoiceEditor.setVisibility(View.GONE);
            if (cloneAudioTextEditor) cloneAudioTextEditor.setVisibility(View.GONE);
            if (cloneAudioUrlEditor) cloneAudioUrlEditor.setVisibility(View.GONE);
        }
    }
}
