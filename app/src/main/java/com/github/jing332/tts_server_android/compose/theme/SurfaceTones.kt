package com.github.jing332.tts_server_android.compose.theme

import androidx.compose.ui.graphics.Color

// 10 套静态主题的 surfaceContainer 七槽（2026-10-10 生成，勿手改；生成器=临时文件/gen_surface_tones.py）。
// 浅色=DSH 式纯白底（用户拍板，参考 deepseek-harness）：lowest/low/container/bright=纯白（卡片/底栏与页底
//   同白，层级靠发丝线边框），high/highest/dim=无彩灰 96/94/92（嵌套块/搜索框底）。色相不进浅色容器。
// 深色=M3 官方 baseline 暗色表 4/10/12/17/22/6/24，整表锚到各主题 background，色相取 primary（彩度≤3）。
// 列表顺序固定：[lowest, low, container, high, highest, dim, bright]，消费方=Theme.kt themedNeutral()。

internal val lightSurfaceTones: Map<AppTheme, List<Color>> = mapOf(
    AppTheme.DEFAULT to listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFF3F3F3), Color(0xFFEEEEEE), Color(0xFFE8E8E8), Color(0xFFFFFFFF)),
    AppTheme.GREEN to listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFF3F3F3), Color(0xFFEEEEEE), Color(0xFFE8E8E8), Color(0xFFFFFFFF)),
    AppTheme.RED to listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFF3F3F3), Color(0xFFEEEEEE), Color(0xFFE8E8E8), Color(0xFFFFFFFF)),
    AppTheme.PINK to listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFF3F3F3), Color(0xFFEEEEEE), Color(0xFFE8E8E8), Color(0xFFFFFFFF)),
    AppTheme.BLUE to listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFF3F3F3), Color(0xFFEEEEEE), Color(0xFFE8E8E8), Color(0xFFFFFFFF)),
    AppTheme.CYAN to listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFF3F3F3), Color(0xFFEEEEEE), Color(0xFFE8E8E8), Color(0xFFFFFFFF)),
    AppTheme.ORANGE to listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFF3F3F3), Color(0xFFEEEEEE), Color(0xFFE8E8E8), Color(0xFFFFFFFF)),
    AppTheme.PURPLE to listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFF3F3F3), Color(0xFFEEEEEE), Color(0xFFE8E8E8), Color(0xFFFFFFFF)),
    AppTheme.BROWN to listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFF3F3F3), Color(0xFFEEEEEE), Color(0xFFE8E8E8), Color(0xFFFFFFFF)),
    AppTheme.GRAY to listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFF3F3F3), Color(0xFFEEEEEE), Color(0xFFE8E8E8), Color(0xFFFFFFFF)),
)

internal val darkSurfaceTones: Map<AppTheme, List<Color>> = mapOf(
    AppTheme.DEFAULT to listOf(Color(0xFF161814), Color(0xFF222420), Color(0xFF262824), Color(0xFF31332F), Color(0xFF3C3E3A), Color(0xFF1A1C18), Color(0xFF40433E)),
    AppTheme.GREEN to listOf(Color(0xFF141815), Color(0xFF202421), Color(0xFF242925), Color(0xFF2F3330), Color(0xFF3A3E3B), Color(0xFF181C19), Color(0xFF3E433F)),
    AppTheme.RED to listOf(Color(0xFF1C1615), Color(0xFF282221), Color(0xFF2C2625), Color(0xFF373130), Color(0xFF423C3B), Color(0xFF201A19), Color(0xFF474140)),
    AppTheme.PINK to listOf(Color(0xFF1C1617), Color(0xFF282223), Color(0xFF2C2627), Color(0xFF373132), Color(0xFF423C3D), Color(0xFF201A1B), Color(0xFF474142)),
    AppTheme.BLUE to listOf(Color(0xFF16171B), Color(0xFF222328), Color(0xFF26282C), Color(0xFF313237), Color(0xFF3C3D42), Color(0xFF1A1B1F), Color(0xFF404246)),
    AppTheme.CYAN to listOf(Color(0xFF131917), Color(0xFF1F2524), Color(0xFF232928), Color(0xFF2D3432), Color(0xFF383F3D), Color(0xFF171D1B), Color(0xFF3D4342)),
    AppTheme.ORANGE to listOf(Color(0xFF1B1714), Color(0xFF272320), Color(0xFF2B2724), Color(0xFF36322F), Color(0xFF423D3A), Color(0xFF1F1B18), Color(0xFF46423E)),
    AppTheme.PURPLE to listOf(Color(0xFF19171B), Color(0xFF252327), Color(0xFF29272B), Color(0xFF343236), Color(0xFF3F3D41), Color(0xFF1D1B1F), Color(0xFF434246)),
    AppTheme.BROWN to listOf(Color(0xFF1B1614), Color(0xFF282220), Color(0xFF2C2625), Color(0xFF37312F), Color(0xFF423C3A), Color(0xFF1F1A18), Color(0xFF47413F)),
    AppTheme.GRAY to listOf(Color(0xFF13181B), Color(0xFF1F2427), Color(0xFF23292B), Color(0xFF2E3336), Color(0xFF393F41), Color(0xFF171C1F), Color(0xFF3E4346)),
)
