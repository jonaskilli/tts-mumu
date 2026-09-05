package com.github.jing332.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.github.jing332.database.constants.SpeechTarget
import com.github.jing332.database.entities.AbstractListGroup.Companion.DEFAULT_GROUP_ID
import com.github.jing332.database.entities.systts.GroupWithSystemTts
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemTtsV2Dao {
    @get:Query("SELECT COUNT(*) FROM system_tts_v2")
    val count: Int

    @get:Query("SELECT COUNT(*) FROM SystemTtsGroup")
    val groupCount: Int

    @get:Query("SELECT * FROM system_tts_v2")
    val all: List<SystemTtsV2>

    @get:Query("SELECT * FROM SystemTtsGroup")
    val allGroup: List<SystemTtsGroup>

    @Query("SELECT * FROM system_tts_v2 WHERE id = :id")
    fun get(id: Long): SystemTtsV2

    @Query("SELECT * FROM system_tts_v2 WHERE isEnabled = '1' AND  groupId = :groupId")
    fun getEnabledListByGroupId(groupId: Long): List<SystemTtsV2>

    fun getEnabledListByGroupId(
        groupId: Long, target: Int = SpeechTarget.ALL,
        isStandbyType: Boolean = false,
    ): List<SystemTtsV2> = getEnabledListByGroupId(groupId).filter {
        it.config is TtsConfigurationDTO && (it.config as TtsConfigurationDTO).run {
            this.speechRule.target == target && this.speechRule.isStandby == isStandbyType
        }
    }


    @Transaction
    @Query("SELECT * FROM SystemTtsGroup ORDER BY `order`")
    fun getAllGroupWithTts(): List<GroupWithSystemTts>

    @Transaction
    @Query("SELECT * FROM SystemTtsGroup ORDER BY `order`")
    fun flowAllGroupWithTts(): Flow<List<GroupWithSystemTts>>

    @get:Query("SELECT * FROM system_tts_v2 WHERE isEnabled = 1")
    val allEnabled: List<SystemTtsV2>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg tts: SystemTtsV2)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(vararg tts: SystemTtsV2)

    @Delete
    fun delete(vararg tts: SystemTtsV2)


    @Query("SELECT * FROM system_tts_v2 WHERE groupId = :groupId")
    fun getByGroup(groupId: Long): List<SystemTtsV2>

    /** 查询使用指定插件的配置项（用于删除插件前统计引用） */
    fun getByPluginId(pluginId: String): List<SystemTtsV2> =
        all.filter {
            ((it.config as? TtsConfigurationDTO)?.source as? PluginTtsSource)?.pluginId == pluginId
        }

    @Query("DELETE from system_tts_v2 WHERE groupId = :groupId")
    fun deleteTtsByGroup(groupId: Long)

    @Query("DELETE FROM system_tts_v2")
    fun deleteAllTts()

    @Query("DELETE FROM SystemTtsGroup")
    fun deleteAllGroups()

    @Query("SELECT DISTINCT categoryPath FROM system_tts_v2 WHERE groupId = :groupId AND categoryPath != '' ORDER BY categoryPath ASC")
    fun getCategoryPathsByGroup(groupId: Long): List<String>

    @Query("UPDATE system_tts_v2 SET categoryPath = :categoryPath WHERE id = :id")
    fun updateCategoryPath(id: Long, categoryPath: String)

    @Transaction
    @Query("SELECT * FROM SystemTtsGroup ORDER BY `order`")
    fun allGroup(): List<GroupWithSystemTts>


    @Query("SELECT * FROM SystemTtsGroup WHERE groupId = :id")
    fun getGroup(id: Long = DEFAULT_GROUP_ID): SystemTtsGroup?

    @Query("SELECT * FROM system_tts_v2 WHERE groupId = :groupId ORDER BY `order` ASC")
    fun getTtsListByGroupId(groupId: Long): List<SystemTtsV2>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGroup(vararg group: SystemTtsGroup)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateGroup(group: SystemTtsGroup)

    @Delete
    fun deleteGroup(group: SystemTtsGroup)

    fun insertGroupWithTts(vararg g: GroupWithSystemTts) {
        for (v in g) {
            insertGroup(v.group)
            insert(*v.list.toTypedArray())
        }
    }

    /**
     * 按照分组和分组内进行排序获取
     */
    fun getEnabledListForSort(target: Int, isStandbyType: Boolean = false): List<SystemTtsV2> {
        val list = mutableListOf<SystemTtsV2>()
        allGroup.forEach { group ->
            list.addAll(
                getEnabledListByGroupId(
                    group.id,
                    target,
                    isStandbyType
                ).sortedBy { it.order })
        }

        return list
    }

    fun updateAllOrder() {
        getAllGroupWithTts().forEachIndexed { index, groupWithSystemTts ->
            val g = groupWithSystemTts.group
            if (g.order != index) updateGroupOrder(g.id, index)

            groupWithSystemTts.list.sortedBy { it.order }.forEachIndexed { subIndex, systemTts ->
                if (systemTts.order != subIndex) updateItemOrder(systemTts.id, subIndex)
            }
        }
    }

    // order 重排必须只写 order 列:此前 update(整实体)在长事务里与用户保存并发时,
    // 会用「事务开始时读出的旧实体」整行写回,把刚保存的 audioParams 等覆盖回旧值
    @Query("UPDATE system_tts_v2 SET `order` = :order WHERE id = :id")
    fun updateItemOrder(id: Long, order: Int)

    @Query("UPDATE `SystemTtsGroup` SET `order` = :order WHERE `groupId` = :id")
    fun updateGroupOrder(id: Long, order: Int)

}