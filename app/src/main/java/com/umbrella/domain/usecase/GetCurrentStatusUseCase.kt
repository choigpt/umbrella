package com.umbrella.domain.usecase

import com.umbrella.data.prefs.PreferencesRepository
import com.umbrella.domain.model.StatusInfo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 현재 상태 조회 UseCase
 */
class GetCurrentStatusUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {

    /**
     * 상태 Flow 반환
     */
    operator fun invoke(): Flow<StatusInfo> {
        return preferencesRepository.statusFlow
    }
}
