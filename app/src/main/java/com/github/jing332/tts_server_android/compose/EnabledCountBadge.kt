package com.github.jing332.tts_server_android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jing332.tts_server_android.R

/**
 * 启用计数徽章（10-13 用户令「主界面右上角菜单和设置页的朗读规则和插件也要后面显示
 * 已启用x，未启用两种显示方式」）：count>0 = 主题色「已启用 N」；count=0 = 红字「未启用」。
 * 朗读规则（全局最多启用一条）/ 插件（启用条数）两处用；密钥入口的 KeyPoolBadge 同族
 * （图一形式=紧贴标题，不落行尾）。未启用文案复用 role_entry_key_pool_empty。
 */
@Composable
fun EnabledCountBadge(enabledCount: Int, modifier: Modifier = Modifier) {
    val has = enabledCount > 0
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(
                (if (has) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    .copy(alpha = 0.12f)
            )
            .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (has) "已启用 $enabledCount" else stringResource(R.string.role_entry_key_pool_empty),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (has) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
