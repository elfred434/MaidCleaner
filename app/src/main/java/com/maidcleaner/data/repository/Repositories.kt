package com.maidcleaner.data.repository

import com.maidcleaner.data.local.dao.WhitelistDao
import com.maidcleaner.data.local.entity.WhitelistEntity
import com.maidcleaner.data.model.ListType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhitelistRepository @Inject constructor(
    private val whitelistDao: WhitelistDao
) {
    fun getWhitelist(): Flow<List<WhitelistEntity>> = whitelistDao.getByType(ListType.WHITELIST)
    fun getBlacklist(): Flow<List<WhitelistEntity>> = whitelistDao.getByType(ListType.BLACKLIST)

    suspend fun addToWhitelist(path: String, packageName: String) {
        whitelistDao.insert(WhitelistEntity(path = path, packageName = packageName, type = ListType.WHITELIST))
    }

    suspend fun addToBlacklist(path: String, packageName: String) {
        whitelistDao.insert(WhitelistEntity(path = path, packageName = packageName, type = ListType.BLACKLIST))
    }

    suspend fun removeFromWhitelist(path: String) = whitelistDao.deleteByPath(path)
    suspend fun isWhitelisted(path: String): Boolean = whitelistDao.exists(path, ListType.WHITELIST)
}
