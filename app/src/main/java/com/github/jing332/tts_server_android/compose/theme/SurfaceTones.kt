package com.github.jing332.tts_server_android.compose.theme

import androidx.compose.ui.graphics.Color

// 10 套静态主题的 surfaceContainer 七槽（2026-10-11 Radix 版，勿手改；生成器=临时文件/gen_radix_tones.py）。
// 数据源=@radix-ui/colors 3.0.0（MIT，shadcn/ui 同款）sRGB 主值，2026-10-11 抓取。
// 浅色映射（用户 10-11 拍板「标签 step4、卡底保持选中的绿」）：
//   Lowest=accent step1（页底）Low=accent step2（卡底，GREEN=#F4FBF6≈旧#F2F8F4）
//   Container/High/Highest=配对中性灰 step3/4/5（sage/mauve/slate/sand，结构与嵌套层不带 accent 彩度）
//   Dim=中性 step3，Bright=accent step1。
// 标签/选中档不在本表：Color2 浅色 secondaryContainer=accent step4，softContainerColor() 取之。
// 深色表维持 10-10 版未动（官方 baseline 锚 background；深色未装机验收过，不随本批盲改）。
// 列表顺序固定：[lowest, low, container, high, highest, dim, bright]，消费方=Theme.kt themedNeutral()。

internal val lightSurfaceTones: Map<AppTheme, List<Color>> = mapOf(
    AppTheme.DEFAULT to listOf(Color(0xFFFBFEFD), Color(0xFFF4FBF7), Color(0xFFEEF1F0), Color(0xFFE6E9E8), Color(0xFFDFE2E0), Color(0xFFEEF1F0), Color(0xFFFBFEFD)),
    AppTheme.GREEN to listOf(Color(0xFFFBFEFC), Color(0xFFF4FBF6), Color(0xFFEEF1F0), Color(0xFFE6E9E8), Color(0xFFDFE2E0), Color(0xFFEEF1F0), Color(0xFFFBFEFC)),
    AppTheme.RED to listOf(Color(0xFFFFFCFC), Color(0xFFFFF7F7), Color(0xFFF2EFF3), Color(0xFFEAE7EC), Color(0xFFE3DFE6), Color(0xFFF2EFF3), Color(0xFFFFFCFC)),
    AppTheme.PINK to listOf(Color(0xFFFFFCFE), Color(0xFFFEF7FB), Color(0xFFF2EFF3), Color(0xFFEAE7EC), Color(0xFFE3DFE6), Color(0xFFF2EFF3), Color(0xFFFFFCFE)),
    AppTheme.BLUE to listOf(Color(0xFFFBFDFF), Color(0xFFF4FAFF), Color(0xFFF0F0F3), Color(0xFFE8E8EC), Color(0xFFE0E1E6), Color(0xFFF0F0F3), Color(0xFFFBFDFF)),
    AppTheme.CYAN to listOf(Color(0xFFFAFDFE), Color(0xFFF2FAFB), Color(0xFFF0F0F3), Color(0xFFE8E8EC), Color(0xFFE0E1E6), Color(0xFFF0F0F3), Color(0xFFFAFDFE)),
    AppTheme.ORANGE to listOf(Color(0xFFFEFCFB), Color(0xFFFFF7ED), Color(0xFFF1F0EF), Color(0xFFE9E8E6), Color(0xFFE2E1DE), Color(0xFFF1F0EF), Color(0xFFFEFCFB)),
    AppTheme.PURPLE to listOf(Color(0xFFFEFCFE), Color(0xFFFBF7FE), Color(0xFFF2EFF3), Color(0xFFEAE7EC), Color(0xFFE3DFE6), Color(0xFFF2EFF3), Color(0xFFFEFCFE)),
    AppTheme.BROWN to listOf(Color(0xFFFEFDFC), Color(0xFFFCF9F6), Color(0xFFF1F0EF), Color(0xFFE9E8E6), Color(0xFFE2E1DE), Color(0xFFF1F0EF), Color(0xFFFEFDFC)),
    AppTheme.GRAY to listOf(Color(0xFFFCFCFC), Color(0xFFF9F9F9), Color(0xFFF0F0F0), Color(0xFFE8E8E8), Color(0xFFE0E0E0), Color(0xFFF0F0F0), Color(0xFFFCFCFC)),
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
