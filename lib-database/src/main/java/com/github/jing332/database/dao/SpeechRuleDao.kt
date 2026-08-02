package com.github.jing332.database.dao

import androidx.room.*
import com.github.jing332.database.entities.SpeechRule
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeechRuleDao {
    @get:Query("SELECT * FROM speech_rules ORDER BY `order` ASC")
    val all: List<SpeechRule>

    @get:Query("SELECT * FROM speech_rules WHERE isEnabled = '1'")
    val allEnabled: List<SpeechRule>

    /**
     * 轻量查询：code返回空字符串，避免大code导致Cursor窗口溢出闪退
     * 用于列表展示等不需要code的场景
     */
    @Query("SELECT id, isEnabled, name, version, ruleId, author, '' AS code, tags, tagsData, `order` FROM speech_rules ORDER BY `order` ASC")
    fun getAllWithoutCode(): List<SpeechRule>

    /**
     * 轻量查询：code返回空字符串，避免大code导致Cursor窗口溢出闪退
     * 用于列表展示等不需要code的场景
     */
    @Query("SELECT id, isEnabled, name, version, ruleId, author, '' AS code, tags, tagsData, `order` FROM speech_rules WHERE isEnabled = '1'")
    fun getAllEnabledWithoutCode(): List<SpeechRule>

    @Query("SELECT * FROM speech_rules ORDER BY `order` ASC")
    fun flowAll(): Flow<List<SpeechRule>>

    /**
     * 轻量Flow查询：code返回空字符串，避免大code导致Cursor窗口溢出闪退
     */
    @Query("SELECT id, isEnabled, name, version, ruleId, author, '' AS code, tags, tagsData, `order` FROM speech_rules ORDER BY `order` ASC")
    fun flowAllWithoutCode(): Flow<List<SpeechRule>>

    @get:Query("SELECT count(*) FROM speech_rules")
    val count: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg data: SpeechRule)

    @Delete
    fun delete(vararg data: SpeechRule)

    @Update
    fun update(vararg data: SpeechRule)

    @Query("SELECT * FROM speech_rules WHERE ruleId = :ruleId AND isEnabled = :isEnabled LIMIT 1")
    fun getByRuleId(ruleId: String, isEnabled: Boolean = true): SpeechRule?

    @Query("SELECT * FROM speech_rules WHERE ruleId = :ruleId LIMIT 1")
    fun getByRuleIdAll(ruleId: String): SpeechRule?

//    @Query("SELECT * FROM speech_rules WHERE ruleId = :ruleId")
//    fun getByRuleId(ruleId: String): SpeechRule?

    fun insertOrUpdate(vararg args: SpeechRule) {
        for (v in args) {
            val old = getByRuleId(v.ruleId)
            if (old == null) {
                insert(v)
                continue
            }

            if (v.ruleId == old.ruleId && v.version > old.version)
                update(v.copy(id = old.id))
        }
    }
}