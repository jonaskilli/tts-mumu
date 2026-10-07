package com.github.jing332.tts_server_android.compose.systts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 序号徽章的最小边长（1 位数时宽=高=本值 ⇒ 正圆） */
private val BadgeMinSize = 20.dp

/**
 * 序号徽章（全站统一，10-05 用户拍板形状「丙」= 胶囊）。
 *
 * 形状：**宽高都不小于 20dp，宽度随内容撑，圆角用百分比 50%**。
 * - 1 位数：宽=高=20dp、圆角=半高 ⇒ **就是正圆**（与密钥池此前观感一致，序号感不丢）；
 * - 2 位数起：宽度自然长成胶囊（「12」约 24.6dp、「123」约 30.9dp），**永不溢出**。
 *
 * 为什么不用「固定 20dp 正圆」：11sp 数字「12」实测字形宽 12.6dp，而 20dp 圆在数字
 * 所占据高度处的可用弦宽只有 18.4dp——两位数只剩 ~3dp/侧余量，**三位数（18.9dp）直接
 * 溢出圆外**。主界面有多个分组、密钥池可放多把密钥，两位数必然出现，故宽度必须随内容走。
 * （注：宽度写死 20dp 时 1 位数≈18.3dp 还会被压成扁圆，故最小边长得双向设。）
 *
 * 配色：浅底 + 深字（**主界面原有那套**）——primaryContainer@50% 底 + onPrimaryContainer 字。
 * 理由：这些行右侧本就有一个饱和绿勾选框，序号再用实心主题色会同屏两个高饱和绿互抢；
 * 浅底既一眼认出、又不喧宾夺主。（密钥池原为实心主题色 + 白字，统一后随之改浅底。）
 *
 * 尺寸：最小边 20dp；左右 padding 6dp、上下 2dp；字号 labelSmall。
 */
@Composable
internal fun OrderBadge(
    number: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            // 高度钉死 20dp（10-07 装机反馈）：minHeight 挡不住系统字体放大——行高随放大
            // 撑到 23dp 而宽度停在 20dp，1 位数被拉成竖椭圆（实测 20.9×23.1）。钉死后
            // 放大档下数字墨迹（≈14dp）仍完整落在 20dp 圆内；宽度照旧随内容走（minWidth 兜底）
            .height(BadgeMinSize)
            .defaultMinSize(minWidth = BadgeMinSize)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                // 百分比圆角：任何宽高比下都是胶囊端；1 位数（宽=高）即正圆
                shape = RoundedCornerShape(percent = 50)
            )
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1
        )
    }
}
