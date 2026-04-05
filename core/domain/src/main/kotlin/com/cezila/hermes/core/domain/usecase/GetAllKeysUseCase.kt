package com.cezila.hermes.core.domain.usecase

import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.repository.KeyRepository
import kotlinx.coroutines.flow.Flow

class GetAllKeysUseCase(
    private val keyRepository: KeyRepository,
) {
    operator fun invoke(): Flow<List<PgpKey>> = keyRepository.getAllKeys()
}
