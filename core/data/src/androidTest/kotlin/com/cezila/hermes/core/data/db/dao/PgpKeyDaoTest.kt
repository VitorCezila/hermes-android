package com.cezila.hermes.core.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.cezila.hermes.core.data.db.HermesDatabase
import com.cezila.hermes.core.data.db.entity.PgpKeyEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PgpKeyDaoTest {

    private lateinit var db: HermesDatabase
    private lateinit var dao: PgpKeyDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HermesDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.pgpKeyDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun buildEntity(
        id: String = "key1",
        createdAt: Long = 1_000L,
        algorithmName: String = "RSA_4096",
    ) = PgpKeyEntity(
        id = id,
        fingerprint = "AABB$id",
        ownerName = "Alice",
        ownerEmail = "alice@example.com",
        algorithmName = algorithmName,
        createdAt = createdAt,
        expiresAt = null,
        isSecret = true,
        armoredPublicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----",
        encryptedPrivateKeyBlob = ByteArray(16),
        privateKeyIv = ByteArray(12),
    )

    @Test
    fun insert_thenObserveAll_emitsOneEntity() = runTest {
        dao.insert(buildEntity())

        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("key1", items[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertMultiple_observeAll_orderedByCreatedAtDesc() = runTest {
        dao.insert(buildEntity(id = "older", createdAt = 1_000L))
        dao.insert(buildEntity(id = "newer", createdAt = 2_000L))

        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertEquals("newer", items[0].id)
            assertEquals("older", items[1].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun insert_duplicate_throwsConstraintException() = runTest {
        dao.insert(buildEntity())
        dao.insert(buildEntity())
    }

    @Test
    fun findById_whenExists_returnsEntity() = runTest {
        dao.insert(buildEntity())
        val entity = dao.findById("key1")
        assertNotNull(entity)
        assertEquals("key1", entity?.id)
    }

    @Test
    fun findById_whenNotExists_returnsNull() = runTest {
        val entity = dao.findById("nonexistent")
        assertNull(entity)
    }

    @Test
    fun deleteById_removesEntity() = runTest {
        dao.insert(buildEntity())
        dao.deleteById("key1")
        assertNull(dao.findById("key1"))
    }

    @Test
    fun deleteById_nonExistentId_returnsZero() = runTest {
        val rowsDeleted = dao.deleteById("ghost")
        assertEquals(0, rowsDeleted)
    }

    @Test
    fun observeAll_emitsUpdatedListAfterInsert() = runTest {
        dao.observeAll().test {
            assertTrue(awaitItem().isEmpty())
            dao.insert(buildEntity())
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeAll_emitsUpdatedListAfterDelete() = runTest {
        dao.insert(buildEntity(id = "key1"))
        dao.insert(buildEntity(id = "key2"))

        dao.observeAll().test {
            val firstEmission = awaitItem()
            assertEquals(2, firstEmission.size)
            dao.deleteById("key1")
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
