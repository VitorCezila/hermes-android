package com.cezila.hermes.core.domain.usecase

import app.cash.turbine.test
import com.cezila.hermes.core.domain.model.KeyAlgorithm
import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.repository.KeyRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAllKeysUseCaseTest {

    private val keyRepository: KeyRepository = mockk()
    private val useCase = GetAllKeysUseCase(keyRepository)

    private val fakeKey = PgpKey(
        id = "0xAABB",
        fingerprint = "AABB",
        ownerName = "Alice",
        ownerEmail = "alice@example.com",
        algorithm = KeyAlgorithm.ED25519,
        createdAt = 1_700_000_000_000L,
        expiresAt = null,
        isSecret = true,
        armoredPublicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----",
    )

    @Test
    fun invoke_delegatesToRepository_emitsCorrectItems() = runTest {
        every { keyRepository.getAllKeys() } returns flowOf(listOf(fakeKey))

        useCase().test {
            assertEquals(listOf(fakeKey), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun invoke_withEmptyList_emitsEmptyList() = runTest {
        every { keyRepository.getAllKeys() } returns flowOf(emptyList())

        useCase().test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun invoke_withMultipleEmissions_forwardsAll() = runTest {
        val fakeKey2 = fakeKey.copy(id = "0xCCDD", fingerprint = "CCDD")
        every { keyRepository.getAllKeys() } returns flow {
            emit(listOf(fakeKey))
            emit(listOf(fakeKey, fakeKey2))
        }

        useCase().test {
            assertEquals(1, awaitItem().size)
            assertEquals(2, awaitItem().size)
            awaitComplete()
        }
    }
}
