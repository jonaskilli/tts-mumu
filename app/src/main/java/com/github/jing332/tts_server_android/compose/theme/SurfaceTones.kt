package com.github.jing332.tts_server_android.compose.theme

import androidx.compose.ui.graphics.Color

// 10 套静态主题的 surfaceContainer 七槽（2026-10-11 晚 V1 版，勿手改；生成器=临时文件/gen_radix_tones_v1.py）。
// 数据源=@radix-ui/colors 3.0.0（MIT，shadcn/ui 同款）sRGB 主值。
// V1（用户拍板「按推荐的来」，修 Radix 版结构层发灰发暗）：浅色结构层弃配对中性灰，
//   改随主题 accent 淡彩——比卡底深一档、比标签(step4)浅一档，全页回到带主题色的清亮：
//   Lowest=step1（页底） Low=step2（卡底） Container=step3（弹窗底/搜索框/次级分组条）
//   High=step3→step4 六成混白（主分组条深槽） Highest=step4→step5 三成混白（最深嵌套槽）
//   Dim=step3 Bright=step1。
// 10-12 L2（用户实拍「卡片色丑」挑定）：**只把 GREEN 一套**的三个结构槽整体提亮一档
//   （container E6F6EB→EAF7EF、high DCF3E4→E2F4E8、highest D1EEDB→DAF1E1），
//   三级台阶 3.24/3.13/3.15 全部保留（app 自己接受的最小台阶=页底→卡片 2.52）。
//   其余 9 套**没动**：它们的级差本来就窄（DEFAULT 3.01、GRAY 1.75、BROWN 2.37），
//   跟着压会连区域都分不出；要用那几套时按同一口径单独算。
// 标签/选中档不在本表：Color2 浅色 secondaryContainer=accent step4，softContainerColor() 取之（不动）。
// 深色表维持 10-10 版未动（未装机验收过，不随本批盲改）。
// 列表顺序固定：[lowest, low, container, high, highest, dim, bright]，消费方=Theme.kt themedNeutral()。

internal val lightSurfaceTones: Map<AppTheme, List<Color>> = mapOf(
    AppTheme.DEFAULT to listOf(Color(0xFFFBFEFD), Color(0xFFF4FBF7), Color(0xFFE6F7ED), Color(0xFFDCF3E7), Color(0xFFD0EFDF), Color(0xFFE6F7ED), Color(0xFFFBFEFD)),
    // 10-13 晚（用户挑色「A·微深，我想比这个再浅一点」）：GREEN 卡底（Low 槽）#F4FBF6 → #EFF6F1，
    //   落在现状与 #ECF3EE 之间偏浅处。组头带=lerp(background, Low, 0.5) 自动跟移、仍比卡浅。
    //   只动 GREEN 这一颗，其余槽与九套主题未动。
    AppTheme.GREEN to listOf(Color(0xFFFBFEFC), Color(0xFFEFF6F1), Color(0xFFEAF7EF), Color(0xFFE2F4E8), Color(0xFFDAF1E1), Color(0xFFEAF7EF), Color(0xFFFBFEFC)),
    AppTheme.RED to listOf(Color(0xFFFFFCFC), Color(0xFFFFF7F7), Color(0xFFFEEBEC), Color(0xFFFFE1E2), Color(0xFFFFD7D8), Color(0xFFFEEBEC), Color(0xFFFFFCFC)),
    AppTheme.PINK to listOf(Color(0xFFFFFCFE), Color(0xFFFEF7FB), Color(0xFFFEE9F5), Color(0xFFFCE1F1), Color(0xFFFAD8ED), Color(0xFFFEE9F5), Color(0xFFFFFCFE)),
    AppTheme.BLUE to listOf(Color(0xFFFBFDFF), Color(0xFFF4FAFF), Color(0xFFE6F4FE), Color(0xFFDCF1FF), Color(0xFFCFECFF), Color(0xFFE6F4FE), Color(0xFFFBFDFF)),
    AppTheme.CYAN to listOf(Color(0xFFFAFDFE), Color(0xFFF2FAFB), Color(0xFFDEF7F9), Color(0xFFD2F3F7), Color(0xFFC4EFF4), Color(0xFFDEF7F9), Color(0xFFFAFDFE)),
    AppTheme.ORANGE to listOf(Color(0xFFFEFCFB), Color(0xFFFFF7ED), Color(0xFFFFEFD6), Color(0xFFFFE5C2), Color(0xFFFFDBAD), Color(0xFFFFEFD6), Color(0xFFFEFCFB)),
    AppTheme.PURPLE to listOf(Color(0xFFFEFCFE), Color(0xFFFBF7FE), Color(0xFFF7EDFE), Color(0xFFF4E6FD), Color(0xFFF0DEFB), Color(0xFFF7EDFE), Color(0xFFFEFCFE)),
    AppTheme.BROWN to listOf(Color(0xFFFEFDFC), Color(0xFFFCF9F6), Color(0xFFF6EEE7), Color(0xFFF2E8DF), Color(0xFFEEE1D4), Color(0xFFF6EEE7), Color(0xFFFEFDFC)),
    AppTheme.GRAY to listOf(Color(0xFFFCFCFC), Color(0xFFF9F9F9), Color(0xFFF0F0F0), Color(0xFFEBEBEB), Color(0xFFE6E6E6), Color(0xFFF0F0F0), Color(0xFFFCFCFC)),
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
