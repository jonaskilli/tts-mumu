package com.github.jing332.deepseekproxy.proxy

/**
 * DeepSeek PoW 求解入口。
 *
 * 实际计算交给 [DeepSeekPowWebView]——它用 App 内已启用的 WebView（Chromium/V8 内核，
 * 原生支持 WebAssembly）直接加载 DeepSeek 官方 wasm（sha3_wasm_bg.wasm），复用与网页端
 * 一致的 `wasm_solve` 逻辑。这样无需在 Android 上重写 keccak，也无需任何额外依赖。
 *
 * 算法（与官方网页端、wasm 一致）：
 *   prefix   = salt + "_" + expireAt + "_"
 *   从 answer=0 起递增，直到
 *     keccak256(challenge + prefix + str(answer)) < 2^256 / difficulty
 *
 * 调用前请确保已执行过 [DeepSeekPowWebView.init]（见 ProxyService）。
 */
object DeepSeekPow {
    suspend fun solve(challenge: String, salt: String, expireAt: Long, difficulty: Int): Long {
        return DeepSeekPowWebView.solve(challenge, salt, expireAt, difficulty)
    }
}
