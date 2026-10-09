package com.github.jing332.tts_server_android.compose.theme

import androidx.compose.ui.graphics.Color

// 10 套静态主题的 surfaceContainer 七槽（2026-10-10 生成，勿手改；生成器=临时文件/gen_surface_tones.py）。
// tone 抄 M3 官方 baseline 表（material-color-utilities，Material Theme Builder 同款）：
//   浅 100/96/94/92/90/87/98、深 4/10/12/17/22/6/24。
// 两处本地化：①深色整表平移，锚到各主题 background（官方锚 surface=tone6，本 app 页底=background）；
// ②色相/彩度取各主题 primary、彩度≤3（=原 themedNeutral 掺 4% 主色的中性度）。超色域自动收缩。
// 列表顺序固定：[lowest, low, container, high, highest, dim, bright]，消费方=Theme.kt themedNeutral()。

internal val lightSurfaceTones: Map<AppTheme, List<Color>> = mapOf(
    AppTheme.DEFAULT to listOf(Color(0xFFFDFFFB), Color(0xFFF5F8F2), Color(0xFFEFF2ED), Color(0xFFE9ECE7), Color(0xFFE3E7E1), Color(0xFFDBDED9), Color(0xFFFAFEF8)),
    AppTheme.GREEN to listOf(Color(0xFFFBFFFC), Color(0xFFF2F8F4), Color(0xFFEDF2EE), Color(0xFFE7ECE8), Color(0xFFE1E7E3), Color(0xFFD9DEDA), Color(0xFFF8FEF9)),
    AppTheme.RED to listOf(Color(0xFFFFFDFC), Color(0xFFFDF5F3), Color(0xFFF7EFED), Color(0xFFF1E9E8), Color(0xFFECE4E2), Color(0xFFE3DBD9), Color(0xFFFFFBF9)),
    AppTheme.PINK to listOf(Color(0xFFFFFDFF), Color(0xFFFDF4F6), Color(0xFFF7EFF0), Color(0xFFF1E9EA), Color(0xFFEBE3E5), Color(0xFFE3DBDC), Color(0xFFFFFAFC)),
    AppTheme.BLUE to listOf(Color(0xFFFEFFFF), Color(0xFFF5F6FC), Color(0xFFF0F0F6), Color(0xFFEAEAF0), Color(0xFFE4E5EA), Color(0xFFDCDCE2), Color(0xFFFBFCFF)),
    AppTheme.CYAN to listOf(Color(0xFFF9FFFF), Color(0xFFF0F8F7), Color(0xFFEAF3F1), Color(0xFFE5EDEB), Color(0xFFDFE7E5), Color(0xFFD7DFDD), Color(0xFFF6FEFC)),
    AppTheme.ORANGE to listOf(Color(0xFFFFFEFA), Color(0xFFFBF5F1), Color(0xFFF5F0EB), Color(0xFFF0EAE6), Color(0xFFEAE4E0), Color(0xFFE1DCD8), Color(0xFFFFFBF7)),
    AppTheme.PURPLE to listOf(Color(0xFFFFFEFF), Color(0xFFF8F5FB), Color(0xFFF2F0F5), Color(0xFFEDEAEF), Color(0xFFE7E4EA), Color(0xFFDEDCE1), Color(0xFFFEFBFF)),
    AppTheme.BROWN to listOf(Color(0xFFFFFEFB), Color(0xFFFCF5F2), Color(0xFFF7EFEC), Color(0xFFF1EAE7), Color(0xFFEBE4E1), Color(0xFFE3DBD9), Color(0xFFFFFBF8)),
    AppTheme.GRAY to listOf(Color(0xFFF9FFFF), Color(0xFFF0F7FB), Color(0xFFEBF2F5), Color(0xFFE5ECEF), Color(0xFFDFE6EA), Color(0xFFD7DEE1), Color(0xFFF6FDFF)),
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
