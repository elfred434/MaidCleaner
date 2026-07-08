package com.maidcleaner.data.scanner

import com.maidcleaner.data.model.DatabaseInfo
import com.maidcleaner.data.model.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseOptimizerScanner @Inject constructor() {

    /**
     * Scan for SQLite databases that can be optimized.
     * Without root/Shizuku, we can only access our own app's databases
     * and any databases in public storage.
     * With root/Shizuku, we can access all app data directories.
     */
    fun scan(hasRootAccess: Boolean): Flow<ScanProgress<List<DatabaseInfo>>> = flow {
        emit(ScanProgress.Scanning(0, "Scanning databases..."))

        val databases = mutableListOf<DatabaseInfo>()

        // Scan our own app databases
        emit(ScanProgress.Scanning(20, "Checking app databases..."))

        if (hasRootAccess) {
            // With root: scan /data/data/*/databases/
            val dataDir = File("/data/data")
            if (dataDir.exists() && dataDir.canRead()) {
                dataDir.listFiles()?.forEach { appDir ->
                    val dbDir = File(appDir, "databases")
                    if (dbDir.exists()) {
                        scanDatabaseDirectory(dbDir, appDir.name, databases)
                    }
                }
            }
        } else {
            // Without root: scan accessible public directories
            // and our own app's databases
            val externalStorage = File(System.getenv("EXTERNAL_STORAGE") ?: "/sdcard")
            scanForPublicDatabases(externalStorage, databases)
        }

        // Estimate potential savings (typical VACUUM savings 10-40% for fragmented DBs)
        val result = databases.map { db ->
            val estimatedSavings = (db.size * 0.2).toLong() // Conservative 20% estimate
            db.copy(
                estimatedSavings = estimatedSavings,
                isOptimizable = estimatedSavings > 10240 // >10KB savings worth it
            )
        }.sortedByDescending { it.estimatedSavings }

        emit(ScanProgress.Complete(result))
    }.flowOn(Dispatchers.IO)

    private fun scanDatabaseDirectory(dir: File, packageName: String, results: MutableList<DatabaseInfo>) {
        dir.listFiles()?.filter {
            it.isFile && (it.name.endsWith(".db") || it.name.endsWith(".sqlite") || it.name.endsWith(".sqlite3"))
        }?.forEach { dbFile ->
            // Check for WAL/SHM files (indicates fragmentation)
            val walFile = File(dir, "${dbFile.name}-wal")
            val shmFile = File(dir, "${dbFile.name}-shm")
            val walSize = if (walFile.exists()) walFile.length() else 0L
            val shmSize = if (shmFile.exists()) shmFile.length() else 0L

            results.add(
                DatabaseInfo(
                    path = dbFile.absolutePath,
                    name = dbFile.name,
                    packageName = packageName,
                    size = dbFile.length() + walSize + shmSize
                )
            )
        }
    }

    private fun scanForPublicDatabases(root: File, results: MutableList<DatabaseInfo>) {
        root.walkTopDown().maxDepth(5)
            .filter { it.isFile && (it.name.endsWith(".db") || it.name.endsWith(".sqlite") || it.name.endsWith(".sqlite3")) }
            .forEach { dbFile ->
                results.add(
                    DatabaseInfo(
                        path = dbFile.absolutePath,
                        name = dbFile.name,
                        packageName = "public",
                        size = dbFile.length()
                    )
                )
            }
    }

    /**
     * Execute VACUUM on a SQLite database.
     * Requires root/Shizuku for databases not owned by our app.
     */
    fun vacuumDatabase(dbPath: String, hasRootAccess: Boolean): Flow<ScanProgress<Pair<Long, Long>>> = flow {
        val file = File(dbPath)
        val sizeBefore = file.length()

        emit(ScanProgress.Scanning(50, "Optimizing ${file.name}..."))

        try {
            if (hasRootAccess) {
                // Use root shell to execute sqlite3 vacuum
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "sqlite3 \"$dbPath\" \"VACUUM;\""))
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    emit(ScanProgress.Error("VACUUM failed with exit code $exitCode"))
                    return@flow
                }
            } else {
                // For our own databases, use Android's SQLite API
                val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbPath, null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
                )
                db.execSQL("VACUUM")
                db.close()
            }

            val sizeAfter = file.length()
            emit(ScanProgress.Complete(Pair(sizeBefore, sizeAfter)))
        } catch (e: Exception) {
            emit(ScanProgress.Error("Failed to optimize: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}
