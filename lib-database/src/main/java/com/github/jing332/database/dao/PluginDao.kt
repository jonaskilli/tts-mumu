package com.github.jing332.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.github.jing332.database.entities.plugin.Plugin
import kotlinx.coroutines.flow.Flow

@Dao
interface PluginDao {
    @get:Query("SELECT * FROM plugin ORDER BY `order` ASC")
    val all: List<Plugin>

    @get:Query("SELECT * FROM plugin WHERE isEnabled = '1' ORDER BY `order` ASC")
    val allEnabled: List<Plugin>

    @Query("SELECT * FROM plugin ORDER BY `order` ASC")
    fun flowAll(): Flow<List<Plugin>>

    /**
     * 轻量Flow查询：code返回空字符串，避免大code导致Cursor窗口溢出闪退
     * 用于插件管理列表等不需要code的场景
     */
    @Query("SELECT id, isEnabled, version, name, pluginId, author, iconUrl, '' AS code, defVars, userVars, `order`, audioParams, pluginHandlesSpeed, pluginHandlesVolume, pluginHandlesPitch FROM plugin ORDER BY `order` ASC")
    fun flowAllWithoutCode(): Flow<List<Plugin>>

    /**
     * 轻量查询：code返回空字符串，避免加载2MB+大code导致Cursor窗口溢出
     * 用于列表展示等不需要code的场景
     */
    @Query("SELECT id, isEnabled, version, name, pluginId, author, iconUrl, '' AS code, defVars, userVars, `order`, audioParams, pluginHandlesSpeed, pluginHandlesVolume, pluginHandlesPitch FROM plugin ORDER BY `order` ASC")
    fun getAllWithoutCode(): List<Plugin>

    /**
     * 轻量查询：code返回空字符串，仅返回已启用的插件，避免大code导致Cursor窗口溢出
     */
    @Query("SELECT id, isEnabled, version, name, pluginId, author, iconUrl, '' AS code, defVars, userVars, `order`, audioParams, pluginHandlesSpeed, pluginHandlesVolume, pluginHandlesPitch FROM plugin WHERE isEnabled = '1' ORDER BY `order` ASC")
    fun getAllEnabledWithoutCode(): List<Plugin>

    /**
     * 轻量单查：按 pluginId 取插件元数据（code 为空串）。
     * 09-07 崩溃实锤：resolveTtsPlayback 曾用 SELECT * 的 getByPluginId，
     * 5MB+ 插件 JS 撑爆 CursorWindow 致点击朗读即 OOM。参数解析只需元数据。
     */
    @Query("SELECT id, isEnabled, version, name, pluginId, author, iconUrl, '' AS code, defVars, userVars, `order`, audioParams, pluginHandlesSpeed, pluginHandlesVolume, pluginHandlesPitch FROM plugin WHERE pluginId = :pluginId LIMIT 1")
    fun getMetaByPluginId(pluginId: String): Plugin?

    /**
     * 轻量全表：全部插件元数据（code 为空串）。
     * getAllTts 批量解析时一次查完建缓存，消灭逐条配置全量查插件的 N+1。
     */
    @Query("SELECT id, isEnabled, version, name, pluginId, author, iconUrl, '' AS code, defVars, userVars, `order`, audioParams, pluginHandlesSpeed, pluginHandlesVolume, pluginHandlesPitch FROM plugin")
    fun getAllMeta(): List<Plugin>

    @get:Query("SELECT count(*) FROM plugin")
    val count: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg data: Plugin)

    @Delete
    fun delete(vararg data: Plugin)

    @Query("DELETE FROM plugin")
    fun deleteAll()

    @Update
    fun update(vararg data: Plugin)

    @Query("SELECT * FROM plugin WHERE pluginId = :pluginId ")
    fun getByPluginId(pluginId: String): Plugin?

    @Query("SELECT * FROM plugin WHERE pluginId = :pluginId AND isEnabled")
    fun getEnabled(pluginId: String): Plugin?

    /**
     * 轻量查询：仅返回插件名(不含code)，避免大code导致Cursor窗口溢出闪退
     */
    @Query("SELECT name FROM plugin WHERE pluginId = :pluginId AND isEnabled LIMIT 1")
    fun getEnabledName(pluginId: String): String?

    /**
     * 轻量查询：仅返回插件名(不含code)，避免大code导致Cursor窗口溢出闪退
     */
    @Query("SELECT name FROM plugin WHERE pluginId = :pluginId LIMIT 1")
    fun getNameByPluginId(pluginId: String): String?

    fun insertOrUpdate(vararg args: Plugin) {
        for (v in args) {
            val old = getByPluginId(v.pluginId)
            if (old == null) {
                insert(v)
                continue
            }

            if (v.pluginId == old.pluginId && v.version > old.version)
                update(v.copy(id = old.id))
        }
    }
}