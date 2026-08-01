package com.github.jing332.database

import android.database.AbstractWindowedCursor
import android.database.Cursor
import android.database.CursorWindow
import android.os.Build
import android.os.CancellationSignal
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory

private const val CURSOR_WINDOW_SIZE = 16L * 1024 * 1024 // 16MB

/**
 * 自定义 SupportSQLiteOpenHelper.Factory，通过包装 SupportSQLiteDatabase
 * 为每个查询结果设置更大的 CursorWindow（16MB），避免大 code 字段导致
 * 默认 2MB Cursor 窗口溢出闪退。
 */
class LargeCursorOpenHelperFactory : SupportSQLiteOpenHelper.Factory {
    private val delegate = FrameworkSQLiteOpenHelperFactory()

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val helper = delegate.create(configuration)
        return object : SupportSQLiteOpenHelper by helper {
            override val writableDatabase: SupportSQLiteDatabase
                get() = LargeCursorDatabase(helper.writableDatabase)

            override val readableDatabase: SupportSQLiteDatabase
                get() = LargeCursorDatabase(helper.readableDatabase)
        }
    }
}

private class LargeCursorDatabase(
    private val delegate: SupportSQLiteDatabase
) : SupportSQLiteDatabase by delegate {

    private fun Cursor.applyLargeWindow(): Cursor {
        if (this is AbstractWindowedCursor) {
            setWindow(createLargeCursorWindow())
        }
        return this
    }

    private fun createLargeCursorWindow(): CursorWindow {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            CursorWindow("large", CURSOR_WINDOW_SIZE)
        } else {
            // API < 28: CursorWindow 构造函数不支持指定大小，
            // 通过反射修改实例字段 mWindowSize
            val window = CursorWindow("large")
            try {
                val field = CursorWindow::class.java.getDeclaredField("mWindowSize")
                field.isAccessible = true
                field.set(window, CURSOR_WINDOW_SIZE)
            } catch (_: Exception) {
                // 反射失败则回退到修改静态字段 sCursorWindowSize
                try {
                    val staticField = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
                    staticField.isAccessible = true
                    staticField.set(null, CURSOR_WINDOW_SIZE)
                } catch (_: Exception) {
                    // 最终回退：使用默认 2MB 窗口
                }
            }
            window
        }
    }

    override fun query(query: String, bindArgs: Array<out Any?>?): Cursor {
        return delegate.query(query, bindArgs).applyLargeWindow()
    }

    override fun query(query: SupportSQLiteQuery): Cursor {
        return delegate.query(query).applyLargeWindow()
    }

    override fun query(query: SupportSQLiteQuery, cancellationSignal: CancellationSignal?): Cursor {
        return delegate.query(query, cancellationSignal).applyLargeWindow()
    }
}
