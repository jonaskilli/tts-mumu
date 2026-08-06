var PluginJS = {
    'name': "角色管理_桥接试听v7",
    'id': "mingwuyan",
    'author': "命無言",
    'iconUrl': 'https://img.picui.cn/free/2025/02/24/67bc5a1bac4cf.png',
    'version': 20260802,
  
    // 【核心修改：新增http开头文本的直接下载逻辑】
    'getAudio': function (text, locale, voice, speed, volume, pitch) {
    }
};

var voices = {
    "tts.default.placeholder": { name: "默认发音人", locale: "zh-CN", gender: "female" }
};

// 定义6个变量，分别对应6排关键词（可按需调整每排内容）
var PRESET_KEYWORDS_ROW1 = ["少女", "少年"];
var PRESET_KEYWORDS_ROW2 = ["女青年", "男青年"];
var PRESET_KEYWORDS_ROW3 = ["女中年", "男中年"];
var PRESET_KEYWORDS_ROW4 = ["女老年", "男老年"];
var PRESET_KEYWORDS_ROW5 = ["女童", "男童"];
var PRESET_KEYWORDS_ROW6 = ["女主", "男主"];

// === 映射缓存（全局，启动时一次性加载，避免每次调用都读文件+解析） ===
var _fayinrenMapCache = null;        // 正向映射：存储值 → 显示值
var _fayinrenReverseMapCache = null; // 反向映射：显示值 → 存储值

// 持有 onLoadUI 内部 refreshCharacterList 的引用，供 onVoiceChanged 等模块级回调跨作用域调用
// （refreshCharacterList 定义在 onLoadUI 闭包内，模块级方法无法直接访问）
var _refreshCharacterListFn = null;

function _initFayinrenMapCache(forceRefresh) {
    if (!forceRefresh && _fayinrenMapCache !== null) return; // 已初始化则跳过（除非强制刷新）
    _fayinrenMapCache = {};
    _fayinrenReverseMapCache = {};
    try {
        var jsonContent = ttsrv.readTxtFile("fayinren_personality_summary.json");
        if (jsonContent && jsonContent.trim() !== "") {
            var mapData = JSON.parse(jsonContent);
            if (Array.isArray(mapData) && (mapData.length === 0 || Array.isArray(mapData[0]))) {
                for (var i = 0; i < mapData.length; i++) {
                    var storeVal = mapData[i][0] || '';
                    var displayVal = mapData[i][1] || '';
                    if (storeVal) _fayinrenMapCache[storeVal] = displayVal;
                    if (displayVal) _fayinrenReverseMapCache[displayVal] = storeVal;
                }
                console.log("fayinren映射缓存已加载，共 " + mapData.length + " 条");
            } else {
                console.error("fayinren_personality_summary.json格式错误：需为二维数组");
            }
        } else {
            console.warn("fayinren_personality_summary.json文件为空");
        }
    } catch (e) {
        console.error("读取fayinren_personality_summary.json失败：" + e.toString());
    }
}

// 正向映射：存储值→显示值（使用缓存，O(1)查找）
function replaceFayinrenName(name) {
    var originalName = name || '';
    if (_fayinrenMapCache === null) _initFayinrenMapCache();
    if (_fayinrenMapCache.hasOwnProperty(originalName)) {
        return _fayinrenMapCache[originalName];
    }
    return originalName;
}

// 反向映射：显示值→存储值（使用缓存，O(1)查找）
function reverseReplaceFayinrenName(displayName) {
    var originalName = displayName || '';
    if (_fayinrenReverseMapCache === null) _initFayinrenMapCache();
    if (_fayinrenReverseMapCache.hasOwnProperty(originalName)) {
        return _fayinrenReverseMapCache[originalName];
    }
    return originalName;
}

var EditorJS = {
    'getAudioSampleRate': function (locale, voice) {
        return 44100;
    },
  
    "isNeedDecode": function (locale, voice) {
        return true;
    },
  
    'onLoadData': function () {
    },
  
    'getLocales': function () {
        return Object.keys(voices).map(function(key) { return voices[key].locale; });
    },
  
    'getVoices': function (locale) {
        return Object.keys(voices).reduce(function(acc, key) {
            if (voices[key].locale === locale) {
                acc[key] = {
                    name: voices[key].name,
                    icon: voices[key].gender
                };
            }
            return acc;
        }, {});
    },
  
    'onLoadUI': function (ctx, linearLayout) {
        initializeFileSystem();
        _initFayinrenMapCache(); // 预加载映射缓存（修复1600+发音人卡死问题）
        
        
        
  
        // 处理 liebiao.json 的函数：不存在/异常时初始化存入 ["默认"]
        function initLiebiaoFile() {
            var liebiaoPath = "liebiao.json";
            var defaultLiebiao = '["默认"]'; // 列表文件默认初始化内容，而非空数组
            try {
                var liebiaoJson = ttsrv.readTxtFile(liebiaoPath);
                if (!liebiaoJson || liebiaoJson.trim() === "") {
                    ttsrv.writeTxtFile(liebiaoPath, defaultLiebiao); // 写入默认内容
                    console.log("liebiao.json 为空，已初始化为 [\"默认\"]");
                    Toast.makeText(ctx, "列表配置文件为空，已初始化", Toast.LENGTH_SHORT).show();
                    return;
                }
                var liebiaoList = JSON.parse(liebiaoJson);
                if (!Array.isArray(liebiaoList)) {
                    ttsrv.writeTxtFile(liebiaoPath, defaultLiebiao); // 格式错误时写入默认内容
                    console.log("liebiao.json 格式错，已初始化为 [\"默认\"]");
                    Toast.makeText(ctx, "列表配置格式错误，已初始化", Toast.LENGTH_SHORT).show();
                } else {
                    console.log("liebiao.json 正常，共" + liebiaoList.length + "条列表数据");
                }
            } catch (e) {
                ttsrv.writeTxtFile(liebiaoPath, defaultLiebiao); // 文件不存在时写入默认内容
                console.error("liebiao.json 不存在，已创建文件并写入 [\"默认\"]：" + e.toString());
                Toast.makeText(ctx, "列表配置文件不存在，已初始化", Toast.LENGTH_SHORT).show();
            }
        }
  
        // 分别调用两个初始化函数
         
        initLiebiaoFile();
        
        // 修复：仅初始化 ttsrv.tts.data（不触碰框架的 ttsrv 本身！）
        // 框架已预定义 ttsrv，直接使用即可，禁止赋值 ttsrv = {}
        if (!ttsrv.tts || typeof ttsrv.tts !== "object") ttsrv.tts = {};
        if (!ttsrv.tts.data || typeof ttsrv.tts.data !== "object") ttsrv.tts.data = {};
        
        // 读取自动备份状态（字符清洗+严谨判断）
        var autoBackupState = "0"; // 默认关闭
        try {
            // 处理原始值：转为字符串+去空格，避免异常格式
            var rawState = String(ttsrv.tts.data.autoBackupEnable || "").trim();
            console.log("自动备份原始状态：值='" + rawState + "', 类型=" + typeof rawState);
            
            // 仅当清洗后是"1"才视为开启
            autoBackupState = (rawState === "1") ? "1" : "0";
        } catch (e) {
            console.error("读取自动备份状态异常：" + e.toString());
            autoBackupState = "0";
        }
        
        // 状态为"1"时执行备份
        if (autoBackupState === "1") {
            backupAllFilesToData();
            console.log("自动备份（备份到角色数据）已执行");
        } else {
            console.log("自动备份关闭，不执行");
        }

        
        // ↑ 修正结束 ↑
  
        
        
        
        
        var fayinrenList = [];
        // 重新读取发音人列表（每次需要时从文件刷新）
        function refreshFayinrenList() {
            // 强制刷新personality映射缓存，确保显示最新的性格信息
            _initFayinrenMapCache(true);
            fayinrenList = [];
            try {
                var fayinrenJson = ttsrv.readTxtFile("fayinren.json");
                if (fayinrenJson) {
                    fayinrenList = JSON.parse(fayinrenJson);
                    // 方案B：fayinrenList 保持 tag 原值（如"女青年01"），不做 replaceFayinrenName 转换
                    // 按类别+序号排序
                    fayinrenList.sort(function (a, b) {
                        var reA = String(a).match(/^(.+?)(\d+)/);
                        var reB = String(b).match(/^(.+?)(\d+)/);
                        if (reA && reB) {
                            if (reA[1] !== reB[1]) return reA[1] < reB[1] ? -1 : 1;
                            return parseInt(reA[2]) - parseInt(reB[2]);
                        }
                        if (reA) return -1;
                        if (reB) return 1;
                        return String(a) < String(b) ? -1 : 1;
                    });
                    console.log("从fayinren.json刷新发音人列表: " + fayinrenList.length + " 条");
                } else {
                    console.log("fayinren.json文件内容为空");
                }
            } catch (e) {
                console.error("读取fayinren.json失败: " + e.toString());
            }
        }
        // 初始加载
        refreshFayinrenList();

        function getKeyMapAndList() {
            var keyMap = getKeyMapFromData();
            var list = [];
            try {
                var fileContent = ttsrv.readTxtFile(KEY_LIST_FILE);
                if (fileContent && fileContent.trim() !== "") {
                    var arr = JSON.parse(fileContent);
                    if (Array.isArray(arr)) {
                        for (var i = 0; i < arr.length; i++) {
                            if (arr[i] && arr[i].length >= 2) {
                                var name = (arr[i][0] || "").trim(); // 清洗名称
                                if (name && keyMap.hasOwnProperty(name)) {
                                    list.push(name);
                                }
                            }
                        }
                    }
                }
            } catch (e) {
                console.error("读取密钥顺序失败：" + e.toString());
            }
            // 回退逻辑也要清洗
            if (list.length === 0) {
                for (var prop in keyMap) {
                    if (keyMap.hasOwnProperty(prop)) {
                        var cleanProp = prop.trim();
                        if (cleanProp) list.push(cleanProp);
                    }
                }
            }
            return { map: keyMap, list: list };
        }

        // 密钥管理对话框（直接显示密钥列表，每项含切换/修改/删除）
        function showKeyManageDialog() {
            var kl = getKeyMapAndList();
            var curName = (ttsrv.tts.data['currentKeyName'] || '').toString().trim();
            var currentLocalKey = "";
            // 【新逻辑：优先读 miyue.txt，失败再回退到备份文件】
            var keyFiles = ["miyue.txt", "gengxin.txt", "miyue_backup.txt"];
            for (var si = 0; si < keyFiles.length; si++) {
                try {
                    var sk = (ttsrv.readTxtFile(keyFiles[si]) || "").toString().trim();
                    if (sk) {
                        currentLocalKey = sk;
                        console.log("从 " + keyFiles[si] + " 读取到本地密钥");
                        break; // 读到第一个非空就停止，不再继续遍历备份文件
                    }
                } catch (eSrc) {
                    console.error("读取" + keyFiles[si] + "失败：" + eSrc.toString());
                }
            }

            // 确保至少有一个启用的密钥
            if (kl.list.length > 0) {
                var hasActive = false;
                // 1. 检查 curName 是否指向有效密钥（清洗名称后比较）
                if (curName) {
                    for (var ci = 0; ci < kl.list.length; ci++) {
                        if (kl.list[ci] === curName && kl.map.hasOwnProperty(curName)) {
                            hasActive = true;
                            break;
                        }
                    }
                }
                // 2. 如果 curName 无效，再通过本地密钥精确匹配
                if (!hasActive && currentLocalKey) {
                    var cleanLocal = currentLocalKey.trim();
                    for (var ci = 0; ci < kl.list.length; ci++) {
                        var cItem = kl.map[kl.list[ci]];
                        var cKey = cItem && cItem.value ? cItem.value.toString().trim() : "";
                        if (cKey && cleanLocal === cKey) {   // 精确相等，不含包含
                            hasActive = true;
                            curName = kl.list[ci];           // 已经 trim 过的名称
                            ttsrv.tts.data['currentKeyName'] = curName;
                            break;
                        }
                    }
                }
                // 【新逻辑：先尝试反向查找，只在真正找不到时才启用第一个】
                if (!hasActive && currentLocalKey) {
                    // 兜底1：通过本地密钥内容反向匹配，找出对应的名称
                    var cleanLocal = currentLocalKey.trim();
                    for (var ci2 = 0; ci2 < kl.list.length; ci2++) {
                        var cItem2 = kl.map[kl.list[ci2]];
                        var cKey2 = cItem2 && cItem2.value ? cItem2.value.toString().trim() : "";
                        if (cKey2 && String(cleanLocal) === String(cKey2)) {
                            hasActive = true;
                            curName = kl.list[ci2];
                            ttsrv.tts.data['currentKeyName'] = curName;
                            console.log("反向匹配到密钥: " + curName);
                            break;
                        }
                    }
                }
                
                // 仍然找不到（列表为空或本地密钥为空）时才启用第一个
                if (!hasActive && kl.list.length > 0) {
                    var firstName = kl.list[0];
                    var firstKey = kl.map[firstName];
                    var firstKeyValue = firstKey && firstKey.value ? firstKey.value.toString().trim() : "";
                    if (firstKeyValue) {
                        saveKeyToLocal(firstKeyValue);
                        ttsrv.tts.data['currentKeyName'] = firstName;
                        curName = firstName;
                        currentLocalKey = firstKeyValue;
                        console.log("无匹配项，自动启用第一个密钥: " + firstName);
                    } else {
                        console.log("列表第一个密钥内容为空，跳过启用");
                    }
                }
            }

            console.log("密钥管理：curName='" + curName + "', currentLocalKey长度=" + currentLocalKey.length);

            function getItemKey(keyItem) {
                return keyItem && keyItem.value ? keyItem.value.toString().trim() : "";
            }

            function refreshKeyManageDialog() {
                try {
                    if (dialog) dialog.dismiss();
                } catch (refreshErr) {
                    console.error("刷新密钥管理弹窗失败：" + refreshErr.toString());
                }
                showKeyManageDialog();
            }

            function runOnUiThreadSafe(fn) {
                try {
                    if (ctx && ctx.runOnUiThread) {
                        ctx.runOnUiThread(new java.lang.Runnable({ run: fn }));
                    } else {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(new java.lang.Runnable({ run: fn }));
                    }
                } catch (uiErr) {
                    try { fn(); } catch (ignoreUiErr) {}
                }
            }

            function normalizeBaseUrl(url) {
                var u = String(url || "").trim();
                if (u.indexOf("http:/") === 0 && u.indexOf("http://") !== 0) u = "http://" + u.substring(6);
                if (u.indexOf("https:/") === 0 && u.indexOf("https://") !== 0) u = "https://" + u.substring(7);
                while (u.length > 1 && u.charAt(u.length - 1) === "/") u = u.substring(0, u.length - 1);
                return u;
            }

            function endsWithText(text, suffix) {
                text = String(text || "");
                suffix = String(suffix || "");
                if (suffix.length > text.length) return false;
                return text.substring(text.length - suffix.length) === suffix;
            }

            function getOpenAiBaseUrl(url) {
                var u = normalizeBaseUrl(url);
                if (endsWithText(u, "/chat/completions")) return u.substring(0, u.length - "/chat/completions".length);
                if (endsWithText(u, "/completions")) return u.substring(0, u.length - "/completions".length);
                return u;
            }

            function getOpenAiChatUrl(url) {
                var u = normalizeBaseUrl(url);
                if (endsWithText(u, "/chat/completions")) return u;
                if (endsWithText(u, "/completions")) return getOpenAiBaseUrl(u) + "/chat/completions";
                return u + "/chat/completions";
            }

            function joinApiPath(baseUrl, path) {
                var base = getOpenAiBaseUrl(baseUrl);
                if (!base) return path;
                return base + (path.charAt(0) === "/" ? path : "/" + path);
            }

            function parseKeyForTest(rawKey) {
                var text = (rawKey || "").toString().trim();
                if (!text) return { ok: false, reason: "密钥内容为空" };
                var parts = text.split("@@");
                if (parts.length >= 3) {
                    var apiKey = parts.slice(2).join("@@").trim();
                    var rawUrl = normalizeBaseUrl((parts[0] || "").trim());
                    var baseUrl = getOpenAiBaseUrl(rawUrl);
                    var chatUrl = getOpenAiChatUrl(rawUrl);
                    var model = (parts[1] || "").trim();
                    if (!baseUrl) return { ok: false, reason: "URL 不能为空" };
                    if (baseUrl.indexOf("http://") !== 0 && baseUrl.indexOf("https://") !== 0) return { ok: false, reason: "URL 必须以 http:// 或 https:// 开头" };
                    if (!model) return { ok: false, reason: "模型名不能为空" };
                    if (!apiKey) return { ok: false, reason: "API Key 不能为空" };
                    return { ok: true, type: "openai", rawUrl: rawUrl, baseUrl: baseUrl, chatUrl: chatUrl, model: model, apiKey: apiKey };
                }
                if (parts.length > 1) return { ok: false, reason: "格式不完整，应为 URL@@模型名@@API Key" };
                return { ok: true, type: "zhipu", apiKey: text };
            }

            function readHttpStream(conn, isError) {
                var stream = null;
                try {
                    stream = isError ? conn.getErrorStream() : conn.getInputStream();
                    if (stream == null) return "";
                    var reader = new java.io.BufferedReader(new java.io.InputStreamReader(stream, "UTF-8"));
                    var sb = new java.lang.StringBuilder();
                    var line;
                    while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                    reader.close();
                    return sb.toString();
                } catch (e) {
                    return "";
                } finally {
                    try { if (stream != null) stream.close(); } catch (closeErr) {}
                }
            }

            function httpJsonRequest(url, method, apiKey, body) {
                var conn = null;
                try {
                    conn = new java.net.URL(url).openConnection();
                    conn.setRequestMethod(method);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Accept", "application/json");
                    if (apiKey) conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                    if (body != null) {
                        conn.setDoOutput(true);
                        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                        var bytes = new java.lang.String(body).getBytes("UTF-8");
                        conn.setRequestProperty("Content-Length", String(bytes.length));
                        var os = conn.getOutputStream();
                        os.write(bytes);
                        os.flush();
                        os.close();
                    }
                    var code = conn.getResponseCode();
                    var content = readHttpStream(conn, code >= 400);
                    return { ok: code >= 200 && code < 300, code: code, body: content || "" };
                } catch (e) {
                    return { ok: false, code: -1, body: e.toString() };
                } finally {
                    try { if (conn != null) conn.disconnect(); } catch (disconnectErr) {}
                }
            }

            function briefResponse(resp) {
                var body = String(resp && resp.body ? resp.body : "");
                var compact = "";
                var lastSpace = false;
                for (var bi = 0; bi < body.length; bi++) {
                    var ch = body.charAt(bi);
                    var isSpace = (ch === " " || ch === "\n" || ch === "\r" || ch === "\t");
                    if (isSpace) {
                        if (!lastSpace) compact += " ";
                        lastSpace = true;
                    } else {
                        compact += ch;
                        lastSpace = false;
                    }
                }
                body = compact.trim();
                if (body.length > 180) body = body.substring(0, 180) + "...";
                if (!body) body = "无响应内容";
                return "HTTP " + (resp ? resp.code : "-1") + "，" + body;
            }

            function testModelKey(rawKey) {
                var parsed = parseKeyForTest(rawKey);
                if (!parsed.ok) return { ok: false, message: parsed.reason };
                if (parsed.type === "zhipu") {
                    var zhipuResp = httpJsonRequest("https://open.bigmodel.cn/api/paas/v4/models", "GET", parsed.apiKey, null);
                    if (zhipuResp.ok) return { ok: true, message: "密钥可用！智谱 /models 验证成功" };
                    return { ok: false, message: "智谱 /models 验证失败：" + briefResponse(zhipuResp) };
                }

                var modelsUrl = joinApiPath(parsed.baseUrl, "/models");
                var modelsResp = httpJsonRequest(modelsUrl, "GET", parsed.apiKey, null);
                if (modelsResp.ok) return { ok: true, message: "密钥可用！模型：" + parsed.model };

                var completionsUrl = joinApiPath(parsed.baseUrl, "/completions");
                var payload = JSON.stringify({ model: parsed.model, prompt: "ping", max_tokens: 1, temperature: 0 });
                var compResp = httpJsonRequest(completionsUrl, "POST", parsed.apiKey, payload);
                if (compResp.ok) return { ok: true, message: "密钥可用！模型：" + parsed.model };

                var chatUrl = parsed.chatUrl || joinApiPath(parsed.baseUrl, "/chat/completions");
                var chatPayload = JSON.stringify({
                    model: parsed.model,
                    messages: [{ role: "user", content: "ping" }],
                    max_tokens: 1,
                    temperature: 0
                });
                var chatResp = httpJsonRequest(chatUrl, "POST", parsed.apiKey, chatPayload);
                if (chatResp.ok) return { ok: true, message: "密钥可用！模型：" + parsed.model };

                return { ok: false, message: "/models、/completions 与 /chat/completions 均验证失败。\n/models：" + briefResponse(modelsResp) + "\n/completions：" + briefResponse(compResp) + "\n/chat/completions：" + briefResponse(chatResp) };
            }

            var currentTestResultPopup = null;
            function showBottomTestTip(anchor, ok, message) {
                try {
                    if (currentTestResultPopup != null && currentTestResultPopup.isShowing()) {
                        currentTestResultPopup.dismiss();
                    }

                    var root = new android.widget.LinearLayout(ctx);
                    root.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    root.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    root.setPadding(dipToPx(20), dipToPx(14), dipToPx(20), dipToPx(14));

                    var bg = new android.graphics.drawable.GradientDrawable();
                    bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                    bg.setCornerRadius(dipToPx(28));
                    bg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                    root.setBackground(bg);
                    try { root.setElevation(dipToPx(8)); } catch (elevErr) {}

                    var icon = new android.widget.TextView(ctx);
                    icon.setText(ok ? "✓" : "!");
                    icon.setTextSize(15);
                    icon.setTextColor(android.graphics.Color.WHITE);
                    icon.setGravity(android.view.Gravity.CENTER);
                    var iconBg = new android.graphics.drawable.GradientDrawable();
                    iconBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                    iconBg.setCornerRadius(dipToPx(4));
                    iconBg.setColor(android.graphics.Color.parseColor(ok ? "#35C759" : "#E53935"));
                    icon.setBackground(iconBg);
                    var iconParams = new android.widget.LinearLayout.LayoutParams(dipToPx(20), dipToPx(20));
                    iconParams.setMargins(0, 0, dipToPx(8), 0);
                    icon.setLayoutParams(iconParams);
                    root.addView(icon);

                    var textView = new android.widget.TextView(ctx);
                    textView.setText((ok ? "" : "测试失败！原因：") + (message || "未知结果"));
                    textView.setTextSize(15);
                    textView.setTextColor(android.graphics.Color.parseColor("#333333"));
                    textView.setSingleLine(false);
                    textView.setMaxLines(4);
                    var textParams = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                    textView.setLayoutParams(textParams);
                    root.addView(textView);

                    var dm = ctx.getResources().getDisplayMetrics();
                    var popupWidth = Math.floor(dm.widthPixels * 0.86);
                    currentTestResultPopup = new android.widget.PopupWindow(
                        root,
                        popupWidth,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        false
                    );
                    currentTestResultPopup.setOutsideTouchable(false);
                    try { currentTestResultPopup.setClippingEnabled(true); } catch (clipErr) {}
                    try { currentTestResultPopup.setElevation(dipToPx(8)); } catch (popupElevErr) {}
                    var dm = ctx.getResources().getDisplayMetrics();
          var yOffset = Math.floor(dm.heightPixels * 0.45);
   currentTestResultPopup.showAtLocation(anchor, android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL, 0, yOffset);

                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new java.lang.Runnable({
                        run: function() {
                            try {
                                if (currentTestResultPopup != null && currentTestResultPopup.isShowing()) {
                                    currentTestResultPopup.dismiss();
                                }
                            } catch (dismissErr) {}
                        }
                    }), 5000);
                } catch (tipErr) {
                    Toast.makeText(ctx, (ok ? "测试成功：" : "测试失败：") + (message || tipErr.toString()), Toast.LENGTH_LONG).show();
                }
            }

            function isCurrentKeyItem(name, keyItem) {
                var itemKey = getItemKey(keyItem);
                var trimmedName = (name || "").trim();   // 清洗名称
                // 【第一优先】按“清洗后的名称”匹配
                if (curName && trimmedName === curName) {
                    return true;
                }
                // 【第二优先】精确匹配密钥内容（用作回退，但必须双方都 trim）
                if (currentLocalKey && itemKey) {
                    var cleanLocal = currentLocalKey.trim();
                    var cleanItem = itemKey.trim();
                    if (cleanLocal === cleanItem) {
                        // 同步更新名称，保证后续刷新高亮持续
                        if (!curName) {
                            ttsrv.tts.data['currentKeyName'] = trimmedName;
                            curName = trimmedName;
                        }
                        return true;
                    }
                }
                return false;
            }

            // 小按钮工厂
            function createSmallButton(text, color) {
                var btn = new android.widget.TextView(ctx);
                btn.setText(text);
                btn.setTextSize(13);
                btn.setTextColor(android.graphics.Color.parseColor(color));
                btn.setPadding(dipToPx(14), dipToPx(6), dipToPx(14), dipToPx(6));
                var shape = new android.graphics.drawable.GradientDrawable();
                shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                shape.setCornerRadius(dipToPx(6));
                // 浅色底 + 彩色描边 + 彩色文字（与其他区域和谐统一，不浓重）
                var cInt = android.graphics.Color.parseColor(color);
                var lr = Math.round(android.graphics.Color.red(cInt) * 0.12 + 255 * 0.88);
                var lg = Math.round(android.graphics.Color.green(cInt) * 0.12 + 255 * 0.88);
                var lb = Math.round(android.graphics.Color.blue(cInt) * 0.12 + 255 * 0.88);
                shape.setColor(android.graphics.Color.argb(255, lr, lg, lb));
                shape.setStroke(dipToPx(1), cInt);
                btn.setBackground(shape);
                var params = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(dipToPx(5), 0, 0, 0);
                btn.setLayoutParams(params);
                return btn;
            }

            var builder = new android.app.AlertDialog.Builder(ctx);
            
            var container = new android.widget.LinearLayout(ctx);
            container.setOrientation(android.widget.LinearLayout.VERTICAL);
            container.setPadding(dipToPx(16), dipToPx(12), dipToPx(16), dipToPx(16));
            container.addView(createDialogTitle("密钥管理"));


            if (kl.list.length === 0) {
                var emptyHint = new android.widget.TextView(ctx);
                emptyHint.setText("暂无密钥，点击下方「新增密钥」添加");
                emptyHint.setTextSize(14);
                emptyHint.setTextColor(android.graphics.Color.parseColor("#757575"));
                emptyHint.setPadding(0, dipToPx(24), 0, dipToPx(24));
                emptyHint.setGravity(android.view.Gravity.CENTER);
                container.addView(emptyHint);
            }

            for (var i = 0; i < kl.list.length; i++) {
                (function(name, keyItem, isCurrent, keyIdx) {
                    var row = new android.widget.LinearLayout(ctx);
                    row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    var rowParams = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    rowParams.setMargins(0, dipToPx(4), 0, dipToPx(4));
                    row.setLayoutParams(rowParams);

                    var rowBg = new android.graphics.drawable.GradientDrawable();
                    rowBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                    rowBg.setCornerRadius(dipToPx(10));
                    if (isCurrent) {
                        rowBg.setColor(android.graphics.Color.parseColor("#E3F2FD"));
                        rowBg.setStroke(dipToPx(1.5), android.graphics.Color.parseColor("#1976D2"));
                    } else {
                        rowBg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                        rowBg.setStroke(dipToPx(1), android.graphics.Color.parseColor("#10000000"));
                    }
                    row.setBackground(rowBg);
                    row.setPadding(dipToPx(14), dipToPx(12), dipToPx(14), dipToPx(12));

                    function switchToThisKey() {
                        if (isCurrent) {
                            Toast.makeText(ctx, "【" + name + "】已是当前密钥", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        var key = getItemKey(keyItem);
                        if (android.text.TextUtils.isEmpty(key)) {
                            Toast.makeText(ctx, "【" + name + "】密钥内容为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        saveKeyToLocal(key);
                        ttsrv.tts.data['currentKeyName'] = name;
                        currentLocalKey = key;
                        curName = name;
                        Toast.makeText(ctx, "已切换到【" + name + "】", Toast.LENGTH_SHORT).show();
                        if (dialog) dialog.dismiss();
                    }

                    // 左侧指示器（参考书籍切换栏：选中=蓝色对勾，未选中=彩色圆点）
                    var keyColors = ["#7E57C2", "#7E57C2", "#26A69A", "#8D6E63", "#66BB6A", "#EC407A", "#FF7043", "#42A5F5"];
                    if (isCurrent) {
                        var checkText = new android.widget.TextView(ctx);
                        checkText.setText("✓");
                        checkText.setTextSize(15);
                        checkText.setTextColor(android.graphics.Color.parseColor("#1976D2"));
                        checkText.setGravity(android.view.Gravity.CENTER);
                        checkText.setPadding(0, 0, dipToPx(8), 0);
                        row.addView(checkText);
                    } else {
                        var keyDot = new android.view.View(ctx);
                        var keyDotParams = new android.widget.LinearLayout.LayoutParams(dipToPx(7), dipToPx(7));
                        keyDotParams.setMargins(0, 0, dipToPx(8), 0);
                        keyDot.setLayoutParams(keyDotParams);
                        var keyDotBg = new android.graphics.drawable.GradientDrawable();
                        keyDotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                        keyDotBg.setColor(android.graphics.Color.parseColor(keyColors[keyIdx % keyColors.length]));
                        keyDot.setBackground(keyDotBg);
                        row.addView(keyDot);
                    }

                    // 密钥名称：点击直接切换当前密钥
                    var nameText = new android.widget.TextView(ctx);
                    nameText.setText(name);
                    nameText.setTextSize(15);
                    nameText.setTextColor(android.graphics.Color.parseColor("#333333"));
                    nameText.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1
                    ));
                    nameText.setOnClickListener(new android.view.View.OnClickListener({
                        onClick: function(v) {
                            switchToThisKey();
                        }
                    }));
                    row.addView(nameText);

                    // 查看/编辑按钮：查看密钥内容，可复制、编辑或删除
                    var viewEditBtn = createSmallButton("查看/编辑", "#1976D2");
                    viewEditBtn.setOnClickListener(new android.view.View.OnClickListener({
                        onClick: function(v) {
                            try {
                                var key = getItemKey(keyItem);
                    
                                var detailBuilder = new android.app.AlertDialog.Builder(ctx);
                                var detailRoot = new android.widget.LinearLayout(ctx);
                                detailRoot.setOrientation(android.widget.LinearLayout.VERTICAL);
                                detailRoot.setPadding(dipToPx(20), dipToPx(16), dipToPx(20), dipToPx(16));
                    
                                function createCardBg() {
                                    var bg = new android.graphics.drawable.GradientDrawable();
                                    bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                                    bg.setCornerRadius(dipToPx(8));
                                    bg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                                    bg.setStroke(dipToPx(1.5), android.graphics.Color.parseColor("#10000000"));
                                    return bg;
                                }
                    
                                var nameLabel = new android.widget.TextView(ctx);
                                nameLabel.setText("名称");
                                nameLabel.setTextSize(14);
                                nameLabel.setTextColor(android.graphics.Color.parseColor("#757575"));
                                nameLabel.setPadding(dipToPx(2), 0, 0, dipToPx(4));
                                detailRoot.addView(nameLabel);
                    
                                var nameInput = new android.widget.EditText(ctx);
                                nameInput.setText(name);
                                nameInput.setSelection(name.length);
                                nameInput.setTextSize(16);
                                nameInput.setSingleLine(true);
                                nameInput.setTextColor(android.graphics.Color.parseColor("#333333"));
                                nameInput.setPadding(dipToPx(12), dipToPx(10), dipToPx(12), dipToPx(10));
                                nameInput.setBackground(createCardBg());
                                detailRoot.addView(nameInput);
                    
                                var spacer = new android.view.View(ctx);
                                spacer.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dipToPx(12)));
                                detailRoot.addView(spacer);
                    
                                var keyLabel = new android.widget.TextView(ctx);
                                keyLabel.setText("密钥内容");
                                keyLabel.setTextSize(14);
                                keyLabel.setTextColor(android.graphics.Color.parseColor("#757575"));
                                keyLabel.setPadding(dipToPx(2), 0, 0, dipToPx(4));
                                detailRoot.addView(keyLabel);
                    
                                var keyInput = new android.widget.EditText(ctx);
                                keyInput.setText(key || "");
                                if (key) keyInput.setSelection(key.length);
                                keyInput.setTextSize(16);
                                keyInput.setSingleLine(false);
                                keyInput.setTextColor(android.graphics.Color.parseColor("#333333"));
                                keyInput.setPadding(dipToPx(12), dipToPx(10), dipToPx(12), dipToPx(10));
                                keyInput.setBackground(createCardBg());
                                detailRoot.addView(keyInput);
                    
                                var btnRow = new android.widget.LinearLayout(ctx);
                                btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                                btnRow.setGravity(android.view.Gravity.CENTER);
                                btnRow.setPadding(0, dipToPx(14), 0, 0);
                    
                                function createTextBtn(text, color) {
                                    var b = new android.widget.TextView(ctx);
                                    b.setText(text);
                                    b.setTextSize(14);
                                    b.setTextColor(android.graphics.Color.parseColor(color));
                                    b.setGravity(android.view.Gravity.CENTER);
                                    b.setSingleLine(true);
                                    b.setPadding(dipToPx(6), dipToPx(10), dipToPx(6), dipToPx(10));
                                    return b;
                                }

                                function applyActionBtnLayout(btn, index) {
                                    var p = new android.widget.LinearLayout.LayoutParams(
                                        0,
                                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                        1
                                    );
                                    if (index > 0) p.setMargins(dipToPx(8), 0, 0, 0);
                                    btn.setLayoutParams(p);
                                }
                    
                                var isDeleting = false;
                                var isSaved = false;
                    
                                var delBtn2 = createTextBtn("删除", "#E53935");
                                applyActionBtnLayout(delBtn2, 0);
                                delBtn2.setOnClickListener(new android.view.View.OnClickListener({
                                    onClick: function(v) {
                                        isDeleting = true;
                                        detailDialog.dismiss();
                                        new android.app.AlertDialog.Builder(ctx)
                                            .setTitle("删除确认")
                                            .setMessage("确定删除【" + name + "】？")
                                            .setPositiveButton("删除", function(d2) {
                                                delete kl.map[name];
                                                var idx = kl.list.indexOf(name);
                                                if (idx !== -1) kl.list.splice(idx, 1);
                                                saveKeyMapToData(kl.map, kl.list);
                                                var cur = (ttsrv.tts.data['currentKeyName'] || '').toString().trim();
                                                if (cur === name) {
                                                    ttsrv.tts.data['currentKeyName'] = "";
                                                    if (kl.list.length > 0) {
                                                        var nextName = kl.list[0];
                                                        var nextKey = kl.map[nextName];
                                                        var nextKeyValue = nextKey && nextKey.value ? nextKey.value.toString().trim() : "";
                                                        if (nextKeyValue) {
                                                            saveKeyToLocal(nextKeyValue);
                                                            ttsrv.tts.data['currentKeyName'] = nextName;
                                                            console.log("删除后自动启用: " + nextName);
                                                        }
                                                    } else {
                                                        try {
                                                            ttsrv.writeTxtFile("miyue.txt", "");
                                                            ttsrv.writeTxtFile("gengxin.txt", "");
                                                        } catch (clearErr) {
                                                            console.error("清除本地密钥失败：" + clearErr.toString());
                                                        }
                                                    }
                                                }
                                                Toast.makeText(ctx, "已删除【" + name + "】", Toast.LENGTH_SHORT).show();
                                                d2.dismiss();
                                                refreshKeyManageDialog();
                                            })
                                            .setNegativeButton("取消", function(d2) { d2.cancel(); })
                                            .show();
                                    }
                                }));
                                btnRow.addView(delBtn2);
                    
                                var copyBtn = createTextBtn("复制", "#1976D2");
                                applyActionBtnLayout(copyBtn, 1);
                                copyBtn.setOnClickListener(new android.view.View.OnClickListener({
                                    onClick: function(v) {
                                        try {
                                            var curKey = keyInput.getText() ? keyInput.getText().toString().trim() : "";
                                            if (curKey) {
                                                var cb = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                                cb.setPrimaryClip(android.content.ClipData.newPlainText("密钥", curKey));
                                                Toast.makeText(ctx, "已复制", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(ctx, "密钥内容为空", Toast.LENGTH_SHORT).show();
                                            }
                                        } catch (copyErr) {
                                            console.error("复制密钥失败：" + copyErr.toString());
                                            Toast.makeText(ctx, "复制失败", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }));
                                btnRow.addView(copyBtn);

                                var testBtn = createTextBtn("测试", "#455A64");
                                applyActionBtnLayout(testBtn, 2);
                                testBtn.setOnClickListener(new android.view.View.OnClickListener({
                                    onClick: function(v) {
                                        try {
                                            var curKey = keyInput.getText() ? keyInput.getText().toString().trim() : "";
                                            if (!curKey) {
                                                Toast.makeText(ctx, "密钥内容为空", Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                            testBtn.setEnabled(false);
                                            testBtn.setText("测试中");
                                            Toast.makeText(ctx, "正在测试模型可用性…", Toast.LENGTH_SHORT).show();
                                            new java.lang.Thread(new java.lang.Runnable({
                                                run: function() {
                                                    var result;
                                                    try {
                                                        result = testModelKey(curKey);
                                                    } catch (testErr) {
                                                        result = { ok: false, message: testErr.toString() };
                                                    }
                                                    runOnUiThreadSafe(function() {
                                                        try {
                                                            testBtn.setEnabled(true);
                                                            testBtn.setText("测试");
                                                            showBottomTestTip(detailRoot, result.ok, result.message || "未知结果");
                                                        } catch (showErr) {
                                                            Toast.makeText(ctx, (result.ok ? "测试成功：" : "测试失败：") + (result.message || showErr.toString()), Toast.LENGTH_LONG).show();
                                                        }
                                                    });
                                                }
                                            })).start();
                                        } catch (clickTestErr) {
                                            testBtn.setEnabled(true);
                                            testBtn.setText("测试");
                                            Toast.makeText(ctx, "测试失败：" + clickTestErr.toString(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }));
                                btnRow.addView(testBtn);
                    
                                var saveBtn = createTextBtn("保存", "#1976D2");
                                applyActionBtnLayout(saveBtn, 3);
                                saveBtn.setOnClickListener(new android.view.View.OnClickListener({
                                    onClick: function(v) {
                                        try {
                                            var newName = nameInput.getText() ? nameInput.getText().toString().trim() : "";
                                            var newKey = keyInput.getText() ? keyInput.getText().toString().trim() : "";
                                            if (android.text.TextUtils.isEmpty(newName)) {
                                                Toast.makeText(ctx, "名称不能为空", Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                            if (android.text.TextUtils.isEmpty(newKey)) {
                                                Toast.makeText(ctx, "密钥内容不能为空", Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                            if (newName !== name && kl.map.hasOwnProperty(newName)) {
                                                Toast.makeText(ctx, "名称【" + newName + "】已存在", Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                            delete kl.map[name];
                                            kl.map[newName] = { keyCode: keyItem ? keyItem.keyCode : null, value: newKey };
                                            if (newName !== name) {
                                                var ri = kl.list.indexOf(name);
                                                if (ri !== -1) kl.list[ri] = newName;
                                                else kl.list.push(newName);
                                            }
                                            saveKeyMapToData(kl.map, kl.list);
                                            var curName = (ttsrv.tts.data['currentKeyName'] || '').toString().trim();
                                            if (curName === name) {
                                                saveKeyToLocal(newKey);
                                                ttsrv.tts.data['currentKeyName'] = newName;
                                            }
                                            Toast.makeText(ctx, "已保存【" + newName + "】", Toast.LENGTH_SHORT).show();
                                            isSaved = true;
                                            name = newName;
                                            key = newKey;
                                            detailDialog.dismiss();
                                            refreshKeyManageDialog();
                                        } catch (saveErr) {
                                            console.error("保存失败：" + saveErr.toString());
                                            Toast.makeText(ctx, "保存失败：" + saveErr.toString(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }));
                                btnRow.addView(saveBtn);
                    
                                detailRoot.addView(btnRow);
                                detailBuilder.setView(detailRoot);
                                var detailDialog = detailBuilder.show();
                                applyDialogRoundCorner(detailDialog);
                    
                                // 关闭弹窗时自动保存（仅当未通过保存按钮保存过时）
                                detailDialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener({
                                    onDismiss: function(d) {
                                        if (isDeleting) return;
                                        if (isSaved) return;
                                        try {
                                            var newName = nameInput.getText() ? nameInput.getText().toString().trim() : "";
                                            var newKey = keyInput.getText() ? keyInput.getText().toString().trim() : "";
                                            if (newName === name && newKey === key) return;
                                            if (android.text.TextUtils.isEmpty(newName) || android.text.TextUtils.isEmpty(newKey)) return;
                                            delete kl.map[name];
                                            kl.map[newName] = { keyCode: keyItem ? keyItem.keyCode : null, value: newKey };
                                            if (newName !== name) {
                                                var di = kl.list.indexOf(name);
                                                if (di !== -1) kl.list[di] = newName;
                                                else kl.list.push(newName);
                                            }
                                            saveKeyMapToData(kl.map, kl.list);
                                            var curName = (ttsrv.tts.data['currentKeyName'] || '').toString().trim();
                                            if (curName === name) {
                                                saveKeyToLocal(newKey);
                                                ttsrv.tts.data['currentKeyName'] = newName;
                                            }
                                            refreshKeyManageDialog();
                                        } catch (saveErr) {
                                            console.error("自动保存失败：" + saveErr.toString());
                                        }
                                    }
                                }));
                            } catch (detailErr) {
                                console.error("显示密钥详情失败：" + detailErr.toString());
                                Toast.makeText(ctx, "显示密钥详情失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }));
                    row.addView(viewEditBtn);

                    // 删除按钮（带确认）
                    var delBtn = createSmallButton("删除", "#E53935");
                    delBtn.setOnClickListener(new android.view.View.OnClickListener({
                        onClick: function(v) {
                            new android.app.AlertDialog.Builder(ctx)
                                .setTitle("删除确认")
                                .setMessage("确定删除【" + name + "】？")
                                .setPositiveButton("删除", function(d) {
                                    delete kl.map[name];
                                    // 从顺序列表中移除
                                    var di2 = kl.list.indexOf(name);
                                    if (di2 !== -1) kl.list.splice(di2, 1);
                                    saveKeyMapToData(kl.map, kl.list);
                                    var cur = (ttsrv.tts.data['currentKeyName'] || '').toString().trim();
                                    if (cur === name) {
                                        ttsrv.tts.data['currentKeyName'] = "";
                                        // 还有密钥时自动启用第一个（按添加顺序）
                                        if (kl.list.length > 0) {
                                            var nextName2 = kl.list[0];
                                            var nextKey2 = kl.map[nextName2];
                                            var nextKeyValue2 = nextKey2 && nextKey2.value ? nextKey2.value.toString().trim() : "";
                                            if (nextKeyValue2) {
                                                saveKeyToLocal(nextKeyValue2);
                                                ttsrv.tts.data['currentKeyName'] = nextName2;
                                                console.log("删除后自动启用: " + nextName2);
                                            }
                                        } else {
                                            try {
                                                ttsrv.writeTxtFile("miyue.txt", "");
                                                ttsrv.writeTxtFile("gengxin.txt", "");
                                            } catch (clearErr) {
                                                console.error("清除本地密钥失败：" + clearErr.toString());
                                            }
                                        }
                                    }
                                    Toast.makeText(ctx, "已删除【" + name + "】", Toast.LENGTH_SHORT).show();
                                    d.dismiss();
                                    if (dialog) dialog.dismiss();
                                })
                                .setNegativeButton("取消", function(d) { d.cancel(); })
                                .show();
                        }
                    }));
                    row.addView(delBtn);

                    container.addView(row);
                })(kl.list[i], kl.map[kl.list[i]], isCurrentKeyItem(kl.list[i], kl.map[kl.list[i]]), i);
            }

            // 新增密钥行（点击新增，与列表融为一体）
            var addRow = new android.widget.LinearLayout(ctx);
            addRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            addRow.setGravity(android.view.Gravity.CENTER);
            var addRowParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            addRowParams.setMargins(0, dipToPx(8), 0, dipToPx(4));
            addRow.setLayoutParams(addRowParams);

            var addBg = new android.graphics.drawable.GradientDrawable();
            addBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            addBg.setCornerRadius(dipToPx(10));
            addBg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
            addRow.setBackground(addBg);
            addRow.setPadding(dipToPx(12), dipToPx(12), dipToPx(12), dipToPx(12));

            var addText = new android.widget.TextView(ctx);
            addText.setText("＋ 新增密钥");
            addText.setTextSize(15);
            addText.setTextColor(android.graphics.Color.parseColor("#1976D2"));
            addText.setGravity(android.view.Gravity.CENTER);
            addRow.addView(addText);

            addRow.setOnClickListener(new android.view.View.OnClickListener({
                onClick: function(v) {
                    dialog.dismiss();
                    showAddKeyDialog();
                }
            }));
            container.addView(addRow);

            var scrollView = new android.widget.ScrollView(ctx);
            scrollView.addView(container);
            builder.setView(scrollView);
            var dialog = builder.show();
            applyDialogRoundCorner(dialog);
        }

        // 新增密钥弹窗（名称+内容一步完成，名称留空则自动生成 key01/key02…）
        function showAddKeyDialog() {
            var kl = getKeyMapAndList();
            var keyMap = kl.map;
            var keyOrder = kl.list.slice();
            var defaultName = getNextKeyCode(keyMap);
        
            var builder = new android.app.AlertDialog.Builder(ctx);
            
            var root = new android.widget.LinearLayout(ctx);
            root.setOrientation(android.widget.LinearLayout.VERTICAL);
            root.setPadding(dipToPx(20), dipToPx(12), dipToPx(20), dipToPx(16));
            root.addView(createDialogTitle("新增密钥"));
        
            function createCardBg() {
                var bg = new android.graphics.drawable.GradientDrawable();
                bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                bg.setCornerRadius(dipToPx(8));
                bg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                bg.setStroke(dipToPx(1.5), android.graphics.Color.parseColor("#10000000"));
                return bg;
            }
        
            var nameLabel = new android.widget.TextView(ctx);
            nameLabel.setText("名称（留空则自动生成 " + defaultName + "）");
            nameLabel.setTextSize(14);
            nameLabel.setTextColor(android.graphics.Color.parseColor("#757575"));
            nameLabel.setPadding(dipToPx(2), 0, 0, dipToPx(4));
            root.addView(nameLabel);
        
            var nameInput = new android.widget.EditText(ctx);
            nameInput.setHint(defaultName);
            nameInput.setTextSize(16);
            nameInput.setSingleLine(true);
            nameInput.setTextColor(android.graphics.Color.parseColor("#333333"));
            nameInput.setPadding(dipToPx(12), dipToPx(10), dipToPx(12), dipToPx(10));
            nameInput.setBackground(createCardBg());
            root.addView(nameInput);
        
            var spacer = new android.view.View(ctx);
            spacer.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dipToPx(12)));
            root.addView(spacer);
        
            var keyLabel = new android.widget.TextView(ctx);
            keyLabel.setText("密钥内容");
            keyLabel.setTextSize(14);
            keyLabel.setTextColor(android.graphics.Color.parseColor("#757575"));
            keyLabel.setPadding(dipToPx(2), 0, 0, dipToPx(4));
            root.addView(keyLabel);
        
            var keyInput = new android.widget.EditText(ctx);
            keyInput.setHint("(智谱API key) 或 (网址@@模型名@@API key)");
            keyInput.setTextSize(16);
            keyInput.setSingleLine(false);
            keyInput.setTextColor(android.graphics.Color.parseColor("#333333"));
            keyInput.setPadding(dipToPx(12), dipToPx(10), dipToPx(12), dipToPx(10));
            keyInput.setBackground(createCardBg());
            root.addView(keyInput);
        
            builder.setView(root);
        
            builder.setPositiveButton("保存", new android.content.DialogInterface.OnClickListener({
                onClick: function(dialog) {
                    var dataKey = keyInput.getText() ? keyInput.getText().toString().trim() : "";
                    if (android.text.TextUtils.isEmpty(dataKey)) {
                        Toast.makeText(ctx, "密钥内容不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    var name = nameInput.getText() ? nameInput.getText().toString().trim() : "";
                    if (android.text.TextUtils.isEmpty(name)) {
                        name = defaultName;
                    }
                    // 名称已存在时提示覆盖确认
                    if (keyMap.hasOwnProperty(name)) {
                        new android.app.AlertDialog.Builder(ctx)
                            .setTitle("覆盖确认")
                            .setMessage("【" + name + "】已存在，是否覆盖？")
                            .setPositiveButton("覆盖", function(d) {
                                keyMap[name] = { keyCode: keyMap[name].keyCode, value: dataKey };
                                if (keyOrder.indexOf(name) === -1) keyOrder.push(name);
                                saveKeyMapToData(keyMap, keyOrder);
                                var curName = (ttsrv.tts.data['currentKeyName'] || '').toString().trim();
                                if (!curName) {
                                    saveKeyToLocal(dataKey);
                                    ttsrv.tts.data['currentKeyName'] = name;
                                }
                                Toast.makeText(ctx, "已保存【" + name + "】", Toast.LENGTH_SHORT).show();
                                d.dismiss();
                                dialog.dismiss();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                        return;
                    }
                    keyMap[name] = { keyCode: defaultName, value: dataKey };
                    if (keyOrder.indexOf(name) === -1) keyOrder.push(name);
                    saveKeyMapToData(keyMap, keyOrder);
                    var curName = (ttsrv.tts.data['currentKeyName'] || '').toString().trim();
                    if (!curName) {
                        saveKeyToLocal(dataKey);
                        ttsrv.tts.data['currentKeyName'] = name;
                    }
                    Toast.makeText(ctx, "已保存【" + name + "】", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            }));
        
            builder.setNegativeButton("取消", function(dialog) { dialog.cancel(); });
            var roundDlg_0 = builder.show();
            applyDialogRoundCorner(roundDlg_0);
        }

        // 输入名称弹窗（保存/覆盖用）
        function showKeyNameDialog(defaultName, dataKey, isEdit, keyMap, onSuccess) {
            var builder = new android.app.AlertDialog.Builder(ctx);
            builder.setTitle(isEdit ? "修改密钥名称" : "输入密钥名称");
            var input = new android.widget.EditText(ctx);
            input.setText(defaultName);
            builder.setView(input);
            builder.setPositiveButton("确定", new android.content.DialogInterface.OnClickListener({
                onClick: function(dialog) {
                    var name = input.getText() ? input.getText().toString().trim() : "";
                    if (android.text.TextUtils.isEmpty(name)) {
                        Toast.makeText(ctx, "名称不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 非编辑模式下，名称已存在时提示覆盖确认
                    if (!isEdit && keyMap.hasOwnProperty(name)) {
                        new android.app.AlertDialog.Builder(ctx)
                            .setTitle("覆盖确认")
                            .setMessage("【" + name + "】已存在，是否覆盖？")
                            .setPositiveButton("覆盖", function(d) {
                                keyMap[name] = { keyCode: keyMap[name].keyCode, value: dataKey };
                                if (onSuccess) onSuccess(name);
                                d.dismiss();
                                dialog.dismiss();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                        return;
                    }
                    if (isEdit) {
                        var oldItem = keyMap[defaultName];
                        delete keyMap[defaultName];
                        keyMap[name] = { keyCode: oldItem.keyCode, value: dataKey };
                    } else {
                        keyMap[name] = { keyCode: defaultName, value: dataKey };
                    }
                    if (onSuccess) onSuccess(name);
                    dialog.dismiss();
                }
            }));
            builder.setNegativeButton("取消", function(dialog) { dialog.cancel(); });
            var roundDlg_1 = builder.show();
            applyDialogRoundCorner(roundDlg_1);
        }
  
        // 辅助函数（保留）
        // 密钥列表持久化文件名
        var KEY_LIST_FILE = "key_list.json";

        // 从持久化存储读取密钥映射（保持添加顺序）
        // 优先读取文件 key_list.json（有序数组格式），回退到 ttsrv.tts.data.keyListJson（旧格式）
        function getKeyMapFromData() {
            if (!ttsrv) ttsrv = {};
            if (!ttsrv.tts) ttsrv.tts = {};
            if (typeof ttsrv.tts.data !== "object") ttsrv.tts.data = {};

            // 优先：从文件读取有序数组格式 [[name, {keyCode, value}], ...]
            try {
                var fileContent = ttsrv.readTxtFile(KEY_LIST_FILE);
                if (fileContent && fileContent.trim() !== "") {
                    var arr = JSON.parse(fileContent);
                    if (Array.isArray(arr)) {
                        var map = {};
                        for (var i = 0; i < arr.length; i++) {
                            if (arr[i] && arr[i].length >= 2) {
                                var n = arr[i][0];
                                var v = arr[i][1];
                                map[n] = v;
                            }
                        }
                        // 同步到 ttsrv.tts.data（兼容旧代码读取）
                        ttsrv.tts.data.keyListJson = JSON.stringify(map);
                        return map;
                    }
                }
            } catch (e) {
                console.error("从文件读取密钥列表失败：" + e.toString());
            }

            // 回退：旧的 ttsrv.tts.data.keyListJson 格式
            var keyListJson = ttsrv.tts.data.keyListJson || "{}";
            try {
                return JSON.parse(keyListJson);
            } catch (e) {
                console.error("解析密钥列表失败：" + e.toString());
                return {};
            }
        }

        // 保存密钥映射到持久化存储（文件 + ttsrv.tts.data 双写）
        // keyOrder: 可选，传入密钥名称顺序数组；未传入则用 keyMap 自身遍历顺序
        function saveKeyMapToData(keyMap, keyOrder) {
            try {
                var order = keyOrder;
                if (!order) {
                    order = [];
                    for (var prop in keyMap) {
                        if (keyMap.hasOwnProperty(prop)) order.push(prop);
                    }
                }
                // 构建有序数组 [[name, value], ...]
                var arr = [];
                for (var i = 0; i < order.length; i++) {
                    var n = order[i];
                    if (keyMap.hasOwnProperty(n)) {
                        arr.push([n, keyMap[n]]);
                    }
                }
                var jsonStr = JSON.stringify(arr);
                // 写入文件（持久化，重启后可恢复）
                ttsrv.writeTxtFile(KEY_LIST_FILE, jsonStr);
                // 同时写入 ttsrv.tts.data（兼容旧代码读取）
                var keyListJson = JSON.stringify(keyMap);
                ttsrv.tts.data.keyListJson = keyListJson;
                console.log("密钥列表已保存到文件（" + arr.length + "项，长度" + jsonStr.length + "）");
            } catch (e) {
                console.error("保存密钥列表失败：" + e.toString());
                Toast.makeText(ctx, "密钥存储失败", Toast.LENGTH_SHORT).show();
            }
        }
  
        function getNextKeyCode(keyMap) {
            var keyCodes = [];
            for (var name in keyMap) {
                if (keyMap[name].keyCode) keyCodes.push(keyMap[name].keyCode);
            }
            if (keyCodes.length === 0) return "key01";
            keyCodes.sort(function(a, b) {
                var numA = parseInt(a.replace("key", ""), 10) || 0;
                var numB = parseInt(b.replace("key", ""), 10) || 0;
                return numA - numB;
            });
            var lastNum = parseInt(keyCodes[keyCodes.length - 1].replace("key", ""), 10) || 0;
            return "key" + (lastNum + 1 < 10 ? "0" + (lastNum + 1) : lastNum + 1);
        }
  
        function saveKeyToLocal(key) {
            try {
                var cleanKey = (key || "").trim();
                ttsrv.writeTxtFile("miyue.txt", cleanKey);
                ttsrv.writeTxtFile("gengxin.txt", cleanKey);
                saveKeyWithMultipleMethods(cleanKey);
                console.log("本地txt已更新（清洗后长度：" + cleanKey.length + "）");
            } catch (e) {
                console.error("本地保存失败：" + e.toString());
                Toast.makeText(ctx, "本地txt保存失败", Toast.LENGTH_SHORT).show();
            }
        }
  
        function saveKeyWithMultipleMethods(key) {
            try {
                ttsrv.writeTxtFile("miyue_backup.txt", key);
                console.log("密钥备份成功");
            } catch (e1) {
                console.error("ttsrv备份失败：" + e1.toString());
                try {
                    if (typeof fs !== "undefined") fs.writeFile("miyue_backup.txt", key);
                } catch (e2) {
                    console.error("fs备份失败：" + e2.toString());
                }
            }
        }
  




        
        var displayMetrics = new android.util.DisplayMetrics();
        var windowManager = ctx.getSystemService(android.content.Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        var density = displayMetrics.density;
        
        var characterRecords = [];
        var characterFilePath = 'characterRecords.json';
        
        try {
            console.log("尝试读取角色数据文件: " + characterFilePath);
            
            try {
                var data = ttsrv.readTxtFile(characterFilePath);
                console.log("使用ttsrv.readTxtFile读取文件成功");
                
                // 空文件或空内容视为空数组（不回退到默认角色数据）
                if (!data || data.trim() === "" || data.trim() === "[]") {
                    characterRecords = [];
                    console.log("角色数据文件为空或无角色，使用空列表");
                } else {
                    characterRecords = JSON.parse(data) || [];
                    // record.voice 保持 tag 原值（如"女青年01"），不做 replaceFayinrenName 转换
                    // 标签显示由 generateVoiceTag 通过 getVoiceByTag(tag) 实时查询当前分组
                }
            } catch (e) {
                console.log("读取角色数据失败: " + e.toString());
                
                // 解析失败时：不回退到默认角色数据，使用空列表（避免已删除的角色复活）
                characterRecords = [];
                console.log("角色数据解析失败，使用空列表（不恢复默认数据）");
            }
            
            console.log("成功解析角色数据，记录数: " + characterRecords.length);
            
            var allNames = [];
            for (var i = 0; i < characterRecords.length; i++) {
                allNames.push(characterRecords[i].name);
            }
            console.log("所有角色名称: " + allNames.join(", "));
        } catch (e) {
            console.error("读取角色数据失败: " + e.toString());
        }
        
        function safeGetName(character) {
            if (character && character.name) {
                return character.name;
            }
            console.warn("角色名称缺失，使用默认值");
            return "未知角色";
        }
        
        
        // ============== 深色模式适配 ==============
        // 检测系统是否处于深色模式（夜间模式）。
        // 使用硬编码值以兼容 Rhino 引擎：
        //   UI_MODE_NIGHT_MASK = 0x30
        //   UI_MODE_NIGHT_YES  = 0x20
        function isNightMode() {
            try {
                var cfg = ctx.getResources().getConfiguration();
                return (cfg.uiMode & 0x30) === 0x20;
            } catch (e) {
                return false;
            }
        }
        // 自适应文字颜色：深色模式用浅色字，浅色模式保持 #333333
        function getAdaptiveTextColor() {
            if (isNightMode()) {
                return android.graphics.Color.parseColor("#E0E0E0");
            }
            return android.graphics.Color.parseColor("#333333");
        }

        // 统一拆分别名字符串：同时支持半角|和全角｜作为分隔符
        // 返回去空、去trim后的数组
        function splitAliases(aliasesStr) {
            if (!aliasesStr || typeof aliasesStr !== "string") return [];
            return aliasesStr.split(/[\|｜]/).map(function(a) { return a.trim(); }).filter(function(a) { return a !== ""; });
        }

        // 生成角色名部分（SpannableStringBuilder，含名字+皇冠+别名）
        function generateDisplayName(character) {
            if (!character) return "无效角色";

            var name = safeGetName(character);
            var ssb = new android.text.SpannableStringBuilder();
            // 深色模式下用浅色字，浅色模式用 #333333
            var CLR_NAME = getAdaptiveTextColor();
            var CLR_BRACKET = getAdaptiveTextColor();
            var SPAN = android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
            
            function appendColored(text, color) {
                var start = ssb.length();
                ssb.append(text);
                if (color !== CLR_NAME) {
                    ssb.setSpan(new android.text.style.ForegroundColorSpan(color), start, ssb.length(), SPAN);
                }
            }
            
            // 合并主名+别名，去重，主名放第一个
            var aliasList = splitAliases(character.aliases);
            var allNames = [name];
            for (var ai = 0; ai < aliasList.length; ai++) {
                if (aliasList[ai] !== name && allNames.indexOf(aliasList[ai]) === -1) {
                    allNames.push(aliasList[ai]);
                }
            }

            // 多个角色合并时：每个名字单独一行，统一圆点前缀（主名/别名待遇相同）
            // 圆点按性别上色：男=青蓝、女=粉红、未知=灰；合并行内同色，列表整体呈现不同彩色
            var g = (character.gender || "");
            var DOT_COLOR = android.graphics.Color.parseColor("#9E9E9E");
            if (g.indexOf("男") !== -1) {
                DOT_COLOR = android.graphics.Color.parseColor("#1976D2");
            } else if (g.indexOf("女") !== -1) {
                DOT_COLOR = android.graphics.Color.parseColor("#E91E63");
            }
            var isFav = (character.usageCount === 50);
            for (var ni = 0; ni < allNames.length; ni++) {
                if (ni > 0) {
                    appendColored("\n", CLR_NAME);
                }
                appendColored("· ", DOT_COLOR);
                if (isFav) {
                    appendColored("【", CLR_BRACKET);
                }
                appendColored(allNames[ni], CLR_NAME);
                if (isFav) {
                    appendColored("】", CLR_BRACKET);
                }
            }

            if (character.age === "主角") {
                appendColored(" 👑", CLR_NAME);
            }
            
            return ssb;
        }

        // 方案B-实时映射：通过 app 接口实时查询配置项是否在当前前台分组启用。
        // character.voice 存的是底层voice（稳定标识），
        // getVoiceByTag(底层voice) 查当前分组，查到说明启用，查不到说明该voice不在当前分组→⚠。
        function isVoiceTagValid(voice) {
            if (!voice) return false;
            try {
                return ttsrv.getVoiceByTag(voice) !== null;
            } catch (e) {
                return false;
            }
        }

        // 生成发音人标签：
        // record.voice 存的是 tag（如"女青年01"），稳定不随分组变化。
        // 用 tag 查 getVoiceByTag（L1匹配 tag==入参），返回当前前台分组该tag绑的配置项displayName（发音人名）。
        // 切分组后重新查 → 自动跟随新分组的发音人名。
        // 查不到（该tag在当前分组无配置项）时显示 tag + ⚠。
        function generateVoiceTag(character) {
            if (!character || !character.voice) return null;

            var voiceTag = String(character.voice);
            var displayText = voiceTag;
            var isValid = false;

            // 用 tag 查当前前台分组的发音人 displayName
            try {
                var liveName = ttsrv.getVoiceByTag(voiceTag);
                if (liveName) {
                    displayText = shortenDisplayName(liveName);
                    isValid = true;
                } else {
                    isValid = false;
                }
            } catch (e) {
                isValid = false;
            }

            var ssb = new android.text.SpannableStringBuilder();
            var CLR_TAG = android.graphics.Color.parseColor("#1976D2");
            var CLR_WARN = android.graphics.Color.parseColor("#D32F2F");
            var SPAN = android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

            var tagStart = ssb.length();
            ssb.append(displayText);
            ssb.setSpan(new android.text.style.ForegroundColorSpan(CLR_TAG), tagStart, ssb.length(), SPAN);

            if (!isValid) {
                var warnStart = ssb.length();
                ssb.append(" ⚠");
                ssb.setSpan(new android.text.style.ForegroundColorSpan(CLR_WARN), warnStart, ssb.length(), SPAN);
            }

            // 追加发音人标记图标（喜欢❤️/路人🚶/坏人😈），emoji 自带颜色，无需额外设色
            try {
                var mk = getVoiceMark(voiceTag);
                if (mk === "like" || mk === "neutral" || mk === "bad") {
                    ssb.append(" ");
                    ssb.append(mk === "like" ? "❤️" : (mk === "bad" ? "😈" : "🚶"));
                }
            } catch (e) {}

            return ssb;
        }

        // 兼容旧调用：返回角色名部分（不含发音人）
        function generateDisplayText(character) {
            if (!character) {
                console.warn("generateDisplayText: 角色对象为空");
                return "无效角色";
            }
            return generateDisplayName(character);
        }


        function splitVoiceDisplay(voiceText) {
            if (!voiceText) return { tag: "", persona: "" };
            // 匹配中文分类词+数字序号前缀
            var match = voiceText.match(/^(.+?\d+)(.*)$/);
            if (match && match[2] && match[2].trim() !== "") {
                return { tag: match[1], persona: match[2] };
            }
            // 无法拆分时整体作为tag
            return { tag: voiceText, persona: "" };
        }

        // 缩减 displayName，保留发音人名：
        // 结构：前缀(分类) · 发音人名 - 后缀(声优本名/游戏来源/空)
        //   如 "鹿·霓商" → "霓商"，"鹿·华月-马海燕" → "华月"，"游音·莱恩哈特-明日方舟" → "莱恩哈特"
        // 其他结构：
        //   发音人名 描述   (如 "爽快思思 女|青年|直率") → 保留空格前的发音人名
        //   名字 [注释]     (如 "玛薇卡 [上新优化]")     → 去[]注释
        // 规则：
        // 1) 去 [xxx] 注释
        // 2) 有 · • ：先按 - 取前段(去后缀)，再按 · • 取最后段(去前缀)，得到发音人名
        // 3) 无 · • ：去空格后的描述部分(保留空格前)，超过 MAX 字则截断
        var DISPLAY_NAME_MAX = 8;
        function shortenDisplayName(name) {
            if (!name) return "";
            var s = String(name).trim();
            // 1) 去括号内的注释/来源（半角[]、全角【】、全角［］、半角/全角圆括号）
            s = s.replace(/\s*[(\[{【［（].*?[)\]}】］）]\s*/g, "").trim();
            if (s === "") return "";

            // 2) 有 · • ：取发音人名（中间段）
            if (/[·•]/.test(s)) {
                // 先按 - 取前段（去声优本名/游戏来源后缀）
                var dashParts = s.split(/[\-—]/);
                var main = dashParts[0].trim();
                // 再按 · • 取最后段（去前缀分类）
                var dotParts = main.split(/[·•]/);
                var last = "";
                for (var i = dotParts.length - 1; i >= 0; i--) {
                    var p = dotParts[i].trim();
                    if (p) { last = p; break; }
                }
                if (last) {
                    if (last.length <= DISPLAY_NAME_MAX) return last;
                    return last.substring(0, DISPLAY_NAME_MAX - 1) + "…";
                }
            }

            // 3) 无 · • ：去空格后的描述部分（保留空格前）
            var spaceIdx = s.indexOf(" ");
            if (spaceIdx > 0) s = s.substring(0, spaceIdx).trim();
            if (s.length <= DISPLAY_NAME_MAX) return s;
            return s.substring(0, DISPLAY_NAME_MAX - 1) + "…";
        }
  
        
        function initializeFileSystem() {
            try {
                console.log("开始初始化文件系统");
                
                // 每次直接从cunfang.txt读取最新书名（支持框架自动切换书籍）
                var currentBookName = "默认";
                try {
                    var cunfangContent = ttsrv.readTxtFile("cunfang.txt");
                    if (cunfangContent && cunfangContent.trim() !== "") {
                        currentBookName = cunfangContent.trim();
                    } else {
                        ttsrv.writeTxtFile("cunfang.txt", "默认");
                    }
                } catch (e) {
                    ttsrv.writeTxtFile("cunfang.txt", "默认");
                }
                ttsrv.tts.data['currentBookName'] = currentBookName;
                console.log("当前书名: [" + currentBookName + "]");
                
                try {
                    var characterData = ttsrv.readTxtFile("characterRecords.json");
                    if (characterData) {
                        var shumingFileName = "shuming." + currentBookName + ".json";
                        ttsrv.writeTxtFile(shumingFileName, characterData);
                        console.log("角色数据已保存到: " + shumingFileName);
                    }
                } catch (e) {
                    console.log("保存shuming文件失败: " + e.toString());
                }
                
                var bookList = [];
                try {
                    var liebiaoData = ttsrv.readTxtFile("liebiao.json");
                    if (liebiaoData) {
                        bookList = JSON.parse(liebiaoData);
                        console.log("读取liebiao.json成功，列表: " + bookList.join(", "));
                    }
                } catch (e) {
                    console.log("liebiao.json读取失败，创建新文件: " + e.toString());
                }
                var needSave = false;
                
                var hasCurrentBook = false;
                for (var i = 0; i < bookList.length; i++) {
                    if (normalizeString(String(bookList[i])) === normalizeString(currentBookName)) {
                        hasCurrentBook = true;
                        break;
                    }
                }
                
                if (!hasCurrentBook && currentBookName !== "默认") {
                    // 当前书名不在列表中，直接追加（保留所有书籍，支持多本书共存）
                    bookList.push(currentBookName);
                    needSave = true;
                    console.log("已添加'" + currentBookName + "'到liebiao.json");
                }
                
                var hasDefault = false;
                for (var i = 0; i < bookList.length; i++) {
                    if (String(bookList[i]).trim() === "默认") {
                        hasDefault = true;
                        break;
                    }
                }
                
                if (!hasDefault) {
                    bookList.push("默认");
                    needSave = true;
                    console.log("已添加'默认'到liebiao.json");
                }
                
                var cleanedList = removeDuplicateBooks(bookList);
                if (cleanedList.length !== bookList.length) {
                    bookList = cleanedList;
                    needSave = true;
                    console.log("已清理重复的书名");
                }
                
                // 注意：此处不再清理"无 shuming 文件的书名"。
                // 框架通过 AI 朗读规则自动生成的书名可能尚未保存 shuming 文件，
                // 误清理会导致多本书被删成一本。旧书名的移除交由重命名/删除流程处理。

                if (needSave) {
                    ttsrv.writeTxtFile("liebiao.json", JSON.stringify(bookList, null, 2));
                    refreshBookListCache(bookList);
                    console.log("已更新liebiao.json");
                }
                
                console.log("文件系统初始化完成");
                
            } catch (e) {
                console.error("文件系统初始化失败: " + e.toString());
            }
        }
        
        function getCurrentBookName() {
            try {
                // 每次直接从cunfang.txt读取最新书名（支持框架自动切换书籍）
                var currentBookName = ttsrv.readTxtFile("cunfang.txt");
                
                if (!currentBookName || currentBookName.trim() === "") {
                    currentBookName = "默认";
                    ttsrv.writeTxtFile("cunfang.txt", currentBookName);
                }
                
                currentBookName = currentBookName.trim();
                // 同步到 tts.data（供其他逻辑使用，但不作为优先读取源）
                ttsrv.tts.data['currentBookName'] = currentBookName;
                return currentBookName;
            } catch (e) {
                console.log("获取当前书名失败，使用默认值: " + e.toString());
                return "默认";
            }
        }
        
        // 内存中的书籍列表缓存
        var _bookListCache = null;

        function getBookList() {
            // 每次直接从 liebiao.json 读取（支持框架自动添加新书名）
            try {
                var liebiaoData = ttsrv.readTxtFile("liebiao.json");
                if (liebiaoData) {
                    var bookList = JSON.parse(liebiaoData);
                    _bookListCache = bookList;
                    // 同步到 tts.data
                    ttsrv.tts.data['bookListData'] = JSON.stringify(bookList);
                    return bookList;
                }
            } catch (e) {
                console.log("读取liebiao.json失败: " + e.toString());
            }
            _bookListCache = ["默认"];
            return ["默认"];
        }

        // 强制刷新书籍列表缓存（写入 liebiao.json 后调用）
        function refreshBookListCache(newList) {
            // 使用 normalizeString 去重，防止大小写/不可见字符差异导致重复
            var dedupedList = [];
            var seenNorm = {};
            for (var i = 0; i < newList.length; i++) {
                var itemNorm = normalizeString(String(newList[i]));
                if (!seenNorm[itemNorm]) {
                    seenNorm[itemNorm] = true;
                    dedupedList.push(newList[i]);
                }
            }
            _bookListCache = dedupedList;
            // 同步到 tts.data（持久化，重启后仍可用）
            ttsrv.tts.data['bookListData'] = JSON.stringify(dedupedList);
            // 同时写入文件（备份）
            try {
                ttsrv.writeTxtFile("liebiao.json", JSON.stringify(dedupedList, null, 2));
            } catch (e) {
                console.log("写入liebiao.json失败（不影响功能，tts.data已更新）: " + e.toString());
            }
            console.log("书籍列表缓存+tts.data已刷新: " + JSON.stringify(dedupedList));
        }
        
        function saveCharacterData() {
            try {
                var currentBookName = getCurrentBookName();
                
                backupOriginalData();
                
  
          // ↓ 纯ES5写法：替换箭头函数+对象解构（手动复制对象属性）
                var saveRecords = [];
                for (var i = 0; i < characterRecords.length; i++) {
                    var char = characterRecords[i];
              // 手动复制所有属性，voice 直接存 tag（与内存一致）
                    saveRecords.push({
                      name: char.name,
                      aliases: char.aliases,
                      voice: char.voice || "",
                      gender: char.gender,
                      age: char.age,
                      usageCount: char.usageCount,
                      genderAgeHistory: char.genderAgeHistory // 保留可能存在的其他字段
                    });
                }
          // ↑ 新增结束
  
  
  
  
  
  // ↓ 修复：序列化 saveRecords 而不是 characterRecords
                var jsonData = JSON.stringify(saveRecords, null, 2);
  // ↑ 修复结束
  
                console.log("准备写入JSON数据，长度: " + jsonData.length);
                
                ttsrv.writeTxtFile("characterRecords.json", jsonData);
                console.log("角色记录已保存到characterRecords.json");
                
                var shumingFileName = "shuming." + currentBookName + ".json";
                ttsrv.writeTxtFile(shumingFileName, jsonData);
                console.log("角色记录已保存到" + shumingFileName);
                
                ttsrv.writeTxtFile("gengxin.json", jsonData);
                console.log("角色记录已保存到gengxin.json");
                
                // 同步更新备份文件，防止框架从旧备份恢复已删除的数据
                ttsrv.writeTxtFile("characterRecords_backup.json", jsonData);
                console.log("角色记录已同步到characterRecords_backup.json");

            } catch (e) {
                console.error("写入文件失败: " + e.toString());
                Toast.makeText(ctx, "保存失败: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
        
          function createGengxinFile() {
            try {
              // 与 saveCharacterData 保持一致：voice 直接存 tag
              var saveRecords = [];
              for (var i = 0; i < characterRecords.length; i++) {
                var char = characterRecords[i];
                // 手动复制属性，voice 直接存 tag
                saveRecords.push({
                  name: char.name,
                  aliases: char.aliases,
                  voice: char.voice || "",
                  gender: char.gender,
                  age: char.age,
                  usageCount: char.usageCount,
                  genderAgeHistory: char.genderAgeHistory // 保留其他字段
                });
              }
              var jsonData = JSON.stringify(saveRecords, null, 2);
              ttsrv.writeTxtFile("gengxin.json", jsonData);
              console.log("已创建/更新gengxin.json文件（已执行反向映射）");
            } catch (e) {
              console.error("创建gengxin.json文件失败: " + e.toString());
            }
          }
          
        
        function backupOriginalData() {
            try {
                console.log("开始备份原始数据");
                
                var currentData = "";
                try {
                    currentData = ttsrv.readTxtFile("characterRecords.json");
                    console.log("成功读取当前文件内容");
                } catch (e) {
                    console.log("无法读取当前文件，使用内存数据备份: " + e.toString());
                    currentData = serializeRecordsForStorage();
                }
                
                ttsrv.writeTxtFile("characterRecords_backup.json", currentData);
                console.log("原始数据备份完成");
                
            } catch (e) {
                console.error("备份原始数据失败: " + e.toString());
            }
        }
        
        // ============================================================
        // 共享 UI 辅助函数（统一弹窗样式标准，与密钥管理一致）
        // ============================================================

        // 卡片背景：#FFFFFF 底色 + 1dp #10000000 半透明边框 + 8dp 圆角
        function createDialogCardBg() {
            var bg = new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dipToPx(8));
            bg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
            bg.setStroke(dipToPx(1), android.graphics.Color.parseColor("#10000000"));
            return bg;
        }


        // 创建小标题TextView（替代AlertDialog默认大标题）
        function createDialogTitle(text) {
            var title = new android.widget.TextView(ctx);
            title.setText(text);
            title.setTextSize(16);
            try { title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); } catch(e) {}
            title.setTextColor(android.graphics.Color.parseColor("#333333"));
            title.setPadding(dipToPx(4), dipToPx(4), dipToPx(4), dipToPx(8));
            return title;
        }

        // 给已显示的弹窗设置圆角背景 + 自适应宽度
        function applyDialogRoundCorner(dialogInstance) {
            try {
                if (!dialogInstance || !dialogInstance.getWindow) return;
                var window = dialogInstance.getWindow();
                if (!window) return;
                // 设置背景圆角drawable
                var bg = new android.graphics.drawable.GradientDrawable();
                bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                bg.setCornerRadius(dipToPx(16));
                bg.setColor(android.graphics.Color.WHITE);
                window.setBackgroundDrawable(bg);
                // 自适应宽度：WRAP_CONTENT，但不超过屏幕85%
                var dm = ctx.getResources().getDisplayMetrics();
                var maxWidth = Math.floor(dm.widthPixels * 0.85);
                var lp = window.getAttributes();
                lp.width = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;
                window.setAttributes(lp);
                // 限制最大宽度
                window.setLayout(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                // 重新获取并限制最大宽度
                var lp2 = window.getAttributes();
                if (lp2.width > maxWidth) {
                    lp2.width = maxWidth;
                    window.setAttributes(lp2);
                }
            } catch (e) {
                console.error("applyDialogRoundCorner失败: " + e.toString());
            }
        }

        // 小按钮：白字 + 彩色圆角背景（13sp, padding 14/6, radius 6dp）
        function createDialogSmallButton(text, color) {
            var btn = new android.widget.TextView(ctx);
            btn.setText(text);
            btn.setTextSize(15);
            btn.setTextColor(android.graphics.Color.parseColor(color));
            btn.setGravity(android.view.Gravity.CENTER);
            btn.setPadding(dipToPx(16), dipToPx(8), dipToPx(16), dipToPx(8));
            var shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadius(dipToPx(8));
            shape.setColor(android.graphics.Color.parseColor("#FFFFFF"));
            shape.setStroke(dipToPx(1), android.graphics.Color.parseColor(color));
            btn.setBackground(shape);
            return btn;
        }

        // 给 TextView 施加圆角彩色按钮样式（用于释放弹窗内的操作按钮）
        function releaseTextBtnStyle(textView, color) {
            textView.setTextColor(android.graphics.Color.parseColor(color));
            textView.setGravity(android.view.Gravity.CENTER);
            textView.setPadding(dipToPx(14), dipToPx(7), dipToPx(14), dipToPx(7));
            var shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadius(dipToPx(8));
            // 透明背景+彩色边框（描边风格）
            shape.setColor(android.graphics.Color.parseColor("#FFFFFF"));
            shape.setStroke(dipToPx(1), android.graphics.Color.parseColor(color));
            textView.setBackground(shape);
            textView.setClickable(true);
        }

        // 文字按钮：彩色文字 + 透明背景（14sp, padding 20/10）
        function createDialogTextButton(text, color) {
            var b = new android.widget.TextView(ctx);
            b.setText(text);
            b.setTextSize(14);
            b.setTextColor(android.graphics.Color.parseColor(color));
            b.setGravity(android.view.Gravity.CENTER);
            b.setPadding(dipToPx(20), dipToPx(10), dipToPx(20), dipToPx(10));
            return b;
        }

        // 标签文字：14sp, #999999, padding 2/0/0/4
        function createDialogLabel(text) {
            var label = new android.widget.TextView(ctx);
            label.setText(text);
            label.setTextSize(14);
            label.setTextColor(android.graphics.Color.parseColor("#757575"));
            label.setPadding(dipToPx(2), 0, 0, dipToPx(4));
            return label;
        }

        // 带样式的输入框：16sp, #333333, padding 12/10, 卡片背景
        function createStyledEditText(hint, singleLine) {
            var input = new android.widget.EditText(ctx);
            if (hint) input.setHint(hint);
            input.setTextSize(16);
            input.setTextColor(android.graphics.Color.parseColor("#333333"));
            input.setHintTextColor(android.graphics.Color.parseColor("#757575"));
            input.setPadding(dipToPx(14), dipToPx(10), dipToPx(14), dipToPx(10));
            input.setBackground(createDialogCardBg());
            if (singleLine !== false) input.setSingleLine(true);
            return input;
        }

        // 圆角行背景：指定底色 + 可选边框
        function createDialogRowBg(bgColor, strokeColor, strokeWidth) {
            var bg = new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dipToPx(8));
            bg.setColor(android.graphics.Color.parseColor(bgColor));
            if (strokeColor && strokeWidth > 0) {
                bg.setStroke(dipToPx(strokeWidth), android.graphics.Color.parseColor(strokeColor));
            }
            return bg;
        }

        // ============================================================
        // ============================================================

        var topButtonsLayout = new android.widget.LinearLayout(ctx);
        topButtonsLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        linearLayout.addView(topButtonsLayout);
        ttsrv.setMargins(topButtonsLayout, 0, 14, 0, 0);

        // 实色填充按钮（鲜艳彩色，白字，统一风格）
        function createSolidButton(text, bgColor) {
            var btn = new android.widget.Button(ctx);
            btn.setText(text);
            btn.setTextSize(13);
            var shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadius(dipToPx(12));
            shape.setColor(android.graphics.Color.parseColor(bgColor));
            btn.setBackground(shape);
            btn.setTextColor(android.graphics.Color.WHITE);
            btn.setAllCaps(false);
            btn.setStateListAnimator(null);
            btn.setPadding(dipToPx(20), dipToPx(12), dipToPx(20), dipToPx(12));
            btn.setTypeface(null, android.graphics.Typeface.BOLD);
            btn.setSingleLine(true);
            var p = new android.widget.LinearLayout.LayoutParams(0, dipToPx(44), 1);
            btn.setLayoutParams(p);
            return btn;
        }

        function createRoundedButton(text, bgColor, textColor) {
            var btn = new android.widget.Button(ctx);
            btn.setText(text);
            btn.setTextSize(15);
            var shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadius(dipToPx(12));
            // 突出但和谐：浅色底 + 彩色描边 + 彩色文字（与书籍卡片风格统一）
            var cInt = android.graphics.Color.parseColor(bgColor);
            var lr = Math.round(android.graphics.Color.red(cInt) * 0.12 + 255 * 0.88);
            var lg = Math.round(android.graphics.Color.green(cInt) * 0.12 + 255 * 0.88);
            var lb = Math.round(android.graphics.Color.blue(cInt) * 0.12 + 255 * 0.88);
            shape.setColor(android.graphics.Color.argb(255, lr, lg, lb));
            shape.setStroke(dipToPx(1.5), cInt);
            btn.setBackground(shape);
            btn.setTextColor(cInt);
            btn.setAllCaps(false);
            btn.setStateListAnimator(null);
            btn.setSingleLine(true);
            btn.setPadding(dipToPx(6), dipToPx(10), dipToPx(6), dipToPx(10));
            btn.setTypeface(null, android.graphics.Typeface.BOLD);
            var p = new android.widget.LinearLayout.LayoutParams(0, dipToPx(48), 1);
            btn.setLayoutParams(p);
            return btn;
        }

        var keyManageBtn = createRoundedButton("🔑  密钥管理", "#1976D2");
                keyManageBtn.getLayoutParams().setMargins(0, 0, dipToPx(8), 0);
        topButtonsLayout.addView(keyManageBtn);

        keyManageBtn.setOnClickListener(new android.view.View.OnClickListener({
            onClick: function(view) {
                showKeyManageDialog();
            }
        }));

        // "新增角色"按钮已移至角色列表下方（+ 添加角色）

        var backupRestoreButton = createRoundedButton("💾  备份恢复", "#3F51B5");
        backupRestoreButton.getLayoutParams().setMargins(dipToPx(8), 0, 0, 0);
        topButtonsLayout.addView(backupRestoreButton);

        // 书籍栏区域（层级1：当前书籍上下文）
        var bookSectionLayout = new android.widget.LinearLayout(ctx);
        bookSectionLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout.addView(bookSectionLayout);
        ttsrv.setMargins(bookSectionLayout, 0, 12, 0, 0);

        // 书名+箭头+修改按钮 融合容器（浅蓝底，视觉权重最高表示上下文层）
        var bookInputLayout = new android.widget.LinearLayout(ctx);
        bookInputLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        bookInputLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        var bookInputParams = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        bookInputLayout.setLayoutParams(bookInputParams);

        // 书籍栏背景（浅蓝底 + 圆角，视觉权重最高表示上下文层）
        var bookBorder = new android.graphics.drawable.GradientDrawable();
        bookBorder.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        bookBorder.setCornerRadius(dipToPx(8));
        bookBorder.setStroke(dipToPx(1), android.graphics.Color.parseColor("#E0E0E0"));
        bookBorder.setColor(android.graphics.Color.parseColor("#E3F2FD"));
        bookInputLayout.setBackground(bookBorder);
        bookInputLayout.setPadding(dipToPx(10), dipToPx(12), dipToPx(8), dipToPx(12));

        // 书名标签
        var bookLabel = new android.widget.TextView(ctx);
        bookLabel.setText("📖");
        bookLabel.setTextSize(14);
        bookLabel.setTextColor(android.graphics.Color.parseColor("#1976D2"));
        bookInputLayout.addView(bookLabel);

        // 书名输入框（透明背景，无边框，默认不可编辑防误触）
        var bookNameEditor = new android.widget.EditText(ctx);
        bookNameEditor.setHint("书名");
        bookNameEditor.setHintTextColor(android.graphics.Color.parseColor("#757575"));
        bookNameEditor.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        bookNameEditor.setTextSize(16);
        bookNameEditor.setHorizontallyScrolling(false);
        bookNameEditor.setMaxLines(2);
        bookNameEditor.setSingleLine(false);
        bookNameEditor.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        bookNameEditor.setTextColor(android.graphics.Color.parseColor("#333333"));
        try { bookNameEditor.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); } catch(e) {}
        bookNameEditor.setFocusable(false);
        bookNameEditor.setFocusableInTouchMode(false);
        var bookEditorParams = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        bookEditorParams.setMargins(8, 0, 0, 0);
        bookNameEditor.setLayoutParams(bookEditorParams);
        bookInputLayout.addView(bookNameEditor);
        
        // "当前"徽章已移除（改用书籍列表弹窗中的选中高亮样式）
        var currentBookTag = null;
        
        // 修改书名按钮（独立小按钮，不藏进列表）
        var editBookBtn = new android.widget.TextView(ctx);
        editBookBtn.setText("✎");
        editBookBtn.setPadding(dipToPx(12), dipToPx(4), dipToPx(12), dipToPx(4));
        editBookBtn.setTextSize(14);
        editBookBtn.setTextColor(android.graphics.Color.parseColor("#757575"));
        editBookBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        var editBookParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        editBookParams.setMargins(0, 0, dipToPx(10), 0);
        editBookBtn.setLayoutParams(editBookParams);
        bookInputLayout.addView(editBookBtn);
        
        // 管理按钮（透明背景，紧贴右边）
        var switchBookButton = new android.widget.TextView(ctx);
        switchBookButton.setText("▾ 管理");
        switchBookButton.setTextSize(14);
        switchBookButton.setPadding(dipToPx(8), dipToPx(4), dipToPx(8), dipToPx(4));
        switchBookButton.setTextColor(android.graphics.Color.parseColor("#1976D2"));
        switchBookButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        var switchBookParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        switchBookButton.setLayoutParams(switchBookParams);
        bookInputLayout.addView(switchBookButton);

        // 完成编辑按钮（编辑模式下显示，点击后结束编辑）
        var doneEditBtn = new android.widget.TextView(ctx);
        doneEditBtn.setText("✓");
        doneEditBtn.setTextSize(14);
        doneEditBtn.setPadding(16, 6, 16, 6);
        doneEditBtn.setTextColor(android.graphics.Color.parseColor("#7E57C2"));
        doneEditBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        doneEditBtn.setVisibility(android.view.View.GONE);
        var doneEditParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        doneEditBtn.setLayoutParams(doneEditParams);
        bookInputLayout.addView(doneEditBtn);

        doneEditBtn.setOnClickListener(new android.view.View.OnClickListener({
            onClick: function(view) {
                endBookEdit();
            }
        }));

        bookSectionLayout.addView(bookInputLayout);


        // ---------------------- 先定义字符串标准化函数（供后续复用） ----------------------
        function normalizeString(str) {
            if (typeof str !== 'string') str = String(str);
            try {
                return str.replace(/[\u200B-\u200D\uFEFF]/g, '').trim().toLowerCase();
            } catch (e) {
                return str.trim().toLowerCase();
            }
        }
  

        // 修复后：通过"标准化角色名"匹配，确保待合并角色被删除
        function refreshCharacterData() {
            try {
                console.log("开始刷新角色列表数据");
                
                // 强制清空映射缓存再重新加载（确保读到最新的性格映射文件）
                _fayinrenMapCache = null;
                _fayinrenReverseMapCache = null;
                _initFayinrenMapCache(true);
                
                // 记录映射表状态，供提示用
                var mapCount = 0;
                if (_fayinrenMapCache) {
                    for (var mk in _fayinrenMapCache) { mapCount++; }
                }
                console.log("性格映射条目数: " + mapCount);
                
                var characterData = ttsrv.readTxtFile("characterRecords.json");
                if (characterData && characterData.trim() !== "") {
                    var parsedData = JSON.parse(characterData);
                    characterRecords = parsedData || [];
                    // record.voice 保持 tag 原值，不做转换

  
  
  
  
  
                    console.log("重新读取角色数据成功，记录数: " + characterRecords.length);
                    
                    // 刷新按钮：角色列表不变，只更新性格标签（不重建行，不闪烁）
                    refreshVoiceTagsOnly();
                    
                    if (mapCount > 0) {
                        Toast.makeText(ctx, "角色列表已刷新（性格映射" + mapCount + "条）", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ctx, "角色列表已刷新（性格映射为空，性格可能不显示）", Toast.LENGTH_LONG).show();
                    }
                } else {
                    console.log("characterRecords.json文件为空");
                    characterRecords = [];
                    refreshCharacterList();
                    Toast.makeText(ctx, "角色数据为空", Toast.LENGTH_SHORT).show();
                }
            } catch (e) {
                console.error("刷新角色数据失败: " + e.toString());
                Toast.makeText(ctx, "刷新失败: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
        
        function initBookSpinner() {
            try {
                var currentBook = getCurrentBookName();
                console.log("初始化书名编辑器，当前书名: [" + currentBook + "]");
                bookNameEditor.setText(currentBook);
            } catch (e) {
                console.error("初始化书名编辑器失败: " + e.toString());
            }
        }
        
        // 书名输入框失去焦点时自动重命名
        bookNameEditor.setOnFocusChangeListener(new android.view.View.OnFocusChangeListener({
            onFocusChange: function(view, hasFocus) {
                if (!hasFocus && isBookEditing) {
                    endBookEdit();
                } else if (hasFocus && isBookEditing) {
                    resetBookEditTimeout();
                }
            }
        }));
        // 编辑框文本变化时重置超时
        bookNameEditor.addTextChangedListener(new android.text.TextWatcher({
            beforeTextChanged: function(s, start, before, count) {},
            onTextChanged: function(s, start, before, count) {
                if (isBookEditing) resetBookEditTimeout();
            },
            afterTextChanged: function(s) {}
        }));
        
        // 回车键自动保存书名（避免输入换行）
        bookNameEditor.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener({
            onEditorAction: function(view, actionId, event) {
                if (actionId === android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                    (event && event.getKeyCode() === android.view.KeyEvent.KEYCODE_ENTER)) {
                    view.clearFocus();
                    return true;
                }
                return false;
            }
        }));
        
        // 弹出书籍列表（书名框和箭头共用）—— 融合管理功能：含当前书、长按删除、新增书籍
        function showBookSwitchDialog() {
            var bookList = getBookList();
            console.log("showBookSwitchDialog 书籍列表: " + JSON.stringify(bookList));
            
            if (!bookList || bookList.length === 0) {
                bookList = ["默认"];
            }
            var currentBookName = getCurrentBookName();
            console.log("当前书名: [" + currentBookName + "]");

            // 去重 + 确保当前书在列表中（使用 normalizeString 容忍大小写/不可见字符差异）
            var displayList = [];
            var seenMap = {};
            for (var i = 0; i < bookList.length; i++) {
                var bn = String(bookList[i]).trim();
                var bnNorm = normalizeString(bn);
                if (bn !== "" && !seenMap[bnNorm]) {
                    seenMap[bnNorm] = true;
                    displayList.push(bn);
                }
            }
            if (!seenMap[normalizeString(currentBookName)]) {
                displayList.unshift(currentBookName);
            }

            var switchBuilder = new android.app.AlertDialog.Builder(ctx);
            var switchScrollView = new android.widget.ScrollView(ctx);
            var switchContainer = new android.widget.LinearLayout(ctx);
            switchContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
            switchContainer.setPadding(dipToPx(16), dipToPx(12), dipToPx(16), dipToPx(16));
            switchContainer.addView(createDialogTitle("书籍列表（点击切换 · 点✕删除）"));

            var switchColors = ["#7E57C2", "#7E57C2", "#26A69A", "#8D6E63", "#66BB6A", "#EC407A", "#FF7043", "#42A5F5"];

            for (var si = 0; si < displayList.length; si++) {
                (function(sBookName, sIdx) {
                    var isCurrent = (normalizeString(sBookName) === normalizeString(currentBookName));
                    var srow = new android.widget.LinearLayout(ctx);
                    srow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    srow.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    var sparams = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    sparams.setMargins(0, dipToPx(4), 0, dipToPx(4));
                    srow.setLayoutParams(sparams);

                    var sbg = new android.graphics.drawable.GradientDrawable();
                    sbg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                    sbg.setCornerRadius(dipToPx(10));
                    if (isCurrent) {
                        sbg.setColor(android.graphics.Color.parseColor("#E3F2FD"));
                        sbg.setStroke(dipToPx(1.5), android.graphics.Color.parseColor("#1976D2"));
                    } else {
                        sbg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                        sbg.setStroke(dipToPx(1), android.graphics.Color.parseColor("#10000000"));
                    }
                    srow.setBackground(sbg);
                    srow.setPadding(dipToPx(14), dipToPx(12), dipToPx(14), dipToPx(12));
                    srow.setClickable(true);

                    // 左侧对勾（仅当前书显示）
                    if (isCurrent) {
                        var checkText = new android.widget.TextView(ctx);
                        checkText.setText("✓");
                        checkText.setTextSize(15);
                        checkText.setTextColor(android.graphics.Color.parseColor("#1976D2"));
                        checkText.setGravity(android.view.Gravity.CENTER);
                        checkText.setPadding(0, 0, dipToPx(8), 0);
                        srow.addView(checkText);
                    } else {
                        var sdot = new android.view.View(ctx);
                        var sdotParams = new android.widget.LinearLayout.LayoutParams(dipToPx(7), dipToPx(7));
                        sdotParams.setMargins(0, 0, dipToPx(8), 0);
                        sdot.setLayoutParams(sdotParams);
                        var sdotBg = new android.graphics.drawable.GradientDrawable();
                        sdotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                        sdotBg.setColor(android.graphics.Color.parseColor(switchColors[sIdx % switchColors.length]));
                        sdot.setBackground(sdotBg);
                        srow.addView(sdot);
                    }

                    var stext = new android.widget.TextView(ctx);
                    stext.setText(sBookName);
                    stext.setTextSize(15);
                    stext.setTextColor(android.graphics.Color.parseColor("#333333"));
                    var stextParams = new android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1
                    );
                    stext.setLayoutParams(stextParams);
                    srow.addView(stext);

                    // 删除图标（默认书籍不可删除，轻量可见式）
                    if (sBookName !== "默认") {
                        var delIcon = new android.widget.TextView(ctx);
                        delIcon.setText("✕");
                        delIcon.setTextSize(14);
                        delIcon.setTextColor(android.graphics.Color.parseColor("#E53935"));
                        delIcon.setPadding(dipToPx(12), dipToPx(4), dipToPx(4), dipToPx(4));
                        delIcon.setOnClickListener(new android.view.View.OnClickListener({
                            onClick: function(view) {
                                var delMsg = "确定删除书籍【" + sBookName + "】？";
                                if (normalizeString(sBookName) === normalizeString(currentBookName)) {
                                    delMsg += "\n删除后将切换到默认书籍";
                                } else {
                                    delMsg += "\n当前书籍不受影响";
                                }
                                new android.app.AlertDialog.Builder(ctx)
                                    .setTitle("删除确认")
                                    .setMessage(delMsg)
                                    .setPositiveButton("删除", function(d) {
                                        if (switchDlg) switchDlg.dismiss();
                                        d.dismiss();
                                        new android.os.Handler(android.os.Looper.getMainLooper()).post(new java.lang.Runnable({
                                            run: function() {
                                                try { deleteMultipleBooks([sBookName]); } catch(de) { console.error("删除书籍失败: " + de.toString()); }
                                            }
                                        }));
                                    })
                                    .setNegativeButton("取消", function(d) { d.cancel(); })
                                    .show();
                            }
                        }));
                        srow.addView(delIcon);
                    }

                    // 单击：切换书籍（当前书点击无操作）
                    srow.setOnClickListener(new android.view.View.OnClickListener({
                        onClick: function(view) {
                            if (isCurrent) return;
                            if (switchDlg) switchDlg.dismiss();
                            var handler = new android.os.Handler(android.os.Looper.getMainLooper());
                            handler.post(new java.lang.Runnable({
                                run: function() {
                                    saveCurrentBookBeforeSwitch(sBookName);
                                }
                            }));
                        }
                    }));

                    switchContainer.addView(srow);
                })(displayList[si], si);
            }

            // 底部操作行：新增书籍 + 全选删除
            var bottomRow = new android.widget.LinearLayout(ctx);
            bottomRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            bottomRow.setGravity(android.view.Gravity.CENTER);
            var bottomRowLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            bottomRowLp.setMargins(0, dipToPx(4), 0, dipToPx(2));
            bottomRow.setLayoutParams(bottomRowLp);

            // + 新增书籍
            var addBookRow = new android.widget.TextView(ctx);
            addBookRow.setText("+ 新增书籍");
            addBookRow.setTextSize(13);
            addBookRow.setTextColor(android.graphics.Color.parseColor("#1976D2"));
            addBookRow.setGravity(android.view.Gravity.CENTER);
            addBookRow.setPadding(dipToPx(16), dipToPx(10), dipToPx(16), dipToPx(6));
            addBookRow.setOnClickListener(new android.view.View.OnClickListener({
                onClick: function(view) {
                    if (switchDlg) switchDlg.dismiss();
                    showInputDialog("请输入新书名", function(inputName) {
                        var newName = (inputName || "").trim();
                        if (newName === "") {
                            Toast.makeText(ctx, "书名不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        var existList = getBookList();
                        for (var ei = 0; ei < existList.length; ei++) {
                            if (String(existList[ei]).trim() === newName) {
                                Toast.makeText(ctx, "书籍【" + newName + "】已存在", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        // 新增书籍：加入列表，创建空数据文件，切换过去
                        existList.push(newName);
                        ttsrv.writeTxtFile("liebiao.json", JSON.stringify(existList, null, 2));
                        refreshBookListCache(existList);
                        ttsrv.writeTxtFile("shuming." + newName + ".json", "[]");
                        try { saveCurrentBookBeforeSwitch(newName); } catch(se) { console.error("切换新书失败: " + se.toString()); }
                        Toast.makeText(ctx, "已新增并切换到【" + newName + "】", Toast.LENGTH_SHORT).show();
                    });
                }
            }));
            bottomRow.addView(addBookRow);

            // 全选删除（打开批量管理弹窗）
            var batchDelRow = new android.widget.TextView(ctx);
            batchDelRow.setText("多选删除");
            batchDelRow.setTextSize(13);
            batchDelRow.setTextColor(android.graphics.Color.parseColor("#EF6C00"));
            batchDelRow.setGravity(android.view.Gravity.CENTER);
            batchDelRow.setPadding(dipToPx(16), dipToPx(10), dipToPx(16), dipToPx(6));
            batchDelRow.setOnClickListener(new android.view.View.OnClickListener({
                onClick: function(view) {
                    if (switchDlg) switchDlg.dismiss();
                    showMultiSelectBookDialog();
                }
            }));
            bottomRow.addView(batchDelRow);

            switchContainer.addView(bottomRow);

            switchScrollView.addView(switchContainer);
            switchBuilder.setView(switchScrollView);
            var switchDlg = switchBuilder.create();
            switchDlg.show();
            applyDialogRoundCorner(switchDlg);
        }
        
        bookNameEditor.setOnClickListener(new android.view.View.OnClickListener({
            onClick: function(view) {
                if (isBookEditing) { return; }
                showBookSwitchDialog();
            }
        }));
        
        switchBookButton.setOnClickListener(new android.view.View.OnClickListener({
            onClick: function(view) { showBookSwitchDialog(); }
        }));
        
        // 修改书名按钮：点击启用书名框编辑
        // 书名编辑状态标记：true=正在编辑，false=未编辑（可切换书籍）
        var isBookEditing = false;
        
        editBookBtn.setClickable(true);
        // 书名编辑超时定时器引用 + 原始书名
        var bookEditTimeoutHandler = null;
        var editingOriginalBook = "";
        // 启动/重置编辑超时（5秒无操作自动结束编辑）
        function resetBookEditTimeout() {
            if (bookEditTimeoutHandler) {
                bookEditTimeoutHandler.removeCallbacks(bookEditTimeoutRunnable);
            }
            bookEditTimeoutHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            bookEditTimeoutHandler.postDelayed(bookEditTimeoutRunnable, 5000);
        }
        var bookEditTimeoutRunnable = new java.lang.Runnable({
            run: function() {
                if (isBookEditing) {
                    endBookEdit();
                }
            }
        });
        editBookBtn.setOnClickListener(new android.view.View.OnClickListener({
            onClick: function(view) {
                // 防止编辑中重复点击覆盖原始书名
                if (isBookEditing) return;
                isBookEditing = true;
                // 记录进入编辑时的原始书名（用于退出时比较，不依赖文件读取）
                editingOriginalBook = bookNameEditor.getText().toString().trim();
                doneEditBtn.setVisibility(android.view.View.VISIBLE);
                bookNameEditor.setEnabled(true);
                bookNameEditor.setFocusable(true);
                bookNameEditor.setFocusableInTouchMode(true);
                bookNameEditor.requestFocus();
                // 延迟设置光标到末尾（requestFocus 后立即 setSelection 可能不生效）
                var textLen = bookNameEditor.getText().length();
                bookNameEditor.setSelection(textLen);
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new java.lang.Runnable({
                    run: function() {
                        try {
                            bookNameEditor.setSelection(bookNameEditor.getText().length());
                        } catch (e) {}
                    }
                }), 100);
                var imm = ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(bookNameEditor, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                resetBookEditTimeout();
            }
        }));

        // 统一结束书名编辑的函数（所有退出路径共用）
        function endBookEdit() {
            if (!isBookEditing) return;
            // 清除超时定时器
            if (bookEditTimeoutHandler) {
                try { bookEditTimeoutHandler.removeCallbacks(bookEditTimeoutRunnable); } catch(e) {}
                bookEditTimeoutHandler = null;
            }
            try {
                var imm = ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(bookNameEditor.getWindowToken(), 0);
            } catch(e) {}
            var newBookName = bookNameEditor.getText().toString().trim();
            // 用进入编辑时的原始书名比较（不依赖 getCurrentBookName 文件读取，避免误判）
            // 仅当书名真正变化且非空时才重命名（避免没改动时提示重命名成功）
            if (newBookName !== "" && newBookName !== editingOriginalBook) {
                renameCurrentBook(newBookName);
            } else if (newBookName === "") {
                // 空名恢复原书名
                bookNameEditor.setText(editingOriginalBook);
            }
            isBookEditing = false;
            doneEditBtn.setVisibility(android.view.View.GONE);
            bookNameEditor.setFocusable(false);
            bookNameEditor.setFocusableInTouchMode(false);
            bookNameEditor.clearFocus();
        }

        // 判断触点是否在指定 view 范围内（Rhino 兼容：用 Rect + getGlobalVisibleRect）
        function renameCurrentBook(newBookName) {
            newBookName = String(newBookName).trim();
            var currentBook = getCurrentBookName();
            console.log("renameCurrentBook: currentBook=[" + currentBook + "], newBookName=[" + newBookName + "]");
            if (newBookName === currentBook) {
                console.log("书名未变化，跳过");
                return;
            }
            if (newBookName === "") {
                console.log("新书名为空，恢复原书名");
                bookNameEditor.setText(currentBook);
                return;
            }
            try {
                // === 第一阶段：关键操作（立即执行，确保下拉栏马上更新）===

                // 1. 更新 cunfang.txt 和 tts.data
                ttsrv.writeTxtFile("cunfang.txt", newBookName);
                ttsrv.tts.data['currentBookName'] = newBookName;

                // 2. 更新书名显示框
                bookNameEditor.setText(newBookName);

                // 3. 构建新书名列表（彻底移除旧名，添加新名）
                var bookList = getBookList();
                console.log("重命名前书籍列表: " + JSON.stringify(bookList) + ", currentBook=[" + currentBook + "]");
                var finalList = [];
                var currentBookNorm = normalizeString(currentBook);
                var newBookNameNorm = normalizeString(newBookName);
                for (var i = 0; i < bookList.length; i++) {
                    var bn = String(bookList[i]).trim();
                    var bnNorm = normalizeString(bn);
                    // 移除旧名（normalize 比较，容忍大小写/不可见字符差异）
                    if (bnNorm === currentBookNorm) {
                        continue;
                    }
                    // 避免新名重复
                    if (bnNorm === newBookNameNorm) {
                        continue;
                    }
                    finalList.push(bookList[i]);
                }
                // 添加新名
                finalList.push(newBookName);
                // 确保"默认"在列表中
                var hasDefault = false;
                for (var hd = 0; hd < finalList.length; hd++) {
                    if (String(finalList[hd]).trim() === "默认") { hasDefault = true; break; }
                }
                if (!hasDefault) { finalList.push("默认"); }

                // refreshBookListCache 会同时更新 _bookListCache + tts.data + liebiao.json
                refreshBookListCache(finalList);
                console.log("第一阶段完成: 书籍列表已更新为: " + JSON.stringify(finalList));

                // 立即显示成功提示
                Toast.makeText(ctx, "已重命名为「" + newBookName + "」", Toast.LENGTH_SHORT).show();

                // === 第二阶段：耗时操作（延后执行，不阻塞 UI）===
                var handler = new android.os.Handler(android.os.Looper.getMainLooper());
                handler.postDelayed(new java.lang.Runnable({
                    run: function() {
                        try {
                            // 迁移 shuming 文件
                            var oldShumingFile = "shuming." + currentBook + ".json";
                            var newShumingFile = "shuming." + newBookName + ".json";
                            var bookData = "[]";
                            try {
                                var rawData = ttsrv.readTxtFile(oldShumingFile);
                                if (rawData && rawData.trim() !== "") {
                                    bookData = rawData;
                                }
                            } catch (eRead) {
                                // voice 直接存 tag（与内存一致）
                                var saveRecords = [];
                                for (var si = 0; si < characterRecords.length; si++) {
                                    var char = characterRecords[si];
                                    if (char) {
                                        saveRecords.push({
                                            name: char.name || "",
                                            aliases: char.aliases || "",
                                            voice: char.voice || "",
                                            gender: char.gender || "",
                                            age: char.age || "",
                                            usageCount: char.usageCount || 0,
                                            genderAgeHistory: char.genderAgeHistory || {}
                                        });
                                    }
                                }
                                bookData = JSON.stringify(saveRecords, null, 2);
                            }
                            ttsrv.writeTxtFile(newShumingFile, bookData);
                            try {
                                ttsrv.deleteFile(oldShumingFile);
                            } catch (e) {
                                try { ttsrv.writeTxtFile(oldShumingFile, "[]"); } catch(e2) {}
                            }

                            // 同步 characterRecords.json 和备份
                            ttsrv.writeTxtFile("characterRecords.json", bookData);
                            ttsrv.writeTxtFile("characterRecords_backup.json", bookData);

                            // 刷新角色列表
                            refreshCharacterList("");
                            // 更新下拉栏
                            initBookSpinner();

                            console.log("第二阶段完成: shuming迁移 + 角色列表刷新");
                        } catch (e2) {
                            console.error("第二阶段出错: " + e2.toString());
                        }
                    }
                }), 50);

            } catch (e) {
                console.error("修改书名失败: " + e.toString());
                Toast.makeText(ctx, "修改失败: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
  
        // ============== 新增：切换前保存当前书籍数据 ==============
        function saveCurrentBookBeforeSwitch(newBookName) {
            try {
                console.log("开始保存当前书籍数据");
                var characterData = "";
                try {
                    characterData = ttsrv.readTxtFile("characterRecords.json");
                    console.log("重新读取characterRecords.json成功");
                } catch (e) {
                    console.log("重新读取characterRecords.json失败: " + e.toString());
                    characterData = serializeRecordsForStorage();
                }
                var currentBookName = getCurrentBookName();
                if (currentBookName && currentBookName.trim() !== "") {
                    var shumingFileName = "shuming." + currentBookName + ".json";
                    ttsrv.writeTxtFile(shumingFileName, characterData);
                    console.log("当前书籍数据已保存到: " + shumingFileName);
                    createGengxinFile();
                } else {
                    console.log("当前书名为空，跳过保存");
                }
                useBook(newBookName);
            } catch (e) {
                console.error("保存当前书籍数据失败: " + e.toString());
                useBook(newBookName);
            }
        }
        // ============== 新增结束 ==============
  
        // ============== 新增：加载目标书籍数据 ==============
        function useBook(newBookName) {
            try {
                console.log("开始使用书籍: [" + newBookName + "]");
                ttsrv.writeTxtFile("cunfang.txt", newBookName);
                ttsrv.tts.data['currentBookName'] = newBookName;
                console.log("已更新cunfang.txt: " + newBookName);
                var shumingFileName = "shuming." + newBookName + ".json";
                console.log("尝试读取书籍文件: " + shumingFileName);
                try {
                    var bookData = ttsrv.readTxtFile(shumingFileName);
                    if (bookData && bookData.trim() !== "") {
                        console.log("成功读取书籍文件，长度: " + bookData.length);
                        try {
                            var parsedData = JSON.parse(bookData);
                            characterRecords = parsedData || [];
                            console.log("成功解析书籍数据，角色数量: " + characterRecords.length);
                            // record.voice 保持 tag 原值，不做转换
                            ttsrv.writeTxtFile("characterRecords.json", bookData);
                            console.log("已更新characterRecords.json");
                            createGengxinFile();
                            refreshCharacterList();
                            bookNameEditor.setText(newBookName);
                            Toast.makeText(ctx, "已切换到书籍: " + newBookName, Toast.LENGTH_SHORT).show();
                        } catch (parseError) {
                            console.error("解析书籍数据失败: " + parseError.toString());
                            Toast.makeText(ctx, "书籍数据格式错误", Toast.LENGTH_SHORT).show();
                            characterRecords = [];
                            createGengxinFile();
                            refreshCharacterList();
                        }
                    } else {
                        console.log("书籍文件为空或不存在");
                        characterRecords = [];
                        ttsrv.writeTxtFile("characterRecords.json", "[]");
                        createGengxinFile();
                        refreshCharacterList();
                        Toast.makeText(ctx, "书籍文件为空，已清空角色数据", Toast.LENGTH_SHORT).show();
                    }
                } catch (e) {
                    console.log("读取书籍文件失败: " + e.toString());
                    characterRecords = [];
                    ttsrv.writeTxtFile("characterRecords.json", "[]");
                    createGengxinFile();
                    refreshCharacterList();
                    Toast.makeText(ctx, "读取书籍失败，已清空角色数据", Toast.LENGTH_SHORT).show();
                }
            } catch (e) {
                console.error("使用书籍失败: " + e.toString());
                Toast.makeText(ctx, "操作失败: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
        // ============== 新增结束 ==============
        
        initBookSpinner();


        // "添加角色"点击逻辑已移至 addCharacterRow 创建处（角色列表下方）

  
        // 注：以下依赖函数无需修改（原代码已存在，仅需确保调用正常）
        // 1. saveCharacterData()：原角色数据保存函数，负责写入 characterRecords.json 和书籍文件
        // 2. createGengxinFile()：原函数，负责更新 gengxin.json
        // 3. refreshCharacterList()：原函数，负责刷新角色列表UI
        // 4. showInputDialog()：原函数，负责显示单行输入弹窗
        
        
        
                
        backupRestoreButton.setOnClickListener(new android.view.View.OnClickListener({
            onClick: function(view) {
                showBackupRestoreDialog();
            }
        }));
        
        function showInputDialog(title, callback, defaultValue) {
            try {
                var builder = new android.app.AlertDialog.Builder(ctx);
                
                var container = new android.widget.LinearLayout(ctx);
                container.setOrientation(android.widget.LinearLayout.VERTICAL);
                container.setPadding(dipToPx(20), dipToPx(12), dipToPx(20), dipToPx(16));
                container.addView(createDialogTitle(title));
                
                var input = createStyledEditText(null, true);
                if (defaultValue !== undefined && defaultValue !== null && defaultValue !== "") {
                    var safeValue = String(defaultValue);
                    input.setText(safeValue);
                    input.setSelection(safeValue.length);
                }
                container.addView(input);
                builder.setView(container);
                
                builder.setPositiveButton("确定", new android.content.DialogInterface.OnClickListener({
                    onClick: function(dialog, which) {
                        var inputText = input.getText().toString();
                        if (callback) {
                            callback(inputText);
                        }
                    }
                }));
                
                builder.setNegativeButton("取消", new android.content.DialogInterface.OnClickListener({
                    onClick: function(dialog, which) {
                        dialog.cancel();
                    }
                }));
                
                var inputDlg = builder.show();
                applyDialogRoundCorner(inputDlg);
            } catch (e) {
                console.error("showInputDialog 异常：" + e.toString());
                Toast.makeText(ctx, "对话框异常：" + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
        
        function showMultiLineInputDialog(title, callback) {
            try {
                var builder = new android.app.AlertDialog.Builder(ctx);
                builder.setTitle(title);
                
                var container = new android.widget.LinearLayout(ctx);
                container.setOrientation(android.widget.LinearLayout.VERTICAL);
                container.setPadding(dipToPx(20), dipToPx(16), dipToPx(20), dipToPx(16));
                
                var input = createStyledEditText(null, false);
                input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                input.setLines(8);
                input.setMaxLines(15);
                
                container.addView(input);
                builder.setView(container);
                
                builder.setPositiveButton("确定", new android.content.DialogInterface.OnClickListener({
                    onClick: function(dialog, which) {
                        var inputText = input.getText().toString();
                        if (callback) {
                            callback(inputText);
                        }
                    }
                }));
                
                builder.setNegativeButton("取消", new android.content.DialogInterface.OnClickListener({
                    onClick: function(dialog, which) {
                        dialog.cancel();
                    }
                }));
                
                var dialog = builder.create();
                dialog.show();
                
            } catch (e) {
                console.error("显示多行输入对话框失败: " + e.toString());
                showInputDialog(title + " (使用单行输入)", callback);
            }
        }
        
        function deleteMultipleBooks(booksToDelete) {
            try {
                // 统一 trim + normalize（去除不可见字符，统一小写）
                var normalizedDelete = [];
                for (var t = 0; t < booksToDelete.length; t++) {
                    normalizedDelete.push(normalizeString(booksToDelete[t]));
                }

                var bookList = getBookList();
                var newBookList = [];

                for (var i = 0; i < bookList.length; i++) {
                    var bnNorm = normalizeString(bookList[i]);
                    if (normalizedDelete.indexOf(bnNorm) === -1) {
                        newBookList.push(bookList[i]);
                    }
                }

                newBookList = removeDuplicateBooks(newBookList);

                var currentBook = getCurrentBookName();
                var currentBookNorm = normalizeString(currentBook);
                console.log("当前书籍: [" + currentBook + "] normalized: [" + currentBookNorm + "]");
                console.log("要删除的书籍(normalized): " + JSON.stringify(normalizedDelete));

                // 用 normalize 后的值比较，避免不可见字符导致匹配失败
                var isCurrentBookDeleted = (normalizedDelete.indexOf(currentBookNorm) !== -1);
                console.log("当前书籍是否被删除: " + isCurrentBookDeleted);

                // 关闭批量删除弹窗
                if (multiSelectBookDialog) {
                    try { multiSelectBookDialog.dismiss(); } catch(de) {}
                    multiSelectBookDialog = null;
                }

                if (isCurrentBookDeleted) {
                    console.log("当前书籍被删除，开始切换到默认书籍");

                    // 1. 写 cunfang.txt 为默认
                    ttsrv.writeTxtFile("cunfang.txt", "默认");
                    ttsrv.tts.data['currentBookName'] = "默认";
                    console.log("已立即更新cunfang.txt为默认");

                    // 2. 加载默认书籍角色数据
                    try {
                        var defaultData = ttsrv.readTxtFile("shuming.默认.json");
                        if (defaultData && defaultData.trim() !== "" && defaultData.trim() !== "[]") {
                            ttsrv.writeTxtFile("characterRecords.json", defaultData);
                            ttsrv.writeTxtFile("gengxin.json", defaultData);
                            characterRecords = JSON.parse(defaultData) || [];
                            // record.voice 保持 tag 原值，不做转换
                            console.log("内存数据已更新为默认数据，角色数量: " + characterRecords.length);
                        } else {
                            var emptyData = "[]";
                            ttsrv.writeTxtFile("characterRecords.json", emptyData);
                            ttsrv.writeTxtFile("gengxin.json", emptyData);
                            characterRecords = [];
                            console.log("默认文件为空或不存在，已清空数据");
                        }
                    } catch (e) {
                        console.log("读取默认书籍失败，创建空数据: " + e.toString());
                        var emptyData = "[]";
                        ttsrv.writeTxtFile("characterRecords.json", emptyData);
                        ttsrv.writeTxtFile("gengxin.json", emptyData);
                        characterRecords = [];
                    }
                }

                // 3. 更新 liebiao.json（不管是否删当前书都要更新）
                ttsrv.writeTxtFile("liebiao.json", JSON.stringify(newBookList, null, 2));
                refreshBookListCache(newBookList);
                console.log("已更新liebiao.json，新列表: " + JSON.stringify(newBookList));

                // 4. 删除被删书籍的 shuming 文件（用原始书名构建文件名）
                for (var fi = 0; fi < booksToDelete.length; fi++) {
                    var bookName = String(booksToDelete[fi]).trim();
                    var shumingFileName = "shuming." + bookName + ".json";
                    try {
                        var deleteResult = ttsrv.deleteFile(shumingFileName);
                        if (deleteResult) {
                            console.log("已删除文件: " + shumingFileName);
                        } else {
                            console.log("文件不存在或删除失败，覆写为空: " + shumingFileName);
                            ttsrv.writeTxtFile(shumingFileName, "[]");
                        }
                    } catch (e) {
                        console.log("删除文件失败，尝试覆写为空: " + e.toString());
                        try {
                            ttsrv.writeTxtFile(shumingFileName, "[]");
                        } catch (e2) {
                            console.error("覆写空数据也失败: " + e2.toString());
                        }
                    }
                }

                // 5. 刷新角色列表（仅删当前书时需要重新加载角色）
                if (isCurrentBookDeleted) {
                    filteredCharRefs_backup = [];
                    refreshCharacterList("");
                    Toast.makeText(ctx, "已删除" + booksToDelete.length + "个书籍，已切换到默认书籍", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ctx, "已删除" + booksToDelete.length + "个书籍，当前书籍不变", Toast.LENGTH_SHORT).show();
                }

                // 6. 最后统一刷新书名显示（确保书籍栏正确）
                initBookSpinner();

            } catch (e) {
                console.error("删除多个书籍失败: " + e.toString());
                Toast.makeText(ctx, "删除失败: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
        
        // 批量删除弹窗引用（供 deleteMultipleBooks 关闭用）
        var multiSelectBookDialog = null;
        
        function showMultiSelectBookDialog() {
            try {
                var bookList = getBookList();
                if (bookList.length === 0) {
                    Toast.makeText(ctx, "书籍列表为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                var currentBookName = getCurrentBookName();
                var checkedItems = new Array(bookList.length);
                for (var i = 0; i < checkedItems.length; i++) {
                    checkedItems[i] = false;
                }
                
                // 用 LinearLayout 替代 BaseAdapter+ListView（Rhino不支持new抽象类）
                var scrollView = new android.widget.ScrollView(ctx);
                var listContainer = new android.widget.LinearLayout(ctx);
                listContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
                listContainer.setPadding(dipToPx(16), dipToPx(12), dipToPx(16), dipToPx(16));
                listContainer.addView(createDialogTitle("批量删除书籍"));

                var rowRefs = []; // 保存每行引用，用于刷新checkbox状态

                for (var bi = 0; bi < bookList.length; bi++) {
                    (function(position) {
                        var row = new android.widget.LinearLayout(ctx);
                        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                        var rowParams = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        rowParams.setMargins(0, 0, 0, dipToPx(3));
                        row.setLayoutParams(rowParams);

                        // 圆角卡片背景
                        var cardBg = new android.graphics.drawable.GradientDrawable();
                        cardBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                        cardBg.setCornerRadius(dipToPx(8));
                        var isCurrent = String(bookList[position]).trim() === currentBookName;
                        if (isCurrent) {
                            cardBg.setColor(android.graphics.Color.parseColor("#E3F2FD"));
                            cardBg.setStroke(dipToPx(1), android.graphics.Color.parseColor("#10000000"));
                        } else {
                            cardBg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                            cardBg.setStroke(dipToPx(1), android.graphics.Color.parseColor("#10000000"));
                        }
                        row.setBackground(cardBg);
                        row.setPadding(dipToPx(14), dipToPx(12), dipToPx(14), dipToPx(12));
                        row.setClickable(true);

                        // Checkbox
                        var cb = new android.widget.CheckBox(ctx);
                        cb.setChecked(checkedItems[position]);
                        cb.setClickable(false);
                        cb.setFocusable(false);
                        var cbParams = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        cbParams.setMargins(0, 0, dipToPx(10), 0);
                        cb.setLayoutParams(cbParams);
                        row.addView(cb);

                        // 书名
                        var nameText = new android.widget.TextView(ctx);
                        nameText.setText(bookList[position]);
                        nameText.setTextSize(15);
                        nameText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                        nameText.setTextColor(android.graphics.Color.parseColor("#333333"));
                        var textParams = new android.widget.LinearLayout.LayoutParams(
                            0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1
                        );
                        nameText.setLayoutParams(textParams);
                        row.addView(nameText);

                        // 当前书标签
                        if (isCurrent) {
                            var tagText = new android.widget.TextView(ctx);
                            tagText.setText("当前");
                            tagText.setTextSize(12);
                            tagText.setTextColor(android.graphics.Color.WHITE);
                            tagText.setGravity(android.view.Gravity.CENTER);
                            tagText.setPadding(dipToPx(8), dipToPx(2), dipToPx(8), dipToPx(2));
                            var tagBg = new android.graphics.drawable.GradientDrawable();
                            tagBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                            tagBg.setCornerRadius(dipToPx(10));
                            tagBg.setColor(android.graphics.Color.parseColor("#1976D2"));
                            tagText.setBackground(tagBg);
                            row.addView(tagText);
                        }

                        // 点击行切换选中状态
                        row.setOnClickListener(new android.view.View.OnClickListener({
                            onClick: function(view) {
                                checkedItems[position] = !checkedItems[position];
                                cb.setChecked(checkedItems[position]);
                            }
                        }));

                        rowRefs.push({ row: row, cb: cb });
                        listContainer.addView(row);
                    })(bi);
                }

                scrollView.addView(listContainer);

                var builder = new android.app.AlertDialog.Builder(ctx);
                builder.setView(scrollView);

                builder.setPositiveButton("删除选中", new android.content.DialogInterface.OnClickListener({
                    onClick: function(dialog, which) {
                        var booksToDelete = [];
                        for (var i = 0; i < checkedItems.length; i++) {
                            if (checkedItems[i]) {
                                booksToDelete.push(bookList[i]);
                            }
                        }
                        if (booksToDelete.length > 0) {
                            deleteMultipleBooks(booksToDelete);
                        } else {
                            Toast.makeText(ctx, "未选择任何书籍", Toast.LENGTH_SHORT).show();
                        }
                    }
                }));

                builder.setNeutralButton("全选", null);
                builder.setNegativeButton("取消", new android.content.DialogInterface.OnClickListener({
                    onClick: function(dialog, which) {
                        dialog.cancel();
                    }
                }));

                multiSelectBookDialog = builder.create();
                var bookDialog = multiSelectBookDialog;
                bookDialog.setOnShowListener(new android.content.DialogInterface.OnShowListener({
                    onShow: function(dialog) {
                        var neutralBtn = bookDialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL);
                        neutralBtn.setOnClickListener(new android.view.View.OnClickListener({
                            onClick: function(view) {
                                var allChecked = true;
                                for (var i = 0; i < checkedItems.length; i++) {
                                    if (!checkedItems[i]) { allChecked = false; break; }
                                }
                                if (allChecked) {
                                    for (var i = 0; i < checkedItems.length; i++) {
                                        checkedItems[i] = false;
                                    }
                                    neutralBtn.setText("全选");
                                } else {
                                    for (var i = 0; i < checkedItems.length; i++) {
                                        checkedItems[i] = true;
                                    }
                                    neutralBtn.setText("取消全选");
                                }
                                // 刷新所有checkbox状态
                                for (var ri = 0; ri < rowRefs.length; ri++) {
                                    rowRefs[ri].cb.setChecked(checkedItems[ri]);
                                }
                            }
                        }));
                    }
                }));

                bookDialog.show();
                applyDialogRoundCorner(bookDialog);
                
            } catch (e) {
                console.error("显示多选书籍对话框失败: " + e.toString());
                Toast.makeText(ctx, "显示书籍列表失败: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
        
        
        
        
        
        
        // 含"设置自动备份"选项的备份恢复对话框函数
        function showBackupRestoreDialog() {
            var optionConfigs = [
                { text: "导出当前书籍到剪贴板", color: "#1976D2", icon: "" },
                { text: "从剪贴板导入书籍", color: "#00838F", icon: "" },
                { text: "备份全部书籍", color: "#2E7D32", icon: "" },
                { text: "从备份完整还原", color: "#F57F17", icon: "" },
                { text: "启用自动备份", color: "#7B1FA2", icon: "" }
            ];
            
            var container = new android.widget.LinearLayout(ctx);
            container.setOrientation(android.widget.LinearLayout.VERTICAL);
            container.setPadding(dipToPx(16), dipToPx(12), dipToPx(16), dipToPx(32));
            
            for (var i = 0; i < optionConfigs.length; i++) {
                (function(cfg, index) {
                    var row = new android.widget.LinearLayout(ctx);
                    row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    var rowParams = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    rowParams.setMargins(0, dipToPx(4), 0, dipToPx(4));
                    row.setLayoutParams(rowParams);
                    
                    var bg = new android.graphics.drawable.GradientDrawable();
                    bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                    bg.setCornerRadius(dipToPx(10));
                    bg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                    bg.setStroke(dipToPx(1), android.graphics.Color.parseColor("#10000000"));
                    row.setBackground(bg);
                    row.setPadding(dipToPx(14), dipToPx(12), dipToPx(14), dipToPx(12));
                    row.setClickable(true);
                    
                    // 简约彩色圆点点缀（保留色彩辨识度）
                    var accentDot = new android.view.View(ctx);
                    var dotParams = new android.widget.LinearLayout.LayoutParams(dipToPx(7), dipToPx(7));
                    dotParams.setMargins(0, 0, dipToPx(8), 0);
                    accentDot.setLayoutParams(dotParams);
                    var dotBg = new android.graphics.drawable.GradientDrawable();
                    dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                    dotBg.setColor(android.graphics.Color.parseColor(cfg.color));
                    accentDot.setBackground(dotBg);
                    row.addView(accentDot);
                    
                    var iconView = new android.widget.TextView(ctx);
                    iconView.setText(cfg.icon);
                    iconView.setTextSize(16);
                    iconView.setTextColor(android.graphics.Color.WHITE);
                    var iconParams = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    iconParams.setMargins(0, 0, dipToPx(12), 0);
                    iconView.setLayoutParams(iconParams);
                    row.addView(iconView);
                    
                    var textView = new android.widget.TextView(ctx);
                    textView.setText(cfg.text);
                    textView.setTextSize(15);
                    textView.setTextColor(android.graphics.Color.parseColor("#333333"));
                    var textParams = new android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1
                    );
                    textView.setLayoutParams(textParams);
                    row.addView(textView);
                    
                    row.setOnClickListener(new android.view.View.OnClickListener({
                        onClick: function(view) {
                            backupRestoreDialog.dismiss();
                            switch (index) {
                                case 0: backupToClipboard(); break;
                                case 1: restoreFromText(); break;
                                case 2: backupAllFilesToData(); break;
                                case 3: restoreAllFilesFromData(); break;
                                case 4: showAutoBackupSettingDialog(); break;
                            }
                        }
                    }));
                    
                    container.addView(row);
                })(optionConfigs[i], i);
            }
            
            var builder = new android.app.AlertDialog.Builder(ctx);
            container.addView(createDialogTitle("备份恢复"), 0);
            builder.setView(container);
            
            backupRestoreDialog = builder.create();
            // 圆角在show后应用
            backupRestoreDialog.show();
            applyDialogRoundCorner(backupRestoreDialog);
        }
        // 修复：确保状态读取准确（处理异常格式+安全初始化层级），解决显示未开启问题
        function showAutoBackupSettingDialog() {
            // 1. 安全初始化层级（避免ttsrv.tts.data未定义导致读取失败）
            if (!ttsrv.tts || typeof ttsrv.tts !== "object") ttsrv.tts = {};
            if (!ttsrv.tts.data || typeof ttsrv.tts.data !== "object") ttsrv.tts.data = {};
            
            // 2. 读取并清洗状态（处理空格、null、undefined等异常，确保"1"能被正确识别）
            var rawState = ttsrv.tts.data.autoBackupEnable;
            var cleanedState = String(rawState || "").trim(); // 强制转字符串+去空格
            var currentState = cleanedState === "1" ? "1" : "0"; // 仅基于清洗后的值判断
            
            var dialogTitle = currentState === "1" 
                ? "当前自动备份：开启（选择操作）" 
                : "当前自动备份：关闭（选择操作）";
            
            var builder = new android.app.AlertDialog.Builder(ctx);
            builder.setTitle(dialogTitle);
            var settingOptions = ["开启自动备份", "关闭自动备份"];
            
            builder.setItems(settingOptions, new android.content.DialogInterface.OnClickListener({
                onClick: function(dialog, which) {
                    // 明确存储为纯净String类型（无空格，避免后续读取异常）
                    var newState = which === 0 ? "1" : "0";
                    ttsrv.tts.data.autoBackupEnable = newState;
                    
                    // 提示结果
                    Toast.makeText(ctx, 
                        newState === "1" 
                            ? "自动备份已开启（下次启动初始化时自动执行）" 
                            : "自动备份已关闭（下次启动初始化时不执行）", 
                        Toast.LENGTH_SHORT
                    ).show();
                    console.log("自动备份状态更新：" + (newState === "1" ? "开启" : "关闭") + "（存储类型：String，值：" + newState + "）");
                    dialog.dismiss();
                }
            }));
            var roundDlg_2 = builder.show();
            applyDialogRoundCorner(roundDlg_2);
        }
  
  
        function backupAllFilesToData() {
                try {
                        if (!ttsrv.tts.data || typeof ttsrv.tts.data !== "object") ttsrv.tts.data = {};
                        // 1. 初始化核心文件集合
                        var allFilesData = {
                                "characterRecords.json": "", "liebiao.json": "",
                                "miyue.txt": "", "gengxin.json": "", "cunfang.txt": ""
                        };
  
                        // 2. 处理核心文件（含 characterRecords.json 过滤）
                        for (var fileName in allFilesData) {
                                try {
                                        var fileContent = ttsrv.readTxtFile(fileName) || "";
                                        // 角色数据文件：过滤 genderAgeHistory + 紧凑JSON
                                        if (fileName === "characterRecords.json") {
                                                if (fileContent.trim() !== "") {
                                                        var characterList = JSON.parse(fileContent);
                                                        var filteredList = [];
                                                        for (var fi = 0; fi < characterList.length; fi++) {
                                                                var ch = characterList[fi];
                                                                if (ch && ch.hasOwnProperty("genderAgeHistory")) {
                                                                        delete ch.genderAgeHistory;
                                                                }
                                                                filteredList.push(ch);
                                                        }
                                                        allFilesData[fileName] = JSON.stringify(filteredList);
                                                        continue;
                                                }
                                        }
                                        // 非角色核心文件直接保留原内容
                                        allFilesData[fileName] = fileContent;
                                } catch (e) {
                                        allFilesData[fileName] = "";
                                        console.error("处理核心文件" + fileName + "失败：" + e);
                                }
                        }

                        // 2.5 同步 tts.data 中的书名列表和当前书名到备份
                        try {
                                allFilesData["__ttsData_bookListData"] = (ttsrv.tts.data['bookListData'] || '').toString();
                                allFilesData["__ttsData_currentBookName"] = (ttsrv.tts.data['currentBookName'] || '').toString();
                        } catch (e) {
                                console.log("同步tts.data到备份失败: " + e.toString());
                        }
  
                        // 3. 读取书籍列表，添加书籍文件到备份集合
                        try {
                                // 优先从 tts.data 获取书名列表（最准确），其次从 liebiao.json
                                var bookListData = (ttsrv.tts.data['bookListData'] || '').toString().trim();
                                var liebiaoContent = "";
                                if (bookListData) {
                                        liebiaoContent = bookListData;
                                        console.log("从 tts.data 获取书名列表用于备份");
                                } else {
                                        liebiaoContent = allFilesData["liebiao.json"] || ttsrv.readTxtFile("liebiao.json") || "[]";
                                        console.log("从 liebiao.json 获取书名列表用于备份");
                                }
                                var bookList = JSON.parse(liebiaoContent);
                                // 仅当书籍列表是数组时才遍历
                                if (Object.prototype.toString.call(bookList) === "[object Array]") {
                                        for (var i = 0; i < bookList.length; i++) {
                                                var bookName = String(bookList[i] || "").trim();
                                                if (bookName === "") continue; // 跳过空书名
                                                var bookFileName = "shuming." + bookName + ".json";
                                                // 读取书籍文件内容
                                                try {
                                                        var bookContent = ttsrv.readTxtFile(bookFileName) || "[]";
                                                        // 过滤书籍文件中的 genderAgeHistory + 紧凑JSON
                                                        if (bookContent.trim() !== "") {
                                                                var bookCharacters = JSON.parse(bookContent);
                                                                var filteredBookChars = [];
                                                                for (var bi = 0; bi < bookCharacters.length; bi++) {
                                                                        var bc = bookCharacters[bi];
                                                                        if (bc && bc.hasOwnProperty("genderAgeHistory")) {
                                                                                delete bc.genderAgeHistory;
                                                                        }
                                                                        filteredBookChars.push(bc);
                                                                }
                                                                bookContent = JSON.stringify(filteredBookChars);
                                                        }
                                                        // 【核心】将书籍文件加入备份集合
                                                        allFilesData[bookFileName] = bookContent;
                                                        console.log("已添加书籍文件到备份：" + bookFileName);
                                                } catch (e) {
                                                        allFilesData[bookFileName] = "[]"; // 空文件用空数组占位
                                                        console.error("处理书籍" + bookFileName + "失败：" + e);
                                                }
                                        }
                                }
                        } catch (e) {
                                console.error("读取书籍列表失败：" + e);
                        }
  
                        // 4. 紧凑格式保存所有备份数据
                        ttsrv.tts.data.backupTest = JSON.stringify(allFilesData);
                        Toast.makeText(ctx, "已备份到角色数据（共" + Object.keys(allFilesData).length + "个文件）", Toast.LENGTH_SHORT).show();
                } catch (e) {
                        Toast.makeText(ctx, "备份失败：" + e, Toast.LENGTH_SHORT).show();
                }
        }
  
        
        function restoreAllFilesFromData() {
                try {
                        if (!ttsrv.tts.data || !ttsrv.tts.data.backupTest) throw "无备份数据";
                        var allFilesData = JSON.parse(ttsrv.tts.data.backupTest);
                        var restoredCount = 0;
                        for (var fileName in allFilesData) {
                                // 跳过 __ttsData_ 元数据键，避免写入垃圾文件
                                if (fileName.indexOf("__ttsData_") === 0) continue;
                                try {
                                        // 正常恢复当前文件
                                        ttsrv.writeTxtFile(fileName, allFilesData[fileName]);
                                        restoredCount++;
  
                                        // 核心修改：恢复characterRecords.json时，同步保存到gengxin.json
                                        if (fileName === "characterRecords.json") {
                                                ttsrv.writeTxtFile("gengxin.json", allFilesData[fileName]);
                                                // record.voice 保持 tag 原值，不做转换
                                                characterRecords = JSON.parse(allFilesData[fileName]) || [];
  
  
                                                console.log("已同步保存characterRecords.json内容到gengxin.json");
                                                restoredCount++; // 计数+1（同步文件也算恢复成功1个）
                                        }
                                } catch (e) {
                                        // 单独捕获同步gengxin.json的错误，不影响主文件恢复
                                        if (fileName === "characterRecords.json") {
                                                console.error("恢复characterRecords.json成功，但同步gengxin.json失败：" + e.toString());
                                        } else {
                                                console.error("恢复" + fileName + "失败：" + e.toString());
                                        }
                                }
                        }
                        // 恢复 tts.data 中的书名列表和当前书名
                        try {
                                if (allFilesData["__ttsData_bookListData"]) {
                                        ttsrv.tts.data['bookListData'] = allFilesData["__ttsData_bookListData"];
                                        _bookListCache = JSON.parse(allFilesData["__ttsData_bookListData"]);
                                        console.log("已恢复 tts.data 书名列表");
                                }
                                if (allFilesData["__ttsData_currentBookName"]) {
                                        ttsrv.tts.data['currentBookName'] = allFilesData["__ttsData_currentBookName"];
                                        console.log("已恢复 tts.data 当前书名");
                                }
                        } catch (e) {
                                console.log("恢复tts.data失败: " + e.toString());
                        }
                        // 还原后数据整体替换，需要重建列表（不能用 refreshCharacterData 只更新标签）
                        refreshCharacterList("");
                        initBookSpinner(); 
                        Toast.makeText(ctx, "恢复成功" + restoredCount + "个文件（含同步的gengxin.json）", Toast.LENGTH_SHORT).show();
                } catch (e) {
                        console.error("恢复失败：" + (e.message || e.toString()));
                        Toast.makeText(ctx, "恢复失败：" + (e.message || e.toString()), Toast.LENGTH_SHORT).show();
                }
        }

        // 1. 书籍备份到剪贴板（新增书名字段，备份JSON格式：{bookName: "书名", characterData: [...]}）
        function backupToClipboard() {
            try {
                // 获取当前书名和角色数据
                var currentBookName = getCurrentBookName();
                var characterData = ttsrv.readTxtFile("characterRecords.json");
                
                if (!currentBookName || currentBookName.trim() === "") {
                    Toast.makeText(ctx, "当前书籍名异常，无法备份", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!characterData || characterData.trim() === "") {
                    Toast.makeText(ctx, "当前书籍角色数据为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 封装「书名+角色数据」为JSON对象
                var backupData = {
                    bookName: currentBookName.trim(),
                    characterData: JSON.parse(characterData) // 提前解析为数组，确保恢复时格式正确
                };
                var backupJson = JSON.stringify(backupData, null, 2);
                
                // 复制到剪贴板
                var clipboard = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                var clip = android.content.ClipData.newPlainText("书籍备份（含书名）", backupJson);
                clipboard.setPrimaryClip(clip);
                
                Toast.makeText(ctx, "已备份书籍：" + currentBookName + "（含角色数据）", Toast.LENGTH_SHORT).show();
            } catch (e) {
                console.error("书籍备份到剪贴板失败: " + e.toString());
                Toast.makeText(ctx, "备份失败: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
        
        // 2. 从文本恢复书籍（自动解析书名，无需手动输入）
        function restoreFromText() {
            showMultiLineInputDialog("请输入书籍备份JSON（含书名）", function(inputText) {
                if (!inputText || inputText.trim() === "") {
                    Toast.makeText(ctx, "输入内容不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                try {
                    // 解析备份JSON（必须包含bookName和characterData字段）
                    var backupData = JSON.parse(inputText);
                    if (!backupData.bookName || !Array.isArray(backupData.characterData)) {
                        throw new Error("备份格式错误，需包含「bookName（书名）」和「characterData（角色数组）」");
                    }
                    
                    // 提取书名和角色数据
                    var restoredBookName = backupData.bookName.trim();
                    var restoredCharacterData = JSON.stringify(backupData.characterData, null, 2);
                    
                    if (restoredBookName === "") {
                        throw new Error("书籍名为空，无法恢复");
                    }
                    
                    // 执行恢复逻辑：更新书籍列表→保存角色数据→切换到恢复的书籍
                    updateBookList(restoredBookName); // 将书名加入书籍列表（去重）
                    ttsrv.writeTxtFile("cunfang.txt", restoredBookName); ttsrv.tts.data['currentBookName'] = restoredBookName; // 更新当前书名
                    ttsrv.writeTxtFile("characterRecords.json", restoredCharacterData); // 保存角色数据
                    ttsrv.writeTxtFile("shuming." + restoredBookName + ".json", restoredCharacterData); // 保存书籍专属文件
                    createGengxinFile(); // 更新gengxin.json
                    
                    // 加载恢复的数据并刷新UI
                    characterRecords = backupData.characterData;
                    initBookSpinner(); // 刷新书籍选择框
                    // useBook 内部会 refreshCharacterList()，无需提前调用
                    useBook(restoredBookName); // 切换到恢复的书籍并刷新列表
                    
                    Toast.makeText(ctx, "成功恢复书籍：" + restoredBookName + "（共" + backupData.characterData.length + "个角色）", Toast.LENGTH_SHORT).show();
                } catch (e) {
                    console.error("书籍恢复失败: " + e.toString());
                    Toast.makeText(ctx, "恢复失败: " + e.toString(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        
        function updateBookList(newBookName) {
            try {
                var bookList = getBookList();
                
                var exists = false;
                for (var i = 0; i < bookList.length; i++) {
                    if (String(bookList[i]).trim() === String(newBookName).trim()) {
                        exists = true;
                        break;
                    }
                }
                
                if (!exists) {
                    bookList.push(newBookName);
                    bookList = removeDuplicateBooks(bookList);
                    ttsrv.writeTxtFile("liebiao.json", JSON.stringify(bookList, null, 2));
                    refreshBookListCache(bookList);
                    console.log("已添加新书名到liebiao.json: " + newBookName);
                }
                
            } catch (e) {
                console.error("更新书籍列表失败: " + e.toString());
            }
        }
        
        function removeDuplicateBooks(bookList) {
            console.log("开始检测重复书籍...");
            console.log("原始书籍列表: " + JSON.stringify(bookList));
            
            var uniqueBooks = [];
            var seenBooks = {};
            var removedCount = 0;
            
            for (var i = 0; i < bookList.length; i++) {
                var bookName = String(bookList[i]).trim();
                var bookNameNorm = normalizeString(bookName);
                
                if (!seenBooks[bookNameNorm]) {
                    seenBooks[bookNameNorm] = true;
                    uniqueBooks.push(bookList[i]);
                    console.log("保留书籍: [" + bookName + "]");
                } else {
                    removedCount++;
                    console.log("移除重复书籍: [" + bookName + "]");
                }
            }
            
            if (removedCount > 0) {
                console.log("共移除 " + removedCount + " 个重复书籍");
                console.log("清理后书籍列表: " + JSON.stringify(uniqueBooks));
            } else {
                console.log("未发现重复书籍");
            }
            
            return uniqueBooks;
        }
        
        // 搜索区容器（无背景，与角色列表同层）
        var searchSectionLayout = new android.widget.LinearLayout(ctx);
        searchSectionLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        searchSectionLayout.setPadding(dipToPx(10), dipToPx(8), dipToPx(12), dipToPx(0));
        linearLayout.addView(searchSectionLayout);
        ttsrv.setMargins(searchSectionLayout, 0, 12, 0, 0);

        // 搜索框 + 刷新 + 全选（同一行，搜索框靠左）
        var headerRowLayout = new android.widget.LinearLayout(ctx);
        headerRowLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        headerRowLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        searchSectionLayout.addView(headerRowLayout);

        // mergeLabel 角色列表标题（旧版样式：👤 + 文字，蓝色加粗）
        var mergeLabel = new android.widget.TextView(ctx);
        mergeLabel.setText("👤 角色列表:");
        mergeLabel.setTextSize(15);
        mergeLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        mergeLabel.setTextColor(android.graphics.Color.parseColor("#1976D2"));
        mergeLabel.setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
        var labelParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            dipToPx(34)
        );
        labelParams.setMargins(0, 0, dipToPx(16), 0);
        mergeLabel.setLayoutParams(labelParams);
        headerRowLayout.addView(mergeLabel);

        // 搜索框 + 全选按钮（融为一体，全选键置于搜索框内右侧，旧版结构）
        var searchContainer = new android.widget.FrameLayout(ctx);
        var containerParams = new android.widget.LinearLayout.LayoutParams(0, dipToPx(34), 1);
        containerParams.setMargins(dipToPx(8), 0, 0, 0);
        searchContainer.setLayoutParams(containerParams);

        var searchBg = new android.graphics.drawable.GradientDrawable();
        searchBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        searchBg.setCornerRadius(dipToPx(8));
        searchBg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
        searchBg.setStroke(dipToPx(1), android.graphics.Color.parseColor("#10000000"));
        searchContainer.setBackground(searchBg);

        var searchInput = new android.widget.EditText(ctx);
        searchInput.setHint("🔍 搜索角色");
        searchInput.setTextSize(16);
        searchInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        searchInput.setPadding(dipToPx(10), dipToPx(4), dipToPx(64), dipToPx(4));
        searchInput.setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
        searchInput.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        var inputParams = new android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        );
        searchInput.setLayoutParams(inputParams);
        searchContainer.addView(searchInput);

        headerRowLayout.addView(searchContainer);

        // 提示文字（独占一行）
        var hintRowLayout = new android.widget.LinearLayout(ctx);
        hintRowLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        var hintRowParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        hintRowParams.setMargins(dipToPx(2), dipToPx(14), 0, dipToPx(8));
        hintRowLayout.setLayoutParams(hintRowParams);
        searchSectionLayout.addView(hintRowLayout);

        var longPressHint = new android.widget.TextView(ctx);
        longPressHint.setText("👉 点击角色名选中 · 长按角色名操作 · 🔊 点击发音人标签更换发音人");
        longPressHint.setTextSize(12);
        longPressHint.setTextColor(android.graphics.Color.parseColor("#757575"));
        longPressHint.setGravity(android.view.Gravity.LEFT);
        var hintTextParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        longPressHint.setLayoutParams(hintTextParams);
        hintRowLayout.addView(longPressHint);

        // 全选按钮（置于搜索框右侧，与搜索框融为一体）
        var selectAllBtn = new android.widget.TextView(ctx);
        selectAllBtn.setText("全选");
        selectAllBtn.setTextSize(14);
        selectAllBtn.setGravity(android.view.Gravity.CENTER);
        var btnShape = new android.graphics.drawable.GradientDrawable();
        btnShape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        btnShape.setCornerRadius(dipToPx(8));
        btnShape.setColor(android.graphics.Color.parseColor("#F3E5F5"));
        btnShape.setStroke(dipToPx(1), android.graphics.Color.parseColor("#7E57C2"));
        selectAllBtn.setBackground(btnShape);
        selectAllBtn.setTextColor(android.graphics.Color.parseColor("#7E57C2"));
        selectAllBtn.setPadding(dipToPx(12), dipToPx(4), dipToPx(12), dipToPx(4));
        var btnParams = new android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        );
        btnParams.gravity = android.view.Gravity.RIGHT | android.view.Gravity.CENTER_VERTICAL;
        btnParams.setMargins(0, dipToPx(3), dipToPx(4), dipToPx(3));
        selectAllBtn.setLayoutParams(btnParams);
        searchContainer.addView(selectAllBtn);

        selectAllBtn.setOnClickListener(new android.view.View.OnClickListener({
            onClick: function(view) {
                if (markedIndices.length < filteredIndices.length) {
                    // 全选
                    markedIndices = [];
                    for (var i = 0; i < filteredIndices.length; i++) {
                        markedIndices.push(filteredIndices[i]);
                    }
                    selectedIndex = filteredIndices.length > 0 ? filteredIndices[0] : -1;
                    selectAllBtn.setText("取消全选");
                    var cancelShape = new android.graphics.drawable.GradientDrawable();
                    cancelShape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                    cancelShape.setCornerRadius(dipToPx(8));
                    cancelShape.setColor(android.graphics.Color.parseColor("#FFF3E0"));
                    cancelShape.setStroke(dipToPx(1), android.graphics.Color.parseColor("#EF6C00"));
                    selectAllBtn.setBackground(cancelShape);
                    selectAllBtn.setTextColor(android.graphics.Color.parseColor("#EF6C00"));
                } else {
                    // 取消全选
                    markedIndices = [];
                    selectedIndex = -1;
                    selectAllBtn.setText("全选");
                    var allShape = new android.graphics.drawable.GradientDrawable();
                    allShape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                    allShape.setCornerRadius(dipToPx(8));
                    allShape.setColor(android.graphics.Color.parseColor("#F3E5F5"));
                    allShape.setStroke(dipToPx(1), android.graphics.Color.parseColor("#7E57C2"));
                    selectAllBtn.setBackground(allShape);
                    selectAllBtn.setTextColor(android.graphics.Color.parseColor("#7E57C2"));
                }
                clearListChoices();
                // 所有标记角色都显示选中状态（多选）
                for (var i = 0; i < filteredIndices.length; i++) {
                    setListItemChecked(i, true);
                }
                updateListViewAppearance();
                if (mergeLabel) mergeLabel.setText("👤 角色列表:");
            }
        }));

        // 用 LinearLayout 替代 ListView：角色列表完全展开显示全部角色，自身永不滚动
        var mergeListView = new android.widget.LinearLayout(ctx);
        mergeListView.setOrientation(android.widget.LinearLayout.VERTICAL);
        var listLp = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        mergeListView.setLayoutParams(listLp);
        linearLayout.addView(mergeListView);
        ttsrv.setMargins(mergeListView, 0, 2, 0, 0);

        // + 添加角色按钮（低调灰色小字，不抢眼）
        var addCharacterRow = new android.widget.TextView(ctx);
        addCharacterRow.setText("+ 添加角色");
        addCharacterRow.setTextSize(12);
        addCharacterRow.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
        addCharacterRow.setGravity(android.view.Gravity.CENTER);
        addCharacterRow.setPadding(dipToPx(8), dipToPx(6), dipToPx(8), dipToPx(6));
        var addRowLp = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        addRowLp.setMargins(0, dipToPx(2), 0, dipToPx(2));
        addCharacterRow.setLayoutParams(addRowLp);
        linearLayout.addView(addCharacterRow);

        // 添加角色点击事件
        addCharacterRow.setOnClickListener(new android.view.View.OnClickListener({
            onClick: function(view) {
                // 输入角色名后直接搜索发音人
                showInputDialog("请输入角色名", function(characterName) {
                    var name = characterName ? characterName.trim() : "";
                    if (name === "") {
                        Toast.makeText(ctx, "角色名不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    showKeywordSelectionDialog(function(selectedVoice) {
                        var newCharacter = {
                            "name": name,
                            "aliases": "",
                            "voice": selectedVoice,
                            "usageCount": 100,
                            "gender": "未知",
                            "age": "未知"
                        };

                        characterRecords.unshift(newCharacter);
                        saveCharacterData();
                        createGengxinFile();
                        refreshCharacterList();

                        Toast.makeText(ctx,
                            "新增角色成功：" + name + "（发音人：" + selectedVoice + "）",
                            Toast.LENGTH_SHORT
                        ).show();
                    });
                });
            }
        }));

        // 行视图数组与选中态，均与 filteredIndices 一一对应
        var rowViews = [];
        var rowCheckStates = [];

        // 创建圆形选中指示器的 Drawable
        // checked=true: 实心圆（蓝色填充+白色内点）；checked=false: 空心圆环（浅灰边框）
        function createCircleDrawable(checked) {
            var d = new android.graphics.drawable.GradientDrawable();
            d.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            if (checked) {
                d.setColor(android.graphics.Color.parseColor("#1976D2"));
                d.setStroke(0, android.graphics.Color.TRANSPARENT);
            } else {
                d.setColor(android.graphics.Color.TRANSPARENT);
                d.setStroke(dipToPx(1.5), android.graphics.Color.parseColor("#10000000"));
            }
            return d;
        }

        function clearListChoices() {
            for (var i = 0; i < rowCheckStates.length; i++) {
                rowCheckStates[i] = false;

            }
        }

        function setListItemChecked(position, checked) {
            if (position < 0 || position >= rowCheckStates.length) return;
            rowCheckStates[position] = checked;

        }

        // 创建单行视图：LinearLayout(横向) = TextView(左) + 圆形指示器View(右)
        function createListRow(displayText, position, record) {
            var row = new android.widget.LinearLayout(ctx);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dipToPx(14), dipToPx(10), dipToPx(14), dipToPx(10));
            var lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, dipToPx(2), 0, dipToPx(2));
            row.setLayoutParams(lp);

            // 名字容器（纵向）：每个名字一行 = [彩色圆点 + 名字]；圆点参考切换书籍菜单的 accentDot（4dp 实心圆）
            var nameContainer = new android.widget.LinearLayout(ctx);
            nameContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
            nameContainer.setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
            var ncLp = new android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1
            );
            nameContainer.setLayoutParams(ncLp);
            row.addView(nameContainer);

            // 计算名字列表（主名 + 去重别名）
            var recName = safeGetName(record);
            var recAliases = splitAliases(record ? record.aliases : "");
            var nameList = [recName];
            for (var k = 0; k < recAliases.length; k++) {
                if (recAliases[k] !== recName && nameList.indexOf(recAliases[k]) === -1) {
                    nameList.push(recAliases[k]);
                }
            }
            // 圆点颜色按"分配的发音人标签"判断：标签自带性别（男青年/女童/女青年/少年…）
            // 男=青蓝、女=粉红；括号一/括号二等无性别标签→灰（与标签视觉呼应）
            var voiceText = (record && record.voice) ? ("" + record.voice) : "";
            var voiceParts = splitVoiceDisplay(voiceText);
            var v = voiceParts.tag || "";
            var dotColor = android.graphics.Color.parseColor("#9E9E9E");
            if (v.indexOf("少年") !== -1) {
                // 少年 = 少男，字面不含"男"字，但性别为男
                dotColor = android.graphics.Color.parseColor("#1976D2");
            } else if (v.indexOf("女") !== -1) {
                dotColor = android.graphics.Color.parseColor("#E91E63");
            } else if (v.indexOf("男") !== -1) {
                dotColor = android.graphics.Color.parseColor("#1976D2");
            }
            var isFav = (record && record.usageCount === 50);
            var isProtagonist = (record && record.age === "主角");

            for (var ni = 0; ni < nameList.length; ni++) {
                var nameLine = new android.widget.LinearLayout(ctx);
                nameLine.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                nameLine.setGravity(android.view.Gravity.CENTER_VERTICAL);

                // 4dp 实心圆点（与切换书籍菜单 accentDot 同款，继续缩小）
                var dotView = new android.view.View(ctx);
                var dotParams = new android.widget.LinearLayout.LayoutParams(dipToPx(4), dipToPx(4));
                dotParams.setMargins(0, 0, dipToPx(8), 0);
                dotView.setLayoutParams(dotParams);
                var dotBg = new android.graphics.drawable.GradientDrawable();
                dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                dotBg.setColor(dotColor);
                dotView.setBackground(dotBg);
                nameLine.addView(dotView);

                // 名字（含【】标星与👑，主名行带皇冠）
                var nameText = new android.widget.TextView(ctx);
                var label = nameList[ni];
                if (isFav) {
                    label = "【" + label + "】";
                }
                if (ni === 0 && isProtagonist) {
                    label = label + " 👑";
                }
                nameText.setText(label);
                nameText.setTextSize(16);
                nameText.setTextColor(getAdaptiveTextColor());
                nameText.setTag("nameText");
                nameText.setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
                if (ni > 0) {
                    var ntLp = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    ntLp.setMargins(0, dipToPx(2), 0, 0);
                    nameText.setLayoutParams(ntLp);
                }
                nameLine.addView(nameText);

                nameContainer.addView(nameLine);
            }

            // 发音人标签部分：独立TextView，小圆角背景框，可点击更换发音人
            if (record && record.voice) {
                var voiceTag = generateVoiceTag(record);
                if (voiceTag) {
                    var voiceView = new android.widget.TextView(ctx);
                    voiceView.setTag("voiceTag");
                    voiceView.setText(voiceTag);
                    voiceView.setTextSize(13);
                    voiceView.setSingleLine(true);
                    voiceView.setGravity(android.view.Gravity.CENTER);
                    voiceView.setPadding(dipToPx(10), dipToPx(5), dipToPx(10), dipToPx(5));
                    // 浅蓝灰圆角小框背景
                    var voiceBg = new android.graphics.drawable.GradientDrawable();
                    voiceBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                    voiceBg.setCornerRadius(dipToPx(8));
                    voiceBg.setColor(android.graphics.Color.parseColor("#E3F2FD"));
                    voiceView.setBackground(voiceBg);
                    var voiceLp = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    voiceLp.setMargins(dipToPx(10), 0, 0, 0);
                    voiceView.setLayoutParams(voiceLp);
                    // 点击发音人标签 = 更换发音人功能
                    var voiceIndex = filteredIndices[position];
                    voiceView.setOnClickListener(new android.view.View.OnClickListener({
                        onClick: function(v) {
                            showVoiceSelectionDialogForFixByIndex(voiceIndex);
                        }
                    }));
                    row.addView(voiceView);
                }
            }

            // 试听按钮（角色有发音人时显示）
            if (record && record.voice) {
                var charPvBtn = new android.widget.TextView(ctx);
                charPvBtn.setText("▶");
                charPvBtn.setTextSize(16);
                charPvBtn.setTextColor(android.graphics.Color.parseColor("#1976D2"));
                charPvBtn.setSingleLine(true);
                charPvBtn.setGravity(android.view.Gravity.CENTER);
                charPvBtn.setPadding(dipToPx(10), dipToPx(8), dipToPx(2), dipToPx(8));
                // record.voice 是 tag，试听直接用 tag 查当前分组的配置项
                var _charPvTag = record.voice;
                var charPvLp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                );
                charPvLp.setMargins(dipToPx(6), 0, 0, 0);
                charPvBtn.setLayoutParams(charPvLp);
                charPvBtn.setOnClickListener(new android.view.View.OnClickListener({
                    onClick: function(v) {
                        try { previewVoiceByName(_charPvTag, charPvBtn); }
                        catch (e) { Toast.makeText(ctx, "试听异常: " + e.toString(), Toast.LENGTH_SHORT).show(); }
                    }
                }));
                row.addView(charPvBtn);

                // 改显示名按钮（✎）：独立入口，改配置项 displayName
                var charRnBtn = new android.widget.TextView(ctx);
                charRnBtn.setText("✎");
                charRnBtn.setTextSize(16);
                charRnBtn.setTextColor(android.graphics.Color.parseColor("#1976D2"));
                charRnBtn.setSingleLine(true);
                charRnBtn.setGravity(android.view.Gravity.CENTER);
                charRnBtn.setPadding(dipToPx(6), dipToPx(8), dipToPx(2), dipToPx(8));
                var charRnLp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                );
                charRnLp.setMargins(dipToPx(2), 0, 0, 0);
                charRnBtn.setLayoutParams(charRnLp);
                var _charRnTag = record.voice;
                charRnBtn.setOnClickListener(new android.view.View.OnClickListener({
                    onClick: function(v) {
                        try { doRenameVoice(_charRnTag, null); }
                        catch (e) { Toast.makeText(ctx, "改名弹窗异常: " + e.toString(), Toast.LENGTH_SHORT).show(); }
                    }
                }));
                row.addView(charRnBtn);

                // 发音人管理按钮（⋮）：删除/标记（喜欢·不喜欢·路人）
                var charMgBtn = new android.widget.TextView(ctx);
                charMgBtn.setText("⋮");
                charMgBtn.setTextSize(18);
                charMgBtn.setTextColor(android.graphics.Color.parseColor("#757575"));
                charMgBtn.setSingleLine(true);
                charMgBtn.setGravity(android.view.Gravity.CENTER);
                charMgBtn.setPadding(dipToPx(6), dipToPx(8), dipToPx(4), dipToPx(8));
                var charMgLp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                );
                charMgLp.setMargins(dipToPx(2), 0, 0, 0);
                charMgBtn.setLayoutParams(charMgLp);
                var _charMgTag = record.voice;
                charMgBtn.setOnClickListener(new android.view.View.OnClickListener({
                    onClick: function(v) {
                        try { showVoiceManageDialog(_charMgTag, null); }
                        catch (e) { Toast.makeText(ctx, "管理弹窗异常: " + e.toString(), Toast.LENGTH_SHORT).show(); }
                    }
                }));
                row.addView(charMgBtn);
            }

            // 右侧圆形选中指示器已移除（选中状态通过背景色体现）

            // 单击：切换标记
            row.setOnClickListener(new android.view.View.OnClickListener({
                onClick: function(v) {
                    try {
                        handleItemClick(position);
                    } catch(e) {
                        console.error("handleItemClick 异常: " + e.toString());
                    }
                }
            }));

            // 长按：通过 OnTouchListener 检测长按（Rhino 兼容）
            // 加移动阈值：手指轻微抖动不取消长按
            var longPressHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            var longPressRunnable = null;
            var touchStartX = 0;
            var touchStartY = 0;
            var TOUCH_SLOP = dipToPx(12); // 允许的移动阈值（约12dp）
            row.setOnTouchListener(new android.view.View.OnTouchListener({
                onTouch: function(v, event) {
                    var action = event.getActionMasked();
                    if (action === android.view.MotionEvent.ACTION_DOWN) {
                        touchStartX = event.getRawX();
                        touchStartY = event.getRawY();
                        longPressRunnable = new java.lang.Runnable({
                            run: function() {
                                try {
                                    handleItemLongClick(position);
                                } catch(e) {
                                    console.error("handleItemLongClick 异常: " + e.toString());
                                    Toast.makeText(ctx, "操作异常: " + e.toString(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        longPressHandler.postDelayed(longPressRunnable, 500);
                    } else if (action === android.view.MotionEvent.ACTION_UP ||
                               action === android.view.MotionEvent.ACTION_CANCEL) {
                        if (longPressRunnable) {
                            longPressHandler.removeCallbacks(longPressRunnable);
                            longPressRunnable = null;
                        }
                    } else if (action === android.view.MotionEvent.ACTION_MOVE) {
                        // 只有移动距离超过阈值才取消长按
                        var dx = Math.abs(event.getRawX() - touchStartX);
                        var dy = Math.abs(event.getRawY() - touchStartY);
                        if (dx > TOUCH_SLOP || dy > TOUCH_SLOP) {
                            if (longPressRunnable) {
                                longPressHandler.removeCallbacks(longPressRunnable);
                                longPressRunnable = null;
                            }
                        }
                    }
                    return false;
                }
            }));

            return row;
        }

        // 根据 filteredIndices 重建全部行
        function buildList() {
            // 刷新personality映射缓存，确保角色列表显示最新的性格信息
            _initFayinrenMapCache(true);

            // 重建前设为不可见，避免 removeAllViews 后标签消失再重现的闪烁
            var wasVisible = (mergeListView.getVisibility() === android.view.View.VISIBLE);
            if (wasVisible) {
                mergeListView.setVisibility(android.view.View.INVISIBLE);
            }
            try {
                // 始终重建全部行（操作后/切书/搜索都需要重建）
                mergeListView.removeAllViews();
                rowViews = [];
                rowCheckStates = [];

                for (var i = 0; i < filteredIndices.length; i++) {
                    var record = characterRecords[filteredIndices[i]];
                    if (!record) continue;
                    // record.voice 保持 tag 原值，标签显示由 generateVoiceTag 通过 getVoiceByTag(tag) 实时查
                    var displayText = generateDisplayText(record);
                    var row = createListRow(displayText, i, record);
                    rowViews.push(row);
                    rowCheckStates.push(false);
                    mergeListView.addView(row);
                    // 行间极淡分隔线：让高矮不一的行都有清晰边界，整体更和谐
                    if (i < filteredIndices.length - 1) {
                        var divider = new android.view.View(ctx);
                        divider.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            dipToPx(1)
                        ));
                        divider.setBackgroundColor(android.graphics.Color.parseColor("#1A000000"));
                        mergeListView.addView(divider);
                    }
                }
            } finally {
                // 无论是否异常，都恢复可见（避免列表卡在不可见状态）
                if (wasVisible) {
                    mergeListView.setVisibility(android.view.View.VISIBLE);
                }
            }
        }

        // 仅刷新性格标签（刷新按钮专用）：不重建行，只更新发音人标签文本
        function refreshVoiceTagsOnly() {
            _initFayinrenMapCache(true);
            for (var i = 0; i < filteredIndices.length && i < rowViews.length; i++) {
                var record = characterRecords[filteredIndices[i]];
                if (!record) continue;
                    var existingRow = rowViews[i];
                    if (existingRow) {
                        var voiceView = existingRow.findViewWithTag("voiceTag");
                        if (voiceView) {
                            var newTag = generateVoiceTag(record);
                            if (newTag) {
                                voiceView.setText(newTag);
                            }
                        }
                    }
            }
        }

        // 更新单个角色的发音人标签（不重建列表，不闪烁）
        function updateSingleRowVoiceTag(charIndex) {
            try {
                // 找到该角色在 filteredIndices 中的位置
                var rowPos = -1;
                for (var i = 0; i < filteredIndices.length; i++) {
                    if (filteredIndices[i] === charIndex) { rowPos = i; break; }
                }
                if (rowPos === -1 || rowPos >= rowViews.length) return;

                var record = characterRecords[charIndex];
                if (!record) return;

                // record.voice 保持 tag 原值，不做转换

                var existingRow = rowViews[rowPos];
                if (!existingRow) return;

                // 更新发音人标签
                var voiceView = existingRow.findViewWithTag("voiceTag");
                if (voiceView) {
                    var newTag = generateVoiceTag(record);
                    if (newTag) {
                        voiceView.setText(newTag);
                    }
                }
            } catch (e) {
                console.error("更新单行标签失败: " + e.toString());
            }
        }

        console.log("填充列表，角色记录数量: " + characterRecords.length);

        var selectedIndex = -1;

        var markedIndices = [];

        var filteredIndices = [];
        var currentKeyword = "";  // 记住当前搜索词，操作后刷新沿用以保持搜索结果
        // 保存当前过滤视图中的角色对象引用，用于操作后保持搜索结果集
        var filteredCharRefs = [];
        // 上一次刷新时的结果集引用快照，供操作后刷新时比对
        var filteredCharRefs_backup = [];
        for (var i = 0; i < characterRecords.length; i++) {
            filteredIndices.push(i);
            filteredCharRefs.push(characterRecords[i]);
        }
        filteredCharRefs_backup = filteredCharRefs.slice();

        // 初始构建列表并应用圆角卡片样式
        buildList();
        mergeListView.post(new java.lang.Runnable({
            run: function() {
                updateListViewAppearance();
            }
        }));

        var searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        var searchRunnable = null;
        searchInput.addTextChangedListener(new android.text.TextWatcher({
            beforeTextChanged: function(s, start, count, after) {},
            onTextChanged: function(s, start, before, count) {},
            afterTextChanged: function(s) {
                if (searchRunnable) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = new java.lang.Runnable({
                    run: function() {
                        try {
                            var keyword = searchInput.getText().toString().trim().toLowerCase();
                            refreshCharacterList(keyword);
                        } catch (e) {
                            console.error("搜索回调异常: " + e.toString());
                        }
                    }
                });
                searchHandler.postDelayed(searchRunnable, 300);
            }
        }));

        // 单击行：切换标记
        function handleItemClick(position) {
            var originalIndex = filteredIndices[position];
            if (markedIndices.indexOf(originalIndex) !== -1) {
                var newMarkedIndices = [];
                for (var idx = 0; idx < markedIndices.length; idx++) {
                    if (markedIndices[idx] !== originalIndex) {
                        newMarkedIndices.push(markedIndices[idx]);
                    }
                }
                markedIndices = newMarkedIndices;
                console.log("取消标记角色索引: " + originalIndex);
                if (selectedIndex === originalIndex) {
                    selectedIndex = -1;
                    setListItemChecked(position, false);
                } else {
                    setListItemChecked(position, false);
                }
            } else {
                markedIndices.push(originalIndex);
                console.log("标记角色索引: " + originalIndex);
                selectedIndex = originalIndex;
                setListItemChecked(position, true);
            }
            updateListViewAppearance();
            if (mergeLabel) mergeLabel.setText("👤 角色列表:");
        }

        var longPressedIndex = -1;
        var firstDialog = null;
        var backupRestoreDialog = null;

        // 长按行：触发操作菜单
        function handleItemLongClick(position) {
            var originalIndex = filteredIndices[position];
            if (markedIndices.indexOf(originalIndex) === -1) {
                markedIndices.push(originalIndex);
                console.log("长按自动标记角色索引: " + originalIndex);
            }
            selectedIndex = originalIndex;
            setListItemChecked(position, true);
            updateListViewAppearance();
            if (mergeLabel) mergeLabel.setText("👤 角色列表:");
            showFirstDialog(originalIndex);
            return true;
        }
        
        // ============================================================
        // ============================================================

        // 长按操作菜单（角色核心功能入口）
        function showFirstDialog(position) {
            longPressedIndex = position;

            // 过滤 markedIndices 中的无效/重复索引（防止 characterRecords 变化后索引失效）
            var validMarked = [];
            for (var vi = 0; vi < markedIndices.length; vi++) {
                var idx = markedIndices[vi];
                if (idx >= 0 && idx < characterRecords.length && validMarked.indexOf(idx) === -1) {
                    validMarked.push(idx);
                }
            }
            // 如果当前长按角色不在有效列表中，补入（长按应自动标记）
            if (position >= 0 && position < characterRecords.length && validMarked.indexOf(position) === -1) {
                validMarked.push(position);
            }
            markedIndices = validMarked;

            // 根据条件动态构建操作列表
            var optionConfigs = [];

            // 合并选项：仅当标记2个及以上角色时显示
            if (markedIndices.length >= 2) {
                optionConfigs.push({ text: "合并+跟随角色", color: "#1976D2", icon: "", action: "merge_follow" });
                optionConfigs.push({ text: "合并+选择发音人", color: "#7E57C2", icon: "", action: "merge_voice" });
            }
            
            // 释放/删除选项：仅当当前角色已合并（有别名且别名不只是自己）时显示
            var character = characterRecords[position];
            var hasMergedAliases = false;
            if (character && character.aliases && character.aliases.trim() !== "") {
                var aliasParts = splitAliases(character.aliases);
                var charName = safeGetName(character);
                // 别名中存在不等于自己名字的项 = 已合并
                for (var ai = 0; ai < aliasParts.length; ai++) {
                    if (aliasParts[ai] !== charName) {
                        hasMergedAliases = true;
                        break;
                    }
                }
            }
            if (hasMergedAliases) {
                optionConfigs.push({ text: "释放/删除【已合并角色】", color: "#FB8C00", icon: "", action: "release" });
            }
            
            // 始终显示的操作（修改角色名仅在单角色时显示，多角色时无意义）
            if (markedIndices.length < 2) {
                optionConfigs.push({ text: "修改角色名", color: "#00838F", icon: "", action: "edit_name" });
            }
            optionConfigs.push({ text: "删除角色", color: "#E53935", icon: "", action: "delete" });
            optionConfigs.push({ text: "设为主角", color: "#F57F17", icon: "", action: "set_main" });

            // 构建自定义列表布局
            var container = new android.widget.LinearLayout(ctx);
            container.setOrientation(android.widget.LinearLayout.VERTICAL);
            container.setPadding(dipToPx(16), dipToPx(12), dipToPx(16), dipToPx(32));
            
            for (var i = 0; i < optionConfigs.length; i++) {
                (function(cfg, index) {
                    var row = new android.widget.LinearLayout(ctx);
                    row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    var rowParams = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    rowParams.setMargins(0, dipToPx(4), 0, dipToPx(4));
                    row.setLayoutParams(rowParams);
                    
                    // 圆角背景
                    var bg = new android.graphics.drawable.GradientDrawable();
                    bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                    bg.setCornerRadius(dipToPx(10));
                    bg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                    bg.setStroke(dipToPx(1), android.graphics.Color.parseColor("#10000000"));
                    row.setBackground(bg);
                    row.setPadding(dipToPx(14), dipToPx(12), dipToPx(14), dipToPx(12));
                    row.setClickable(true);
                    
                    // 简约彩色圆点点缀（保留色彩辨识度）
                    var accentDot = new android.view.View(ctx);
                    var dotParams = new android.widget.LinearLayout.LayoutParams(dipToPx(7), dipToPx(7));
                    dotParams.setMargins(0, 0, dipToPx(8), 0);
                    accentDot.setLayoutParams(dotParams);
                    var dotBg = new android.graphics.drawable.GradientDrawable();
                    dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                    dotBg.setColor(android.graphics.Color.parseColor(cfg.color));
                    accentDot.setBackground(dotBg);
                    row.addView(accentDot);
                    
                    var iconView = new android.widget.TextView(ctx);
                    iconView.setText(cfg.icon);
                    iconView.setTextSize(16);
                    iconView.setTextColor(android.graphics.Color.WHITE);
                    var iconParams = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    iconParams.setMargins(0, 0, dipToPx(12), 0);
                    iconView.setLayoutParams(iconParams);
                    row.addView(iconView);
                    
                    // 文字
                    var textView = new android.widget.TextView(ctx);
                    textView.setText(cfg.text);
                    textView.setTextSize(15);
                    textView.setTextColor(android.graphics.Color.parseColor("#333333"));
                    var textParams = new android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1
                    );
                    textView.setLayoutParams(textParams);
                    row.addView(textView);
                    
                    // 点击事件
                    row.setOnClickListener(new android.view.View.OnClickListener({
                        onClick: function(view) {
                            firstDialog.dismiss();
                            // 延迟到下一轮消息循环执行，避免dismiss未完成时show新弹窗导致闪退
                            var actionKey = cfg.action;
                            var actionPos = position;
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(new java.lang.Runnable({
                                run: function() {
                                    try {
                                        handleFirstDialogAction(actionKey, actionPos);
                                    } catch(e) {
                                        console.error("操作执行异常: " + e.toString());
                                        try { Toast.makeText(ctx, "操作异常: " + e.toString(), Toast.LENGTH_SHORT).show(); } catch(t) {}
                                    }
                                }
                            }));
                        }
                    }));
                    
                    container.addView(row);
                })(optionConfigs[i], i);
            }
            
            var builder = new android.app.AlertDialog.Builder(ctx);
            container.addView(createDialogTitle("选择操作"), 0);
            builder.setView(container);
            
            firstDialog = builder.create();
            firstDialog.show();
            applyDialogRoundCorner(firstDialog);
        }
        
        function handleFirstDialogAction(action, position) {
            switch (action) {
                case "merge_follow":
                    if (markedIndices.length < 2) {
                        Toast.makeText(ctx, "请标记至少2个角色", Toast.LENGTH_SHORT).show();
                    } else {
                        var markedNames = [];
                        for (var i = 0; i < markedIndices.length; i++) {
                            markedNames.push(generateDisplayText(characterRecords[markedIndices[i]]));
                        }
                        var followBuilder = new android.app.AlertDialog.Builder(ctx);
                        // 自定义卡片列表
                        var followContainer = new android.widget.LinearLayout(ctx);
                        followContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
                        followContainer.setPadding(dipToPx(16), dipToPx(12), dipToPx(16), dipToPx(32));
                        followContainer.addView(createDialogTitle("选择要跟随的目标角色"), 0);

                        for (var fi = 0; fi < markedNames.length; fi++) {
                            (function(fname, fidx) {
                                var frow = new android.widget.LinearLayout(ctx);
                                frow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                                frow.setGravity(android.view.Gravity.CENTER_VERTICAL);
                                var fparams = new android.widget.LinearLayout.LayoutParams(
                                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                fparams.setMargins(0, dipToPx(4), 0, dipToPx(4));
                                frow.setLayoutParams(fparams);

                                var fbg = new android.graphics.drawable.GradientDrawable();
                                fbg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                                fbg.setCornerRadius(dipToPx(10));
                                fbg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                                fbg.setStroke(dipToPx(1), android.graphics.Color.parseColor("#10000000"));
                                frow.setBackground(fbg);
                                frow.setPadding(dipToPx(14), dipToPx(12), dipToPx(14), dipToPx(12));
                                frow.setClickable(true);

                                // 与角色列表完全一致的显示：nameContainer（纵向）= 每个名字一行 [4dp圆点 + 名字]
                                var fCharRecord = characterRecords[markedIndices[fidx]];
                                var fNameContainer = new android.widget.LinearLayout(ctx);
                                fNameContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
                                fNameContainer.setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
                                var fNcLp = new android.widget.LinearLayout.LayoutParams(
                                    0,
                                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                    1
                                );
                                fNameContainer.setLayoutParams(fNcLp);
                                frow.addView(fNameContainer);

                                // 计算名字列表（主名 + 去重别名）
                                var fRec = fCharRecord;
                                var fRecName = safeGetName(fRec);
                                var fRecAliases = splitAliases(fRec ? fRec.aliases : "");
                                var fNameList = [fRecName];
                                for (var fki = 0; fki < fRecAliases.length; fki++) {
                                    if (fRecAliases[fki] !== fRecName && fNameList.indexOf(fRecAliases[fki]) === -1) {
                                        fNameList.push(fRecAliases[fki]);
                                    }
                                }
                                // 圆点颜色按"分配的发音人标签"判断（与角色列表一致）
                                var fVoiceText = (fRec && fRec.voice) ? ("" + fRec.voice) : "";
                                var fVoiceParts = splitVoiceDisplay(fVoiceText);
                                var fV = fVoiceParts.tag || "";
                                var fDotColor = android.graphics.Color.parseColor("#9E9E9E");
                                if (fV.indexOf("少年") !== -1) {
                                    fDotColor = android.graphics.Color.parseColor("#1976D2");
                                } else if (fV.indexOf("女") !== -1) {
                                    fDotColor = android.graphics.Color.parseColor("#E91E63");
                                } else if (fV.indexOf("男") !== -1) {
                                    fDotColor = android.graphics.Color.parseColor("#1976D2");
                                }
                                var fIsFav = (fRec && fRec.usageCount === 50);
                                var fIsProtagonist = (fRec && fRec.age === "主角");

                                for (var fni = 0; fni < fNameList.length; fni++) {
                                    var fNameLine = new android.widget.LinearLayout(ctx);
                                    fNameLine.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                                    fNameLine.setGravity(android.view.Gravity.CENTER_VERTICAL);

                                    // 4dp 实心圆点（与角色列表同款，小点保留）
                                    var fDotView = new android.view.View(ctx);
                                    var fDotParams = new android.widget.LinearLayout.LayoutParams(dipToPx(4), dipToPx(4));
                                    fDotParams.setMargins(0, 0, dipToPx(8), 0);
                                    fDotView.setLayoutParams(fDotParams);
                                    var fDotBg = new android.graphics.drawable.GradientDrawable();
                                    fDotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                                    fDotBg.setColor(fDotColor);
                                    fDotView.setBackground(fDotBg);
                                    fNameLine.addView(fDotView);

                                    // 名字（含【】标星与👑，主名行带皇冠）
                                    var fNameText = new android.widget.TextView(ctx);
                                    var fLabel = fNameList[fni];
                                    if (fIsFav) {
                                        fLabel = "【" + fLabel + "】";
                                    }
                                    if (fni === 0 && fIsProtagonist) {
                                        fLabel = fLabel + " 👑";
                                    }
                                    fNameText.setText(fLabel);
                                    fNameText.setTextSize(15);
                                    fNameText.setTextColor(getAdaptiveTextColor());
                                    fNameText.setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
                                    if (fni > 0) {
                                        var fNtLp = new android.widget.LinearLayout.LayoutParams(
                                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                                        );
                                        fNtLp.setMargins(0, dipToPx(2), 0, 0);
                                        fNameText.setLayoutParams(fNtLp);
                                    }
                                    fNameLine.addView(fNameText);

                                    fNameContainer.addView(fNameLine);
                                }

                                // 发音人标签（与角色列表一致：浅蓝灰圆角小框）
                                if (fCharRecord && fCharRecord.voice) {
                                    var fVoiceTag = generateVoiceTag(fCharRecord);
                                    if (fVoiceTag) {
                                        var fVoiceView = new android.widget.TextView(ctx);
                                        fVoiceView.setText(fVoiceTag);
                                        fVoiceView.setTextSize(13);
                                        fVoiceView.setSingleLine(true);
                                        fVoiceView.setGravity(android.view.Gravity.CENTER);
                                        fVoiceView.setPadding(dipToPx(10), dipToPx(5), dipToPx(10), dipToPx(5));
                                        var fVoiceBg = new android.graphics.drawable.GradientDrawable();
                                        fVoiceBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                                        fVoiceBg.setCornerRadius(dipToPx(8));
                                        fVoiceBg.setColor(android.graphics.Color.parseColor("#E3F2FD"));
                                        fVoiceView.setBackground(fVoiceBg);
                                        var fVoiceLp = new android.widget.LinearLayout.LayoutParams(
                                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                                        );
                                        fVoiceLp.setMargins(dipToPx(10), 0, 0, 0);
                                        fVoiceView.setLayoutParams(fVoiceLp);
                                        frow.addView(fVoiceView);
                                    }
                                }

                                frow.setOnClickListener(new android.view.View.OnClickListener({
                                    onClick: function(view) {
                                        selectedIndex = markedIndices[fidx];
                                        firstDialog.dismiss();
                                        if (followDialog) followDialog.dismiss();
                                        new android.os.Handler(android.os.Looper.getMainLooper()).post(new java.lang.Runnable({
                                            run: function() {
                                                try { doMergeOperation("follow_target"); } catch(e) { console.error("合并失败: " + e.toString()); }
                                            }
                                        }));
                                    }
                                }));

                                followContainer.addView(frow);
                            })(markedNames[fi], fi);
                        }
                        followBuilder.setView(followContainer);
                        var followDialog = followBuilder.create();
                        followDialog.show();
                        applyDialogRoundCorner(followDialog);
                    }
                    break;
                case "merge_voice":
                    showVoiceSelectionDialogForMerge();
                    break;
                case "release":
                    doReleaseOperation();
                    break;
                case "edit_name":
                    doEditCharacterOperation(position);
                    break;
                case "delete":
                    doDeleteCharacterOperation();
                    break;
                case "set_main":
                    setAsMainCharacter();
                    break;
            }
        }

        // === 试听功能 ===
        var _pvMediaPlayer = null;
        var _pvCurrentBtn = null;
        var _pvHandler = null;
        try { _pvHandler = new android.os.Handler(android.os.Looper.getMainLooper()); } catch (eHp) {}

        function _pvStop() {
            try {
                if (_pvMediaPlayer !== null) {
                    try { if (_pvMediaPlayer.isPlaying()) _pvMediaPlayer.stop(); } catch (e3) {}
                    _pvMediaPlayer.release();
                    _pvMediaPlayer = null;
                }
            } catch (e) {}
            if (_pvCurrentBtn !== null) {
                try { _pvCurrentBtn.setText("▶"); _pvCurrentBtn.setTextColor(android.graphics.Color.parseColor("#1976D2")); } catch (e) {}
                _pvCurrentBtn = null;
            }
        }

        function _pvPlay(ttsurl, label, btn) {
            try {
                _pvStop();
                _pvCurrentBtn = btn;
                btn.setText("…");
                btn.setTextColor(android.graphics.Color.parseColor("#FF6F00"));
                var mp = new android.media.MediaPlayer();
                _pvMediaPlayer = mp;
                mp.setDataSource(ctx, android.net.Uri.parse(ttsurl));
                mp.setOnPreparedListener(new android.media.MediaPlayer.OnPreparedListener({
                    onPrepared: function (p) {
                        try { p.start(); btn.setText("■"); btn.setTextColor(android.graphics.Color.parseColor("#F44336")); Toast.makeText(ctx, "试听：" + label, Toast.LENGTH_SHORT).show(); }
                        catch (e7) { _pvStop(); }
                    }
                }));
                mp.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener({ onCompletion: function (p) { _pvStop(); } }));
                mp.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener({
                    onError: function (p, w, x) { _pvStop(); Toast.makeText(ctx, "播放出错", Toast.LENGTH_SHORT).show(); return true; }
                }));
                mp.prepareAsync();
            } catch (e) { _pvStop(); Toast.makeText(ctx, "播放失败", Toast.LENGTH_SHORT).show(); }
        }

        // ===== 发音人管理弹窗（删除/标记） =====
        // 入口：角色行试听按钮旁的 ⋮ 按钮，或换发音人弹窗里试听旁的 ⋮
        // 删除流程：删配置项 → 从 fayinren.json 删 tag → 调用朗读规则重分配受影响角色 → 刷新
        // 标记流程：写 voice_marks.json → 刷新
        function showVoiceManageDialog(voiceTag, onChange) {
            try {
                if (!voiceTag) {
                    Toast.makeText(ctx, "无发音人可管理", Toast.LENGTH_SHORT).show();
                    return;
                }
                var tag = String(voiceTag);

                // 先查当前 displayName 供显示
                var currentName = tag;
                try {
                    var liveName = ttsrv.getVoiceByTag(tag);
                    if (liveName) currentName = liveName;
                } catch (e) {}

                var builder = new android.app.AlertDialog.Builder(ctx);
                var container = new android.widget.LinearLayout(ctx);
                container.setOrientation(android.widget.LinearLayout.VERTICAL);
                container.setPadding(dipToPx(16), dipToPx(12), dipToPx(16), dipToPx(32));
                container.addView(createDialogTitle("管理发音人"));

                // 当前发音人信息
                var infoView = new android.widget.TextView(ctx);
                infoView.setText("标签：" + tag + "\n显示名：" + currentName + "\n标记：" + getVoiceMarkLabel(tag));
                infoView.setTextSize(13);
                infoView.setTextColor(android.graphics.Color.parseColor("#757575"));
                var infoLp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                );
                infoLp.setMargins(0, 0, 0, dipToPx(12));
                infoView.setLayoutParams(infoLp);
                container.addView(infoView);

                var options = [
                    { text: "❤️ 喜欢", color: "#43A047", action: "mark_like" },
                    { text: "🚶 路人", color: "#9E9E9E", action: "mark_neutral" },
                    { text: "😈 坏人", color: "#E53935", action: "mark_bad" },
                    { text: "✖ 不喜欢，删除", color: "#E53935", action: "delete" }
                ];

                for (var i = 0; i < options.length; i++) {
                    (function(cfg) {
                        var row = new android.widget.LinearLayout(ctx);
                        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                        var rowParams = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        rowParams.setMargins(0, dipToPx(4), 0, dipToPx(4));
                        row.setLayoutParams(rowParams);

                        var bg = new android.graphics.drawable.GradientDrawable();
                        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                        bg.setCornerRadius(dipToPx(10));
                        bg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                        bg.setStroke(dipToPx(1), android.graphics.Color.parseColor("#10000000"));
                        row.setBackground(bg);
                        row.setPadding(dipToPx(14), dipToPx(12), dipToPx(14), dipToPx(12));
                        row.setClickable(true);

                        var dot = new android.view.View(ctx);
                        var dotParams = new android.widget.LinearLayout.LayoutParams(dipToPx(7), dipToPx(7));
                        dotParams.setMargins(0, 0, dipToPx(8), 0);
                        dot.setLayoutParams(dotParams);
                        var dotBg = new android.graphics.drawable.GradientDrawable();
                        dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                        dotBg.setColor(android.graphics.Color.parseColor(cfg.color));
                        dot.setBackground(dotBg);
                        row.addView(dot);

                        var textView = new android.widget.TextView(ctx);
                        textView.setText(cfg.text);
                        textView.setTextSize(15);
                        textView.setTextColor(android.graphics.Color.parseColor("#333333"));
                        var textParams = new android.widget.LinearLayout.LayoutParams(
                            0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1
                        );
                        textView.setLayoutParams(textParams);
                        row.addView(textView);

                        row.setOnClickListener(new android.view.View.OnClickListener({
                            onClick: function(v) {
                                voiceManageDlg.dismiss();
                                var actionKey = cfg.action;
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(new java.lang.Runnable({
                                    run: function() {
                                        try {
                                            if (actionKey === "delete") {
                                                doDeleteVoiceAndReassign(tag, onChange);
                                            } else if (actionKey === "mark_like") {
                                                setVoiceMark(tag, "like");
                                                Toast.makeText(ctx, "已标记为 喜欢", Toast.LENGTH_SHORT).show();
                                                if (onChange) try { onChange(); } catch (e) {}
                                                refreshCharacterList();
                                            } else if (actionKey === "mark_bad") {
                                                setVoiceMark(tag, "bad");
                                                Toast.makeText(ctx, "已标记为 坏人", Toast.LENGTH_SHORT).show();
                                                if (onChange) try { onChange(); } catch (e) {}
                                                refreshCharacterList();
                                            } else if (actionKey === "mark_neutral") {
                                                setVoiceMark(tag, "neutral");
                                                Toast.makeText(ctx, "已标记为 路人", Toast.LENGTH_SHORT).show();
                                                if (onChange) try { onChange(); } catch (e) {}
                                                refreshCharacterList();
                                            }
                                        } catch (e) {
                                            Toast.makeText(ctx, "操作异常: " + e.toString(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }));
                            }
                        }));

                        container.addView(row);
                    })(options[i]);
                }

                builder.setView(container);
                var voiceManageDlg = builder.create();
                voiceManageDlg.show();
                applyDialogRoundCorner(voiceManageDlg);
            } catch (e) {
                Toast.makeText(ctx, "弹窗异常: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }

        // ===== 发音人标记管理（voice_marks.json） =====
        // 结构：{ "女青年01": "like", "女青年02": "dislike", ... }
        // 值：like / dislike / neutral
        var _voiceMarksCache = null; // null=未加载

        function loadVoiceMarks() {
            if (_voiceMarksCache !== null) return _voiceMarksCache;
            try {
                var raw = ttsrv.readTxtFile("voice_marks.json");
                if (raw && raw.trim() !== "") {
                    var obj = JSON.parse(raw);
                    if (obj && typeof obj === "object") {
                        _voiceMarksCache = obj;
                        return _voiceMarksCache;
                    }
                }
            } catch (e) {}
            _voiceMarksCache = {};
            return _voiceMarksCache;
        }

        function saveVoiceMarks() {
            try {
                ttsrv.writeTxtFile("voice_marks.json", JSON.stringify(_voiceMarksCache || {}, null, 2));
            } catch (e) {
                console.error("保存 voice_marks.json 失败: " + e.toString());
            }
        }

        function getVoiceMark(tag) {
            try {
                var marks = loadVoiceMarks();
                return marks[String(tag)] || "";
            } catch (e) { return ""; }
        }

        function getVoiceMarkLabel(tag) {
            var m = getVoiceMark(tag);
            if (m === "like") return "❤️ 喜欢";
            if (m === "bad") return "😈 坏人";
            if (m === "neutral") return "🚶 路人";
            return "未标记";
        }

        function setVoiceMark(tag, mark) {
            try {
                var marks = loadVoiceMarks();
                if (mark) {
                    marks[String(tag)] = mark;
                } else {
                    delete marks[String(tag)];
                }
                saveVoiceMarks();
                // 同步更新发音人列表的缓存标记
                try {
                    for (var i = 0; i < fayinrenList.length; i++) {
                        if (fayinrenList[i] && fayinrenList[i].tag === tag) {
                            fayinrenList[i].mark = mark;
                        }
                    }
                } catch (e) {}
            } catch (e) {
                Toast.makeText(ctx, "标记失败: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }

        // 改显示名
        function doRenameVoice(tag, onChange) {
            try {
                var currentName = tag;
                try {
                    var liveName = ttsrv.getVoiceByTag(tag);
                    if (liveName) currentName = liveName;
                } catch (e) {}

                var builder = new android.app.AlertDialog.Builder(ctx);
                var container = new android.widget.LinearLayout(ctx);
                container.setOrientation(android.widget.LinearLayout.VERTICAL);
                container.setPadding(dipToPx(20), dipToPx(12), dipToPx(20), dipToPx(16));
                container.addView(createDialogTitle("修改显示名"));
                container.addView(createDialogLabel("新显示名（留空则使用标签名）"));

                var nameInput = createStyledEditText(null, true);
                nameInput.setText(currentName);
                nameInput.setSelection(currentName.length);
                container.addView(nameInput);

                builder.setView(container);
                builder.setPositiveButton("保存", new android.content.DialogInterface.OnClickListener({
                    onClick: function(dialog, which) {
                        var newName = nameInput.getText() ? nameInput.getText().toString().trim() : "";
                        if (!newName) {
                            Toast.makeText(ctx, "名称不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // 子线程调用避免卡顿
                        new java.lang.Thread(new java.lang.Runnable({
                            run: function() {
                                var err = ttsrv.updateConfigDisplayName(tag, newName);
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(new java.lang.Runnable({
                                    run: function() {
                                        if (err) {
                                            Toast.makeText(ctx, "改名失败: " + err, Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(ctx, "已改为：" + newName, Toast.LENGTH_SHORT).show();
                                            if (onChange) try { onChange(); } catch (e) {}
                                            refreshCharacterList();
                                        }
                                    }
                                }));
                            }
                        })).start();
                        dialog.dismiss();
                    }
                }));
                builder.setNegativeButton("取消", new android.content.DialogInterface.OnClickListener({
                    onClick: function(dialog, which) { dialog.cancel(); }
                }));
                var dlg = builder.show();
                applyDialogRoundCorner(dlg);
            } catch (e) {
                Toast.makeText(ctx, "改名弹窗异常: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }

        // 删除配置项 + 从 fayinren.json 删 tag + 调用朗读规则重分配
        function doDeleteVoiceAndReassign(tag, onChange) {
            try {
                // 1. 找出所有绑该 tag 的角色
                var affectedChars = [];
                for (var i = 0; i < characterRecords.length; i++) {
                    var r = characterRecords[i];
                    if (r && r.voice === tag) {
                        affectedChars.push({ index: i, name: safeGetName(r) });
                    }
                }

                // 2. 确认弹窗
                var msg = "确认删除发音人配置项【" + tag + "】？\n\n";
                msg += "显示名：" + (function() {
                    try { var n = ttsrv.getVoiceByTag(tag); return n || "(无)"; } catch (e) { return "(无)"; }
                })() + "\n\n";
                msg += "受影响角色：" + affectedChars.length + " 个\n";
                if (affectedChars.length > 0) {
                    var names = affectedChars.slice(0, 5).map(function(c) { return c.name; }).join("、");
                    if (affectedChars.length > 5) names += " 等";
                    msg += names + "\n";
                }
                msg += "\n删除后将调用朗读规则，按原逻辑从现存配置项中重新分配受影响角色。";

                new android.app.AlertDialog.Builder(ctx)
                    .setTitle("删除确认")
                    .setMessage(msg)
                    .setPositiveButton("确认删除", new android.content.DialogInterface.OnClickListener({
                        onClick: function(dialog, which) {
                            // 子线程执行删除+重分配，避免卡顿
                            new java.lang.Thread(new java.lang.Runnable({
                                run: function() {
                                    var results = doDeleteVoiceInternal(tag, affectedChars);
                                    new android.os.Handler(android.os.Looper.getMainLooper()).post(new java.lang.Runnable({
                                        run: function() {
                                            Toast.makeText(ctx, results, Toast.LENGTH_LONG).show();
                                            if (onChange) try { onChange(); } catch (e) {}
                                            refreshCharacterList();
                                        }
                                    }));
                                }
                            })).start();
                            dialog.dismiss();
                        }
                    }))
                    .setNegativeButton("取消", new android.content.DialogInterface.OnClickListener({
                        onClick: function(dialog, which) { dialog.cancel(); }
                    }))
                    .show();
            } catch (e) {
                Toast.makeText(ctx, "删除弹窗异常: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }

        // 删除的内部实现（在子线程跑），返回结果文案
        // 流程：删配置项 → 从 fayinren.json 删 tag → 调用朗读规则重分配受影响角色
        function doDeleteVoiceInternal(tag, affectedChars) {
            var log = [];
            // 1. 删配置项
            try {
                var err = ttsrv.deleteConfigByTag(tag);
                if (err) {
                    log.push("删配置项失败：" + err);
                    return log.join("\n");
                }
                log.push("已删配置项：" + tag);
            } catch (e) {
                return "删配置项异常：" + e.toString();
            }

            // 2. 从 fayinren.json 删 tag（避免规则重生该配置项）
            try {
                var raw = ttsrv.readTxtFile("fayinren.json");
                if (raw && raw.trim() !== "") {
                    var arr = JSON.parse(raw);
                    if (Array.isArray(arr)) {
                        var newArr = [];
                        for (var i = 0; i < arr.length; i++) {
                            if (arr[i] !== tag) newArr.push(arr[i]);
                        }
                        ttsrv.writeTxtFile("fayinren.json", JSON.stringify(newArr, null, 2));
                        log.push("已从fayinren.json移除：" + tag + "（剩余" + newArr.length + "条）");
                    }
                }
            } catch (e) {
                log.push("更新fayinren.json失败：" + e.toString());
            }

            // 3. 受影响角色：清空 voice，让朗读规则重新分配
            if (affectedChars.length === 0) {
                log.push("无角色受影响");
                return log.join("\n");
            }

            // 先清空受影响角色的 voice，规则运行时会按原逻辑重新分配
            var cleared = 0;
            for (var j = 0; j < affectedChars.length; j++) {
                var idx = affectedChars[j].index;
                if (characterRecords[idx]) {
                    characterRecords[idx].voice = "";
                    cleared++;
                }
            }
            log.push("已清空" + cleared + "个角色的发音人，准备重分配");

            // 4. 保存（规则运行前先持久化清空状态，避免规则读不到）
            try {
                saveCharacterData();
                createGengxinFile();
            } catch (e) {
                log.push("保存清空状态失败：" + e.toString());
            }

            // 5. 调用朗读规则重分配
            try {
                var ruleErr = runMatchingSpeechRule();
                if (ruleErr) {
                    log.push("朗读规则运行失败：" + ruleErr);
                    log.push("受影响角色标签将变⚠，请手动换发音人");
                } else {
                    log.push("朗读规则已运行，角色已重新分配");
                    // 规则运行后会更新 characterRecords.json，重新加载
                    try {
                        var newJson = ttsrv.readTxtFile("characterRecords.json");
                        if (newJson && newJson.trim() !== "") {
                            var newRecords = JSON.parse(newJson);
                            if (Array.isArray(newRecords)) {
                                characterRecords = newRecords;
                                log.push("角色数据已重新加载");
                            }
                        }
                    } catch (e) {
                        log.push("重新加载角色数据失败：" + e.toString());
                    }
                }
            } catch (e) {
                log.push("调用朗读规则异常：" + e.toString());
            }

            return log.join("\n");
        }

        // 查找并运行与当前插件配套的朗读规则（ruleId == engineId）
        // 成功返回 null，失败返回错误字符串
        function runMatchingSpeechRule() {
            try {
                var listJson = ttsrv.getSpeechRuleList();
                if (!listJson) return "获取朗读规则列表失败";
                var list = JSON.parse(listJson);
                if (!Array.isArray(list) || list.length === 0) {
                    return "未找到任何朗读规则";
                }
                // 优先找 ruleId == 当前插件 engineId 的规则
                var engineId = String(ttsrv.tts.data.engineId || "");
                var targetRule = null;
                for (var i = 0; i < list.length; i++) {
                    if (String(list[i].ruleId) === engineId) {
                        targetRule = list[i];
                        break;
                    }
                }
                // 找不到精确匹配，用第一个
                if (!targetRule) targetRule = list[0];
                if (!targetRule || !targetRule.ruleId) return "未找到有效的朗读规则";

                return ttsrv.runSpeechRule(String(targetRule.ruleId));
            } catch (e) {
                return "运行朗读规则异常：" + e.toString();
            }
        }

        function previewVoiceByName(tag, btn) {
            try {
                if (_pvCurrentBtn === btn && _pvMediaPlayer !== null) {
                    // 用户点击正在播放的按钮，停止播放
                    _pvStop(); return;
                }
                // 切换到新发音人，先停止当前播放
                _pvStop();
                btn.setText("…"); btn.setTextColor(android.graphics.Color.parseColor("#FF6F00"));
                new java.lang.Thread(new java.lang.Runnable({
                    run: function () {
                        try {
                            // 优先尝试通过 app TTS 配置项试听（ttsrv.getAudioByTag）
                            try {
                                var previewText = "你好，这是试听语音。";
                                // tag 直接查当前分组（与标签显示一致）
                                var tagCandidates = [tag];
                                var audioPath = null;
                                for (var ti = 0; ti < tagCandidates.length; ti++) {
                                    try {
                                        audioPath = ttsrv.getAudioByTag(tagCandidates[ti], previewText);
                                        if (audioPath) break;
                                    } catch (eTag) { console.log("getAudioByTag尝试失败(" + tagCandidates[ti] + "): " + eTag.toString()); }
                                }
                                if (audioPath) {
                                    var fileUri = "file://" + audioPath;
                                    var playLabel = tag;
                                    _pvHandler.post(new java.lang.Runnable({ run: function () { _pvPlay(fileUri, playLabel, btn); } }));
                                    return;
                                }
                                // 桥接试听未匹配到配置项，直接提示（不使用本地服务器兜底）
                                _pvHandler.post(new java.lang.Runnable({ run: function () {
                                    _pvStop();
                                    Toast.makeText(ctx, "未匹配到配置项：" + tag, Toast.LENGTH_LONG).show();
                                } }));
                                return;
                            } catch (eAppTts) {
                                _pvHandler.post(new java.lang.Runnable({ run: function () {
                                    _pvStop();
                                    Toast.makeText(ctx, "试听异常：" + eAppTts.toString(), Toast.LENGTH_SHORT).show();
                                } }));
                                return;
                            }
                        } catch (e) { _pvHandler.post(new java.lang.Runnable({ run: function () { _pvStop(); Toast.makeText(ctx, "试听异常：" + e.toString(), Toast.LENGTH_SHORT).show(); } })); }
                    }
                })).start();
            } catch (e) { _pvStop(); Toast.makeText(ctx, "试听异常：" + e.toString(), Toast.LENGTH_SHORT).show(); }
        }

        // 通用发音人筛选弹窗（美化版：自定义卡片列表）
        function showFilteredVoiceDialog(voiceList, callback) {
            var builder = new android.app.AlertDialog.Builder(ctx);
            // voiceList 为 {name, value} 对象数组：name 用于显示(displayName)，value 用于回调(tag)
            var voiceOptions = voiceList;

            // 声明弹窗引用（供点击回调中dismiss使用）
            var voiceDialog;

            // 自定义卡片列表容器
            var voiceContainer = new android.widget.LinearLayout(ctx);
            voiceContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
            voiceContainer.setPadding(dipToPx(16), dipToPx(12), dipToPx(16), dipToPx(16));
            voiceContainer.addView(createDialogTitle("选择新发音人"));

            // 颜色数组：确保相邻项颜色不同
            var dotColors = ["#7E57C2", "#7E57C2", "#26A69A", "#8D6E63", "#66BB6A", "#EC407A", "#FF7043", "#42A5F5"];
            var prevColorIdx = -1;

            for (var vi = 0; vi < voiceOptions.length; vi++) {
                (function(vopt, vidx) {
                    var vrow = new android.widget.LinearLayout(ctx);
                    vrow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    vrow.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    var vparams = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    vparams.setMargins(0, dipToPx(4), 0, dipToPx(4));
                    vrow.setLayoutParams(vparams);

                    var vbg = new android.graphics.drawable.GradientDrawable();
                    vbg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                    vbg.setCornerRadius(dipToPx(10));
                    vbg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                    vbg.setStroke(dipToPx(1), android.graphics.Color.parseColor("#10000000"));
                    vrow.setBackground(vbg);
                    vrow.setPadding(dipToPx(14), dipToPx(12), dipToPx(14), dipToPx(12));
                    vrow.setClickable(true);

                    var vdot = new android.view.View(ctx);
                    var vdotParams = new android.widget.LinearLayout.LayoutParams(dipToPx(7), dipToPx(7));
                    vdotParams.setMargins(0, 0, dipToPx(8), 0);
                    vdot.setLayoutParams(vdotParams);
                    var vdotBg = new android.graphics.drawable.GradientDrawable();
                    vdotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                    // 确保与上一行颜色不同
                    var colorIdx = vidx % dotColors.length;
                    if (colorIdx === prevColorIdx) {
                        colorIdx = (colorIdx + 1) % dotColors.length;
                    }
                    prevColorIdx = colorIdx;
                    var dotColor = dotColors[colorIdx];
                    vdotBg.setColor(android.graphics.Color.parseColor(dotColor));
                    vdot.setBackground(vdotBg);
                    vrow.addView(vdot);

                    var vtext = new android.widget.TextView(ctx);
                    // vopt.name 已在构建 items 时经过 shortenDisplayName 缩减，直接显示
                    vtext.setText(vopt.name);
                    vtext.setTextSize(15);
                    vtext.setTextColor(android.graphics.Color.parseColor("#333333"));
                    var vtextParams = new android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1
                    );
                    vtext.setLayoutParams(vtextParams);
                    vrow.addView(vtext);

                    // === 标记图标（喜欢❤️/路人🚶/坏人😈），emoji 自带颜色，作为选声音参考 ===
                    var _vmark = getVoiceMark(vopt.value);
                    if (_vmark === "like" || _vmark === "neutral" || _vmark === "bad") {
                        var markView = new android.widget.TextView(ctx);
                        markView.setText(_vmark === "like" ? "❤️" : (_vmark === "bad" ? "😈" : "🚶"));
                        markView.setTextSize(14);
                        markView.setSingleLine(true);
                        markView.setGravity(android.view.Gravity.CENTER);
                        markView.setPadding(dipToPx(6), 0, dipToPx(4), 0);
                        vrow.addView(markView);
                    }

                    // === [新增] 试听按钮 ===
                    var pvBtn = new android.widget.TextView(ctx);
                    pvBtn.setText("▶");
                    pvBtn.setTextSize(16);
                    pvBtn.setTextColor(android.graphics.Color.parseColor("#1976D2"));
                    pvBtn.setSingleLine(true);
                    pvBtn.setGravity(android.view.Gravity.CENTER);
                    pvBtn.setPadding(dipToPx(12), dipToPx(8), dipToPx(4), dipToPx(8));
                    var pvBtnLp = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    pvBtn.setLayoutParams(pvBtnLp);
                    var _pvVoiceName = vopt.value;
                    pvBtn.setOnClickListener(new android.view.View.OnClickListener({
                        onClick: function(v) {
                            try { previewVoiceByName(_pvVoiceName, pvBtn); }
                            catch (e) { Toast.makeText(ctx, "试听异常: " + e.toString(), Toast.LENGTH_SHORT).show(); }
                        }
                    }));
                    vrow.addView(pvBtn);

                    // === 发音人管理按钮（⋮）：试听后觉得难听可当场删除/改名 ===
                    var mgBtn = new android.widget.TextView(ctx);
                    mgBtn.setText("⋮");
                    mgBtn.setTextSize(18);
                    mgBtn.setTextColor(android.graphics.Color.parseColor("#757575"));
                    mgBtn.setSingleLine(true);
                    mgBtn.setGravity(android.view.Gravity.CENTER);
                    mgBtn.setPadding(dipToPx(6), dipToPx(8), dipToPx(4), dipToPx(8));
                    var mgBtnLp = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    mgBtnLp.setMargins(dipToPx(2), 0, 0, 0);
                    mgBtn.setLayoutParams(mgBtnLp);
                    var _mgVoiceTag = vopt.value;
                    var _mgDialog = voiceDialog;
                    mgBtn.setOnClickListener(new android.view.View.OnClickListener({
                        onClick: function(v) {
                            try {
                                // 管理弹窗关闭后，本弹窗的列表可能已过期（如删除后），关闭本弹窗
                                showVoiceManageDialog(_mgVoiceTag, function() {
                                    try { _mgDialog.dismiss(); } catch (e) {}
                                });
                            } catch (e) { Toast.makeText(ctx, "管理弹窗异常: " + e.toString(), Toast.LENGTH_SHORT).show(); }
                        }
                    }));
                    vrow.addView(mgBtn);

                    vrow.setOnClickListener(new android.view.View.OnClickListener({
                        onClick: function(view) {
                            var selectedVoice = vopt.value;
                            var selectedName = vopt.name;
                            console.log("新发音人已更改为: " + selectedName);
                            voiceDialog.dismiss();
                            // 延迟执行回调，避免在dialog回调中操作其他dialog导致闪退
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(new java.lang.Runnable({
                                run: function() {
                                    try {
                                        callback(selectedVoice);
                                    } catch (e) {
                                        console.error("发音人选择回调执行失败: " + e.toString());
                                    }
                                    Toast.makeText(ctx, "新发音人已设置为: " + selectedName, Toast.LENGTH_SHORT).show();
                                }
                            }));
                        }
                    }));

                    voiceContainer.addView(vrow);
                })(voiceOptions[vi], vi);
            }

            // 使用ScrollView包装（如果选项很多）
            var scrollView = new android.widget.ScrollView(ctx);
            scrollView.addView(voiceContainer);
            builder.setView(scrollView);

            // 左下角「搜索」按钮
            builder.setNeutralButton("搜索", new android.content.DialogInterface.OnClickListener({
                onClick: function(dialog, which) {
                    dialog.dismiss();
                    showVoiceSearchDialog(callback);
                }
            }));

            // 取消按钮
            builder.setNegativeButton("取消", new android.content.DialogInterface.OnClickListener({
                onClick: function(dialog) {
                    dialog.dismiss();
                }
            }));

            voiceDialog = builder.create();
            voiceDialog.show();
            applyDialogRoundCorner(voiceDialog);
        }
        
        // 通用发音人搜索弹窗（统一复用 showKeywordSelectionDialog，自带自定义关键词+搜索栏）
        function showVoiceSearchDialog(callback) {
            showKeywordSelectionDialog(callback);
        }
        
        // 通用筛选逻辑（每次调用前刷新发音人列表）
        function filterAndShowVoiceList(keyword, callback) {
            refreshFayinrenList();
            // fayinrenList 保持 tag 原值（如"女青年01"），先用 tag 筛选出子集
            var fullVoiceList = fayinrenList.length > 0 ? fayinrenList.slice() : ["默认发音人"];
            fullVoiceList = fullVoiceList.sort(function (a, b) {
                var reA = String(a).match(/^(.+?)(\d+)/);
                var reB = String(b).match(/^(.+?)(\d+)/);
                if (reA && reB) {
                    if (reA[1] !== reB[1]) return reA[1] < reB[1] ? -1 : 1;
                    return parseInt(reA[2]) - parseInt(reB[2]);
                }
                if (reA) return -1;
                if (reB) return 1;
                return String(a) < String(b) ? -1 : 1;
            });
            var filteredList = fullVoiceList;

            if (keyword !== "") {
                var lowerKeyword = keyword.toLowerCase();
                filteredList = fullVoiceList.filter(function(voice) {
                    return String(voice).toLowerCase().indexOf(lowerKeyword) !== -1;
                });
            }

            if (filteredList.length === 0) {
                Toast.makeText(ctx, "未找到包含「" + keyword + "」的发音人", Toast.LENGTH_SHORT).show();
                filteredList = fullVoiceList;
            }

            // 子线程对筛选子集查 getVoiceByTag 获取 displayName，避免主线程卡顿
            var mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

            new java.lang.Thread(new java.lang.Runnable({
                run: function () {
                    try {
                        var items = [];
                        for (var i = 0; i < filteredList.length; i++) {
                            var tag = filteredList[i];
                            var displayName = tag;
                            try {
                                var liveName = ttsrv.getVoiceByTag(tag);
                                if (liveName) displayName = shortenDisplayName(liveName);
                            } catch (e) {}
                            items.push({ name: displayName, value: tag });
                        }
                        mainHandler.post(new java.lang.Runnable({
                            run: function () {
                                try { showFilteredVoiceDialog(items, callback); }
                                catch (e) { Toast.makeText(ctx, "加载发音人失败: " + e.toString(), Toast.LENGTH_SHORT).show(); }
                            }
                        }));
                    } catch (e) {
                        mainHandler.post(new java.lang.Runnable({
                            run: function () {
                                Toast.makeText(ctx, "加载发音人失败: " + e.toString(), Toast.LENGTH_SHORT).show();
                            }
                        }));
                    }
                }
            })).start();
        }
        
        // 保留原有辅助函数
        function dipToPx(dip) {
            return Math.round(dip * ctx.getResources().getDisplayMetrics().density);
        }
        
        // 合并时弹出关键词选择弹窗
        function showVoiceSelectionDialogForMerge() {
            showKeywordSelectionDialog(function(selectedVoice) {
                doMergeOperation(selectedVoice);
            });
        }
        
        // ============== 自定义关键词（运行时可增删，持久化到文件） ==============
        var CUSTOM_KEYWORDS_FILE = "custom_keywords.json";
        var _customKeywordsCache = null;
        function loadCustomKeywords() {
            if (_customKeywordsCache !== null) return _customKeywordsCache;
            try {
                var raw = ttsrv.readTxtFile(CUSTOM_KEYWORDS_FILE);
                if (raw && raw.trim() !== "") {
                    var arr = JSON.parse(raw);
                    if (Array.isArray(arr)) {
                        _customKeywordsCache = arr;
                        return arr;
                    }
                }
            } catch (e) {
                console.log("读取自定义关键词失败: " + e.toString());
            }
            _customKeywordsCache = [];
            return [];
        }
        function saveCustomKeywords(arr) {
            _customKeywordsCache = arr;
            try {
                ttsrv.writeTxtFile(CUSTOM_KEYWORDS_FILE, JSON.stringify(arr, null, 2));
            } catch (e) {
                console.error("保存自定义关键词失败: " + e.toString());
            }
        }
        function addCustomKeyword(kw) {
            kw = String(kw).trim();
            if (kw === "") return false;
            var arr = loadCustomKeywords();
            for (var i = 0; i < arr.length; i++) {
                if (String(arr[i]).trim() === kw) return false; // 已存在
            }
            arr.push(kw);
            saveCustomKeywords(arr);
            return true;
        }
        function removeCustomKeyword(kw) {
            var arr = loadCustomKeywords();
            var newArr = [];
            for (var i = 0; i < arr.length; i++) {
                if (String(arr[i]).trim() !== String(kw).trim()) newArr.push(arr[i]);
            }
            saveCustomKeywords(newArr);
        }

        // 关键词选择弹窗（6行2列关键词网格 + 自定义关键词 + 搜索栏）
        function showKeywordSelectionDialog(callback) {
            var builder = new android.app.AlertDialog.Builder(ctx);
            var rootLayout = new android.widget.LinearLayout(ctx);
            rootLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
            rootLayout.setPadding(dipToPx(16), dipToPx(12), dipToPx(16), dipToPx(16));
            rootLayout.addView(createDialogTitle("选择关键词"));

            // 12个关键词，6行2列，按用户指定顺序排列
            var keywordRows = [
                ["女童", "男童"],
                ["少女", "少年"],
                ["女青年", "男青年"],
                ["女中年", "男中年"],
                ["女老年", "男老年"],
                ["女主", "男主"]
            ];
            // 每行不同颜色，确保相邻行颜色不同
            var rowColors = ["#7E57C2", "#26A69A", "#FB8C00", "#42A5F5", "#66BB6A", "#EC407A"];

            var gridContainer = new android.widget.LinearLayout(ctx);
            gridContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
            gridContainer.setGravity(android.view.Gravity.CENTER);

            // 先声明弹窗引用，供点击回调使用
            var keywordDialog;

            for (var ri = 0; ri < keywordRows.length; ri++) {
                var rowLayout = new android.widget.LinearLayout(ctx);
                rowLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                rowLayout.setGravity(android.view.Gravity.CENTER);
                var rowColor = rowColors[ri % rowColors.length];

                for (var ci = 0; ci < keywordRows[ri].length; ci++) {
                    (function(kw, color) {
                        var btn = createDialogSmallButton(kw, color);
                        var btnParams = new android.widget.LinearLayout.LayoutParams(
                            0,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            1
                        );
                        btnParams.setMargins(dipToPx(5), dipToPx(5), dipToPx(5), dipToPx(5));
                        btn.setLayoutParams(btnParams);

                        btn.setOnClickListener(new android.view.View.OnClickListener({
                            onClick: function(view) {
                                if (keywordDialog) keywordDialog.dismiss();
                                filterAndShowVoiceList(kw, callback);
                            }
                        }));

                        rowLayout.addView(btn);
                    })(keywordRows[ri][ci], rowColor);
                }
                gridContainer.addView(rowLayout);
            }
            // === 自定义关键词：按一行两列网格直接显示，点击即搜索 ===
            var customKws = loadCustomKeywords();
            if (customKws && customKws.length > 0) {
                // 小标题
                var customTitle = new android.widget.TextView(ctx);
                customTitle.setText("自定义关键词");
                customTitle.setTextSize(13);
                customTitle.setTextColor(android.graphics.Color.parseColor("#888888"));
                var customTitleLp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                );
                customTitleLp.setMargins(dipToPx(5), dipToPx(10), dipToPx(5), dipToPx(2));
                customTitle.setLayoutParams(customTitleLp);
                gridContainer.addView(customTitle);

                // 自定义关键词颜色（低调灰蓝，与预设区分但不抢眼）
                var customColor = "#EF6C00";

                // 按一行两列分组
                for (var ci = 0; ci < customKws.length; ci += 2) {
                    var customRow = new android.widget.LinearLayout(ctx);
                    customRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    customRow.setGravity(android.view.Gravity.CENTER);

                    // 当前行关键词（最多2个）
                    var pair = [];
                    pair.push(customKws[ci]);
                    if (ci + 1 < customKws.length) pair.push(customKws[ci + 1]);

                    for (var pj = 0; pj < pair.length; pj++) {
                        (function(kw, color) {
                            var btn = createDialogSmallButton(kw, color);
                            var btnParams = new android.widget.LinearLayout.LayoutParams(
                                0,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                1
                            );
                            btnParams.setMargins(dipToPx(5), dipToPx(5), dipToPx(5), dipToPx(5));
                            btn.setLayoutParams(btnParams);

                            btn.setOnClickListener(new android.view.View.OnClickListener({
                                onClick: function(view) {
                                    if (keywordDialog) keywordDialog.dismiss();
                                    filterAndShowVoiceList(kw, callback);
                                }
                            }));

                            customRow.addView(btn);
                        })(pair[pj], customColor);
                    }

                    // 奇数个时补一个占位空 View，保持两列对齐
                    if (pair.length === 1) {
                        var placeholder = new android.view.View(ctx);
                        var phLp = new android.widget.LinearLayout.LayoutParams(
                            0,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            1
                        );
                        phLp.setMargins(dipToPx(5), dipToPx(5), dipToPx(5), dipToPx(5));
                        placeholder.setLayoutParams(phLp);
                        customRow.addView(placeholder);
                    }

                    gridContainer.addView(customRow);
                }
            }

            // === 自定义关键词管理入口：仅保留"添加"和"管理"两个入口 ===
            var kwManageRow = new android.widget.LinearLayout(ctx);
            kwManageRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            kwManageRow.setGravity(android.view.Gravity.CENTER);
            var kwManageRowLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            kwManageRowLp.setMargins(dipToPx(5), dipToPx(8), dipToPx(5), dipToPx(2));
            kwManageRow.setLayoutParams(kwManageRowLp);

            // + 添加按钮（小尺寸）
            var addBtn = new android.widget.TextView(ctx);
            addBtn.setText("+ 添加");
            addBtn.setTextSize(12);
            addBtn.setTextColor(android.graphics.Color.parseColor("#1976D2"));
            addBtn.setGravity(android.view.Gravity.CENTER);
            addBtn.setPadding(dipToPx(8), dipToPx(4), dipToPx(8), dipToPx(4));
            var addBtnBg = new android.graphics.drawable.GradientDrawable();
            addBtnBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            addBtnBg.setCornerRadius(dipToPx(6));
            addBtnBg.setColor(android.graphics.Color.parseColor("#E3F2FD"));
            addBtn.setBackground(addBtnBg);
            var addBtnLp = new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1
            );
            addBtnLp.setMargins(0, 0, dipToPx(3), 0);
            addBtn.setLayoutParams(addBtnLp);
            addBtn.setOnClickListener(new android.view.View.OnClickListener({
                onClick: function(view) {
                    showInputDialog("输入自定义关键词", function(inputText) {
                        var kw = (inputText || "").trim();
                        if (kw === "") {
                            Toast.makeText(ctx, "关键词不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (addCustomKeyword(kw)) {
                            Toast.makeText(ctx, "已添加「" + kw + "」", Toast.LENGTH_SHORT).show();
                            if (keywordDialog) keywordDialog.dismiss();
                            showKeywordSelectionDialog(callback);
                        } else {
                            Toast.makeText(ctx, "该关键词已存在", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }));
            kwManageRow.addView(addBtn);

            // ✎ 管理按钮（小尺寸）
            var manageBtn = new android.widget.TextView(ctx);
            manageBtn.setText("✎ 管理");
            manageBtn.setTextSize(12);
            manageBtn.setTextColor(android.graphics.Color.parseColor("#616161"));
            manageBtn.setGravity(android.view.Gravity.CENTER);
            manageBtn.setPadding(dipToPx(8), dipToPx(4), dipToPx(8), dipToPx(4));
            var manageBtnBg = new android.graphics.drawable.GradientDrawable();
            manageBtnBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            manageBtnBg.setCornerRadius(dipToPx(6));
            manageBtnBg.setColor(android.graphics.Color.parseColor("#F5F5F5"));
            manageBtn.setBackground(manageBtnBg);
            var manageBtnLp = new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1
            );
            manageBtnLp.setMargins(dipToPx(3), 0, 0, 0);
            manageBtn.setLayoutParams(manageBtnLp);
            manageBtn.setOnClickListener(new android.view.View.OnClickListener({
                onClick: function(view) {
                    if (keywordDialog) keywordDialog.dismiss();
                    showCustomKeywordManageDialog(callback);
                }
            }));
            kwManageRow.addView(manageBtn);

            gridContainer.addView(kwManageRow);

            // 用 ScrollView 包裹关键词区域，数量多时可滑动
            var keywordScroll = new android.widget.ScrollView(ctx);
            keywordScroll.addView(gridContainer);
            rootLayout.addView(keywordScroll);

            // 下方搜索栏
            var searchInput = createStyledEditText("输入关键词搜索", true);
            searchInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            searchInput.setMinHeight(dipToPx(40));
            var searchParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            searchParams.setMargins(0, dipToPx(10), 0, 0);
            searchInput.setLayoutParams(searchParams);
            rootLayout.addView(searchInput);

            // 使用 setView 而非 setContentView（与其他弹窗保持一致）
            builder.setView(rootLayout);

            // 确定按钮：搜索筛选
            builder.setPositiveButton("确定", new android.content.DialogInterface.OnClickListener({
                onClick: function(dialog, which) {
                    var kw = searchInput.getText().toString().trim();
                    dialog.dismiss();
                    filterAndShowVoiceList(kw, callback);
                }
            }));

            // 取消按钮
            builder.setNegativeButton("取消", new android.content.DialogInterface.OnClickListener({
                onClick: function(dialog) {
                    dialog.dismiss();
                }
            }));

            keywordDialog = builder.create();
            keywordDialog.show();
            applyDialogRoundCorner(keywordDialog);
        }

        // 自定义关键词管理弹窗：列出所有自定义关键词，可删除/添加
        function showCustomKeywordManageDialog(callback) {
            var mgrBuilder = new android.app.AlertDialog.Builder(ctx);
            var mgrScroll = new android.widget.ScrollView(ctx);
            var mgrRoot = new android.widget.LinearLayout(ctx);
            mgrRoot.setOrientation(android.widget.LinearLayout.VERTICAL);
            mgrRoot.setPadding(dipToPx(20), dipToPx(12), dipToPx(20), dipToPx(16));
            mgrRoot.addView(createDialogTitle("管理自定义关键词"));
            mgrScroll.addView(mgrRoot);

            var keywords = loadCustomKeywords();
            var mgrDialog = null;

            function rebuildList() {
                // 移除旧的列表区域（保留标题和底部按钮区）
                var childCount = mgrRoot.getChildCount();
                // 从后往前移除，直到只剩标题（index 0）
                while (mgrRoot.getChildCount() > 1) {
                    mgrRoot.removeViewAt(mgrRoot.getChildCount() - 1);
                }
                keywords = loadCustomKeywords();

                if (keywords.length === 0) {
                    // 无关键词时不显示提示文字，直接显示添加输入框
                } else {
                    for (var i = 0; i < keywords.length; i++) {
                        (function(kw, idx) {
                            var row = new android.widget.LinearLayout(ctx);
                            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                            var rowLp = new android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            rowLp.setMargins(0, dipToPx(3), 0, dipToPx(3));
                            row.setLayoutParams(rowLp);

                            var kwText = new android.widget.TextView(ctx);
                            kwText.setText((idx + 1) + ". " + kw);
                            kwText.setTextSize(14);
                            kwText.setTextColor(getAdaptiveTextColor());
                            var kwLp = new android.widget.LinearLayout.LayoutParams(
                                0,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                1
                            );
                            kwText.setLayoutParams(kwLp);
                            // 点击关键词直接搜索
                            kwText.setOnClickListener(new android.view.View.OnClickListener({
                                onClick: function(v) {
                                    if (mgrDialog) mgrDialog.dismiss();
                                    filterAndShowVoiceList(kw, callback);
                                }
                            }));
                            row.addView(kwText);

                            var delBtn = createDialogSmallButton("删除", "#F44336");
                            var delLp = new android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            delBtn.setLayoutParams(delLp);
                            delBtn.setOnClickListener(new android.view.View.OnClickListener({
                                onClick: function(v) {
                                    removeCustomKeyword(kw);
                                    Toast.makeText(ctx, "已删除「" + kw + "」", Toast.LENGTH_SHORT).show();
                                    rebuildList();
                                }
                            }));
                            row.addView(delBtn);

                            mgrRoot.addView(row);
                        })(keywords[i], i);
                    }
                }

                // 添加关键词输入行
                var addRow = new android.widget.LinearLayout(ctx);
                addRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                addRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
                var addRowLp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                );
                addRowLp.setMargins(0, dipToPx(10), 0, dipToPx(4));
                addRow.setLayoutParams(addRowLp);

                var addInput = createStyledEditText("输入新关键词", true);
                addInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
                var addInputLp = new android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
                );
                addInput.setLayoutParams(addInputLp);
                addRow.addView(addInput);

                var addConfirmBtn = createDialogSmallButton("添加", "#7E57C2");
                addRow.addView(addConfirmBtn);
                addConfirmBtn.setOnClickListener(new android.view.View.OnClickListener({
                    onClick: function(v) {
                        var kw = addInput.getText().toString().trim();
                        if (kw === "") {
                            Toast.makeText(ctx, "关键词不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (addCustomKeyword(kw)) {
                            Toast.makeText(ctx, "已添加「" + kw + "」", Toast.LENGTH_SHORT).show();
                            rebuildList();
                        } else {
                            Toast.makeText(ctx, "该关键词已存在", Toast.LENGTH_SHORT).show();
                        }
                    }
                }));

                mgrRoot.addView(addRow);
            }

            rebuildList();

            mgrBuilder.setView(mgrScroll);
            mgrBuilder.setPositiveButton("完成", new android.content.DialogInterface.OnClickListener({
                onClick: function(dialog) {
                    dialog.dismiss();
                    // 返回关键词选择弹窗
                    showKeywordSelectionDialog(callback);
                }
            }));
            mgrDialog = mgrBuilder.create();
            mgrDialog.show();
            applyDialogRoundCorner(mgrDialog);
        }

        // 点击发音人标签触发更换发音人（按角色索引）
        function showVoiceSelectionDialogForFixByIndex(charIndex) {
            if (charIndex < 0 || !characterRecords[charIndex]) {
                Toast.makeText(ctx, "角色无效", Toast.LENGTH_SHORT).show();
                return;
            }
            longPressedIndex = charIndex;
            showVoiceSelectionDialogForFix();
        }

        // 更换发音人时直接弹出搜索弹窗
        function showVoiceSelectionDialogForFix() {
            if (longPressedIndex === -1 || !characterRecords[longPressedIndex]) {
                showKeywordSelectionDialog(function(selectedVoice) {
                    doFixVoiceOperation(selectedVoice);
                });
                return;
            }
            
            var character = characterRecords[longPressedIndex];
            var currentVoice = (character.voice || "").trim();
            
            // 如果没有当前发音人，直接进关键词选择弹窗
            if (!currentVoice) {
                showKeywordSelectionDialog(function(selectedVoice) {
                    doFixVoiceOperation(selectedVoice);
                });
                return;
            }
            
            // 检查当前发音人是否匹配某个预设关键词
            var matchedKeyword = null;
            var allKeywords = PRESET_KEYWORDS_ROW1.concat(PRESET_KEYWORDS_ROW2)
                .concat(PRESET_KEYWORDS_ROW3).concat(PRESET_KEYWORDS_ROW4)
                .concat(PRESET_KEYWORDS_ROW5).concat(PRESET_KEYWORDS_ROW6);
            for (var ki = 0; ki < allKeywords.length; ki++) {
                if (currentVoice.indexOf(allKeywords[ki]) !== -1) {
                    matchedKeyword = allKeywords[ki];
                    break;
                }
            }
            
            // 如果没匹配到关键词，直接进关键词选择弹窗
            if (!matchedKeyword) {
                showKeywordSelectionDialog(function(selectedVoice) {
                    doFixVoiceOperation(selectedVoice);
                });
                return;
            }
            
            // 弹出选择：用当前关键词筛选 or 搜索其他
            var fixContainer = new android.widget.LinearLayout(ctx);
            fixContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
            fixContainer.setPadding(dipToPx(16), dipToPx(12), dipToPx(16), dipToPx(32));
            
            // 小标题
            fixContainer.addView(createDialogTitle("更换发音人"));
            
            // 当前发音人提示（不显示匹配关键词）
            var hintView = new android.widget.TextView(ctx);
            hintView.setText("当前发音人：" + currentVoice);
            hintView.setTextSize(13);
            hintView.setTextColor(android.graphics.Color.parseColor("#757575"));
            hintView.setGravity(android.view.Gravity.CENTER);
            var hintParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            hintParams.setMargins(0, 0, 0, dipToPx(12));
            hintView.setLayoutParams(hintParams);
            fixContainer.addView(hintView);
            
            // 选项配置
            var fixOptions = [
                { text: "筛选「" + matchedKeyword + "」类发音人", color: "#43A047", icon: "" },
                { text: "搜索其他关键词", color: "#1976D2", icon: "" }
            ];
            
            for (var fi = 0; fi < fixOptions.length; fi++) {
                (function(cfg) {
                    var row = new android.widget.LinearLayout(ctx);
                    row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    row.setGravity(android.view.Gravity.CENTER);
                    var rowParams = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    rowParams.setMargins(0, dipToPx(4), 0, dipToPx(4));
                    row.setLayoutParams(rowParams);
                    
                    var bg = new android.graphics.drawable.GradientDrawable();
                    bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                    bg.setCornerRadius(dipToPx(10));
                    bg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                    bg.setStroke(dipToPx(1), android.graphics.Color.parseColor("#10000000"));
                    row.setBackground(bg);
                    row.setPadding(dipToPx(14), dipToPx(12), dipToPx(14), dipToPx(12));
                    row.setClickable(true);
                    
                    // 简约彩色圆点点缀
                    var accentDot = new android.view.View(ctx);
                    var dotParams = new android.widget.LinearLayout.LayoutParams(dipToPx(7), dipToPx(7));
                    dotParams.setMargins(0, 0, dipToPx(8), 0);
                    accentDot.setLayoutParams(dotParams);
                    var dotBg = new android.graphics.drawable.GradientDrawable();
                    dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                    dotBg.setColor(android.graphics.Color.parseColor(cfg.color));
                    accentDot.setBackground(dotBg);
                    row.addView(accentDot);
                    
                    var iconView = new android.widget.TextView(ctx);
                    iconView.setText(cfg.icon);
                    iconView.setTextSize(16);
                    iconView.setTextColor(android.graphics.Color.parseColor("#333333"));
                    var iconParams = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    iconParams.setMargins(0, 0, dipToPx(12), 0);
                    iconView.setLayoutParams(iconParams);
                    row.addView(iconView);
                    
                    var textView = new android.widget.TextView(ctx);
                    textView.setText(cfg.text);
                    textView.setTextSize(15);
                    textView.setTextColor(android.graphics.Color.parseColor("#333333"));
                    textView.setGravity(android.view.Gravity.CENTER);
                    var textParams = new android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1
                    );
                    textView.setLayoutParams(textParams);
                    row.addView(textView);
                    
                    row.setOnClickListener(new android.view.View.OnClickListener({
                        onClick: function(view) {
                            fixChoiceDialog.dismiss();
                            if (cfg.text.indexOf("筛选") === 0) {
                                // 用当前关键词直接筛选
                                filterAndShowVoiceList(matchedKeyword, function(selectedVoice) {
                                    doFixVoiceOperation(selectedVoice);
                                });
                            } else {
                                // 进关键词选择弹窗
                                showKeywordSelectionDialog(function(selectedVoice) {
                                    doFixVoiceOperation(selectedVoice);
                                });
                            }
                        }
                    }));
                    
                    fixContainer.addView(row);
                })(fixOptions[fi]);
            }
            
            var fixBuilder = new android.app.AlertDialog.Builder(ctx);
            fixBuilder.setView(fixContainer);
            
            var fixChoiceDialog = fixBuilder.create();
            fixChoiceDialog.show();
            applyDialogRoundCorner(fixChoiceDialog);
        }
        
        
        
        // 性别年龄搜索弹窗（复用发音人搜索弹窗结构）
        function setAsMainCharacter() {
            if (longPressedIndex === -1) {
                Toast.makeText(ctx, "请长按一个角色", Toast.LENGTH_SHORT).show();
                return;
            }
            
            backupOriginalData(); // 保留原有备份机制
            
            try {
                var character = characterRecords[longPressedIndex];
                if (character) {
                    character.age = '主角'; // 设置年龄为"主角"
                    character.usageCount = 100; // 设置固定状态标记
                    
                    // 保存数据并刷新列表（复用原有函数，确保一致性）
                    saveCharacterData();
                    createGengxinFile();
                    refreshCharacterList();
                    
                    Toast.makeText(ctx, "已将角色「" + safeGetName(character) + "」设为主角", Toast.LENGTH_SHORT).show();
                }
            } catch (e) {
                console.error("设为主角失败: " + e.toString());
                Toast.makeText(ctx, "操作失败: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
         
        
        
        
        // 执行合并操作（支持多角色合并到目标角色）
        function doMergeOperation(newVoiceName) {
            var targetRecord = null;
            var targetIndex = -1;
            if (selectedIndex >= 0 && markedIndices.indexOf(selectedIndex) !== -1) {
                targetRecord = characterRecords[selectedIndex];
                targetIndex = selectedIndex;
            }
            
            // 单角色场景：仅更新发音人
            if (markedIndices.length === 1) {
                if (!targetRecord) {
                    Toast.makeText(ctx, "请标记并选中一个角色", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                try {
                    if (newVoiceName !== "follow_target") {
                        targetRecord.voice = newVoiceName; // 仅更新当前角色发音人
                    }
                    
                    saveCharacterData();
                    createGengxinFile();
                    
                    Toast.makeText(ctx, "角色发音人更新成功", Toast.LENGTH_SHORT).show();
                    
                    selectedIndex = -1;
                    markedIndices = [];
                    clearListChoices();
                    refreshCharacterList();
                    
                } catch (e) {
                    console.error("更新发音人失败: " + e.toString());
                    Toast.makeText(ctx, "更新失败: " + e.toString(), Toast.LENGTH_SHORT).show();
                }
                return;
            }
            
            // 多角色合并场景：校验目标角色
            if (!targetRecord) {
                Toast.makeText(ctx, "请标记并选中一个角色作为目标角色", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 收集待合并角色（排除目标角色）
            var mergeRecords = [];
            for (var i = 0; i < markedIndices.length; i++) {
                var index = markedIndices[i];
                if (index !== targetIndex) {
                    mergeRecords.push(characterRecords[index]);
                }
            }
            
            if (mergeRecords.length === 0) {
                Toast.makeText(ctx, "请标记至少一个要合并的角色", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                var normalizedTargetName = normalizeString(safeGetName(targetRecord));
                var allAliases = {};
                
                // 收集目标角色的别名
                if (targetRecord.aliases) {
                    var targetAliases = splitAliases(targetRecord.aliases);
                    for (var i = 0; i < targetAliases.length; i++) {
                        allAliases[normalizeString(targetAliases[i])] = true;
                    }
                } else {
                    allAliases[normalizeString(targetRecord.name)] = true;
                }

                // 收集待合并角色的别名
                for (var i = 0; i < mergeRecords.length; i++) {
                    var mergeRecord = mergeRecords[i];
                    if (mergeRecord.aliases) {
                        var mergeAliases = splitAliases(mergeRecord.aliases);
                        for (var j = 0; j < mergeAliases.length; j++) {
                            allAliases[normalizeString(mergeAliases[j])] = true;
                        }
                    } else {
                        allAliases[normalizeString(mergeRecord.name)] = true;
                    }
                }
                
                // 合并别名（去重）
                var mergedAliases = [];
                for (var alias in allAliases) {
                    mergedAliases.push(alias);
                }
                targetRecord.aliases = mergedAliases.join('|');
                
                // 若指定新发音人，更新目标角色发音人
                if (newVoiceName && newVoiceName !== "follow_target") {
                    targetRecord.voice = newVoiceName;
                }
                
                // 移除待合并角色（保留目标角色）
                var newCharacterRecords = [];
                for (var i = 0; i < characterRecords.length; i++) {
                    var skip = false;
                    for (var j = 0; j < mergeRecords.length; j++) {
                        if (characterRecords[i] === mergeRecords[j]) {
                            skip = true;
                            break;
                        }
                    }
                    if (!skip) {
                        newCharacterRecords.push(characterRecords[i]);
                    }
                }
                characterRecords = newCharacterRecords;
                
                // 保存并刷新
                saveCharacterData();
                createGengxinFile();
                
                Toast.makeText(ctx, "角色合并成功", Toast.LENGTH_SHORT).show();
                
                selectedIndex = -1;
                markedIndices = [];
                clearListChoices();
                refreshCharacterList();
                
            } catch (e) {
                console.error("合并操作失败: " + e.toString());
                Toast.makeText(ctx, "合并失败: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        } 
  
  
  // 执行释放角色操作（从别名拆分新角色，支持自定义选择要拆分的别名）
        function doReleaseOperation() {
            backupOriginalData();
            
            if (markedIndices.length === 0) {
                Toast.makeText(ctx, "请标记至少一个角色", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 去重 + 倒序排列（避免索引混乱）
            var uniqueMarked = [];
            for (var mi = 0; mi < markedIndices.length; mi++) {
                if (uniqueMarked.indexOf(markedIndices[mi]) === -1) {
                    uniqueMarked.push(markedIndices[mi]);
                }
            }
            var processQueue = uniqueMarked.sort(function(a, b) { return b - a; });
            var operationCount = 0;

            function processNext() {
                if (processQueue.length === 0) {
                    // 全部处理完，仅在有过操作时才保存刷新+提示
                    if (operationCount > 0) {
                        saveCharacterData();
                        createGengxinFile();
                        selectedIndex = -1;
                        markedIndices = [];
                        clearListChoices();
                        refreshCharacterList();
                        Toast.makeText(ctx, "操作完成（" + operationCount + "项）", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                
                var index = processQueue.shift();
                var character = characterRecords[index];
                
                if (!character) {
                    console.log("processNext: 角色索引 " + index + " 无效，跳过");
                    processNext();
                    return;
                }
                
                // 不再跳过没有别名的角色，直接显示弹窗让用户操作
                var charName = (character.name || "").trim();
                
                // 获取所有名字（主名+别名）
                var allAliases = splitAliases(character.aliases);

                // 合并主名+别名，去重（主名放第一个）
                var allNames = [charName];
                for (var i = 0; i < allAliases.length; i++) {
                    if (allAliases[i] !== charName && allNames.indexOf(allAliases[i]) === -1) {
                        allNames.push(allAliases[i]);
                    }
                }

                if (allNames.length === 0) {
                    processNext();
                    return;
                }

                // 自定义弹窗：每个名字一行卡片，可独立操作
                var releaseScroll = new android.widget.ScrollView(ctx);
                var releaseRoot = new android.widget.LinearLayout(ctx);
                releaseRoot.setOrientation(android.widget.LinearLayout.VERTICAL);
                releaseRoot.setPadding(dipToPx(16), dipToPx(12), dipToPx(16), dipToPx(16));
                releaseRoot.addView(createDialogTitle("释放/删除已合并角色"));

                var nameViews = [];
                for (var ni = 0; ni < allNames.length; ni++) {
                    (function(name) {
                        var row = new android.widget.LinearLayout(ctx);
                        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                        var rowParams = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        rowParams.setMargins(0, dipToPx(4), 0, dipToPx(4));
                        row.setLayoutParams(rowParams);
                        
                        // 圆角卡片背景
                        var cardBg = new android.graphics.drawable.GradientDrawable();
                        cardBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                        cardBg.setCornerRadius(dipToPx(10));
                        cardBg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                        cardBg.setStroke(dipToPx(1), android.graphics.Color.parseColor("#10000000"));
                        row.setBackground(cardBg);
                        row.setPadding(dipToPx(14), dipToPx(12), dipToPx(14), dipToPx(12));

                        // 名字标签（小圆点+文字）
                        var dotView = new android.view.View(ctx);
                        var dotParams = new android.widget.LinearLayout.LayoutParams(dipToPx(8), dipToPx(8));
                        dotParams.setMargins(0, 0, dipToPx(10), 0);
                        dotView.setLayoutParams(dotParams);
                        var dotBg = new android.graphics.drawable.GradientDrawable();
                        dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                        // 颜色数组，确保相邻不同
                        var releaseDotColors = ["#7E57C2", "#7E57C2", "#26A69A", "#8D6E63", "#66BB6A", "#EC407A", "#FF7043", "#42A5F5"];
                        dotBg.setColor(android.graphics.Color.parseColor(releaseDotColors[ni % releaseDotColors.length]));
                        dotView.setBackground(dotBg);
                        row.addView(dotView);

                        var nameText = new android.widget.TextView(ctx);
                        nameText.setText(name);
                        nameText.setTextSize(15);
                        nameText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                        nameText.setTextColor(android.graphics.Color.parseColor("#333333"));
                        var textParams = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                        nameText.setLayoutParams(textParams);
                        row.addView(nameText);

                        // 释放并固定按钮（绿色圆角+图标）
                        var releaseBtn = new android.widget.TextView(ctx);
                        releaseBtn.setText("释放并固定");
                        releaseBtn.setTextSize(13);
                        releaseTextBtnStyle(releaseBtn, "#2E7D32");
                        var btnParams = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        btnParams.setMargins(dipToPx(6), 0, 0, 0);
                        releaseBtn.setLayoutParams(btnParams);
                        row.addView(releaseBtn);

                        // 删除按钮（红色圆角+图标）
                        var deleteBtn = new android.widget.TextView(ctx);
                        deleteBtn.setText("删除");
                        deleteBtn.setTextSize(13);
                        releaseTextBtnStyle(deleteBtn, "#C62828");
                        var delParams = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        delParams.setMargins(dipToPx(6), 0, 0, 0);
                        deleteBtn.setLayoutParams(delParams);
                        row.addView(deleteBtn);

                        // 释放并固定按钮点击
                        releaseBtn.setOnClickListener(new android.view.View.OnClickListener({
                            onClick: function(view) {
                                // 先选择发音人（关键词弹窗），确认后再执行释放
                                showKeywordSelectionDialog(function(selectedVoice) {
                                    // 从原角色移除这个名字
                                    var charRemoved = false;
                                    if (name === character.name) {
                                        // 从 character 实时获取名字列表（避免 allNames 陈旧）
                                        var liveAliases = splitAliases(character.aliases);
                                        var remaining = [];
                                        for (var k = 0; k < liveAliases.length; k++) {
                                            if (liveAliases[k] !== name && remaining.indexOf(liveAliases[k]) === -1) remaining.push(liveAliases[k]);
                                        }
                                        if (remaining.length > 0) {
                                            character.name = remaining[0];
                                            character.aliases = remaining.join("|");
                                        } else {
                                            characterRecords.splice(index, 1);
                                            charRemoved = true;
                                        }
                                    } else {
                                        var newAliases = [];
                                        var currentAliases = splitAliases(character.aliases);
                                        for (var k = 0; k < currentAliases.length; k++) {
                                            if (currentAliases[k] !== name) newAliases.push(currentAliases[k]);
                                        }
                                        character.aliases = newAliases.join("|");
                                    }

                                    // 创建新角色或固定已有角色
                                    var exists = false;
                                    var newIndex = -1;
                                    for (var j = 0; j < characterRecords.length; j++) {
                                        if ((characterRecords[j].name || "").trim() === name) {
                                            exists = true;
                                            newIndex = j;
                                            break;
                                        }
                                    }
                                    if (!exists) {
                                        var newRecord = {
                                            name: name,
                                            aliases: "",
                                            voice: selectedVoice,
                                            gender: "",
                                            age: "",
                                            usageCount: 100
                                        };
                                        characterRecords.splice(index + 1, 0, newRecord);
                                        newIndex = index + 1;
                                    } else {
                                        if (characterRecords[newIndex]) {
                                            characterRecords[newIndex].voice = selectedVoice;
                                            characterRecords[newIndex].usageCount = 100;
                                        }
                                    }

                                    saveCharacterData();
                                    createGengxinFile();
                                    refreshCharacterList();

                                    // 禁用当前行按钮
                                    operationCount++;
                                    releaseBtn.setEnabled(false);
                                    releaseBtn.setAlpha(0.5);
                                    deleteBtn.setEnabled(false);
                                    deleteBtn.setAlpha(0.5);
                                    nameText.setText(name + " ✓已释放并固定");
                                    nameText.setTextColor(android.graphics.Color.parseColor("#7E57C2"));
                                    Toast.makeText(ctx, "已释放并固定：" + name + "，发音人：" + selectedVoice, Toast.LENGTH_SHORT).show();

                                    if (charRemoved && releaseDialog) {
                                        try {
                                            releaseDialog.dismiss();
                                            new android.os.Handler(android.os.Looper.getMainLooper()).post(new java.lang.Runnable({
                                                run: function() {
                                                    try { processNext(); } catch (e) { console.error("processNext失败: " + e.toString()); }
                                                }
                                            }));
                                        } catch (e) { console.error("dismiss失败: " + e.toString()); }
                                    }
                                });
                            }
                        }));

                        // 删除按钮点击
                        deleteBtn.setOnClickListener(new android.view.View.OnClickListener({
                            onClick: function(view) {
                                var charRemoved = false;
                                if (name === character.name) {
                                    // 从 character 实时获取名字列表（避免 allNames 陈旧）
                                    var liveAliases = splitAliases(character.aliases);
                                    var remaining = [];
                                    for (var k = 0; k < liveAliases.length; k++) {
                                        if (liveAliases[k] !== name && remaining.indexOf(liveAliases[k]) === -1) remaining.push(liveAliases[k]);
                                    }
                                    if (remaining.length > 0) {
                                        character.name = remaining[0];
                                        character.aliases = remaining.join("|");
                                    } else {
                                        characterRecords.splice(index, 1);
                                        charRemoved = true;
                                    }
                                } else {
                                    var newAliases = [];
                                    var currentAliases = splitAliases(character.aliases);
                                    for (var k = 0; k < currentAliases.length; k++) {
                                        if (currentAliases[k] !== name) newAliases.push(currentAliases[k]);
                                    }
                                    character.aliases = newAliases.join("|");
                                }
                                operationCount++;
                                saveCharacterData();
                                createGengxinFile();
                                refreshCharacterList();
                                releaseBtn.setEnabled(false);
                                releaseBtn.setAlpha(0.5);
                                deleteBtn.setEnabled(false);
                                deleteBtn.setAlpha(0.5);
                                nameText.setText(name + " ✓已删除");
                                nameText.setTextColor(android.graphics.Color.parseColor("#F44336"));
                                Toast.makeText(ctx, "已删除：" + name, Toast.LENGTH_SHORT).show();
                                if (charRemoved && releaseDialog) {
                                    try {
                                        releaseDialog.dismiss();
                                        new android.os.Handler(android.os.Looper.getMainLooper()).post(new java.lang.Runnable({
                                            run: function() { try { processNext(); } catch(e) { console.error("processNext失败: " + e.toString()); } }
                                        }));
                                    } catch (e) { console.error("dismiss失败: " + e.toString()); }
                                }
                            }
                        }));

                        releaseRoot.addView(row);
                        nameViews.push({ name: name, releaseBtn: releaseBtn, deleteBtn: deleteBtn });
                    })(allNames[ni]);
                }

                releaseScroll.addView(releaseRoot);
            var releaseDialog = new android.app.AlertDialog.Builder(ctx)
                    .setView(releaseScroll)
                    .setPositiveButton("完成", new android.content.DialogInterface.OnClickListener({
                        onClick: function(dialog) {
                            dialog.dismiss();
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(new java.lang.Runnable({
                                run: function() { try { processNext(); } catch(e) { console.error("processNext失败: " + e.toString()); } }
                            }));
                        }
                    }))
                    .setNegativeButton("跳过", new android.content.DialogInterface.OnClickListener({
                        onClick: function(dialog) {
                            dialog.dismiss();
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(new java.lang.Runnable({
                                run: function() { try { processNext(); } catch(e) { console.error("processNext失败: " + e.toString()); } }
                            }));
                        }
                    }))
                    .create();

                releaseDialog.show();
                applyDialogRoundCorner(releaseDialog);
            }
            
            processNext();
        }
        
  
  
        // 执行更换发音人操作（单个角色指定新发音人）
        function doFixVoiceOperation(selectedVoice) {
            if (longPressedIndex === -1) {
                Toast.makeText(ctx, "请长按一个角色", Toast.LENGTH_SHORT).show();
                return;
            }
            doFixVoiceForIndex(longPressedIndex, selectedVoice);
        }
        
        function doFixVoiceForIndex(index, selectedVoice) {
            backupOriginalData();
            try {
                var character = characterRecords[index];
                if (character) {
                    character.voice = selectedVoice;
                    character.usageCount = 100;
                    saveCharacterData();
                    createGengxinFile();
                    Toast.makeText(ctx, "已为角色更换发音人", Toast.LENGTH_SHORT).show();
                    // 全列表刷新（与合并/释放操作一致，确保标签立即更新）
                    selectedIndex = -1;
                    markedIndices = [];
                    clearListChoices();
                    refreshCharacterList();
                }
            } catch (e) {
                console.error("更换发音人失败: " + e.toString());
                Toast.makeText(ctx, "操作失败: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
        
        // 执行删除角色操作（删除所有标记角色）
        function doDeleteCharacterOperation() {
            if (markedIndices.length === 0) {
                Toast.makeText(ctx, "请标记至少一个角色", Toast.LENGTH_SHORT).show();
                return;
            }
            
            backupOriginalData();
            
            try {
                // 筛选保留未标记角色
                var newCharacterRecords = [];
                for (var i = 0; i < characterRecords.length; i++) {
                    if (markedIndices.indexOf(i) === -1) {
                        newCharacterRecords.push(characterRecords[i]);
                    }
                }
                
                characterRecords = newCharacterRecords;
                
                // 保存并刷新
                saveCharacterData();
                createGengxinFile();
                
                // 验证保存是否成功：读取文件确认内容与内存一致
                var saveVerified = false;
                try {
                    var savedData = ttsrv.readTxtFile("characterRecords.json");
                    if (savedData) {
                        var savedRecords = JSON.parse(savedData);
                        if (Array.isArray(savedRecords) && savedRecords.length === characterRecords.length) {
                            saveVerified = true;
                        }
                    }
                } catch (verifyErr) {
                    console.error("保存验证读取异常: " + verifyErr.toString());
                }
                
                // 验证失败则重试：直接写入文件
                if (!saveVerified) {
                    console.error("保存验证失败，重试写入。内存记录数: " + characterRecords.length);
                    var retryJson = serializeRecordsForStorage();
                    try {
                        ttsrv.writeTxtFile("characterRecords.json", retryJson);
                        var retryBookName = getCurrentBookName();
                        ttsrv.writeTxtFile("shuming." + retryBookName + ".json", retryJson);
                        ttsrv.writeTxtFile("gengxin.json", retryJson);
                        // 同步更新备份文件，防止框架从备份恢复旧数据
                        ttsrv.writeTxtFile("characterRecords_backup.json", retryJson);
                        console.log("重试写入完成");
                    } catch (retryErr) {
                        console.error("重试写入失败: " + retryErr.toString());
                        Toast.makeText(ctx, "保存失败，请重试", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                
                Toast.makeText(ctx, "已删除" + markedIndices.length + "个角色", Toast.LENGTH_SHORT).show();
                refreshCharacterList();
                
            } catch (e) {
                console.error("删除角色失败: " + e.toString());
                Toast.makeText(ctx, "操作失败: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
        
        // 刷新角色列表（更新UI显示）
        // keyword 为字符串时（含空串）：按关键词重新过滤（来自搜索框输入）
        // keyword 为 undefined 时（操作后刷新）：保持当前搜索结果集不变，仅更新显示文本
        function refreshCharacterList(keyword) {
            try {
                // 未传 keyword（操作后刷新）时，沿用当前搜索词，保持搜索结果不跳回全部；
                // 显式传入（含搜索框清空传""）则记住为当前搜索词
                if (keyword === undefined) {
                    keyword = currentKeyword;
                } else {
                    currentKeyword = keyword;
                }
                filteredIndices = [];
                filteredCharRefs = [];

                if (keyword) {
                    // 按关键词过滤（搜索框触发，或操作后沿用搜索词）
                    for (var i = 0; i < characterRecords.length; i++) {
                        var record = characterRecords[i];
                        if (!record) continue;
                        var charName = (record.name || "").toLowerCase();
                        var charAlias = (record.aliases || "").toLowerCase();
                        var charGender = (record.gender || "").toLowerCase();
                        var charAge = (record.age || "").toLowerCase();
                        var charVoice = (record.voice || "").toLowerCase();
                        var matched = charName.indexOf(keyword) !== -1 ||
                                      charAlias.indexOf(keyword) !== -1 ||
                                      charGender.indexOf(keyword) !== -1 ||
                                      charAge.indexOf(keyword) !== -1 ||
                                      charVoice.indexOf(keyword) !== -1;
                        if (!matched) {
                            continue;
                        }
                        filteredIndices.push(i);
                        filteredCharRefs.push(record);
                    }
                } else {
                    // 无搜索词：显示全部角色
                    for (var j = 0; j < characterRecords.length; j++) {
                        filteredIndices.push(j);
                        filteredCharRefs.push(characterRecords[j]);
                    }
                }

                // 备份当前结果集引用，供下次操作后刷新时比对
                filteredCharRefs_backup = filteredCharRefs.slice();

                markedIndices = [];
                selectedIndex = -1;
                clearListChoices();
                buildList();
                mergeListView.post(new java.lang.Runnable({
                    run: function() {
                        updateListViewAppearance();
                    }
                }));
                if (mergeLabel) mergeLabel.setText("👤 角色列表:");
                if (selectAllBtn) {
                    selectAllBtn.setText("全选");
                    var allShape2 = new android.graphics.drawable.GradientDrawable();
                    allShape2.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                    allShape2.setCornerRadius(dipToPx(8));
                    allShape2.setColor(android.graphics.Color.parseColor("#F3E5F5"));
                    allShape2.setStroke(dipToPx(1.5), android.graphics.Color.parseColor("#7E57C2"));
                    selectAllBtn.setBackground(allShape2);
                    selectAllBtn.setTextColor(android.graphics.Color.parseColor("#7E57C2"));
                }
                updateListViewHeight();
            } catch (e) {
                console.error("refreshCharacterList 异常: " + e.toString());
                Toast.makeText(ctx, "列表刷新异常: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }

        // 将 refreshCharacterList 暴露给模块级回调（onVoiceChanged 等），
        // 因其定义在 onLoadUI 闭包内，模块级方法无法直接访问。
        _refreshCharacterListFn = refreshCharacterList;

        // 更新列表外观（标记/选中状态可视化）
        
                function createRoundedDrawable(color, strokeColor) {
            var d = new android.graphics.drawable.GradientDrawable();
            d.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            d.setCornerRadius(dipToPx(10));
            d.setColor(android.graphics.Color.parseColor(color));
            if (strokeColor) d.setStroke(dipToPx(1), android.graphics.Color.parseColor(strokeColor));
            return d;
        }

        // 修复后：直接通过已知布局获取系统颜色，避免遍历查找失败
        function updateListViewAppearance() {
            var systemTextColor = getAdaptiveTextColor();

            if (mergeLabel && mergeLabel instanceof android.widget.TextView) {
                mergeLabel.setTextColor(systemTextColor);
            }

            var firstVisible = 0;
            for (var i = 0; i < rowViews.length; i++) {
                var child = rowViews[i];
                var position = firstVisible + i;
                if (child && position < filteredIndices.length) {
                    var originalIndex = filteredIndices[position];
                    // 行视图是 LinearLayout：名字文本色需设置到内部每行 nameText（圆点后第2个子View）
                    var textColor = systemTextColor;
                    var nameBox = (child.getChildCount() > 0) ? child.getChildAt(0) : null;
                    if (nameBox && nameBox instanceof android.widget.LinearLayout) {
                        for (var nc = 0; nc < nameBox.getChildCount(); nc++) {
                            var nl = nameBox.getChildAt(nc);
                            if (nl && nl instanceof android.widget.LinearLayout) {
                                var nt = nl.getChildAt(1);
                                if (nt && nt instanceof android.widget.TextView) {
                                    nt.setTextColor(textColor);
                                }
                            }
                        }
                    }

                    // 默认无背景（去掉大框），仅选中/标记时才设置高亮背景
                    var bgDrawable = null;

                    if (markedIndices.indexOf(originalIndex) !== -1 && originalIndex !== selectedIndex) {
                        // 已标记（非当前选中）：极淡暖底+极淡边框
                        bgDrawable = createRoundedDrawable("#FFFDE7", "#33FFD54F");
                        textColor = android.graphics.Color.parseColor("#333333");
                    } else if (markedIndices.indexOf(originalIndex) !== -1 && originalIndex === selectedIndex) {
                        // 已标记+当前选中：淡暖底+淡边框
                        bgDrawable = createRoundedDrawable("#FFF8E1", "#66FFD54F");
                        textColor = android.graphics.Color.parseColor("#333333");
                    } else if (originalIndex === selectedIndex) {
                        // 仅选中：极淡蓝底+极淡边框
                        bgDrawable = createRoundedDrawable("#F5F7FA", "#10000000");
                        textColor = android.graphics.Color.parseColor("#333333");
                    }

                    if (bgDrawable) {
                        child.setBackground(bgDrawable);
                    } else {
                        child.setBackground(null);
                    }
                    // 不再覆盖padding，保持createListRow中设定的值
                }
            }
        }
  
        
        // 列表现在使用 LinearLayout 自适应高度，无需手动计算；保留函数以兼容现有调用
        function updateListViewHeight() {
            // no-op: LinearLayout 高度由内容自动撑开
        }
        
        // 初始设置列表高度
        updateListViewHeight();
  
    // showMultiLineInputDialog 已在上方统一定义（使用 createStyledEditText 样式），此处删除重复定义
  
  // 修改角色名/别名（并列编辑：一个弹窗同时显示角色名和别名）
    function doEditCharacterOperation(position) {
        try {
            if (position < 0 || position >= characterRecords.length) {
                Toast.makeText(ctx, "角色索引无效", Toast.LENGTH_SHORT).show();
                return;
            }
            var targetChar = characterRecords[position];
            if (!targetChar) {
                Toast.makeText(ctx, "角色数据异常", Toast.LENGTH_SHORT).show();
                return;
            }
            var originalName = String(targetChar.name || "");
            var originalAlias = String(targetChar.aliases || "");

            showEditCharacterDialog(originalName, originalAlias, position, targetChar);
        } catch (e) {
            console.error("doEditCharacterOperation 异常：" + e.toString());
            Toast.makeText(ctx, "操作失败：" + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    // 直观修改角色弹窗：所有名称并列显示，可独立编辑/增删
    function showEditCharacterDialog(oldName, oldAlias, position, targetChar) {
        try {
            var builder = new android.app.AlertDialog.Builder(ctx);
            var scroll = new android.widget.ScrollView(ctx);
            var root = new android.widget.LinearLayout(ctx);
            root.setOrientation(android.widget.LinearLayout.VERTICAL);
            root.setPadding(dipToPx(20), dipToPx(12), dipToPx(20), dipToPx(16));
            root.addView(createDialogTitle("修改角色名称"));
            scroll.addView(root);

            // 说明文字（统一标签样式）
            root.addView(createDialogLabel("角色所有名称（第1个为主名，其余为别名）"));

            // 名称输入框列表
            var nameInputs = [];
            var nameRows = [];

            // 收集所有名称：主名 + 别名
            var allNames = [oldName];
            var aliasParts = splitAliases(oldAlias);
            for (var i = 0; i < aliasParts.length; i++) {
                if (aliasParts[i] !== oldName && allNames.indexOf(aliasParts[i]) === -1) {
                    allNames.push(aliasParts[i]);
                }
            }

            function createNameRow(defaultText, index) {
                var row = new android.widget.LinearLayout(ctx);
                row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                var rowLp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                );
                rowLp.setMargins(0, dipToPx(4), 0, dipToPx(4));
                row.setLayoutParams(rowLp);

                var numLabel = new android.widget.TextView(ctx);
                numLabel.setText((index + 1) + ". ");
                numLabel.setTextSize(14);
                numLabel.setTextColor(android.graphics.Color.parseColor("#757575"));
                row.addView(numLabel);

                // 统一样式的输入框
                var edit = createStyledEditText(null, true);
                edit.setText(defaultText);
                edit.setSelection(defaultText.length);
                var editLp = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                edit.setLayoutParams(editLp);
                row.addView(edit);

                // 统一样式的删除按钮
                var delBtn = createDialogSmallButton("删除", "#E53935");
                var delLp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                );
                delLp.setMargins(dipToPx(6), 0, 0, 0);
                delBtn.setLayoutParams(delLp);
                row.addView(delBtn);

                delBtn.setOnClickListener(new android.view.View.OnClickListener({
                    onClick: function(v) {
                        var idx = nameRows.indexOf(row);
                        if (idx !== -1) {
                            nameRows.splice(idx, 1);
                            nameInputs.splice(idx, 1);
                            root.removeView(row);
                            // 重新编号
                            for (var j = 0; j < nameRows.length; j++) {
                                var child = nameRows[j].getChildAt(0);
                                if (child) child.setText((j + 1) + ". ");
                            }
                        }
                    }
                }));

                nameRows.push(row);
                nameInputs.push(edit);
                root.addView(row);
            }

            for (var n = 0; n < allNames.length; n++) {
                createNameRow(allNames[n], n);
            }

            // 新增名称按钮（统一样式）
            var addBtn = createDialogTextButton("+ 新增名称", "#7E57C2");
            addBtn.setGravity(android.view.Gravity.LEFT);
            root.addView(addBtn);

            addBtn.setOnClickListener(new android.view.View.OnClickListener({
                onClick: function(v) {
                    createNameRow("", nameRows.length);
                }
            }));

            builder.setView(scroll);

            builder.setPositiveButton("保存", new android.content.DialogInterface.OnClickListener({
                onClick: function(dialog, which) {
                    var names = [];
                    for (var i = 0; i < nameInputs.length; i++) {
                        var txt = nameInputs[i].getText() ? nameInputs[i].getText().toString().trim() : "";
                        if (txt !== "" && names.indexOf(txt) === -1) {
                            names.push(txt);
                        }
                    }

                    if (names.length === 0) {
                        Toast.makeText(ctx, "至少需要保留一个名称", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    var newName = names[0];
                    var newAlias = names.slice(1).join("|");

                    // 检查主名是否与其他角色重名（排除自己）
                    for (var i = 0; i < characterRecords.length; i++) {
                        if (i !== position && characterRecords[i]) {
                            var otherName = characterRecords[i].name || "";
                            if (normalizeString(otherName) === normalizeString(newName)) {
                                Toast.makeText(ctx, "主名称【" + newName + "】已存在，请使用其他名称", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    }

                    // 保存时重新通过 position 获取对象（避免弹窗期间数组变化导致引用失效）
                    var charToUpdate = characterRecords[position];
                    if (!charToUpdate) {
                        Toast.makeText(ctx, "角色数据已变更，请重新操作", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    charToUpdate.name = newName;
                    charToUpdate.aliases = newAlias;

                    saveCharacterData();
                    createGengxinFile();
                    refreshCharacterList();

                    Toast.makeText(ctx, "角色修改成功：" + newName, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            }));

            builder.setNegativeButton("取消", new android.content.DialogInterface.OnClickListener({
                onClick: function(dialog, which) { dialog.cancel(); }
            }));

            var addCharDlg = builder.show();
            applyDialogRoundCorner(addCharDlg);
        } catch (e) {
            console.error("showEditCharacterDialog 异常：" + e.toString());
            Toast.makeText(ctx, "弹窗异常：" + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
    
    },
  
    // 发音人切换回调：前台切换发音人列表后，强制刷新personality缓存
    'onVoiceChanged': function (locale, voice) {
        try {
            _initFayinrenMapCache(true);
            console.log("onVoiceChanged: personality缓存已强制刷新");
            // 刷新角色列表，获取当前已启用配置项的实际发音人
            try {
                if (_refreshCharacterListFn) {
                    _refreshCharacterListFn();
                } else {
                    console.warn("onVoiceChanged: refreshCharacterList 尚未初始化（onLoadUI 未执行）");
                }
            } catch (eRef) { console.error("onVoiceChanged刷新列表失败: " + eRef.toString()); }
        } catch (e) {
            console.error("onVoiceChanged刷新缓存失败: " + e.toString());
        }
    }
  }
  
  // Item类：用于发音人/性别年龄选择的"名称-值"映射
  function Item(name, value) {
    this.name = name;
    this.value = value;
  }
