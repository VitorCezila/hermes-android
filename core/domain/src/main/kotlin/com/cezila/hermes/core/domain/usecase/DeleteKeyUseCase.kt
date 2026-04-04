package com.cezila.hermes.core.domain.usecase

import com.cezila.hermes.core.domain.repository.KeyRepository

class DeleteKeyUseCase(
    private val keyRepository: KeyRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> = keyRepository.deleteKey(id)
}
