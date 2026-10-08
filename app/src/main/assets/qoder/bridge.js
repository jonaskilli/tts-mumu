/**
 * Qoder WASM 桥 —— window.QoderWasm 接口实现（供 Kotlin WebView 桥调用）。
 *
 * 调用约定：evaluateJavascript 不能 await Promise，所有方法异步完成后经
 * window.AndroidBridge.onResult(callId, json) 回传；结果 JSON 形如
 * {"ok":true,"data":...} 或 {"ok":false,"error":"..."}。
 *
 * prepareInfer 的明文 payload（buildQoderInferPayload）从插件 qoder-adapter.ts 移植：
 * 逐项复刻官方 G4A()，model_config 10 字段齐、business 缺失被路由到故障节点
 * （2026-09-20 实锤：缺 business 时 qfmodel 恒落故障节点 [FAIL]node）。
 */
'use strict';

/** 朗读场景客户端元数据（qoder-product.js cli 身份，照抄）。 */
const QODER_CLIENT_METADATA = {
    client_type: '5',
    business_product: 'cli',
    business_type: 'agent',
    scene: 'assistant',
};
/** 加密端点 host（agent_chat_generation 所在）。 */
const QODER_ENCRYPTED_INFER_BASE = 'https://api2.qoder.sh';

/**
 * 构造加密端点明文请求体（插件 buildQoderInferPayload 移植）。
 * ask: {modelKey, userText, systemText?, isReasoning?, history?, maxTokens?,
 *       reasoningEffort?, contextWindow?, displayName?, maxInputTokens?, isVl?, source?}
 */
function qoderBuildInferPayload(ask, requestId) {
    const isReasoning = ask.isReasoning ?? false;
    const text = ask.userText;
    const parameters = {};
    if (ask.maxTokens !== undefined) parameters.max_tokens = ask.maxTokens;
    if (ask.reasoningEffort !== undefined) {
        parameters.reasoning_effort = ask.reasoningEffort;
        parameters.enable_thinking = ask.reasoningEffort !== 'none';
    }
    if (ask.contextWindow !== undefined) parameters.context_length = ask.contextWindow;
    const messages = [];
    for (const m of ask.history ?? []) {
        // 逐字段搬运：只保留协议认识的键，避免把内部字段原样发给上游
        messages.push({
            role: m.role,
            content: m.content,
        });
    }
    if (messages.length === 0) messages.push({ role: 'user', content: text });
    return {
        request_id: requestId,
        request_set_id: requestId,
        chat_record_id: requestId,
        session_id: crypto.randomUUID(),
        stream: true,
        chat_task: 'FREE_INPUT',
        chat_context: {
            text: text,
            features: [],
            extra: {
                context: [],
                modelConfig: { key: ask.modelKey, is_reasoning: isReasoning },
                originalContent: text,
            },
            chatPrompt: '',
            imageUrls: null,
        },
        is_reply: true,
        is_retry: false,
        source: 1,
        version: '3',
        agent_id: 'agent_common',
        task_id: 'common',
        session_type: 'qodercli',
        aliyun_user_type: '',
        model_config: {
            key: ask.modelKey,
            // 官方 Uyc() 的 model_config 有 10 个字段，逐项对齐（早期只传 6 个）
            display_name: ask.displayName ?? '',
            model: '',
            format: 'openai',
            is_vl: ask.isVl ?? true,
            is_reasoning: isReasoning,
            api_key: '',
            url: '',
            source: ask.source ?? 'system',
            max_input_tokens: ask.maxInputTokens ?? ask.contextWindow ?? 200000,
        },
        custom_model: null,
        system: ask.systemText ? [{ type: 'text', text: ask.systemText }] : [],
        messages: messages,
        // 无工具时空数组而非缺字段（与客户端一致）
        tools: [],
        parameters: parameters,
        // business 决定服务端路由（sec_scan → 安全池，其余 → 默认池）；缺字段走异常分支
        business: { type: 'agent' },
    };
}

/** WebView 桥运行时：上下文句柄表 + callId 派发。 */
const qoderBridge = {
    contexts: new Map(),
    nextCtxId: 1,
    nextCallId: 1,

    _reply(callId, fn) {
        (async () => {
            try {
                const data = await fn();
                window.AndroidBridge.onResult(String(callId), JSON.stringify({ ok: true, data: data }));
            } catch (e) {
                window.AndroidBridge.onResult(String(callId), JSON.stringify({ ok: false, error: (e && e.message) ? e.message : String(e) }));
            }
        })();
    },
};

/** Kotlin 调用的统一入口：window.QoderWasm.call(method, argsJson, callId)。 */
window.QoderWasm = {
    /**
     * 同步派发入口（evaluateJavascript 调用后立即返回，结果走 onResult 回调）。
     * args 为 JSON 对象（非字符串），字段按 method 而异。
     */
    call(method, args, callId) {
        switch (method) {
            case 'ready':
                qoderBridge._reply(callId, async () => {
                    await qoderGetGlue();
                    return { version: QODER_COSY_VERSION };
                });
                return true;
            case 'generateAuthFields':
                qoderBridge._reply(callId, async () => {
                    return await qoderGenerateRuntimeAuthFields({
                        uid: args.uid,
                        securityOauthToken: args.securityOauthToken,
                    });
                });
                return true;
            case 'createContext':
                qoderBridge._reply(callId, async () => {
                    const client = await QoderEncryptedInfer.create({
                        user: { uid: args.uid, securityOauthToken: args.securityOauthToken },
                        machineId: args.machineId,
                        metadata: QODER_CLIENT_METADATA,
                        host: QODER_ENCRYPTED_INFER_BASE,
                    });
                    const ctxId = String(qoderBridge.nextCtxId++);
                    qoderBridge.contexts.set(ctxId, client);
                    return { ctxId: ctxId };
                });
                return true;
            case 'prepareInfer':
                qoderBridge._reply(callId, async () => {
                    const client = qoderBridge.contexts.get(args.ctxId);
                    if (!client) throw new Error('ctx not found: ' + args.ctxId);
                    const requestId = crypto.randomUUID();
                    const payload = qoderBuildInferPayload({
                        modelKey: args.modelKey,
                        userText: args.userText,
                        isReasoning: args.isReasoning === true,
                        history: args.history,
                        maxTokens: args.maxTokens,
                        source: args.source,
                    }, requestId);
                    const r = client.prepareInfer({
                        payload: payload,
                        modelKey: args.modelKey,
                        source: args.source ?? 'system',
                    });
                    return { url: r.url, headers: r.headers, body: r.body };
                });
                return true;
            case 'decryptModelCatalog':
                qoderBridge._reply(callId, async () => {
                    return await qoderDecryptModelCatalog(args.encrypted, args.machineId);
                });
                return true;
            case 'dropContext':
                qoderBridge.contexts.delete(String(args.ctxId));
                return true;
            default:
                window.AndroidBridge.onResult(String(callId), JSON.stringify({ ok: false, error: 'unknown method: ' + method }));
                return false;
        }
    },
};

window.__qoderBridgeReady = true;
