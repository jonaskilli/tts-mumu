package com.github.jing332.tts_server_android.compose.theme

import androidx.compose.ui.graphics.Color

// 10 套静态主题的 surfaceContainer 七槽（2026-10-10 生成，勿手改；生成器=临时文件/gen_surface_tones.py）。
// 浅色=fresh green 体系（用户拍板以设置页浅绿 #F2F8F4 为卡底基底全站重排）：
//   tone 阶梯 Lowest=99 Low=96.5 Container=94.5 High=92.5 Highest=90.5 Dim=88 Bright=98，
//   整表平移使 GREEN Low 槽精确=#F2F8F4；色相随主题 primary、彩度≤8（清新淡彩，非灰阶）。
// 深色=M3 官方 baseline 暗色表 4/10/12/17/22/6/24，整表锚到各主题 background，色相取 primary（彩度≤3）。
// 列表顺序固定：[lowest, low, container, high, highest, dim, bright]，消费方=Theme.kt themedNeutral()。

internal val lightSurfaceTones: Map<AppTheme, List<Color>> = mapOf(
    AppTheme.DEFAULT to listOf(Color(0xFFF8FFF2), Color(0xFFF1F9EB), Color(0xFFEBF4E5), Color(0xFFE5EEDF), Color(0xFFE0E8DA), Color(0xFFD8E1D3), Color(0xFFF5FEEF)),
    AppTheme.GREEN to listOf(Color(0xFFF2FFF6), Color(0xFFEBFAEF), Color(0xFFE5F5E9), Color(0xFFE0EFE4), Color(0xFFDAE9DE), Color(0xFFD3E2D7), Color(0xFFEFFFF3)),
    AppTheme.RED to listOf(Color(0xFFFFF9F5), Color(0xFFFFF2EE), Color(0xFFFFECE8), Color(0xFFFCE7E2), Color(0xFFF6E1DD), Color(0xFFEFDAD6), Color(0xFFFFF6F2)),
    AppTheme.PINK to listOf(Color(0xFFFFF9FC), Color(0xFFFFF1F5), Color(0xFFFFECEF), Color(0xFFFBE6EA), Color(0xFFF6E0E4), Color(0xFFEED9DD), Color(0xFFFFF6FA)),
    AppTheme.BLUE to listOf(Color(0xFFFBFDFF), Color(0xFFF4F6FF), Color(0xFFEEF0FF), Color(0xFFE9EAFA), Color(0xFFE3E4F4), Color(0xFFDCDDED), Color(0xFFF8FAFF)),
    AppTheme.CYAN to listOf(Color(0xFFECFFFE), Color(0xFFE5FBF7), Color(0xFFDFF5F1), Color(0xFFDAF0EB), Color(0xFFD4EAE6), Color(0xFFCDE3DF), Color(0xFFE9FFFB)),
    AppTheme.ORANGE to listOf(Color(0xFFFFFBEF), Color(0xFFFFF4E8), Color(0xFFFDEEE3), Color(0xFFF7E9DD), Color(0xFFF1E3D7), Color(0xFFEADCD0), Color(0xFFFFF8ED)),
    AppTheme.PURPLE to listOf(Color(0xFFFFFBFF), Color(0xFFFBF4FF), Color(0xFFF5EEFD), Color(0xFFEFE8F7), Color(0xFFEAE3F1), Color(0xFFE2DCEA), Color(0xFFFFF8FF)),
    AppTheme.BROWN to listOf(Color(0xFFFFFAF2), Color(0xFFFFF3EB), Color(0xFFFFEDE5), Color(0xFFFAE7E0), Color(0xFFF5E2DA), Color(0xFFEDDBD3), Color(0xFFFFF7EF)),
    AppTheme.GRAY to listOf(Color(0xFFEEFFFF), Color(0xFFE7FAFF), Color(0xFFE1F4FD), Color(0xFFDBEEF8), Color(0xFFD6E8F2), Color(0xFFCFE1EB), Color(0xFFEBFEFF)),
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
