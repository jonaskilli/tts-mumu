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


var WAIT_API_RESULT_COUNT = 5; // 默认与原逻辑完全兼容

var rizhi = 1;//等于0 关闭投票日志，等于1 开启投票日志。



var bingfa = 1;//默认3并发数


var xiawen = 1300;//   字数越大缓存越多。

var shouci = 800;//   首次使用缓存字数，只在首次使用。

var NAME_ANALYZE_NEXT_CONTEXT_EXTRA_MAX = 3000; // 姓名分析下文在基础截取之后最多再外延3000个字符，同时不能超过现有已加载文本末尾

var SEQ_ADD_RATIO = 1; // 总引号数＞5时生效：总左引号数量 × 该比例 = 最终添加序号的数量，取整数

var NEXT_CHAPTER_COUNT = 3; // 0=仅本章，1=本章+后1章，2=本章+后2章.

var xiawens = xiawen; // 保存初始的下文长度默认配置
// 时间记录变量：初始化当前时间减2小时，精确到分钟
var shijian = new Date(Date.now() - 2 * 60 * 60 * 1000);
shijian.setSeconds(0);
shijian.setMilliseconds(0);

// 姓名性别年龄分析API：单独超时时间（不修改则默认使用全局超时）
var NAME_ANALYZE_TIMEOUT = 120000;
// 别名校验分析API：单独超时时间（不修改则默认使用全局超时）
var ALIAS_ANALYZE_TIMEOUT = 120000;




// ===================== 新增：投票别名合并开关 =====================
// 0 = 关闭别名合并（原逻辑，按API返回的原始名字投票）
// 1 = 开启别名合并（匹配本地角色主名/别名，同一主名视为同一人）
var ENABLE_ALIAS_VOTE_MERGE = 1;
// ===================== 别名图谱/共现/远程观察配置 =====================
var ENABLE_ALIAS_GRAPH = 1;
var ENABLE_ALIAS_POSITIVE_GRAPH = 1;
var ENABLE_ALIAS_NEGATIVE_GRAPH = 1;
var ENABLE_ALIAS_COOCUR_STATS = 1;
var GRAPH_POSITIVE_HINT_MIN = 1.5;
var GRAPH_NEGATIVE_SOFT_BLOCK = 1.0;
var GRAPH_NEGATIVE_HARD_BLOCK = 4.0;
var COOCUR_MAX_NAMES = 50;
var COOCUR_MAX_SENTENCES = 260;
var COOCUR_NEG_SENTENCE_MIN = 2;
var COOCUR_NEG_ADJACENT_MIN = 2;
var GRAPH_SIMPLE_LOG = 1; // 新增日志只打印中文短句
var ENABLE_REMOTE_UPLOAD = 0; // 远程上传总开关：0=关闭，1=开启
var ENABLE_GRAPH_REMOTE_UPLOAD = 0; // 图谱远程上传开关
var GRAPH_REMOTE_ENDPOINT = "https";
var GRAPH_REMOTE_MAX_QUEUE = 3000;
var GRAPH_REMOTE_EDGE_LIMIT = 30;
var GRAPH_REMOTE_TOKEN = "";
var GRAPH_REMOTE_QUEUE_FILE = "graph_remote_chapter_queue.json";
var ENABLE_MODEL_RELATION_EVIDENCE = 1;
var ENABLE_GRAPH_CHAPTER_DEDUP = 1; // 同章同人物同原因只累计一次图谱分
var GRAPH_CHAPTER_EVIDENCE_MAX = 3000; // 章节证据去重缓存上限
var ENABLE_GRAPH_GROUP_NAME_FILTER = 1;
var ENABLE_ALIAS_GROUP_MEMBER_MERGE_BLOCK = 1; // \u5355\u4eba\u4e0e\u7fa4\u4f53\u79f0\u547c\u7981\u6b62\u4e92\u5e76
var ENABLE_GRAPH_POSITIVE_CHAIN_CLOSURE = 1; // 正向同人链路闭合：0=关闭，1=开启
var GRAPH_POSITIVE_CHAIN_SCORE = 1.2; // 正向链路追加正证分
var GRAPH_CLOSURE_MAX_NEIGHBORS = 80; // 单次闭合最多扫描邻居数
var ENABLE_GRAPH_BOOK_CACHE = 1; // graph files per book
var ENABLE_GRAPH_CONFLICT_MODEL_VERIFY = 1; // 正反图谱冲突时调用模型复核并修正
var GRAPH_CONFLICT_VERIFY_TIMEOUT = 45000;
var GRAPH_CONFLICT_EMPTY_CHOICES_RETRY_MAX = 2; // 冲突校验遇到空choices/缺message时额外重试次数，防止瞬时空响应直接失败
var GRAPH_CONFLICT_VERIFY_MIN_CONFIDENCE = 80;
var GRAPH_CONFLICT_POSITIVE_MIN = GRAPH_POSITIVE_HINT_MIN;
var GRAPH_CONFLICT_NEGATIVE_MIN = GRAPH_NEGATIVE_SOFT_BLOCK;
var GRAPH_CONFLICT_VERIFY_FIX_SCORE = 4.5;

// ===================== 别名校验最近N章三维辅助配置 =====================
// 目的：给别名校验模型补充最近N章的正图谱、反图谱、共现统计三维证据；角色列表默认不进入prompt。
var ENABLE_ALIAS_RECENT_CHAPTER_HINT = 1; // 0=关闭最近N章辅助，1=开启
var ALIAS_RECENT_CHAPTER_RANGE = 5; // 别名校验携带最近N章辅助数据
var ALIAS_RECENT_CHAPTER_MARK_LIMIT = 60; // 每个角色/边/共现pair最多保留章节标记数量，防膨胀
var ALIAS_RECENT_ROLE_LIMIT = 80; // 最近N章角色列表最多输出数量（可按书籍角色密度调大/调小）
var ALIAS_RECENT_GRAPH_POS_LIMIT = 40; // 最近N章正图谱最多输出边数（同人/别名证据，默认加大）
var ALIAS_RECENT_GRAPH_NEG_LIMIT = 60; // 最近N章反图谱最多输出边数（非同人/互动反证，默认加大）
var ALIAS_RECENT_COOCUR_LIMIT = 80; // 最近N章共现统计最多输出pair数（默认加大，给模型更宽视角）
var ALIAS_RECENT_GRAPH_REASON_LIMIT = 6; // 最近N章正反图谱每条边最多输出原因数量
var ALIAS_RECENT_GRAPH_EXTRA_MAX_LEN = 340; // 最近N章正反图谱每条边证据文本最大字符数
var ALIAS_RECENT_COOCUR_EVIDENCE_LIMIT = 4; // 最近N章共现统计每个pair最多输出证据样例数量
var ALIAS_RECENT_COOCUR_EVIDENCE_STORE_LIMIT = 12; // 每个共现pair最多保存证据样例数量，防膨胀
var ALIAS_RECENT_COOCUR_EVIDENCE_MAX_LEN = 180; // 共现证据样例最大字符数
var ENABLE_ALIAS_RECENT_ROLE_LIST = 0; // 0=别名校验不输出最近N章角色列表，1=输出最近N章角色列表
var ENABLE_ALIAS_REFINE_GRAPH_HINT = 1; // 0=关闭别名清洗局部三维辅助，1=开启（只围绕主名、旧别名、新名字）
var ALIAS_REFINE_GRAPH_HINT_PAIR_LIMIT = 30; // 别名清洗局部三维辅助最多输出pair数量，防止prompt过长
var ALIAS_REFINE_GRAPH_HINT_EVIDENCE_MAX_LEN = 220; // 别名清洗局部三维辅助单条证据最大字符数
var ENABLE_ALIAS_RAW_REMOTE_LOG = 1; // 远程日志记录别名校验原始请求/原始返回（需远程上传总开关开启）
var ALIAS_RAW_REMOTE_LOG_MAX_LEN = 12000; // 原始请求/返回日志最大字符数
var ENABLE_MODEL_RAW_REMOTE_LOG = 1; // 远程日志记录批量姓名分析/别名清洗/冲突校验原始请求与返回；只写远程日志，不在App界面打印
var MODEL_RAW_REMOTE_LOG_MAX_LEN = ALIAS_RAW_REMOTE_LOG_MAX_LEN; // 其他模型原始请求/返回日志最大字符数，默认复用别名校验长度
var ENABLE_NAME_ANALYSIS_RECENT_ROLE_HINT = 1; // 批量姓名分析携带最近N章/跨章召回的已知角色姓名复用表：0=关闭，1=开启
var NAME_ANALYSIS_RECENT_ROLE_RANGE = 5; // 批量姓名分析已知角色姓名复用表取最近N章
var NAME_ANALYSIS_RECENT_ROLE_LIMIT = 80; // 批量姓名分析已知角色姓名复用表最多输出角色数量，防止prompt膨胀
var NAME_ANALYSIS_RECENT_ALIAS_LIMIT = 8; // 批量姓名分析已知角色姓名复用表每个角色最多输出别名数量
var ENABLE_NAME_ANALYSIS_CROSS_CHAPTER_ROLE_HIT = 1; // 批量姓名分析：开启跨章命中补强，只补当前批文本明确命中的历史角色
var NAME_ANALYSIS_CROSS_CHAPTER_ROLE_LIMIT = 5; // 跨章补强最多补入角色数量，避免prompt膨胀
var NAME_ANALYSIS_CROSS_CHAPTER_WEAK_ALIAS_FILTER = 1; // 跨章补强过滤泛称/代词/群体/纯职务称谓
var ENABLE_NAME_ANALYSIS_CROSS_CHAPTER_GENERIC_MAIN_FILTER = 1; // 跨章补强额外过滤泛称主名，避免“两女/二老”等旧角色主名被召回
var ENABLE_RELATION_DESCRIPTOR_POSITIVE_BLOCK = 1; // 关系/身份描述禁止写入正图谱，避免“同一关系描述”触发误合并
var ENABLE_NARRATOR_SYSTEM_PRESERVE_FIX = 1; // 模型已判定为旁白时，防止后续映射兜底落到系统
var ENABLE_SPECIAL_SPEAKER_BYPASS = 1; // 旁白/系统等特殊说话人不进入角色列表、不走别名校验、不写人物图谱
var ENABLE_NARRATION_OBJECT_NAME_FIX = 1; // 模型已判定age=旁白时，禁止把物品/地点/事件名当作旁白角色名入库
var ENABLE_NAME_ANALYSIS_PARSED_RESULT_LOG = 1; // 远程记录批量姓名分析解析摘要，便于观察角色复用和特殊说话人
var ENABLE_NAME_ANALYSIS_CACHE_TRACE = 1; // 姓名分析对白缓存命中、未命中、写入和序号映射远程观察开关
var NAME_ANALYSIS_CACHE_TRACE_TEXT_MAX = 220; // 每条姓名分析缓存观察日志最多记录220字原文，避免远程日志膨胀
var ENABLE_REMOTE_LOG_STATE_COALESCE = 1; // 相同章节内重复的状态型远程事件合并计数；请求、响应、重试、对齐和最终裁决不合并
var ENABLE_ALIAS_CHECK_REASON_CONSISTENCY = 1; // 别名校验isAlias与reason自相矛盾时阻止直接合并
var ENABLE_VOICE_AGE_EVIDENCE = 1; // 批量姓名分析额外返回“发声音龄”原文证据
var ENABLE_VOICE_AGE_AUDIT = 1; // 发声音龄证据必须通过独立审计后才能生效
var COMBINED_EVIDENCE_AUDIT_RETRY_MAX = 0; // 0=沿用现有API重试轮数；大于0时可单独限制合并审计轮数
var ENABLE_TEMPORARY_VOICE_STATE = 1; // 开启临时伪装/换声状态机；临时状态只覆盖朗读标签，不写角色卡
var ENABLE_TEMPORARY_VOICE_CACHE_RESTORE = 1; // 允许在同书、同章、连续缓存序号和对白哈希均匹配时恢复临时状态
var ENABLE_TEMPORARY_VOICE_CROSS_CHAPTER = 1; // 同书且顺序进入下一章时允许携带已审计临时状态；跳章、回退、换书仍清理
var TEMPORARY_VOICE_MAX_ROLE_DIALOGUES = 30; // 临时状态最多覆盖该角色30句对白，仅作为漏掉结束信号时的最后保险
var VOICE_AGE_EVIDENCE_TEXT_MAX = 260; // 证据原文及远程日志的单字段长度上限
var ENABLE_VOICE_AGE_BOOK_CACHE = 1; // 年龄证据按书籍和规则数据版本独立持久化，供后续升级复用
var ENABLE_AGE_VOICE_BINDING_BACKUP = 1; // 审计确认的自然年龄段切换前备份旧年龄段与音色绑定
var AGE_VOICE_BINDING_BACKUP_LIMIT = 12; // 每个角色最多保留年龄段音色绑定备份数
var ENABLE_FIXED_VOICE_HARD_LOCK = 1; // 显式固定发音人硬锁最终朗读标签，自然年龄更新和临时换声均不得覆盖
var ENABLE_MAIN_ROLE_VOICE_AUTO_LOCK = 1; // 检测到男主/女主音色时自动视为锁定自然音色
var ENABLE_FIXED_VOICE_EXPLICIT_LOCK_ALL_ROLES = 1; // 普通角色/系统角色/特殊角色只要有显式固定标记，也一律锁定自然音色
var ENABLE_LEGACY_USAGE100_VOICE_LOCK_MIGRATION = 0; // 兼容旧版仅usageCount=100的固定音色；默认关闭

// ===================== 模型语义证据评分配置 =====================
// 本地封闭式同人/非同人结构已完全移出本地；此类证据由批量姓名分析 __relations 返回，并交由别名/证据审计模型裁决。
var GRAPH_MODEL_NAME_IDENTITY_SCORE = 5.0; // 模型审计采纳的同人/别名证据
var GRAPH_MODEL_DIALOGUE_RELATION_SCORE = 4.5; // 模型审计采纳的对话/称呼反证
var GRAPH_MODEL_ACTION_RELATION_SCORE = 4.0; // 模型审计采纳的动作对象/互动反证
var GRAPH_MODEL_SOCIAL_RELATION_SCORE = 5.0; // 模型审计采纳的社会关系反证
var GRAPH_MODEL_CO_PRESENCE_SCORE = 4.0; // 模型审计采纳的并列/人数反证
var GRAPH_MODEL_EXPLICIT_DIFFERENT_SCORE = 6.5; // 模型审计采纳的明确非同人反证
var ENABLE_COMPOUND_GRAPH_EVIDENCE = 1; // 复合链路直接落地：0=关闭，1=开启
var GRAPH_COMPOUND_SPEAKER_INTERACTION_SCORE = 2.5;
var GRAPH_COMPOUND_RELATIONSHIP_INTERACTION_SCORE = 2.5;
var GRAPH_COMPOUND_EXPLICIT_DIFFERENT_SCORE = 5.0;
var GRAPH_COMPOUND_NAME_ALIAS_SCORE = 2.5;
var GRAPH_COMPOUND_VERIFIED_SAME_SCORE = 2.5;

// ===================== 角色命名与说话主体通用原则 =====================
function getV908CharacterNamingAndSpeakerRules(scene) {
  var title = "【角色命名、实际说话主体与朗读角色连续性通用原则】\n";
  var text = "";
  text += "1. 必须分别判断两个问题：当前这段内容实际由谁发声；两个称呼是否应归入同一个稳定朗读角色记录。当前由某个意识控制并发声，不能自动推出该意识与宿主是同一角色。\n";
  text += "2. 当前实际说话主体优先依据发声者、控制意识、灵魂、元神、分魂、器灵、傀儡控制者或叙事明确指定的发声主体判断，不能只依据肉身、外貌、宿主名称或声音传出位置。\n";
  text += "3. 如果宿主原本具有独立意识、独立人格、独立身份或独立言行，另一个意识只是附身、临时控制、借用身体发声或尚未完全取代宿主，则控制者与宿主仍是两个不同角色。附身期间的内容可以归属于控制者，但不能因此把控制者与宿主判为同一稳定朗读角色。\n";
  text += "4. 如果文本明确说明某个身体、傀儡、化身、空壳或载体本身没有独立意识、独立人格和独立说话身份，只是由同一控制意识持续占据、驱使或显化，则朗读角色连续性跟随该控制意识；载体称呼、占据后的称呼以及同一控制意识后续变身形成的外貌称呼，可以依据当前文本证据归入同一稳定朗读角色。\n";
  text += "5. 必须结合时间状态判断：控制发生前的两个独立主体、控制期间的实际发声主体、控制结束后恢复自主意识的宿主，不得混为同一阶段。变身、易容、外貌变化、肉身变化或声音来源变化本身不构成拆分依据；只有出现新的独立意识、原宿主恢复自主意识或控制关系结束时，才重新判断角色边界。证据不足时保守处理，不凭共用身体或声音位置强行判同。\n";
  text += "6. name字段、别名字段、证据里的a/b字段，只能使用正文真实出现的称呼、已知主名、已知别名或模型基于当前语境生成的临时角色名；不要创造‘某某（被附身状态）’、‘某某（被控制）’、‘某某傀儡状态’这类解释型状态名。\n";
  text += "7. 关系/身份描述不是人物别名。师徒、亲属、主从、敌友、同伴等可作为关系证据，但不能单独作为same_person依据。明确本名、真名、原名、又名、自称、人称、号称、道号、法号、尊号、A（B）、A就是B等，可作为same_person候选证据；最终是否采纳由证据审计、冲突校验和别名判断决定。\n";
  text += "8. 组合称谓、群体称谓、二人组、某某二老、众人、众女、护卫队、佣兵队、几名女生、族人、护卫等，不等于其中单个成员，不能与单个成员输出same_person。只有组合/群体称谓本身在当前文本中作为实际说话人出现时，才允许作为新角色说话人输出；成员关系只能作为group_member/identity_relation/weak_relation等证据。\n";
  if (scene === "name_analyze") {
    text += "9. 姓名分析时，先完成每个编号引号块的性质判断和实际说话主体归属，再在__relations中返回当前文本能支持的原子证据；不要输出合并/拆分动作。\n";
  } else if (scene === "alias_check") {
    text += "9. 别名校验判断的是两个称呼是否应归入同一稳定朗读角色记录。当前说话主体、宿主自主性和控制意识连续性必须共同核对，不能因共用身体或借身发声直接判同。\n";
  } else if (scene === "alias_refine") {
    text += "9. 别名清洗时，只保留正文真实称呼或已有角色名；按宿主自主性、控制意识连续性和时间状态清理错误别名，不要保留解释型状态名。\n";
  } else if (scene === "graph_conflict") {
    text += "9. 冲突校验时，比较新旧证据强度、章节顺序和控制关系所处时间阶段，允许新证据推翻旧证据；不要建议创建解释型状态名。\n";
  }
  return title + text + "\n";
}

// ===================== 角色配置（集中管理，视觉工整）=====================
// 1. 主角配置：[显示前缀, 性别, 年龄, 发音人前缀, 数量]
var MAIN_ROLES_CONFIG = [
  ['主角 男主', '主角', '男主', '男主', 20],
  ['主角 女主', '主角', '女主', '女主', 20]
];
// 2. 批量角色配置：[类型前缀, 性别, 年龄, 发音人前缀, 数量]
var BATCH_ROLES = [
  ['女/少女', '女', '少女', '少女', 300],
  ['男/少年', '男', '少年', '少年', 300],
  ['女/女青年', '女', '女青年', '女青年', 300],
  ['男/男青年', '男', '男青年', '男青年', 300],
  ['女/女中年', '女', '女中年', '女中年', 300],
  ['男/男中年', '男', '男中年', '男中年', 300],
  ['女/女老年', '女', '女老年', '女老年', 300],
  ['男/男老年', '男', '男老年', '男老年', 300],
  ['女/女童', '女', '女童', '女童', 300],
  ['男/男童', '男', '男童', '男童', 300],
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
          // voice标签
          var seqVoice = (voicePrefix === '男主') ? i.toString() : padZero(i, 2);
          // key 直接用 tag（如"女青年01"），不再用【前缀+序号】格式
          var name = voicePrefix + seqVoice;
          chars[name] = { gender: gender, age: age, voice: voicePrefix + seqVoice };
      }
  }

  // 生成批量角色
  BATCH_ROLES.forEach(function (item) {
      var type = item[0], gender = item[1], age = item[2], voicePre = item[3], count = item[4];
      for (var i = 1; i <= count; i++) {
          var seq = padZero(i, 2);
          // key 直接用 tag（如"女青年01"），不再用【前缀+序号】格式
          var name = voicePre + seq;
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

var roleToRootIdMap = {};


// ===================== 核心：双场景独立密钥轮换管理（热更新版，ES5兼容，新增API自动补全逻辑）=====================
var DualKeyManager = (function() {
  // 兜底默认配置，和原代码完全对齐
  var defaultConfig = {
      endpoint: 'https://open.bigmodel.cn/api/paas/v4/chat/completions',
      model: "glm-4.6v-flash",
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

          // ===================== 【核心修改：自动补全API列表到3个】=====================
          pools.nameAnalyze.list = fillApiListToMinCount(pools.nameAnalyze.list, MIN_CONCURRENT_COUNT);
          pools.aliasAnalyze.list = fillApiListToMinCount(pools.aliasAnalyze.list, MIN_CONCURRENT_COUNT);

          // 加载两个场景的独立索引
          loadIndex("nameAnalyze");
          loadIndex("aliasAnalyze");
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




// ===================== 修复后：通用并发API请求工具（彻底解决提前唤醒问题，严格等待指定数量结果）=====================
function concurrentApiRequest(scene, requestBuilder, responseParser, maxConcurrent, timeout) {
  // 移除5的上限限制，仅保留最小1并发的合法性校验
var safeBingfa = parseInt(bingfa, 10);
if (isNaN(safeBingfa) || safeBingfa < 1) safeBingfa = 1;

// 优先使用传入的maxConcurrent参数，否则使用全局bingfa配置，无上限限制
if (!maxConcurrent || maxConcurrent <= 0) {
  maxConcurrent = safeBingfa;
}

  
  timeout = timeout || 18000;
  var errors = [];
  // 存储所有成功的结果（带返回时间戳，用于投票排序）
  var successResults = [];
  // 原子操作：线程安全的计数
  var successCount = new java.util.concurrent.atomic.AtomicInteger(0); // 已成功的请求数
  var finishedRequestCount = new java.util.concurrent.atomic.AtomicInteger(0); // 已完成的请求总数（成功+失败）
  // 模式判断：是否需要等待多结果
  var needWaitMultiResult = WAIT_API_RESULT_COUNT >= 3;
  // 需要等待的成功结果数量（不超过实际并发数）
  var targetSuccessCount = needWaitMultiResult ? Math.min(WAIT_API_RESULT_COUNT, maxConcurrent) : 1;
  // 闭锁：统一初始化为1，满足条件时唤醒主线程，彻底解决提前唤醒问题
  var countDownLatch = new java.util.concurrent.CountDownLatch(1);
  // 标记是否已经唤醒过主线程，避免重复操作
  var hasWakedUp = new java.util.concurrent.atomic.AtomicBoolean(false);

  // ===================== 核心：按规则获取本次并发的API列表 =====================
  var apiScene = (scene === "relationAudit" || scene === "aliasRefine") ? "aliasAnalyze" : scene;
  var apiList = DualKeyManager.getAvailableApiList(apiScene, maxConcurrent);
  var concurrentCount = apiList.length;
  // 无可用API，直接返回失败，后续走原兜底逻辑
  if (concurrentCount === 0) {
    console.error("【并发" + scene + "】无可用API密钥，终止并发");
    return { success: false, data: null, errors: ["无可用API密钥"] };
  }
  console.log("【并发" + scene + "】启动，目标等待成功数：" + targetSuccessCount + "，并发总数：" + concurrentCount);

  // 单线程请求核心逻辑
  function createSingleRequestTask(apiConfig) {
    return function() {
      var requestStartTime = Date.now();
      try {
        // 已经唤醒主线程，且达到目标成功数，直接终止当前线程，节省资源
        if (hasWakedUp.get() && successCount.get() >= targetSuccessCount) return;
        
        // 构建请求参数（与原逻辑100%一致）
        var requestParams = requestBuilder(apiConfig);
        if (!requestParams) throw new Error("请求参数构建失败");
        // 发起HTTP请求（复用原同步请求方法）
        var response = ttsrv.httpPost(
          requestParams.endpoint,
          JSON.stringify(requestParams.data),
          requestParams.headers
        );
        // 响应解析与格式校验（与原逻辑100%一致）
        var parsedResult = responseParser(response);
        if (!parsedResult) throw new Error("响应解析失败，无有效结果");

        // 线程安全的添加成功结果
        var currentSuccessNum = successCount.incrementAndGet();
        // 记录结果+返回时间戳（用于最晚返回排序）
        successResults.push({
          data: parsedResult,
          apiConfig: apiConfig,
          responseTime: Date.now() - requestStartTime,
          timestamp: Date.now()
        });

        // ===================== 模式分支处理 =====================
        if (!needWaitMultiResult) {
          // 原模式：1/2值，第一个成功立即唤醒主线程
          console.log("【" + (scene === "nameAnalyze" ? "🔴✅ 姓名分析" : (scene === "relationAudit" ? "🟣✅ 证据审计" : (scene === "aliasRefine" ? "🟦✅ 别名清洗" : "🔵✅ 别名校验"))) + "成功！】 单结果模式，立即使用，模型：" + apiConfig.model + "，密钥末尾4位：" + apiConfig.key.slice(-4));
          if (hasWakedUp.compareAndSet(false, true)) {
            countDownLatch.countDown();
          }
        } else {
          // 多结果模式：打印当前进度
          console.log("【" + (scene === "nameAnalyze" ? "🔴 姓名分析" : (scene === "relationAudit" ? "🟣 证据审计" : (scene === "aliasRefine" ? "🟦 别名清洗" : "🔵 别名校验"))) + "成功" + currentSuccessNum + "/" + targetSuccessCount + "个】 模型：" + apiConfig.model + "，密钥末尾4位：" + apiConfig.key.slice(-4));
          // 达到目标成功数，唤醒主线程
          if (currentSuccessNum >= targetSuccessCount && hasWakedUp.compareAndSet(false, true)) {
            console.log("【并发" + scene + "】已达到目标成功数" + targetSuccessCount + "个，停止等待，开始处理结果");
            countDownLatch.countDown();
          }
        }
      } catch (err) {
        // 单请求失败，仅记录错误，不影响其他线程
        var errorMsg = "密钥末尾" + apiConfig.key.slice(-4) + "：" + (err.message || "请求超时/未知错误");
        errors.push(errorMsg);
        console.error("【并发" + scene + "】请求失败：" + errorMsg);
      } finally {
        // 【修复核心】请求完成（无论成功失败），才计入已完成总数
        var finishedNum = finishedRequestCount.incrementAndGet();
        // 多结果模式下，所有请求都完成了，无论成功多少，都唤醒主线程
        if (needWaitMultiResult && finishedNum >= concurrentCount && hasWakedUp.compareAndSet(false, true)) {
          console.log("【并发" + scene + "】所有" + concurrentCount + "个请求已完成，共收集到" + successCount.get() + "个有效结果，开始处理");
          countDownLatch.countDown();
        }
      }
    };
  }

  // 为每个API创建独立并发线程
  for (var i = 0; i < apiList.length; i++) {
    // IIFE解决循环闭包陷阱，保证每个线程拿到独立的API配置
    (function(apiConfig) {
      var thread = new java.lang.Thread(new java.lang.Runnable({
        run: createSingleRequestTask(apiConfig)
      }));
      thread.start();
    })(apiList[i]);
  }

  // 主线程等待，超时时间与原配置对齐
  try {
    var waitSuccess = countDownLatch.await(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
    if (!waitSuccess) {
      errors.push("并发请求超时（" + timeout/1000 + "秒），已收集到" + successCount.get() + "个有效结果");
      console.error("【并发" + scene + "】请求超时，已收集" + successCount.get() + "个结果，开始处理");
    }
  } catch (e) {
    errors.push("主线程等待异常：" + e.message);
    console.error("【并发" + scene + "】主线程等待异常：" + e.message);
  }

  // 返回最终结果
  if (successCount.get() > 0) {
    return {
      success: true,
      // 单结果模式返回第一个数据，多结果模式返回所有成功结果数组
      data: needWaitMultiResult ? successResults : successResults[0].data,
      isMultiResult: needWaitMultiResult,
      errors: errors
    };
  } else {
    return { success: false, data: null, errors: errors };
  }
}



// ===================== 最终完整版：姓名分析结果投票函数（日志开关+对话原文打印+格式优化）=====================
function graphNormalizeModelRelationType(t) {
  t = graphSafeString(t || "", 60).toLowerCase();
  if (!t) return "";
  if (t.indexOf("same") !== -1 || t.indexOf("alias") !== -1 || t.indexOf("同一") !== -1 || t.indexOf("别名") !== -1) return "same_person";
  if (t.indexOf("identity") !== -1 || t.indexOf("substitution") !== -1 || t.indexOf("身份") !== -1 || t.indexOf("附身") !== -1 || t.indexOf("操控") !== -1 || t.indexOf("冒充") !== -1) return "identity_relation";
  if (t.indexOf("weak") !== -1 || t.indexOf("mention") !== -1 || t.indexOf("提及") !== -1 || t.indexOf("想起") !== -1 || t.indexOf("寻找") !== -1 || t.indexOf("调查") !== -1) return "weak_relation";
  if (t.indexOf("different") !== -1 || t.indexOf("direct") !== -1 || t.indexOf("interaction") !== -1 || t.indexOf("speaker") !== -1 || t.indexOf("relationship") !== -1 || t.indexOf("listed") !== -1 || t.indexOf("counted") !== -1 || t.indexOf("action") !== -1 || t.indexOf("不同") !== -1 || t.indexOf("互动") !== -1 || t.indexOf("关系") !== -1 || t.indexOf("并列") !== -1) return "different_person";
  return t;
}

function graphNormalizeEvidenceFamily(f, relationType) {
  f = graphSafeString(f || "", 80).toLowerCase();
  relationType = graphNormalizeModelRelationType(relationType || "");
  if (!f) {
    if (relationType === "same_person") return "name_identity";
    if (relationType === "identity_relation") return "identity_relation";
    if (relationType === "weak_relation") return "weak_relation";
    return "dialogue_relation";
  }
  if (/explicit_difference|explicit_different|not_same|not_person|different_explicit/.test(f)) return "explicit_difference";
  if (/self_claim|called_as|introduced_as|parenthetical_alias|descriptor_alias|name_alias|explicit_same|name_identity/.test(f)) return "name_identity";
  if (/speaker|dialogue|reply|vocative|direct_interaction/.test(f)) return "dialogue_relation";
  if (/action|mutual|attack|save|follow|give|ask_to|object/.test(f)) return "action_relation";
  if (/relationship|social|master|disciple|family|enemy|friend/.test(f)) return "social_relation";
  if (/listed|counted|co_presence|together|parallel/.test(f)) return "co_presence";
  if (/identity|possession|control|puppet|impersonation|disguise|soul|body/.test(f)) return "identity_relation";
  if (/weak|mention|memory|search|investigate|attention/.test(f)) return "weak_relation";
  return f;
}

function graphNormalizeEvidenceSubtype(s) {
  s = graphSafeString(s || "", 80).toLowerCase();
  if (!s) return "";
  // anchorType（如 current_text_direct_pair）只表示证据锚点，不得污染 evidenceSubtype。
  if (/current_text|direct_pair|bridge_pair|anchor|recent_chapter|graph_hint/.test(s)) return "";
  return s;
}

function graphEvidenceNormalizeText(text) {
  text = graphSafeString(text || "", 200000);
  text = text.replace(/【\d{1,3}】/g, "");
  text = text.replace(/[\s\u3000"“”'‘’`´·・,，、。.!！？?；;：:《》〈〉（）()【】\[\]{}]/g, "");
  return text;
}



function graphRelationReasonFromFamily(relationType, family) {
  relationType = graphNormalizeModelRelationType(relationType);
  family = graphNormalizeEvidenceFamily(family, relationType);
  if (relationType === "same_person") return "model_name_identity_positive";
  if (relationType === "identity_relation" || family === "identity_relation") return "model_identity_relation_evidence";
  if (relationType === "weak_relation" || family === "weak_relation") return "model_weak_relation_audit";
  if (relationType === "different_person") {
    if (family === "explicit_difference") return "model_explicit_different_negative";
    if (family === "action_relation") return "model_action_relation_negative";
    if (family === "social_relation") return "model_social_relation_negative";
    if (family === "co_presence") return "model_co_presence_negative";
    return "model_dialogue_relation_negative";
  }
  if (family === "name_identity") return "model_name_identity_positive";
  if (family === "explicit_difference") return "model_explicit_different_negative";
  if (family === "dialogue_relation") return "model_dialogue_relation_negative";
  if (family === "action_relation") return "model_action_relation_negative";
  if (family === "social_relation") return "model_social_relation_negative";
  if (family === "co_presence") return "model_co_presence_negative";
  return "";
}

function graphNormalizeAuditDecision(decision) {
  decision = graphSafeString(decision || "", 40).toLowerCase();
  if (decision === "accepted") return "accept";
  if (decision === "rejected") return "reject";
  if (decision === "downgraded") return "downgrade";
  if (decision === "to_verify" || decision === "conflict_verify") return "verify";
  if (decision === "accept" || decision === "reject" || decision === "downgrade" || decision === "verify") return decision;
  return "downgrade";
}

function graphPrecheckModelRelationShape(raw) {
  var r = raw || {};
  var a = graphNormalizeName(r.a || r.nameA || r.from || r.left || "");
  var b = graphNormalizeName(r.b || r.nameB || r.to || r.right || "");
  var relationType = graphNormalizeModelRelationType(r.relationType || r.type || r.relation || "");
  var family = graphNormalizeEvidenceFamily(r.evidenceFamily || r.family || "", relationType);
  var subtype = graphNormalizeEvidenceSubtype(r.evidenceSubtype || r.subtype || "");
  var evidenceText = graphSafeString(r.evidenceText || r.evidence || r.reason || r.text || "", 520);
  var summary = graphSafeString(r.summary || r.reasonSummary || "", 320);
  var confidence = Number(r.confidence || r.score || 0);
  if (!a || !b || a === b) return { ok: false, reason: "missing_pair_name" };
  if (!relationType || !family) return { ok: false, reason: "unsupported_relation_type" };
  if (!evidenceText && !summary) return { ok: false, reason: "missing_evidence_text" };
  var directPair = r.directPair === true || r.directPair === "true";
  var bridgeNames = Array.isArray(r.bridgeNames) ? r.bridgeNames : [];
  var reason = graphRelationReasonFromFamily(relationType, family);
  if (!reason) return { ok: false, reason: "unsupported_relation_type" };
  var flags = [];
  if (confidence && confidence < 60) flags.push("low_confidence");
  if (directPair && bridgeNames.length > 0) flags.push("bridge_conflict");
  return { ok: true, relation: {
    relationId: graphSafeString(r.relationId || r.id || "", 80),
    a: a, b: b, type: relationType, relationType: relationType,
    evidenceFamily: family, evidenceSubtype: subtype || "unknown_subtype",
    evidenceText: evidenceText, evidence: evidenceText, summary: summary,
    seq: graphSafeString(r.seq || r.sequence || "", 20),
    anchorType: graphSafeString(r.anchorType || r.anchor || "", 60),
    directPair: directPair,
    bridgeNames: bridgeNames,
    confidence: confidence || 80,
    reason: reason,
    source: "name_semantic_channel",
    shapeFlags: flags,
    raw: r
  }};
}


function graphV908IsFirstPersonOrHonorificTitleName(name) {
  name = graphNormalizeName(name || "");
  if (!name) return false;
  if (/^(本岛主|本座|本尊|本少主|本公子|本小姐|本夫人|本宫|本王|本皇|本帝|本门主|本宗主|本掌门|本长老|本上人)$/.test(name)) return true;
  if (/^(老夫|老朽|老衲|贫道|贫僧|在下|鄙人|妾身|小女子|小女|小子|晚辈)$/.test(name)) return true;
  if (/^[\u4e00-\u9fa5]{1,3}某$/.test(name)) return true;
  return false;
}

function normalizeModelRelationList(apiResult) {
  var rels = apiResult && (apiResult.__relations || apiResult.relations || apiResult._relations);
  if (!rels || !Array.isArray(rels)) return [];
  var out = [];
  for (var i = 0; i < rels.length; i++) {
    var r = rels[i] || {};
    var a = graphNormalizeName(r.a || r.nameA || r.from || r.left);
    var b = graphNormalizeName(r.b || r.nameB || r.to || r.right);
    var relationType = graphNormalizeModelRelationType(r.relationType || r.type || r.relation || "");
    var family = graphNormalizeEvidenceFamily(r.evidenceFamily || r.family || "", relationType);
    var subtype = graphNormalizeEvidenceSubtype(r.evidenceSubtype || r.subtype || "");
    var evidenceText = graphSafeString(r.evidenceText || r.evidence || r.reason || r.text || "", 420);
    // normalize阶段只做字段标准化，不再因为“老者/少女/众女/众人/自称称号”等本地拒收。
    // 缺字段、同名、无证据等基础结构问题交给后续shape precheck；真假交给证据审计/别名/冲突校验模型。
    out.push({
      a: a, b: b,
      type: relationType,
      relationType: relationType,
      evidenceFamily: family,
      evidenceSubtype: subtype || "unknown_subtype",
      evidence: evidenceText,
      evidenceText: evidenceText,
      summary: graphSafeString(r.summary || r.reasonSummary || "", 260),
      anchorType: graphSafeString(r.anchorType || r.anchor || "", 60),
      directPair: r.directPair === true || r.directPair === "true",
      bridgeNames: Array.isArray(r.bridgeNames) ? r.bridgeNames : [],
      seq: graphSafeString(r.seq || r.sequence || "", 20),
      confidence: Number(r.confidence || r.score || 0),
      raw: r
    });
  }
  return out;
}


function voteModelRelations(successResults) {
  var bucket = {};
  var out = [];
  var beforeCount = 0;
  var sourceResultCount = (successResults && successResults.length) ? successResults.length : 0;
  for (var i = 0; i < sourceResultCount; i++) {
    var rels = normalizeModelRelationList(successResults[i].data);
    beforeCount += rels.length;
    for (var j = 0; j < rels.length; j++) {
      var r = rels[j];
      var pair = graphPairKey(r.a, r.b);
      var evKey = graphEvidenceNormalizeText(r.evidenceText || "").substring(0, 120);
      var key = pair + "||" + String(r.relationType || r.type).toLowerCase() + "||" + String(r.evidenceFamily || "").toLowerCase() + "||" + String(r.evidenceSubtype || "").toLowerCase() + "||" + evKey;
      if (!bucket[key]) {
        bucket[key] = {
          a: r.a, b: r.b, type: r.type, relationType: r.relationType,
          evidenceFamily: r.evidenceFamily, evidenceSubtype: r.evidenceSubtype,
          evidenceText: r.evidenceText, evidence: [], summary: r.summary || "",
          anchorType: r.anchorType || "", directPair: r.directPair === true,
          bridgeNames: r.bridgeNames || [], votes: 0, maxConfidence: 0, seq: r.seq || "",
          rawSamples: []
        };
      }
      bucket[key].votes++;
      if (r.evidenceText && bucket[key].evidence.length < 5) bucket[key].evidence.push(r.evidenceText);
      if (bucket[key].rawSamples.length < 5) bucket[key].rawSamples.push(r.raw || r);
      var confidence = Number(r.confidence || 0);
      if (confidence >= Number(bucket[key].maxConfidence || 0)) {
        bucket[key].maxConfidence = confidence;
        bucket[key].type = r.type || bucket[key].type;
        bucket[key].relationType = r.relationType || bucket[key].relationType;
        bucket[key].evidenceFamily = r.evidenceFamily || bucket[key].evidenceFamily;
        bucket[key].evidenceSubtype = r.evidenceSubtype || bucket[key].evidenceSubtype;
        bucket[key].summary = r.summary || bucket[key].summary || "";
        bucket[key].anchorType = r.anchorType || bucket[key].anchorType || "";
        bucket[key].directPair = r.directPair === true || bucket[key].directPair === true;
        bucket[key].bridgeNames = (r.bridgeNames && r.bridgeNames.length) ? r.bridgeNames : (bucket[key].bridgeNames || []);
        bucket[key].seq = r.seq || bucket[key].seq || "";
        if (r.evidenceText) bucket[key].evidenceText = r.evidenceText;
      }
    }
  }
  for (var k in bucket) {
    if (!bucket.hasOwnProperty(k)) continue;
    var item = bucket[k];
    out.push({
      a: item.a, b: item.b, type: item.type, relationType: item.relationType,
      evidenceFamily: item.evidenceFamily, evidenceSubtype: item.evidenceSubtype,
      evidence: item.evidence.join(" | "), evidenceText: item.evidenceText || item.evidence.join(" | "),
      summary: item.summary || "", anchorType: item.anchorType || "", directPair: item.directPair === true,
      bridgeNames: item.bridgeNames || [], seq: item.seq, confidence: item.maxConfidence, votes: item.votes,
      rawSamples: item.rawSamples
    });
  }
  out._groupMeta = {
    sourceResultCount: sourceResultCount,
    relationCountBeforeGroup: beforeCount,
    relationCountAfterGroup: out.length,
    voteMeaning: "single_result_bucket_hits_or_multi_result_votes",
    filterPolicy: "no_local_confidence_filter"
  };
  return out;
}

// 多结果模式下只归并模型明确给出的发声音龄证据，不对“没有证据”进行猜测补票。
function voteVoiceAgeEvidence(successResults) {
  if (!successResults || !Array.isArray(successResults)) return [];
  var bucket = {};
  for (var i = 0; i < successResults.length; i++) {
    var apiData = successResults[i] && successResults[i].data ? successResults[i].data : {};
    var list = Array.isArray(apiData.__voiceAgeEvidence) ? apiData.__voiceAgeEvidence : [];
    for (var j = 0; j < list.length; j++) {
      var item = list[j] || {};
      var seq = graphSafeString(item.seq || "", 20);
      var subjectName = graphNormalizeName(item.subjectName || item.name || "");
      var stage = graphSafeString(item.finalVoiceAgeStage || item.stage || "", 30);
      var action = graphSafeString(item.stateAction || "", 30);
      var scope = graphSafeString(item.applyScope || "", 30);
      var coverageMode = graphSafeString(item.coverageMode || "", 40);
      var effectiveThroughSeq = graphSafeString(item.effectiveThroughSeq || "", 20);
      var endTiming = graphSafeString(item.endTiming || "", 30);
      var continuesBeyondBatch = item.continuesBeyondBatch === true ? "1" : "0";
      var evidenceText = graphSafeString(item.evidenceText || "", VOICE_AGE_EVIDENCE_TEXT_MAX);
      var signature = [seq, subjectName, stage, action, scope, coverageMode, effectiveThroughSeq, endTiming, continuesBeyondBatch, evidenceText].join("|");
      if (!seq || !subjectName || !signature) continue;
      if (!bucket[signature]) {
        var copied = {};
        try { copied = JSON.parse(JSON.stringify(item)); } catch(e) { copied = item; }
        copied.evidenceId = graphSafeString(item.evidenceId || ("age_vote_" + graphHash(signature)), 80);
        copied.votes = 0;
        copied.sourceModels = [];
        bucket[signature] = copied;
      }
      bucket[signature].votes++;
      var modelName = successResults[i] && successResults[i].apiConfig ? graphSafeString(successResults[i].apiConfig.model || "", 80) : "";
      if (modelName && bucket[signature].sourceModels.indexOf(modelName) === -1) bucket[signature].sourceModels.push(modelName);
      if (Number(item.confidence || 0) > Number(bucket[signature].confidence || 0)) bucket[signature].confidence = Number(item.confidence || 0);
    }
  }
  var out = [];
  for (var key in bucket) if (bucket.hasOwnProperty(key)) out.push(bucket[key]);
  out.sort(function(a, b) {
    var seqDiff = parseInt(a.seq || "0", 10) - parseInt(b.seq || "0", 10);
    if (seqDiff) return seqDiff;
    return Number(b.votes || 0) - Number(a.votes || 0);
  });
  return out; // 不设年龄证据数量硬上限，由批次文本字符数和证据去重从源头控制
}

// 临时状态续判不能被姓名投票丢掉。多结果时先选结构覆盖最完整的一份，随后仍由本地逐stateId验收并对坏项单独重试。
function voteTemporaryVoiceStateReview(successResults) {
  successResults = Array.isArray(successResults) ? successResults : [];
  var best = null;
  var bestScore = -1;
  var bestTimestamp = -1;
  for (var i = 0; i < successResults.length; i++) {
    var data = successResults[i] && successResults[i].data || {};
    var review = data.__temporaryVoiceStateReview;
    if (!review || typeof review !== "object" || Array.isArray(review)) continue;
    var reviews = Array.isArray(review.reviews) ? review.reviews : [];
    var unique = {};
    var completeItems = 0;
    for (var j = 0; j < reviews.length; j++) {
      var stateId = graphSafeString(reviews[j] && reviews[j].stateId || "", 120);
      if (!stateId || unique[stateId]) continue;
      unique[stateId] = true;
      if (reviews[j].hasOwnProperty("hasDialogue") && reviews[j].decision) completeItems++;
    }
    var score = Object.keys(unique).length * 10 + completeItems * 4 + (review.reviewComplete === true ? 1000 : 0) + (Number(review.activeStateCount) === Object.keys(unique).length ? 100 : 0) + (review.activeStateSetId ? 20 : 0);
    var timestamp = Number(successResults[i] && successResults[i].timestamp || 0);
    if (score > bestScore || (score === bestScore && timestamp >= bestTimestamp)) {
      bestScore = score;
      bestTimestamp = timestamp;
      try { best = JSON.parse(JSON.stringify(review)); } catch(copyReviewErr) { best = review; }
    }
  }
  return best || {};
}

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
            var exactAliasRecordForMap = (typeof characterManager !== 'undefined' && characterManager && characterManager.findMainCharacterRecordByExactName) ? characterManager.findMainCharacterRecordByExactName(aliasList[j]) : null;
            if (!exactAliasRecordForMap || graphNormalizeName(exactAliasRecordForMap.name) === graphNormalizeName(mainName)) {
              nameToMainNameMap[aliasList[j]] = mainName;
            }
          }
        }
      }
      // 生成后同步到内存，后续直接复用
      characterManager.nameToMainNameMap = nameToMainNameMap;
    }
    console.log("【🔴 投票别名合并】已" + (enableMerge ? "开启" : "关闭") + "，内存映射表共" + Object.keys(nameToMainNameMap).length + "条记录");
  }
  // ========== 优化结束 ==========

  // 第一步：收集所有对话序号，按数字升序排序
  var allSeqSet = {};
  for (var i = 0; i < successResults.length; i++) {
    var apiResult = successResults[i].data;
    for (var seq in apiResult) {
      if (apiResult.hasOwnProperty(seq) && /^\d+$/.test(seq)) {
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

    // 无有效结果兜底：性别留空，由调用方走中性 duihua，避免随机男女声误导
    if (seqAllResults.length === 0) {
      finalResult[currentSeq] = {
        name: "未知",
        gender: "",
        age: ""
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
      console.log("【🔴 序号" + currentSeq + " 姓名投票统计】");
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

  var modelRawRelationCount = 0;
  for (var rawRi = 0; rawRi < successResults.length; rawRi++) {
    try {
      var rawRels = successResults[rawRi].data && successResults[rawRi].data.__relations;
      if (rawRels && Array.isArray(rawRels)) modelRawRelationCount += rawRels.length;
    } catch(rawRelErr) {}
  }
  finalResult.__relations = voteModelRelations(successResults);
  finalResult.__voiceAgeEvidence = voteVoiceAgeEvidence(successResults);
  finalResult.__temporaryVoiceStateReview = voteTemporaryVoiceStateReview(successResults);
  console.log("【🔴✅ 姓名分析投票完成】 处理了" + sortedSeqList.length + "个对话，基于" + successResults.length + "个API结果");
  semanticShortLog("模型原始" + modelRawRelationCount + "条，归并" + finalResult.__relations.length + "条");
  if (finalResult.__relations.length > 0) graphShortLog("模型关系" + finalResult.__relations.length + "条");
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
            var exactAliasRecordForMap = (typeof characterManager !== 'undefined' && characterManager && characterManager.findMainCharacterRecordByExactName) ? characterManager.findMainCharacterRecordByExactName(aliasList[j]) : null;
            if (!exactAliasRecordForMap || graphNormalizeName(exactAliasRecordForMap.name) === graphNormalizeName(mainName)) {
              nameToMainNameMap[aliasList[j]] = mainName;
            }
          }
        }
      }
      characterManager.nameToMainNameMap = nameToMainNameMap;
    }
    console.log("【🔵 别名投票别名合并】已" + (enableMerge ? "开启" : "关闭") + "，内存映射表共" + Object.keys(nameToMainNameMap).length + "条记录");
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
 //   console.log("【🔵✅ 别名校验投票完成】 无有效别名结果，使用最晚返回的结果");
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

  console.log("【🔵✅ 别名校验投票完成】 选中主名：" + selectedMainName + "，基于" + successResults.length + "个API结果");
  return finalResult;
}

// ===================== 新增：别名清洗结果投票函数（主名+别名清洗专用）=====================
function voteAliasRefineResult(successResults) {
  if (!successResults || !Array.isArray(successResults) || successResults.length === 0) {
    return null;
  }

  // 按返回时间从晚到早排序，平票时优先最晚返回
  var sortedByTime = successResults.sort(function(a, b) {
    return b.timestamp - a.timestamp;
  });

  // 仅保留结构合法的结果
  var validResults = [];
  for (var i = 0; i < sortedByTime.length; i++) {
    var item = sortedByTime[i];
    if (!item || !item.data) continue;

    var data = item.data;
    if (typeof data.isSamePerson !== "boolean") continue;
    if (data.isSamePerson && (!data.mainName || !data.mainName.toString().trim())) continue;
    if (!Array.isArray(data.confirmedAliases)) continue;
    if (!Array.isArray(data.removedAliases)) continue;

    validResults.push(item);
  }

  if (validResults.length === 0) {
    return null;
  }

  // 第一步：先统计 isSamePerson 的真假票数
  var sameCount = 0;
  var notSameCount = 0;
  for (var i = 0; i < validResults.length; i++) {
    if (validResults[i].data.isSamePerson) {
      sameCount++;
    } else {
      notSameCount++;
    }
  }

  // 如果 false 票严格多于 true 票，直接返回最晚的 false 结果
  if (notSameCount > sameCount) {
    for (var i = 0; i < validResults.length; i++) {
      if (!validResults[i].data.isSamePerson) {
        return validResults[i].data;
      }
    }
  }

  // 第二步：只对 isSamePerson=true 的结果统计主名
  var samePersonResults = [];
  for (var i = 0; i < validResults.length; i++) {
    if (validResults[i].data.isSamePerson) {
      samePersonResults.push(validResults[i]);
    }
  }

  if (samePersonResults.length === 0) {
    return validResults[0].data;
  }

  var mainNameCountMap = {};
  for (var i = 0; i < samePersonResults.length; i++) {
    var mainName = samePersonResults[i].data.mainName;
    mainNameCountMap[mainName] = (mainNameCountMap[mainName] || 0) + 1;
  }

  var maxCount = 0;
  var topMainNames = [];
  for (var name in mainNameCountMap) {
    if (mainNameCountMap.hasOwnProperty(name)) {
      if (mainNameCountMap[name] > maxCount) {
        maxCount = mainNameCountMap[name];
        topMainNames = [name];
      } else if (mainNameCountMap[name] === maxCount) {
        topMainNames.push(name);
      }
    }
  }

  var selectedMainName = topMainNames[0];
  if (topMainNames.length > 1) {
    // 平票时取最晚返回的那个主名
    for (var i = 0; i < samePersonResults.length; i++) {
      var currentMainName = samePersonResults[i].data.mainName;
      if (topMainNames.indexOf(currentMainName) !== -1) {
        selectedMainName = currentMainName;
        break;
      }
    }
  }

  // 第三步：对选中主名下的 confirmedAliases / removedAliases 进行投票
  var aliasCountMap = {};
  var removedCountMap = {};

  for (var i = 0; i < samePersonResults.length; i++) {
    var resultData = samePersonResults[i].data;
    if (resultData.mainName !== selectedMainName) continue;

    for (var j = 0; j < resultData.confirmedAliases.length; j++) {
      var alias = (resultData.confirmedAliases[j] || "").toString().trim();
      if (!alias) continue;
      aliasCountMap[alias] = (aliasCountMap[alias] || 0) + 1;
    }

    for (var k = 0; k < resultData.removedAliases.length; k++) {
      var removedAlias = (resultData.removedAliases[k] || "").toString().trim();
      if (!removedAlias) continue;
      removedCountMap[removedAlias] = (removedCountMap[removedAlias] || 0) + 1;
    }
  }

  // 第四步：简单多数规则
  // confirmed票 >= removed票，则保留
  var finalConfirmedAliases = [];
  var seenConfirmed = {};

  for (var aliasName in aliasCountMap) {
    if (!aliasCountMap.hasOwnProperty(aliasName)) continue;

    var confirmedVotes = aliasCountMap[aliasName] || 0;
    var removedVotes = removedCountMap[aliasName] || 0;

    if (confirmedVotes >= removedVotes) {
      if (!seenConfirmed[aliasName]) {
        seenConfirmed[aliasName] = true;
        finalConfirmedAliases.push(aliasName);
      }
    }
  }

  // removed票 > confirmed票，才进入最终剔除列表
  var finalRemovedAliases = [];
  var seenRemoved = {};

  for (var removedName in removedCountMap) {
    if (!removedCountMap.hasOwnProperty(removedName)) continue;

    var confirmedVotes2 = aliasCountMap[removedName] || 0;
    var removedVotes2 = removedCountMap[removedName] || 0;

    if (removedVotes2 > confirmedVotes2) {
      if (!seenRemoved[removedName]) {
        seenRemoved[removedName] = true;
        finalRemovedAliases.push(removedName);
      }
    }
  }

  // 找到该主名下最晚返回的完整结果，用它的reason
  var latestMatchedData = null;
  for (var i = 0; i < samePersonResults.length; i++) {
    if (samePersonResults[i].data.mainName === selectedMainName) {
      latestMatchedData = samePersonResults[i].data;
      break;
    }
  }

  var finalResult = {
    isSamePerson: true,
    mainName: selectedMainName,
    confirmedAliases: finalConfirmedAliases,
    removedAliases: finalRemovedAliases,
    reason: latestMatchedData && latestMatchedData.reason ? latestMatchedData.reason : null
  };

  console.log("【🔵✅ 别名清洗投票完成】 主名：" + selectedMainName + "，确认别名数：" + finalConfirmedAliases.length + "，剔除别名数：" + finalRemovedAliases.length + "，基于" + successResults.length + "个API结果");
  return finalResult;
}






// 在智谱AI开放平台注册获取API_KEY: https://open.bigmodel.cn/
var CONFIG = {
    resetUsageCount: 100,
    activeRecordLimit: 200,
    contextHistoryLength: 1500,
    apiModel: "glm-4.5-flash",
    apiTemperature: 0.1,
    saveVoicesToFile: 1
};
var MAX_ALIAS_CHECK_CHARACTERS = 100;// 发给api分析的角色前50个或其他个数



// 新增：角色分析API重试次数配置（默认3次，可修改）
var CHARACTER_ANALYZE_RETRY_MAX = 8;
var next100Chars = "";
var prevContextChars = ""; // 当前本段前约500字、句号边界对齐的直接上文
var text2 = "";
var nameAnalysisContextMeta = null; // 保存实际共享上文、App当前段和下文外延边界，仅在真正发起姓名分析时写入远程快照

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
  this.globalVoiceUsage = {}; // 跨书全局发音人使用次数
  this.loadGlobalVoiceUsage();
  this.aliasPositiveGraphFile = graphBookCacheFile("alias_positive_graph", "default");
  this.aliasNegativeGraphFile = graphBookCacheFile("alias_negative_graph", "default");
  this.aliasCooccurStatsFile = graphBookCacheFile("alias_cooccur_stats", "default");
  this.aliasPositiveGraph = {};
  this.aliasNegativeGraph = {};
  this.aliasCooccurStats = {};
  this.aliasGraphBookKey = "";
  this.mergedRecordsFile = graphBookCacheFile("mergedRecords", "default");
  this.mergedRecords = {};
  this.lastAliasGraphScanKey = "";
  this.graphConflictVerifyChapterText = "";
  this.graphConflictVerifyChapterId = "";
  this.graphConflictVerifyCount = 0;
  this.graphConflictVerifySeen = {};
  this.temporaryVoiceStates = {}; // 按“书籍+稳定recordId”保存正在生效的临时换声状态
  this.temporaryVoiceAppliedEvents = {}; // 证据哈希去重，并保存重放时应复用的临时标签
  this.temporaryVoiceRestoreAttempted = {}; // 每个缓存快照只尝试恢复一次，避免重复导入
  this.voiceAgeAppliedEvidence = {}; // 自然发声音龄证据去重，避免同一证据重复写角色卡
  this.voiceAgeEvidenceFile = graphBookCacheFile("voice_age_evidence", "default"); // 与图谱相同，按数据版本+书籍隔离
  this.voiceAgeEvidenceCache = { schema: "v908_voice_age_evidence_cache", dataVersion: graphRuleDataVersion(), bookKey: "default", evidence: {}, updatedAt: "" };
  this._v908LastAnalysisByCharacterId = {}; // 把姓名分析结果传递到别名归并后的统一音色应用入口
  this.pendingVoiceAgeEvidence = null; // 暂存本批通过本地预检、等待合并审计的发声音龄证据
  this._v908CombinedAuditCommitTrace = []; // 记录别名→图谱→稳定角色→年龄的提交顺序，便于远程观察和模拟测试
  this.loadAliasGraphData();
  this.loadAliasCooccurStats();
  // mergedRecords 等 setAliasGraphBook 拿到 bookKey 后再读取，避免默认 default 污染日志。
}


// ===================== 轻量别名图谱、共现统计、远程日志 =====================
var graphRemoteQueue = [];
var graphRemoteCoalesceIndex = {}; // 仅索引允许合并的状态事件，保存同章重复次数而不是反复追加整条内容
var graphRemoteChapterKey = "";
var graphRemoteChapterIndex = "";
var GRAPH_RULE_SOURCE = "tts-rule-2.85-v908";
var graphCurrentBookUrl = "";
var graphCurrentChapterIndex = "";
var graphCurrentChapterKey = "";

var graphAliasRecentChapters = []; // 别名校验/姓名分析用最近章节索引
var graphAliasRecentChapterFile = "alias_recent_chapters.json";

function graphAliasMarkLimit() {
  var limit = parseInt(typeof ALIAS_RECENT_CHAPTER_MARK_LIMIT !== "undefined" ? ALIAS_RECENT_CHAPTER_MARK_LIMIT : 60, 10);
  if (isNaN(limit) || limit <= 0) limit = 60;
  return limit;
}

function graphAliasRecentChapterSafeKey() {
  var bookKey = "default";
  try {
    if (typeof characterManager !== "undefined" && characterManager && characterManager.aliasGraphBookKey) {
      bookKey = characterManager.aliasGraphBookKey || "default";
    } else if (typeof graphCurrentBookUrl !== "undefined" && graphCurrentBookUrl) {
      bookKey = graphBookCacheSafeKey("", graphCurrentBookUrl);
    }
  } catch(e) {}
  graphAliasRecentChapterFile = "alias_recent_chapters." + graphBookCacheSafeKey(bookKey, "") + ".json";
}

function graphAliasRecentChapterLoad() {
  try {
    graphAliasRecentChapterSafeKey();
    var saved = graphReadJsonSafe(graphAliasRecentChapterFile, null);
    if (saved && Array.isArray(saved.indices)) {
      graphAliasRecentChapters = saved.indices;
    } else {
      graphAliasRecentChapters = [];
    }
  } catch(e) {
    graphAliasRecentChapters = [];
  }
}

function graphAliasRecentChapterAppend(newIndex) {
  if (!newIndex) return;
  newIndex = graphSafeString(newIndex, 80);
  if (!newIndex || newIndex === "unknown") return;
  if (graphAliasRecentChapters.length === 0) graphAliasRecentChapterLoad();
  if (graphAliasRecentChapters.indexOf(newIndex) === -1) {
    graphAliasRecentChapters.push(newIndex);
  }
  var limit = graphAliasMarkLimit();
  if (graphAliasRecentChapters.length > limit) {
    graphAliasRecentChapters = graphAliasRecentChapters.slice(-limit);
  }
}

function graphAliasRecentChapterSave() {
  try {
    graphAliasRecentChapterSafeKey();
    ttsrv.writeTxtFile(graphAliasRecentChapterFile, JSON.stringify({
      indices: graphAliasRecentChapters || [],
      bookKey: typeof characterManager !== "undefined" && characterManager ? characterManager.aliasGraphBookKey || "" : "",
      updatedAt: graphNowIso()
    }));
  } catch(e) {}
}

function graphAliasGetRecentIndices(range) {
  range = parseInt(range || ALIAS_RECENT_CHAPTER_RANGE || 20, 10);
  if (isNaN(range) || range <= 0) range = 20;
  if (graphAliasRecentChapters.length === 0) graphAliasRecentChapterLoad();
  if (graphAliasRecentChapters.length === 0) return [];
  var sorted = graphAliasRecentChapters.slice().sort(function(a, b) {
    var na = Number(a), nb = Number(b);
    if (!isNaN(na) && !isNaN(nb)) return na - nb;
    return String(a).localeCompare(String(b));
  });
  return sorted.slice(-range);
}

function graphArrayIntersectsChapters(arr, recentChapters) {
  if (!arr || !Array.isArray(arr) || !recentChapters || !recentChapters.length) return false;
  for (var i = 0; i < recentChapters.length; i++) {
    if (arr.indexOf(recentChapters[i]) !== -1) return true;
  }
  return false;
}

function graphFilteredRecentChapters(arr, recentChapters) {
  var out = [];
  if (!arr || !Array.isArray(arr) || !recentChapters || !recentChapters.length) return out;
  for (var i = 0; i < recentChapters.length; i++) {
    if (arr.indexOf(recentChapters[i]) !== -1) out.push(recentChapters[i]);
  }
  return out;
}

function graphTrimChapterArray(arr) {
  if (!arr || !Array.isArray(arr)) return [];
  var seen = {};
  var out = [];
  for (var i = 0; i < arr.length; i++) {
    var v = graphSafeString(arr[i], 80);
    if (!v || v === "unknown" || seen[v]) continue;
    seen[v] = true;
    out.push(v);
  }
  var limit = graphAliasMarkLimit();
  if (out.length > limit) out = out.slice(-limit);
  return out;
}

function graphPushChapterMark(obj) {
  if (!obj) return false;
  var curChap = graphCurrentChapterId();
  if (!curChap || curChap === "unknown") return false;
  if (!obj.chapters || !Array.isArray(obj.chapters)) obj.chapters = [];
  if (obj.chapters.indexOf(curChap) === -1) obj.chapters.push(curChap);
  obj.chapters = graphTrimChapterArray(obj.chapters);
  return true;
}

function graphAliasRecentValue(name, fallback) {
  try {
    if (name === "ALIAS_RECENT_COOCUR_EVIDENCE_STORE_LIMIT" && typeof ALIAS_RECENT_COOCUR_EVIDENCE_STORE_LIMIT !== "undefined") return ALIAS_RECENT_COOCUR_EVIDENCE_STORE_LIMIT;
    if (name === "ALIAS_RECENT_COOCUR_EVIDENCE_LIMIT" && typeof ALIAS_RECENT_COOCUR_EVIDENCE_LIMIT !== "undefined") return ALIAS_RECENT_COOCUR_EVIDENCE_LIMIT;
    if (name === "ALIAS_RECENT_COOCUR_EVIDENCE_MAX_LEN" && typeof ALIAS_RECENT_COOCUR_EVIDENCE_MAX_LEN !== "undefined") return ALIAS_RECENT_COOCUR_EVIDENCE_MAX_LEN;
    if (name === "ALIAS_RECENT_GRAPH_REASON_LIMIT" && typeof ALIAS_RECENT_GRAPH_REASON_LIMIT !== "undefined") return ALIAS_RECENT_GRAPH_REASON_LIMIT;
    if (name === "ALIAS_RECENT_GRAPH_EXTRA_MAX_LEN" && typeof ALIAS_RECENT_GRAPH_EXTRA_MAX_LEN !== "undefined") return ALIAS_RECENT_GRAPH_EXTRA_MAX_LEN;
  } catch(e) {}
  return fallback;
}

function graphCleanEvidenceSnippet(text, maxLen) {
  maxLen = parseInt(maxLen || graphAliasRecentValue("ALIAS_RECENT_COOCUR_EVIDENCE_MAX_LEN", 180), 10);
  if (isNaN(maxLen) || maxLen <= 0) maxLen = 180;
  var s = graphSafeString(text || "", maxLen * 2);
  s = s.replace(/[\r\n\t]+/g, " ").replace(/[ ]{2,}/g, " ").trim();
  if (s.length > maxLen) s = s.substring(0, maxLen);
  return s;
}

function graphBuildAdjacentDialogEvidence(prevItem, currItem) {
  prevItem = prevItem || {};
  currItem = currItem || {};
  var pn = graphNormalizeName(prevItem.name || "");
  var cn = graphNormalizeName(currItem.name || "");
  var pt = graphSafeString(prevItem.text || prevItem.dialog || prevItem.content || prevItem.line || "", 80);
  var ct = graphSafeString(currItem.text || currItem.dialog || currItem.content || currItem.line || "", 80);
  return graphCleanEvidenceSnippet((pn ? pn + "：" : "") + pt + " / " + (cn ? cn + "：" : "") + ct);
}

function graphPushCooccurEvidence(stats, a, b, kind, text, meta) {
  if (!stats || !ENABLE_ALIAS_COOCUR_STATS) return false;
  var st = graphGetPairStats(stats, a, b);
  if (!st) return false;
  if (!st.evidence || !Array.isArray(st.evidence)) st.evidence = [];
  meta = meta || {};
  var curChap = meta.chapterId || graphCurrentChapterId();
  var ev = {
    chapter: graphSafeString(curChap || "", 80),
    kind: graphSafeString(kind || "共现证据", 60),
    text: graphCleanEvidenceSnippet(text || kind || "共现证据"),
    decision: graphSafeString(meta.decision || "", 30),
    evidenceKey: graphSafeString(meta.evidenceKey || "", 160),
    evidenceHash: graphSafeString(meta.evidenceHash || "", 80),
    batchKey: graphSafeString(meta.batchKey || "", 80),
    relationId: graphSafeString(meta.relationId || "", 80),
    relationType: graphSafeString(meta.relationType || "", 60),
    evidenceFamily: graphSafeString(meta.evidenceFamily || "", 80),
    evidenceSubtype: graphSafeString(meta.evidenceSubtype || "", 80),
    anchorType: graphSafeString(meta.anchorType || "", 80),
    summary: graphSafeString(meta.summary || "", 180),
    source: graphSafeString(meta.source || "", 60),
    time: graphNowIso()
  };
  if (!ev.text) return false;
  for (var i = 0; i < st.evidence.length; i++) {
    var old = st.evidence[i] || {};
    if (old.chapter === ev.chapter && old.kind === ev.kind && ((ev.evidenceKey && old.evidenceKey === ev.evidenceKey) || (!ev.evidenceKey && old.text === ev.text))) return false;
  }
  st.evidence.push(ev);
  var limit = parseInt(graphAliasRecentValue("ALIAS_RECENT_COOCUR_EVIDENCE_STORE_LIMIT", 12), 10);
  if (isNaN(limit) || limit <= 0) limit = 12;
  if (st.evidence.length > limit) st.evidence = st.evidence.slice(-limit);
  return true;
}

function graphFilterRecentEvidence(evidence, recentChapters, limit) {
  var out = [];
  if (!evidence || !Array.isArray(evidence)) return out;
  limit = parseInt(limit || graphAliasRecentValue("ALIAS_RECENT_COOCUR_EVIDENCE_LIMIT", 4), 10);
  if (isNaN(limit) || limit <= 0) limit = 4;
  for (var i = 0; i < evidence.length; i++) {
    var ev = evidence[i] || {};
    if (recentChapters && recentChapters.length && ev.chapter && recentChapters.indexOf(ev.chapter) === -1) continue;
    out.push({
      chapter: graphSafeString(ev.chapter || "", 40),
      kind: graphSafeString(ev.kind || "", 60),
      text: graphCleanEvidenceSnippet(ev.text || ""),
      decision: graphSafeString(ev.decision || "", 30),
      evidenceKey: graphSafeString(ev.evidenceKey || "", 160),
      relationId: graphSafeString(ev.relationId || "", 80),
      batchKey: graphSafeString(ev.batchKey || "", 80)
    });
  }
  if (out.length > limit) out = out.slice(-limit);
  return out;
}

function graphSafeString(v, maxLen) {
  var s = "";
  try { s = String(v == null ? "" : v); } catch (e) { s = ""; }
  if (maxLen && s.length > maxLen) return s.substring(0, maxLen);
  return s;
}

function graphNowIso() {
  try { return new Date().toISOString(); } catch (e) { return "" + Date.now(); }
}

function graphShortLog(msg) {
  if (!GRAPH_SIMPLE_LOG) return;
  try { console.log("【🕸图谱】" + graphSafeString(msg, 80)); } catch (e) {}
}

function aliasShortLog(msg) {
  if (!GRAPH_SIMPLE_LOG) return;
  try { console.log("【🔵别名】" + graphSafeString(msg, 80)); } catch (e) {}
}

function semanticShortLog(msg) {
  if (!GRAPH_SIMPLE_LOG) return;
  try { console.log("【🟣语义证据】" + graphSafeString(msg, 80)); } catch (e) {}
}

function auditShortLog(msg) {
  if (!GRAPH_SIMPLE_LOG) return;
  try { console.log("【🟣证据审计】" + graphSafeString(msg, 80)); } catch (e) {}
}

function conflictShortLog(msg) {
  if (!GRAPH_SIMPLE_LOG) return;
  try { console.log("【🟠冲突校验】" + graphSafeString(msg, 80)); } catch (e) {}
}

function aliasRefineShortLog(msg) {
  if (!GRAPH_SIMPLE_LOG) return;
  try { console.log("【🟦别名清洗】" + graphSafeString(msg, 80)); } catch (e) {}
}

function graphRemoteEnabled() {
  return !!(ENABLE_REMOTE_UPLOAD && ENABLE_GRAPH_REMOTE_UPLOAD && GRAPH_REMOTE_ENDPOINT);
}

function graphEventName(type) {
  var map = {
    "cooccur_scan_start": "共现扫描开始",
    "cooccur_scan_done": "共现扫描完成",
    "graph_positive_edge": "正图谱记录",
    "graph_book_cache": "\u4e66\u7c4d\u56fe\u8c31\u7f13\u5b58",
    "graph_negative_edge": "反图谱记录",
    "alias_check_start": "别名校验开始",
    "alias_recent_chapter_hint": "别名最近章节辅助",
    "alias_recent_role_list_removed": "最近角色列表移除",
    "alias_llm_raw_request": "别名模型原始请求",
    "alias_llm_raw_response": "别名模型原始返回",
    "name_llm_raw_request": "姓名分析模型原始请求",
    "name_llm_raw_response": "姓名分析模型原始返回",
    "alias_refine_llm_raw_request": "别名清洗模型原始请求",
    "alias_refine_llm_raw_response": "别名清洗模型原始返回",
    "graph_conflict_llm_raw_request": "冲突校验模型原始请求",
    "graph_conflict_llm_raw_response": "冲突校验模型原始返回",
    "graph_conflict_empty_choices_retry": "冲突校验空返回重试",
    "alias_graph_hint": "图谱提示命中",
    "alias_check_result": "别名校验结果",
    "alias_refine_result": "别名清洗结果",
    "alias_refine_graph_hint": "别名清洗局部三维辅助",
    "alias_refine_main_redirect": "别名清洗主名重定向",
    "alias_new_name_candidate_without_record": "别名新名字尚无角色卡",
    "alias_reuse_existing_target": "别名复用已有目标角色卡",
    "alias_main_promotion_in_place": "别名主名原角色卡就地晋升",
    "alias_main_redirect_new_record_blocked": "别名主名晋升阻止新建角色卡",
    "alias_refine_removed_aliases_applied": "别名清洗剔除应用",
    "alias_merge_confirmed": "别名合并确认",
    "alias_final_decision_observe": "别名最终决策观察",
    "alias_existing_role_reuse_observe": "别名已有角色复用观察",
    "alias_new_role_fallback_observe": "别名新建兜底观察",
    "voice_assigned": "发音人轮询",
    "special_voice_assigned": "特殊说话人发音人分配",
    "alias_check_queue_created": "新名字别名校验队列创建",
    "alias_check_queue_item_start": "新名字别名校验项开始",
    "alias_check_queue_item_done": "新名字别名校验项完成",
    "alias_check_queue_observation_registered": "新名字别名校验观察队列已登记（未完成）",
    "name_analysis_dialog_cache_hit": "姓名分析对白缓存命中",
    "name_analysis_dialog_cache_miss": "姓名分析对白缓存未命中",
    "name_analysis_dialog_cache_written": "姓名分析对白缓存已写入",
    "name_analysis_batch_text_snapshot": "姓名分析本批提取文本快照",
    "name_analysis_dialog_cache_snapshot": "姓名分析对白缓存完整快照",
    "name_analysis_batch_alignment": "姓名分析批次序号对齐",
    "name_analysis_alignment_retry": "姓名分析对齐失败重试",
    "name_analysis_alignment_retry_success": "姓名分析对齐重试成功",
    "name_analysis_alignment_blocked": "姓名分析对齐失败已阻断",
    "name_analysis_batch_result_used": "姓名分析批量结果直接使用",
    "new_role_create_begin": "新角色创建开始",
    "new_role_create_after_alias": "别名校验后创建新角色",
    "new_role_create_without_alias": "未经过别名校验创建新角色",
    "model_relation_audit_missing_decision": "模型证据审计缺失裁决",
    "name_semantic_self_title_blocked": "姓名语义自称称号证据拒收",
    "model_relation_apply": "模型关系证据",
    "model_relation_detail": "模型关系明细",
    "model_relation_blocked": "\u6a21\u578b\u5173\u7cfb\u62e6\u622a",
    "alias_merge_blocked": "\u522b\u540d\u5408\u5e76\u62e6\u622a",
    "triad_closure": "三角闭合",
    "positive_chain_closure": "正链闭合",
    "graph_closure_skip": "闭合跳过",
    "graph_conflict_verify_start": "图谱冲突校验开始",
    "graph_conflict_verify_payload": "图谱冲突校验输入",
    "graph_conflict_verify_result": "图谱冲突校验结果",
    "graph_conflict_fix": "图谱冲突修正",
    "graph_conflict_verify_skip": "图谱冲突校验跳过",
    "remote_chapter_upload": "章节日志上传",
    "name_analysis_parsed_result": "姓名分析解析摘要",
    "special_speaker_bypass_character_record": "特殊说话人跳过角色库",
    "narration_name_fixed_to_narrator": "旁白名字修正",
    "model_same_person_auto_positive_suspended": "同人正边暂停",
    "alias_check_inconsistent_result": "别名校验矛盾结果",
    "state_alias_name_normalized": "状态说明名归一",
    "alias_split_by_conflict": "冲突拆分别名",
    "role_record_restored": "角色记录恢复",
    "compound_graph_edge": "复合图谱边落地",
    "compound_graph_skipped": "复合图谱跳过",
    "alias_recent_compound_hint": "别名复合证据辅助",
    "alias_refine_compound_hint": "别名清洗复合证据",
    "duplicate_alias_main_conflict_start": "主名别名冲突校验开始",
    "duplicate_alias_main_conflict_result": "主名别名冲突校验结果",
    "duplicate_alias_main_conflict_fix": "主名别名冲突修复",
    "alias_main_map_conflict_skipped": "别名映射冲突跳过",
    "role_record_merged": "角色记录合并",
    "identity_substitution_evidence": "身份替代证据",
    "alias_bridge_gate_blocked": "别名桥接闸门拦截",
    "alias_refine_bridge_gate_blocked": "别名清洗桥接闸门拦截",
    "graph_positive_bridge_gate_blocked": "正图谱桥接闸门拦截",
    "positive_chain_bridge_gate_blocked": "正链桥接闸门拦截",
    "alias_bridge_cleanup_removed": "桥接污染别名清理",
    "merged_records_backup_saved": "分书角色备份保存",
    "merged_records_backup_loaded": "分书角色备份读取",
    "role_record_restore_voice_fallback": "角色恢复音色容错",
    "direct_pair_gate_pass": "直连证据闸门通过",
    "alias_gate_to_conflict_verify": "别名闸门转冲突校验",
    "alias_refine_gate_to_conflict_verify": "清洗闸门转冲突校验",
    "graph_positive_gate_to_conflict_verify": "正图谱闸门转冲突校验",
    "character_records_snapshot": "全量角色列表快照",
    "character_cache_load": "角色缓存读取",
    "character_cache_save": "角色缓存保存",
    "character_book_cache_switch": "角色书籍缓存切换",
    "character_snapshot_source": "角色快照来源",
    "character_chapter_mark_applied": "角色章节标记已写入",
    "character_age_history_added": "角色性别年龄历史已记录",
    "character_age_majority_checked": "角色性别年龄多数检查",
    "character_age_reuse_update_applied": "角色性别年龄复用更新生效",
    "character_age_reuse_update_skipped": "角色性别年龄复用更新跳过",
    "character_age_same_segment_kept": "同年龄段年龄音色保持",
    "character_age_voice_binding_backup_saved": "年龄段音色绑定备份保存",
    "character_age_voice_binding_backup_restored": "年龄段音色绑定备份恢复",
    "character_age_voice_reassigned": "年龄段变化发音人重分配",
    "character_fixed_voice_lock_kept": "固定发音人锁定保持",
    "voice_age_model_raw": "发声音龄模型原始证据",
    "voice_age_precheck": "发声音龄本地预检",
    "voice_age_priority": "发声音龄优先级裁决",
    "voice_age_audit_request": "发声音龄审计请求",
    "voice_age_audit_raw_request": "发声音龄审计原始请求",
    "voice_age_audit_raw_response": "发声音龄审计原始返回",
    "voice_age_audit_retry": "发声音龄审计重试",
    "voice_age_audit_result": "发声音龄审计结果",
    "voice_age_audit_accepted": "发声音龄审计采纳",
    "voice_age_audit_rejected": "发声音龄审计拒收",
    "voice_age_audit_verify": "发声音龄审计转复核",
    "voice_age_audit_exception": "发声音龄审计异常项",
    "voice_age_audit_sparse_apply": "发声音龄稀疏审计应用汇总",
    "voice_age_audit_structure_incomplete": "发声音龄审计结构不完整",
    "combined_audit_request": "合并证据审计请求",
    "combined_audit_raw_request": "合并证据审计原始请求",
    "combined_audit_raw_response": "合并证据审计原始返回",
    "combined_audit_module_status": "合并证据审计模块完整性",
    "combined_audit_partial_preserved": "合并证据审计保留完整模块",
    "combined_audit_single_fallback": "合并证据审计单项降级",
    "combined_audit_multi_incomplete_rejected": "合并证据审计多项不完整整包拒收",
    "combined_audit_bundle_retry": "合并证据审计整包重试",
    "combined_audit_retry_exhausted": "合并证据审计重试耗尽",
    "combined_audit_commit": "合并证据审计按序提交",
    "combined_audit_not_applicable": "合并证据审计模块不适用",
    "standalone_alias_fallback_start": "单独别名检验降级开始",
    "standalone_alias_fallback_result": "单独别名检验降级结果",
    "pending_voice_age_stored": "发声音龄待审证据已暂存",
    "pending_voice_age_applied": "发声音龄待审结果已提交",
    "pending_voice_age_rejected": "发声音龄待审结果已拒收",
    "name_semantic_relation_deferred_to_combined_audit": "姓名语义证据等待合并审计",
    "voice_age_applied": "发声音龄可信更新已应用",
    "voice_age_no_evidence": "发声音龄无可信证据保持原值",
    "voice_age_duplicate": "发声音龄重复证据跳过",
    "voice_age_segment_normalized": "发声音龄年龄段已规范化",
    "voice_age_same_segment_keep": "发声音龄同段保持原发音人",
    "voice_age_same_segment_prefiltered": "发声音龄同段本地预检跳过",
    "voice_age_fixed_voice_prefiltered": "固定音色年龄候选本地跳过",
    "voice_age_same_segment_voice_changed_error": "发声音龄同段异常换声报警",
    "voice_age_book_cache_loaded": "发声音龄分书缓存已读取",
    "voice_age_book_cache_saved": "发声音龄分书缓存已保存",
    "voice_age_book_cache_updated": "发声音龄分书缓存已更新",
    "temporary_voice_feature_config": "临时换声功能配置",
    "temporary_voice_feature_disabled": "临时换声总开关已关闭",
    "temporary_voice_active_state_prompt": "临时换声生效状态已附加到提示词",
    "temporary_voice_state_review_raw": "临时换声状态续判原始返回",
    "temporary_voice_state_review_per_role": "临时换声状态逐角色续判",
    "temporary_voice_state_review_incomplete": "临时换声状态续判不完整",
    "temporary_voice_state_review_fallback_start": "临时换声状态单独续判开始",
    "temporary_voice_state_review_fallback_result": "临时换声状态单独续判结果",
    "temporary_voice_state_review_retry_exhausted": "临时换声状态单独续判重试耗尽",
    "temporary_voice_state_end_detected": "临时换声结束信号已发现",
    "temporary_voice_state_replace_detected": "临时换声替换信号已发现",
    "temporary_voice_state_no_dialogue_carried": "临时换声角色本批无对白继续携带",
    "temporary_voice_state_carried_to_next_batch": "临时换声状态继续携带到下一批",
    "temporary_voice_state_continue_audited": "临时换声续判已经年龄审计采纳",
    "temporary_voice_state_cross_chapter_carried": "临时换声状态携带到顺序下一章",
    "temporary_voice_state_discontinuity_cleared": "临时换声状态因阅读不连续清理",
    "temporary_voice_state_safety_expire": "临时换声状态达到对白安全上限",
    "temporary_voice_transition_applied": "临时换声状态边界已应用",
    "temporary_voice_range_decided": "临时换声本批作用范围已判定",
    "temporary_voice_transition_scheduled": "临时换声状态边界已调度",
    "temporary_voice_state_start": "临时换声状态开始",
    "temporary_voice_state_reuse": "临时换声状态复用",
    "temporary_voice_state_replace": "临时换声状态替换",
    "temporary_voice_state_end": "临时换声状态结束",
    "temporary_voice_state_expire": "临时换声状态安全失效",
    "temporary_voice_cache_saved": "临时换声缓存快照已保存",
    "temporary_voice_cache_restore": "临时换声缓存快照已恢复",
    "temporary_voice_cache_restore_rejected": "临时换声缓存恢复已拒绝",
    "temporary_voice_duplicate": "临时换声重复事件复用",
    "fixed_voice_cancelled": "固定音色已取消",
    "existing_record_reconcile_merge": "现有角色卡同人协调合并",
    "existing_record_reconcile_split": "现有角色卡同人协调拆分",
    "compound_same_person_certified": "复合同人证据认证",
    "compound_different_person_certified": "复合非同人证据认证",
    "compound_evidence_to_conflict_verify": "复合证据转冲突校验",
    "model_relation_anchor_downgraded": "模型关系锚点降级",
    "alias_hint_cleaned": "别名提示清洗",
    "name_semantic_model_raw": "姓名语义模型原始证据",
    "name_semantic_voted_raw": "姓名语义归并后证据",
    "name_semantic_shape_rejected": "姓名语义字段拒收",
    "name_semantic_shape_summary": "姓名语义字段预检汇总",
    "name_semantic_pending": "姓名语义待审计池",
    "model_relation_audit_request": "模型证据审计请求",
    "model_relation_audit_raw_request": "模型证据审计原始请求",
    "model_relation_audit_raw_response": "模型证据审计原始返回",
    "model_relation_audit_result": "模型证据审计结果",
    "model_relation_audit_retry": "模型证据审计重试",
    "model_relation_audit_incomplete_retry": "模型证据审计数组不完整重试",
    "model_relation_audit_non_json_retry": "模型证据审计非 JSON 重试",
    "model_relation_audit_retry_success": "模型证据审计重试成功",
    "model_relation_audit_accepted": "模型证据审计采纳",
    "model_relation_audit_rejected": "模型证据审计拒收",
    "model_relation_audit_downgraded": "模型证据审计降级",
    "model_relation_audit_to_verify": "模型证据审计转复核",
    "model_relation_audit_prompt_policy": "模型证据审计分类型策略",
    "model_relation_audit_leak_observe": "模型证据审计放行规模观察",
    "model_relation_audit_all_accepted_verification": "模型证据审计全部采纳核验",
    "model_relation_audit_exception_summary": "模型证据审计异常项汇总",
    "model_relation_name_identity_cross_batch_case": "模型身份关系跨批说话人案例",
    "compound_self_reference_removed": "复合自引用已清理",
    "compound_self_reference_blocked": "复合自引用已阻断",
    "role_record_split": "角色卡拆分",
    "role_record_merge_call_observe": "角色卡合并调用观察",
    "role_record_merge_duplicate_suspected": "角色卡疑似重复合并调用",
    "name_semantic_model_raw_summary": "姓名语义模型原始统计",
    "character_record_empty_chapters": "角色章节为空提示",
    "relation_descriptor_positive_blocked": "关系描述正证拦截",
    "positive_chain_blocked_by_descriptor": "正链关系描述拦截",
    "graph_conflict_apply": "图谱冲突应用",
    "name_analysis_recent_role_hint": "姓名分析最近角色提示",
    "name_analyze_narration_rule_hint": "姓名分析旁白规则提示",
    "alias_check_with_relation_audit": "别名校验附带证据审计",
    "alias_evidence_observed": "别名证据观察",
    "name_semantic_relation_audit_only": "语义证据仅审计记录",
    "name_semantic_group_field_check": "语义归并字段检查",
    "alias_check_relation_audit_consumed": "别名校验接管本批待审计证据",
    "alias_check_relation_audit_missing": "别名校验缺少审计返回",
    "name_semantic_relation_deferred_to_alias_check": "语义证据等待别名审计",
    "model_relation_audit_deferred_alias_queue_empty": "别名队列结束后转单独审计",
    "model_relation_audit_pending_overwrite_fallback": "待审计证据覆盖前兜底审计",
    "model_relation_audit_deferred_waiting_alias": "模型证据审计等待别名校验",
    "alias_refine_start": "别名清洗开始",
    "alias_refine_raw_request": "别名清洗原始请求",
    "alias_refine_raw_response": "别名清洗原始返回",
    "model_relation_audit_apply": "模型证据审计落图统计",
    "alias_check_embedded_audit_incomplete": "别名内嵌证据审计不完整",
    "character_book_switch_records_replaced": "切书后角色记录已替换",
    "character_external_update_alias_conflict": "外部角色更新别名冲突",
    "character_external_update_detected": "检测到外部角色更新",
    "character_external_update_error": "外部角色更新异常",
    "character_external_update_merged": "外部角色更新已合并",
    "character_external_update_rejected": "外部角色更新已拒绝",
    "compound_source_reason_prefiltered": "复合证据来源预过滤",
    "graph_audit_suggestion": "图谱审计建议",
    "merged_record_backup_not_reused": "合并角色备份未复用",
    "name_analysis_cross_chapter_role_hit": "姓名分析跨章角色命中",
    "name_analysis_cross_chapter_role_skipped": "姓名分析跨章角色跳过统计",
    "name_analysis_reuse_table_final": "姓名分析角色复用表定稿",
    "persistent_compound_duplicate_evidence_skipped": "持久复合证据重复跳过",
    "persistent_compound_graph_edge": "持久复合图谱边",
    "persistent_compound_name_identity_hint": "持久复合姓名身份提示",
    "persistent_compound_name_identity_subtype_scan": "持久复合姓名身份子类扫描",
    "persistent_compound_reconcile_error": "持久复合角色协调异常",
    "persistent_compound_record_reconcile": "持久复合角色记录协调",
    "persistent_compound_scan_done": "持久复合证据扫描完成",
    "persistent_compound_scan_error": "持久复合证据扫描异常",
    "persistent_compound_scan_start": "持久复合证据扫描开始",
    "persistent_compound_signature_skipped": "持久复合证据签名跳过",
    "speaker_mapping_preserve_narrator": "说话人映射保留旁白",
    "speaker_system_result_trace": "系统说话人结果追踪",
    "name_analysis_next_context_extended": "姓名分析下文外延",
  };
  return map[type] || ("未命名事件：" + (type || "图谱事件"));
}

function graphCnEventName(type) {
  return graphEventName(type);
}

function graphCanonicalRemoteEvent(eventType, data) {
  eventType = graphSafeString(eventType || "", 120);
  data = data || {};
  // 停用远程重复事件，只保留新版语义证据分层与统一角色拆分事件。
  if (eventType === "name_semantic_relations_raw") return null;
  if (eventType === "name_semantic_relation_raw") return null;
  if (eventType === "name_semantic_relation_shape_passed") return null;
  if (eventType === "name_semantic_relation_shape_rejected") return null;
  if (eventType === "name_semantic_relation_pending") return null;
  if (eventType === "duplicate_alias_main_conflict_fix" && data && data.action === "merge_records") return null;
  if (eventType === "role_record_restored") return null;
  if (eventType === "alias_split_by_conflict") return { eventType: "role_record_split", data: data };
  return { eventType: eventType, data: data };
}

function graphRemoteShouldCoalesce(eventType) {
  if (!ENABLE_REMOTE_LOG_STATE_COALESCE) return false;
  return eventType === "character_cache_save" ||
    eventType === "character_chapter_mark_applied" ||
    eventType === "voice_age_no_evidence" ||
    eventType === "persistent_compound_signature_skipped" ||
    eventType === "name_analyze_narration_rule_hint" ||
    eventType === "temporary_voice_feature_config";
}

function graphRemoteCoalesceKey(eventType, chapterKey, chapterIndex, data) {
  return eventType + "|" + graphSafeString(chapterKey || "", 160) + "|" + graphSafeString(chapterIndex || "", 60) + "|" + graphHash(JSON.stringify(data || {}));
}

function graphRemoteRebuildCoalesceIndex() {
  graphRemoteCoalesceIndex = {};
  for (var i = 0; i < (graphRemoteQueue || []).length; i++) {
    var event = graphRemoteQueue[i] || {};
    if (!graphRemoteShouldCoalesce(event.eventType || "")) continue;
    graphRemoteCoalesceIndex[graphRemoteCoalesceKey(event.eventType, event.chapterKey, event.chapterIndex, event.data)] = i;
  }
}

function graphRemoteEnsureLoaded() {
  if (graphRemoteEnsureLoaded.loaded) return;
  graphRemoteEnsureLoaded.loaded = true;
  try {
    var saved = graphReadJsonSafe(GRAPH_REMOTE_QUEUE_FILE || "graph_remote_chapter_queue.json", null);
    if (saved && saved.events && Array.isArray(saved.events)) {
      graphRemoteQueue = saved.events;
      graphRemoteChapterKey = saved.chapterKey || "";
      graphRemoteChapterIndex = saved.chapterIndex || "";
      graphRemoteRebuildCoalesceIndex();
    }
  } catch (e) {}
}

function graphRemoteWriteLocal() {
  try {
    graphWriteJsonSafe(GRAPH_REMOTE_QUEUE_FILE || "graph_remote_chapter_queue.json", {
      chapterKey: graphRemoteChapterKey || "",
      chapterIndex: graphRemoteChapterIndex || graphCurrentChapterIndex || "",
      updatedAt: graphNowIso(),
      events: graphRemoteQueue || []
    });
  } catch (e) {}
}

function graphBookCacheSafeKey(bookName, bookUrl) {
  bookName = graphSafeString(bookName || "", 120).trim();
  bookUrl = graphSafeString(bookUrl || "", 500).trim();
  var key = bookName || (bookUrl ? ("book-" + graphHash(bookUrl)) : "default");
  key = key.replace(/[\\/:*?"<>|]/g, "_");
  key = key.replace(/[\s\u3000]+/g, "_");
  key = key.replace(/_+/g, "_");
  key = key.replace(/^_+|_+$/g, "");
  if (!key) key = "default";
  if (key.length > 80) key = key.substring(0, 80);
  return key;
}

function graphRuleDataVersion() {
  var v = "";
  try {
    if (typeof SpeechRuleJS !== "undefined" && SpeechRuleJS && SpeechRuleJS.version) v = SpeechRuleJS.version;
  } catch(e0) {}
  if (!v) {
    try {
      var m = String(typeof GRAPH_RULE_SOURCE !== "undefined" ? GRAPH_RULE_SOURCE : "").match(/v(\d+)/i);
      if (m && m[1]) v = m[1];
    } catch(e1) {}
  }
  if (!v) v = "908"; // 与 JSON 顶层 version 统一，避免缓存和日志出现旧版本号。
  v = String(v).replace(/^v/i, "");
  return "v" + v;
}

function graphBookCacheFile(prefix, bookKey) {
  bookKey = graphBookCacheSafeKey(bookKey || "default", "");
  return prefix + "." + graphRuleDataVersion() + "." + bookKey + ".json";
}

function graphBuildStableChapterKey(bookUrl, chapterIndex) {
  bookUrl = graphSafeString(bookUrl || "", 500);
  chapterIndex = graphSafeString(chapterIndex == null ? "" : chapterIndex, 80);
  if (bookUrl || chapterIndex) return "chapter:" + graphHash(bookUrl + "#" + chapterIndex);
  return "chapter:unknown";
}

function graphBuildChapterKey(text) {
  if (typeof graphCurrentChapterKey !== "undefined" && graphCurrentChapterKey) return graphCurrentChapterKey;
  if (typeof graphCurrentChapterIndex !== "undefined" && graphCurrentChapterIndex !== "") return "chapter:" + graphCurrentChapterIndex;
  return "chapter:unknown";
}

function graphFlushDanglingPendingNameSemanticRelations(reason, chapterFullContent) {
  try {
    var mgr = (typeof characterManager !== "undefined") ? characterManager : null;
    if (!mgr || !mgr.pendingNameSemanticRelations) return { skipped: true, reason: "no_manager_or_pending" };
    var p = mgr.pendingNameSemanticRelations;
    if (!p || p.consumed || !p.relations || !p.relations.length) return { skipped: true, reason: "no_unconsumed_pending" };
    if (p.auditBuffered) return { skipped: true, reason: "waiting_ordered_graph_commit" };
    if (!p.hasNewRoleCandidate) return { skipped: true, reason: "pending_not_waiting_alias" };
    if (!mgr.auditPendingNameSemanticRelationsIfNeeded) return { skipped: true, reason: "audit_function_missing" };
    graphRemoteLog("model_relation_audit_deferred_alias_queue_empty", {
      relationCount: p.relations.length,
      batchNames: (p.batchNames || []).slice(0, 40),
      reason: reason || "alias_queue_completed",
      chapterId: p.chapterId || "",
      batchKey: p.batchKey || "",
      relationIds: p.relations.map(function(r){ return r && r.relationId || ""; }).slice(0, 80)
    });
    return mgr.auditPendingNameSemanticRelationsIfNeeded(chapterFullContent || p.chapterText || "", {
      forceStandalone: true,
      forceReason: reason || "alias_queue_completed"
    });
  } catch (e) {
    try { graphRemoteLog("model_relation_audit_deferred_alias_queue_empty", { error: graphSafeString(e && e.message || e, 260), reason: reason || "alias_queue_completed" }); } catch(e2) {}
    return { skipped: true, error: String(e && e.message || e || "") };
  }
}

// 只把“同书、章节编号顺序+1”视为可携带临时状态的章节切换。
// 这里仅做结构连续性判断；新章首批仍必须把状态交给模型重新续判并经过年龄审计。
function graphV908IsSequentialChapterIndex(oldIndex, newIndex) {
  var oldNumber = Number(String(oldIndex == null ? "" : oldIndex).match(/\d+/) || NaN);
  var newNumber = Number(String(newIndex == null ? "" : newIndex).match(/\d+/) || NaN);
  return isFinite(oldNumber) && isFinite(newNumber) && newNumber === oldNumber + 1;
}

function graphSetCurrentChapterKey(bookUrl, chapterIndex) {
  try {
    graphRemoteEnsureLoaded();
    var newBookUrl = graphSafeString(bookUrl || "", 500);
    var newIndex = graphSafeString(chapterIndex == null ? "" : chapterIndex, 80);
    var newKey = graphBuildStableChapterKey(newBookUrl, newIndex);
    var oldIndex = graphCurrentChapterIndex || graphRemoteChapterIndex || "";
    var oldBookUrl = graphCurrentBookUrl || "";
    var oldKey = graphCurrentChapterKey || graphRemoteChapterKey || "";
    var realSwitch = (oldIndex !== "" && newIndex !== "" && String(oldIndex) !== String(newIndex)) || (oldBookUrl !== "" && newBookUrl !== "" && oldBookUrl !== newBookUrl);

    if (realSwitch) {
      try {
        var managerForTemporaryVoice = (typeof characterManager !== "undefined") ? characterManager : null;
        var oldAndNewBookSame = !!(oldBookUrl && newBookUrl && oldBookUrl === newBookUrl);
        var mayCarryToNextChapter = ENABLE_TEMPORARY_VOICE_STATE && ENABLE_TEMPORARY_VOICE_CROSS_CHAPTER && oldAndNewBookSame && graphV908IsSequentialChapterIndex(oldIndex, newIndex);
        if (managerForTemporaryVoice && managerForTemporaryVoice.temporaryVoiceStates) {
          if (mayCarryToNextChapter) {
            var carriedStateIds = [];
            for (var temporaryStateKey in managerForTemporaryVoice.temporaryVoiceStates) {
              if (!managerForTemporaryVoice.temporaryVoiceStates.hasOwnProperty(temporaryStateKey)) continue;
              var temporaryState = managerForTemporaryVoice.temporaryVoiceStates[temporaryStateKey] || {};
              temporaryState.previousChapterId = temporaryState.chapterId || graphSafeString(oldIndex, 80);
              temporaryState.chapterId = graphSafeString(newIndex, 80);
              temporaryState.crossChapterCarryPending = true;
              temporaryState.carriedAt = graphNowIso();
              carriedStateIds.push(temporaryState.stateId || temporaryStateKey);
            }
            if (carriedStateIds.length) graphRemoteLog("temporary_voice_state_cross_chapter_carried", { oldChapterId: graphSafeString(oldIndex, 80), newChapterId: graphSafeString(newIndex, 80), stateIds: carriedStateIds, stateCount: carriedStateIds.length, policy: "同书顺序下一章先携带，新章首批必须重新模型续判并审计" });
          } else if (managerForTemporaryVoice.clearTemporaryVoiceStates) {
            var clearReason = oldBookUrl !== newBookUrl ? "book_switch" : (ENABLE_TEMPORARY_VOICE_STATE ? "non_sequential_chapter_switch" : "temporary_voice_feature_disabled");
            var stateCountBeforeClear = Object.keys(managerForTemporaryVoice.temporaryVoiceStates || {}).length;
            managerForTemporaryVoice.clearTemporaryVoiceStates(clearReason);
            if (stateCountBeforeClear) graphRemoteLog("temporary_voice_state_discontinuity_cleared", { reason: clearReason, oldChapterId: graphSafeString(oldIndex, 80), newChapterId: graphSafeString(newIndex, 80), clearedStateCount: stateCountBeforeClear });
          }
        }
      } catch(tempStateChapterErr) {}
      graphFlushDanglingPendingNameSemanticRelations("chapter_switch_alias_queue_completed", "");
      graphRemoteFlushChapter("真实章节切换", oldKey || graphRemoteChapterKey || ("chapter:" + oldIndex), "章节切换", oldIndex);
    }

    graphCurrentBookUrl = newBookUrl;
    graphCurrentChapterIndex = newIndex;
    graphCurrentChapterKey = newKey;
    graphRemoteChapterKey = newKey;
    graphRemoteChapterIndex = newIndex;
    try {
      graphAliasRecentChapterAppend(newIndex);
      graphAliasRecentChapterSave();
    } catch(aliasChapErr) {}
    graphRemoteWriteLocal();
  } catch (e) {}
}

function graphRemoteSetChapter(chapterKey, label) {
  if (!graphRemoteEnabled()) return;
  graphRemoteEnsureLoaded();
  chapterKey = graphSafeString(chapterKey || graphBuildChapterKey(""), 120);
  if (!graphRemoteChapterKey) {
    graphRemoteChapterKey = chapterKey;
    graphRemoteChapterIndex = graphCurrentChapterIndex || graphRemoteChapterIndex || "";
    graphRemoteWriteLocal();
  }
}

function graphRemoteLog(eventType, data) {
  if (!graphRemoteEnabled()) return;
  try {
    graphRemoteEnsureLoaded();
    data = data || {};
    var canonicalEvent = graphCanonicalRemoteEvent ? graphCanonicalRemoteEvent(eventType, data) : { eventType: eventType, data: data };
    if (!canonicalEvent) return;
    eventType = canonicalEvent.eventType;
    data = canonicalEvent.data || data || {};
    var chapterKey = graphRemoteChapterKey || graphCurrentChapterKey || "";
    var chapterIndex = graphRemoteChapterIndex || graphCurrentChapterIndex || "";
    var now = graphNowIso();
    if (graphRemoteShouldCoalesce(eventType)) {
      var coalesceKey = graphRemoteCoalesceKey(eventType, chapterKey, chapterIndex, data);
      var existingIndex = graphRemoteCoalesceIndex[coalesceKey];
      var existingEvent = typeof existingIndex === "number" ? graphRemoteQueue[existingIndex] : null;
      if (existingEvent && existingEvent.eventType === eventType && existingEvent.chapterKey === chapterKey && existingEvent.chapterIndex === chapterIndex) {
        existingEvent.repeatCount = Number(existingEvent.repeatCount || 1) + 1;
        existingEvent.lastTime = now;
        graphRemoteWriteLocal();
        return;
      }
    }
    graphRemoteQueue.push({
      source: GRAPH_RULE_SOURCE,
      eventType: eventType,
      cnEvent: graphCnEventName(eventType),
      chapterKey: chapterKey,
      chapterIndex: chapterIndex,
      time: now,
      data: data || {}
    });
    if (graphRemoteShouldCoalesce(eventType)) graphRemoteCoalesceIndex[coalesceKey] = graphRemoteQueue.length - 1;
    if (graphRemoteQueue.length > GRAPH_REMOTE_MAX_QUEUE) {
      while (graphRemoteQueue.length > GRAPH_REMOTE_MAX_QUEUE) graphRemoteQueue.shift();
      graphRemoteRebuildCoalesceIndex();
    }
    graphRemoteWriteLocal();
  } catch (e) {}
}


function graphBuildCharacterRecordsSnapshot(chapterIndex) {
  try {
    var mgr = (typeof characterManager !== "undefined") ? characterManager : null;
    if (!mgr || !mgr.characterRecords || !Array.isArray(mgr.characterRecords)) return null;
    var bookKey = mgr.aliasGraphBookKey || "default";
    var out = [];
    for (var i = 0; i < mgr.characterRecords.length; i++) {
      var r = mgr.characterRecords[i];
      if (!r) continue;
      var chapters = Array.isArray(r.chapters) ? r.chapters.slice(0) : [];
      var mainName = graphNormalizeName(r.name || "");
      var chapterFallback = graphSafeString(r.lastSeenChapter || r.lastSeen || r.chapterIndex || "", 40);
      if (!chapters.length && chapterFallback) chapters = [chapterFallback];
      var chaptersEmpty = chapters.length === 0;
      var lastSeen = chapters.length ? chapters[chapters.length - 1] : "";
      var backupAvailable = false;
      try { backupAvailable = !!(mgr.mergedRecords && mgr.mergedRecords[mainName]); } catch(e1) {}
      out.push({
        mainName: mainName,
        aliases: graphSafeString(r.aliases || "", 500),
        gender: graphSafeString(r.gender || "", 20),
        age: graphSafeString(r.age || "", 30),
        voice: graphSafeString(r.voice || "", 80),
        voiceId: graphSafeString(r.voiceId || r.voiceKey || "", 120),
        chapters: chapters,
        usageCount: Number(r.usageCount || 0),
        lastSeenChapter: lastSeen,
        merged: !!r.mergedInto,
        mergedInto: graphNormalizeName(r.mergedInto || ""),
        backupAvailable: backupAvailable,
        chaptersEmptyWarning: chaptersEmpty
      });
    }
    return { source: GRAPH_RULE_SOURCE, eventType: "character_records_snapshot", cnEvent: graphCnEventName("character_records_snapshot"), chapterIndex: graphSafeString(chapterIndex || graphCurrentChapterIndex || "", 40), bookKey: bookKey, recordCount: out.length, records: out, time: graphNowIso() };
  } catch(e) { return null; }
}

function graphRemoteFlushChapter(reason, chapterKey, label, chapterIndex) {
  if (!graphRemoteEnabled()) return;
  graphRemoteEnsureLoaded();
  if (!graphRemoteQueue || graphRemoteQueue.length === 0) return;
  var eventsToSend = graphRemoteQueue.slice(0);
  var sendKey = chapterKey || graphRemoteChapterKey || "unknown";
  var sendIndex = chapterIndex || graphRemoteChapterIndex || graphCurrentChapterIndex || "";
  var characterSnapshot = graphBuildCharacterRecordsSnapshot ? graphBuildCharacterRecordsSnapshot(sendIndex) : null;
  if (characterSnapshot) eventsToSend.push(characterSnapshot);
  graphRemoteQueue = [];
  graphRemoteCoalesceIndex = {};
  graphRemoteWriteLocal();
  graphShortLog("上传章节日志" + eventsToSend.length + "条");

  var runner = function() {
    try {
      var headers = { "Content-Type": "application/json", "Connection": "keep-alive" };
      if (GRAPH_REMOTE_TOKEN) headers["Authorization"] = "Bearer " + GRAPH_REMOTE_TOKEN;
      var payload = {
        source: GRAPH_RULE_SOURCE,
        eventType: "remote_chapter_upload",
        cnEvent: graphCnEventName("remote_chapter_upload"),
        chapterKey: sendKey,
        chapterIndex: sendIndex,
        label: label || "",
        reason: reason || "",
        eventCount: eventsToSend.length,
        time: graphNowIso(),
        characterRecordsSnapshot: characterSnapshot,
        events: eventsToSend
      };
      try { ttsrv.httpPost(GRAPH_REMOTE_ENDPOINT, JSON.stringify(payload), headers); } catch (postErr) {}
    } catch (e) {}
  };
  try {
    if (typeof java !== "undefined" && java.lang && java.lang.Thread) {
      var thread = new java.lang.Thread(new java.lang.Runnable({ run: runner }));
      thread.start();
    } else {
      runner();
    }
  } catch (e2) {}
}

function graphReadJsonSafe(fileName, fallback) {
  try {
    var content = ttsrv.readTxtFile(fileName);
    var text = content != null ? String(content).trim() : "";
    if (!text) return fallback;
    var parsed = JSON.parse(text);
    return parsed || fallback;
  } catch (e) {
    return fallback;
  }
}

function graphWriteJsonSafe(fileName, data) {
  try {
    ttsrv.writeTxtFile(fileName, JSON.stringify(data || {}, null, 2));
    return true; // 中文注释：写入成功必须明确返回true，避免上层把成功误记为失败
  } catch (e) {
    return false; // 中文注释：写入异常明确返回false，由具体调用方记录失败原因
  }
}

function graphNormalizeName(name) {
  return graphSafeString(name, 40).trim();
}


function graphNormalizeStateAliasName(name) {
  name = graphNormalizeName(name);
  if (!name) return "";
  var m = name.match(/^(.{1,18})[（\(]([^）\)]{1,40})(?:[）\)])$/);
  if (m && /(附身|被附身|附体|操控|被操控|控制|被控制|傀儡|借体|寄身|变身|夺舍中|状态)/.test(m[2])) {
    return graphNormalizeName(m[1]);
  }
  return name;
}

function graphIsGroupName(name) {
  if (!ENABLE_GRAPH_GROUP_NAME_FILTER) return false;
  name = graphNormalizeName(name);
  if (!name) return false;
  if (/^(众人|众修士|众弟子|诸人|诸修|二人|两人|三人|四人|几人|数人|一行人|一群人|众女|众男)$/.test(name)) return true;
  if (/^[一二两三四五六七八九十数几0-9]+(名|个|位|群).*(修士|女子|男子|弟子|人|老者|大汉|少年|少女|儒生|汉子)$/.test(name)) return true;
  if (/(众人|众修士|众弟子|四名|三名|两名|数名|几名|一群|一行)$/.test(name)) return true;
  return false;
}

function graphIsAliasGroupName(name) {
  name = graphNormalizeName(name);
  if (!name) return false;
  if (graphIsGroupName(name)) return true;
  if (/^(\u4f17\u4eba|\u4f17\u4fee\u58eb|\u4f17\u5f1f\u5b50|\u8bf8\u4eba|\u8bf8\u4fee|\u4e8c\u4eba|\u4e24\u4eba|\u4e09\u4eba|\u56db\u4eba|\u51e0\u4eba|\u6570\u4eba|\u4e00\u884c\u4eba|\u4e00\u7fa4\u4eba|\u5176\u4ed6\u51e0\u4eba|\u5176\u4ed6\u4fee\u58eb|\u5728\u5750\u4fee\u58eb)$/.test(name)) return true;
  if (/[\u4e00\u4e8c\u4e24\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d\u5341\u6570\u51e00-9]+(\u540d|\u4e2a|\u4f4d|\u4eba).*(\u4fee\u58eb|\u5973\u5b50|\u7537\u5b50|\u5f1f\u5b50|\u8001\u8005|\u5927\u6c49|\u5c11\u5e74|\u5c11\u5973|\u5112\u751f|\u6c49\u5b50|\u6cd5\u58eb|\u957f\u8001|\u50e7\u4eba|\u4eba)/.test(name)) return true;
  if (/^(\u9ad8\u77ee|\u4e00\u9ad8\u4e00\u77ee|\u4e00\u80d6\u4e00\u7626|\u4e00\u7537\u4e00\u5973|\u4e24\u7537|\u4e24\u5973).*(\u4fee\u58eb|\u7537\u5b50|\u5973\u5b50|\u6cd5\u58eb|\u8001\u8005|\u4eba)/.test(name)) return true;
  if (/(\u4e8c\u4eba|\u4e24\u4eba|\u4e09\u4eba|\u56db\u4eba|\u51e0\u4eba|\u6570\u4eba|\u4f17\u4eba|\u4e00\u884c\u4eba|\u4e00\u7fa4\u4eba)$/.test(name)) return true;
  return false;
}

function graphAliasMergeBlockReason(a, b) {
  if (!ENABLE_ALIAS_GROUP_MEMBER_MERGE_BLOCK) return "";
  a = graphNormalizeName(a);
  b = graphNormalizeName(b);
  if (!a || !b || a === b) return "";
  var groupA = graphIsAliasGroupName(a);
  var groupB = graphIsAliasGroupName(b);
  if (groupA !== groupB) return "\u7fa4\u4f53/\u5355\u4eba\u4e0d\u5408\u5e76";
  return "";
}

function graphV908AliasFinalDecisionStatus(aliasApiResult, aliasAfterBlock, targetMainRecord, blockReason) {
  var apiIsAlias = !!(aliasApiResult && aliasApiResult.isAlias);
  var apiMainName = graphSafeString(aliasApiResult && aliasApiResult.mainName || "", 80);
  var finalIsAlias = !!(aliasAfterBlock && aliasAfterBlock.isAlias);
  var finalMainName = graphSafeString(aliasAfterBlock && aliasAfterBlock.mainName || "", 80);

  if (blockReason) {
    return "alias_blocked_to_not_alias";
  }
  if (apiIsAlias && !finalIsAlias) {
    return "alias_api_true_final_false";
  }
  if (finalIsAlias && finalMainName && targetMainRecord) {
    return "is_alias_reuse_existing";
  }
  if (finalIsAlias && finalMainName && !targetMainRecord) {
    return "is_alias_main_missing";
  }
  if (apiIsAlias && apiMainName && !targetMainRecord) {
    return "is_alias_api_main_missing";
  }
  return "not_alias";
}

function graphV908AliasObservePayload(stage, newName, aliasApiResult, aliasAfterBlock, targetMainRecord, blockReason, extra) {
  extra = extra || {};
  var apiIsAlias = !!(aliasApiResult && aliasApiResult.isAlias);
  var finalIsAlias = !!(aliasAfterBlock && aliasAfterBlock.isAlias);
  var apiMainName = graphSafeString(aliasApiResult && aliasApiResult.mainName || "", 80);
  var finalMainName = graphSafeString(aliasAfterBlock && aliasAfterBlock.mainName || "", 80);
  var targetName = targetMainRecord ? graphSafeString(targetMainRecord.name || targetMainRecord.mainName || "", 80) : "";
  var status = graphV908AliasFinalDecisionStatus(aliasApiResult, aliasAfterBlock, targetMainRecord, blockReason);

  var payload = {
    stage: graphSafeString(stage || "", 80),
    newName: graphSafeString(newName || "", 80),
    aliasApiIsAlias: apiIsAlias,
    aliasApiMainName: apiMainName,
    aliasFinalIsAlias: finalIsAlias,
    aliasFinalMainName: finalMainName,
    aliasTargetFound: !!targetMainRecord,
    aliasTargetName: targetName,
    aliasBlockReason: graphSafeString(blockReason || "", 200),
    aliasFinalDecision: status,
    chapterIndex: graphCurrentChapterId()
  };

  for (var k in extra) {
    if (extra.hasOwnProperty(k)) payload[k] = extra[k];
  }
  return payload;
}

function graphSpecialSpeakerType(name, gender, age) {
  name = graphNormalizeName(name);
  gender = graphSafeString(gender || "", 20);
  age = graphSafeString(age || "", 20);
  if (name === "旁白" || (gender === "特殊" && age === "旁白")) return "旁白";
  if (name === "系统" || (gender === "特殊" && age === "系统")) return "系统";
  return "";
}

function graphPairExplicitContradiction(a, b, reason) {
  a = graphNormalizeName(a);
  b = graphNormalizeName(b);
  reason = graphSafeString(reason || "", 700);
  if (!a || !b || !reason) return false;
  var ea = graphEscapeRegExp(a);
  var eb = graphEscapeRegExp(b);
  var gap = "[^。！？\n]{0,16}";
  var relationWords = "师父|师傅|师尊|徒弟|弟子|手下|属下|下属|父亲|母亲|儿子|女儿|妻子|丈夫|老板|上司|领导|同事|同学|朋友|亲戚|敌人|仇人|道侣|主人|仆人";
  var regs = [
    new RegExp(ea + gap + "(不是|并非|绝非|并不是|非)" + gap + eb),
    new RegExp(eb + gap + "(不是|并非|绝非|并不是|非)" + gap + ea),
    new RegExp(ea + gap + "(和|与|跟|及)" + gap + eb + gap + "(不是|并非|绝非|并不是|非)" + gap + "(同一人|同一个人|一人|别名)"),
    new RegExp(eb + gap + "(和|与|跟|及)" + gap + ea + gap + "(不是|并非|绝非|并不是|非)" + gap + "(同一人|同一个人|一人|别名)"),
    new RegExp(ea + gap + "(和|与|跟|及)" + gap + eb + gap + "(是|为|属于)?" + gap + "(两个人|不同人物|不同的人|不同角色|两个角色)"),
    new RegExp(eb + gap + "(和|与|跟|及)" + gap + ea + gap + "(是|为|属于)?" + gap + "(两个人|不同人物|不同的人|不同角色|两个角色)"),
    new RegExp(ea + gap + "只是" + gap + eb + "的(" + relationWords + ")"),
    new RegExp(eb + gap + "只是" + gap + ea + "的(" + relationWords + ")"),
    new RegExp(ea + gap + "(正在|曾经)?(对|向|朝|冲)" + gap + eb + gap + "(说话|说道|问道|对话|交谈)" + gap + "(说明|表明).{0,12}(二者|两人|他们|她们)" + gap + "(不同|不是同一人|非同一人)"),
    new RegExp(eb + gap + "(正在|曾经)?(对|向|朝|冲)" + gap + ea + gap + "(说话|说道|问道|对话|交谈)" + gap + "(说明|表明).{0,12}(二者|两人|他们|她们)" + gap + "(不同|不是同一人|非同一人)")
  ];
  for (var i = 0; i < regs.length; i++) {
    try { if (regs[i].test(reason)) return true; } catch (e) {}
  }
  return false;
}

function graphAliasCheckReasonContradiction(result, newName, mainName) {
  if (!ENABLE_ALIAS_CHECK_REASON_CONSISTENCY || !result || !result.isAlias) return "";
  var reason = graphSafeString(result.reason || "", 700);
  var a = graphNormalizeName(newName || "");
  var b = graphNormalizeName(mainName || (result && result.mainName) || "");
  if (!reason || !a || !b) return "";
  // 一致性检查只做绝对高精度字段-理由矛盾修正；不再扫描“附身/操控/假冒/直接对话”等泛词。
  if (graphPairExplicitContradiction(a, b, reason)) return "isAlias=true但reason明确绑定当前pair为非同人/关系/直接对话";
  return "";
}


// ===================== direct-pair evidence gate（正向写入门槛，不做反向判死）=====================
function graphIdentitySubstitutionType(text) {
  // 身份替代语义不再由本地关键词正则判定，统一迁移到批量姓名分析 __relations 语义通道。
  // 本函数保留为空实现，只用于兼容旧调用点，避免本地再生产 identity_substitution_evidence。
  return "";
}

function graphHasDirectPairEvidenceText(a, b, text) {
  a = graphNormalizeName(a); b = graphNormalizeName(b);
  text = graphSafeString(text || "", 4000);
  if (!a || !b || !text) return false;
  var ea = graphEscapeRegExp(a), eb = graphEscapeRegExp(b);
  var tight = "[\\s\\u3000,，、:：·・\\-—()（）【】《》〈〉“”‘’]{0,6}";
  var gap = "[^。！？\\n]{0,50}";
  var aliasCue = "(本名|真名|原名|又名|别名|又称|也叫|名叫|名为|名唤|叫做|叫作|自称|号称|人称|道号|法号|尊号|就是|即是|即为|即|正是|便是|乃是|也就是|其实就是|同一人|同一个人|化名|扮作|假扮|冒充|顶替|取代|化为|变成|被称为|被称作|称为|称作|介绍为|引见为)";
  var regs = [
    new RegExp(ea + gap + aliasCue + gap + eb),
    new RegExp(eb + gap + aliasCue + gap + ea),
    new RegExp(ea + "[（(]" + eb + "[）)]"),
    new RegExp(eb + "[（(]" + ea + "[）)]"),
    new RegExp("(介绍|引见|引荐|将|把)" + gap + ea + gap + "(介绍为|引见为)" + gap + eb),
    new RegExp("(介绍|引见|引荐|将|把)" + gap + eb + gap + "(介绍为|引见为)" + gap + ea)
  ];
  for (var i = 0; i < regs.length; i++) {
    try { if (regs[i].test(text)) return true; } catch (e) {}
  }
  // 身份替代直接结构：只作为“可进入证据裁决”的直连证据，不自动作为正链闭合强证。
  if (graphIdentitySubstitutionType(text)) {
    var near1 = new RegExp(ea + gap + eb).test(text);
    var near2 = new RegExp(eb + gap + ea).test(text);
    if (near1 || near2) return true;
  }
  return false;
}


function graphNormalizeVariantForGate(name) {
  name = graphNormalizeName(name);
  if (!name) return "";
  // 只做极少量人工确认的字形差异，不开放编辑距离，避免把不同人物误合。
  return name.replace(/薰/g, "熏").replace(/儒衫/g, "儒装");
}

function graphIsWhitelistedNameVariant(a, b) {
  a = graphNormalizeName(a); b = graphNormalizeName(b);
  if (!a || !b || a === b) return false;
  return graphNormalizeVariantForGate(a) === graphNormalizeVariantForGate(b);
}

function graphGateShouldApplyToPositiveReason(reason) {
  reason = graphSafeString(reason || "", 100);
  // 本地封闭式 本地封闭式 已删除；冲突确认同人是高强度证据，不再重复过普通正边 gate。
  if (reason === "graph_conflict_verified_same_person") return false;
  return true;
}

function graphReasonHasBridgeRisk(a, b, reason) {
  reason = graphSafeString(reason || "", 1200);
  if (!reason) return false;
  // 高风险桥接：A=X，X=B；或依赖历史图谱/复用表/小说背景，而没有当前pair原文。
  if (/(而[^。！？\n]{1,24}(即|就是|本名|为|是)[^。！？\n]{1,24})/.test(reason)) return true;
  if (/(最近N章正图谱|正图谱证据显示|角色复用表|已知角色复用表|已存角色列表|小说背景|根据背景|据小说背景|无歧义|应为同一人|前文已知|历史内容中明确)/.test(reason)) return true;
  return false;
}

function graphStrictPositiveReasons(edge) {
  var out = [];
  if (!edge || !edge.reasons) return out;
  for (var i = 0; i < edge.reasons.length; i++) {
    var r = graphSafeString(edge.reasons[i] || "", 80);
    if (r === "graph_conflict_verified_same_person" || r === "alias_refine_confirmed" || r === "model_name_identity_positive") out.push(r);
  }
  return out;
}

function graphPairHasStrictPositive(manager, a, b) {
  if (!manager || !manager.aliasPositiveGraph) return false;
  var edge = graphGetEdgeSnapshot(manager.aliasPositiveGraph, a, b);
  if (!edge || Number(edge.score || 0) <= 0) return false;
  return graphStrictPositiveReasons(edge).length > 0;
}

CharacterManager.prototype.cleanupBridgeAliasIfExists = function(mainRecord, aliasName, gateReason) {
  if (!mainRecord || !aliasName) return false;
  var alias = graphNormalizeName(aliasName);
  if (!alias || alias === graphNormalizeName(mainRecord.name)) return false;
  var removed = this.removeAliasFromRecord ? this.removeAliasFromRecord(mainRecord, alias) : false;
  if (removed) {
    if (this.rebuildNameToMainNameMap) this.rebuildNameToMainNameMap();
    this.saveRecords();
    graphRemoteLog("alias_bridge_cleanup_removed", { mainName: graphNormalizeName(mainRecord.name), aliasName: alias, reason: graphSafeString(gateReason || "direct-pair gate blocked", 220) });
  }
  return removed;
};

function graphIsRelationDescriptorName(name) {
  name = graphNormalizeName(name);
  if (!name) return false;
  if (name === "旁白" || name === "系统" || name === "未知") return false;
  var relationWords = "弟子|徒弟|学生|门生|老师|师父|师傅|父亲|母亲|儿子|女儿|兄弟|姐姐|妹妹|妻子|丈夫|夫君|夫人|道侣|未婚夫|未婚妻|前男友|前女友|下人|仆人|仆从|侍女|丫鬟|属下|手下|部下|上司|老板|秘书|助理|保镖|司机|管家|员工|队员|成员|门人|同伴|朋友|敌人|仇人|同学|同事|队友|主人";
  var relationRe = new RegExp("(" + relationWords + ")");
  if (!relationRe.test(name)) return false;
  if (new RegExp("的[^|]{0,16}(" + relationWords + ")").test(name)) return true;
  if (/^(某|这|那|该|本|此)?(门派|宗门|门中|宫中|府中|族中|家族|公司|集团|组织|队伍|团队|帮派|商盟|学校|学院|部门|机构|势力)/.test(name) && relationRe.test(name)) return true;
  if (name.length >= 3 && /(亲传弟子|内门弟子|外门弟子|关门弟子|门下弟子|下人|仆人|仆从|侍女|丫鬟|属下|手下|部下|秘书|助理|保镖|司机|管家|员工|队员|成员|门人|同事|同学|队友)$/.test(name)) return true;
  if (name.length >= 4 && /(父亲|母亲|儿子|女儿|妻子|丈夫|夫君|未婚夫|未婚妻|前男友|前女友|朋友|敌人|仇人|同伴)$/.test(name)) return true;
  return false;
}

function graphRelationDescriptorBlockReason(a, b) {
  var da = graphIsRelationDescriptorName(a);
  var db = graphIsRelationDescriptorName(b);
  if (da || db) return "关系/身份描述不可作为同人正证";
  return "";
}

function graphIsInvalidName(name) {
  name = graphNormalizeName(name);
  if (!name) return true;
  if (name === "未知" || name === "旁白" || name === "系统") return true;
  if (name.indexOf("群众") === 0) return true;
  if (graphIsGroupName(name)) return true;
  if (name.length > 16) return true;
  if (/^(男|女|男人|女人|男子|女子|老者|少年|少女|青年|中年|老人|小孩|师兄|师姐|师弟|师妹)$/.test(name)) return true;
  return false;
}

function graphEscapeRegExp(str) {
  return graphSafeString(str, 80).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function graphHash(text) {
  text = graphSafeString(text, 4000);
  var h = 0;
  for (var i = 0; i < text.length; i++) {
    h = ((h << 5) - h + text.charCodeAt(i)) | 0;
  }
  return String(h);
}

function graphPairKey(a, b) {
  a = graphNormalizeName(a);
  b = graphNormalizeName(b);
  return a < b ? a + "||" + b : b + "||" + a;
}

function graphSplitAliases(record) {
  var out = [];
  function addOne(x) {
    x = graphNormalizeName(x);
    if (!x || out.indexOf(x) !== -1) return;
    out.push(x);
  }
  if (!record) return out;
  addOne(record.name);
  var arr = graphSafeString(record.aliases, 300).split("|");
  for (var i = 0; i < arr.length; i++) addOne(arr[i]);
  return out;
}

function graphAddWeightedEdge(graph, a, b, score, reason, extra, evidenceKey, meta) {
  a = graphNormalizeName(a);
  b = graphNormalizeName(b);
  if (!a || !b || a === b) return false;
  if (!graph[a]) graph[a] = {};
  if (!graph[b]) graph[b] = {};
  if (!graph[a][b]) graph[a][b] = { score: 0, count: 0, reasons: [], lastSeen: "", chapters: [], evidenceSamples: [] };
  if (!graph[b][a]) graph[b][a] = { score: 0, count: 0, reasons: [], lastSeen: "", chapters: [], evidenceSamples: [] };
  meta = meta || {};
  function update(edge) {
    edge.score = Math.min(99, Number(edge.score || 0) + Number(score || 0));
    edge.count = Number(edge.count || 0) + 1;
    edge.lastSeen = graphNowIso();
    graphPushChapterMark(edge);
    var r = graphSafeString(reason || "evidence", 80);
    if (!edge.reasons) edge.reasons = [];
    if (edge.reasons.length < 12 && edge.reasons.indexOf(r) === -1) edge.reasons.push(r);
    if (extra) edge.extra = graphSafeString(extra, 180);
    var evKey = graphSafeString(evidenceKey || meta.evidenceKey || "", 120);
    var evText = graphSafeString(extra || meta.evidenceText || "", 260);
    if (evKey || evText) {
      if (!edge.evidenceSamples || !Array.isArray(edge.evidenceSamples)) edge.evidenceSamples = [];
      var sample = {
        chapter: graphSafeString(meta.chapterId || graphCurrentChapterId(), 80),
        reason: r,
        evidenceKey: evKey,
        evidenceHash: graphSafeString(meta.evidenceHash || "", 80),
        batchKey: graphSafeString(meta.batchKey || "", 80),
        relationId: graphSafeString(meta.relationId || "", 80),
        relationType: graphSafeString(meta.relationType || "", 60),
        evidenceFamily: graphSafeString(meta.evidenceFamily || "", 80),
        evidenceSubtype: graphSafeString(meta.evidenceSubtype || "", 80),
        anchorType: graphSafeString(meta.anchorType || "", 80),
        source: graphSafeString(meta.source || "", 60),
        text: graphSafeString(evText, 260),
        time: graphNowIso()
      };
      var dup = false;
      for (var si = 0; si < edge.evidenceSamples.length; si++) {
        var old = edge.evidenceSamples[si] || {};
        if (old.chapter === sample.chapter && old.reason === sample.reason && ((sample.evidenceKey && old.evidenceKey === sample.evidenceKey) || (!sample.evidenceKey && old.text === sample.text))) { dup = true; break; }
      }
      if (!dup) {
        edge.evidenceSamples.push(sample);
        if (edge.evidenceSamples.length > 20) edge.evidenceSamples = edge.evidenceSamples.slice(edge.evidenceSamples.length - 20);
      }
    }
  }
  update(graph[a][b]);
  update(graph[b][a]);
  return true;
}

function graphGetEdgeScore(graph, a, b) {
  a = graphNormalizeName(a);
  b = graphNormalizeName(b);
  if (!a || !b || !graph || !graph[a] || !graph[a][b]) return 0;
  return Number(graph[a][b].score || 0);
}

function graphGetEdgeReasons(graph, a, b) {
  a = graphNormalizeName(a);
  b = graphNormalizeName(b);
  if (!a || !b || !graph || !graph[a] || !graph[a][b]) return [];
  return graph[a][b].reasons || [];
}

function graphGetEdgeSnapshot(graph, a, b) {
  a = graphNormalizeName(a);
  b = graphNormalizeName(b);
  if (!a || !b || !graph || !graph[a] || !graph[a][b]) return null;
  var e = graph[a][b] || {};
  return {
    score: Number(e.score || 0),
    count: Number(e.count || 0),
    reasons: e.reasons || [],
    extra: graphSafeString(e.extra || "", 180),
    lastSeen: graphSafeString(e.lastSeen || "", 40),
    chapters: e.chapters || [],
    evidenceSamples: e.evidenceSamples || []
  };
}

function graphRemoveEdge(graph, a, b) {
  a = graphNormalizeName(a);
  b = graphNormalizeName(b);
  if (!a || !b || !graph) return false;
  var removed = false;
  if (graph[a] && graph[a][b]) { delete graph[a][b]; removed = true; }
  if (graph[b] && graph[b][a]) { delete graph[b][a]; removed = true; }
  return removed;
}

function graphParseJsonObject(text) {
  text = graphSafeString(text || "", 4000).replace(/\x60\x60\x60json|\x60\x60\x60/g, "").trim();
  try { return JSON.parse(text); } catch (e) {}
  var start = text.indexOf("{");
  var end = text.lastIndexOf("}");
  if (start >= 0 && end > start) {
    try { return JSON.parse(text.substring(start, end + 1)); } catch (e2) {}
  }
  return null;
}

function graphNormalizeVerifiedRelation(v) {
  v = graphSafeString(v || "", 60).toLowerCase();
  if (v === "control_host" || v.indexOf("control") !== -1 || v.indexOf("host") !== -1 || v.indexOf("附身") !== -1 || v.indexOf("操控") !== -1 || v.indexOf("宿主") !== -1 || v.indexOf("傀儡") !== -1) return "different_person";
  if (v === "different" || v === "different_person" || v === "negative" || v.indexOf("不同") !== -1 || v.indexOf("不是") !== -1 || v.indexOf("非") !== -1 || v.indexOf("different") !== -1 || v.indexOf("not") !== -1) return "different_person";
  if (v === "same" || v === "same_person" || v === "positive" || v.indexOf("同一") !== -1 || v.indexOf("同人") !== -1 || v.indexOf("别名") !== -1 || v.indexOf("alias") !== -1) return "same_person";
  return "uncertain";
}

function graphCooccurMarkChapter(stats, a, b) {
  if (!stats || !ENABLE_ALIAS_COOCUR_STATS) return;
  a = graphNormalizeName(a);
  b = graphNormalizeName(b);
  if (!a || !b || a === b) return;
  var key = graphPairKey(a, b);
  if (!stats[key]) return;
  graphPushChapterMark(stats[key]);
}

function graphGetPairStats(stats, a, b) {
  if (!stats) return null;
  a = graphNormalizeName(a);
  b = graphNormalizeName(b);
  if (!a || !b || a === b) return null;
  if (graphIsGroupName(a) || graphIsGroupName(b)) return null;
  var key = graphPairKey(a, b);
  if (!stats[key]) {
    stats[key] = { a: a, b: b, chapterCount: 0, sameSentence: 0, adjacentSpeaker: 0, directInteraction: 0, listedTogether: 0, explicitRelation: 0, positiveMention: 0, modelPositive: 0, modelNegative: 0, updatedAt: "", chapters: [], evidence: [] };
  }
  if (!stats[key].chapters || !Array.isArray(stats[key].chapters)) stats[key].chapters = [];
  if (!stats[key].evidence || !Array.isArray(stats[key].evidence)) stats[key].evidence = [];
  return stats[key];
}

function graphCurrentChapterId() {
  return graphSafeString((typeof graphCurrentChapterIndex !== "undefined" && graphCurrentChapterIndex) || (typeof graphRemoteChapterIndex !== "undefined" && graphRemoteChapterIndex) || "unknown", 80);
}

function graphPruneSeenMap(seenMap) {
  try {
    var keys = Object.keys(seenMap || {});
    var max = parseInt(GRAPH_CHAPTER_EVIDENCE_MAX, 10) || 3000;
    if (keys.length <= max) return;
    keys.sort(function(a, b) { return String(seenMap[a]).localeCompare(String(seenMap[b])); });
    while (keys.length > max) delete seenMap[keys.shift()];
  } catch (e) {}
}

function graphBuildEvidenceHashText(text) {
  var normalized = graphEvidenceNormalizeText(text || "");
  if (normalized.length > 800) normalized = normalized.substring(0, 800);
  return normalized;
}

function graphBuildEvidenceHash(text) {
  var normalized = graphBuildEvidenceHashText(text || "");
  if (!normalized) return "";
  return graphHash(normalized);
}

function graphBuildRelationEvidenceMeta(r, batchKey) {
  r = r || {};
  var evidenceText = graphSafeString(r.evidenceText || r.evidence || r.summary || "", 1200);
  var evidenceHash = graphSafeString(r.evidenceHash || graphBuildEvidenceHash(evidenceText), 80);
  var reason = graphSafeString(r.reason || graphRelationReasonFromFamily(r.relationType, r.evidenceFamily) || "evidence", 100);
  var pair = graphPairKey(r.a || "", r.b || "");
  var chapterId = graphSafeString(r.chapterId || graphCurrentChapterId(), 80);
  var bk = graphSafeString(r.batchKey || batchKey || "", 80);
  var evidenceKey = graphSafeString(r.evidenceKey || (pair + "|" + reason + "|" + evidenceHash), 180);
  return { chapterId: chapterId, batchKey: bk, evidenceHash: evidenceHash, evidenceKey: evidenceKey, reason: reason, evidenceText: evidenceText };
}

function graphEnsureRelationEvidenceMeta(r, batchKey) {
  if (!r) return r;
  var meta = graphBuildRelationEvidenceMeta(r, batchKey || r.batchKey || "");
  r.chapterId = r.chapterId || meta.chapterId;
  r.batchKey = r.batchKey || meta.batchKey;
  r.evidenceHash = r.evidenceHash || meta.evidenceHash;
  r.evidenceKey = r.evidenceKey || meta.evidenceKey;
  if (!r.evidence && r.evidenceText) r.evidence = r.evidenceText;
  return r;
}

function graphMarkChapterEvidenceOnce(stats, a, b, reason, evidenceKey, evidenceText) {
  if (!ENABLE_GRAPH_CHAPTER_DEDUP || !stats) return true;
  a = graphNormalizeName(a);
  b = graphNormalizeName(b);
  if (!a || !b || a === b) return false;
  if (!stats.__chapterEvidenceSeen) stats.__chapterEvidenceSeen = {};
  var evKey = graphSafeString(evidenceKey || "", 180);
  if (!evKey) {
    var evHash = graphBuildEvidenceHash(evidenceText || "");
    evKey = evHash ? ("txt:" + evHash) : "reason_only";
  }
  var key = graphCurrentChapterId() + "|" + graphPairKey(a, b) + "|" + graphSafeString(reason || "", 80) + "|" + evKey;
  if (stats.__chapterEvidenceSeen[key]) return false;
  stats.__chapterEvidenceSeen[key] = graphNowIso();
  graphPruneSeenMap(stats.__chapterEvidenceSeen);
  return true;
}

function graphIsNegativeClosureReason(reason) {
  // 不同人三角闭合退休。反向闭合不再以任何本地/模型反证作为新产出种子。
  return false;
}

function graphIsPositiveClosureReason(reason) {
  reason = graphSafeString(reason || "", 80);
  return reason === "model_name_identity_positive" ||
    reason === "alias_refine_confirmed" ||
    reason === "graph_conflict_verified_same_person";
}

function graphEdgeHasClosureReason(edge, positive) {
  if (!edge || !edge.reasons) return false;
  for (var i = 0; i < edge.reasons.length; i++) {
    if (positive ? graphIsPositiveClosureReason(edge.reasons[i]) : graphIsNegativeClosureReason(edge.reasons[i])) return true;
  }
  return false;
}

function graphCollectClosureNeighbors(graph, name, positive) {
  var out = [];
  name = graphNormalizeName(name);
  if (!graph || !graph[name]) return out;
  var max = parseInt(GRAPH_CLOSURE_MAX_NEIGHBORS, 10) || 80;
  for (var n in graph[name]) {
    if (!graph[name].hasOwnProperty(n)) continue;
    if (out.length >= max) break;
    n = graphNormalizeName(n);
    if (!n || n === name || graphIsInvalidName(n)) continue;
    if (positive && graphIsRelationDescriptorName(n)) continue;
    if (graphEdgeHasClosureReason(graph[name][n], positive)) out.push(n);
  }
  return out;
}

function graphPruneScans(scanMap) {
  try {
    var keys = Object.keys(scanMap || {});
    if (keys.length <= 80) return;
    keys.sort(function(a, b) { return String(scanMap[a]).localeCompare(String(scanMap[b])); });
    while (keys.length > 80) delete scanMap[keys.shift()];
  } catch (e) {}
}

CharacterManager.prototype.loadAliasGraphData = function() {
  this.aliasPositiveGraph = graphReadJsonSafe(this.aliasPositiveGraphFile || "alias_positive_graph.json", {});
  this.aliasNegativeGraph = graphReadJsonSafe(this.aliasNegativeGraphFile || "alias_negative_graph.json", {});
};

CharacterManager.prototype.saveAliasGraphData = function() {
  graphWriteJsonSafe(this.aliasPositiveGraphFile || "alias_positive_graph.json", this.aliasPositiveGraph || {});
  graphWriteJsonSafe(this.aliasNegativeGraphFile || "alias_negative_graph.json", this.aliasNegativeGraph || {});
};

CharacterManager.prototype.loadAliasCooccurStats = function() {
  this.aliasCooccurStats = graphReadJsonSafe(this.aliasCooccurStatsFile || "alias_cooccur_stats.json", {});
};

CharacterManager.prototype.saveAliasCooccurStats = function() {
  graphWriteJsonSafe(this.aliasCooccurStatsFile || "alias_cooccur_stats.json", this.aliasCooccurStats || {});
};


CharacterManager.prototype.restoreVoiceWithFallback = function(record, fallbackGender, fallbackAge) {
  if (!record) return { voiceRestored: false, voiceFallback: false, oldVoice: "", newVoice: "", fallbackReason: "no_record" };
  var oldVoice = record.voice || record.voiceId || "";
  var ok = !!(oldVoice && this.isVoiceAvailable && this.isVoiceAvailable(oldVoice));
  if (ok) {
    record.voice = oldVoice;
    return { voiceRestored: true, voiceFallback: false, oldVoice: oldVoice, newVoice: oldVoice, fallbackReason: "" };
  }
  var reason = oldVoice ? "backup_voice_missing_or_unavailable" : "backup_voice_missing";
  var newVoice = this.assignVoice ? this.assignVoice(record.gender || fallbackGender || "男", record.age || fallbackAge || "男青年", { targetName: record.name || "", assignType: "角色恢复音色容错", sourceStage: "restore_voice_fallback", afterAliasCheck: false, isSpecialSpeaker: false }) : "";
  if (!newVoice) newVoice = (record.gender || fallbackGender) === "女" ? "duihuaB" : ((record.gender || fallbackGender) === "男" ? "duihuaA" : "duihua");
  record.voice = newVoice;
  graphRemoteLog("role_record_restore_voice_fallback", { name: graphNormalizeName(record.name || ""), oldVoice: oldVoice, newVoice: newVoice, fallbackReason: reason });
  return { voiceRestored: false, voiceFallback: true, oldVoice: oldVoice, newVoice: newVoice, fallbackReason: reason };
};

CharacterManager.prototype.resetGraphConflictVerifyBudgetIfNeeded = function() {
  var chapterId = graphCurrentChapterId();
  if (this.graphConflictVerifyChapterId !== chapterId) {
    this.graphConflictVerifyChapterId = chapterId;
    this.graphConflictVerifyCount = 0;
    this.graphConflictVerifySeen = {};
  }
};

CharacterManager.prototype.graphConflictVerifyBudgetOk = function(a, b, stage, originalSourceReason, evidenceText) {
  this.resetGraphConflictVerifyBudgetIfNeeded();
  var evidenceKey = graphEvidenceNormalizeText(String(evidenceText || "")).substring(0, 160);
  var reasonKey = graphSafeString(originalSourceReason || "", 100);
  var key = graphCurrentChapterId() + "|" + graphPairKey(a, b) + "|" + evidenceKey + "|" + reasonKey;
  if (this.graphConflictVerifySeen[key]) {
    graphRemoteLog("graph_conflict_verify_skip", { a: graphNormalizeName(a), b: graphNormalizeName(b), stage: stage || "", reason: "same_pair_evidence_seen", originalSourceReason: reasonKey, evidenceKey: evidenceKey });
    return false;
  }
  this.graphConflictVerifySeen[key] = graphNowIso();
  this.graphConflictVerifyCount = Number(this.graphConflictVerifyCount || 0) + 1;
  return true;
};

CharacterManager.prototype.buildGraphConflictVerifyPrompt = function(payload) {
  return "你是小说人物正反图谱冲突校验器。请只根据输入的图谱、共现统计、当前证据和上下文判断两个人名是否同一具体人物。\n" +
    "判定原则：别名、化名、本名、名字、自称、以某名出现、上下文交替称呼通常支持same_person；两人直接对话、并列出现、互相称呼、主仆/朋友/敌对等关系通常支持different_person；证据不足返回uncertain。\n" +
    getV908CharacterNamingAndSpeakerRules("graph_conflict") +
    "【关系描述图谱规则】\n" +
    "关系/身份描述（师徒、亲属、主从、职场、组织、恋爱、敌友、同伴、同事、同学等）可以作为反图谱证据，但不得作为正图谱同人证据；若positive_chain_closed通过同一关系描述把两人闭合为同人，应优先怀疑正链扩散。\n" +
    "【复合证据规则】compound_* 表示同一pair在当前章/最近章同时命中多来源证据；请判断这些来源是否独立、是否被附身/操控/冒充/关系描述误导。复合反证是反向倾向，不是一票否决；复合正证必须有明确本名/介绍/括号同人或冲突确认同人支撑。\n" +
    "【主角色与别名冲突规则】若一个名字既是独立主角色又被加入另一角色aliases，必须判断是否应完整合并或拆分。附身、操控、傀儡、借体、夺舍、假冒、顶替、冒充等身份替代关系不机械判同人/非同人，必须按当前文本和新旧证据裁决；若判same_person应merge_records，若判different_person应remove_alias_keep_separate或拆分。\n\n" +
    "评分标准：\n" +
    "1. 明确出现A是B的名字、B是A的名字、A以B之名、B以A之名、A自称B、B自称A、A即B、A就是B、A名为B、B名为A、介绍A为B、这位是B、描述名A后被称为B、上下文多次交替称呼且无互相对话矛盾时，relation=same_person，confidence必须>=90；A是描述名而B是介绍出的称呼名时，A与第三方互动不能直接证明A≠B。\n" +
    "2. 明确出现A与B直接对话、互相称呼、A和B并列、二人/双方/我二人、主仆/师徒/敌友/契约等两人关系，且没有明确别名证据时，relation=different_person，confidence必须>=85。\n" +
    "3. 如果正图谱证据只来自positive_chain_closed，但反图谱有直接对话、并列、模型different_person等强证据，优先判different_person，confidence>=85，并标记wrongSide=positive。\n" +
    "4. 如果反图谱证据只来自model_same_person_blocked或共现误伤，但当前证据明确是名字/化名/自称，优先判same_person，confidence>=90，并标记wrongSide=negative。\n" +
    "5. 只有证据互相矛盾且无法分辨、或上下文不足时，才返回uncertain或confidence<80。\n" +
    "如果图谱里已有一边明显由误写、串名、闭合扩散造成，请指出应保留哪一边。\n" +
    "必须只输出JSON：{\"relation\":\"same_person|different_person|uncertain\",\"confidence\":0-100,\"wrongSide\":\"positive|negative|none\",\"reason\":\"简短中文原因\"}\n" +
    "输入：\n" + JSON.stringify(payload, null, 2);
};

CharacterManager.prototype.callGraphConflictVerifier = function(prompt, meta) {
  meta = meta || {};
  try {
    var apiList = DualKeyManager.getAvailableApiList("aliasAnalyze", 1);
    if (!apiList || apiList.length === 0) throw new Error("无可用别名校验API");
    var apiConfig = apiList[0];
    var requestTimeout = parseInt(GRAPH_CONFLICT_VERIFY_TIMEOUT, 10) || ALIAS_ANALYZE_TIMEOUT || 45000;
    var requestData = {
      model: apiConfig.model,
      messages: [
        { role: "system", content: "严格遵守格式要求，仅输出JSON，格式错误则任务失败" },
        { role: "user", content: prompt }
      ],
      temperature: 0.1
    };
    var headers = {
      "Content-Type": "application/json",
      "Authorization": "Bearer " + apiConfig.key,
      "Connection": "keep-alive",
      "Timeout": String(requestTimeout)
    };
    if (ENABLE_MODEL_RAW_REMOTE_LOG) {
      graphRemoteLog("graph_conflict_llm_raw_request", {
        scene: "graph_conflict",
        a: graphNormalizeName(meta.a || ""),
        b: graphNormalizeName(meta.b || ""),
        stage: graphSafeString(meta.stage || "", 80),
        incomingKind: graphSafeString(meta.incomingKind || "", 30),
        endpoint: graphSafeString(apiConfig.endpoint || "", 200),
        model: graphSafeString(apiConfig.model || "", 80),
        requestData: graphSafeString(JSON.stringify(requestData), MODEL_RAW_REMOTE_LOG_MAX_LEN)
      });
    }
    var maxEmptyChoicesRetry = parseInt(GRAPH_CONFLICT_EMPTY_CHOICES_RETRY_MAX, 10);
    if (isNaN(maxEmptyChoicesRetry) || maxEmptyChoicesRetry < 0) maxEmptyChoicesRetry = 0;
    var emptyChoicesRetry = 0;
    var body = "{}";
    var outer = null;
    while (true) {
      var response = ttsrv.httpPost(apiConfig.endpoint, JSON.stringify(requestData), headers);
      body = String(response.body().string() || "{}");
      if (ENABLE_MODEL_RAW_REMOTE_LOG) {
        graphRemoteLog("graph_conflict_llm_raw_response", {
          scene: "graph_conflict",
          a: graphNormalizeName(meta.a || ""),
          b: graphNormalizeName(meta.b || ""),
          stage: graphSafeString(meta.stage || "", 80),
          incomingKind: graphSafeString(meta.incomingKind || "", 30),
          requestAttempt: emptyChoicesRetry + 1,
          responseBody: graphSafeString(body, MODEL_RAW_REMOTE_LOG_MAX_LEN)
        });
      }
      outer = JSON.parse(body);
      if (outer.choices && outer.choices[0] && outer.choices[0].message) break;
      if (emptyChoicesRetry >= maxEmptyChoicesRetry) throw new Error("缺少choices[0].message，空返回重试已耗尽");
      emptyChoicesRetry++;
      graphRemoteLog("graph_conflict_empty_choices_retry", {
        scene: "graph_conflict",
        a: graphNormalizeName(meta.a || ""),
        b: graphNormalizeName(meta.b || ""),
        stage: graphSafeString(meta.stage || "", 80),
        incomingKind: graphSafeString(meta.incomingKind || "", 30),
        retryIndex: emptyChoicesRetry,
        maxRetry: maxEmptyChoicesRetry,
        reason: "缺少choices[0].message"
      });
    }
    var content = String(outer.choices[0].message.content || "").trim();
    var parsed = graphParseJsonObject(content);
    if (!parsed) throw new Error("校验JSON解析失败");
    return {
      relation: graphNormalizeVerifiedRelation(parsed.relation || parsed.result || parsed.type),
      confidence: Number(parsed.confidence || parsed.score || 0),
      wrongSide: graphSafeString(parsed.wrongSide || parsed.wrong || parsed.errorSide || "", 20),
      reason: graphSafeString(parsed.reason || parsed.evidence || "", 300),
      raw: graphSafeString(content, 500)
    };
  } catch (e) {
    return { relation: "failed", confidence: 0, wrongSide: "", reason: graphSafeString(e && e.message ? e.message : e, 260), raw: "" };
  }
};

CharacterManager.prototype.verifyGraphConflictAndFix = function(kind, a, b, score, reason, extra, stage, options) {
  if (!ENABLE_ALIAS_GRAPH || !ENABLE_GRAPH_CONFLICT_MODEL_VERIFY) return { allow: true, verified: false };
  options = options || {};
  var defaultAllow = options.defaultAllow !== false;
  kind = kind === "negative" ? "negative" : "positive";
  a = graphNormalizeName(a);
  b = graphNormalizeName(b);
  if (graphIsInvalidName(a) || graphIsInvalidName(b) || a === b) return { allow: defaultAllow, verified: false };

  var posScore = graphGetEdgeScore(this.aliasPositiveGraph, a, b);
  var negScore = graphGetEdgeScore(this.aliasNegativeGraph, a, b);
  var posMin = Number(GRAPH_CONFLICT_POSITIVE_MIN || GRAPH_POSITIVE_HINT_MIN || 1.5);
  var negMin = Number(GRAPH_CONFLICT_NEGATIVE_MIN || GRAPH_NEGATIVE_SOFT_BLOCK || 1.0);
  var forceVerify = !!options.forceVerify;
  var hasConflict = forceVerify || (kind === "positive" && negScore >= negMin) || (kind === "negative" && posScore >= posMin);
  if (!hasConflict) return { allow: true, verified: false };
  var originalSourceReason = graphSafeString(options.originalSourceReason || options.sourceReason || reason || "", 100);
  if (/^(alias_gate_to_conflict_verify|alias_refine_gate_to_conflict_verify|graph_positive_gate_to_conflict_verify)$/.test(originalSourceReason)) originalSourceReason = graphSafeString(reason || "", 100);
  var originalEvidenceText = graphSafeString(options.originalEvidenceText || options.evidenceText || extra || "", 1200);
  if (!originalEvidenceText) originalEvidenceText = graphSafeString(extra || reason || "", 1200);
  if (!this.graphConflictVerifyBudgetOk(a, b, stage || kind, originalSourceReason, originalEvidenceText)) return { allow: true, verified: false, skipped: true, duplicate: true };

  var pairKey = graphPairKey(a, b);
  var stats = this.aliasCooccurStats && this.aliasCooccurStats[pairKey] ? this.aliasCooccurStats[pairKey] : null;
  var contextText = graphSafeString(options.contextText || this.graphConflictVerifyChapterText || this.contextHistory2 || "", 2600);
  var payload = {
    a: a,
    b: b,
    stage: graphSafeString(stage || "", 60),
    incoming: { kind: kind, score: Number(score || 0), reason: graphSafeString(reason || "", 80), originalSourceReason: originalSourceReason, extra: graphSafeString(extra || "", 260) },
    positiveGraph: graphGetEdgeSnapshot(this.aliasPositiveGraph, a, b),
    negativeGraph: graphGetEdgeSnapshot(this.aliasNegativeGraph, a, b),
    cooccurStats: stats,
    modelType: graphSafeString(options.modelType || "", 60),
    originalRelation: graphSafeString(options.originalRelation || "", 60),
    chapterIndex: graphCurrentChapterId(),
    context: contextText.slice(-2600)
  };

  conflictShortLog("开始 " + a + "↔" + b);
  graphRemoteLog("graph_conflict_verify_start", {
    a: a,
    b: b,
    stage: payload.stage,
    incomingKind: kind,
    incomingReason: payload.incoming.reason,
    originalSourceReason: payload.incoming.originalSourceReason,
    positiveScore: posScore,
    negativeScore: negScore,
    forceVerify: forceVerify
  });

  var conflictPrompt = this.buildGraphConflictVerifyPrompt(payload);
  graphRemoteLog("graph_conflict_verify_payload", {
    a: a,
    b: b,
    stage: payload.stage,
    hasPositiveGraph: !!payload.positiveGraph,
    hasNegativeGraph: !!payload.negativeGraph,
    hasCooccurStats: !!payload.cooccurStats,
    contextLen: payload.context ? String(payload.context).length : 0,
    payloadSummary: graphSafeString(JSON.stringify(payload), 3200),
    promptHead: graphSafeString(conflictPrompt, 1800)
  });
  var result = this.callGraphConflictVerifier(conflictPrompt, { a: a, b: b, stage: payload.stage, incomingKind: kind });
  conflictShortLog("结果 " + graphSafeString(result && result.relation || "failed", 30) + " " + Number(result && result.confidence || 0));
  graphRemoteLog("graph_conflict_verify_result", {
    a: a,
    b: b,
    stage: payload.stage,
    incomingKind: kind,
    relation: result.relation,
    confidence: result.confidence,
    wrongSide: result.wrongSide,
    reason: graphSafeString(result.reason, 260)
  });

  var minConfidence = parseInt(GRAPH_CONFLICT_VERIFY_MIN_CONFIDENCE, 10) || 80;
  if (!result || result.relation === "failed" || result.relation === "uncertain" || Number(result.confidence || 0) < minConfidence) {
    return { allow: defaultAllow, verified: true, relation: result ? result.relation : "failed", confidence: result ? result.confidence : 0 };
  }

  var allowIncoming = defaultAllow;
  var removedPositive = false;
  var removedNegative = false;
  var addedPositive = false;
  var addedNegative = false;
  var fixScore = Number(GRAPH_CONFLICT_VERIFY_FIX_SCORE || 4.5);
  var fixExtra = "冲突校验:" + graphSafeString(result.reason || "", 140);

  if (result.relation === "same_person") {
    if (negScore > 0) removedNegative = graphRemoveEdge(this.aliasNegativeGraph, a, b);
    if (kind === "negative") {
      var conflictDescriptorReason = graphRelationDescriptorBlockReason(a, b);
      if (ENABLE_RELATION_DESCRIPTOR_POSITIVE_BLOCK && conflictDescriptorReason) {
        graphRemoteLog("relation_descriptor_positive_blocked", { stage: "graph_conflict_fix", a: graphNormalizeName(a), b: graphNormalizeName(b), reason: conflictDescriptorReason, sourceReason: "graph_conflict_verified_same_person" });
        addedPositive = false;
        allowIncoming = true;
      } else {
        graphAddWeightedEdge(this.aliasPositiveGraph, a, b, fixScore, "graph_conflict_verified_same_person", fixExtra);
        addedPositive = true;
        allowIncoming = false;
      }
    } else {
      allowIncoming = true;
    }
  } else if (result.relation === "different_person") {
    if (posScore > 0) removedPositive = graphRemoveEdge(this.aliasPositiveGraph, a, b);
    try { if (this.splitAliasByConflict) this.splitAliasByConflict(a, b, result.reason || fixExtra); } catch(splitErr) {}
    if (kind === "positive") {
      graphAddWeightedEdge(this.aliasNegativeGraph, a, b, fixScore, "graph_conflict_verified_different_person", fixExtra);
      addedNegative = true;
      allowIncoming = false;
    } else {
      allowIncoming = true;
    }
  }

  if (removedPositive || removedNegative || addedPositive || addedNegative || !allowIncoming) {
    this.saveAliasGraphData();
    conflictShortLog("修正 " + graphSafeString(result.relation || "", 30));
    graphRemoteLog("graph_conflict_fix", {
      a: a,
      b: b,
      stage: payload.stage,
      relation: result.relation,
      confidence: result.confidence,
      incomingKind: kind,
      allowIncoming: allowIncoming,
      removedPositive: removedPositive,
      removedNegative: removedNegative,
      addedPositive: addedPositive,
      addedNegative: addedNegative,
      reason: graphSafeString(result.reason, 260)
    });
  }
  return { allow: allowIncoming, verified: true, relation: result.relation, confidence: result.confidence };
};

CharacterManager.prototype.setAliasGraphBook = function(bookName, bookUrl) {
  if (!ENABLE_GRAPH_BOOK_CACHE) return;
  var bookKey = graphBookCacheSafeKey(bookName, bookUrl);
  if (!bookKey) bookKey = "default";
  if (this.aliasGraphBookKey === bookKey) return;
  // 切书时无条件清理内存临时换声，原角色卡自然音色从未被临时状态改写。
  if (this.aliasGraphBookKey && this.clearTemporaryVoiceStates) this.clearTemporaryVoiceStates("book_cache_switch");
  try {
    if (this.aliasGraphBookKey) {
      this.saveAliasGraphData();
      this.saveAliasCooccurStats();
      if (this.saveMergedRecords) this.saveMergedRecords();
      if (this.saveVoiceAgeEvidenceCache) this.saveVoiceAgeEvidenceCache("book_cache_switch");
    }
  } catch (e) {}
  this.aliasGraphBookKey = bookKey;
  this.aliasPositiveGraphFile = graphBookCacheFile("alias_positive_graph", bookKey);
  this.aliasNegativeGraphFile = graphBookCacheFile("alias_negative_graph", bookKey);
  this.aliasCooccurStatsFile = graphBookCacheFile("alias_cooccur_stats", bookKey);
  this.mergedRecordsFile = graphBookCacheFile("mergedRecords", bookKey);
  this.voiceAgeEvidenceFile = graphBookCacheFile("voice_age_evidence", bookKey);
  this.aliasPositiveGraph = {};
  this.aliasNegativeGraph = {};
  this.aliasCooccurStats = {};
  this.voiceAgeEvidenceCache = { schema: "v908_voice_age_evidence_cache", dataVersion: graphRuleDataVersion(), bookKey: bookKey, evidence: {}, updatedAt: "" };
  this.voiceAgeAppliedEvidence = {};
  this.lastAliasGraphScanKey = "";
  this.loadAliasGraphData();
  this.loadAliasCooccurStats();
  if (this.loadMergedRecords) this.loadMergedRecords();
  if (this.loadVoiceAgeEvidenceCache) this.loadVoiceAgeEvidenceCache();
  graphShortLog("书图谱 " + bookKey);
  graphRemoteLog("character_book_cache_switch", { bookKey: bookKey, source: "setAliasGraphBook", positiveFile: this.aliasPositiveGraphFile, negativeFile: this.aliasNegativeGraphFile, cooccurFile: this.aliasCooccurStatsFile, mergedRecordsFile: this.mergedRecordsFile, voiceAgeEvidenceFile: this.voiceAgeEvidenceFile, chapterIndex: graphCurrentChapterId() });
  graphRemoteLog("graph_book_cache", { bookKey: bookKey, dataVersion: graphRuleDataVersion(), positiveFile: this.aliasPositiveGraphFile, negativeFile: this.aliasNegativeGraphFile, cooccurFile: this.aliasCooccurStatsFile, mergedRecordsFile: this.mergedRecordsFile, voiceAgeEvidenceFile: this.voiceAgeEvidenceFile, legacyIgnored: true });
};

CharacterManager.prototype.recordPositiveAliasEdge = function(a, b, score, reason, extra, evidenceKey, meta) {
  if (!ENABLE_ALIAS_GRAPH || !ENABLE_ALIAS_POSITIVE_GRAPH) return false;
  reason = reason || "positive_alias";
  score = score || 3;
  var mergeBlockReason = graphAliasMergeBlockReason(a, b);
  if (mergeBlockReason) {
    graphShortLog("\u6b63\u8bc1\u62e6\u622a " + graphNormalizeName(a) + "\u2192" + graphNormalizeName(b));
    graphRemoteLog("alias_merge_blocked", { stage: "positive_edge", newName: graphNormalizeName(a), mainName: graphNormalizeName(b), reason: mergeBlockReason, sourceReason: reason });
    return false;
  }
  var relationDescriptorReason = graphRelationDescriptorBlockReason(a, b);
  if (ENABLE_RELATION_DESCRIPTOR_POSITIVE_BLOCK && relationDescriptorReason) {
    graphShortLog("关系描述正证拦截 " + graphNormalizeName(a) + "→" + graphNormalizeName(b));
    graphRemoteLog("relation_descriptor_positive_blocked", { stage: "record_positive_edge", a: graphNormalizeName(a), b: graphNormalizeName(b), reason: relationDescriptorReason, sourceReason: reason, extra: graphSafeString(extra || "", 180) });
    return false;
  }
  if (graphGateShouldApplyToPositiveReason(reason) && this.directPairEvidenceGate) {
    var gate = this.directPairEvidenceGate(a, b, extra || "", this._aliasDirectPairGateContext || this.contextHistory2 || "", "record_positive_edge");
    if (!gate.allow) {
      if (gate.needVerify && this.verifyGraphConflictAndFix) {
        graphRemoteLog("graph_positive_gate_to_conflict_verify", { a: graphNormalizeName(a), b: graphNormalizeName(b), reason: graphSafeString(extra || "", 260), gateReason: graphSafeString(gate.reason || "", 180), sourceReason: reason, tier: gate.tier || "B" });
        var gateDecision = this.verifyGraphConflictAndFix("positive", a, b, score, reason, extra || reason || "", "graph_positive_gate_to_conflict_verify", { defaultAllow: false, forceVerify: true, contextText: this._aliasDirectPairGateContext || this.contextHistory2 || "", originalSourceReason: reason, originalEvidenceText: extra || reason || "" });
        if (!gateDecision.allow) return false;
      } else {
        graphRemoteLog("graph_positive_bridge_gate_blocked", { a: graphNormalizeName(a), b: graphNormalizeName(b), reason: graphSafeString(extra || "", 260), gateReason: graphSafeString(gate.reason || "", 180), sourceReason: reason, tier: gate.tier || "C" });
        return false;
      }
    }
  }
  if (!this.aliasCooccurStats) this.aliasCooccurStats = {};
  meta = meta || {};
  if (!meta.evidenceKey && evidenceKey) meta.evidenceKey = evidenceKey;
  if (!meta.evidenceText && extra) meta.evidenceText = extra;
  if (!meta.chapterId) meta.chapterId = graphCurrentChapterId();
  var conflictDecision = this.verifyGraphConflictAndFix ? this.verifyGraphConflictAndFix("positive", a, b, score, reason, extra || "", "record_positive_edge", { defaultAllow: true }) : { allow: true };
  if (!conflictDecision.allow) return false;
  if (!graphMarkChapterEvidenceOnce(this.aliasCooccurStats, a, b, reason, evidenceKey || meta.evidenceKey, extra || meta.evidenceText || "")) return false;
  graphCooccurMarkChapter(this.aliasCooccurStats, a, b);
  if (graphAddWeightedEdge(this.aliasPositiveGraph, a, b, score, reason, extra || "", evidenceKey || meta.evidenceKey, meta)) {
    var chainAdded = this.applyPositiveChainClosure ? this.applyPositiveChainClosure(a, b, reason) : 0;
    this.saveAliasGraphData();
    if (chainAdded) this.saveAliasCooccurStats();
    graphShortLog("正证 " + graphNormalizeName(a) + "→" + graphNormalizeName(b));
    graphRemoteLog("graph_positive_edge", { a: graphNormalizeName(a), b: graphNormalizeName(b), score: score, reason: reason, extra: graphSafeString(extra, 180), evidenceKey: graphSafeString(evidenceKey || meta.evidenceKey || "", 160), evidenceHash: graphSafeString(meta.evidenceHash || "", 80), batchKey: graphSafeString(meta.batchKey || "", 80), relationId: graphSafeString(meta.relationId || "", 80) });
    return true;
  }
  return false;
};

CharacterManager.prototype.recordNegativeAliasEdge = function(a, b, score, reason, extra, evidenceKey, meta) {
  if (!ENABLE_ALIAS_GRAPH || !ENABLE_ALIAS_NEGATIVE_GRAPH) return false;
  reason = reason || "negative_alias";
  score = score || 2;
  if (!this.aliasCooccurStats) this.aliasCooccurStats = {};
  meta = meta || {};
  if (!meta.evidenceKey && evidenceKey) meta.evidenceKey = evidenceKey;
  if (!meta.evidenceText && extra) meta.evidenceText = extra;
  if (!meta.chapterId) meta.chapterId = graphCurrentChapterId();
  var conflictDecision = this.verifyGraphConflictAndFix ? this.verifyGraphConflictAndFix("negative", a, b, score, reason, extra || "", "record_negative_edge", { defaultAllow: true }) : { allow: true };
  if (!conflictDecision.allow) return false;
  if (!graphMarkChapterEvidenceOnce(this.aliasCooccurStats, a, b, reason, evidenceKey || meta.evidenceKey, extra || meta.evidenceText || "")) return false;
  graphCooccurMarkChapter(this.aliasCooccurStats, a, b);
  if (graphAddWeightedEdge(this.aliasNegativeGraph, a, b, score, reason, extra || "", evidenceKey || meta.evidenceKey, meta)) {
    this.saveAliasGraphData();
    graphShortLog("反证 " + graphNormalizeName(a) + "≠" + graphNormalizeName(b));
    graphRemoteLog("graph_negative_edge", { a: graphNormalizeName(a), b: graphNormalizeName(b), score: score, reason: reason, extra: graphSafeString(extra, 180), evidenceKey: graphSafeString(evidenceKey || meta.evidenceKey || "", 160), evidenceHash: graphSafeString(meta.evidenceHash || "", 80), batchKey: graphSafeString(meta.batchKey || "", 80), relationId: graphSafeString(meta.relationId || "", 80) });
    return true;
  }
  return false;
};



CharacterManager.prototype.applyPositiveChainClosure = function(a, b, reason) {
  if (!ENABLE_ALIAS_GRAPH || !ENABLE_ALIAS_POSITIVE_GRAPH || !ENABLE_GRAPH_POSITIVE_CHAIN_CLOSURE) return 0;
  if (!graphIsPositiveClosureReason(reason)) return 0;
  a = graphNormalizeName(a);
  b = graphNormalizeName(b);
  if (graphIsInvalidName(a) || graphIsInvalidName(b) || a === b) return 0;
  if (!this.aliasCooccurStats) this.aliasCooccurStats = {};
  if (!this.aliasCooccurStats.__positiveChains) this.aliasCooccurStats.__positiveChains = {};
  var graph = this.aliasPositiveGraph || {};
  var added = 0;
  var that = this;

  function tryAdd(x, y, via) {
    x = graphNormalizeName(x);
    y = graphNormalizeName(y);
    via = graphNormalizeName(via);
    if (graphIsInvalidName(x) || graphIsInvalidName(y) || graphIsInvalidName(via) || x === y) return;
    if (ENABLE_RELATION_DESCRIPTOR_POSITIVE_BLOCK && (graphIsRelationDescriptorName(x) || graphIsRelationDescriptorName(y) || graphIsRelationDescriptorName(via))) {
      graphRemoteLog("positive_chain_blocked_by_descriptor", { a: x, b: y, via: via, reason: "关系/身份描述节点禁止参与正链闭合" });
      return;
    }
    // 正链闭合只能使用强正边；identity_substitution / 普通model_same_person / 未过gate的桥接边不扩散。
    var edgeXV = graphGetEdgeSnapshot(that.aliasPositiveGraph, x, via);
    var edgeVY = graphGetEdgeSnapshot(that.aliasPositiveGraph, via, y);
    if (!graphStrictPositiveReasons(edgeXV).length || !graphStrictPositiveReasons(edgeVY).length) {
      graphRemoteLog("positive_chain_bridge_gate_blocked", { a: x, b: y, via: via, reason: "正链来源不是双强正证", sourceReasonsLeft: edgeXV && edgeXV.reasons || [], sourceReasonsRight: edgeVY && edgeVY.reasons || [] });
      return;
    }
    var negClosureReasons = graphGetEdgeReasons(that.aliasNegativeGraph, x, y);
    if (graphReasonListHas(negClosureReasons, "graph_conflict_verified_different_person")) {
      graphRemoteLog("graph_conflict_verify_skip", { a: x, b: y, stage: "positive_chain_closure", reason: "verifiedDifferentSkipPositiveChain", incomingReason: "positive_chain_closed", via: via });
      graphRemoteLog("graph_closure_skip", { kind: "正链", a: x, b: y, via: via, reason: "已有冲突校验非同人，正链闭合跳过" });
      return;
    }
    if (graphGetEdgeScore(that.aliasNegativeGraph, x, y) >= GRAPH_NEGATIVE_HARD_BLOCK) {
      var closureDecision = that.verifyGraphConflictAndFix ? that.verifyGraphConflictAndFix("positive", x, y, GRAPH_POSITIVE_CHAIN_SCORE, "positive_chain_closed", "正链:" + x + "=" + via + "=" + y, "positive_chain_closure", { defaultAllow: false }) : { allow: false };
      if (!closureDecision.allow) {
        graphRemoteLog("graph_closure_skip", { kind: "正链", a: x, b: y, via: via, reason: "强反证阻断" });
        return;
      }
    }
    var key = graphPairKey(x, y) + "|via:" + via;
    if (that.aliasCooccurStats.__positiveChains[key]) return;
    that.aliasCooccurStats.__positiveChains[key] = graphNowIso();
    graphPruneSeenMap(that.aliasCooccurStats.__positiveChains);
    if (!graphMarkChapterEvidenceOnce(that.aliasCooccurStats, x, y, "positive_chain_closed", "via:" + graphNormalizeName(via), "正链:" + x + "=" + via + "=" + y)) return;
    if (graphAddWeightedEdge(that.aliasPositiveGraph, x, y, GRAPH_POSITIVE_CHAIN_SCORE, "positive_chain_closed", "正链:" + x + "=" + via + "=" + y)) {
      added++;
      graphRemoteLog("positive_chain_closure", { names: x + "/" + via + "/" + y, added: 1 });
    }
  }

  var left = graphCollectClosureNeighbors(graph, a, true);
  for (var i = 0; i < left.length; i++) {
    if (left[i] !== b) tryAdd(b, left[i], a);
  }
  var right = graphCollectClosureNeighbors(graph, b, true);
  for (var j = 0; j < right.length; j++) {
    if (right[j] !== a) tryAdd(a, right[j], b);
  }
  if (added) graphShortLog("正链闭合 " + added);
  return added;
};

function graphBuildCompactRelationAuditItems(relations) {
  relations = relations || [];
  var auditItems = [];
  for (var i = 0; i < relations.length; i++) {
    var r = relations[i] || {};
    auditItems.push({
      relationId: r.relationId || ("rel_" + (i + 1)),
      a: r.a,
      b: r.b,
      relationType: r.relationType,
      evidenceFamily: r.evidenceFamily,
      evidenceSubtype: r.evidenceSubtype || "",
      evidenceText: graphSafeString(r.evidenceText || "", 420),
      summary: graphSafeString(r.summary || "", 260),
      seq: r.seq || "",
      confidence: r.confidence || 0,
      directPair: r.directPair === true,
      bridgeNames: r.bridgeNames || [],
      shapeFlags: r.shapeFlags || [],
      anchorType: r.anchorType || "",
      chapterId: r.chapterId || graphCurrentChapterId(),
      batchKey: r.batchKey || "",
      evidenceHash: r.evidenceHash || "",
      evidenceKey: r.evidenceKey || ""
    });
  }
  return auditItems;
}

function graphRelationAuditCandidateSetId(relations) {
  var auditItems = graphBuildCompactRelationAuditItems(relations || []);
  var signatures = [];
  for (var i = 0; i < auditItems.length; i++) {
    var item = auditItems[i] || {};
    signatures.push([
      graphSafeString(item.relationId || "", 80),
      graphSafeString(item.evidenceHash || "", 120),
      graphSafeString(item.relationType || "", 40),
      graphSafeString(item.evidenceFamily || "", 60),
      graphSafeString(item.evidenceSubtype || "", 60)
    ].join("|"));
  }
  signatures.sort();
  return "relation_set_" + graphHash(signatures.join("#"));
}

CharacterManager.prototype.buildNameSemanticRelationAuditPrompt = function(relations, chapterFullContent, options) {
  relations = relations || [];
  options = options || {};
  var auditItems = graphBuildCompactRelationAuditItems(relations);
  var candidateSetId = graphRelationAuditCandidateSetId(relations);
  graphRemoteLog("model_relation_audit_prompt_policy", { evidenceJudgedByFamilyAndSubtype: true, nameIdentityCanUseSelfClaimOrIntroduction: true, bothEndpointsNeedNotBeBatchSpeakers: true, dialogueAttributionCheckedWhenApplicable: true, clearCoreferenceAllowed: true, externalKnowledgeForbidden: true, lexicalCategoryBanEnabled: false, candidateCount: auditItems.length });
  for (var policyIndex = 0; policyIndex < auditItems.length; policyIndex++) {
    var policyItem = auditItems[policyIndex] || {};
    if (policyItem.evidenceFamily === "name_identity" && /^(self_claim|called_as|introduced_as|parenthetical_alias|explicit_same_person)$/.test(policyItem.evidenceSubtype || "")) graphRemoteLog("model_relation_name_identity_cross_batch_case", { relationId: policyItem.relationId || "", a: policyItem.a || "", b: policyItem.b || "", evidenceSubtype: policyItem.evidenceSubtype || "", seq: policyItem.seq || "", evidenceText: policyItem.evidenceText || "", policy: "身份名一端无需另作为本批编号说话人" });
  }
  return "你是小说人物关系证据审计AI。你的任务不是抽取新证据，而是审计【批量姓名分析模型】已经返回的新批次证据是否可信。\n\n" +
    getV908CharacterNamingAndSpeakerRules("alias_check") +
    "【审计原则】\n" +
    "1. evidenceText 必须能在【当前待分析文本】中找到对应原文锚点；只允许标点、空白等不改变语义的轻微差异。改写、概括、省略号拼接、只在summary中成立、依赖输入范围外信息，都不能直接采纳。\n" +
    "2. 必须先按evidenceFamily/evidenceSubtype分别核对，禁止给所有证据套用‘a、b都必须是本批编号说话人’的统一条件。\n" +
    "2a. name_identity中的self_claim、called_as、introduced_as、parenthetical_alias、explicit_same_person等，只要当前输入能确认被描述/正在说话的角色与其自称、被称或被介绍的名字属于同一人即可；作为本名/别名的一端不需要另外成为本批说话人。例如‘神秘老者道：我名为药老’可以支持神秘老者=药老。\n" +
    "2b. 只有dialogue_relation中的speaker_addressee、reply_relation、vocative_address才重点核对说话人、受话人、回复对象和对白归属；仅在对白内容中提到某个名称，不自动等于正在对该名称说话。\n" +
    "2c. action_relation、social_relation、co_presence、identity_relation按各自的动作/关系/共现/身份语义核对，两端同样不要求都是本批说话人。允许当前输入中清晰可追溯的代词和身份描述，但禁止使用原著知识或模型记忆补全。\n" +
    "2d. 必须逐条核对a、b、relationType/evidenceFamily/evidenceSubtype的强度是否与原文一致；仅同场、提及、想起、寻找、调查、声音来源或间接关系，不得冒充直接同人/直接非同人证据。\n" +
    "3. evidenceText有真实锚点但表述不精确时，放入downgrade并在correctedEvidenceText写入当前文本中的原文片段；没有可用锚点时放入reject。\n" +
    "4. 如果证据是模型幻觉、文本没有支持、人物归属错误、把被谈论对象当说话人，放入reject。\n" +
    "5. 如果证据有价值但强度不足、仅提及/想起/寻找/调查/间接关系，放入downgrade。\n" +
    "6. 如果新证据与旧正反图谱明显冲突，放入verify。\n" +
    "7. 只有每一条证据都按自己的evidenceFamily/evidenceSubtype完成原文锚点、人物归属和关系强度核对且无需修正时，才允许使用__ALL__全部采纳；不得因为身份名不是本批说话人而误拒name_identity。\n" +
    "8. 不要输出合并/拆分动作，只输出证据审计结论。\n\n" +
    (options.omitSourceText ? "【当前待分析文本】\n使用合并请求顶部的【共享当前批文本】，不得引用共享紧邻上文作为图谱关系证据。\n\n" : ("【当前待分析文本】\n" + String(chapterFullContent || "") + "\n\n")) +
    "【待审计新证据】\n候选总数=" + auditItems.length + "，candidateSetId=" + candidateSetId + "。\n" + JSON.stringify(auditItems) + "\n\n" +
    "【强制输出格式】只输出JSON，不要输出Markdown。\n" +
    "A. 如果全部证据都采纳，必须返回auditComplete=true、allAccepted=true，并且acceptedAll必须返回[\"__ALL__\"]；不要逐条复述relationId；三个非采纳数组必须存在且为空；allAcceptedVerification八个字段必须全部为true。\n" +
    "B. 如果不是全部采纳，必须返回auditComplete=true、allAccepted=false、acceptedAll=[]，并完整返回所有非采纳证据：downgrade/reject/verify三个数组必须都存在；没有问题的采纳项不要返回。\n" +
    "C. downgrade/reject/verify数组中的每一项必须包含relationId和auditReason，可选correctedEvidenceText、usableForAlias、usableForGraph、usableForRecordDecision；relationId必须精确来自待审计新证据。\n" +
    "D. 必须原样返回candidateCount=" + auditItems.length + "和candidateSetId=" + candidateSetId + "；缺少或不匹配视为本次审计失败。缺少auditComplete/allAccepted/acceptedAll/downgrade/reject/verify任意字段、relationId无法匹配、非采纳项缺auditReason，也视为失败；allAccepted=true时acceptedAll不是[\"__ALL__\"]，或allAcceptedVerification缺失/任一项不为true，也视为失败。\n" +
    "{\n" +
    '  "auditComplete": true,\n' +
    '  "candidateCount": ' + auditItems.length + ',\n' +
    '  "candidateSetId": "' + candidateSetId + '",\n' +
    '  "allAccepted": true/false,\n' +
    '  "acceptedAll": ["__ALL__"],\n' +
    '  "allAcceptedVerification": {"everyEvidenceTextGrounded":true,"everyEvidenceJudgedByFamilyAndSubtype":true,"everyNameIdentitySelfClaimOrIntroductionPreserved":true,"noSpeakerRequirementMisappliedToNonDialogueEvidence":true,"everyDialogueAttributionCheckedWhenApplicable":true,"everyCoreferenceCheckedWhenUsed":true,"noExternalKnowledgeUsed":true,"everyRelationStrengthChecked":true},\n' +
    '  "downgrade": [{"relationId":"rel_3","auditReason":"理由","correctedEvidenceText":"","usableForAlias":false,"usableForGraph":false,"usableForRecordDecision":false}],\n' +
    '  "reject": [{"relationId":"rel_4","auditReason":"理由","correctedEvidenceText":"","usableForAlias":false,"usableForGraph":false,"usableForRecordDecision":false}],\n' +
    '  "verify": [{"relationId":"rel_5","auditReason":"理由","correctedEvidenceText":"","usableForAlias":false,"usableForGraph":false,"usableForRecordDecision":false}]\n' +
    "}";
};



function graphBuildRelationAuditCompleteness(relations, audits) {
  relations = relations || [];
  audits = audits || [];
  var expected = {};
  var missing = [];
  var relationCount = 0;
  for (var i = 0; i < relations.length; i++) {
    var rid = graphSafeString((relations[i] || {}).relationId || ("rel_" + (i + 1)), 80);
    if (!rid) continue;
    expected[rid] = true;
    relationCount++;
  }
  var got = {};
  for (var j = 0; j < audits.length; j++) {
    var aid = graphSafeString((audits[j] || {}).relationId || "", 80);
    if (aid) got[aid] = true;
  }
  for (var k in expected) {
    if (expected.hasOwnProperty(k) && !got[k]) missing.push(k);
  }
  return { complete: missing.length === 0 && audits.length >= relationCount, relationCount: relationCount, auditCount: audits.length, missingRelationIds: missing };
}

function graphIsRelationAuditComplete(relations, audits) {
  return graphBuildRelationAuditCompleteness(relations, audits).complete === true;
}

function graphRelationAuditExpectedInfo(relations) {
  relations = relations || [];
  var ids = [];
  var map = {};
  for (var i = 0; i < relations.length; i++) {
    var rid = graphSafeString((relations[i] || {}).relationId || ("rel_" + (i + 1)), 80);
    if (!rid) continue;
    ids.push(rid);
    map[rid] = true;
  }
  return { ids: ids, map: map, count: ids.length };
}

function graphRelationAuditIsAllAcceptedMarker(item) {
  if (typeof item === "string" || typeof item === "number") {
    var s = graphSafeString(String(item), 80).toUpperCase();
    return s === "__ALL__" || s === "ALL" || s === "全部采纳";
  }
  item = item || {};
  var v = graphSafeString(item.relationId || item.id || item.relation_id || item.marker || item.value || "", 80).toUpperCase();
  return v === "__ALL__" || v === "ALL" || v === "全部采纳";
}

function graphRelationAuditAllAcceptedVerificationOk(apiResult) {
  var check = apiResult && apiResult.allAcceptedVerification;
  return !!(check && typeof check === "object" &&
    check.everyEvidenceTextGrounded === true &&
    check.everyEvidenceJudgedByFamilyAndSubtype === true &&
    check.everyNameIdentitySelfClaimOrIntroductionPreserved === true &&
    check.noSpeakerRequirementMisappliedToNonDialogueEvidence === true &&
    check.everyDialogueAttributionCheckedWhenApplicable === true &&
    check.everyCoreferenceCheckedWhenUsed === true &&
    check.noExternalKnowledgeUsed === true &&
    check.everyRelationStrengthChecked === true);
}

function graphRelationAuditReadRelationId(item) {
  if (typeof item === "string" || typeof item === "number") return graphSafeString(String(item), 80);
  item = item || {};
  return graphSafeString(item.relationId || item.id || item.relation_id || "", 80);
}

function graphRelationAuditBuildAcceptAudits(relations, reasonText) {
  relations = relations || [];
  var out = [];
  for (var i = 0; i < relations.length; i++) {
    var r = relations[i] || {};
    var rid = graphSafeString(r.relationId || ("rel_" + (i + 1)), 80);
    out.push({
      relationId: rid,
      decision: "accept",
      auditReason: reasonText || "审计结构完整，未列入降级/拒收/复核数组，按采纳处理",
      correctedEvidenceText: graphSafeString(r.evidenceText || r.summary || "", 420),
      usableForAlias: true,
      usableForGraph: true,
      usableForRecordDecision: false
    });
  }
  return out;
}

function graphNormalizeSparseRelationAuditResult(relations, apiResult, scene) {
  relations = relations || [];
  var expected = graphRelationAuditExpectedInfo(relations);
  var candidateSetId = graphRelationAuditCandidateSetId(relations);
  var fail = function(reason, extra) {
    return { complete: false, audits: [], reason: reason, relationCount: expected.count, candidateSetId: candidateSetId, auditCount: 0, missingRelationIds: expected.ids.slice(0), extra: extra || {} };
  };
  if (!apiResult || typeof apiResult !== "object") return fail("返回不是对象");
  if (Number(apiResult.candidateCount) !== expected.count) return fail("candidateCount与本批候选总数不一致", { returnedCandidateCount: apiResult.candidateCount });
  if (graphSafeString(apiResult.candidateSetId || "", 160) !== candidateSetId) return fail("candidateSetId与本批候选集合不一致", { returnedCandidateSetId: apiResult.candidateSetId || "" });
  if (apiResult.auditComplete !== true) return fail("缺少auditComplete=true");
  if (typeof apiResult.allAccepted !== "boolean") return fail("缺少allAccepted布尔字段");
  if (!Array.isArray(apiResult.acceptedAll)) return fail("缺少acceptedAll数组");
  if (!Array.isArray(apiResult.downgrade)) return fail("缺少downgrade数组");
  if (!Array.isArray(apiResult.reject)) return fail("缺少reject数组");
  if (!Array.isArray(apiResult.verify)) return fail("缺少verify数组");

  var used = {};
  var audits = graphRelationAuditBuildAcceptAudits(relations, apiResult.allAccepted ? "模型审计明确返回全部采纳" : "审计结构完整，未列入降级/拒收/复核数组，按采纳处理");
  var auditIndex = {};
  for (var ai = 0; ai < audits.length; ai++) auditIndex[audits[ai].relationId] = ai;

  if (apiResult.allAccepted === true) {
    if (apiResult.downgrade.length || apiResult.reject.length || apiResult.verify.length) return fail("allAccepted=true时非采纳数组必须为空");
    if (!graphRelationAuditAllAcceptedVerificationOk(apiResult)) return fail("allAccepted=true时allAcceptedVerification八项必须全部为true");
    if (apiResult.acceptedAll.length === 1 && graphRelationAuditIsAllAcceptedMarker(apiResult.acceptedAll[0])) {
      graphRemoteLog("model_relation_audit_all_accepted_verification", { scene: scene || "", relationCount: expected.count, allAcceptedVerification: apiResult.allAcceptedVerification || {}, complete: true });
      return { complete: true, audits: audits, reason: "全部采纳__ALL__", relationCount: expected.count, candidateSetId: candidateSetId, auditCount: audits.length, missingRelationIds: [] };
    }
    var acceptedMap = {};
    for (var aa = 0; aa < apiResult.acceptedAll.length; aa++) {
      if (graphRelationAuditIsAllAcceptedMarker(apiResult.acceptedAll[aa])) return fail("acceptedAll使用__ALL__时必须只包含一个__ALL__标记");
      var arid = graphRelationAuditReadRelationId(apiResult.acceptedAll[aa]);
      if (!arid) return fail("acceptedAll存在空relationId");
      if (!expected.map[arid]) return fail("acceptedAll包含未知relationId", { relationId: arid });
      acceptedMap[arid] = true;
    }
    var missingAccepted = [];
    for (var mi = 0; mi < expected.ids.length; mi++) {
      if (!acceptedMap[expected.ids[mi]]) missingAccepted.push(expected.ids[mi]);
    }
    if (missingAccepted.length > 0 || apiResult.acceptedAll.length < expected.count) return fail("acceptedAll未完整覆盖全部relationId且未使用__ALL__标记", { missingRelationIds: missingAccepted });
    return { complete: true, audits: audits, reason: "全部采纳", relationCount: expected.count, candidateSetId: candidateSetId, auditCount: audits.length, missingRelationIds: [] };
  }

  if (apiResult.acceptedAll.length > 0) return fail("allAccepted=false时acceptedAll必须为空数组");

  function applyExceptionArray(arr, decision) {
    for (var i = 0; i < arr.length; i++) {
      var item = arr[i] || {};
      if (typeof item !== "object") return "非采纳数组元素必须是对象";
      var rid = graphRelationAuditReadRelationId(item);
      if (!rid) return decision + "数组存在空relationId";
      if (!expected.map[rid]) return decision + "数组包含未知relationId:" + rid;
      if (used[rid]) return "relationId重复出现在非采纳数组:" + rid;
      var auditReason = graphSafeString(item.auditReason || item.reason || item.audit_reason || "", 260);
      if (!auditReason) return decision + "数组缺少auditReason/reason:" + rid;
      used[rid] = decision;
      var idx = auditIndex[rid];
      if (typeof idx !== "number") return decision + "数组无法匹配本地relationId:" + rid;
      audits[idx] = {
        relationId: rid,
        decision: decision,
        auditReason: auditReason,
        correctedEvidenceText: graphSafeString(item.correctedEvidenceText || item.evidenceText || item.evidence || "", 420),
        usableForAlias: item.hasOwnProperty("usableForAlias") ? item.usableForAlias === true : decision === "accept",
        usableForGraph: item.hasOwnProperty("usableForGraph") ? item.usableForGraph === true : decision === "accept",
        usableForRecordDecision: item.hasOwnProperty("usableForRecordDecision") ? item.usableForRecordDecision === true : false
      };
    }
    return "";
  }

  var err = applyExceptionArray(apiResult.downgrade, "downgrade");
  if (err) return fail(err);
  err = applyExceptionArray(apiResult.reject, "reject");
  if (err) return fail(err);
  err = applyExceptionArray(apiResult.verify, "verify");
  if (err) return fail(err);

  var exceptionCount = apiResult.downgrade.length + apiResult.reject.length + apiResult.verify.length;
  if (exceptionCount <= 0) return fail("allAccepted=false但downgrade/reject/verify均为空");
  graphRemoteLog("model_relation_audit_exception_summary", { scene: scene || "", relationCount: expected.count, downgradeCount: apiResult.downgrade.length, rejectCount: apiResult.reject.length, verifyCount: apiResult.verify.length, implicitAcceptedCount: Math.max(0, expected.count - exceptionCount) });
  return { complete: true, audits: audits, reason: "异常数组完整", relationCount: expected.count, candidateSetId: candidateSetId, auditCount: audits.length, exceptionCount: exceptionCount, missingRelationIds: [] };
}


CharacterManager.prototype.auditNameSemanticRelationsByAliasApi = function(relations, chapterFullContent) {
  if (!relations || !relations.length) return { success: true, audits: [] };
  auditShortLog("开始 " + relations.length + " 条");
  var prompt = this.buildNameSemanticRelationAuditPrompt(relations, chapterFullContent || "");
  graphRemoteLog("model_relation_audit_request", { relationCount: relations.length, promptHead: graphSafeString(prompt, 2600), relations: relations.slice(0, 30) });
  var requestTimeout = ALIAS_ANALYZE_TIMEOUT;
  var maxRetryRound = Math.ceil(CHARACTER_ANALYZE_RETRY_MAX / bingfa);
  var currentRound = 0;
  var finalAudit = null;
  function sleep(ms) {
    var start = Date.now();
    while (Date.now() - start < ms) {}
  }
  function buildAuditRequest(apiConfig) {
    var requestData = {
      model: apiConfig.model,
      messages: [
        { role: "system", content: "严格遵守格式要求，仅输出JSON。审计必须按evidenceFamily/evidenceSubtype逐条核对原文锚点、人物归属和关系强度；自称/介绍等name_identity不要求身份名另作本批说话人。必须原样返回candidateCount/candidateSetId，并返回auditComplete/allAccepted/acceptedAll/downgrade/reject/verify完整结构；全部采纳时acceptedAll必须为[\"__ALL__\"]且allAcceptedVerification八项必须全部为true；格式错误或数组不完整则任务失败。" },
        { role: "user", content: prompt }
      ],
      temperature: 0.1
    };
    var headers = {
      "Content-Type": "application/json",
      "Authorization": "Bearer " + apiConfig.key,
      "Connection": "keep-alive",
      "Timeout": requestTimeout.toString()
    };
    if (ENABLE_MODEL_RAW_REMOTE_LOG) {
      graphRemoteLog("model_relation_audit_raw_request", {
        scene: "model_relation_audit",
        endpoint: graphSafeString(apiConfig.endpoint || "", 200),
        model: graphSafeString(apiConfig.model || "", 80),
        requestData: graphSafeString(JSON.stringify(requestData), MODEL_RAW_REMOTE_LOG_MAX_LEN)
      });
    }
    return { endpoint: apiConfig.endpoint, data: requestData, headers: headers };
  }
  function parseAuditResponse(response) {
    var responseBody = String(response.body().string() || "{}");
    graphRemoteLog("model_relation_audit_raw_response", { scene: "model_relation_audit", responseBody: graphSafeString(responseBody, MODEL_RAW_REMOTE_LOG_MAX_LEN) });
    var apiOuterResponse = JSON.parse(responseBody);
    if (!apiOuterResponse.choices || !apiOuterResponse.choices[0] || !apiOuterResponse.choices[0].message) {
      throw new Error("API响应格式错误：缺少choices[0].message");
    }
    var content = apiOuterResponse.choices[0].message.content.trim();
    var cleanJson = content.replace(/```json|```/g, "").trim();
    var apiResult = JSON.parse(cleanJson);
    var normalized = graphNormalizeSparseRelationAuditResult(relations, apiResult, "standalone_relation_audit");
    if (!normalized.complete) {
      graphRemoteLog("model_relation_audit_incomplete_retry", { retryCount: currentRound, relationCount: normalized.relationCount, auditCount: normalized.auditCount, missingRelationIds: normalized.missingRelationIds.slice(0, 80), reason: normalized.reason, extra: normalized.extra || {} });
      throw new Error("返回格式错误：" + normalized.reason);
    }
    return normalized;
  }
  while (currentRound < maxRetryRound && !finalAudit) {
    currentRound++;
    var concurrentResult = concurrentApiRequest("relationAudit", buildAuditRequest, parseAuditResponse, null, requestTimeout);
    if (concurrentResult.success) {
      finalAudit = concurrentResult.isMultiResult ? concurrentResult.data[0].data : concurrentResult.data;
    } else {
      graphRemoteLog("model_relation_audit_retry", { retryCount: currentRound, relationCount: relations.length, auditCount: 0, missingRelationIds: [], reason: "并发请求失败或返回结构不完整", errors: concurrentResult.errors || [] });
      if (currentRound < maxRetryRound) sleep(250);
    }
  }
  if (!finalAudit || !finalAudit.complete || !graphIsRelationAuditComplete(relations, finalAudit.audits || [])) {
    auditShortLog("失败，本批未应用");
    graphRemoteLog("model_relation_audit_result", { success: false, relationCount: relations.length, errors: ["证据审计按原重试链路仍未得到完整结构"] });
    return { success: false, audits: [] };
  }
  graphRemoteLog("model_relation_audit_result", { success: true, auditCount: finalAudit.audits.length, reason: finalAudit.reason || "", audits: finalAudit.audits.slice(0, 50) });
  return { success: true, audits: finalAudit.audits };
};

CharacterManager.prototype.applyAuditedNameSemanticRelations = function(relations, audits, chapterFullContent) {
  var auditMap = {};
  audits = audits || [];
  for (var i = 0; i < audits.length; i++) {
    var au = audits[i] || {};
    var rid = graphSafeString(au.relationId || "", 80);
    if (rid) auditMap[rid] = au;
  }
  var pos = 0, neg = 0, weak = 0, rejected = 0, downgraded = 0, verify = 0, missingDecision = 0;
  var acceptedTotal = 0, acceptedNoGraph = 0;
  var acceptedSamples = [], rejectedSamples = [], downgradedSamples = [], verifySamples = [];
  for (var j = 0; j < relations.length; j++) {
    var r = relations[j] || {};
    graphEnsureRelationEvidenceMeta(r, r.batchKey || "");
    var relationMeta = graphBuildRelationEvidenceMeta(r, r.batchKey || "");
    var audit = auditMap[r.relationId] || null;
    var missingAuditDecision = !audit;
    if (missingAuditDecision) {
      missingDecision++;
      graphRemoteLog("model_relation_audit_missing_decision", { relationId: r.relationId || "", a: r.a, b: r.b, relationType: r.relationType, evidenceFamily: r.evidenceFamily || "", evidenceSubtype: r.evidenceSubtype || "", evidenceText: graphSafeString(r.evidenceText || r.summary || "", 260), action: "skip_without_default_decision", auditReason: "审计缺失不再默认降级；本批正常应已被完整性重试拦截" });
      continue;
    }
    var decision = graphNormalizeAuditDecision(audit.decision || "");
    var auditReason = graphSafeString(audit.auditReason || audit.reason || "", 260);
    var evidence = graphSafeString(audit.correctedEvidenceText || r.evidenceText || r.summary || "", 420);
    var payload = { relationId: r.relationId, a: r.a, b: r.b, relationType: r.relationType, evidenceFamily: r.evidenceFamily, evidenceSubtype: r.evidenceSubtype || "", anchorType: r.anchorType || "", evidenceText: graphSafeString(evidence, 260), summary: graphSafeString(r.summary || "", 220), decision: decision, auditReason: auditReason, chapterId: relationMeta.chapterId || "", batchKey: relationMeta.batchKey || "", evidenceHash: relationMeta.evidenceHash || "", evidenceKey: relationMeta.evidenceKey || "" };
    // 缺失审计已在上方直接跳过，不再默认降级。
    if (decision === "reject") {
      rejected++;
      if (rejectedSamples.length < 20) rejectedSamples.push(payload);
      graphRemoteLog("model_relation_audit_rejected", payload);
      continue;
    }
    if (decision === "downgrade") {
      downgraded++;
      weak++;
      if (downgradedSamples.length < 20) downgradedSamples.push(payload);
      var weakStats = graphGetPairStats(this.aliasCooccurStats, r.a, r.b);
      if (weakStats) {
        weakStats.updatedAt = graphNowIso();
        weakStats.modelWeakEvidence = Number(weakStats.modelWeakEvidence || 0) + 1;
        graphCooccurMarkChapter(this.aliasCooccurStats, r.a, r.b);
        graphPushCooccurEvidence(this.aliasCooccurStats, r.a, r.b, "审计降级:" + (r.reason || "model_relation_downgrade"), evidence, { decision: "downgrade", evidenceKey: relationMeta.evidenceKey, evidenceHash: relationMeta.evidenceHash, batchKey: relationMeta.batchKey, chapterId: relationMeta.chapterId, relationId: r.relationId, relationType: r.relationType || "", evidenceFamily: r.evidenceFamily || "", evidenceSubtype: r.evidenceSubtype || "", anchorType: r.anchorType || "", summary: r.summary || "", source: "model_relation_audit" });
      }
      graphRemoteLog("model_relation_audit_downgraded", payload);
      continue;
    }
    if (decision === "verify") {
      verify++;
      if (verifySamples.length < 20) verifySamples.push(payload);
      graphRemoteLog("model_relation_audit_to_verify", payload);
      var verifyKind = (r.reason === "model_name_identity_positive" || r.relationType === "same_person") ? "positive" : "negative";
      var verifyScore = verifyKind === "positive" ? GRAPH_MODEL_NAME_IDENTITY_SCORE : GRAPH_MODEL_DIALOGUE_RELATION_SCORE;
      if (this.verifyGraphConflictAndFix) this.verifyGraphConflictAndFix(verifyKind, r.a, r.b, verifyScore, r.reason || "model_relation_audit_to_verify", evidence, "model_relation_audit_to_verify", { defaultAllow: false, forceVerify: true, contextText: chapterFullContent, modelType: "name_semantic_audit", originalSourceReason: r.reason || "model_relation_audit_to_verify", originalEvidenceText: evidence });
      continue;
    }
    // accept
    acceptedTotal++;
    graphRemoteLog("model_relation_audit_accepted", payload);
    var st = graphGetPairStats(this.aliasCooccurStats, r.a, r.b);
    if (st) {
      st.updatedAt = graphNowIso();
      st.modelEvidence = Number(st.modelEvidence || 0) + 1;
      graphCooccurMarkChapter(this.aliasCooccurStats, r.a, r.b);
      graphPushCooccurEvidence(this.aliasCooccurStats, r.a, r.b, r.reason, evidence, { decision: "accept", evidenceKey: relationMeta.evidenceKey, evidenceHash: relationMeta.evidenceHash, batchKey: relationMeta.batchKey, chapterId: relationMeta.chapterId, relationId: r.relationId, relationType: r.relationType || "", evidenceFamily: r.evidenceFamily || "", evidenceSubtype: r.evidenceSubtype || "", anchorType: r.anchorType || "", summary: r.summary || "", source: "model_relation_audit" });
    }
    if (acceptedSamples.length < 30) acceptedSamples.push(payload);
    if (r.reason === "model_name_identity_positive" || r.relationType === "same_person") {
      var gate = this.directPairEvidenceGate ? this.directPairEvidenceGate(r.a, r.b, r.reason + " " + r.evidenceFamily, evidence, "name_semantic_audit") : { tier: "A", allow: true };
      if (gate && gate.tier === "C") {
        rejected++;
        graphRemoteLog("graph_positive_bridge_gate_blocked", { a: r.a, b: r.b, reason: r.reason, evidence: graphSafeString(evidence, 260), gateReason: gate.reason || "", source: "name_semantic_audit" });
        continue;
      }
      if (gate && gate.tier === "B") {
        var verifySame = this.verifyGraphConflictAndFix ? this.verifyGraphConflictAndFix("positive", r.a, r.b, GRAPH_MODEL_NAME_IDENTITY_SCORE, r.reason, evidence, "name_semantic_audit", { defaultAllow: false, forceVerify: true, contextText: chapterFullContent, modelType: "name_semantic_audit", originalSourceReason: r.reason, originalEvidenceText: evidence }) : { allow: true };
        if (!verifySame.allow) { acceptedNoGraph++; continue; }
      }
      if (this.recordPositiveAliasEdge(r.a, r.b, GRAPH_MODEL_NAME_IDENTITY_SCORE, r.reason, evidence, relationMeta.evidenceKey, { evidenceKey: relationMeta.evidenceKey, evidenceHash: relationMeta.evidenceHash, batchKey: relationMeta.batchKey, chapterId: relationMeta.chapterId, relationId: r.relationId, relationType: r.relationType || "", evidenceFamily: r.evidenceFamily || "", evidenceSubtype: r.evidenceSubtype || "", anchorType: r.anchorType || "", source: "model_relation_audit", evidenceText: evidence })) pos++;
      else acceptedNoGraph++;
    } else if (r.reason === "model_identity_relation_evidence" || r.reason === "model_weak_relation_audit") {
      weak++;
      graphRemoteLog("name_semantic_relation_audit_only", { a: r.a, b: r.b, reason: r.reason, evidenceFamily: r.evidenceFamily, evidenceSubtype: r.evidenceSubtype || "", anchorType: r.anchorType || "", evidenceText: graphSafeString(evidence, 260), summary: graphSafeString(r.summary || "", 220), note: "审计采纳但仅作弱提示/身份关系证据" });
    } else {
      var score = GRAPH_MODEL_DIALOGUE_RELATION_SCORE;
      if (r.reason === "model_action_relation_negative") score = GRAPH_MODEL_ACTION_RELATION_SCORE;
      else if (r.reason === "model_social_relation_negative") score = GRAPH_MODEL_SOCIAL_RELATION_SCORE;
      else if (r.reason === "model_co_presence_negative") score = GRAPH_MODEL_CO_PRESENCE_SCORE;
      else if (r.reason === "model_explicit_different_negative") score = GRAPH_MODEL_EXPLICIT_DIFFERENT_SCORE;
      var verifyNeg = this.verifyGraphConflictAndFix ? this.verifyGraphConflictAndFix("negative", r.a, r.b, score, r.reason, evidence, "name_semantic_audit", { defaultAllow: true, contextText: chapterFullContent, modelType: "name_semantic_audit", originalSourceReason: r.reason, originalEvidenceText: evidence }) : { allow: true };
      if (verifyNeg && verifyNeg.allow && this.recordNegativeAliasEdge(r.a, r.b, score, r.reason, evidence, relationMeta.evidenceKey, { evidenceKey: relationMeta.evidenceKey, evidenceHash: relationMeta.evidenceHash, batchKey: relationMeta.batchKey, chapterId: relationMeta.chapterId, relationId: r.relationId, relationType: r.relationType || "", evidenceFamily: r.evidenceFamily || "", evidenceSubtype: r.evidenceSubtype || "", anchorType: r.anchorType || "", source: "model_relation_audit", evidenceText: evidence })) neg++;
      else acceptedNoGraph++;
    }
  }
  var applySummary = {
    acceptedTotal: acceptedTotal,
    positiveApplied: pos,
    negativeApplied: neg,
    weakApplied: weak,
    acceptedNoGraph: acceptedNoGraph,
    rejectedTotal: rejected,
    downgradedTotal: downgraded,
    verifyTotal: verify,
    missingDecisionTotal: missingDecision,
    // 兼容旧字段；apply 只保留统计，逐条样本由 accepted/rejected/downgraded/to_verify 事件承载。
    positive: pos,
    negative: neg,
    weak: weak,
    rejected: rejected,
    downgraded: downgraded,
    verify: verify
  };
  graphRemoteLog("model_relation_audit_apply", applySummary);
  // 这里只记录模型审计后的放行规模，便于远程抽查；不在本地按词类或正则再次做语义裁决。
  graphRemoteLog("model_relation_audit_leak_observe", {
    relationCount: relations.length,
    acceptedTotal: acceptedTotal,
    positiveApplied: pos,
    negativeApplied: neg,
    weakApplied: weak,
    acceptedNoGraph: acceptedNoGraph,
    rejectedTotal: rejected,
    downgradedTotal: downgraded,
    verifyTotal: verify,
    missingDecisionTotal: missingDecision,
    localSemanticGateEnabled: false,
    semanticLeakJudgedLocally: false,
    note: "只观察审计后放行规模；是否存在模型幻觉由远程日志和人工复核判断，不新增本地语义词表或正则"
  });
  auditShortLog("返回 采纳" + acceptedTotal + " 降级" + downgraded + " 复核" + verify + " 拒收" + rejected + " 未落图" + acceptedNoGraph);
  auditShortLog("落图 正" + pos + " 反" + neg + " 弱" + weak);
  if (pos || neg || weak) { this.saveAliasGraphData(); this.saveAliasCooccurStats(); }
  return { positive: pos, negative: neg, audit: weak, rejected: rejected, downgraded: downgraded, verify: verify, missingDecision: missingDecision, acceptedTotal: acceptedTotal, acceptedNoGraph: acceptedNoGraph };
};


CharacterManager.prototype.graphBatchHasNewRoleCandidate = function(batchNames) {
  batchNames = batchNames || [];
  for (var i = 0; i < batchNames.length; i++) {
    var n = graphNormalizeName(batchNames[i] || "");
    if (graphIsInvalidName(n) || n === "未知") continue;
    if (graphSpecialSpeakerType(n, "", "")) continue;
    var existing = this.findCharacterRecord ? this.findCharacterRecord(n) : null;
    if (!existing) return true;
  }
  return false;
};

CharacterManager.prototype.setPendingNameSemanticRelations = function(relations, chapterFullContent, batchNames, hasNewRoleCandidate) {
  relations = relations || [];
  try {
    var oldPending = this.pendingNameSemanticRelations;
    if (oldPending && !oldPending.consumed && oldPending.hasNewRoleCandidate && oldPending.relations && oldPending.relations.length && this.auditPendingNameSemanticRelationsIfNeeded) {
      graphRemoteLog("model_relation_audit_pending_overwrite_fallback", {
        oldRelationCount: oldPending.relations.length,
        oldBatchNames: (oldPending.batchNames || []).slice(0, 40),
        oldChapterId: oldPending.chapterId || "",
        oldBatchKey: oldPending.batchKey || "",
        newRelationCount: relations.length,
        reason: "pending_would_be_overwritten_before_alias_consumed"
      });
      this.auditPendingNameSemanticRelationsIfNeeded(oldPending.chapterText || "", {
        forceStandalone: true,
        forceReason: "pending_overwrite_before_alias_consumed"
      });
    }
  } catch (pendingFlushErr) {
    try { graphRemoteLog("model_relation_audit_pending_overwrite_fallback", { error: graphSafeString(pendingFlushErr && pendingFlushErr.message || pendingFlushErr, 260), reason: "pending_overwrite_fallback_exception" }); } catch(e0) {}
  }
  var pendingBatchKey = graphHash(graphSafeString(chapterFullContent || "", 5000) + "#" + relations.length);
  for (var ri = 0; ri < relations.length; ri++) graphEnsureRelationEvidenceMeta(relations[ri], pendingBatchKey);
  this.pendingNameSemanticRelations = {
    relations: relations,
    // 合并审计需要看到姓名分析实际使用的完整当前批文本；这里不再二次截字符。
    chapterText: String(chapterFullContent || ""),
    batchNames: batchNames || [],
    hasNewRoleCandidate: !!hasNewRoleCandidate,
    consumed: false,
    auditBuffered: false, // 合并审计结果已缓冲但尚未按提交顺序落图时，防止重复请求
    createdAt: graphNowIso(),
    chapterId: graphCurrentChapterId(),
    batchKey: pendingBatchKey
  };
  graphRemoteLog("name_semantic_pending", {
    stage: "pending_after_shape_precheck",
    count: relations.length,
    hasNewRoleCandidate: !!hasNewRoleCandidate,
    chapterId: this.pendingNameSemanticRelations.chapterId,
    batchKey: this.pendingNameSemanticRelations.batchKey,
    batchNames: (batchNames || []).slice(0, 40),
    relationIds: relations.map(function(r){ return r && r.relationId || ""; }).slice(0, 80),
    evidenceHashes: relations.map(function(r){ return r && r.evidenceHash || ""; }).slice(0, 80)
  });
};

CharacterManager.prototype.getPendingNameSemanticRelationsForAliasCheck = function(chapterFullContent, newName) {
  var p = this.pendingNameSemanticRelations;
  if (!p || p.consumed || p.auditBuffered || !p.relations || !p.relations.length) return [];
  return p.relations;
};

CharacterManager.prototype.buildAliasCheckRelationAuditBlock = function(relations, newName, chapterFullContent) {
  relations = relations || [];
  if (!relations.length) return "";
  var auditItems = graphBuildCompactRelationAuditItems(relations);
  var candidateSetId = graphRelationAuditCandidateSetId(relations);
  return "【当前批次新证据审计任务】\n" +
    "以下证据来自当前批量姓名分析模型，属于整批待审计证据，不一定都与【新名字】直接相关；你在判断【新名字】是否为别名时，同时审计这些新证据。\n" +
    "审计原则：\n" +
    "1. evidenceText必须能在【当前章节内容】中找到对应原文锚点；只允许标点、空白等不改变语义的轻微差异。改写、概括、省略号拼接、只在summary中成立、依赖输入范围外信息，都不能直接采纳。\n" +
    "2. 必须按evidenceFamily/evidenceSubtype分别核对，禁止给所有证据套用‘a、b都必须是本批编号说话人’的条件。name_identity中的self_claim/called_as/introduced_as/parenthetical_alias等，只要当前输入确认角色与其自称/被称/被介绍的名字属于同一人即可；身份名不需要另外成为本批说话人。\n" +
    "2a. dialogue_relation才重点核对说话人、受话人、回复对象和对白归属；对白内容中仅提到名称不自动等于对该名称说话。action/social/co_presence/identity_relation按各自语义核对，两端同样不要求都是本批说话人；清晰代词可用，输入外知识不可用。\n" +
    "2b. 必须逐条核对a、b及relationType/evidenceFamily/evidenceSubtype强度是否与原文一致；仅同场、提及、想起、寻找、调查、声音来源或间接关系，不得冒充直接同人/直接非同人证据。\n" +
    "3. evidenceText有真实锚点但表述不精确时，放入downgrade并在correctedEvidenceText写入当前章节中的原文片段；没有可用锚点时放入reject。\n" +
    "4. 如果证据是模型幻觉、文本没有支持、人物归属错误、把被谈论对象当说话人，放入reject。\n" +
    "5. 如果证据有价值但强度不足、仅提及/想起/寻找/调查/间接关系，放入downgrade。\n" +
    "6. 如果新证据与旧正反图谱明显冲突，放入verify。\n" +
    "7. 只有每一条证据都按自己的证据族/子类型完成核对且无需修正时，才允许使用__ALL__；不得因身份名不是本批说话人而误拒自称/介绍证据。\n" +
    "8. 不要输出合并/拆分动作，只输出证据审计结论。\n\n" +
    "【待审计新证据】\n候选总数=" + auditItems.length + "，candidateSetId=" + candidateSetId + "。\n" + JSON.stringify(auditItems) + "\n\n" +
    "【强制输出补充】\n" +
    "本次输入包含【当前批次新证据审计任务】，所以输出JSON除了isAlias/mainName/reason外，还必须包含完整审计结构：auditComplete、allAccepted、acceptedAll、downgrade、reject、verify。\n" +
    "全部采纳时：allAccepted=true，acceptedAll必须返回[\"__ALL__\"]，不要逐条列出relationId，downgrade/reject/verify必须为空数组；allAcceptedVerification八个字段必须全部为true。\n" +
    "不是全部采纳时：allAccepted=false，acceptedAll必须为空数组，downgrade/reject/verify必须完整列出所有非采纳项；未列入三类数组的relationId在结构完整前提下视为采纳。\n" +
    "必须原样返回candidateCount=" + auditItems.length + "和candidateSetId=" + candidateSetId + "。downgrade/reject/verify每项必须有relationId和auditReason，relationId必须精确匹配待审计证据；缺字段、缺数组、candidateCount/candidateSetId不匹配、relationId不匹配均视为审计失败；allAccepted=true时acceptedAll不是[\"__ALL__\"]，或allAcceptedVerification缺失/任一项不为true，也视为失败。\n" +
    '  "auditComplete": true,\n' +
    '  "candidateCount": ' + auditItems.length + ',\n' +
    '  "candidateSetId": "' + candidateSetId + '",\n' +
    '  "allAccepted": true/false,\n' +
    '  "acceptedAll": ["__ALL__"],\n' +
    '  "allAcceptedVerification": {"everyEvidenceTextGrounded":true,"everyEvidenceJudgedByFamilyAndSubtype":true,"everyNameIdentitySelfClaimOrIntroductionPreserved":true,"noSpeakerRequirementMisappliedToNonDialogueEvidence":true,"everyDialogueAttributionCheckedWhenApplicable":true,"everyCoreferenceCheckedWhenUsed":true,"noExternalKnowledgeUsed":true,"everyRelationStrengthChecked":true},\n' +
    '  "downgrade": [{"relationId":"rel_3","auditReason":"理由","correctedEvidenceText":"","usableForAlias":false,"usableForGraph":false,"usableForRecordDecision":false}],\n' +
    '  "reject": [{"relationId":"rel_4","auditReason":"理由","correctedEvidenceText":"","usableForAlias":false,"usableForGraph":false,"usableForRecordDecision":false}],\n' +
    '  "verify": [{"relationId":"rel_5","auditReason":"理由","correctedEvidenceText":"","usableForAlias":false,"usableForGraph":false,"usableForRecordDecision":false}]\n';
};

CharacterManager.prototype.auditPendingNameSemanticRelationsIfNeeded = function(chapterFullContent, options) {
  options = options || {};
  var p = this.pendingNameSemanticRelations;
  if (!p || p.consumed || !p.relations || !p.relations.length) return { skipped: true };
  if (p.auditBuffered) return { skipped: true, bufferedForOrderedCommit: true };
  if (p.hasNewRoleCandidate && !options.forceStandalone) {
    graphRemoteLog("model_relation_audit_deferred_waiting_alias", { relationCount: p.relations.length, batchNames: (p.batchNames || []).slice(0, 40), reason: "batch_has_new_role_candidate", chapterId: p.chapterId || "", batchKey: p.batchKey || "" });
    return { skipped: true, deferredToAlias: true };
  }
  if (p.hasNewRoleCandidate && options.forceStandalone) {
    graphRemoteLog("model_relation_audit_deferred_alias_queue_empty", {
      relationCount: p.relations.length,
      batchNames: (p.batchNames || []).slice(0, 40),
      reason: options.forceReason || "alias_queue_completed",
      chapterId: p.chapterId || "",
      batchKey: p.batchKey || "",
      relationIds: p.relations.map(function(r){ return r && r.relationId || ""; }).slice(0, 80)
    });
  }
  var auditResult = this.auditNameSemanticRelationsByAliasApi ? this.auditNameSemanticRelationsByAliasApi(p.relations, chapterFullContent || p.chapterText || "") : { success: false, audits: [] };
  p.consumed = true;
  p.consumedBy = p.hasNewRoleCandidate ? (options.forceReason || "standalone_audit_alias_queue_completed") : "standalone_audit_no_new_role";
  p.consumedAt = graphNowIso();
  if (!auditResult.success) return { positive: 0, negative: 0, audit: 0, rejected: p.relations.length, auditFailed: true };
  return this.applyAuditedNameSemanticRelations ? this.applyAuditedNameSemanticRelations(p.relations, auditResult.audits || [], chapterFullContent || p.chapterText || "") : { positive: 0, negative: 0, audit: 0, rejected: 0 };
};


CharacterManager.prototype.applyModelRelationEvidence = function(relations, chapterFullContent, batchNames) {
  if (!ENABLE_ALIAS_GRAPH || !ENABLE_MODEL_RELATION_EVIDENCE || !relations || !Array.isArray(relations) || relations.length === 0) return { positive: 0, negative: 0, audit: 0, rejected: 0 };
  if (!this.aliasCooccurStats) this.aliasCooccurStats = {};
  chapterFullContent = chapterFullContent || this.graphConflictVerifyChapterText || "";
  batchNames = batchNames || [];
  semanticShortLog("归并 " + relations.length + " 条");
  var groupMeta = relations._groupMeta || {};
  var sourceResultCount = Number(groupMeta.sourceResultCount || 0);
  var relationCountBeforeGroup = Number(groupMeta.relationCountBeforeGroup || relations.length);
  var relationCountAfterGroup = Number(groupMeta.relationCountAfterGroup || relations.length);
  // 单结果且归并前后没有变化时，原始响应已经完整记录，不再重复上传整批关系数组。
  if (sourceResultCount > 1 || relationCountBeforeGroup !== relationCountAfterGroup) {
    graphRemoteLog("name_semantic_voted_raw", {
      stage: "grouped_raw_before_shape_precheck",
      relationCount: relations.length,
      sourceResultCount: sourceResultCount,
      relationCountBeforeGroup: relationCountBeforeGroup,
      relationCountAfterGroup: relationCountAfterGroup,
      voteMeaning: groupMeta.voteMeaning || "single_result_bucket_hits_or_multi_result_votes",
      filterPolicy: groupMeta.filterPolicy || "no_local_confidence_filter",
      samples: relations.slice(0, 80)
    });
  }
  var pending = [];
  var shapeRejected = 0;
  var relationBatchKey = graphHash(graphSafeString(chapterFullContent || "", 5000) + "#" + relations.length);
  for (var i = 0; i < relations.length; i++) {
    var raw = relations[i] || {};
    var pc = graphPrecheckModelRelationShape(raw);
    if (!pc.ok) {
      shapeRejected++;
      graphRemoteLog("name_semantic_shape_rejected", { stage: "shape_precheck_rejected", reason: pc.reason, raw: raw });
      continue;
    }
    var r = pc.relation;
    r.relationId = r.relationId || ("rel_" + (pending.length + 1));
    graphEnsureRelationEvidenceMeta(r, relationBatchKey);
    pending.push(r);
  }
  graphRemoteLog("name_semantic_shape_summary", {
    stage: "shape_precheck_complete",
    inputCount: relations.length,
    passedCount: pending.length,
    rejectedCount: shapeRejected,
    passedRelationIds: pending.map(function(r){ return r && r.relationId || ""; }).slice(0, 80),
    passedEvidenceHashes: pending.map(function(r){ return r && r.evidenceHash || ""; }).slice(0, 80)
  });
  semanticShortLog("格式通过 " + pending.length + " 条，格式拒收 " + shapeRejected + " 条");
  if (!pending.length) return { positive: 0, negative: 0, audit: 0, rejected: shapeRejected };

  var hasNewRoleCandidate = this.graphBatchHasNewRoleCandidate ? this.graphBatchHasNewRoleCandidate(batchNames || []) : false;
  if (this.setPendingNameSemanticRelations) this.setPendingNameSemanticRelations(pending, chapterFullContent, batchNames || [], hasNewRoleCandidate);

  // 这里只做本地字段预检并暂存。是否走A+B+C、B+C或单项流程，由统一容错路由在角色处理出口决定。
  auditShortLog((hasNewRoleCandidate ? "等待三合一审计 " : "等待二合一审计 ") + pending.length + " 条");
  graphRemoteLog("name_semantic_relation_deferred_to_combined_audit", {
    count: pending.length,
    batchNames: (batchNames || []).slice(0, 40),
    hasNewRoleCandidate: !!hasNewRoleCandidate,
    targetFlow: hasNewRoleCandidate ? "alias+voice_age+graph" : "voice_age+graph",
    chapterId: graphCurrentChapterId(),
    batchKey: relationBatchKey,
    relationIds: pending.map(function(r){ return r && r.relationId || ""; }).slice(0, 80)
  });
  return { positive: 0, negative: 0, audit: 0, rejected: shapeRejected, pending: pending.length, deferredToCombinedAudit: true, hasNewRoleCandidate: !!hasNewRoleCandidate };
};


// ===================== 本地高精度正/反图谱识别辅助（正边收紧 + 反边扩题材）=====================
function graphReasonListHas(reasons, reason) {
  if (!reasons) return false;
  for (var i = 0; i < reasons.length; i++) if (reasons[i] === reason) return true;
  return false;
}
function graphReasonStartsWith(reasons, prefix) {
  if (!reasons) return false;
  for (var i = 0; i < reasons.length; i++) if (String(reasons[i] || "").indexOf(prefix) === 0) return true;
  return false;
}
function graphAnyReason(reasons, arr) {
  if (!reasons || !arr) return false;
  for (var i = 0; i < arr.length; i++) if (graphReasonListHas(reasons, arr[i])) return true;
  return false;
}
function graphCleanSourceReasons(reasons) {
  var out = [];
  var seen = {};
  var removed = [];
  reasons = reasons || [];
  for (var i = 0; i < reasons.length; i++) {
    var r = graphSafeString(reasons[i] || "", 100);
    if (!r) continue;
    if (r.indexOf("compound_") === 0 || r.indexOf("复合:") === 0 || r === "triad_interaction_closed" || r === "positive_chain_closed" || r === "cross_chapter_recall_candidate" || r === "audit_recall_candidate") {
      removed.push(r);
      continue;
    }
    if (!seen[r]) { seen[r] = true; out.push(r); }
  }
  if (removed.length) graphRemoteLog("compound_self_reference_removed", { removedReasons: removed.slice(0, 20) });
  return out;
}

function graphV908IsForbiddenCompoundSourceReason(reason) {
  reason = graphSafeString(reason || "", 120);
  if (!reason) return true;
  if (reason.indexOf("compound_") === 0) return true;
  if (reason.indexOf("复合:") === 0) return true;
  if (reason === "positive_chain_closed" || reason === "triad_interaction_closed") return true;
  return false;
}
function graphV908FilterCompoundSourceReasonsBeforeScan(reasons, stats) {
  var out = [];
  var seen = {};
  reasons = reasons || [];
  for (var i = 0; i < reasons.length; i++) {
    var r = graphSafeString(reasons[i] || "", 120);
    if (!r) continue;
    if (graphV908IsForbiddenCompoundSourceReason(r)) {
      if (stats) {
        stats.filteredCount = Number(stats.filteredCount || 0) + 1;
        if (!stats.filteredReasons) stats.filteredReasons = [];
        if (stats.filteredReasons.indexOf(r) === -1 && stats.filteredReasons.length < 20) stats.filteredReasons.push(r);
      }
      continue;
    }
    if (!seen[r]) { seen[r] = true; out.push(r); }
  }
  return out;
}

function graphCompoundEvidenceIsDirty(kind, text) {
  kind = graphSafeString(kind || "", 120);
  text = graphSafeString(text || "", 300);
  if (kind.indexOf("复合") !== -1 || text.indexOf("复合") !== -1) return true;
  if (kind.indexOf("compound_") === 0 || text.indexOf("compound_") !== -1) return true;
  if (kind === "三角闭合" || kind === "正链闭合" || kind === "triad_interaction_closed" || kind === "positive_chain_closed") return true;
  if (text.indexOf("三角闭合") !== -1 || text.indexOf("正链") !== -1 || text.indexOf("triad_interaction_closed") !== -1 || text.indexOf("positive_chain_closed") !== -1) return true;
  return false;
}

function graphCompoundChapterMarkOnce(st, reason, chapter) {
  if (!st) return true;
  reason = graphSafeString(reason || "", 80);
  chapter = graphSafeString(chapter || "", 40);
  var key = reason + "@" + chapter;
  if (!st.compoundChapterMarks) st.compoundChapterMarks = [];
  for (var i = 0; i < st.compoundChapterMarks.length; i++) if (st.compoundChapterMarks[i] === key) return false;
  st.compoundChapterMarks.push(key);
  if (st.compoundChapterMarks.length > 80) st.compoundChapterMarks = st.compoundChapterMarks.slice(st.compoundChapterMarks.length - 80);
  return true;
}

function graphCompoundEvidenceText(posEdge, negEdge, st) {
  var parts = [];
  if (posEdge && posEdge.extra && !graphCompoundEvidenceIsDirty("positiveEdge", posEdge.extra)) parts.push("正:" + graphSafeString(posEdge.extra, 120));
  if (negEdge && negEdge.extra && !graphCompoundEvidenceIsDirty("negativeEdge", negEdge.extra)) parts.push("反:" + graphSafeString(negEdge.extra, 120));
  if (st && st.evidence && st.evidence.length) {
    var ev = graphFilterRecentEvidence(st.evidence, null, 4);
    for (var i = 0; i < ev.length && parts.length < 3; i++) {
      var kind = ev[i].kind || "";
      var txt = ev[i].text || "";
      if (graphCompoundEvidenceIsDirty(kind, txt)) continue;
      parts.push("[" + kind + "]" + graphSafeString(txt, 120));
    }
  }
  return parts.join(" || ");
}


// ===================== 持久化全库复合扫描 + 同人正证子类型复合 =====================
var ENABLE_PERSISTENT_COMPOUND_GRAPH_SCAN = 1;
var ENABLE_NAME_IDENTITY_SUBTYPE_COMPOUND = 1;
var PERSISTENT_COMPOUND_LOG_PREFIX = "【复合证据】";

function graphV908ArrayUniqueSorted(arr) {
  var seen = {}, out = [];
  arr = arr || [];
  for (var i = 0; i < arr.length; i++) {
    var v = graphSafeString(arr[i] || "", 160);
    if (!v || seen[v]) continue;
    seen[v] = true;
    out.push(v);
  }
  out.sort();
  return out;
}
function graphV908CollectGraphPairs(graph, pairMap, filterStats) {
  if (!graph || !pairMap) return;
  for (var a in graph) {
    if (!graph.hasOwnProperty(a) || a.indexOf("__") === 0) continue;
    var row = graph[a];
    if (!row) continue;
    for (var b in row) {
      if (!row.hasOwnProperty(b) || b.indexOf("__") === 0) continue;
      var edge = row[b] || {};
      var cleanReasons = graphV908FilterCompoundSourceReasonsBeforeScan(edge.reasons || [], filterStats);
      if (!cleanReasons.length) continue;
      graphV908AddCompoundPair(pairMap, a, b);
    }
  }
}
function graphV908AddCompoundPair(pairMap, a, b) {
  a = graphNormalizeName(a); b = graphNormalizeName(b);
  if (!pairMap || !a || !b || a === b) return;
  if (graphIsInvalidName(a) || graphIsInvalidName(b) || graphIsGroupName(a) || graphIsGroupName(b)) return;
  pairMap[graphPairKey(a, b)] = { a: a, b: b };
}
function graphV908CollectPersistentCompoundPairs(cm, names, filterStats) {
  var pairMap = {};
  names = names || [];
  for (var i = 0; i < names.length; i++) {
    for (var j = i + 1; j < names.length; j++) graphV908AddCompoundPair(pairMap, names[i], names[j]);
  }
  var cur = graphCurrentChapterId();
  if (cm && cm.aliasCooccurStats) {
    for (var k in cm.aliasCooccurStats) {
      if (!cm.aliasCooccurStats.hasOwnProperty(k) || k.indexOf("__") === 0) continue;
      var st = cm.aliasCooccurStats[k];
      if (st && st.a && st.b) graphV908AddCompoundPair(pairMap, st.a, st.b);
    }
  }
  if (cm) {
    graphV908CollectGraphPairs(cm.aliasPositiveGraph, pairMap, filterStats);
    graphV908CollectGraphPairs(cm.aliasNegativeGraph, pairMap, filterStats);
  }
  return pairMap;
}
function graphV908NameIdentitySubtypeLevel(subtype) {
  subtype = graphNormalizeEvidenceSubtype(subtype || "");
  if (!subtype || subtype === "unknown_subtype") return "";
  if (subtype === "explicit_same_person" || subtype === "explicit_same" || subtype === "name_alias_statement" || subtype === "alias_statement" || subtype === "parenthetical_alias" || subtype === "self_claim" || subtype === "introduced_as_same_person") return "strong";
  if (subtype === "introduced_as" || subtype === "descriptor_alias" || subtype === "name_alias") return "medium";
  if (subtype === "called_as" || subtype === "honorific_title" || subtype === "temporary_descriptor" || subtype === "relation_title" || subtype === "kinship_title") return "weak";
  return "";
}
function graphV908CollectNameIdentitySubtypeInfo(posEdge, st) {
  var info = { strong: 0, medium: 0, weak: 0, total: 0, subtypes: [], evidenceKeys: [], evidenceHashes: [], chapters: [], items: [] };
  var seen = {};
  function addSample(sample, source) {
    sample = sample || {};
    var reason = graphSafeString(sample.reason || sample.kind || "", 100);
    var relationType = graphSafeString(sample.relationType || "", 60);
    var family = graphSafeString(sample.evidenceFamily || "", 80);
    var subtype = graphNormalizeEvidenceSubtype(sample.evidenceSubtype || "");
    if (reason !== "model_name_identity_positive" && relationType !== "same_person" && family !== "name_identity") return;
    var level = graphV908NameIdentitySubtypeLevel(subtype);
    if (!level) return;
    var evKey = graphSafeString(sample.evidenceKey || "", 180);
    var evHash = graphSafeString(sample.evidenceHash || "", 80);
    var txt = graphSafeString(sample.text || sample.summary || "", 180);
    var uniq = subtype + "|" + (evKey || evHash || txt || source || "sample");
    if (seen[uniq]) return;
    seen[uniq] = true;
    info[level] = Number(info[level] || 0) + 1;
    info.total++;
    info.subtypes.push(subtype);
    if (evKey) info.evidenceKeys.push(evKey);
    if (evHash) info.evidenceHashes.push(evHash);
    if (sample.chapter) info.chapters.push(sample.chapter);
    info.items.push({ subtype: subtype, level: level, evidenceKey: evKey, evidenceHash: evHash, chapter: graphSafeString(sample.chapter || "", 80), source: source || "" });
  }
  if (posEdge && posEdge.evidenceSamples && Array.isArray(posEdge.evidenceSamples)) {
    for (var i = 0; i < posEdge.evidenceSamples.length; i++) addSample(posEdge.evidenceSamples[i], "positive_edge");
  }
  if (st && st.evidence && Array.isArray(st.evidence)) {
    for (var j = 0; j < st.evidence.length; j++) addSample(st.evidence[j], "cooccur_stats");
  }
  info.subtypes = graphV908ArrayUniqueSorted(info.subtypes);
  info.evidenceKeys = graphV908ArrayUniqueSorted(info.evidenceKeys);
  info.evidenceHashes = graphV908ArrayUniqueSorted(info.evidenceHashes);
  info.chapters = graphV908ArrayUniqueSorted(info.chapters);
  return info;
}
function graphV908NameIdentitySubtypeDecision(info) {
  info = info || {};
  var s = Number(info.strong || 0), m = Number(info.medium || 0), w = Number(info.weak || 0);
  if (s >= 2) return { action: "positive", reason: "compound_strong_name_identity_positive", label: "同人子类型强强复合" };
  if (s >= 1 && m >= 1) return { action: "positive", reason: "compound_mixed_name_identity_positive", label: "同人子类型强中复合" };
  if (s >= 1 && w >= 2) return { action: "positive", reason: "compound_strong_weak_name_identity_positive", label: "同人子类型强弱弱复合" };
  if (m >= 3) return { action: "positive", reason: "compound_medium_name_identity_positive", label: "同人子类型中中中复合" };
  if (m >= 2 && w >= 2) return { action: "positive", reason: "compound_medium_weak_name_identity_positive", label: "同人子类型中中弱弱复合" };
  if (w >= 4) return { action: "hint", reason: "weak_name_identity_hint", label: "弱同人提示" };
  return { action: "", reason: "", label: "" };
}
function graphV908CompoundSignature(pairKey, direction, reason, sourceReasons, subtypeInfo) {
  sourceReasons = graphV908ArrayUniqueSorted(sourceReasons || []);
  subtypeInfo = subtypeInfo || {};
  var body = [pairKey, direction, reason, sourceReasons.join("+"), (subtypeInfo.subtypes || []).join("+"), (subtypeInfo.evidenceKeys || []).join("+"), (subtypeInfo.evidenceHashes || []).join("+"), (subtypeInfo.chapters || []).join("+")].join("|");
  return "pcompound:" + graphHash(body);
}
function graphV908CompoundSignatureSeen(st, signature) {
  if (!st || !signature) return false;
  if (!st.compoundSourceSignatures || !Array.isArray(st.compoundSourceSignatures)) st.compoundSourceSignatures = [];
  return st.compoundSourceSignatures.indexOf(signature) !== -1;
}
function graphV908MarkCompoundSignature(st, signature) {
  if (!st || !signature) return;
  if (!st.compoundSourceSignatures || !Array.isArray(st.compoundSourceSignatures)) st.compoundSourceSignatures = [];
  if (st.compoundSourceSignatures.indexOf(signature) === -1) st.compoundSourceSignatures.push(signature);
  if (st.compoundSourceSignatures.length > 500) st.compoundSourceSignatures = st.compoundSourceSignatures.slice(st.compoundSourceSignatures.length - 500);
}

function graphV908IsCompoundReasonName(reason) {
  reason = graphSafeString(reason || "", 120);
  return reason.indexOf("compound_") === 0 || reason.indexOf("复合:") === 0 || reason === "triad_interaction_closed" || reason === "positive_chain_closed";
}
function graphV908IsSemanticCompoundSourceReason(reason) {
  reason = graphSafeString(reason || "", 120);
  if (!reason || graphV908IsCompoundReasonName(reason)) return false;
  return reason === "model_name_identity_positive" ||
    reason === "model_dialogue_relation_negative" ||
    reason === "model_action_relation_negative" ||
    reason === "model_social_relation_negative" ||
    reason === "model_co_presence_negative" ||
    reason === "model_explicit_different_negative" ||
    reason === "alias_refine_confirmed" ||
    reason === "graph_conflict_verified_same_person" ||
    reason === "graph_conflict_verified_different_person";
}
function graphV908SampleSemanticReason(sample) {
  sample = sample || {};
  return graphSafeString(sample.reason || sample.kind || "", 120);
}
function graphV908SampleSemanticContributionKey(sample, reason) {
  sample = sample || {};
  reason = graphSafeString(reason || graphV908SampleSemanticReason(sample), 120);
  if (!graphV908IsSemanticCompoundSourceReason(reason)) return "";
  var chapter = graphSafeString(sample.chapter || sample.chapterId || graphCurrentChapterId(), 80);
  var key = graphSafeString(sample.evidenceKey || "", 180);
  if (!key) {
    var h = graphSafeString(sample.evidenceHash || "", 80);
    if (!h) h = graphBuildEvidenceHash(sample.text || sample.summary || sample.evidenceText || reason || "");
    key = h ? ("hash:" + h) : "reason_only";
  }
  return chapter + "|" + reason + "|" + key;
}
function graphV908CollectCompoundSemanticEvidenceKeys(posEdge, negEdge, st, sourceReasons) {
  var sourceSet = {};
  sourceReasons = graphCleanSourceReasons(sourceReasons || []);
  for (var i = 0; i < sourceReasons.length; i++) {
    if (graphV908IsSemanticCompoundSourceReason(sourceReasons[i])) sourceSet[sourceReasons[i]] = true;
  }
  var out = [];
  var seen = {};
  function addSample(sample) {
    sample = sample || {};
    var reason = graphV908SampleSemanticReason(sample);
    if (!sourceSet[reason]) return;
    var ck = graphV908SampleSemanticContributionKey(sample, reason);
    if (!ck || seen[ck]) return;
    seen[ck] = true;
    out.push(ck);
  }
  function addEdge(edge) {
    if (!edge || !edge.evidenceSamples || !Array.isArray(edge.evidenceSamples)) return;
    for (var i = 0; i < edge.evidenceSamples.length; i++) addSample(edge.evidenceSamples[i]);
  }
  addEdge(posEdge);
  addEdge(negEdge);
  if (st && st.evidence && Array.isArray(st.evidence)) {
    for (var j = 0; j < st.evidence.length; j++) addSample(st.evidence[j]);
  }
  return graphV908ArrayUniqueSorted(out);
}
function graphV908CompoundContributionMarkKey(direction, reason, semanticKey) {
  return graphSafeString(direction || "", 20) + "|" + graphSafeString(reason || "", 120) + "|" + graphSafeString(semanticKey || "", 240);
}
function graphV908CompoundHasNewSemanticContribution(st, direction, reason, semanticKeys) {
  if (!st) return true;
  semanticKeys = semanticKeys || [];
  if (!st.compoundSemanticContributionKeys || !Array.isArray(st.compoundSemanticContributionKeys)) st.compoundSemanticContributionKeys = [];
  if (!semanticKeys.length) return true;
  for (var i = 0; i < semanticKeys.length; i++) {
    var k = graphV908CompoundContributionMarkKey(direction, reason, semanticKeys[i]);
    if (st.compoundSemanticContributionKeys.indexOf(k) === -1) return true;
  }
  return false;
}
function graphV908MarkCompoundSemanticContribution(st, direction, reason, semanticKeys) {
  if (!st) return;
  semanticKeys = semanticKeys || [];
  if (!st.compoundSemanticContributionKeys || !Array.isArray(st.compoundSemanticContributionKeys)) st.compoundSemanticContributionKeys = [];
  for (var i = 0; i < semanticKeys.length; i++) {
    var k = graphV908CompoundContributionMarkKey(direction, reason, semanticKeys[i]);
    if (k && st.compoundSemanticContributionKeys.indexOf(k) === -1) st.compoundSemanticContributionKeys.push(k);
  }
  if (st.compoundSemanticContributionKeys.length > 1000) st.compoundSemanticContributionKeys = st.compoundSemanticContributionKeys.slice(st.compoundSemanticContributionKeys.length - 1000);
}
function graphV908CompoundReconcileSignature(edge, direction, pairKey) {
  edge = edge || {};
  var samples = [];
  if (edge.evidenceSamples && Array.isArray(edge.evidenceSamples)) {
    for (var i = 0; i < edge.evidenceSamples.length; i++) {
      var sm = edge.evidenceSamples[i] || {};
      samples.push(graphSafeString(sm.evidenceKey || sm.evidenceHash || sm.text || "", 180));
    }
  }
  return "reconcile:" + graphHash([pairKey, direction, graphV908ArrayUniqueSorted(edge.reasons || []).join("+"), graphV908ArrayUniqueSorted(samples).join("+")].join("|"));
}
function graphV908ReconcileSignatureSeen(st, signature) {
  if (!st || !signature) return false;
  if (!st.compoundReconcileSignatures || !Array.isArray(st.compoundReconcileSignatures)) st.compoundReconcileSignatures = [];
  return st.compoundReconcileSignatures.indexOf(signature) !== -1;
}
function graphV908MarkReconcileSignature(st, signature) {
  if (!st || !signature) return;
  if (!st.compoundReconcileSignatures || !Array.isArray(st.compoundReconcileSignatures)) st.compoundReconcileSignatures = [];
  if (st.compoundReconcileSignatures.indexOf(signature) === -1) st.compoundReconcileSignatures.push(signature);
  if (st.compoundReconcileSignatures.length > 500) st.compoundReconcileSignatures = st.compoundReconcileSignatures.slice(st.compoundReconcileSignatures.length - 500);
}
function graphV908PositiveSubtypeCompoundReasons() {
  return ["compound_strong_name_identity_positive","compound_mixed_name_identity_positive","compound_strong_weak_name_identity_positive","compound_medium_name_identity_positive","compound_medium_weak_name_identity_positive"];
}
function graphV908PositiveCompoundRecordReasons() {
  return ["compound_name_alias_positive","compound_introduced_alias_positive","compound_parenthetical_alias_positive","compound_verified_same_person_positive","compound_strong_name_identity_positive","compound_mixed_name_identity_positive","compound_strong_weak_name_identity_positive","compound_medium_name_identity_positive","compound_medium_weak_name_identity_positive"];
}

CharacterManager.prototype.applyPersistentCompoundGraphEvidence = function(names, chapterText) {
  if (!ENABLE_PERSISTENT_COMPOUND_GRAPH_SCAN || !ENABLE_COMPOUND_GRAPH_EVIDENCE || !ENABLE_ALIAS_GRAPH) return { positive: 0, negative: 0, hint: 0, skipped: 0, pairs: 0 };
  if (!this.aliasCooccurStats) this.aliasCooccurStats = {};
  var filterStats = { filteredCount: 0, filteredReasons: [] };
  var pairMap = graphV908CollectPersistentCompoundPairs(this, names || [], filterStats);
  var keys = Object.keys(pairMap || {});
  var pos = 0, neg = 0, hint = 0, skipped = 0;
  graphRemoteLog("persistent_compound_scan_start", { pairCount: keys.length, chapter: graphCurrentChapterId(), policy: "full_persistent_graph_scan_no_pair_limit_prefilter_compound_sources", filteredSourceReasonCount: filterStats.filteredCount || 0, filteredSourceReasons: filterStats.filteredReasons || [] });
  for (var pi = 0; pi < keys.length; pi++) {
    var item = pairMap[keys[pi]] || {};
    var a = item.a, b = item.b;
    var pairKey = graphPairKey(a, b);
    var st = graphGetPairStats(this.aliasCooccurStats, a, b);
    if (!st) { skipped++; continue; }
    var pe = graphGetEdgeSnapshot(this.aliasPositiveGraph, a, b);
    var ne = graphGetEdgeSnapshot(this.aliasNegativeGraph, a, b);
    var posReasons = graphV908FilterCompoundSourceReasonsBeforeScan(pe ? pe.reasons || [] : [], filterStats);
    var negReasons = graphV908FilterCompoundSourceReasonsBeforeScan(ne ? ne.reasons || [] : [], filterStats);
    if (st) {
      if (Number(st.sameSentence || 0) > 0) negReasons.push("same_sentence_cooccur");
      if (Number(st.adjacentSpeaker || 0) > 0) negReasons.push("adjacent_speaker_cooccur");
      if (Number(st.modelPositive || 0) > 0 && !graphReasonListHas(posReasons, "model_name_identity_positive")) posReasons.push("model_name_identity_positive");
      if (Number(st.modelNegative || 0) > 0 && !graphReasonListHas(negReasons, "model_dialogue_relation_negative")) negReasons.push("model_dialogue_relation_negative");
    }
    posReasons = graphV908FilterCompoundSourceReasonsBeforeScan(posReasons, filterStats);
    negReasons = graphV908FilterCompoundSourceReasonsBeforeScan(negReasons, filterStats);
    var allReasons = posReasons.concat(negReasons);
    var hasSemantic = graphAnyReason(allReasons, ["model_name_identity_positive","model_dialogue_relation_negative","model_action_relation_negative","model_social_relation_negative","model_co_presence_negative","model_explicit_different_negative","graph_conflict_verified_same_person","graph_conflict_verified_different_person","alias_refine_confirmed"]);
    if (!hasSemantic) { skipped++; continue; }
    var evidence = graphCompoundEvidenceText(pe, ne, st);
    var wrote = false;
    var subtypeInfo = ENABLE_NAME_IDENTITY_SUBTYPE_COMPOUND ? graphV908CollectNameIdentitySubtypeInfo(pe, st) : { strong: 0, medium: 0, weak: 0, subtypes: [], evidenceKeys: [], evidenceHashes: [], chapters: [] };
    var subtypeDecision = ENABLE_NAME_IDENTITY_SUBTYPE_COMPOUND ? graphV908NameIdentitySubtypeDecision(subtypeInfo) : { action: "", reason: "", label: "" };
    function writeNeg(reason, score, label, src) {
      if (wrote) return;
      src = graphV908FilterCompoundSourceReasonsBeforeScan(src || [], filterStats);
      var semanticKeys = graphV908CollectCompoundSemanticEvidenceKeys(pe, ne, st, src);
      var sig = graphV908CompoundSignature(pairKey, "negative", reason, src, { evidenceKeys: semanticKeys });
      if (graphV908CompoundSignatureSeen(st, sig)) { skipped++; wrote = true; graphRemoteLog("persistent_compound_signature_skipped", { a: a, b: b, direction: "negative", reason: reason, signature: sig }); return; }
      if (!graphV908CompoundHasNewSemanticContribution(st, "negative", reason, semanticKeys)) {
        skipped++;
        wrote = true;
        graphV908MarkCompoundSignature(st, sig);
        graphRemoteLog("persistent_compound_duplicate_evidence_skipped", { a: a, b: b, direction: "negative", reason: reason, signature: sig, semanticEvidenceKeys: semanticKeys.slice(0, 12), note: "同一语义证据已为该复合原因计分，本次只跳过重复加分" });
        graphShortLog(PERSISTENT_COMPOUND_LOG_PREFIX + "重复证据已计分，跳过：" + a + "/" + b);
        return;
      }
      var extra = "复合:" + src.join("+") + (evidence ? "；证据:" + evidence : "");
      st.compoundNegative = Number(st.compoundNegative || 0) + 1;
      graphPushCooccurEvidence(this.aliasCooccurStats, a, b, label, extra, { decision: "compound_negative", evidenceKey: sig, source: "persistent_compound_scan" });
      if (this.recordNegativeAliasEdge(a, b, score, reason, extra, sig, { evidenceKey: sig, evidenceText: extra, source: "persistent_compound_scan", semanticEvidenceKeys: semanticKeys.join("|") })) {
        graphV908MarkCompoundSignature(st, sig);
        graphV908MarkCompoundSemanticContribution(st, "negative", reason, semanticKeys);
        graphRemoteLog("persistent_compound_graph_edge", { a: a, b: b, direction: "negative", reason: reason, score: score, sourceReasons: src, signature: sig, semanticEvidenceKeys: semanticKeys.slice(0, 12), evidence: graphSafeString(evidence, 500) });
        neg++;
      } else skipped++;
      wrote = true;
    }
    function writePos(reason, score, label, src, subInfo) {
      if (wrote) return;
      src = graphV908FilterCompoundSourceReasonsBeforeScan(src || [], filterStats);
      var semanticKeys = (subInfo && subInfo.evidenceKeys && subInfo.evidenceKeys.length) ? graphV908ArrayUniqueSorted(subInfo.evidenceKeys || []) : graphV908CollectCompoundSemanticEvidenceKeys(pe, ne, st, src);
      var sig = graphV908CompoundSignature(pairKey, "positive", reason, src, subInfo || { evidenceKeys: semanticKeys });
      if (graphV908CompoundSignatureSeen(st, sig)) { skipped++; wrote = true; graphRemoteLog("persistent_compound_signature_skipped", { a: a, b: b, direction: "positive", reason: reason, signature: sig }); return; }
      if (!graphV908CompoundHasNewSemanticContribution(st, "positive", reason, semanticKeys)) {
        skipped++;
        wrote = true;
        graphV908MarkCompoundSignature(st, sig);
        graphRemoteLog("persistent_compound_duplicate_evidence_skipped", { a: a, b: b, direction: "positive", reason: reason, signature: sig, semanticEvidenceKeys: semanticKeys.slice(0, 12), note: "同一语义证据已为该复合原因计分，本次只跳过重复加分" });
        graphShortLog(PERSISTENT_COMPOUND_LOG_PREFIX + "重复证据已计分，跳过：" + a + "/" + b);
        return;
      }
      var extra = "复合:" + src.join("+") + (subInfo && subInfo.subtypes && subInfo.subtypes.length ? "；子类型:" + subInfo.subtypes.join("+") + "；强" + Number(subInfo.strong || 0) + "中" + Number(subInfo.medium || 0) + "弱" + Number(subInfo.weak || 0) : "") + (evidence ? "；证据:" + evidence : "");
      st.compoundPositive = Number(st.compoundPositive || 0) + 1;
      graphPushCooccurEvidence(this.aliasCooccurStats, a, b, label, extra, { decision: "compound_positive", evidenceKey: sig, relationType: "same_person", evidenceFamily: "name_identity", evidenceSubtype: reason, source: "persistent_compound_scan" });
      if (this.recordPositiveAliasEdge(a, b, score, reason, extra, sig, { evidenceKey: sig, relationType: "same_person", evidenceFamily: "name_identity", evidenceSubtype: reason, evidenceText: extra, source: "persistent_compound_scan", semanticEvidenceKeys: semanticKeys.join("|") })) {
        graphV908MarkCompoundSignature(st, sig);
        graphV908MarkCompoundSemanticContribution(st, "positive", reason, semanticKeys);
        graphRemoteLog("persistent_compound_graph_edge", { a: a, b: b, direction: "positive", reason: reason, score: score, sourceReasons: src, subtypeInfo: subInfo || {}, signature: sig, semanticEvidenceKeys: semanticKeys.slice(0, 12), evidence: graphSafeString(evidence, 500) });
        if (subInfo && (subInfo.strong || subInfo.medium || subInfo.weak)) graphRemoteLog("persistent_compound_name_identity_subtype_scan", { a: a, b: b, decision: reason, strong: subInfo.strong || 0, medium: subInfo.medium || 0, weak: subInfo.weak || 0, subtypes: subInfo.subtypes || [], signature: sig, semanticEvidenceKeys: semanticKeys.slice(0, 12) });
        if (subInfo && (subInfo.strong || subInfo.medium || subInfo.weak)) graphShortLog(PERSISTENT_COMPOUND_LOG_PREFIX + "同人子类型 " + a + "=" + b + " 强" + Number(subInfo.strong || 0) + "中" + Number(subInfo.medium || 0) + "弱" + Number(subInfo.weak || 0));
        pos++;
      } else skipped++;
      wrote = true;
    }
    function writeHint(reason, label, subInfo) {
      if (wrote) return;
      var semanticKeys = (subInfo && subInfo.evidenceKeys && subInfo.evidenceKeys.length) ? graphV908ArrayUniqueSorted(subInfo.evidenceKeys || []) : [];
      var sig = graphV908CompoundSignature(pairKey, "hint", reason, ["model_name_identity_positive"], subInfo || { evidenceKeys: semanticKeys });
      if (graphV908CompoundSignatureSeen(st, sig)) { skipped++; wrote = true; return; }
      if (!graphV908CompoundHasNewSemanticContribution(st, "hint", reason, semanticKeys)) {
        skipped++;
        wrote = true;
        graphV908MarkCompoundSignature(st, sig);
        graphRemoteLog("persistent_compound_duplicate_evidence_skipped", { a: a, b: b, direction: "hint", reason: reason, signature: sig, semanticEvidenceKeys: semanticKeys.slice(0, 12), note: "弱同人提示的语义证据已记录，本次跳过重复提示" });
        return;
      }
      var extra = "提示:" + label + "；子类型:" + ((subInfo && subInfo.subtypes) || []).join("+") + "；强" + Number(subInfo && subInfo.strong || 0) + "中" + Number(subInfo && subInfo.medium || 0) + "弱" + Number(subInfo && subInfo.weak || 0) + (evidence ? "；证据:" + evidence : "");
      graphPushCooccurEvidence(this.aliasCooccurStats, a, b, label, extra, { decision: "weak_hint", evidenceKey: sig, relationType: "same_person", evidenceFamily: "name_identity", evidenceSubtype: reason, source: "persistent_compound_scan" });
      graphV908MarkCompoundSignature(st, sig);
      graphV908MarkCompoundSemanticContribution(st, "hint", reason, semanticKeys);
      graphRemoteLog("persistent_compound_name_identity_hint", { a: a, b: b, reason: reason, strong: subInfo && subInfo.strong || 0, medium: subInfo && subInfo.medium || 0, weak: subInfo && subInfo.weak || 0, subtypes: subInfo && subInfo.subtypes || [], signature: sig, semanticEvidenceKeys: semanticKeys.slice(0, 12), note: "弱弱弱弱只做提示，不直接动角色卡" });
      graphShortLog(PERSISTENT_COMPOUND_LOG_PREFIX + "弱同人提示 " + a + "/" + b + "：弱证" + Number(subInfo && subInfo.weak || 0) + "条，仅提示不合并");
      hint++;
      wrote = true;
    }
    if (graphReasonListHas(negReasons, "model_explicit_different_negative") && graphAnyReason(negReasons, ["model_dialogue_relation_negative","model_action_relation_negative","model_social_relation_negative","model_co_presence_negative","graph_conflict_verified_different_person"])) writeNeg.call(this, "compound_explicit_different_negative", GRAPH_COMPOUND_EXPLICIT_DIFFERENT_SCORE, "复合明确非同人反证", negReasons);
    if (!wrote && graphAnyReason(negReasons, ["model_dialogue_relation_negative","model_action_relation_negative"]) && graphAnyReason(negReasons, ["model_social_relation_negative","model_co_presence_negative","model_explicit_different_negative","graph_conflict_verified_different_person"])) writeNeg.call(this, "compound_speaker_interaction_negative", GRAPH_COMPOUND_SPEAKER_INTERACTION_SCORE, "复合模型互动反证", negReasons);
    if (!wrote && graphReasonListHas(negReasons, "model_social_relation_negative") && graphAnyReason(negReasons, ["model_dialogue_relation_negative","model_action_relation_negative","model_explicit_different_negative","graph_conflict_verified_different_person"])) writeNeg.call(this, "compound_relationship_interaction_negative", GRAPH_COMPOUND_RELATIONSHIP_INTERACTION_SCORE, "复合模型关系反证", negReasons);
    if (!wrote && graphReasonListHas(posReasons, "model_name_identity_positive") && graphAnyReason(posReasons, ["alias_refine_confirmed","graph_conflict_verified_same_person"])) writePos.call(this, "compound_name_alias_positive", GRAPH_COMPOUND_NAME_ALIAS_SCORE, "复合模型同人正证", posReasons, null);
    if (!wrote && graphReasonListHas(posReasons, "graph_conflict_verified_same_person") && graphAnyReason(posReasons, ["model_name_identity_positive","alias_refine_confirmed"])) writePos.call(this, "compound_verified_same_person_positive", GRAPH_COMPOUND_VERIFIED_SAME_SCORE, "复合冲突确认同人", posReasons, null);
    if (!wrote && subtypeDecision && subtypeDecision.action === "positive") writePos.call(this, subtypeDecision.reason, GRAPH_COMPOUND_NAME_ALIAS_SCORE, subtypeDecision.label, ["model_name_identity_positive"], subtypeInfo);
    if (!wrote && subtypeDecision && subtypeDecision.action === "hint") writeHint.call(this, subtypeDecision.reason, subtypeDecision.label, subtypeInfo);
    if (!wrote) skipped++;
  }
  if (pos || neg || hint) graphShortLog(PERSISTENT_COMPOUND_LOG_PREFIX + "全库扫描 pair " + keys.length + "，正" + pos + "，反" + neg + "，提示" + hint);
  graphRemoteLog("persistent_compound_scan_done", { pairCount: keys.length, positive: pos, negative: neg, hint: hint, skipped: skipped, chapter: graphCurrentChapterId(), filteredSourceReasonCount: filterStats.filteredCount || 0, filteredSourceReasons: filterStats.filteredReasons || [], sourceFilterPolicy: "prefilter_compound_positive_chain_triad_before_scan" });
  return { positive: pos, negative: neg, hint: hint, skipped: skipped, pairs: keys.length };
};

CharacterManager.prototype.persistentCompoundRecordReconciliation = function(chapterText) {
  if (!this.aliasCooccurStats) this.aliasCooccurStats = {};
  var pairMap = graphV908CollectPersistentCompoundPairs(this, []);
  var count = 0;
  for (var pk in pairMap) {
    if (!pairMap.hasOwnProperty(pk)) continue;
    var a = pairMap[pk].a, b = pairMap[pk].b;
    var recA = this.findCharacterRecord ? this.findCharacterRecord(a) : null;
    var recB = this.findCharacterRecord ? this.findCharacterRecord(b) : null;
    if (!recA || !recB) continue;
    var st = graphGetPairStats(this.aliasCooccurStats, a, b);
    var pe = graphGetEdgeSnapshot(this.aliasPositiveGraph, a, b);
    var ne = graphGetEdgeSnapshot(this.aliasNegativeGraph, a, b);
    if (recA !== recB && pe) {
      var psig = graphV908CompoundReconcileSignature(pe, "positive", pk);
      if (!graphV908ReconcileSignatureSeen(st, psig)) {
        var cs = graphV87CertifiedCompoundSame(pe, st);
        if (cs.certified) {
          graphRemoteLog("persistent_compound_record_reconcile", { a: a, b: b, relation: "same_person", decision: "merge_allowed", reason: cs.reason, sourceFamilies: cs.families || [], signature: psig });
          graphV908MarkReconcileSignature(st, psig);
          if (this.v87ReconcileExistingRecords(a, b, "same_person", cs.reason || "persistent_compound_same_person", "persistent_compound_record_reconcile", chapterText || "", true)) count++;
        } else if (pe && graphV87ReasonsHasAny(pe.reasons || [], graphV908PositiveCompoundRecordReasons())) {
          graphRemoteLog("compound_evidence_to_conflict_verify", { a: a, b: b, relation: "same_person", reason: cs.reason, sourceFamilies: cs.families, action: "graph_conflict_verify", sourceStage: "persistent_compound_record_reconcile" });
          graphV908MarkReconcileSignature(st, psig);
          if (this.verifyGraphConflictAndFix) this.verifyGraphConflictAndFix("positive", a, b, 4.5, "persistent_compound_uncertified_same_person", pe.extra || cs.reason || "", "persistent_compound_record_reconcile", { defaultAllow: false, forceVerify: true, contextText: chapterText || this.contextHistory2 || "" });
        }
      }
    }
    if (recA === recB && ne) {
      var nsig = graphV908CompoundReconcileSignature(ne, "negative", pk);
      if (!graphV908ReconcileSignatureSeen(st, nsig)) {
        var cd = graphV87CertifiedCompoundDifferent(ne, st);
        if (cd.certified) {
          graphRemoteLog("persistent_compound_record_reconcile", { a: a, b: b, relation: "different_person", decision: "split_allowed", reason: cd.reason, sourceFamilies: cd.families || [], signature: nsig });
          graphV908MarkReconcileSignature(st, nsig);
          if (this.v87ReconcileExistingRecords(a, b, "different_person", cd.reason || "persistent_compound_different_person", "persistent_compound_record_reconcile", chapterText || "", true)) count++;
        } else if (ne && graphV87ReasonsHasAny(ne.reasons || [], ["compound_explicit_different_negative","compound_speaker_interaction_negative","compound_relationship_interaction_negative"])) {
          graphRemoteLog("compound_evidence_to_conflict_verify", { a: a, b: b, relation: "different_person", reason: cd.reason, sourceFamilies: cd.families, action: "graph_conflict_verify", sourceStage: "persistent_compound_record_reconcile" });
          graphV908MarkReconcileSignature(st, nsig);
          if (this.verifyGraphConflictAndFix) this.verifyGraphConflictAndFix("negative", a, b, 4.5, "persistent_compound_uncertified_different_person", ne.extra || cd.reason || "", "persistent_compound_record_reconcile", { defaultAllow: false, forceVerify: true, contextText: chapterText || this.contextHistory2 || "" });
        }
      }
    }
  }
  return count;
};

CharacterManager.prototype.applyCompoundGraphEvidence = function(names, chapterText) {
  if (!ENABLE_COMPOUND_GRAPH_EVIDENCE || !ENABLE_ALIAS_GRAPH) return { positive: 0, negative: 0, skipped: 0 };
  if (!this.aliasCooccurStats) this.aliasCooccurStats = {};
  names = names || [];
  var pairMap = {};
  function addPair(a, b) {
    a = graphNormalizeName(a); b = graphNormalizeName(b);
    if (graphIsInvalidName(a) || graphIsInvalidName(b) || a === b || graphIsGroupName(a) || graphIsGroupName(b)) return;
    pairMap[graphPairKey(a,b)] = { a: a, b: b };
  }
  for (var i = 0; i < names.length; i++) for (var j = i + 1; j < names.length; j++) addPair(names[i], names[j]);
  var curChapter = graphCurrentChapterId();
  for (var k in this.aliasCooccurStats) {
    if (!this.aliasCooccurStats.hasOwnProperty(k) || k.indexOf("__") === 0) continue;
    var st0 = this.aliasCooccurStats[k];
    if (st0 && st0.a && st0.b && graphArrayIntersectsChapters(st0.chapters, [curChapter])) addPair(st0.a, st0.b);
  }
  var keys = Object.keys(pairMap);
  if (keys.length > 160) keys = keys.slice(0, 160);
  var pos = 0, neg = 0, skipped = 0;
  var currentCompoundFilterStats = { filteredCount: 0, filteredReasons: [] };
  for (var pi = 0; pi < keys.length; pi++) {
    var item = pairMap[keys[pi]];
    var a = item.a, b = item.b;
    var recA = this.findCharacterRecord ? this.findCharacterRecord(a) : null;
    var recB = this.findCharacterRecord ? this.findCharacterRecord(b) : null;
    if (recA && recB && recA === recB) { skipped++; graphRemoteLog("compound_graph_skipped", { a: a, b: b, reason: "sameMainName" }); continue; }
    var pe = graphGetEdgeSnapshot(this.aliasPositiveGraph, a, b);
    var ne = graphGetEdgeSnapshot(this.aliasNegativeGraph, a, b);
    var st = this.aliasCooccurStats ? this.aliasCooccurStats[graphPairKey(a,b)] : null;
    var posReasons = graphV908FilterCompoundSourceReasonsBeforeScan(pe ? pe.reasons || [] : [], currentCompoundFilterStats);
    var negReasons = graphV908FilterCompoundSourceReasonsBeforeScan(ne ? ne.reasons || [] : [], currentCompoundFilterStats);
    if (st) {
      if (Number(st.sameSentence || 0) > 0) negReasons.push("same_sentence_cooccur");
      if (Number(st.adjacentSpeaker || 0) > 0) negReasons.push("adjacent_speaker_cooccur");
      if (Number(st.modelPositive || 0) > 0 && !graphReasonListHas(posReasons, "model_name_identity_positive")) posReasons.push("model_name_identity_positive");
      if (Number(st.modelNegative || 0) > 0 && !graphReasonListHas(negReasons, "model_dialogue_relation_negative")) negReasons.push("model_dialogue_relation_negative");
    }
    posReasons = graphV908FilterCompoundSourceReasonsBeforeScan(posReasons, currentCompoundFilterStats);
    negReasons = graphV908FilterCompoundSourceReasonsBeforeScan(negReasons, currentCompoundFilterStats);
    var allReasons = posReasons.concat(negReasons);
    var hasSemantic = graphAnyReason(allReasons, ["model_name_identity_positive","model_dialogue_relation_negative","model_action_relation_negative","model_social_relation_negative","model_co_presence_negative","graph_conflict_verified_same_person","graph_conflict_verified_different_person","alias_refine_confirmed"]);
    if (!hasSemantic) { skipped++; if (allReasons.length > 1) graphRemoteLog("compound_graph_skipped", { a: a, b: b, reason: "onlyStatisticalReasons", sourceReasons: allReasons }); continue; }
    var evidence = graphCompoundEvidenceText(pe, ne, st);
    var wrote = false;
    function writeNeg(reason, score, label, src) {
      if (wrote) return;
      if (st && !graphCompoundChapterMarkOnce(st, reason, curChapter)) { skipped++; graphRemoteLog("compound_graph_skipped", { a: a, b: b, reason: "chapterDedup", compoundReason: reason, chapter: curChapter }); wrote = true; return; }
      var extra = "复合:" + src.join("+") + (evidence ? "；证据:" + evidence : "");
      if (st) { st.compoundNegative = Number(st.compoundNegative || 0) + 1; graphPushCooccurEvidence(this.aliasCooccurStats, a, b, label, extra); }
      this.recordNegativeAliasEdge(a, b, score, reason, extra);
      graphRemoteLog("compound_graph_edge", { a: a, b: b, direction: "negative", reason: reason, score: score, sourceReasons: src, evidence: graphSafeString(evidence, 500), chapter: curChapter });
      neg++; wrote = true;
    }
    function writePos(reason, score, label, src) {
      if (wrote) return;
      if (st && !graphCompoundChapterMarkOnce(st, reason, curChapter)) { skipped++; graphRemoteLog("compound_graph_skipped", { a: a, b: b, reason: "chapterDedup", compoundReason: reason, chapter: curChapter }); wrote = true; return; }
      var extra = "复合:" + src.join("+") + (evidence ? "；证据:" + evidence : "");
      if (st) { st.compoundPositive = Number(st.compoundPositive || 0) + 1; graphPushCooccurEvidence(this.aliasCooccurStats, a, b, label, extra); }
      this.recordPositiveAliasEdge(a, b, score, reason, extra);
      graphRemoteLog("compound_graph_edge", { a: a, b: b, direction: "positive", reason: reason, score: score, sourceReasons: src, evidence: graphSafeString(evidence, 500), chapter: curChapter });
      pos++; wrote = true;
    }
    // 复合链路只使用模型审计、别名确认、冲突确认等原子证据；本地封闭式 本地封闭式 已删除。
    if (graphReasonListHas(negReasons, "model_explicit_different_negative") && graphAnyReason(negReasons, ["model_dialogue_relation_negative","model_action_relation_negative","model_social_relation_negative","model_co_presence_negative","graph_conflict_verified_different_person"])) writeNeg.call(this, "compound_explicit_different_negative", GRAPH_COMPOUND_EXPLICIT_DIFFERENT_SCORE, "复合明确非同人反证", negReasons);
    if (!wrote && graphAnyReason(negReasons, ["model_dialogue_relation_negative","model_action_relation_negative"]) && graphAnyReason(negReasons, ["model_social_relation_negative","model_co_presence_negative","model_explicit_different_negative","graph_conflict_verified_different_person"])) writeNeg.call(this, "compound_speaker_interaction_negative", GRAPH_COMPOUND_SPEAKER_INTERACTION_SCORE, "复合模型互动反证", negReasons);
    if (!wrote && graphReasonListHas(negReasons, "model_social_relation_negative") && graphAnyReason(negReasons, ["model_dialogue_relation_negative","model_action_relation_negative","model_explicit_different_negative","graph_conflict_verified_different_person"])) writeNeg.call(this, "compound_relationship_interaction_negative", GRAPH_COMPOUND_RELATIONSHIP_INTERACTION_SCORE, "复合模型关系反证", negReasons);
    if (!wrote && graphReasonListHas(posReasons, "model_name_identity_positive") && graphAnyReason(posReasons, ["alias_refine_confirmed","graph_conflict_verified_same_person"])) writePos.call(this, "compound_name_alias_positive", GRAPH_COMPOUND_NAME_ALIAS_SCORE, "复合模型同人正证", posReasons);
    if (!wrote && graphReasonListHas(posReasons, "graph_conflict_verified_same_person") && graphAnyReason(posReasons, ["model_name_identity_positive","alias_refine_confirmed"])) writePos.call(this, "compound_verified_same_person_positive", GRAPH_COMPOUND_VERIFIED_SAME_SCORE, "复合冲突确认同人", posReasons);
  }
  if (currentCompoundFilterStats.filteredCount) graphRemoteLog("compound_source_reason_prefiltered", { scope: "current_chapter_compound", filteredSourceReasonCount: currentCompoundFilterStats.filteredCount || 0, filteredSourceReasons: currentCompoundFilterStats.filteredReasons || [], chapter: curChapter });
  if (pos || neg) { this.saveAliasGraphData(); this.saveAliasCooccurStats(); }
  return { positive: pos, negative: neg, skipped: skipped, filteredSourceReasonCount: currentCompoundFilterStats.filteredCount || 0 };
};

CharacterManager.prototype.updateAliasGraphsFromCache = function(dialogList, chapterFullContent, modelRelations) {
  if (!ENABLE_ALIAS_GRAPH || !ENABLE_ALIAS_COOCUR_STATS) return;
  dialogList = dialogList || [];
  if (!dialogList.length) return;
  var text = graphSafeString(chapterFullContent || "", 120000);
  this.graphConflictVerifyChapterText = text;
  graphRemoteSetChapter(graphBuildChapterKey(text), "共现扫描");
  try { graphAliasRecentChapterAppend(graphCurrentChapterId()); graphAliasRecentChapterSave(); } catch(aliasRecentErr0) {}
  var scanKey = dialogList.length + ":" + graphHash(text.substring(0, 3500) + "#" + JSON.stringify(dialogList).substring(0, 1500));
  if (this.lastAliasGraphScanKey === scanKey) return;
  if (!this.aliasCooccurStats) this.aliasCooccurStats = {};
  if (!this.aliasCooccurStats.__scans) this.aliasCooccurStats.__scans = {};
  if (this.aliasCooccurStats.__scans[scanKey]) {
    this.lastAliasGraphScanKey = scanKey;
    return;
  }
  this.lastAliasGraphScanKey = scanKey;
  this.aliasCooccurStats.__scans[scanKey] = graphNowIso();
  graphPruneScans(this.aliasCooccurStats.__scans);

  graphRemoteLog("cooccur_scan_start", { dialogCount: dialogList.length, textLen: text.length });

  var names = [];
  var seen = {};
  for (var i = 0; i < dialogList.length; i++) {
    var n = graphNormalizeName(dialogList[i] && dialogList[i].name);
    if (graphIsInvalidName(n)) continue;
    if (!seen[n]) {
      seen[n] = true;
      names.push(n);
    }
  }
  if (names.length > COOCUR_MAX_NAMES) names = names.slice(0, COOCUR_MAX_NAMES);
  if (this.markCharacterRecordChapterByName) {
    for (var mi = 0; mi < names.length; mi++) this.markCharacterRecordChapterByName(names[mi]);
    try { this.saveRecords(); } catch(saveMarkErr) {}
  }

  var posEdges = 0;
  var negEdges = 0;
  var coHits = 0;
  var remoteEdgeCount = 0;
  var scanEdgeSeen = {};

  function addScanEdgeOnce(a, b, reason) {
    var key = graphPairKey(a, b) + "|" + graphSafeString(reason || "", 80);
    if (scanEdgeSeen[key]) return false;
    scanEdgeSeen[key] = true;
    return true;
  }

  function remoteGraphEdgeLog(eventType, data) {
    if (remoteEdgeCount >= GRAPH_REMOTE_EDGE_LIMIT) return;
    graphRemoteLog(eventType, data);
    remoteEdgeCount++;
  }

  function addPos(a, b, score, reason, extra) {
    if (!addScanEdgeOnce(a, b, reason)) return;
    if (graphGateShouldApplyToPositiveReason(reason) && this.directPairEvidenceGate) {
      var localPosGate = this.directPairEvidenceGate(a, b, extra || reason || "", text || "", "cooccur_positive_edge");
      if (!localPosGate.allow) {
        if (localPosGate.needVerify && this.verifyGraphConflictAndFix) {
          graphRemoteLog("graph_positive_gate_to_conflict_verify", { a: graphNormalizeName(a), b: graphNormalizeName(b), reason: graphSafeString(extra || reason || "", 220), gateReason: graphSafeString(localPosGate.reason || "", 180), sourceReason: reason, stage: "cooccur_positive_edge", tier: localPosGate.tier || "B" });
          var localPosDecision = this.verifyGraphConflictAndFix("positive", a, b, score, reason, extra || reason || "", "graph_positive_gate_to_conflict_verify", { defaultAllow: false, forceVerify: true, contextText: text, originalSourceReason: reason, originalEvidenceText: extra || reason || "" });
          if (!localPosDecision.allow) return;
        } else {
          graphRemoteLog("graph_positive_bridge_gate_blocked", { a: graphNormalizeName(a), b: graphNormalizeName(b), reason: graphSafeString(extra || reason || "", 220), gateReason: graphSafeString(localPosGate.reason || "", 180), sourceReason: reason, stage: "cooccur_positive_edge", tier: localPosGate.tier || "C" });
          return;
        }
      }
    }
    var conflictDecision = this.verifyGraphConflictAndFix ? this.verifyGraphConflictAndFix("positive", a, b, score, reason, extra, "cooccur_positive_edge", { defaultAllow: true, contextText: text }) : { allow: true };
    if (!conflictDecision.allow) return;
    if (!graphMarkChapterEvidenceOnce(this.aliasCooccurStats, a, b, reason, "", extra || reason || "")) return;
    if (graphAddWeightedEdge(this.aliasPositiveGraph, a, b, score, reason, extra, "", { chapterId: graphCurrentChapterId(), evidenceText: extra || reason || "" })) {
      posEdges++;
      posEdges += this.applyPositiveChainClosure ? this.applyPositiveChainClosure(a, b, reason) : 0;
      remoteGraphEdgeLog("graph_positive_edge", { a: a, b: b, score: score, reason: reason, extra: graphSafeString(extra, 160) });
    }
  }

  function addNeg(a, b, score, reason, extra) {
    if (!addScanEdgeOnce(a, b, reason)) return;
    var conflictDecision = this.verifyGraphConflictAndFix ? this.verifyGraphConflictAndFix("negative", a, b, score, reason, extra, "cooccur_negative_edge", { defaultAllow: true, contextText: text }) : { allow: true };
    if (!conflictDecision.allow) return;
    if (!graphMarkChapterEvidenceOnce(this.aliasCooccurStats, a, b, reason, "", extra || reason || "")) return;
    if (graphAddWeightedEdge(this.aliasNegativeGraph, a, b, score, reason, extra, "", { chapterId: graphCurrentChapterId(), evidenceText: extra || reason || "" })) {
      negEdges++;
      remoteGraphEdgeLog("graph_negative_edge", { a: a, b: b, score: score, reason: reason, extra: graphSafeString(extra, 160) });
    }
  }

  for (var j = 1; j < dialogList.length; j++) {
    var prev = graphNormalizeName(dialogList[j - 1] && dialogList[j - 1].name);
    var curr = graphNormalizeName(dialogList[j] && dialogList[j].name);
    if (graphIsInvalidName(prev) || graphIsInvalidName(curr) || prev === curr) continue;
    var stAdj = graphGetPairStats(this.aliasCooccurStats, prev, curr);
    if (stAdj) {
      stAdj.adjacentSpeaker = Number(stAdj.adjacentSpeaker || 0) + 1;
      stAdj.updatedAt = graphNowIso();
      graphCooccurMarkChapter(this.aliasCooccurStats, prev, curr);
      graphPushCooccurEvidence(this.aliasCooccurStats, prev, curr, "相邻说话", graphBuildAdjacentDialogEvidence(dialogList[j - 1], dialogList[j]));
      if (stAdj.adjacentSpeaker >= COOCUR_NEG_ADJACENT_MIN) {
        addNeg.call(this, prev, curr, 0.7, "adjacent_speaker_cooccur", "相邻说话" + stAdj.adjacentSpeaker + "次");
      }
    }
  }

  var sentences = text.split(/[。！？!?；;\n]+/);
  if (sentences.length > COOCUR_MAX_SENTENCES) sentences = sentences.slice(0, COOCUR_MAX_SENTENCES);

  for (var x = 0; x < names.length; x++) {
    for (var y = x + 1; y < names.length; y++) {
      var A = names[x];
      var B = names[y];
      var ea = graphEscapeRegExp(A);
      var eb = graphEscapeRegExp(B);
      var st = graphGetPairStats(this.aliasCooccurStats, A, B);
      if (!st) continue;
      st.chapterCount = Number(st.chapterCount || 0) + 1;
      st.updatedAt = graphNowIso();
      graphCooccurMarkChapter(this.aliasCooccurStats, A, B);

      // 本地封闭式同人/非同人结构已完全迁移到批量姓名分析；这里不再产生 本地封闭式 正/反边。
      var sameSentence = 0;
      var sameSentenceExample = "";
      for (var s = 0; s < sentences.length; s++) {
        var sen = sentences[s];
        if (sen && sen.indexOf(A) !== -1 && sen.indexOf(B) !== -1) {
          sameSentence++;
          if (!sameSentenceExample) sameSentenceExample = sen;
          if (sameSentence >= 6) break;
        }
      }
      if (sameSentence > 0) {
        st.sameSentence = Number(st.sameSentence || 0) + sameSentence;
        graphPushCooccurEvidence(this.aliasCooccurStats, A, B, "同句共现", sameSentenceExample || ("同句共现" + sameSentence + "次"));
        coHits++;
        if (sameSentence >= COOCUR_NEG_SENTENCE_MIN) {
          addNeg.call(this, A, B, Math.min(2.5, sameSentence * 0.45), "same_sentence_cooccur", "同句共现" + sameSentence + "次");
        }
      }
    }
  }

  var modelSummary = this.applyModelRelationEvidence ? this.applyModelRelationEvidence(modelRelations || [], text, names) : { positive: 0, negative: 0 };
  var compoundSummary = this.applyCompoundGraphEvidence ? this.applyCompoundGraphEvidence(names, text) : { positive: 0, negative: 0, skipped: 0 };
  this.saveAliasGraphData();
  this.saveAliasCooccurStats();
  graphShortLog("共现" + coHits + " 正" + (posEdges + (modelSummary.positive || 0) + (compoundSummary.positive || 0)) + " 反" + (negEdges + (modelSummary.negative || 0) + (compoundSummary.negative || 0)));
  graphRemoteLog("cooccur_scan_done", { names: names.length, cooccurHits: coHits, positiveEdges: posEdges + (modelSummary.positive || 0) + (compoundSummary.positive || 0), negativeEdges: negEdges + (modelSummary.negative || 0) + (compoundSummary.negative || 0), compoundPositive: compoundSummary.positive || 0, compoundNegative: compoundSummary.negative || 0, compoundSkipped: compoundSummary.skipped || 0, modelRelations: modelRelations ? modelRelations.length : 0, dialogCount: dialogList.length });
};



CharacterManager.prototype.markRecordActiveChapter = function(record) {
  if (!record) return false;
  var beforeCount = record.chapters && Array.isArray(record.chapters) ? record.chapters.length : 0;
  var ok = graphPushChapterMark(record);
  var afterCount = record.chapters && Array.isArray(record.chapters) ? record.chapters.length : 0;
  graphRemoteLog("character_chapter_mark_applied", { name: graphNormalizeName(record.name || ""), chapterIndex: graphCurrentChapterId(), beforeCount: beforeCount, afterCount: afterCount, applied: !!ok });
  return ok;
};

CharacterManager.prototype.markCharacterRecordChapterByName = function(name) {
  try {
    if (!name || !this.findCharacterRecord) return false;
    var rec = this.findCharacterRecord(name);
    if (!rec) return false;
    return this.markRecordActiveChapter ? this.markRecordActiveChapter(rec) : graphPushChapterMark(rec);
  } catch(e) { return false; }
};

function graphAliasBuildCandidateMap(candidateList, newName) {
  var map = {};
  var mainMap = (typeof characterManager !== "undefined" && characterManager && characterManager.nameToMainNameMap) ? characterManager.nameToMainNameMap : null;
  function add(n) {
    n = graphNormalizeName(n);
    if (!n) return;
    map[n] = true;
    if (mainMap && mainMap[n]) map[graphNormalizeName(mainMap[n])] = true;
  }
  add(newName);
  candidateList = candidateList || [];
  for (var i = 0; i < candidateList.length; i++) {
    if (candidateList[i] && candidateList[i].name) add(candidateList[i].name);
  }
  return map;
}

function graphAliasPairFocused(a, b, newName, candidateMap) {
  a = graphNormalizeName(a);
  b = graphNormalizeName(b);
  newName = graphNormalizeName(newName);
  if (!a || !b) return false;
  if (newName && (a === newName || b === newName)) return true;
  return !!(candidateMap && candidateMap[a] && candidateMap[b]);
}

CharacterManager.prototype.getAliasRecentRoleList = function(recentChapters, targetGender, candidateList) {
  var roles = [];
  if (!recentChapters || recentChapters.length === 0) return roles;
  var records = this.characterRecords || [];
  try {
    var fileContent = ttsrv.readTxtFile("characterRecords.json");
    var parsed = JSON.parse(fileContent || "[]") || [];
    if (parsed && parsed.length) records = parsed;
  } catch(e) {}

  var candidateMap = graphAliasBuildCandidateMap(candidateList || [], "");
  for (var i = 0; i < records.length; i++) {
    var rec = records[i];
    if (!rec || !rec.name) continue;
    if (targetGender && rec.gender && String(rec.gender).trim() !== String(targetGender).trim()) continue;
    if (!graphArrayIntersectsChapters(rec.chapters, recentChapters)) continue;
    var aliases = [];
    if (rec.aliases && rec.aliases.trim()) {
      var arr = rec.aliases.split("|");
      for (var a = 0; a < arr.length; a++) {
        var al = arr[a].trim();
        if (al && al !== rec.name && aliases.indexOf(al) === -1) aliases.push(al);
      }
    }
    var hitChapters = graphFilteredRecentChapters(rec.chapters, recentChapters);
    roles.push({
      name: graphSafeString(rec.name, 60),
      aliases: aliases.slice(0, 8),
      gender: graphSafeString(rec.gender || "", 12),
      age: graphSafeString(rec.age || "", 12),
      voice: graphSafeString(rec.voice || "", 30),
      chapters: hitChapters,
      recentHitCount: hitChapters.length,
      inCandidateList: !!candidateMap[graphNormalizeName(rec.name)]
    });
  }
  roles.sort(function(a, b) {
    var ac = a.chapters && a.chapters.length ? Number(a.chapters[a.chapters.length - 1]) : 0;
    var bc = b.chapters && b.chapters.length ? Number(b.chapters[b.chapters.length - 1]) : 0;
    if (isNaN(ac)) ac = 0;
    if (isNaN(bc)) bc = 0;
    if (bc !== ac) return bc - ac;
    return Number(b.recentHitCount || 0) - Number(a.recentHitCount || 0);
  });
  var limit = parseInt(ALIAS_RECENT_ROLE_LIMIT, 10) || 40;
  return roles.slice(0, limit);
};


function graphNameReuseRecordName(rec) {
  return graphNormalizeName((rec && (rec.name || rec.mainName)) || "");
}

function graphNameReuseExtractAliases(rec, limit) {
  var aliases = [];
  limit = parseInt(limit, 10) || 8;
  if (!rec) return aliases;
  var mainName = graphNameReuseRecordName(rec);
  function pushOne(v) {
    v = graphNormalizeName(v || "");
    if (!v || v === mainName) return;
    if (aliases.indexOf(v) === -1) aliases.push(v);
  }
  if (Array.isArray(rec.aliases)) {
    for (var i = 0; i < rec.aliases.length; i++) {
      pushOne(rec.aliases[i]);
      if (aliases.length >= limit) break;
    }
  } else if (rec.aliases && String(rec.aliases).trim()) {
    var arr = String(rec.aliases).split(/[|,，、\/]+/);
    for (var a = 0; a < arr.length; a++) {
      pushOne(arr[a]);
      if (aliases.length >= limit) break;
    }
  }
  if (aliases.length < limit && rec.aliasesText) {
    var arr2 = String(rec.aliasesText).split(/[|,，、\/]+/);
    for (var b = 0; b < arr2.length; b++) {
      pushOne(arr2[b]);
      if (aliases.length >= limit) break;
    }
  }
  return aliases.slice(0, limit);
}

function graphNameReuseIsWeakCrossAlias(name) {
  name = graphNormalizeName(name || "");
  if (!name) return true;
  if (/^(旁白|系统|作者|叙述|未知|不明|无名|某人|某某|某|路人|甲|乙|丙|丁)$/.test(name)) return true;
  if (/^(我|吾|俺|本座|本尊|老夫|老身|妾身|在下|贫道|贫僧|小女子|弟子|属下|晚辈|前辈)$/.test(name)) return true;
  if (/^(他|她|它|其|此人|此女|此子|此老|此獠|此僧|此妖|此魔|此鬼|那人|那名|那位|那厮|那老者|那男子|那女子|对方|前者|后者|二者|此二人|这人|这位|那家伙|这家伙)$/.test(name)) return true;
  if (/^(众人|众修|众修士|众弟子|诸人|诸修|诸位|大家|一众人|一干人等|一行人|一群人|一伙人|二人|两人|三人|四人|五人|几人|数人|多人|众女|众男|众老者|众长老)$/.test(name)) return true;
  if (/^[一二两三四五六七八九十数几0-9]+(名|个|位|群|队|伙).{0,8}(修士|弟子|男女|男子|女子|青年|老者|大汉|汉子|人)$/.test(name)) return true;
  if (/^[一二两三四五六七八九十数几0-9]+(男|女|老|童|修|道|僧|妖|魔|鬼|男修|女修|修士|弟子|男子|女子|青年|老者|大汉|汉子)$/.test(name)) return true;
  if (/^(老者|老头|老翁|老叟|老妪|老妇|老妇人|老怪|老魔|老鬼|老道|道人|道士|和尚|僧人|儒生|书生|修士|男修|女修|弟子|门人|侍女|侍从|仆人|少女|少年|青年|男子|女子|大汉|汉子|壮汉|美妇|妇人|中年人|年轻人|小童|童子|孩童|小孩|丫鬟|侍卫|护卫|随从)$/.test(name)) return true;
  if (/^(前辈|道友|兄台|道兄|师兄|师姐|师妹|师弟|师叔|师伯|师父|师傅|师尊|主人|主上|少主|公子|小姐|夫人|娘子|长老|护法|门主|宫主|宗主|岛主|阁主|掌柜|掌门|族长|圣祖|老祖|真人|仙师|上人|大人)$/.test(name)) return true;
  if (typeof graphV908IsFirstPersonOrHonorificTitleName === "function" && graphV908IsFirstPersonOrHonorificTitleName(name)) return true;
  return false;
}

function graphNameReuseIsGenericCrossMainName(name) {
  name = graphNormalizeName(name || "");
  if (!name) return true;
  if (graphNameReuseIsWeakCrossAlias(name)) return true;
  if (/^(两|二|三|四|五|六|七|八|九|十|数|几)(人组|女组|男组|老者组|修士组|弟子组)$/.test(name)) return true;
  if (/^.{1,12}(弟子|门人|族人|护卫|侍卫|随从|侍从|仆人|侍女|丫鬟|修士|男修|女修|男子|女子|少年|少女|老者|青年|大汉|汉子)$/.test(name)) return true;
  if (/(身边|旁边|身旁|为首|带头|领头)的?.{1,8}$/.test(name)) return true;
  return false;
}

function graphNameReuseFindRecordMatchInText(rec, text, forCrossChapter) {
  text = String(text || "");
  if (!rec || !text) return null;
  var candidates = [];
  var mainName = graphNameReuseRecordName(rec);
  if (mainName) candidates.push({ alias: mainName, type: "mainName" });
  var aliases = graphNameReuseExtractAliases(rec, 20);
  for (var i = 0; i < aliases.length; i++) {
    var al = graphNormalizeName(aliases[i]);
    if (al && al !== mainName) candidates.push({ alias: al, type: "alias" });
  }
  candidates.sort(function(a, b) { return String(b.alias || "").length - String(a.alias || "").length; });
  for (var c = 0; c < candidates.length; c++) {
    var name = candidates[c].alias;
    if (!name) continue;
    if (forCrossChapter && NAME_ANALYSIS_CROSS_CHAPTER_WEAK_ALIAS_FILTER && graphNameReuseIsWeakCrossAlias(name)) continue;
    var pos = text.indexOf(name);
    if (pos >= 0) return { alias: name, type: candidates[c].type, pos: pos };
  }
  return null;
}

function graphNameReuseRecordChapters(rec) {
  return graphTrimChapterArray((rec && rec.chapters) || []);
}

CharacterManager.prototype.getNameAnalysisRecentRoleList = function(recentChapters, fullBatchContent) {
  var roles = [];
  if (!ENABLE_NAME_ANALYSIS_RECENT_ROLE_HINT || !recentChapters || recentChapters.length === 0) return roles;
  fullBatchContent = String(fullBatchContent || "");
  var records = this.characterRecords || [];
  try {
    var fileContent = ttsrv.readTxtFile("characterRecords.json");
    var parsed = JSON.parse(fileContent || "[]") || [];
    if (parsed && parsed.length) records = parsed;
  } catch(e) {}
  for (var i = 0; i < records.length; i++) {
    var rec = records[i];
    var recName = graphNameReuseRecordName(rec);
    if (!rec || !recName) continue;
    var recChapters = graphNameReuseRecordChapters(rec);
    if (!graphArrayIntersectsChapters(recChapters, recentChapters)) continue;
    var aliasLimit = parseInt(NAME_ANALYSIS_RECENT_ALIAS_LIMIT, 10) || 8;
    var aliases = graphNameReuseExtractAliases(rec, aliasLimit);
    var hitChapters = graphFilteredRecentChapters(recChapters, recentChapters);
    var match = graphNameReuseFindRecordMatchInText(rec, fullBatchContent, false);
    roles.push({ name: graphSafeString(recName, 60), aliases: aliases, gender: graphSafeString(rec.gender || "", 12), age: graphSafeString(rec.age || "", 12), chapters: hitChapters, recentHitCount: hitChapters.length, batchTextHit: !!match, matchedAlias: match ? match.alias : "", matchType: match ? match.type : "", matchPos: match ? match.pos : 999999 });
  }
  roles.sort(function(a, b) {
    var ah = a.batchTextHit ? 1 : 0;
    var bh = b.batchTextHit ? 1 : 0;
    if (bh !== ah) return bh - ah;
    if (ah && bh && a.matchPos !== b.matchPos) return a.matchPos - b.matchPos;
    var ac = a.chapters && a.chapters.length ? Number(a.chapters[a.chapters.length - 1]) : 0;
    var bc = b.chapters && b.chapters.length ? Number(b.chapters[b.chapters.length - 1]) : 0;
    if (isNaN(ac)) ac = 0;
    if (isNaN(bc)) bc = 0;
    if (bc !== ac) return bc - ac;
    return Number(b.recentHitCount || 0) - Number(a.recentHitCount || 0);
  });
  var limit = parseInt(NAME_ANALYSIS_RECENT_ROLE_LIMIT, 10) || 80;
  return roles.slice(0, limit);
};

CharacterManager.prototype.getNameAnalysisCrossChapterRoleList = function(recentChapters, fullBatchContent) {
  var roles = [];
  var stats = { recentRangeSkipped: 0, mergedSkipped: 0, emptyChapterSkipped: 0, weakMainNameSkipped: 0, weakMainNameSamples: [], weakAliasSkipped: 0, noTextMatchSkipped: 0, duplicateSkipped: 0, scanned: 0 };
  this._nameAnalysisCrossChapterReuseStats = stats;
  if (!ENABLE_NAME_ANALYSIS_CROSS_CHAPTER_ROLE_HIT || !recentChapters || recentChapters.length === 0) return roles;
  fullBatchContent = String(fullBatchContent || "");
  if (!fullBatchContent) return roles;
  var records = this.characterRecords || [];
  try {
    var fileContent = ttsrv.readTxtFile("characterRecords.json");
    var parsed = JSON.parse(fileContent || "[]") || [];
    if (parsed && parsed.length) records = parsed;
  } catch(e) {}
  var seen = {};
  for (var i = 0; i < records.length; i++) {
    var rec = records[i];
    stats.scanned++;
    var recName = graphNameReuseRecordName(rec);
    if (!rec || !recName) continue;
    if (ENABLE_NAME_ANALYSIS_CROSS_CHAPTER_GENERIC_MAIN_FILTER && graphNameReuseIsGenericCrossMainName(recName)) {
      stats.weakMainNameSkipped++;
      if (stats.weakMainNameSamples.length < 20) stats.weakMainNameSamples.push(recName);
      continue;
    }
    var chapters = graphNameReuseRecordChapters(rec);
    if (!chapters.length) { stats.emptyChapterSkipped++; continue; }
    if (graphArrayIntersectsChapters(chapters, recentChapters)) { stats.recentRangeSkipped++; continue; }
    if (rec.merged || rec.mergedInto) { stats.mergedSkipped++; continue; }
    var match = graphNameReuseFindRecordMatchInText(rec, fullBatchContent, true);
    if (!match || !match.alias) {
      var weakProbe = graphNameReuseFindRecordMatchInText(rec, fullBatchContent, false);
      if (weakProbe && graphNameReuseIsWeakCrossAlias(weakProbe.alias)) stats.weakAliasSkipped++;
      else stats.noTextMatchSkipped++;
      continue;
    }
    var key = graphSafeString(rec.recordId || rec.id || recName, 120);
    if (seen[key]) { stats.duplicateSkipped++; continue; }
    seen[key] = true;
    var aliasLimit = parseInt(NAME_ANALYSIS_RECENT_ALIAS_LIMIT, 10) || 8;
    var aliases = graphNameReuseExtractAliases(rec, aliasLimit);
    roles.push({ name: graphSafeString(recName, 60), aliases: aliases, gender: graphSafeString(rec.gender || "", 12), age: graphSafeString(rec.age || "", 12), chapters: chapters, recentHitCount: 0, crossChapterHit: true, batchTextHit: true, matchedAlias: match.alias, matchType: match.type, matchPos: match.pos });
  }
  roles.sort(function(a, b) {
    if (a.matchPos !== b.matchPos) return a.matchPos - b.matchPos;
    return String(b.matchedAlias || "").length - String(a.matchedAlias || "").length;
  });
  var crossLimit = parseInt(NAME_ANALYSIS_CROSS_CHAPTER_ROLE_LIMIT, 10) || 5;
  return roles.slice(0, crossLimit);
};

CharacterManager.prototype.buildNameAnalysisRecentRoleHint = function(fullBatchContent) {
  if (!ENABLE_NAME_ANALYSIS_RECENT_ROLE_HINT) return "";
  var rangeVal = parseInt(NAME_ANALYSIS_RECENT_ROLE_RANGE, 10) || 5;
  try { graphAliasRecentChapterAppend(graphCurrentChapterId()); graphAliasRecentChapterSave(); } catch(e0) {}
  var recentChapters = graphAliasGetRecentIndices(rangeVal);
  if (!recentChapters || recentChapters.length === 0) return "";
  var recentRoles = this.getNameAnalysisRecentRoleList ? this.getNameAnalysisRecentRoleList(recentChapters, fullBatchContent) : [];
  var crossRoles = this.getNameAnalysisCrossChapterRoleList ? this.getNameAnalysisCrossChapterRoleList(recentChapters, fullBatchContent) : [];
  var recentHit = [];
  var recentRest = [];
  for (var rr = 0; rr < recentRoles.length; rr++) {
    if (recentRoles[rr] && recentRoles[rr].batchTextHit) recentHit.push(recentRoles[rr]);
    else recentRest.push(recentRoles[rr]);
  }
  var limit = parseInt(NAME_ANALYSIS_RECENT_ROLE_LIMIT, 10) || 80;
  var roleList = recentHit.concat(crossRoles).concat(recentRest).slice(0, limit);
  var lines = [];
  lines.push("以下是最近" + rangeVal + "章/跨章召回的已知角色姓名复用表，只用于统一已经登记过的主名/别名，不是别名推理工具；未登记的新称呼必须原样输出，让后续别名校验处理。");
  lines.push("最近章节索引：" + recentChapters.join("|"));
  if (!roleList.length) {
    lines.push("暂无最近N章/跨章召回的已知角色姓名复用表。");
  } else {
    for (var i = 0; i < roleList.length; i++) {
      var r = roleList[i];
      var aliasText = r.aliases && r.aliases.length ? r.aliases.join(" / ") : "无";
      lines.push((i + 1) + ". 主名：" + r.name + "；别名：" + aliasText + "；性别：" + (r.gender || "未知") + "；章节：" + (r.chapters || []).join("|") + "；备注：角色姓名复用表只用于统一主名/别名，不提供历史年龄段；只有文本说话人称呼命中该主名或上述已登记别名时，才输出主名“" + r.name + "”；若出现未登记新称呼，不要猜成该主名。");
    }
  }
  var hintText = lines.join("\n");
  var crossStats = this._nameAnalysisCrossChapterReuseStats || {};
  graphRemoteLog("name_analysis_reuse_table_final", { range: rangeVal, recentChapters: recentChapters, recentRoleCount: recentRoles.length, recentMatchedCount: recentHit.length, crossHitCount: crossRoles.length, recentUnmatchedCount: recentRest.length, finalRoleCount: roleList.length, hintLen: hintText.length, crossEnabled: !!ENABLE_NAME_ANALYSIS_CROSS_CHAPTER_ROLE_HIT });
  if (crossRoles.length) {
    graphRemoteLog("name_analysis_cross_chapter_role_hit", { count: crossRoles.length, roles: crossRoles.slice(0, 12).map(function(r) { return { name: r.name, matchedAlias: r.matchedAlias || "", matchType: r.matchType || "", chapters: r.chapters || [] }; }) });
  }
  graphRemoteLog("name_analysis_cross_chapter_role_skipped", { recentRangeSkipped: crossStats.recentRangeSkipped || 0, mergedSkipped: crossStats.mergedSkipped || 0, emptyChapterSkipped: crossStats.emptyChapterSkipped || 0, weakMainNameSkipped: crossStats.weakMainNameSkipped || 0, weakMainNameSamples: (crossStats.weakMainNameSamples || []).slice(0, 20), weakAliasSkipped: crossStats.weakAliasSkipped || 0, noTextMatchSkipped: crossStats.noTextMatchSkipped || 0, duplicateSkipped: crossStats.duplicateSkipped || 0, scanned: crossStats.scanned || 0 });
  return hintText;
};

CharacterManager.prototype.getAliasRecentGraphData = function(recentChapters, newName, candidateList) {
  var result = { positiveEdges: [], negativeEdges: [] };
  if (!recentChapters || recentChapters.length === 0) return result;
  var candidateMap = graphAliasBuildCandidateMap(candidateList || [], newName);
  function collect(graph, out, maxLimit, minScore) {
    if (!graph || typeof graph !== "object") return;
    var seen = {};
    for (var a in graph) {
      if (!graph.hasOwnProperty(a) || graphIsInvalidName(a)) continue;
      for (var b in graph[a]) {
        if (!graph[a].hasOwnProperty(b) || graphIsInvalidName(b)) continue;
        var pairKey = graphPairKey(a, b);
        if (seen[pairKey]) continue;
        seen[pairKey] = true;
        var edge = graph[a][b] || {};
        if (!graphArrayIntersectsChapters(edge.chapters, recentChapters)) continue;
        if (!graphAliasPairFocused(a, b, newName, candidateMap)) continue;
        var score = Number(edge.score || 0);
        if (score < minScore) continue;
        out.push({
          a: graphSafeString(a, 60),
          b: graphSafeString(b, 60),
          score: score,
          count: Number(edge.count || 0),
          reasons: (edge.reasons || []).slice(0, parseInt(graphAliasRecentValue("ALIAS_RECENT_GRAPH_REASON_LIMIT", 6), 10) || 6),
          extra: graphSafeString(edge.extra || "", parseInt(graphAliasRecentValue("ALIAS_RECENT_GRAPH_EXTRA_MAX_LEN", 220), 10) || 220),
          lastSeen: graphSafeString(edge.lastSeen || "", 40),
          chapters: graphFilteredRecentChapters(edge.chapters, recentChapters)
        });
      }
    }
    out.sort(function(x, y) { return Number(y.score || 0) - Number(x.score || 0); });
    if (out.length > maxLimit) out.splice(maxLimit, out.length - maxLimit);
  }
  collect(this.aliasPositiveGraph || {}, result.positiveEdges, parseInt(ALIAS_RECENT_GRAPH_POS_LIMIT, 10) || 12, GRAPH_POSITIVE_HINT_MIN || 1);
  collect(this.aliasNegativeGraph || {}, result.negativeEdges, parseInt(ALIAS_RECENT_GRAPH_NEG_LIMIT, 10) || 16, GRAPH_NEGATIVE_SOFT_BLOCK || 1);
  return result;
};

CharacterManager.prototype.getAliasRecentCooccurData = function(recentChapters, newName, candidateList) {
  var pairs = [];
  if (!recentChapters || recentChapters.length === 0) return pairs;
  var stats = this.aliasCooccurStats || {};
  var candidateMap = graphAliasBuildCandidateMap(candidateList || [], newName);
  for (var key in stats) {
    if (!stats.hasOwnProperty(key) || key.indexOf("__") === 0) continue;
    var entry = stats[key];
    if (!entry || !entry.a || !entry.b) continue;
    if (!graphArrayIntersectsChapters(entry.chapters, recentChapters)) continue;
    if (!graphAliasPairFocused(entry.a, entry.b, newName, candidateMap)) continue;
    var weight = Number(entry.directInteraction || 0) * 5 + Number(entry.explicitRelation || 0) * 5 + Number(entry.listedTogether || 0) * 3 + Number(entry.sameSentence || 0) + Number(entry.adjacentSpeaker || 0) + Number(entry.modelPositive || 0) * 3 + Number(entry.modelNegative || 0) * 3 + Number(entry.positiveMention || 0) * 4 + Number(entry.localHighPrecisionNegative || 0) * 4 + Number(entry.mentionObject || 0) * 2 + Number(entry.compoundPositive || 0) * 5 + Number(entry.compoundNegative || 0) * 5;
    if (weight <= 0) continue;
    pairs.push({
      a: graphSafeString(entry.a, 60),
      b: graphSafeString(entry.b, 60),
      chapterCount: Number(entry.chapterCount || 0),
      sameSentence: Number(entry.sameSentence || 0),
      adjacentSpeaker: Number(entry.adjacentSpeaker || 0),
      directInteraction: Number(entry.directInteraction || 0),
      modelPositive: Number(entry.modelPositive || 0),
      modelNegative: Number(entry.modelNegative || 0),
      listedTogether: Number(entry.listedTogether || 0),
      explicitRelation: Number(entry.explicitRelation || 0),
      positiveMention: Number(entry.positiveMention || 0),
      localHighPrecisionNegative: Number(entry.localHighPrecisionNegative || 0),
      mentionObject: Number(entry.mentionObject || 0),
      compoundPositive: Number(entry.compoundPositive || 0),
      compoundNegative: Number(entry.compoundNegative || 0),
      chapters: graphFilteredRecentChapters(entry.chapters, recentChapters),
      evidence: graphFilterRecentEvidence(entry.evidence || [], recentChapters, graphAliasRecentValue("ALIAS_RECENT_COOCUR_EVIDENCE_LIMIT", 4)),
      weight: weight
    });
  }
  pairs.sort(function(a, b) { return Number(b.weight || 0) - Number(a.weight || 0); });
  var limit = parseInt(ALIAS_RECENT_COOCUR_LIMIT, 10) || 18;
  return pairs.slice(0, limit);
};

CharacterManager.prototype.buildAliasRecentChapterHint = function(newName, gender, candidateList) {
  if (!ENABLE_ALIAS_RECENT_CHAPTER_HINT) return "";
  var rangeVal = parseInt(ALIAS_RECENT_CHAPTER_RANGE, 10) || 20;
  try { graphAliasRecentChapterAppend(graphCurrentChapterId()); graphAliasRecentChapterSave(); } catch(e0) {}
  var recentChapters = graphAliasGetRecentIndices(rangeVal);
  if (!recentChapters || recentChapters.length === 0) return "";

  var recentRoleListEnabled = (typeof ENABLE_ALIAS_RECENT_ROLE_LIST === "undefined" || ENABLE_ALIAS_RECENT_ROLE_LIST !== 0);
  var roleList = [];
  if (recentRoleListEnabled) {
    roleList = this.getAliasRecentRoleList ? this.getAliasRecentRoleList(recentChapters, gender, candidateList) : [];
  } else {
    graphRemoteLog("alias_recent_role_list_removed", { newName: graphNormalizeName(newName), range: rangeVal, recentChapters: recentChapters, reason: "当前规则仍不向别名校验prompt输出最近N章角色列表（角色复用表仅用于批量姓名分析）" });
  }
  var graphData = this.getAliasRecentGraphData ? this.getAliasRecentGraphData(recentChapters, newName, candidateList) : { positiveEdges: [], negativeEdges: [] };
  var cooccurData = this.getAliasRecentCooccurData ? this.getAliasRecentCooccurData(recentChapters, newName, candidateList) : [];

  var lines = [];
  lines.push("以下是最近" + rangeVal + "章三维辅助视角，只用于提供正图谱、反图谱、共现统计证据；其中共现次数为命中过最近章节的历史累计值，不等于全部发生在最近N章；证据字段为最近章节命中的样例/原因文本。当前待判定新名字：" + graphNormalizeName(newName));
  lines.push("最近章节索引：" + recentChapters.join("、"));
  lines.push("【最近N章正图谱】");
  if (!graphData.positiveEdges || graphData.positiveEdges.length === 0) {
    lines.push("暂无相关正图谱证据");
  } else {
    for (var p = 0; p < graphData.positiveEdges.length; p++) {
      var pe = graphData.positiveEdges[p];
      lines.push("- " + pe.a + " = " + pe.b + "：分" + Number(pe.score || 0).toFixed(1) + "，次" + Number(pe.count || 0) + "，章" + (pe.chapters || []).join("|") + "，因" + (pe.reasons || []).join("|") + (pe.extra ? "，证据:" + pe.extra : ""));
    }
  }

  lines.push("【最近N章反图谱】");
  if (!graphData.negativeEdges || graphData.negativeEdges.length === 0) {
    lines.push("暂无相关反图谱证据");
  } else {
    for (var n = 0; n < graphData.negativeEdges.length; n++) {
      var ne = graphData.negativeEdges[n];
      lines.push("- " + ne.a + " ≠ " + ne.b + "：分" + Number(ne.score || 0).toFixed(1) + "，次" + Number(ne.count || 0) + "，章" + (ne.chapters || []).join("|") + "，因" + (ne.reasons || []).join("|") + (ne.extra ? "，证据:" + ne.extra : ""));
    }
  }

  var compoundLines = [];
  function collectCompoundLines(arr, sign) {
    arr = arr || [];
    for (var ci = 0; ci < arr.length; ci++) {
      var e = arr[ci] || {};
      var rs = e.reasons || [];
      var hasCompound = false;
      for (var ri = 0; ri < rs.length; ri++) if (String(rs[ri] || "").indexOf("compound_") === 0) hasCompound = true;
      if (hasCompound) compoundLines.push("- " + e.a + " " + sign + " " + e.b + "：分" + Number(e.score || 0).toFixed(1) + "，章" + (e.chapters || []).join("|") + "，因" + rs.join("|") + (e.extra ? "，证据:" + e.extra : ""));
    }
  }
  collectCompoundLines(graphData.positiveEdges, "=");
  collectCompoundLines(graphData.negativeEdges, "≠");
  lines.push("【最近N章复合图谱证据】");
  lines.push(compoundLines.length ? compoundLines.join("\n") : "暂无相关复合图谱证据");
  if (compoundLines.length) graphRemoteLog("alias_recent_compound_hint", { newName: graphNormalizeName(newName), count: compoundLines.length, lines: graphSafeString(compoundLines.join("\n"), 3000) });

  lines.push("【最近N章共现统计】");
  if (!cooccurData || cooccurData.length === 0) {
    lines.push("暂无相关共现统计");
  } else {
    for (var c = 0; c < cooccurData.length; c++) {
      var cc = cooccurData[c];
      var evText = "";
      if (cc.evidence && cc.evidence.length) {
        var evArr = [];
        for (var ei = 0; ei < cc.evidence.length; ei++) {
          var ev = cc.evidence[ei] || {};
          evArr.push("[" + (ev.chapter || "") + "/" + (ev.kind || "") + "]" + (ev.text || ""));
        }
        evText = "，证据:" + evArr.join(" || ");
      }
      lines.push("- " + cc.a + " & " + cc.b + "：同章" + cc.chapterCount + "，同句" + cc.sameSentence + "，相邻" + cc.adjacentSpeaker + "，直接互动" + cc.directInteraction + "，并列" + cc.listedTogether + "，关系" + cc.explicitRelation + "，模型正" + cc.modelPositive + "，模型反" + cc.modelNegative + "，本地正" + (cc.positiveMention || 0) + "，本地反" + (cc.localHighPrecisionNegative || 0) + "，提及" + (cc.mentionObject || 0) + "，复合正" + (cc.compoundPositive || 0) + "，复合反" + (cc.compoundNegative || 0) + "，章" + (cc.chapters || []).join("|") + evText);
    }
  }

  var hint = lines.join("\n");
  graphRemoteLog("alias_recent_chapter_hint", {
    newName: graphNormalizeName(newName),
    range: rangeVal,
    recentChapters: recentChapters,
    roleListEnabled: recentRoleListEnabled,
    roleCount: roleList.length,
    positiveEdgeCount: graphData.positiveEdges ? graphData.positiveEdges.length : 0,
    negativeEdgeCount: graphData.negativeEdges ? graphData.negativeEdges.length : 0,
    cooccurCount: cooccurData.length,
    hint: graphSafeString(hint, 6000)
  });
  return hint;
};

CharacterManager.prototype.buildAliasEvidenceHint = function(newName, chapterFullContent, currentDialogueText, gender, age) {
  if (!ENABLE_ALIAS_GRAPH) return "";
  newName = graphNormalizeName(newName);
  if (!newName) return "";
  var positives = [];
  var negatives = [];

  for (var i = 0; i < this.characterRecords.length; i++) {
    var rec = this.characterRecords[i];
    if (!rec || !rec.name) continue;
    var aliases = graphSplitAliases(rec);
    var maxPos = 0, maxNeg = 0, posReasons = [], negReasons = [], bestStats = null;
    for (var a = 0; a < aliases.length; a++) {
      var al = aliases[a];
      var ps = graphGetEdgeScore(this.aliasPositiveGraph, newName, al);
      var ns = graphGetEdgeScore(this.aliasNegativeGraph, newName, al);
      if (ps > maxPos) { maxPos = ps; posReasons = graphGetEdgeReasons(this.aliasPositiveGraph, newName, al); }
      if (ns > maxNeg) { maxNeg = ns; negReasons = graphGetEdgeReasons(this.aliasNegativeGraph, newName, al); }
      var st = this.aliasCooccurStats ? this.aliasCooccurStats[graphPairKey(newName, al)] : null;
      if (st && (!bestStats || Number(st.sameSentence || 0) + Number(st.adjacentSpeaker || 0) > Number(bestStats.sameSentence || 0) + Number(bestStats.adjacentSpeaker || 0))) bestStats = st;
    }
    if (maxPos >= GRAPH_POSITIVE_HINT_MIN) positives.push({ name: rec.name, score: maxPos, reasons: posReasons });
    var coUseful = bestStats && (Number(bestStats.sameSentence || 0) >= COOCUR_NEG_SENTENCE_MIN || Number(bestStats.adjacentSpeaker || 0) >= COOCUR_NEG_ADJACENT_MIN || Number(bestStats.directInteraction || 0) > 0 || Number(bestStats.listedTogether || 0) > 0 || Number(bestStats.explicitRelation || 0) > 0);
    if (maxNeg >= GRAPH_NEGATIVE_SOFT_BLOCK || coUseful) negatives.push({ name: rec.name, score: maxNeg, reasons: negReasons, stats: bestStats });
  }

  positives.sort(function(a, b) { return Number(b.score || 0) - Number(a.score || 0); });
  negatives.sort(function(a, b) { return Number(b.score || 0) - Number(a.score || 0); });
  if (positives.length === 0 && negatives.length === 0) return "";

  var lines = [];
  lines.push("以下是全局图谱/共现辅助，不能单独决定合并；反向/共现证据优先用于避免误合并，最近N章证据会在下一栏单独给出。");
  if (positives.length > 0) {
    lines.push("【正向候选】");
    for (var p = 0; p < Math.min(5, positives.length); p++) {
      lines.push("- " + newName + " → " + positives[p].name + "：分" + Number(positives[p].score || 0).toFixed(1) + "，因" + (positives[p].reasons || []).join("|"));
    }
  }
  if (negatives.length > 0) {
    lines.push("【反向/共现】");
    for (var n = 0; n < Math.min(8, negatives.length); n++) {
      var ne = negatives[n];
      var st2 = ne.stats || {};
      var level = Number(ne.score || 0) >= GRAPH_NEGATIVE_HARD_BLOCK ? "强排除" : "谨慎";
      lines.push("- " + newName + " ≠ " + ne.name + "：" + level + "，反分" + Number(ne.score || 0).toFixed(1) + "，同句" + Number(st2.sameSentence || 0) + "，相邻" + Number(st2.adjacentSpeaker || 0) + "，因" + (ne.reasons || []).join("|"));
    }
  }
  graphRemoteLog("alias_graph_hint", { newName: newName, graphHint: lines.join("\n"), positiveCount: positives.length, negativeCount: negatives.length });
  return lines.join("\n");
};

CharacterManager.prototype.logAliasCheckFlow = function(newName, result, graphHint, recentHint) {
  var ok = !!(result && result.isAlias && result.mainName);
  aliasShortLog(ok ? ("合并候选 " + newName + "→" + result.mainName) : ("未合并 " + newName));
  graphRemoteLog("alias_check_result", {
    newName: graphNormalizeName(newName),
    isAlias: ok,
    mainName: result && result.mainName ? graphSafeString(result.mainName, 60) : "",
    reason: result && result.reason ? graphSafeString(result.reason, 500) : "",
    mode: "strict",
    hasGraphHint: !!graphHint,
    graphHint: graphSafeString(graphHint, 1800),
    hasRecentHint: !!recentHint,
    recentHint: graphSafeString(recentHint || "", 2200)
  });
};

CharacterManager.prototype.logAliasRefineFlow = function(mainName, newName, result) {
  var ok = !!(result && result.isSamePerson);
  aliasRefineShortLog(ok ? ("通过 " + newName + "→" + mainName) : ("拒绝 " + newName));
  graphRemoteLog("alias_refine_result", {
    mainName: graphNormalizeName(mainName),
    newName: graphNormalizeName(newName),
    isSamePerson: ok,
    finalMainName: result && result.mainName ? graphSafeString(result.mainName, 60) : "",
    confirmedAliases: result && result.confirmedAliases ? result.confirmedAliases : [],
    removedAliases: result && result.removedAliases ? result.removedAliases : [],
    reason: result && result.reason ? graphSafeString(result.reason, 500) : ""
  });
};


// ===================== 全局发音人轮询 =====================
CharacterManager.prototype.loadGlobalVoiceUsage = function() {
  try {
      var content = ttsrv.readTxtFile("globalVoiceUsage.json");
      content = content ? String(content).trim() : "";
      this.globalVoiceUsage = content ? JSON.parse(content) : {};
  } catch (e) {
      this.globalVoiceUsage = {};
  }
};

CharacterManager.prototype.saveGlobalVoiceUsage = function() {
  try {
      ttsrv.writeTxtFile("globalVoiceUsage.json", JSON.stringify(this.globalVoiceUsage || {}, null, 2));
  } catch (e) {
      try { console.warn("【🗣️发音人】全局计数保存失败"); } catch (_w) {}
  }
};

CharacterManager.prototype.selectVoiceByGlobalRandom = function(candidates, label) {
  if (!candidates || candidates.length === 0) return null;
  if (!this.globalVoiceUsage) this.loadGlobalVoiceUsage();
  label = label || "候选";

  // 先随机打散，再按匹配层级和全局使用次数排序；同次数保留随机顺序。
  for (var si = candidates.length - 1; si > 0; si--) {
      var ri = Math.floor(Math.random() * (si + 1));
      var tmp = candidates[si];
      candidates[si] = candidates[ri];
      candidates[ri] = tmp;
  }

  var that = this;
  candidates.sort(function(a, b) {
      var levelA = typeof a.matchLevel === "number" ? a.matchLevel : 0;
      var levelB = typeof b.matchLevel === "number" ? b.matchLevel : 0;
      if (levelA !== levelB) return levelA - levelB;
      var countA = that.globalVoiceUsage[a.voice] || 0;
      var countB = that.globalVoiceUsage[b.voice] || 0;
      return countA - countB;
  });

  var selectedVoice = candidates[0].voice;
  this.globalVoiceUsage[selectedVoice] = (this.globalVoiceUsage[selectedVoice] || 0) + 1;
  this.saveGlobalVoiceUsage();
  this.voiceUsageMap[selectedVoice] = (this.voiceUsageMap[selectedVoice] || 0) + 1;
  this.usedVoices[selectedVoice] = true;
  try { console.log("【🗣️发音人】" + label + " " + selectedVoice + " 全局" + this.globalVoiceUsage[selectedVoice]); } catch (e) {}
  try {
      if (typeof graphRemoteLog === "function") {
          var voiceCtx = this._voiceAssignContext || {};
          var voiceAliasApiResultForObserve = voiceCtx.aliasApiResultForObserve || null;
          var voiceAliasAfterBlockForObserve = voiceCtx.aliasAfterBlockForObserve || null;
          var voiceAliasTargetMainRecordForObserve = voiceCtx.aliasTargetMainRecordForObserve || null;
          var voiceAliasMergeBlockReasonForObserve = voiceCtx.aliasMergeBlockReasonForObserve || "";
          var voiceEventType = voiceCtx.isSpecialSpeaker ? "special_voice_assigned" : "voice_assigned";
          graphRemoteLog(voiceEventType, {
              targetName: graphNormalizeName(voiceCtx.targetName || ""),
              assignType: graphSafeString(voiceCtx.assignType || label || "", 80),
              sourceStage: graphSafeString(voiceCtx.sourceStage || "", 80),
              afterAliasCheck: voiceCtx.afterAliasCheck === true,
              isSpecialSpeaker: voiceCtx.isSpecialSpeaker === true,
              aliasCheckResult: graphSafeString(voiceCtx.aliasCheckResult || "", 80),
              aliasApiIsAlias: !!(voiceAliasApiResultForObserve && voiceAliasApiResultForObserve.isAlias),
              aliasApiMainName: graphSafeString(voiceAliasApiResultForObserve && voiceAliasApiResultForObserve.mainName || "", 80),
              aliasFinalIsAlias: !!(voiceAliasAfterBlockForObserve && voiceAliasAfterBlockForObserve.isAlias),
              aliasFinalMainName: graphSafeString(voiceAliasAfterBlockForObserve && voiceAliasAfterBlockForObserve.mainName || "", 80),
              aliasTargetFound: !!voiceAliasTargetMainRecordForObserve,
              aliasBlockReason: graphSafeString(voiceAliasMergeBlockReasonForObserve || "", 200),
              aliasFinalDecision: graphV908AliasFinalDecisionStatus(voiceAliasApiResultForObserve, voiceAliasAfterBlockForObserve, voiceAliasTargetMainRecordForObserve, voiceAliasMergeBlockReasonForObserve),
              fromSplit: voiceCtx.fromSplit === true,
              voice: selectedVoice,
              label: label,
              globalUsage: this.globalVoiceUsage[selectedVoice] || 0,
              candidateCount: candidates.length
          });
          this._voiceAssignContext = null;
      }
  } catch (e2) {}
  return selectedVoice;
};

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
          if (!record.chapters || !Array.isArray(record.chapters)) {
              record.chapters = [];
          } else {
              record.chapters = graphTrimChapterArray(record.chapters);
          }
          if (!record.voice || record.voice === "") {
              record.gender = null;
              record.age = null;
          }
          if (record.voice) {
              this.usedVoices[record.voice] = true;
          }
      }
      try { if (this.repairDuplicateAliasMainRecords) this.repairDuplicateAliasMainRecords("loadRecords"); } catch(repairErr) {}
  } catch (e) {
      this.characterRecords = [];
  }
};

CharacterManager.prototype.updateContext = function(text2) {

  this.contextHistory2 = this.contextHistory
  this.contextHistory = (this.contextHistory + "\n" + text2).slice(-CONFIG.contextHistoryLength);
};

CharacterManager.prototype.findMainCharacterRecordByExactName = function(characterName) {
  var normalized = graphNormalizeName(characterName).toLowerCase();
  if (!normalized) return null;
  for (var i = 0; i < this.characterRecords.length; i++) {
      var record = this.characterRecords[i];
      if (!record || !record.name) continue;
      if (graphNormalizeName(record.name).toLowerCase() === normalized) return record;
  }
  return null;
};

CharacterManager.prototype.findCharacterRecord = function(characterName) {
  var normalized = graphNormalizeName(characterName).toLowerCase();
  if (!normalized) return null;
  // 主名优先，防止“乌丑既是主名又在极阴祖师aliases里”时被别名映射抢走，造成音色打架。
  var exact = this.findMainCharacterRecordByExactName ? this.findMainCharacterRecordByExactName(characterName) : null;
  if (exact) return exact;
  for (var i = 0; i < this.characterRecords.length; i++) {
      var record = this.characterRecords[i];
      if (!record || !record.aliases) continue;
      var aliases = String(record.aliases || "").split('|');
      for (var j = 0; j < aliases.length; j++) {
          var alias = graphNormalizeName(aliases[j]).toLowerCase();
          if (alias === normalized) return record;
      }
  }
  return null;
};

CharacterManager.prototype.removeAliasFromRecord = function(record, aliasName) {
  if (!record || !record.aliases) return false;
  aliasName = graphNormalizeName(aliasName);
  var arr = String(record.aliases || "").split("|");
  var out = [];
  var removed = false;
  for (var i = 0; i < arr.length; i++) {
    var a = graphNormalizeName(arr[i]);
    if (!a) continue;
    if (a === aliasName && a !== record.name) { removed = true; continue; }
    if (out.indexOf(a) === -1) out.push(a);
  }
  if (out.indexOf(record.name) === -1) out.unshift(record.name);
  if (removed) record.aliases = out.join("|");
  return removed;
};


CharacterManager.prototype.rebuildNameToMainNameMap = function() {
  var nameMap = {};
  if (!this.characterRecords) this.characterRecords = [];
  for (var i = 0; i < this.characterRecords.length; i++) {
    var rec = this.characterRecords[i];
    if (!rec || !rec.name) continue;
    var mainName = graphNormalizeName(rec.name);
    if (!mainName) continue;
    nameMap[mainName] = mainName;
    var arr = String(rec.aliases || mainName).split("|");
    for (var j = 0; j < arr.length; j++) {
      var al = graphNormalizeName(arr[j]);
      if (!al) continue;
      var exact = this.findMainCharacterRecordByExactName ? this.findMainCharacterRecordByExactName(al) : null;
      if (!exact || graphNormalizeName(exact.name) === mainName) nameMap[al] = mainName;
    }
  }
  this.nameToMainNameMap = nameMap;
  return nameMap;
};

CharacterManager.prototype.mergeCharacterRecords = function(target, source, reason) {
  if (!target || !source || target === source) return false;
  if (!target.mergedRecords || !Array.isArray(target.mergedRecords)) target.mergedRecords = [];
  try {
    var backup = JSON.parse(JSON.stringify(source));
    backup.mergedInto = graphNormalizeName(target.name);
    backup.mergedAt = graphNowIso();
    target.mergedRecords.push(backup);
    if (target.mergedRecords.length > 20) target.mergedRecords.shift();
    if (this.storeMergedRecordBackup) this.storeMergedRecordBackup(target, source, reason);
  } catch (backupErr) {}
  var aliasMap = {};
  function addAlias(n) { n = graphNormalizeName(n); if (n && !aliasMap[n]) aliasMap[n] = true; }
  addAlias(target.name); addAlias(source.name);
  var arr1 = String(target.aliases || "").split("|");
  var arr2 = String(source.aliases || "").split("|");
  for (var i = 0; i < arr1.length; i++) addAlias(arr1[i]);
  for (var j = 0; j < arr2.length; j++) addAlias(arr2[j]);
  var aliases = [target.name];
  for (var k in aliasMap) if (aliasMap.hasOwnProperty(k) && k !== target.name && !graphAliasMergeBlockReason(k, target.name)) aliases.push(k);
  target.aliases = aliases.join("|");
  if (!Array.isArray(target.chapters)) target.chapters = [];
  if (!Array.isArray(source.chapters)) source.chapters = [];
  var curMergeChapter = graphCurrentChapterId();
  if (curMergeChapter && curMergeChapter !== "unknown") {
    if (target.chapters.indexOf(curMergeChapter) === -1 && source.chapters.indexOf(curMergeChapter) === -1) target.chapters.push(curMergeChapter);
  }
  target.chapters = graphTrimChapterArray((target.chapters || []).concat(source.chapters || []));
  if (!target.genderAgeHistory) target.genderAgeHistory = [];
  if (source.genderAgeHistory && source.genderAgeHistory.length) target.genderAgeHistory = target.genderAgeHistory.concat(source.genderAgeHistory);
  for (var r = this.characterRecords.length - 1; r >= 0; r--) if (this.characterRecords[r] === source) this.characterRecords.splice(r, 1);
  if (this.rebuildNameToMainNameMap) this.rebuildNameToMainNameMap();
  graphRemoteLog("role_record_merged", { target: graphNormalizeName(target.name), source: graphNormalizeName(source.name), reason: graphSafeString(reason || "", 220), aliases: target.aliases, chapters: target.chapters || [] });
  return true;
};

CharacterManager.prototype.resolveDuplicateAliasMainConflict = function(mainRecord, aliasName, sourceReason, contextText) {
  if (!mainRecord || !mainRecord.name) return { allowAlias: false, action: "invalid" };
  aliasName = graphNormalizeName(aliasName);
  var exactRecord = this.findMainCharacterRecordByExactName ? this.findMainCharacterRecordByExactName(aliasName) : null;
  if (!exactRecord || exactRecord === mainRecord) return { allowAlias: true, action: "no_conflict" };
  var mainName = graphNormalizeName(mainRecord.name);
  graphRemoteLog("duplicate_alias_main_conflict_start", { mainName: mainName, aliasName: aliasName, aliasRecordVoice: exactRecord.voice || "", mainRecordVoice: mainRecord.voice || "", sourceReason: graphSafeString(sourceReason || "", 120) });
  var extra = "主角色与别名冲突:" + aliasName + "既是独立主名又将加入" + mainName + " aliases；" + graphSafeString(sourceReason || "", 120);
  var decision = this.verifyGraphConflictAndFix ? this.verifyGraphConflictAndFix("positive", aliasName, mainName, 3.5, "duplicate_alias_main_conflict", extra, "duplicate_alias_main_conflict", { defaultAllow: false, forceVerify: true, contextText: contextText || this.contextHistory2 || "" }) : { allow: false, relation: "uncertain" };
  graphRemoteLog("duplicate_alias_main_conflict_result", { mainName: mainName, aliasName: aliasName, allowAlias: !!decision.allow, relation: decision.relation || "", confidence: decision.confidence || 0, reason: graphSafeString(decision.reason || "", 240) });
  if (decision.allow) {
    this.mergeCharacterRecords(mainRecord, exactRecord, decision.reason || extra);
    this.saveRecords();
    return { allowAlias: true, action: "merge_records" };
  }
  this.removeAliasFromRecord(mainRecord, aliasName);
  if (!this.aliasCooccurStats) this.aliasCooccurStats = {};
  graphGetPairStats(this.aliasCooccurStats, mainName, aliasName);
  graphCooccurMarkChapter(this.aliasCooccurStats, mainName, aliasName);
  graphPushCooccurEvidence(this.aliasCooccurStats, mainName, aliasName, "主名别名冲突修复", decision.reason || extra);
  if (graphMarkChapterEvidenceOnce(this.aliasCooccurStats, mainName, aliasName, "duplicate_alias_main_conflict_removed", "", decision.reason || extra || "")) {
    graphAddWeightedEdge(this.aliasNegativeGraph, mainName, aliasName, 4.5, "duplicate_alias_main_conflict_removed", decision.reason || extra, "", { chapterId: graphCurrentChapterId(), evidenceText: decision.reason || extra || "" });
  }
  this.saveAliasGraphData();
  this.saveAliasCooccurStats();
  this.saveRecords();
  graphRemoteLog("duplicate_alias_main_conflict_fix", { action: "remove_alias_keep_separate", mainName: mainName, aliasName: aliasName, reason: graphSafeString(decision.reason || extra, 240) });
  return { allowAlias: false, action: "remove_alias_keep_separate" };
};

CharacterManager.prototype.repairDuplicateAliasMainRecords = function(stage) {
  if (!this.characterRecords || !this.characterRecords.length) return 0;
  var fixed = 0;
  for (var i = 0; i < this.characterRecords.length; i++) {
    var rec = this.characterRecords[i];
    if (!rec || !rec.name || !rec.aliases) continue;
    var arr = String(rec.aliases || "").split("|");
    for (var j = 0; j < arr.length; j++) {
      var al = graphNormalizeName(arr[j]);
      if (!al || al === rec.name) continue;
      var exact = this.findMainCharacterRecordByExactName ? this.findMainCharacterRecordByExactName(al) : null;
      if (exact && exact !== rec) {
        var res = this.resolveDuplicateAliasMainConflict(rec, al, "repairDuplicateAliasMainRecords:" + (stage || ""), this.contextHistory2 || "");
        if (res && res.action !== "no_conflict") fixed++;
      }
    }
  }
  if (fixed) { this.saveRecords(); graphRemoteLog("duplicate_alias_main_conflict_fix", { action: "repair_scan_done", stage: stage || "", fixed: fixed }); }
  return fixed;
};

CharacterManager.prototype.moveRecordToTop = function(characterName) {
  var normalized = characterName.trim().toLowerCase();
  for (var i = 0; i < this.characterRecords.length; i++) {
      var record = this.characterRecords[i];
      var recordName = record.name.trim().toLowerCase();
      if (recordName === normalized) {
          var removed = this.characterRecords.splice(i, 1)[0];
          if (this.markRecordActiveChapter) this.markRecordActiveChapter(removed);
          this.characterRecords.unshift(removed);
          return;
      }
      var aliases = record.aliases.split('|');
      for (var j = 0; j < aliases.length; j++) {
          var alias = aliases[j].trim().toLowerCase();
          if (alias === normalized) {
              var removed = this.characterRecords.splice(i, 1)[0];
              if (this.markRecordActiveChapter) this.markRecordActiveChapter(removed);
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
  // 标签扩容兜底：GENSHIN_CHARACTERS 仅含初始序号(如女青年01~100)，
  // 配置项可能有序号超出范围的 tag(如女青年517)，GENSHIN 遍历不到。
  // 扫描 tagsData 中「已知前缀+序号」格式的 key，若 GENSHIN 有该前缀但无此序号，
  // 视为扩容标签加入 availableVoices，避免被误判为失效而清空 record.voice。
  if (tagsData) {
      var _extPrefixes = {};
      for (var _ek in GENSHIN_CHARACTERS) {
          if (GENSHIN_CHARACTERS.hasOwnProperty(_ek)) {
              var _evp = GENSHIN_CHARACTERS[_ek].voice.toString().replace(/\d+$/, '');
              if (_evp) _extPrefixes[_evp] = true;
          }
      }
      for (var _tdKey in tagsData) {
          if (!Object.prototype.hasOwnProperty.call(tagsData, _tdKey)) continue;
          if (this.availableVoices[_tdKey]) continue; // 已存在，跳过
          var _em = _tdKey.match(/^(.+?)(\d+)$/);
          if (_em && _extPrefixes[_em[1]]) {
              this.availableVoices[_tdKey] = true;
          }
      }
  }
  // 失效发音人保留策略：配置项已删除时，detectAvailableVoices 不清空 record.voice，
  // 保留旧 tag 用于角色列表显示 ⚠（未朗读前的过渡态）；
  // 朗读到该角色时由 processCharacter 的 isVoiceInvalid 分支重新分配一个有效的并写回 record.voice
  try {
      for (var i = 0; i < this.characterRecords.length; i++) {
          var _rec = this.characterRecords[i];
          if (_rec && _rec.voice && _rec.voice !== "" && !this.availableVoices[_rec.voice]) {
              console.log("detectAvailableVoices: 发音人已失效(保留显示): " + _rec.voice);
          }
      }
  } catch (_cleanErr) {
      console.error("失效发音人检测异常: " + _cleanErr.toString());
  }
};

CharacterManager.prototype.isVoiceAvailable = function(tag) {
  return this.availableVoices.hasOwnProperty(tag);
};

CharacterManager.prototype.assignVoice = function (gender, age, voiceAssignContext) {
  var rawAssignGender = gender;
  var rawAssignAge = age;
  var normalizedAssign = graphV908NormalizeVoiceAssignGenderAge(gender, age);
  gender = normalizedAssign.gender;
  age = normalizedAssign.age;
  if (voiceAssignContext) {
      voiceAssignContext.rawGender = rawAssignGender;
      voiceAssignContext.rawAge = rawAssignAge;
      voiceAssignContext.normalizedGender = gender;
      voiceAssignContext.normalizedAge = age;
      this._voiceAssignContext = voiceAssignContext;
  }
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
      // 遍历动态标签，找可用的发音人；候选内部走全局轮询
      var duihuaExactCandidates = [];
      for (var i = 0; i < duihuaCandidates.length; i++) {
          var voiceTag = duihuaCandidates[i];
          var isUsed = usedVoiceMap.hasOwnProperty(voiceTag) || mainRoleVoiceBlacklist.hasOwnProperty(voiceTag);
          var isAvailable = this.isVoiceAvailable(voiceTag);
          if (!isUsed && isAvailable) {
              duihuaExactCandidates.push({ name: "【对话 " + voiceTag + "】", voice: voiceTag, matchLevel: 0 });
          }
      }
      if (duihuaExactCandidates.length > 0) {
          return this.selectVoiceByGlobalRandom(duihuaExactCandidates, "动态发音人");
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
      '男': ['男青年', '少年', '男童', '男中年', '男老年'],
      '女': ['女青年', '少女', '女童', '女中年', '女老年'],
      '特殊': ['系统', '旁白']
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

      // 第三步：去重列表有值，按全局计数最少优先复用
      if (uniqueVoiceList.length > 0) {
          var reuseCandidates = [];
          for (var uv = 0; uv < uniqueVoiceList.length; uv++) {
              reuseCandidates.push({ name: "同龄复用", voice: uniqueVoiceList[uv], matchLevel: 0 });
          }
          return this.selectVoiceByGlobalRandom(reuseCandidates, "同龄复用");
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
          var sameTypeFallbackCandidates = [];
          for (var stv = 0; stv < allSameTypeVoices.length; stv++) {
              sameTypeFallbackCandidates.push({ name: "同龄兜底", voice: allSameTypeVoices[stv].voice, matchLevel: 0 });
          }
          return this.selectVoiceByGlobalRandom(sameTypeFallbackCandidates, "同龄兜底");
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
      // 同性别有可用发音人，按全局计数最少优先分配
      if (allSameGenderVoices.length > 0) {
          var sameGenderCandidates = [];
          for (var sgv = 0; sgv < allSameGenderVoices.length; sgv++) {
              sameGenderCandidates.push({ name: "同性别兜底", voice: allSameGenderVoices[sgv].voice, matchLevel: 0 });
          }
          return this.selectVoiceByGlobalRandom(sameGenderCandidates, "同性别兜底");
      }
      // 极端到同性别都没可用发音人，才返回null（正常配置下永远走不到这）
      return null;
  }

  // 最终候选：匹配层级优先，同层级按全局计数最少优先
  return this.selectVoiceByGlobalRandom(candidates, "候选发音人");
};




// ===================== 最终完整版：角色分析主函数（新增对话映射提取，适配投票日志）=====================
CharacterManager.prototype.analyzeCharacter = function(fullText, characterId, allDialogues) {
  // ========== 原有配置完全保留，零改动 ==========
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
  // ========== 对话缓存：命中直接复用，未命中才重新启动批量姓名分析 ==========
  var currentDialogueText = allDialogues[targetIndex].text || "";
  var cacheMatchResult = matchDialogFromCache(currentDialogueText, characterId);
  if (cacheMatchResult) {
    return cacheMatchResult;
  }
  var batchParseResult = generateBatchSeqContent(allDialogues, next100Chars);
  var currentSeq = batchParseResult.characterIdToSeq[String(characterId)] || "";
  var mappedCurrentBlock = batchParseResult.characterIdToBlock[String(characterId)] || null;
  var currentTextMatched = !!(mappedCurrentBlock && normalizeNameAnalysisDialogueText(currentDialogueText) === normalizeNameAnalysisDialogueText(mappedCurrentBlock.dialogText || ""));
  graphRemoteLog("name_analysis_batch_alignment", {
    stage: "initial_batch_parse",
    characterId: graphSafeString(characterId || "", 80),
    currentDialogueText: graphSafeString(currentDialogueText, NAME_ANALYSIS_CACHE_TRACE_TEXT_MAX),
    currentBlockText: graphSafeString(mappedCurrentBlock && mappedCurrentBlock.dialogText || "", NAME_ANALYSIS_CACHE_TRACE_TEXT_MAX),
    currentBlockCount: batchParseResult.currentBlocks.length,
    futureBlockCount: batchParseResult.futureBlocks.length,
    numberedBlockCount: batchParseResult.quoteBlocks.length,
    expectedSeqs: batchParseResult.expectedSeqs.slice(0, 120),
    mappedSeq: currentSeq,
    mappingMethod: batchParseResult.mappingMethod || "",
    textMatched: currentTextMatched,
    alignmentOk: batchParseResult.mappingOk === true && !!currentSeq && currentTextMatched,
    mappingErrors: (batchParseResult.mappingErrors || []).slice(0, 40)
  });

  // 整批映射无法可靠建立时改用当前对白单条批次，避免把错误序号继续交给角色缓存和图谱
  if (!batchParseResult.mappingOk || !currentSeq || !currentTextMatched) {
    graphRemoteLog("name_analysis_alignment_retry", {
      stage: "batch_mapping_to_single_target",
      retryCount: 1,
      characterId: graphSafeString(characterId || "", 80),
      oldMappedSeq: currentSeq,
      mappingMethod: batchParseResult.mappingMethod || "",
      mappingErrors: (batchParseResult.mappingErrors || []).slice(0, 40),
      reason: !batchParseResult.mappingOk ? "批量对白与外层引号块未能完整对齐" : (!currentSeq ? "当前对白缺少明确序号映射" : "当前对白原文与映射块不一致")
    });
    batchParseResult = generateSingleTargetNameAnalysisBatch(currentDialogueText, characterId);
    currentSeq = batchParseResult.characterIdToSeq[String(characterId)] || "";
    mappedCurrentBlock = batchParseResult.characterIdToBlock[String(characterId)] || null;
    currentTextMatched = !!(mappedCurrentBlock && normalizeNameAnalysisDialogueText(currentDialogueText) === normalizeNameAnalysisDialogueText(mappedCurrentBlock.dialogText || ""));
    if (batchParseResult.mappingOk && currentSeq && currentTextMatched) {
      graphRemoteLog("name_analysis_alignment_retry_success", { stage: "single_target_rebuild", retryCount: 1, characterId: graphSafeString(characterId || "", 80), mappedSeq: currentSeq, currentDialogueText: graphSafeString(currentDialogueText, NAME_ANALYSIS_CACHE_TRACE_TEXT_MAX) });
    }
  }
  if (!batchParseResult.mappingOk || !currentSeq || !currentTextMatched || !batchParseResult.expectedSeqs.length) {
    graphRemoteLog("name_analysis_alignment_blocked", {
      stage: "before_name_api",
      characterId: graphSafeString(characterId || "", 80),
      mappedSeq: currentSeq,
      mappingMethod: batchParseResult.mappingMethod || "",
      mappingErrors: (batchParseResult.mappingErrors || []).slice(0, 40),
      reason: "批量与单条输入均未建立安全序号映射，禁止请求结果进入缓存、角色卡和图谱"
    });
    return this.analyzeCharacterFallback(fullText, characterId, "alignment_failed_before_api");
  }

  var fullBatchContent = batchParseResult.content;
  var nameAnalysisBatchId = "name_batch_" + graphHash([
    graphCurrentChapterId(),
    currentSeq,
    normalizeNameAnalysisCacheMatchText(currentDialogueText),
    fullBatchContent
  ].join("|"));
  if (ENABLE_NAME_ANALYSIS_CACHE_TRACE) {
    var currentInputDialogueSnapshot = [];
    for (var inputIndex = 0; inputIndex < (allDialogues || []).length; inputIndex++) {
      var inputDialogue = allDialogues[inputIndex] || {};
      var inputText = String(inputDialogue.text || "");
      var inputNormalized = normalizeNameAnalysisCacheMatchText(inputText);
      currentInputDialogueSnapshot.push({
        listIndex: inputIndex + 1,
        characterId: graphSafeString(inputDialogue.id || "", 80),
        normalizedHash: graphHash(inputNormalized),
        mappedSeq: batchParseResult.characterIdToSeq[String(inputDialogue.id)] || ""
      });
    }
    var actualSharedPreviousContext = String(prevContextChars || "");
    var actualCurrentAppSegment = String(typeof text2 !== "undefined" ? text2 : "");
    var actualDownstreamContext = String(typeof next100Chars !== "undefined" ? next100Chars : "");
    graphRemoteLog("name_analysis_batch_text_snapshot", {
      batchId: nameAnalysisBatchId,
      chapterId: graphCurrentChapterId(),
      characterId: graphSafeString(characterId || "", 80),
      currentSeq: currentSeq,
      mappingMethod: batchParseResult.mappingMethod || "",
      singleTargetFallback: batchParseResult.singleTargetFallback === true,
      contextMode: "shared_previous_plus_continuous_batch",
      sharedPreviousContextText: actualSharedPreviousContext,
      sharedPreviousContextLength: actualSharedPreviousContext.length,
      sharedPreviousEndsAtCurrentStart: !!(nameAnalysisContextMeta && nameAnalysisContextMeta.sharedPreviousEndsAtCurrentStart),
      currentAppSegmentText: actualCurrentAppSegment,
      currentAppSegmentLength: actualCurrentAppSegment.length,
      downstreamContextText: actualDownstreamContext,
      downstreamContextLength: actualDownstreamContext.length,
      contextExtension: nameAnalysisContextMeta || {},
      currentInputDialogues: currentInputDialogueSnapshot,
      finalNumberedBatchText: String(fullBatchContent || ""),
      dialogueContexts: graphBuildNameAnalysisDialogueContextSnapshot(batchParseResult.sourceRawText || "", batchParseResult.quoteBlocks || [], actualSharedPreviousContext),
      currentBlockCount: batchParseResult.currentBlocks.length,
      futureBlockCount: batchParseResult.futureBlocks.length,
      numberedBlockCount: batchParseResult.quoteBlocks.length,
      completeOuterQuoteCount: Number(batchParseResult.completeOuterQuoteCount || 0)
    });
  }
  var nameAnalysisRecentRoleHint = this.buildNameAnalysisRecentRoleHint ? this.buildNameAnalysisRecentRoleHint(fullBatchContent) : "";
  // 把此前审计通过且仍生效的临时换声状态送入本批，让模型逐角色继续判断。
  var activeTemporaryVoicePack = this.buildActiveTemporaryVoiceStatePackV908 ? this.buildActiveTemporaryVoiceStatePackV908(currentDialogueText) : { items: [], activeStateCount: 0, activeStateSetId: "temp_set_empty" };
  var temporaryVoiceStatePrompt = this.buildTemporaryVoiceStatePromptV908 ? this.buildTemporaryVoiceStatePromptV908(activeTemporaryVoicePack) : "";
  graphRemoteLog("name_analyze_narration_rule_hint", { enabled: true, rule: "编号引号块不等于全是对白；先判实际发声或非发声，非发声按旁白；只有明确系统主体才输出系统。复用表边界去重；一致性检查绝对高精度；direct-pair三档分流；绑定式角色备份；本地槽位解析分流；模型证据锚点规则；别名图谱审计建议；远程日志附全量角色快照。", hasRecentRoleHint: !!nameAnalysisRecentRoleHint });
  
  // 序号与对白原文映射直接复用统一解析结果，不再用第二套正则重新推算
  var dialogTextMap = batchParseResult.dialogTextMap || {};

  var prompt = 
"你是喜马拉雅听书软件中智能朗读功能的人声分配AI。任务是处理小说手稿中所有带【01】【02】序号标记的编号引号块；每个序号必须对应一个结果。第一步先判断编号引号块是否属于实际发声内容，只有确认属于实际发声后，才继续判断说话人。\n\n" +
"【主任务第一步：编号引号块性质判定——最高优先级】\n" +
"1. 编号引号块不等于全是对白。必须先判断该引号块是实际发声内容，还是叙述中的名称、术语、称号、引用词、强调词、物品名、生物名或其他非发声内容。\n" +
"2. 如果引号块在句法上只是叙述句的宾语、定语、同位语、被指称对象或其他非发声成分，而不是由某个主体实际说出，则该编号必须按旁白处理，不能把引号内名称本身识别为说话角色。\n" +
"3. 只有确认编号引号块属于实际发声内容后，才进入说话人归属判断。即使引号块确属对白，对白内部被提到、被询问、被评价或被称呼的人名，也不自动等于该对白的说话人。\n" +
"4. 无论编号块最终判为对白还是旁白，每个编号都必须返回完整结果，不能遗漏或错位。\n\n" +
"【旁白与系统输出规则】\n" +
"1. 编号块判为旁白时，name必须输出“旁白”，gender输出“特殊”，age输出“旁白”；不要输出“系统”。只有文本明确存在系统主体（如系统提示、系统说道、系统音、系统面板等）时，才可输出“系统”。\n" +
"2. 编号块判为旁白时，禁止把其中的物品名、法宝名、地点名、事件名、动作对象名、术语名或生物名作为name输出；旁白只能输出name=旁白。\n\n" +
"你要具备下面的能力，中文小说说话人识别（专业名称为「对话归因/说话人归属识别」），核心是先区分实际发声与非发声编号块，再将真实对白精准匹配到对应人物：\n" +
"1. 指代消解能力：人称代词（他/她）、身份代称（门主/师兄）、昵称与本名的精准对应，是该任务的核心难点，直接决定复杂场景的准确率；\n" +
"2. 隐式对话识别能力：无“XX说/道”等明确提示词的连续对话，能否通过上下文语境、人物交替逻辑正确归因；\n" +
"3. 中文小说语料适配度：熟悉网文叙事习惯、引号用法和神态动作绑定话术，避免把叙述中的引号内专名当成对白，也避免旁白与对白、动作发出者与说话人错位；\n" +
"4. 多人对话追踪能力：维护3人以上交叉对话的逻辑链，避免连续对话中出现说话人错位。\n" +
"**【实际对白说话人判断核心原则】**\n" +
"1. 严禁将实际对白双引号“”内部提及的人名当作说话人；引号内名字通常是说话者谈论、询问、评价或称呼的其他人，除非原文明确属于本人自我介绍或身份声明。\n" +
"2. 示例：`张伟说：“别提了，都是为了王明那个项目。”` 中，说话人是张伟，绝非王明。\n" +
"3. 连续对话中，说话人通常交替出现；若某角色连续多句对白，需检查是否有明确提示词（如“他接着说”）或上下文支持，避免错归为同一人。\n" +
getV908CharacterNamingAndSpeakerRules("name_analyze") +
"【最近N章/跨章召回的已知角色姓名复用表】\n" + (nameAnalysisRecentRoleHint ? nameAnalysisRecentRoleHint : "暂无最近N章/跨章召回的已知角色姓名复用表") + "\n\n" +
"【已知角色复用表使用边界 - 必须遵守】\n" +
"1. 复用表只用于把已经登记的主名/别名规范成主名，不负责推理新别名。\n" +
"2. 当前说话人称呼如果没有命中复用表中某角色的主名或别名，禁止直接输出该角色主名；应输出文本里的当前称呼。\n" +
"3. 当前分析姓名与已有角色姓名无明确关联时，禁止任何形式的猜疑：例如列表里有李明，但文本当前说话人是李总，且李总不是李明的已登记别名，也没有明确本名/介绍/就是证据，则必须输出李总，不能输出李明。\n" +
"4. 如果文本明确出现本名/真名/又称/就是/即是/自称/介绍为/A（B）等强同人表达，且当前称呼尚未登记为别名，姓名分析仍应输出文本当前称呼，并在__relations中报告same_person正证，让别名校验环节处理合并。\n" +
"5. 例：已知角色表只有范静梅、无范夫人别名；文本是范夫人说话，应输出范夫人。若文本还明确证明范夫人就是范静梅，则输出范夫人，并在__relations写范夫人=范静梅的强证据。\n\n" +
"【普通age判断与分级发声音龄证据——两条通道必须分开】\n" +
"1. 普通age判断是必做任务：每个编号块都必须返回name、gender、age。age只作为新角色首次建卡的保守初值；已有角色不会因为本轮普通age再次更新角色卡。普通姓名分析给出的初始年龄属于L0 provisional，不是已认证证据。\n" +
"2. __voiceAgeEvidence字段必须始终存在，默认值是[]，它是顺带收集通道而不是逐序号必做清单。先正常完成说话人和普通age判断；正常阅读中恰好遇到可逐字定位的明确年龄原文时，按L4→L3→L2→L1分级顺带加入对应等级的候选。不要为了凑数量主动搜索、拼凑或编造证据；也不要因为大多数批次返回[]很常见就消极漏掉真正明确的证据。绝大多数普通批次返回[]完全正常。\n" +
"3. evidenceLevel只能是L0、L1、L2、L3、L4：L4=明确当前自然声线年龄，或明确自然年龄/声线发生变化；L3=明确实际年龄、岁数或人生阶段；L2=明确外貌年龄，或叙述者明确使用年龄描述；L1=关系、辈分、称呼、经历时间线和上下文间接支持，只存证，不单独换声；L0=普通姓名分析的暂定年龄，只由每个编号结果的age表达，不要为它编造正式证据。称号是否构成L1证据由你依据原文判断，本地不会按关键词推断。\n" +
"4. 跨年龄段出现多条证据时，低等级不能推翻高等级；同年龄段仍要持久化证据但不更换音色；同段更高等级替换主证据元数据并把旧证据标记superseded；跨段高等级只有在审计结果acceptedChanges中明确列出才更新年龄和音色；同级冲突只有supersedesPrior=true并通过成对审计才可替换。所有旧证据保留，不删除。\n" +
"5. level/type/cue必须一致：L4使用voice；L3使用direct_age；L2使用appearance；L1使用relationship、context或timeline；L0若作为迁移兼容输入只能使用provisional。voice必须明确写出少年声、青年嗓音、中年声线、苍老声音等年龄感；direct_age必须明确写出年龄、岁数或成长阶段；appearance必须明确写出模样、面容或外表呈现的年龄。L1只能保存证据，stateAction使用evidence_only或persistent_update均不得据此换声。低沉、沙哑、冰冷、柔和、响亮、虚弱、淡淡地道、轻笑、低声、沉声或普通气质不能单独证明年龄。\n" +
"6. 把subjectName及称号从引文中剔除后，剩余连续原文仍必须支持目标年龄段；普通姓名、称号、角色设定、常识印象和普通age估计不能反向成为证据。若只剩‘根据角色设定/姓名/称号/沿用原年龄/年龄没有变化’，该候选必须删除。\n" +
"7. 冲突时按voice > direct_age > appearance裁决：模样少年但声音苍老取老年声线；模样老年但声音少年或青年取少年/青年声线。finalVoiceAgeStage只能填写童年、少年、青年、中年、老年之一；临时换声结束并恢复自然音色时填写空字符串。\n" +
"8. 自然且持续的真实声线/年龄状态使用applyScope=persistent、stateAction=persistent_update、coverageMode=natural_persistent、effectiveFromSeq=seq。临时伪装、压嗓、变声或模仿不得写入自然角色卡；只作用当前对白时使用one_shot，原文明示持续时才使用start，恢复本声时使用end。\n" +
"9. temporalScope只能是current、past、flashback、uncertain；回忆和过去声线不能作为当前状态。one_shot必须coverageMode=current_dialogue、effectiveFromSeq=seq、effectiveThroughSeq=seq、continuesBeyondBatch=false；start必须继续检查后续序号和旁白并正确填写through_seq或beyond_batch。\n" +
"10. 每条证据必须包含evidenceId、seq、subjectName、evidenceLevel、evidenceType、evidenceSubtype、cues、finalVoiceAgeStage、priorityCueType、decisionBasis、applyScope、stateAction、effectiveFromSeq、coverageMode、effectiveThroughSeq、continuesBeyondBatch、linkedEndEvidenceText、endTiming、temporalScope、evidenceText、reason、confidence、supersedesPrior、priorEvidenceId；subjectName必须是该seq实际说话人，任何字段都不得省略，允许为空的字段也必须显式返回空字符串。\n" +
"11. evidenceText、linkedEndEvidenceText和每个cue.evidenceText都必须是输入中的连续原文。最终自检：删除证据中的姓名和称号后，剩余原文本身还能否支持对应等级？L1可以是间接支持但只能存证；L2-L4若不能直接支持就删除候选。\n" +
"12. 可选返回__voiceAgeScan对象用于诊断：{\\\"scannedLevels\\\":[\\\"L4\\\",\\\"L3\\\",\\\"L2\\\",\\\"L1\\\"],\\\"candidateCount\\\":数字,\\\"summary\\\":\\\"中文扫描简述\\\",\\\"emptyReason\\\":\\\"无候选时可说明原文为何没有可定位证据\\\"}。它只是诊断信息，省略或为空都不影响结果，不要为了填它而编造内容。\n\n" +
temporaryVoiceStatePrompt +
"**【输出要求】**\n" +
"1. 分析文本中所有带【01】【02】【03】...序号标记的编号引号块；每个序号对应一个结果，序号和编号块一一对应，不能遗漏或错位；\n" +
"2. 返回严格JSON，key为编号序号，value包含name、gender、age；必须包含文本中所有序号，不能遗漏；\n" +
"3. 如果无法确定说话人姓名，就用前后对这个人的描述作为名字；如果连描述也没有，再按性别年龄使用群众类临时名；\n" +
"4. 你还必须返回 __relations 数组，用于记录当前批次文本中能被原文锚定的人物/角色关系原子证据；没有证据时返回空数组；\n" +
"5. __relations中的a/b命名遵守前述角色命名通用原则；命中已登记主名/别名时才规范成主名，未登记新称呼按正文当前称呼输出；\n" +
"6. __relations中每条relation.evidenceText必须是【当前待分析内容】中的原文片段，必须能在该区块中找到；不得从【上文内容】取证，也禁止只写‘上下文明确/可知/证明/显然/无歧义’等总结句。本条不限制__voiceAgeEvidence；年龄证据仍按上面的专属规则，可引用紧邻【上文内容】中逐字可定位的年龄原句。\n" +
"7. summary必须是模型总结句，用于审计和冲突校验，只能复述evidenceText支持的事实，不能新增原文没有的人名，不能写合并/拆分建议；\n" +
"8. 如果证据是A=X、X=B桥接链，bridgeNames必须列出X，directPair=false；不得伪装成directPair=true；\n" +
"9. 角色不按物种或物品类型限定：只要当前文本把某实体作为说话人、行动主体、意识主体、被称呼对象、对话对象、关系对象、身份对象，就按角色处理；若只是普通物品/材料/地点/功法名/组织名且没有承担角色功能，则不要输出关系证据；\n" +
"10. __relations 的 evidenceFamily / evidenceSubtype 只是对已经发现的明确证据做分类，不是任务清单；不要为了覆盖类别而补造证据，也不要因为格式列出了某类标签就强行寻找某类关系。\n" +
"11. 当前文本只支持几条关系就只输出几条；证据不足时返回空数组，少输出优先于错输出。\n" +
"12. 必须始终返回__voiceAgeEvidence数组；它与__relations相互独立，任一为空都不影响另一项。\n" +
"\n" +
"【__relations字段格式】\n" +
"每条关系必须包含：a、b、relationType、evidenceFamily、evidenceSubtype、evidenceText、summary、seq、anchorType、directPair、bridgeNames、confidence。\n" +
"relationType只能用：same_person、different_person、identity_relation、weak_relation。\n" +
"evidenceFamily按证据族填写：name_identity、explicit_difference、dialogue_relation、action_relation、social_relation、co_presence、identity_relation、weak_relation。\n" +
"evidenceSubtype填写具体链路名，如self_claim、called_as、introduced_as、parenthetical_alias、explicit_same_person、name_alias_statement、introduced_as_same_person、explicit_different_person、speaker_addressee、reply_relation、vocative_address、action_object、mutual_action、relationship、listed_together、counted_people、possession、control、puppet、impersonation、disguise、assumed_identity、soul_body_split、mention、memory、search、investigate、attention。\n" +
"以上枚举只是分类标签，不要求覆盖；不要把分类标签当成逐类扫描任务。\n" +
"\n" +
"【证据族与链路】\n" +
"1. name_identity / same_person：self_claim自称、called_as被称/人称/号称、introduced_as介绍为/名为、explicit_same_person A就是B/A即B/A正是B、name_alias_statement本名/真名/原名/又名/道号/法号/尊号、introduced_as_same_person介绍/引见/称为、parenthetical_alias A（B）。\n" +
"1b. explicit_difference / different_person：explicit_different_person A不是B/A并非B/A绝非B/A与B不是同一人。\n" +
"2. dialogue_relation / different_person：speaker_addressee A对B说/问/解释、reply_relation B回应A、vocative_address A称呼B为前辈/道友/师兄/掌柜等。\n" +
"3. action_relation / different_person：action_object A带着/攻击/救下/抓住/交给/吩咐B，mutual_action A与B交手/对峙/同行/互望。\n" +
"4. social_relation / different_person：师徒、亲属、主仆、敌友、同伴、道侣、上下级等明确关系。\n" +
"5. co_presence / different_person：listed_together A与B并列出现，counted_people A和B二人/两人/二者/三人之一。\n" +
"6. identity_relation：附身、操控、傀儡、冒充、顶替、乔装、化名、假身份、残魂/元神/魂魄与身体关系；只记录证据，不直接建议合并或拆分。\n" +
"7. weak_relation：提到、想起、寻找、调查、注意到等弱语义关联；只作为审计/提示，不要写成强同人或强非同人。\n" +
"\n" +"【已迁移到模型的封闭结构证据】\n" +
"以下结构原先由本地封闭式正则识别，现已全部由你在 __relations 中按原文证据输出，再交由证据审计/别名校验裁决；没有明确原文就不要输出。\n" +
"1. explicit_same_person：A就是B、A即是B、A即为B、A正是B、A便是B、A乃是B、A也就是B、A其实就是B、A与B是同一人/同一个人。\n" +
"2. name_alias_statement：A本名B、A真名B、A原名B、A又名B、A别名B、A又称B、A也叫B、A名叫/名为/名唤B、A叫做/叫作B、A自称B、A号称B、A人称B、A道号/法号/尊号B、A被称为/被称作B、A称为/称作B。\n" +
"3. introduced_as_same_person：介绍A为B、引见/引荐A为B、将/把A介绍为B、将/把A称为B、将/把A叫做B、将/把A名为B。\n" +
"4. parenthetical_alias：A（B）、A(B)、B（A）、B(A)；若括号内是身份、关系、状态、附身、操控、伪装、亲属、师徒等解释，不要当作普通别名。\n" +
"5. explicit_different_person：A不是B、A并非B、A绝非B、A非是B、A并不是B、A不是B本人/本尊/同一人/同一个人、A和B不是同一人、A与B并非一人。输出时 relationType=different_person，evidenceFamily=explicit_difference，evidenceSubtype=explicit_different_person。\n" +
"\n" +
"【正确示例】\n" +
"原文：韩立对曲魂说道：‘你留在这里。’ → relationType=different_person，evidenceFamily=dialogue_relation，evidenceSubtype=speaker_addressee，a=韩立，b=曲魂，evidenceText=韩立对曲魂说道。\n" +
"原文：曲魂带着韩立走出了殿阁。 → relationType=different_person，evidenceFamily=action_relation，evidenceSubtype=action_object，a=曲魂，b=韩立。\n" +
"原文：这位厉飞雨，其实就是韩立。 → relationType=same_person，evidenceFamily=name_identity，evidenceSubtype=explicit_same_person，a=厉飞雨，b=韩立。\n" +
"原文：残魂操控着曲魂的身体。 → relationType=identity_relation，evidenceFamily=identity_relation，evidenceSubtype=control，a=残魂，b=曲魂。\n" +
"原文：韩立正在寻找那只开口说话的石头。 → 如果石头在当前文本承担角色功能，可返回weak_relation或action_relation；如果只是普通石头，不输出。\n" +
"\n" +
"【错误示例】\n" +
"不要输出 evidenceText=‘上下文明确证明A就是B’；这是总结句，不是原文。\n" +
"不要因为A和B同段共现但没有动作/称呼/关系/身份结构就输出强证据。\n" +
"不要把复合结论、闭合结论、合并建议、拆分建议写入__relations。\n" +
"不要把关系描述当别名：师父、弟子、同伴等只能作为不同人关系证据，不能单独作为same_person。\n" +
"\n" +
"【普通age与年龄证据正反示例】\n" +
"1. 正确常规判断：原文只有‘血刀老祖冷冷地道’，可以保守返回name=血刀老祖、gender=男、age=男老年，但__voiceAgeEvidence必须为[]。普通age允许参考称号和人物印象，证据数组不允许。\n" +
"2. 错误常规判断：因为没有硬年龄证据就把age留空或写‘未知’。普通age是必填项，不能消极拒答。\n" +
"3. 正确有证据支撑：原文‘林舟开口时，是清亮的少年嗓音’可返回：\n" +
'[{"evidenceId":"age_01_1","seq":"01","subjectName":"林舟","evidenceLevel":"L4","evidenceType":"voice","evidenceSubtype":"voice_age","cues":[{"type":"voice","stage":"少年","evidenceText":"清亮的少年嗓音"}],"finalVoiceAgeStage":"少年","priorityCueType":"voice","decisionBasis":"原文直接说明实际发声呈现少年年龄感","applyScope":"persistent","stateAction":"persistent_update","effectiveFromSeq":"01","coverageMode":"natural_persistent","effectiveThroughSeq":"","continuesBeyondBatch":false,"linkedEndEvidenceText":"","endTiming":"","temporalScope":"current","evidenceText":"林舟开口时，是清亮的少年嗓音","reason":"当前自然声线有明确原文证据","confidence":96,"supersedesPrior":false,"priorEvidenceId":""}]\n' +
"4. 错误证据支撑：不得返回：\n" +
'[{"evidenceId":"age_01_1","seq":"01","subjectName":"血刀老祖","cues":[{"type":"voice","stage":"老年","evidenceText":"血刀老祖冷冷地道"}],"finalVoiceAgeStage":"老年","applyScope":"persistent","stateAction":"persistent_update","evidenceText":"血刀老祖冷冷地道"}]\n' +
"错误原因：‘老祖’和‘冷冷地道’可参与普通age估计，却没有证明实际老年声线。\n" +
"5. 正确临时换声：原文‘萧炎刻意压低嗓音，装成苍老声音，从现在起要用这副嗓音骗过守卫’可返回：\n" +
'[{"evidenceId":"age_01_temp","seq":"01","subjectName":"萧炎","cues":[{"type":"voice","stage":"老年","evidenceText":"刻意压低嗓音，装成苍老声音"}],"finalVoiceAgeStage":"老年","decisionBasis":"原文明示刻意伪装并延续","applyScope":"scene","stateAction":"start","effectiveFromSeq":"01","coverageMode":"beyond_batch","effectiveThroughSeq":"","continuesBeyondBatch":true,"linkedEndEvidenceText":"","endTiming":"","temporalScope":"current","evidenceText":"刻意压低嗓音，装成苍老声音，从现在起要用这副嗓音骗过守卫","reason":"临时苍老声线从本句开始并延续到后续批次","confidence":98}]\n' +
"6. 错误临时换声：上面的明确伪装不得写成applyScope=persistent、stateAction=persistent_update；仅有‘他低声道’也不得创建start或one_shot。\n" +
"\n" +
"输出格式示例：\n" +
"{\n" +
"  \"01\": {\"name\": \"血刀老祖\", \"gender\": \"男\", \"age\": \"男老年\"},\n" +
"  \"02\": {\"name\": \"韩立\", \"gender\": \"男\", \"age\": \"男青年\"},\n" +
"  \"__voiceAgeEvidence\": [],\n" +
"  \"__relations\": [\n" +
"    {\"a\":\"韩立\",\"b\":\"店主\",\"relationType\":\"different_person\",\"evidenceFamily\":\"dialogue_relation\",\"evidenceSubtype\":\"speaker_addressee\",\"evidenceText\":\"韩立小心的询问一家店铺的掌柜\",\"summary\":\"韩立向掌柜询问\",\"seq\":\"01\",\"anchorType\":\"current_text_direct_pair\",\"directPair\":true,\"bridgeNames\":[],\"confidence\":85}\n" +
"  ]\n" +
"}\n" +
"\n" +
"\n";

  function sleep(ms) {
    var start = Date.now();
    while (Date.now() - start < ms) {}
  }

  var batchResult = null;
  var maxRetryRound = Math.ceil(CHARACTER_ANALYZE_RETRY_MAX / bingfa);
  var currentRound = 0;
  var expectedSeqMap = {};
  for (var expectedIndex = 0; expectedIndex < batchParseResult.expectedSeqs.length; expectedIndex++) {
    expectedSeqMap[batchParseResult.expectedSeqs[expectedIndex]] = true;
  }

  // 严格比较统一解析器产生的完整序号集合；缺失、额外或字段不全都必须进入重试
  function validateNameAnalyzeBatchResult(result) {
    var errors = [];
    if (!result || typeof result !== "object" || Array.isArray(result)) return { ok: false, errors: ["result_not_object"] };
    for (var ei = 0; ei < batchParseResult.expectedSeqs.length; ei++) {
      var expectedSeq = batchParseResult.expectedSeqs[ei];
      var expectedItem = result[expectedSeq];
      if (!expectedItem) errors.push("missing_seq_" + expectedSeq);
      else if (!expectedItem.name || !expectedItem.gender || !expectedItem.age) errors.push("incomplete_fields_" + expectedSeq);
    }
    for (var resultKey in result) {
      if (!result.hasOwnProperty(resultKey) || !/^\d+$/.test(String(resultKey))) continue;
      if (!expectedSeqMap[resultKey]) errors.push("unexpected_seq_" + resultKey);
    }
    return { ok: errors.length === 0, errors: errors };
  }

  function buildNameAnalyzeRequest(apiConfig) {
    var requestData = {
      model: apiConfig.model,
      messages: [
        { role: 'system', content: prompt },
        { role: 'user', content: "【上文内容】\n" + String(prevContextChars || "") + "\n【当前待分析内容（本段原文+下文截取及必要外延）】\n" + fullBatchContent }
      ],
      temperature: CONFIG.apiTemperature
    };
    var headers = {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + apiConfig.key,
      'Connection': 'keep-alive',
      'Timeout': requestTimeout.toString()
    };
    if (ENABLE_MODEL_RAW_REMOTE_LOG) {
      graphRemoteLog("name_llm_raw_request", {
        scene: "name_analyze",
        targetSeq: currentSeq,
        characterId: graphSafeString(characterId || "", 80),
        dialogCount: allDialogues ? allDialogues.length : 0,
        hasRecentRoleHint: !!nameAnalysisRecentRoleHint,
        promptLen: String(prompt || "").length,
        batchContentLen: String(fullBatchContent || "").length,
        endpoint: graphSafeString(apiConfig.endpoint || "", 200),
        model: graphSafeString(apiConfig.model || "", 80),
        requestData: graphSafeString(JSON.stringify(requestData), MODEL_RAW_REMOTE_LOG_MAX_LEN)
      });
    }
    return {
      endpoint: apiConfig.endpoint,
      data: requestData,
      headers: headers
    };
  }

  function parseNameAnalyzeResponse(response) {
    var responseBody = response.body ? String(response.body().string() || "{}") : "{}";
    if (ENABLE_MODEL_RAW_REMOTE_LOG) {
      graphRemoteLog("name_llm_raw_response", {
        scene: "name_analyze",
        targetSeq: currentSeq,
        characterId: graphSafeString(characterId || "", 80),
        dialogCount: allDialogues ? allDialogues.length : 0,
        responseBody: graphSafeString(responseBody, MODEL_RAW_REMOTE_LOG_MAX_LEN)
      });
    }
    var apiResponse = JSON.parse(responseBody);
    if (!apiResponse.choices || !apiResponse.choices[0] || !apiResponse.choices[0].message || !apiResponse.choices[0].message.content) {
      throw new Error("API返回格式错误（无content字段）");
    }
    var aiResult = String(apiResponse.choices[0].message.content || "").trim();
    var jsonMatch = aiResult.match(/\{[\s\S]*\}/);
    if (!jsonMatch) {
      throw new Error("AI返回非JSON格式");
    }
    var parsedResult = JSON.parse(jsonMatch[0]);
    var parsedValidation = validateNameAnalyzeBatchResult(parsedResult);
    if (!parsedValidation.ok) throw new Error("姓名分析序号对齐失败：" + parsedValidation.errors.join(","));
    if (parsedResult.__relations && !Array.isArray(parsedResult.__relations)) {
      parsedResult.__relations = [];
    }
    if (!parsedResult.__relations) parsedResult.__relations = [];
    if (parsedResult.__voiceAgeEvidence && !Array.isArray(parsedResult.__voiceAgeEvidence)) parsedResult.__voiceAgeEvidence = [];
    if (!parsedResult.__voiceAgeEvidence) parsedResult.__voiceAgeEvidence = [];
    if (activeTemporaryVoicePack.items && activeTemporaryVoicePack.items.length && (!parsedResult.__temporaryVoiceStateReview || typeof parsedResult.__temporaryVoiceStateReview !== "object")) parsedResult.__temporaryVoiceStateReview = {};
    graphRemoteLog("voice_age_model_raw", {
      scene: "name_analyze",
      stage: "model_raw_before_precheck_and_audit",
      targetSeq: currentSeq,
      characterId: graphSafeString(characterId || "", 80),
      evidenceCount: parsedResult.__voiceAgeEvidence.length,
      evidence: parsedResult.__voiceAgeEvidence,
      countLimitApplied: false
    });
    graphRemoteLog("name_semantic_model_raw", {
      scene: "name_analyze",
      stage: "model_raw_before_vote_and_precheck",
      targetSeq: currentSeq,
      characterId: graphSafeString(characterId || "", 80),
      relationCount: parsedResult.__relations.length,
      rawRelations: parsedResult.__relations.slice(0, 80)
    });
    if (activeTemporaryVoicePack.items && activeTemporaryVoicePack.items.length) graphRemoteLog("temporary_voice_state_review_raw", { stage: "name_analyze_parse", activeStateCount: activeTemporaryVoicePack.items.length, activeStateSetId: activeTemporaryVoicePack.activeStateSetId, rawReview: parsedResult.__temporaryVoiceStateReview || {} });
    return parsedResult;
  }

  while (currentRound < maxRetryRound && !batchResult) {
    currentRound++;
    var concurrentResult = concurrentApiRequest(
      "nameAnalyze",
      buildNameAnalyzeRequest.bind(this),
      parseNameAnalyzeResponse,
      null,
      requestTimeout
    );
    if (concurrentResult.success) {
      if (concurrentResult.isMultiResult) {
        batchResult = voteNameAnalyzeResult(concurrentResult.data, dialogTextMap);
      } else {
        batchResult = concurrentResult.data;
      }
      var votedValidation = validateNameAnalyzeBatchResult(batchResult);
      if (!votedValidation.ok) {
        graphRemoteLog("name_analysis_alignment_retry", { stage: "model_result_validation", retryCount: currentRound, characterId: graphSafeString(characterId || "", 80), targetSeq: currentSeq, expectedSeqs: batchParseResult.expectedSeqs.slice(0, 120), errors: votedValidation.errors.slice(0, 80), reason: "模型归并结果序号集合或字段不完整" });
        batchResult = null;
        if (currentRound < maxRetryRound) sleep(250);
      } else if (currentRound > 1) {
        graphRemoteLog("name_analysis_alignment_retry_success", { stage: "model_result_validation", retryCount: currentRound, characterId: graphSafeString(characterId || "", 80), targetSeq: currentSeq, expectedSeqs: batchParseResult.expectedSeqs.slice(0, 120) });
      }
    } else {
      graphRemoteLog("name_analysis_alignment_retry", { stage: "name_api_round_failed", retryCount: currentRound, characterId: graphSafeString(characterId || "", 80), targetSeq: currentSeq, expectedSeqs: batchParseResult.expectedSeqs.slice(0, 120), errors: (concurrentResult.errors || []).slice(0, 40), reason: "API失败、非JSON、序号缺失、额外序号或字段不完整" });
      if (currentRound < maxRetryRound) {
        sleep(250);
      }
    }
  }

  if (!batchResult) {
    console.error("【批量分析】所有重试均失败，走降级兜底逻辑");
    graphRemoteLog("name_analysis_alignment_blocked", { stage: "name_api_retry_exhausted", characterId: graphSafeString(characterId || "", 80), targetSeq: currentSeq, expectedSeqs: batchParseResult.expectedSeqs.slice(0, 120), retryMax: CHARACTER_ANALYZE_RETRY_MAX, reason: "姓名分析所有重试均失败，固定使用duihua且禁止写缓存、角色卡和图谱" });
    return this.analyzeCharacterFallback(fullText, characterId, "name_api_retry_exhausted");
  }

  // 年龄证据这里只做本地预检并暂存；API审计稍后与别名/图谱证据合并，避免三条流程各自重复调用。
  var auditedVoiceAgeEvidence = [];
  this._v908LastVoiceAgePrefilterStats = { inputCount: 0, fixedVoiceSkippedCount: 0, sameSegmentSkippedCount: 0, outputCount: 0 };
  try {
    var rawVoiceAgeEvidence = Array.isArray(batchResult.__voiceAgeEvidence) ? batchResult.__voiceAgeEvidence : [];
    var temporaryReviewCandidates = this.resolveTemporaryVoiceStateReviewV908 ? this.resolveTemporaryVoiceStateReviewV908(batchResult, activeTemporaryVoicePack, batchParseResult.expectedSeqs, fullBatchContent, String(prevContextChars || "")) : [];
    if (temporaryReviewCandidates && temporaryReviewCandidates.length) rawVoiceAgeEvidence = rawVoiceAgeEvidence.concat(temporaryReviewCandidates);
    if (!ENABLE_TEMPORARY_VOICE_STATE) {
      var ignoredTemporaryEvidenceCount = 0;
      rawVoiceAgeEvidence = rawVoiceAgeEvidence.filter(function(evidence) {
        var action = evidence && evidence.stateAction || "";
        var temporary = action === "one_shot" || action === "start" || action === "end" || action === "continue" || action === "replace" || evidence && evidence.sourceType === "temporary_state_review";
        if (temporary) ignoredTemporaryEvidenceCount++;
        return !temporary;
      });
      if (ignoredTemporaryEvidenceCount) graphRemoteLog("temporary_voice_feature_disabled", { ignoredTemporaryEvidenceCount: ignoredTemporaryEvidenceCount, clearedMemoryStateCount: 0, ignoredCacheSnapshot: true, returnedNaturalVoice: true });
    }
    if (ENABLE_VOICE_AGE_EVIDENCE && rawVoiceAgeEvidence.length && this.precheckVoiceAgeEvidence) {
      auditedVoiceAgeEvidence = this.precheckVoiceAgeEvidence(
        rawVoiceAgeEvidence,
        batchResult,
        batchParseResult.expectedSeqs,
        fullBatchContent,
        String(prevContextChars || "")
      );
      if (this.setPendingVoiceAgeEvidenceV908) {
        this.setPendingVoiceAgeEvidenceV908(auditedVoiceAgeEvidence, fullBatchContent, String(prevContextChars || ""), batchParseResult.expectedSeqs, batchResult);
      }
    }
  } catch(voiceAgeAuditErr) {
    graphRemoteLog("voice_age_audit_result", { success: false, reason: "voice_age_precheck_or_pending_exception", error: graphSafeString(voiceAgeAuditErr && voiceAgeAuditErr.message || voiceAgeAuditErr, 320), action: "keep_existing_voice_age_and_continue_name_pipeline" });
    auditedVoiceAgeEvidence = [];
  }
  batchResult.__voiceAgeEvidence = auditedVoiceAgeEvidence;
  var voiceAgeEvidenceBySeq = {};
  for (var vaeIndex = 0; vaeIndex < auditedVoiceAgeEvidence.length; vaeIndex++) {
    var vaeItem = auditedVoiceAgeEvidence[vaeIndex] || {};
    var scheduledSeqs = graphV908VoiceAgeScheduleSeqsV908(vaeItem);
    if (scheduledSeqs.length && (vaeItem.sourceType === "temporary_state_review" || vaeItem.stateAction === "start")) graphRemoteLog("temporary_voice_transition_scheduled", { evidenceId: vaeItem.evidenceId || "", activeStateId: vaeItem.activeStateId || "", stateAction: vaeItem.stateAction || "", reviewDecision: vaeItem.reviewDecision || "", scheduledSeqs: scheduledSeqs, endBoundarySeq: vaeItem.endBoundarySeq || vaeItem.effectiveThroughSeq || "", endTiming: vaeItem.endTiming || "" });
    for (var scheduledIndex = 0; scheduledIndex < scheduledSeqs.length; scheduledIndex++) {
      var vaeSeq = graphSafeString(scheduledSeqs[scheduledIndex] || "", 20);
      if (!vaeSeq) continue;
      if (!voiceAgeEvidenceBySeq[vaeSeq]) voiceAgeEvidenceBySeq[vaeSeq] = [];
      // 中文注释：同一待审对象在内存中保持同一引用，合并审计提交后当前句可立即看到accepted结果。
      voiceAgeEvidenceBySeq[vaeSeq].push(vaeItem);
    }
  }

  var dialogList = [];
  var returnedNamesForLog = [];
  var specialSpeakersForLog = [];
  var knownRoleReuseHitsForLog = [];
  var unknownNewNamesForLog = [];
  var seenReturnedForLog = {};
  var seenUnknownForLog = {};
  for (var blockIndex = 0; blockIndex < batchParseResult.quoteBlocks.length; blockIndex++) {
    var parsedBlock = batchParseResult.quoteBlocks[blockIndex] || {};
    var seq = parsedBlock.seq;
    var rawDialog = parsedBlock.dialogText || "";
    var itemResult = batchResult[seq];
    var originalItemName = itemResult && itemResult.name ? graphNormalizeName(itemResult.name) : "";
    if (ENABLE_NARRATION_OBJECT_NAME_FIX && itemResult && itemResult.gender === "特殊" && itemResult.age === "旁白" && graphNormalizeName(itemResult.name) !== "旁白") {
      graphRemoteLog("narration_name_fixed_to_narrator", { seq: seq, oldName: graphNormalizeName(itemResult.name), reason: "模型age=旁白，name疑似物品/地点/事件/对象名，修正为旁白" });
      itemResult.name = "旁白";
    }
    if (ENABLE_NARRATOR_SYSTEM_PRESERVE_FIX && itemResult && graphNormalizeName(itemResult.name) === "旁白") {
      if (itemResult.gender !== "特殊" || itemResult.age !== "旁白") {
        graphRemoteLog("speaker_mapping_preserve_narrator", { seq: seq, oldGender: itemResult.gender || "", oldAge: itemResult.age || "", reason: "模型原始name为旁白，保持旁白发音人而非系统" });
      }
      itemResult.gender = "特殊";
      itemResult.age = "旁白";
    } else if (itemResult && graphNormalizeName(itemResult.name) === "系统") {
      graphRemoteLog("speaker_system_result_trace", { seq: seq, dialog: graphSafeString(rawDialog, 120), gender: itemResult.gender || "", age: itemResult.age || "", reason: "模型原始输出系统，当前规则不做语义覆盖，仅记录观察" });
    }
    var retNameForLog = itemResult && itemResult.name ? graphNormalizeName(itemResult.name) : "";
    if (retNameForLog && !seenReturnedForLog[retNameForLog]) {
      seenReturnedForLog[retNameForLog] = true;
      returnedNamesForLog.push(retNameForLog);
    }
    var specialTypeForLog = graphSpecialSpeakerType(retNameForLog, itemResult ? itemResult.gender : "", itemResult ? itemResult.age : "");
    if (specialTypeForLog) {
      specialSpeakersForLog.push({ seq: seq, name: retNameForLog, gender: itemResult.gender || "", age: itemResult.age || "", originalName: originalItemName });
    }
    if (retNameForLog && !specialTypeForLog && retNameForLog !== "未知") {
      var matchedRecordForLog = this.findCharacterRecord ? this.findCharacterRecord(retNameForLog) : null;
      if (matchedRecordForLog && matchedRecordForLog.name) {
        knownRoleReuseHitsForLog.push({ seq: seq, returnedName: retNameForLog, matchedMainName: graphNormalizeName(matchedRecordForLog.name), matchType: graphNormalizeName(matchedRecordForLog.name) === retNameForLog ? "main" : "alias" });
      } else if (!seenUnknownForLog[retNameForLog]) {
        seenUnknownForLog[retNameForLog] = true;
        unknownNewNamesForLog.push(retNameForLog);
      }
    }
    dialogList.push({
      seq: seq,
      dialogContent: rawDialog,
      name: itemResult.name,
      gender: itemResult.gender,
      age: itemResult.age,
      voiceAgeEvidence: voiceAgeEvidenceBySeq[seq] || [] // 先缓存待审证据；统一审计提交后会原位更新为最终结果
    });
  }

  var relForParsedLog = batchResult.__relations || batchResult.relations || batchResult._relations || [];
  if (ENABLE_NAME_ANALYSIS_PARSED_RESULT_LOG) {
    var relationSamplesForLog = [];
    if (relForParsedLog && relForParsedLog.length) {
      for (var rs = 0; rs < relForParsedLog.length && relationSamplesForLog.length < 8; rs++) {
        var rrLog = relForParsedLog[rs] || {};
        relationSamplesForLog.push({ a: graphNormalizeName(rrLog.a || rrLog.nameA || rrLog.from || rrLog.left), b: graphNormalizeName(rrLog.b || rrLog.nameB || rrLog.to || rrLog.right), relationType: graphSafeString(rrLog.relationType || rrLog.type || rrLog.relation || "", 40), evidenceFamily: graphSafeString(rrLog.evidenceFamily || rrLog.family || "", 60), evidenceSubtype: graphSafeString(rrLog.evidenceSubtype || rrLog.subtype || "", 60), evidenceText: graphSafeString(rrLog.evidenceText || rrLog.evidence || rrLog.reason || rrLog.text || "", 220), summary: graphSafeString(rrLog.summary || "", 160), directPair: rrLog.directPair === true || rrLog.directPair === "true" });
      }
    }
    var voiceAgePrefilterStats = this._v908LastVoiceAgePrefilterStats || {};
    graphRemoteLog("name_analysis_parsed_result", { targetSeq: currentSeq, characterId: graphSafeString(characterId || "", 80), returnedNames: returnedNamesForLog.slice(0, 80), specialSpeakers: specialSpeakersForLog.slice(0, 30), knownRoleReuseHits: knownRoleReuseHitsForLog.slice(0, 60), unknownNewNames: unknownNewNamesForLog.slice(0, 60), relationCount: relForParsedLog ? relForParsedLog.length : 0, relationSamples: relationSamplesForLog, voiceAgeRawEvidenceCount: rawVoiceAgeEvidence && rawVoiceAgeEvidence.length || 0, voiceAgeEvidenceCount: auditedVoiceAgeEvidence.length, voiceAgeAuditCandidateCount: auditedVoiceAgeEvidence.filter(function(x){ return x && x.precheckPassed === true; }).length, voiceAgeSameSegmentSkippedCount: Number(voiceAgePrefilterStats.sameSegmentSkippedCount || 0), voiceAgeFixedVoiceSkippedCount: Number(voiceAgePrefilterStats.fixedVoiceSkippedCount || 0), voiceAgeAcceptedCount: auditedVoiceAgeEvidence.filter(function(x){ return x && x.accepted === true; }).length });
    if (unknownNewNamesForLog.length) graphRemoteLog("alias_check_queue_created", { names: unknownNewNamesForLog.slice(0, 60), total: unknownNewNamesForLog.length, targetSeq: currentSeq, chapterIndex: graphCurrentChapterId() });
    if (unknownNewNamesForLog.length) graphRemoteLog("alias_check_queue_observation_registered", { names: unknownNewNamesForLog.slice(0, 60), total: unknownNewNamesForLog.length, completed: false, note: "批量姓名分析阶段仅登记观察名单，不代表逐项校验已完成；实际执行以alias_check_queue_item_start/item_done日志为准" });
  }

  var currentBlockIndex = 0;
  for (var cacheBlockIndex = 0; cacheBlockIndex < batchParseResult.quoteBlocks.length; cacheBlockIndex++) {
    if (batchParseResult.quoteBlocks[cacheBlockIndex].seq === currentSeq) {
      currentBlockIndex = cacheBlockIndex;
      break;
    }
  }
  var newCache = {
    currentIndex: currentBlockIndex + 2,
    dialogList: dialogList,
    relationEvidence: relForParsedLog || [],
    temporaryVoiceSnapshot: this.exportTemporaryVoiceSnapshot ? this.exportTemporaryVoiceSnapshot() : null,
    cacheBatchId: nameAnalysisBatchId,
    cacheCreatedChapterId: graphCurrentChapterId(),
    cacheCreatedAt: graphNowIso()
  };
  var cacheWritten = writeDialogCache(newCache);
  if (cacheWritten && ENABLE_NAME_ANALYSIS_CACHE_TRACE) {
    graphRemoteLog("name_analysis_dialog_cache_written", { characterId: graphSafeString(characterId || "", 80), mappedSeq: currentSeq, currentDialogueText: graphSafeString(currentDialogueText, NAME_ANALYSIS_CACHE_TRACE_TEXT_MAX), modelBlockText: graphSafeString(mappedCurrentBlock && mappedCurrentBlock.dialogText || "", NAME_ANALYSIS_CACHE_TRACE_TEXT_MAX), textMatched: currentTextMatched, currentIndex: newCache.currentIndex, dialogCount: dialogList.length, singleTargetFallback: batchParseResult.singleTargetFallback === true });
    graphRemoteLog("name_analysis_dialog_cache_snapshot", {
      stage: "cache_written",
      batchId: newCache.cacheBatchId,
      cacheCreatedChapterId: newCache.cacheCreatedChapterId,
      cacheCreatedAt: newCache.cacheCreatedAt,
      currentIndex: newCache.currentIndex,
      dialogCount: dialogList.length,
      mappedSeq: currentSeq,
      currentDialogueText: String(currentDialogueText || ""),
      cacheDialogues: graphBuildNameAnalysisCacheSnapshot(dialogList)
    });
  }
  if (this.updateAliasGraphsFromCache) {
    // fullBatchContent已经包含下文截取，禁止再次拼接next100Chars造成审计原文重复。
    this.updateAliasGraphsFromCache(dialogList, fullBatchContent, relForParsedLog || []);
  }

  var currentResult = batchResult[currentSeq];
  currentResult.__voiceAgeEvidence = voiceAgeEvidenceBySeq[currentSeq] || [];
  currentResult.__dialogCacheMeta = {
    matchedCacheIndex: currentBlockIndex + 1,
    matchedSeq: currentSeq,
    currentDialogueHash: graphHash(normalizeNameAnalysisDialogueText(currentDialogueText)),
    bookKey: this.aliasGraphBookKey || graphBookCacheSafeKey("", graphCurrentBookUrl || ""),
    chapterId: graphCurrentChapterId(),
    temporaryVoiceSnapshot: newCache.temporaryVoiceSnapshot || null,
    source: "fresh_batch_analysis"
  };
  try {
    if (currentResult && currentResult.name) graphRemoteLog("name_analysis_batch_result_used", { seq: currentSeq, name: graphNormalizeName(currentResult.name), gender: currentResult.gender || "", age: currentResult.age || "", sourceStage: "batch_name_analysis_result", currentChapter: graphCurrentChapterId(), characterId: graphSafeString(characterId || "", 80), currentDialogueText: graphSafeString(currentDialogueText, NAME_ANALYSIS_CACHE_TRACE_TEXT_MAX), modelBlockText: graphSafeString(mappedCurrentBlock && mappedCurrentBlock.dialogText || "", NAME_ANALYSIS_CACHE_TRACE_TEXT_MAX), textMatched: currentTextMatched, singleTargetFallback: batchParseResult.singleTargetFallback === true });
  } catch(batchUseErr) {}
  if (ENABLE_NARRATION_OBJECT_NAME_FIX && currentResult && currentResult.gender === "特殊" && currentResult.age === "旁白" && graphNormalizeName(currentResult.name) !== "旁白") {
    graphRemoteLog("narration_name_fixed_to_narrator", { seq: currentSeq, oldName: graphNormalizeName(currentResult.name), reason: "当前序号age=旁白，返回前修正为旁白" });
    currentResult.name = "旁白";
  }
  if (ENABLE_NARRATOR_SYSTEM_PRESERVE_FIX && currentResult && graphNormalizeName(currentResult.name) === "旁白") {
    currentResult.gender = "特殊";
    currentResult.age = "旁白";
  }
  return currentResult;
};








CharacterManager.prototype.getAllCharacterNamesAndAliases = function(targetGender) {
  var allNamesSet = new Set(); // 用Set自动去重：存储所有主名+别名
  var nameMap = {}; // 保留主名与别名的对应关系（主名→主名，别名→主名）

  // 核心逻辑：先过滤同性角色，再取前MAX_ALIAS_CHECK_CHARACTERS（50）个
  var filteredRecords = this.characterRecords.filter(function(record) {
      // 兼容原有逻辑：未传递性别/角色无性别 → 不过滤
      if (!targetGender || !record.gender) return true;
      // 仅保留与目标性别相同的角色（去空格避免匹配误差）
      return record.gender.trim() === targetGender.trim();
  });

  // 截取过滤后的前50个角色（确保不超过限制）
  var apiLimitedRecords = filteredRecords.slice(0, MAX_ALIAS_CHECK_CHARACTERS);
//  //console.log("【API别名校验】仅提取前" + MAX_ALIAS_CHECK_CHARACTERS + "个同性角色（目标性别：" + (targetGender || "无") + "），实际有效：" + apiLimitedRecords.length + "个");

  // 遍历“过滤后+截取后”的角色记录，提取主名和别名
  for (var i = 0; i < apiLimitedRecords.length; i++) {
      var record = apiLimitedRecords[i];
      if (!record) continue;
      var mainName = record.name.trim();
      if (!mainName) continue;

      // 1. 添加主名（去重）
      allNamesSet.add(mainName);
      nameMap[mainName] = mainName;

      // 2. 添加别名（去重，且不与主名重复）
      if (record.aliases && record.aliases.trim()) {
          var aliasList = record.aliases.split("|")
              .map(alias => alias.trim())
              .filter(alias => alias && alias !== mainName); // 排除与主名相同的别名
          for (var j = 0; j < aliasList.length; j++) {
              var alias = aliasList[j];
              allNamesSet.add(alias);
              nameMap[alias] = mainName; // 别名关联到主名
          }
      }
  }

  // 3. 转换为API要求的格式：[{name:"XXX"},{name:"XXX"}]（无重复，符合JSON规范）
  var nameListForApi = Array.from(allNamesSet).map(name => ({ name: name }));
  // 4. 保留原映射关系（用于后续别名匹配逻辑，不传给API）
  this.nameToMainNameMap = nameMap;

  //console.log("【传给API的角色列表】共" + nameListForApi.length + "个（主名+别名），列表预览：" + JSON.stringify(nameListForApi.slice(0, 5)) + "...");
  return nameListForApi;
};


CharacterManager.prototype.checkAliasByApi = function(newName, chapterFullContent, newCharacterGender, currentDialogueText) {
  
  // 同步延时函数（和姓名分析逻辑对齐）
  function sleep(ms) {
    var start = Date.now();
    while (Date.now() - start < ms) {}
  }
  // ========== 原有基础参数校验完全保留 ==========
  chapterFullContent = chapterFullContent || "";
  newName = newName || "";
  
  if (!newName || newName.trim() === "" || !chapterFullContent || chapterFullContent.trim() === "") {
    return null;
  }
  // 原有本地角色列表获取逻辑完全保留
  var nameListForApi = this.getAllCharacterNamesAndAliases(newCharacterGender);
  if (nameListForApi.length === 0) {
    return null;
  }  if (this.updateAliasGraphsFromCache) {
    try {
      var graphCache = readDialogCache();
      this.updateAliasGraphsFromCache(graphCache.dialogList || [], chapterFullContent || "", []);
    } catch (graphCacheErr) {}
  }
  graphRemoteSetChapter(graphBuildChapterKey(chapterFullContent || this.contextHistory2 || ""), "别名校验");
  try { graphAliasRecentChapterAppend(graphCurrentChapterId()); graphAliasRecentChapterSave(); } catch(aliasRecentErr1) {}
  var graphEvidenceHint = this.buildAliasEvidenceHint ? this.buildAliasEvidenceHint(newName, chapterFullContent, currentDialogueText, newCharacterGender, "") : "";
  var recentChapterHint = this.buildAliasRecentChapterHint ? this.buildAliasRecentChapterHint(newName, newCharacterGender, nameListForApi) : "";
  var aliasPendingRelations = this.getPendingNameSemanticRelationsForAliasCheck ? this.getPendingNameSemanticRelationsForAliasCheck(chapterFullContent, newName) : [];
  var aliasPendingAuditBlock = this.buildAliasCheckRelationAuditBlock ? this.buildAliasCheckRelationAuditBlock(aliasPendingRelations, newName, chapterFullContent) : "";
  var aliasPendingVoiceAge = this.getPendingVoiceAgeEvidenceForCombinedAuditV908 ? this.getPendingVoiceAgeEvidenceForCombinedAuditV908() : [];
  var aliasPendingVoiceAgeBlock = this.buildCombinedVoiceAgeAuditBlockV908 ? this.buildCombinedVoiceAgeAuditBlockV908(aliasPendingVoiceAge, { omitSourceText: true }) : "";
  var aliasPendingAgeState = this.pendingVoiceAgeEvidence || {};
  var aliasPendingGraphState = this.pendingNameSemanticRelations || {};
  var aliasSharedCurrentText = String(aliasPendingAgeState.currentText || aliasPendingGraphState.chapterText || chapterFullContent || "");
  var aliasSharedPreviousText = String(aliasPendingAgeState.previousText || "");
  var aliasCombinedSharedSourceBlock = (aliasPendingAuditBlock || aliasPendingVoiceAgeBlock) ? graphV908BuildCombinedSharedSourceBlock(aliasSharedPreviousText, aliasSharedCurrentText) : "";
  var aliasDirectContextText = String(this.contextHistory2 || '') + String(text2 || '') + String(next100Chars || '');
  var aliasContextForPrompt = aliasCombinedSharedSourceBlock ? "使用本请求后附的【共享紧邻上文】和【共享当前批文本】，不要把同一批正文重复读取两遍。" : aliasDirectContextText;
  if (aliasPendingRelations && aliasPendingRelations.length) {
    aliasShortLog("附带审计 " + aliasPendingRelations.length + " 条证据");
    graphRemoteLog("alias_check_with_relation_audit", { newName: graphNormalizeName(newName), relationCount: aliasPendingRelations.length, relationIds: aliasPendingRelations.map(function(r){ return r.relationId || ""; }).slice(0, 30) });
  }
  graphRemoteLog("alias_check_start", { newName: newName.trim(), mode: "strict", localRoleCount: nameListForApi.length, hasGraphHint: !!graphEvidenceHint, hasRecentChapterHint: !!recentChapterHint, recentRange: ALIAS_RECENT_CHAPTER_RANGE, relationAuditCount: aliasPendingRelations ? aliasPendingRelations.length : 0, voiceAgeAuditCount: aliasPendingVoiceAge ? aliasPendingVoiceAge.length : 0, combinedFlow: "alias+voice_age+graph" });
  aliasShortLog("校验 " + newName.trim());
  
  // 别名校验固定采用严谨模式
  var prompt = "你是专业的小说人物别名识别AI。你的唯一任务是：基于提供的小说上下文，判断【新名字】是否应归入【本地已存角色列表】中某一个稳定朗读角色记录。\n\n" +
       "【核心定义】\n" +
       "本任务判断的是两个称呼是否应归入同一个稳定朗读角色记录。通常要求它们是同一个人物的不同称呼；若文本明确说明某个傀儡、空壳、化身或载体本身没有独立意识、独立人格和独立说话身份，并由同一控制意识持续占据或显化，朗读角色连续性可以跟随该控制意识。若宿主原本具有独立意识、独立身份或独立言行，则附身者与宿主仍是不同角色。当前由谁发声与是否归入同一稳定朗读角色记录必须分别判断。\n" +
       "只有小说上下文存在明确、直接、无歧义的证据时，才能判定应归入同一稳定朗读角色；证据不足或存在其他合理解释时必须判定为非别名。\n\n" +
       getV908CharacterNamingAndSpeakerRules("alias_check") +
       "【强制判断步骤】\n" +
       "1. 确定判断对象：本次判断的唯一对象是【新名字】，它是当前小说对话里的说话人。\n" +
       "2. 限定匹配范围：仅在【本地已存角色列表】中匹配，列表内所有角色均与【新名字】性别一致。\n" +
       "3. 执行证据审查：必须在小说上下文中找到以下至少一种明确证据，缺一不可：\n" +
       "   a) 其他角色明确使用两个名字称呼同一人物，且有上下文连贯性\n" +
       "   b) 小说旁白/叙述明确说明两个名字指代同一人\n" +
       "   c) 人物身份特征（职位、关系、外貌、行为）完全一致且有文本支撑\n" +
       "   d) 对话中明确的人物指代关系（如「A对B说：C如何如何」，且上下文证明C即D）\n" +
       "4. 执行排他性检查：确认新名字不可能指代列表中的其他人物，也不可能是一个全新人物。\n" +
       "5. 执行一致性检查：确认新名字与匹配到的主名在人物关系、立场、行为逻辑上完全一致。\n\n" +
       "【绝对禁止判定】\n" +
       "以下情况必须判定为非别名（isAlias: false）：\n" +
       "1. 仅名字读音、字形、字数相近，无上下文明确指代关系\n" +
       "   示例：新名字「张三」，列表有「张山」「张叁」，无上下文明确说明，一律非别名\n" +
       "2. 可能是但不确定的情况，一律非别名\n" +
       "   示例：新名字「李总」，列表有「李明」，虽然都姓李且可能是总经理，但无上下文明确说明「李总=李明」，一律非别名\n" +
       "3. 指代不同人物的关系称呼\n" +
       "   示例：新名字「族长夫人」，列表有「族长」，明显是两个人物，非别名\n" +
       "4. 列表外的人物，一律非别名\n" +
       "5. 存在任何歧义、可能指代多人、或无法100%确认的情况，一律非别名\n\n" +
       "【正确判定标准】\n" +
       "必须同时满足：\n" +
       "- 小说上下文中有明确文本证据\n" +
       "- 证据直接证明两个名字为同一人\n" +
       "- 无任何其他解释可能性\n" +
       "- 人物身份、关系、行为完全一致\n\n" +
       "正确示例1：新名字「建国」，列表有「李建国」，上下文明确「李建国，小名建国」「建国，全名李建国」，判定为别名\n" +
       "正确示例2：新名字「李总」，列表有「李建国」，上下文明确「公司总经理李建国」「李总走了进来，李建国关上门」，判定为别名\n" +
       "错误示例1：新名字「小李」，列表有「李明」「李华」，上下文仅显示「小李来了」，无法确定是李明还是李华，非别名\n" +
       "错误示例2：新名字「王局」，列表有「王建国」，虽都姓王且可能是局长，但无上下文明确「王局=王建国」，非别名\n\n";
  // 公共输入信息和输出要求
  prompt += "【输入信息】\n" +
     "【本地已存角色列表】\n" + JSON.stringify(nameListForApi, null, 2) + "\n\n" +
     "【图谱与共现提示】\n" + (graphEvidenceHint ? graphEvidenceHint : "暂无有效图谱证据") + "\n\n" +
     "【最近N章三维辅助数据】\n" + (recentChapterHint ? recentChapterHint : "暂无最近N章辅助数据") + "\n\n" +
     "【最近N章辅助使用规则】\n" +
     "1. 本版不向模型输出最近N章角色列表；最近N章辅助只包含正图谱、反图谱、共现统计。\n" +
     "2. 最近N章正图谱可增强同人判断，但仍需要结合当前上下文或明确证据；如果正边来源只是师徒、亲属、主从、职场、组织、恋爱、敌友、同伴、同事、同学等关系/身份描述，应降低可信度，不能单独作为同人别名依据；\n" +
     "3. 最近N章反图谱/直接互动/并列/关系证据优先用于阻止误合并；\n" +
     "4. 共现统计是命中过最近章节的历史累计辅助，不能把累计次数误认为全发生在最近N章。\n\n" +
     "【待分析小说上下文】\n" + aliasContextForPrompt + "\n\n" +
     "【新名字】是\n" + newName.trim() + "\n" +
     "---\n" +
     "【输出要求】\n" +
     "1. 仅输出标准JSON格式，无任何多余内容\n" +
     "2. 包含3个必填字段：\n" +
     "    - isAlias：布尔值，true=是别名，false=非别名\n" +
     "    - mainName：字符串，是别名则返回列表中对应的主名，非别名则返回null\n" +
     "    - reason：字符串，是别名时描述判断依据，非别名时填写null\n" +
     "3. 判定为别名时，必须在reason中简要说明核心判断依据；reason要给正文锚点，不要只写模型总结句\n" +
     "4. 可选输出 graphAuditSuggestions：只在判断当前新名字时顺手发现图谱/最近N章/复合数据与当前正文强证直接冲突时填写，最多2条；没有明显问题必须返回空数组或省略。它只是候选建议，不代表最终修改。\n" +
     "5. 如果输入中附带【当前批次新证据审计任务】，必须同时输出完整审计结构auditComplete/allAccepted/acceptedAll/downgrade/reject/verify；别名校验主结果不完整则本次失败，审计结构不完整则别名结果可接收但审计会被单独重审。\n" +
     "6. 必须100%确定才能判定为别名，有任何不确定=非别名\n\n" +
     "【可选图谱审计】不主动审计全图，不基于感觉找错；只允许输出与当前newName/mainName相关的明显冲突，例如图谱A≠B但正文出现A自称B，或图谱A=B但正文出现直接对话/并列/关系强反证。\n\n" +
     "【输出格式】\n" +
     "{\n" +
     '  \"isAlias\": true/false,\n' +
     '  \"mainName\": \"列表中的主名\" 或 null,\n' +
     '  \"reason\": \"判断依据描述\" 或 null,\n' +
     '  \"graphAuditSuggestions\": []\n' +
     "}";
  // 保留不含年龄/图谱任务的别名提示，单独别名降级时直接复用，避免再次携带已保留的两方结果。
  var aliasStandalonePrompt = aliasCombinedSharedSourceBlock ? prompt.replace(aliasContextForPrompt, aliasDirectContextText) : prompt;
  if (aliasCombinedSharedSourceBlock) {
    prompt += "\n\n" + aliasCombinedSharedSourceBlock;
  }
  if (aliasPendingAuditBlock) {
    prompt += "\n\n" + aliasPendingAuditBlock;
  }
  if (aliasPendingVoiceAgeBlock) {
    prompt += "\n\n" + aliasPendingVoiceAgeBlock;
  }
  if (aliasPendingAuditBlock || aliasPendingVoiceAgeBlock) {
    prompt += "\n\n【合并审计模块独立输出规则】\n" +
      "别名结果仍使用顶层isAlias/mainName/reason；图谱证据审计仍使用顶层auditComplete/allAccepted/acceptedAll/downgrade/reject/verify；发声音龄审计必须放入voiceAgeAudit对象。三个模块分别完成，禁止因为一方拒收就省略另一方。\n" +
      '发声音龄必须使用voiceAgeAudit稀疏结构：返回candidateCount、candidateSetId、allAccepted、acceptedAll、acceptedChanges、allAcceptedVerification、downgrade、reject、verify；全部采纳只返回["__ALL__"]，混合结果只列异常evidenceId；会永久跨年龄段换声的采纳项必须逐条写入acceptedChanges。';
  }

  // ========== 原有变量完全保留 ==========
  var finalResult = null;
  var maxRetryRound = (typeof graphV908CombinedMaxRetryRound === "function") ? graphV908CombinedMaxRetryRound() : Math.max(1, Math.ceil(CHARACTER_ANALYZE_RETRY_MAX / Math.max(1, parseInt(bingfa, 10) || 1)));
  var currentRound = 0;
  var requestTimeout = ALIAS_ANALYZE_TIMEOUT;
  // 构建请求参数（与原逻辑100%一致）
  function buildAliasAnalyzeRequest(apiConfig) {
    var requestData = {
      model: apiConfig.model,
      messages: [
        { role: "system", content: "严格遵守格式要求，仅输出JSON，格式错误则任务失败" },
        { role: "user", content: prompt }
      ],
      temperature: 0.1
    };
    var headers = {
      "Content-Type": "application/json",
      "Authorization": "Bearer " + apiConfig.key,
      "Connection": "keep-alive",
      "Timeout": requestTimeout.toString()
    };
    if (ENABLE_ALIAS_RAW_REMOTE_LOG) {
      graphRemoteLog("alias_llm_raw_request", {
        scene: "alias_check",
        newName: graphNormalizeName(newName),
        mode: "strict",
        endpoint: graphSafeString(apiConfig.endpoint || "", 200),
        model: graphSafeString(apiConfig.model || "", 80),
        requestData: graphSafeString(JSON.stringify(requestData), ALIAS_RAW_REMOTE_LOG_MAX_LEN)
      });
    }
    graphRemoteLog("combined_audit_raw_request", { flow: "alias+voice_age+graph", newName: graphNormalizeName(newName), endpoint: graphSafeString(apiConfig.endpoint || "", 200), model: graphSafeString(apiConfig.model || "", 80), requestData: graphSafeString(JSON.stringify(requestData), MODEL_RAW_REMOTE_LOG_MAX_LEN) });
    return {
      endpoint: apiConfig.endpoint,
      data: requestData,
      headers: headers
    };
  }
  // 响应解析与格式校验（与原逻辑100%一致）
  function parseAliasAnalyzeResponse(response) {
    var responseBody = String(response.body().string() || "{}");
    if (ENABLE_ALIAS_RAW_REMOTE_LOG) {
      graphRemoteLog("alias_llm_raw_response", { scene: "alias_check", newName: graphNormalizeName(newName), mode: "strict", responseBody: graphSafeString(responseBody, ALIAS_RAW_REMOTE_LOG_MAX_LEN) });
    }
    graphRemoteLog("combined_audit_raw_response", { flow: "alias+voice_age+graph", newName: graphNormalizeName(newName), responseBody: graphSafeString(responseBody, MODEL_RAW_REMOTE_LOG_MAX_LEN) });
    var apiOuterResponse = JSON.parse(responseBody);
    if (!apiOuterResponse.choices || !apiOuterResponse.choices[0] || !apiOuterResponse.choices[0].message) {
      throw new Error("API响应格式错误：缺少\"choices[0].message\"字段");
    }
    var actualResultContent = apiOuterResponse.choices[0].message.content.trim();
    var cleanJson = actualResultContent.replace(/```json|```/g, "").trim();
    var apiResult = JSON.parse(cleanJson);
    // 这里只校验外层JSON可解析。三个模块的字段完整性由统一路由分别判断，才能保留其中完整的两方结果。
    return apiResult;
  }

  // ========== 两项及以上不完整才整包重试；只有一项不完整时保留另外两项 ==========
  var combinedStatus = null;
  while (currentRound < maxRetryRound && !finalResult) {
    currentRound++;
    graphRemoteLog("combined_audit_request", { flow: "alias+voice_age+graph", round: currentRound, maxRetryRound: maxRetryRound, required: { alias: true, voiceAge: aliasPendingVoiceAge.length > 0, graph: aliasPendingRelations.length > 0 }, newName: graphNormalizeName(newName) });
    var concurrentResult = concurrentApiRequest(
      "aliasAnalyze",
      buildAliasAnalyzeRequest,
      parseAliasAnalyzeResponse,
      null,
      requestTimeout
    );
    if (concurrentResult.success) {
      var selectedCombined = graphV908SelectBestCombinedResult(concurrentResult, true, aliasPendingVoiceAge, aliasPendingRelations);
      combinedStatus = selectedCombined.status;
      graphRemoteLog("combined_audit_module_status", graphV908CombinedStatusLogPayload("alias+voice_age+graph", combinedStatus, currentRound, newName));
      if (combinedStatus.incompleteCount >= 2) {
        graphRemoteLog("combined_audit_multi_incomplete_rejected", graphV908CombinedStatusLogPayload("alias+voice_age+graph", combinedStatus, currentRound, newName));
        if (currentRound < maxRetryRound) {
          graphRemoteLog("combined_audit_bundle_retry", { flow: "alias+voice_age+graph", round: currentRound, nextRound: currentRound + 1, incompleteModules: combinedStatus.incompleteNames.slice(0) });
          sleep(250);
        }
      } else {
        finalResult = selectedCombined.raw || {};
      }
    } else {
      if (currentRound < maxRetryRound) {
        sleep(250);
      }
    }
  }

  if (!combinedStatus) combinedStatus = graphV908BuildCombinedAuditStatus(true, aliasPendingVoiceAge, aliasPendingRelations, finalResult || {});
  if (!finalResult && combinedStatus.incompleteCount >= 2) graphRemoteLog("combined_audit_retry_exhausted", graphV908CombinedStatusLogPayload("alias+voice_age+graph", combinedStatus, currentRound, newName));
  var combinedResolution = this.resolveCombinedAuditFallbacksV908 ? this.resolveCombinedAuditFallbacksV908({
    flow: "alias+voice_age+graph",
    status: combinedStatus,
    raw: finalResult || {},
    aliasStandalonePrompt: aliasStandalonePrompt,
    aliasContext: { newName: newName },
    voiceAgeCandidates: aliasPendingVoiceAge,
    relations: aliasPendingRelations,
    chapterText: chapterFullContent
  }) : null;
  finalResult = combinedResolution && combinedResolution.alias && combinedResolution.alias.complete ? combinedResolution.alias.result : { isAlias: false, mainName: null, reason: "别名模块不完整或重试失败，安全判定为非别名" };
  if (finalResult && finalResult.isAlias && finalResult.mainName) {
    var aliasBlockReason = graphAliasMergeBlockReason(newName, finalResult.mainName);
    if (aliasBlockReason) {
      aliasShortLog("\u5408\u5e76\u62e6\u622a " + graphNormalizeName(newName) + "\u2192" + graphNormalizeName(finalResult.mainName));
      graphRemoteLog("alias_merge_blocked", { stage: "alias_check", newName: graphNormalizeName(newName), mainName: graphNormalizeName(finalResult.mainName), reason: aliasBlockReason });
      finalResult = { isAlias: false, mainName: null, reason: aliasBlockReason };
    }
  }
  var aliasReasonContradiction = graphAliasCheckReasonContradiction(finalResult, newName, finalResult && finalResult.mainName);
  if (aliasReasonContradiction) {
    graphRemoteLog("alias_check_inconsistent_result", { newName: graphNormalizeName(newName), mainName: graphNormalizeName(finalResult && finalResult.mainName || ""), reason: graphSafeString(finalResult && finalResult.reason || "", 260), fixReason: aliasReasonContradiction });
    finalResult = { isAlias: false, mainName: null, reason: aliasReasonContradiction + "；原reason=" + graphSafeString(finalResult && finalResult.reason || "", 160) };
  }
  if (finalResult && finalResult.isAlias && finalResult.mainName && this.directPairEvidenceGate) {
    var aliasGateContext = String(chapterFullContent || "") + "\n" + String(currentDialogueText || "") + "\n" + String(this.contextHistory2 || "");
    var aliasGate = this.directPairEvidenceGate(newName, finalResult.mainName, finalResult.reason || "", aliasGateContext, "alias_check");
    if (!aliasGate.allow) {
      if (aliasGate.needVerify && this.verifyGraphConflictAndFix) {
        graphRemoteLog("alias_gate_to_conflict_verify", { newName: graphNormalizeName(newName), mainName: graphNormalizeName(finalResult.mainName), reason: graphSafeString(finalResult.reason || "", 280), gateReason: graphSafeString(aliasGate.reason || "", 180), tier: aliasGate.tier || "B" });
        var aliasVerify = this.verifyGraphConflictAndFix("positive", newName, finalResult.mainName, 3.5, "alias_check_positive", finalResult.reason || "", "alias_gate_to_conflict_verify", { defaultAllow: false, forceVerify: true, contextText: aliasGateContext, originalSourceReason: "alias_check_positive", originalEvidenceText: finalResult.reason || "" });
        if (!aliasVerify.allow) finalResult = { isAlias: false, mainName: null, reason: "direct-pair evidence gate verify not passed：" + graphSafeString(aliasVerify.reason || aliasGate.reason || "", 160) + "；原reason=" + graphSafeString(finalResult.reason || "", 160) };
      } else {
        graphRemoteLog("alias_bridge_gate_blocked", { newName: graphNormalizeName(newName), mainName: graphNormalizeName(finalResult.mainName), reason: graphSafeString(finalResult.reason || "", 280), gateReason: graphSafeString(aliasGate.reason || "", 180), directInContext: !!aliasGate.directInContext, directInReason: !!aliasGate.directInReason, bridgeRisk: !!aliasGate.bridgeRisk, tier: aliasGate.tier || "C" });
        finalResult = { isAlias: false, mainName: null, reason: "direct-pair evidence gate blocked：" + graphSafeString(aliasGate.reason || "", 160) + "；原reason=" + graphSafeString(finalResult.reason || "", 160) };
      }
    }
  }
  // 先提交最终别名结论，再提交已缓冲的图谱审计；年龄结果只缓冲，等角色recordId稳定后再提交和应用。
  if (this.traceCombinedAuditCommitV908) this.traceCombinedAuditCommitV908("alias", { flow: "alias+voice_age+graph", newName: graphNormalizeName(newName), isAlias: !!finalResult.isAlias, mainName: graphNormalizeName(finalResult.mainName || ""), complete: !!(combinedResolution && combinedResolution.alias && combinedResolution.alias.complete) });
  if (this.bufferPendingGraphAuditV908) this.bufferPendingGraphAuditV908(combinedResolution && combinedResolution.graph, combinedResolution && combinedResolution.graph && combinedResolution.graph.auditSource || "alias_combined_audit");
  if (this.bufferPendingVoiceAgeAuditV908) this.bufferPendingVoiceAgeAuditV908(combinedResolution && combinedResolution.voiceAge, combinedResolution && combinedResolution.voiceAge && combinedResolution.voiceAge.auditSource || "alias_combined_audit");
  if (this.logAliasCheckFlow) this.logAliasCheckFlow(newName, finalResult, graphEvidenceHint, recentChapterHint);
  return finalResult;
};


// ===================== 别名清洗局部三维辅助（只围绕主名、旧别名、新名字）=====================
CharacterManager.prototype.buildAliasRefineGraphHint = function(aliasCandidates) {
  if (!ENABLE_ALIAS_REFINE_GRAPH_HINT || !ENABLE_ALIAS_GRAPH) return "";
  if (!aliasCandidates || !aliasCandidates.length) return "";
  var names = [];
  var seen = {};
  for (var i = 0; i < aliasCandidates.length; i++) {
    var nm = graphNormalizeName(aliasCandidates[i]);
    if (!nm || graphIsInvalidName(nm) || seen[nm]) continue;
    seen[nm] = true;
    names.push(nm);
  }
  if (names.length < 2) return "";

  var posLines = [];
  var negLines = [];
  var compoundLines = [];
  var coLines = [];
  var pairLimit = parseInt(ALIAS_REFINE_GRAPH_HINT_PAIR_LIMIT, 10) || 30;
  var evidenceMaxLen = parseInt(ALIAS_REFINE_GRAPH_HINT_EVIDENCE_MAX_LEN, 10) || 220;

  function reasonText(edge) {
    if (!edge) return "";
    return (edge.reasons || []).join("|") + (edge.extra ? "，证据:" + graphSafeString(edge.extra, evidenceMaxLen) : "");
  }
  function cooccurEvidenceText(st) {
    var evText = "";
    if (st && st.evidence && st.evidence.length) {
      var ev = graphFilterRecentEvidence(st.evidence, null, 2);
      var arr = [];
      for (var e = 0; e < ev.length; e++) {
        arr.push("[" + (ev[e].chapter || "") + "/" + (ev[e].kind || "") + "]" + graphSafeString(ev[e].text || "", evidenceMaxLen));
      }
      if (arr.length) evText = "，证据:" + arr.join(" || ");
    }
    return evText;
  }

  for (var a = 0; a < names.length; a++) {
    for (var b = a + 1; b < names.length; b++) {
      if (posLines.length + negLines.length + coLines.length >= pairLimit) break;
      var n1 = names[a], n2 = names[b];
      var pe = graphGetEdgeSnapshot(this.aliasPositiveGraph, n1, n2);
      var ne = graphGetEdgeSnapshot(this.aliasNegativeGraph, n1, n2);
      var st = this.aliasCooccurStats ? this.aliasCooccurStats[graphPairKey(n1, n2)] : null;
      if (pe && Number(pe.score || 0) > 0) {
        var peLine = "- " + n1 + " = " + n2 + "：分" + Number(pe.score || 0).toFixed(1) + "，次" + Number(pe.count || 0) + "，章" + (pe.chapters || []).join("|") + "，因" + reasonText(pe);
        posLines.push(peLine);
        if (graphReasonStartsWith(pe.reasons || [], "compound_")) compoundLines.push(peLine);
      }
      if (ne && Number(ne.score || 0) > 0) {
        var neLine = "- " + n1 + " ≠ " + n2 + "：分" + Number(ne.score || 0).toFixed(1) + "，次" + Number(ne.count || 0) + "，章" + (ne.chapters || []).join("|") + "，因" + reasonText(ne);
        negLines.push(neLine);
        if (graphReasonStartsWith(ne.reasons || [], "compound_")) compoundLines.push(neLine);
      }
      if (st && (Number(st.sameSentence || 0) > 0 || Number(st.adjacentSpeaker || 0) > 0 || Number(st.directInteraction || 0) > 0 || Number(st.listedTogether || 0) > 0 || Number(st.explicitRelation || 0) > 0 || Number(st.modelPositive || 0) > 0 || Number(st.modelNegative || 0) > 0 || Number(st.localHighPrecisionNegative || 0) > 0 || Number(st.positiveMention || 0) > 0 || Number(st.compoundPositive || 0) > 0 || Number(st.compoundNegative || 0) > 0)) {
        coLines.push("- " + n1 + " & " + n2 + "：同章" + Number(st.chapterCount || 0) + "，同句" + Number(st.sameSentence || 0) + "，相邻" + Number(st.adjacentSpeaker || 0) + "，直接互动" + Number(st.directInteraction || 0) + "，并列" + Number(st.listedTogether || 0) + "，关系" + Number(st.explicitRelation || 0) + "，模型正" + Number(st.modelPositive || 0) + "，模型反" + Number(st.modelNegative || 0) + "，本地正" + Number(st.positiveMention || 0) + "，本地反" + Number(st.localHighPrecisionNegative || 0) + "，复合正" + Number(st.compoundPositive || 0) + "，复合反" + Number(st.compoundNegative || 0) + cooccurEvidenceText(st));
      }
    }
  }

  if (posLines.length === 0 && negLines.length === 0 && compoundLines.length === 0 && coLines.length === 0) return "";
  var lines = [];
  lines.push("以下为别名清洗阶段的局部三维辅助，只围绕当前主名、旧别名和新名字；用于清洗错误别名，不扩大合并候选范围。");
  lines.push("【清洗用正图谱】");
  lines.push(posLines.length ? posLines.slice(0, pairLimit).join("\n") : "暂无局部正图谱证据");
  lines.push("【清洗用反图谱】");
  lines.push(negLines.length ? negLines.slice(0, pairLimit).join("\n") : "暂无局部反图谱证据");
  lines.push("【局部复合图谱证据】");
  lines.push(compoundLines.length ? compoundLines.slice(0, pairLimit).join("\n") : "暂无局部复合图谱证据");
  if (compoundLines.length) graphRemoteLog("alias_refine_compound_hint", { names: names, count: compoundLines.length, lines: graphSafeString(compoundLines.join("\n"), 3000) });
  lines.push("【清洗用共现统计】");
  lines.push(coLines.length ? coLines.slice(0, pairLimit).join("\n") : "暂无局部共现统计");
  var hint = lines.join("\n");
  graphRemoteLog("alias_refine_graph_hint", { names: names, positiveCount: posLines.length, negativeCount: negLines.length, compoundCount: compoundLines.length, cooccurCount: coLines.length, hint: graphSafeString(hint, 4000) });
  return hint;
};

// ===================== 新增：别名清洗API（主名+现有别名+新别名，清洗无关别名）=====================
CharacterManager.prototype.refineAliasGroupByApi = function(mainRecord, newName, chapterFullContent, currentDialogueText) {
  // 同步延时函数（和现有别名分析逻辑保持一致）
  function sleep(ms) {
    var start = Date.now();
    while (Date.now() - start < ms) {}
  }

  if (!mainRecord || !mainRecord.name) return null;

  newName = graphNormalizeStateAliasName((newName || "").trim());
  chapterFullContent = chapterFullContent || "";
  currentDialogueText = currentDialogueText || "";

  if (!newName) return null;

  var mainName = mainRecord.name.trim();
  var currentAliases = [];

  if (mainRecord.aliases && mainRecord.aliases.trim()) {
    currentAliases = mainRecord.aliases.split("|")
      .map(function(alias) { return alias.trim(); })
      .filter(function(alias) { return alias !== ""; });
  }

  // 保证主名一定在候选列表里
  if (currentAliases.indexOf(mainName) === -1) {
    currentAliases.unshift(mainName);
  }

  // 构建去重后的“主名+旧别名+新名字”
  var aliasCandidates = [];
  var seenAliasMap = {};
  for (var i = 0; i < currentAliases.length; i++) {
    var oldAlias = currentAliases[i];
    if (oldAlias && !seenAliasMap[oldAlias]) {
      seenAliasMap[oldAlias] = true;
      aliasCandidates.push(oldAlias);
    }
  }
  if (newName && !seenAliasMap[newName]) {
    seenAliasMap[newName] = true;
    aliasCandidates.push(newName);
  }

  var refineGraphHint = this.buildAliasRefineGraphHint ? this.buildAliasRefineGraphHint(aliasCandidates) : "";

  var prompt =
    "你是专业的小说人物别名清洗AI。你的任务是：已知【新名字】已经初步判断可能归属于【主名】，现在请继续判断【主名+现有别名列表+新名字】中，哪些名字真正属于同一个人物，哪些是历史误判留下的无关别名。\n\n" +
    "【任务目标】\n" +
    "1. 确认【新名字】是否确实属于【主名】对应的人物；\n" +
    "2. 清洗【现有别名列表】中与【主名】无关的错误别名；\n" +
    "3. 输出最终应该保留的别名列表，以及应剔除的无关别名；\n" +
    "4. 主名必须是同一个稳定朗读角色的真正核心名称；若上下文明确显示主名本身选错，也可以重新指定更合适的主名。\n\n" +
    getV908CharacterNamingAndSpeakerRules("alias_refine") +
    "【判断标准】\n" +
    "1. 只有在上下文中明确或高度确定指向同一具体人物的名字，才能保留为 confirmedAliases；\n" +
    "2. 如果某个别名明显属于其他人物、身份称呼不稳定、仅偶然被误判关联、或上下文无法支撑其属于主名，则加入 removedAliases；\n" +
    "3. 不要因为名字相似、姓氏相同、身份相近就随意保留；\n" +
    "4. 若【新名字】实际上并不属于该主名人物，则 isSamePerson=false；\n" +
    "5. 如果主名本身应更换，请返回新的 mainName，并让 confirmedAliases 围绕新的 mainName 组织。\n" +
    "6. 清洗时若正图谱中的正边来源只是关系/身份描述（如某人的弟子、下人、秘书、父亲、朋友等），不得单独把它当作保留别名的依据；必须结合当前正文中的同一性证据复核，关系描述也可作为排除误合并的反证参考。\n" +
    "7. 姓氏/称号+人物称呼（如某道友、某兄、某前辈、某长老、某掌柜、某先生、某小姐、某夫人、某公子、某姑娘、某队长、某老板、某医生、某警官等）如果上下文唯一指向同一人物，应作为朗读角色可用别名保留，不要仅因其是礼貌称呼就剔除；但某人的弟子/下人/秘书/父亲/朋友等关系身份描述仍不得当作同人别名。\n\n" +
    "【清洗用局部三维辅助】\n" + (refineGraphHint ? refineGraphHint : "暂无局部图谱/共现证据") + "\n\n" +
    "【输入信息】\n" +
    "【当前主名】\n" + mainName + "\n\n" +
    "【当前主名及别名列表】\n" + aliasCandidates.join("|") + "\n\n" +
    "【新名字】\n" + newName + "\n\n" +
    "【当前对话】\n" + currentDialogueText + "\n\n" +
    "【小说上下文】\n" + String(this.contextHistory2 || '').slice(-1000) + text2 + String(next100Chars || '').slice(0, 500) + "\n\n" +
    "【输出要求】\n" +
    "1. 仅输出JSON，不要输出解释文字；\n" +
    "2. 必须包含以下字段：\n" +
    "   - isSamePerson: 布尔值，true=新名字属于该主名人物，false=不属于\n" +
    "   - mainName: 字符串或null，最终确认的主名\n" +
    "   - confirmedAliases: 数组，最终确认属于该主名人物的别名列表（不必重复放主名）\n" +
    "   - removedAliases: 数组，应从旧别名中剔除的无关别名\n" +
    "   - reason: 字符串或null，简要说明依据；必须给正文锚点，不要只写模型总结句\n" +
    "   - graphAuditSuggestions: 可选数组，只在清洗当前主名/别名组时顺手发现图谱或最近N章数据与当前正文强证直接冲突时填写，最多2条；没有明显问题返回空数组或省略。\n\n" +
    "【可选图谱审计】不主动审计全图，不鼓励泛找错；只允许输出与当前mainName、newName、confirmedAliases、removedAliases相关的明显冲突建议，最终是否修复必须交给graph_conflict_verify。\n\n" +
    "【输出格式】\n" +
    "{\n" +
    '  "isSamePerson": true/false,\n' +
    '  "mainName": "最终主名" 或 null,\n' +
    '  "confirmedAliases": ["别名1","别名2"],\n' +
    '  "removedAliases": ["错误别名1","错误别名2"],\n' +
    '  "reason": "简要说明" 或 null,\n' +
    '  "graphAuditSuggestions": []\n' +
    "}";

  var finalResult = null;
  var maxRetryRound = Math.ceil(CHARACTER_ANALYZE_RETRY_MAX / bingfa);
  var currentRound = 0;
  var requestTimeout = ALIAS_ANALYZE_TIMEOUT;

  // 构建请求
  function buildAliasRefineRequest(apiConfig) {
    var requestData = {
      model: apiConfig.model,
      messages: [
        { role: "system", content: "严格遵守格式要求，仅输出JSON，格式错误则任务失败" },
        { role: "user", content: prompt }
      ],
      temperature: 0.1
    };
    var headers = {
      "Content-Type": "application/json",
      "Authorization": "Bearer " + apiConfig.key,
      "Connection": "keep-alive",
      "Timeout": requestTimeout.toString()
    };
    if (ENABLE_MODEL_RAW_REMOTE_LOG) {
      graphRemoteLog("alias_refine_llm_raw_request", {
        scene: "alias_refine",
        mainName: graphNormalizeName(mainName),
        newName: graphNormalizeName(newName),
        aliasCandidateCount: aliasCandidates.length,
        hasRefineGraphHint: !!refineGraphHint,
        endpoint: graphSafeString(apiConfig.endpoint || "", 200),
        model: graphSafeString(apiConfig.model || "", 80),
        requestData: graphSafeString(JSON.stringify(requestData), MODEL_RAW_REMOTE_LOG_MAX_LEN)
      });
    }
    return {
      endpoint: apiConfig.endpoint,
      data: requestData,
      headers: headers
    };
  }

  // 解析响应
  function parseAliasRefineResponse(response) {
    var responseBody = String(response.body().string() || "{}");
    if (ENABLE_MODEL_RAW_REMOTE_LOG) {
      graphRemoteLog("alias_refine_llm_raw_response", {
        scene: "alias_refine",
        mainName: graphNormalizeName(mainName),
        newName: graphNormalizeName(newName),
        aliasCandidateCount: aliasCandidates.length,
        hasRefineGraphHint: !!refineGraphHint,
        responseBody: graphSafeString(responseBody, MODEL_RAW_REMOTE_LOG_MAX_LEN)
      });
    }
    var apiOuterResponse = JSON.parse(responseBody);

    if (!apiOuterResponse.choices || !apiOuterResponse.choices[0] || !apiOuterResponse.choices[0].message) {
      throw new Error("API响应格式错误：缺少choices[0].message");
    }

    var actualResultContent = apiOuterResponse.choices[0].message.content.trim();
    var cleanJson = actualResultContent.replace(/```json|```/g, "").trim();
    var apiResult = JSON.parse(cleanJson);

    var requiredFields = ["isSamePerson", "mainName", "confirmedAliases", "removedAliases", "reason"];
    var missingFields = [];
    for (var i = 0; i < requiredFields.length; i++) {
      if (!apiResult.hasOwnProperty(requiredFields[i])) {
        missingFields.push(requiredFields[i]);
      }
    }
    if (missingFields.length > 0) {
      throw new Error("返回格式错误：缺少必选字段" + missingFields.join(","));
    }

    if (typeof apiResult.isSamePerson !== "boolean") {
      throw new Error("返回格式错误：isSamePerson必须是布尔值");
    }
    if (apiResult.mainName !== null && typeof apiResult.mainName !== "string") {
      throw new Error("返回格式错误：mainName必须是字符串或null");
    }
    if (!Array.isArray(apiResult.confirmedAliases)) {
      throw new Error("返回格式错误：confirmedAliases必须是数组");
    }
    if (!Array.isArray(apiResult.removedAliases)) {
      throw new Error("返回格式错误：removedAliases必须是数组");
    }

    return apiResult;
  }

  // 并发请求循环
  while (currentRound < maxRetryRound && !finalResult) {
    currentRound++;
    var concurrentResult = concurrentApiRequest(
      "aliasRefine",
      buildAliasRefineRequest,
      parseAliasRefineResponse,
      null,
      requestTimeout
    );

    if (concurrentResult.success) {
      if (concurrentResult.isMultiResult) {
        finalResult = voteAliasRefineResult(concurrentResult.data);
      } else {
        finalResult = concurrentResult.data;
      }
    } else {
      if (currentRound < maxRetryRound) {
        sleep(250);
      }
    }
  }

  if (!finalResult) {
    console.error("【别名清洗】所有重试均失败");
    return null;
  }

  if (finalResult && finalResult.isSamePerson) {
    var refineBlockReason = graphAliasMergeBlockReason(newName, finalResult.mainName || mainName);
    if (refineBlockReason) {
      aliasShortLog("\u6e05\u6d17\u62e6\u622a " + graphNormalizeName(newName) + "\u2192" + graphNormalizeName(finalResult.mainName || mainName));
      graphRemoteLog("alias_merge_blocked", { stage: "alias_refine", newName: graphNormalizeName(newName), mainName: graphNormalizeName(finalResult.mainName || mainName), reason: refineBlockReason });
      finalResult.isSamePerson = false;
      finalResult.reason = refineBlockReason;
    }
  }
  if (finalResult && finalResult.isSamePerson && this.directPairEvidenceGate) {
    var refineGateContext = String(chapterFullContent || "") + "\n" + String(currentDialogueText || "") + "\n" + String(this.contextHistory2 || "");
    var refineGate = this.directPairEvidenceGate(newName, finalResult.mainName || mainName, finalResult.reason || "", refineGateContext, "alias_refine");
    if (!refineGate.allow) {
      if (refineGate.needVerify && this.verifyGraphConflictAndFix) {
        graphRemoteLog("alias_refine_gate_to_conflict_verify", { newName: graphNormalizeName(newName), mainName: graphNormalizeName(finalResult.mainName || mainName), reason: graphSafeString(finalResult.reason || "", 300), gateReason: graphSafeString(refineGate.reason || "", 180), tier: refineGate.tier || "B" });
        var refineVerify = this.verifyGraphConflictAndFix("positive", newName, finalResult.mainName || mainName, 3.5, "alias_refine_confirmed", finalResult.reason || "", "alias_refine_gate_to_conflict_verify", { defaultAllow: false, forceVerify: true, contextText: refineGateContext, originalSourceReason: "alias_refine_confirmed", originalEvidenceText: finalResult.reason || "" });
        if (!refineVerify.allow) {
          finalResult.isSamePerson = false;
          finalResult.reason = "direct-pair evidence gate verify not passed：" + graphSafeString(refineVerify.reason || refineGate.reason || "", 160);
        }
      } else {
        graphRemoteLog("alias_refine_bridge_gate_blocked", { newName: graphNormalizeName(newName), mainName: graphNormalizeName(finalResult.mainName || mainName), reason: graphSafeString(finalResult.reason || "", 300), gateReason: graphSafeString(refineGate.reason || "", 180), directInContext: !!refineGate.directInContext, directInReason: !!refineGate.directInReason, bridgeRisk: !!refineGate.bridgeRisk, tier: refineGate.tier || "C" });
        finalResult.isSamePerson = false;
        finalResult.removedAliases = finalResult.removedAliases || [];
        if (finalResult.removedAliases.indexOf(newName) === -1) finalResult.removedAliases.push(newName);
        finalResult.reason = "direct-pair evidence gate blocked：" + graphSafeString(refineGate.reason || "", 160);
      }
    }
  }
  if (this.logAliasRefineFlow) this.logAliasRefineFlow(mainName, newName, finalResult);
  return finalResult;
};
// ===================== 新增：规范化别名清洗结果（本地最终兜底整理）=====================
CharacterManager.prototype.normalizeAliasRefineResult = function(mainRecord, refineResult, newName) {
  if (!mainRecord || !mainRecord.name || !refineResult) return null;

  var mainName = (refineResult.mainName || mainRecord.name || "").trim();
  if (!mainName) return null;

  var confirmedAliases = [];
  var seenMap = {};
  var localRemovedMap = {};
  var manager = this;

  function pushAlias(alias) {
    var rawAliasForStateNormalize = (alias || "").trim();
    alias = graphNormalizeStateAliasName(rawAliasForStateNormalize);
    if (rawAliasForStateNormalize && alias && rawAliasForStateNormalize !== alias) graphRemoteLog("state_alias_name_normalized", { raw: graphNormalizeName(rawAliasForStateNormalize), normalized: alias, stage: "alias_refine_normalize" });
    if (!alias) return;
    if (alias === mainName) return; // 主名不重复进别名列表
    if (graphAliasMergeBlockReason(alias, mainName)) { localRemovedMap[alias] = true; return; }
    if (manager.directPairEvidenceGate) {
      var aliasGate = manager.directPairEvidenceGate(alias, mainName, (refineResult && refineResult.reason) || "", manager._aliasDirectPairGateContext || manager.contextHistory2 || "", "alias_refine_normalize");
      if (!aliasGate.allow) {
        if (aliasGate.needVerify && manager.verifyGraphConflictAndFix) {
          graphRemoteLog("alias_refine_gate_to_conflict_verify", { newName: alias, mainName: graphNormalizeName(mainName), reason: graphSafeString((refineResult && refineResult.reason) || "", 280), gateReason: graphSafeString(aliasGate.reason || "", 180), stage: "normalizeAliasRefineResult", tier: aliasGate.tier || "B" });
          var aliasDecision = manager.verifyGraphConflictAndFix("positive", alias, mainName, 3.5, "alias_refine_confirmed", (refineResult && refineResult.reason) || "", "alias_refine_gate_to_conflict_verify", { defaultAllow: false, forceVerify: true, contextText: manager._aliasDirectPairGateContext || manager.contextHistory2 || "", originalSourceReason: "alias_refine_confirmed", originalEvidenceText: (refineResult && refineResult.reason) || "" });
          if (!aliasDecision.allow) { localRemovedMap[alias] = true; return; }
        } else {
          localRemovedMap[alias] = true;
          graphRemoteLog("alias_refine_bridge_gate_blocked", { newName: alias, mainName: graphNormalizeName(mainName), reason: graphSafeString((refineResult && refineResult.reason) || "", 280), gateReason: graphSafeString(aliasGate.reason || "", 180), stage: "normalizeAliasRefineResult", tier: aliasGate.tier || "C" });
          manager.cleanupBridgeAliasIfExists(mainRecord, alias, aliasGate.reason || "direct-pair gate blocked");
          return;
        }
      }
    }
    var exactAliasMain = manager.findMainCharacterRecordByExactName ? manager.findMainCharacterRecordByExactName(alias) : null;
    if (exactAliasMain && exactAliasMain !== mainRecord) {
      var conflictResult = manager.resolveDuplicateAliasMainConflict ? manager.resolveDuplicateAliasMainConflict(mainRecord, alias, "alias_refine_confirmed", manager.contextHistory2 || "") : { allowAlias: false };
      if (!conflictResult.allowAlias) { localRemovedMap[alias] = true; return; }
    }
    if (!seenMap[alias]) {
      seenMap[alias] = true;
      confirmedAliases.push(alias);
    }
  }

  // 先放AI确认的别名
  if (Array.isArray(refineResult.confirmedAliases)) {
    for (var i = 0; i < refineResult.confirmedAliases.length; i++) {
      pushAlias(refineResult.confirmedAliases[i]);
    }
  }

  // 建 removedMap，避免被AI明确剔除的名字又被补回去
  var removedMap = {};
  if (Array.isArray(refineResult.removedAliases)) {
    for (var j = 0; j < refineResult.removedAliases.length; j++) {
      var removedAlias = (refineResult.removedAliases[j] || "").trim();
      if (removedAlias) removedMap[removedAlias] = true;
    }
  }

  // 若AI判定“仍是同一人”，则允许补充新名字（前提：AI没明确把它剔除）
  newName = (newName || "").trim();
  if (refineResult.isSamePerson && newName && !removedMap[newName]) {
    pushAlias(newName);
  }

  var finalRemovedAliases = Array.isArray(refineResult.removedAliases) ? refineResult.removedAliases.slice(0) : [];
  for (var lr in localRemovedMap) {
    if (localRemovedMap.hasOwnProperty(lr) && finalRemovedAliases.indexOf(lr) === -1) finalRemovedAliases.push(lr);
  }
  return {
    mainName: mainName,
    aliases: confirmedAliases,
    removedAliases: finalRemovedAliases
  };
};






CharacterManager.prototype.processCharacter = function (fullText, characterId, allDialogues, chapterFullContent) {
  // 新增参数：chapterFullContent（当前章节完整内容，用于别名校验）
  var analysis = this.analyzeCharacter(fullText, characterId, allDialogues);
  if (!analysis) {
      return null;
  }
  var currentDialogueText = "";
  for (var i = 0; i < allDialogues.length; i++) {
      if (allDialogues[i].id === characterId) {
          currentDialogueText = allDialogues[i].text;
          break;
      }
  }
  var cleanText = currentDialogueText.replace(/^(“?)【\d+】/, "$1");
  if (analysis.__safeDialogueFallback === true) {
      // 姓名分析对齐或重试失败时固定使用duihua，并在这里截断角色卡、别名校验和图谱写入链路
      return { text: cleanText, tag: "duihua" };
  }
  try { if (!this._v85DuplicateRepairDone && this.repairDuplicateAliasMainRecords) { this.repairDuplicateAliasMainRecords("processCharacter_once"); this._v85DuplicateRepairDone = true; } } catch(repairOnceErr) {}
  var rawNewCharacterName = analysis.name.trim();
  var newCharacterName = graphNormalizeStateAliasName(rawNewCharacterName); // 从API解析的新角色名；禁止状态说明名新增音色
  if (rawNewCharacterName && newCharacterName && rawNewCharacterName !== newCharacterName) { graphRemoteLog("state_alias_name_normalized", { raw: graphNormalizeName(rawNewCharacterName), normalized: newCharacterName, stage: "processCharacter" }); analysis.name = newCharacterName; }
  var specialSpeakerType = graphSpecialSpeakerType(newCharacterName, analysis.gender, analysis.age);
  if (ENABLE_NARRATION_OBJECT_NAME_FIX && analysis.gender === "特殊" && analysis.age === "旁白" && graphNormalizeName(newCharacterName) !== "旁白") {
      graphRemoteLog("narration_name_fixed_to_narrator", { stage: "processCharacter", oldName: graphNormalizeName(newCharacterName), reason: "age=旁白，禁止物品/地点/事件名进入角色列表" });
      newCharacterName = "旁白";
      analysis.name = "旁白";
      specialSpeakerType = "旁白";
  }
  if (ENABLE_SPECIAL_SPEAKER_BYPASS && specialSpeakerType) {
      var specialTag = this.assignVoice ? this.assignVoice("特殊", specialSpeakerType, { targetName: newCharacterName, assignType: "特殊说话人", sourceStage: "special_speaker_bypass", afterAliasCheck: false, isSpecialSpeaker: true }) : "";
      if (!specialTag) specialTag = specialSpeakerType === "旁白" ? "括号4" : "括号1";
      graphRemoteLog("special_speaker_bypass_character_record", { name: graphNormalizeName(newCharacterName), specialType: specialSpeakerType, gender: analysis.gender || "", age: analysis.age || "", tag: specialTag, reason: "特殊说话人不进入角色列表、不走别名校验、不写人物图谱" });
      return { text: cleanText, tag: specialTag };
  }
  
  // -------------------------- 别名校验核心逻辑（已升级为二阶段清洗） --------------------------
  var targetMainRecord = null; // 匹配到的主角色记录
  var aliasCheckCompletedForNewName = false;
  var aliasCheckResultForNewName = "";
  var aliasCheckApiResultForObserve = null;
  var aliasMergeBlockReasonForObserve = "";

  // 当前版本固定执行严谨别名校验；未知说话人仍直接走安全兜底
  if (newCharacterName !== "未知") {
      // 1. 先检查新名字是否已在本地（主名字/别名）
      var existingRecord = this.findCharacterRecord(newCharacterName);
      if (!existingRecord) {
          graphRemoteLog("alias_new_name_candidate_without_record", { newName: graphNormalizeName(newCharacterName), chapterId: graphCurrentChapterId(), source: "batch_name_analysis", cardCreated: false });
          // 2. 调用API校验是否为已有角色的别名（第一阶段）
          graphRemoteLog("alias_check_queue_item_start", { name: graphNormalizeName(newCharacterName), stage: "processCharacter", chapterIndex: graphCurrentChapterId() });
          var aliasCheckResult = this.checkAliasByApi(
              newCharacterName,
              chapterFullContent,
              analysis.gender,
              currentDialogueText
          );
          aliasCheckCompletedForNewName = true;
          aliasCheckResultForNewName = aliasCheckResult && aliasCheckResult.isAlias ? "is_alias" : "not_alias";
          try {
              aliasCheckApiResultForObserve = aliasCheckResult ? JSON.parse(JSON.stringify(aliasCheckResult)) : null;
          } catch(e) {
              aliasCheckApiResultForObserve = aliasCheckResult || null;
          }
          graphRemoteLog("alias_check_queue_item_done", { name: graphNormalizeName(newCharacterName), stage: "processCharacter", result: aliasCheckResultForNewName, mainName: aliasCheckResult && aliasCheckResult.mainName ? graphNormalizeName(aliasCheckResult.mainName) : "", reason: graphSafeString(aliasCheckResult && aliasCheckResult.reason || "", 180) });

          if (aliasCheckResult && !aliasCheckResult.isAlias && aliasCheckResult.mainName && this.recordNegativeAliasEdge) {
              this.recordNegativeAliasEdge(newCharacterName, aliasCheckResult.mainName, 3, "alias_api_rejected", aliasCheckResult.reason || "别名API拒绝");
          }

          if (aliasCheckResult && aliasCheckResult.isAlias && aliasCheckResult.mainName) {
              var processAliasBlockReason = graphAliasMergeBlockReason(newCharacterName, aliasCheckResult.mainName);
              if (processAliasBlockReason) {
                  aliasShortLog("\u5408\u5e76\u62e6\u622a " + graphNormalizeName(newCharacterName) + "\u2192" + graphNormalizeName(aliasCheckResult.mainName));
                  graphRemoteLog("alias_merge_blocked", { stage: "process", newName: graphNormalizeName(newCharacterName), mainName: graphNormalizeName(aliasCheckResult.mainName), reason: processAliasBlockReason });
                  aliasMergeBlockReasonForObserve = processAliasBlockReason;
                  graphRemoteLog("alias_final_decision_observe", graphV908AliasObservePayload(
                      "alias_blocked_before_new_role",
                      newCharacterName,
                      aliasCheckApiResultForObserve,
                      { isAlias: false, mainName: null, reason: processAliasBlockReason },
                      null,
                      processAliasBlockReason,
                      {
                          oldAliasCheckResultForNewName: aliasCheckResultForNewName || "",
                          note: "alias api returned true but local block rule converted it to not_alias"
                      }
                  ));
                  aliasCheckResult = { isAlias: false, mainName: null, reason: processAliasBlockReason };
                  aliasCheckResultForNewName = "alias_blocked_to_not_alias";
              }
          }

          if (aliasCheckResult && aliasCheckResult.isAlias && aliasCheckResult.mainName) {
              // 3. 第一阶段校验通过：查找对应的主角色记录
              targetMainRecord = this.findCharacterRecord(aliasCheckResult.mainName);
              if (aliasCheckApiResultForObserve && aliasCheckApiResultForObserve.isAlias) {
                  graphRemoteLog("alias_final_decision_observe", graphV908AliasObservePayload(
                      "after_target_main_lookup",
                      newCharacterName,
                      aliasCheckApiResultForObserve,
                      aliasCheckResult,
                      targetMainRecord,
                      aliasMergeBlockReasonForObserve,
                      {
                          oldAliasCheckResultForNewName: aliasCheckResultForNewName || "",
                          targetLookupMainName: graphSafeString(aliasCheckResult && aliasCheckResult.mainName || "", 80)
                      }
                  ));
              }
              if (targetMainRecord) {
                  graphRemoteLog("alias_reuse_existing_target", { newName: graphNormalizeName(newCharacterName), targetRecordId: targetMainRecord.recordId || "", targetMainName: graphNormalizeName(targetMainRecord.name || ""), voice: targetMainRecord.voice || "", cardCreated: false });

                  // ===================== 第二阶段：别名清洗 =====================
                  var refineResult = this.refineAliasGroupByApi(
                      targetMainRecord,
                      newCharacterName,
                      chapterFullContent,
                      currentDialogueText
                  );

                  // 第二阶段成功：清洗旧别名 + 新增有效别名
                  if (refineResult && refineResult.isSamePerson && refineResult.mainName) {
                      this._aliasDirectPairGateContext = String(chapterFullContent || "") + "\n" + String(currentDialogueText || "") + "\n" + String(this.contextHistory2 || "");
                      var processGate = this.directPairEvidenceGate ? this.directPairEvidenceGate(newCharacterName, refineResult.mainName || targetMainRecord.name, refineResult.reason || aliasCheckResult.reason || "", this._aliasDirectPairGateContext, "process_alias_refine") : { allow: true };
                      if (!processGate.allow) {
                          if (processGate.needVerify && this.verifyGraphConflictAndFix) {
                              graphRemoteLog("alias_refine_gate_to_conflict_verify", { newName: graphNormalizeName(newCharacterName), mainName: graphNormalizeName(refineResult.mainName || targetMainRecord.name), reason: graphSafeString(refineResult.reason || aliasCheckResult.reason || "", 320), gateReason: graphSafeString(processGate.reason || "", 180), stage: "processCharacter", tier: processGate.tier || "B" });
                              var processDecision = this.verifyGraphConflictAndFix("positive", newCharacterName, refineResult.mainName || targetMainRecord.name, 3.5, "alias_refine_confirmed", refineResult.reason || aliasCheckResult.reason || "", "alias_refine_gate_to_conflict_verify", { defaultAllow: false, forceVerify: true, contextText: this._aliasDirectPairGateContext, originalSourceReason: "alias_refine_confirmed", originalEvidenceText: refineResult.reason || aliasCheckResult.reason || "" });
                              if (!processDecision.allow) refineResult.isSamePerson = false;
                          } else {
                              graphRemoteLog("alias_refine_bridge_gate_blocked", { newName: graphNormalizeName(newCharacterName), mainName: graphNormalizeName(refineResult.mainName || targetMainRecord.name), reason: graphSafeString(refineResult.reason || aliasCheckResult.reason || "", 320), gateReason: graphSafeString(processGate.reason || "", 180), stage: "processCharacter", tier: processGate.tier || "C" });
                              this.cleanupBridgeAliasIfExists(targetMainRecord, newCharacterName, processGate.reason || "direct-pair gate blocked");
                              refineResult.isSamePerson = false;
                          }
                      }
                  }
                  if (refineResult && refineResult.isSamePerson && refineResult.mainName) {
                      var normalizedRefine = this.normalizeAliasRefineResult(
                          targetMainRecord,
                          refineResult,
                          newCharacterName
                      );

                      if (normalizedRefine) {
                          // 已确认属于现有卡的新本名必须在原卡上就地晋升；不能因finalMainName尚无独立卡而新建第二张角色卡。
                          if (normalizedRefine.mainName !== targetMainRecord.name) {
                              var oldMainNameForRedirect = targetMainRecord.name;
                              var createdNewMainRecordForRedirect = false;
                              var switchedMainRecord = this.findMainCharacterRecordByExactName ? this.findMainCharacterRecordByExactName(normalizedRefine.mainName) : null;
                              if (!switchedMainRecord && this.characterRecords && Array.isArray(this.characterRecords)) {
                                  for (var exactMainIndex = 0; exactMainIndex < this.characterRecords.length; exactMainIndex++) {
                                      if (graphNormalizeName(this.characterRecords[exactMainIndex] && this.characterRecords[exactMainIndex].name || "") === graphNormalizeName(normalizedRefine.mainName)) {
                                          switchedMainRecord = this.characterRecords[exactMainIndex];
                                          break;
                                      }
                                  }
                              }
                              if (switchedMainRecord && switchedMainRecord !== targetMainRecord) {
                                  targetMainRecord = switchedMainRecord;
                              } else {
                                  var promotedRecordId = targetMainRecord.recordId || "";
                                  var promotedVoice = targetMainRecord.voice || "";
                                  var promotedChapters = Array.isArray(targetMainRecord.chapters) ? targetMainRecord.chapters.slice(0) : [];
                                  var promotedAliasList = String(targetMainRecord.aliases || oldMainNameForRedirect).split("|");
                                  targetMainRecord.name = graphNormalizeName(normalizedRefine.mainName);
                                  var promotedAliasOut = [targetMainRecord.name];
                                  var addPromotedAlias = function(value) {
                                      value = graphNormalizeName(value || "");
                                      if (value && promotedAliasOut.indexOf(value) === -1) promotedAliasOut.push(value);
                                  };
                                  addPromotedAlias(oldMainNameForRedirect);
                                  for (var promotedAliasIndex = 0; promotedAliasIndex < promotedAliasList.length; promotedAliasIndex++) addPromotedAlias(promotedAliasList[promotedAliasIndex]);
                                  for (var normalizedAliasIndex = 0; normalizedAliasIndex < normalizedRefine.aliases.length; normalizedAliasIndex++) addPromotedAlias(normalizedRefine.aliases[normalizedAliasIndex]);
                                  targetMainRecord.aliases = promotedAliasOut.join("|");
                                  if (this.markRecordActiveChapter) this.markRecordActiveChapter(targetMainRecord);
                                  if (this.rebuildNameToMainNameMap) this.rebuildNameToMainNameMap();
                                  var promotedStateInfo = this.findTemporaryVoiceStateForRecord ? this.findTemporaryVoiceStateForRecord(targetMainRecord) : null;
                                  if (promotedStateInfo && promotedStateInfo.state) promotedStateInfo.state.roleName = targetMainRecord.name;
                                  graphRemoteLog("alias_main_redirect_new_record_blocked", { oldMainName: graphNormalizeName(oldMainNameForRedirect), finalMainName: targetMainRecord.name, recordId: promotedRecordId, reason: "confirmed_same_record_promote_in_place" });
                                  graphRemoteLog("alias_main_promotion_in_place", { oldMainName: graphNormalizeName(oldMainNameForRedirect), finalMainName: targetMainRecord.name, recordIdBefore: promotedRecordId, recordIdAfter: targetMainRecord.recordId || "", voiceBefore: promotedVoice, voiceAfter: targetMainRecord.voice || "", chaptersBefore: promotedChapters, chaptersAfter: targetMainRecord.chapters || [], createdNewRecord: false });
                              }
                              graphRemoteLog("alias_refine_main_redirect", {
                                  oldMainName: graphNormalizeName(oldMainNameForRedirect),
                                  finalMainName: graphNormalizeName(normalizedRefine.mainName),
                                  newName: graphNormalizeName(newCharacterName),
                                  createdNewMainRecord: createdNewMainRecordForRedirect,
                                  removedAliases: normalizedRefine.removedAliases || []
                              });
                          }

                          // 主名固定放第一位，后面跟确认过的别名；若别名本身已有独立角色卡，必须先裁决并执行合并/拆分，不能只跳过映射。
                          var finalAliasList = [targetMainRecord.name];
                          for (var a = 0; a < normalizedRefine.aliases.length; a++) {
                              var aliasItem = graphNormalizeName(normalizedRefine.aliases[a]);
                              if (!aliasItem || aliasItem === targetMainRecord.name || finalAliasList.indexOf(aliasItem) !== -1) continue;
                              var aliasExactRecord = this.findMainCharacterRecordByExactName ? this.findMainCharacterRecordByExactName(aliasItem) : null;
                              if (aliasExactRecord && aliasExactRecord !== targetMainRecord) {
                                  var conflictRes = this.resolveDuplicateAliasMainConflict ? this.resolveDuplicateAliasMainConflict(targetMainRecord, aliasItem, "alias_refine_confirmed", chapterFullContent || currentDialogueText || "") : { allowAlias: false };
                                  if (!conflictRes || !conflictRes.allowAlias) continue;
                              }
                              if (finalAliasList.indexOf(aliasItem) === -1) finalAliasList.push(aliasItem);
                          }
                          // mergeCharacterRecords 可能已经把旧角色卡的别名并入 targetMainRecord，这里重新吸收一次，避免覆盖合并结果。
                          var existedAliasArr = String(targetMainRecord.aliases || targetMainRecord.name).split("|");
                          for (var eaIdx = 0; eaIdx < existedAliasArr.length; eaIdx++) {
                              var existedAlias = graphNormalizeName(existedAliasArr[eaIdx]);
                              if (existedAlias && finalAliasList.indexOf(existedAlias) === -1) finalAliasList.push(existedAlias);
                          }

                          targetMainRecord.aliases = finalAliasList.join("|");
                          if (this.markRecordActiveChapter) this.markRecordActiveChapter(targetMainRecord);

                          // 同步刷新内存映射表，避免后续投票/匹配仍使用旧别名
                          if (this.rebuildNameToMainNameMap) this.rebuildNameToMainNameMap();

                          this.saveRecords();
                          var removedAliasMapForRefine = {};
                          if (normalizedRefine.removedAliases && normalizedRefine.removedAliases.length) {
                              for (var rm = 0; rm < normalizedRefine.removedAliases.length; rm++) {
                                  var rmName = graphNormalizeName(normalizedRefine.removedAliases[rm]);
                                  if (rmName) removedAliasMapForRefine[rmName] = true;
                              }
                          }
                          var positiveEdgeBlockedByRemoved = !!(removedAliasMapForRefine[graphNormalizeName(newCharacterName)] || removedAliasMapForRefine[graphNormalizeName(targetMainRecord.name)]);
                          if (!positiveEdgeBlockedByRemoved && this.recordPositiveAliasEdge) {
                              this._aliasDirectPairGateContext = String(chapterFullContent || "") + "\n" + String(currentDialogueText || "") + "\n" + String(this.contextHistory2 || "");
                              this.recordPositiveAliasEdge(newCharacterName, targetMainRecord.name, 3.5, "alias_refine_confirmed", refineResult.reason || aliasCheckResult.reason || "别名清洗确认");
                          } else {
                              graphRemoteLog("alias_refine_removed_aliases_applied", { newName: graphNormalizeName(newCharacterName), mainName: graphNormalizeName(targetMainRecord.name), removedAliases: normalizedRefine.removedAliases || [], skippedPositiveEdge: positiveEdgeBlockedByRemoved });
                          }
                          if (typeof graphRemoteLog === "function") graphRemoteLog("alias_merge_confirmed", { newName: newCharacterName, mainName: targetMainRecord.name, aliases: finalAliasList });
                      }
                  }

                  // 方案A兜底：
                  // 若第二阶段失败 / 返回不是同一人，则当前句仍复用第一阶段锁定的主角色，
                  // 但不修改aliases，避免污染别名库。
                  if (this.markRecordActiveChapter) this.markRecordActiveChapter(targetMainRecord);
                  this.saveRecords();
                  graphRemoteLog("alias_existing_role_reuse_observe", graphV908AliasObservePayload(
                      "reuse_existing_role_before_return",
                      newCharacterName,
                      aliasCheckApiResultForObserve,
                      aliasCheckResult,
                      targetMainRecord,
                      aliasMergeBlockReasonForObserve,
                      {
                          returnedVoice: graphSafeString(targetMainRecord.voice || "", 80),
                          recordId: graphSafeString(targetMainRecord.recordId || "", 80)
                      }
                  ));
                  return {
                      text: cleanText,
                      tag: targetMainRecord.voice || "default",
                      characterInfo: targetMainRecord
                  };
              }
          }
      } else {
          targetMainRecord = existingRecord; // 新名字已存在，直接使用现有记录
      }
  }
  // -------------------------- 别名校验逻辑结束 --------------------------
  
  // 原有新建/更新角色逻辑（适配targetMainRecord）
  if (newCharacterName === "未知") {
      // 判断不出角色名时，按已识别的性别走兜底：男→duihuaA，女→duihuaB，性别未知→duihua
      var tag = analysis.gender === "男" ? "duihuaA" : analysis.gender === "女" ? "duihuaB" : "duihua";
      console.log("【兜底分配】角色无法识别(性别:" + (analysis.gender || "未知") + ")→" + tag + " | 文本:" + cleanText.substring(0, 20));
      return { text: cleanText, tag: tag };
  }
  
  // 若未匹配到主角色记录，执行原有新建角色逻辑
  if (!targetMainRecord) {
      if (aliasCheckApiResultForObserve && aliasCheckApiResultForObserve.isAlias) {
          graphRemoteLog("alias_new_role_fallback_observe", graphV908AliasObservePayload(
              "before_create_new_role",
              newCharacterName,
              aliasCheckApiResultForObserve,
              aliasCheckResult,
              targetMainRecord || null,
              aliasMergeBlockReasonForObserve,
              {
                  oldAliasCheckResultForNewName: aliasCheckResultForNewName || "",
                  fallbackReason: aliasMergeBlockReasonForObserve ? "alias_blocked" : "alias_target_missing_or_not_found"
              }
          ));
      }
      graphRemoteLog("new_role_create_begin", { name: graphNormalizeName(newCharacterName), source: "姓名分析新名字", fromAliasCheck: aliasCheckCompletedForNewName === true, aliasCheckResult: aliasCheckResultForNewName || "", fromSplit: false, stage: "processCharacter" });
      if (aliasCheckCompletedForNewName === true) graphRemoteLog("new_role_create_after_alias", { name: graphNormalizeName(newCharacterName), aliasCheckResult: aliasCheckResultForNewName || "", stage: "processCharacter" });
      else graphRemoteLog("new_role_create_without_alias", { name: graphNormalizeName(newCharacterName), source: "普通新角色链路", stage: "processCharacter" });
      var voice = this.assignVoice(analysis.gender, analysis.age, {
          targetName: newCharacterName,
          assignType: "新角色发音人分配",
          sourceStage: aliasCheckCompletedForNewName === true ? "new_role_create_after_alias" : "new_role_create_without_alias",
          afterAliasCheck: aliasCheckCompletedForNewName === true,
          aliasCheckResult: aliasCheckResultForNewName || "",
          aliasApiResultForObserve: aliasCheckApiResultForObserve,
          aliasAfterBlockForObserve: aliasCheckResult || null,
          aliasTargetMainRecordForObserve: targetMainRecord || null,
          aliasMergeBlockReasonForObserve: aliasMergeBlockReasonForObserve || "",
          isSpecialSpeaker: false
      });
      if (!voice) {
          var tag2 = analysis.gender === "男" ? "duihuaA" : 
                    analysis.gender === "女" ? "duihuaB" : 
                    "duihua";
          return { text: cleanText, tag: tag2 };
      }
      targetMainRecord = {
          name: newCharacterName,
          aliases: newCharacterName, // 初始别名=主名字
          gender: analysis.gender,
          age: analysis.age,
          voice: voice,
          usageCount: CONFIG.resetUsageCount,
          chapters: graphCurrentChapterId() === "unknown" ? [] : [graphCurrentChapterId()],
          genderAgeHistory: [],
          voiceAgeVerified: false,
          voiceAgeProvisional: true,
          voiceAgeSource: "initial_model_without_audited_evidence"
      };
      this.characterRecords.unshift(targetMainRecord);
  } else {
      // 已有角色不再累计姓名模型猜测的年龄；只有独立证据审计accept后，才在统一出口更新自然年龄段。
      var fixedVoiceLockedForExisting = graphV908IsFixedVoiceRecord(targetMainRecord);
      if (fixedVoiceLockedForExisting) {
          graphV908MarkFixedVoiceRecord(targetMainRecord, targetMainRecord.fixedVoiceReason || "existing_role_fixed_voice_detected");
      }
      // 新增：已有角色发音人校验（2个条件满足其一则重新分配）
      // 条件1：发音人字段为空/空字符串；条件2：发音人未在系统data（availableVoices）中加载
      var isVoiceInvalid = !fixedVoiceLockedForExisting && (!targetMainRecord.voice ||
          targetMainRecord.voice === "" ||
          !this.availableVoices[targetMainRecord.voice]);
      if (isVoiceInvalid) {
          // 新增：区分无效原因，方便调试
          var invalidReason = !targetMainRecord.voice || targetMainRecord.voice === ""
              ? "发音人字段为空"
              : "发音人[" + targetMainRecord.voice + "]未在系统data中加载";
          // 音色字段损坏时优先使用角色卡已保存的性别/年龄；仅字段为空时才使用本轮临时初值。
          var repairGender = targetMainRecord.gender || analysis.gender;
          var repairAge = targetMainRecord.age || analysis.age;
          var newVoice = this.assignVoice(repairGender, repairAge, { targetName: targetMainRecord.name || newCharacterName, assignType: "已有角色发音人修复", sourceStage: "existing_role_voice_repair", afterAliasCheck: false, isSpecialSpeaker: false });
          if (newVoice) {
              targetMainRecord.voice = newVoice; // 更新为新发音人
              if (!targetMainRecord.gender) targetMainRecord.gender = repairGender;
              if (!targetMainRecord.age) targetMainRecord.age = repairAge;
              this.saveRecords(); // 持久化更新结果
          } else {
              // 新增：极端情况（无可用发音人），降级为默认对话标签
              targetMainRecord.voice = analysis.gender === "男" ? "duihuaA" : 
                                       analysis.gender === "女" ? "duihuaB" : 
                                       "duihua";
          }
      }
      // 原有角色更新逻辑（完全保留，无任何修改）
      if (targetMainRecord.usageCount === 100) {
          this.moveRecordToTop(targetMainRecord.name);
          this.saveRecords();
          return { text: cleanText, tag: targetMainRecord.voice || "default", characterInfo: targetMainRecord };
      }
      if (targetMainRecord.usageCount === 50) {
          if (!targetMainRecord.voice || targetMainRecord.voice === "") {
              targetMainRecord.voice = this.assignVoice(targetMainRecord.gender, targetMainRecord.age, { targetName: targetMainRecord.name || newCharacterName, assignType: "已有角色发音人补齐", sourceStage: "existing_role_voice_fill", afterAliasCheck: false, isSpecialSpeaker: false });
          } else {
              var voiceInfo = null;
              for (var key in GENSHIN_CHARACTERS) {
                  if (GENSHIN_CHARACTERS[key].voice === targetMainRecord.voice) {
                      voiceInfo = GENSHIN_CHARACTERS[key];
                      break;
                  }
              }
              if (!fixedVoiceLockedForExisting && voiceInfo && (voiceInfo.gender !== targetMainRecord.gender || voiceInfo.age !== targetMainRecord.age)) {
                  targetMainRecord.voice = this.assignVoice(targetMainRecord.gender, targetMainRecord.age, { targetName: targetMainRecord.name || newCharacterName, assignType: "已有角色发音人补齐", sourceStage: "existing_role_voice_fill", afterAliasCheck: false, isSpecialSpeaker: false });
              }
          }
          this.moveRecordToTop(targetMainRecord.name);
          this.saveRecords();
          return { text: cleanText, tag: targetMainRecord.voice || "default", characterInfo: targetMainRecord };
      }
      if (!targetMainRecord.voice || targetMainRecord.voice === "") {
          var fillGender = targetMainRecord.gender || analysis.gender;
          var fillAge = targetMainRecord.age || analysis.age;
          targetMainRecord.voice = this.assignVoice(fillGender, fillAge, { targetName: targetMainRecord.name || newCharacterName, assignType: "已有角色发音人补齐", sourceStage: "existing_role_voice_fill", afterAliasCheck: false, isSpecialSpeaker: false });
          if (!targetMainRecord.voice) {
              var tag3 = fillGender === "男" ? "duihuaA" : 
                        fillGender === "女" ? "duihuaB" : 
                        "duihua";
              return { text: cleanText, tag: tag3 };
          }
          if (!targetMainRecord.gender) targetMainRecord.gender = fillGender;
          if (!targetMainRecord.age) targetMainRecord.age = fillAge;
      }
      if (targetMainRecord.gender === null || targetMainRecord.age === null) {
          if (targetMainRecord.gender === null) targetMainRecord.gender = analysis.gender;
          if (targetMainRecord.age === null) targetMainRecord.age = analysis.age;
          targetMainRecord.voiceAgeVerified = false;
          targetMainRecord.voiceAgeProvisional = true;
          targetMainRecord.voiceAgeSource = "missing_field_repaired_from_initial_model";
      }
      targetMainRecord.usageCount--;
      if (targetMainRecord.usageCount < 0) targetMainRecord.usageCount = CONFIG.resetUsageCount;
  }
  if (this.markRecordActiveChapter) this.markRecordActiveChapter(targetMainRecord);
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
    var cache = readDialogCache();
    var dialogList = cache.dialogList || [];
    var narrationList = [];
    // 筛选name为旁白的有效条目
    for (var i = 0; i < dialogList.length; i++) {
      var item = dialogList[i];
      if (item && item.name && item.name.trim() === "旁白" && item.dialogContent) {
        narrationList.push(item);
      }
    }
    return narrationList;
  } catch (e) {
    // 异常返回空数组，完全不影响原有流程
    return [];
  }
}






  
  // ===================== 新增：批量对话缓存辅助函数（ES5兼容，无侵入）=====================
  // ===================== 终极兼容版：根源读取函数（直接替换原函数即可）=====================
function readDialogCache() {
  try {
      var content = ttsrv.readTxtFile("dialog_cache.json");
      // 兼容空文件、空字符串：直接走兜底
      if (!content || content.trim() === "") {
          return { currentIndex: 1, dialogList: [], relationEvidence: [], temporaryVoiceSnapshot: null, cacheBatchId: "", cacheCreatedChapterId: "", cacheCreatedAt: "" };
      }
      var rawCache = JSON.parse(content);
      // 兼容空对象：强制兜底核心字段
      if (!rawCache || typeof rawCache !== "object") {
          return { currentIndex: 1, dialogList: [], relationEvidence: [], temporaryVoiceSnapshot: null, cacheBatchId: "", cacheCreatedChapterId: "", cacheCreatedAt: "" };
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
          dialogList: safeDialogList,
          relationEvidence: Array.isArray(rawCache.relationEvidence) ? rawCache.relationEvidence : [],
          temporaryVoiceSnapshot: rawCache.temporaryVoiceSnapshot && typeof rawCache.temporaryVoiceSnapshot === "object" ? rawCache.temporaryVoiceSnapshot : null,
          cacheBatchId: graphSafeString(rawCache.cacheBatchId || "", 160),
          cacheCreatedChapterId: graphSafeString(rawCache.cacheCreatedChapterId || "", 80),
          cacheCreatedAt: graphSafeString(rawCache.cacheCreatedAt || "", 80)
      };
  } catch (e) {
      // 任何异常（文件不存在、JSON解析失败），都返回安全兜底结构
      return { currentIndex: 1, dialogList: [], relationEvidence: [], temporaryVoiceSnapshot: null, cacheBatchId: "", cacheCreatedChapterId: "", cacheCreatedAt: "" };
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
    
    // 缓存对话按换行分割，逐行匹配
    var cacheLines = splitDialogByLine(cacheDialogItem.dialogContent);
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
  var cache = readDialogCache();
  var dialogList = cache.dialogList;
  var currentIndex = cache.currentIndex;
  var MAX_OFFSET = 3;

  if (!dialogList || dialogList.length === 0) {
      return null;
  }

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
              cache.currentIndex = finalMatchedIndex + 0;
              writeDialogCache(cache);
          }
          return {
              name: roleName,
              gender: characterRecord.gender,
              age: characterRecord.age,
              voice: characterRecord.voice
          };
      }
  }

  return null;
}

// 姓名分析上下文边界：上文对齐句号；下文补齐半截引号并外延2个句号，且不进入下一句双引号对话
function extendPrevContentStartPos(fullText, hardStart, endPos) {
    if (!fullText) return 0;
    var len = fullText.length;
    if (endPos < 0) endPos = 0;
    if (endPos > len) endPos = len;
    if (hardStart < 0) hardStart = 0;
    if (hardStart > endPos) hardStart = endPos;
    if (hardStart <= 0) return 0;
    // 从硬起点往前找句号，起点落在该句号之后
    var i = hardStart - 1;
    while (i >= 0) {
        if (fullText.charAt(i) === "\u3002") {
            return i + 1;
        }
        i--;
    }
    return 0;
}

function extendNextContentEndPos(fullText, startPos, endPos) {
    var emptyResult = { endPos: endPos || 0, hardEndPos: endPos || 0, extendedChars: 0, baseHadUnclosedQuote: false, quoteClosed: true, foundPeriods: 0, stopReason: "text_end" };
    if (!fullText) return emptyResult;
    var len = fullText.length;
    if (startPos < 0) startPos = 0;
    if (endPos < startPos) endPos = startPos;
    if (endPos > len) endPos = len;
    if (startPos >= len) {
        emptyResult.endPos = len;
        emptyResult.hardEndPos = len;
        emptyResult.stopReason = "chapter_end";
        return emptyResult;
    }

    var maxExtra = parseInt(NAME_ANALYZE_NEXT_CONTEXT_EXTRA_MAX, 10);
    if (isNaN(maxExtra) || maxExtra < 0) maxExtra = 3000;
    var baseEndPos = endPos;
    var hardEndPos = Math.min(len, baseEndPos + maxExtra); // 外延最多3000字符，同时不能超过现有已加载文本末尾
    var basePart = fullText.substring(startPos, baseEndPos);
    var leftCount = (basePart.match(/“/g) || []).length;
    var rightCount = (basePart.match(/”/g) || []).length;
    var needClose = Math.max(0, leftCount - rightCount);
    var baseHadUnclosedQuote = needClose > 0;
    var i = baseEndPos;

    // 先在3000字符硬上限内补齐当前半截对白；找不到右引号时直接停在硬上限
    while (i < hardEndPos && needClose > 0) {
        if (fullText.charAt(i) === "”") needClose--;
        i++;
    }
    if (needClose > 0) {
        return {
            endPos: hardEndPos,
            hardEndPos: hardEndPos,
            extendedChars: Math.max(0, hardEndPos - baseEndPos),
            baseHadUnclosedQuote: baseHadUnclosedQuote,
            quoteClosed: false,
            foundPeriods: 0,
            stopReason: hardEndPos >= len ? "chapter_end" : "extension_hard_limit"
        };
    }

    // 补齐右引号后继续寻找两个句号；遇下一条左双引号、3000字符上限或文本末尾时提前停止
    var foundPeriod = 0;
    var j = i;
    var stopReason = "two_periods";
    while (j < hardEndPos && foundPeriod < 2) {
        var ch = fullText.charAt(j);
        if (ch === "“") {
            stopReason = "before_next_dialogue";
            break;
        }
        j++;
        if (ch === "。") foundPeriod++;
    }
    if (foundPeriod >= 2) {
        stopReason = "two_periods";
    } else if (j >= len) {
        stopReason = "chapter_end";
    } else if (j >= hardEndPos) {
        stopReason = hardEndPos >= len ? "chapter_end" : "extension_hard_limit";
    }
    return {
        endPos: Math.min(j, len),
        hardEndPos: hardEndPos,
        extendedChars: Math.max(0, Math.min(j, len) - baseEndPos),
        baseHadUnclosedQuote: baseHadUnclosedQuote,
        quoteClosed: true,
        foundPeriods: foundPeriod,
        stopReason: stopReason
    };
}

// 姓名分析映射只清理格式噪声，不删除正文汉字和标点，避免短对白再次发生内容错位
function normalizeNameAnalysisDialogueText(text) {
    return String(text || "")
        .replace(/.[\u4e00-\u9fa5]+音效./g, "")
        .replace(/【\d+】/g, "")
        .replace(/^[\s\u3000]*“/, "")
        .replace(/”[\s\u3000]*$/, "")
        .replace(/[\s\u3000\u2000-\u200F\u2028-\u202F\uFEFF]/g, "")
        .trim();
}

// 姓名分析缓存的严格文本匹配口径：只清理格式噪声，保留正文标点差异供远程日志观察
function normalizeNameAnalysisCacheMatchText(text) {
    return String(text || "")
        .replace(/.[\u4e00-\u9fa5]+音效./g, "")
        .replace(/[\s\u3000\u2000-\u200F\u2028-\u202F\uFEFF]/g, "")
        .replace(/【\d+】/g, "")
        .replace(/[“”"''"]/g, "")
        .replace(/\s+/g, "")
        .replace(/[^\u4e00-\u9fa5\u3002\uff1f\uff01\uff0c\uff1b\uff1a\u3001\u201c\u201d\u2018\u2019\uff08\uff09\u3010\u3011\u300a\u300b\u2026\u2014\u00b7a-zA-Z0-9.,!?;:"'()\[\]{}<>-]/g, "")
        .trim();
}

// 远程观察使用完整对白文本；不参与匹配、不改变缓存结果
function graphBuildNameAnalysisBlockSnapshot(blocks) {
    var output = [];
    blocks = blocks || [];
    for (var i = 0; i < blocks.length; i++) {
        var block = blocks[i] || {};
        var fullText = String(block.dialogText || "");
        var normalized = normalizeNameAnalysisCacheMatchText(fullText);
        output.push({
            cacheIndex: i + 1,
            seq: graphSafeString(block.seq || padZero(i + 1, 2), 20),
            source: graphSafeString(block.source || "", 30),
            start: Number(block.start || 0),
            end: Number(block.end || 0),
            rawText: String(block.rawText || ""),
            dialogueText: fullText,
            normalizedText: normalized,
            normalizedHash: graphHash(normalized),
            complete: block.complete === true
        });
    }
    return output;
}

function graphBuildNameAnalysisCacheSnapshot(dialogList) {
    var output = [];
    dialogList = dialogList || [];
    for (var i = 0; i < dialogList.length; i++) {
        var item = dialogList[i] || {};
        var fullText = String(item.dialogContent || "");
        var normalized = normalizeNameAnalysisCacheMatchText(fullText);
        output.push({
            cacheIndex: i + 1,
            seq: graphSafeString(item.seq || padZero(i + 1, 2), 20),
            dialogueText: fullText,
            normalizedText: normalized,
            normalizedHash: graphHash(normalized),
            name: graphNormalizeName(item.name || ""),
            gender: graphSafeString(item.gender || "", 30),
            age: graphSafeString(item.age || "", 40),
            voiceAgeEvidenceCount: Array.isArray(item.voiceAgeEvidence) ? item.voiceAgeEvidence.length : 0
        });
    }
    return output;
}

function graphBuildNameAnalysisDialogueContextSnapshot(sourceRawText, quoteBlocks, sharedPreviousContext) {
    sourceRawText = String(sourceRawText || "");
    sharedPreviousContext = String(sharedPreviousContext || "");
    quoteBlocks = quoteBlocks || [];
    var output = [];
    var previousQuoteEnd = 0;
    var sharedTailStart = sharedPreviousContext.length;
    while (sharedTailStart > 0) {
        var sharedPreviousChar = sharedPreviousContext.charAt(sharedTailStart - 1);
        if (sharedPreviousChar === "。" || sharedPreviousChar === "！" || sharedPreviousChar === "？" || sharedPreviousChar === "!" || sharedPreviousChar === "?") break;
        sharedTailStart--;
    }
    var sharedImmediateTail = sharedPreviousContext.substring(sharedTailStart);
    for (var i = 0; i < quoteBlocks.length; i++) {
        var block = quoteBlocks[i] || {};
        var start = Math.max(0, Number(block.start || 0));
        var sentenceStart = start;
        while (sentenceStart > 0) {
            var previousChar = sourceRawText.charAt(sentenceStart - 1);
            if (previousChar === "。" || previousChar === "！" || previousChar === "？" || previousChar === "!" || previousChar === "?") break;
            sentenceStart--;
        }
        var betweenStart = i > 0 ? Math.max(0, previousQuoteEnd) : 0;
        var sameSentencePrefix = sourceRawText.substring(sentenceStart, start);
        var joinsSharedPrevious = i === 0 && sentenceStart === 0 && !!sharedImmediateTail;
        output.push({
            seq: graphSafeString(block.seq || "", 20),
            source: graphSafeString(block.source || "", 20),
            dialogueText: String(block.dialogText || ""),
            dialogueHash: graphHash(normalizeNameAnalysisCacheMatchText(block.dialogText || "")),
            // 这是从本句最近一个句末标点之后，到当前左引号之前的真正紧邻文本；不会跨过句号、问号或叹号。
            immediatePreDialogueText: (joinsSharedPrevious ? sharedImmediateTail : "") + sameSentencePrefix,
            immediateContextSource: joinsSharedPrevious ? "shared_previous_tail_plus_current_prefix" : "current_batch_same_sentence_prefix",
            previousSentenceBoundaryFoundInCurrentBatch: sentenceStart > 0,
            sameSentencePrefix: sameSentencePrefix,
            textBetweenPreviousDialogueAndThisDialogue: sourceRawText.substring(betweenStart, start),
            usesSharedPreviousTail: joinsSharedPrevious
        });
        previousQuoteEnd = Math.max(previousQuoteEnd, Number(block.end || start) + 1);
    }
    return output;
}

// 复用原有换行引号修复，但当前段和下文分别处理，避免跨来源修复后无法判断序号来源
function repairNameAnalysisBatchText(text) {
    text = String(text || "");
    text = text.replace(/(.[\u4e00-\u9fa5]+音效.)/g, "");
    text = text.replace(/【\d+】/g, "");
    text = text.replace(/(”[^“”]*\n)([^“”\n]+”)/g, "$1“$2");
    text = text.replace(/(“[^“”\n]+)(\n[^“”]*“)/g, "$1”$2");
    text = text.replace(/[『「【〈〉〔‘’〕】」』]/g, "");
    return text;
}

// 只解析完整的最外层中文双引号块；同形嵌套引号留在外层内容中，不重复编号
function parseNameAnalysisOuterQuoteBlocks(fullRawText, currentBoundary) {
    var blocks = [];
    var depth = 0;
    var blockStart = -1;
    for (var i = 0; i < fullRawText.length; i++) {
        var ch = fullRawText.charAt(i);
        if (ch === "“") {
            if (depth === 0) blockStart = i;
            depth++;
        } else if (ch === "”" && depth > 0) {
            depth--;
            if (depth === 0 && blockStart >= 0) {
                blocks.push({
                    start: blockStart,
                    end: i,
                    source: blockStart < currentBoundary ? "current" : "future",
                    rawText: fullRawText.substring(blockStart, i + 1),
                    dialogText: fullRawText.substring(blockStart + 1, i),
                    normalizedText: normalizeNameAnalysisDialogueText(fullRawText.substring(blockStart + 1, i)),
                    complete: true,
                    seq: ""
                });
                blockStart = -1;
            }
        }
    }
    return blocks;
}

// 使用精确文本和单调顺序建立朗读对白到外层引号块的映射；有歧义时宁可失败也不猜序号
function mapNameAnalysisDialoguesToBlocks(dialoguesList, currentBlocks) {
    dialoguesList = dialoguesList || [];
    currentBlocks = currentBlocks || [];
    var result = { ok: false, characterIdToSeq: {}, characterIdToBlock: {}, errors: [], method: "" };
    if (!dialoguesList.length) {
        result.errors.push("current_dialogue_list_empty");
        return result;
    }
    var dialogueNorms = [];
    var blockNorms = [];
    var i, j;
    for (i = 0; i < dialoguesList.length; i++) dialogueNorms.push(normalizeNameAnalysisDialogueText(dialoguesList[i] && dialoguesList[i].text || ""));
    for (j = 0; j < currentBlocks.length; j++) blockNorms.push(currentBlocks[j].normalizedText || normalizeNameAnalysisDialogueText(currentBlocks[j].dialogText || ""));

    var mapped = [];
    var positionalOk = dialoguesList.length === currentBlocks.length;
    if (positionalOk) {
        for (i = 0; i < dialogueNorms.length; i++) {
            if (!dialogueNorms[i] || dialogueNorms[i] !== blockNorms[i]) {
                positionalOk = false;
                break;
            }
            mapped[i] = i;
        }
    }

    if (positionalOk) {
        result.method = "position_exact";
    } else {
        // 当前引号块可能包含额外的引号内旁白，因此用最长公共子序列做严格文本对齐
        var n = dialogueNorms.length;
        var m = blockNorms.length;
        var dp = [];
        for (i = 0; i <= n; i++) {
            dp[i] = [];
            for (j = 0; j <= m; j++) dp[i][j] = 0;
        }
        for (i = n - 1; i >= 0; i--) {
            for (j = m - 1; j >= 0; j--) {
                if (dialogueNorms[i] && dialogueNorms[i] === blockNorms[j]) dp[i][j] = dp[i + 1][j + 1] + 1;
                else dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
            }
        }
        if (dp[0][0] !== n) {
            result.errors.push("not_all_current_dialogues_mapped");
            result.errors.push("mapped_" + dp[0][0] + "_of_" + n);
            return result;
        }
        i = 0;
        j = 0;
        while (i < n && j < m) {
            if (dialogueNorms[i] && dialogueNorms[i] === blockNorms[j] && dp[i][j] === dp[i + 1][j + 1] + 1) {
                mapped[i] = j;
                i++;
                j++;
            } else if (dp[i][j + 1] >= dp[i + 1][j]) {
                j++;
            } else {
                i++;
            }
        }
        result.method = "ordered_exact_lcs";

        // 只有在额外引号块存在时检查重复短对白歧义；前后映射范围内出现多个相同块即拒绝猜测
        for (i = 0; i < n; i++) {
            var lower = i > 0 ? mapped[i - 1] + 1 : 0;
            var upper = i < n - 1 ? mapped[i + 1] - 1 : m - 1;
            var sameCount = 0;
            for (j = lower; j <= upper; j++) if (dialogueNorms[i] && dialogueNorms[i] === blockNorms[j]) sameCount++;
            if (sameCount > 1) result.errors.push("ambiguous_dialogue_" + i);
        }
        if (result.errors.length) return result;
    }

    for (i = 0; i < dialoguesList.length; i++) {
        var item = dialoguesList[i] || {};
        var block = currentBlocks[mapped[i]];
        if (!item.id || !block || !block.seq) {
            result.errors.push("missing_character_id_or_seq_" + i);
            continue;
        }
        result.characterIdToSeq[String(item.id)] = block.seq;
        result.characterIdToBlock[String(item.id)] = block;
    }
    result.ok = result.errors.length === 0 && Object.keys(result.characterIdToSeq).length === dialoguesList.length;
    return result;
}

function generateBatchSeqContent(dialoguesList, nextContent) {
    var isEndQuoteAutoAdded = /(\u201c[^\u201d]+)$/.test(String(typeof text2 !== "undefined" ? text2 : ""));
    var cleanedSegment = "";
    if (typeof text2 === "string" && text2.length > 0) {
        cleanedSegment = String(text2).replace(/【\d+】/g, "");
        if (isEndQuoteAutoAdded) cleanedSegment = cleanedSegment.replace(/\u201d$/, "");
        if (cleanedSegment && cleanedSegment.charAt(cleanedSegment.length - 1) !== "\n") cleanedSegment += "\n";
    } else {
        for (var i = 0; i < (dialoguesList || []).length; i++) {
            var dialogText = dialoguesList[i].text || "";
            var cleanItem = dialogText.replace(/【\d+】/g, "");
            if (isEndQuoteAutoAdded && i === dialoguesList.length - 1) cleanItem = cleanItem.replace(/\u201d$/, "");
            cleanedSegment += cleanItem + "\n";
        }
    }
    var cleanedNextContent = String(nextContent || "").replace(/【\d+】/g, "");
    cleanedSegment = repairNameAnalysisBatchText(cleanedSegment);
    cleanedNextContent = repairNameAnalysisBatchText(cleanedNextContent);
    var currentBoundary = cleanedSegment.length;
    var fullRawText = cleanedSegment + cleanedNextContent;
    var allCompleteBlocks = parseNameAnalysisOuterQuoteBlocks(fullRawText, currentBoundary);
    var totalQuoteCount = allCompleteBlocks.length;
    var stopAddIndex = totalQuoteCount <= 5 ? totalQuoteCount : Math.max(Math.floor(totalQuoteCount * SEQ_ADD_RATIO), 1);
    if (stopAddIndex > totalQuoteCount) stopAddIndex = totalQuoteCount;

    var quoteBlocks = [];
    var currentBlocks = [];
    var futureBlocks = [];
    var dialogTextMap = {};
    var expectedSeqs = [];
    for (var b = 0; b < stopAddIndex; b++) {
        var block = allCompleteBlocks[b];
        block.seq = padZero(b + 1, 2);
        quoteBlocks.push(block);
        expectedSeqs.push(block.seq);
        dialogTextMap[block.seq] = block.dialogText;
        if (block.source === "current") currentBlocks.push(block);
        else futureBlocks.push(block);
    }

    var contentParts = [];
    var lastPos = 0;
    for (var q = 0; q < quoteBlocks.length; q++) {
        contentParts.push(fullRawText.substring(lastPos, quoteBlocks[q].start));
        contentParts.push("【" + quoteBlocks[q].seq + "】");
        lastPos = quoteBlocks[q].start;
    }
    contentParts.push(fullRawText.substring(lastPos));
    var mapping = mapNameAnalysisDialoguesToBlocks(dialoguesList || [], currentBlocks);
    return {
        content: contentParts.join(""),
        sourceRawText: fullRawText,
        currentBoundary: currentBoundary,
        quoteBlocks: quoteBlocks,
        currentBlocks: currentBlocks,
        futureBlocks: futureBlocks,
        expectedSeqs: expectedSeqs,
        dialogTextMap: dialogTextMap,
        characterIdToSeq: mapping.characterIdToSeq || {},
        characterIdToBlock: mapping.characterIdToBlock || {},
        mappingOk: mapping.ok === true,
        mappingMethod: mapping.method || "",
        mappingErrors: mapping.errors || [],
        completeOuterQuoteCount: totalQuoteCount,
        numberedQuoteCount: quoteBlocks.length,
        singleTargetFallback: false
    };
}

// 批量映射无法可靠建立时，只给当前对白添加01序号，避免继续沿用可能错位的整批结果
function generateSingleTargetNameAnalysisBatch(currentDialogueText, characterId) {
    var cleaned = String(currentDialogueText || "").replace(/【\d+】/g, "").trim();
    cleaned = cleaned.replace(/^[\s\u3000]*“/, "").replace(/”[\s\u3000]*$/, "");
    var normalized = normalizeNameAnalysisDialogueText(cleaned);
    if (!normalized) {
        return { content: "", quoteBlocks: [], currentBlocks: [], futureBlocks: [], expectedSeqs: [], dialogTextMap: {}, characterIdToSeq: {}, characterIdToBlock: {}, mappingOk: false, mappingMethod: "single_target_failed", mappingErrors: ["single_target_text_empty"], singleTargetFallback: true };
    }
    var block = { start: 4, end: cleaned.length + 5, source: "current", rawText: "“" + cleaned + "”", dialogText: cleaned, normalizedText: normalized, complete: true, seq: "01" };
    var idKey = String(characterId || "");
    var idToSeq = {};
    var idToBlock = {};
    if (idKey) {
        idToSeq[idKey] = "01";
        idToBlock[idKey] = block;
    }
    return {
        content: "【01】“" + cleaned + "”",
        sourceRawText: "“" + cleaned + "”",
        currentBoundary: cleaned.length + 2,
        quoteBlocks: [block],
        currentBlocks: [block],
        futureBlocks: [],
        expectedSeqs: ["01"],
        dialogTextMap: { "01": cleaned },
        characterIdToSeq: idToSeq,
        characterIdToBlock: idToBlock,
        mappingOk: !!idKey,
        mappingMethod: "single_target_exact",
        mappingErrors: idKey ? [] : ["single_target_character_id_empty"],
        completeOuterQuoteCount: 1,
        numberedQuoteCount: 1,
        singleTargetFallback: true
    };
}


// 姓名分析对白缓存匹配：保留当前序号及前后各2项容错，并补充真正的远程命中/未命中观察
function matchDialogFromCache(currentDialogText, characterId) {
  var cache = readDialogCache();
  var dialogList = cache.dialogList;
  var currentIndex = cache.currentIndex;
  var MAX_FORWARD_OFFSET = 2;  // 保留原代码的向后索引容错范围
  var MAX_BACKWARD_OFFSET = 2; // 保留原代码的向前索引容错范围

  function firstDifferentCharacterIndex(left, right) {
      left = String(left || "");
      right = String(right || "");
      var limit = Math.min(left.length, right.length);
      for (var diffIndex = 0; diffIndex < limit; diffIndex++) if (left.charAt(diffIndex) !== right.charAt(diffIndex)) return diffIndex;
      return left.length === right.length ? -1 : limit;
  }

  function collectExactCacheIndices(cleanCurrentText) {
      var indices = [];
      if (!cleanCurrentText) return indices;
      for (var cacheIndex = 0; cacheIndex < (dialogList || []).length; cacheIndex++) {
          var lines = String((dialogList[cacheIndex] || {}).dialogContent || "").split("\n");
          for (var lineIndex = 0; lineIndex < lines.length; lineIndex++) {
              if (normalizeNameAnalysisCacheMatchText(lines[lineIndex]) === cleanCurrentText) {
                  indices.push(cacheIndex + 1);
                  break;
              }
          }
      }
      return indices;
  }

  function logCacheMiss(reason, extra) {
      var cleanCurrentForLog = normalizeNameAnalysisCacheMatchText(currentDialogText);
      var exactCacheIndices = collectExactCacheIndices(cleanCurrentForLog);
      var cacheCount = dialogList && dialogList.length ? dialogList.length : 0;
      var missClass = "cache_text_mismatch";
      var normalRefresh = false;
      if (reason === "cache_empty") {
          missClass = "cold_start";
          normalRefresh = true;
      } else if (reason === "cache_index_out_of_range" && currentIndex === cacheCount + 1) {
          missClass = "batch_exhausted";
          normalRefresh = true;
      } else if (reason === "cache_index_out_of_range") {
          missClass = "invalid_cursor";
      } else if (reason === "text_not_matched_in_current_and_offset_range" && cacheCount > 0 && currentIndex >= Math.max(1, cacheCount - 2) && exactCacheIndices.length === 0) {
          missClass = "tail_text_mismatch";
          normalRefresh = true;
      } else if (reason === "current_dialogue_empty_after_clean") {
          missClass = "current_dialogue_empty_after_clean";
      }

      // 中文注释：冷启动、正常读完一批、以及缓存尾部没有同文项时都交给下一批模型续判，不误清临时换声状态。
      try {
          var shouldClearTemporaryState = !normalRefresh;
          // 顺序跨章携带后，新章第一次通常必然是cache_empty；此时保留状态交给新批模型续判，不能当作跳章清理。
          if (reason === "cache_empty" && typeof characterManager !== "undefined" && characterManager && characterManager.temporaryVoiceStates) {
              for (var carriedKey in characterManager.temporaryVoiceStates) {
                  if (characterManager.temporaryVoiceStates.hasOwnProperty(carriedKey) && characterManager.temporaryVoiceStates[carriedKey] && characterManager.temporaryVoiceStates[carriedKey].crossChapterCarryPending === true) {
                      shouldClearTemporaryState = false;
                      break;
                  }
              }
          }
          if (shouldClearTemporaryState && typeof characterManager !== "undefined" && characterManager && characterManager.clearTemporaryVoiceStates && Object.keys(characterManager.temporaryVoiceStates || {}).length > 0) {
              graphRemoteLog("temporary_voice_state_discontinuity_cleared", { reason: "dialog_cache_discontinuity_" + (reason || "unknown"), clearedStateCount: Object.keys(characterManager.temporaryVoiceStates || {}).length });
              characterManager.clearTemporaryVoiceStates("dialog_cache_discontinuity_" + (reason || "unknown"));
          }
      } catch(tempDiscontinuityErr) {}
      if (!ENABLE_NAME_ANALYSIS_CACHE_TRACE) return;
      var cacheSnapshot = graphBuildNameAnalysisCacheSnapshot(dialogList || []);
      var candidateWindow = [];
      for (var snapshotIndex = 0; snapshotIndex < cacheSnapshot.length; snapshotIndex++) {
          var distance = cacheSnapshot[snapshotIndex].cacheIndex - currentIndex;
          if (distance >= -2 && distance <= 2) {
              var candidate = cacheSnapshot[snapshotIndex];
              candidate.offsetFromExpected = distance;
              candidate.firstDifferentCharacterIndex = firstDifferentCharacterIndex(cleanCurrentForLog, candidate.normalizedText);
              candidateWindow.push(candidate);
          }
      }
      var payload = {
          characterId: graphSafeString(characterId || "", 80),
          currentDialogueText: String(currentDialogText || ""),
          normalizedCurrentDialogueText: cleanCurrentForLog,
          currentDialogueHash: graphHash(cleanCurrentForLog),
          expectedCacheIndex: currentIndex,
          cacheDialogCount: cacheCount,
          reason: reason || "text_not_matched",
          missClass: missClass,
          normalRefresh: normalRefresh,
          cacheBatchId: cache.cacheBatchId || "",
          cacheCreatedChapterId: cache.cacheCreatedChapterId || "",
          cacheCreatedAt: cache.cacheCreatedAt || "",
          currentChapterId: graphCurrentChapterId(),
          exactTextMatchIndicesInFullCache: exactCacheIndices,
          nearestExactOffset: exactCacheIndices.length ? exactCacheIndices.reduce(function(best, indexValue) {
              var currentOffset = indexValue - currentIndex;
              return best === null || Math.abs(currentOffset) < Math.abs(best) ? currentOffset : best;
          }, null) : null,
          expectedCandidateWindow: candidateWindow,
          cacheDialogues: cacheSnapshot
      };
      extra = extra || {};
      for (var key in extra) if (extra.hasOwnProperty(key)) payload[key] = extra[key];
      graphRemoteLog("name_analysis_dialog_cache_miss", payload);
  }

  // 无有效缓存或游标越界时直接启动重新分析，不把它伪装成缓存命中
  if (!dialogList || dialogList.length === 0 || currentIndex < 1 || currentIndex > dialogList.length) {
      logCacheMiss(!dialogList || dialogList.length === 0 ? "cache_empty" : "cache_index_out_of_range");
      return null;
  }

  // 复用统一缓存清理口径，确保只在清理后的对白文本完全相等时命中
  var cleanCurrent = normalizeNameAnalysisCacheMatchText(currentDialogText);
  if (!cleanCurrent) {
      logCacheMiss("current_dialogue_empty_after_clean");
      return null;
  }
  var matchedResult = null;
  var finalMatchedIndex = -1;
  var matchedItem = null;
  var matchedLine = "";
  var matchDirection = "";
  var matchedOffset = 0;

  function tryCacheItem(arrayIndex, direction, offset) {
      if (arrayIndex < 0 || arrayIndex >= dialogList.length) return false;
      var item = dialogList[arrayIndex] || {};
      var cacheLines = String(item.dialogContent || "").split("\n").filter(function(line) { return line.trim() !== ""; });
      for (var lineIndex = 0; lineIndex < cacheLines.length; lineIndex++) {
          if (normalizeNameAnalysisCacheMatchText(cacheLines[lineIndex]) === cleanCurrent) {
              matchedItem = item;
              matchedLine = cacheLines[lineIndex];
              matchDirection = direction;
              matchedOffset = offset;
              finalMatchedIndex = arrayIndex + 1;
              matchedResult = {
                  name: item.name,
                  gender: item.gender,
                  age: item.age,
                  __voiceAgeEvidence: Array.isArray(item.voiceAgeEvidence) ? item.voiceAgeEvidence : [],
                  __dialogCacheMeta: {
                      matchedCacheIndex: arrayIndex + 1,
                      matchedSeq: graphSafeString(item.seq || padZero(arrayIndex + 1, 2), 20),
                      currentDialogueHash: graphHash(cleanCurrent),
                      bookKey: cache.temporaryVoiceSnapshot && cache.temporaryVoiceSnapshot.bookKey || (typeof characterManager !== "undefined" && characterManager ? characterManager.aliasGraphBookKey : ""),
                      chapterId: cache.temporaryVoiceSnapshot && cache.temporaryVoiceSnapshot.chapterId || graphCurrentChapterId(),
                      temporaryVoiceSnapshot: cache.temporaryVoiceSnapshot || null,
                      source: "dialog_cache_hit"
                  }
              };
              return true;
          }
      }
      return false;
  }

  var currentArrayIndex = currentIndex - 1;
  tryCacheItem(currentArrayIndex, "current", 0);
  if (!matchedResult) {
      for (var backwardOffset = 1; backwardOffset <= MAX_BACKWARD_OFFSET; backwardOffset++) {
          if (tryCacheItem(currentArrayIndex - backwardOffset, "backward", -backwardOffset)) break;
      }
  }
  if (!matchedResult) {
      for (var forwardOffset = 1; forwardOffset <= MAX_FORWARD_OFFSET; forwardOffset++) {
          if (tryCacheItem(currentArrayIndex + forwardOffset, "forward", forwardOffset)) break;
      }
  }

  // 匹配成功后推进缓存游标，并记录真正的“原对白—序号—角色名”链路
  if (matchedResult && finalMatchedIndex > 0) {
      cache.currentIndex = finalMatchedIndex + 1;
      writeDialogCache(cache);
      if (ENABLE_NAME_ANALYSIS_CACHE_TRACE) {
          graphRemoteLog("name_analysis_dialog_cache_hit", {
              characterId: graphSafeString(characterId || "", 80),
              currentDialogueText: String(currentDialogText || ""),
              currentDialogueHash: graphHash(cleanCurrent),
              expectedCacheIndex: currentIndex,
              matchedCacheIndex: finalMatchedIndex,
              matchedSeq: graphSafeString(matchedItem && matchedItem.seq || padZero(finalMatchedIndex, 2), 20),
              matchDirection: matchDirection,
              offset: matchedOffset,
              cachedDialogueText: String(matchedLine || matchedItem && matchedItem.dialogContent || ""),
              cachedName: graphNormalizeName(matchedItem && matchedItem.name || ""),
              cachedGender: matchedItem && matchedItem.gender || "",
              cachedAge: matchedItem && matchedItem.age || "",
              voiceAgeEvidenceCount: matchedItem && Array.isArray(matchedItem.voiceAgeEvidence) ? matchedItem.voiceAgeEvidence.length : 0,
              temporaryVoiceSnapshotPresent: !!cache.temporaryVoiceSnapshot,
              exactTextMatched: true,
              nextCacheIndex: cache.currentIndex
          });
      }
      return matchedResult;
  }

  logCacheMiss("text_not_matched_in_current_and_offset_range", { maxBackwardOffset: MAX_BACKWARD_OFFSET, maxForwardOffset: MAX_FORWARD_OFFSET });
  return null;
}




CharacterManager.prototype.analyzeCharacterFallback = function(fullText, characterId, reason) {
  // 对齐或API重试耗尽时固定走duihua，不再随机生成性别年龄，也不允许后续创建角色卡或写图谱
  return { name: "未知", gender: "", age: "", __safeDialogueFallback: true, fallbackReason: reason || "name_analysis_fallback" };
};








function graphV908NormalizeGenderForVoice(gender, age) {
  gender = graphSafeString(gender || "", 20);
  age = graphSafeString(age || "", 30);
  if (gender === "女" || gender.indexOf("女") >= 0 || age.indexOf("女") >= 0 || age.indexOf("妇") >= 0 || age.indexOf("少女") >= 0) return "女";
  if (gender === "男" || gender.indexOf("男") >= 0 || age.indexOf("男") >= 0 || age.indexOf("少年") >= 0 || age.indexOf("男子") >= 0 || age.indexOf("老者") >= 0 || age.indexOf("修士") >= 0 || age.indexOf("道士") >= 0) return "男";
  if (gender.indexOf("特殊") >= 0 || age.indexOf("旁白") >= 0 || age.indexOf("系统") >= 0) return "特殊";
  return gender || "男";
}

function graphV908NormalizeAgeForVoice(gender, age) {
  age = graphSafeString(age || "", 40);
  var g = graphV908NormalizeGenderForVoice(gender, age);
  if (!age) return g === "女" ? "女青年" : (g === "特殊" ? "系统" : "男青年");
  // 少年必须继续结合gender归一，不能在这里提前返回；否则“女+少年”会绕过后面的少女分支。
  if (age === "男青年" || age === "男中年" || age === "男老年" || age === "男童" || age === "女青年" || age === "女中年" || age === "女老年" || age === "女童" || age === "少女" || age === "系统" || age === "旁白" || age === "男主" || age === "女主") return age;
  if (age.indexOf("旁白") >= 0) return "旁白";
  if (age.indexOf("系统") >= 0 || g === "特殊") return "系统";
  if (age.indexOf("女童") >= 0 || age.indexOf("女娃") >= 0) return "女童";
  if (age.indexOf("男童") >= 0) return "男童";
  if (age.indexOf("幼") >= 0 || age.indexOf("童") >= 0 || age.indexOf("小孩") >= 0 || age.indexOf("孩童") >= 0) return g === "女" ? "女童" : "男童";
  if (age.indexOf("少女") >= 0 || age.indexOf("小姑娘") >= 0) return "少女";
  if (age.indexOf("少年") >= 0) return g === "女" ? "少女" : "少年";
  if (age.indexOf("老") >= 0 || age.indexOf("老妇") >= 0 || age.indexOf("老妪") >= 0 || age.indexOf("婆") >= 0) return g === "女" ? "女老年" : "男老年";
  if (age.indexOf("中年") >= 0 || age.indexOf("壮年") >= 0) return g === "女" ? "女中年" : "男中年";
  if (age.indexOf("青年") >= 0 || age.indexOf("年轻") >= 0 || age.indexOf("成年") >= 0 || age.indexOf("女子") >= 0 || age.indexOf("男子") >= 0 || age.indexOf("修士") >= 0 || age.indexOf("道士") >= 0) return g === "女" ? "女青年" : "男青年";
  if (age === "青年") return g === "女" ? "女青年" : "男青年";
  if (age === "中年") return g === "女" ? "女中年" : "男中年";
  if (age === "老年") return g === "女" ? "女老年" : "男老年";
  return age;
}

function graphV908VoiceSegmentKey(gender, age) {
  var g = graphV908NormalizeGenderForVoice(gender, age);
  var a = graphV908NormalizeAgeForVoice(g, age);
  return g + "/" + a;
}

function graphV908NormalizeVoiceAssignGenderAge(gender, age) {
  // 发音人分配统一走声音年龄段归一化，避免“男/中年”“男/老年”因精确匹配失败落到男青年兜底。
  var g = graphV908NormalizeGenderForVoice(gender || "", age || "");
  var a = graphV908NormalizeAgeForVoice(g, age || "");
  return { gender: g, age: a };
}

function graphV908VoiceInfoByTag(voiceTag) {
  voiceTag = graphSafeString(voiceTag || "", 80);
  if (!voiceTag || typeof GENSHIN_CHARACTERS === "undefined") return null;
  for (var k in GENSHIN_CHARACTERS) {
    if (!GENSHIN_CHARACTERS.hasOwnProperty(k)) continue;
    var info = GENSHIN_CHARACTERS[k] || {};
    if (info.voice === voiceTag) return info;
  }
  return null;
}

function graphV908VoiceSegmentFromVoice(voiceTag) {
  var info = graphV908VoiceInfoByTag(voiceTag);
  if (!info) return "";
  return graphV908VoiceSegmentKey(info.gender || "", info.age || "");
}

function graphV908VoiceSegmentOfRecord(record) {
  if (!record) return "";
  var byVoice = graphV908VoiceSegmentFromVoice(record.voice || record.voiceId || "");
  if (byVoice) return byVoice;
  return graphV908VoiceSegmentKey(record.gender || "", record.age || "");
}

function graphV908IsMainRoleVoiceTag(voiceTag) {
  voiceTag = graphSafeString(voiceTag || "", 80);
  if (!voiceTag) return false;
  if (/^(男主|女主)\d+$/.test(voiceTag)) return true;
  var info = graphV908VoiceInfoByTag(voiceTag);
  if (!info) return false;
  return info.gender === "主角" || info.age === "男主" || info.age === "女主";
}

function graphV908FixedVoiceTagOfRecord(record) {
  if (!record) return "";
  // 只读取显式固定音色字段；普通自动分配出来的 voice/voiceId 不能当 fixedVoiceTag，否则普通角色会被误判为固定发音人。
  return graphSafeString(record.fixedVoiceTag || record.manualFixedVoiceTag || record.lockedVoiceTag || record.voiceLockTag || record.fixedTag || "", 80);
}

function graphV908IsFixedVoiceRecord(record) {
  if (!record) return false;
  if (typeof ENABLE_FIXED_VOICE_HARD_LOCK !== "undefined" && !ENABLE_FIXED_VOICE_HARD_LOCK) return false;
  // 用户显式取消固定后，优先于旧缓存字段和男/女主自动保护；再次手动固定时会清除此标记。
  if (record.manualVoiceUnlock === true) return false;
  var voice = graphSafeString(record.voice || record.voiceId || "", 80);
  var fixedTag = graphV908FixedVoiceTagOfRecord(record);

  // 任何显式固定标记都硬锁，不再只保护男主/女主。
  if (typeof ENABLE_FIXED_VOICE_EXPLICIT_LOCK_ALL_ROLES === "undefined" || ENABLE_FIXED_VOICE_EXPLICIT_LOCK_ALL_ROLES) {
    if (record.voiceLocked === true || record.fixedVoiceLocked === true || record.manualVoiceLocked === true || record.fixedVoice === true || record.lockVoice === true || record.isVoiceFixed === true || record.userFixedVoice === true) return true;
    if (record.fixedVoiceAt || record.fixedVoiceReason || record.voiceLockReason || record.voiceLockedAt || record.manualFixedVoiceAt) return true;
    if (record.fixedVoiceTag || record.manualFixedVoiceTag || record.lockedVoiceTag || record.voiceLockTag || record.fixedTag) return true;
  }

  // 旧版如果已经写了 fixedVoiceTag，但当前 voice 被误改，也仍然视为固定，后续会恢复 fixedVoiceTag。
  if (fixedTag && voice && fixedTag === voice) return true;

  // 男主/女主音色自动保护，兼容旧缓存无显式锁字段的情况。
  if (typeof ENABLE_MAIN_ROLE_VOICE_AUTO_LOCK === "undefined" || ENABLE_MAIN_ROLE_VOICE_AUTO_LOCK) {
    if (graphV908IsMainRoleVoiceTag(voice || fixedTag)) return true;
  }

  // 仅usageCount=100无法可靠区分“普通新角色”和“旧版手动固定”，默认关闭。用户确需迁移旧缓存时可手动开启。
  if (typeof ENABLE_LEGACY_USAGE100_VOICE_LOCK_MIGRATION !== "undefined" && ENABLE_LEGACY_USAGE100_VOICE_LOCK_MIGRATION) {
    if (Number(record.usageCount || 0) === 100 && (voice || fixedTag)) return true;
  }
  return false;
}

function graphV908MarkFixedVoiceRecord(record, reason, forceCaptureCurrentVoice) {
  if (!record) return false;
  var voice = graphSafeString(record.voice || record.voiceId || "", 80);
  if (!voice) return false;
  var oldFixedTag = graphV908FixedVoiceTagOfRecord(record);
  record.voiceLocked = true;
  record.fixedVoiceLocked = true;
  record.manualVoiceLocked = true;
  // 普通锁状态刷新不得把已经锁定的原音色覆盖成漂移后的音色；只有用户主动固定时才强制重新取当前值。
  if (forceCaptureCurrentVoice === true || !oldFixedTag) record.fixedVoiceTag = voice;
  record.manualVoiceUnlock = false;
  record.fixedVoiceAt = graphNowIso();
  record.fixedVoiceReason = graphSafeString(reason || "manual_fixed_voice", 120);
  record.fixedVoiceSegment = graphV908VoiceSegmentOfRecord(record);
  return true;
}

function graphV908CancelFixedVoiceRecord(record) {
  if (!record) return false;
  var fields = ["fixedVoiceTag", "manualFixedVoiceTag", "lockedVoiceTag", "voiceLockTag", "fixedTag", "voiceLocked", "fixedVoiceLocked", "manualVoiceLocked", "fixedVoice", "lockVoice", "isVoiceFixed", "userFixedVoice", "fixedVoiceAt", "fixedVoiceReason", "voiceLockReason", "voiceLockedAt", "manualFixedVoiceAt", "fixedVoiceSegment"];
  for (var i = 0; i < fields.length; i++) {
    try { delete record[fields[i]]; } catch(e) { record[fields[i]] = undefined; }
  }
  record.manualVoiceUnlock = true;
  return true;
}

CharacterManager.prototype.enforceFixedVoiceRecordV908 = function(record, reason) {
  if (!record || !graphV908IsFixedVoiceRecord(record)) return { locked: false, tag: record && record.voice || "" };
  var lockedTag = graphV908FixedVoiceTagOfRecord(record) || graphSafeString(record.voice || record.voiceId || "", 80);
  var beforeVoice = graphSafeString(record.voice || record.voiceId || "", 80);
  if (lockedTag && beforeVoice !== lockedTag) record.voice = lockedTag;
  var clearedTemporaryState = false;
  try {
    var stateInfo = this.findTemporaryVoiceStateForRecord ? this.findTemporaryVoiceStateForRecord(record) : null;
    if (stateInfo && stateInfo.key) {
      if (this.endTemporaryVoiceState) this.endTemporaryVoiceState(stateInfo.key, "fixed_voice_hard_lock", null);
      else if (this.temporaryVoiceStates) delete this.temporaryVoiceStates[stateInfo.key];
      clearedTemporaryState = true;
    }
  } catch(e0) {}
  graphRemoteLog("character_fixed_voice_lock_kept", {
    name: graphNormalizeName(record.name || ""),
    recordId: record.recordId || "",
    fixedVoiceTag: lockedTag,
    beforeVoice: beforeVoice,
    restoredFixedVoice: !!lockedTag && beforeVoice !== lockedTag,
    clearedTemporaryState: clearedTemporaryState,
    reason: graphSafeString(reason || "fixed_voice_hard_lock", 160)
  });
  return { locked: true, tag: lockedTag || beforeVoice || "default", clearedTemporaryState: clearedTemporaryState };
};

CharacterManager.prototype.saveAgeVoiceBindingBackup = function(record, fromSegment, toSegment, reason) {
  if (typeof ENABLE_AGE_VOICE_BINDING_BACKUP !== "undefined" && !ENABLE_AGE_VOICE_BINDING_BACKUP) return false;
  if (!record || !record.name) return false;
  if (!record.voice && !record.age) return false;
  if (!record.ageVoiceBindingBackups || !Array.isArray(record.ageVoiceBindingBackups)) record.ageVoiceBindingBackups = [];
  fromSegment = fromSegment || graphV908VoiceSegmentOfRecord(record);
  if (!fromSegment) return false;
  var backup = {
    schema: "v908_age_voice_binding_backup",
    status: "active",
    recordId: record.recordId || "",
    name: graphNormalizeName(record.name || ""),
    aliases: record.aliases || record.name || "",
    gender: record.gender || "",
    age: record.age || "",
    voice: record.voice || "",
    segmentKey: fromSegment,
    savedBeforeSegment: toSegment || "",
    savedAtChapter: graphCurrentChapterId(),
    savedAt: graphNowIso(),
    reason: graphSafeString(reason || "", 220)
  };
  for (var i = record.ageVoiceBindingBackups.length - 1; i >= 0; i--) {
    var old = record.ageVoiceBindingBackups[i] || {};
    if (old.segmentKey === backup.segmentKey && old.voice === backup.voice && old.age === backup.age && old.gender === backup.gender) {
      old.status = "active";
      old.savedBeforeSegment = backup.savedBeforeSegment;
      old.lastRefreshChapter = backup.savedAtChapter;
      old.lastRefreshAt = backup.savedAt;
      old.reason = backup.reason;
      graphRemoteLog("character_age_voice_binding_backup_saved", { name: backup.name, action: "refresh", segmentKey: backup.segmentKey, voice: backup.voice, age: backup.age, gender: backup.gender, savedBeforeSegment: backup.savedBeforeSegment, reason: backup.reason });
      return true;
    }
  }
  record.ageVoiceBindingBackups.push(backup);
  var limit = parseInt(typeof AGE_VOICE_BINDING_BACKUP_LIMIT !== "undefined" ? AGE_VOICE_BINDING_BACKUP_LIMIT : 12, 10) || 12;
  while (record.ageVoiceBindingBackups.length > limit) record.ageVoiceBindingBackups.shift();
  graphRemoteLog("character_age_voice_binding_backup_saved", { name: backup.name, action: "create", segmentKey: backup.segmentKey, voice: backup.voice, age: backup.age, gender: backup.gender, savedBeforeSegment: backup.savedBeforeSegment, backupCount: record.ageVoiceBindingBackups.length, reason: backup.reason });
  return true;
};

CharacterManager.prototype.findAgeVoiceBindingBackup = function(record, targetSegment) {
  if (!record || !record.ageVoiceBindingBackups || !Array.isArray(record.ageVoiceBindingBackups)) return null;
  targetSegment = targetSegment || "";
  if (!targetSegment) return null;
  var recordId = record.recordId || "";
  var name = graphNormalizeName(record.name || "");
  for (var i = record.ageVoiceBindingBackups.length - 1; i >= 0; i--) {
    var b = record.ageVoiceBindingBackups[i] || {};
    if (b.status && b.status !== "active") continue;
    if (b.segmentKey !== targetSegment) continue;
    if (recordId && b.recordId && b.recordId !== recordId) continue;
    if (b.name && name && b.name !== name) continue;
    if (!b.voice) continue;
    if (this.isVoiceAvailable && !this.isVoiceAvailable(b.voice)) continue;
    return { index: i, backup: b };
  }
  return null;
};

// 初始化CharacterManager
var characterManager = new CharacterManager();
characterManager.loadRecords();

// -------------------------- SpeechRuleJS核心对象（整合＜＞本地音效） --------------------------
var SpeechRuleJS = {
  name: "多角色朗读2.85发音人轮询+增强别名检验v90.8同步显示名",
  id: "mingwuyan_v908",
  author: "命無言、萌新、M",
  version: 20260809,
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
          "括号4": "『』括号发音人"
      };

              // 加入GENSHIN_CHARACTERS发音人标签
      for (var name in GENSHIN_CHARACTERS) {
          if (GENSHIN_CHARACTERS.hasOwnProperty(name)) {
              var info = GENSHIN_CHARACTERS[name];
              tags[info.voice.toString()] = name.toString(); // 规避：属性转原始String
          }
      }


      // 循环添加localSound1~localSound100
      for (var i = 1; i <= 100; i++) {
          var tagKey = ("localSound" + i).toString(); // 规避：tagKey转原始String
          var tagName = ("本地音效" + i).toString(); // 规避：tagName转原始String
          tags[tagKey] = tagName;
      }
      return tags;
  })(),


  tagsData: (function() {
      var 统一Hint = "\n       “轰隆”  “轰隆！” “轰隆。。”\n         输入 轰隆  就可匹配，\n       支持用|分隔多个拟声词，@/＜/＞开头为正则（＜前插/＞后插/@替换）";
      
      var tagsData = {
          // 对话标签配置
          duihua: {
              role: {
                  label: "角色名",
                  hint: "输入角色关键词（如“张三”“主角”）"
              },
                // 整合性别+年龄为单选择框，格式：男/青年
              genderAge: {
                  label: "性别/年龄",
                  hint: "选择角色的性别和年龄阶段",
                  items: '{男/少年: "男/少年",男/男青年: "男/男青年",男/男中年: "男/男中年",男/男老年: "男/男老年",男/男孩: "男/男孩",女/女童: "女/女童",女/少女: "女/少女",女/女青年: "女/女青年",女/女中年: "女/女中年",女/女老年: "女/女老年",男/主角: "男/主角",女/主角: "女/主角"}',
                  default: '男/青年'
               }

          }
      };

      // 循环添加localSound1~localSound100
      for (var i = 1; i <= 100; i++) {
          var tagKey = ("localSound" + i).toString();
          var label = ("音频名称（本地音效" + i + "）").toString();
          tagsData[tagKey] = {
              audioName: {
                  label: label,
                  hint: 统一Hint
              }
          };
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
          var rsTag = genshinTagKey;
          //console.log("GENSHIN生效！tag=", tag, "生成tagName=", rsTag);
          return rsTag;
      }

      // 标签扩容兜底：GENSHIN_CHARACTERS 仅含初始序号(如女青年01~100)，
      // 配置项可能有序号超出范围的 tag(如女青年517)，GENSHIN 循环匹配不到。
      // 检查 tag 是否符合已知发音人前缀+序号模式，若是则视为扩容标签，tagName = tag。
      if (GENSHIN_CHARACTERS && tag) {
          var _voicePrefixes = {};
          for (var _pk in GENSHIN_CHARACTERS) {
              if (Object.prototype.hasOwnProperty.call(GENSHIN_CHARACTERS, _pk)) {
                  var _vpre = GENSHIN_CHARACTERS[_pk].voice.toString().replace(/\d+$/, '');
                  if (_vpre) _voicePrefixes[_vpre] = true;
              }
          }
          var _pm = tag.match(/^(.+?)(\d+)$/);
          if (_pm && _voicePrefixes[_pm[1]]) {
              return tag;
          }
      }

      // 2. duihua标签处理（括号完全匹配，复用GENSHIN逻辑）
      if ("duihua" == tag) {
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
  
          // 最终拼接（括号不变）
          var rsTag = rolePart + genderAgeWhole;
  
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
  
      // Rhino 兼容：把 Java LinkedHashMap 转成纯 JS 对象
      var _origTagsData = tagsData;
      if (tagsData && typeof tagsData.get === 'function' && !Object.prototype.hasOwnProperty.call(tagsData, 'duihua')) {
          var _converted = {};
          try {
              var _entrySet = tagsData.entrySet();
              var _iter = _entrySet.iterator();
              while (_iter.hasNext()) {
                  var _entry = _iter.next();
                  var _key = String(_entry.getKey());
                  var _val = _entry.getValue();
                  // 递归转换内层 Map
                  if (_val && typeof _val.get === 'function' && !_val.length) {
                      var _inner = {};
                      var _innerIter = _val.entrySet().iterator();
                      while (_innerIter.hasNext()) {
                          var _innerEntry = _innerIter.next();
                          var _innerKey = String(_innerEntry.getKey());
                          var _innerVal = _innerEntry.getValue();
                          // List 转数组
                          if (_innerVal && typeof _innerVal.size === 'function' && !_innerVal.length) {
                              var _arr = [];
                              for (var _ai = 0; _ai < _innerVal.size(); _ai++) {
                                  var _aItem = _innerVal.get(_ai);
                                  if (_aItem && typeof _aItem.get === 'function' && !_aItem.length) {
                                      var _aObj = {};
                                      var _aIter = _aItem.entrySet().iterator();
                                      while (_aIter.hasNext()) {
                                          var _aEntry = _aIter.next();
                                          _aObj[String(_aEntry.getKey())] = String(_aEntry.getValue());
                                      }
                                      _arr.push(_aObj);
                                  } else {
                                      _arr.push(_aItem);
                                  }
                              }
                              _inner[_innerKey] = _arr;
                          } else {
                              _inner[_innerKey] = _innerVal;
                          }
                      }
                      _converted[_key] = _inner;
                  } else {
                      _converted[_key] = _val;
                  }
              }
              tagsData = _converted;
          } catch(convErr) {
              console.error("tagsData转换异常: " + convErr.message);
          }
      }
      // 方案B兜底：确保 this.characterManager 指向全局 characterManager
      if (!this.characterManager) {
          this.characterManager = characterManager;
      }

  
  
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
      
      
  
      
  
  
  
  
  
  
  
  
  
  
  
      
      
      // 动态扩展角色表
      try {
          var batchPrefixMaxSeq = {};
          var batchPrefixInfo = {};
          for (var bi = 0; bi < BATCH_ROLES.length; bi++) {
              var bItem = BATCH_ROLES[bi];
              var bType = bItem[0], bGender = bItem[1], bAge = bItem[2], bVoicePre = bItem[3];
              var bPrefix = bType.substring(bType.lastIndexOf('/') + 1);
              batchPrefixInfo[bPrefix] = { gender: bGender, age: bAge, voicePre: bVoicePre };
              batchPrefixMaxSeq[bPrefix] = 0;
          }
          for (var gName in GENSHIN_CHARACTERS) {
              if (GENSHIN_CHARACTERS.hasOwnProperty(gName)) {
                  var gVoice = GENSHIN_CHARACTERS[gName].voice.toString();
                  var gMatch = gVoice.match(/^([\u4E00-\u9FA5]+?)(\d+)$/);
                  if (gMatch && batchPrefixMaxSeq.hasOwnProperty(gMatch[1])) {
                      var gSeq = parseInt(gMatch[2], 10);
                      if (gSeq > batchPrefixMaxSeq[gMatch[1]]) {
                          batchPrefixMaxSeq[gMatch[1]] = gSeq;
                      }
                  }
              }
          }
          var neededMaxSeq = {};
          for (var tdKey in tagsData) {
              if (Object.prototype.hasOwnProperty.call(tagsData, tdKey) || typeof tagsData.get === 'function') {
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
          for (var extPrefix in neededMaxSeq) {
              if (neededMaxSeq.hasOwnProperty(extPrefix)) {
                  var extInfo = batchPrefixInfo[extPrefix];
                  var curMax = batchPrefixMaxSeq[extPrefix];
                  var needMax = neededMaxSeq[extPrefix];
                  if (needMax > curMax) {
                      for (var ni = curMax + 1; ni <= needMax; ni++) {
                          var nSeq = padZero(ni, 2);
                          var nVoice = extInfo.voicePre + nSeq;
                          GENSHIN_CHARACTERS[nVoice] = { gender: extInfo.gender, age: extInfo.age, voice: nVoice };
                          SpeechRuleJS.tags[nVoice] = nVoice;
                      }
                      batchPrefixMaxSeq[extPrefix] = needMax;
                  }
              }
          }
      } catch (extErr) {
          console.error("动态扩展角色表异常: " + extErr.message);
      }
      text2 = text.replace(/[(]([\u4E00-\u9Fa5]{1,5})音效[)]/g, "");
      nameAnalysisContextMeta = null;
      
      // 短纯汉字引号内容继续保留给姓名分析模型，禁止提前去引号造成后续序号整体错位
      text = text.replace(/[〖〗‘’〈〔〕〉]/g, "");
      
      text = text.replace(/(“[^“”]+)$/g, "$1”");
      text = text.replace(/(^|音效[)])([^“”)]+”)/g, "$1“$2");
      
      text = text.replace(/[【「『]([\u4E00-\u9Fa5]+)[】』」]/g, "$1");
      
      text = text.replace(/(“[^“”\n]*)[【「『』」】]([^“”\n]*”)/g, "$1$2");
      text = text.replace(/(“[^“”\n]*)[【「『』」】]([^“”\n]*”)/g, "$1$2");
      text = text.replace(/(“[^“”\n]*)[【「『』」】]([^“”\n]*”)/g, "$1$2");
      text = text.replace(/(“[^“”\n]*)[【「『』」】]([^“”\n]*”)/g, "$1$2");
 //     text = text.replace(/(^|”)([^a-zA-Z0-9\u4e00-\u9fa5\n]+)($)/g, "$1（时间转场音效）$3");
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
      // 生成1~100完整本地音效标签数组
      var allLocalSoundTags = [];
      for (var i = 1; i <= 100; i++) {
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






      // ===================== gengxin安全合并与书籍切换载荷 =====================
      function graphV908JsonClone(obj) {
          try { return JSON.parse(JSON.stringify(obj || {})); } catch(e) { return {}; }
      }
      function graphV908RecordName(rec) {
          rec = rec || {};
          return graphNormalizeName(rec.name || rec.mainName || rec.displayName || "");
      }
      function graphV908AliasList(rec) {
          var out = [];
          var seen = {};
          function add(v) {
              v = graphNormalizeName(v || "");
              if (!v || seen[v]) return;
              seen[v] = true;
              out.push(v);
          }
          rec = rec || {};
          add(rec.name || rec.mainName || "");
          var aliases = rec.aliases;
          if (aliases && Array.isArray(aliases)) {
              for (var i = 0; i < aliases.length; i++) add(aliases[i]);
          } else if (aliases !== undefined && aliases !== null) {
              var aliasText = String(aliases || "");
              var parts = aliasText.split(/[|｜,，、;；\/]+/);
              for (var j = 0; j < parts.length; j++) add(parts[j]);
          }
          if (rec.aliasesText) {
              var parts2 = String(rec.aliasesText || "").split(/[|｜,，、;；\/]+/);
              for (var k = 0; k < parts2.length; k++) add(parts2[k]);
          }
          return out;
      }
      function graphV908AliasTextFromRecords(oldRec, incomingRec) {
          var arr = [];
          var seen = {};
          function add(v) {
              v = graphNormalizeName(v || "");
              if (!v || seen[v]) return;
              seen[v] = true;
              arr.push(v);
          }
          var oldList = graphV908AliasList(oldRec || {});
          var newList = graphV908AliasList(incomingRec || {});
          for (var i = 0; i < oldList.length; i++) add(oldList[i]);
          for (var j = 0; j < newList.length; j++) add(newList[j]);
          if (!arr.length) add(graphV908RecordName(incomingRec || oldRec || {}));
          return arr.join("|");
      }
      function graphV908ChapterList(rec) {
          rec = rec || {};
          var out = [];
          var seen = {};
          function add(v) {
              v = graphSafeString(v || "", 40);
              if (!v || seen[v]) return;
              seen[v] = true;
              out.push(v);
          }
          if (rec.chapters && Array.isArray(rec.chapters)) {
              for (var i = 0; i < rec.chapters.length; i++) add(rec.chapters[i]);
          }
          if (rec.lastSeenChapter) add(rec.lastSeenChapter);
          try { out = graphTrimChapterArray(out); } catch(e) {}
          return out;
      }
      function graphV908BuildRecordIndex(records) {
          var mainIndex = {}, aliasIndex = {};
          records = records || [];
          function addAlias(alias, idx) {
              alias = graphNormalizeName(alias || "");
              if (!alias) return;
              if (!aliasIndex[alias]) aliasIndex[alias] = [];
              if (aliasIndex[alias].indexOf(idx) === -1) aliasIndex[alias].push(idx);
          }
          for (var i = 0; i < records.length; i++) {
              var rec = records[i] || {};
              var name = graphV908RecordName(rec);
              if (name && mainIndex[name] === undefined) mainIndex[name] = i;
              var aliases = graphV908AliasList(rec);
              for (var j = 0; j < aliases.length; j++) addAlias(aliases[j], i);
          }
          return { mainIndex: mainIndex, aliasIndex: aliasIndex };
      }
      function graphV908CountEmptyChapters(records) {
          var count = 0;
          records = records || [];
          for (var i = 0; i < records.length; i++) {
              var rec = records[i] || {};
              if (!rec.chapters || !Array.isArray(rec.chapters) || rec.chapters.length === 0) count++;
          }
          return count;
      }
      function graphV908RebuildVoiceUsage(cm) {
          if (!cm || !cm.characterRecords) return;
          cm.usedVoices = {};
          cm.voiceUsageMap = {};
          for (var i = 0; i < cm.characterRecords.length; i++) {
              var r = cm.characterRecords[i] || {};
              if (r.voice) {
                  var tag = String(r.voice || "");
                  cm.usedVoices[tag] = true;
                  cm.voiceUsageMap[tag] = (cm.voiceUsageMap[tag] || 0) + 1;
              }
          }
          try { cm.nameToMainNameMap = null; } catch(e) {}
      }
      function graphV908MergeOneExternalRecord(oldRec, incomingRec, matchBy) {
          oldRec = oldRec || {};
          incomingRec = incomingRec || {};
          var merged = graphV908JsonClone(oldRec);
          var allowed = ["gender", "age", "voice", "voiceId", "usageCount"];
          if (matchBy !== "alias") {
              allowed.push("name");
              allowed.push("mainName");
          }
          for (var i = 0; i < allowed.length; i++) {
              var k = allowed[i];
              if (incomingRec.hasOwnProperty(k) && incomingRec[k] !== undefined && incomingRec[k] !== null && String(incomingRec[k]) !== "") {
                  if (k === "mainName") merged.name = incomingRec[k]; else merged[k] = incomingRec[k];
              }
          }
          merged.aliases = graphV908AliasTextFromRecords(oldRec, incomingRec);

          var oldChapters = graphV908ChapterList(oldRec);
          var incomingChapters = graphV908ChapterList(incomingRec);
          var chapterSeen = {}, chapters = [];
          function addChapter(c) { c = graphSafeString(c || "", 40); if (!c || chapterSeen[c]) return; chapterSeen[c] = true; chapters.push(c); }
          for (var c1 = 0; c1 < oldChapters.length; c1++) addChapter(oldChapters[c1]);
          for (var c2 = 0; c2 < incomingChapters.length; c2++) addChapter(incomingChapters[c2]);
          merged.chapters = chapters.length ? graphTrimChapterArray(chapters) : [];
          if (oldRec.lastSeenChapter) merged.lastSeenChapter = oldRec.lastSeenChapter;
          else if (incomingRec.lastSeenChapter) merged.lastSeenChapter = incomingRec.lastSeenChapter;

          var preserveIfOld = ["recordId", "genderAgeHistory", "ageVoiceBindingBackups", "voiceSegmentBackups", "voiceBindingBackups", "backupRecord", "mergedRecords", "merged", "mergedInto", "fixedVoiceTag", "manualFixedVoiceTag", "lockedVoiceTag", "voiceLockTag", "fixedTag", "voiceLocked", "fixedVoiceLocked", "manualVoiceLocked", "fixedVoice", "lockVoice", "isVoiceFixed", "userFixedVoice", "fixedVoiceAt", "fixedVoiceReason", "voiceLockReason", "voiceLockedAt", "manualFixedVoiceAt", "fixedVoiceSegment", "manualVoiceUnlock"];
          for (var p = 0; p < preserveIfOld.length; p++) {
              var pk = preserveIfOld[p];
              if (oldRec.hasOwnProperty(pk)) merged[pk] = oldRec[pk];
              else if (incomingRec.hasOwnProperty(pk)) merged[pk] = incomingRec[pk];
          }
          return merged;
      }
      function graphV908MergeExternalCharacterRecords(cm, incomingRecords, sourceStage) {
          if (!cm) return { ok: false, reason: "manager_missing" };
          incomingRecords = incomingRecords || [];
          var oldRecords = (cm.characterRecords && Array.isArray(cm.characterRecords)) ? cm.characterRecords : [];
          var index = graphV908BuildRecordIndex(oldRecords);
          var result = [];
          var usedOld = {};
          var stats = { incomingCount: incomingRecords.length, oldCount: oldRecords.length, matchedByMainName: 0, matchedByAlias: 0, aliasConflict: 0, newCount: 0, preservedOldCount: 0, skippedInvalid: 0, preservedRecordIdCount: 0, preservedChaptersCount: 0, emptyChaptersBefore: graphV908CountEmptyChapters(oldRecords), emptyChaptersAfter: 0 };
          for (var i = 0; i < incomingRecords.length; i++) {
              var inc = incomingRecords[i] || {};
              var incomingName = graphV908RecordName(inc);
              if (!incomingName) { stats.skippedInvalid++; continue; }
              var matchIndex = -1;
              var matchBy = "";
              if (index.mainIndex[incomingName] !== undefined) {
                  matchIndex = index.mainIndex[incomingName];
                  matchBy = "mainName";
              } else {
                  var aliases = graphV908AliasList(inc);
                  for (var ai = 0; ai < aliases.length; ai++) {
                      var arr = index.aliasIndex[aliases[ai]] || [];
                      if (arr.length === 1) { matchIndex = arr[0]; matchBy = "alias"; break; }
                      if (arr.length > 1) {
                          stats.aliasConflict++;
                          graphRemoteLog("character_external_update_alias_conflict", { source: sourceStage || "gengxin", incomingName: incomingName, alias: aliases[ai], matchedCount: arr.length, action: "skip_merge" });
                          matchIndex = -2;
                          break;
                      }
                  }
              }
              if (matchIndex === -2) continue;
              if (matchIndex >= 0) {
                  var oldRec = oldRecords[matchIndex] || {};
                  var merged = graphV908MergeOneExternalRecord(oldRec, inc, matchBy);
                  result.push(merged);
                  usedOld[matchIndex] = true;
                  if (matchBy === "mainName") stats.matchedByMainName++; else stats.matchedByAlias++;
                  if (oldRec.recordId && merged.recordId === oldRec.recordId) stats.preservedRecordIdCount++;
                  if (graphV908ChapterList(oldRec).length && graphV908ChapterList(merged).length) stats.preservedChaptersCount++;
              } else {
                  var fresh = graphV908JsonClone(inc);
                  if (fresh.mainName && !fresh.name) fresh.name = fresh.mainName;
                  if (!fresh.aliases) fresh.aliases = fresh.name || incomingName;
                  if (!fresh.chapters || !Array.isArray(fresh.chapters)) fresh.chapters = [];
                  else fresh.chapters = graphTrimChapterArray(fresh.chapters);
                  result.push(fresh);
                  stats.newCount++;
              }
          }
          for (var oi = 0; oi < oldRecords.length; oi++) {
              if (!usedOld[oi]) {
                  result.push(oldRecords[oi]);
                  stats.preservedOldCount++;
              }
          }
          cm.characterRecords = result;
          graphV908RebuildVoiceUsage(cm);
          stats.emptyChaptersAfter = graphV908CountEmptyChapters(result);
          graphRemoteLog("character_external_update_merged", stats);
          return { ok: true, mode: "external_merge", stats: stats };
      }
      function graphV908BuildGengxinBookSwitchPayload(bookName, fileName, recordsContent) {
          var arr = [];
          try { arr = JSON.parse(String(recordsContent || "[]")); } catch(e) { arr = []; }
          if (!Array.isArray(arr)) arr = [];
          return JSON.stringify({ __ttsRoleGengxinMode: "book_switch_replace", version: 908, bookName: String(bookName || ""), fileName: String(fileName || ""), records: arr });
      }
      function graphV908ParseGengxinPayload(jsonFileContent) {
          var raw = "";
          try {
              raw = String(jsonFileContent || "");
          } catch(e) {
              raw = "";
          }

          if (!raw || raw.trim() === "") {
              return {
                  ok: false,
                  reason: "empty_skipped",
                  silent: true,
                  contentLen: 0
              };
          }

          var payload = null;
          try {
              payload = JSON.parse(raw);
          } catch(e) {
              return {
                  ok: false,
                  reason: "json_parse_failed",
                  silent: false,
                  contentLen: raw.length
              };
          }

          if (Array.isArray(payload)) return { ok: true, mode: "external_merge", records: payload, source: "array_legacy_external_update" };
          if (payload && Array.isArray(payload.records)) {
              var mode = graphSafeString(payload.__ttsRoleGengxinMode || payload.mode || "external_merge", 80);
              if (mode === "book_switch_replace") return { ok: true, mode: "book_switch_replace", records: payload.records, bookName: payload.bookName || "", fileName: payload.fileName || "" };
              return { ok: true, mode: "external_merge", records: payload.records, source: mode };
          }
          return {
              ok: false,
              reason: "payload_not_array_or_records",
              silent: false,
              contentLen: raw.length
          };
      }
      function graphV908ApplyGengxinPayloadToManager(cm, jsonFileContent, sourceStage) {
          if (!cm) return { ok: false, reason: "manager_missing" };

          var raw = "";
          try {
              raw = String(jsonFileContent || "");
          } catch(e) {
              raw = "";
          }

          if (!raw || raw.trim() === "") {
              return {
                  ok: false,
                  reason: "empty_skipped",
                  silent: true,
                  contentLen: 0
              };
          }

          var parsed = graphV908ParseGengxinPayload(raw);
          if (!parsed.ok) {
              if (parsed.silent || parsed.reason === "empty_skipped") {
                  return parsed;
              }
              graphRemoteLog("character_external_update_rejected", {
                  source: sourceStage || "gengxin",
                  reason: parsed.reason || "parse_failed",
                  contentLen: parsed.contentLen !== undefined ? parsed.contentLen : graphSafeString(raw || "", 200000).length
              });
              return parsed;
          }

          graphRemoteLog("character_external_update_detected", { source: sourceStage || "gengxin", mode: parsed.mode, incomingCount: parsed.records.length, currentCount: (cm.characterRecords && cm.characterRecords.length) || 0, currentEmptyChapters: graphV908CountEmptyChapters(cm.characterRecords || []), bookName: parsed.bookName || "", fileName: parsed.fileName || "", chapterIndex: graphCurrentChapterId() });
          if (parsed.mode === "book_switch_replace") {
              cm.characterRecords = parsed.records || [];
              graphV908RebuildVoiceUsage(cm);
              graphRemoteLog("character_book_switch_records_replaced", { source: sourceStage || "gengxin", bookName: parsed.bookName || "", fileName: parsed.fileName || "", recordCount: cm.characterRecords.length, emptyChaptersCount: graphV908CountEmptyChapters(cm.characterRecords), mode: "book_switch_replace", chapterIndex: graphCurrentChapterId() });
              return { ok: true, mode: "book_switch_replace" };
          }
          return graphV908MergeExternalCharacterRecords(cm, parsed.records || [], sourceStage || "gengxin_external_update");
      }

      // -------------------------- 书籍切换与角色备份（直连版：从 data.json 读取） --------------------------
      try {
          if (text.includes("“")) {
              // 直连版：替换原 httpGet getBookshelf 为读取本地 data.json
              var dataJsonContent = "";
              try {
                  dataJsonContent = ttsrv.readTxtFile("data.json").toString();
              } catch (e) {}
              // 仅当 data.json 有效时执行后续
              if (dataJsonContent && dataJsonContent.trim() !== "") {
                  var bookData = JSON.parse(dataJsonContent.toString());
                  var firstBook = {
                      name: String(bookData.bookName || "未知书名").trim(),
                      bookUrl: bookData.bookUrl ? String(bookData.bookUrl) : "",
                      durChapterIndex: (typeof bookData.durChapterIndex !== 'undefined') ? bookData.durChapterIndex : 0
                  };
                  var newBookName = firstBook.name;
                  var oldBookName = "";
                  var cunfangReadSuccess = false;
              
                      // 读取缓存的旧书名，判断是否需要换书
                      try {
                          var rawContent = ttsrv.readTxtFile("cunfang.txt").toString();
                          oldBookName = String(rawContent).trim().toString();
                          cunfangReadSuccess = true;
                      } catch (e) {}
      
                      // ===================== 第一步：先处理换书逻辑（仅当书名不一致时执行） =====================
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
                                      graphRemoteLog("character_book_cache_switch", { source: "save_old_book_character_records", bookName: oldBookName, fileName: oldShumingFileName, recordCount: (function(){ try { return JSON.parse(characterRecordsContent.toString()).length || 0; } catch(e){ return 0; } })(), chapterIndex: graphCurrentChapterId() });
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
                                      var bookSwitchPayload = graphV908BuildGengxinBookSwitchPayload(newBookName, newShumingFileName, newShumingContent.toString());
                                      ttsrv.writeTxtFile("gengxin.json", bookSwitchPayload.toString());
                                      graphRemoteLog("character_book_cache_switch", { source: "load_new_book_to_gengxin_payload", mode: "book_switch_replace", bookName: newBookName, fileName: newShumingFileName, recordCount: (function(){ try { return JSON.parse(newShumingContent.toString()).length || 0; } catch(e){ return 0; } })(), chapterIndex: graphCurrentChapterId() });
                                  } else {
                                      throw new Error("文件空");
                                  }
                              } catch (e) {
                                  var emptyArrayContent = "[]";
                                  var emptyBookSwitchPayload = graphV908BuildGengxinBookSwitchPayload(newBookName, newShumingFileName, emptyArrayContent.toString());
                                  ttsrv.writeTxtFile("gengxin.json", emptyBookSwitchPayload.toString());
                                  graphRemoteLog("character_book_cache_switch", { source: "new_book_empty_gengxin_payload", mode: "book_switch_replace", bookName: newBookName, fileName: newShumingFileName, recordCount: 0, chapterIndex: graphCurrentChapterId() });
                              }
                              // 3. 更新缓存书名（原有逻辑完全保留）
                              try {
                                  ttsrv.writeTxtFile("cunfang.txt", newBookName.toString());
                              } catch (cunfangError) {}
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
                              // 5. 换书重置逻辑（按之前需求保留：重置时间、清空上下文）
                              shijian = new Date(Date.now() - 2 * 60 * 60 * 1000);
                              shijian.setSeconds(0);
                              shijian.setMilliseconds(0);
                              // 换书强制清空旧下文残留，避免异常
                          //    next100Chars = "";
      
                              //console.log("【换书成功】已从「" + oldBookName + "」切换到「" + newBookName + "」，已重置时间和上下文");
                          } catch (fileError) {
                              console.error("【换书逻辑异常】", fileError.message);
                              prevContextChars = "";
                              next100Chars = "";
                          }
                      }
      
                      // ===================== 第二步：无论换不换书，统一执行下文内容获取 =====================
                      try {
                          var rawBookUrlForGraph = firstBook.bookUrl ? firstBook.bookUrl.toString() : "";
                          var bookUrl = encodeURIComponent(rawBookUrlForGraph);
                          var currentChapterIndex = firstBook.durChapterIndex;
                          if (typeof characterManager !== 'undefined' && characterManager && characterManager.setAliasGraphBook) {
                              characterManager.setAliasGraphBook(newBookName, rawBookUrlForGraph);
                          }
                          graphSetCurrentChapterKey(rawBookUrlForGraph, currentChapterIndex);
                          // 直连版：替换原 httpGet getBookContent 多章节循环为直接读取 data.json 的 texts 字段
                          var fullChapterContent = String(bookData.texts || "").toString();
                          var loadedChapters = fullChapterContent ? 1 : 0;
      
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
      
                          // 计算直接上文；下文在硬截取后补齐半截引号并外延2个句号，且不进入下一句双引号对话
                          if (finalIndex !== -1) {
                              var prevHardStart = Math.max(0, finalIndex - 500);
                              var prevStart = extendPrevContentStartPos(fullChapterContent, prevHardStart, finalIndex);
                              prevContextChars = fullChapterContent.substring(prevStart, finalIndex);

                              var startPos = finalIndex + textToSearch.length;
                              var remainingLength = fullChapterContent.length - startPos;
                              var extractLength = Math.min(xiawen, remainingLength);
                              var baseEndPos = startPos + extractLength;
                              var extensionResult = extendNextContentEndPos(fullChapterContent, startPos, baseEndPos);
                              var endPos = extensionResult.endPos;
                              next100Chars = fullChapterContent.substring(startPos, endPos);
                              nameAnalysisContextMeta = {
                                  contextMode: "shared_previous_plus_continuous_batch",
                                  sharedPreviousEndsAtCurrentStart: true,
                                  currentStartPos: finalIndex,
                                  currentEndPos: startPos,
                                  baseLength: extractLength,
                                  finalLength: next100Chars.length,
                                  maxExtraChars: NAME_ANALYZE_NEXT_CONTEXT_EXTRA_MAX,
                                  hardEndPos: extensionResult.hardEndPos,
                                  extendedChars: extensionResult.extendedChars,
                                  baseHadUnclosedQuote: extensionResult.baseHadUnclosedQuote,
                                  quoteClosed: extensionResult.quoteClosed,
                                  foundPeriods: extensionResult.foundPeriods,
                                  stopReason: extensionResult.stopReason,
                                  extensionTail: fullChapterContent.substring(baseEndPos, endPos)
                              };
                            //  //console.log("【上下文获取成功】上文" + prevContextChars.length + "字，下文" + next100Chars.length + "字，当前书籍：" + newBookName);
                          } else {
                              //console.log("【章节匹配失败】上下文置空，当前书籍：" + newBookName);
                              prevContextChars = "";
                              next100Chars = "";
                              nameAnalysisContextMeta = { contextMode: "chapter_match_failed", sharedPreviousEndsAtCurrentStart: false };
                          }
                      } catch (chapterError) {
                          console.error("【上下文获取异常】", chapterError.message);
                          prevContextChars = "";
                          next100Chars = "";
                          nameAnalysisContextMeta = { contextMode: "chapter_context_exception", sharedPreviousEndsAtCurrentStart: false, error: graphSafeString(chapterError && chapterError.message || chapterError, 260) };
                      }
                  }
              }
      } catch (e) {
          console.error("【书籍模块全局异常】", e.message);
          prevContextChars = "";
          next100Chars = "";
          nameAnalysisContextMeta = { contextMode: "book_context_exception", sharedPreviousEndsAtCurrentStart: false, error: graphSafeString(e && e.message || e, 260) };
      }
      




      // -------------------------- 角色记录更新与发音人检测（含100个本地音效） --------------------------
      var graphV908HandleGengxinChecked = false;
      var graphV908HandleGengxinExists = false;
      var graphV908HandleGengxinContent = "";
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
          graphV908HandleGengxinChecked = true;
          graphV908HandleGengxinExists = updateExists;
          graphV908HandleGengxinContent = jsonFileContent || "";
          if (updateExists) {
              if (jsonFileContent.trim() !== "") {
                  try {
                      if (!this.characterManager) this.characterManager = new CharacterManager();
                      var oldContextHistory = this.characterManager.contextHistory ? this.characterManager.contextHistory.toString() : "";
                      var applyResult = graphV908ApplyGengxinPayloadToManager(this.characterManager, jsonFileContent.toString(), "handleText_primary_gengxin_check");
                      if (applyResult && applyResult.ok) {
                          this.characterManager.contextHistory = oldContextHistory;
                          this.characterManager.saveRecords();
                      }
                  } catch (parseError) {
                      graphRemoteLog("character_external_update_error", { source: "handleText_primary_gengxin_check", error: graphSafeString(parseError && parseError.message || parseError, 240) });
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
          
      
      // 二次检查gengxin.json更新（外部数组走安全合并，书籍切换载荷走替换）
      try {
          var jsonFileExists = false;
          jsonFileContent = "";

          if (typeof graphV908HandleGengxinChecked !== "undefined" && graphV908HandleGengxinChecked) {
              jsonFileExists = graphV908HandleGengxinExists === true;
              jsonFileContent = graphV908HandleGengxinContent || "";
          } else {
              try {
                  jsonFileContent = ttsrv.readTxtFile("gengxin.json").toString(); // 兼容：转原始String
                  jsonFileExists = true;
              } catch (e) {
                  jsonFileExists = false;
              }
          }

          if (jsonFileExists && jsonFileContent && jsonFileContent.trim() !== "") {
              if (typeof characterManager === 'undefined') {
                  characterManager = new CharacterManager();
                  characterManager.loadRecords();
              }
              try {
                  var applyResult2 = graphV908ApplyGengxinPayloadToManager(characterManager, jsonFileContent.toString(), "handleText_secondary_gengxin_check");
                  if (applyResult2 && applyResult2.ok) characterManager.saveRecords();
              } catch (parseError) {
                  graphRemoteLog("character_external_update_error", { source: "handleText_secondary_gengxin_check", error: graphSafeString(parseError && parseError.message || parseError, 240) });
              }
          }
      } catch (e) {
      }




// -------------------------- 读取yinxiao.json处理%标记（在线音效/本地音效继续保留） --------------------------
      var yinXiaoList = [];
      var yinXiaoContent = "";
            

      // 第二条文本处理链同样保留短对白/引号内旁白，确保两条链路使用一致的引号来源



      text = text.replace(/“(((锵|咔嚓|哗啦|轰隆|咕噜|滴答|叮咚|咚咚|哐当|噼啪|扑通|吧嗒|吱呀|嘎吱|嗡嗡|喵喵|汪汪|咩咩|哞哞|呱呱|叽喳|啾啾|嘎嘎|嘶嘶|嘟嘟|嘀嘀|砰砰|乓乓|噼里啪啦|稀里哗啦|丁零当啷|叽里咕噜|乒乒乓乓|淅淅沥沥|窸窸窣窣|滴滴答答|叮叮当当|轰轰隆隆|咕咕噜噜|噼噼啪啪|吱吱呀呀|哔哔剥剥|咔咔嚓嚓|扑扑簌簌|踢踢踏踏|咕嘟咕嘟|呼哧呼哧|咯吱咯吱|当啷当啷|哗啦哗啦|沙沙|唰唰|淅沥|咕咚|啪嗒|骨碌碌|轰|咚|唰|砰|铛|咣|咻|嗖|嘭|嚓|咣当|咕嘟|咕隆|哗|唧唧|喳喳|呱嗒|嗒嗒|哒哒|铮铮|铮|嗡|呲|呲啦|咝|咝咝|呜|呜呜|呼呼|飕飕|轰隆隆|咕噜噜|叮铃铃|嘀铃铃|嘀嗒嗒|哐啷|哐啷啷|啪嚓|啪嗒|骨碌|咕噜|咕咕|笃笃|笃|嗒|嘎|嘎嘎|嘎啦|嘎嘣|嘣|嘣嘣|噔|噔噔|噔噔噔|噗|噗噗|噗噜噜|哧|哧溜|哧啦|当|当当|哔|哔哔|哔剥|剥|剥剥|咿呀|咿咿呀呀|吱|吱吱|吱扭|吱嘎|轧轧|轧|轧然|霍霍|霍|霍啦|飕|飕飕|飒飒|飒|萧萧|萧|簌簌|簌|咕|咕咕|咕儿|呱|呱呱|呱唧|唧|唧唧|唧咕|啾|啾啾|啾唧|啁啾|啁|啁啁|嘤|嘤嘤|嗡|嗡嗡|嗡营|营营|铮|铮铮|铮鏦|鏦|鏦然|叮|叮叮|叮当|叮咚|叮铃|铃|铃铃|泠泠|淙淙|潺潺|溅溅|汩汩|咕嘟|咕嘟咕嘟|哗|哗哗|哗啦|哗啦啦|澎|澎湃|澎澎|汹|汹涌|汹汹|轧|轧轧|轧然|吱|吱吱|吱扭|嘎|嘎吱|嘎巴|嘎嘣|嘣|嘣嘣|啪|啪啪|啪嚓|啪嗒|嗒|嗒嗒|哒|哒哒|咚|咚咚|噔|噔噔|噗|噗通|噗嗤|嗤|嗤嗤|嗤啦|咝|咝咝|咻|咻咻|嗖|嗖嗖|飕|飕飕|呜|呜呜|呼|呼呼|呼啦|呼啦啦|哗|哗啦|哗啦啦|咕|咕噜|咕咚|咕嘟|嘟|嘟嘟|嘟噜|噜|噜噜|哞|哞哞|咩|咩咩|喵|喵喵|汪|汪汪|嗷|嗷嗷|咯|咯咯|咯吱|吱|吱吱|呱|呱呱|叽|叽叽|喳|喳喳|啾|啾啾|嘶|嘶嘶|吼|吼吼|唳|唳唳|吠|汪汪|嗡|嗡嗡|营|营营|铮|铮铮|叮|叮当|叮咚|当|当当|哐|哐当|砰|砰砰|乓|乓乓|咣|咣当|嚓|咔嚓|啪|啪嗒|嗒|嗒嗒|嘀|嘀嗒|嗒|嗒嗒|哒|哒哒|嘟|嘟嘟|哔|哔哔|噗|噗噗|哧|哧哧|咝|咝咝|唰|唰唰|淅沥|沥沥|沥|沙|沙沙|飒|飒飒|萧|萧萧|簌|簌簌|哗|哗哗|轰|轰轰|咕|咕咕|咚|咚咚|吱|吱吱|嘎|嘎嘎|当|当当|乓|乓乓|砰|砰砰|啪|啪啪|哐|哐哐|咣|咣咣|叮|叮叮|铮|铮铮|嗡|嗡嗡|嘟|嘟嘟|哔|哔哔|噗|噗噗|哧|哧哧|咻|咻咻|嗖|嗖嗖|飕|飕飕|呜|呜呜|呼|呼呼|哗|哗哗|轰|轰轰|咕|咕咕|咚|咚咚|吱|吱吱|嘎|嘎嘎|咯噔|咕叽|咕叽咕叽|咕噜咕噜|哗啦啦|噼啪|噼噼啪啪|咚咚咚|哐哐|咣咣|叮叮当|叮叮咚咚|吱嘎吱嘎|吱呀呀|轰隆轰隆|咕咚咕咚|吧嗒吧嗒|嘀嗒嘀嗒|沙沙沙|飒飒飒|嗡嗡嗡|喵呜|汪汪汪|咩咩咩|哞哞哞|呱呱呱|叽叽叽|喳喳喳|啾啾啾|嘶嘶嘶|呼呼呼|呜呜呜|哒哒哒|嗒嗒嗒|砰砰砰|乓乓乓|嚓嚓嚓|唰唰唰|淅沥沥|哗哗哗|咕咕咕|咚咚咚|吱吱吱|嘎嘎嘎|当当当|铮铮铮|噗噗噗|哧哧哧|咻咻咻|嗖嗖嗖|飕飕飕|哐当哐当|咕噜咕噜|噼里啪啦轰隆隆|稀里哗啦丁零当啷|叽里咕噜乒乒乓乓|窸窸窣窣滴滴答答|叮叮当当轰轰隆隆|噼噼啪啪吱吱呀呀|哔哔剥剥咔咔嚓嚓|扑扑簌簌踢踢踏踏|咕嘟咕嘟呼哧呼哧|咯吱咯吱当啷当啷|哗啦哗啦唧唧喳喳|呱嗒呱嗒铮铮作响|咣当咣当扑通扑通|吧唧吧唧咕叽咕叽|沙啦沙啦飒啦飒啦|簌啦簌啦霍啦霍啦|咝啦咝啦哧溜哧溜|嘟噜嘟噜哔剥哔剥|噼啪噼啪咔嚓咔嚓|轰隆轰隆咕咚咕咚|叮咚叮咚嘀嗒嘀嗒|哗啦哗啦呼啦呼啦|吧嗒吧嗒啪嗒啪嗒|吱呀吱呀嘎吱嘎吱|嗡嗡嗡嗡喵喵喵喵|汪汪汪汪咩咩咩咩|哞哞哞哞呱呱呱呱|叽叽叽叽喳喳喳喳|啾啾啾啾嘶嘶嘶嘶|呼呼呼呼呜呜呜呜|咚咚咚咚吱吱吱吱|嘎嘎嘎嘎当当当当|铮铮铮铮噗噗噗噗|哧哧哧哧咻咻咻咻|嗖嗖嗖嗖飕飕飕飕|哐哐哐哐咣咣咣咣|嚓嚓嚓嚓唰唰唰唰|淅沥淅沥哗哗哗哗|咕咕咕咕咚咚咚咚|噼里啪啦稀里哗啦|丁零当啷叽里咕噜|乒乒乓乓淅淅沥沥|窸窸窣窣滴滴答答|叮叮当当轰轰隆隆|噼噼啪啪吱吱呀呀|哔哔剥剥咔咔嚓嚓|扑扑簌簌踢踢踏踏|咕嘟咕嘟呼哧呼哧|咯吱咯吱当啷当啷)([！？。，；：、]*)){1,3})”/g, '$1');



     








      // 确保CharacterManager已初始化
      if (typeof characterManager === 'undefined') {
          characterManager = new CharacterManager();
          characterManager.loadRecords();
      }

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
          var tagsArr = [];
          for (var num = 1; num <= 100; num++) {
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



// ===================== 绑定式备份 + 槽位分流 + 图谱审计建议补丁 =====================
function v87BookKeyOfManager(mgr) {
  var bookKey = "default";
  try {
    if (mgr && mgr.aliasGraphBookKey) bookKey = mgr.aliasGraphBookKey;
    else if (typeof graphCurrentBookUrl !== "undefined" && graphCurrentBookUrl) bookKey = graphBookCacheSafeKey("", graphCurrentBookUrl);
  } catch(e) {}
  bookKey = graphBookCacheSafeKey(bookKey || "default", "");
  return bookKey || "default";
}

function v87EnsureMergedRecordsBook(mgr) {
  if (!mgr) return graphBookCacheFile("mergedRecords", "default");
  var bookKey = v87BookKeyOfManager(mgr);
  var file = graphBookCacheFile("mergedRecords", bookKey);
  if (mgr.mergedRecordsFile !== file) {
    mgr.mergedRecordsFile = file;
    mgr.aliasGraphBookKey = bookKey;
    mgr.mergedRecords = graphReadJsonSafe(file, {});
    if (!mgr.mergedRecords || typeof mgr.mergedRecords !== "object") mgr.mergedRecords = {};
    mgr._mergedRecordsLoadedFile = file;
  }
  if (!mgr.mergedRecords || typeof mgr.mergedRecords !== "object") mgr.mergedRecords = {};
  return file;
}

function v87EnsureRecordId(mgr, rec) {
  if (!rec) return "";
  if (!rec.recordId) {
    var name = graphNormalizeName(rec.name || "unknown");
    var seed = v87BookKeyOfManager(mgr) + "|" + name + "|" + graphSafeString(rec.voice || "", 80) + "|" + graphSafeString((rec.chapters || []).join("|"), 200) + "|" + graphNowIso() + "|" + Math.random();
    rec.recordId = "char_" + graphHash(seed);
  }
  return rec.recordId;
}

function v87SplitAliasesString(s) {
  var out = [];
  var map = {};
  var arr = String(s || "").split("|");
  for (var i = 0; i < arr.length; i++) {
    var n = graphNormalizeName(arr[i]);
    if (n && !map[n]) { map[n] = true; out.push(n); }
  }
  return out;
}

// mergedRecords 是 merge rollback history，不再是按name复用表。
CharacterManager.prototype.loadMergedRecords = function() {
  var file = v87EnsureMergedRecordsBook(this);
  this.mergedRecords = graphReadJsonSafe(file, {});
  if (!this.mergedRecords || typeof this.mergedRecords !== "object") this.mergedRecords = {};
  this._mergedRecordsLoadedFile = file;
  graphRemoteLog("merged_records_backup_loaded", { file: file, count: Object.keys(this.mergedRecords || {}).length, bookKey: this.aliasGraphBookKey || "default", schema: "v87_merge_history" });
};

CharacterManager.prototype.saveMergedRecords = function() {
  var file = v87EnsureMergedRecordsBook(this);
  graphWriteJsonSafe(file, this.mergedRecords || {});
};

CharacterManager.prototype.storeMergedRecordBackup = function(target, source, reason) {
  if (!target || !source || !source.name) return;
  var file = v87EnsureMergedRecordsBook(this);
  var bookKey = v87BookKeyOfManager(this);
  var sourceId = v87EnsureRecordId(this, source);
  var targetId = v87EnsureRecordId(this, target);
  var sourceName = graphNormalizeName(source.name);
  var targetName = graphNormalizeName(target.name);
  if (!sourceName || !targetName) return;
  var backupRecord = {};
  try { backupRecord = JSON.parse(JSON.stringify(source)); } catch(e) { backupRecord = { name: source.name, aliases: source.aliases || source.name, gender: source.gender || "", age: source.age || "", voice: source.voice || "", usageCount: source.usageCount || CONFIG.resetUsageCount, chapters: source.chapters || [], genderAgeHistory: source.genderAgeHistory || [] }; }
  backupRecord.recordId = sourceId;
  var mergeId = "merge_" + graphHash(bookKey + "|" + targetId + "|" + sourceId + "|" + graphCurrentChapterId() + "|" + graphNowIso());
  var sourceAliases = v87SplitAliasesString(source.aliases || source.name);
  if (sourceAliases.indexOf(sourceName) === -1) sourceAliases.unshift(sourceName);
  this.mergedRecords[mergeId] = {
    schema: "v87_bound_merge_backup",
    mergeId: mergeId,
    status: "merged",
    bookKey: bookKey,
    sourceRecordId: sourceId,
    sourceName: sourceName,
    sourceAliases: sourceAliases,
    targetRecordId: targetId,
    targetMainName: targetName,
    mergedAtChapter: graphCurrentChapterId(),
    mergeReason: graphSafeString(reason || "", 260),
    mergeTime: graphNowIso(),
    backupRecord: backupRecord,
    restorePolicy: "only_when_splitting_this_target_alias_after_conflict_different"
  };
  try {
    var keys = Object.keys(this.mergedRecords || {});
    if (keys.length > 240) {
      var self = this;
      keys.sort(function(a, b) { return String((self.mergedRecords[a] || {}).mergeTime || "").localeCompare(String((self.mergedRecords[b] || {}).mergeTime || "")); });
      while (keys.length > 240) delete this.mergedRecords[keys.shift()];
    }
  } catch(pruneErr) {}
  this.saveMergedRecords();
  graphRemoteLog("merged_records_backup_saved", { file: file, mergeId: mergeId, sourceName: sourceName, sourceRecordId: sourceId, target: targetName, targetRecordId: targetId, bookKey: bookKey, reason: graphSafeString(reason || "", 180), schema: "v87_bound_merge_backup" });
};

function v87FindBoundMergeBackup(mgr, targetRecord, splitName) {
  if (!mgr || !targetRecord || !splitName) return null;
  v87EnsureMergedRecordsBook(mgr);
  var bookKey = v87BookKeyOfManager(mgr);
  var targetName = graphNormalizeName(targetRecord.name);
  var targetId = targetRecord.recordId || "";
  splitName = graphNormalizeName(splitName);
  var best = null;
  var bestKey = "";
  var records = mgr.mergedRecords || {};
  for (var k in records) {
    if (!records.hasOwnProperty(k)) continue;
    var m = records[k] || {};
    if (m.status && m.status !== "merged") continue;
    if (m.bookKey && m.bookKey !== bookKey) continue;
    var targetOk = false;
    if (targetId && m.targetRecordId && m.targetRecordId === targetId) targetOk = true;
    if (!targetOk && graphNormalizeName(m.targetMainName || "") === targetName) targetOk = true;
    if (!targetOk) continue;
    var aliasOk = graphNormalizeName(m.sourceName || "") === splitName;
    var aliases = Array.isArray(m.sourceAliases) ? m.sourceAliases : v87SplitAliasesString((m.backupRecord && m.backupRecord.aliases) || "");
    for (var i = 0; !aliasOk && i < aliases.length; i++) if (graphNormalizeName(aliases[i]) === splitName) aliasOk = true;
    if (!aliasOk) continue;
    best = m; bestKey = k;
  }
  if (best) return { key: bestKey, item: best };
  return null;
}

var v87OldMergeCharacterRecords = CharacterManager.prototype.mergeCharacterRecords;
CharacterManager.prototype.mergeCharacterRecords = function(target, source, reason) {
  try { v87EnsureRecordId(this, target); v87EnsureRecordId(this, source); } catch(e0) {}
  var sourceStageText = graphSafeString(reason || "", 260);
  var sourceStage = /compound|复合/i.test(sourceStageText) ? "compound" : (/conflict|冲突|graph/i.test(sourceStageText) ? "graph_conflict" : (/alias|别名/i.test(sourceStageText) ? "alias" : "unknown"));
  var targetId = target && target.recordId || "";
  var sourceId = source && source.recordId || "";
  var targetObserveName = graphNormalizeName(target && target.name || "");
  var sourceObserveName = graphNormalizeName(source && source.name || "");
  // 观察键按两张卡的稳定ID无方向排序；只增强重复调用报警，不阻断或改变原有合卡/拆卡行为。
  var pairIds = [targetId || ("name:" + targetObserveName), sourceId || ("name:" + sourceObserveName)].sort();
  var observeKey = [graphV908CurrentBookKey(this), pairIds[0], pairIds[1]].join("|");
  var now = Date.now();
  if (!this._v908RecentMergeObserve) this._v908RecentMergeObserve = {};
  var previousObserve = this._v908RecentMergeObserve[observeKey] || null;
  graphRemoteLog("role_record_merge_call_observe", { sourceStage: sourceStage, reason: sourceStageText, targetRecordId: targetId, sourceRecordId: sourceId, normalizedPairIds: pairIds, targetName: targetObserveName, sourceName: sourceObserveName, targetPresentBefore: !!(this.characterRecords && this.characterRecords.indexOf(target) !== -1), sourcePresentBefore: !!(this.characterRecords && this.characterRecords.indexOf(source) !== -1) });
  if (previousObserve && now - Number(previousObserve.at || 0) < 8000) graphRemoteLog("role_record_merge_duplicate_suspected", { targetRecordId: targetId, sourceRecordId: sourceId, firstSourceStage: previousObserve.sourceStage || "", secondSourceStage: sourceStage, intervalMs: now - Number(previousObserve.at || 0), behaviorChanged: false });
  this._v908RecentMergeObserve[observeKey] = { at: now, sourceStage: sourceStage };
  var merged = v87OldMergeCharacterRecords ? v87OldMergeCharacterRecords.apply(this, arguments) : false;
  graphRemoteLog("role_record_merge_call_observe", { sourceStage: sourceStage, reason: sourceStageText, targetRecordId: targetId, sourceRecordId: sourceId, targetPresentAfter: !!(this.characterRecords && this.characterRecords.indexOf(target) !== -1), sourcePresentAfter: !!(this.characterRecords && this.characterRecords.indexOf(source) !== -1), mergedResult: !!merged, phase: "after_call" });
  return merged;
};

CharacterManager.prototype.splitAliasByConflict = function(a, b, reason) {
  a = graphNormalizeName(a); b = graphNormalizeName(b);
  if (!a || !b || a === b || !this.characterRecords) return false;
  var recA = this.findCharacterRecord ? this.findCharacterRecord(a) : null;
  var recB = this.findCharacterRecord ? this.findCharacterRecord(b) : null;
  if (!recA || !recB || recA !== recB) return false;
  var rec = recA;
  v87EnsureRecordId(this, rec);
  var splitName = (graphNormalizeName(rec.name) === a) ? b : a;
  if (graphNormalizeName(rec.name) !== a && graphNormalizeName(rec.name) !== b) splitName = b;
  if (!splitName || splitName === graphNormalizeName(rec.name)) return false;
  var aliasListBefore = v87SplitAliasesString(rec.aliases || rec.name);
  var wasAliasOnTarget = aliasListBefore.indexOf(splitName) !== -1;
  var removed = this.removeAliasFromRecord ? this.removeAliasFromRecord(rec, splitName) : false;
  var exact = this.findMainCharacterRecordByExactName ? this.findMainCharacterRecordByExactName(splitName) : null;
  var restoredFromBackup = false;
  var backupNotReusedReason = "";
  var createdNewRecord = false;
  var voiceInfo = { voiceRestored: false, voiceFallback: false, oldVoice: "", newVoice: "", fallbackReason: "" };

  if (!exact) {
    var bound = wasAliasOnTarget ? v87FindBoundMergeBackup(this, rec, splitName) : null;
    if (bound && bound.item && bound.item.backupRecord) {
      try {
        exact = JSON.parse(JSON.stringify(bound.item.backupRecord));
        restoredFromBackup = true;
        this.mergedRecords[bound.key].status = "restored";
        this.mergedRecords[bound.key].restoredAt = graphNowIso();
        this.mergedRecords[bound.key].restoredChapter = graphCurrentChapterId();
        if (this.saveMergedRecords) this.saveMergedRecords();
      } catch(bookBackupErr) { exact = null; restoredFromBackup = false; }
    } else {
      backupNotReusedReason = wasAliasOnTarget ? "no_bound_merge_backup" : "split_name_not_target_alias";
      if (this.mergedRecords && this.mergedRecords[splitName]) backupNotReusedReason = "name_match_without_merge_binding";
      graphRemoteLog("merged_record_backup_not_reused", { splitName: splitName, target: graphNormalizeName(rec.name), reason: backupNotReusedReason, rule: "backup_requires_merge_binding" });
    }
    if (!exact && rec.mergedRecords && Array.isArray(rec.mergedRecords)) {
      for (var bi = rec.mergedRecords.length - 1; bi >= 0; bi--) {
        var backup = rec.mergedRecords[bi];
        if (backup && graphNormalizeName(backup.name) === splitName && graphNormalizeName(backup.mergedInto || rec.name) === graphNormalizeName(rec.name)) {
          exact = JSON.parse(JSON.stringify(backup));
          delete exact.mergedInto;
          delete exact.mergedAt;
          rec.mergedRecords.splice(bi, 1);
          restoredFromBackup = true;
          break;
        }
      }
    }
    if (!exact) {
      var voice = this.assignVoice ? this.assignVoice(rec.gender || "男", rec.age || "男青年", { targetName: splitName, assignType: "角色拆分新建发音人", sourceStage: "role_record_split", afterAliasCheck: false, isSpecialSpeaker: false, fromSplit: true }) : "";
      exact = { name: splitName, aliases: splitName, gender: rec.gender || "男", age: rec.age || "男青年", voice: voice || "default", usageCount: CONFIG.resetUsageCount, chapters: graphCurrentChapterId() === "unknown" ? [] : [graphCurrentChapterId()], genderAgeHistory: rec.genderAgeHistory ? rec.genderAgeHistory.slice(-3) : [] };
      createdNewRecord = true;
    }
    exact.name = graphNormalizeName(exact.name || splitName) || splitName;
    if (!exact.aliases) exact.aliases = exact.name;
    if (!Array.isArray(exact.chapters)) exact.chapters = [];
    var curSplitChapter = graphCurrentChapterId();
    if (curSplitChapter && curSplitChapter !== "unknown" && exact.chapters.indexOf(curSplitChapter) === -1) exact.chapters.push(curSplitChapter);
    v87EnsureRecordId(this, exact);
    voiceInfo = this.restoreVoiceWithFallback ? this.restoreVoiceWithFallback(exact, rec.gender || "男", rec.age || "男青年") : voiceInfo;
    this.characterRecords.push(exact);
  }
  if (removed || exact) {
    if (this.rebuildNameToMainNameMap) this.rebuildNameToMainNameMap();
    this.saveRecords();
    graphRemoteLog("role_record_split", {
      mainName: graphNormalizeName(rec.name),
      splitName: splitName,
      reason: graphSafeString(reason || "", 260),
      removedAlias: !!removed,
      backupBindingRequired: true,
      restoredFromBackup: !!restoredFromBackup,
      backupNotReusedReason: backupNotReusedReason,
      createdNewRecord: !!createdNewRecord,
      recordId: exact && exact.recordId || "",
      voice: exact && exact.voice || "",
      voiceRestored: !!voiceInfo.voiceRestored,
      voiceFallback: !!voiceInfo.voiceFallback,
      oldVoice: voiceInfo.oldVoice || "",
      newVoice: voiceInfo.newVoice || (exact && exact.voice) || "",
      fallbackReason: voiceInfo.fallbackReason || "",
      usageCount: exact && exact.usageCount || 0,
      aliases: exact && exact.aliases || "",
      chapters: exact && exact.chapters || [],
      genderAgeHistory: exact && exact.genderAgeHistory || []
    });
    return true;
  }
  return false;
};

// 快照组包时从真实持久角色表重新读取，避免上传空records。
graphBuildCharacterRecordsSnapshot = function(chapterIndex) {
  try {
    var mgr = (typeof characterManager !== "undefined") ? characterManager : null;
    var records = [];
    try {
      var fileText = ttsrv.readTxtFile("characterRecords.json");
      if (fileText && String(fileText).trim()) records = JSON.parse(String(fileText));
    } catch(readErr) { records = []; }
    if ((!records || !Array.isArray(records) || records.length === 0) && mgr && mgr.characterRecords && Array.isArray(mgr.characterRecords)) records = mgr.characterRecords;
    if (!records || !Array.isArray(records)) records = [];
    var bookKey = mgr ? v87BookKeyOfManager(mgr) : "default";
    var out = [];
    for (var i = 0; i < records.length; i++) {
      var r = records[i];
      if (!r || !r.name) continue;
      var chapters = Array.isArray(r.chapters) ? r.chapters.slice(0) : [];
      var mainName = graphNormalizeName(r.name || "");
      var chapterFallback = graphSafeString(r.lastSeenChapter || r.lastSeen || r.chapterIndex || "", 40);
      if (!chapters.length && chapterFallback) chapters = [chapterFallback];
      var chaptersEmpty = chapters.length === 0;
      var lastSeen = chapters.length ? chapters[chapters.length - 1] : "";
      var aliasesArr = v87SplitAliasesString(r.aliases || r.name);
      var backupAvailable = false;
      try { backupAvailable = !!v87FindBoundMergeBackup(mgr, r, mainName); } catch(e1) {}
      out.push({ recordId: graphSafeString(r.recordId || "", 80), mainName: mainName, aliases: aliasesArr, aliasesText: graphSafeString(r.aliases || "", 500), gender: graphSafeString(r.gender || "", 20), age: graphSafeString(r.age || "", 30), voice: graphSafeString(r.voice || "", 80), voiceId: graphSafeString(r.voiceId || r.voiceKey || "", 120), chapters: chapters, usageCount: Number(r.usageCount || 0), lastSeenChapter: lastSeen, merged: !!r.mergedInto, mergedInto: graphNormalizeName(r.mergedInto || ""), backupAvailable: backupAvailable, chaptersEmptyWarning: chaptersEmpty });
    }
    var sourceStore = records === (mgr && mgr.characterRecords) ? "memory_characterRecords" : "characterRecords.json";
    var emptyChaptersCount = 0;
    for (var si = 0; si < out.length; si++) if (out[si] && out[si].chaptersEmptyWarning) emptyChaptersCount++;
    graphRemoteLog("character_snapshot_source", { source: sourceStore, recordCount: out.length, emptyChaptersCount: emptyChaptersCount, bookName: bookKey, chapterIndex: graphSafeString(chapterIndex || graphCurrentChapterIndex || "", 40), snapshotStage: "before_remote_upload_after_character_update" });
    return { source: GRAPH_RULE_SOURCE, eventType: "character_records_snapshot", cnEvent: graphCnEventName("character_records_snapshot"), chapterIndex: graphSafeString(chapterIndex || graphCurrentChapterIndex || "", 40), bookKey: bookKey, recordCount: out.length, records: out, sourceStore: sourceStore, snapshotStage: "before_remote_upload_after_character_update", time: graphNowIso() };
  } catch(e) { return null; }
};

// 身份替代日志只用于真正身份替代/魂魄/伪装类；普通别名走 alias_evidence_observed。
graphIdentitySubstitutionType = function(text) {
  text = graphSafeString(text || "", 1200);
  if (!text) return "";
  if (/(残魂|分魂|主魂|魂魄|元神|神魂|鬼脸|借魂|藏魂|炼魂|夺魂)/.test(text)) return "soul_fragment_or_remnant";
  if (/(假冒|假扮|冒充|顶替|冒名顶替|取代|代替|乔装|改扮|伪装|易容|以[^。！？\n]{0,8}身份|用了[^。！？\n]{0,8}身份|盗用[^。！？\n]{0,8}身份)/.test(text)) return "impersonation_or_replacement";
  if (/(附身|附体|借体|寄身|夺舍|借[^。！？\n]{0,8}身体)/.test(text)) return "possession_or_body_borrowing";
  if (/(操控|控制|驱使|远程操控|傀儡|炼成傀儡|炼制成傀儡|制成傀儡)/.test(text)) return "control_or_puppet";
  if (/(化身|分身)/.test(text) && /(本体|真身|身份|幻化|化作|化为)/.test(text)) return "avatar_form";
  return "";
};

function v87EvidenceLooksModelSummaryOnly(text) {
  text = graphSafeString(text || "", 1000);
  if (!text) return false;
  var hasAnchor = /(自称|称为|称作|被称为|被称作|叫做|叫作|名为|名叫|本名|真名|原名|又称|号称|人称|介绍为|引见为|即是|即为|正是|便是|乃是|\(|（)/.test(text);
  var summary = /(上下文明确|可知|证明|显然|无歧义|身份一致|可以判断|可判断|可推断|逻辑一致|表明二者?是同一人|说明二者?是同一人)/.test(text);
  return summary && !hasAnchor;
}

CharacterManager.prototype.directPairEvidenceGate = function(a, b, reason, contextText, stage) {
  a = graphNormalizeName(a); b = graphNormalizeName(b);
  reason = graphSafeString(reason || "", 1400);
  contextText = graphSafeString(contextText || "", 6000);
  stage = graphSafeString(stage || "direct_pair_gate", 80);
  if (!a || !b || a === b) return { allow: true, tier: "A", reason: "same_or_empty" };
  var directInContext = graphHasDirectPairEvidenceText(a, b, contextText);
  var directInReason = graphHasDirectPairEvidenceText(a, b, reason) && !v87EvidenceLooksModelSummaryOnly(reason);
  var reasonSummaryOnly = v87EvidenceLooksModelSummaryOnly(reason);
  var bridgeRisk = graphReasonHasBridgeRisk(a, b, reason);
  var strictExisting = graphPairHasStrictPositive(this, a, b);
  var variantPair = graphIsWhitelistedNameVariant(a, b);
  var idType = graphIdentitySubstitutionType((contextText || "") + "\n" + (reason || ""));
  if (variantPair) {
    graphRemoteLog("direct_pair_gate_pass", { a: a, b: b, stage: stage, tier: "A", reason: "whitelisted_name_variant" });
    graphRemoteLog("alias_evidence_observed", { a: a, b: b, stage: stage, evidenceType: "name_variant", action: "may_enter_direct_pair_gate" });
    return { allow: true, tier: "A", reason: "whitelisted_name_variant", directInContext: directInContext, directInReason: directInReason, bridgeRisk: bridgeRisk };
  }
  if (directInContext || directInReason) {
    graphRemoteLog("direct_pair_gate_pass", { a: a, b: b, stage: stage, tier: "A", reason: directInContext ? "direct_pair_context_anchor" : "direct_pair_reason_anchor", directInContext: directInContext, directInReason: directInReason, reasonSummaryOnly: reasonSummaryOnly, bridgeRisk: bridgeRisk });
    graphRemoteLog("alias_evidence_observed", { a: a, b: b, stage: stage, evidenceType: idType ? "identity_substitution_anchor" : "direct_pair_anchor", triggerText: graphSafeString(reason || contextText, 220), action: "may_enter_direct_pair_gate" });
    return { allow: true, tier: "A", reason: directInContext ? "direct_pair_context_anchor" : "direct_pair_reason_anchor", directInContext: directInContext, directInReason: directInReason, bridgeRisk: bridgeRisk };
  }
  if (strictExisting) {
    graphRemoteLog("direct_pair_gate_pass", { a: a, b: b, stage: stage, tier: "A", reason: "strict_existing_positive" });
    return { allow: true, tier: "A", reason: "strict_existing_positive", directInContext: directInContext, directInReason: directInReason, bridgeRisk: bridgeRisk };
  }
  if (idType && (contextText.indexOf(a) !== -1 || reason.indexOf(a) !== -1) && (contextText.indexOf(b) !== -1 || reason.indexOf(b) !== -1)) {
    graphRemoteLog("identity_substitution_evidence", { a: a, b: b, identityType: idType, type: idType, stage: stage, triggerText: graphSafeString(reason || contextText, 220), action: "evidence_only_to_verify_not_direct_edge" });
    return { allow: false, tier: "B", needVerify: true, reason: "identity_substitution_needs_verify", directInContext: directInContext, directInReason: directInReason, bridgeRisk: bridgeRisk };
  }
  if (bridgeRisk) return { allow: false, tier: "C", needVerify: false, reason: "bridge_inference_without_direct_pair_anchor", directInContext: directInContext, directInReason: directInReason, bridgeRisk: bridgeRisk };
  if (reasonSummaryOnly) return { allow: false, tier: "B", needVerify: true, reason: "model_summary_without_text_anchor", directInContext: directInContext, directInReason: directInReason, bridgeRisk: bridgeRisk };
  return { allow: false, tier: "B", needVerify: true, reason: "no_direct_pair_anchor_to_verify", directInContext: directInContext, directInReason: directInReason, bridgeRisk: bridgeRisk };
};

// 本地 speaker/action 槽位语义判定代码已删除；相关证据由批量姓名分析 __relations 返回。

function v87NormalizeAuditSuggestions(list) {
  if (!list || !Array.isArray(list)) return [];
  var out = [];
  for (var i = 0; i < list.length && out.length < 2; i++) {
    var s = list[i] || {};
    var a = graphNormalizeName(s.a || s.nameA || s.left || s.alias || s.mainName || "");
    var b = graphNormalizeName(s.b || s.nameB || s.right || s.mainName || s.alias || "");
    if (!a || !b || a === b) continue;
    out.push({ kind: graphSafeString(s.kind || s.type || "graph_audit", 80), a: a, b: b, currentGraphReason: graphSafeString(s.currentGraphReason || s.reason || "", 120), conflictEvidenceText: graphSafeString(s.conflictEvidenceText || s.evidenceText || s.evidence || "", 260), suggestedRelation: graphSafeString(s.suggestedRelation || s.relation || "", 40), confidence: Number(s.confidence || s.score || 0), needsVerify: s.needsVerify !== false });
  }
  return out;
}
CharacterManager.prototype.handleGraphAuditSuggestions = function(list, stage, contextText) {
  var arr = v87NormalizeAuditSuggestions(list);
  for (var i = 0; i < arr.length; i++) {
    var s = arr[i];
    graphRemoteLog("graph_audit_suggestion", { stage: stage || "", kind: s.kind, a: s.a, b: s.b, suggestedRelation: s.suggestedRelation, confidence: s.confidence, evidence: s.conflictEvidenceText, currentGraphReason: s.currentGraphReason, needsVerify: s.needsVerify });
    if (s.needsVerify && this.verifyGraphConflictAndFix && s.confidence >= 80) {
      var kind = s.suggestedRelation === "same_person" ? "positive" : (s.suggestedRelation === "different_person" ? "negative" : "positive");
      this.verifyGraphConflictAndFix(kind, s.a, s.b, 3.5, "graph_audit_suggestion", s.conflictEvidenceText || s.currentGraphReason || "", stage || "graph_audit_suggestion", { defaultAllow: false, forceVerify: true, contextText: contextText || this.contextHistory2 || "" });
    }
  }
};
var v87OldCheckAliasByApi = CharacterManager.prototype.checkAliasByApi;
CharacterManager.prototype.checkAliasByApi = function(newName, chapterFullContent, newCharacterGender, currentDialogueText) {
  var res = v87OldCheckAliasByApi ? v87OldCheckAliasByApi.apply(this, arguments) : null;
  try { if (res && res.graphAuditSuggestions) this.handleGraphAuditSuggestions(res.graphAuditSuggestions, "alias_check", String(chapterFullContent || "") + "\n" + String(currentDialogueText || "")); } catch(e) {}
  return res;
};
var v87OldRefineAliasGroupByApi = CharacterManager.prototype.refineAliasGroupByApi;
CharacterManager.prototype.refineAliasGroupByApi = function(mainRecord, newName, chapterFullContent, currentDialogueText) {
  var res = v87OldRefineAliasGroupByApi ? v87OldRefineAliasGroupByApi.apply(this, arguments) : null;
  try { if (res && res.graphAuditSuggestions) this.handleGraphAuditSuggestions(res.graphAuditSuggestions, "alias_refine", String(chapterFullContent || "") + "\n" + String(currentDialogueText || "")); } catch(e) {}
  return res;
};

// 保存/读取角色表前后补日志，并在保存前补recordId，给绑定式备份和快照提供稳定键。
var v87OldSaveRecords = CharacterManager.prototype.saveRecords;
CharacterManager.prototype.saveRecords = function() {
  try {
    if (this.characterRecords && Array.isArray(this.characterRecords)) {
      for (var i = 0; i < this.characterRecords.length; i++) v87EnsureRecordId(this, this.characterRecords[i]);
    }
  } catch(e) {}
  var count = this.characterRecords && Array.isArray(this.characterRecords) ? this.characterRecords.length : 0;
  var emptyChapters = 0;
  try {
    for (var j = 0; j < count; j++) {
      var r = this.characterRecords[j] || {};
      if (!r.chapters || !Array.isArray(r.chapters) || r.chapters.length === 0) emptyChapters++;
    }
  } catch(e1) {}
  var ret = v87OldSaveRecords ? v87OldSaveRecords.apply(this, arguments) : ttsrv.writeTxtFile("characterRecords.json", JSON.stringify(this.characterRecords || []));
  graphRemoteLog("character_cache_save", { source: "memory_to_characterRecords.json", recordCount: count, emptyChaptersCount: emptyChapters, bookName: (typeof bookName !== "undefined" ? String(bookName || "") : ""), chapterIndex: graphCurrentChapterId() });
  return ret;
};

var v87OldLoadRecords = CharacterManager.prototype.loadRecords;
CharacterManager.prototype.loadRecords = function() {
  var ret = v87OldLoadRecords ? v87OldLoadRecords.apply(this, arguments) : undefined;
  var count = this.characterRecords && Array.isArray(this.characterRecords) ? this.characterRecords.length : 0;
  var emptyChapters = 0;
  try {
    for (var i = 0; i < count; i++) {
      var r = this.characterRecords[i] || {};
      if (!r.chapters || !Array.isArray(r.chapters) || r.chapters.length === 0) emptyChapters++;
    }
  } catch(e) {}
  graphRemoteLog("character_cache_load", { source: "characterRecords.json", recordCount: count, emptyChaptersCount: emptyChapters, bookName: (typeof bookName !== "undefined" ? String(bookName || "") : ""), chapterIndex: graphCurrentChapterId() });
  return ret;
};




// ===================== 通用现有角色卡协调 + 复合证据认证（无角色专修）=====================
function graphV87EvidenceLooksLikeModelSummary(text) {
  text = graphSafeString(text || "", 500);
  if (!text) return false;
  var summary = /(上下文明确|可知|可以看出|证明|显然|无歧义|应为同一人|同一人物|判断为同一人|模型判定|身份一致|背景显示)/.test(text);
  var anchor = /(自称|自我介绍|在下|本人|我叫|我名|名叫|名为|名唤|本名|真名|原名|又称|又名|号称|人称|被称为|被称作|称为|称作|介绍为|引见为|正是|便是|乃是|就是|即是|即为|\(|（)/.test(text);
  return summary && !anchor;
}

graphIdentitySubstitutionType = function(text) {
  text = graphSafeString(text || "", 1000);
  if (!text) return "";
  if (/(残魂|分魂|主魂|魂魄|元神|鬼脸|夺舍|附身|附体|借体|寄身)/.test(text)) return /(夺舍|附身|附体|借体|寄身)/.test(text) ? "possession_or_body_borrowing" : "remnant_or_soul_fragment";
  if (/(假冒|假扮|冒充|顶替|冒名顶替|乔装|改扮|伪装|扮作|化名|假身份|伪身份)/.test(text)) return "impersonation_or_disguise";
  if (/(操控|控制|驱使|远程操控|傀儡|炼成傀儡|炼制成傀儡|制成傀儡)/.test(text)) return "control_or_puppet";
  return "";
};

var graphV87OldHasDirectPairEvidenceText = graphHasDirectPairEvidenceText;
graphHasDirectPairEvidenceText = function(a, b, text) {
  a = graphNormalizeName(a); b = graphNormalizeName(b);
  text = graphSafeString(text || "", 5000);
  if (!a || !b || !text) return false;
  if (graphV87EvidenceLooksLikeModelSummary(text)) return false;
  var ea = graphEscapeRegExp(a), eb = graphEscapeRegExp(b);
  var gap = "[^。！？\\n]{0,70}";
  var intro = "(自我介绍|介绍自己|报上姓名|通名|报姓名|开口|说道|答道|回道)?";
  var self = "(在下|本人|我叫|我名|我乃|我便是|我就是|在下名叫|在下叫|在下乃是|在下便是|在下正是)";
  var regs = [
    new RegExp(eb + gap + intro + gap + self + gap + ea),
    new RegExp(ea + gap + intro + gap + self + gap + eb),
    new RegExp(eb + gap + "(自称|称自己为|自号|自报姓名|自报家门)" + gap + ea),
    new RegExp(ea + gap + "(自称|称自己为|自号|自报姓名|自报家门)" + gap + eb),
    new RegExp(eb + gap + "(名字|姓名|名讳|全名)" + gap + "(是|叫|为|乃|正是|便是)" + gap + ea),
    new RegExp(ea + gap + "(名字|姓名|名讳|全名)" + gap + "(是|叫|为|乃|正是|便是)" + gap + eb)
  ];
  for (var i = 0; i < regs.length; i++) { try { if (regs[i].test(text)) return true; } catch(e) {} }
  return graphV87OldHasDirectPairEvidenceText ? graphV87OldHasDirectPairEvidenceText(a, b, text) : false;
};

// voteModelRelations 统一在前置定义中完成归并与字段保留；此处不再覆盖，避免丢失 evidenceFamily/evidenceSubtype。

function graphV87ReasonsHasAny(reasons, list) {
  reasons = reasons || [];
  for (var i = 0; i < list.length; i++) if (graphReasonListHas(reasons, list[i])) return true;
  return false;
}
function graphV87SourceFamilies(reasons, extra) {
  var fam = {};
  reasons = graphCleanSourceReasons(reasons || []);
  extra = graphSafeString(extra || "", 1400);
  function add(x) { if (x) fam[x] = true; }
  for (var i = 0; i < reasons.length; i++) {
    var r = graphSafeString(reasons[i] || "", 100);
    if (/model_name_identity_positive/.test(r)) add("model_name_identity_positive");
    else if (/graph_conflict_verified_same_person/.test(r)) add("conflict_verified_same");
    else if (/alias_refine_confirmed/.test(r)) add("alias_refine_confirmed");
    else if (/model_explicit_different_negative/.test(r)) add("model_explicit_different_negative");
    else if (/graph_conflict_verified_different_person/.test(r)) add("conflict_verified_different");
    else if (/model_(dialogue|action|social|co_presence)_relation_negative/.test(r)) add("model_semantic_negative");
  }
  var out = [];
  for (var k in fam) if (fam.hasOwnProperty(k)) out.push(k);
  return out;
}

function graphV87ExtractCompoundSourceReasons(extra) {
  extra = graphSafeString(extra || "", 1200);
  var m = extra.match(/复合:([^；\n\r]+)/);
  if (!m || !m[1]) return [];
  var raw = m[1].split(/[+|,，、\s]+/);
  var out = [];
  var seen = {};
  for (var i = 0; i < raw.length; i++) {
    var r = graphSafeString(raw[i] || "", 100);
    if (!r || r.indexOf("compound_") === 0 || r === "positive_chain_closed" || r === "triad_interaction_closed") continue;
    if (!seen[r]) { seen[r] = true; out.push(r); }
  }
  return out;
}
function graphV87CompoundSourceStrongEnough(direction, sourceReasons, families) {
  sourceReasons = sourceReasons || [];
  families = families || graphV87SourceFamilies(sourceReasons, "");
  if (families.length < 2) return false;
  if (direction === "positive") {
    return graphV87ReasonsHasAny(sourceReasons, ["model_name_identity_positive","alias_refine_confirmed","graph_conflict_verified_same_person"]) &&
      graphV87ReasonsHasAny(sourceReasons, ["model_name_identity_positive","alias_refine_confirmed","graph_conflict_verified_same_person"]);
  }
  if (direction === "negative") {
    return graphV87ReasonsHasAny(sourceReasons, ["model_dialogue_relation_negative","model_action_relation_negative","model_social_relation_negative","model_co_presence_negative","model_explicit_different_negative","graph_conflict_verified_different_person"]) &&
      graphV87ReasonsHasAny(sourceReasons, ["model_dialogue_relation_negative","model_action_relation_negative","model_social_relation_negative","model_co_presence_negative","model_explicit_different_negative","graph_conflict_verified_different_person"]);
  }
  return false;
}

function graphV87CertifiedCompoundSame(edge, st) {
  if (!edge) return { certified: false, reason: "no_positive_edge", families: [] };
  var rawReasons = edge.reasons || [];
  var extra = edge.extra || "";
  var currentAnchor = !!(edge.lastSeen || graphArrayIntersectsChapters(edge.chapters, [graphCurrentChapterId()]));
  var subtypeCompoundReasons = (typeof graphV908PositiveSubtypeCompoundReasons === "function") ? graphV908PositiveSubtypeCompoundReasons() : [];
  var hasSubtypeCompound = graphV87ReasonsHasAny(rawReasons, subtypeCompoundReasons);
  if (hasSubtypeCompound) {
    var subtypeSrcReasons = graphV87ExtractCompoundSourceReasons(extra);
    var subtypeFamilies = graphV87SourceFamilies(subtypeSrcReasons, "");
    subtypeFamilies.push("model_name_identity_subtype_compound");
    return { certified: true, reason: "compound_name_identity_subtype_positive", families: graphV908ArrayUniqueSorted(subtypeFamilies), sourceReasons: subtypeSrcReasons, independentFamilyCount: graphV908ArrayUniqueSorted(subtypeFamilies).length, hasCurrentAnchor: currentAnchor, persistentSemanticAnchor: true };
  }
  var hasCompoundStrong = graphV87ReasonsHasAny(rawReasons, ["compound_name_alias_positive","compound_introduced_alias_positive","compound_parenthetical_alias_positive","compound_verified_same_person_positive"]);
  if (hasCompoundStrong) {
    var srcReasons = graphV87ExtractCompoundSourceReasons(extra);
    var families = graphV87SourceFamilies(srcReasons, "");
    var ok = currentAnchor && graphV87CompoundSourceStrongEnough("positive", srcReasons, families);
    if (ok) return { certified: true, reason: "compound_positive_l3", families: families, sourceReasons: srcReasons, independentFamilyCount: families.length, hasCurrentAnchor: currentAnchor };
    return { certified: false, reason: "compound_positive_not_l3", families: families, sourceReasons: srcReasons, independentFamilyCount: families.length, hasCurrentAnchor: currentAnchor };
  }
  var reasons = graphCleanSourceReasons(rawReasons);
  var families2 = graphV87SourceFamilies(reasons, "");
  return { certified: false, reason: "not_compound_record_decision", families: families2, independentFamilyCount: families2.length, hasCurrentAnchor: currentAnchor };
}
function graphV87CertifiedCompoundDifferent(edge, st) {
  if (!edge) return { certified: false, reason: "no_negative_edge", families: [] };
  var rawReasons = edge.reasons || [];
  var extra = edge.extra || "";
  var currentAnchor = !!(edge.lastSeen || graphArrayIntersectsChapters(edge.chapters, [graphCurrentChapterId()]));
  var hasCompoundStrong = graphV87ReasonsHasAny(rawReasons, ["compound_explicit_different_negative","compound_speaker_interaction_negative","compound_relationship_interaction_negative"]);
  if (hasCompoundStrong) {
    var srcReasons = graphV87ExtractCompoundSourceReasons(extra);
    var families = graphV87SourceFamilies(srcReasons, "");
    var weakOnly = graphV87ReasonsHasAny(srcReasons, ["same_sentence_cooccur","adjacent_speaker_cooccur","triad_interaction_closed","model_weak_relation_audit"]) &&
      !graphV87ReasonsHasAny(srcReasons, ["model_dialogue_relation_negative","model_action_relation_negative","model_social_relation_negative","model_co_presence_negative","model_explicit_different_negative","graph_conflict_verified_different_person"]);
    var ok = !weakOnly && currentAnchor && graphV87CompoundSourceStrongEnough("negative", srcReasons, families);
    if (ok) return { certified: true, reason: "compound_negative_l3", families: families, sourceReasons: srcReasons, independentFamilyCount: families.length, hasCurrentAnchor: currentAnchor };
    return { certified: false, reason: weakOnly ? "only_statistical_negative" : "compound_negative_not_l3", families: families, sourceReasons: srcReasons, independentFamilyCount: families.length, hasCurrentAnchor: currentAnchor };
  }
  var reasons = graphCleanSourceReasons(rawReasons);
  var families2 = graphV87SourceFamilies(reasons, "");
  return { certified: false, reason: "not_compound_record_decision", families: families2, independentFamilyCount: families2.length, hasCurrentAnchor: currentAnchor };
}

CharacterManager.prototype.v87ChooseMergeTarget = function(recA, recB) {
  function weight(r) {
    if (!r) return 0;
    var aliases = v87SplitAliasesString ? v87SplitAliasesString(r.aliases || r.name) : String(r.aliases || r.name || "").split("|");
    return Number((r.chapters || []).length || 0) * 10 + aliases.length * 3 + Number(r.usageCount || 0) / 100;
  }
  return weight(recA) >= weight(recB) ? { target: recA, source: recB } : { target: recB, source: recA };
};

CharacterManager.prototype.v87ReconcileExistingRecords = function(a, b, relation, reason, sourceStage, contextText, cert) {
  a = graphNormalizeName(a); b = graphNormalizeName(b);
  relation = graphSafeString(relation || "", 40);
  if (!a || !b || a === b || !this.characterRecords) return false;
  var recA = this.findCharacterRecord ? this.findCharacterRecord(a) : null;
  var recB = this.findCharacterRecord ? this.findCharacterRecord(b) : null;
  if (!recA || !recB) return false;
  if (relation === "same_person") {
    if (recA === recB) return false;
    var chosen = this.v87ChooseMergeTarget(recA, recB);
    graphRemoteLog("existing_record_reconcile_merge", { a: a, b: b, target: graphNormalizeName(chosen.target.name), source: graphNormalizeName(chosen.source.name), reason: graphSafeString(reason || "", 220), sourceStage: sourceStage || "", certified: !!cert });
    var ok = this.mergeCharacterRecords ? this.mergeCharacterRecords(chosen.target, chosen.source, reason || sourceStage || "existing_record_reconcile_same_person") : false;
    if (ok && this.saveRecords) this.saveRecords();
    return !!ok;
  }
  if (relation === "different_person") {
    if (recA !== recB) return false;
    graphRemoteLog("existing_record_reconcile_split", { a: a, b: b, mainName: graphNormalizeName(recA.name), reason: graphSafeString(reason || "", 220), sourceStage: sourceStage || "", certified: !!cert });
    return this.splitAliasByConflict ? !!this.splitAliasByConflict(a, b, reason || sourceStage || "existing_record_reconcile_different_person") : false;
  }
  return false;
};

var v87OldVerifyGraphConflictAndFix = CharacterManager.prototype.verifyGraphConflictAndFix;
CharacterManager.prototype.verifyGraphConflictAndFix = function(kind, a, b, score, reason, extra, stage, options) {
  var ret = v87OldVerifyGraphConflictAndFix ? v87OldVerifyGraphConflictAndFix.apply(this, arguments) : { allow: false };
  try {
    if (ret && ret.verified && ret.relation === "same_person" && Number(ret.confidence || 0) >= 80) {
      this.v87ReconcileExistingRecords(a, b, "same_person", ret.reason || extra || reason || "graph_conflict_verified_same_person", stage || "graph_conflict_verify", (options && options.contextText) || "", true);
    } else if (ret && ret.verified && ret.relation === "different_person" && Number(ret.confidence || 0) >= 80) {
      this.v87ReconcileExistingRecords(a, b, "different_person", ret.reason || extra || reason || "graph_conflict_verified_different_person", stage || "graph_conflict_verify", (options && options.contextText) || "", true);
    }
  } catch(e) {}
  return ret;
};

function graphV87PositiveReasonCanMerge(reason, extra) {
  reason = graphSafeString(reason || "", 100);
  if (graphV87IsLocalSingleClosedReason(reason)) return false;
  var edge = { reasons: [reason], extra: extra || "", chapters: [graphCurrentChapterId()], lastSeen: graphNowIso() };
  return graphV87CertifiedCompoundSame(edge, null).certified;
}
function graphV87NegativeReasonCanSplit(reason, extra) {
  reason = graphSafeString(reason || "", 100);
  if (graphV87IsLocalSingleClosedReason(reason)) return false;
  var edge = { reasons: [reason], extra: extra || "", chapters: [graphCurrentChapterId()], lastSeen: graphNowIso() };
  return graphV87CertifiedCompoundDifferent(edge, null).certified;
}

var v87OldRecordPositiveAliasEdge = CharacterManager.prototype.recordPositiveAliasEdge;
CharacterManager.prototype.recordPositiveAliasEdge = function(a, b, score, reason, extra) {
  var ret = v87OldRecordPositiveAliasEdge ? v87OldRecordPositiveAliasEdge.apply(this, arguments) : undefined;
  try { if (graphV87PositiveReasonCanMerge(reason, extra)) this.v87ReconcileExistingRecords(a, b, "same_person", extra || reason || "record_positive_alias_edge", "record_positive_alias_edge", extra || "", true); } catch(e) {}
  return ret;
};

var v87OldRecordNegativeAliasEdge = CharacterManager.prototype.recordNegativeAliasEdge;
CharacterManager.prototype.recordNegativeAliasEdge = function(a, b, score, reason, extra) {
  var ret = v87OldRecordNegativeAliasEdge ? v87OldRecordNegativeAliasEdge.apply(this, arguments) : undefined;
  try { if (graphV87NegativeReasonCanSplit(reason, extra)) this.v87ReconcileExistingRecords(a, b, "different_person", extra || reason || "record_negative_alias_edge", "record_negative_alias_edge", extra || "", true); } catch(e) {}
  return ret;
};

CharacterManager.prototype.v87RunCompoundRecordReconciliation = function(names, chapterText) {
  if (!this.aliasCooccurStats) return 0;
  var pairMap = {};
  function addPair(a, b) { a = graphNormalizeName(a); b = graphNormalizeName(b); if (!a || !b || a === b || graphIsInvalidName(a) || graphIsInvalidName(b)) return; pairMap[graphPairKey(a,b)] = { a: a, b: b }; }
  names = names || [];
  for (var i = 0; i < names.length; i++) for (var j = i + 1; j < names.length; j++) addPair(names[i], names[j]);
  var cur = graphCurrentChapterId();
  for (var k in this.aliasCooccurStats) {
    if (!this.aliasCooccurStats.hasOwnProperty(k) || k.indexOf("__") === 0) continue;
    var st0 = this.aliasCooccurStats[k];
    if (st0 && st0.a && st0.b && graphArrayIntersectsChapters(st0.chapters, [cur])) addPair(st0.a, st0.b);
  }
  var count = 0;
  for (var pk in pairMap) {
    if (!pairMap.hasOwnProperty(pk)) continue;
    var a = pairMap[pk].a, b = pairMap[pk].b;
    var recA = this.findCharacterRecord ? this.findCharacterRecord(a) : null;
    var recB = this.findCharacterRecord ? this.findCharacterRecord(b) : null;
    if (!recA || !recB) continue;
    var st = this.aliasCooccurStats[graphPairKey(a,b)] || null;
    var pe = graphGetEdgeSnapshot(this.aliasPositiveGraph, a, b);
    var ne = graphGetEdgeSnapshot(this.aliasNegativeGraph, a, b);
    if (recA !== recB && pe) {
      var cs = graphV87CertifiedCompoundSame(pe, st);
      if (cs.certified) {
        graphRemoteLog("compound_same_person_certified", { a: a, b: b, decision: "merge_allowed", sourceFamilies: cs.families, independentFamilyCount: cs.independentFamilyCount || 0, hasCurrentAnchor: !!cs.hasCurrentAnchor, reason: cs.reason, action: "existing_record_reconcile_merge" });
        if (this.v87ReconcileExistingRecords(a, b, "same_person", cs.reason || "compound_same_person_certified", "compound_same_person_certified", chapterText || "", true)) count++;
      } else if (pe && graphV87ReasonsHasAny(pe.reasons || [], (typeof graphV908PositiveCompoundRecordReasons === "function") ? graphV908PositiveCompoundRecordReasons() : ["compound_name_alias_positive","compound_introduced_alias_positive","compound_parenthetical_alias_positive","compound_verified_same_person_positive"])) {
        graphRemoteLog("compound_evidence_to_conflict_verify", { a: a, b: b, relation: "same_person", reason: cs.reason, sourceFamilies: cs.families, action: "graph_conflict_verify" });
        if (this.verifyGraphConflictAndFix) this.verifyGraphConflictAndFix("positive", a, b, 4.5, "compound_uncertified_same_person", pe.extra || cs.reason || "", "compound_evidence_to_conflict_verify", { defaultAllow: false, forceVerify: true, contextText: chapterText || this.contextHistory2 || "" });
      }
    }
    if (recA === recB && ne) {
      var cd = graphV87CertifiedCompoundDifferent(ne, st);
      if (cd.certified) {
        graphRemoteLog("compound_different_person_certified", { a: a, b: b, decision: "split_allowed", sourceFamilies: cd.families, independentFamilyCount: cd.independentFamilyCount || 0, hasCurrentAnchor: !!cd.hasCurrentAnchor, reason: cd.reason, targetMainName: graphNormalizeName(recA.name), action: "alias_split_by_compound_certified" });
        if (this.v87ReconcileExistingRecords(a, b, "different_person", cd.reason || "compound_different_person_certified", "compound_different_person_certified", chapterText || "", true)) count++;
      } else if (ne && graphV87ReasonsHasAny(ne.reasons || [], ["compound_explicit_different_negative","compound_speaker_interaction_negative","compound_relationship_interaction_negative"])) {
        graphRemoteLog("compound_evidence_to_conflict_verify", { a: a, b: b, relation: "different_person", reason: cd.reason, sourceFamilies: cd.families, action: "graph_conflict_verify" });
        if (this.verifyGraphConflictAndFix) this.verifyGraphConflictAndFix("negative", a, b, 4.5, "compound_uncertified_different_person", ne.extra || cd.reason || "", "compound_evidence_to_conflict_verify", { defaultAllow: false, forceVerify: true, contextText: chapterText || this.contextHistory2 || "" });
      }
    }
  }
  return count;
};

var v87OldApplyCompoundGraphEvidence = CharacterManager.prototype.applyCompoundGraphEvidence;
CharacterManager.prototype.applyCompoundGraphEvidence = function(names, chapterText) {
  var ret = v87OldApplyCompoundGraphEvidence ? v87OldApplyCompoundGraphEvidence.apply(this, arguments) : { positive: 0, negative: 0, skipped: 0 };
  try { this.v87RunCompoundRecordReconciliation(names || [], chapterText || ""); } catch(e) {}
  try {
    var pscan = this.applyPersistentCompoundGraphEvidence ? this.applyPersistentCompoundGraphEvidence(names || [], chapterText || "") : { positive: 0, negative: 0, hint: 0, skipped: 0 };
    ret.persistentPositive = pscan.positive || 0;
    ret.persistentNegative = pscan.negative || 0;
    ret.persistentHint = pscan.hint || 0;
    ret.positive = Number(ret.positive || 0) + Number(pscan.positive || 0);
    ret.negative = Number(ret.negative || 0) + Number(pscan.negative || 0);
    ret.skipped = Number(ret.skipped || 0) + Number(pscan.skipped || 0);
  } catch(e2) { graphRemoteLog("persistent_compound_scan_error", { error: graphSafeString(e2 && e2.message || e2, 260) }); }
  try { if (this.persistentCompoundRecordReconciliation) this.persistentCompoundRecordReconciliation(chapterText || ""); } catch(e3) { graphRemoteLog("persistent_compound_reconcile_error", { error: graphSafeString(e3 && e3.message || e3, 260) }); }
  return ret;
};

function graphV87CleanHintReasonList(reasons) {
  var out = [];
  var seen = {};
  reasons = reasons || [];
  for (var i = 0; i < reasons.length; i++) {
    var r = graphSafeString(reasons[i] || "", 80);
    if (!r || r === "triad_interaction_closed" || r === "positive_chain_closed" || r.indexOf("compound_") === 0) continue;
    if (!seen[r]) { seen[r] = true; out.push(r); }
  }
  return out;
}
function graphV87CleanHintExtra(extra) {
  extra = graphSafeString(extra || "", 260);
  if (!extra) return "";
  extra = extra.replace(/复合:[^；，。\n]{0,160}[；，。]?/g, "");
  extra = extra.replace(/triad_interaction_closed|positive_chain_closed|compound_[a-zA-Z0-9_]+/g, "");
  extra = extra.replace(/[|+]{2,}/g, "|").replace(/^\s*[|+；，,]+|[|+；，,]+\s*$/g, "");
  return graphSafeString(extra, 220);
}
var v87OldGetAliasRecentGraphData = CharacterManager.prototype.getAliasRecentGraphData;
CharacterManager.prototype.getAliasRecentGraphData = function(recentChapters, newName, candidateList) {
  var data = v87OldGetAliasRecentGraphData ? v87OldGetAliasRecentGraphData.apply(this, arguments) : { positiveEdges: [], negativeEdges: [] };
  function cleanArr(arr) {
    arr = arr || [];
    for (var i = 0; i < arr.length; i++) {
      var beforeReasons = (arr[i].reasons || []).join("|");
      var beforeExtra = arr[i].extra || "";
      arr[i].reasons = graphV87CleanHintReasonList(arr[i].reasons || []);
      arr[i].extra = graphV87CleanHintExtra(arr[i].extra || "");
      if (beforeReasons !== (arr[i].reasons || []).join("|") || beforeExtra !== (arr[i].extra || "")) graphRemoteLog("alias_hint_cleaned", { newName: graphNormalizeName(newName), a: arr[i].a, b: arr[i].b, beforeReasons: graphSafeString(beforeReasons, 180), afterReasons: (arr[i].reasons || []).join("|"), beforeExtra: graphSafeString(beforeExtra, 180), afterExtra: graphSafeString(arr[i].extra || "", 180) });
    }
  }
  cleanArr(data.positiveEdges); cleanArr(data.negativeEdges);
  return data;
};


// ===================== 发声音龄证据审计 + 临时换声状态机 =====================

function graphV908Clone(value, fallback) {
  try { return JSON.parse(JSON.stringify(value)); } catch(e) { return typeof fallback === "undefined" ? null : fallback; }
}

function graphV908CurrentBookKey(manager) {
  var key = manager && manager.aliasGraphBookKey ? manager.aliasGraphBookKey : "";
  if (!key) key = graphBookCacheSafeKey("", graphCurrentBookUrl || "");
  return key || "default";
}

function graphV908NewVoiceAgeEvidenceCache(manager) {
  return {
    schema: "v908_voice_age_evidence_cache",
    dataVersion: graphRuleDataVersion(),
    bookKey: graphV908CurrentBookKey(manager),
    evidence: {},
    updatedAt: graphNowIso()
  };
}

CharacterManager.prototype.loadVoiceAgeEvidenceCache = function() {
  if (!ENABLE_VOICE_AGE_BOOK_CACHE) return false;
  var file = this.voiceAgeEvidenceFile || graphBookCacheFile("voice_age_evidence", graphV908CurrentBookKey(this));
  var raw = graphReadJsonSafe(file, {});
  var expectedVersion = graphRuleDataVersion();
  var expectedBookKey = graphV908CurrentBookKey(this);
  var valid = !!(raw && raw.schema === "v908_voice_age_evidence_cache" && raw.dataVersion === expectedVersion && raw.bookKey === expectedBookKey && raw.evidence && typeof raw.evidence === "object" && !Array.isArray(raw.evidence));
  this.voiceAgeEvidenceCache = valid ? raw : graphV908NewVoiceAgeEvidenceCache(this);
  this.voiceAgeAppliedEvidence = {};
  var evidence = this.voiceAgeEvidenceCache.evidence || {};
  var count = 0;
  var appliedCount = 0;
  for (var key in evidence) {
    if (!evidence.hasOwnProperty(key)) continue;
    count++;
    var item = evidence[key] || {};
    if (item.appliedToNaturalRecord === true) {
      this.voiceAgeAppliedEvidence[key] = true;
      appliedCount++;
    }
  }
  graphRemoteLog("voice_age_book_cache_loaded", { file: file, bookKey: expectedBookKey, dataVersion: expectedVersion, valid: valid, evidenceCount: count, appliedCount: appliedCount, legacyIgnored: !valid && !!(raw && Object.keys(raw).length) });
  return valid;
};

CharacterManager.prototype.saveVoiceAgeEvidenceCache = function(reason) {
  if (!ENABLE_VOICE_AGE_BOOK_CACHE) return false;
  if (!this.voiceAgeEvidenceCache || typeof this.voiceAgeEvidenceCache !== "object") this.voiceAgeEvidenceCache = graphV908NewVoiceAgeEvidenceCache(this);
  this.voiceAgeEvidenceCache.schema = "v908_voice_age_evidence_cache";
  this.voiceAgeEvidenceCache.dataVersion = graphRuleDataVersion();
  this.voiceAgeEvidenceCache.bookKey = graphV908CurrentBookKey(this);
  this.voiceAgeEvidenceCache.updatedAt = graphNowIso();
  if (!this.voiceAgeEvidenceCache.evidence || typeof this.voiceAgeEvidenceCache.evidence !== "object") this.voiceAgeEvidenceCache.evidence = {};
  var file = this.voiceAgeEvidenceFile || graphBookCacheFile("voice_age_evidence", graphV908CurrentBookKey(this));
  var ok = graphWriteJsonSafe(file, this.voiceAgeEvidenceCache);
  graphRemoteLog("voice_age_book_cache_saved", { file: file, bookKey: this.voiceAgeEvidenceCache.bookKey, dataVersion: this.voiceAgeEvidenceCache.dataVersion, evidenceCount: Object.keys(this.voiceAgeEvidenceCache.evidence || {}).length, reason: reason || "update", success: !!ok });
  return !!ok;
};

CharacterManager.prototype.storeVoiceAgeEvidenceCacheV908 = function(list, stage, meta) {
  if (!ENABLE_VOICE_AGE_BOOK_CACHE || !list || !list.length) return 0;
  if (!this.voiceAgeEvidenceCache || typeof this.voiceAgeEvidenceCache !== "object" || this.voiceAgeEvidenceCache.bookKey !== graphV908CurrentBookKey(this) || this.voiceAgeEvidenceCache.dataVersion !== graphRuleDataVersion()) {
    this.voiceAgeEvidenceCache = graphV908NewVoiceAgeEvidenceCache(this);
  }
  var map = this.voiceAgeEvidenceCache.evidence || {};
  meta = meta || {};
  var stored = 0;
  for (var i = 0; i < list.length; i++) {
    var source = list[i] || {};
    var hash = graphSafeString(source.evidenceHash || "", 120);
    if (!hash) hash = "age_" + graphHash([graphV908CurrentBookKey(this), graphCurrentChapterId(), source.evidenceId || i, source.seq || "", source.subjectName || "", source.stateAction || "", graphV908NormalizeAnchorText(source.evidenceText || "")].join("|"));
    var old = map[hash] || {};
    var entry = graphV908Clone(source, {}) || {};
    entry.evidenceHash = hash;
    entry.bookKey = graphV908CurrentBookKey(this);
    entry.dataVersion = graphRuleDataVersion();
    entry.chapterId = graphSafeString(source.chapterId || graphCurrentChapterId(), 80);
    entry.batchKey = graphSafeString(meta.batchKey || source.batchKey || old.batchKey || "", 160);
    entry.currentTextHash = graphSafeString(meta.currentTextHash || old.currentTextHash || "", 120);
    entry.previousTextHash = graphSafeString(meta.previousTextHash || old.previousTextHash || "", 120);
    entry.collectedAt = old.collectedAt || source.collectedAt || graphNowIso();
    entry.lastUpdatedAt = graphNowIso();
    entry.lastStage = stage || "update";
    if (old.appliedToNaturalRecord === true && entry.appliedToNaturalRecord !== true) entry.appliedToNaturalRecord = true;
    if (old.appliedRecordId && !entry.appliedRecordId) entry.appliedRecordId = old.appliedRecordId;
    map[hash] = entry;
    stored++;
  }
  this.voiceAgeEvidenceCache.evidence = map;
  this.saveVoiceAgeEvidenceCache(stage || "update");
  graphRemoteLog("voice_age_book_cache_updated", { stage: stage || "update", bookKey: graphV908CurrentBookKey(this), dataVersion: graphRuleDataVersion(), storedCount: stored, totalCount: Object.keys(map).length, countLimitApplied: false });
  return stored;
};

function graphV908NormalizeAnchorText(text) {
  return String(text || "")
    .replace(/[\s\u3000\u2000-\u200F\u2028-\u202F\uFEFF]/g, "")
    .replace(/[「『]/g, "“")
    .replace(/[」』]/g, "”")
    .replace(/…{2}/g, "……")
    .trim();
}

function graphV908TextContainsAnchor(sourceText, evidenceText) {
  var source = graphV908NormalizeAnchorText(sourceText);
  var evidence = graphV908NormalizeAnchorText(evidenceText);
  return !!(source && evidence && source.indexOf(evidence) !== -1);
}

function graphV908ContextWindowAround(sourceText, evidenceText, radius) {
  var source = String(sourceText || "");
  var evidence = String(evidenceText || "");
  radius = Math.max(80, parseInt(radius, 10) || 220);
  if (!source || !evidence) return "";
  var index = source.indexOf(evidence);
  if (index < 0) {
    // 仅用于审计辅助窗口；预检仍以归一化后的逐字锚点为准。
    var normalizedEvidence = graphV908NormalizeAnchorText(evidence);
    if (normalizedEvidence && graphV908NormalizeAnchorText(source).indexOf(normalizedEvidence) >= 0) return graphSafeString(evidence, VOICE_AGE_EVIDENCE_TEXT_MAX);
    return "";
  }
  return source.substring(Math.max(0, index - radius), Math.min(source.length, index + evidence.length + radius));
}

function graphV908VoiceAgePriority(type) {
  if (type === "voice") return 3;
  if (type === "direct_age") return 2;
  if (type === "appearance") return 1;
  return 0;
}

function graphV908NormalizeVoiceAgeStage(gender, stage) {
  var raw = graphSafeString(stage || "", 40);
  if (!raw) return "";
  var g = graphV908NormalizeGenderForVoice(gender || "", raw);
  if (raw === "少年") return g === "女" ? "少女" : "少年"; // 女性“少年”归一为“少女”，避免同段误判成跨段换声
  if (raw === "男童" || raw === "女童" || raw === "少女" || raw === "男青年" || raw === "女青年" || raw === "男中年" || raw === "女中年" || raw === "男老年" || raw === "女老年") return raw;
  if (/^(童年|幼年|儿童|孩童|小童)$/.test(raw)) return g === "女" ? "女童" : "男童";
  if (/^(少年期|少年|少男|少女期|少女)$/.test(raw)) return g === "女" ? "少女" : "少年";
  if (/^(青年期|青年|年轻|年轻人)$/.test(raw)) return g === "女" ? "女青年" : "男青年";
  if (/^(中年期|中年|壮年)$/.test(raw)) return g === "女" ? "女中年" : "男中年";
  if (/^(老年期|老年|年老|苍老)$/.test(raw)) return g === "女" ? "女老年" : "男老年";
  return "";
}

function graphV908SubjectMatches(manager, subjectName, expectedName) {
  subjectName = graphNormalizeName(subjectName || "");
  expectedName = graphNormalizeName(expectedName || "");
  if (!subjectName || !expectedName) return false;
  if (subjectName === expectedName) return true;
  try {
    var a = manager && manager.findCharacterRecord ? manager.findCharacterRecord(subjectName) : null;
    var b = manager && manager.findCharacterRecord ? manager.findCharacterRecord(expectedName) : null;
    if (a && b && a === b) return true;
    var map = manager && manager.nameToMainNameMap ? manager.nameToMainNameMap : {};
    var ma = graphNormalizeName(map[subjectName] || subjectName);
    var mb = graphNormalizeName(map[expectedName] || expectedName);
    return !!(ma && mb && ma === mb);
  } catch(e) { return false; }
}

function graphV908UniqueStringArray(values, maxLen) {
  values = Array.isArray(values) ? values : [];
  var out = [];
  var seen = {};
  for (var i = 0; i < values.length; i++) {
    var value = graphSafeString(values[i] || "", maxLen || 80);
    if (!value || seen[value]) continue;
    seen[value] = true;
    out.push(value);
  }
  return out;
}

function graphV908SameStringSet(a, b) {
  a = graphV908UniqueStringArray(a || [], 40).sort();
  b = graphV908UniqueStringArray(b || [], 40).sort();
  if (a.length !== b.length) return false;
  for (var i = 0; i < a.length; i++) if (a[i] !== b[i]) return false;
  return true;
}

function graphV908FindRecordByIdOrName(manager, recordId, roleName) {
  var records = manager && Array.isArray(manager.characterRecords) ? manager.characterRecords : [];
  for (var i = 0; i < records.length; i++) {
    if (recordId && graphSafeString(records[i] && records[i].recordId || "", 100) === graphSafeString(recordId, 100)) return records[i];
  }
  return manager && manager.findCharacterRecord ? manager.findCharacterRecord(roleName || "") : null;
}

// 只把当前书、当前连续阅读链中仍生效的少量状态送入下一批，不重复携带全部历史证据。
CharacterManager.prototype.buildActiveTemporaryVoiceStatePackV908 = function(currentDialogueText) {
  if (!this.temporaryVoiceStates) this.temporaryVoiceStates = {};
  var cleared = 0;
  if (!ENABLE_TEMPORARY_VOICE_STATE) {
    cleared = Object.keys(this.temporaryVoiceStates).length;
    if (cleared && this.clearTemporaryVoiceStates) this.clearTemporaryVoiceStates("temporary_voice_feature_disabled_before_prompt");
    graphRemoteLog("temporary_voice_feature_config", { enabled: false, cacheRestoreEnabled: false, crossChapterEnabled: false, activeStateCount: 0, activePromptAttached: false });
    graphRemoteLog("temporary_voice_feature_disabled", { ignoredTemporaryEvidenceCount: 0, clearedMemoryStateCount: cleared, ignoredCacheSnapshot: true, returnedNaturalVoice: true });
    return { items: [], activeStateCount: 0, activeStateSetId: "temp_set_empty", disabled: true };
  }
  // App恰好在批次/章节边界重启时，先用书籍、章节顺序和下一对白哈希恢复快照，再把状态交给本批模型续判。
  if (ENABLE_TEMPORARY_VOICE_CACHE_RESTORE && Object.keys(this.temporaryVoiceStates).length === 0) {
    try {
      var cacheForBoundaryRestore = readDialogCache();
      var snapshotForBoundaryRestore = cacheForBoundaryRestore && cacheForBoundaryRestore.temporaryVoiceSnapshot;
      if (snapshotForBoundaryRestore && snapshotForBoundaryRestore.schema === "v908_temporary_voice_snapshot" && snapshotForBoundaryRestore.bookKey === graphV908CurrentBookKey(this)) {
        var currentHashForBoundary = graphHash(normalizeNameAnalysisDialogueText(currentDialogueText || ""));
        var nextHashesForBoundary = Array.isArray(snapshotForBoundaryRestore.nextExpectedDialogueHashes) ? snapshotForBoundaryRestore.nextExpectedDialogueHashes : (snapshotForBoundaryRestore.nextExpectedDialogueHash ? [snapshotForBoundaryRestore.nextExpectedDialogueHash] : []);
        var sameChapterForBoundary = snapshotForBoundaryRestore.chapterId === graphCurrentChapterId();
        var sequentialChapterForBoundary = ENABLE_TEMPORARY_VOICE_CROSS_CHAPTER && graphV908IsSequentialChapterIndex(snapshotForBoundaryRestore.chapterId, graphCurrentChapterId());
        var nextHashMatches = !!currentHashForBoundary && nextHashesForBoundary.indexOf(currentHashForBoundary) !== -1;
        var oldBatchConsumed = Number(snapshotForBoundaryRestore.nextExpectedIndex || 0) > ((cacheForBoundaryRestore.dialogList || []).length || 0);
        if ((sameChapterForBoundary && nextHashMatches) || (sequentialChapterForBoundary && (nextHashMatches || oldBatchConsumed))) {
          this.temporaryVoiceStates = graphV908Clone(snapshotForBoundaryRestore.states || {}, {}) || {};
          this.temporaryVoiceAppliedEvents = graphV908Clone(snapshotForBoundaryRestore.events || {}, {}) || {};
          for (var restoredKey in this.temporaryVoiceStates) {
            if (!this.temporaryVoiceStates.hasOwnProperty(restoredKey)) continue;
            this.temporaryVoiceStates[restoredKey].previousChapterId = this.temporaryVoiceStates[restoredKey].chapterId || snapshotForBoundaryRestore.chapterId || "";
            this.temporaryVoiceStates[restoredKey].chapterId = graphCurrentChapterId();
            this.temporaryVoiceStates[restoredKey].crossChapterCarryPending = sequentialChapterForBoundary;
          }
          graphRemoteLog("temporary_voice_cache_restore", { snapshotId: snapshotForBoundaryRestore.snapshotId || "", bookKey: snapshotForBoundaryRestore.bookKey || "", snapshotChapterId: snapshotForBoundaryRestore.chapterId || "", currentChapterId: graphCurrentChapterId(), restoreMode: sequentialChapterForBoundary ? "sequential_chapter_batch_boundary" : "same_chapter_next_dialogue_hash", nextHashMatches: nextHashMatches, oldBatchConsumed: oldBatchConsumed, activeStateCount: Object.keys(this.temporaryVoiceStates).length });
        }
      }
    } catch(boundaryRestoreErr) {
      graphRemoteLog("temporary_voice_cache_restore_rejected", { reason: "batch_boundary_restore_exception", error: graphSafeString(boundaryRestoreErr && boundaryRestoreErr.message || boundaryRestoreErr, 260) });
    }
  }
  var bookKey = graphV908CurrentBookKey(this);
  var items = [];
  for (var key in this.temporaryVoiceStates) {
    if (!this.temporaryVoiceStates.hasOwnProperty(key)) continue;
    var state = this.temporaryVoiceStates[key] || {};
    if (state.bookKey !== bookKey) continue;
    if (!state.stateId) state.stateId = "temp_state_" + graphHash([bookKey, state.recordId || "", state.roleName || "", state.startEvidenceHash || key].join("|"));
    var record = graphV908FindRecordByIdOrName(this, state.recordId || "", state.roleName || "");
    var aliases = [];
    if (record) aliases = String(record.aliases || record.name || "").split("|");
    aliases.push(state.roleName || "");
    aliases = graphV908UniqueStringArray(aliases.map(function(x){ return graphNormalizeName(x || ""); }), 120);
    items.push({
      stateId: state.stateId,
      stateKey: key,
      subjectName: graphNormalizeName(record && record.name || state.roleName || ""),
      aliases: aliases,
      recordId: state.recordId || record && record.recordId || "",
      gender: record && record.gender || state.gender || "",
      naturalVoiceAgeStage: state.naturalAgeStage || record && record.age || "",
      temporaryVoiceAgeStage: state.temporaryVoiceAgeStage || "",
      startEvidenceText: graphSafeString(state.startEvidenceText || "", VOICE_AGE_EVIDENCE_TEXT_MAX),
      startEvidenceSummary: graphSafeString(state.startEvidenceSummary || "", 320),
      startEvidenceHash: state.startEvidenceHash || "",
      startChapterId: state.startChapterId || state.chapterId || "",
      startSeq: state.startSeq || "",
      latestAcceptedEvidenceText: graphSafeString(state.latestAcceptedEvidenceText || "", VOICE_AGE_EVIDENCE_TEXT_MAX),
      latestAcceptedSummary: graphSafeString(state.latestAcceptedSummary || "", 320),
      latestAcceptedEvidenceHash: state.latestAcceptedEvidenceHash || "",
      lastConfirmedChapterId: state.lastConfirmedChapterId || state.chapterId || "",
      lastConfirmedSeq: state.lastConfirmedSeq || "",
      roleDialogueCount: Number(state.roleDialogueCount || 0),
      crossChapterCarryPending: state.crossChapterCarryPending === true
    });
  }
  items.sort(function(a, b){ return String(a.stateId).localeCompare(String(b.stateId)); });
  var signatures = items.map(function(item){ return [item.stateId, item.subjectName, item.temporaryVoiceAgeStage, item.startEvidenceHash].join("|"); });
  var pack = { items: items, activeStateCount: items.length, activeStateSetId: items.length ? ("temp_set_" + graphHash(signatures.join("#"))) : "temp_set_empty", disabled: false };
  graphRemoteLog("temporary_voice_feature_config", { enabled: true, cacheRestoreEnabled: !!ENABLE_TEMPORARY_VOICE_CACHE_RESTORE, crossChapterEnabled: !!ENABLE_TEMPORARY_VOICE_CROSS_CHAPTER, activeStateCount: items.length, activePromptAttached: items.length > 0 });
  if (items.length) graphRemoteLog("temporary_voice_active_state_prompt", { activeStateCount: items.length, activeStateSetId: pack.activeStateSetId, states: items.map(function(item){ return { stateId: item.stateId, subjectName: item.subjectName, temporaryStage: item.temporaryVoiceAgeStage, startEvidenceHash: item.startEvidenceHash, latestAcceptedEvidenceHash: item.latestAcceptedEvidenceHash, crossChapterCarryPending: item.crossChapterCarryPending }; }) });
  return pack;
};

CharacterManager.prototype.buildTemporaryVoiceStatePromptV908 = function(pack) {
  pack = pack || { items: [], activeStateCount: 0, activeStateSetId: "temp_set_empty" };
  if (!ENABLE_TEMPORARY_VOICE_STATE || !pack.items || !pack.items.length) return "";
  return "【当前仍在生效的临时换声状态——必须逐项续判】\n" +
    "以下状态已通过此前审计。结合紧邻上文与当前批文本，逐个判断本批是否出现该角色对白，以及旧临时声线是continue、end还是replace。不能用省略返回表示继续。\n" +
    "activeStateCount=" + pack.activeStateCount + "，activeStateSetId=" + pack.activeStateSetId + "。\n" +
    JSON.stringify(pack.items) + "\n" +
    "必须返回顶层__temporaryVoiceStateReview：{\"reviewComplete\":true,\"activeStateCount\":" + pack.activeStateCount + ",\"activeStateSetId\":\"" + pack.activeStateSetId + "\",\"reviews\":[...]}。\n" +
    "reviews必须与上述stateId一一对应。每项必须含stateId、subjectName、hasDialogue、dialogueSeqs、decision、temporaryDialogueSeqs、replacementDialogueSeqs、naturalDialogueSeqs、endBoundarySeq、endTiming、newTemporaryVoiceAgeStage、evidenceText、summary、confidence。\n" +
    "1. 先根据本批姓名分析结果列出该角色全部对白序号。hasDialogue=true时decision只能continue/end/replace，并必须摘录本批连续原文证据和简短概括。\n" +
    "2. hasDialogue=false且本批没有明确状态变化时decision=not_applicable，三个范围数组和evidenceText可为空；旧状态保持，不进入年龄审计。\n" +
    "3. 无对白但旁白明确恢复本声或改用另一种临时声线时，仍返回end/replace及连续原文证据。\n" +
    "4. temporaryDialogueSeqs、replacementDialogueSeqs、naturalDialogueSeqs互不重叠，合并后必须恰好等于dialogueSeqs。continue时全部对白都放temporaryDialogueSeqs。\n" +
    "5. end/replace必须填写精确endBoundarySeq与before_dialogue/after_dialogue；replace还必须填写新临时发声音龄段。不得用角色姓名印象、称号或原著知识判断。\n" +
    "6. 对输入中已有active state的continue/end/replace只写入__temporaryVoiceStateReview，不要在__voiceAgeEvidence重复输出同一事件；__voiceAgeEvidence继续负责新发现的自然年龄、one_shot和start。\n" +
    "7. 跨章携带状态也不能机械继续；新章首批仍按本批原文逐项判断并送审。\n\n";
};

CharacterManager.prototype.normalizeTemporaryVoiceStateReviewV908 = function(rawReview, activePack, batchResult, expectedSeqs, currentText, previousText) {
  activePack = activePack || { items: [], activeStateCount: 0, activeStateSetId: "temp_set_empty" };
  batchResult = batchResult || {};
  expectedSeqs = expectedSeqs || [];
  var expectedSeqMap = {};
  for (var ei = 0; ei < expectedSeqs.length; ei++) expectedSeqMap[String(expectedSeqs[ei])] = true;
  var stateMap = {};
  for (var si = 0; si < activePack.items.length; si++) stateMap[activePack.items[si].stateId] = activePack.items[si];
  var raw = rawReview && typeof rawReview === "object" && !Array.isArray(rawReview) ? rawReview : {};
  var reviews = Array.isArray(raw.reviews) ? raw.reviews : [];
  var globalErrors = [];
  if (raw.reviewComplete !== true) globalErrors.push("review_complete_missing_or_false");
  if (Number(raw.activeStateCount) !== activePack.items.length) globalErrors.push("active_state_count_mismatch");
  if (graphSafeString(raw.activeStateSetId || "", 160) !== activePack.activeStateSetId) globalErrors.push("active_state_set_id_mismatch");
  var duplicateIds = {};
  var unknownIds = [];
  var reviewMap = {};
  for (var ri = 0; ri < reviews.length; ri++) {
    var rawId = graphSafeString(reviews[ri] && reviews[ri].stateId || "", 120);
    if (!rawId || !stateMap[rawId]) { if (rawId) unknownIds.push(rawId); continue; }
    if (reviewMap[rawId]) duplicateIds[rawId] = true;
    else reviewMap[rawId] = reviews[ri];
  }
  var validReviews = [];
  var invalidStateIds = [];
  var candidates = [];
  var validationByState = {};
  var staleSet = globalErrors.indexOf("active_state_set_id_mismatch") !== -1;
  for (var pi = 0; pi < activePack.items.length; pi++) {
    var stateItem = activePack.items[pi];
    var stateId = stateItem.stateId;
    var review = reviewMap[stateId] || null;
    var errors = [];
    if (staleSet) errors.push("active_state_set_id_mismatch");
    if (!review) errors.push("review_missing");
    if (duplicateIds[stateId]) errors.push("duplicate_state_id");
    var actualDialogueSeqs = [];
    for (var asi = 0; asi < expectedSeqs.length; asi++) {
      var seqValue = String(expectedSeqs[asi]);
      var returnedName = graphNormalizeName(batchResult[seqValue] && batchResult[seqValue].name || "");
      var matched = graphV908SubjectMatches(this, returnedName, stateItem.subjectName);
      if (!matched && stateItem.aliases) {
        for (var aliasIndex = 0; aliasIndex < stateItem.aliases.length; aliasIndex++) if (graphV908SubjectMatches(this, returnedName, stateItem.aliases[aliasIndex])) { matched = true; break; }
      }
      if (matched) actualDialogueSeqs.push(seqValue);
    }
    var normalized = null;
    if (review) {
      normalized = {
        stateId: stateId,
        subjectName: graphNormalizeName(review.subjectName || ""),
        hasDialogue: review.hasDialogue === true,
        dialogueSeqs: graphV908UniqueStringArray(review.dialogueSeqs || [], 20),
        decision: graphSafeString(review.decision || "", 30),
        temporaryDialogueSeqs: graphV908UniqueStringArray(review.temporaryDialogueSeqs || [], 20),
        replacementDialogueSeqs: graphV908UniqueStringArray(review.replacementDialogueSeqs || [], 20),
        naturalDialogueSeqs: graphV908UniqueStringArray(review.naturalDialogueSeqs || [], 20),
        endBoundarySeq: graphSafeString(review.endBoundarySeq || "", 20),
        endTiming: graphSafeString(review.endTiming || "", 30),
        newTemporaryVoiceAgeStage: graphSafeString(review.newTemporaryVoiceAgeStage || "", 40),
        evidenceText: graphSafeString(review.evidenceText || "", VOICE_AGE_EVIDENCE_TEXT_MAX),
        summary: graphSafeString(review.summary || "", 400),
        confidence: Number(review.confidence || 0),
        actualDialogueSeqs: actualDialogueSeqs,
        source: "batch_name_analysis"
      };
      if (!graphV908SubjectMatches(this, normalized.subjectName, stateItem.subjectName)) errors.push("subject_name_not_active_state");
      if (normalized.hasDialogue !== (actualDialogueSeqs.length > 0)) errors.push("has_dialogue_mismatch");
      if (!graphV908SameStringSet(normalized.dialogueSeqs, actualDialogueSeqs)) errors.push("dialogue_seqs_not_exact");
      var allRange = normalized.temporaryDialogueSeqs.concat(normalized.replacementDialogueSeqs).concat(normalized.naturalDialogueSeqs);
      if (!graphV908SameStringSet(allRange, normalized.dialogueSeqs) || graphV908UniqueStringArray(allRange, 20).length !== allRange.length) errors.push("dialogue_range_not_exact_partition");
      for (var vsi = 0; vsi < allRange.length; vsi++) if (!expectedSeqMap[allRange[vsi]]) errors.push("dialogue_range_seq_outside_batch");
      if (normalized.hasDialogue && !/^(continue|end|replace)$/.test(normalized.decision)) errors.push("dialogue_decision_invalid");
      if (!normalized.hasDialogue && !/^(not_applicable|end|replace)$/.test(normalized.decision)) errors.push("no_dialogue_decision_invalid");
      if (normalized.decision === "not_applicable" && (normalized.dialogueSeqs.length || normalized.evidenceText || normalized.summary)) errors.push("not_applicable_fields_not_empty");
      if (normalized.decision === "continue" && !graphV908SameStringSet(normalized.temporaryDialogueSeqs, normalized.dialogueSeqs)) errors.push("continue_range_invalid");
      if (normalized.decision !== "not_applicable") {
        if (!normalized.evidenceText || !graphV908TextContainsAnchor(currentText, normalized.evidenceText)) errors.push("review_evidence_not_anchored_in_current_batch");
        if (!normalized.summary) errors.push("review_summary_empty");
      }
      if (normalized.decision === "end" || normalized.decision === "replace") {
        if (normalized.hasDialogue && (!normalized.endBoundarySeq || !expectedSeqMap[normalized.endBoundarySeq])) errors.push("transition_boundary_invalid");
        if (normalized.hasDialogue && normalized.dialogueSeqs.indexOf(normalized.endBoundarySeq) === -1) errors.push("transition_boundary_not_subject_dialogue");
        if (normalized.endTiming !== "before_dialogue" && normalized.endTiming !== "after_dialogue") errors.push("transition_timing_invalid");
        if (normalized.hasDialogue && normalized.decision === "end" && normalized.replacementDialogueSeqs.length) errors.push("end_must_not_have_replacement_dialogues");
        if (normalized.hasDialogue && normalized.decision === "replace" && normalized.naturalDialogueSeqs.length) errors.push("replace_must_not_have_natural_dialogues");
        if (normalized.hasDialogue && normalized.endTiming === "after_dialogue" && normalized.temporaryDialogueSeqs.indexOf(normalized.endBoundarySeq) === -1) errors.push("after_dialogue_boundary_must_use_old_temporary_voice");
        if (normalized.hasDialogue && normalized.endTiming === "before_dialogue" && normalized.decision === "end" && normalized.naturalDialogueSeqs.indexOf(normalized.endBoundarySeq) === -1) errors.push("end_before_boundary_must_use_natural_voice");
        if (normalized.hasDialogue && normalized.endTiming === "before_dialogue" && normalized.decision === "replace" && normalized.replacementDialogueSeqs.indexOf(normalized.endBoundarySeq) === -1) errors.push("replace_before_boundary_must_use_new_temporary_voice");
      }
      if (normalized.decision === "replace" && !graphV908NormalizeVoiceAgeStage(stateItem.gender || "", normalized.newTemporaryVoiceAgeStage)) errors.push("replacement_stage_invalid");
    }
    validationByState[stateId] = { valid: errors.length === 0, errors: graphV908UniqueStringArray(errors, 120), actualDialogueSeqs: actualDialogueSeqs };
    if (errors.length) {
      invalidStateIds.push(stateId);
      continue;
    }
    validReviews.push(normalized);
    if (normalized.decision === "not_applicable") continue;
    var candidateStage = normalized.decision === "end" ? "" : (normalized.decision === "replace" ? graphV908NormalizeVoiceAgeStage(stateItem.gender || "", normalized.newTemporaryVoiceAgeStage) : stateItem.temporaryVoiceAgeStage);
    candidates.push({
      evidenceId: "temp_review_" + graphHash([stateId, graphCurrentChapterId(), normalized.decision, graphV908NormalizeAnchorText(normalized.evidenceText), normalized.dialogueSeqs.join(",")].join("|")),
      seq: normalized.dialogueSeqs[0] || normalized.endBoundarySeq || "",
      subjectName: stateItem.subjectName,
      cues: [],
      finalVoiceAgeStage: candidateStage,
      decisionBasis: normalized.summary,
      applyScope: "scene",
      stateAction: normalized.decision,
      endTiming: normalized.endTiming,
      temporalScope: "current",
      evidenceText: normalized.evidenceText,
      reason: normalized.summary,
      confidence: normalized.confidence,
      sourceType: "temporary_state_review",
      activeStateId: stateId,
      activeStateKey: stateItem.stateKey || "",
      hasDialogue: normalized.hasDialogue,
      dialogueSeqs: normalized.dialogueSeqs,
      temporaryDialogueSeqs: normalized.temporaryDialogueSeqs,
      replacementDialogueSeqs: normalized.replacementDialogueSeqs,
      naturalDialogueSeqs: normalized.naturalDialogueSeqs,
      endBoundarySeq: normalized.endBoundarySeq,
      reviewDecision: normalized.decision,
      summary: normalized.summary,
      incomingTemporaryVoiceAgeStage: stateItem.temporaryVoiceAgeStage || "",
      precheckPassed: false,
      accepted: false
    });
  }
  return {
    complete: globalErrors.length === 0 && invalidStateIds.length === 0 && unknownIds.length === 0 && reviews.length === activePack.items.length,
    globalErrors: globalErrors,
    invalidStateIds: graphV908UniqueStringArray(invalidStateIds, 120),
    unknownStateIds: graphV908UniqueStringArray(unknownIds, 120),
    duplicateStateIds: Object.keys(duplicateIds),
    validReviews: validReviews,
    candidates: candidates,
    validationByState: validationByState,
    activeStateCount: activePack.items.length,
    activeStateSetId: activePack.activeStateSetId
  };
};

CharacterManager.prototype.reviewTemporaryVoiceStatesStandaloneV908 = function(activePack, batchResult, expectedSeqs, currentText, previousText) {
  if (!activePack || !activePack.items || !activePack.items.length) return { success: true, normalized: { complete: true, validReviews: [], candidates: [], invalidStateIds: [] } };
  var manager = this;
  var requestTimeout = ALIAS_ANALYZE_TIMEOUT;
  var maxRetryRound = graphV908CombinedMaxRetryRound();
  var round = 0;
  var speakerTable = [];
  for (var i = 0; i < expectedSeqs.length; i++) speakerTable.push({ seq: String(expectedSeqs[i]), name: graphNormalizeName(batchResult[String(expectedSeqs[i])] && batchResult[String(expectedSeqs[i])].name || "") });
  var prompt = "你只执行临时换声状态续判，不重新做姓名分析、年龄证据抽取或图谱抽取。\n" +
    manager.buildTemporaryVoiceStatePromptV908(activePack) +
    "【本批已完成的说话人序号表】\n" + JSON.stringify(speakerTable) + "\n【紧邻上文】\n" + graphSafeString(previousText || "", 4200) + "\n【当前批文本】\n" + graphSafeString(currentText || "", 8200) + "\n只输出包含__temporaryVoiceStateReview的严格JSON。";
  graphRemoteLog("temporary_voice_state_review_fallback_start", { activeStateCount: activePack.items.length, activeStateSetId: activePack.activeStateSetId, stateIds: activePack.items.map(function(x){ return x.stateId; }), maxRetryRound: maxRetryRound });
  function sleep(ms) { var start = Date.now(); while (Date.now() - start < ms) {} }
  function buildRequest(apiConfig) {
    return { endpoint: apiConfig.endpoint, data: { model: apiConfig.model, messages: [{ role: "system", content: "只输出JSON；逐个返回输入中的临时换声状态续判，不得漏stateId。" }, { role: "user", content: prompt }], temperature: 0.1 }, headers: { "Content-Type": "application/json", "Authorization": "Bearer " + apiConfig.key, "Connection": "keep-alive", "Timeout": requestTimeout.toString() } };
  }
  function parseResponse(response) {
    var body = String(response.body().string() || "{}");
    var outer = JSON.parse(body);
    if (!outer.choices || !outer.choices[0] || !outer.choices[0].message || !outer.choices[0].message.content) throw new Error("临时换声单独续判缺少content");
    var content = String(outer.choices[0].message.content || "").replace(/```json|```/g, "").trim();
    var match = content.match(/\{[\s\S]*\}/);
    if (!match) throw new Error("临时换声单独续判返回非JSON");
    var raw = JSON.parse(match[0]);
    var normalized = manager.normalizeTemporaryVoiceStateReviewV908(raw.__temporaryVoiceStateReview || raw, activePack, batchResult, expectedSeqs, currentText, previousText);
    if (!normalized.complete) throw new Error("临时换声单独续判结构不完整:" + normalized.invalidStateIds.join(","));
    return { raw: raw, normalized: normalized };
  }
  while (round < maxRetryRound) {
    round++;
    var result = concurrentApiRequest("relationAudit", buildRequest, parseResponse, null, requestTimeout);
    if (result.success) {
      var picked = result.isMultiResult && Array.isArray(result.data) ? (result.data[0] && (result.data[0].data || result.data[0])) : result.data;
      if (picked && picked.normalized && picked.normalized.complete) {
        graphRemoteLog("temporary_voice_state_review_fallback_result", { success: true, retryCount: round, activeStateSetId: activePack.activeStateSetId, validStateIds: picked.normalized.validReviews.map(function(x){ return x.stateId; }) });
        return { success: true, normalized: picked.normalized, raw: picked.raw || {} };
      }
    }
    if (round < maxRetryRound) sleep(250);
  }
  graphRemoteLog("temporary_voice_state_review_retry_exhausted", { activeStateCount: activePack.items.length, activeStateSetId: activePack.activeStateSetId, stateIds: activePack.items.map(function(x){ return x.stateId; }), retryCount: round, policy: "旧临时状态保持，不采纳未经审计的结束或替换" });
  return { success: false, reason: "temporary_state_review_retry_exhausted" };
};

CharacterManager.prototype.resolveTemporaryVoiceStateReviewV908 = function(batchResult, activePack, expectedSeqs, currentText, previousText) {
  activePack = activePack || { items: [], activeStateCount: 0, activeStateSetId: "temp_set_empty" };
  if (!ENABLE_TEMPORARY_VOICE_STATE || !activePack.items.length) return [];
  var raw = batchResult && batchResult.__temporaryVoiceStateReview || {};
  graphRemoteLog("temporary_voice_state_review_raw", { activeStateCount: activePack.items.length, activeStateSetId: activePack.activeStateSetId, rawReview: raw });
  var normalized = this.normalizeTemporaryVoiceStateReviewV908(raw, activePack, batchResult, expectedSeqs, currentText, previousText);
  var validReviews = normalized.validReviews.slice(0);
  var candidates = normalized.candidates.slice(0);
  if (!normalized.complete) {
    var retryStateIds = normalized.invalidStateIds.slice(0);
    var repairableHeaderErrorMap = { review_complete_missing_or_false: true, active_state_count_mismatch: true };
    var headerOnlyRepairable = normalized.globalErrors.length > 0 && normalized.invalidStateIds.length === 0 && normalized.unknownStateIds.length === 0 && normalized.duplicateStateIds.length === 0 && validReviews.length === activePack.items.length;
    for (var headerErrorIndex = 0; headerOnlyRepairable && headerErrorIndex < normalized.globalErrors.length; headerErrorIndex++) {
      if (!repairableHeaderErrorMap[normalized.globalErrors[headerErrorIndex]]) headerOnlyRepairable = false;
    }
    // 只有reviewComplete或activeStateCount总数字段写错、而每个stateId内容都完整时，本地修正头部并保留全部逐状态结果。
    // activeStateSetId不匹配会在normalize阶段把全部状态标为无效，仍会进入整组单独续判，避免串批。
    if (normalized.globalErrors.length && !retryStateIds.length && !headerOnlyRepairable) retryStateIds = activePack.items.map(function(x){ return x.stateId; });
    graphRemoteLog("temporary_voice_state_review_incomplete", { activeStateSetId: activePack.activeStateSetId, globalErrors: normalized.globalErrors, invalidStateIds: normalized.invalidStateIds, unknownStateIds: normalized.unknownStateIds, duplicateStateIds: normalized.duplicateStateIds, preservedValidStateIds: validReviews.map(function(x){ return x.stateId; }), retryStateIds: retryStateIds, headerRepairedLocally: headerOnlyRepairable, localSemanticJudgmentUsed: false });
    var retryItems = [];
    for (var i = 0; i < activePack.items.length; i++) if (retryStateIds.indexOf(activePack.items[i].stateId) !== -1) retryItems.push(activePack.items[i]);
    if (retryItems.length) {
      var retrySignatures = retryItems.map(function(item){ return [item.stateId, item.subjectName, item.temporaryVoiceAgeStage, item.startEvidenceHash].join("|"); });
      var retryPack = { items: retryItems, activeStateCount: retryItems.length, activeStateSetId: "temp_set_" + graphHash(retrySignatures.sort().join("#")), disabled: false };
      var retry = this.reviewTemporaryVoiceStatesStandaloneV908(retryPack, batchResult, expectedSeqs, currentText, previousText);
      if (retry && retry.success && retry.normalized) {
        var retriedIdMap = {};
        for (var retryIdIndex = 0; retryIdIndex < retryStateIds.length; retryIdIndex++) retriedIdMap[retryStateIds[retryIdIndex]] = true;
        validReviews = validReviews.filter(function(reviewItem){ return !retriedIdMap[reviewItem.stateId]; });
        candidates = candidates.filter(function(candidateItem){ return !retriedIdMap[candidateItem.activeStateId]; });
        validReviews = validReviews.concat(retry.normalized.validReviews || []);
        candidates = candidates.concat(retry.normalized.candidates || []);
      }
    }
  }
  var validMap = {};
  for (var vi = 0; vi < validReviews.length; vi++) {
    var review = validReviews[vi];
    validMap[review.stateId] = review;
    graphRemoteLog("temporary_voice_state_review_per_role", { stateId: review.stateId, subjectName: review.subjectName, hasDialogue: review.hasDialogue, dialogueSeqs: review.dialogueSeqs, decision: review.decision, temporaryDialogueSeqs: review.temporaryDialogueSeqs, replacementDialogueSeqs: review.replacementDialogueSeqs, naturalDialogueSeqs: review.naturalDialogueSeqs, evidenceText: review.evidenceText, summary: review.summary, enteredAgeAudit: review.decision !== "not_applicable" });
    if (review.decision === "not_applicable") graphRemoteLog("temporary_voice_state_no_dialogue_carried", { stateId: review.stateId, subjectName: review.subjectName, reason: "本批无该角色对白且无明确状态变化，原状态保持", roleDialogueCountIncreased: false });
    if (review.decision === "end") graphRemoteLog("temporary_voice_state_end_detected", { stateId: review.stateId, subjectName: review.subjectName, hasDialogue: review.hasDialogue, endBoundarySeq: review.endBoundarySeq || "", endTiming: review.endTiming || "", evidenceText: review.evidenceText || "", enteredAgeAudit: true });
    if (review.decision === "replace") graphRemoteLog("temporary_voice_state_replace_detected", { stateId: review.stateId, subjectName: review.subjectName, hasDialogue: review.hasDialogue, endBoundarySeq: review.endBoundarySeq || "", endTiming: review.endTiming || "", newTemporaryVoiceAgeStage: review.newTemporaryVoiceAgeStage || "", evidenceText: review.evidenceText || "", enteredAgeAudit: true });
  }
  for (var pi = 0; pi < activePack.items.length; pi++) {
    var stateRef = this.temporaryVoiceStates && this.temporaryVoiceStates[activePack.items[pi].stateKey];
    if (stateRef) {
      stateRef.crossChapterCarryPending = false;
      stateRef.lastReviewAttemptChapterId = graphCurrentChapterId();
      stateRef.lastReviewComplete = !!validMap[activePack.items[pi].stateId];
    }
  }
  batchResult.__temporaryVoiceStateReview = { reviewComplete: validReviews.length === activePack.items.length, activeStateCount: activePack.items.length, activeStateSetId: activePack.activeStateSetId, reviews: validReviews };
  return candidates;
};

// 同一条已审计候选可按序号机械展开为start/continue/end/replace，不重新判断语义。
function graphV908VoiceAgeScheduleSeqsV908(item) {
  item = item || {};
  var seqs = [];
  if (item.sourceType === "temporary_state_review") {
    seqs = (item.dialogueSeqs || []).slice(0);
    if (item.endBoundarySeq) seqs.push(item.endBoundarySeq);
  } else {
    if (item.seq) seqs.push(item.seq);
    if (item.stateAction === "start" && item.coverageMode === "through_seq" && item.effectiveThroughSeq) seqs.push(item.effectiveThroughSeq);
  }
  return graphV908UniqueStringArray(seqs, 20);
}

function graphV908ExpandVoiceAgeEvidenceForSeqV908(item, seq) {
  item = item || {};
  seq = graphSafeString(seq || item.seq || "", 20);
  if (item.sourceType === "temporary_state_review") {
    var temporarySeqs = item.temporaryDialogueSeqs || [];
    var replacementSeqs = item.replacementDialogueSeqs || [];
    var naturalSeqs = item.naturalDialogueSeqs || [];
    var transitionAtBoundary = item.endBoundarySeq && seq === item.endBoundarySeq;
    if (item.reviewDecision === "end" && transitionAtBoundary) {
      var endClone = graphV908Clone(item, {}) || {};
      endClone.stateAction = "end";
      endClone.finalVoiceAgeStage = "";
      return [endClone];
    }
    if (naturalSeqs.indexOf(seq) !== -1 && item.reviewDecision === "end") return [];
    if (item.reviewDecision === "replace" && (transitionAtBoundary || replacementSeqs.indexOf(seq) === 0)) {
      var replaceClone = graphV908Clone(item, {}) || {};
      replaceClone.stateAction = "replace";
      return [replaceClone];
    }
    if (temporarySeqs.indexOf(seq) !== -1 || replacementSeqs.indexOf(seq) !== -1) {
      var continueClone = graphV908Clone(item, {}) || {};
      continueClone.stateAction = "continue";
      if (replacementSeqs.indexOf(seq) !== -1) continueClone.finalVoiceAgeStage = item.finalVoiceAgeStage || continueClone.finalVoiceAgeStage;
      else continueClone.finalVoiceAgeStage = item.incomingTemporaryVoiceAgeStage || continueClone.finalVoiceAgeStage;
      return [continueClone];
    }
    return [];
  }
  if (item.stateAction === "start" && item.coverageMode === "through_seq" && item.effectiveThroughSeq) {
    var rangeOut = [];
    if (seq === item.effectiveFromSeq || seq === item.seq) rangeOut.push(graphV908Clone(item, {}) || {});
    if (seq === item.effectiveThroughSeq) {
      var scheduledEnd = graphV908Clone(item, {}) || {};
      scheduledEnd.stateAction = "end";
      scheduledEnd.finalVoiceAgeStage = "";
      scheduledEnd.endTiming = item.endTiming === "before_dialogue" ? "before_dialogue" : "after_dialogue";
      scheduledEnd.evidenceId = item.evidenceId + "_scheduled_end";
      scheduledEnd.evidenceHash = item.evidenceHash + "_scheduled_end";
      rangeOut.push(scheduledEnd);
    }
    return rangeOut;
  }
  return [graphV908Clone(item, {}) || {}];
}

// v90.8 证据等级归一：只认 L0-L4；缺失或非法归 L0（本地不按关键词推断，仅做结构归一）。
function graphV908NormalizeEvidenceLevel(level) {
  var v = graphSafeString(level || "", 8).toUpperCase();
  if (v === "L0" || v === "L1" || v === "L2" || v === "L3" || v === "L4") return v;
  return "L0";
}
// v90.8 等级-cue 允许矩阵：L4=voice；L3=direct_age；L2=appearance；L1=relationship/context/timeline；L0=provisional。
function graphV908AllowedCueTypesForLevel(level) {
  switch (level) {
    case "L4": return ["voice"];
    case "L3": return ["direct_age"];
    case "L2": return ["appearance"];
    case "L1": return ["relationship", "context", "timeline"];
    default: return ["provisional"];
  }
}
CharacterManager.prototype.precheckVoiceAgeEvidence = function(list, batchResult, expectedSeqs, currentText, previousText) {
  list = Array.isArray(list) ? list.slice(0) : [];
  batchResult = batchResult || {};
  var expectedMap = {};
  for (var i = 0; i < (expectedSeqs || []).length; i++) expectedMap[String(expectedSeqs[i])] = true;
  var seenSignature = {};
  var seenEvidenceId = {};
  var output = [];
  this._v908LastVoiceAgePrefilterStats = { inputCount: list.length, fixedVoiceSkippedCount: 0, sameSegmentSkippedCount: 0, outputCount: 0 };
  for (var index = 0; index < list.length; index++) {
    var source = list[index] || {};
    var item = graphV908Clone(source, {}) || {};
    item.evidenceId = graphSafeString(item.evidenceId || ("age_" + (index + 1)), 80);
    item.seq = graphSafeString(item.seq || "", 20);
    item.subjectName = graphNormalizeName(item.subjectName || item.name || "");
    item.applyScope = graphSafeString(item.applyScope || "uncertain", 30);
    item.stateAction = graphSafeString(item.stateAction || "", 30);
    item.endTiming = graphSafeString(item.endTiming || "", 30);
    item.temporalScope = graphSafeString(item.temporalScope || "uncertain", 30);
    item.evidenceText = graphSafeString(item.evidenceText || "", VOICE_AGE_EVIDENCE_TEXT_MAX);
    item.reason = graphSafeString(item.reason || item.decisionBasis || "", 320);
    item.confidence = Number(item.confidence || 0);
    item.sourceType = graphSafeString(item.sourceType || "voice_age_evidence", 60);
    item.activeStateId = graphSafeString(item.activeStateId || "", 120);
    item.hasDialogue = item.hasDialogue === true;
    item.dialogueSeqs = graphV908UniqueStringArray(item.dialogueSeqs || [], 20);
    item.temporaryDialogueSeqs = graphV908UniqueStringArray(item.temporaryDialogueSeqs || [], 20);
    item.replacementDialogueSeqs = graphV908UniqueStringArray(item.replacementDialogueSeqs || [], 20);
    item.naturalDialogueSeqs = graphV908UniqueStringArray(item.naturalDialogueSeqs || [], 20);
    item.endBoundarySeq = graphSafeString(item.endBoundarySeq || "", 20);
    item.reviewDecision = graphSafeString(item.reviewDecision || "", 30);
    item.summary = graphSafeString(item.summary || "", 400);
    item.effectiveFromSeq = graphSafeString(item.effectiveFromSeq || item.seq || "", 20);
    item.coverageMode = graphSafeString(item.coverageMode || (item.stateAction === "one_shot" ? "current_dialogue" : (item.stateAction === "start" ? "beyond_batch" : "")), 40);
    item.effectiveThroughSeq = graphSafeString(item.effectiveThroughSeq || "", 20);
    item.continuesBeyondBatch = item.continuesBeyondBatch === true;
    item.linkedEndEvidenceText = graphSafeString(item.linkedEndEvidenceText || "", VOICE_AGE_EVIDENCE_TEXT_MAX);
    // v90.8 分级证据字段：本地按原规则宽松风格接收；缺失给默认，不因缺字段整批硬拒。
    item.evidenceLevel = graphV908NormalizeEvidenceLevel(item.evidenceLevel);
    item.evidenceType = graphSafeString(item.evidenceType || "", 40);
    item.evidenceSubtype = graphSafeString(item.evidenceSubtype || "", 60);
    item.priorityCueType = graphSafeString(item.priorityCueType || "", 40);
    item.supersedesPrior = item.supersedesPrior === true;
    item.priorEvidenceId = graphSafeString(item.priorEvidenceId || "", 80);
    item.accepted = false;
    item.precheckPassed = false;
    item.precheckReason = "";
    var expectedItem = batchResult[item.seq] || {};
    var expectedName = graphNormalizeName(expectedItem.name || "");
    var isTemporaryStateReview = item.sourceType === "temporary_state_review";
    var rejectReason = "";
    if (seenEvidenceId[item.evidenceId]) rejectReason = "duplicate_evidence_id_in_batch";
    else seenEvidenceId[item.evidenceId] = true;
    if (!rejectReason && !isTemporaryStateReview && (!item.seq || !expectedMap[item.seq])) rejectReason = "seq_not_in_current_batch";
    else if (!rejectReason && isTemporaryStateReview && item.hasDialogue && (!item.seq || !expectedMap[item.seq])) rejectReason = "temporary_review_seq_not_in_current_batch";
    else if (!rejectReason && (!isTemporaryStateReview || item.hasDialogue) && (!expectedName || !graphV908SubjectMatches(this, item.subjectName, expectedName))) rejectReason = "subject_not_current_seq_speaker";
    else if (!rejectReason && isTemporaryStateReview && (!item.activeStateId || !this.temporaryVoiceStates || !Object.keys(this.temporaryVoiceStates).some(function(stateKey){ return !!(this.temporaryVoiceStates[stateKey] && this.temporaryVoiceStates[stateKey].stateId === item.activeStateId); }, this))) rejectReason = "active_temporary_state_not_found";
    else if (!rejectReason && !item.evidenceText) rejectReason = "evidence_text_empty";

    var anchorRegion = "";
    if (!rejectReason) {
      if (graphV908TextContainsAnchor(currentText, item.evidenceText)) anchorRegion = "current_text";
      else if (graphV908TextContainsAnchor(previousText, item.evidenceText)) anchorRegion = "direct_previous_context";
      else rejectReason = "evidence_text_not_anchored";
    }

    var validActions = { one_shot: true, start: true, end: true, continue: true, replace: true, persistent_update: true };
    if (!rejectReason && !validActions[item.stateAction]) rejectReason = "state_action_invalid";
    if (!rejectReason && item.temporalScope !== "current") rejectReason = "not_current_temporal_scope";
    if (!rejectReason && item.applyScope === "uncertain") rejectReason = "apply_scope_uncertain";
    if (!rejectReason && item.stateAction === "one_shot" && item.applyScope !== "current_dialogue") rejectReason = "one_shot_scope_mismatch";
    if (!rejectReason && item.stateAction === "start" && item.applyScope !== "scene") rejectReason = "start_scope_mismatch";
    if (!rejectReason && item.stateAction === "persistent_update" && item.applyScope !== "persistent") rejectReason = "persistent_scope_mismatch";
    if (!rejectReason && item.stateAction === "end" && item.endTiming !== "before_dialogue" && item.endTiming !== "after_dialogue") rejectReason = "end_timing_invalid";
    if (!rejectReason && (item.stateAction === "continue" || item.stateAction === "replace") && item.applyScope !== "scene") rejectReason = "temporary_review_scope_mismatch";
    if (!rejectReason && item.stateAction === "replace" && item.endTiming !== "before_dialogue" && item.endTiming !== "after_dialogue") rejectReason = "replace_timing_invalid";

    // 新临时事件只检查结构与范围；是否真的存在持续/结束语义交由年龄审计模型。
    if (!rejectReason && !isTemporaryStateReview && item.stateAction === "one_shot") {
      if (item.coverageMode !== "current_dialogue" || item.effectiveFromSeq !== item.seq || item.effectiveThroughSeq !== item.seq || item.continuesBeyondBatch) rejectReason = "one_shot_range_invalid";
    }
    if (!rejectReason && !isTemporaryStateReview && item.stateAction === "start") {
      if (item.effectiveFromSeq !== item.seq || (item.coverageMode !== "through_seq" && item.coverageMode !== "beyond_batch")) rejectReason = "start_range_invalid";
      else if (item.coverageMode === "through_seq" && (!item.effectiveThroughSeq || !expectedMap[item.effectiveThroughSeq] || item.continuesBeyondBatch || !item.linkedEndEvidenceText || !graphV908TextContainsAnchor(currentText, item.linkedEndEvidenceText))) rejectReason = "start_through_range_incomplete";
      else if (item.coverageMode === "through_seq" && !graphV908SubjectMatches(this, item.subjectName, batchResult[item.effectiveThroughSeq] && batchResult[item.effectiveThroughSeq].name || "")) rejectReason = "start_through_boundary_not_subject_dialogue";
      else if (item.coverageMode === "through_seq" && item.effectiveThroughSeq === item.effectiveFromSeq && item.endTiming === "before_dialogue") rejectReason = "start_and_end_before_same_dialogue_conflict";
      else if (item.coverageMode === "beyond_batch" && (item.effectiveThroughSeq || !item.continuesBeyondBatch)) rejectReason = "start_beyond_batch_range_invalid";
    }

    var cues = Array.isArray(item.cues) ? item.cues : [];
    var normalizedCues = [];
    var maxPriority = 0;
    var topStages = {};
    var topTypes = {};
    if (!rejectReason && !isTemporaryStateReview && cues.length > 6) rejectReason = "too_many_age_cues";
    if (!rejectReason && !isTemporaryStateReview && item.stateAction !== "end" && cues.length === 0) rejectReason = "age_cues_empty";
    for (var cueIndex = 0; cueIndex < cues.length && !rejectReason; cueIndex++) {
      var cue = cues[cueIndex] || {};
      var cueType = graphSafeString(cue.type || "", 30);
      var priority = graphV908VoiceAgePriority(cueType);
      var cueText = graphSafeString(cue.evidenceText || "", VOICE_AGE_EVIDENCE_TEXT_MAX);
      var cueStage = graphV908NormalizeVoiceAgeStage(expectedItem.gender || "", cue.stage || "");
      if (!priority) { rejectReason = "cue_type_invalid"; break; }
      if (!cueText || (!graphV908TextContainsAnchor(currentText, cueText) && !graphV908TextContainsAnchor(previousText, cueText))) { rejectReason = "cue_text_not_anchored"; break; }
      if (!cueStage) { rejectReason = "cue_stage_invalid"; break; }
      normalizedCues.push({ type: cueType, stage: cueStage, evidenceText: cueText, priority: priority });
      if (priority > maxPriority) { maxPriority = priority; topStages = {}; topTypes = {}; }
      if (priority === maxPriority) { topStages[cueStage] = true; topTypes[cueType] = true; }
    }
    item.cues = normalizedCues;

    var selectedStage = "";
    var topStageKeys = Object.keys(topStages);
    if (!rejectReason && !isTemporaryStateReview && item.stateAction !== "end") {
      if (topStageKeys.length !== 1) rejectReason = topStageKeys.length > 1 ? "same_priority_stage_conflict" : "no_usable_age_stage";
      else selectedStage = topStageKeys[0];
    }
    var declaredStage = graphV908NormalizeVoiceAgeStage(expectedItem.gender || "", item.finalVoiceAgeStage || item.stage || "");
    if (!rejectReason && !isTemporaryStateReview && item.stateAction !== "end" && (!declaredStage || declaredStage !== selectedStage)) rejectReason = "final_stage_not_follow_priority";
    if (!rejectReason && isTemporaryStateReview && item.stateAction !== "end" && !declaredStage) rejectReason = "temporary_review_stage_invalid";
    if (!rejectReason && item.stateAction === "end" && declaredStage) rejectReason = "end_event_must_restore_natural_voice";
    item.finalVoiceAgeStage = item.stateAction === "end" ? "" : (isTemporaryStateReview ? declaredStage : selectedStage);
    item.priorityCueType = isTemporaryStateReview ? "audited_active_state_transition" : (maxPriority === 3 ? "voice" : (maxPriority === 2 ? "direct_age" : (maxPriority === 1 ? "appearance" : "end_event")));
    var existingVoiceAgeRecord = this.findCharacterRecord ? this.findCharacterRecord(item.subjectName) : null;
    item.currentVoiceGender = existingVoiceAgeRecord && existingVoiceAgeRecord.gender || "";
    item.currentVoiceAge = existingVoiceAgeRecord && existingVoiceAgeRecord.age || "";
    item.currentVoiceTag = existingVoiceAgeRecord && existingVoiceAgeRecord.voice || "";
    item.currentVoiceSegment = existingVoiceAgeRecord ? graphV908VoiceSegmentKey(item.currentVoiceGender, item.currentVoiceAge) : "";
    var targetVoiceGender = graphV908NormalizeGenderForVoice(item.currentVoiceGender || expectedItem.gender || "", item.finalVoiceAgeStage || "");
    var targetVoiceAge = item.finalVoiceAgeStage ? graphV908NormalizeVoiceAgeStage(targetVoiceGender, item.finalVoiceAgeStage) : "";
    item.targetVoiceSegment = targetVoiceAge ? graphV908VoiceSegmentKey(targetVoiceGender, targetVoiceAge) : "";
    // v90.8：只有 L2-L4 才可能跨年龄段换声；L0(暂定)/L1(仅存证) 永不跨段更新自然音色。
    item.willChangeVoiceSegment = item.stateAction === "persistent_update" && (item.evidenceLevel === "L2" || item.evidenceLevel === "L3" || item.evidenceLevel === "L4") && !!item.currentVoiceSegment && !!item.targetVoiceSegment && item.currentVoiceSegment !== item.targetVoiceSegment;
    item.anchorRegion = anchorRegion;
    item.evidenceHash = "age_" + graphHash([graphV908CurrentBookKey(this), graphCurrentChapterId(), item.seq, item.subjectName, item.stateAction, item.endTiming, item.finalVoiceAgeStage, graphV908NormalizeAnchorText(item.evidenceText)].join("|"));
    var signature = [item.seq, item.subjectName, item.stateAction, item.endTiming, item.finalVoiceAgeStage, graphV908NormalizeAnchorText(item.evidenceText)].join("|");
    if (!rejectReason && seenSignature[signature]) rejectReason = "duplicate_evidence_in_batch";
    if (!rejectReason) seenSignature[signature] = true;

    // 用户固定音色优先于永久年龄变化和全部临时换声；固定期间不审计、不缓存、不调度。
    if (!rejectReason && existingVoiceAgeRecord && graphV908IsFixedVoiceRecord(existingVoiceAgeRecord)) {
      this._v908LastVoiceAgePrefilterStats.fixedVoiceSkippedCount++;
      graphRemoteLog("voice_age_fixed_voice_prefiltered", {
        evidenceId: item.evidenceId,
        evidenceHash: item.evidenceHash,
        seq: item.seq,
        subjectName: item.subjectName,
        stateAction: item.stateAction,
        currentVoiceSegment: item.currentVoiceSegment,
        targetVoiceSegment: item.targetVoiceSegment,
        fixedVoiceTag: graphV908FixedVoiceTagOfRecord(existingVoiceAgeRecord) || existingVoiceAgeRecord.voice || "",
        reason: "用户固定音色期间禁止自然年龄更新和临时换声"
      });
      continue;
    }

    // 已有稳定角色的永久年龄候选与当前声音年龄段相同时，本地直接跳过；临时状态、新角色和无法定位角色不走此门槛。
    if (!rejectReason && existingVoiceAgeRecord && item.stateAction === "persistent_update" && item.currentVoiceSegment && item.targetVoiceSegment && item.currentVoiceSegment === item.targetVoiceSegment) {
      this._v908LastVoiceAgePrefilterStats.sameSegmentSkippedCount++;
      graphRemoteLog("voice_age_same_segment_prefiltered", {
        evidenceId: item.evidenceId,
        evidenceHash: item.evidenceHash,
        seq: item.seq,
        subjectName: item.subjectName,
        currentVoiceSegment: item.currentVoiceSegment,
        targetVoiceSegment: item.targetVoiceSegment,
        evidenceText: item.evidenceText,
        reason: "当前稳定角色与候选年龄段相同，无需审计、缓存或重新分配发音人"
      });
      continue;
    }

    item.precheckPassed = !rejectReason;
    item.precheckReason = rejectReason || (isTemporaryStateReview ? "local_state_id_range_anchor_passed" : "local_shape_anchor_priority_passed");
    item.auditDecision = rejectReason ? "reject" : "pending";
    item.auditReason = rejectReason ? rejectReason : "waiting_independent_audit";
    graphRemoteLog("voice_age_priority", { evidenceId: item.evidenceId, seq: item.seq, subjectName: item.subjectName, cueCount: item.cues.length, priorityCueType: item.priorityCueType, finalVoiceAgeStage: item.finalVoiceAgeStage, stateAction: item.stateAction, applyScope: item.applyScope, currentVoiceSegment: item.currentVoiceSegment, targetVoiceSegment: item.targetVoiceSegment, willChangeVoiceSegment: item.willChangeVoiceSegment, result: rejectReason ? "reject" : "pass", reason: item.precheckReason });
    graphRemoteLog("voice_age_precheck", { evidenceId: item.evidenceId, evidenceHash: item.evidenceHash, seq: item.seq, subjectName: item.subjectName, passed: item.precheckPassed, reason: item.precheckReason, anchorRegion: item.anchorRegion, evidenceText: item.evidenceText });
    if (item.stateAction === "start" || item.stateAction === "one_shot") graphRemoteLog("temporary_voice_range_decided", { evidenceId: item.evidenceId, subjectName: item.subjectName, startSeq: item.effectiveFromSeq || item.seq || "", coverageMode: item.coverageMode || "", effectiveThroughSeq: item.effectiveThroughSeq || "", continuesBeyondBatch: item.continuesBeyondBatch === true, endTiming: item.endTiming || "", precheckPassed: item.precheckPassed });
    output.push(item);
  }
  this._v908LastVoiceAgePrefilterStats.outputCount = output.length;
  return output;
};

function graphV908VoiceAgeCandidateSetId(candidates) {
  var signatures = [];
  candidates = candidates || [];
  for (var i = 0; i < candidates.length; i++) {
    var c = candidates[i] || {};
    signatures.push([graphSafeString(c.evidenceId || "", 80), graphSafeString(c.evidenceHash || "", 120), graphSafeString(c.stateAction || "", 30)].join("|"));
  }
  signatures.sort();
  return "age_set_" + graphHash(signatures.join("#"));
}

function graphV908VoiceAgeAllAcceptedVerificationOk(apiResult) {
  var check = apiResult && apiResult.allAcceptedVerification;
  return !!(check && typeof check === "object" &&
    check.everyEvidenceTextGrounded === true &&
    check.everySubjectMatched === true &&
    check.everyStageSupported === true &&
    check.everyVoicePriorityChecked === true &&
    check.everyScopeChecked === true &&
    check.everyTemporaryRangeChecked === true &&
    check.everyActiveStateTransitionChecked === true);
}

// 单独年龄审计、年龄+图谱、别名+年龄+图谱统一复用同一份语义规则，避免三条流程裁决标准分叉。
function buildVoiceAgeAuditPolicyPromptV908() {
  return "1. evidenceText和每个cue.evidenceText必须能在当前文本或紧邻上文中逐字定位；说话主体必须与seq对应人物一致。\n" +
    "2. voice必须由原文明示声音所呈现的年龄感；低沉、沙哑、冰冷、柔和、响亮、虚弱或情绪词本身不足以证明年龄段。appearance必须由原文明示外表所呈现的年龄；普通长相、身材、动作、表情、美丑或气质描述本身不足以证明年龄段。\n" +
    "2a. ‘淡淡的道’、‘轻笑道’、‘冷冷地说’、‘低声道’、‘沉声道’、‘平静地说’只表示方式或情绪；若没有同段明确年龄感，必须reject，不能根据角色姓名或既有印象补成年龄证据。\n" +
    "3. 姓名和称号中的年龄词不能作证。剔除subjectName及称号后，剩余引文仍须明确支持年龄段；‘血刀老祖开口’不能仅凭‘老祖’判老年。紧邻上文只可引用其中真实存在的年龄原句，不能把‘延续此前年龄/没有变化’当成新证据。\n" +
    "4. 发声音龄冲突时严格按voice声音证据 > direct_age明确年龄 > appearance外貌年龄；模样少年但声音苍老取老年，模样老年但声音年轻取少年/青年。\n" +
    "5. persistent_update只允许明确适用于当前且自然、持续的真实声线/年龄状态；它可能永久改变已有角色的年龄段和自然发音人，因此跨年龄段更新必须由当前原文明确支持，不能根据普通语气、动作、情绪、姓名或称号推断。没有伪装信号时，不能仅因声音年龄与外貌不同就擅自改成one_shot。临时伪装、压嗓、变声、模仿不得写入自然角色卡。\n" +
    "6. one_shot默认只作用当前对白；start必须有明确持续信号，并核对effectiveFromSeq、coverageMode、effectiveThroughSeq、continuesBeyondBatch和linkedEndEvidenceText；end必须有明确恢复/停止信号并正确判断在本句前还是本句后结束。\n" +
    "7. 过去、回忆、闪回、主体不明、作用域不明、证据不充分时必须reject或verify。\n" +
    "8. 对sourceType=temporary_state_review的候选，还要核对activeStateId、hasDialogue、对白序号范围、continue/end/replace边界、原文证据和简短概括；不得因格式省略而默认结束旧状态。\n";
}

// 三条年龄审计流程也统一复用这一份稀疏返回协议，避免返回格式分叉。
function buildVoiceAgeSparseAuditSchemaPrompt(candidates, wrapInVoiceAgeAudit) {
  candidates = candidates || [];
  var candidateSetId = graphV908VoiceAgeCandidateSetId(candidates);
  var inner = '{"auditComplete":true,"candidateCount":' + candidates.length + ',"candidateSetId":"' + candidateSetId + '","allAccepted":true/false,"acceptedAll":["__ALL__"],"acceptedChanges":[{"evidenceId":"会跨年龄段换声的原ID","auditReason":"原文为什么足以支持永久改变自然年龄段和发音人"}],"allAcceptedVerification":{"everyEvidenceTextGrounded":true,"everySubjectMatched":true,"everyStageSupported":true,"everyVoicePriorityChecked":true,"everyScopeChecked":true,"everyTemporaryRangeChecked":true,"everyActiveStateTransitionChecked":true},"downgrade":[{"evidenceId":"原ID","auditReason":"中文理由"}],"reject":[],"verify":[]}';
  var schema = wrapInVoiceAgeAudit ? '{"voiceAgeAudit":' + inner + '}' : inner;
  return "候选总数=" + candidates.length + "，candidateSetId=" + candidateSetId + "。返回格式：" + schema + "。全部采纳时allAccepted=true且acceptedAll只能是[\"__ALL__\"]，三个异常数组为空；混合结果时allAccepted=false、acceptedAll=[]，只在downgrade/reject/verify列异常evidenceId与auditReason，未列出的普通候选视为采纳。acceptedChanges必须始终存在：只列出willChangeVoiceSegment=true且你明确同意永久跨段换声的候选，并逐条给出auditReason；同段保声、临时换声以及被列入异常数组的候选不得放入acceptedChanges。任何仍会被隐式采纳的跨段候选若未出现在acceptedChanges，本年龄模块视为结构不完整并重试。allAcceptedVerification七项在两种模式下都必须完整且全为true；不得返回未知或重复ID。";
}

function graphV908NormalizeVoiceAgeAuditResult(candidates, apiResult) {
  candidates = candidates || [];
  var expected = {};
  var ids = [];
  for (var i = 0; i < candidates.length; i++) {
    var candidateId = graphSafeString(candidates[i] && candidates[i].evidenceId || "", 80);
    if (!candidateId || expected[candidateId]) return { complete: false, decisions: [], reason: !candidateId ? "candidate_evidence_id_empty" : "candidate_evidence_id_duplicate" };
    expected[candidateId] = candidates[i];
    ids.push(candidateId);
  }
  var candidateSetId = graphV908VoiceAgeCandidateSetId(candidates);
  function fail(reason, extra) {
    return { complete: false, decisions: [], reason: reason, missingEvidenceIds: ids.slice(0), candidateCount: candidates.length, candidateSetId: candidateSetId, extra: extra || {} };
  }
  if (!apiResult || typeof apiResult !== "object") return fail("audit_result_not_object");
  if (apiResult.auditComplete !== true) return fail("audit_complete_missing_or_false");
  if (Number(apiResult.candidateCount) !== candidates.length) return fail("candidate_count_mismatch", { returned: apiResult.candidateCount });
  if (graphSafeString(apiResult.candidateSetId || "", 160) !== candidateSetId) return fail("candidate_set_id_mismatch", { returned: apiResult.candidateSetId || "" });
  if (typeof apiResult.allAccepted !== "boolean") return fail("all_accepted_boolean_missing");
  if (!Array.isArray(apiResult.acceptedAll) || !Array.isArray(apiResult.acceptedChanges) || !Array.isArray(apiResult.downgrade) || !Array.isArray(apiResult.reject) || !Array.isArray(apiResult.verify)) return fail("sparse_audit_arrays_missing");
  if (!graphV908VoiceAgeAllAcceptedVerificationOk(apiResult)) return fail("all_accepted_verification_incomplete");

  var out = [];
  var outIndex = {};
  for (var oi = 0; oi < candidates.length; oi++) {
    var baseId = ids[oi];
    outIndex[baseId] = out.length;
    out.push({ evidenceId: baseId, decision: "accept", auditReason: apiResult.allAccepted ? "模型审计明确返回全部采纳" : "未列入异常数组，按采纳处理", sparseImplicitAccept: true });
  }

  var acceptedChangeMap = {};
  for (var acceptedChangeIndex = 0; acceptedChangeIndex < apiResult.acceptedChanges.length; acceptedChangeIndex++) {
    var acceptedChangeRaw = apiResult.acceptedChanges[acceptedChangeIndex] || {};
    if (typeof acceptedChangeRaw !== "object") return fail("accepted_changes_item_not_object");
    var acceptedChangeId = graphSafeString(acceptedChangeRaw.evidenceId || acceptedChangeRaw.id || "", 80);
    if (!acceptedChangeId) return fail("accepted_changes_evidence_id_empty");
    if (!expected[acceptedChangeId]) return fail("accepted_changes_contains_unknown_evidence_id", { evidenceId: acceptedChangeId });
    if (acceptedChangeMap[acceptedChangeId]) return fail("accepted_changes_evidence_id_duplicate", { evidenceId: acceptedChangeId });
    if (expected[acceptedChangeId].willChangeVoiceSegment !== true || expected[acceptedChangeId].stateAction !== "persistent_update") return fail("accepted_changes_contains_non_cross_segment_candidate", { evidenceId: acceptedChangeId });
    var acceptedChangeReason = graphSafeString(acceptedChangeRaw.auditReason || acceptedChangeRaw.reason || "", 500);
    if (!acceptedChangeReason) return fail("accepted_changes_audit_reason_empty", { evidenceId: acceptedChangeId });
    acceptedChangeMap[acceptedChangeId] = { auditReason: acceptedChangeReason, details: graphV908Clone(acceptedChangeRaw, {}) };
  }

  if (apiResult.allAccepted === true) {
    if (apiResult.acceptedAll.length !== 1 || String(apiResult.acceptedAll[0]).toUpperCase() !== "__ALL__") return fail("all_accepted_marker_invalid");
    if (apiResult.downgrade.length || apiResult.reject.length || apiResult.verify.length) return fail("all_accepted_but_exception_array_not_empty");
  } else {
    if (apiResult.acceptedAll.length) return fail("all_accepted_false_but_accepted_all_not_empty");
  }

  var used = {};
  function applyExceptions(arr, decision) {
    for (var index = 0; index < arr.length; index++) {
      var raw = arr[index] || {};
      if (typeof raw !== "object") return decision + "_item_not_object";
      var id = graphSafeString(raw.evidenceId || raw.id || "", 80);
      if (!id) return decision + "_evidence_id_empty";
      if (!expected[id]) return decision + "_contains_unknown_evidence_id:" + id;
      if (used[id]) return "evidence_id_repeated_in_exception_arrays:" + id;
      var reason = graphSafeString(raw.auditReason || raw.reason || "", 500);
      if (!reason) return decision + "_audit_reason_empty:" + id;
      used[id] = decision;
      out[outIndex[id]] = { evidenceId: id, decision: decision, auditReason: reason, sparseImplicitAccept: false, auditDetails: graphV908Clone(raw, {}) };
    }
    return "";
  }
  var error = applyExceptions(apiResult.downgrade, "downgrade");
  if (error) return fail(error);
  error = applyExceptions(apiResult.reject, "reject");
  if (error) return fail(error);
  error = applyExceptions(apiResult.verify, "verify");
  if (error) return fail(error);
  var exceptionCount = apiResult.downgrade.length + apiResult.reject.length + apiResult.verify.length;
  if (!apiResult.allAccepted && !exceptionCount) return fail("all_accepted_false_but_no_exception");

  var explicitlyAcceptedChangeCount = 0;
  for (var candidateIndex = 0; candidateIndex < candidates.length; candidateIndex++) {
    var candidate = candidates[candidateIndex] || {};
    var candidateEvidenceId = ids[candidateIndex];
    var candidateDecision = out[outIndex[candidateEvidenceId]] || {};
    var explicitChange = acceptedChangeMap[candidateEvidenceId] || null;
    if (candidateDecision.decision !== "accept" && explicitChange) return fail("accepted_changes_candidate_also_listed_as_exception", { evidenceId: candidateEvidenceId, decision: candidateDecision.decision });
    if (candidate.willChangeVoiceSegment === true && candidate.stateAction === "persistent_update" && candidateDecision.decision === "accept") {
      if (!explicitChange) return fail("cross_segment_accept_missing_from_accepted_changes", { evidenceId: candidateEvidenceId, currentVoiceSegment: candidate.currentVoiceSegment || "", targetVoiceSegment: candidate.targetVoiceSegment || "" });
      candidateDecision.auditReason = explicitChange.auditReason;
      candidateDecision.sparseImplicitAccept = false;
      candidateDecision.explicitChangeAccepted = true;
      candidateDecision.acceptedChangeDetails = explicitChange.details;
      explicitlyAcceptedChangeCount++;
    }
  }
  return { complete: true, decisions: out, reason: apiResult.allAccepted ? "sparse_all_accepted" : "sparse_exception_arrays_complete", candidateCount: candidates.length, candidateSetId: candidateSetId, exceptionCount: exceptionCount, explicitlyAcceptedChangeCount: explicitlyAcceptedChangeCount };
}

CharacterManager.prototype.auditVoiceAgeEvidenceByAliasApi = function(candidates, currentText, previousText) {
  candidates = candidates || [];
  if (!candidates.length) return { success: true, decisions: [] };
  if (!ENABLE_VOICE_AGE_AUDIT) return { success: false, decisions: [], reason: "voice_age_audit_disabled" };
  var requestTimeout = ALIAS_ANALYZE_TIMEOUT;
  var maxRetryRound = Math.max(1, Math.ceil(CHARACTER_ANALYZE_RETRY_MAX / Math.max(1, parseInt(bingfa, 10) || 1)));
  var currentRound = 0;
  var manager = this;
  var auditPayload = [];
  for (var i = 0; i < candidates.length; i++) {
    var c = candidates[i] || {};
    auditPayload.push({ evidenceId: c.evidenceId, seq: c.seq, subjectName: c.subjectName, cues: c.cues || [], finalVoiceAgeStage: c.finalVoiceAgeStage || "", priorityCueType: c.priorityCueType || "", currentVoiceGender: c.currentVoiceGender || "", currentVoiceAge: c.currentVoiceAge || "", currentVoiceTag: c.currentVoiceTag || "", currentVoiceSegment: c.currentVoiceSegment || "", targetVoiceSegment: c.targetVoiceSegment || "", willChangeVoiceSegment: c.willChangeVoiceSegment === true, applyScope: c.applyScope, stateAction: c.stateAction, effectiveFromSeq: c.effectiveFromSeq || "", coverageMode: c.coverageMode || "", effectiveThroughSeq: c.effectiveThroughSeq || "", continuesBeyondBatch: c.continuesBeyondBatch === true, linkedEndEvidenceText: c.linkedEndEvidenceText || "", endTiming: c.endTiming || "", temporalScope: c.temporalScope, evidenceText: c.evidenceText, decisionBasis: c.decisionBasis || "", reason: c.reason || "", confidence: c.confidence || 0, anchorRegion: c.anchorRegion || "", sourceType: c.sourceType || "voice_age_evidence", activeStateId: c.activeStateId || "", hasDialogue: c.hasDialogue === true, dialogueSeqs: c.dialogueSeqs || [], temporaryDialogueSeqs: c.temporaryDialogueSeqs || [], replacementDialogueSeqs: c.replacementDialogueSeqs || [], naturalDialogueSeqs: c.naturalDialogueSeqs || [], endBoundarySeq: c.endBoundarySeq || "", reviewDecision: c.reviewDecision || "", summary: c.summary || "" });
  }
  var candidateSetId = graphV908VoiceAgeCandidateSetId(candidates);
  // actionCounts属于本次API请求日志，必须在当前函数内统计，避免独立审计触发未定义变量。
  var actionCounts = { persistent_update: 0, start: 0, end: 0, one_shot: 0, continue: 0, replace: 0, activeStateTransition: 0 };
  for (var actionCountIndex = 0; actionCountIndex < candidates.length; actionCountIndex++) {
    var actionName = candidates[actionCountIndex] && candidates[actionCountIndex].stateAction || "";
    if (actionCounts.hasOwnProperty(actionName)) actionCounts[actionName]++;
    if (candidates[actionCountIndex] && candidates[actionCountIndex].sourceType === "temporary_state_review") actionCounts.activeStateTransition++;
  }
  var prompt = "你是小说朗读的发声音龄证据审计AI。你只审计给定候选，禁止抽取新证据，禁止使用角色姓名印象、称号、世界知识或原著知识。\n\n" +
    "【硬性规则】\n" +
    buildVoiceAgeAuditPolicyPromptV908() + "\n" +
    "【紧邻上文】\n" + String(previousText || "") + "\n\n【当前待分析文本】\n" + String(currentText || "") + "\n\n" +
    "【待审计候选】\n" + JSON.stringify(auditPayload) + "\n\n" +
    buildVoiceAgeSparseAuditSchemaPrompt(candidates, false) + "只输出严格JSON。";
  graphRemoteLog("voice_age_audit_request", { evidenceCount: candidates.length, sourceExtractedCount: candidates.length, votedEvidenceCount: candidates.length, candidateCount: candidates.length, candidateSetId: candidateSetId, evidenceIds: candidates.map(function(x){ return x.evidenceId; }), actionCounts: actionCounts, countLimitApplied: false, promptHead: graphSafeString(prompt, 2800) });

  function sleep(ms) { var start = Date.now(); while (Date.now() - start < ms) {} }
  function buildRequest(apiConfig) {
    var requestData = {
      model: apiConfig.model,
      messages: [
        { role: "system", content: "仅输出JSON。逐条审计发声音龄原文、主体、声音优先级、当前时间、作用域及临时状态边界；使用candidateCount/candidateSetId、acceptedChanges和稀疏异常数组返回。willChangeVoiceSegment=true的永久跨段更新只有明确写入acceptedChanges才可采纳；任何语义不确定项必须进入reject/downgrade/verify。" },
        { role: "user", content: prompt }
      ],
      temperature: 0.1
    };
    var headers = { "Content-Type": "application/json", "Authorization": "Bearer " + apiConfig.key, "Connection": "keep-alive", "Timeout": requestTimeout.toString() };
    if (ENABLE_MODEL_RAW_REMOTE_LOG) graphRemoteLog("voice_age_audit_raw_request", { endpoint: graphSafeString(apiConfig.endpoint || "", 200), model: graphSafeString(apiConfig.model || "", 80), requestData: graphSafeString(JSON.stringify(requestData), MODEL_RAW_REMOTE_LOG_MAX_LEN) });
    return { endpoint: apiConfig.endpoint, data: requestData, headers: headers };
  }
  function parseResponse(response) {
    var responseBody = String(response.body().string() || "{}");
    graphRemoteLog("voice_age_audit_raw_response", { responseBody: graphSafeString(responseBody, MODEL_RAW_REMOTE_LOG_MAX_LEN) });
    var outer = JSON.parse(responseBody);
    if (!outer.choices || !outer.choices[0] || !outer.choices[0].message || !outer.choices[0].message.content) throw new Error("发声音龄审计缺少choices[0].message.content");
    var content = String(outer.choices[0].message.content || "").replace(/```json|```/g, "").trim();
    var match = content.match(/\{[\s\S]*\}/);
    if (!match) throw new Error("发声音龄审计返回非JSON");
    var normalized = graphV908NormalizeVoiceAgeAuditResult(candidates, JSON.parse(match[0]));
    if (!normalized.complete) {
      graphRemoteLog("voice_age_audit_structure_incomplete", { candidateCount: candidates.length, candidateSetId: candidateSetId, reason: normalized.reason || "unknown", missingEvidenceIds: normalized.missingEvidenceIds || [], standaloneFallbackWillRetry: true });
      throw new Error("发声音龄审计结构不完整:" + normalized.reason);
    }
    return normalized;
  }

  while (currentRound < maxRetryRound) {
    currentRound++;
    var concurrentResult = concurrentApiRequest("relationAudit", buildRequest, parseResponse, null, requestTimeout);
    if (concurrentResult.success) {
      var resultList = [];
      if (concurrentResult.isMultiResult) {
        for (var ri = 0; ri < concurrentResult.data.length; ri++) if (concurrentResult.data[ri] && concurrentResult.data[ri].data) resultList.push(concurrentResult.data[ri].data);
      } else resultList.push(concurrentResult.data);
      var finalDecisions = [];
      for (var ci = 0; ci < candidates.length; ci++) {
        var candidateId = candidates[ci].evidenceId;
        var votes = { accept: 0, reject: 0, verify: 0, downgrade: 0 };
        var reasons = [];
        var bestAccept = null;
        for (var rli = 0; rli < resultList.length; rli++) {
          var decisions = resultList[rli].decisions || [];
          for (var di = 0; di < decisions.length; di++) {
            if (decisions[di].evidenceId !== candidateId) continue;
            var d = decisions[di];
            votes[d.decision] = (votes[d.decision] || 0) + 1;
            if (d.auditReason) reasons.push(d.auditReason);
            if (d.decision === "accept") bestAccept = d;
          }
        }
        var accepted = votes.accept > resultList.length / 2;
        if (accepted && bestAccept) {
          bestAccept.voteSummary = votes;
          bestAccept.auditReason = graphSafeString(reasons.join(" | ") || bestAccept.auditReason, 500);
          finalDecisions.push(bestAccept);
        } else {
          var nonAcceptDecision = votes.verify > votes.reject && votes.verify >= votes.downgrade ? "verify" : (votes.downgrade > votes.reject ? "downgrade" : "reject");
          finalDecisions.push({ evidenceId: candidateId, decision: nonAcceptDecision, auditReason: graphSafeString("多结果未形成accept严格多数；" + reasons.join(" | "), 500), voteSummary: votes });
        }
      }
      var finalDecisionCounts = { accept: 0, downgrade: 0, reject: 0, verify: 0 };
      for (var finalDecisionIndex = 0; finalDecisionIndex < finalDecisions.length; finalDecisionIndex++) {
        var finalDecisionName = finalDecisions[finalDecisionIndex] && finalDecisions[finalDecisionIndex].decision || "reject";
        finalDecisionCounts[finalDecisionName] = Number(finalDecisionCounts[finalDecisionName] || 0) + 1;
      }
      graphRemoteLog("voice_age_audit_result", { success: true, retryCount: currentRound, resultCount: resultList.length, candidateCount: candidates.length, candidateSetId: candidateSetId, candidateSetIdMatched: true, allAccepted: finalDecisionCounts.accept === candidates.length, implicitAcceptedCount: finalDecisionCounts.accept, decisionCounts: finalDecisionCounts, decisions: finalDecisions });
      return { success: true, decisions: finalDecisions };
    }
    graphRemoteLog("voice_age_audit_retry", { retryCount: currentRound, maxRetryRound: maxRetryRound, errors: concurrentResult.errors || [], reason: "api_failed_or_audit_structure_incomplete" });
    if (currentRound < maxRetryRound) sleep(250);
  }
  graphRemoteLog("voice_age_audit_result", { success: false, evidenceCount: candidates.length, reason: "audit_retry_exhausted", action: "all_voice_age_evidence_rejected_without_affecting_relation_pipeline" });
  return { success: false, decisions: [], reason: "audit_retry_exhausted" };
};

// ===================== 别名/发声音龄/图谱证据合并审计与精确降级 =====================

function graphV908HasOwn(obj, key) {
  return !!(obj && Object.prototype.hasOwnProperty.call(obj, key));
}

function graphV908NotApplicableModule(name) {
  return { name: name, required: false, complete: true, notApplicable: true, reason: "本批没有该模块待审内容" };
}

function graphV908NormalizeAliasModule(apiResult, required) {
  if (!required) return graphV908NotApplicableModule("alias");
  var source = apiResult && apiResult.aliasCheck && typeof apiResult.aliasCheck === "object" ? apiResult.aliasCheck : apiResult;
  if (!source || typeof source !== "object") return { name: "alias", required: true, complete: false, reason: "alias_result_not_object" };
  if (!graphV908HasOwn(source, "isAlias") || !graphV908HasOwn(source, "mainName") || !graphV908HasOwn(source, "reason")) {
    return { name: "alias", required: true, complete: false, reason: "alias_required_fields_missing" };
  }
  if (typeof source.isAlias !== "boolean") return { name: "alias", required: true, complete: false, reason: "alias_isAlias_not_boolean" };
  if (source.mainName != null && typeof source.mainName !== "string") return { name: "alias", required: true, complete: false, reason: "alias_mainName_type_invalid" };
  if (source.reason != null && typeof source.reason !== "string") return { name: "alias", required: true, complete: false, reason: "alias_reason_type_invalid" };
  if (source.isAlias === true && !graphNormalizeName(source.mainName || "")) return { name: "alias", required: true, complete: false, reason: "alias_true_but_mainName_empty" };
  if (source.isAlias === true && !graphSafeString(source.reason || "", 1200).trim()) return { name: "alias", required: true, complete: false, reason: "alias_true_but_reason_empty" };
  var result = {
    isAlias: source.isAlias,
    mainName: source.mainName == null ? null : graphSafeString(source.mainName, 120),
    reason: source.reason == null ? null : graphSafeString(source.reason, 1200)
  };
  if (graphV908HasOwn(source, "confidence")) result.confidence = Number(source.confidence || 0);
  result.graphAuditSuggestions = Array.isArray(source.graphAuditSuggestions) ? source.graphAuditSuggestions : (Array.isArray(apiResult && apiResult.graphAuditSuggestions) ? apiResult.graphAuditSuggestions : []);
  // isAlias=false或证据全部拒收属于完整业务结论，不得误判为“不完整”。
  return { name: "alias", required: true, complete: true, notApplicable: false, reason: "alias_fields_complete", result: result };
}

function graphV908NormalizeVoiceAgeModule(candidates, apiResult) {
  candidates = candidates || [];
  if (!candidates.length) return graphV908NotApplicableModule("voiceAge");
  var source = apiResult && apiResult.voiceAgeAudit && typeof apiResult.voiceAgeAudit === "object" ? apiResult.voiceAgeAudit : (apiResult && apiResult.ageAudit && typeof apiResult.ageAudit === "object" ? apiResult.ageAudit : null);
  var normalized = graphV908NormalizeVoiceAgeAuditResult(candidates, source);
  if (!normalized.complete) return { name: "voiceAge", required: true, complete: false, reason: normalized.reason || "voice_age_audit_incomplete", missingEvidenceIds: normalized.missingEvidenceIds || [] };
  return { name: "voiceAge", required: true, complete: true, notApplicable: false, reason: normalized.reason || "voice_age_audit_complete", decisions: normalized.decisions || [] };
}

function graphV908NormalizeGraphModule(relations, apiResult) {
  relations = relations || [];
  if (!relations.length) return graphV908NotApplicableModule("graph");
  var source = apiResult && apiResult.graphEvidenceAudit && typeof apiResult.graphEvidenceAudit === "object" ? apiResult.graphEvidenceAudit : (apiResult && apiResult.graphAudit && typeof apiResult.graphAudit === "object" ? apiResult.graphAudit : apiResult);
  var normalized = graphNormalizeSparseRelationAuditResult(relations, source, "combined_graph_audit");
  if (!normalized.complete) return { name: "graph", required: true, complete: false, reason: normalized.reason || "graph_audit_incomplete", missingRelationIds: normalized.missingRelationIds || [] };
  return { name: "graph", required: true, complete: true, notApplicable: false, reason: normalized.reason || "graph_audit_complete", audits: normalized.audits || [] };
}

function graphV908BuildCombinedAuditStatus(requiredAlias, candidates, relations, apiResult) {
  var modules = {
    alias: graphV908NormalizeAliasModule(apiResult || {}, !!requiredAlias),
    voiceAge: graphV908NormalizeVoiceAgeModule(candidates || [], apiResult || {}),
    graph: graphV908NormalizeGraphModule(relations || [], apiResult || {})
  };
  var names = ["alias", "voiceAge", "graph"];
  var incomplete = [];
  var requiredCount = 0;
  for (var i = 0; i < names.length; i++) {
    var module = modules[names[i]];
    if (module.required) requiredCount++;
    if (module.required && !module.complete) incomplete.push(names[i]);
  }
  return {
    modules: modules,
    requiredCount: requiredCount,
    incompleteNames: incomplete,
    incompleteCount: incomplete.length,
    action: incomplete.length === 0 ? "accept_all_complete" : (incomplete.length === 1 ? "preserve_complete_and_run_single_fallback" : "reject_bundle_and_retry_combined")
  };
}

function graphV908CombinedStatusLogPayload(flow, status, round, newName) {
  status = status || graphV908BuildCombinedAuditStatus(false, [], [], {});
  function item(module) {
    module = module || {};
    return { required: !!module.required, complete: !!module.complete, notApplicable: !!module.notApplicable, reason: module.reason || "" };
  }
  return {
    flow: flow || "",
    round: Number(round || 0),
    newName: graphNormalizeName(newName || ""),
    requiredCount: Number(status.requiredCount || 0),
    incompleteCount: Number(status.incompleteCount || 0),
    incompleteModules: (status.incompleteNames || []).slice(0),
    action: status.action || "",
    modules: { alias: item(status.modules && status.modules.alias), voiceAge: item(status.modules && status.modules.voiceAge), graph: item(status.modules && status.modules.graph) }
  };
}

function graphV908SelectBestCombinedResult(concurrentResult, requiredAlias, candidates, relations) {
  var rawList = [];
  if (concurrentResult && concurrentResult.isMultiResult && Array.isArray(concurrentResult.data)) {
    for (var i = 0; i < concurrentResult.data.length; i++) {
      var entry = concurrentResult.data[i];
      var raw = entry && graphV908HasOwn(entry, "data") ? entry.data : entry;
      if (raw && typeof raw === "object") rawList.push(raw);
    }
  } else if (concurrentResult && concurrentResult.data && typeof concurrentResult.data === "object") {
    rawList.push(concurrentResult.data);
  }
  if (!rawList.length) rawList.push({});
  var bestRaw = rawList[0];
  var bestStatus = graphV908BuildCombinedAuditStatus(requiredAlias, candidates, relations, bestRaw);
  for (var j = 1; j < rawList.length; j++) {
    var status = graphV908BuildCombinedAuditStatus(requiredAlias, candidates, relations, rawList[j]);
    if (status.incompleteCount < bestStatus.incompleteCount) {
      bestRaw = rawList[j];
      bestStatus = status;
    }
  }
  return { raw: bestRaw, status: bestStatus, resultCount: rawList.length };
}

function graphV908CombinedMaxRetryRound() {
  var configured = parseInt(COMBINED_EVIDENCE_AUDIT_RETRY_MAX, 10) || 0;
  if (configured > 0) return configured;
  return Math.max(1, Math.ceil(CHARACTER_ANALYZE_RETRY_MAX / Math.max(1, parseInt(bingfa, 10) || 1)));
}

CharacterManager.prototype.traceCombinedAuditCommitV908 = function(stage, data) {
  if (!this._v908CombinedAuditCommitTrace) this._v908CombinedAuditCommitTrace = [];
  var entry = { stage: stage || "", at: graphNowIso(), data: data || {} };
  this._v908CombinedAuditCommitTrace.push(entry);
  if (this._v908CombinedAuditCommitTrace.length > 80) this._v908CombinedAuditCommitTrace.splice(0, this._v908CombinedAuditCommitTrace.length - 80);
  graphRemoteLog("combined_audit_commit", entry);
  return entry;
};

CharacterManager.prototype.setPendingVoiceAgeEvidenceV908 = function(checked, currentText, previousText, expectedSeqs, batchResult) {
  checked = Array.isArray(checked) ? checked : [];
  if (!checked.length) return null;
  var candidates = [];
  var ids = [];
  for (var i = 0; i < checked.length; i++) {
    if (checked[i] && checked[i].precheckPassed) candidates.push(checked[i]);
    if (checked[i] && checked[i].evidenceId) ids.push(checked[i].evidenceId);
  }
  var batchKey = "age_batch_" + graphHash([graphCurrentChapterId(), graphSafeString(currentText || "", 5000), ids.join("|")].join("#"));
  var old = this.pendingVoiceAgeEvidence;
  if (old && !old.consumed && old.batchKey !== batchKey) {
    graphRemoteLog("pending_voice_age_rejected", { batchKey: old.batchKey || "", reason: "unexpected_pending_overwrite", evidenceCount: old.candidates ? old.candidates.length : 0 });
  }
  this.pendingVoiceAgeEvidence = {
    checked: checked,
    candidates: candidates,
    // 当前批和紧邻上文已经由源头控制长度；合并审计不在这里再次截断。
    currentText: String(currentText || ""),
    previousText: String(previousText || ""),
    expectedSeqs: (expectedSeqs || []).slice(0),
    batchResult: batchResult || null,
    batchKey: batchKey,
    chapterId: graphCurrentChapterId(),
    consumed: false,
    auditBuffered: false,
    createdAt: graphNowIso()
  };
  graphRemoteLog("pending_voice_age_stored", { batchKey: batchKey, chapterId: graphCurrentChapterId(), checkedCount: checked.length, candidateCount: candidates.length, evidenceIds: ids.slice(0, 80), policy: "wait_for_alias_age_graph_or_age_graph_combined_audit" });
  return this.pendingVoiceAgeEvidence;
};

CharacterManager.prototype.getPendingVoiceAgeEvidenceForCombinedAuditV908 = function() {
  var pending = this.pendingVoiceAgeEvidence;
  if (!pending || pending.consumed || pending.auditBuffered) return [];
  return pending.candidates || [];
};

function graphV908BuildCombinedSharedSourceBlock(previousText, currentText) {
  return "【共享紧邻上文】\n" + String(previousText || "") + "\n\n" +
    "【共享当前批文本】\n" + String(currentText || "") + "\n";
}

CharacterManager.prototype.buildCombinedVoiceAgeAuditBlockV908 = function(candidates, options) {
  candidates = candidates || [];
  options = options || {};
  if (!candidates.length) return "";
  var pending = this.pendingVoiceAgeEvidence || {};
  var auditPayload = [];
  for (var i = 0; i < candidates.length; i++) {
    var c = candidates[i] || {};
    auditPayload.push({
      evidenceId: c.evidenceId,
      seq: c.seq,
      subjectName: c.subjectName,
      evidenceLevel: c.evidenceLevel || "L0",
      evidenceType: c.evidenceType || "",
      evidenceSubtype: c.evidenceSubtype || "",
      supersedesPrior: c.supersedesPrior === true,
      priorEvidenceId: c.priorEvidenceId || "",
      cues: c.cues || [],
      finalVoiceAgeStage: c.finalVoiceAgeStage || "",
      priorityCueType: c.priorityCueType || "",
      currentVoiceGender: c.currentVoiceGender || "",
      currentVoiceAge: c.currentVoiceAge || "",
      currentVoiceTag: c.currentVoiceTag || "",
      currentVoiceSegment: c.currentVoiceSegment || "",
      targetVoiceSegment: c.targetVoiceSegment || "",
      willChangeVoiceSegment: c.willChangeVoiceSegment === true,
      applyScope: c.applyScope,
      stateAction: c.stateAction,
      effectiveFromSeq: c.effectiveFromSeq || "",
      coverageMode: c.coverageMode || "",
      effectiveThroughSeq: c.effectiveThroughSeq || "",
      continuesBeyondBatch: c.continuesBeyondBatch === true,
      linkedEndEvidenceText: c.linkedEndEvidenceText || "",
      endTiming: c.endTiming || "",
      temporalScope: c.temporalScope,
      evidenceText: c.evidenceText,
      reason: c.reason || "",
      confidence: c.confidence || 0,
      anchorRegion: c.anchorRegion || "",
      sourceType: c.sourceType || "voice_age_evidence",
      activeStateId: c.activeStateId || "",
      hasDialogue: c.hasDialogue === true,
      dialogueSeqs: c.dialogueSeqs || [],
      temporaryDialogueSeqs: c.temporaryDialogueSeqs || [],
      replacementDialogueSeqs: c.replacementDialogueSeqs || [],
      naturalDialogueSeqs: c.naturalDialogueSeqs || [],
      endBoundarySeq: c.endBoundarySeq || "",
      reviewDecision: c.reviewDecision || "",
      summary: c.summary || ""
    });
  }
  var candidateSetId = graphV908VoiceAgeCandidateSetId(candidates);
  return "【当前批次发声音龄证据审计任务】\n" +
    "你只审计给定候选，禁止抽取新证据，禁止使用姓名印象、称号、世界知识或原著知识。\n" +
    buildVoiceAgeAuditPolicyPromptV908() +
    (options.omitSourceText ? "【审计原文】\n使用合并请求顶部的【共享紧邻上文】和【共享当前批文本】。\n" : ("【紧邻上文】\n" + String(pending.previousText || "") + "\n【当前文本】\n" + String(pending.currentText || "") + "\n")) +
    "【候选】\n" + JSON.stringify(auditPayload) + "\n" +
    buildVoiceAgeSparseAuditSchemaPrompt(candidates, true);
};

CharacterManager.prototype.runStandaloneAliasCheckV908 = function(prompt, context) {
  context = context || {};
  if (!prompt) return { success: false, reason: "standalone_alias_prompt_empty" };
  var requestTimeout = ALIAS_ANALYZE_TIMEOUT;
  var maxRetryRound = graphV908CombinedMaxRetryRound();
  var round = 0;
  var newName = graphNormalizeName(context.newName || "");
  graphRemoteLog("standalone_alias_fallback_start", { newName: newName, maxRetryRound: maxRetryRound, reason: "only_alias_module_incomplete" });
  function sleep(ms) { var start = Date.now(); while (Date.now() - start < ms) {} }
  function buildRequest(apiConfig) {
    var requestData = { model: apiConfig.model, messages: [{ role: "system", content: "严格遵守格式要求，仅输出别名检验JSON。" }, { role: "user", content: prompt }], temperature: 0.1 };
    return { endpoint: apiConfig.endpoint, data: requestData, headers: { "Content-Type": "application/json", "Authorization": "Bearer " + apiConfig.key, "Connection": "keep-alive", "Timeout": requestTimeout.toString() } };
  }
  function parseResponse(response) {
    var body = String(response.body().string() || "{}");
    var outer = JSON.parse(body);
    if (!outer.choices || !outer.choices[0] || !outer.choices[0].message || !outer.choices[0].message.content) throw new Error("单独别名检验缺少choices[0].message.content");
    var content = String(outer.choices[0].message.content || "").replace(/```json|```/g, "").trim();
    var match = content.match(/\{[\s\S]*\}/);
    if (!match) throw new Error("单独别名检验返回非JSON");
    var raw = JSON.parse(match[0]);
    var normalized = graphV908NormalizeAliasModule(raw, true);
    if (!normalized.complete) throw new Error("单独别名检验结构不完整:" + normalized.reason);
    return raw;
  }
  while (round < maxRetryRound) {
    round++;
    var concurrentResult = concurrentApiRequest("aliasAnalyze", buildRequest, parseResponse, null, requestTimeout);
    if (concurrentResult.success) {
      var raw = concurrentResult.isMultiResult ? voteAliasAnalyzeResult(concurrentResult.data || []) : concurrentResult.data;
      var normalized = graphV908NormalizeAliasModule(raw, true);
      if (normalized.complete) {
        graphRemoteLog("standalone_alias_fallback_result", { success: true, newName: newName, retryCount: round, isAlias: !!normalized.result.isAlias, mainName: graphNormalizeName(normalized.result.mainName || "") });
        return { success: true, module: normalized };
      }
    }
    if (round < maxRetryRound) sleep(250);
  }
  graphRemoteLog("standalone_alias_fallback_result", { success: false, newName: newName, retryCount: round, reason: "standalone_alias_retry_exhausted" });
  return { success: false, reason: "standalone_alias_retry_exhausted" };
};

CharacterManager.prototype.resolveCombinedAuditFallbacksV908 = function(options) {
  options = options || {};
  var status = options.status || graphV908BuildCombinedAuditStatus(!!(options.aliasContext && options.aliasContext.newName), options.voiceAgeCandidates || [], options.relations || [], options.raw || {});
  var modules = { alias: status.modules.alias, voiceAge: status.modules.voiceAge, graph: status.modules.graph };
  var flow = options.flow || "";
  if (status.incompleteCount >= 2) {
    // 两项及以上不完整：整包拒收，连同表面完整的一方也不得提交；只允许重启合并流程。
    if (modules.alias.required) modules.alias = { name: "alias", required: true, complete: false, reason: "combined_bundle_rejected_multiple_incomplete" };
    if (modules.voiceAge.required) modules.voiceAge = { name: "voiceAge", required: true, complete: false, reason: "combined_bundle_rejected_multiple_incomplete" };
    if (modules.graph.required) modules.graph = { name: "graph", required: true, complete: false, reason: "combined_bundle_rejected_multiple_incomplete" };
    return { alias: modules.alias, voiceAge: modules.voiceAge, graph: modules.graph, rejectedWholeBundle: true };
  }
  if (status.incompleteCount === 1) {
    var missing = status.incompleteNames[0];
    var preserved = [];
    if (modules.alias.complete && modules.alias.required) preserved.push("alias");
    if (modules.voiceAge.complete && modules.voiceAge.required) preserved.push("voiceAge");
    if (modules.graph.complete && modules.graph.required) preserved.push("graph");
    graphRemoteLog("combined_audit_partial_preserved", { flow: flow, incompleteModule: missing, preservedModules: preserved, policy: "only_incomplete_module_runs_standalone" });
    graphRemoteLog("combined_audit_single_fallback", { flow: flow, module: missing, preservedModules: preserved });
    if (missing === "alias") {
      var aliasFallback = this.runStandaloneAliasCheckV908(options.aliasStandalonePrompt || "", options.aliasContext || {});
      modules.alias = aliasFallback && aliasFallback.success ? aliasFallback.module : { name: "alias", required: true, complete: false, reason: aliasFallback && aliasFallback.reason || "standalone_alias_failed" };
    } else if (missing === "voiceAge") {
      var ageFallback = this.auditVoiceAgeEvidenceByAliasApi ? this.auditVoiceAgeEvidenceByAliasApi(options.voiceAgeCandidates || [], this.pendingVoiceAgeEvidence && this.pendingVoiceAgeEvidence.currentText || "", this.pendingVoiceAgeEvidence && this.pendingVoiceAgeEvidence.previousText || "") : { success: false, decisions: [] };
      var ageNormalized = ageFallback && ageFallback.success && Array.isArray(ageFallback.decisions) && ageFallback.decisions.length === (options.voiceAgeCandidates || []).length ? { complete: true, decisions: ageFallback.decisions || [], reason: "standalone_sparse_voice_age_complete" } : { complete: false, reason: ageFallback && ageFallback.reason || "standalone_voice_age_failed" };
      modules.voiceAge = ageNormalized.complete ? { name: "voiceAge", required: true, complete: true, reason: "standalone_voice_age_complete", auditSource: "standalone_voice_age_audit", decisions: ageNormalized.decisions || [] } : { name: "voiceAge", required: true, complete: false, reason: ageNormalized.reason || "standalone_voice_age_failed", auditSource: "standalone_voice_age_audit" };
    } else if (missing === "graph") {
      var graphFallback = this.auditNameSemanticRelationsByAliasApi ? this.auditNameSemanticRelationsByAliasApi(options.relations || [], options.chapterText || "") : { success: false, audits: [] };
      modules.graph = graphFallback && graphFallback.success && graphIsRelationAuditComplete(options.relations || [], graphFallback.audits || []) ? { name: "graph", required: true, complete: true, reason: "standalone_graph_complete", audits: graphFallback.audits || [] } : { name: "graph", required: true, complete: false, reason: graphFallback && graphFallback.reason || "standalone_graph_failed" };
    }
  }
  return { alias: modules.alias, voiceAge: modules.voiceAge, graph: modules.graph, rejectedWholeBundle: false };
};

CharacterManager.prototype.runCombinedAuditRequestV908 = function(flow, prompt, requiredAlias, candidates, relations) {
  candidates = candidates || [];
  relations = relations || [];
  var requestTimeout = ALIAS_ANALYZE_TIMEOUT;
  var maxRetryRound = graphV908CombinedMaxRetryRound();
  var currentRound = 0;
  var finalRaw = null;
  var finalStatus = null;
  function sleep(ms) { var start = Date.now(); while (Date.now() - start < ms) {} }
  function buildRequest(apiConfig) {
    var requestData = { model: apiConfig.model, messages: [{ role: "system", content: "仅输出JSON；分别完成输入中适用的发声音龄和图谱证据审计模块，不得互相省略。" }, { role: "user", content: prompt }], temperature: 0.1 };
    graphRemoteLog("combined_audit_raw_request", { flow: flow, endpoint: graphSafeString(apiConfig.endpoint || "", 200), model: graphSafeString(apiConfig.model || "", 80), requestData: graphSafeString(JSON.stringify(requestData), MODEL_RAW_REMOTE_LOG_MAX_LEN) });
    return { endpoint: apiConfig.endpoint, data: requestData, headers: { "Content-Type": "application/json", "Authorization": "Bearer " + apiConfig.key, "Connection": "keep-alive", "Timeout": requestTimeout.toString() } };
  }
  function parseResponse(response) {
    var body = String(response.body().string() || "{}");
    graphRemoteLog("combined_audit_raw_response", { flow: flow, responseBody: graphSafeString(body, MODEL_RAW_REMOTE_LOG_MAX_LEN) });
    var outer = JSON.parse(body);
    if (!outer.choices || !outer.choices[0] || !outer.choices[0].message || !outer.choices[0].message.content) throw new Error("合并审计缺少choices[0].message.content");
    var content = String(outer.choices[0].message.content || "").replace(/```json|```/g, "").trim();
    var match = content.match(/\{[\s\S]*\}/);
    if (!match) throw new Error("合并审计返回非JSON");
    return JSON.parse(match[0]);
  }
  while (currentRound < maxRetryRound && !finalRaw) {
    currentRound++;
    graphRemoteLog("combined_audit_request", { flow: flow, round: currentRound, maxRetryRound: maxRetryRound, required: { alias: !!requiredAlias, voiceAge: candidates.length > 0, graph: relations.length > 0 } });
    var concurrentResult = concurrentApiRequest("relationAudit", buildRequest, parseResponse, null, requestTimeout);
    if (concurrentResult.success) {
      var selected = graphV908SelectBestCombinedResult(concurrentResult, requiredAlias, candidates, relations);
      finalStatus = selected.status;
      graphRemoteLog("combined_audit_module_status", graphV908CombinedStatusLogPayload(flow, finalStatus, currentRound, ""));
      if (finalStatus.incompleteCount >= 2) {
        graphRemoteLog("combined_audit_multi_incomplete_rejected", graphV908CombinedStatusLogPayload(flow, finalStatus, currentRound, ""));
        if (currentRound < maxRetryRound) {
          graphRemoteLog("combined_audit_bundle_retry", { flow: flow, round: currentRound, nextRound: currentRound + 1, incompleteModules: finalStatus.incompleteNames.slice(0) });
          sleep(250);
        }
      } else finalRaw = selected.raw || {};
    } else if (currentRound < maxRetryRound) {
      graphRemoteLog("combined_audit_bundle_retry", { flow: flow, round: currentRound, nextRound: currentRound + 1, reason: "api_or_outer_json_failed", errors: concurrentResult.errors || [] });
      sleep(250);
    }
  }
  if (!finalStatus) finalStatus = graphV908BuildCombinedAuditStatus(requiredAlias, candidates, relations, finalRaw || {});
  if (!finalRaw && finalStatus.incompleteCount >= 2) graphRemoteLog("combined_audit_retry_exhausted", graphV908CombinedStatusLogPayload(flow, finalStatus, currentRound, ""));
  return { raw: finalRaw || {}, status: finalStatus, success: !!finalRaw, retryCount: currentRound };
};

CharacterManager.prototype.bufferPendingVoiceAgeAuditV908 = function(module, source) {
  var pending = this.pendingVoiceAgeEvidence;
  if (!pending || pending.consumed) return { skipped: true };
  pending.auditBuffered = true;
  this._v908BufferedVoiceAgeAudit = { module: module || graphV908NotApplicableModule("voiceAge"), batchKey: pending.batchKey || "", source: source || "", bufferedAt: graphNowIso() };
  return this._v908BufferedVoiceAgeAudit;
};

CharacterManager.prototype.bufferPendingGraphAuditV908 = function(module, source) {
  var pending = this.pendingNameSemanticRelations;
  if (!pending || pending.consumed || !pending.relations || !pending.relations.length) return { skipped: true };
  pending.auditBuffered = true;
  this._v908BufferedGraphAudit = { module: module || { name: "graph", required: true, complete: false, reason: "graph_module_missing" }, batchKey: pending.batchKey || "", source: source || "", bufferedAt: graphNowIso() };
  return this._v908BufferedGraphAudit;
};

CharacterManager.prototype.commitPendingGraphAuditV908 = function(module, chapterText, source) {
  var pending = this.pendingNameSemanticRelations;
  if (!pending || pending.consumed || !pending.relations || !pending.relations.length) return { skipped: true };
  module = module || { complete: false, reason: "graph_module_missing" };
  pending.consumed = true;
  pending.auditBuffered = false;
  pending.consumedBy = source || "combined_audit";
  pending.consumedAt = graphNowIso();
  var summary = { positive: 0, negative: 0, audit: 0, rejected: pending.relations.length, auditFailed: true };
  if (module.complete && Array.isArray(module.audits) && graphIsRelationAuditComplete(pending.relations, module.audits)) {
    summary = this.applyAuditedNameSemanticRelations ? this.applyAuditedNameSemanticRelations(pending.relations, module.audits, chapterText || pending.chapterText || "") : summary;
    summary.auditFailed = false;
  }
  this.traceCombinedAuditCommitV908("graph", { source: source || "", complete: !!module.complete, reason: module.reason || "", relationCount: pending.relations.length, summary: summary });
  try { delete this._v908BufferedGraphAudit; } catch(e0) { this._v908BufferedGraphAudit = null; }
  return summary;
};

// 稀疏审计日志分“审计提交”和“对白实际应用”两阶段累计，避免尚未调用assignVoice时就把同段保持/重分配写成0并误当最终结果。
function graphV908NewVoiceAgeSparseApplySummary(batchKey) {
  return {
    batchKey: batchKey || "",
    acceptedByAudit: 0,
    acceptedApplied: 0,
    rejected: 0,
    duplicate: 0,
    persistentPending: 0,
    persistentApplied: 0,
    sameSegmentKeepVoice: 0,
    reassign: 0,
    bindingRestore: 0,
    fixedVoiceKept: 0,
    temporaryStart: 0,
    temporaryEnd: 0,
    temporaryReplace: 0,
    temporaryContinue: 0
  };
}

function graphV908LogVoiceAgeSparseApply(manager, phase, evidenceId) {
  if (!manager) return;
  if (!manager._v908VoiceAgeSparseApply) manager._v908VoiceAgeSparseApply = graphV908NewVoiceAgeSparseApplySummary("");
  var payload = graphV908Clone(manager._v908VoiceAgeSparseApply, {}) || {};
  payload.phase = phase || "unknown";
  payload.lastEvidenceId = evidenceId || "";
  graphRemoteLog("voice_age_audit_sparse_apply", payload);
}

function graphV908ApplyVoiceAgeConflictGuard(checked) {
  var groups = {};
  for (var i = 0; i < checked.length; i++) {
    var evidence = checked[i];
    if (!evidence || !evidence.accepted) continue;
    var category = evidence.stateAction === "persistent_update" ? "persistent" : (evidence.stateAction === "end" ? "end" : "temporary");
    var key = (evidence.sourceType === "temporary_state_review" ? (evidence.activeStateId || evidence.seq) : evidence.seq) + "|" + category;
    if (!groups[key]) groups[key] = [];
    groups[key].push(evidence);
  }
  for (var groupKey in groups) {
    if (!groups.hasOwnProperty(groupKey) || groups[groupKey].length <= 1) continue;
    var signatureMap = {};
    for (var j = 0; j < groups[groupKey].length; j++) {
      var item = groups[groupKey][j];
      signatureMap[[item.stateAction, item.endTiming || "", item.finalVoiceAgeStage || ""].join("|")] = true;
    }
    var signatureKeys = Object.keys(signatureMap);
    groups[groupKey].sort(function(a, b){ return Number(b.confidence || 0) - Number(a.confidence || 0); });
    for (var k = 0; k < groups[groupKey].length; k++) {
      if (signatureKeys.length > 1 || k > 0) {
        groups[groupKey][k].accepted = false;
        groups[groupKey][k].auditDecision = "reject";
        groups[groupKey][k].auditReason = signatureKeys.length > 1 ? "conflicting_accepted_voice_age_actions" : "duplicate_accepted_voice_age_action";
      }
    }
  }
}

CharacterManager.prototype.applyAcceptedTemporaryStateReviewsV908 = function(checked) {
  if (!ENABLE_TEMPORARY_VOICE_STATE) return { applied: 0, ended: 0, replaced: 0 };
  checked = checked || [];
  var summary = { applied: 0, ended: 0, replaced: 0 };
  for (var i = 0; i < checked.length; i++) {
    var evidence = checked[i] || {};
    if (evidence.sourceType !== "temporary_state_review" || evidence.accepted !== true) continue;
    var stateKey = evidence.activeStateKey || "";
    var state = stateKey && this.temporaryVoiceStates ? this.temporaryVoiceStates[stateKey] : null;
    if (!state && this.temporaryVoiceStates) {
      for (var key in this.temporaryVoiceStates) {
        if (!this.temporaryVoiceStates.hasOwnProperty(key)) continue;
        if (this.temporaryVoiceStates[key] && this.temporaryVoiceStates[key].stateId === evidence.activeStateId) { stateKey = key; state = this.temporaryVoiceStates[key]; break; }
      }
    }
    if (!state) continue;
    state.latestAcceptedEvidenceText = graphSafeString(evidence.evidenceText || "", VOICE_AGE_EVIDENCE_TEXT_MAX);
    state.latestAcceptedSummary = graphSafeString(evidence.summary || evidence.reason || "", 320);
    state.latestAcceptedEvidenceHash = evidence.evidenceHash || "";
    state.lastConfirmedChapterId = graphCurrentChapterId();
    state.lastConfirmedSeq = evidence.dialogueSeqs && evidence.dialogueSeqs.length ? evidence.dialogueSeqs[evidence.dialogueSeqs.length - 1] : (evidence.endBoundarySeq || state.lastConfirmedSeq || "");
    state.lastReviewDecision = evidence.reviewDecision || evidence.stateAction || "";
    state.lastReviewAcceptedAt = graphNowIso();
    summary.applied++;
    graphRemoteLog("temporary_voice_state_continue_audited", { stateId: state.stateId || evidence.activeStateId || "", subjectName: evidence.subjectName || state.roleName || "", decision: evidence.reviewDecision || evidence.stateAction || "", accepted: true, hasDialogue: evidence.hasDialogue === true, dialogueSeqs: evidence.dialogueSeqs || [], latestAcceptedEvidenceText: state.latestAcceptedEvidenceText, latestAcceptedSummary: state.latestAcceptedSummary, lastConfirmedChapterId: state.lastConfirmedChapterId, lastConfirmedSeq: state.lastConfirmedSeq });

    // 中文注释：无对白的状态变化没有可挂载的朗读句，审计提交后立即执行；有对白的边界由缓存序号精确调度。
    if (evidence.hasDialogue === false && evidence.stateAction === "end") {
      this.endTemporaryVoiceState(stateKey, "audited_no_dialogue_end", evidence);
      summary.ended++;
      graphRemoteLog("temporary_voice_transition_applied", { stateId: evidence.activeStateId || "", action: "end", timing: "no_dialogue_commit", evidenceId: evidence.evidenceId || "" });
    } else if (evidence.hasDialogue === false && evidence.stateAction === "replace") {
      var record = graphV908FindRecordByIdOrName(this, state.recordId || "", state.roleName || evidence.subjectName || "");
      if (record) {
        var oldTag = state.temporaryVoiceTag || "";
        var newTag = this.allocateTemporaryVoice(record, evidence);
        state.temporaryVoiceAgeStage = evidence.finalVoiceAgeStage || state.temporaryVoiceAgeStage || "";
        state.temporaryVoiceTag = newTag || state.temporaryVoiceTag;
        state.replacedAt = graphNowIso();
        summary.replaced++;
        this.rememberTemporaryVoiceEvent(evidence, record, state.temporaryVoiceTag, { timing: "no_dialogue_commit", replacedTemporaryVoiceTag: oldTag });
        graphRemoteLog("temporary_voice_transition_applied", { stateId: evidence.activeStateId || "", action: "replace", timing: "no_dialogue_commit", oldTemporaryVoiceTag: oldTag, newTemporaryVoiceTag: state.temporaryVoiceTag, newTemporaryVoiceAgeStage: state.temporaryVoiceAgeStage, evidenceId: evidence.evidenceId || "" });
      }
    }
  }
  return summary;
};

CharacterManager.prototype.updateVoiceAgeEvidenceInDialogCacheV908 = function(checked) {
  checked = checked || [];
  if (!checked.length) return false;
  var byId = {};
  for (var i = 0; i < checked.length; i++) if (checked[i] && checked[i].evidenceId) byId[checked[i].evidenceId] = checked[i];
  try {
    var cache = readDialogCache();
    var changed = false;
    var list = cache && Array.isArray(cache.dialogList) ? cache.dialogList : [];
    for (var j = 0; j < list.length; j++) {
      var oldEvidence = Array.isArray(list[j].voiceAgeEvidence) ? list[j].voiceAgeEvidence : [];
      var matched = false;
      var replacement = [];
      for (var e = 0; e < oldEvidence.length; e++) {
        var id = oldEvidence[e] && oldEvidence[e].evidenceId;
        if (id && byId[id]) { replacement.push(byId[id]); matched = true; }
        else replacement.push(oldEvidence[e]);
      }
      if (matched) { list[j].voiceAgeEvidence = replacement; changed = true; }
    }
    if (changed) writeDialogCache(cache);
    return changed;
  } catch(e0) {
    graphRemoteLog("pending_voice_age_rejected", { reason: "dialog_cache_update_exception", error: graphSafeString(e0 && e0.message || e0, 260) });
    return false;
  }
};

CharacterManager.prototype.commitPendingVoiceAgeAuditV908 = function(module, source) {
  var pending = this.pendingVoiceAgeEvidence;
  if (!pending || pending.consumed) return { skipped: true };
  module = module || { complete: false, reason: "voice_age_module_missing" };
  var decisions = module.complete && Array.isArray(module.decisions) ? module.decisions : [];
  var decisionMap = {};
  for (var i = 0; i < decisions.length; i++) if (decisions[i] && decisions[i].evidenceId) decisionMap[decisions[i].evidenceId] = decisions[i];
  var acceptedCount = 0;
  var rejectedCount = 0;
  for (var j = 0; j < pending.checked.length; j++) {
    var item = pending.checked[j];
    if (!item.precheckPassed) {
      item.accepted = false;
      rejectedCount++;
      graphRemoteLog("voice_age_audit_rejected", { evidenceId: item.evidenceId, seq: item.seq, subjectName: item.subjectName, decision: "reject", reason: item.precheckReason, stage: "local_precheck" });
      continue;
    }
    var decision = decisionMap[item.evidenceId];
    item.auditDecision = decision ? decision.decision : "reject";
    item.auditReason = decision ? decision.auditReason : (module.reason || "combined_or_standalone_audit_failed");
    item.auditDetails = decision || null;
    item.accepted = !!(decision && decision.decision === "accept");
    if (item.accepted) acceptedCount++; else rejectedCount++;
    graphRemoteLog(item.accepted ? "voice_age_audit_accepted" : (item.auditDecision === "verify" ? "voice_age_audit_verify" : "voice_age_audit_rejected"), { evidenceId: item.evidenceId, evidenceHash: item.evidenceHash, seq: item.seq, subjectName: item.subjectName, finalVoiceAgeStage: item.finalVoiceAgeStage, stateAction: item.stateAction, applyScope: item.applyScope, decision: item.auditDecision, reason: item.auditReason, source: source || "combined_audit" });
    if (!item.accepted) graphRemoteLog("voice_age_audit_exception", { evidenceId: item.evidenceId, evidenceHash: item.evidenceHash, seq: item.seq, subjectName: item.subjectName, sourceType: item.sourceType || "voice_age_evidence", decision: item.auditDecision, auditReason: item.auditReason });
  }
  graphV908ApplyVoiceAgeConflictGuard(pending.checked);
  var temporaryReviewApplySummary = this.applyAcceptedTemporaryStateReviewsV908 ? this.applyAcceptedTemporaryStateReviewsV908(pending.checked) : { applied: 0, ended: 0, replaced: 0 };
  acceptedCount = pending.checked.filter(function(x){ return x && x.accepted === true; }).length;
  rejectedCount = pending.checked.length - acceptedCount;
  var sparseApply = graphV908NewVoiceAgeSparseApplySummary(pending.batchKey || "");
  sparseApply.acceptedByAudit = acceptedCount;
  sparseApply.acceptedApplied = Number(temporaryReviewApplySummary && temporaryReviewApplySummary.applied || 0);
  sparseApply.rejected = rejectedCount;
  for (var sparseIndex = 0; sparseIndex < pending.checked.length; sparseIndex++) {
    var sparseItem = pending.checked[sparseIndex] || {};
    if (!sparseItem.accepted) continue;
    if (sparseItem.stateAction === "persistent_update") sparseApply.persistentPending++;
    else if (sparseItem.stateAction === "start" || sparseItem.stateAction === "one_shot") sparseApply.temporaryStart++;
    else if (sparseItem.stateAction === "end") sparseApply.temporaryEnd++;
    else if (sparseItem.stateAction === "replace") sparseApply.temporaryReplace++;
    else if (sparseItem.stateAction === "continue") sparseApply.temporaryContinue++;
  }
  this._v908VoiceAgeSparseApply = sparseApply;
  graphV908LogVoiceAgeSparseApply(this, "audit_commit_pending_dialogue_application", "");
  pending.consumed = true;
  pending.auditBuffered = false;
  pending.consumedAt = graphNowIso();
  pending.consumedBy = source || "combined_audit";
  this.updateVoiceAgeEvidenceInDialogCacheV908(pending.checked);
  graphRemoteLog(module.complete ? "pending_voice_age_applied" : "pending_voice_age_rejected", { batchKey: pending.batchKey || "", source: source || "", moduleComplete: !!module.complete, reason: module.reason || "", checkedCount: pending.checked.length, acceptedCount: acceptedCount, rejectedCount: rejectedCount, temporaryReviewApplySummary: temporaryReviewApplySummary });
  this.traceCombinedAuditCommitV908("voiceAge", { source: source || "", complete: !!module.complete, reason: module.reason || "", acceptedCount: acceptedCount, rejectedCount: rejectedCount });
  try { delete this._v908BufferedVoiceAgeAudit; } catch(e0) { this._v908BufferedVoiceAgeAudit = null; }
  return { committed: true, acceptedCount: acceptedCount, rejectedCount: rejectedCount, checked: pending.checked };
};

CharacterManager.prototype.resolvePendingAgeAndGraphWithoutAliasV908 = function(chapterText) {
  var pendingAge = this.pendingVoiceAgeEvidence;
  var pendingGraph = this.pendingNameSemanticRelations;
  var hasUnbufferedAge = !!(pendingAge && !pendingAge.consumed && !pendingAge.auditBuffered);
  var hasGraph = !!(pendingGraph && !pendingGraph.consumed && !pendingGraph.auditBuffered && pendingGraph.relations && pendingGraph.relations.length);
  if (!hasUnbufferedAge && !hasGraph) return { skipped: true, reason: "no_unresolved_age_or_graph_module" };
  var candidates = hasUnbufferedAge ? (pendingAge.candidates || []) : [];
  var relations = hasGraph ? (pendingGraph.relations || []) : [];
  var sharedPreviousText = hasUnbufferedAge ? String(pendingAge.previousText || "") : "";
  var sharedCurrentText = hasUnbufferedAge ? String(pendingAge.currentText || "") : String(hasGraph && pendingGraph.chapterText || chapterText || "");
  var sharedSourceBlock = graphV908BuildCombinedSharedSourceBlock(sharedPreviousText, sharedCurrentText);
  var ageBlock = this.buildCombinedVoiceAgeAuditBlockV908(candidates, { omitSourceText: true });
  var graphPrompt = relations.length && this.buildNameSemanticRelationAuditPrompt ? this.buildNameSemanticRelationAuditPrompt(relations, sharedCurrentText, { omitSourceText: true }) : "";
  var prompt = "你将一次完成当前适用的发声音龄证据审计和图谱证据审计。两个模块相互独立：一方全部拒收也必须完整返回另一方，禁止漏字段。\n\n" +
    sharedSourceBlock + "\n" +
    (graphPrompt ? ("【图谱证据审计模块】\n" + graphPrompt + "\n\n") : "") +
    (ageBlock ? ("【发声音龄证据审计模块】\n" + ageBlock + "\n\n") : "") +
    "【合并输出】图谱审计使用顶层auditComplete/allAccepted/acceptedAll/downgrade/reject/verify；年龄审计使用voiceAgeAudit对象。只输出一个顶层JSON对象。";
  var run = null;
  if (candidates.length || relations.length) run = this.runCombinedAuditRequestV908("voice_age+graph", prompt, false, candidates, relations);
  else run = { raw: {}, status: graphV908BuildCombinedAuditStatus(false, candidates, relations, {}), success: true, retryCount: 0 };
  var resolution = this.resolveCombinedAuditFallbacksV908({ flow: "voice_age+graph", status: run.status, raw: run.raw, voiceAgeCandidates: candidates, relations: relations, chapterText: chapterText || (pendingGraph && pendingGraph.chapterText) || "" });
  // 无新角色时别名模块明确标记为不适用，提交顺序仍从alias占位开始，便于日志还原。
  this.traceCombinedAuditCommitV908("alias", { flow: "voice_age+graph", complete: true, notApplicable: true });
  if (hasGraph) this.bufferPendingGraphAuditV908(resolution.graph, resolution.graph && resolution.graph.auditSource || "age_graph_combined_audit");
  if (hasUnbufferedAge) this.bufferPendingVoiceAgeAuditV908(resolution.voiceAge, resolution.voiceAge && resolution.voiceAge.auditSource || "age_graph_combined_audit");
  return { run: run, resolution: resolution };
};

CharacterManager.prototype.applyPersistentVoiceAgeEvidence = function(record, evidence) {
  if (!record || !evidence || !evidence.accepted || evidence.stateAction !== "persistent_update") return false;
  if (!this._v908VoiceAgeSparseApply) this._v908VoiceAgeSparseApply = graphV908NewVoiceAgeSparseApplySummary("");
  // 双保险：固定音色在审计后、真正落角色卡前再拦一次，既不改年龄也不记已应用证据。
  var fixedGuard = this.enforceFixedVoiceRecordV908 ? this.enforceFixedVoiceRecordV908(record, "persistent_voice_age_apply_guard") : { locked: graphV908IsFixedVoiceRecord(record), tag: record.voice || "" };
  if (fixedGuard.locked) {
    this._v908VoiceAgeSparseApply.fixedVoiceKept++;
    graphV908LogVoiceAgeSparseApply(this, "persistent_fixed_voice_skipped", evidence.evidenceId || "");
    return false;
  }
  var hash = evidence.evidenceHash || ("age_" + graphHash(JSON.stringify(evidence)));
  var appliedHashes = Array.isArray(record.voiceAgeAppliedHashes) ? record.voiceAgeAppliedHashes : [];
  if (record.voiceAgeEvidenceHash === hash || appliedHashes.indexOf(hash) !== -1 || this.voiceAgeAppliedEvidence[hash]) {
    this._v908VoiceAgeSparseApply.duplicate++;
    graphRemoteLog("voice_age_duplicate", { name: graphNormalizeName(record.name || ""), evidenceId: evidence.evidenceId || "", evidenceHash: hash, action: "persistent_update_skipped" });
    graphV908LogVoiceAgeSparseApply(this, "persistent_duplicate_skipped", evidence.evidenceId || "");
    return false;
  }
  var targetGender = graphV908NormalizeGenderForVoice(record.gender || "", evidence.finalVoiceAgeStage || "");
  var targetAge = graphV908NormalizeVoiceAgeStage(targetGender, evidence.finalVoiceAgeStage || "");
  if (!targetAge) return false;
  var beforeGender = record.gender || "";
  var beforeAge = record.age || "";
  var beforeVoice = record.voice || "";
  var beforeSegment = graphV908VoiceSegmentKey(beforeGender, beforeAge);
  var targetSegment = graphV908VoiceSegmentKey(targetGender, targetAge);
  graphRemoteLog("voice_age_segment_normalized", { name: graphNormalizeName(record.name || ""), rawStage: evidence.finalVoiceAgeStage || "", gender: targetGender, normalizedStage: targetAge, beforeSegment: beforeSegment, targetSegment: targetSegment });
  // 同段证据即使因旧缓存或竞态绕过预检，也在最终落盘前跳过；不重分配、不写年龄证据缓存。
  if (beforeSegment && targetSegment && beforeSegment === targetSegment) {
    this._v908VoiceAgeSparseApply.sameSegmentKeepVoice++;
    graphRemoteLog("voice_age_same_segment_keep", { name: graphNormalizeName(record.name || ""), beforeSegment: beforeSegment, targetSegment: targetSegment, beforeVoice: beforeVoice, afterVoice: beforeVoice, assignVoiceCalled: false, skippedBeforeMutation: true });
    graphV908LogVoiceAgeSparseApply(this, "persistent_same_segment_skipped", evidence.evidenceId || "");
    return false;
  }
  var fixedLocked = false;
  var action = "cross_segment_update";
  var backupSaved = false;
  var restoredFromBackup = false;

  if (beforeSegment !== targetSegment) {
    if (beforeVoice && beforeSegment && this.saveAgeVoiceBindingBackup) backupSaved = this.saveAgeVoiceBindingBackup(record, beforeSegment, targetSegment, "audited_voice_age_segment_changed");
    var restored = this.findAgeVoiceBindingBackup ? this.findAgeVoiceBindingBackup(record, targetSegment) : null;
    record.gender = targetGender;
    record.age = targetAge;
    if (restored && restored.backup && restored.backup.voice) {
      record.voice = restored.backup.voice;
      restored.backup.lastRestoredAt = graphNowIso();
      restored.backup.lastRestoredChapter = graphCurrentChapterId();
      restoredFromBackup = true;
      action = "restore_audited_age_voice_binding";
      graphRemoteLog("character_age_voice_binding_backup_restored", { name: graphNormalizeName(record.name || ""), targetSegment: targetSegment, restoredVoice: record.voice || "", evidenceHash: hash, source: "audited_voice_age" });
    } else {
      var reassigned = this.assignVoice ? this.assignVoice(targetGender, targetAge, { targetName: record.name || "", assignType: "审计确认的自然发声音龄变化", sourceStage: "audited_voice_age", afterAliasCheck: true, isSpecialSpeaker: false, ageSegmentChanged: true, oldVoice: beforeVoice, oldSegment: beforeSegment, newSegment: targetSegment }) : "";
      if (reassigned) record.voice = reassigned;
      action = reassigned ? "reassign_for_audited_natural_stage" : "audited_stage_updated_voice_fallback_kept";
    }
  }

  record.voiceAgeVerified = true;
  record.voiceAgeProvisional = false;
  record.verifiedVoiceAgeStage = targetAge;
  record.voiceAgeSource = "audited_original_text_evidence";
  record.voiceAgeEvidenceText = graphSafeString(evidence.evidenceText || "", VOICE_AGE_EVIDENCE_TEXT_MAX);
  record.voiceAgeEvidenceReason = graphSafeString(evidence.reason || evidence.decisionBasis || "", 320);
  record.voiceAgeAuditReason = graphSafeString(evidence.auditReason || "", 320);
  record.voiceAgeEvidenceHash = hash;
  record.voiceAgeUpdatedChapter = graphCurrentChapterId();
  record.voiceAgeUpdatedAt = graphNowIso();
  appliedHashes.push(hash);
  while (appliedHashes.length > 20) appliedHashes.shift();
  record.voiceAgeAppliedHashes = appliedHashes;
  this.voiceAgeAppliedEvidence[hash] = true;
  this._v908VoiceAgeSparseApply.persistentApplied++;
  this._v908VoiceAgeSparseApply.acceptedApplied++;
  if (action === "reassign_for_audited_natural_stage") this._v908VoiceAgeSparseApply.reassign++;
  if (action === "restore_audited_age_voice_binding") this._v908VoiceAgeSparseApply.bindingRestore++;
  // 按书、按规则版本只保存真正改变自然年龄段且已经落到角色卡的最小记录；候选、送审和同段结果不入缓存。
  if (this.storeVoiceAgeEvidenceCacheV908) {
    var cachedAppliedEvidence = {
      evidenceHash: hash,
      evidenceId: evidence.evidenceId || "",
      appliedToNaturalRecord: true,
      appliedRecordId: record.recordId || "",
      subjectName: graphNormalizeName(record.name || evidence.subjectName || ""),
      beforeSegment: beforeSegment,
      targetSegment: targetSegment,
      outcome: action,
      chapterId: graphCurrentChapterId(),
      evidenceText: graphSafeString(evidence.evidenceText || "", VOICE_AGE_EVIDENCE_TEXT_MAX),
      auditReason: graphSafeString(evidence.auditReason || evidence.reason || "", 320),
      appliedAt: graphNowIso()
    };
    this.storeVoiceAgeEvidenceCacheV908([cachedAppliedEvidence], "natural_record_applied", {});
  }
  graphRemoteLog("voice_age_applied", { name: graphNormalizeName(record.name || ""), recordId: record.recordId || "", evidenceId: evidence.evidenceId || "", evidenceHash: hash, beforeGender: beforeGender, beforeAge: beforeAge, beforeVoice: beforeVoice, beforeSegment: beforeSegment, afterGender: record.gender || beforeGender, afterAge: record.age || beforeAge, afterVoice: record.voice || beforeVoice, targetSegment: targetSegment, fixedVoiceLocked: fixedLocked, backupSaved: backupSaved, restoredFromBackup: restoredFromBackup, action: action, evidenceText: record.voiceAgeEvidenceText });
  graphV908LogVoiceAgeSparseApply(this, "persistent_dialogue_application", evidence.evidenceId || "");
  return true;
};

CharacterManager.prototype.temporaryVoiceStateKey = function(record) {
  if (!record) return "";
  try { if (typeof v87EnsureRecordId === "function") v87EnsureRecordId(this, record); } catch(e) {}
  var recordId = graphSafeString(record.recordId || graphNormalizeName(record.name || ""), 100);
  return graphV908CurrentBookKey(this) + "|" + recordId;
};

CharacterManager.prototype.findTemporaryVoiceStateForRecord = function(record) {
  if (!record) return null;
  if (!this.temporaryVoiceStates) this.temporaryVoiceStates = {};
  var key = this.temporaryVoiceStateKey(record);
  if (key && this.temporaryVoiceStates[key]) return { key: key, state: this.temporaryVoiceStates[key] };
  var aliases = String(record.aliases || record.name || "").split("|");
  var aliasMap = {};
  for (var i = 0; i < aliases.length; i++) { var n = graphNormalizeName(aliases[i]); if (n) aliasMap[n] = true; }
  for (var oldKey in this.temporaryVoiceStates) {
    if (!this.temporaryVoiceStates.hasOwnProperty(oldKey)) continue;
    var state = this.temporaryVoiceStates[oldKey] || {};
    if (state.bookKey !== graphV908CurrentBookKey(this)) continue;
    if (aliasMap[graphNormalizeName(state.roleName || "")]) {
      delete this.temporaryVoiceStates[oldKey];
      state.recordId = record.recordId || state.recordId;
      state.roleName = graphNormalizeName(record.name || state.roleName);
      this.temporaryVoiceStates[key] = state;
      graphRemoteLog("temporary_voice_state_replace", { reason: "alias_record_id_migrated", oldStateKey: oldKey, newStateKey: key, recordId: state.recordId || "", roleName: state.roleName || "", temporaryVoiceTag: state.temporaryVoiceTag || "" });
      return { key: key, state: state };
    }
  }
  return null;
};

CharacterManager.prototype.endTemporaryVoiceState = function(key, reason, evidence) {
  if (!key || !this.temporaryVoiceStates || !this.temporaryVoiceStates[key]) return null;
  var state = this.temporaryVoiceStates[key];
  delete this.temporaryVoiceStates[key];
  graphRemoteLog("temporary_voice_state_end", { stateKey: key, roleName: state.roleName || "", recordId: state.recordId || "", temporaryVoiceTag: state.temporaryVoiceTag || "", naturalVoiceTag: state.naturalVoiceTag || "", roleDialogueCount: state.roleDialogueCount || 0, reason: reason || "explicit_end", evidenceId: evidence && evidence.evidenceId || "", evidenceHash: evidence && evidence.evidenceHash || "", endTiming: evidence && evidence.endTiming || "" });
  return state;
};

CharacterManager.prototype.clearTemporaryVoiceStates = function(reason) {
  if (!this.temporaryVoiceStates) this.temporaryVoiceStates = {};
  for (var key in this.temporaryVoiceStates) {
    if (!this.temporaryVoiceStates.hasOwnProperty(key)) continue;
    var state = this.temporaryVoiceStates[key] || {};
    graphRemoteLog("temporary_voice_state_expire", { stateKey: key, roleName: state.roleName || "", recordId: state.recordId || "", temporaryVoiceTag: state.temporaryVoiceTag || "", roleDialogueCount: state.roleDialogueCount || 0, reason: reason || "safety_clear" });
  }
  this.temporaryVoiceStates = {};
};

CharacterManager.prototype.allocateTemporaryVoice = function(record, evidence) {
  var fixedGuard = this.enforceFixedVoiceRecordV908 ? this.enforceFixedVoiceRecordV908(record, "temporary_voice_allocate_guard") : { locked: graphV908IsFixedVoiceRecord(record), tag: record && record.voice || "" };
  if (fixedGuard.locked) return fixedGuard.tag || record && record.voice || "default";
  var gender = graphV908NormalizeGenderForVoice(record && record.gender || "", evidence && evidence.finalVoiceAgeStage || "");
  var age = graphV908NormalizeVoiceAgeStage(gender, evidence && evidence.finalVoiceAgeStage || "");
  if (!age) return record && record.voice || "default";
  var tag = this.assignVoice ? this.assignVoice(gender, age, { targetName: record && record.name || "", assignType: "临时伪装换声", sourceStage: "temporary_voice_state", afterAliasCheck: true, isSpecialSpeaker: false, temporaryVoice: true, evidenceHash: evidence && evidence.evidenceHash || "", stateAction: evidence && evidence.stateAction || "" }) : "";
  return tag || (record && record.voice) || "default";
};

CharacterManager.prototype.rememberTemporaryVoiceEvent = function(evidence, record, tag, extra) {
  if (!evidence || !evidence.evidenceHash) return;
  if (!this.temporaryVoiceAppliedEvents) this.temporaryVoiceAppliedEvents = {};
  this.temporaryVoiceAppliedEvents[evidence.evidenceHash] = {
    evidenceHash: evidence.evidenceHash,
    evidenceId: evidence.evidenceId || "",
    action: evidence.stateAction || "",
    endTiming: evidence.endTiming || "",
    tag: tag || "",
    recordId: record && record.recordId || "",
    roleName: record && graphNormalizeName(record.name || "") || "",
    bookKey: graphV908CurrentBookKey(this),
    chapterId: graphCurrentChapterId(),
    savedAt: graphNowIso(),
    extra: extra || {}
  };
  var keys = Object.keys(this.temporaryVoiceAppliedEvents);
  while (keys.length > 120) delete this.temporaryVoiceAppliedEvents[keys.shift()];
};

CharacterManager.prototype.applyAuditedVoiceAgeForDialogue = function(record, analysis, currentDialogueText, characterId) {
  if (!record) return { tag: "default", naturalTag: "default", stateAction: "no_record" };
  if (!this.temporaryVoiceStates) this.temporaryVoiceStates = {};
  if (!this.temporaryVoiceAppliedEvents) this.temporaryVoiceAppliedEvents = {};
  if (!this.voiceAgeAppliedEvidence) this.voiceAgeAppliedEvidence = {};
  try { if (typeof v87EnsureRecordId === "function") v87EnsureRecordId(this, record); } catch(e0) {}
  // 最终发音出口的绝对保护：用户固定的普通角色、男女主角和系统角色都不允许自然变声或临时换声。
  var fixedGuard = this.enforceFixedVoiceRecordV908 ? this.enforceFixedVoiceRecordV908(record, "dialogue_voice_final_guard") : { locked: graphV908IsFixedVoiceRecord(record), tag: record.voice || "" };
  if (fixedGuard.locked) return { tag: fixedGuard.tag || record.voice || "default", naturalTag: fixedGuard.tag || record.voice || "default", stateAction: "fixed_voice", acceptedEvidenceCount: 0 };
  var naturalTag = record.voice || "default";
  var finalTag = naturalTag;
  var acceptedOriginal = [];
  var allEvidence = analysis && Array.isArray(analysis.__voiceAgeEvidence) ? analysis.__voiceAgeEvidence : [];
  for (var i = 0; i < allEvidence.length; i++) {
    var e = allEvidence[i] || {};
    if (e.accepted === true && graphV908SubjectMatches(this, e.subjectName || "", analysis.name || record.name || "")) acceptedOriginal.push(e);
  }

  // 自然年龄证据即使关闭临时换声也照常应用；总开关只截断临时动作、状态复用和缓存恢复。
  for (var persistentIndex = 0; persistentIndex < acceptedOriginal.length; persistentIndex++) {
    if (acceptedOriginal[persistentIndex].stateAction === "persistent_update") {
      this.applyPersistentVoiceAgeEvidence(record, acceptedOriginal[persistentIndex]);
      naturalTag = record.voice || naturalTag;
      finalTag = naturalTag;
    }
  }
  if (!ENABLE_TEMPORARY_VOICE_STATE) {
    var clearedCount = Object.keys(this.temporaryVoiceStates || {}).length;
    if (clearedCount && this.clearTemporaryVoiceStates) this.clearTemporaryVoiceStates("temporary_voice_feature_disabled_before_apply");
    this.temporaryVoiceAppliedEvents = {};
    graphRemoteLog("temporary_voice_feature_disabled", { ignoredTemporaryEvidenceCount: acceptedOriginal.filter(function(item){ return item.stateAction !== "persistent_update"; }).length, clearedMemoryStateCount: clearedCount, ignoredCacheSnapshot: true, returnedNaturalVoice: true });
    return { tag: record.voice || naturalTag || "default", naturalTag: record.voice || naturalTag || "default", stateAction: "disabled", acceptedEvidenceCount: acceptedOriginal.length };
  }

  if (analysis && analysis.__dialogCacheMeta && this.tryRestoreTemporaryVoiceSnapshot) this.tryRestoreTemporaryVoiceSnapshot(analysis.__dialogCacheMeta, currentDialogueText);
  var matchedSeq = graphSafeString(analysis && analysis.__dialogCacheMeta && analysis.__dialogCacheMeta.matchedSeq || "", 20);
  var accepted = [];
  for (var expandIndex = 0; expandIndex < acceptedOriginal.length; expandIndex++) {
    if (acceptedOriginal[expandIndex].stateAction === "persistent_update") continue;
    var expanded = graphV908ExpandVoiceAgeEvidenceForSeqV908(acceptedOriginal[expandIndex], matchedSeq || acceptedOriginal[expandIndex].seq || "");
    for (var expandedIndex = 0; expandedIndex < expanded.length; expandedIndex++) if (expanded[expandedIndex]) accepted.push(expanded[expandedIndex]);
  }
  var stateInfo = this.findTemporaryVoiceStateForRecord(record);
  var stateKey = stateInfo ? stateInfo.key : this.temporaryVoiceStateKey(record);
  var state = stateInfo ? stateInfo.state : null;
  if (state && state.bookKey !== graphV908CurrentBookKey(this)) {
    graphRemoteLog("temporary_voice_state_expire", { stateKey: stateKey, roleName: state.roleName || "", recordId: state.recordId || "", reason: "book_mismatch_before_dialogue" });
    delete this.temporaryVoiceStates[stateKey];
    state = null;
  } else if (state && state.chapterId !== graphCurrentChapterId()) {
    if (ENABLE_TEMPORARY_VOICE_CROSS_CHAPTER) {
      state.previousChapterId = state.chapterId || "";
      state.chapterId = graphCurrentChapterId();
      state.crossChapterCarryPending = true;
    } else {
      graphRemoteLog("temporary_voice_state_expire", { stateKey: stateKey, roleName: state.roleName || "", recordId: state.recordId || "", reason: "cross_chapter_disabled_before_dialogue" });
      delete this.temporaryVoiceStates[stateKey];
      state = null;
    }
  }

  var endBefore = null;
  var endAfter = null;
  var temporaryActions = [];
  for (var ai = 0; ai < accepted.length; ai++) {
    var evidence = accepted[ai];
    if (evidence.stateAction === "end") {
      if (evidence.endTiming === "after_dialogue") endAfter = evidence;
      else endBefore = evidence;
    } else temporaryActions.push(evidence);
  }
  if (state) {
    state.naturalVoiceTag = naturalTag;
    state.naturalAgeStage = record.age || state.naturalAgeStage || "";
  }

  if (endBefore && state) {
    this.endTemporaryVoiceState(stateKey, "explicit_end_before_dialogue", endBefore);
    this.rememberTemporaryVoiceEvent(endBefore, record, naturalTag, { timing: "before_dialogue" });
    graphRemoteLog("temporary_voice_transition_applied", { stateId: endBefore.activeStateId || state.stateId || "", action: "end", targetSeq: matchedSeq, timing: "before_dialogue", evidenceId: endBefore.evidenceId || "" });
    state = null;
    finalTag = naturalTag;
  }

  var dialogueCounted = false;
  var replaceAfter = null;
  var lastTemporaryAction = null;
  for (var actionIndex = 0; actionIndex < temporaryActions.length; actionIndex++) {
    var temporaryAction = temporaryActions[actionIndex] || {};
    lastTemporaryAction = temporaryAction;
    var eventHash = temporaryAction.evidenceHash || "";
    var oldEvent = eventHash ? this.temporaryVoiceAppliedEvents[eventHash] : null;
    if (temporaryAction.stateAction === "continue") {
      if (state) {
        finalTag = state.temporaryVoiceTag || naturalTag;
        state.latestAcceptedEvidenceText = graphSafeString(temporaryAction.evidenceText || state.latestAcceptedEvidenceText || "", VOICE_AGE_EVIDENCE_TEXT_MAX);
        state.latestAcceptedSummary = graphSafeString(temporaryAction.summary || temporaryAction.reason || state.latestAcceptedSummary || "", 320);
        state.latestAcceptedEvidenceHash = temporaryAction.evidenceHash || state.latestAcceptedEvidenceHash || "";
        state.lastConfirmedChapterId = graphCurrentChapterId();
        state.lastConfirmedSeq = matchedSeq || temporaryAction.seq || state.lastConfirmedSeq || "";
        state.lastReviewDecision = "continue";
        graphRemoteLog("temporary_voice_transition_applied", { stateId: state.stateId || temporaryAction.activeStateId || "", action: "continue", targetSeq: matchedSeq, temporaryVoiceTag: finalTag, evidenceId: temporaryAction.evidenceId || "" });
        graphRemoteLog("temporary_voice_state_carried_to_next_batch", { stateId: state.stateId || temporaryAction.activeStateId || "", subjectName: state.roleName || temporaryAction.subjectName || "", temporaryVoiceAgeStage: state.temporaryVoiceAgeStage || "", lastConfirmedChapterId: state.lastConfirmedChapterId || "", lastConfirmedSeq: state.lastConfirmedSeq || "", reason: "本批continue续判审计通过" });
      }
      continue;
    }
    if (temporaryAction.stateAction === "replace" && temporaryAction.endTiming === "after_dialogue") {
      replaceAfter = temporaryAction;
      if (state) finalTag = state.temporaryVoiceTag || naturalTag;
      continue;
    }
    if (oldEvent && temporaryAction.stateAction !== "replace") {
      finalTag = oldEvent.tag || naturalTag;
      graphRemoteLog("temporary_voice_duplicate", { evidenceId: temporaryAction.evidenceId || "", evidenceHash: eventHash, stateAction: temporaryAction.stateAction, reusedTag: finalTag, recordId: record.recordId || "" });
      continue;
    }
    if (temporaryAction.stateAction === "one_shot") {
      finalTag = this.allocateTemporaryVoice(record, temporaryAction);
      this.rememberTemporaryVoiceEvent(temporaryAction, record, finalTag, { scope: "current_dialogue" });
      graphRemoteLog("temporary_voice_state_start", { mode: "one_shot", stateStored: false, roleName: graphNormalizeName(record.name || ""), recordId: record.recordId || "", naturalVoiceTag: naturalTag, temporaryVoiceTag: finalTag, temporaryVoiceAgeStage: temporaryAction.finalVoiceAgeStage || "", evidenceId: temporaryAction.evidenceId || "", evidenceHash: eventHash });
    } else if (temporaryAction.stateAction === "start" || temporaryAction.stateAction === "replace") {
      var oldState = state;
      finalTag = this.allocateTemporaryVoice(record, temporaryAction);
      state = {
        schema: "v908_temporary_voice_state",
        stateId: temporaryAction.activeStateId || "temp_state_" + graphHash([graphV908CurrentBookKey(this), record.recordId || record.name || "", eventHash || temporaryAction.evidenceId || ""].join("|")),
        bookKey: graphV908CurrentBookKey(this), chapterId: graphCurrentChapterId(), recordId: record.recordId || "", roleName: graphNormalizeName(record.name || ""), characterId: graphSafeString(characterId || "", 80),
        gender: record.gender || "", temporaryVoiceAgeStage: temporaryAction.finalVoiceAgeStage || "", temporaryVoiceTag: finalTag,
        naturalVoiceTag: naturalTag, naturalAgeStage: record.age || "", startEvidenceHash: eventHash,
        startEvidenceText: graphSafeString(temporaryAction.evidenceText || "", VOICE_AGE_EVIDENCE_TEXT_MAX), startEvidenceSummary: graphSafeString(temporaryAction.summary || temporaryAction.reason || "", 320),
        startChapterId: graphCurrentChapterId(), startSeq: matchedSeq || temporaryAction.seq || "",
        latestAcceptedEvidenceText: graphSafeString(temporaryAction.evidenceText || "", VOICE_AGE_EVIDENCE_TEXT_MAX), latestAcceptedSummary: graphSafeString(temporaryAction.summary || temporaryAction.reason || "", 320), latestAcceptedEvidenceHash: eventHash,
        lastConfirmedChapterId: graphCurrentChapterId(), lastConfirmedSeq: matchedSeq || temporaryAction.seq || "",
        roleDialogueCount: 0, startedAt: graphNowIso(), lastUsedAt: graphNowIso(), lastDialogueHash: graphHash(normalizeNameAnalysisDialogueText(currentDialogueText || ""))
      };
      this.temporaryVoiceStates[stateKey] = state;
      this.rememberTemporaryVoiceEvent(temporaryAction, record, finalTag, { scope: "scene", action: temporaryAction.stateAction });
      graphRemoteLog(oldState || temporaryAction.stateAction === "replace" ? "temporary_voice_state_replace" : "temporary_voice_state_start", { mode: "scene", reason: temporaryAction.stateAction === "replace" ? "audited_replace_before_dialogue" : (oldState ? "new_start_replaces_old_temporary_state" : "audited_start"), roleName: state.roleName, recordId: state.recordId, stateId: state.stateId, naturalVoiceTag: state.naturalVoiceTag, temporaryVoiceTag: state.temporaryVoiceTag, temporaryVoiceAgeStage: state.temporaryVoiceAgeStage, replacedTemporaryVoiceTag: oldState && oldState.temporaryVoiceTag || "", evidenceId: temporaryAction.evidenceId || "", evidenceHash: eventHash });
      graphRemoteLog("temporary_voice_transition_applied", { stateId: state.stateId, action: temporaryAction.stateAction, targetSeq: matchedSeq, timing: temporaryAction.endTiming || "before_dialogue", temporaryVoiceTag: finalTag, evidenceId: temporaryAction.evidenceId || "" });
      if (temporaryAction.coverageMode === "beyond_batch" || temporaryAction.stateAction === "replace") graphRemoteLog("temporary_voice_state_carried_to_next_batch", { stateId: state.stateId, subjectName: state.roleName, temporaryVoiceAgeStage: state.temporaryVoiceAgeStage, lastConfirmedChapterId: state.lastConfirmedChapterId, lastConfirmedSeq: state.lastConfirmedSeq, reason: temporaryAction.stateAction === "replace" ? "替换后的新临时状态继续" : "本批未发现结束信号" });
    }
  }

  if (state && !endBefore && !dialogueCounted && (!temporaryActions.length || temporaryActions.some(function(item){ return item.stateAction === "continue" || item.stateAction === "start" || item.stateAction === "replace"; }))) {
    var maxDialogues = Math.max(1, parseInt(TEMPORARY_VOICE_MAX_ROLE_DIALOGUES, 10) || 30);
    if (Number(state.roleDialogueCount || 0) >= maxDialogues) {
      graphRemoteLog("temporary_voice_state_safety_expire", { stateKey: stateKey, stateId: state.stateId || "", roleName: state.roleName || "", recordId: state.recordId || "", temporaryVoiceTag: state.temporaryVoiceTag || "", roleDialogueCount: state.roleDialogueCount || 0, maxRoleDialogues: maxDialogues, lastReviewDecision: state.lastReviewDecision || "", reason: "max_role_dialogues_reached" });
      delete this.temporaryVoiceStates[stateKey];
      state = null;
      finalTag = naturalTag;
    } else {
      state.roleDialogueCount = Number(state.roleDialogueCount || 0) + 1;
      dialogueCounted = true;
      state.lastUsedAt = graphNowIso();
      state.lastDialogueHash = graphHash(normalizeNameAnalysisDialogueText(currentDialogueText || ""));
      finalTag = state.temporaryVoiceTag || naturalTag;
      graphRemoteLog("temporary_voice_state_reuse", { stateKey: stateKey, stateId: state.stateId || "", roleName: state.roleName || "", recordId: state.recordId || "", temporaryVoiceTag: finalTag, naturalVoiceTag: naturalTag, roleDialogueCount: state.roleDialogueCount, maxRoleDialogues: maxDialogues, reason: temporaryActions.length ? "audited_temporary_transition_or_continue" : "no_new_evidence_active_scene_state" });
    }
  }

  if (endAfter) {
    var stateBeforeEnd = this.findTemporaryVoiceStateForRecord(record);
    if (stateBeforeEnd && stateBeforeEnd.state) {
      finalTag = stateBeforeEnd.state.temporaryVoiceTag || finalTag;
      this.endTemporaryVoiceState(stateBeforeEnd.key, "explicit_end_after_dialogue", endAfter);
    }
    this.rememberTemporaryVoiceEvent(endAfter, record, finalTag, { timing: "after_dialogue" });
    graphRemoteLog("temporary_voice_transition_applied", { stateId: endAfter.activeStateId || stateBeforeEnd && stateBeforeEnd.state && stateBeforeEnd.state.stateId || "", action: "end", targetSeq: matchedSeq, timing: "after_dialogue", evidenceId: endAfter.evidenceId || "" });
    state = null;
  }

  if (replaceAfter) {
    var oldStateAfter = this.findTemporaryVoiceStateForRecord(record);
    var oldTagAfter = oldStateAfter && oldStateAfter.state && oldStateAfter.state.temporaryVoiceTag || finalTag;
    var replacementTagAfter = this.allocateTemporaryVoice(record, replaceAfter);
    var replacementState = oldStateAfter && oldStateAfter.state ? oldStateAfter.state : { schema: "v908_temporary_voice_state", bookKey: graphV908CurrentBookKey(this), recordId: record.recordId || "", roleName: graphNormalizeName(record.name || ""), characterId: graphSafeString(characterId || "", 80), naturalVoiceTag: naturalTag, naturalAgeStage: record.age || "", roleDialogueCount: 0, startedAt: graphNowIso() };
    replacementState.stateId = replaceAfter.activeStateId || replacementState.stateId || "temp_state_" + graphHash([graphV908CurrentBookKey(this), record.recordId || record.name || "", replaceAfter.evidenceHash || ""].join("|"));
    replacementState.chapterId = graphCurrentChapterId();
    replacementState.temporaryVoiceAgeStage = replaceAfter.finalVoiceAgeStage || replacementState.temporaryVoiceAgeStage || "";
    replacementState.temporaryVoiceTag = replacementTagAfter || replacementState.temporaryVoiceTag || naturalTag;
    replacementState.latestAcceptedEvidenceText = graphSafeString(replaceAfter.evidenceText || "", VOICE_AGE_EVIDENCE_TEXT_MAX);
    replacementState.latestAcceptedSummary = graphSafeString(replaceAfter.summary || replaceAfter.reason || "", 320);
    replacementState.latestAcceptedEvidenceHash = replaceAfter.evidenceHash || "";
    replacementState.lastConfirmedChapterId = graphCurrentChapterId();
    replacementState.lastConfirmedSeq = matchedSeq;
    replacementState.lastReviewDecision = "replace";
    replacementState.lastUsedAt = graphNowIso();
    this.temporaryVoiceStates[stateKey] = replacementState;
    this.rememberTemporaryVoiceEvent(replaceAfter, record, replacementState.temporaryVoiceTag, { timing: "after_dialogue", replacedTemporaryVoiceTag: oldTagAfter });
    graphRemoteLog("temporary_voice_state_replace", { mode: "scene", reason: "audited_replace_after_dialogue", stateId: replacementState.stateId, roleName: replacementState.roleName, recordId: replacementState.recordId, replacedTemporaryVoiceTag: oldTagAfter, temporaryVoiceTag: replacementState.temporaryVoiceTag, temporaryVoiceAgeStage: replacementState.temporaryVoiceAgeStage, evidenceId: replaceAfter.evidenceId || "" });
    graphRemoteLog("temporary_voice_transition_applied", { stateId: replacementState.stateId, action: "replace", targetSeq: matchedSeq, timing: "after_dialogue", oldTemporaryVoiceTag: oldTagAfter, newTemporaryVoiceTag: replacementState.temporaryVoiceTag, evidenceId: replaceAfter.evidenceId || "" });
  }

  if (!acceptedOriginal.length) graphRemoteLog("voice_age_no_evidence", { name: graphNormalizeName(record.name || ""), recordId: record.recordId || "", keptAge: record.age || "", keptVoice: record.voice || "", voiceAgeVerified: record.voiceAgeVerified === true, voiceAgeProvisional: record.voiceAgeProvisional === true, temporaryStateActive: !!state, returnedTag: finalTag, reason: state ? "reuse_active_temporary_state_without_new_evidence" : "no_audited_voice_age_evidence_keep_record" });
  return { tag: finalTag || naturalTag || "default", naturalTag: naturalTag || "default", stateAction: lastTemporaryAction ? lastTemporaryAction.stateAction : (endBefore || endAfter ? "end" : (state ? "reuse" : "none")), acceptedEvidenceCount: acceptedOriginal.length };
};

CharacterManager.prototype.exportTemporaryVoiceSnapshot = function() {
  if (!this.temporaryVoiceStates) this.temporaryVoiceStates = {};
  if (!this.temporaryVoiceAppliedEvents) this.temporaryVoiceAppliedEvents = {};
  if (!ENABLE_TEMPORARY_VOICE_STATE) return null;
  var bookKey = graphV908CurrentBookKey(this);
  var chapterId = graphCurrentChapterId();
  var states = {};
  for (var key in this.temporaryVoiceStates) {
    if (!this.temporaryVoiceStates.hasOwnProperty(key)) continue;
    var state = this.temporaryVoiceStates[key] || {};
    if (state.bookKey === bookKey && state.chapterId === chapterId) states[key] = graphV908Clone(state, {});
  }
  var events = {};
  var eventKeys = Object.keys(this.temporaryVoiceAppliedEvents);
  for (var i = Math.max(0, eventKeys.length - 120); i < eventKeys.length; i++) {
    var event = this.temporaryVoiceAppliedEvents[eventKeys[i]] || {};
    if (event.bookKey === bookKey && event.chapterId === chapterId) events[eventKeys[i]] = graphV908Clone(event, {});
  }
  if (!Object.keys(states).length && !Object.keys(events).length) return null; // 中文注释：没有活动状态或临时事件时不制造空快照
  return { schema: "v908_temporary_voice_snapshot", bookKey: bookKey, chapterId: chapterId, states: states, events: events, savedAt: graphNowIso(), snapshotId: "temp_" + graphHash(bookKey + "|" + chapterId + "|" + JSON.stringify(states) + "|" + JSON.stringify(events)) };
};

CharacterManager.prototype.persistTemporaryVoiceSnapshotToDialogCache = function(currentDialogueText, cacheMeta) {
  try {
    var cache = readDialogCache();
    if (!ENABLE_TEMPORARY_VOICE_STATE) {
      if (cache.temporaryVoiceSnapshot) {
        cache.temporaryVoiceSnapshot = null;
        writeDialogCache(cache);
        graphRemoteLog("temporary_voice_feature_disabled", { ignoredTemporaryEvidenceCount: 0, clearedMemoryStateCount: Object.keys(this.temporaryVoiceStates || {}).length, ignoredCacheSnapshot: true, returnedNaturalVoice: true });
      }
      return false;
    }
    var snapshot = this.exportTemporaryVoiceSnapshot();
    if (!snapshot) {
      if (cache.temporaryVoiceSnapshot) {
        var clearedSnapshotId = cache.temporaryVoiceSnapshot.snapshotId || "";
        cache.temporaryVoiceSnapshot = null;
        if (writeDialogCache(cache)) graphRemoteLog("temporary_voice_cache_saved", { snapshotId: clearedSnapshotId, cleared: true, activeStateCount: 0, eventCount: 0, reason: "temporary_state_and_events_empty" });
      }
      return false;
    }
    var matchedIndex = Number(cacheMeta && cacheMeta.matchedCacheIndex || 0);
    var currentHash = graphHash(normalizeNameAnalysisDialogueText(currentDialogueText || ""));
    snapshot.lastProcessedIndex = matchedIndex;
    snapshot.lastDialogueHash = currentHash;
    snapshot.nextExpectedIndex = Number(cache.currentIndex || 0);
    var nextItem = snapshot.nextExpectedIndex > 0 && cache.dialogList ? cache.dialogList[snapshot.nextExpectedIndex - 1] : null;
    snapshot.nextExpectedDialogueHashes = [];
    if (nextItem) {
      var nextLines = String(nextItem.dialogContent || "").split("\n");
      for (var nli = 0; nli < nextLines.length; nli++) {
        var normalizedNextLine = normalizeNameAnalysisDialogueText(nextLines[nli] || "");
        if (!normalizedNextLine) continue;
        var nextLineHash = graphHash(normalizedNextLine);
        if (snapshot.nextExpectedDialogueHashes.indexOf(nextLineHash) === -1) snapshot.nextExpectedDialogueHashes.push(nextLineHash);
      }
    }
    snapshot.nextExpectedDialogueHash = snapshot.nextExpectedDialogueHashes.length ? snapshot.nextExpectedDialogueHashes[0] : "";
    cache.temporaryVoiceSnapshot = snapshot;
    if (writeDialogCache(cache)) graphRemoteLog("temporary_voice_cache_saved", { snapshotId: snapshot.snapshotId, bookKey: snapshot.bookKey, chapterId: snapshot.chapterId, activeStateCount: Object.keys(snapshot.states || {}).length, eventCount: Object.keys(snapshot.events || {}).length, lastProcessedIndex: snapshot.lastProcessedIndex, lastDialogueHash: snapshot.lastDialogueHash, nextExpectedIndex: snapshot.nextExpectedIndex, nextExpectedDialogueHash: snapshot.nextExpectedDialogueHash });
  } catch(e) { graphRemoteLog("temporary_voice_cache_restore_rejected", { reason: "cache_snapshot_write_exception", error: graphSafeString(e && e.message || e, 260) }); }
};

CharacterManager.prototype.tryRestoreTemporaryVoiceSnapshot = function(cacheMeta, currentDialogueText) {
  if (!ENABLE_TEMPORARY_VOICE_STATE || !ENABLE_TEMPORARY_VOICE_CACHE_RESTORE || !cacheMeta || !cacheMeta.temporaryVoiceSnapshot) return false;
  if (!this.temporaryVoiceRestoreAttempted) this.temporaryVoiceRestoreAttempted = {};
  var snapshot = cacheMeta.temporaryVoiceSnapshot || {};
  var incomingIndex = Number(cacheMeta.matchedCacheIndex || 0);
  var incomingHash = graphHash(normalizeNameAnalysisDialogueText(currentDialogueText || ""));
  var attemptKey = graphSafeString(snapshot.snapshotId || "", 120) + "|" + incomingIndex + "|" + incomingHash;
  if (this.temporaryVoiceRestoreAttempted[attemptKey]) return false;
  this.temporaryVoiceRestoreAttempted[attemptKey] = true;
  var bookOk = snapshot.bookKey === graphV908CurrentBookKey(this);
  var chapterOk = snapshot.chapterId === graphCurrentChapterId();
  var sameLineOk = incomingIndex === Number(snapshot.lastProcessedIndex || 0) && incomingHash === snapshot.lastDialogueHash;
  var nextHashes = Array.isArray(snapshot.nextExpectedDialogueHashes) ? snapshot.nextExpectedDialogueHashes : (snapshot.nextExpectedDialogueHash ? [snapshot.nextExpectedDialogueHash] : []);
  var nextLineOk = incomingIndex === Number(snapshot.nextExpectedIndex || 0) && nextHashes.indexOf(incomingHash) !== -1;
  var schemaOk = snapshot.schema === "v908_temporary_voice_snapshot";
  if (!schemaOk || !bookOk || !chapterOk || (!sameLineOk && !nextLineOk)) {
    graphRemoteLog("temporary_voice_cache_restore_rejected", { snapshotId: snapshot.snapshotId || "", schemaOk: schemaOk, bookOk: bookOk, chapterOk: chapterOk, sameLineOk: sameLineOk, nextLineOk: nextLineOk, snapshotBookKey: snapshot.bookKey || "", currentBookKey: graphV908CurrentBookKey(this), snapshotChapterId: snapshot.chapterId || "", currentChapterId: graphCurrentChapterId(), incomingIndex: incomingIndex, incomingHash: incomingHash, lastProcessedIndex: snapshot.lastProcessedIndex || 0, lastDialogueHash: snapshot.lastDialogueHash || "", nextExpectedIndex: snapshot.nextExpectedIndex || 0, nextExpectedDialogueHash: snapshot.nextExpectedDialogueHash || "", reason: "strict_book_chapter_sequence_hash_guard_failed" });
    return false;
  }
  if (Object.keys(this.temporaryVoiceStates || {}).length > 0) return false;
  this.temporaryVoiceStates = graphV908Clone(snapshot.states || {}, {}) || {};
  this.temporaryVoiceAppliedEvents = graphV908Clone(snapshot.events || {}, {}) || {};
  graphRemoteLog("temporary_voice_cache_restore", { snapshotId: snapshot.snapshotId || "", bookKey: snapshot.bookKey || "", chapterId: snapshot.chapterId || "", restoreMode: sameLineOk ? "same_dialogue_replay" : "next_continuous_dialogue", incomingIndex: incomingIndex, incomingHash: incomingHash, activeStateCount: Object.keys(this.temporaryVoiceStates).length, eventCount: Object.keys(this.temporaryVoiceAppliedEvents).length });
  return true;
};

// 包装姓名分析：把当前序号的审计结果传给别名归并后的统一出口。
var graphV908OldAnalyzeCharacter = CharacterManager.prototype.analyzeCharacter;
CharacterManager.prototype.analyzeCharacter = function(fullText, characterId, allDialogues) {
  var result = graphV908OldAnalyzeCharacter ? graphV908OldAnalyzeCharacter.apply(this, arguments) : null;
  if (!this._v908LastAnalysisByCharacterId) this._v908LastAnalysisByCharacterId = {};
  var currentText = "";
  for (var i = 0; allDialogues && i < allDialogues.length; i++) if (allDialogues[i] && allDialogues[i].id === characterId) { currentText = allDialogues[i].text || ""; break; }
  this._v908LastAnalysisByCharacterId[String(characterId || "")] = { analysis: result, currentDialogueText: currentText, capturedAt: Date.now() };
  return result;
};

// 包装角色处理：无论原链路从哪个已有角色分支早退，最终都统一应用自然年龄审计和临时状态。
var graphV908OldProcessCharacter = CharacterManager.prototype.processCharacter;
CharacterManager.prototype.processCharacter = function(fullText, characterId, allDialogues, chapterFullContent) {
  var result = graphV908OldProcessCharacter ? graphV908OldProcessCharacter.apply(this, arguments) : null;
  var key = String(characterId || "");
  var context = this._v908LastAnalysisByCharacterId && this._v908LastAnalysisByCharacterId[key] ? this._v908LastAnalysisByCharacterId[key] : null;
  try { if (this._v908LastAnalysisByCharacterId) delete this._v908LastAnalysisByCharacterId[key]; } catch(e0) {}
  if (!context || !context.analysis || context.analysis.__safeDialogueFallback) return result;
  // 若本句未触发别名检验，则在统一出口执行B+C；若别名流程已经缓冲年龄结果，这里会自动跳过重复请求。
  try { if (this.resolvePendingAgeAndGraphWithoutAliasV908) this.resolvePendingAgeAndGraphWithoutAliasV908(chapterFullContent || fullText || ""); } catch(v908ResolveErr) {
    graphRemoteLog("combined_audit_retry_exhausted", { flow: "voice_age+graph", reason: "process出口合并审计异常", error: graphSafeString(v908ResolveErr && v908ResolveErr.message || v908ResolveErr, 320) });
  }
  var hadBufferedCombinedAudit = !!(this._v908BufferedGraphAudit || this._v908BufferedVoiceAgeAudit);
  // 旧角色流程此时已经完成别名合并/新建；现在才提交图谱证据，避免图谱先落地后又被旧别名分支反向覆盖。
  if (this._v908BufferedGraphAudit && this.commitPendingGraphAuditV908) this.commitPendingGraphAuditV908(this._v908BufferedGraphAudit.module, chapterFullContent || fullText || "", this._v908BufferedGraphAudit.source || "process_after_alias");
  if (!result || !result.characterInfo) {
    // 即使没有可应用的角色卡，也要把本批年龄审计结果写回缓存，防止后续命中时重复审计。
    if (this._v908BufferedVoiceAgeAudit && this.commitPendingVoiceAgeAuditV908) this.commitPendingVoiceAgeAuditV908(this._v908BufferedVoiceAgeAudit.module, this._v908BufferedVoiceAgeAudit.source || "process_without_record");
    return result;
  }
  // 图谱可能在审计提交后合并/拆分角色，必须重新解析最终稳定记录，再允许年龄证据落地。
  var stableRecord = this.findCharacterRecord && context.analysis && context.analysis.name ? this.findCharacterRecord(context.analysis.name) : null;
  if (stableRecord) result.characterInfo = stableRecord;
  if (hadBufferedCombinedAudit && this.traceCombinedAuditCommitV908) this.traceCombinedAuditCommitV908("stableRecord", { name: graphNormalizeName(result.characterInfo && result.characterInfo.name || ""), recordId: result.characterInfo ? v87EnsureRecordId(this, result.characterInfo) : "", characterId: key });
  if (this._v908BufferedVoiceAgeAudit && this.commitPendingVoiceAgeAuditV908) this.commitPendingVoiceAgeAuditV908(this._v908BufferedVoiceAgeAudit.module, this._v908BufferedVoiceAgeAudit.source || "process_stable_record");
  var applied = this.applyAuditedVoiceAgeForDialogue(result.characterInfo, context.analysis, context.currentDialogueText || result.text || "", characterId);
  result.tag = applied && applied.tag ? applied.tag : (result.characterInfo.voice || result.tag || "default");
  this.saveRecords();
  if (this.persistTemporaryVoiceSnapshotToDialogCache) this.persistTemporaryVoiceSnapshotToDialogCache(context.currentDialogueText || result.text || "", context.analysis.__dialogCacheMeta || {});
  return result;
};

// -------------------------- 模块导出（手机端ES5兼容） --------------------------
if (typeof module !== 'undefined' && module.exports) {
  module.exports = SpeechRuleJS;
} else {
  this.SpeechRuleJS = SpeechRuleJS;
}

function setFixedVoice(characterName) {
  if (!characterManager) return "❌ 角色管理器未初始化";
  var charName = characterName ? characterName.toString().trim() : "";
  if (!charName) return "❌ 角色名不能为空";
  var record = characterManager.findCharacterRecord(charName);
  if (record) {
      record.usageCount = 100;
      graphV908MarkFixedVoiceRecord(record, "manual_setFixedVoice", true);
      if (characterManager.enforceFixedVoiceRecordV908) characterManager.enforceFixedVoiceRecordV908(record, "manual_setFixedVoice");
      characterManager.saveRecords();
      return "✅ 已硬锁" + charName + "发音人：" + record.voice.toString() + "。自然年龄变化与临时换声都不会改动该音色，直到手动取消固定。";
  } else {
      return "❌ 未找到角色：" + charName;
  }
}

function cancelFixedVoice(characterName) {
  if (!characterManager) return "❌ 角色管理器未初始化";
  var charName = characterName ? characterName.toString().trim() : "";
  if (!charName) return "❌ 角色名不能为空";
  var record = characterManager.findCharacterRecord(charName);
  if (!record) return "❌ 未找到角色：" + charName;
  var oldFixedTag = graphV908FixedVoiceTagOfRecord(record) || record.voice || "";
  var stateInfo = characterManager.findTemporaryVoiceStateForRecord ? characterManager.findTemporaryVoiceStateForRecord(record) : null;
  if (stateInfo && stateInfo.key && characterManager.endTemporaryVoiceState) characterManager.endTemporaryVoiceState(stateInfo.key, "manual_fixed_voice_cancelled", null);
  graphV908CancelFixedVoiceRecord(record);
  characterManager.saveRecords();
  graphRemoteLog("fixed_voice_cancelled", { name: graphNormalizeName(record.name || charName), recordId: record.recordId || "", oldFixedVoiceTag: oldFixedTag, currentVoice: record.voice || "", reason: "用户手动取消固定音色" });
  return "✅ 已取消" + charName + "的固定音色。后续审计通过的自然年龄变化或临时换声可以重新生效。";
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
      for (var num = 1; num <= 100; num++) {
          var tagKey = ("localSound" + num).toString();
          var tagName = ("本地音效" + num).toString();
          SpeechRuleJS.tags[tagKey] = tagName;
      }
  }
})();
