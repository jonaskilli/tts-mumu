package com.github.jing332.tts_server_android.service.systts.help

/**
 * 多渠道账号池抽象（10-09 全渠道移植，蓝本=gitee deepseek-harness-codearts 插件）。
 *
 * 设计（照插件 product.ts/xx-adapter.ts 分层）：
 *  - 每渠道一个实现 object，只管「协议」：登录流程、续期、签到/余额、对话请求的
 *    头族/请求体变换/响应解析差异、模型清单；
 *  - 账号存储/轮换状态机/静默续期调度/池页 UI 全部渠道无关（AccountPool/SseAggregator/
 *    AccountRefreshScheduler/AccountPoolScreen 共用），按 Account.provider 路由到实现；
 *  - CodeBuddy 不迁进本接口（AccountPool 内硬编码协议已实测稳定，避免为抽象而抽象
 *    触碰已验证代码）；其余渠道从这里长出来。provider 值与插件 id 一致（便于对照排障）。
 */
interface ChatChannel {
    /** 渠道 id（= Account.provider，照插件：zcode/qoder/lobsterai/...） */
    val id: String

    /** 用户可见渠道名（池页/日志用） */
    val displayName: String

    /** 对话端点基址（版本段含全；密钥 value 网址段用它） */
    val chatBaseUrl: String

    /** 是否已支持（探查期可先注册占位，UI 显示「待接入」） */
    val available: Boolean get() = true

    /**
     * 对话请求头族（鉴权头之外的身份字段；Authorization 由调用方按凭据加）。
     * 空表=无特殊头。
     */
    fun chatHeaders(accessToken: String): Map<String, String> = emptyMap()

    /**
     * 请求体变换（加思考字段/资源通道/机器指纹等渠道专属字段）。
     * 默认原样透传。入参 JSON 串，出参 JSON 串。
     */
    fun patchBody(bodyJson: String, model: String): String = bodyJson

    /**
     * 按-请求注头（10-08 钩子落地）：需要 model 的头族（如 AutoClaw 的
     * X-Request-Model: {model}）在这里返回，SseAggregator 每次对话请求时合并进
     * extraHeaders。默认空表=无按请求头。
     */
    fun perRequestHeaders(model: String): Map<String, String> = emptyMap()

    /**
     * 响应解析：非流式 JSON 串 → 标准 OpenAI 形（choices[0].message.content）。
     * 默认原样（本来就标准）；Anthropic 族的渠道在此翻译。
     */
    fun parseResponse(body: String): String = body

    /**
     * 错误分类（SseAggregator 轮换状态机用）：
     * 返回 RATE_LIMIT=记限流标记+换号 / AUTH=续期+换号 / OTHER=直接报。
     */
    fun classifyError(httpStatus: Int, body: String): ErrClass

    /** 限流兜底时长（ms）——渠道各自的兜底口径 */
    val rateLimitFallbackMs: Long get() = 3_600_000L

    /** 续期：成功返回更新后的 token 对 (access, refresh, expiresAt)，失败返回 null+错误 */
    fun refresh(acc: AccountPool.Account): Triple<String, String, Long>?

    /** 签到（无签到能力的渠道返回 false to「无签到接口」） */
    fun checkIn(acc: AccountPool.Account): Pair<Boolean, String> = false to "该渠道无签到接口"

    /** 余额（返回 NaN=不支持；正常返回积分/额度数值） */
    fun queryCredits(acc: AccountPool.Account): Double = Double.NaN

    /**
     * 余额明细（10-10 移植插件「长期/临时」两桶）：**一次请求**同时给出合计与两桶，
     * 避免 UI 先问合计再问分池发两次请求。
     * 距扣费截止不足 15 天的算临时（再不用就作废、优先消耗），其余算长期
     * （窗口与插件同值，见 AccountPool.CREDIT_EXPIRING_WINDOW_MS）。
     * 返回 null = 该渠道没有「会不会作废」这个维度（默认；UI 不显示分池行，
     * 走 queryCredits 单值即可）。
     */
    fun queryCreditDetail(acc: AccountPool.Account): CreditDetail? = null

    /** 模型清单（拉取或静态表；失败返回空表由调用方兜底） */
    fun fetchModels(accessToken: String): List<String>

    /**
     * 渠道自管对话（10-10 qoder WASM 引入）：加密端点 URL/鉴权（WASM 生成的
     * COSY 签名头）与通用 /chat/completions 形状完全不同，通用 chatCompletion
     * 无法承载。返回 null=本渠道不走自管路径（默认，通用流处理）；非 null=
     * 已完成一次对话，语义与 chatCompletion 对齐（ok=true 时 second 为聚合回答文本）。
     */
    fun chatViaChannel(
        acc: AccountPool.Account,
        bodyJson: String,
        model: String,
        cancelled: SseAggregator.Cancelled = SseAggregator.Cancelled { false },
    ): Pair<Boolean, String>? = null

    enum class ErrClass { RATE_LIMIT, AUTH, OTHER }
}

/**
 * 余额明细（10-10「长期/临时」分池）：合计 + 两桶。
 * permanent = 距扣费截止 ≥15 天（或拿不到到期时刻）的部分；
 * ephemeral = 距扣费截止 <15 天、再不用就作废的部分。
 */
data class CreditDetail(val total: Double, val permanent: Double, val ephemeral: Double)

/** 渠道注册表：provider 字符串 → 实现。新渠道在这里挂一行。 */
object ChatChannels {
    private val registry = linkedMapOf<String, ChatChannel>()

    fun register(channel: ChatChannel) {
        registry[channel.id] = channel
    }

    fun byProvider(provider: String): ChatChannel? = registry[provider]

    fun all(): List<ChatChannel> = registry.values.toList()

    /**
     * 按上游基址反查渠道（10-11 手动「拉取模型」渠道路由用）。
     * 渠道专协议上游（autoclaw/workbuddy/zcode 等）没有通用 GET /models 端点，
     * 手动拉取若不路由到 ChatChannel.fetchModels 必然 404（真机实锤）。
     * 归一=去尾斜杠逐字比较（chatBaseUrl 与分组 baseUrl 同源，够用；不引 KeyListFile
     * 防循环依赖）。
     */
    fun byBaseUrl(baseUrl: String): ChatChannel? {
        val norm = baseUrl.trim().trimEnd('/')
        return registry.values.firstOrNull { it.chatBaseUrl.trim().trimEnd('/') == norm }
    }
}
