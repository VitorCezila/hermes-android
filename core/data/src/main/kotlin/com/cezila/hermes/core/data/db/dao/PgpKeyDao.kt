package com.cezila.hermes.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cezila.hermes.core.data.db.entity.PgpKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PgpKeyDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: PgpKeyEntity)

    @Query("SELECT * FROM pgp_keys ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PgpKeyEntity>>

    @Query("SELECT * FROM pgp_keys WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PgpKeyEntity?

    @Query("DELETE FROM pgp_keys WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
