package com.cezila.hermes.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cezila.hermes.core.data.db.dao.PgpKeyDao
import com.cezila.hermes.core.data.db.entity.PgpKeyEntity

@Database(entities = [PgpKeyEntity::class], version = 1, exportSchema = false)
abstract class HermesDatabase : RoomDatabase() {
    abstract fun pgpKeyDao(): PgpKeyDao
}
