package com.cezila.hermes.core.domain.usecase

import com.cezila.hermes.core.domain.model.PgpKey
import com.cezila.hermes.core.domain.repository.KeyRepository

class GetKeyByIdUseCase(
    private val keyRepository: KeyRepository,
) {
    suspend operator fun invoke(id: String): Result<PgpKey?> = keyRepository.getKeyById(id)
}
