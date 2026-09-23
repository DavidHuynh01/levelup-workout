package com.davidhuynh.levelup.ui.workout.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidhuynh.levelup.data.prefs.UserPreferencesStore
import com.davidhuynh.levelup.data.repository.WorkoutDraft
import com.davidhuynh.levelup.data.repository.WorkoutRepositoryImpl
import com.davidhuynh.levelup.domain.logic.RestTimer
import com.davidhuynh.levelup.domain.logic.VolumeCalculator
import com.davidhuynh.levelup.domain.logic.WeightConverter
import com.davidhuynh.levelup.domain.model.Exercise
import com.davidhuynh.levelup.domain.model.MuscleGroup
import com.davidhuynh.levelup.domain.model.PrAward
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.domain.repository.AuthRepository
import com.davidhuynh.levelup.domain.repository.ExerciseRepository
import com.davidhuynh.levelup.domain.usecase.SaveWorkoutUseCase
import com.davidhuynh.levelup.domain.util.AppClock
import com.davidhuynh.levelup.domain.util.DataResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SetRow(
    val key: Long,
    val reps: String = "",
    val weight: String = "",
    val isWarmup: Boolean = false,
) {
    val repsValue: Int get() = reps.toIntOrNull() ?: 0
    val weightValue: Double get() = weight.replace(',', '.').toDoubleOrNull() ?: 0.0
}

data class ExerciseBlock(
    val key: Long,
    val exercise: Exercise,
    val sets: List<SetRow>,
)

data class RestState(
    val startedAtMillis: Long,
    val durationSeconds: Int,
    val remainingSeconds: Int,
) {
    val isFinished: Boolean get() = remainingSeconds == 0
    val label: String get() = RestTimer.format(remainingSeconds)

    val fraction: Float
        get() = if (durationSeconds <= 0) 0f else remainingSeconds / durationSeconds.toFloat()
}

data class LogWorkoutUiState(
    val isEdit: Boolean = false,
    val restSeconds: Int = RestTimer.DEFAULT_SECONDS,
    val rest: RestState? = null,
    val name: String = "",
    val date: LocalDate = LocalDate.now(),
    val blocks: List<ExerciseBlock> = emptyList(),
    val catalogue: List<Exercise> = emptyList(),
    val searchQuery: String = "",
    val isPickerOpen: Boolean = false,
    val weightUnit: WeightUnit = WeightUnit.LB,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val awards: List<PrAward> = emptyList(),
    val savedWorkoutId: String? = null,
) {

    val totalVolumeDisplay: String
        get() {
            val kg = blocks.sumOf { block ->
                block.sets
                    .filter { !it.isWarmup }
                    .sumOf { it.repsValue * WeightConverter.toKg(it.weightValue, weightUnit) }
            }
            return WeightConverter.formatVolume(kg, weightUnit)
        }

    val workingSetCount: Int
        get() = blocks.sumOf { block -> block.sets.count { !it.isWarmup && it.repsValue > 0 } }

    val filteredCatalogue: List<Exercise>
        get() = if (searchQuery.isBlank()) {
            catalogue
        } else {
            catalogue.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

    val canSave: Boolean get() = !isSaving && workingSetCount > 0
}

class LogWorkoutViewModel(
    private val userId: String,
    private val workoutId: String?,

    private val repeatOfWorkoutId: String? = null,
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepositoryImpl,
    private val saveWorkout: SaveWorkoutUseCase,
    private val authRepository: AuthRepository,
    private val preferences: UserPreferencesStore,
    private val clock: AppClock,
) : ViewModel() {

    private val _state = MutableStateFlow(LogWorkoutUiState(isEdit = workoutId != null))
    val state: StateFlow<LogWorkoutUiState> = _state.asStateFlow()

    private var nextKey = 1L

    init {
        viewModelScope.launch {
            val unit = authRepository.getUser(userId)?.weightUnit ?: WeightUnit.LB
            val catalogue = exerciseRepository.search(userId, "")
            val rememberedRest = preferences.restSeconds.first()
            _state.update {
                it.copy(
                    catalogue = catalogue,
                    weightUnit = unit,
                    date = clock.today(),
                    restSeconds = rememberedRest,
                    isLoading = false,
                )
            }
            when {
                workoutId != null -> loadWorkoutInto(workoutId, unit, keepDate = true)
                repeatOfWorkoutId != null -> loadWorkoutInto(repeatOfWorkoutId, unit, keepDate = false)
            }
        }
    }

    private suspend fun loadWorkoutInto(id: String, unit: WeightUnit, keepDate: Boolean) {
        val workout = workoutRepository.getWorkout(id) ?: return
        _state.update { current ->
            current.copy(
                name = workout.name,
                date = if (keepDate) workout.localDate else current.date,
                blocks = workout.exercises.map { exercise ->
                    ExerciseBlock(
                        key = nextKey++,
                        exercise = exercise.exercise,
                        sets = exercise.sets.map { set ->
                            SetRow(
                                key = nextKey++,
                                reps = set.reps.toString(),
                                weight = WeightConverter.formatValue(set.weightKg, unit),
                                isWarmup = set.isWarmup,
                            )
                        },
                    )
                },
            )
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }

    fun onDateChange(date: LocalDate) = _state.update { it.copy(date = date) }

    fun openPicker() = _state.update { it.copy(isPickerOpen = true, searchQuery = "") }

    fun closePicker() = _state.update { it.copy(isPickerOpen = false) }

    fun onSearchChange(value: String) = _state.update { it.copy(searchQuery = value) }

    fun addExercise(exercise: Exercise) {
        _state.update { current ->
            current.copy(
                isPickerOpen = false,
                searchQuery = "",
                blocks = current.blocks + ExerciseBlock(
                    key = nextKey++,
                    exercise = exercise,

                    sets = listOf(SetRow(key = nextKey++)),
                ),
            )
        }
    }

    fun createAndAddExercise(name: String, muscleGroup: MuscleGroup) {
        viewModelScope.launch {
            when (val result = exerciseRepository.createCustom(userId, name, muscleGroup, null)) {
                is DataResult.Success -> {
                    _state.update { it.copy(catalogue = exerciseRepository.search(userId, "")) }
                    addExercise(result.value)
                }
                is DataResult.Failure -> _state.update { it.copy(error = result.message) }
            }
        }
    }

    fun removeExercise(blockKey: Long) = _state.update { current ->
        current.copy(blocks = current.blocks.filterNot { it.key == blockKey })
    }

    fun addSet(blockKey: Long) = _state.update { current ->
        current.copy(
            blocks = current.blocks.map { block ->
                if (block.key != blockKey) return@map block

                val previous = block.sets.lastOrNull { !it.isWarmup } ?: block.sets.lastOrNull()
                block.copy(
                    sets = block.sets + SetRow(
                        key = nextKey++,
                        reps = previous?.reps.orEmpty(),
                        weight = previous?.weight.orEmpty(),
                    ),
                )
            }
        )
    }

    fun removeSet(blockKey: Long, setKey: Long) = _state.update { current ->
        current.copy(
            blocks = current.blocks.map { block ->
                if (block.key != blockKey) block
                else block.copy(sets = block.sets.filterNot { it.key == setKey })
            }
        )
    }

    fun updateSet(
        blockKey: Long,
        setKey: Long,
        reps: String? = null,
        weight: String? = null,
        isWarmup: Boolean? = null,
    ) = _state.update { current ->
        current.copy(
            blocks = current.blocks.map { block ->
                if (block.key != blockKey) return@map block
                block.copy(
                    sets = block.sets.map { set ->
                        if (set.key != setKey) return@map set
                        set.copy(
                            reps = reps?.filter { it.isDigit() }?.take(3) ?: set.reps,
                            weight = weight?.filter { it.isDigit() || it == '.' || it == ',' }?.take(6)
                                ?: set.weight,
                            isWarmup = isWarmup ?: set.isWarmup,
                        )
                    }
                )
            }
        )
    }

    fun save() {
        val current = _state.value
        if (!current.canSave) return

        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val zone = clock.zone()

            val performedAt = if (current.date == clock.today()) {
                clock.nowMillis()
            } else {
                current.date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
            }

            val draft = WorkoutDraft(
                workoutId = workoutId,
                userId = userId,
                name = current.name.trim(),
                performedAt = performedAt,
                zoneId = zone.id,
                exercises = current.blocks.map { block ->
                    WorkoutDraft.ExerciseDraft(
                        exerciseId = block.exercise.id,
                        sets = block.sets
                            .filter { it.repsValue > 0 }
                            .map { row ->
                                WorkoutDraft.SetDraft(
                                    reps = row.repsValue,
                                    weightKg = WeightConverter.toKg(row.weightValue, current.weightUnit),
                                    isWarmup = row.isWarmup,
                                )
                            },
                    )
                },
            )

            when (val result = saveWorkout(draft)) {
                is DataResult.Success -> _state.update {
                    it.copy(
                        isSaving = false,
                        awards = result.value.awards,
                        savedWorkoutId = result.value.workoutId,
                    )
                }
                is DataResult.Failure -> _state.update {
                    it.copy(isSaving = false, error = result.message)
                }
            }
        }
    }

    fun dismissAwards() = _state.update { it.copy(awards = emptyList()) }

    fun clearError() = _state.update { it.copy(error = null) }

    private var tickJob: Job? = null

    fun startRest(seconds: Int = _state.value.restSeconds) {
        val now = clock.nowMillis()
        _state.update {
            it.copy(
                restSeconds = seconds,
                rest = RestState(
                    startedAtMillis = now,
                    durationSeconds = seconds,
                    remainingSeconds = seconds,
                ),
            )
        }
        viewModelScope.launch { preferences.setRestSeconds(seconds) }
        restartTicking()
    }

    fun extendRest() {
        val rest = _state.value.rest ?: return
        val extended = RestTimer.extend(rest.durationSeconds)
        _state.update {
            it.copy(
                rest = rest.copy(
                    durationSeconds = extended,
                    remainingSeconds = RestTimer.remainingSeconds(
                        rest.startedAtMillis,
                        extended,
                        clock.nowMillis(),
                    ),
                ),
            )
        }
        restartTicking()
    }

    fun stopRest() {
        tickJob?.cancel()
        tickJob = null
        _state.update { it.copy(rest = null) }
    }

    private fun restartTicking() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                val rest = _state.value.rest ?: break
                val remaining = RestTimer.remainingSeconds(
                    rest.startedAtMillis,
                    rest.durationSeconds,
                    clock.nowMillis(),
                )
                _state.update { it.copy(rest = rest.copy(remainingSeconds = remaining)) }
                if (remaining == 0) break
                delay(1_000)
            }
        }
    }

    override fun onCleared() {
        tickJob?.cancel()
        super.onCleared()
    }
}
