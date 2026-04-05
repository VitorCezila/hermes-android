package com.cezila.hermes.core.domain.usecase

import com.cezila.hermes.core.domain.repository.KeyRepository

class ClearAllKeysUseCase(
    private val keyRepository: KeyRepository,
) {
    suspend operator fun invoke(): Result<Unit> = keyRepository.clearAllKeys()
}
