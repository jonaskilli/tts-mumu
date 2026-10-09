package com.github.jing332.tts_server_android.compose.theme

import androidx.compose.ui.graphics.Color

// 10 套静态主题的 surfaceContainer 七槽（2026-10-10 生成，勿手改；生成器=临时文件/gen_surface_tones.py）。
// 浅色=fresh green 体系（用户拍板以设置页浅绿 #F2F8F4 为卡底基底全站重排）：
//   tone 阶梯 Lowest=99 Low=96.5 Container=94.5 High=92.5 Highest=90.5 Dim=88 Bright=98，
//   整表平移使 GREEN Low 槽精确=#F2F8F4；色相随主题 primary、彩度≤8（清新淡彩，非灰阶）。
// 深色=M3 官方 baseline 暗色表 4/10/12/17/22/6/24，整表锚到各主题 background，色相取 primary（彩度≤3）。
// 列表顺序固定：[lowest, low, container, high, highest, dim, bright]，消费方=Theme.kt themedNeutral()。

internal val lightSurfaceTones: Map<AppTheme, List<Color>> = mapOf(
    AppTheme.DEFAULT to listOf(Color(0xFFFCFFF9), Color(0xFFF4F8F2), Color(0xFFEFF2EC), Color(0xFFE9ECE7), Color(0xFFE3E6E1), Color(0xFFDCDFDA), Color(0xFFF9FCF6)),
    AppTheme.GREEN to listOf(Color(0xFFF9FFFB), Color(0xFFF2F8F4), Color(0xFFEDF2EE), Color(0xFFE7ECE8), Color(0xFFE1E7E3), Color(0xFFDAE0DB), Color(0xFFF7FCF8)),
    AppTheme.RED to listOf(Color(0xFFFFFCFA), Color(0xFFFDF5F3), Color(0xFFF7EFED), Color(0xFFF1E9E8), Color(0xFFECE4E2), Color(0xFFE5DDDB), Color(0xFFFFF9F7)),
    AppTheme.PINK to listOf(Color(0xFFFFFCFD), Color(0xFFFDF5F6), Color(0xFFF7EFF0), Color(0xFFF1E9EB), Color(0xFFECE3E5), Color(0xFFE4DCDE), Color(0xFFFFF9FA)),
    AppTheme.BLUE to listOf(Color(0xFFFDFDFF), Color(0xFFF6F6FC), Color(0xFFF0F0F6), Color(0xFFEAEBF0), Color(0xFFE5E5EB), Color(0xFFDDDEE4), Color(0xFFFAFAFF)),
    AppTheme.CYAN to listOf(Color(0xFFF7FFFE), Color(0xFFF0F8F7), Color(0xFFEAF3F1), Color(0xFFE5EDEB), Color(0xFFDFE7E5), Color(0xFFD8E0DE), Color(0xFFF4FDFB)),
    AppTheme.ORANGE to listOf(Color(0xFFFFFDF8), Color(0xFFFBF6F1), Color(0xFFF5F0EB), Color(0xFFF0EAE6), Color(0xFFEAE4E0), Color(0xFFE3DDD9), Color(0xFFFFFAF5)),
    AppTheme.PURPLE to listOf(Color(0xFFFFFDFF), Color(0xFFF8F5FB), Color(0xFFF2F0F5), Color(0xFFEDEAF0), Color(0xFFE7E4EA), Color(0xFFE0DDE3), Color(0xFFFCFAFF)),
    AppTheme.BROWN to listOf(Color(0xFFFFFCF9), Color(0xFFFCF5F2), Color(0xFFF7EFEC), Color(0xFFF1EAE7), Color(0xFFEBE4E1), Color(0xFFE4DDDA), Color(0xFFFFF9F6)),
    AppTheme.GRAY to listOf(Color(0xFFF8FFFF), Color(0xFFF1F8FB), Color(0xFFEBF2F5), Color(0xFFE5ECF0), Color(0xFFE0E6EA), Color(0xFFD9DFE3), Color(0xFFF5FCFF)),
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
