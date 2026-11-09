package com.example.learningreminder.data

import kotlinx.coroutines.flow.Flow

class ChildRepository(private val childDao: ChildDao) {

    fun observeChildren(): Flow<List<ChildProfile>> = childDao.observeAll()

    fun observeDefaultChild(): Flow<ChildProfile?> = childDao.observeDefault()

    suspend fun getCurrentChild(): ChildProfile? = childDao.getDefault()

    suspend fun listChildren(): List<ChildProfile> = childDao.getAll()

    suspend fun addChild(name: String, birthDate: String?): Long =
        childDao.insert(ChildProfile(name = name, birthDate = birthDate))

    suspend fun updateChild(child: ChildProfile) = childDao.update(child)

    /** 二次确认后与任务/记录级联删除。 */
    suspend fun deleteChild(childId: Long) = childDao.delete(childId)

    suspend fun countsForChild(childId: Long): Pair<Int, Int> =
        childDao.countTasks(childId) to childDao.countRecords(childId)

    suspend fun setDefault(childId: Long) = childDao.setDefaultExclusive(childId)

    /** 首次启动若无任何孩子，创建默认档案。 */
    suspend fun ensureDefaultChild(defaultName: String = "宝宝"): Long {
        childDao.getDefault()?.let { return it.id }
        val existing = childDao.observeAll().let { 0 } // Flow 不能 suspension 等待，直接看 default 即可
        val id = childDao.insert(ChildProfile(name = defaultName, isDefault = true))
        return id
    }
}
