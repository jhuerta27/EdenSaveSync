package com.jotasoft.edensavesync.domain.usecase

import com.jotasoft.edensavesync.domain.repository.AuthRepository
import javax.inject.Inject

class PairingCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    fun exportCode(): String = authRepository.exportPairingCode()
    fun importCode(code: String): Boolean = authRepository.importPairingCode(code)
}
