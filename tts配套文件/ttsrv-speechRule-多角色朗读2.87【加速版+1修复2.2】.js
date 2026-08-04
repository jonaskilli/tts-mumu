// ES5兼容的补0函数（保留不变）
function padZero(num, length) {
  num = num.toString();
  while (num.length < length) num = '0' + num;
  return num;
}

// ===================== 新增：API结果等待数量配置 =====================
// 1/2 = 与原逻辑一致，取第一个成功返回的结果立即使用
// ≥3 = 等待对应数量的API返回结果后，按投票规则选择最优结果
// 超时后返回的结果数量不足时，按实际返回的结果数执行规则


var WAIT_API_RESULT_COUNT = 1; // 默认与原逻辑完全兼容

var rizhi = 1;//等于0 关闭投票日志，等于1 开启投票日志。



// ===================== 别名分析模式配置 =====================
// 0 = 关闭别名分析（不进行API校验）
// 1 = 严谨模式（100%确定性才合并）
// 2 = 宽松模式（80%置信度即可合并）

var bieming = 1; // 默认使用80%宽松模式


var bingfa = 1;//默认3并发数


var xiawen = 1800;//   字数越大缓存越多。

var shouci = 300;//   首次使用缓存字数，只在首次使用。

var SEQ_ADD_RATIO = 0.9; // 总引号数＞5时生效：总左引号数量 × 该比例 = 最终添加序号的数量，取整数

var NEXT_CHAPTER_COUNT = 4; // 0=仅本章，1=本章+后1章，2=本章+后2章.

var xiawens = xiawen; // 保存初始的下文长度默认配置

// ===================== 永久章节缓存配置（对齐其他APP脚本，存储到 ../xiaoshuo/）=====================
var CACHE_ROOT = "../xiaoshuo/";
var PROGRESS_FILE = CACHE_ROOT + "reading_progress.json";

// 时间记录变量：初始化当前时间减2小时，精确到分钟
var shijian = new Date(Date.now() - 2 * 60 * 60 * 1000);
shijian.setSeconds(0);
shijian.setMilliseconds(0);

// 姓名性别年龄分析API：单独超时时间（不修改则默认使用全局超时）
var NAME_ANALYZE_TIMEOUT = 25000;
// 别名校验分析API：单独超时时间（不修改则默认使用全局超时）
var ALIAS_ANALYZE_TIMEOUT = 25000;




// ===================== 新增：投票别名合并开关 =====================
// 0 = 关闭别名合并（原逻辑，按API返回的原始名字投票）
// 1 = 开启别名合并（匹配本地角色主名/别名，同一主名视为同一人）
var ENABLE_ALIAS_VOTE_MERGE = 1;

var pendingAliasNames = {};      // { name: true }
var pendingAliasContext = null;  // { contextHistory2: ..., text2: ..., next100Chars: ... }

// ===================== 角色配置（集中管理，视觉工整）=====================
// 1. 主角配置：[显示前缀, 性别, 年龄, 发音人前缀, 数量]
var MAIN_ROLES_CONFIG = [
  ['主角 男主', '主角', '男主', '男主', 20],
  ['主角 女主', '主角', '女主', '女主', 20]
];
// 2. 批量角色配置：[类型前缀, 性别, 年龄, 发音人前缀, 数量]
var BATCH_ROLES = [
  ['女/少女', '女', '少女', '少女', 100],
  ['男/少年', '男', '少年', '少年', 100],
  ['女/女青年', '女', '女青年', '女青年', 100],
  ['男/男青年', '男', '男青年', '男青年', 100],
  ['女/女中年', '女', '女中年', '女中年', 100],
  ['男/男中年', '男', '男中年', '男中年', 100],
  ['女/女老年', '女', '女老年', '女老年', 100],
  ['男/男老年', '男', '男老年', '男老年', 100],
  ['女/女童', '女', '女童', '女童', 100],
  ['男/男童', '男', '男童', '男童', 100],
  ['男/特殊', '特殊', '特殊', '特殊男', 20],
  ['女/特殊', '特殊', '特殊', '特殊女', 20]
];
// 3. 特殊角色配置：[键名, 性别, 年龄, 发音人标签]
var SPECIAL_ROLES = [
  ['【】括号发音人', '特殊', '系统', '括号1'],
  ['在线音效', '特', '特殊', '括号2'],
  ['「」括号发音人', '特', '特殊', '括号3'],
  ['『对话旁白』', '特殊', '旁白', '括号4']
];
// ===================== 生成角色对象（逻辑简洁，无冗余）=====================
var GENSHIN_CHARACTERS = (function () {
  var chars = {};

  // 生成主角（从 MAIN_ROLES_CONFIG 动态生成）
  for (var idx = 0; idx < MAIN_ROLES_CONFIG.length; idx++) {
      var cfg = MAIN_ROLES_CONFIG[idx];
      var displayPrefix = cfg[0], gender = cfg[1], age = cfg[2], voicePrefix = cfg[3], count = cfg[4];
      for (var i = 1; i <= count; i++) {
          // 显示名
          var seqDisplay = padZero(i, 2);
          // voice标签
          var seqVoice = (voicePrefix === '男主') ? i.toString() : padZero(i, 2);
          var name = '【' + displayPrefix + seqDisplay + '】';
          chars[name] = { gender: gender, age: age, voice: voicePrefix + seqVoice };
      }
  }

  // 生成批量角色
  BATCH_ROLES.forEach(function (item) {
      var type = item[0], gender = item[1], age = item[2], voicePre = item[3], count = item[4];
      for (var i = 1; i <= count; i++) {
          var seq = padZero(i, 2);
          var name = '【' + type + seq + '】';
          chars[name] = { gender: gender, age: age, voice: voicePre + seq };
      }
  });

  // 生成特殊角色
  SPECIAL_ROLES.forEach(function (item) {
      chars[item[0]] = { gender: item[1], age: item[2], voice: item[3] };
  });

  return chars;
})();

// ===================== 标签映射（关键：让标签列表显示所有角色）=====================
if (typeof SpeechRuleJS !== 'undefined' && SpeechRuleJS.tags) {
  for (var key in GENSHIN_CHARACTERS) {
      if (GENSHIN_CHARACTERS.hasOwnProperty(key)) {
          var voiceTag = GENSHIN_CHARACTERS[key].voice;
          SpeechRuleJS.tags[voiceTag] = key;
      }
  }
}







// 新增1：功能控制开关（1=开启音效流程，0=关闭，不影响原功能）
var ENABLE_AUDIO_API = 1;
// 新增2：音效搜索API密钥
//var AUDIO_API_KEY = "wWTHunLyFmTt7uWaAeWuqjm4qDqXzcL9l2yHt8KX";
var AUDIO_API_KEY = "a5bCYcgrNiGbWeX5j0HSf7EeMg6ot1E9FxSV6EYeAA";
// 新增：智能音效模式（off=关闭，full=全模式，replace_only=仅替换模式）
var AUDIO_MODE = "off";
var lastAudioApiIndex = 0; // 用于循环切换API_KEY的索引（参考gengxin.txt逻辑）
            
// 新增3：音效配置（调整最小时长）- 完全保留原配置，无任何改动
var AUDIO_CONFIG = {
    soundApiEndpoint: "https://freesound.org/apiv2",
    targetDuration: 3.0, // 目标音效时长（秒）
    minDuration: 0.1,    // 最小时长改为0.1秒（原来是0.5秒）
    maxDuration: 5.0,    // 最大时长（秒）- 硬性限制
    searchPageSize: 30,  // 搜索返回30个结果
    maxResultsToCheck: 30, // 实际检查前30个结果
    zhipuApiEndpoint: "", 
    zhipuApiModel: "glm-4.5-flash",
    logPrefix: "【智能音效】",
    timeout: 3000,
    apiKey: "" // 用于存储智谱API密钥（从gengxin.txt同步）
};

// 工具函数：正则特殊字符转义（完全保留原始逻辑，无改动）
function escapeRegExp(str) {
    var escaped = str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    // 新增日志：仅调试用，不影响功能
    return escaped;
}


var roleToRootIdMap = {};


// ===================== 核心：双场景独立密钥轮换管理（热更新版，ES5兼容，新增API自动补全逻辑）=====================
var DualKeyManager = (function() {
  // 兜底默认配置，和原代码完全对齐
  var defaultConfig = {
      endpoint: 'https://open.bigmodel.cn/api/paas/v4/chat/completions',
      model: "glm-4-flash",
      key: 'b26b869ffd7e4a1dac61666db27de213.ayAJYkmqeA1w3OL'
  };
  var keyFileName = "miyue.txt"; // 密钥文件路径，和原逻辑一致
  // 两个场景独立的配置
  var pools = {
      nameAnalyze: { list: [], index: 0, indexFile: "nameKeyIndex.txt" }, // 姓名性别年龄分析
      aliasAnalyze: { list: [], index: 0, indexFile: "aliasKeyIndex.txt" }  // 别名校验分析
  };
  // 【新增：并发最小数量配置，可直接修改】
  var MIN_CONCURRENT_COUNT = 3;

  // 私有：加载单场景的轮换索引
  function loadIndex(scene) {
      try {
          var idx = parseInt(ttsrv.readTxtFile(pools[scene].indexFile), 10);
          pools[scene].index = !isNaN(idx) && idx >= 0 ? idx : 0;
      } catch (e) {
          pools[scene].index = 0;
      }
  }
  // 私有：保存单场景的轮换索引
  function saveIndex(scene) {
      try {
          ttsrv.writeTxtFile(pools[scene].indexFile, pools[scene].index.toString());
      } catch (e) {}
  }

  // 【新增：核心辅助函数 - 自动填充API列表到最小并发数】
  // 规则：1组→重复3次，2组→轮流补全到3个，≥3组→不改动
  function fillApiListToMinCount(apiList, minCount) {
      if (!apiList || !Array.isArray(apiList)) return [];
      minCount = parseInt(minCount, 10) || MIN_CONCURRENT_COUNT;
      var originalLength = apiList.length;
      
      // 空列表直接返回，走兜底逻辑
      if (originalLength === 0) return [];
      // 数量已满足要求，直接返回原列表副本
      if (originalLength >= minCount) return apiList.slice();
      
      // 不足最小数量，循环重复原列表补全
      var filledList = [];
      for (var i = 0; i < minCount; i++) {
          var targetIndex = i % originalLength; // 循环取原列表索引
          filledList.push(apiList[targetIndex]);
      }
      return filledList;
  }

  // 私有：解析单组密钥内容，完全保留原@@逻辑
  function parseSingleGroup(content) {
      var pool = [];
      if (!content || content.trim() === "") return pool;
      var contentTrim = content.trim();
      // 原逻辑：有@@按【地址@@模型@@密钥】分组，每3个为一组
      if (contentTrim.indexOf("@@") !== -1) {
          var splitArr = contentTrim.split("@@");
          for (var i = 0; i < splitArr.length; i += 3) {
              var endpoint = splitArr[i] ? splitArr[i].trim() : "";
              var model = splitArr[i + 1] ? splitArr[i + 1].trim() : "";
              var key = splitArr[i + 2] ? splitArr[i + 2].trim() : "";
              // 原地址格式化逻辑完全保留
              if (endpoint) {
                  if (endpoint.endsWith("/")) endpoint = endpoint.slice(0, -1);
                  if (endpoint.endsWith("/chat/completions")) endpoint = endpoint.slice(0, -17);
                  endpoint += "/chat/completions";
              }
              // 仅密钥非空时加入池，空字段用默认值兜底
              if (key) {
                  pool.push({
                      endpoint: endpoint || defaultConfig.endpoint,
                      model: model || defaultConfig.model,
                      key: key
                  });
              }
          }
      } else {
          // 原逻辑：无@@，整段内容为单密钥，用默认地址和模型
          pool.push({
              endpoint: defaultConfig.endpoint,
              model: defaultConfig.model,
              key: contentTrim
          });
      }
      return pool;
  }

  // 私有：加载并解析整个密钥文件，按##拆分双场景
  function loadKeyFile() {
  try {
      var fileContent = ttsrv.readTxtFile(keyFileName).trim();
      var hasSplit = fileContent.indexOf("##") !== -1;
      var nameContent, aliasContent;
      if (hasSplit) {
          // 有##：前面=姓名分析密钥，后面=别名分析密钥
          var splitArr = fileContent.split("##");
          nameContent = splitArr[0] ? splitArr[0].trim() : "";
          aliasContent = splitArr[1] ? splitArr[1].trim() : "";
      } else {
          // 无##：两个场景共用同一套密钥
          nameContent = fileContent;
          aliasContent = fileContent;
      }
      // 分别解析两个场景的密钥池
      pools.nameAnalyze.list = parseSingleGroup(nameContent);
      pools.aliasAnalyze.list = parseSingleGroup(aliasContent);

      // ===================== 自动补全API列表到最小并发数 =====================
  //    pools.nameAnalyze.list = fillApiListToMinCount(pools.nameAnalyze.list, MIN_CONCURRENT_COUNT);
//      pools.aliasAnalyze.list = fillApiListToMinCount(pools.aliasAnalyze.list, MIN_CONCURRENT_COUNT);

      // ----- 修改部分开始 -----
      // 加载姓名分析索引
      loadIndex("nameAnalyze");

      if (hasSplit) {
          // 有 ## 分隔：两套密钥各自独立，按原逻辑分别加载索引
          loadIndex("aliasAnalyze");
      } else {
          // 无 ## 分隔：共用同一套密钥，别名场景复用姓名场景的索引（全局连续轮换）
          pools.aliasAnalyze.index = pools.nameAnalyze.index;
          pools.aliasAnalyze.indexFile = pools.nameAnalyze.indexFile; // 写入时也会更新主索引文件
      }
      // ----- 修改部分结束 -----

      // 校验索引范围，避免超出池长度
      ["nameAnalyze", "aliasAnalyze"].forEach(function(scene) {
          if (pools[scene].list.length > 0 && pools[scene].index >= pools[scene].list.length) {
              pools[scene].index = pools[scene].index % pools[scene].list.length;
              saveIndex(scene);
          }
      });
      return true;
  } catch (e) {
      // 文件读取失败，两个场景都用空池，兜底默认配置
      pools.nameAnalyze.list = [];
      pools.aliasAnalyze.list = [];
      return false;
  }
  }

  // 新增：【核心】获取当前场景的可用API密钥列表（支持指定获取数量，自动轮换）
  function getAvailableApiList(scene, needCount) {
      // 每次调用强制重新读取密钥文件，保留热更新特性
      loadKeyFile();
      var sceneConfig = pools[scene];
      if (!sceneConfig) return [];
      var totalAvailable = sceneConfig.list.length;
      // 无可用密钥，返回空数组，后续走兜底
      if (totalAvailable === 0) return [];
      // 未指定数量，返回全部可用
      if (!needCount || needCount <= 0) needCount = totalAvailable;
      // 需求数量超过总可用，返回全部
      if (needCount > totalAvailable) needCount = totalAvailable;
      var result = [];
      var currentIndex = sceneConfig.index;
      // 按轮换规则取密钥，循环取数
      for (var i = 0; i < needCount; i++) {
          var targetIndex = (currentIndex + i) % totalAvailable;
          result.push(sceneConfig.list[targetIndex]);
      }
      // 更新索引，下次从当前结束的位置继续，实现轮询不重复
      sceneConfig.index = (currentIndex + needCount) % totalAvailable;
      saveIndex(scene);
      return result;
  }
  // 公开：【姓名分析场景调用】获取下一个密钥，保留原方法，兼容旧逻辑
  function getNextNameAnalyzeKey() {
      var list = getAvailableApiList("nameAnalyze", 1);
      return list.length > 0 ? list[0] : defaultConfig;
  }
  // 公开：【别名分析场景调用】获取下一个密钥，保留原方法，兼容旧逻辑
  function getNextAliasAnalyzeKey() {
      var list = getAvailableApiList("aliasAnalyze", 1);
      return list.length > 0 ? list[0] : defaultConfig;
  }
  // 暴露方法
  return {
      getNextNameAnalyzeKey: getNextNameAnalyzeKey,
      getNextAliasAnalyzeKey: getNextAliasAnalyzeKey,
      getAvailableApiList: getAvailableApiList // 新增：给并发工具调用
  };
})();




// ===================== 日志辅助：截断过长文本，便于打印AI返回内容 =====================
function apiLogTruncate(s, len) {
  len = len || 800;
  s = String(s == null ? "" : s);
  if (s.length <= len * 2) return s;
  return s.substring(0, len) + "\n...[已截断，共 " + s.length + " 字符]...\n" + s.substring(s.length - len);
}

// ===================== 修复后：通用并发API请求工具（彻底解决提前唤醒问题，严格等待指定数量结果）=====================
function concurrentApiRequest(scene, requestBuilder, responseParser, maxConcurrent, timeout, sharedState) {
  // sharedState 可选：{ resultsArray, successCounter, latchHolder, targetCount }
  var safeBingfa = parseInt(bingfa, 10);
  if (isNaN(safeBingfa) || safeBingfa < 1) safeBingfa = 1;
  if (!maxConcurrent || maxConcurrent <= 0) maxConcurrent = safeBingfa;

  timeout = timeout || 18000;
  var errors = [];
  var ownResults = !sharedState;
  var successResults = sharedState ? sharedState.resultsArray : [];
  var successCount = sharedState ? sharedState.successCounter : new java.util.concurrent.atomic.AtomicInteger(0);
  var needWaitMultiResult = WAIT_API_RESULT_COUNT >= 3;
  var targetSuccessCount = sharedState ? sharedState.targetCount : (needWaitMultiResult ? Math.min(WAIT_API_RESULT_COUNT, maxConcurrent) : 1);
  var latchHolder = sharedState ? sharedState.latchHolder : null;
  var hasWakedUp = sharedState ? sharedState.hasWakedUp : new java.util.concurrent.atomic.AtomicBoolean(false);

  // 如果之前已经达标，直接返回
  if (sharedState && successCount.get() >= targetSuccessCount) {
    // 确保唤醒当前 latch
    if (latchHolder && latchHolder.latch && hasWakedUp.compareAndSet(false, true)) {
      latchHolder.latch.countDown();
    }
    return {
      success: true,
      data: needWaitMultiResult ? successResults : (successResults.length ? successResults[0].data : null),
      isMultiResult: needWaitMultiResult,
      errors: [],
      sharedState: sharedState
    };
  }

  // 获取本轮密钥
  var apiList = DualKeyManager.getAvailableApiList(scene, maxConcurrent);
  var concurrentCount = apiList.length;
  if (concurrentCount === 0) {
    console.error("【并发" + scene + "】无可用API密钥，终止并发");
    return { success: false, data: null, errors: ["无可用API密钥"], sharedState: sharedState };
  }
  console.log("【并发" + scene + "】启动，目标等待成功数：" + targetSuccessCount + "，并发总数：" + concurrentCount);

  // 本轮并发任务总数 & 已完成计数（用于「全部结束但未达标时立即重试，不再等待超时」）
  var totalTasks = concurrentCount;
  var completedTasks = new java.util.concurrent.atomic.AtomicInteger(0);
  var allCompletedFlag = new java.util.concurrent.atomic.AtomicBoolean(false);

  function createSingleRequestTask(apiConfig) {
    return function() {
      var requestStartTime = Date.now();
      try {
        // 已经达标就不再处理
        if (hasWakedUp.get() && successCount.get() >= targetSuccessCount) return;

        var requestParams = requestBuilder(apiConfig);
        if (!requestParams) throw new Error("请求参数构建失败");

        var response = ttsrv.httpPost(
          requestParams.endpoint,
          JSON.stringify(requestParams.data),
          requestParams.headers
        );
        var parsedResult = responseParser(response);
        if (!parsedResult) throw new Error("响应解析失败，无有效结果");

        var currentNum = successCount.incrementAndGet();
        successResults.push({
          data: parsedResult,
          apiConfig: apiConfig,
          responseTime: Date.now() - requestStartTime,
          timestamp: Date.now()
        });

        if (!needWaitMultiResult) {
          console.log("【" + (scene === "nameAnalyze" ? "🔴🔴🔴✅ 姓名分析" : "🔵🔵🔵✅ 别名校验") + "成功！】单结果模式，立即使用，模型：" + apiConfig.model + "，密钥末尾4位：" + apiConfig.key.slice(-4));
          if (hasWakedUp.compareAndSet(false, true) && latchHolder && latchHolder.latch) {
            latchHolder.latch.countDown();
          }
        } else {
          console.log("【" + (scene === "nameAnalyze" ? "🔴🔴🔴 姓名分析" : "🔵🔵🔵 别名校验") + "成功" + currentNum + "/" + targetSuccessCount + "个】 模型：" + apiConfig.model + "，密钥末尾4位：" + apiConfig.key.slice(-4));
          if (currentNum >= targetSuccessCount && hasWakedUp.compareAndSet(false, true) && latchHolder && latchHolder.latch) {
            console.log("【并发" + scene + "】已达到目标成功数" + targetSuccessCount + "个，停止等待，开始处理结果");
            latchHolder.latch.countDown();
          }
        }
      } catch (err) {
        var errorMsg = "密钥末尾" + apiConfig.key.slice(-4) + "：" + (err.message || "请求超时/未知错误");
        errors.push(errorMsg);
        console.error("【并发" + scene + "】请求失败：" + errorMsg);
      } finally {
        // 关键修复①：无论请求成功或失败（含解析失败/格式错误/超时），每个并发任务结束都计入「已完成」；
        // 当本轮所有并发请求都已结束，但仍未达到目标成功数时，立即唤醒闩锁进入下一轮重试，
        // 不再傻等整个超时时间（requestTimeout）才重试（之前解析失败时就是这样干等）。
        var doneCount = completedTasks.incrementAndGet();
        if (doneCount >= totalTasks && !hasWakedUp.get() && successCount.get() < targetSuccessCount) {
          if (allCompletedFlag.compareAndSet(false, true) && latchHolder && latchHolder.latch) {
            console.log("【并发" + scene + "】本轮全部请求结束但未达目标成功数，立即重试（不再等待超时）");
            latchHolder.latch.countDown();
          }
        }
      }
    };
  }

  for (var i = 0; i < apiList.length; i++) {
    (function(apiConfig) {
      var thread = new java.lang.Thread(new java.lang.Runnable({
        run: createSingleRequestTask(apiConfig)
      }));
      thread.start();
    })(apiList[i]);
  }

  // 只有自己管理 loch 时才内部等待，否则由外部调用方通过 latch 控制
  if (!latchHolder) {
    var ownLatch = new java.util.concurrent.CountDownLatch(1);
    try {
      var waitSuccess = ownLatch.await(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
      if (!waitSuccess) {
        errors.push("并发请求超时（" + timeout/1000 + "秒），已收集到" + successCount.get() + "个有效结果");
        console.error("【并发" + scene + "】请求超时，已收集" + successCount.get() + "个结果，开始处理");
      }
    } catch (e) {
      errors.push("主线程等待异常：" + e.message);
    }
  }

  // 返回结果（带有共享状态）
  var result = {
    success: successCount.get() > 0,
    data: needWaitMultiResult ? successResults : (successResults.length ? successResults[0].data : null),
    isMultiResult: needWaitMultiResult,
    errors: errors,
    sharedState: sharedState
  };
  return result;
}



// ===================== 最终完整版：姓名分析结果投票函数（日志开关+对话原文打印+格式优化）=====================
function voteNameAnalyzeResult(successResults, dialogTextMap) {
  // 入参兜底，避免不传参数报错
  if (!successResults || !Array.isArray(successResults) || successResults.length === 0) {
    return null;
  }
  // 对话文本映射兜底，非对象/未传则用空对象
  dialogTextMap = (typeof dialogTextMap === 'object' && dialogTextMap !== null) ? dialogTextMap : {};

  // 共用顶部开关，0=关闭合并，1=开启合并
  var enableMerge = ENABLE_ALIAS_VOTE_MERGE === 1;
  var nameToMainNameMap = {};

  // ========== 优化核心：直接读内存映射表，无重复遍历 ==========
  if (enableMerge) {
    // 优先复用内存里已经生成好的别名映射表（checkAliasByApi时已生成，实时更新）
    if (typeof characterManager !== 'undefined' && characterManager.nameToMainNameMap) {
      nameToMainNameMap = characterManager.nameToMainNameMap;
    } 
    // 极端兜底：映射表不存在时，仅遍历一次内存变量生成，绝不读本地文件
    else if (typeof characterManager !== 'undefined' && Array.isArray(characterManager.characterRecords)) {
      var records = characterManager.characterRecords;
      for (var i = 0; i < records.length; i++) {
        var record = records[i];
        if (!record || !record.name) continue;
        var mainName = record.name.trim();
        // 主名映射自身
        nameToMainNameMap[mainName] = mainName;
        // 别名映射到主名
        if (record.aliases && record.aliases.trim()) {
          var aliasList = record.aliases.split("|")
            .map(function(alias) { return alias.trim(); })
            .filter(function(alias) { return alias && alias !== mainName; });
          for (var j = 0; j < aliasList.length; j++) {
            nameToMainNameMap[aliasList[j]] = mainName;
          }
        }
      }
      // 生成后同步到内存，后续直接复用
      characterManager.nameToMainNameMap = nameToMainNameMap;
    }
    console.log("【🔴🔴🔴 投票别名合并】已" + (enableMerge ? "开启" : "关闭") + "，内存映射表共" + Object.keys(nameToMainNameMap).length + "条记录");
  }
  // ========== 优化结束 ==========

  // 第一步：收集所有对话序号，按数字升序排序
  var allSeqSet = {};
  for (var i = 0; i < successResults.length; i++) {
    var apiResult = successResults[i].data;
    for (var seq in apiResult) {
      if (apiResult.hasOwnProperty(seq)) {
        allSeqSet[seq] = true;
      }
    }
  }
  var sortedSeqList = Object.keys(allSeqSet).sort(function(a, b) {
    return parseInt(a, 10) - parseInt(b, 10);
  });

  // 第二步：按顺序逐个处理每个序号
  var finalResult = {};
  for (var seqIdx = 0; seqIdx < sortedSeqList.length; seqIdx++) {
    var currentSeq = sortedSeqList[seqIdx];
    var seqAllResults = [];
    for (var apiIdx = 0; apiIdx < successResults.length; apiIdx++) {
      var apiItem = successResults[apiIdx];
      var apiSeqResult = apiItem.data[currentSeq];
      if (apiSeqResult && apiSeqResult.name && apiSeqResult.gender && apiSeqResult.age) {
        var originalName = apiSeqResult.name;
        var mainName = originalName;
        if (enableMerge && nameToMainNameMap.hasOwnProperty(originalName)) {
          mainName = nameToMainNameMap[originalName];
        }
        seqAllResults.push({
          name: originalName,
          mainName: mainName,
          gender: apiSeqResult.gender,
          age: apiSeqResult.age,
          timestamp: apiItem.timestamp,
          apiConfig: apiItem.apiConfig // 保留API配置，用于获取模型名
        });
      }
    }

    // 无有效结果兜底
    if (seqAllResults.length === 0) {
      finalResult[currentSeq] = {
        name: "未知",
        gender: Math.random() > 0.5 ? "男" : "女",
        age: Math.random() > 0.5 ? "青年" : "中年"
      };
      continue;
    }

    // 核心1：选主名（次数最多→平票选最晚）
    var nameCountMap = {};
    var nameModelMap = {}; // 存储每个姓名对应的模型列表
    for (var i = 0; i < seqAllResults.length; i++) {
      var countKey = enableMerge ? seqAllResults[i].mainName : seqAllResults[i].name;
      var modelName = seqAllResults[i].apiConfig.model; // 提取API模型名称
      // 统计票数
      nameCountMap[countKey] = (nameCountMap[countKey] || 0) + 1;
      // 收集对应模型名称
      if (!nameModelMap[countKey]) {
        nameModelMap[countKey] = [];
      }
      nameModelMap[countKey].push(modelName);
    }

    var maxNameCount = 0;
    var topNameList = [];
    for (var name in nameCountMap) {
      if (nameCountMap.hasOwnProperty(name)) {
        if (nameCountMap[name] > maxNameCount) {
          maxNameCount = nameCountMap[name];
          topNameList = [name];
        } else if (nameCountMap[name] === maxNameCount) {
          topNameList.push(name);
        }
      }
    }

    var selectedMainName = topNameList[0];
    if (topNameList.length > 1) {
      var sortedByTime = seqAllResults.sort(function(a, b) {
        return b.timestamp - a.timestamp;
      });
      for (var i = 0; i < sortedByTime.length; i++) {
        var currentKey = enableMerge ? sortedByTime[i].mainName : sortedByTime[i].name;
        if (topNameList.indexOf(currentKey) !== -1) {
          selectedMainName = currentKey;
          break;
        }
      }
    }

    // ===================== 日志开关控制+对话原文打印+格式优化 =====================
    // 仅当rizhi=1时，才打印详细投票日志
    if (rizhi === 1) {
      console.log("【🔴🔴🔴 序号" + currentSeq + " 姓名投票统计】");
      // 打印当前序号对应的对话原文，兜底无文本提示
      var currentDialog = dialogTextMap[currentSeq] ? dialogTextMap[currentSeq] : "无对应对话文本";
      console.log("对应对话：《" + currentDialog + "》");
      // 循环每个姓名，单独一行打印
      for (var nameKey in nameCountMap) {
        if (nameCountMap.hasOwnProperty(nameKey)) {
          var voteCount = nameCountMap[nameKey];
          var modelList = nameModelMap[nameKey].join("、");
          console.log("【" + nameKey + "】：" + voteCount + "票，对应模型：" + modelList);
        }
      }
      // 单独一行打印最终选中结果
      console.log("✅ 最终选中姓名：【" + selectedMainName + "】");
      console.log("----------------------------------------"); // 分割线，区分不同序号的投票
    }
    // ===================== 日志打印结束，后续原有逻辑完全不变 =====================

    // 核心2：选性别（仅选中主名的结果统计）
    var nameMatchedResults = seqAllResults.filter(function(item) {
      return enableMerge ? item.mainName === selectedMainName : item.name === selectedMainName;
    });

    var genderCountMap = {};
    for (var i = 0; i < nameMatchedResults.length; i++) {
      var gender = nameMatchedResults[i].gender;
      genderCountMap[gender] = (genderCountMap[gender] || 0) + 1;
    }

    var maxGenderCount = 0;
    var topGenderList = [];
    for (var gender in genderCountMap) {
      if (genderCountMap.hasOwnProperty(gender)) {
        if (genderCountMap[gender] > maxGenderCount) {
          maxGenderCount = genderCountMap[gender];
          topGenderList = [gender];
        } else if (genderCountMap[gender] === maxGenderCount) {
          topGenderList.push(gender);
        }
      }
    }

    var selectedGender = topGenderList[0];
    if (topGenderList.length > 1) {
      var sortedByTime = nameMatchedResults.sort(function(a, b) {
        return b.timestamp - a.timestamp;
      });
      for (var i = 0; i < sortedByTime.length; i++) {
        var currentGender = sortedByTime[i].gender;
        if (topGenderList.indexOf(currentGender) !== -1) {
          selectedGender = currentGender;
          break;
        }
      }
    }

    // 核心3：选年龄（仅选中主名+性别的结果统计）
    var genderMatchedResults = nameMatchedResults.filter(function(item) {
      return item.gender === selectedGender;
    });

    var ageCountMap = {};
    for (var i = 0; i < genderMatchedResults.length; i++) {
      var age = genderMatchedResults[i].age;
      ageCountMap[age] = (ageCountMap[age] || 0) + 1;
    }

    var maxAgeCount = 0;
    var topAgeList = [];
    for (var age in ageCountMap) {
      if (ageCountMap.hasOwnProperty(age)) {
        if (ageCountMap[age] > maxAgeCount) {
          maxAgeCount = ageCountMap[age];
          topAgeList = [age];
        } else if (ageCountMap[age] === maxAgeCount) {
          topAgeList.push(age);
        }
      }
    }

    var selectedAge = topAgeList[0];
    if (topAgeList.length > 1) {
      var sortedByTime = genderMatchedResults.sort(function(a, b) {
        return b.timestamp - a.timestamp;
      });
      for (var i = 0; i < sortedByTime.length; i++) {
        var currentAge = sortedByTime[i].age;
        if (topAgeList.indexOf(currentAge) !== -1) {
          selectedAge = currentAge;
          break;
        }
      }
    }

    finalResult[currentSeq] = {
      name: selectedMainName,
      gender: selectedGender,
      age: selectedAge
    };
  }

  console.log("【🔴🔴🔴✅ 姓名分析投票完成】 处理了" + sortedSeqList.length + "个对话，基于" + successResults.length + "个API结果");
  return finalResult;
}








// ===================== 最终优化版：别名分析结果投票函数（直接读内存变量，零冗余损耗）=====================
function voteAliasAnalyzeResult(successResults) {
  if (!successResults || !Array.isArray(successResults) || successResults.length === 0) {
    return null;
  }

  // 共用顶部开关，0=关闭合并，1=开启合并
  var enableMerge = ENABLE_ALIAS_VOTE_MERGE === 1;
  var nameToMainNameMap = {};

  // ========== 优化核心：直接读内存映射表，无重复遍历 ==========
  if (enableMerge) {
    // 优先复用内存里已经生成好的别名映射表
    if (typeof characterManager !== 'undefined' && characterManager.nameToMainNameMap) {
      nameToMainNameMap = characterManager.nameToMainNameMap;
    } 
    // 极端兜底：映射表不存在时，仅遍历一次内存变量生成
    else if (typeof characterManager !== 'undefined' && Array.isArray(characterManager.characterRecords)) {
      var records = characterManager.characterRecords;
      for (var i = 0; i < records.length; i++) {
        var record = records[i];
        if (!record || !record.name) continue;
        var mainName = record.name.trim();
        nameToMainNameMap[mainName] = mainName;
        if (record.aliases && record.aliases.trim()) {
          var aliasList = record.aliases.split("|")
            .map(function(alias) { return alias.trim(); })
            .filter(function(alias) { return alias && alias !== mainName; });
          for (var j = 0; j < aliasList.length; j++) {
            nameToMainNameMap[aliasList[j]] = mainName;
          }
        }
      }
      characterManager.nameToMainNameMap = nameToMainNameMap;
    }
    console.log("【🔵🔵🔵 别名投票别名合并】已" + (enableMerge ? "开启" : "关闭") + "，内存映射表共" + Object.keys(nameToMainNameMap).length + "条记录");
  }
  // ========== 优化结束 ==========

  // 1. 按返回时间从晚到早排序（原逻辑完全保留）
  var sortedByTime = successResults.sort(function(a, b) {
    return b.timestamp - a.timestamp;
  });

  // 2. 统计主名出现次数（适配内存映射表）
  var mainNameCountMap = {};
  for (var i = 0; i < sortedByTime.length; i++) {
    var resultData = sortedByTime[i].data;
    if (resultData.isAlias && resultData.mainName) {
      var originalMainName = resultData.mainName;
      var countKey = originalMainName;
      if (enableMerge && nameToMainNameMap.hasOwnProperty(originalMainName)) {
        countKey = nameToMainNameMap[originalMainName];
      }
      mainNameCountMap[countKey] = (mainNameCountMap[countKey] || 0) + 1;
    }
  }

  // 3. 无有效别名结果兜底
  var hasValidAlias = Object.keys(mainNameCountMap).length > 0;
  if (!hasValidAlias) {
 //   console.log("【🔵🔵🔵✅ 别名校验投票完成】 无有效别名结果，使用最晚返回的结果");
    return sortedByTime[0].data;
  }

  // 4. 找出出现次数最多的主名
  var maxCount = 0;
  var topMainNames = [];
  for (var mainName in mainNameCountMap) {
    if (mainNameCountMap.hasOwnProperty(mainName)) {
      if (mainNameCountMap[mainName] > maxCount) {
        maxCount = mainNameCountMap[mainName];
        topMainNames = [mainName];
      } else if (mainNameCountMap[mainName] === maxCount) {
        topMainNames.push(mainName);
      }
    }
  }

  // 5. 平票选最晚返回的主名
  var selectedMainName = topMainNames[0];
  if (topMainNames.length > 1) {
    for (var i = 0; i < sortedByTime.length; i++) {
      var currentResult = sortedByTime[i].data;
      if (!currentResult.isAlias || !currentResult.mainName) continue;
      
      var currentMainName = currentResult.mainName;
      if (enableMerge && nameToMainNameMap.hasOwnProperty(currentMainName)) {
        currentMainName = nameToMainNameMap[currentMainName];
      }

      if (topMainNames.indexOf(currentMainName) !== -1) {
        selectedMainName = currentMainName;
        break;
      }
    }
  }

  // 6. 找到选中主名对应的完整结果
  var finalResult = null;
  for (var i = 0; i < sortedByTime.length; i++) {
    var currentResult = sortedByTime[i].data;
    if (!currentResult.isAlias || !currentResult.mainName) continue;

    var currentMainName = currentResult.mainName;
    if (enableMerge && nameToMainNameMap.hasOwnProperty(currentMainName)) {
      currentMainName = nameToMainNameMap[currentMainName];
    }

    if (currentMainName === selectedMainName) {
      finalResult = currentResult;
      break;
    }
  }

  // 兜底逻辑
  if (!finalResult) {
    finalResult = sortedByTime[0].data;
  }

  console.log("【🔵🔵🔵✅ 别名校验投票完成】 选中主名：" + selectedMainName + "，基于" + successResults.length + "个API结果");
  return finalResult;
}






// 在智谱AI开放平台注册获取API_KEY: https://open.bigmodel.cn/
var API_KEY = 'b26b869ffd7e4a1dac61666db27de213.ayAJYkmqeA1w3OLR'; // 替换为你的API Key
var API_ENDPOINT = 'https://open.bigmodel.cn/api/paas/v4/chat/completions'; // 智谱API端点
var CONFIG = {
    resetUsageCount: 100,
    activeRecordLimit: 100,
    contextHistoryLength: 1500,
    reEvaluateThreshold: 10,
    topHistoryRecords: 4,
    voiceCheckLimit: 8,
    apiModel: "glm-4.5-flash",
    apiTemperature: 0.1,
    saveVoicesToFile: 1
};
var MAX_ALIAS_CHECK_CHARACTERS = 50;// 发给api分析的角色前50个或其他个数



// 新增：角色分析API重试次数配置（默认3次，可修改）
var CHARACTER_ANALYZE_RETRY_MAX = 8;
var next100Chars = "";
var shuohua = "";
var text2 = "";
var text3 = "";
var lastGroupIndex = 0;

// -------------------------- 辅助函数：判断是否为单一关键词连续重复（ES5兼容） --------------------------
function isSingleKeywordRepeat(text, keywords) {
  var commonPunctuation = "-。，！？：；、·…—\"“”'’()（）【】〖〗「」『』";
  var punctReg = new RegExp("[" + escapeRegExp(commonPunctuation) + "]", "g");
  var pureText = text.replace(punctReg, "");
  if (pureText.length === 0) return { isRepeat: false, keyword: null };

  for (var i = 0; i < keywords.length; i++) {
      var kw = keywords[i];
      var kwLen = kw.length;
      if (kwLen === 0 || kwLen > pureText.length) continue;

      var isMatch = true;
      for (var j = 0; j < pureText.length; j += kwLen) {
          var segment = pureText.substr(j, kwLen);
          if (segment !== kw) {
              isMatch = false;
              break;
          }
      }
      if (isMatch) {
          return { isRepeat: true, keyword: kw };
      }
  }
  return { isRepeat: false, keyword: null };
}

// -------------------------- 辅助函数：正则特殊字符转义（ES5兼容） --------------------------
function escapeRegExp(str) {
  var specialChars = /[.*+?^${}()|[\]\\]/g;
  return str.replace(specialChars, '\\$&');
}

// -------------------------- CharacterManager类（ES5兼容） --------------------------

function CharacterManager() {
  this.characterRecords = []; // 角色记录
  this.contextHistory2 = ""; // 上下文历史
  this.contextHistory = ""; // 上下文历史
  this.activeRecordLimit = CONFIG.activeRecordLimit; // 使用配置的活跃记录限制
  this.voiceUsageMap = {}; // 发音人使用情况
  this.usedVoices = {}; // 对象模拟Set（ES5兼容）
  this.availableVoices = {}; // 对象模拟Set（ES5兼容）
  this.duihuaVoicePool = {}; // 新增：初始化对话标签发音人池（关键修复）
}

CharacterManager.prototype.saveRecords = function() {
  ttsrv.writeTxtFile("characterRecords.json", JSON.stringify(this.characterRecords));
};

CharacterManager.prototype.loadRecords = function() {
  try {
      var fileContent = ttsrv.readTxtFile("characterRecords.json");
      if (!fileContent) {
          ttsrv.writeTxtFile("characterRecords.json", "[]");
          this.characterRecords = [];
          return;
      }
      this.characterRecords = JSON.parse(fileContent) || [];
      for (var i = 0; i < this.characterRecords.length; i++) {
          var record = this.characterRecords[i];
          if (!record.hasOwnProperty('aliases')) {
              record.aliases = record.name;
          }
          if (!record.voice || record.voice === "") {
              record.gender = null;
              record.age = null;
          }
          if (record.voice) {
              this.usedVoices[record.voice] = true;
          }
      }
  } catch (e) {
      this.characterRecords = [];
  }
};

CharacterManager.prototype.updateContext = function(text2) {

  this.contextHistory2 = this.contextHistory
  this.contextHistory = (this.contextHistory + "\n" + text2).slice(-CONFIG.contextHistoryLength);
};

CharacterManager.prototype.findCharacterRecord = function(characterName) {
  var normalized = characterName.trim().toLowerCase();
  for (var i = 0; i < this.characterRecords.length; i++) {
      var record = this.characterRecords[i];
      var recordName = record.name.trim().toLowerCase();
      if (recordName === normalized) {
          return record;
      }
      var aliases = record.aliases.split('|');
      for (var j = 0; j < aliases.length; j++) {
          var alias = aliases[j].trim().toLowerCase();
          if (alias === normalized) {
              return record;
          }
      }
  }
  return null;
};

CharacterManager.prototype.moveRecordToTop = function(characterName) {
  var normalized = characterName.trim().toLowerCase();
  for (var i = 0; i < this.characterRecords.length; i++) {
      var record = this.characterRecords[i];
      var recordName = record.name.trim().toLowerCase();
      if (recordName === normalized) {
          var removed = this.characterRecords.splice(i, 1)[0];
          this.characterRecords.unshift(removed);
          return;
      }
      var aliases = record.aliases.split('|');
      for (var j = 0; j < aliases.length; j++) {
          var alias = aliases[j].trim().toLowerCase();
          if (alias === normalized) {
              var removed = this.characterRecords.splice(i, 1)[0];
              this.characterRecords.unshift(removed);
              return;
          }
      }
  }
};

CharacterManager.prototype.detectAvailableVoices = function(tagsData) {
  this.availableVoices = {};
  for (var name in GENSHIN_CHARACTERS) {
      if (GENSHIN_CHARACTERS.hasOwnProperty(name)) {
          var info = GENSHIN_CHARACTERS[name];
          var voiceTag = info.voice;
          if (tagsData && tagsData[voiceTag]) {
              this.availableVoices[voiceTag] = true;
          }
      }
  }
};

CharacterManager.prototype.isVoiceAvailable = function(tag) {
  return this.availableVoices.hasOwnProperty(tag);
};

CharacterManager.prototype.assignVoice = function (gender, age) {
  // ===================== 【核心新增：duihua动态标签最高优先级匹配】=====================
  // 适配循环运行：容错判空，第一次未初始化也不会报错
  if (this.duihuaVoicePool) {
      var groupKey = gender + "/" + age;
      var duihuaCandidates = this.duihuaVoicePool[groupKey] || [];
      // 预生成的已用发音人黑名单，和原有逻辑完全对齐
      var usedVoiceMap = {};
      var mainRoleVoiceBlacklist = {};
      for (var j = 0; j < this.characterRecords.length; j++) {
          var record = this.characterRecords[j];
          if (record.voice) {
              usedVoiceMap[record.voice] = true;
              if (record.age === '主角') {
                  mainRoleVoiceBlacklist[record.voice] = true;
              }
          }
      }
      // 遍历动态标签，找可用的发音人
      for (var i = 0; i < duihuaCandidates.length; i++) {
          var voiceTag = duihuaCandidates[i];
          var isUsed = usedVoiceMap.hasOwnProperty(voiceTag) || mainRoleVoiceBlacklist.hasOwnProperty(voiceTag);
          var isAvailable = this.isVoiceAvailable(voiceTag);
          // 找到未使用、可用的动态标签，直接优先分配
          if (!isUsed && isAvailable) {
              this.voiceUsageMap[voiceTag] = (this.voiceUsageMap[voiceTag] || 0) + 1;
              this.usedVoices[voiceTag] = true;
              return voiceTag;
          }
      }
  }
  // ===================== 【新增逻辑结束，以下仅修改同年龄兜底核心逻辑】=====================

  // 预生成全局已用发音人数据，替换原前8个排除逻辑
  var usedVoiceMap = {};
  var mainRoleVoiceBlacklist = {};
  for (var j = 0; j < this.characterRecords.length; j++) {
      var record = this.characterRecords[j];
      if (record.voice) {
          usedVoiceMap[record.voice] = true;
          if (record.age === '主角') {
              mainRoleVoiceBlacklist[record.voice] = true;
          }
      }
  }

  var agePriority = {
      '男': ['男青年', '男中年', '少年', '男老年'],
      '女': ['女青年', '少女', '女中年', '女童'],
      '特殊': ['系统']
  };

  // 核心候选池：同性别同年龄匹配，排除所有已分配发音人
  var candidates = [];
  for (var name in GENSHIN_CHARACTERS) {
      if (GENSHIN_CHARACTERS.hasOwnProperty(name)) {
          var info = GENSHIN_CHARACTERS[name];
          if (info.gender === gender && info.age === age) {
              var isUsed = usedVoiceMap.hasOwnProperty(info.voice) || mainRoleVoiceBlacklist.hasOwnProperty(info.voice);
              var isAvailable = this.isVoiceAvailable(info.voice);
              if (!isUsed && isAvailable) {
                  candidates.push({ name: name, voice: info.voice, matchLevel: 0 });
              }
          }
      }
  }

  // ===================== 【核心修复：按要求重写同年龄复用逻辑】=====================
  // 第一层兜底：同性别同年龄全部分配完，按角色记录顺序去重，选去重后最末尾的发音人
  if (candidates.length === 0) {
      // 第一步：先获取当前性别+年龄的所有可用发音人映射，过滤无效发音人
      var sameTypeAvailableMap = {};
      for (var name in GENSHIN_CHARACTERS) {
          if (GENSHIN_CHARACTERS.hasOwnProperty(name)) {
              var info = GENSHIN_CHARACTERS[name];
              if (info.gender === gender && info.age === age && this.isVoiceAvailable(info.voice)) {
                  sameTypeAvailableMap[info.voice] = true;
              }
          }
      }

      // 第二步：按角色列表从上到下遍历，首次出现记录、重复忽略，生成去重列表
      var uniqueVoiceList = [];
      var recordedSet = {};
      for (var i = 0; i < this.characterRecords.length; i++) {
          var record = this.characterRecords[i];
          var voice = record.voice;
          // 仅保留当前类型可用、且未被记录过的发音人
          if (voice && sameTypeAvailableMap[voice] && !recordedSet[voice]) {
              recordedSet[voice] = true;
              uniqueVoiceList.push(voice);
          }
      }

      // 第三步：去重列表有值，选最下面的（列表最后一个元素）
      if (uniqueVoiceList.length > 0) {
          var selectedVoice = uniqueVoiceList[uniqueVoiceList.length - 1];
          this.voiceUsageMap[selectedVoice] = (this.voiceUsageMap[selectedVoice] || 0) + 1;
          this.usedVoices[selectedVoice] = true;
          return selectedVoice;
      }

      // 兜底逻辑：无历史分配记录时，按原序号排序选最大的，避免无返回值
      var allSameTypeVoices = [];
      for (var name in GENSHIN_CHARACTERS) {
          if (GENSHIN_CHARACTERS.hasOwnProperty(name)) {
              var info = GENSHIN_CHARACTERS[name];
              if (info.gender === gender && info.age === age && this.isVoiceAvailable(info.voice)) {
                  var numMatch = info.voice.match(/\d+$/);
                  var seqNum = numMatch ? parseInt(numMatch[0], 10) : 0;
                  allSameTypeVoices.push({
                      voice: info.voice,
                      seq: seqNum
                  });
              }
          }
      }
      if (allSameTypeVoices.length > 0) {
          allSameTypeVoices.sort(function (a, b) {
              return a.seq - b.seq;
          });
          var selectedVoice = allSameTypeVoices[allSameTypeVoices.length - 1].voice;
          this.voiceUsageMap[selectedVoice] = (this.voiceUsageMap[selectedVoice] || 0) + 1;
          this.usedVoices[selectedVoice] = true;
          return selectedVoice;
      }
  }

  // 年龄降级匹配逻辑：仅同年龄无任何可用发音人时才触发
  if (candidates.length === 0 && agePriority[gender]) {
      for (var i = 0; i < agePriority[gender].length; i++) {
          var similarAge = agePriority[gender][i];
          for (var name in GENSHIN_CHARACTERS) {
              if (GENSHIN_CHARACTERS.hasOwnProperty(name)) {
                  var info = GENSHIN_CHARACTERS[name];
                  if (info.gender === gender && info.age === similarAge) {
                      var isUsed = usedVoiceMap.hasOwnProperty(info.voice) || mainRoleVoiceBlacklist.hasOwnProperty(info.voice);
                      var isAvailable = this.isVoiceAvailable(info.voice);
                      if (!isUsed && isAvailable) {
                          candidates.push({
                              name: name,
                              voice: info.voice,
                              matchLevel: i + 1
                          });
                      }
                  }
              }
          }
          if (candidates.length > 0) break;
      }
  }

  // 【新增终极兜底：彻底杜绝返回null，同性别全量匹配，绝对不触发duihua】
  if (candidates.length === 0) {
      var allSameGenderVoices = [];
      for (var name in GENSHIN_CHARACTERS) {
          if (GENSHIN_CHARACTERS.hasOwnProperty(name)) {
              var info = GENSHIN_CHARACTERS[name];
              if (info.gender === gender && this.isVoiceAvailable(info.voice)) {
                  var numMatch = info.voice.match(/\d+$/);
                  var seqNum = numMatch ? parseInt(numMatch[0], 10) : 0;
                  allSameGenderVoices.push({
                      voice: info.voice,
                      seq: seqNum
                  });
              }
          }
      }
      // 同性别有可用发音人，选序号最小的优先分配
      if (allSameGenderVoices.length > 0) {
          allSameGenderVoices.sort(function (a, b) {
              return a.seq - b.seq;
          });
          var selectedVoice = allSameGenderVoices[0].voice;
          this.voiceUsageMap[selectedVoice] = (this.voiceUsageMap[selectedVoice] || 0) + 1;
          this.usedVoices[selectedVoice] = true;
          return selectedVoice;
      }
      // 极端到同性别都没可用发音人，才返回null（正常配置下永远走不到这）
      return null;
  }

  // 原排序逻辑，一字未改
  var that = this;
  candidates.sort(function (a, b) {
      if (a.matchLevel !== b.matchLevel) {
          return a.matchLevel - b.matchLevel;
      }
      var countA = that.voiceUsageMap[a.voice] || 0;
      var countB = that.voiceUsageMap[b.voice] || 0;
      return countA - countB;
  });

  // 最终选中逻辑，一字未改
  var selectedVoice = candidates[0].voice;
  this.voiceUsageMap[selectedVoice] = (this.voiceUsageMap[selectedVoice] || 0) + 1;
  this.usedVoices[selectedVoice] = true;
  return selectedVoice;
};


function voteBatchAliasAnalyzeResult(successResults, nameMapInput) {
  if (!successResults || !Array.isArray(successResults) || successResults.length === 0) return null;

  var allSeqSet = {};
  for (var i = 0; i < successResults.length; i++) {
    var data = successResults[i].data;
    for (var seq in data) {
      if (data.hasOwnProperty(seq)) allSeqSet[seq] = true;
    }
  }
  var sortedSeqs = Object.keys(allSeqSet).sort(function(a, b) {
    return parseInt(a, 10) - parseInt(b, 10);
  });

  var nameMap = nameMapInput || {};
  var finalResult = {};

  for (var s = 0; s < sortedSeqs.length; s++) {
    var seq = sortedSeqs[s];
    var currentName = nameMap[seq] || "未知";

    // 收集所有 API 的判定
    var seqItems = [];
    for (var i = 0; i < successResults.length; i++) {
      var apiResult = successResults[i];
      var item = apiResult.data[seq];
      if (item && typeof item.isAlias === "boolean" && item.hasOwnProperty("mainName")) {
        // ========== 核心修复：mainName 和原始名字相同 → 强制视为非别名 ==========
        var isAliasEffective = item.isAlias;
        var mainNameEffective = item.mainName;
        if (isAliasEffective && mainNameEffective === currentName) {
          isAliasEffective = false;
          mainNameEffective = null;
        }
        // ==================================================================
        seqItems.push({
          isAlias: isAliasEffective,
          mainName: mainNameEffective || null,
          reason: item.reason || null,
          timestamp: apiResult.timestamp,
          model: apiResult.apiConfig && apiResult.apiConfig.model ? apiResult.apiConfig.model : "未知模型"
        });
      }
    }
    if (seqItems.length === 0) continue;

    // 统计票数和模型
    var countMap = {};
    var modelMap = {};
    var latestMap = {};

    for (var j = 0; j < seqItems.length; j++) {
      var mn = seqItems[j].mainName === null ? "非别名（新角色）" : seqItems[j].mainName;
      countMap[mn] = (countMap[mn] || 0) + 1;
      if (!modelMap[mn]) modelMap[mn] = [];
      modelMap[mn].push(seqItems[j].model);
      if (!latestMap[mn] || seqItems[j].timestamp > latestMap[mn]) {
        latestMap[mn] = seqItems[j].timestamp;
      }
    }

    var maxCount = 0;
    var topCandidates = [];
    for (var mn in countMap) {
      if (countMap.hasOwnProperty(mn)) {
        if (countMap[mn] > maxCount) {
          maxCount = countMap[mn];
          topCandidates = [mn];
        } else if (countMap[mn] === maxCount) {
          topCandidates.push(mn);
        }
      }
    }

    var selectedKey = topCandidates[0];
    if (topCandidates.length > 1) {
      var latestTime = 0;
      for (var k = 0; k < topCandidates.length; k++) {
        if (latestMap[topCandidates[k]] > latestTime) {
          latestTime = latestMap[topCandidates[k]];
          selectedKey = topCandidates[k];
        }
      }
    }

    var isAlias = (selectedKey !== "非别名（新角色）");
    finalResult[seq] = {
      isAlias: isAlias,
      mainName: isAlias ? selectedKey : null,
      reason: null
    };

    // 日志
    if (rizhi === 1) {
      console.log("【🔵🔵🔵 序号" + seq + " 别名投票】");
      console.log("对应名字：" + currentName);
      for (var mn in countMap) {
        if (countMap.hasOwnProperty(mn)) {
          var models = modelMap[mn].join("、");
          console.log("【" + mn + "】：" + countMap[mn] + "票，对应模型：" + models);
        }
      }
      var finalStr = isAlias ? "合并到主名【" + selectedKey + "】" : "非别名（新角色）";
      console.log("✅ 最终：" + finalStr);
      console.log("----------------------------------------");
    }
  }

  console.log("【🔵🔵🔵✅ 批量别名投票完成】基于 " + successResults.length + " 个 API 结果");
  return finalResult;
}




CharacterManager.prototype.analyzeCharacter = function(fullText, characterId, allDialogues) {
  var requestTimeout = NAME_ANALYZE_TIMEOUT;
  var targetIndex = -1;
  for (var i = 0; i < allDialogues.length; i++) {
    if (allDialogues[i].id === characterId) {
      targetIndex = i;
      break;
    }
  }
  if (targetIndex === -1) {
    return this.analyzeCharacterFallback(fullText, characterId);
  }
  var currentDialogueText = allDialogues[targetIndex].text || "";
  var cacheMatchResult = matchDialogFromCache(currentDialogueText);
  if (cacheMatchResult) {
    return cacheMatchResult;
  }
  var fullBatchContent = generateBatchSeqContent(allDialogues, next100Chars);
  var dialogTextMap = {};
  var seqReg = /【(\d{2})】[\s\S]*?(["“])([\s\S]*?)(["”])/g;
  var seqMatch;
  while ((seqMatch = seqReg.exec(fullBatchContent)) !== null) {
    var seq = seqMatch[1];
    var rawDialog = seqMatch[3];
    dialogTextMap[seq] = rawDialog;
  }
  var prompt = 
"你是喜马拉雅听书软件中智能朗读功能的人声分配AI，任务是精准判断小说手稿中所有带【01】【02】序号标记的对话的说话人，每个序号对应一段对话。\n\n" +
"你要具备下面的能力，中文小说说话人识别（专业名称为「对话归因/说话人归属识别」），核心是将小说中的对话精准匹配到对应人物：\n" +
"1. 指代消解能力：人称代词（他/她）、身份代称（门主/师兄）、昵称与本名的精准对应，是该任务的核心难点，直接决定复杂场景的准确率 ；\n" +
"2. 隐式对话识别能力：无“XX说/道”等明确提示词的连续对话，能否通过上下文语境、人物交替逻辑正确归因；\n" +
"3. 中文小说语料适配度：对网文叙事习惯、对话格式、神态动作绑定话术的熟悉程度，避免把旁白和对话混淆、动作发出者与说话人错位；\n" +
"4. 多人对话追踪能力：3人以上交叉对话的逻辑链维护，避免连续对话中出现说话人错位。\n" +
"**【核心原则 - 最高优先级】**\n" +
"1. 严禁将对话双引号“”**内部**提及的人名当作说话人，双引号内名字是「说话者谈论的其他人」，除非是本人自我介绍；\n" +
"2. 示例：`张伟说：“别提了，都是为了王明那个项目。”` 中，说话人是**张伟**，绝非王明。\n" +
"3. 连续对话中，说话人通常交替出现，若某角色连续多句对话，需检查是否有明确提示词（如“他接着说”）或上下文支持，避免错归为同一人。\n" +
"**【输出要求】**\n" +
"1. 分析文本中所有带【01】【02】【03】...序号标记的对话，每个序号对应一个结果，序号和对话一一对应，不能错位；\n" +
"2. 返回严格的JSON格式，key为对话的序号（如'01'、'02'，必须和文本里的序号完全一致），value为对应角色信息；\n" +
"3. 如果无法确定说话人姓名，就用前后对这个人的描述作为名字，如果连描述也没有，就根据性别年龄填写“群众男青年”、“群众男中年”、“群众男老年”、“群众男童”、“群众少女”、“群众女青年”、“群众女中年”、“群众女老年”、“群众女童”、“系统”其中的一个；\n" +
"4. 必须包含文本中所有序号的对话结果，不能遗漏、不能多返回、不能少返回。\n" +
"5. 输出前，请仔细核对每个序号对应的对话内容与上下文，确保说话人归属无误；如遇歧义，优先选择上下文中最合理的角色，并避免因序号相邻而误判。\n" +
"输出格式示例：\n" +
"{\n" +
"  \"01\": {\n" +
"    \"name\": \"分析出的说话人姓名\",\n" +
"    \"gender\": \"性别（男/女/特殊）\",\n" +
"    \"age\": \"年龄分类（女性：女童/少女/女青年/女中年/女老年）；（男性：男童/少年/男青年/男中年/男老年）；（特殊：系统/旁白）\"\n" +
"  },\n" +
"  \"02\": {\n" +
"    \"name\": \"分析出的说话人姓名\",\n" +
"    \"gender\": \"性别（男/女/特殊）\",\n" +
"    \"age\": \"年龄分类（女性：女童/少女/女青年/女中年/女老年）；（男性：男童/少年/男青年/男中年/男老年）；（系统：系统/旁白）\"\n" +
"  }\n" +
"}\n" +
"\n\n";
  function sleep(ms) {
    var start = Date.now();
    while (Date.now() - start < ms) {}
  }
  var batchResult = null;
  var shuohua = typeof shuohua !== 'undefined' ? shuohua : "";
  // 修复②：重试次数直接按配置值执行，不再除以并发数（否则并发3、重试8会被算成 3 轮）
  var maxRetryRound = CHARACTER_ANALYZE_RETRY_MAX;
  var currentRound = 0;
  // 共享状态：跨重试轮次累积结果
  var accumulatedResults = [];
  var accumulatedCount = new java.util.concurrent.atomic.AtomicInteger(0);
  var latchHolder = { latch: null };
  var hasWakedUp = new java.util.concurrent.atomic.AtomicBoolean(false);
  var needWaitMultiResult = WAIT_API_RESULT_COUNT >= 3;
  var targetSuccessCount = needWaitMultiResult ? Math.min(WAIT_API_RESULT_COUNT, bingfa) : 1;
  function buildNameAnalyzeRequest(apiConfig) {
    var requestData = {
      model: apiConfig.model,
      messages: [
        { role: 'system', content: prompt },
        { role: 'user', content: "【上文历史内容】\n" + this.contextHistory2 + "\n【当前待分析对话内容】\n" + fullBatchContent }
      ],
      thinking_mode: false,
      temperature: CONFIG.apiTemperature,
      thinking: { type: "disabled" },
      disable_think: true,
      no_chain_of_thought: true,
      do_sample: false
    };
    var headers = {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + apiConfig.key,
      'Connection': 'keep-alive',
      'Timeout': requestTimeout.toString()
    };
    return {
      endpoint: apiConfig.endpoint,
      data: requestData,
      headers: headers
    };
  }
  function parseNameAnalyzeResponse(response) {
    var responseBody = response.body ? String(response.body().string() || "{}") : "{}";
    var apiResponse;
    try {
      apiResponse = JSON.parse(responseBody);
    } catch (e) {
      // 出错时打印AI返回的原始内容，便于排查
      console.log("【姓名分析-日志】响应体非合法JSON，AI返回内容：\n" + apiLogTruncate(responseBody));
      throw new Error("API返回格式错误（响应体非JSON）");
    }
    if (!apiResponse.choices || !apiResponse.choices[0] || !apiResponse.choices[0].message || !apiResponse.choices[0].message.content) {
      console.log("【姓名分析-日志】API返回格式错误（无content字段），AI返回内容：\n" + apiLogTruncate(responseBody));
      throw new Error("API返回格式错误（无content字段）");
    }
    var content = apiResponse.choices[0].message.content.trim();
    var jsonStart = content.indexOf("{");
    var jsonEnd = content.lastIndexOf("}");
    if (jsonStart === -1 || jsonEnd === -1) {
      console.log("【姓名分析-日志】返回内容无有效JSON（缺失{}），AI返回内容：\n" + apiLogTruncate(content));
      throw new Error("返回内容无有效JSON（缺失{}）");
    }
    var jsonStr = content.substring(jsonStart, jsonEnd + 1);
    var parsedResult;
    try {
      parsedResult = JSON.parse(jsonStr);
    } catch (e) {
      console.log("【姓名分析-日志】JSON解析失败，AI返回内容：\n" + apiLogTruncate(content));
      throw new Error("返回内容JSON解析失败");
    }
    var resultKeys = Object.keys(parsedResult);
    if (resultKeys.length === 0) {
      console.log("【姓名分析-日志】返回格式错误：空结果，AI返回内容：\n" + apiLogTruncate(content));
      throw new Error("返回格式错误：空结果");
    }
    var validSeqReg = /^\d{2}$/;
    for (var i = 0; i < resultKeys.length; i++) {
      var key = resultKeys[i];
      if (!validSeqReg.test(key)) {
        console.log("【姓名分析-日志】序号" + key + "不符合规范，AI返回内容：\n" + apiLogTruncate(content));
        throw new Error("序号" + key + "不符合规范");
      }
      var item = parsedResult[key];
      // ✅ 增强校验：不仅要存在，还要是非空字符串
      if (!item || 
          typeof item.name !== 'string' || item.name.trim() === '' ||
          typeof item.gender !== 'string' || item.gender.trim() === '' ||
          typeof item.age !== 'string' || item.age.trim() === '') {
        console.log("【姓名分析-日志】序号" + key + "字段无效，AI返回内容：\n" + apiLogTruncate(content));
        throw new Error("序号" + key + "字段无效（必须为非空字符串）");
      }
    }
    return parsedResult;
  }
  // 重试循环
  while (currentRound < maxRetryRound && !batchResult) {
    currentRound++;
    // 每轮创建新闩锁
    var currentLatch = new java.util.concurrent.CountDownLatch(1);
    latchHolder.latch = currentLatch;
    var sharedState = {
      resultsArray: accumulatedResults,
      successCounter: accumulatedCount,
      latchHolder: latchHolder,
      targetCount: targetSuccessCount,
      hasWakedUp: hasWakedUp
    };
    // 调用并发请求（不内部等待，由外部 latch 控制）
    concurrentApiRequest(
      "nameAnalyze",
      buildNameAnalyzeRequest.bind(this),
      parseNameAnalyzeResponse,
      null,
      requestTimeout,
      sharedState
    );
    // 等待当前轮次的闩锁
    try {
      currentLatch.await(requestTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
    } catch (e) {}
    // 只要有累积结果，就尝试投票
    if (accumulatedCount.get() > 0) {
      if (needWaitMultiResult && accumulatedResults.length > 0) {
        batchResult = voteNameAnalyzeResult(accumulatedResults, dialogTextMap);
      } else if (accumulatedResults.length > 0) {
        batchResult = accumulatedResults[0].data;
      }
      if (batchResult) break;
    }
    if (!batchResult && currentRound < maxRetryRound) {
      sleep(250);
    }
  }
  if (!batchResult) {
    console.error("【批量分析】所有重试均失败，走降级兜底逻辑");
    return this.analyzeCharacterFallback(fullText, characterId);
  }
  // ✅ 第一步：生成并校验dialogList（所有字段必须有效）
  var seqReg2 = /【(\d{2})】[\s\S]*?(["“])([\s\S]*?)(["”])/g;
  var match;
  var dialogList = [];
  var hasInvalidItem = false;
  while ((match = seqReg2.exec(fullBatchContent)) !== null) {
    var seq2 = match[1];
    var rawDialog = match[3];
    var itemResult = batchResult[seq2] || this.analyzeCharacterFallback("", "");
    // ✅ 强制校验每个结果的字段有效性
    if (!itemResult.name || !itemResult.gender || !itemResult.age ||
        typeof itemResult.name !== 'string' || itemResult.name.trim() === '') {
      hasInvalidItem = true;
      break;
    }
    dialogList.push({
      seq: seq2,
      dialogContent: rawDialog,
      name: itemResult.name.trim(),
      gender: itemResult.gender.trim(),
      age: itemResult.age.trim()
    });
  }
  // ✅ 有无效数据直接走兜底，不保存缓存
  if (hasInvalidItem || dialogList.length === 0) {
    console.error("【批量分析】生成的对话列表包含无效数据，走降级兜底");
    return this.analyzeCharacterFallback(fullText, characterId);
  }
  // ✅ 第二步：获取当前要返回的结果并校验（确保返回值有效）
  var currentSeq = padZero(targetIndex + 1, 2);
  var currentResult = batchResult[currentSeq] || this.analyzeCharacterFallback("", "");
  // ✅ 强制校验返回结果的字段有效性
  if (!currentResult.name || !currentResult.gender || !currentResult.age ||
      typeof currentResult.name !== 'string' || currentResult.name.trim() === '') {
    console.error("【批量分析】当前对话结果无效，走降级兜底");
    return this.analyzeCharacterFallback(fullText, characterId);
  }
  // ✅ 第三步：所有校验都通过后，再写入永久章节缓存（关键！最后一步才写缓存）
  var _loc = locateParagraphInFullText(text2);
  if (_loc && _loc.seqList && _loc.seqList.length > 0) {
      var _chapterTitle = _loc.chapterTitle;
      var _seqList = _loc.seqList;
      var _currentCount = allDialogues.length;
      var _lastCurrentSeq = _seqList[_seqList.length - 1];
      var _nextSeq = _lastCurrentSeq + 1;
      var _toMerge = {};
      var _involvedSeqs = [];
      for (var _p = 0; _p < dialogList.length; _p++) {
          var _realSeq;
          if (_p < _currentCount && _p < _seqList.length) {
              _realSeq = _seqList[_p];
          } else {
              _realSeq = _nextSeq + (_p - _currentCount);
              if (_loc.totalDialogs && _realSeq > _loc.totalDialogs) continue;
          }
          var _seqStr = String(_realSeq);
          _involvedSeqs.push(_seqStr);
          _toMerge[_seqStr] = {
              name: dialogList[_p].name,
              gender: dialogList[_p].gender,
              age: dialogList[_p].age,
              dialogText: dialogList[_p].dialogContent
          };
      }
      if (_involvedSeqs.length > 0) {
          overwriteChapterResults(_chapterTitle, _involvedSeqs, _toMerge);
      }
      writeProgress(getBookNameSafely(), _chapterTitle, _lastCurrentSeq);
  } else {
      // 定位失败，回退旧临时缓存
      var newCache = {
        currentIndex: targetIndex + 2,
        dialogList: dialogList
      };
      writeDialogCache(newCache);
  }
  // ✅ 第四步：处理别名分析（也放在缓存之后，不影响缓存写入）
  var unknownNames = [];
  var uniqueNamesSet = {};
  for (var seqKey in batchResult) {
    if (batchResult.hasOwnProperty(seqKey)) {
      var name = batchResult[seqKey].name.trim();
      if (name && name !== "未知" && !uniqueNamesSet[name]) {
        uniqueNamesSet[name] = true;
        if (!this.findCharacterRecord(name)) {
          unknownNames.push(name);
        }
      }
    }
  }
  var totalUnique = Object.keys(uniqueNamesSet).length;
  var unmatchedCount = unknownNames.length;
  if (unmatchedCount > 0) {
    console.log("🔵🔵【批量别名分析】共发现" + totalUnique + "个有效名字，" + unmatchedCount + "个未匹配本地角色");
    pendingAliasNames = {};
    for (var i = 0; i < unknownNames.length; i++) {
      pendingAliasNames[unknownNames[i]] = true;
    }
    pendingAliasContext = {
      contextHistory2: this.contextHistory2,
      text2: text2,
      next100Chars: next100Chars
    };
  } else {
    pendingAliasNames = {};
    pendingAliasContext = null;
  }
  shuohua = currentResult.name.trim();
  return currentResult;
};




CharacterManager.prototype.getAllCharacterNamesAndAliases = function(targetGender) {
  var allNamesSet = {};
  var nameMap = {};

  var filteredRecords = this.characterRecords.filter(function(record) {
    if (!targetGender || !record.gender) return true;
    return record.gender.trim() === targetGender.trim();
  });

  var apiLimitedRecords = filteredRecords.slice(0, MAX_ALIAS_CHECK_CHARACTERS);

  for (var i = 0; i < apiLimitedRecords.length; i++) {
    var record = apiLimitedRecords[i];
    if (!record) continue;
    var mainName = record.name.trim();
    if (!mainName) continue;

    allNamesSet[mainName] = true;
    nameMap[mainName] = mainName;

    if (record.aliases && record.aliases.trim()) {
      var aliasList = record.aliases.split("|")
        .map(function(alias) { return alias.trim(); })
        .filter(function(alias) { return alias && alias !== mainName; });
      for (var j = 0; j < aliasList.length; j++) {
        var alias = aliasList[j];
        allNamesSet[alias] = true;
        nameMap[alias] = mainName;
      }
    }
  }

  var nameListForApi = [];
  for (var nameKey in allNamesSet) {
    if (allNamesSet.hasOwnProperty(nameKey)) {
      nameListForApi.push({ name: nameKey });
    }
  }

  this.nameToMainNameMap = nameMap;
  return nameListForApi;
};





CharacterManager.prototype.processCharacter = function (fullText, characterId, allDialogues, chapterFullContent) {

  // ========== 【新增】<<发音人标注>> 快速通道 ==========
  // 如果对话内容中带有 <<发音人标签>> 标注，直接提取作为发音人，跳过AI分析
  var currentDialogueText = "";
  for (var i = 0; i < allDialogues.length; i++) {
    if (allDialogues[i].id === characterId) {
      currentDialogueText = allDialogues[i].text || "";
      break;
    }
  }
  
  // 检测 <<发音人标签>> 标注（非贪婪匹配）
  var annotationMatch = currentDialogueText.match(/<<(.*?)>>/);
  if (annotationMatch && annotationMatch[1]) {
    var annotatedVoice = annotationMatch[1].trim();
    
    // 清理文本：移除序号标记和 <<标注>> 本身
    var cleanText = String(currentDialogueText)
      .replace(/【\d+】/, "")   // 移除【序号】标记
      .replace(/<<[^><]+>>/, "");        // 移除 <<标注>>
    
    
    console.log("【<<标注>>快速通道】检测到 <<" + annotatedVoice + ">> 标注，跳过AI分析，直接使用标注发音人");
    return { text: cleanText, tag: annotatedVoice, characterInfo: record };
  }
  // ========== 【新增结束】==========



  var analysis = this.analyzeCharacter(fullText, characterId, allDialogues);
  if (!analysis) {
    return null;
  }
  var currentDialogueText = "";
  for (var i = 0; i < allDialogues.length; i++) {
    if (allDialogues[i].id === characterId) {
      // ✅ 修复1：安全获取text属性，避免undefined
      currentDialogueText = allDialogues[i].text || "";
      break;
    }
  }
  // ✅ 修复2：强制转字符串后再调用replace，彻底避免类型错误
  var cleanText = String(currentDialogueText).replace(/^(“?)【\d+】/, "$1");
  // ✅ 修复3：核心修复 - 安全获取name并trim，兜底为空字符串
  var newCharacterName = (analysis.name || "").trim();
  var targetMainRecord = null;

  if (bieming !== 0 && newCharacterName !== "未知" && Object.keys(pendingAliasNames).length > 0 && pendingAliasContext) {
    var ctx = pendingAliasContext;
    var namesToCheck = Object.keys(pendingAliasNames);
    var allRolesList = this.getAllCharacterNamesAndAliases(null);
    
    // ✅ 修复4：上下文变量统一安全转字符串
    var contextText = (String(ctx.contextHistory2 || '')) + '\n' + (String(ctx.text2 || '')) + '\n' + (String(ctx.next100Chars || ''));
    var nameSeqText = '';
    for (var i = 0; i < namesToCheck.length; i++) {
      nameSeqText += '【' + padZero(i + 1, 2) + '】' + String(namesToCheck[i] || '') + '\n';
    }
    
    var mode = bieming || 2;
    var promptPart = mode === 1
    ? "【严谨模式】必须同时满足以下条件才判定为别名：1)上下文明确指向已存角色（对话归属/动作主语/旁白指代）；2)与已存角色名字存在可解释关联（谐音/字号/绰号/身份代称/关系称谓）；3)排除同名不同人、临时NPC干扰；4)置信度≥95%。\n"
    : "【宽松模式】满足以下条件即可判定：1)上下文大概率指向已存角色；2)与已存角色存在合理关联或高度相似；3)置信度≥75%。\n";
    var batchPrompt = "你是专业的小说人物别名识别AI。请基于小说上下文和角色关系，逐一判断【待分析名字列表】中每个名字是否为【本地已存角色列表】中某个角色的别名。\n" +
    promptPart +
    "别名类型包括：字号/表字、绰号/外号、身份称谓、关系称谓、谐音/简称、笔名/化名。排除同名不同人、临时NPC、全新角色、未明确指向的泛指称谓。\n" +
    "【本地已存角色列表】\n" + JSON.stringify(allRolesList) + "\n\n" +
    "【小说上下文】\n" + contextText + "\n\n" +
    "【待分析名字列表】\n" + nameSeqText + "\n\n" +
    "【输出要求】\n" +
    "输出一个JSON，键为序号（如\"01\"），值为 {isAlias: true/false, mainName: \"匹配主名或null\", reason: \"(置信度百分比)或null\"}。\n" +
    "必须覆盖所有序号。优先根据上下文语境判断，不要仅基于字面相似度。";
    
    var requestTimeout = ALIAS_ANALYZE_TIMEOUT;
    // 修复②：重试次数直接按配置值执行，不再除以并发数（否则并发3、重试8会被算成 3 轮）
    var maxRetryRound = CHARACTER_ANALYZE_RETRY_MAX;
    var currentRound = 0;
    var finalMap = null;
    var accumulatedResults = [];
    var accumulatedCount = new java.util.concurrent.atomic.AtomicInteger(0);
    var latchHolder = { latch: null };
    var hasWakedUp = new java.util.concurrent.atomic.AtomicBoolean(false);
    var needWaitMultiResult = WAIT_API_RESULT_COUNT >= 3;
    var targetSuccessCount = needWaitMultiResult ? Math.min(WAIT_API_RESULT_COUNT, bingfa) : 1;
    
    function buildBatchRequest(apiConfig) {
      var requestData = {
        model: apiConfig.model,
        messages: [
          { role: "system", content: "严格遵守格式要求，仅输出JSON" },
          { role: "user", content: batchPrompt }
        ],
        temperature: 0.1,
        thinking_mode: false,
        thinking: { type: "disabled" },
        disable_think: true,
        no_chain_of_thought: true,
        do_sample: false
      };
      var headers = {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + apiConfig.key,
        "Connection": "keep-alive",
        "Timeout": requestTimeout.toString()
      };
      return { endpoint: apiConfig.endpoint, data: requestData, headers: headers };
    }
    
    function parseBatchResponse(response) {
      var responseBody = String(response.body().string() || "{}");
      var apiResponse;
      try {
        apiResponse = JSON.parse(responseBody);
      } catch (e) {
        console.log("【别名校验-日志】响应体非合法JSON，AI返回内容：\n" + apiLogTruncate(responseBody));
        throw new Error("响应格式错误（响应体非JSON）");
      }
      if (!apiResponse.choices || !apiResponse.choices[0] || !apiResponse.choices[0].message) {
        console.log("【别名校验-日志】响应格式错误（无choices/message），AI返回内容：\n" + apiLogTruncate(responseBody));
        throw new Error("响应格式错误");
      }
      var content = apiResponse.choices[0].message.content.trim();
      var jsonStart = content.indexOf("{");
      var jsonEnd = content.lastIndexOf("}");
      if (jsonStart === -1 || jsonEnd === -1) {
        console.log("【别名校验-日志】未找到JSON，AI返回内容：\n" + apiLogTruncate(content));
        throw new Error("未找到JSON");
      }
      var result;
      try {
        result = JSON.parse(content.substring(jsonStart, jsonEnd + 1));
      } catch (e) {
        console.log("【别名校验-日志】JSON解析失败，AI返回内容：\n" + apiLogTruncate(content));
        throw new Error("返回内容JSON解析失败");
      }
      for (var j = 0; j < namesToCheck.length; j++) {
        var seq = padZero(j + 1, 2);
        var item = result[seq];
        if (!item || typeof item.isAlias !== "boolean" || !item.hasOwnProperty("mainName")) {
          console.log("【别名校验-日志】序号" + seq + "格式错误，AI返回内容：\n" + apiLogTruncate(content));
          throw new Error("序号" + seq + "格式错误");
        }
      }
      return result;
    }
    
    function sleep2(ms) {
      var start = Date.now();
      while (Date.now() - start < ms) {}
    }
    
    while (currentRound < maxRetryRound && !finalMap) {
      currentRound++;
      var currentLatch = new java.util.concurrent.CountDownLatch(1);
      latchHolder.latch = currentLatch;
      var sharedState = {
        resultsArray: accumulatedResults,
        successCounter: accumulatedCount,
        latchHolder: latchHolder,
        targetCount: targetSuccessCount,
        hasWakedUp: hasWakedUp
      };
      concurrentApiRequest(
        "aliasAnalyze",
        buildBatchRequest,
        parseBatchResponse,
        null,
        requestTimeout,
        sharedState
      );
      try {
        currentLatch.await(requestTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
      } catch (e) {}
      if (accumulatedCount.get() > 0) {
        if (needWaitMultiResult && accumulatedResults.length > 0) {
          var nameMap = {};
          for (var i = 0; i < namesToCheck.length; i++) {
            nameMap[padZero(i + 1, 2)] = namesToCheck[i];
          }
          finalMap = voteBatchAliasAnalyzeResult(accumulatedResults, nameMap);
        } else if (accumulatedResults.length > 0) {
          finalMap = accumulatedResults[0].data;
        }
        if (finalMap) break;
      }
      if (!finalMap && currentRound < maxRetryRound) {
        sleep2(250);
      }
    }
    
    if (finalMap) {
      for (var i = 0; i < namesToCheck.length; i++) {
        var seq = padZero(i + 1, 2);
        var name = namesToCheck[i];
        var res = finalMap[seq];
        if (res && res.isAlias && res.mainName) {
          var mainRecord = this.findCharacterRecord(res.mainName);
          if (mainRecord) {
            var aliases = mainRecord.aliases ? mainRecord.aliases.split("|").map(function(a) { return a.trim(); }) : [];
            if (aliases.indexOf(name) === -1) {
              aliases.push(name);
              mainRecord.aliases = aliases.join("|");
            }
          }
        }
      }
      this.saveRecords();
    }
    
    pendingAliasNames = {};
    pendingAliasContext = null;
  } else if (bieming === 0) {
    var existingRecord = this.findCharacterRecord(newCharacterName);
    if (existingRecord) targetMainRecord = existingRecord;
  }
  if (!targetMainRecord) {
    var record = this.findCharacterRecord(newCharacterName);
    if (record) targetMainRecord = record;
  }
  if (newCharacterName === "未知") {
    return { text: cleanText, tag: "duihua" };
  }
  if (!targetMainRecord) {
    var voice = this.assignVoice(analysis.gender, analysis.age);
    if (!voice) {
      return { text: cleanText, tag: analysis.gender === "男" ? "duihuaA" : analysis.gender === "女" ? "duihuaB" : "duihua" };
    }
    targetMainRecord = {
      name: newCharacterName,
      aliases: newCharacterName,
      gender: analysis.gender,
      age: analysis.age,
      voice: voice,
      usageCount: CONFIG.resetUsageCount,
      genderAgeHistory: [{ gender: analysis.gender, age: analysis.age }]
    };
    this.characterRecords.unshift(targetMainRecord);
  } else {
    var isVoiceInvalid = !targetMainRecord.voice || targetMainRecord.voice === "" || !this.availableVoices[targetMainRecord.voice];
    if (isVoiceInvalid) {
      var newVoice = this.assignVoice(analysis.gender, analysis.age);
      if (newVoice) {
        targetMainRecord.voice = newVoice;
        targetMainRecord.gender = analysis.gender;
        targetMainRecord.age = analysis.age;
        this.saveRecords();
      } else {
        targetMainRecord.voice = analysis.gender === "男" ? "duihuaA" : analysis.gender === "女" ? "duihuaB" : "duihua";
      }
    }
    if (targetMainRecord.usageCount === 100) {
      this.moveRecordToTop(targetMainRecord.name);
      this.saveRecords();
      return { text: cleanText, tag: targetMainRecord.voice || "default", characterInfo: targetMainRecord };
    }
    if (targetMainRecord.usageCount === 50) {
      if (!targetMainRecord.voice || targetMainRecord.voice === "") {
        targetMainRecord.voice = this.assignVoice(targetMainRecord.gender, targetMainRecord.age);
      } else {
        var voiceInfo = null;
        for (var key in GENSHIN_CHARACTERS) {
          if (GENSHIN_CHARACTERS[key].voice === targetMainRecord.voice) {
            voiceInfo = GENSHIN_CHARACTERS[key];
            break;
          }
        }
        if (voiceInfo && (voiceInfo.gender !== targetMainRecord.gender || voiceInfo.age !== targetMainRecord.age)) {
          targetMainRecord.voice = this.assignVoice(targetMainRecord.gender, targetMainRecord.age);
        }
      }
      this.moveRecordToTop(targetMainRecord.name);
      return { text: cleanText, tag: targetMainRecord.voice || "default", characterInfo: targetMainRecord };
    }
    if (!targetMainRecord.voice || targetMainRecord.voice === "") {
      targetMainRecord.voice = this.assignVoice(analysis.gender, analysis.age);
      if (!voice) {
        return { text: cleanText, tag: analysis.gender === "男" ? "duihuaA" : analysis.gender === "女" ? "duihuaB" : "duihua" };
      }
      targetMainRecord.gender = analysis.gender;
      targetMainRecord.age = analysis.age;
    }
    if (targetMainRecord.gender === null || targetMainRecord.age === null) {
      targetMainRecord.gender = analysis.gender;
      targetMainRecord.age = analysis.age;
    }
    if (!targetMainRecord.genderAgeHistory) targetMainRecord.genderAgeHistory = [];
    targetMainRecord.usageCount--;
    targetMainRecord.genderAgeHistory.unshift({ gender: analysis.gender, age: analysis.age });
    if (targetMainRecord.genderAgeHistory.length >= CONFIG.reEvaluateThreshold) this.reEvaluateCharacter(targetMainRecord);
    if (targetMainRecord.usageCount < 0) this.reEvaluateCharacter(targetMainRecord);
  }
  this.moveRecordToTop(targetMainRecord.name);
  if (this.characterRecords.length > this.activeRecordLimit) {
    var removed = this.characterRecords.pop();
    var voiceStillUsed = false;
    for (var i = 0; i < this.characterRecords.length; i++) {
      if (this.characterRecords[i].voice === removed.voice) {
        voiceStillUsed = true;
        break;
      }
    }
    if (!voiceStillUsed) {
      delete this.usedVoices[removed.voice];
      delete this.voiceUsageMap[removed.voice];
    }
  }
  this.saveRecords();
  return { text: cleanText, tag: targetMainRecord.voice || "default", characterInfo: targetMainRecord };
};




// 新增：读取缓存中旁白条目的辅助函数（ES5兼容，复用原有缓存逻辑）
function getCacheNarrationList() {
  try {
    var progress = readProgress();
    if (!progress) return [];
    var cache = readChapterCache(progress.chapterTitle);
    var results = cache.results || {};
    var narrationList = [];
    // 筛选name为旁白的有效条目（兼容dialogText新字段）
    for (var key in results) {
        if (results.hasOwnProperty(key)) {
            var item = results[key];
            if (item && item.name && String(item.name).trim() === "旁白" && (item.dialogText || item.dialogContent)) {
                narrationList.push(item);
            }
        }
    }
    return narrationList;
  } catch (e) {
    // 异常返回空数组，完全不影响原有流程
    return [];
  }
}






  
// ===================== 永久章节缓存函数（对齐其他APP脚本，存储到 ../xiaoshuo/{书名}/{章节}.json）=====================
function sanitizeFileName(name) {
    return String(name || "").replace(/\?/g, "？").replace(/[\\/:*?"<>|]/g, "＿");
}
function getBookNameSafely() {
    try {
        var dataStr = ttsrv.readTxtFile("data.json").toString();
        if (!dataStr || dataStr.trim() === "") return "default_book";
        var data = JSON.parse(dataStr);
        return String(data.bookName || "未知书名").trim().replace(/[\\/:*?"<>|]/g, "_").substring(0, 50);
    } catch (e) { return "default_book_" + new Date().getTime(); }
}
function getBookDir() {
    return CACHE_ROOT + sanitizeFileName(getBookNameSafely()) + "/";
}
function getChapterCachePath(chapterTitle) {
    return getBookDir() + sanitizeFileName(chapterTitle) + ".json";
}
function readProgress() {
    try {
        var content = ttsrv.readTxtFile(PROGRESS_FILE).toString();
        if (!content || content.trim() === "") return null;
        var prog = JSON.parse(content);
        if (prog && prog.bookName && prog.chapterTitle && typeof prog.lastSeq === "number") return prog;
    } catch (e) {}
    return null;
}
function writeProgress(bookName, chapterTitle, lastSeq) {
    try {
        ttsrv.writeTxtFile(PROGRESS_FILE, JSON.stringify({
            bookName: bookName, chapterTitle: chapterTitle, lastSeq: lastSeq
        }));
    } catch (e) {}
}
// 提取段落中所有引号内对话（复刻其他APP脚本的 extractDialogs）
function extractQuoteDialogs(paragraph) {
    var dialogs = [];
    var len = paragraph.length, idx = 0;
    while (idx < len) {
        var leftPos = paragraph.indexOf("\u201c", idx);
        if (leftPos === -1) break;
        var rightPos = paragraph.indexOf("\u201d", leftPos + 1);
        var content, hasRightQuote;
        if (rightPos !== -1) { content = paragraph.substring(leftPos + 1, Math.min(rightPos, leftPos + 1 + 1000)); hasRightQuote = true; }
        else { content = paragraph.substring(leftPos + 1, Math.min(len, leftPos + 1 + 1000)); hasRightQuote = false; }
        if (content && content.length >= 1) {
            dialogs.push({ content: content, index: leftPos, length: hasRightQuote ? content.length + 2 : content.length + 1, hasRightQuote: hasRightQuote });
        }
        idx = hasRightQuote ? rightPos + 1 : len;
    }
    return dialogs;
}
// 全文定位：识别章节标题 + 计算段落对话的章节内全局序号（复刻其他APP脚本的 locateParagraphInFullText）
function locateParagraphInFullText(paragraph) {
    try {
        var dataStr = ttsrv.readTxtFile("data.json").toString();
        if (!dataStr || dataStr.trim() === "") return null;
        var data = JSON.parse(dataStr);
        var fullText = String(data.texts || "");
    } catch (e) { return null; }
    if (!fullText) return null;

    var paraPos = fullText.indexOf(paragraph);
    if (paraPos === -1) {
        var shortPara = paragraph.substring(0, Math.min(paragraph.length, 50));
        paraPos = fullText.indexOf(shortPara);
        if (paraPos === -1) return null;
    }

    var lines = fullText.split(/\r?\n/);
    var chapters = [];
    var currentTitle = null, currentBody = "";
    for (var i = 0; i < lines.length; i++) {
        var line = lines[i];
        if (line.trim() === "") continue;
        var startsWithIndent = (line.charAt(0) === "\u3000" || line.charAt(0) === " ");
        if (!startsWithIndent && !currentTitle) {
            currentTitle = line.trim();
            currentBody = "";
        } else if (!startsWithIndent && currentTitle) {
            if (currentBody.trim() !== "") chapters.push({ title: currentTitle, fullText: currentBody });
            currentTitle = line.trim();
            currentBody = "";
        } else {
            currentBody += line + "\n";
        }
    }
    if (currentTitle && currentBody.trim() !== "") chapters.push({ title: currentTitle, fullText: currentBody });

    var chapterTitle = null;
    var chapterFullText = "";
    var cumulativeLength = 0;
    for (var i = 0; i < chapters.length; i++) {
        var ch = chapters[i];
        var nextCumulative = cumulativeLength + ch.fullText.length;
        if (paraPos < nextCumulative) {
            chapterTitle = ch.title;
            chapterFullText = ch.fullText;
            break;
        }
        cumulativeLength = nextCumulative;
    }
    if (!chapterTitle) return null;

    var paraPosInChapter = chapterFullText.indexOf(paragraph);
    if (paraPosInChapter === -1) {
        paraPosInChapter = chapterFullText.indexOf(paragraph.substring(0, Math.min(paragraph.length, 50)));
        if (paraPosInChapter === -1) return null;
    }
    var beforeText = chapterFullText.substring(0, paraPosInChapter);
    var beforeCount = (beforeText.match(/\u201c/g) || []).length;
    var dialogs = extractQuoteDialogs(paragraph);
    var seqList = [];
    for (var n = 0; n < dialogs.length; n++) seqList.push(beforeCount + n + 1);
    var totalDialogs = (chapterFullText.match(/\u201c/g) || []).length;
    return { chapterTitle: chapterTitle, seqList: seqList, totalDialogs: totalDialogs };
}
function readChapterCache(chapterTitle) {
    try {
        var path = getChapterCachePath(chapterTitle);
        var content = ttsrv.readTxtFile(path).toString();
        if (!content || content.trim() === "") return { title: chapterTitle, results: {} };
        var cache = JSON.parse(String(content));
        return (cache && cache.results) ? cache : { title: chapterTitle, results: {} };
    } catch (e) { return { title: chapterTitle, results: {} }; }
}
function writeChapterCache(chapterTitle, cacheData) {
    try { ttsrv.writeTxtFile(getChapterCachePath(chapterTitle), JSON.stringify(cacheData, null, 2)); } catch (e) {}
}
function mergeChapterResults(chapterTitle, newResults) {
    var cache = readChapterCache(chapterTitle);
    if (!cache.results) cache.results = {};
    for (var key in newResults) {
        if (newResults.hasOwnProperty(key)) cache.results[key] = newResults[key];
    }
    writeChapterCache(chapterTitle, cache);
}
// ★ 覆盖式写入：先删除本次涉及序号的所有旧值，再写入新值，彻底防止旧错误结果残留
function overwriteChapterResults(chapterTitle, involvedSeqs, newResults) {
    var cache = readChapterCache(chapterTitle);
    if (!cache.results) cache.results = {};
    // 第一步：删除本次涉及序号的所有旧值（无论本次是否返回了该序号）
    for (var i = 0; i < involvedSeqs.length; i++) {
        delete cache.results[involvedSeqs[i]];
    }
    // 第二步：写入本次新值
    for (var key in newResults) {
        if (newResults.hasOwnProperty(key)) cache.results[key] = newResults[key];
    }
    writeChapterCache(chapterTitle, cache);
}
function tryMatchTextWithNewlines(cachedText, cleanCurrent) {
    if (!cachedText) return false;
    if (cleanDialogText(cachedText) === cleanCurrent) return true;
    var lines = String(cachedText).split("\n");
    for (var i = 0; i < lines.length; i++) {
        if (cleanDialogText(lines[i]) === cleanCurrent) return true;
    }
    return false;
}

  // ===================== 新增：批量对话缓存辅助函数（ES5兼容，无侵入）=====================
  // ===================== 终极兼容版：根源读取函数（直接替换原函数即可）=====================
function readDialogCache() {
  try {
      var content = ttsrv.readTxtFile("dialog_cache.json");
      // 兼容空文件、空字符串：直接走兜底
      if (!content || content.trim() === "") {
          return { currentIndex: 1, dialogList: [] };
      }
      var rawCache = JSON.parse(content);
      // 兼容空对象：强制兜底核心字段
      if (!rawCache || typeof rawCache !== "object") {
          return { currentIndex: 1, dialogList: [] };
      }

      // 根源1：强制过滤dialogList，只保留带dialogContent的有效对象，剔除null/undefined/脏数据
      var safeDialogList = Array.isArray(rawCache.dialogList) 
          ? rawCache.dialogList.filter(function(item) {
              return item && typeof item === "object" && item.dialogContent !== undefined;
          }) 
          : [];

      // 根源2：强制修正currentIndex，永远不超出数组合法范围，彻底杜绝越界
      var safeCurrentIndex = typeof rawCache.currentIndex === "number" && rawCache.currentIndex >= 1
          ? rawCache.currentIndex
          : 1;
      // 核心修正：索引最大不能超过「数组长度+1」，哪怕你写100，也会被拉回合法值
      var maxLegalIndex = safeDialogList.length + 1;
      if (safeCurrentIndex > maxLegalIndex) {
          safeCurrentIndex = Math.max(1, safeDialogList.length);
      }

      // 返回绝对安全的结构，没有任何undefined风险
      return {
          currentIndex: safeCurrentIndex,
          dialogList: safeDialogList
      };
  } catch (e) {
      // 任何异常（文件不存在、JSON解析失败），都返回安全兜底结构
      return { currentIndex: 1, dialogList: [] };
  }
}

  // 写入对话缓存文件
  function writeDialogCache(cacheData) {
    try {
        ttsrv.writeTxtFile("dialog_cache.json", JSON.stringify(cacheData, null, 2));
        return true;
    } catch (e) {
        return false;
    }
  }
  
// 修复后：全局统一的文本清理规则，彻底清除所有不可见空白符
function cleanDialogText(text) {
  return text

      .replace(/(.[\u4e00-\u9fa5]+音效.)/g, "") // 清除音效
      .replace(/[\s\u3000\u2000-\u200F\u2028-\u202F\uFEFF]/g, "") // 清除所有半角/全角/零宽/换行不可见空白符
      .replace(/【\d+】/g, "") // 移除序号标记
      .replace(/[“”"''"]/g, "") // 移除所有引号
      .replace(/[^\u4e00-\u9fa5\u3002\uff1f\uff01\uff0c\uff1b\uff1a\u3001\u201c\u201d\u2018\u2019\uff08\uff09\u3010\u3011\u300a\u300b\u2026\u2014\u00b7a-zA-Z0-9.,!?;:"'()\[\]{}<>-]/g, "")
      .trim();
}


// 通用：按换行分割文本，过滤空行，返回有效行数组
function splitDialogByLine(text) {
    if (!text || text.trim() === "") return [];
    var lines = text.split("\n");
    var validLines = [];
    for (var i = 0; i < lines.length; i++) {
        var line = lines[i].trim();
        if (line !== "") validLines.push(line);
    }
    return validLines;
}

// 通用：单行文本匹配核心逻辑（对话/旁白共用）
function matchSingleLine(targetText, cacheDialogItem) {
    var targetClean = cleanDialogText(targetText);
    if (targetClean === "") return false;

    // 兼容新缓存字段dialogText和旧字段dialogContent
    var rawContent = cacheDialogItem.dialogContent || cacheDialogItem.dialogText || "";
    // 缓存对话按换行分割，逐行匹配
    var cacheLines = splitDialogByLine(rawContent);
    for (var i = 0; i < cacheLines.length; i++) {
        var lineClean = cleanDialogText(cacheLines[i]);
        if (lineClean === targetClean && lineClean !== "") {
            return true;
        }
    }
    return false;
}

// ===================== 新增辅助函数 =====================

function matchNarrationFromCache(narrationText) {
  var progress = readProgress();
  if (!progress) return null;
  var chapterTitle = progress.chapterTitle;
  var cache = readChapterCache(chapterTitle);
  var results = cache.results || {};
  var MAX_OFFSET = 3;

  // 将章节缓存results转换为兼容的dialogList结构（保留原匹配逻辑不变）
  var seqKeys = [];
  for (var k in results) { if (results.hasOwnProperty(k)) seqKeys.push(parseInt(k, 10)); }
  if (seqKeys.length === 0) return null;
  seqKeys.sort(function(a, b) { return a - b; });

  var dialogList = [];
  for (var si = 0; si < seqKeys.length; si++) {
      var _nItem = results[String(seqKeys[si])];
      dialogList.push({
          seq: String(seqKeys[si]),
          dialogContent: _nItem.dialogText || _nItem.dialogContent || "",
          name: _nItem.name, gender: _nItem.gender, age: _nItem.age
      });
  }

  var predictedSeq = progress.lastSeq + 1;
  var currentIndex = 1;
  for (var di = 0; di < seqKeys.length; di++) {
      if (seqKeys[di] >= predictedSeq) { currentIndex = di + 1; break; }
      currentIndex = di + 1;
  }
  if (currentIndex < 1) currentIndex = 1;
  if (currentIndex > dialogList.length) currentIndex = dialogList.length;

  var cleanCurrent = cleanDialogText(narrationText);
  if (cleanCurrent === "") {
      return null;
  }

  var matchedItem = null;
  var finalMatchedIndex = -1;

  function getValidLineCount(dialogContent) {
      if (!dialogContent) return 0;
      var lines = dialogContent.split("\n").filter(function(line) {
          return line.trim() !== "";
      });
      return lines.length;
  }

  function isLineMatchExact(targetText, cacheDialogContent) {
      if (!cacheDialogContent || cacheDialogContent.trim() === "") return false;
      var cacheLines = cacheDialogContent.split("\n").filter(function(line) {
          return line.trim() !== "";
      });
      if (cacheLines.length < 2) return false;
      for (var i = 0; i < cacheLines.length; i++) {
          var cleanCacheLine = cleanDialogText(cacheLines[i]);
          if (cleanCacheLine === cleanCurrent && cleanCurrent !== "") {
              return true;
          }
      }
      return false;
  }

  function tryMatchEntry(entry, idx) {
      if (getValidLineCount(entry.dialogContent) < 2) {
          return false;
      }
      if (isLineMatchExact(narrationText, entry.dialogContent)) {
          matchedItem = entry;
          finalMatchedIndex = idx + 1;
          return true;
      }
      return false;
  }

  var currentArrayIndex = currentIndex - 1;
  if (currentArrayIndex >= 0 && currentArrayIndex < dialogList.length) {
      tryMatchEntry(dialogList[currentArrayIndex], currentArrayIndex);
  }

  if (!matchedItem) {
      for (var offset = 1; offset <= MAX_OFFSET; offset++) {
          var targetIdx = currentIndex - 1 - offset;
          if (targetIdx < 0) break;
          if (tryMatchEntry(dialogList[targetIdx], targetIdx)) break;
      }
  }

  if (!matchedItem) {
      for (var offset = 1; offset <= MAX_OFFSET; offset++) {
          var targetIdx = currentIndex - 1 + offset;
          if (targetIdx >= dialogList.length) break;
          if (tryMatchEntry(dialogList[targetIdx], targetIdx)) break;
      }
  }

  if (!matchedItem) {
      for (var i = 0; i < dialogList.length; i++) {
          if (tryMatchEntry(dialogList[i], i)) break;
      }
  }

  if (matchedItem && matchedItem.name) {
      var roleName = matchedItem.name;
      var characterRecord = null;

      characterRecord = characterManager.findCharacterRecord(roleName);
      if (!characterRecord || !characterRecord.voice) {
          if (characterManager && characterManager.characterRecords) {
              for (var i = 0; i < characterManager.characterRecords.length; i++) {
                  var rec = characterManager.characterRecords[i];
                  if (!rec) continue;
                  if (rec.name === roleName) {
                      characterRecord = rec;
                      break;
                  }
                  if (rec.aliases) {
                      var aliases = rec.aliases.split("|");
                      for (var j = 0; j < aliases.length; j++) {
                          if (aliases[j].trim() === roleName) {
                              characterRecord = rec;
                              break;
                          }
                      }
                      if (characterRecord) break;
                  }
              }
          }
      }

      if (!characterRecord || !characterRecord.voice) {
          try {
              var fileContent = ttsrv.readTxtFile("characterRecords.json");
              if (fileContent && fileContent.trim() !== "") {
                  var recordsFromFile = JSON.parse(fileContent);
                  if (Array.isArray(recordsFromFile)) {
                      for (var i = 0; i < recordsFromFile.length; i++) {
                          var rec = recordsFromFile[i];
                          if (!rec || !rec.name) continue;
                          if (rec.name === roleName) {
                              characterRecord = rec;
                              break;
                          }
                          if (rec.aliases) {
                              var aliases = rec.aliases.split("|");
                              for (var j = 0; j < aliases.length; j++) {
                                  if (aliases[j].trim() === roleName) {
                                      characterRecord = rec;
                                      break;
                                  }
                              }
                              if (characterRecord) break;
                          }
                      }
                      if (characterRecord && characterRecord.voice) {
                          var existing = characterManager.findCharacterRecord(roleName);
                          if (!existing) {
                              characterManager.characterRecords.unshift(characterRecord);
                              characterManager.saveRecords();
                          }
                      }
                  }
              }
          } catch (fileErr) {}
      }

      if (characterRecord && characterRecord.voice) {
          if (finalMatchedIndex > 0) {
              var _nHitSeq = seqKeys[finalMatchedIndex - 1] || finalMatchedIndex;
              writeProgress(progress.bookName, chapterTitle, _nHitSeq);
          }
          return {
              name: roleName,
              gender: characterRecord.gender,
              age: characterRecord.age,
              voice: characterRecord.voice
          };
      }
  }

  // ===================== 回退机制：进度浮动匹配失败，全文重新定位章节+序号后再查缓存（复刻其他APP脚本）=====================
  if (!matchedItem) {
      var _narrClean = cleanDialogText(narrationText);
      var _nreloc = locateParagraphInFullText(text2);
      if (_nreloc && _nreloc.seqList && _nreloc.seqList.length > 0 && _narrClean !== "") {
          var _nreChapter = _nreloc.chapterTitle;
          var _nreSeqList = _nreloc.seqList;
          var _nreCache = (_nreChapter === chapterTitle) ? cache : readChapterCache(_nreChapter);
          var _nreResults = _nreCache.results || {};
          for (var _nri = 0; _nri < _nreSeqList.length; _nri++) {
              var _nrePredicted = _nreSeqList[_nri];
              for (var _nroff = -2; _nroff <= 2; _nroff++) {
                  var _ntrySeq = _nrePredicted + _nroff;
                  if (_ntrySeq < 1) continue;
                  var _ntryItem = _nreResults[String(_ntrySeq)];
                  if (!_ntryItem) continue;
                  var _ntryText = _ntryItem.dialogText || _ntryItem.dialogContent || "";
                  if (tryMatchTextWithNewlines(_ntryText, _narrClean)) {
                      var _nroleName = _ntryItem.name;
                      var _nrec = characterManager.findCharacterRecord(_nroleName);
                      if (_nrec && _nrec.voice) {
                          writeProgress(progress.bookName, _nreChapter, _ntrySeq);
                          return {
                              name: _nroleName,
                              gender: _nrec.gender,
                              age: _nrec.age,
                              voice: _nrec.voice
                          };
                      }
                  }
              }
          }
          writeProgress(progress.bookName, _nreChapter, _nreSeqList[_nreSeqList.length - 1]);
      }
  }

  return null;
}

// 生成带【01】【02】序号的批量对话内容（仅修改此函数，其他代码100%不动）
function generateBatchSeqContent(dialoguesList, nextContent) {
    // ===================== 序号添加比例配置（顶部变量，默认0.8，可直接修改调整） =====================

    // ===================== 前置判断：严格按要求通过text2匹配正则，判断末尾右引号是否为自动添加 =====================
    var isEndQuoteAutoAdded = /(“[^”]+)$/.test(text2);
    
    // ===================== 第一步：先清理两个核心内容（dialoguesList + nextContent） =====================
    // 1. 清理对话列表：去旧序号、移除自动补的末尾引号，逐行拼接
    var cleanedDialogues = "";
    for (var i = 0; i < dialoguesList.length; i++) {
        var dialogText = dialoguesList[i].text || "";
        // 清理原有旧序号，避免重复
        var cleanItem = dialogText.replace(/^【\d+】/, "");
        // 严格限定：仅最后一条对话、且是自动补的引号，才移除末尾右引号
        if (isEndQuoteAutoAdded && i === dialoguesList.length - 1) {
            cleanItem = cleanItem.replace(/”$/, "");
        }
        // 逐行拼接清理后的对话
        cleanedDialogues += cleanItem + "\n";
    }

    // 2. 清理下文内容：移除旧序号
    var cleanedNextContent = (nextContent || "").replace(/【\d+】/g, "");

    // ===================== 第二步：拼接两个清理后的内容，得到完整原始文本 =====================
    var fullRawText = cleanedDialogues + cleanedNextContent;

    // ===================== 第三步：执行指定的两个正则替换，处理换行引号错位问题 =====================
    // 正则1：补换行后缺失的左引号
    fullRawText = fullRawText.replace(/(.[\u4e00-\u9fa5]+音效.)/g, "");// 清除音效
    fullRawText = fullRawText.replace(/【\d\d?】/g, "");
    fullRawText = fullRawText.replace(/(”[^“”]*\n)([^“”\n]+”)/g, "$1“$2");
    // 正则2：补换行前缺失的右引号
    fullRawText = fullRawText.replace(/(“[^“”\n]+)(\n[^“”]*“)/g, "$1”$2");

    fullRawText = fullRawText.replace(/[『「【〈〉〔‘’〕】」』]/g, "");

    // ===================== 第四步：按规则计算添加序号的数量 =====================
    var allLeftQuotes = fullRawText.match(/“/g);
    var totalQuoteCount = allLeftQuotes ? allLeftQuotes.length : 0;
    var stopAddIndex;

    // 核心修改：总引号数≤5，全部加序号；超过5个，按配置比例计算
    if (totalQuoteCount <= 5) {
        stopAddIndex = totalQuoteCount;
    } else {
        // 超过5个，按比例计算，保底至少1个序号，避免有对话却无序号的异常
        stopAddIndex = Math.max(Math.floor(totalQuoteCount * SEQ_ADD_RATIO), 1);
    }

    var seqCounter = 0; // 序号计数器，按匹配顺序递增

    // ===================== 第五步：统一给处理后的文本添加序号 =====================
    var finalContentWithSeq = fullRawText.replace(/“/g, function (match) {
        seqCounter++;
        // 仅在计算好的范围内添加序号，超出范围保留原引号
        if (seqCounter <= stopAddIndex) {
            return "【" + padZero(seqCounter, 2) + "】" + match;
        }
        return match;
    });

    // 返回最终带序号的完整内容（直接传给API分析）
    return finalContentWithSeq;
}


// ==================== 修改后完整matchDialogFromCache函数（直接替换原函数即可，其他代码全不动）====================
function matchDialogFromCache(currentDialogText) {
  var progress = readProgress();
  if (!progress) return null;
  var chapterTitle = progress.chapterTitle;
  var cache = readChapterCache(chapterTitle);
  var results = cache.results || {};

  // 将章节缓存results转换为兼容的dialogList结构（保留原匹配逻辑不变）
  var seqKeys = [];
  for (var k in results) { if (results.hasOwnProperty(k)) seqKeys.push(parseInt(k, 10)); }
  if (seqKeys.length === 0) return null;
  seqKeys.sort(function(a, b) { return a - b; });

  var dialogList = [];
  for (var si = 0; si < seqKeys.length; si++) {
      var _dItem = results[String(seqKeys[si])];
      dialogList.push({
          seq: String(seqKeys[si]),
          dialogContent: _dItem.dialogText || _dItem.dialogContent || "",
          name: _dItem.name, gender: _dItem.gender, age: _dItem.age
      });
  }

  var predictedSeq = progress.lastSeq + 1;
  var currentIndex = 1;
  for (var di = 0; di < seqKeys.length; di++) {
      if (seqKeys[di] >= predictedSeq) { currentIndex = di + 1; break; }
      currentIndex = di + 1;
  }
  if (currentIndex < 1) currentIndex = 1;
  if (currentIndex > dialogList.length) currentIndex = dialogList.length;

  var MAX_FORWARD_OFFSET = 2;  // 完全保留原代码的偏移范围
  var MAX_BACKWARD_OFFSET = 2; // 完全保留原代码的偏移范围

  // 完全复用原代码的文本清理规则，无任何改动，保证匹配一致性
  function cleanDialogText(text) {
      return text

          .replace(/.[\u4e00-\u9fa5]+音效./g, "") // 清除音效
          .replace(/[\s\u3000\u2000-\u200F\u2028-\u202F\uFEFF]/g, "") // 清除所有半角/全角/零宽/换行不可见空白符
          .replace(/【\d+】/g, "")
          .replace(/[“”"''"]/g, "")
          .replace(/\s+/g, "")
          .replace(/[^\u4e00-\u9fa5\u3002\uff1f\uff01\uff0c\uff1b\uff1a\u3001\u201c\u201d\u2018\u2019\uff08\uff09\u3010\u3011\u300a\u300b\u2026\u2014\u00b7a-zA-Z0-9.,!?;:"'()\[\]{}<>-]/g, "")
          .trim();
  }

  var cleanCurrent = cleanDialogText(currentDialogText);
  var matchedResult = null;
  var finalMatchedIndex = -1;

  // ===================== 匹配优先级1：当前目标位置（最高优先级，原逻辑不变，仅改匹配规则）=====================
  var currentArrayIndex = currentIndex - 1;
  if (currentArrayIndex >= 0 && currentArrayIndex < dialogList.length) {
      var currentTargetItem = dialogList[currentArrayIndex];
      // 核心修改：缓存内容按换行拆分，逐行匹配，无任何整体匹配
      var cacheLines = currentTargetItem.dialogContent.split("\n").filter(function(line) {
          return line.trim() !== ""; // 过滤空行，避免无效匹配
      });
      var isMatch = false;
      // 遍历拆分后的每一行，分别清理匹配，匹配上任意一行就算成功
      for (var i = 0; i < cacheLines.length; i++) {
          var cleanCacheLine = cleanDialogText(cacheLines[i]);
          //console.log("【优先匹配】当前序号" + (currentArrayIndex + 1) + " | 当前清理后：" + cleanCurrent + " | 缓存拆分行" + (i+1) + "清理后：" + cleanCacheLine);
          if (cleanCacheLine === cleanCurrent && cleanCurrent !== "") {
              isMatch = true;
              break;
          }
      }

      if (isMatch) {
          matchedResult = {
              name: currentTargetItem.name,
              gender: currentTargetItem.gender,
              age: currentTargetItem.age
          };
          finalMatchedIndex = currentArrayIndex + 1;
          //console.log("【匹配成功】命中当前序号" + finalMatchedIndex + "，角色：" + currentTargetItem.name);
      }
  }

  // ===================== 匹配优先级2：向前偏移（原逻辑不变，仅改匹配规则）=====================
  if (!matchedResult) {
      for (var offset = 1; offset <= MAX_FORWARD_OFFSET; offset++) {
          var targetArrayIndex = currentIndex - 1 - offset;
          if (targetArrayIndex < 0) break;

          var targetItem = dialogList[targetArrayIndex];
          // 同样按换行拆分逐行匹配，和主逻辑完全一致
          var cacheLines = targetItem.dialogContent.split("\n").filter(function(line) {
              return line.trim() !== "";
          });
          var isMatch = false;
          for (var i = 0; i < cacheLines.length; i++) {
              var cleanCacheLine = cleanDialogText(cacheLines[i]);
              //console.log("【向前匹配】尝试序号" + (targetArrayIndex + 1) + " | 当前清理后：" + cleanCurrent + " | 缓存拆分行" + (i+1) + "清理后：" + cleanCacheLine);
              if (cleanCacheLine === cleanCurrent && cleanCurrent !== "") {
                  isMatch = true;
                  break;
              }
          }

          if (isMatch) {
              matchedResult = {
                  name: targetItem.name,
                  gender: targetItem.gender,
                  age: targetItem.age
              };
              finalMatchedIndex = targetArrayIndex + 1;
              //console.log("【匹配成功】命中向前序号" + finalMatchedIndex + "，角色：" + targetItem.name);
              break;
          }
      }
  }

  // ===================== 匹配优先级3：向后偏移（原逻辑不变，仅改匹配规则）=====================
  if (!matchedResult) {
      for (var offset = 1; offset <= MAX_BACKWARD_OFFSET; offset++) {
          var targetArrayIndex = currentIndex - 1 + offset;
          if (targetArrayIndex >= dialogList.length) break;

          var targetItem = dialogList[targetArrayIndex];
          // 同样按换行拆分逐行匹配，和主逻辑完全一致
          var cacheLines = targetItem.dialogContent.split("\n").filter(function(line) {
              return line.trim() !== "";
          });
          var isMatch = false;
          for (var i = 0; i < cacheLines.length; i++) {
              var cleanCacheLine = cleanDialogText(cacheLines[i]);
              //console.log("【向后匹配】尝试序号" + (targetArrayIndex + 1) + " | 当前清理后：" + cleanCurrent + " | 缓存拆分行" + (i+1) + "清理后：" + cleanCacheLine);
              if (cleanCacheLine === cleanCurrent && cleanCurrent !== "") {
                  isMatch = true;
                  break;
              }
          }

          if (isMatch) {
              matchedResult = {
                  name: targetItem.name,
                  gender: targetItem.gender,
                  age: targetItem.age
              };
              finalMatchedIndex = targetArrayIndex + 1;
              //console.log("【匹配成功】命中向后序号" + finalMatchedIndex + "，角色：" + targetItem.name);
              break;
          }
      }
  }

  // ===================== 匹配成功：更新进度指针（永久章节缓存）=====================
  if (matchedResult && finalMatchedIndex > 0) {
      var _dHitSeq = seqKeys[finalMatchedIndex - 1] || finalMatchedIndex;
      writeProgress(progress.bookName, chapterTitle, _dHitSeq);
      return matchedResult;
  }

  // ===================== 回退机制：进度浮动匹配失败，全文重新定位章节+序号后再查缓存（复刻其他APP脚本）=====================
  // 缺口修复：原代码到这里直接返回null触发AI，缺少"重新定位再查"这一步，会浪费AI额度
  if (!matchedResult) {
      var _reloc = locateParagraphInFullText(text2);
      if (_reloc && _reloc.seqList && _reloc.seqList.length > 0) {
          var _reChapter = _reloc.chapterTitle;
          var _reSeqList = _reloc.seqList;
          // 章节可能变了，重新读对应章节缓存
          var _reCache = (_reChapter === chapterTitle) ? cache : readChapterCache(_reChapter);
          var _reResults = _reCache.results || {};
          // 用重新定位算出的真实序号，±2范围内查缓存
          for (var _ri = 0; _ri < _reSeqList.length; _ri++) {
              var _rePredicted = _reSeqList[_ri];
              for (var _roff = -2; _roff <= 2; _roff++) {
                  var _trySeq = _rePredicted + _roff;
                  if (_trySeq < 1) continue;
                  var _tryItem = _reResults[String(_trySeq)];
                  if (!_tryItem) continue;
                  var _tryText = _tryItem.dialogText || _tryItem.dialogContent || "";
                  if (tryMatchTextWithNewlines(_tryText, cleanCurrent)) {
                      // 命中：更新进度指针到重新定位的章节和序号
                      writeProgress(progress.bookName, _reChapter, _trySeq);
                      return {
                          name: _tryItem.name,
                          gender: _tryItem.gender,
                          age: _tryItem.age
                      };
                  }
              }
          }
          // 重新定位后仍未命中：更新进度指针（让后续保存逻辑能复用这次定位结果，避免重复定位）
          writeProgress(progress.bookName, _reChapter, _reSeqList[_reSeqList.length - 1]);
      }
  }

  // ===================== 全部匹配失败：返回null（完全保留原逻辑，无任何改动）=====================
  //console.log("【匹配失败】双向容错匹配全部失败，触发重新分析");
  return null;
}




CharacterManager.prototype.analyzeCharacterFallback = function(fullText, characterId) {
  return { name: "未知", gender: Math.random() > 0.5 ? "男" : "女", age: Math.random() > 0.5 ? "青年" : "中年" };
};








CharacterManager.prototype.reEvaluateCharacter = function(record) {
  if (record.usageCount === 100 || record.usageCount === 50) return;
  if (record.gender === null || record.age === null) {
      if (record.genderAgeHistory && record.genderAgeHistory.length > 0) {
          for (var i = 0; i < record.genderAgeHistory.length; i++) {
              var entry = record.genderAgeHistory[i];
              if (entry && entry.gender !== null && entry.age !== null) {
                  record.gender = entry.gender;
                  record.age = entry.age;
                  break;
              }
          }
      }
      if (record.gender === null || record.age === null) {
          record.gender = "男";
          record.age = "青年";
      }
  }
  if (!record.genderAgeHistory || record.genderAgeHistory.length === 0) {
      record.genderAgeHistory = [{ gender: record.gender, age: record.age }];
      return;
  }
  var genderCount = {};
  var ageCount = {};
  for (var i = 0; i < record.genderAgeHistory.length; i++) {
      var entry = record.genderAgeHistory[i];
      if (!entry) continue;
      if (entry.gender === null || entry.age === null) continue;
      genderCount[entry.gender] = (genderCount[entry.gender] || 0) + 1;
      ageCount[entry.age] = (ageCount[entry.age] || 0) + 1;
  }
  if (Object.keys(genderCount).length === 0) genderCount[record.gender] = 1;
  if (Object.keys(ageCount).length === 0) ageCount[record.age] = 1;
  var mostCommonGender = "";
  var maxGenderCount = 0;
  for (var gender in genderCount) {
      if (genderCount.hasOwnProperty(gender)) {
          if (mostCommonGender === "") {
              mostCommonGender = gender;
              maxGenderCount = genderCount[gender];
          }
          if (genderCount[gender] > maxGenderCount) {
              mostCommonGender = gender;
              maxGenderCount = genderCount[gender];
          }
      }
  }
  var mostCommonAge = "";
  var maxAgeCount = 0;
  for (var age in ageCount) {
      if (ageCount.hasOwnProperty(age)) {
          if (mostCommonAge === "") {
              mostCommonAge = age;
              maxAgeCount = ageCount[age];
          }
          if (ageCount[age] > maxAgeCount) {
              mostCommonAge = age;
              maxAgeCount = ageCount[age];
          }
      }
  }
  var needReassign = false;
  if (record.gender !== mostCommonGender || record.age !== mostCommonAge || !record.voice) {
      needReassign = true;
  }
  var topRecords = [];
  for (var j = 0; j < record.genderAgeHistory.length; j++) {
      var entry = record.genderAgeHistory[j];
      if (!entry) continue;
      if (entry.gender === null || entry.age === null) continue;
      if (entry.gender === mostCommonGender && entry.age === mostCommonAge) {
          topRecords.push(entry);
          if (topRecords.length >= CONFIG.topHistoryRecords) break;
      }
  }
  record.gender = mostCommonGender;
  record.age = mostCommonAge;
  record.genderAgeHistory = topRecords;
  if (needReassign) {
      var newVoice = this.assignVoice(mostCommonGender, mostCommonAge);
      if (newVoice) record.voice = newVoice;
  }
  record.usageCount = CONFIG.resetUsageCount;
};

// 初始化CharacterManager
var characterManager = new CharacterManager();
characterManager.loadRecords();

// -------------------------- SpeechRuleJS核心对象（整合＜＞本地音效） --------------------------
var SpeechRuleJS = {
  name: "多角色朗读2.87【加速版+1修复2.2】",
  id: "mingwuyan",
  author: "命無言",
  version: 62,
  zdfp: 1,
  
  tags: (function() {
      var tags = {
          narration: "旁白",
          duihua: "对话",
          duihuaA: "男",
          duihuaB: "女",
          "括号2": "在线音效",
          "括号1": "【】括号发音人",
          "括号3": "「」括号发音人",
          "括号4": "『』括号发音人",
          localSound1: "本地音效1",
          localSound2: "本地音效2",
          localSound3: "本地音效3"
      };
      
              // 加入GENSHIN_CHARACTERS发音人标签
      for (var name in GENSHIN_CHARACTERS) {
          if (GENSHIN_CHARACTERS.hasOwnProperty(name)) {
              var info = GENSHIN_CHARACTERS[name];
              tags[info.voice.toString()] = name.toString(); // 规避：属性转原始String
          }
      }
      
      
      // 新增：循环添加localSound4~localSound100（与前3个一致）
      for (var i = 4; i <= 990; i++) {
          var tagKey = ("localSound" + i).toString(); // 规避：tagKey转原始String
          var tagName = ("本地音效" + i).toString(); // 规避：tagName转原始String
          tags[tagKey] = tagName;
      }
      return tags;
  })(),


  tagsData: (function() {
      var 统一Hint = "\n       “轰隆”  “轰隆！” “轰隆。。”\n         输入 轰隆  就可匹配，\n       支持用|分隔多个拟声词，@/＜/＞开头为正则（＜前插/＞后插/@替换）";
      
      var tagsData = {
          dialogue: {
              role: {
                  label: "匹配角色名",
                  hint: "可用|分隔多个角色关键词"
              }
          },
          // 对话标签：完全模仿原代码格式（无多余逗号、字段名简化）
          duihua: {
              role: { // 字段名用 role（和 dialogue 标签一致，避免冲突）
                  label: "角色名",
                  hint: "输入角色关键词（如“张三”“主角”）"
              },
                // 整合性别+年龄为单选择框，格式：男/青年
              genderAge: {
                  label: "性别/年龄",
                  hint: "选择角色的性别和年龄阶段",
                  items: '{男/少年: "男/少年",男/男青年: "男/男青年",男/男中年: "男/男中年",男/男老年: "男/男老年",男/男孩: "男/男孩",女/女童: "女/女童",女/少女: "女/少女",女/女青年: "女/女青年",女/女中年: "女/女中年",女/女老年: "女/女老年",男/主角: "男/主角",女/主角: "女/主角"}',
                  default: '男/青年'
               },
               // 整合性别+年龄为单选择框，格式：男/青年
              personality: {
                  label: "角色性格", // 独立标签名
                  hint: "选择角色的性格特质（独立配置，不影响其他选项）", // 独立提示语
          //        items: personalityItemsConfig, 
          //        default: moren // 独立默认值
               }

          },
          // 本地音效1~3：完全保留原代码（一字未改）
          localSound1: {
              audioName: {
                  label: "音频名称（本地音效1）",
                  hint: 统一Hint
              }
          },
          localSound2: {
              audioName: {
                  label: "音频名称（本地音效2）",
                  hint: 统一Hint
              }
          },
          localSound3: {
              audioName: {
                  label: "音频名称（本地音效3）",
                  hint: 统一Hint
              }
          }
      };

      // 循环添加localSound4~localSound990：完全保留原代码
      for (var i = 4; i <= 990; i++) {
          var tagKey = ("localSound" + i).toString();
          var label = ("音频名称（本地音效" + i + "）").toString();
          tagsData[tagKey] = {
              audioName: {
                  label: label,
                  hint: 统一Hint
              }
          };
      }
      
      // 新增：为 GENSHIN_CHARACTERS 所有标签添加【独立性格选择框】（无冲突+无未定义错误）
      for (var name in GENSHIN_CHARACTERS) {
          if (GENSHIN_CHARACTERS.hasOwnProperty(name)) {
              var voiceTag = GENSHIN_CHARACTERS[name].voice.toString();
              // 直接内嵌性格配置（无需外部变量，彻底避免ReferenceError）
              var personalityConfig = {
                  label: "角色性格", // 独立标签名
                  hint: "选择角色的性格特质（独立配置，不影响其他选项）", // 独立提示语
          //        items: personalityItemsConfig, 
         //         default: moren // 独立默认值
              };
              
              // 1. 若标签已存在（如括号1、男主1），在原有配置上新增性格选项
              if (tagsData[voiceTag]) {
                  tagsData[voiceTag].personality = personalityConfig; // 字段名：personality（与genderAge无冲突）
              } 
              // 2. 若标签不存在，新建配置（仅含性格选择框）
              else {
                  tagsData[voiceTag] = {
                      personality: personalityConfig
                  };
              }
          }
      }
      
      return tagsData;
      
  })(),


  getTagName: function(tag, tagData) {
      // 工具函数：数组扁平化（移到内部，避免作用域问题，括号完全匹配）
      var forceFlattenArray = function(arr) {
          var result = [];
          for (var i = 0; i < arr.length; i++) {
              var item = arr[i];
              if (Object.prototype.toString.call(item) === '[object Array]') {
                  result = result.concat(forceFlattenArray(item));
              } else {
                  result.push(item);
              }
          }
          return result;
      };
  
      // 1. GENSHIN标签处理（括号完全匹配）
      var genshinTagKey = "";
      if (GENSHIN_CHARACTERS) {
          for (var tagKey in GENSHIN_CHARACTERS) {
              if (Object.prototype.hasOwnProperty.call(GENSHIN_CHARACTERS, tagKey)) {
                  var genshinConfig = GENSHIN_CHARACTERS[tagKey];
                  if (genshinConfig.voice === tag) {
                      genshinTagKey = tagKey;
                      break;
                  }
              }
          }
      }
  
      if (genshinTagKey !== "") {
          var basePart = genshinTagKey;
          var genshinPersonality = "";
          if (tagData && tagData.personality) {
              if (Object.prototype.toString.call(tagData.personality) === '[object Array]') {
                  var flatGenshinP = forceFlattenArray(tagData.personality);
                  for (var g = 0; g < flatGenshinP.length; g++) {
                      var pItem = flatGenshinP[g];
                      genshinPersonality = typeof pItem === 'object' && pItem !== null 
                          ? (pItem.value || "").trim() 
                          : (pItem + "").trim();
                      if (genshinPersonality) {
                          break;
                      }
                  }
              } else {
                  genshinPersonality = (tagData.personality + "").trim();
              }
          }
          var personality = genshinPersonality !== "" && genshinPersonality !== "无" ? genshinPersonality : "";
          var personalityWhole = personality ? ("" + personality) : "";
  
          var rsTag = basePart + personalityWhole;
          //console.log("GENSHIN生效！tag=", tag, "性格=", genshinPersonality, "生成tagName=", rsTag);
          return rsTag;
      }
  
      // 2. duihua标签处理（括号完全匹配，复用GENSHIN逻辑）
      else if ("duihua" == tag) {
          // 角色名部分（括号不变）
          var roleContent = tagData && tagData.role && tagData.role.trim() !== "" 
              ? tagData.role.trim() 
              : "";
          var rolePrefix = "";
          var roleSuffix = "";
          var rolePart = roleContent.length > 15 
              ? (rolePrefix + roleContent.substring(0, 15) + ".." + roleSuffix) 
              : (rolePrefix + roleContent + roleSuffix);
  
          // 性别年龄部分（括号不变）
          var genderAgeContent = tagData && tagData.genderAge ? tagData.genderAge : "";
          var genderAgePrefix = "（";
          var genderAgeSuffix = "）";
          var genderAgeWhole = genderAgeContent ? (genderAgePrefix + genderAgeContent + genderAgeSuffix) : "";
  
          // 性格部分（括号完全匹配）
          var duihuaPersonality = "";
          if (tagData && tagData.personality) {
              if (Object.prototype.toString.call(tagData.personality) === '[object Array]') {
                  var flatDuihuaP = forceFlattenArray(tagData.personality);
                  for (var d = 0; d < flatDuihuaP.length; d++) {
                      var pItem = flatDuihuaP[d];
                      duihuaPersonality = typeof pItem === 'object' && pItem !== null 
                          ? (pItem.value || "").trim() 
                          : (pItem + "").trim();
                      if (duihuaPersonality) {
                          break;
                      }
                  }
              } else {
                  duihuaPersonality = (tagData.personality + "").trim();
              }
          }
          var personality = duihuaPersonality !== "" && duihuaPersonality !== "无" ? duihuaPersonality : "";
          var separator = "";
          var personalityPrefix = "|";
          var personalitySuffix = "";
          var personalityWhole = personality ? (separator + personalityPrefix + personality + personalitySuffix) : "";
  
          // 最终拼接（括号不变）
          var rsTag = rolePart + personalityWhole + genderAgeWhole;
  
          //console.log("duihua生效！性格=", duihuaPersonality, "生成tagName=", rsTag);
          return rsTag;
      }
  
      // 3. 其他标签（括号不变）
      else {
          return this.tags[tag] || "旁白";
      }
  }, // 结尾逗号保留（对象方法格式）
  
      
  characterManager: characterManager,
  LOCAL_REGEX_PREFIX: "@_local_", // 本地正则专属前缀（隔离在线）

  // -------------------------- 核心工具：仅替换「目标内容」中的符号（不碰外层标签/系统符号） --------------------------
  replaceTargetContentSymbols: function(targetStr) {
      return targetStr
          .replace(/“/g, "###LEFT_QUOTE###")
          .replace(/”/g, "###RIGHT_QUOTE###")
          .replace(/〖/g, "###LEFT_DOUBLE_ANGLE###")
          .replace(/〗/g, "###RIGHT_DOUBLE_ANGLE###")
          .replace(/【/g, "###LEFT_SQUARE###")
          .replace(/】/g, "###RIGHT_SQUARE###")
          .replace(/『/g, "###LEFT_DOUBLE_CURLY###")
          .replace(/』/g, "###RIGHT_DOUBLE_CURLY###")
          .replace(/「/g, "###LEFT_SINGLE_ANGLE###")
          .replace(/」/g, "###RIGHT_SINGLE_ANGLE###");
  },
  restoreTargetContentSymbols: function(text) {
      return text
          .replace(/###LEFT_QUOTE###/g, "“")
          .replace(/###RIGHT_QUOTE###/g, "”")
          .replace(/###LEFT_DOUBLE_ANGLE###/g, "〖")
          .replace(/###RIGHT_DOUBLE_ANGLE###/g, "〗")
          .replace(/###LEFT_SQUARE###/g, "【")
          .replace(/###RIGHT_SQUARE###/g, "】")
          .replace(/###LEFT_DOUBLE_CURLY###/g, "『")
          .replace(/###RIGHT_DOUBLE_CURLY###/g, "』")
          .replace(/###LEFT_SINGLE_ANGLE###/g, "「")
          .replace(/###RIGHT_SINGLE_ANGLE###/g, "」");
  },

  // -------------------------- 解析在线音效关键词（保留完整原始关键词，新增originFullKW；支持全角/半角＜＞） --------------------------
  
  
  
  
  
  parseSoundKeywords: function(yinXiaoList) {
      var regexKWs = [];    
      var normalKWs = [];   
      var specialKWs = [];  
      // 新增1：定义母关键词组（和你原有变量顺序一致，不打乱结构）
      var normalKWGroups = [];  
      var keywordReg = /^(\d{1,2})?(\D+?)(\d{1,2})?$/;
      var soundRegexSymbols = ['<', '>', '＜', '＞']; 
  
      for (var i = 0; i < yinXiaoList.length; i++) {
          var item = yinXiaoList[i];
          if (!item || !item.name) continue;
          var fullName = item.name.trim();
          // 新增2：记录当前项的母关键词（未分割的完整name）
          var motherKW = fullName;
  
          var firstChar = fullName.charAt(0);
          var isRegexSymbol = (soundRegexSymbols.indexOf(firstChar) !== -1);
          if (isRegexSymbol || (fullName.startsWith("@") && !fullName.startsWith(this.LOCAL_REGEX_PREFIX))) {
              try {
                  var regexStr = fullName.slice(1);
                  var regex = new RegExp(regexStr, 'g');
                  regexKWs.push({
                      regex: regex,
                      originKW: fullName,
                      flag: firstChar
                  });
              } catch (e) {
                  // 保留你原有空catch，不改动
              }
              continue;
          }
  
          // 保留你原有代码的普通关键词拆分逻辑（一行都不删）
          var names = fullName.split("|");
          // 新增3：临时存当前母关键词的子关键词（避免打乱原有循环）
          var currentChildren = [];
          for (var j = 0; j < names.length; j++) {
              var subName = names[j].trim();
              if (!subName) continue;
              // 新增4：存入当前母关键词的子关键词列表
              currentChildren.push(subName);
              // 以下是你原有代码，完全保留
              var match = subName.match(keywordReg);
              if (match) {
                  var prefixNum = match[1] ? parseInt(match[1], 10) : 0;
                  var coreKW = match[2].trim();
                  var suffixNum = match[3] ? parseInt(match[3], 10) : 0;
                  if ((prefixNum >=1 && prefixNum <=9) || (suffixNum >=1 && suffixNum <=9)) {
                      specialKWs.push({
                          prefixLen: prefixNum,
                          coreKW: coreKW,
                          suffixLen: suffixNum,
                          originFullKW: subName,
                          originKW: subName
                      });
                  } else {
                      normalKWs.push(subName);
                  }
              } else {
                  normalKWs.push(subName);
              }
          }
          // 新增5：将当前母关键词+子关键词列表存入组（仅当有子关键词时）
          if (currentChildren.length > 0) {
              normalKWGroups.push({
                  motherKW: motherKW,
                  children: currentChildren
              });
          }
      }
      
      // 新增6：返回对象中加入normalKWGroups（无尾逗号，和你原有格式一致）
      return { regexKWs: regexKWs, normalKWs: normalKWs, specialKWs: specialKWs, normalKWGroups: normalKWGroups };
  }, // 关键：保留对象属性的逗号（分隔后面的函数）
  
  handleText: function(text, tagsData) {
  
  
       // 新增：ES5 兼容的数组扁平化函数（解决 forceFlattenArray 未定义问题）
      var forceFlattenArray = function(arr) {
          var result = [];
          for (var i = 0; i < arr.length; i++) {
              var item = arr[i];
              // 判断是否为数组（ES5 兼容写法）
              if (Object.prototype.toString.call(item) === '[object Array]') {
                  // 递归扁平化嵌套数组
                  result = result.concat(forceFlattenArray(item));
              } else {
                  result.push(item);
              }
          }
          return result;
      };

      // 新增：判断数组的辅助函数（适配原有代码）
      var isArray = function(arr) {
          return Object.prototype.toString.call(arr) === '[object Array]';
      };

      // ========== 动态扩展角色表（方案2）==========
      // 问题：BATCH_ROLES 硬编码数量(如女青年100)，但用户配置项可能有女青年101~400，
      //       导致 detectAvailableVoices 遍历 GENSHIN_CHARACTERS 时漏掉这些 tag。
      // 方案：扫描 tagsData 的 key（即配置项 tag），对形如「前缀+序号」的 tag，
      //       若序号超过 GENSHIN_CHARACTERS 现有最大序号，按需补齐 GENSHIN_CHARACTERS 和 tags。
      // 依据：BATCH_ROLES 配置提供合法前缀及性别/年龄，仅扩展已知前缀，避免误伤自定义 tag。
      try {
          // 1. 统计每个 BATCH_ROLES 前缀在 GENSHIN_CHARACTERS 中的现有最大序号
          var batchPrefixMaxSeq = {}; // 前缀 -> 现有最大序号
          var batchPrefixInfo = {};   // 前缀 -> {gender, age, voicePre}
          for (var bi = 0; bi < BATCH_ROLES.length; bi++) {
              var bItem = BATCH_ROLES[bi];
              var bType = bItem[0], bGender = bItem[1], bAge = bItem[2], bVoicePre = bItem[3];
              // 前缀取 bType 的最后一段（如"女/女青年"->"女青年"），与 voice 前缀一致
              var bPrefix = bType.substring(bType.lastIndexOf('/') + 1);
              batchPrefixInfo[bPrefix] = { gender: bGender, age: bAge, voicePre: bVoicePre };
              batchPrefixMaxSeq[bPrefix] = 0;
          }
          for (var gName in GENSHIN_CHARACTERS) {
              if (GENSHIN_CHARACTERS.hasOwnProperty(gName)) {
                  var gVoice = GENSHIN_CHARACTERS[gName].voice.toString();
                  // voice 形如 "女青年01"，匹配前缀+序号
                  var gMatch = gVoice.match(/^([\u4E00-\u9FA5]+?)(\d+)$/);
                  if (gMatch && batchPrefixMaxSeq.hasOwnProperty(gMatch[1])) {
                      var gSeq = parseInt(gMatch[2], 10);
                      if (gSeq > batchPrefixMaxSeq[gMatch[1]]) {
                          batchPrefixMaxSeq[gMatch[1]] = gSeq;
                      }
                  }
              }
          }
          // 2. 扫描 tagsData 的 key，找出每个前缀需要扩展到的最大序号
          var neededMaxSeq = {}; // 前缀 -> 需要的最大序号
          for (var tdKey in tagsData) {
              if (tagsData.hasOwnProperty(tdKey)) {
                  var tdMatch = tdKey.match(/^([\u4E00-\u9FA5]+?)(\d+)$/);
                  if (tdMatch && batchPrefixInfo.hasOwnProperty(tdMatch[1])) {
                      var tdSeq = parseInt(tdMatch[2], 10);
                      var tdPrefix = tdMatch[1];
                      if (!neededMaxSeq[tdPrefix] || tdSeq > neededMaxSeq[tdPrefix]) {
                          neededMaxSeq[tdPrefix] = tdSeq;
                      }
                  }
              }
          }
          // 3. 按需补齐 GENSHIN_CHARACTERS 和 SpeechRuleJS.tags
          for (var extPrefix in neededMaxSeq) {
              if (neededMaxSeq.hasOwnProperty(extPrefix)) {
                  var extInfo = batchPrefixInfo[extPrefix];
                  var curMax = batchPrefixMaxSeq[extPrefix];
                  var needMax = neededMaxSeq[extPrefix];
                  if (needMax > curMax) {
                      for (var ni = curMax + 1; ni <= needMax; ni++) {
                          var nSeq = padZero(ni, 2);
                          var nName = '【' + extPrefix + nSeq + '】';
                          var nVoice = extInfo.voicePre + nSeq;
                          GENSHIN_CHARACTERS[nName] = { gender: extInfo.gender, age: extInfo.age, voice: nVoice };
                          SpeechRuleJS.tags[nVoice] = nName;
                      }
                      // 更新现有最大序号，避免同一次 handleText 内重复扩展
                      batchPrefixMaxSeq[extPrefix] = needMax;
                  }
              }
          }
      } catch (extErr) {
          console.error("动态扩展角色表异常: " + extErr.message);
      }
      // ========== 动态扩展结束 ==========

      text2 = text.replace(/[(]([\u4E00-\u9Fa5]{1,5})音效[)]/g, "");
      // ========== 组1：音效前置2 ==========

// 1. 时间转场(时间等待)
text = text.replace(/^(?!\()(?!.*?(?:古语|古话|俗话|老话|有道是|常言道|乐园|一时间|轮回眼|轮回乐园))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|只需|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|除非|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|必定|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来|需要|付出|耗费))[^“”「」？.?!])*(?:(?:(经过|熬过|度过|时间|流逝|等待|僵持|消逝|日子|岁月|枯坐|苦守)[^？?]{0,20}?(轮回|煎熬|漫长的|无尽的|流逝着|正在流逝|快速流逝|一分一秒|一秒秒|不知不觉|度日如年|日复一日|季节交替|寒来暑往|春去秋来|秋去冬来))|(?:(轮回|煎熬|漫长的|无尽的|正在流逝|快速流逝|一分一秒|一秒秒|不知不觉|度日如年|日复一日|季节交替|寒来暑往|春去秋来|秋去冬来)[^？?]{0,20}?(经过|熬过|度过|时间|流逝|等待|僵持|消逝|日子|岁月|枯坐|苦守))|(?:(等了|等待|等候|等到|静候|坐等)[^？?]{0,20}?(有|大约|大概|接近|差不多)[^？?]{0,20}?(分钟|十秒|小时|盏茶|炷香|刻钟)))(?!.*(?:停滞|冻结|停止))/gm, '(时间流逝音效)$&');

// 2. 时间转场(时光回溯)
text = text.replace(/^(?!\()(?!.*时间转场)(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:想|记忆|回忆|思忆|追忆|祈祷|愿望|幻觉|梦中|梦里|思考|思索|思绪|脑海|脑中|脑里|心中|心念|心思|心里|心头|心愿|胸中|曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|那次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|只需|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|除非|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|必定|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来|可以|岁月静好))[^“”「」？.?!])*(?:(?:(?:时间|时光|岁月|光阴|光影|时刻|年代|纪元|世界|宇宙|历史|过往|事发|此刻)[^？.?!]{0,15}?(?:追溯|回溯|回推|回退|退回|回到|回转|飘回|调回|闪回|切回|切换|拨回|流转|逆转|倒转|调转|倒流|倒带|倒退|重返|再现|重现)[^？.?!]{0,10}?(?:年[以之前]前|(?:[0-9]+(?:年|个月|月|天|日|小时|分钟|秒|刻钟))(?:[以之前]前|前)(?![后之后以])|天[以之前]前|之前|以前|从前|刚刚|刚才|过去|先前|当初|当日|当时|当天|当年))|(?:(?:追溯|回溯|回推|回退|退回|回到|回转|飘回|调回|闪回|切回|切换|拨回|流转|逆转|倒转|调转|倒流|倒带|倒退|倒放|重返|再现|重现)[^？.?!]{0,15}?(?:时间|时光|岁月|光阴|光影|时刻|年代|纪元|世界|宇宙|历史|过往|事发|此刻)[^？.?!]{0,10}?(?:年[以之前]前|(?:[0-9]+(?:年|个月|月|天|日|小时|分钟|秒|刻钟))(?:[以之前]前|前)(?![后之后以])|天[以之前]前|之前|以前|从前|刚刚|刚才|过去|先前|当初|当日|当时|当天|当年))|(?:(?:追溯|回溯|回推|回退|退回|回到|回转|飘回|调回|闪回|切回|切换|拨回|流转|逆转|倒转|调转|倒流|倒带|倒退|倒放|重返|再现|重现)[^？.?!]{0,10}?(?:时间|时光|岁月|光阴|光影|时刻|年代|纪元|世界|宇宙|历史|过往|事发|当时|此刻|当下))|(?:(时间|时光|岁月|光阴|光影|时刻|年代|纪元|世界|宇宙|历史|过往|事发|当时|此刻|当下)[^？?]{0,20}?(当即|立即|随之|随即|顷刻|骤然|蓦然)[^？?]{0,20}?(回退|退回|回到|闪回|切回|切换|拨回|流转|逆转|逆流|倒转|调转|倒流|倒带|倒退|倒放|重返|再现|重现))|(?:(施展|抬手|挥手|施法|运转)[^？?]{0,20}?(光阴|光阴长河|光阴河流|光阴支流|时空之力|时间法则)[^？?]{0,20}?(倒流|倒转|逆流|重返|恢复原貌)))(?!.*(?:的效果|的能力|的作用|的次数|的后果|会造成|会导|打断|中断|忘记|停止|结束|未来|将来|之后|以后|过后|稍后|分钟后|小时后|天后|月后|年后))/gm, '(时间回溯音效)$&');

// 3. 心理转场(杀机涌现)
text = text.replace(/^(?!.*?(?:古语|古话|俗话|老话|有道是|常言道|能感|能察|能捕|利器|自起|四伏的|大盛))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:祈祷|幻觉|梦中|梦里|思考|思索|思绪|脑海|脑中|脑里|胸中|曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|只需|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|除非|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|必定|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来))[^“”「」？.?!])*(?:(?:(杀机|杀心|杀意|杀念|阴芒|寒芒|精芒|厉芒|寒光)[^？?]{0,20}?(刹|骤|霎|顿时|突然|充满|充斥|泛起|涌现|涌出|涌动|涌现|汹涌|捕捉|觉察|察觉|感知|眼眸|眸子|眼中|周围|四伏|四面八方))|(?:(刹|骤|霎|顿时|突然|充满|充斥|泛起|涌现|涌出|涌动|涌现|汹涌|捕捉|觉察|察觉|感知|眼眸|眸子|眼中|周围|四伏|四面八方)[^？?]{0,20}?(杀机|杀心|杀意|杀念|阴芒|寒芒|精芒|厉芒|寒光)))(?!.*(?:掩藏(?!好|住|下|了)|掩饰(?!好|住|下|了)|压制(?!好|住|下|了)))/gm, '(杀机涌现音效)$&');

// 4. 地点转场(地点变化)  原正则有后行断言，改为直接匹配
text = text.replace(/^(\s*(?:[^\n。！？]{0,8}?)(?:来到|回到|抵达|行至|踏入|走进|穿过|赶到|赶往|奔赴|降临|重返|途经|到达)\s*[^\n。！？]*?(?:[城镇乡村庄店省市县区街巷道路府邸宫殿寺观庵庙馆校山岳峰岭河湖海洲林原漠谷岛场院亭台楼阁轩榭江溪潭渊岩崖坡丘洞窟穴]|[^\n。！？\s]{2,5}[省市县区镇乡村庄])(?![^\n。！？]{0,5}(?:之前|之际|前夕|途中|路过|经过|途经|那天|那一刻|的话|的可能|的概率|的几率|的计划|的打算|的时间|的行程|的路线))(?=[^\n。！？]*[。！？]?))/gm, '(地点转场音效)$1');

// 5. 心理转场(短促惊骇)
text = text.replace(/^(?!\()(?!.*?(?:古语|古话|俗话|老话|有道是|常言道|失焦|焦距))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|只需|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|除非|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|必定|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来))[^“”「」？.?!])*(?:(?:(感|觉|顿|瞬|霎|刹|骤|顷|听|看|瞥|立即|此刻|突然|瞳孔)[^？?]{0,20}?(剧震|汗毛倒|汗毛直|毫毛直|毫毛倒|寒毛直|寒毛倒|脊背生|毛骨悚|不寒而|背后汗毛|背上汗毛|背上寒毛|背后寒毛|背后汗毛|浑身一颤|头皮发|头皮麻|后背发|如坠冰|惊出一|悚然一|冷汗涔涔|头皮就酥|脖子发凉|脖子一凉|全身发冷|心里发毛|魂飞魄散|战栗|手脚冰凉|手足冰凉|心神紧绷|心弦紧绷|猛地一缩|瞬时放大|瞬间放大|肝胆欲裂|目眦欲裂|悚然失色))|(?:(剧震|汗毛倒|汗毛直|毫毛直|毫毛倒|寒毛直|寒毛倒|脊背生|毛骨悚|不寒而|背后汗毛|背上汗毛|背上寒毛|背后寒毛|背后汗毛|浑身一颤|头皮发|头皮麻|后背发|如坠冰|惊出一|悚然一|冷汗涔涔|头皮就酥|脖子发凉|脖子一凉|全身发冷|心里发毛|魂飞魄散|战栗|手脚冰凉|手足冰凉|猛地一缩|瞬时放大|瞬间放大|肝胆欲裂|目眦欲裂|悚然失色)[^？?]{0,20}?(感|觉|顿|瞬|霎|刹|骤|顷|听|看|瞥|此刻|立即|突然|瞳孔))|(?:(瞳孔|瞳仁|瞳眸)[^？?]{0,20}?(紧缩|猛缩|一缩|成针尖|剧震)))(?!.*(?:消失|散去|脱离))/gm, '(心理惊悚音效)$&');

// 6. 心理转场(猜疑警觉)
text = text.replace(/^(?!.*?(?:古语|古话|俗话|老话|有道是|常言道|不安好|不安全|不安静))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来|压下))[^“”「」？.?!])*(?:(?:(察觉|觉察|发觉|直觉|感到|感知|猜测|意识|怀疑|猜疑|有种|泛起|浮现|殊不知|并不知|一无所知|浑然不觉|浑然不知)[^？?]{0,20}?(被|或许|可能|大概|也许|似乎|有人|存在|他对|对他|她对|对她|对自|暗处|隐藏|隐匿|隐约|身份|伪装|计划|计谋|谋划|谋算|若有若无)[^？?]{0,20}?(跟踪|行踪|追踪|盯梢|尾随|识破|看穿|看破|看透|窥探|窥视|怀疑|调查|暴露|有防范|了防范))|(?:(被|或许|可能|大概|也许|似乎|有人|存在|暗处|隐藏|隐匿|隐约|身份|伪装|计划|计谋|谋划|谋算|若有若无)[^？?]{0,20}?(跟踪|行踪|追踪|盯梢|尾随|识破|看穿|看透|看破|窥探|窥视|怀疑|调查|暴露|有防范|了防范)[^？?]{0,20}?(察觉|觉察|发觉|感到|感知|猜测|意识|怀疑|猜疑|有种|泛起|浮现|殊不知|并不知|一无所知|浑然不觉|浑然不知))|(?:(心理|心里|心中|心内|心头)[^？?]{0,20}?(有种|有些|一种|某种|感觉|感到|隐约|隐隐|莫名|泛起|浮现|滋生|生出|忽然|忽地|顿时|没来由|不由得)[^？?]{0,20}?(不安|窥探|窥视|咯噔))|(?:(不安|窥探|窥视|咯噔)[^？?]{0,20}?(有种|有些|一种|某种|感觉|感到|隐约|隐隐|泛起|浮现|滋生|生出|忽然|忽地|顿时|没来由|不由得)[^？?]{0,20}?(心理|心里|心中|心内|心头)))(?!.*(?:不说破|停止|的话))/gm, '(猜疑警觉音效)$&');

// 7. 心理转场(心神大震)
text = text.replace(/^(?!\()(?!.*?(?:古语|古话|俗话|老话|有道是|常言道|胃|肚|但我|谎))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|只需|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|除非|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|必定|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来))[^“”「」？.?!])*(?:(?:(内心|心神|心中|心念|心里|心头|心情|心湖|瞳孔|浑身|道心|念头|念及|想到|闻言|只觉|顿觉|如同)[^？?]{0,20}?(棒喝|战栗|激荡|摇荡|一震|大震|大惊|巨震|剧震|剧颤|震撼|五雷轰|细思极恐|极不平静|惊涛骇浪|翻江倒海|晴天霹雳|怦然而动|砰然而动))|(?:(棒喝|战栗|激荡|摇荡|一震|大震|巨震|剧震|剧颤|震撼|五雷轰|细思极恐|极不平静|惊涛骇浪|翻江倒海|晴天霹雳)[^？?]{0,20}?(内心|心神|心中|心念|心里|心头|心情|心湖|瞳孔|道心|念头|念及|想到|只觉|顿觉|如同|呆愣|愣住|愣在))|(?:(头脑|脑子)[^？?]{0,4}?(轰的|轰地)))(?!.*(?:飞出|炸碎|碎裂|平息|撞在|砸在|止住))/gm, '(心神大震音效)$&');

// 8. 心理转场(心理落差)
text = text.replace(/^(?!.*?(?:古语|古话|俗话|老话|有道是|常言道|踢皮球))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|要是|要么|只要|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来))[^“”「」？.?!])*(?:(?:(刹|霎|转瞬|骤然|瞬间|顿时|突然|倏忽|不由|油然|忽然|感觉|感到|感受|心中|心理|心里|心头|心气|斗志|大喜|兴奋|激动|高兴|瘫坐|瘫倒|浑身|全身|心生|产生|生出|出现|一种|有种|有了种|没来由|整个人)[^？?]{0,20}?(变得|幻想|幻梦|美梦|梦想|极大|极度|极其|巨大|非常|转向|皮球|过山车|的力气|有力气|身气力|身骨头|脊梁骨|世界给|被世界|转直下)[^？?]{0,20}?(破灭|幻灭|消极|消沉|颓靡|蔫了|低迷|失落感|落差|挫败|颓废|颓败|颓然|抛弃|悔恨|后悔|瘫软|失去了|被抽走|被抽出|被抽掉|情低沉|绪低沉|绪低落|情低落|理落差|情落差|绪落差|头丧气|心丧气|无精打采|泄了气))|(?:(变得|幻想|幻梦|美梦|梦想|极大|极度|极其|非常|巨大|转向|皮球|过山车|的力气|有力气|身气力|身骨头|脊梁骨|世界给|被世界|转直下)[^？?]{0,20}?(破灭|幻灭|消极|消沉|颓靡|蔫了|低迷|失落感|落差|挫败|颓废|颓败|颓然|抛弃|悔恨|后悔|瘫软|失去了|被抽出|被抽走|被抽出|被抽掉|情低沉|绪低沉|绪低落|情低落|理落差|情落差|绪落差|头丧气|心丧气|泄了气)[^？?]{0,20}?(刹|霎|转瞬|瞬间|顿时|骤然|突然|倏忽|不由|油然|忽然|感觉|感到|感受|心中|心理|心里|心头|心气|斗志|大喜|兴奋|激动|高兴|瘫坐|瘫倒|浑身|全身|心生|产生|生出|出现|一种|有种|有了种|没来由|整个人))|(?:(刹|霎|转瞬|骤然|瞬间|顿时|突然|倏忽|不由|油然|忽然|感觉|感到|感受|心中|心理|心里|心头|心气|斗志|意志|大喜|兴奋|激动|高兴|瘫倒|瘫坐|浑身|全身|心生|产生|生出|出现|一种|有种|有了种|没来由|整个人)[^？?]{0,20}?(破灭|幻灭|消极|消沉|颓靡|蔫了|低迷|失落感|落差|挫败|颓废|颓败|颓然|抛弃|悔恨|后悔|瘫软|失去了|被抽走|被抽掉|被抽出|情低沉|绪低沉|绪低落|情低落|理落差|情落差|绪落差|头丧气|心丧气|泄了气的)[^？?]{0,20}?(变得|幻想|幻梦|美梦|梦想|极大|极度|极其|非常|巨大|转向|皮球|过山车|的力气|有力气|身气力|身骨头|脊梁骨|世界给|被世界|转直下)))(?!.*(?:的时候|的话|并未|并没|未让|振作|昂扬|停止|停下|止住))/gm, '(心理落差音效)$&');

// 9. 心理转场(拼凑真相)
text = text.replace(/^(?!.*?(?:古语|古话|俗话|老话|有道是|常言道))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|无论|不论|虽然|不管|尽管|就算|哪怕|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|说不定|指不定|不准|准备|打算|盘算|计划|企图|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|效果|作用|用作|用来))[^“”「」？.?!])*(?:(?:(大脑|头脑|脑子|脑海|脑中|脑内|思考|思绪|思索|心里|心中|心内|心头)[^？?]{0,20}?(交织|拼凑|拼建|还原|复原|浮现|复现|糅合|归拢|串起|串联|挖掘|放电影|电影般|如电影|幻灯片|速涌现|不断闪|不停闪|抽丝剥茧)[^？?]{0,20}?(掠影|真相|碎片|零星|记忆|事情|观点|要素|线索|迹象|一幕幕|一幕又|光怪陆离))|(?:(大脑|头脑|脑子|脑海|脑中|脑内|思考|思绪|思索|心里|心中|心内|心头)[^？?]{0,20}?(真相|碎片|零星|记忆|事情|观点|要素|线索|迹象|一幕幕|一幕又|光怪陆离)[^？?]{0,20}?(交织|拼凑|拼建|还原|复原|浮现|复现|糅合|归拢|串起|串联|挖掘|放电影|电影般|如电影|幻灯片|速涌现|不断闪|不停闪|抽丝剥茧))|(?:(真相|碎片|零星|记忆|事情|观点|要素|线索|迹象|一幕幕|一幕又|光怪陆离)[^？?]{0,20}?(大脑|头脑|脑子|脑海|脑中|脑内|思考|思绪|思索|心里|心中|心内|心头)[^？?]{0,20}?(交织|拼凑|拼建|还原|复原|浮现|复现|糅合|归拢|串起|串联|挖掘|放电影|电影般|如电影|幻灯片|速涌现|不断闪|不停闪|抽丝剥茧))|(?:(真相|碎片|零星|记忆|事情|观点|要素|线索|迹象|一幕幕|一幕又|光怪陆离)[^？?]{0,20}?(交织|拼凑|拼建|还原|复原|浮现|复现|糅合|归拢|串起|串联|挖掘|放电影|电影般|如电影|幻灯片|速涌现|不断闪|不停闪|抽丝剥茧)[^？?]{0,20}?(大脑|头脑|脑子|脑海|脑中|脑内|思考|心里|心中|心内|心头))|(?:(开始|控制|制住|拘押|连忙|迅速|对)[^？?]{0,20}?(进行|施法|施展|展开|运转|动手)[^？?]{0,20}?(搜魂|篡改记忆|夺取记忆)))(?!.*(?:停止|停下|止住))/gm, '(思考转场音效)$&');

// 10. 心理转场(心神回归)
text = text.replace(/^(?!\()(?!.*?(?:古语|古话|俗话|老话|有道是|常言道|壁垒|意识到|心神恍惚|心神不宁))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|只需|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|除非|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|必定|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来))[^“”「」？.?!])*(?:(?:(破开|退出|回到|来到|穿透|脱离|进入|抽回|收回|收归|回归|返回|收入|放入|投入|沉入|沉浸|遁入)[^？?]{0,20}?(冥想|识海|窍穴|元神|心神|神念|意识|元海|气府|空窍|紫府|打坐|入定|现实|幻境|迷雾|阵法|迷阵|虚幻|空间|次元|界壁|虚拟))|(?:(冥想|识海|窍穴|元神|心神|意识|元海|气府|空窍|紫府|打坐|入定|现实|幻境|迷雾|阵法|迷阵|虚幻|空间|次元|界壁|虚拟)[^？?]{0,20}?(破开|退出|回到|来到|穿透|脱离|进入|抽回|收回|收归|回归|返回|收入|放入|投入|沉入|沉浸|遁入)))(?!.*(?:学习|读书))/gm, '(心神回归音效)$&');

// 11. 心理转场(渐入梦境)
text = text.replace(/^(?!\()(?!.*?(?:古语|古话|俗话|老话|有道是|常言道|昨|醒|白日梦|梦想|梦幻|梦寐|梦鬼|梦中情人))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来|感觉|一切|醒))[^“”「」？.?!])*(?:(?:(做了|沉入|陷入)[^？?]{0,20}?(很长|一个)[^？?]{0,20}?(的梦|梦见))|(?:(迅速|直接|开始|正在|继续|被迫|瞬间|顿时|陡然|霎时|随即|立即|当即|立刻|下一刻)[^？?]{0,20}?(陷入|陷落|拉入|拉进|扯入|扯进|沉入)[^？?]{0,20}?(梦境|梦魇|梦中场景))|(?:(躺|睡着|意识|大脑|心神|逐渐|渐渐|当下)[^？?]{0,20}?(着梦|的梦|梦乡)[^？?]{0,20}?(梦里|梦中|梦见))|(?:(将死|垂死|临死|恍惚|朦胧中|恍恍惚惚|油尽灯枯|回光返照)[^？?]{0,20}?(仿佛|好似|就像|如同)[^？?]{0,20}?(看到|梦见|梦回|看见|回到|穿越)[^？?]{0,20}?(身影|人影|倩影|过去|少年|青年|中年|年轻)))(?!.*(?:就好了|实现|停止|停下|止住))/gm, '(渐入梦境音效)$&');

// 12. 心理转场(面露囧色)
text = text.replace(/^(?!\()(?!.*?(?:古语|古话|俗话|老话|有道是|常言道|大定|泰然|俱到))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|只需|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|除非|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|必定|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来))[^“”「」？.?!])*(?:(?:(一脸|一时|面色|面面|脸色|脸上|面带|面露|面现|心里|心中|心内|心下)[^？?]{0,20}?(一囧|大囧|窘迫|尴尬|迥然|囧然|一苦|一尴|一黑|黑线|苦涩|悻悻|暴汗|凝噎|尬住|语噎|语塞|相觑|茫然))|(?:(一囧|大囧|窘迫|尴尬|迥然|囧然|一苦|一尴|一黑|黑线|苦涩|悻悻|暴汗|凝噎|尬住|语噎|语塞|相觑|茫然)[^？?]{0,20}?(一脸|一时|面色|面面|脸色|脸上|面带|面露|面现|心内|心下|心里|心中)))(?!.*(?:收拾|不再))/gm, '(尴尬情绪音效)$&');

// 13. 回忆转场(脑海浮现)
text = text.replace(/^(?!\()(?!.*?(?:古语|古话|俗话|老话|有道是|常言道))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|只需|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|除非|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|必定|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来))[^“”「」？.?!])*(?:(?:(回想起|回忆起|记忆中|忆当中|联想到|浮现)[^？?]{0,10}?(出|上|小时候|当年|童年|年轻时|少年时|多年前|那年|年代|早年|年前|当初|当时|从前|往昔|过往|往事|往日|往时|昔日|旧时|时光|过去|儿时|之前|以前|曾经|旧日|少时|幼时|岁月|时代|画面|场景|镜头|影像|片段|故人|面容|身影|背影))|(?:(出|上|小时候|当年|童年|年轻时|少年时|多年前|那年|年代|早年|年前|当初|当时|从前|往昔|过往|往事|往日|往时|昔日|旧时|时光|过去|儿时|之前|以前|曾经|旧日|少时|幼时|岁月|时代|画面|场景|镜头|影像|片段|故人|面容|身影|背影)[^？?]{0,10}?(回想起|回忆起|记忆中|忆当中|联想到|浮现)))(?!.*(?:停止|止住|停下))/gm, '(人物回忆音效)$&');

// 14. 剧情转场(突发惊变)
text = text.replace(/^(?!\()(?!.*?(?:古语|古话|俗话|老话|有道是|常言道|正当权|正当利|正当诉|正当年|正当防卫|频频|不大不小|料的是|料的回答))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:想|记忆|回忆|思忆|追忆|祈祷|愿望|幻觉|梦中|梦里|思考|思索|思绪|脑海|脑中|脑里|心中|心念|心思|心里|心头|心愿|胸中|曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|那次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|只需|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|除非|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|必定|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来|预防|防止))[^“”「」？.?!])*(?:(?:(此时|此刻|之时|就在|就当|正当|无防|不备|那一刻|那一刹|那一瞬)(?![^？?]*?(?:无法|没法|无力|没能|未能|不能|可能|从未|不曾|未曾|曾被|曾经|未必|并未|并没|并无|却没|却不|却未|上回|上次|下回|下次|早已|早就|终将|必将|将来|未来|只要|定当|定是|定会|定能|可能|若能|未能|也能|才能|就能|时常|时常|往常|说不定|指不定|如果|如若|若是|一旦|但凡|就算|不论|不需|无需|也需|还需|无须|必须|不必|不得|不可|不许|不愿|不用|不会|是否|能否|或许|也许|至于|为何|为啥|为什))[^？?]{0,20}?(异变|剧变|惊变|变故|意外|离奇的|超出预料|出乎预料|不在预料|未经预料|出乎他预|出乎她预|出乎所有人)[^？?]{0,20}?(骤|陡|刹那|瞬间|突然|猛的|猛地|咋)[^？?]{0,8}?(出现|发生|降|生))|(?:(异变|剧变|惊变|变故|意外|离奇的|超出预料|出乎预料|不在预料|未经预料|出乎他预|出乎她预|出乎所有人)[^？?]{0,20}?(此时|此刻|之时|就在|就当|正当|无防|不备|那一刻|那一刹|那一瞬)[^？?]{0,20}?(骤|陡|刹那|瞬间|突然|猛的|猛地|咋)[^？?]{0,10}?(出现|发生|降|生)))(?!.*(?:停止|停下|应该|保持|平常心|从容|容应对))/gm, '(突发惊变音效)$&');

// 15. 剧情转场(争端推进)
text = text.replace(/^(?!\()(?!.*?(?:古语|古话|俗话|老话|有道是|常言道))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:想|记忆|回忆|思忆|追忆|祈祷|愿望|幻觉|梦中|梦里|思考|思索|思绪|脑海|脑中|脑里|心中|心念|心思|心里|心头|心愿|胸中|曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|那次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|只需|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|除非|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|必定|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来))[^“”「」？.?!])*(?:(?:(气氛|氛围|大战|开战|复仇的种子|仇恨的种子)[^？?]{0,20}?(霎|转瞬|一瞬|顿时|刹时|陡然|急转|就此|此刻|一触|已然|已经|这一刻|一时间)[^？?]{0,20}?(紧张|紧绷|种下|埋下|即发|在所难免|无法挽回|无可避免|无法避免))|(?:(此时|此刻|有种|似乎|如同|像是|一种)[^？?]{0,20}?(暴雨|风暴|暴风雨|山雨欲来)[^？?]{0,20}?(前夕|前的平静|前的宁静))|(?:(对)[^？?]{0,20}?(的仇恨|的愤怒|的恨意|的积怨)[^？?]{0,20}?(不断|不停|一点点|一步步|进一步)[^？?]{0,20}?(加深|积攒|转化|深化|激化|坚定了|的种子|的火种)))(?!.*(?:停止|停下|止住))/gm, '(争端推进音效)$&');

// 16. 剧情转场(悬疑氛围)
text = text.replace(/^(?!\()(?!.*?(?:古语|古话|俗话|老话|有道是|常言道|详细))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|只需|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|除非|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|必定|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来))[^“”「」？.?!])*(?:(?:(神秘|诡异|诡谲|不祥的|离奇|古怪|蹊跷|匪夷所思|扑朔迷离|迷雾重重|疑云密布|阴森|恐怖|悚然|惊悚|心悸|悸动|骇人|令人不安)[^？?]{0,20}?(出现|面纱|谜团|谜题|情形|真相|氛围|气氛|感觉|预感|征兆|迹象|阴影|黑影))|(?:(出现|面纱|谜团|谜题|情形|真相|氛围|气氛|感觉|预感|征兆|迹象|阴影|黑影)(?![^？?]*?(?:无法|没法|无力|没能|未能|不能|可能|从未|不曾|未曾|曾被|曾经|未必|并未|并没|并无|却没|却不|却未|上回|上次|下回|下次|早已|早就|即将|终将|必将|将要|将来|未来|将要|快要|只要|定当|定是|定会|定能|可能|若能|未能|也能|才能|就能|时常|时常|往常|说不定|指不定|如果|如若|若是|一旦|但凡|就算|不论|不需|无需|也需|还需|无须|必须|不必|不得|不可|不许|不愿|不用|不会|是否|能否|或许|也许|至于|为何|为啥|为什|并不|完全不|比这|相比|比较起来|比那|比起来|一点儿))[^？?]{0,20}?(神秘|诡异|诡谲|不祥的|离奇|古怪|蹊跷|匪夷所思|扑朔迷离|迷雾重重|疑云密布|阴森|恐怖|悚然|惊悚|心悸|悸动|骇人|令人不安))|(?:(尸体|女尸|女鬼)[^？?]{0,20}?(头发|枯骨)[^？?]{0,20}?(晃动|飘荡|飘动|碰触|抓住|抓握)))(?!.*(?:无解|破解|冲破))/gm, '(悬疑转场音效)$&');

// 17. 剧情转场(绝望笼罩)
text = text.replace(/^(?!\()(?!.*?(?:祈祷|古语|古话|俗话|老话|有道是|常言道|根源|原由|由来|原因|死寂之力|死寂城))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|只需|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|除非|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|必定|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来))[^“”「」？.?!])*(?:(?:(不详|恐惧|森寒|冰凉|绝望|死亡|寂灭|死寂感|死寂的感|强烈心悸|强烈的心悸)[^？?]{0,20}?(笼罩|阴影|蔓延|升腾|滋生|环绕|游荡|荡漾|侵蚀|迎面|逼近|一股))|(?:(笼罩|阴影|蔓延|升腾|滋生|环绕|游荡|荡漾|侵蚀|迎面|逼近|一股)[^？?]{0,20}?(不详|恐惧|森寒|冰凉|绝望|死亡|寂灭|死寂感|死寂的感|强烈心悸|强烈的心悸)))(?!.*(?:散去|停歇|平息|走出|走了出来))/gm, '(绝望笼罩音效)$&');

// 18. 敲门声
text = text.replace(/^(?!.*?(?:古语|古话|俗话|老话|有道是|常言道|铁门|钢铁|看门|城门|山门|芝麻|之门|宗门|面门|脑门|快门|自拍|拍摄|掌门|佛门|门村|窍门|车门|门票|罗雀|嗓门|卷帘|拍卖|拍照|拍出|拍进|拍入|拍手|竹杠|敲诈|勒索|门楣|门风|拍马|门槛|卡门|门贴|门帖|门帘|门神|气门|没有敲|没敲|敢敲|不愿敲|从来不|不用敲|碎了|的肩|的头|的背|鬼门关|哪敢|心扉|敲门砖|关门弟子|不敢|没胆子|不敲|敲锣|衙门|心门|不叩))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:想|记忆|回忆|思忆|追忆|祈祷|愿望|幻觉|梦中|梦里|思考|思索|思绪|脑海|脑中|脑里|心中|心念|心思|心里|心头|心愿|胸中|曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|那次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|只需|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|除非|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|必定|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来))[^“”「」？.?!])*(?:(?:(敲|拍|叩|扣响|咚咚|砰砰)[^？?]{0,10}?(门|房门|屋门|大门|床板|木板|黑板|天花板))|(?:(门|房门|屋门|大门|床板|木板|黑板|天花板)[^？?]{0,10}?(敲|拍|叩|扣响|咚咚|砰砰)))(?!.*(?:停下|阻止|停止|不妥|不合适))/gm, '(敲门音效)$&');

// 19. 敲锣打鼓
text = text.replace(/^(?!.*?(?:古语|古话|俗话|老话|有道是|常言道|传花|白事|哀乐|丧事|治丧|吊唁|祭典|铿锵有力))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:想|记忆|回忆|思忆|追忆|祈祷|愿望|幻觉|梦中|梦里|思考|思索|思绪|脑海|脑中|脑里|心中|心念|心思|心里|心头|心愿|胸中|曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|那次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|只需|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|除非|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|必定|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来))[^“”「」？.?!])*(?:(?:(铿锵铿锵|咚锵|敲锣打鼓|锣鼓喧天|锣鼓喧嚣|戏班子|接亲)[^？?]{0,20}?(表演|热场|演出|喇叭|唢呐|庆祝|庆贺|迎接|指挥|正在|看见|看到|看着|眼中|发出|响起|传来|传出|听见|听到|听着|耳边|耳畔|耳中))|(?:(表演|热场|演出|喇叭|唢呐|庆祝|庆贺|迎接|指挥|正在|看见|看到|看着|立刻|眼中|发出|响起|传来|传出|听见|听到|听着|耳边|耳畔|耳中)[^？?]{0,20}?(铿锵铿锵|咚锵|敲锣打鼓|锣鼓喧天|锣鼓喧嚣|戏班子|接亲)))(?!.*(?:停下|平息|停歇))/gm, '(敲锣打鼓音效)$&');

// 20. 战鼓敲响
text = text.replace(/^(?!\()(?!.*?(?:古语|古话|俗话|老话|有道是|常言道|一声|一锤|传花|心跳|击鼓传花|如擂|如击鼓))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:想|记忆|回忆|思忆|追忆|祈祷|愿望|幻觉|梦中|梦里|思考|思索|思绪|脑海|脑中|脑里|心中|心念|心思|心里|心头|心愿|胸中|曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|那次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|只需|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|除非|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|必定|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来))[^“”「」？.?!])*(?:(?:(战鼓|大鼓|铁鼓)[^？?]{0,20}?(咚|嘭|冬|敲|锤|擂响|响起|传来|传出|听见|听到|耳边|耳畔|耳中))|(?:(咚|嘭|冬|敲|锤|擂响|响起|传来|传出|听见|听到|耳边|耳畔|耳中)[^？?]{0,20}?(战鼓|大鼓|铁鼓))|(?:(号角|烽烟|狼烟|战事|战争)[^？?]{0,20}?(鼓声|击鼓|擂鼓|击战鼓|擂战鼓|战鼓擂)[^？?]{0,20}?(正在|开始|继续|接着|看见|看到|看着|看向|瞧见|瞧到|瞧着|瞥见|瞥到|听到|听见|听闻|听着|耳边|耳旁|耳内|耳中|耳畔|传来|传入|传出|传进|发出|之声|一声|声响|响声|响起|响彻|轰鸣))|(?:(正在|开始|继续|接着|看见|看到|看着|看向|瞧见|瞧到|瞧着|瞥见|瞥到|听到|听见|听闻|听着|耳边|耳旁|耳内|耳中|耳畔|传来|传入|传出|传进|发出|之声|一声|声响|响声|响起|响彻|轰鸣)[^？?]{0,20}?(号角|烽烟|狼烟|战事|战争)[^？?]{0,20}?(鼓声|击鼓|擂鼓|击战鼓|擂战鼓|战鼓擂)))(?!.*(?:停下|止住|停止))/gm, '(战鼓敲响音效)$&');

// 21. 敲脑瓜镚
text = text.replace(/^(?!.*?(?:古语|古话|俗话|老话|有道是|常言道|采摘|糖炒|炒栗|食物|零食|干果|屎盆子|冤枉|记得|记忆|记起|记住|门|欣赏|赏景|赏心|头颅|碎|裂|烂|子弹|破绽|拼杀|对拼|攻击|战斗))(?:^|(?:[。！？.?!][”\"]?))(?:(?!(?:想|记忆|回忆|思忆|追忆|祈祷|愿望|幻觉|梦中|梦里|思考|思索|思绪|脑海|脑中|脑里|心中|心念|心思|心里|心头|心愿|胸中|曾经|从前|以前|以往|经常|时常|往常|之前|先前|此前|早前|早已|早先|早就|方才|刚才|刚刚|已经|那天|前天|那时|届时|昔年|当时|当年|当初|上次|上回|下回|下次|那次|每每|每当|每逢|每次|每回|每时|每年|每天|每月|每周|每日|未来|即便|即使|如果|如若|如同|譬如|恰如|宛若|宛如|犹如|有如|就如|俨如|恰似|好似|仿似|似乎|若是|若非|若能|若使|假若|假使|假如|假设|倘若|好比|仿佛|仿若|好像|像是|像要|就像|像这|像那|要是|要么|只要|只需|无论|不论|虽然|不管|尽管|就算|一旦|万一|但凡|哪怕|除非|因为|为何|为啥|为什|凭什|凭啥|难怪|未能|未必|并未|并无|并没|从未|从不|无法|没法|可能|才能|无能|不能|必能|方能|只能|没能|所能|能够|能否|是否|允许|准许|默许|禁止|也许|或许|防止|不得|不可|不许|不愿|不必|不用|莫非|难道|难不成|之所以|是为了|说不定|指不定|作用是|作用为|不准|准备|打算|盘算|计划|企图|必定|定当|定会|定是|定要|定然|将要|将会|将来|必将|终将|即将|喜欢|喜好|爱好|习惯|擅长|擅于|效果|用于|用作|用来|刀背|锤子|巨锤|铁棒|棍子|铁棍|锅|翼))[^“”「」？.?!])*(?:(?:(脑壳|脑袋|脑瓜|额头|头顶|的头)[^？?]{0,20}?(赏了|敲在|敲中|敲了|敲过|敲下|砸过|砸在|砸中|叩|弹|曲指|屈指|指敲|一记|筷子敲|筷尾敲)[^？?]{0,20}?(板栗|毛栗|脑瓜崩|脑瓜蹦))|(?:(一个板|一记板|板栗|毛栗|曲指|屈指|筷子|筷尾)[^？?]{0,20}?(敲|叩|砸|弹)[^？?]{0,20}?(脑壳|脑袋|脑门|脑瓜|额头))|(?:(赏了|敲在|敲中|敲了|敲过|敲下|砸过|砸在|砸中|叩|弹|曲指|屈指|指敲|一记|筷子敲|筷尾敲)[^？?]{0,20}?(毛栗|板栗|脑壳|脑袋|脑瓜))|(?:(脑瓜崩|脑瓜蹦)[^？?]{0,20}?(敲|砸|赏|弹|一声|发出|响起|声响|传来|传出|传入|听见|听到|听着|听闻|耳边|耳旁|耳畔|耳中)))(?!.*(?:脑浆|停下|止住|重伤|受伤|流血))/gm, '(敲板栗音效)$&');

// ========== 组2：至底恢复双引号 ==========
// 22. 多个连续改成2个（原正则已是ES5）
text = text.replace(/([(][一-龥]{1,6}音效[)])([(][一-龥]{1,6}音效[)])?([(][一-龥]{1,6}音效[)])?([(][一-龥]{1,6}音效[)])?([(][一-龥]{1,6}音效[)])?([(][一-龥]{1,6}音效[)])?/g, '$1$2');

// ========== 组3：直接替换（无$0） ==========
// 23. 去除拟声词双引号 (原后行断言改为捕获前一字符，但此处简化)
text = text.replace(/[“\"\\[「]([吼啪轰隆铿锵乒乓砰咚哗踏挞滴溜答嗒嘀吱嗵通噗嗤丁叮钉呤铃当铛噹啷刷唰咣噌蹬噔嘎嘭哐嗡噼哩里叻沥淅窸窣哧咔嚓察哒飕嗖咻铮钲镗滋嗞呲泚厮嘶丝咝沙莎痧唦汩嗬咕噜嘟呱瓜喵叽唧嗷啾笃踱呜哆啦呀吧呼]+(?:[^\u4E00-\u9FFF\w”\"\\]」]+[吼啪轰隆铿锵乒乓砰咚哗踏挞滴溜答嗒嘀吱嗵通噗嗤丁叮钉呤铃当铛噹啷刷唰咣噌蹬噔嘎嘭哐嗡噼哩里叻沥淅窸窣哧咔嚓察哒飕嗖咻铮钲镗滋嗞呲泚厮嘶丝咝沙莎痧唦汩嗬咕噜嘟呱瓜喵叽唧嗷啾笃踱呜哆啦呀吧呼]+)*[^\u4E00-\u9FFF\w”\"\\]」]*)[”\"\\]」]/g, '$1');

// 24. 男老咳 (保持原逻辑，无ES6特性)
text = text.replace(/(“<<[^>]*?老年[^>]*?男[^>]*?>>[^”]*?)(咳+)(.*$)|(“<<[^>]*?男[^>]*?老年[^>]*?>>[^”]*?)(咳+)(.*$)/gm, '$1$4(男老咳音效)$3$6');

// 25. 女老咳
text = text.replace(/(“<<[^>]*?老年[^>]*?女[^>]*?>>[^”]*?)(咳+)(.*$)|(“<<[^>]*?女[^>]*?老年[^>]*?>>[^”]*?)(咳+)(.*$)/gm, '$1$4(女老咳音效)$3$6');

// 26. 男中咳
text = text.replace(/(“<<[^>]*?中年[^>]*?男[^>]*?>>[^”]*?)(咳+)(.*$)|(“<<[^>]*?男[^>]*?中年[^>]*?>>[^”]*?)(咳+)(.*$)/gm, '$1$4(男中咳音效)$3$6');

// 27. 女中咳
text = text.replace(/(“<<[^>]*?中年[^>]*?女[^>]*?>>[^”]*?)(咳+)(.*$)|(“<<[^>]*?女[^>]*?中年[^>]*?>>[^”]*?)(咳+)(.*$)/gm, '$1$4(女中咳音效)$3$6');

// 28. 男青咳
text = text.replace(/(“<<[^>]*?青年[^>]*?男[^>]*?>>[^”]*?)(咳+)(.*$)|(“<<[^>]*?男[^>]*?青年[^>]*?>>[^”]*?)(咳+)(.*$)/gm, '$1$4(男青咳音效)$3$6');

// 29. 女青咳
text = text.replace(/(“<<[^>]*?青年[^>]*?女[^>]*?>>[^”]*?)(咳+)(.*$)|(“<<[^>]*?女[^>]*?青年[^>]*?>>[^”]*?)(咳+)(.*$)/gm, '$1$4(女青咳音效)$3$6');

// 30. 男少咳 (原有后行断言，改写为：[^女])
text = text.replace(/(“<<[^>]*?少年[^>]*?(?:[^女][^>]*)?>>[^”]*?)(咳+)(.*$)|(“<<[^>]*?男[^>]*?少年[^>]*?>>[^”]*?)(咳+)(.*$)/gm, '$1$4(男少咳音效)$3$6');

// 31. 女少咳
text = text.replace(/(“<<[^>]*?少[^>]*?女[^>]*?>>[^”]*?)(咳+)(.*$)|(“<<[^>]*?女[^>]*?少[^>]*?>>[^”]*?)(咳+)(.*$)/gm, '$1$4(女少咳音效)$3$6');

// 32. 男童咳
text = text.replace(/(“<<[^>]*?童[^>]*?男[^>]*?>>[^”]*?)(咳+)(.*$)|(“<<[^>]*?男[^>]*?童[^>]*?>>[^”]*?)(咳+)(.*$)/gm, '$1$4(男童咳音效)$3$6');

// 33. 女童咳
text = text.replace(/(“<<[^>]*?童[^>]*?女[^>]*?>>[^”]*?)(咳+)(.*$)|(“<<[^>]*?女[^>]*?童[^>]*?>>[^”]*?)(咳+)(.*$)/gm, '$1$4(女童咳音效)$3$6');

// 34. 猫叫声兜底 (原用 \p{L}，替换为 a-zA-Z\u4e00-\u9fa5)
text = text.replace(/(?:^|[^a-zA-Z\u4e00-\u9fa5])(喵[喵呜]*?)(?:[^！？]*?(?![a-zA-Z\u4e00-\u9fa5])[喵呜]+)*(?![a-zA-Z\u4e00-\u9fa5])/gm, '$&'); // 注意：此处较难完整保留，简化为直接替换匹配，需外部逻辑补偿，这里用$&代替
// 更精确改写：使用捕获组保留前置字符，但原意是仅匹配喵叫部分并整体替换为音效，因此直接匹配并替换
text = text.replace(/(喵[喵呜]*(?:[^！？]*[喵呜]+)*)/g, '(猫叫兜底音效)');

// 35. 猫怒叫 (原正则依赖前一步的(猫叫兜底音效)，保持逻辑)
text = text.replace(/(.*?(?:怒|厮叫|厮打|厮斗|抓挠|互挠|打架|打斗|生气|发飙|发毛|炸毛|毛炸|毛竖|倒竖|根竖|弓背|背弓|弓起背|弓着背|发火的|领地|哈气|呲牙|凶狠|很凶|凶得|凶厉|凶相|威吓|威胁的|警告的).{0,25})\(猫叫兜底音效\)(.*)|(.*?)\(猫叫兜底音效\)(.{0,25}(?:怒|厮叫|厮打|厮斗|抓挠|互挠|打架|打斗|生气|发飙|发毛|炸毛|毛炸|毛竖|倒竖|根竖|弓背|背弓|弓起背|弓着背|发火的|领地|哈气|呲牙|凶狠|很凶|凶得|凶厉|凶相|威吓|威胁的|警告的).*)/gm, '$1$4(猫怒叫音效)$3$6');

// 36. 奶猫叫
text = text.replace(/(.*?(?:萌萌|可爱|软萌|奶声|奶猫|小只|小一只|猫宝宝|小猫咪|没断奶|刚出生).{0,25})\(猫叫兜底音效\)(.*)|(.*?)\(猫叫兜底音效\)(.{0,25}(?:萌萌|可爱|软萌|奶声|奶猫|小只|小一只|猫宝宝|小猫咪|没断奶|刚出生).*)/gm, '$1$4(奶猫叫音效)$3$6');

// 37. 系统文:触发系统对话【】
text = text.replace(/(?:^|[\r\n。：:！？!?])\s*【([^】]*)】/gm, '(系统提示音效)【$1】');

// 38. 脏话消音 (原(?<![0-9a-zA-Z])改为(?:^|[^0-9a-zA-Z]))
text = text.replace(/(?:^|[^0-9a-zA-Z])(?:(?:[我握卧你他妈娘奶母妹姐麻狗马的卖泥尼爹死艹淦赣草操插叉槽糙日曰吊叼屌干沙傻鲨煞杂了个哔逼比笔币鼻壁批屁])[*＊#＃xXꁘ]+|[*＊#＃xXꁘ]+(?:[我握卧你他妈娘奶母妹姐麻狗马的卖泥尼爹死艹淦赣草操插叉槽糙日曰吊叼屌干沙傻鲨煞杂了个哔逼比笔币鼻壁批屁]))(?![0-9])/gm, '$&'); 
// 简化为直接替换匹配到的整段为音效，因前后可能有字符，保留$&
text = text.replace(/([我握卧你他妈娘奶母妹姐麻狗马的卖泥尼爹死艹淦赣草操插叉槽糙日曰吊叼屌干沙傻鲨煞杂了个哔逼比笔币鼻壁批屁][*＊#＃xXꁘ]+|[*＊#＃xXꁘ]+[我握卧你他妈娘奶母妹姐麻狗马的卖泥尼爹死艹淦赣草操插叉槽糙日曰吊叼屌干沙傻鲨煞杂了个哔逼比笔币鼻壁批屁])/g, '(脏话消音音效)');

// 39. 人物无语“……"
text = text.replace(/^(.*?[:：]\s*)?([“\"‘'«‹]?)(?:<<.*?>>\s*)?(\.{2,}|…+|\.\s*\.\s*\.|。{2,}|\.{3,}|……+|\.\s*\.\s*\.\s*)([”\"'’›»]?)$/gm, '$1$2(冷乌鸦音效)$4');

// 40. 无语问号“？？？”
text = text.replace(/^(.*?[:：]\s*)?([“\"‘'«‹]?)(?:<<.*?>>\s*)?([\?？]+)([”\"'’›»]?)$/gm, '$1$2(无语问号音效)$4');

// 41. 章节转场(衔接标题) (原有后行断言，改为直接匹配)
text = text.replace(/^(第\s*[零一两二三四五六七八九十百千0-9]+\s*[章节卷部集]|[上下前后]\s*卷|[终序]\s*章|[引结]\s*言|[楔尾]\s*子)(?=[\s，。、；：:？！…—（）《》【】\"“”‘']|$)(.*)/gm, '(衔接标题音效)『$1$2』(衔接正文音效)');

// 42. 时间转场(时间变化) (原有后行断言，改为直接匹配)
text = text.replace(/^(\s*)(时间(?:来|回|到|倒|追|穿|过|转|一转|一晃)[^，。！？—～……]*[，。！？—～……]|(?:\d{1,9}|[一两三四五六七八九十百千万]+|半|半个|大半个|几|数|次|片)(?:(?:日|几日|天|多天|年|多年|月|周|几周|个月|多个月|小时|个小时|时辰|星期|个星期|个时辰|晌|钟|分钟|刻钟|柱香|炷香|盏茶|息|秒|刻))?(?:前|后|过|之后|过后|过去)[^，。！？—～……]*[，。！？—～……]|(?:天蒙蒙亮|日头西|日落西|日上三|太阳西|凌晨|拂晓|破晓|翌日|次日|清晨|早晨|早上|上午|中午|晌午|正午|下午|午后|傍晚|黄昏|夕阳|薄暮|午夜|夜晚|夜里|夜幕|晚间|晚上|夜深|深夜|半夜|夜半|子夜|三更|五更|更深|春夜|夏夜|秋夜|冬夜|星夜|月夜|一晃|时光[飞荏苒]逝|岁月如[梭流]|光阴似箭|转眼间)[^，。！？—～……]*[，。！？—～……])/gm, '$1(时间变化音效)$2');

// ========== 组4：最底拟声词（将 \p{L} 替换为 a-zA-Z\u4e00-\u9fa5） ==========
// 43. 狗叫声(汪)
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])(汪+)(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(狗叫声音效)$2');

// 44. 猫叫声(喵)
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])(喵+)(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(猫叫声音效)$2');

// 45. 羊叫声(咩)
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])(咩+)(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(羊叫声音效)$2');

// 46. 牛叫声(哞)
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])(哞+)(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(牛叫声音效)$2');

// 47. 人咋舌(啧)
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])(啧+)(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(人啧啧音效)$2');

// 48. 老鼠叫(吱)
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])(吱吱+)(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(老鼠叫音效)$2');

// 49. 破风声
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])(嗖+|咻+)(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(破风声音效)$2');


// 51. 男桀笑
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])(桀+)(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(男桀笑音效)$2');

// 52. 狼嚎声(嗷)
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])(嗷+|嗷呜|嗷呜呜|嗷呜呜呜|呜嗷|呜嗷汪)(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(狼嚎声音效)$2');

// 53. 猛禽叫(唳)
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])(唳+)(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(猛禽叫声音效)$2');

// 54. 女咳嗽
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])(咳+)(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(女咳嗽音效)$2');

// 55. 男咳嗽
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])(咳+)(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(男咳嗽音效)$2');

// 56. 男短呕
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])(呕+|哕+)(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(男短呕音效)$2');

// 57. 女短呕
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])(呕+|哕+)(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(女短呕音效)$2');

// 58. 人嘘声(嘘)
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])(嘘+)(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(人嘘声音效)$2');

// 59. 人阿嚏(湫)
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])([啊阿哈](?:嚏|啾))(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(打喷嚏音效)$2');

// 60. 人打嗝(嗝)
text = text.replace(/(^|[^a-zA-Z\u4e00-\u9fa5])(嗝|嗝儿)(?![a-zA-Z\u4e00-\u9fa5])/gm, '$1(人打嗝音效)$2');
      
      text = text.replace(/“([\u4E00-\u9FFF]{1,15})”/g, "$1");
      text = text.replace(/[〖〗‘’〈〔〕〉]/g, "");
      
      text = text.replace(/(^|音效[)])([^“”)]+”)/g, "$1“$2");
      text = text.replace(/(“[^“”(]+)([(][一-龥()]+音效[)]|$)/g, "$1”$2");
      
      text = text.replace(/[【「『]([\u4E00-\u9Fa5]+)[】』」]/g, "$1");
      
      text = text.replace(/(“[^“”\n]*)[【「『』」】]([^“”\n]*”)/g, "$1$2");
      text = text.replace(/(“[^“”\n]*)[【「『』」】]([^“”\n]*”)/g, "$1$2");
      text = text.replace(/(“[^“”\n]*)[【「『』」】]([^“”\n]*”)/g, "$1$2");
      text = text.replace(/(“[^“”\n]*)[【「『』」】]([^“”\n]*”)/g, "$1$2");

      text = text.replace(/(“[^a-zA-Z0-9\u4e00-\u9fa5]+”)/g, "");
      var soundKeywords = [];
      var yinXiaoList = [];
      
      
      
      
      
      try {
          var yinXiaoContent = ttsrv.readTxtFile("yinxiao.json");
          if (yinXiaoContent && yinXiaoContent.trim() !== "") {
              yinXiaoList = JSON.parse(yinXiaoContent);
              for (var i = 0; i < yinXiaoList.length; i++) {
                  var item = yinXiaoList[i];
                  if (item && item.name) {
                      var names = item.name.split("|"); // 按“|”分割多关键词
                      for (var j = 0; j < names.length; j++) {
                          var name = names[j].trim();
                          // 新增过滤：跳过开头是“#”的name，仅保留非#开头且非空的name
                          if (name !== "" && !name.startsWith("#")) { 
                              soundKeywords.push(name);
                          }
                      }
                  }
              }
          }
      } catch (e) {
      }
      







      var commonPunctuation = "。，！？：；、·…—-";
      var parsedKWs = this.parseSoundKeywords(yinXiaoList);

      // ========== 本地音效双匹配逻辑（只换匹配内容，不碰标签壳） ==========
      var localSoundOnoMap = {}; 
      var localSoundRegexMap = {}; 
      // 新增：生成1~100完整本地音效标签数组（含97个新增标签）
      var allLocalSoundTags = [];
      for (var i = 1; i <= 990; i++) {
          allLocalSoundTags.push("localSound" + i);
      }

      // 1. 读取本地音效配置（修复反斜杠转义问题）- 覆盖1~100
      for (var i = 0; i < allLocalSoundTags.length; i++) {
      var tagKey = allLocalSoundTags[i];
      if (tagsData && tagsData[tagKey] && tagsData[tagKey].audioName) {
        var audioNameConfig = tagsData[tagKey].audioName;
        // 强制扁平化数组，兼容嵌套结构
        var flatConfig = forceFlattenArray(audioNameConfig);
        var allOnoList = [];
        var allRegexList = [];
        
        // 直接遍历配置项读取value，彻底避免JSON.stringify导致的二次转义
        for (var j = 0; j < flatConfig.length; j++) {
            var configItem = flatConfig[j];
            var inputValue = "";
            
            // 安全读取value，兼容JS对象和Java原生对象
            if (typeof configItem === 'object' && configItem !== null) {
                inputValue = configItem.value !== undefined ? (configItem.value + "").trim() : "";
                // 兜底兼容Java对象的get方法
                if (inputValue === "" && typeof configItem.get === 'function') {
                    var tempVal = configItem.get("value");
                    inputValue = tempVal ? (tempVal + "").trim() : "";
                }
            }
            if (inputValue === "") continue;

            // 正则关键词处理
            if (inputValue.startsWith("@") || inputValue.startsWith("＜") || inputValue.startsWith("＞") || inputValue.startsWith("<")) {
                allRegexList.push({
                    originKW: inputValue,
                    type: inputValue.charAt(0),
                    regex: new RegExp(inputValue.slice(1), 'g')
                });
            } else {
                // 普通关键词分割
                var allParts = inputValue.split('|');
                for (var m = 0; m < allParts.length; m++) {
                    var part = allParts[m].trim();
                    if (part) allOnoList.push(part);
                }
            }
        }
        var tagName = this.tags[tagKey];
        
        localSoundOnoMap[tagKey] = {};
        for (var k = 0; k < allOnoList.length; k++) {
            localSoundOnoMap[tagKey][allOnoList[k]] = tagKey;
        }

        localSoundRegexMap[tagKey] = allRegexList;
      }
      }

      // 2. 本地普通音效：替换「匹配到的内容」- 覆盖1~100（修改版：保留匹配内容，仅留中文汉字）
      var onoMarkedText = text;
      for (var i = 0; i < allLocalSoundTags.length; i++) {
          var tagKey = allLocalSoundTags[i];
          var tagAudioMap = localSoundOnoMap[tagKey];
          if (!tagAudioMap) continue;
          var tagName = this.tags[tagKey];
          
          for (var targetOno in tagAudioMap) {
              var escapedOno = targetOno.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
              var onoReg = new RegExp('“(' + escapedOno + ')([。，！？：；、…—-]{0,2})”', 'g');
              
              onoMarkedText = onoMarkedText.replace(onoReg, function(match, onoContent) {
                  // 核心修改：过滤匹配内容，仅保留中文汉字
                  var onlyChineseContent = onoContent.replace(/[^\u4e00-\u9fa5]/g, "");
                  // 兜底兼容：过滤后为空时，保留原始匹配内容
                  var finalContent = onlyChineseContent || onoContent;
                  // 原符号转义逻辑保留
                  var replacedContent = SpeechRuleJS.replaceTargetContentSymbols(finalContent);
                  var startMark = "{{" + tagName + "_" + replacedContent + "}}";
                  var endMark = "{{" + tagName + "结束}}";
                  return "\n" + startMark + finalContent + endMark + "\n";
              });
          }
      }

      // 3. 本地＞＜正则音效：替换「匹配到的内容」- 覆盖1~100（修改版：保留匹配内容，仅留中文汉字）
      var regexMarkedText = onoMarkedText;
      for (var i = 0; i < allLocalSoundTags.length; i++) {
          var tagKey = allLocalSoundTags[i];
          var regexList = localSoundRegexMap[tagKey];
          if (!regexList || regexList.length === 0) continue;
          var tagName = this.tags[tagKey];

          for (var r = 0; r < regexList.length; r++) {
              var rkw = regexList[r];
              
              regexMarkedText = regexMarkedText.replace(rkw.regex, function(match) {
                  // 核心修改：过滤匹配内容，仅保留中文汉字
                  var onlyChineseContent = match.replace(/[^\u4e00-\u9fa5]/g, "");
                  // 兜底兼容：过滤后为空时，保留原始匹配内容
                  var finalContent = onlyChineseContent || match;
                  // 原符号转义逻辑保留
                  var replacedContent = SpeechRuleJS.replaceTargetContentSymbols(finalContent);
                  var newContentWithTag = "{{" + tagName + "_" + replacedContent + "}}" + finalContent + "{{" + tagName + "结束}}";

                  // 原全角/半角符号前后插逻辑完全保留
                  if (rkw.type === "＜" || rkw.type === "<") {
                      return "\n" + newContentWithTag + "\n" + match;
                  } else if (rkw.type === "＞" || rkw.type === ">") {
                      return match + "\n" + newContentWithTag + "\n";
                  } else {
                      return "\n" + newContentWithTag + "\n";
                  }
              });
              rkw.regex.lastIndex = 0;
          }
      }


      text = regexMarkedText;
      // ========== 本地音效双匹配结束 ==========

      // -------------------------- 在线音效处理（双引号内：用originFullKW替换，保留完整关键词；支持全角/半角＜＞） --------------------------
      if (soundKeywords.length > 0 && text.includes("“")) {
          var quotedReg = new RegExp('“.*?”', 'g'); // 最兼容正则写法
          text = text.replace(quotedReg, function(match) {
              var result = match;

              // 1. 在线正则关键词：替换「关键词本身」（保留全/半角符号）
              for (var r = 0; r < parsedKWs.regexKWs.length; r++) {
                  var rkw = parsedKWs.regexKWs[r];
              
              
                                  
                                      // -------------------------- 新增：跳过<>开头的正则关键词（双引号内不匹配） --------------------------
                  if (rkw.flag === "<" || rkw.flag === "＜" || rkw.flag === "＞" || rkw.flag === ">" || rkw.flag === "@") { // 半角<、全角＜开头的正则，直接跳过
                      continue; 
                  }
                  // --------------------------------------------------------------------------------------------------
                  
                  
              
              
              
              
                  var tempResult = "";
                  var lastIndex = 0;
                  var regexMatch;
                  var matchCount = 0;

                  while ((regexMatch = rkw.regex.exec(result)) !== null) {
                      matchCount++;
                      var matchedContent = regexMatch[0];
                      // 只替换关键词本身（rkw.originKW，含全/半角符号）
                      var replacedKeyword = SpeechRuleJS.replaceTargetContentSymbols(rkw.originKW);
                      var tag = "";

                      // 兼容判断：全角＜、半角<归为左符号（前插）；全角＞、半角>归为右符号（后插）
                      if (rkw.flag === "＜" || rkw.flag === "<") {
                          tag = "\n〖" + replacedKeyword + "〗\n" + matchedContent;
                      } else if (rkw.flag === "＞" || rkw.flag === ">") {
                          tag = matchedContent + "\n〖" + replacedKeyword + "〗\n";
                      } else {
                          tag = '〖' + replacedKeyword + '〗';
                      }

                      tempResult += result.substring(lastIndex, regexMatch.index) + tag;
                      lastIndex = rkw.regex.lastIndex;
                  }
                  
                  if (matchCount > 0) {
                      tempResult += result.substring(lastIndex);
                      result = tempResult;
                  }
                  rkw.regex.lastIndex = 0;
              }

              // 2. 在线特殊关键词：用originFullKW（完整原始关键词）替换
              for (var s = 0; s < parsedKWs.specialKWs.length; s++) {
                  var skw = parsedKWs.specialKWs[s];
                  var prefixLen = Math.floor(skw.prefixLen) || 1;
                  var suffixLen = Math.floor(skw.suffixLen) || 1;
                  var specialReg = new RegExp(
                      '(.{0,' + prefixLen + '})' + 
                      escapeRegExp(skw.coreKW) + 
                      '(.{0,' + suffixLen + '})' + 
                      '(?=[' + commonPunctuation + ']|$|' + escapeRegExp(skw.coreKW) + ')', 
                      'g'
                  );

                  var tempResult = "";
                  var lastIndex = 0;
                  var matchResult;
                  var matchCount = 0;
                  while ((matchResult = specialReg.exec(result)) !== null) {
                      matchCount++;
                      var matchedContent = matchResult[0];
                      // 关键修改：用完整原始关键词（skw.originFullKW）替换，而非核心KW
                      var replacedKeyword = SpeechRuleJS.replaceTargetContentSymbols(skw.originFullKW);
                      tempResult += result.substring(lastIndex, matchResult.index) + '〖' + replacedKeyword + '〗';
                      lastIndex = matchResult.index + matchResult[0].length;
                  }
                  
                  if (matchCount > 0) {
                      tempResult += result.substring(lastIndex);
                      result = tempResult;
                  }
              }












         
              
              // 3. 在线普通关键词：替换「关键词本身」
              var repeatCheck = isSingleKeywordRepeat(result.replace(/〖.*?〗/g, ''), parsedKWs.normalKWs);
              if (repeatCheck.isRepeat) {
                  // 新增1：ES5循环找子关键词对应的母关键词组（不破坏原有重复检测逻辑）
                  var matchedGroup = null;
                  for (var g = 0; g < parsedKWs.normalKWGroups.length; g++) {
                      var group = parsedKWs.normalKWGroups[g];
                      for (var c = 0; c < group.children.length; c++) {
                          if (group.children[c] === repeatCheck.keyword) {
                              matchedGroup = group;
                              break;
                          }
                      }
                      if (matchedGroup) break;
                  }
                  // 新增2：优先用母关键词，无匹配则保留原有子关键词
                  var replaceKW = matchedGroup ? matchedGroup.motherKW : repeatCheck.keyword;
              
                  // 以下是你原有代码，完全保留（仅把repeatCheck.keyword改成replaceKW）
                  var kw = repeatCheck.keyword; // 保留原有kw变量（用于匹配定位）
                  var kwLen = kw.length;
                  var normalResult = "";
                  var currentPos = 0;
                  var matchCount = 0;
              
                  while (currentPos <= result.length - kwLen) {
                      var kwIndex = result.indexOf(kw, currentPos);
                      if (kwIndex === -1) break;
                      
                      if (result.slice(currentPos, kwIndex).includes("〖")) {
                          currentPos = kwIndex + kwLen;
                          continue;
                      }
              
                      var nextKwPos = kwIndex + kwLen;
                      var isContinuous = result.substr(nextKwPos, kwLen) === kw;
                      var nextChar = result[nextKwPos] || "";
                      var isAllowed = isContinuous || nextKwPos >= result.length || 
                                     commonPunctuation.includes(nextChar) || nextChar === "\"" || nextChar === "”";
              
                      if (isAllowed) {
                          matchCount++;
                          // 仅修改这里：用replaceKW（母关键词）替换原有kw
                          var replacedKw = SpeechRuleJS.replaceTargetContentSymbols(replaceKW);
                          normalResult += result.substring(currentPos, kwIndex) + "〖" + replacedKw + "〗";
                          currentPos = isContinuous ? nextKwPos : kwIndex + kwLen;
                      } else {
                          normalResult += result.substring(currentPos, kwIndex + kwLen);
                          currentPos = kwIndex + kwLen;
                      }
                  }
                  
                  if (matchCount > 0) {
                      result = currentPos < result.length ? normalResult + result.substring(currentPos) : normalResult;
                  }
              } else {
                  // 新增3：ES5循环遍历母关键词组（替代原有直接定义singleKWReg的逻辑）
                  for (var g = 0; g < parsedKWs.normalKWGroups.length; g++) {
                      var group = parsedKWs.normalKWGroups[g];
                      // 新增4：ES5循环拼接子关键词正则（保留原有escapeRegExp逻辑）
                      var childrenAlt = "";
                      for (var c = 0; c < group.children.length; c++) {
                          if (c > 0) childrenAlt += "|"; // 保留原有分隔符逻辑
                          childrenAlt += escapeRegExp(group.children[c]);
                      }
                      if (!childrenAlt) continue;
              
                      // 以下是你原有singleKWReg的完整逻辑，完全保留（仅改replacedKw来源）
                      var singleKWReg = new RegExp('(^|\\s|[' + commonPunctuation + '])(' + childrenAlt + ')([' + commonPunctuation + ']{0,2})?' + '(?=\\s|[' + commonPunctuation + ']|["”]|$)', 'g');
                      result = result.replace(singleKWReg, function(match, prefix, matchedChildKW, punc) {
                          if (match.includes("〖")) return match;
                          var afterPunc = text.substring(text.indexOf(match) + match.length, text.indexOf(match) + match.length + 1);
                          if (afterPunc && !commonPunctuation.includes(afterPunc) && afterPunc !== " " && afterPunc !== "" && afterPunc !== "\"" && afterPunc !== "”") {
                              return match;
                          }
                          // 仅修改这里：用母关键词替换原有matchedChildKW
                          var replacedKw = SpeechRuleJS.replaceTargetContentSymbols(group.motherKW);
                          return prefix + "\n〖" + replacedKw + "〗\n" + (punc || "");
                      });
                  }
              }
              



              // 处理引号占位符（兼容原有逻辑）
              result = result.replace(/〖([^〗]*)“([^〗]*)〗/g, function(m, p1, p2) {
                  return "〖" + p1 + "###LEFT_QUOTE###" + p2 + "〗";
              });
              result = result.replace(/〖([^〗]*)”([^〗]*)〗/g, function(m, p1, p2) {
                  return "〖" + p1 + "###RIGHT_QUOTE###" + p2 + "〗";
              });

              return result;
          });
      }

      // -------------------------- 在线音效独立场景匹配（用originFullKW替换，保留完整关键词；支持全角/半角＜＞） --------------------------
      if (soundKeywords.length > 0) {
          
          for (var r = 0; r < parsedKWs.regexKWs.length; r++) {
              var rkw = parsedKWs.regexKWs[r];
              var tempResult = "";
              var lastIndex = 0;
              var regexMatch;
              var matchCount = 0;
              
              while ((regexMatch = rkw.regex.exec(text)) !== null) {
                  matchCount++;
                  var matchedContent = regexMatch[0];
                  // 保留原始关键词（含全/半角符号）
                  var replacedKeyword = SpeechRuleJS.replaceTargetContentSymbols(rkw.originKW);
                  var tag = "";

                  // 兼容判断：全角＜、半角<归为左符号；全角＞、半角>归为右符号
                  if (rkw.flag === "＜" || rkw.flag === "<") {
                      tag = "\n〖" + replacedKeyword + "〗\n" + matchedContent;
                  } else if (rkw.flag === "＞" || rkw.flag === ">") {
                      tag = matchedContent + "\n〖" + replacedKeyword + "〗\n";
                  } else {
                      tag = '〖' + replacedKeyword + '〗';
                  }

                  tempResult += text.substring(lastIndex, regexMatch.index) + tag;
                  lastIndex = rkw.regex.lastIndex;
              }
              
              if (matchCount > 0) {
                  tempResult += text.substring(lastIndex);
                  text = tempResult;
              }
              rkw.regex.lastIndex = 0;
          }

          // 在线独立特殊关键词：用originFullKW（完整原始关键词）替换
          for (var s = 0; s < parsedKWs.specialKWs.length; s++) {
              var skw = parsedKWs.specialKWs[s];
              var prefixLen = Math.floor(skw.prefixLen) || 1;
              var suffixLen = Math.floor(skw.suffixLen) || 1;
              var specialIndependentReg = new RegExp(
                  '(.{0,' + prefixLen + '})' + 
                  escapeRegExp(skw.coreKW) + 
                  '(.{0,' + suffixLen + '})' + 
                  '(?=\\s|[。，！？：；、·…—-]|啊|呀|呢|啦|$|' + escapeRegExp(skw.coreKW) + ')', 
                  'g'
              );

              var tempResult = "";
              var lastIndex = 0;
              var matchResult;
              var matchCount = 0;
              
              while ((matchResult = specialIndependentReg.exec(text)) !== null) {
                  matchCount++;
                  var matchedContent = matchResult[0];
                  // 关键修改：用完整原始关键词（skw.originFullKW）替换
                  var replacedKeyword = SpeechRuleJS.replaceTargetContentSymbols(skw.originFullKW);
                  tempResult += text.substring(lastIndex, matchResult.index) + '〖' + replacedKeyword + '〗';
                  lastIndex = matchResult.index + matchResult[0].length;
              }
              
              if (matchCount > 0) {
                  tempResult += text.substring(lastIndex);
                  text = tempResult;
              }
          }











// 普通关键词：先处理重复，再处理单个（保留你原有注释）
          var escapedNormalKWs = [];
          for (var n = 0; n < parsedKWs.normalKWs.length; n++) {
              escapedNormalKWs.push(escapeRegExp(parsedKWs.normalKWs[n]));
          }
          var normalKWAlt = escapedNormalKWs.join("|");
          if (normalKWAlt) {
              // 新增1：先处理重复关键词（复用你原有repeatKWReg逻辑，仅改replacedKw）
              var repeatCheck = isSingleKeywordRepeat(text.replace(/〖.*?〗/g, ''), parsedKWs.normalKWs);
              if (repeatCheck.isRepeat) {
                  // 新增2：ES5循环找母关键词组
                  var matchedGroup = null;
                  for (var g = 0; g < parsedKWs.normalKWGroups.length; g++) {
                      var group = parsedKWs.normalKWGroups[g];
                      for (var c = 0; c < group.children.length; c++) {
                          if (group.children[c] === repeatCheck.keyword) {
                              matchedGroup = group;
                              break;
                          }
                      }
                      if (matchedGroup) break;
                  }
                  var replaceKW = matchedGroup ? matchedGroup.motherKW : repeatCheck.keyword;
          
                  // 以下是你原有重复关键词的while循环逻辑，完全保留
                  var kw = repeatCheck.keyword;
                  var kwLen = kw.length;
                  var normalResult = "";
                  var currentPos = 0;
                  var matchCount = 0;
          
                  while (currentPos <= text.length - kwLen) {
                      var kwIndex = text.indexOf(kw, currentPos);
                      if (kwIndex === -1) break;
                      
                      if (text.slice(currentPos, kwIndex).includes("〖")) {
                          currentPos = kwIndex + kwLen;
                          continue;
                      }
          
                      var nextKwPos = kwIndex + kwLen;
                      var isContinuous = text.substr(nextKwPos, kwLen) === kw;
                      var nextChar = text[nextKwPos] || "";
                      var isAllowed = isContinuous || nextKwPos >= text.length || commonPunctuation.includes(nextChar);
          
                      if (isAllowed) {
                          matchCount++;
                          // 仅修改这里：用母关键词
                          var replacedKw = SpeechRuleJS.replaceTargetContentSymbols(replaceKW);
                          normalResult += text.substring(currentPos, kwIndex) + "〖" + replacedKw + "〗";
                          currentPos = isContinuous ? nextKwPos : kwIndex + kwLen;
                      } else {
                          normalResult += text.substring(currentPos, kwIndex + kwLen);
                          currentPos = kwIndex + kwLen;
                      }
                  }
                  
                  if (matchCount > 0) {
                      text = currentPos < text.length ? normalResult + text.substring(currentPos) : normalResult;
                  }
              } else {
                  // 新增3：ES5循环遍历母关键词组（处理非重复关键词）
                  for (var g = 0; g < parsedKWs.normalKWGroups.length; g++) {
                      var group = parsedKWs.normalKWGroups[g];
                      // 新增4：ES5循环拼接子关键词正则
                      var childrenAlt = "";
                      for (var c = 0; c < group.children.length; c++) {
                          if (c > 0) childrenAlt += "|";
                          childrenAlt += escapeRegExp(group.children[c]);
                      }
                      if (!childrenAlt) continue;
          
                      // 1. 重复子关键词匹配（你原有repeatKWReg逻辑，仅改replacedKw）
                      var repeatKWReg = new RegExp(
                          '(^|\\s|["“]|[' + commonPunctuation + '])(' + childrenAlt + ')([' + commonPunctuation + ']{0,1})?' + '\\2' +
                          '(?=\\s|[' + commonPunctuation + ']|["”]|啊|呀|呢|啦|$)', 
                          'g'
                      );
                      text = text.replace(repeatKWReg, function(match, prefix, matchedChildKW, punc) {
                          if (match.includes("〖")) return match;
                          // 仅修改这里：用母关键词
                          var replacedKw = SpeechRuleJS.replaceTargetContentSymbols(group.motherKW);
                          return prefix + "〖" + replacedKw + "〗" + (punc || "") + "〖" + replacedKw + "〗";
                      });
          
                      // 2. 单个子关键词匹配（你原有singleKWReg逻辑，仅改replacedKw）
                      var singleKWReg = new RegExp(
                          '(^|\\s|["“]|[' + commonPunctuation + '])(' + childrenAlt + ')([' + commonPunctuation + ']{0,2})?' +
                          '(?=\\s|[' + commonPunctuation + ']|["”]|啊|呀|呢|啦|$)', 
                          'g'
                      );
                      text = text.replace(singleKWReg, function(match, prefix, matchedChildKW, punc) {
                          if (match.includes("〖")) return match;
                          var afterPunc = text.substring(text.indexOf(match) + match.length, text.indexOf(match) + match.length + 1);
                          if (afterPunc && !commonPunctuation.includes(afterPunc) && afterPunc !== " " && afterPunc !== "" && 
                              afterPunc !== "\"" && afterPunc !== "”" && !["啊","呀","呢","啦"].includes(afterPunc)) {
                              return match;
                          }
                          // 仅修改这里：用母关键词
                          var replacedKw = SpeechRuleJS.replaceTargetContentSymbols(group.motherKW);
                          return prefix + "\n〖" + replacedKw + "〗\n" + (punc || "");
                      });
                  }
              }
          
          }
          


          text = text.replace(/〖([^〗]*)“([^〗]*)〗/g, function(m, p1, p2) {
              return "〖" + p1 + "###LEFT_QUOTE###" + p2 + "〗";
          });
          text = text.replace(/〖([^〗]*)”([^〗]*)〗/g, function(m, p1, p2) {
              return "〖" + p1 + "###RIGHT_QUOTE###" + p2 + "〗";
          });
      }







       // -------------------------- 书籍切换与角色备份（原版流程复刻：从 data.json 读取） --------------------------
      try {
          if (text.includes("“")) {
              // 【仅修改此处：替换原httpGet书架接口为读取data.json】
              var dataJsonContent = "";
              try {
                  dataJsonContent = ttsrv.readTxtFile("data.json").toString();
              } catch (e) {}
              
              // 【原版逻辑：仅当数据有效时执行后续】
              if (dataJsonContent && dataJsonContent.trim() !== "") {
                  var bookData = JSON.parse(dataJsonContent.toString());
                  // 【原版逻辑：取第一本书信息，对应原firstBook】
                  var newBookName = String(bookData.bookName || "未知书名").trim();
                  // 【仅修改此处：替换原多章节httpGet为直接读取texts字段】
                  var fullChapterContent = String(bookData.texts || "").toString();
                  
                  var oldBookName = "";
                  var cunfangReadSuccess = false;
                  
                  // 读取缓存的旧书名，判断是否需要换书
                  try {
                      var rawContent = ttsrv.readTxtFile("cunfang.txt").toString();
                      oldBookName = String(rawContent).trim().toString();
                      cunfangReadSuccess = true;
                  } catch (e) {}
      
                  // ===================== 第一步：先处理换书逻辑（仅当书名不一致时执行）【原版完全保留】 =====================
                  if (cunfangReadSuccess && oldBookName !== newBookName) {
                      try {
                          // 1. 旧书角色备份（原有逻辑完全保留）
                          if (oldBookName && oldBookName !== "") {
                              try {
                                  var characterRecordsContent = "[]";
                                  try {
                                      var rawRecords = ttsrv.readTxtFile("characterRecords.json").toString();
                                      characterRecordsContent = String(rawRecords).toString();
                                  } catch (e) {}
                                  var oldShumingFileName = "shuming." + oldBookName + ".json";
                                  ttsrv.writeTxtFile(oldShumingFileName, characterRecordsContent.toString());
                              } catch (saveError) {}
                          }
                          // 2. 新书角色加载（原有逻辑完全保留）
                          var newShumingFileName = "shuming." + newBookName + ".json";
                          var newFileExists = false;
                          try {
                              var newShumingContent = ttsrv.readTxtFile(newShumingFileName).toString();
                              var jsNewShumingContent = String(newShumingContent).toString();
                              if (jsNewShumingContent && jsNewShumingContent.length > 0) {
                                  newFileExists = true;
                                  ttsrv.writeTxtFile("gengxin.json", newShumingContent.toString());
                              } else {
                                  throw new Error("文件空");
                              }
                          } catch (e) {
                              var emptyArrayContent = "[]";
                              ttsrv.writeTxtFile("gengxin.json", emptyArrayContent.toString());
                          }
                          // 3. 更新缓存书名（原有逻辑完全保留）
                          try {
                              ttsrv.writeTxtFile("cunfang.txt", newBookName.toString());
                          } catch (cunfangError) {}
                          // ★ 换书时重置永久章节缓存进度指针
                          try { ttsrv.writeTxtFile(PROGRESS_FILE, "{}"); } catch (_pe) {}
                          // 4. 书籍列表更新（原有逻辑完全保留）
                          var liebiaoContent = "[]";
                          try {
                              liebiaoContent = String(ttsrv.readTxtFile("liebiao.json").toString());
                          } catch (e) {}
                          var liebiaoArray = [];
                          try {
                              liebiaoArray = JSON.parse(liebiaoContent.toString());
                          } catch (e) {}
                          var isInArray = false;
                          for (var i = 0; i < liebiaoArray.length; i++) {
                              if (liebiaoArray[i].toString() === newBookName.toString()) {
                                  isInArray = true;
                                  break;
                              }
                          }
                          if (!isInArray) {
                              liebiaoArray.push(newBookName.toString());
                              ttsrv.writeTxtFile("liebiao.json", JSON.stringify(liebiaoArray, null, 2).toString());
                          }
                          // 5. 换书重置逻辑（原有逻辑完全保留）
                          shijian = new Date(Date.now() - 2 * 60 * 60 * 1000);
                          shijian.setSeconds(0);
                          shijian.setMilliseconds(0);
                          // 换书强制清空旧下文残留，避免异常【原版注释状态保留】
                          //    next100Chars = "";
      
                          //console.log("【换书成功】已从「" + oldBookName + "」切换到「" + newBookName + "」，已重置时间和上下文");
                      } catch (fileError) {
                          console.error("【换书逻辑异常】", fileError.message);
                          next100Chars = "";
                      }
                  }
      
                  // ===================== 第二步：无论换不换书，统一执行下文内容获取【原版完全保留】 =====================
                  try {
                      // 【已删除原多章节循环加载逻辑，替换为上方直接读取fullChapterContent】
                      
                      // 文本匹配定位，原有逻辑完全保留
                      var textToSearch = text2.toString();
                      var finalIndex = -1;
                      var historyTail10 = "";
                      
                      if (characterManager.contextHistory2 && characterManager.contextHistory2.length >= 10) {
                          historyTail10 = characterManager.contextHistory2.slice(-10).trim();
                      }
      
                      var historyPos = -1;
                      if (historyTail10) {
                          historyPos = fullChapterContent.indexOf(historyTail10);
                      }
      
                      var currentMatchPositions = [];
                      var tempPos = fullChapterContent.indexOf(textToSearch);
                      while (tempPos !== -1) {
                          currentMatchPositions.push(tempPos);
                          tempPos = fullChapterContent.indexOf(textToSearch, tempPos + textToSearch.length);
                      }
      
                      // 定位最终匹配位置，原有逻辑完全保留
                      if (currentMatchPositions.length > 0) {
                          if (historyPos !== -1) {
                              var minDistance = Infinity;
                              var closestPos = -1;
                              for (var p = 0; p < currentMatchPositions.length; p++) {
                                  var distance = Math.abs(currentMatchPositions[p] - historyPos);
                                  if (distance < minDistance) {
                                      minDistance = distance;
                                      closestPos = currentMatchPositions[p];
                                  }
                              }
                              finalIndex = closestPos !== -1 ? closestPos : currentMatchPositions[0];
                          } else {
                              finalIndex = currentMatchPositions[0];
                          }
                      } else {
                          finalIndex = fullChapterContent.indexOf(textToSearch);
                      }
      
                      // 时间差判断，动态设置xiawen，原有需求逻辑完全保留
                      var now = new Date();
                      var diffMinutes = (now.getTime() - shijian.getTime()) / (60 * 1000);
                      if (diffMinutes > 30) {
                          xiawen = shouci;
                      } else {
                          xiawen = xiawens;
                      }
                      // 对比完成后，刷新当前时间到shijian，原有需求逻辑完全保留
                      shijian = new Date(now);
                      shijian.setSeconds(0);
                      shijian.setMilliseconds(0);
      
                      // 计算并更新下文内容
                      if (finalIndex !== -1) {
                          var startPos = finalIndex + textToSearch.length;
                          var remainingLength = fullChapterContent.length - startPos;
                          var extractLength = Math.min(xiawen, remainingLength);
                          next100Chars = fullChapterContent.substring(startPos, startPos + extractLength);
                        //  //console.log("【下文获取成功】共" + next100Chars.length + "字，当前书籍：" + newBookName);
                      } else {
                          //console.log("【章节匹配失败】下文置空，当前书籍：" + newBookName);
                          next100Chars = "";
                      }
                  } catch (chapterError) {
                      console.error("【下文获取异常】", chapterError.message);
                      next100Chars = "";
                  }
              }
          }
      } catch (e) {
      //    console.error("【书籍模块全局异常】", e.message);
          next100Chars = "";
      }
      

      




      // -------------------------- 角色记录更新与发音人检测（含100个本地音效） --------------------------
      try {
          var updateFilePath = "gengxin.json";
          var updateExists = false;
          var jsonFileContent = "";
          try {
              jsonFileContent = ttsrv.readTxtFile(updateFilePath).toString(); // 兼容：转原始String
              updateExists = true;
          } catch (e) {
              updateExists = false;
          }
          if (updateExists) {
              if (jsonFileContent.trim() !== "") {
                  try {
                      var newRecords = JSON.parse(jsonFileContent.toString()); // 兼容：转原始String
                      // 确保角色记录是数组
                      if (!Array.isArray(newRecords)) throw new Error("角色记录非数组");
                      var oldManager = this.characterManager;
                      this.characterManager = new CharacterManager();
                      this.characterManager.characterRecords = newRecords;
                      this.characterManager.usedVoices = {};
                      this.characterManager.voiceUsageMap = {};
                      this.characterManager.contextHistory = oldManager.contextHistory ? oldManager.contextHistory.toString() : "";
                      // 初始化发音人使用状态（含100个本地音效）
                      for (var i = 0; i < this.characterManager.characterRecords.length; i++) {
                          var record = this.characterManager.characterRecords[i];
                          if (record && record.voice) {
                              var voiceTag = record.voice.toString(); // 兼容：转原始String
                              this.characterManager.usedVoices[voiceTag] = true;
                              this.characterManager.voiceUsageMap[voiceTag] = (this.characterManager.voiceUsageMap[voiceTag] || 0) + 1;
                          }
                      }
                      this.characterManager.saveRecords();
                  } catch (parseError) {
                  }
              }
              try {
                  ttsrv.deleteFile(updateFilePath);
              } catch (deleteError) {
              }
          }
      } catch (e) {
      }

      // 检测可用发音人（含localSound1~100）
      this.characterManager.detectAvailableVoices(tagsData);


   // 新增：在handleText中实时读取duihua配置（ES5强制解嵌套，8个缩进）

      // 新增：在handleText中实时读取duihua配置（ES5强制解嵌套，8
      // 新增：在handleText中实时读取duihua配置（ES5强制解嵌套，8个缩进）
      if (tagsData && tagsData['duihua']) {
              try {
                      // 1. ES5：获取原始数组
                      var roles = tagsData['duihua']['role'] || [];
                      var genderAges = tagsData['duihua']['genderAge'] || [];

                      // 2. ES5：强制双重扁平化（解决Rhino数组识别失败）
                      roles = forceFlattenArray(roles); // 第一次强制扁平化
                      roles = forceFlattenArray(roles); // 第二次处理残留嵌套
                      genderAges = forceFlattenArray(genderAges);
                      genderAges = forceFlattenArray(genderAges);

                      // 3. 兼容单个对象转为数组
                      if (!isArray(roles)) {
                              roles = [roles];
                      }
                      if (!isArray(genderAges)) {
                              genderAges = [genderAges];
                      }


                      // 4. 遍历角色（手动解嵌套兜底）
                      var finalRoles = [];
                      for (var i = 0; i < roles.length; i++) {
                              var item = roles[i];
                              // 兜底：如果还是数组，手动展开
                              if (isArray(item)) {
                                      for (var j = 0; j < item.length; j++) {
                                              finalRoles.push(item[j]);
                                      }
                              } else {
                                      finalRoles.push(item);
                              }
                      }
                      roles = finalRoles;

                      // 清空之前的对话标签配置（避免重复）
                      DUIHUA_CHARACTERS = {};
                      // 5. 遍历单个角色对象
                      for (var roleIdx = 0; roleIdx < roles.length; roleIdx++) {
                              var roleItem = roles[roleIdx];
                              // 关键修复：确保genderAgeItem索引对应，且兜底空对象
                              var genderAgeItem = genderAges[roleIdx] || {};

                              // 兜底：如果genderAgeItem是数组，取第一个元素
                              if (isArray(genderAgeItem) && genderAgeItem.length > 0) {
                                      genderAgeItem = genderAgeItem[0];
                              }

                              // 6. 安全取value（ES5，增强判空）
                              var roleValue = "";
                              if (typeof roleItem === 'object' && roleItem !== null) {
                                      // 兼容：直接访问value或通过索引获取
                                      roleValue = roleItem.value !== undefined ? (roleItem.value + "").trim() : "";
                                      // 兜底：如果value为空，尝试通过get方法（适配Java对象）
                                      if (roleValue === "" && typeof roleItem.get === 'function') {
                                              var tempVal = roleItem.get("value");
                                              roleValue = tempVal ? (tempVal + "").trim() : "";
                                      }
                              }

                              var genderAgeValue = "";
                              if (typeof genderAgeItem === 'object' && genderAgeItem !== null) {
                                      // 关键修复：安全获取genderAge的value（原代码可能漏了这步）
                                      genderAgeValue = genderAgeItem.value !== undefined ? (genderAgeItem.value + "").trim() : "";
                                      if (genderAgeValue === "" && typeof genderAgeItem.get === 'function') {
                                              var tempGaVal = genderAgeItem.get("value");
                                              genderAgeValue = tempGaVal ? (tempGaVal + "").trim() : "";
                                      }
                              }


                              // 7. 校验并添加角色（关键修复：增强判空，避免undefined操作）
                              if (roleValue !== '' && genderAgeValue !== '' && genderAgeValue.indexOf('/') !== -1) {
                                      var genderAgeArr = genderAgeValue.split('/');
                                      var gender = genderAgeArr[0] ? genderAgeArr[0].trim() : "";
                                      var age = genderAgeArr[1] ? genderAgeArr[1].trim() : "";

                                      if (gender && age) {
                                              // 关键：添加到对话标签专属配置对象（键名格式统一）
                                              var charKey = "【对话 " + roleValue + "】";
                                              DUIHUA_CHARACTERS[charKey] = {
                                                      gender: gender,
                                                      age: age,
                                                      voice: roleValue // role值作为发音人标识
                                              };
                                              // 添加到全局可用发音人
                                              this.characterManager.availableVoices[roleValue] = true;
                                              var groupKey = gender + "/" + age;
                                              // 关键修复：确保duihuaVoicePool已初始化
                                              if (!this.characterManager.duihuaVoicePool) {
                                                      this.characterManager.duihuaVoicePool = {};
                                              }
                                              if (!this.characterManager.duihuaVoicePool[groupKey]) {
                                                      this.characterManager.duihuaVoicePool[groupKey] = [];
                                              }
                                              this.characterManager.duihuaVoicePool[groupKey].push(roleValue);
                      // 新增：同步构建 role→系统根节点ID 映射表
                                              roleToRootIdMap[roleValue] = roleItem.id;

                                      } else {
                                              //console.log("【handleText】❌ 跳过：性别/年龄解析失败");
                                      }
                              } else {
                                      //console.log("【handleText】❌ 跳过：角色名空或性别格式错误");
                              }
                      }

                      // 关键步骤：将对话标签配置追加到 GENSHIN_CHARACTERS（确保分配发音人时能识别）
                      for (var charKey in DUIHUA_CHARACTERS) {
                              if (DUIHUA_CHARACTERS.hasOwnProperty(charKey)) {
                                      // 避免覆盖原有配置
                                      if (!GENSHIN_CHARACTERS[charKey]) {
                                              GENSHIN_CHARACTERS[charKey] = DUIHUA_CHARACTERS[charKey];
                                      }
                              }
                      }
                      // 同步更新标签映射（让SpeechRuleJS.tags识别新发音人）
                      for (var charKey in DUIHUA_CHARACTERS) {
                              if (DUIHUA_CHARACTERS.hasOwnProperty(charKey)) {
                                      var voiceTag = DUIHUA_CHARACTERS[charKey].voice;
                                      if (!SpeechRuleJS.tags[voiceTag]) {
                                              SpeechRuleJS.tags[voiceTag] = charKey;
                                      }
                              }
                      }

                      // 最终验证
                      var allVoices = Object.keys(this.characterManager.availableVoices);
                      var duihuaRoles = allVoices.filter(function(v) {
                              return v === '青年20' || v === '幼女20';
                      });
                      //console.log("【handleText】duihua解析完成，可用发音人总数：" + allVoices.length);
                      //console.log("【handleText】包含duihua角色：" + duihuaRoles.join(','));
                      //console.log("【handleText】GENSHIN_CHARACTERS已追加对话标签配置，总数：" + Object.keys(GENSHIN_CHARACTERS).length);
              } catch (globalErr) {
                      console.error("【handleText】duihua配置解析异常：", globalErr.message);
              }
      } else {
              //console.log("【handleText】❌ 未获取到duihua配置");
      }


      // 保存可用发音人列表（duihua动态标签置顶，硬编标签后置）
      if (CONFIG.saveVoicesToFile === 1) {
          try {
              var duihuaVoices = []; // duihua动态标签（置顶）
              var hardcodeVoices = []; // 硬编标签（后置）
              
              // 遍历所有可用发音人，按类型分类
              for (var key in this.characterManager.availableVoices) {
                  if (this.characterManager.availableVoices.hasOwnProperty(key)) {
                      var voiceTag = key.toString(); // 兼容：转原始String
                      // 判断是否为duihua动态标签（通过roleToRootIdMap映射表识别）
                      var isDuihuaVoice = roleToRootIdMap.hasOwnProperty(voiceTag);
                      
                      if (isDuihuaVoice) {
                          duihuaVoices.push(voiceTag); // duihua标签加入置顶数组
                      } else {
                          hardcodeVoices.push(voiceTag); // 硬编标签加入后置数组
                      }
                  }
              }
              
              // 合并数组：duihua标签在前，硬编标签在后
              var voicesArray = duihuaVoices.concat(hardcodeVoices);
              ttsrv.writeTxtFile("fayinren.json", JSON.stringify(voicesArray, null, 2).toString()); // 兼容：转原始String
              //console.log("【发音人保存】fayinren.json已更新，duihua标签" + duihuaVoices.length + "个置顶，硬编标签" + hardcodeVoices.length + "个后置");
          } catch (saveError) {
              //console.log("【发音人保存异常】" + saveError.message);
          }
      }
      
      
      
                // ===================== 发音人 personality 全自动提取工具（有效数据过滤+二维数组）=====================
          (function extractFayinrenPersonalityAuto() {
                  var logPrefix = "[发音人Personality提取]";
          
                  // 步骤0：复用原代码中的工具函数（适配duihua的role解析）
                  var forceFlattenArray = function(arr) {
                          var result = [];
                          for (var i = 0; i < arr.length; i++) {
                                  var item = arr[i];
                                  if (Object.prototype.toString.call(item) === '[object Array]') {
                                          result = result.concat(forceFlattenArray(item));
                                  } else {
                                          result.push(item);
                                  }
                          }
                          return result;
                  };
                  var isArray = function(arr) {
                          return Object.prototype.toString.call(arr) === '[object Array]';
                  };
          
                  // 步骤1：自动读取fayinren.json纯数组标签（不变）
                  var extractAllTagsFromFayinren = function() {
                          var tags = [];
                          try {
                                  var fileContent = ttsrv.readTxtFile("fayinren.json");
                                  if (!fileContent || fileContent === "[]") {
                                          return tags;
                                  }
                                  var parsedData = JSON.parse(fileContent);
                                  if (Object.prototype.toString.call(parsedData) === "[object Array]") {
                                          var tagSet = {};
                                          for (var i = 0; i < parsedData.length; i++) {
                                                  var tag = String(parsedData[i] || "").trim();
                                                  if (tag && !tagSet[tag]) {
                                                          tagSet[tag] = true;
                                                          tags.push(tag);
                                                  }
                                          }
                                  }
                          } catch (e) {
                          }
                          return tags;
                  };
          
                  // 步骤2：100% 复用本地音效 extractByRegex 逻辑（不变）
                  var extractByRegex = function(configStr) {
                          if (typeof configStr !== "string") {
                                  configStr = String(configStr || "");
                          }
                          // 仅调试日志简化，提取逻辑不变
                          var regex = /value=([^}]+)/i;
                          var match = configStr.match(regex);
                          var personality = "";
                          if (match && match[1]) {
                                  personality = match[1].trim();
                          }
                          return personality;
                  };
          
                  // 步骤3：有效数据过滤 + 二维数组汇总
                  var allTags = extractAllTagsFromFayinren();
                  var globalTagsData = tagsData || {};
                  var personalityArray = []; // 二维数组存储有效数据
                  var successCount = 0;
          
                  // ===================== 核心修复：每个role独立匹配对应性格 =====================
                  var duihuaConfig = globalTagsData.duihua || {};
                  
                  // 1. 解析duihua的role数组（动态角色标识，如“男青年20”“女童20”）
                  var duihuaRoles = [];
                  if (duihuaConfig.role) {
                          duihuaRoles = forceFlattenArray(duihuaConfig.role);
                          duihuaRoles = forceFlattenArray(duihuaRoles);
                          if (!isArray(duihuaRoles)) duihuaRoles = [duihuaRoles];
                          // 提取每个role的value（发音人标识）
                          duihuaRoles = duihuaRoles.map(function(roleItem) {
                                  var value = "";
                                  if (typeof roleItem === 'object' && roleItem !== null) {
                                          value = roleItem.value !== undefined ? (roleItem.value + "").trim() : "";
                                          if (value === "" && typeof roleItem.get === 'function') {
                                                  var tempVal = roleItem.get("value");
                                                  value = tempVal ? (tempVal + "").trim() : "";
                                          }
                                  }
                                  return value;
                          }).filter(function(v) { return v !== ""; });
                  }
          
                  // 2. 解析duihua的personality数组（与role按索引一一对应）
                  var duihuaPersonalities = [];
                  if (duihuaConfig.personality) {
                          // 性格数组也需要扁平化（和role数组处理逻辑一致）
                          duihuaPersonalities = forceFlattenArray(duihuaConfig.personality);
                          duihuaPersonalities = forceFlattenArray(duihuaPersonalities);
                          if (!isArray(duihuaPersonalities)) duihuaPersonalities = [duihuaPersonalities];
                          // 提取每个personality的value（性格值）
                          duihuaPersonalities = duihuaPersonalities.map(function(personalityItem) {
                                  var value = "";
                                  if (typeof personalityItem === 'object' && personalityItem !== null) {
                                          value = personalityItem.value !== undefined ? (personalityItem.value + "").trim() : "";
                                          if (value === "" && typeof personalityItem.get === 'function') {
                                                  var tempVal = personalityItem.get("value");
                                                  value = tempVal ? (tempVal + "").trim() : "";
                                          }
                                  }
                                  return value;
                          });
                  }
          
                  // 3. 按索引配对：role[i] ↔ personality[i]（核心逻辑）
                  if (duihuaRoles.length > 0 && duihuaPersonalities.length > 0) {
                          for (var r = 0; r < duihuaRoles.length; r++) {
                                  var roleTag = duihuaRoles[r];
                                  // 按相同索引取性格（若性格数组长度不足，默认空）
                                  var rolePersonality = duihuaPersonalities[r] || "";
                                  
                                  // 独立有效性验证（每个role的性格单独判断）
                                  var isvalid = false;
                                  if (rolePersonality && rolePersonality !== "无") {
                                          isvalid = true;
                                  }
          
                                  if (isvalid) {
                                          // 格式：[role标签, role标签+独立性格值]
                                          personalityArray.push([roleTag, roleTag + rolePersonality]);
                                          successCount++;
                                  } else {
                                  }
                          }
                  } else if (duihuaRoles.length === 0) {
                  } else if (duihuaPersonalities.length === 0) {
                  }
                  // =============================================================================
          
                  if (allTags.length === 0 && successCount === 0) {
                          return;
                  }
          
                  // 复用本地音效for循环批量处理其他硬编标签（不变）
                  for (var i = 0; i < allTags.length; i++) {
                          var fayinrenTag = allTags[i];
                          if (fayinrenTag === "duihua") continue; // 跳过duihua标签本身
                          var tagConfig = globalTagsData[fayinrenTag] || {};
                          var personality = "";
          
                          if (tagConfig.personality) {
                                  personality = extractByRegex(tagConfig.personality);
                          } else if (tagConfig.xingge) {
                                  personality = extractByRegex(tagConfig.xingge);
                          }
          
                          var isvalid = false;
                          var invalidReason = "";
                          if (!personality) {
                                  invalidReason = "personality为空或未配置";
                          } else if (personality === "无") {
                                  invalidReason = "personality为'无'";
                          } else {
                                  isvalid = true;
                          }
          
          
                          if (isvalid) {
                                  personalityArray.push([fayinrenTag, fayinrenTag + personality]);
                                  successCount++;
                          } else {
                          }
                  }
          
                  // 保存有效数据（不变）
                  if (successCount > 0) {
                          var jsonContent = JSON.stringify(personalityArray, null, 2);
                          var fileName = "fayinren_personality_summary.json";
                          ttsrv.writeTxtFile(fileName, jsonContent);
          
                  } else {
                  }
          
          })();
          
      
      // 二次检查gengxin.json更新
      try {
          var jsonFileExists = false;
          jsonFileContent = "";
          try {
              jsonFileContent = ttsrv.readTxtFile("gengxin.json").toString(); // 兼容：转原始String
              jsonFileExists = true;
          } catch (e) {
              jsonFileExists = false;
          }
          if (jsonFileExists && jsonFileContent.trim() !== "") {
              if (typeof characterManager === 'undefined') {
                  characterManager = new CharacterManager();
                  characterManager.loadRecords();
              }
              try {
                  var updateRecords = JSON.parse(jsonFileContent.toString()); // 兼容：转原始String
                  if (Array.isArray(updateRecords)) {
                      characterManager.characterRecords = updateRecords;
                      characterManager.saveRecords();
                  }
              } catch (parseError) {
              }
          }
      } catch (e) {
      }




// -------------------------- 新增：读取yinxiao.json处理%标记（ES5兼容，复用原有方法） --------------------------
   //   //console.log("\n" + AUDIO_CONFIG.logPrefix + "===== 解析yinxiao.json中的%标记（开头%提内容，结尾%提网址） =====");
      var dynamicForbiddenChars = []; // 存储：开头% / 前后都% 的name提取内容（去%后）
      var targetExcludeUrls = [];     // 存储：结尾% / 前后都% 的item提取网址
      var yinXiaoList = [];
      var yinXiaoContent = "";
            

      text = text.replace(/“([\u4E00-\u9FFF]{1,15})”/g, "$1");



      text = text.replace(/“(((锵|咔嚓|哗啦|轰隆|咕噜|滴答|叮咚|咚咚|哐当|噼啪|扑通|吧嗒|吱呀|嘎吱|嗡嗡|喵喵|汪汪|咩咩|哞哞|呱呱|叽喳|啾啾|嘎嘎|嘶嘶|嘟嘟|嘀嘀|砰砰|乓乓|噼里啪啦|稀里哗啦|丁零当啷|叽里咕噜|乒乒乓乓|淅淅沥沥|窸窸窣窣|滴滴答答|叮叮当当|轰轰隆隆|咕咕噜噜|噼噼啪啪|吱吱呀呀|哔哔剥剥|咔咔嚓嚓|扑扑簌簌|踢踢踏踏|咕嘟咕嘟|呼哧呼哧|咯吱咯吱|当啷当啷|哗啦哗啦|沙沙|唰唰|淅沥|咕咚|啪嗒|骨碌碌|轰|咚|唰|砰|铛|咣|咻|嗖|嘭|嚓|咣当|咕嘟|咕隆|哗|唧唧|喳喳|呱嗒|嗒嗒|哒哒|铮铮|铮|嗡|呲|呲啦|咝|咝咝|呜|呜呜|呼呼|飕飕|轰隆隆|咕噜噜|叮铃铃|嘀铃铃|嘀嗒嗒|哐啷|哐啷啷|啪嚓|啪嗒|骨碌|咕噜|咕咕|笃笃|笃|嗒|嘎|嘎嘎|嘎啦|嘎嘣|嘣|嘣嘣|噔|噔噔|噔噔噔|噗|噗噗|噗噜噜|哧|哧溜|哧啦|当|当当|哔|哔哔|哔剥|剥|剥剥|咿呀|咿咿呀呀|吱|吱吱|吱扭|吱嘎|轧轧|轧|轧然|霍霍|霍|霍啦|飕|飕飕|飒飒|飒|萧萧|萧|簌簌|簌|咕|咕咕|咕儿|呱|呱呱|呱唧|唧|唧唧|唧咕|啾|啾啾|啾唧|啁啾|啁|啁啁|嘤|嘤嘤|嗡|嗡嗡|嗡营|营营|铮|铮铮|铮鏦|鏦|鏦然|叮|叮叮|叮当|叮咚|叮铃|铃|铃铃|泠泠|淙淙|潺潺|溅溅|汩汩|咕嘟|咕嘟咕嘟|哗|哗哗|哗啦|哗啦啦|澎|澎湃|澎澎|汹|汹涌|汹汹|轧|轧轧|轧然|吱|吱吱|吱扭|嘎|嘎吱|嘎巴|嘎嘣|嘣|嘣嘣|啪|啪啪|啪嚓|啪嗒|嗒|嗒嗒|哒|哒哒|咚|咚咚|噔|噔噔|噗|噗通|噗嗤|嗤|嗤嗤|嗤啦|咝|咝咝|咻|咻咻|嗖|嗖嗖|飕|飕飕|呜|呜呜|呼|呼呼|呼啦|呼啦啦|哗|哗啦|哗啦啦|咕|咕噜|咕咚|咕嘟|嘟|嘟嘟|嘟噜|噜|噜噜|哞|哞哞|咩|咩咩|喵|喵喵|汪|汪汪|嗷|嗷嗷|咯|咯咯|咯吱|吱|吱吱|呱|呱呱|叽|叽叽|喳|喳喳|啾|啾啾|嘶|嘶嘶|吼|吼吼|唳|唳唳|吠|汪汪|嗡|嗡嗡|营|营营|铮|铮铮|叮|叮当|叮咚|当|当当|哐|哐当|砰|砰砰|乓|乓乓|咣|咣当|嚓|咔嚓|啪|啪嗒|嗒|嗒嗒|嘀|嘀嗒|嗒|嗒嗒|哒|哒哒|嘟|嘟嘟|哔|哔哔|噗|噗噗|哧|哧哧|咝|咝咝|唰|唰唰|淅沥|沥沥|沥|沙|沙沙|飒|飒飒|萧|萧萧|簌|簌簌|哗|哗哗|轰|轰轰|咕|咕咕|咚|咚咚|吱|吱吱|嘎|嘎嘎|当|当当|乓|乓乓|砰|砰砰|啪|啪啪|哐|哐哐|咣|咣咣|叮|叮叮|铮|铮铮|嗡|嗡嗡|嘟|嘟嘟|哔|哔哔|噗|噗噗|哧|哧哧|咻|咻咻|嗖|嗖嗖|飕|飕飕|呜|呜呜|呼|呼呼|哗|哗哗|轰|轰轰|咕|咕咕|咚|咚咚|吱|吱吱|嘎|嘎嘎|咯噔|咕叽|咕叽咕叽|咕噜咕噜|哗啦啦|噼啪|噼噼啪啪|咚咚咚|哐哐|咣咣|叮叮当|叮叮咚咚|吱嘎吱嘎|吱呀呀|轰隆轰隆|咕咚咕咚|吧嗒吧嗒|嘀嗒嘀嗒|沙沙沙|飒飒飒|嗡嗡嗡|喵呜|汪汪汪|咩咩咩|哞哞哞|呱呱呱|叽叽叽|喳喳喳|啾啾啾|嘶嘶嘶|呼呼呼|呜呜呜|哒哒哒|嗒嗒嗒|砰砰砰|乓乓乓|嚓嚓嚓|唰唰唰|淅沥沥|哗哗哗|咕咕咕|咚咚咚|吱吱吱|嘎嘎嘎|当当当|铮铮铮|噗噗噗|哧哧哧|咻咻咻|嗖嗖嗖|飕飕飕|哐当哐当|咕噜咕噜|噼里啪啦轰隆隆|稀里哗啦丁零当啷|叽里咕噜乒乒乓乓|窸窸窣窣滴滴答答|叮叮当当轰轰隆隆|噼噼啪啪吱吱呀呀|哔哔剥剥咔咔嚓嚓|扑扑簌簌踢踢踏踏|咕嘟咕嘟呼哧呼哧|咯吱咯吱当啷当啷|哗啦哗啦唧唧喳喳|呱嗒呱嗒铮铮作响|咣当咣当扑通扑通|吧唧吧唧咕叽咕叽|沙啦沙啦飒啦飒啦|簌啦簌啦霍啦霍啦|咝啦咝啦哧溜哧溜|嘟噜嘟噜哔剥哔剥|噼啪噼啪咔嚓咔嚓|轰隆轰隆咕咚咕咚|叮咚叮咚嘀嗒嘀嗒|哗啦哗啦呼啦呼啦|吧嗒吧嗒啪嗒啪嗒|吱呀吱呀嘎吱嘎吱|嗡嗡嗡嗡喵喵喵喵|汪汪汪汪咩咩咩咩|哞哞哞哞呱呱呱呱|叽叽叽叽喳喳喳喳|啾啾啾啾嘶嘶嘶嘶|呼呼呼呼呜呜呜呜|咚咚咚咚吱吱吱吱|嘎嘎嘎嘎当当当当|铮铮铮铮噗噗噗噗|哧哧哧哧咻咻咻咻|嗖嗖嗖嗖飕飕飕飕|哐哐哐哐咣咣咣咣|嚓嚓嚓嚓唰唰唰唰|淅沥淅沥哗哗哗哗|咕咕咕咕咚咚咚咚|噼里啪啦稀里哗啦|丁零当啷叽里咕噜|乒乒乓乓淅淅沥沥|窸窸窣窣滴滴答答|叮叮当当轰轰隆隆|噼噼啪啪吱吱呀呀|哔哔剥剥咔咔嚓嚓|扑扑簌簌踢踢踏踏|咕嘟咕嘟呼哧呼哧|咯吱咯吱当啷当啷)([！？。，；：、]*)){1,3})”/g, '$1');



     








      // 确保CharacterManager已初始化
      if (typeof characterManager === 'undefined') {
          characterManager = new CharacterManager();
          characterManager.loadRecords();
      }
      var originalText = text.toString(); // 兼容：转原始String

      // -------------------------- 文本分割与对话处理（含100个本地音效去标签） --------------------------
      var splitText = this.fx(text.toString()); // 兼容：转原始String
      // 还原引号符号
      splitText = splitText.replace(/###LEFT_QUOTE###/g, "“").toString();
      splitText = splitText.replace(/###RIGHT_QUOTE###/g, "”").toString();
      text = splitText.split("\n");

      var list = [];
      var allDialogues = [];
      this.characterManager.updateContext(text2);

      // 收集对话（过滤音效标记）

      // 修复后：收集对话（过滤音效标记+占位符，确保id唯一）
      for (var i = 0; i < text.length; i++) {
          var tmpStr = text[i] ? text[i].trim().toString() : ""; 
          if (!tmpStr) continue;
          // 1. 新增：排除音效保护占位符（###PROTECTED_XXX###）
          if (tmpStr.indexOf("〖") !== -1 || tmpStr.indexOf("{{") !== -1 || tmpStr.indexOf("###PROTECTED_") !== -1) continue;
          if (tmpStr.indexOf("“") === 0) {
              var match = tmpStr.match(/【(.*?)】/);
              if (match && match[1]) {
                  var dialogueId = match[1].toString();
                  // 2. 新增：校验id唯一性，避免重复添加
                  var isIdDuplicate = allDialogues.some(item => item.id === dialogueId);
                  if (!isIdDuplicate) {
                      allDialogues.push({ 
                          id: dialogueId, 
                          text: tmpStr.toString(), 
                          index: i 
                      });
                  }
              }
          }
      }
      
      // 生成100个本地音效标签数组（含localSound1~100）
      var allLocalSoundTags = (function() {
          var tagsArr = ["localSound1", "localSound2", "localSound3"];
          // 新增：添加localSound4~100（97个）
          for (var num = 4; num <= 990; num++) {
              tagsArr.push(("localSound" + num).toString()); // 兼容：转原始String
          }
          return tagsArr;
      })();

      // 逐行处理：去标签+还原内容（覆盖100个本地音效）
      for (var i = 0; i < text.length; i++) {
          var tmpStr = text[i] ? text[i].trim().toString() : ""; // 兼容：转原始String
          
          if (!tmpStr) {
              continue;
          }

          // 步骤1：去除100个本地音效标签
          var originalTextLine = tmpStr;
          var newContentWithTag = "";
          var targetTagKey = null;
          for (var j = 0; j < allLocalSoundTags.length; j++) {
              var tagKey = allLocalSoundTags[j].toString(); // 兼容：转原始String
              var tagName = this.tags[tagKey] ? this.tags[tagKey].toString() : tagKey;
              // 匹配标签格式：{{本地音效X_内容}}内容{{本地音效X结束}}
              var escapedTagName = escapeRegExp(tagName);
              var tagReg = new RegExp("(\\{\\{" + escapedTagName + "_([\\s\\S]*?)\\}\\}([\\s\\S]*?)\\{\\{" + escapedTagName + "结束\\}\\})", 'g');
              var tagMatch = tmpStr.match(tagReg);

              if (tagMatch && tagMatch.length > 0) {
                  newContentWithTag = tagMatch[0].toString(); // 兼容：转原始String
                  targetTagKey = tagKey;
                  originalTextLine = tmpStr.replace(tagReg, "").trim().toString(); // 兼容：转原始String
                  break;
              }
          }

          // 步骤2：处理原内容（还原符号+分配标签）
          if (originalTextLine) {
              var originalItem = {};
              var restoredText = this.restoreTargetContentSymbols(originalTextLine.toString()); // 兼容：转原始String

              // 括号发音人处理
              if (restoredText.indexOf("【括号1】") === 0) {
                  originalItem = { 
                      text: restoredText.replace("【括号1】", "").toString(), 
                      tag: "括号1" 
                  };
              } else if (restoredText.indexOf("〖括号2】") === 0) {
                  originalItem = { 
                      text: restoredText.replace("〖括号2】", "").toString(), 
                      tag: "括号2" 
                  };
              } else if (restoredText.indexOf("「括号3】") === 0) {
                  originalItem = { 
                      text: restoredText.replace("「括号3】", "").toString(), 
                      tag: "括号3" 
                  };
              } else if (restoredText.indexOf("『括号4】") === 0) {
                  originalItem = { 
                      text: restoredText.replace("『括号4】", "").toString(), 
                      tag: "括号4" 
                  };
              } 
              // 在线音效处理
              else if (restoredText.indexOf("〖") !== -1) {
                  originalItem = { text: restoredText.toString(), tag: "narration" };
              } 
              // 对话处理（API分配角色）
              
              
                
              // 对话处理（新增旁白缓存校验逻辑）
              else if (restoredText.indexOf("“") === 0) {
                // ========== 新增：核心校验逻辑 开始 ==========
                // 1. 读取缓存中的旁白条目列表
                var cacheNarrationList = getCacheNarrationList();
                var isMisplacedQuote = false;
                // 2. 缓存中有旁白条目，执行匹配校验
                if (cacheNarrationList.length > 0) {
                  // 复用原有全局统一的文本清理规则，保证匹配一致性
                  var cleanCurrent = cleanDialogText(restoredText);
                  if (cleanCurrent !== "") {
                    // 遍历所有缓存旁白条目，复用原有单行匹配逻辑
                    for (var nIdx = 0; nIdx < cacheNarrationList.length; nIdx++) {
                      var narrationItem = cacheNarrationList[nIdx];
                      // 匹配上任意一行，即判定为双引号标错
                      if (matchSingleLine(restoredText, narrationItem)) {
                        isMisplacedQuote = true;
                        break;
                      }
                    }
                  }
                }
                // 3. 匹配成功：判定为双引号标错，强制打旁白标签，跳过对话处理
                if (isMisplacedQuote) {
                  originalItem = { 
                    text: restoredText.replace(/^(“?)【\d+】/, "$1").toString(), 
                    tag: "narration" 
                  };
                } 
                // ========== 新增：核心校验逻辑 结束 ==========
                // 4. 未匹配到旁白，完全走原有对话处理逻辑，零改动
                else {
                  var dialogMatch = restoredText.match(/【(.*?)】/);
                  // 安全赋值章节内容，完全保留原逻辑
                  var chapterFullContent = "";
                  if (text && Array.isArray(text) && typeof text.join === "function") {
                    chapterFullContent = text.join("\n");
                  }
                  if (next100Chars && next100Chars.trim()) {
                    chapterFullContent += "\n" + next100Chars;
                  }
                  if (dialogMatch && dialogMatch[1]) {
                    var apiResult = this.characterManager.processCharacter(
                      restoredText.toString(), 
                      dialogMatch[1].toString(), 
                      allDialogues,
                      chapterFullContent
                    );
                    
                    if (apiResult) {
                      apiResult.text = this.restoreTargetContentSymbols(apiResult.text.toString());
                      var roleName = apiResult.tag.toString();
                      if (roleToRootIdMap.hasOwnProperty(roleName)) {
                        var rootId = roleToRootIdMap[roleName] || "0";
                        originalItem = { 
                          text: apiResult.text.toString(), 
                          tag: "duihua", 
                          id: rootId 
                        };
                      } else {
                        originalItem = { 
                          text: apiResult.text.toString(), 
                          tag: roleName 
                        };
                      }
                    } else {
                      originalItem = { 
                        text: restoredText.replace(/^(“?)【\d+】/, "$1").toString(), 
                        tag: "duihua" 
                      };
                    }
                  } else {
                    originalItem = { 
                      text: restoredText.replace(/^(“?)【\d+】/, "$1").toString(), 
                      tag: "duihua" 
                    };
                  }
                }
              }
              
              // 旁白处理

              else {
                // 新增：先匹配缓存里的对话，匹配成功用对应角色，失败才用旁白
                var narrationMatchResult = matchNarrationFromCache(restoredText.toString());
                if (narrationMatchResult && narrationMatchResult.voice) {
                    var targetVoice = narrationMatchResult.voice.toString();
                    // 核心修复：兼容duihua动态发音人，和对话处理逻辑保持一致
                    if (roleToRootIdMap.hasOwnProperty(targetVoice)) {
                        // 是duihua动态发音人，按系统要求设置tag和id
                        var rootId = roleToRootIdMap[targetVoice] || "0";
                        originalItem = { 
                            text: restoredText.toString(), 
                            tag: "duihua", 
                            id: rootId 
                        };
                    } else {
                        // 是硬编发音人，直接使用原voice作为tag
                        originalItem = { 
                            text: restoredText.toString(), 
                            tag: targetVoice 
                        };
                    }
                } else {
                    // 匹配失败，保留原旁白逻辑
                    originalItem = { text: restoredText.toString(), tag: "narration" };
                }
              }
            
              list.push(originalItem);
          }

          // 步骤3：处理音效内容（添加到结果列表）
          if (newContentWithTag && targetTagKey) {
              var cleanNewContent = newContentWithTag
                  .replace(/\{\{.*?\}\}([\s\S]*?)\{\{.*?结束\}\}/, "$1")
                  .trim()
                  .toString(); // 兼容：转原始String
              cleanNewContent = this.restoreTargetContentSymbols(cleanNewContent.toString());
              var newItem = { 
                  text: cleanNewContent.toString(), 
                  tag: targetTagKey.toString() 
              };
              list.push(newItem);
          }

      }

      // 输出处理结果总览
      for (var k = 0; k < list.length; k++) {
          var item = list[k];
          var itemType = item.tag.indexOf("localSound") === 0 ? "【本地音效】" : 
                        item.tag.indexOf("括号") === 0 ? "【括号发音】" : 
                        item.tag === "duihua" ? "【对话】" : "【旁白】";
      }
      return list;
  },

  // -------------------------- fx分割函数（ES5兼容，支持100个音效） --------------------------
  fx: function(input) {
      if (!input) return "";
      input = input.toString(); // 兼容：转原始String
      // 分割特殊符号内容
      input = input.replace(/【(.*?)】/g, "\n【括号1】$1\n").toString();
      input = input.replace(/〖(.*?)〗/g, "\n〖括号2】$1\n").toString();
      input = input.replace(/「(.*?)」/g, "\n「括号3】$1\n").toString();
      input = input.replace(/『(.*?)』/g, "\n『括号4】$1\n").toString();
      
      var counter = 1;
      // 分割对话内容（双引号包裹）
      input = input.replace(/(["“])(.*?)(["”])/g, function(match, p1, p2, p3) {
          match = match ? match.toString() : "";
          p1 = p1 ? p1.toString() : "";
          p2 = p2 ? p2.toString() : "";
          p3 = p3 ? p3.toString() : "";
          return ("\n" + p1 + "【" + (counter++) + "】" + p2 + p3 + "\n").toString();
      });

      return input.toString();
  },

  // -------------------------- 符号替换工具（100个音效通用） --------------------------
  replaceTargetContentSymbols: function(targetStr) {
      targetStr = targetStr ? targetStr.toString() : "";
      return targetStr
          .replace(/“/g, "###LEFT_QUOTE###").toString()
          .replace(/”/g, "###RIGHT_QUOTE###").toString()
          .replace(/〖/g, "###LEFT_DOUBLE_ANGLE###").toString()
          .replace(/〗/g, "###RIGHT_DOUBLE_ANGLE###").toString()
          .replace(/【/g, "###LEFT_SQUARE###").toString()
          .replace(/】/g, "###RIGHT_SQUARE###").toString()
          .replace(/『/g, "###LEFT_DOUBLE_CURLY###").toString()
          .replace(/』/g, "###RIGHT_DOUBLE_CURLY###").toString()
          .replace(/「/g, "###LEFT_SINGLE_ANGLE###").toString()
          .replace(/」/g, "###RIGHT_SINGLE_ANGLE###").toString();
  },
  restoreTargetContentSymbols: function(text) {
      text = text ? text.toString() : "";
      return text
          .replace(/###LEFT_QUOTE###/g, "“").toString()
          .replace(/###RIGHT_QUOTE###/g, "”").toString()
          .replace(/###LEFT_DOUBLE_ANGLE###/g, "〖").toString()
          .replace(/###RIGHT_DOUBLE_ANGLE###/g, "〗").toString()
          .replace(/###LEFT_SQUARE###/g, "【").toString()
          .replace(/###RIGHT_SQUARE###/g, "】").toString()
          .replace(/###LEFT_DOUBLE_CURLY###/g, "『").toString()
          .replace(/###RIGHT_DOUBLE_CURLY###/g, "』").toString()
          .replace(/###LEFT_SINGLE_ANGLE###/g, "「").toString()
          .replace(/###RIGHT_SINGLE_ANGLE###/g, "」").toString();
  }
};

// -------------------------- 模块导出（手机端ES5兼容） --------------------------
if (typeof module !== 'undefined' && module.exports) {
  module.exports = SpeechRuleJS;
} else {
  this.SpeechRuleJS = SpeechRuleJS;
}

// -------------------------- 辅助函数（支持100个本地音效查询） --------------------------
function printAvailableVoices() {
  var output = "=== 可用发音人（含100个本地音效） ===".toString();
  var voices = [];
  if (characterManager && characterManager.availableVoices) {
      for (var key in characterManager.availableVoices) {
          if (characterManager.availableVoices.hasOwnProperty(key)) {
              voices.push(key.toString()); // 兼容：转原始String
          }
      }
  }
  if (voices.length === 0) {
      output += "\n⚠️  无可用发音人";
  } else {
      for (var i = 0; i < voices.length; i++) {
          var voiceTag = voices[i].toString();
          var voiceName = SpeechRuleJS.tags[voiceTag] ? SpeechRuleJS.tags[voiceTag].toString() : voiceTag;
          output += "\n" + (i+1) + ". " + voiceTag + "（" + voiceName + "）";
      }
  }
  return output;
}

function qjs() {
  var output = "=== 角色统计 ===".toString();
  if (!characterManager || !Array.isArray(characterManager.characterRecords)) {
      return output + "\n⚠️  角色管理器未初始化或无角色记录";
  }
  var records = characterManager.characterRecords;
  if (records.length === 0) {
      return output + "\n⚠️  无角色记录";
  }
  for (var i = 0; i < records.length; i++) {
      var r = records[i];
      if (!r) {
          output += "\n" + (i+1) + ". 无效角色记录（空值）";
          continue;
      }
      var name = r.name ? r.name.toString() : "未知角色";
      var gender = r.gender ? r.gender.toString() : "未知";
      var age = r.age ? r.age.toString() : "未知";
      var voice = r.voice ? r.voice.toString() : "未分配";
      var usage = r.usageCount || 0;
      output += "\n" + (i+1) + ". " + name + "（" + gender + "/" + age + "）";
      output += "\n   发音人：" + voice + " | 剩余评估：" + usage + "次";
  }
  return output;
}

function showConfig() {
  var output = "=== 运行配置 ===".toString();
  if (typeof CONFIG === 'undefined') {
      return output + "\n⚠️  配置对象未定义";
  }
  for (var key in CONFIG) {
      if (CONFIG.hasOwnProperty(key)) {
          output += "\n" + key + "：" + CONFIG[key].toString(); // 兼容：转原始String
      }
  }
  output += "\nAPI模型：" + CONFIG.apiModel.toString();
  return output;
}

function setFixedVoice(characterName) {
  if (!characterManager) return "❌ 角色管理器未初始化";
  var charName = characterName ? characterName.toString().trim() : "";
  if (!charName) return "❌ 角色名不能为空";
  var record = characterManager.findCharacterRecord(charName);
  if (record) {
      record.usageCount = 100;
      characterManager.saveRecords();
      return "✅ 固定" + charName + "发音人：" + record.voice.toString();
  } else {
      return "❌ 未找到角色：" + charName;
  }
}

function setFixedGenderAge(characterName) {
  if (!characterManager) return "❌ 角色管理器未初始化";
  var charName = characterName ? characterName.toString().trim() : "";
  if (!charName) return "❌ 角色名不能为空";
  var record = characterManager.findCharacterRecord(charName);
  if (record) {
      record.usageCount = 50;
      characterManager.saveRecords();
      return "✅ 固定" + charName + "性别年龄：" + record.gender.toString() + "/" + record.age.toString();
  } else {
      return "❌ 未找到角色：" + charName;
  }
}

// -------------------------- 初始化（含100个本地音效注册） --------------------------
try {
  if (typeof characterManager === 'undefined') {
      characterManager = new CharacterManager();
  }
  characterManager.loadRecords();
} catch (e) {
  characterManager = new CharacterManager();
}

// 注册100个本地音效标签（确保选择后显示输入框）
(function() {
  if (typeof SpeechRuleJS !== 'undefined' && typeof SpeechRuleJS.tags === 'object') {
      for (var num = 4; num <= 990; num++) {
          var tagKey = ("localSound" + num).toString();
          var tagName = ("本地音效" + num).toString();
          SpeechRuleJS.tags[tagKey] = tagName;
      }
  }
})();

// 打印可用发音人（含100个本地音效）
printAvailableVoices();
