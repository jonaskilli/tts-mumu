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

    /** 模型清单（拉取或静态表；失败返回空表由调用方兜底） */
    fun fetchModels(accessToken: String): List<String>

    enum class ErrClass { RATE_LIMIT, AUTH, OTHER }
}

/** 渠道注册表：provider 字符串 → 实现。新渠道在这里挂一行。 */
object ChatChannels {
    private val registry = linkedMapOf<String, ChatChannel>()

    fun register(channel: ChatChannel) {
        registry[channel.id] = channel
    }

    fun byProvider(provider: String): ChatChannel? = registry[provider]

    fun all(): List<ChatChannel> = registry.values.toList()
}
