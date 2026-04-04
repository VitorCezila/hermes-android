package com.cezila.hermes.core.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // SQLite cannot ALTER COLUMN, so recreate the table with nullable private key fields
        database.execSQL(
            """
            CREATE TABLE pgp_keys_new (
                id TEXT PRIMARY KEY NOT NULL,
                fingerprint TEXT NOT NULL,
                ownerName TEXT NOT NULL,
                ownerEmail TEXT NOT NULL,
                algorithmName TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                expiresAt INTEGER,
                isSecret INTEGER NOT NULL,
                armoredPublicKey TEXT NOT NULL,
                encryptedPrivateKeyBlob BLOB,
                privateKeyIv BLOB
            )
            """.trimIndent()
        )
        database.execSQL("INSERT INTO pgp_keys_new SELECT * FROM pgp_keys")
        database.execSQL("DROP TABLE pgp_keys")
        database.execSQL("ALTER TABLE pgp_keys_new RENAME TO pgp_keys")
    }
}
