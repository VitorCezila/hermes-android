package com.cezila.hermes.core.data.di

import android.content.Context
import androidx.room.Room
import com.cezila.hermes.core.data.db.HermesDatabase
import com.cezila.hermes.core.data.db.dao.PgpKeyDao
import com.cezila.hermes.core.data.db.migration.MIGRATION_1_2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HermesDatabase =
        Room.databaseBuilder(context, HermesDatabase::class.java, "hermes.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun providePgpKeyDao(db: HermesDatabase): PgpKeyDao = db.pgpKeyDao()
}
