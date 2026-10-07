package com.github.jing332.tts_server_android.service.systts.help

/**
 * 渠道注册表装配（10-09 全渠道批）：App 启动时调一次 install()，各渠道实现挂进注册表。
 * Account.provider → ChatChannel 路由；provider 值与 gitee 插件 id 一致（排障对照）。
 *
 * 未接入渠道：qoder 对话（WASM 锁死，available=false 显示「待接入」）；
 * codearts/lobsterai/trae/raccoon/loomy/gemini 的「登录流程」按各渠道引擎逐步接 UI，
 * 本批先保证凭据在池内时的 续期/签到/余额/对话 四件事可用。
 */
object ChannelBootstrap {
    private var installed = false

    fun install() {
        if (installed) return
        installed = true
        ChatChannels.register(WorkbuddyChannel)
        ChatChannels.register(OpencodeChannel)
        ChatChannels.register(ClineChannel)
        ChatChannels.register(ZcodeChannel)
        ChatChannels.register(MinimaxChannel)
        ChatChannels.register(LobsteraiChannel)
        ChatChannels.register(TraeChannel)
        ChatChannels.register(GeminiChannel)
        ChatChannels.register(CodeartsChannel)
        ChatChannels.register(LoomyChannel)
        ChatChannels.register(RaccoonChannel)
        ChatChannels.register(QoderChannel)
    }
}
