package com.github.jing332.tts_server_android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jing332.tts_server_android.R

/**
 * 密钥启用池计数徽章（10-13 用户令·三处密钥入口统一为图一形式：角色管理顶栏 / 系统TTS三点菜单 /
 * 设置页，都紧贴标题摆、不落行尾）。
 * 池里有钥 = 主题色「启用 N」（数值 = 密钥页「启用池(N)」同源口径，miyue 池值条数）；
 * 池空 = 红字「未启用」（用户令「假如说没有启用的话，要用红字写一下，未启用」——
 * 原口径「池空不显示」作废）。
 */
@Composable
fun KeyPoolBadge(poolCount: Int, modifier: Modifier = Modifier) {
    val has = poolCount > 0
    // 未启用红（10-13 晚用户令「未启用的红色不正宗，应该改为日志那个表示错误的红色」）：
    // 日志失败条同款正宗红——浅色 #F44336 / 深色 #E57373（与 LogScreen 失败丸/源错误正文同源）；
    // colorScheme.error 在动态取色下偏暗紫红，弃用。EnabledCountBadge 同批同款。
    val emptyRed = if (isSystemInDarkTheme()) Color(0xFFE57373) else Color(0xFFF44336)
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(
                (if (has) MaterialTheme.colorScheme.primary else emptyRed)
                    .copy(alpha = 0.12f)
            )
            .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (has) stringResource(R.string.role_entry_key_pool_badge, poolCount)
            else stringResource(R.string.role_entry_key_pool_empty),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (has) MaterialTheme.colorScheme.primary else emptyRed,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
