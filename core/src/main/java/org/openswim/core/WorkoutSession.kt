package org.openswim.core

sealed interface SessionPhase {
    data object NotStarted : SessionPhase
    data object Active : SessionPhase
    data class Rest(val remainingSeconds: Int) : SessionPhase
    data class Paused(val previous: SessionPhase) : SessionPhase
    data object Complete : SessionPhase
}

/** Immutable, manually driven session. The watch supplies elapsed wall time; this class owns progression. */
data class WorkoutSession(
    val workout: Workout,
    val phase: SessionPhase = SessionPhase.NotStarted,
    val instructionIndex: Int = 0,
    val completedDistance: Int = 0,
    val completedRepetitions: Int = 0,
    val elapsedSeconds: Int = 0
) {
    private val instructions get() = workout.instructions()
    val current: Instruction? get() = instructions.getOrNull(instructionIndex)
    val completedSets: Int get() = instructions.take(completedRepetitions).count { instruction ->
        instruction.position.repetition == instruction.set.repetitions - 1
    }

    fun start(): WorkoutSession = if (phase == SessionPhase.NotStarted) copy(phase = SessionPhase.Active) else this

    fun advance(): WorkoutSession {
        if (phase != SessionPhase.Active) return this
        val instruction = current ?: return this
        val nextIndex = instructionIndex + 1
        val completed = copy(
            instructionIndex = nextIndex,
            completedDistance = completedDistance + instruction.set.step.distance.amount,
            completedRepetitions = completedRepetitions + 1
        )
        if (nextIndex == instructions.size) return completed.copy(phase = SessionPhase.Complete)
        val rest = instruction.set.step.restAfter.seconds
        return completed.copy(phase = if (rest > 0) SessionPhase.Rest(rest) else SessionPhase.Active)
    }

    fun tick(): WorkoutSession = when (val state = phase) {
        is SessionPhase.Rest -> copy(
            elapsedSeconds = elapsedSeconds + 1,
            phase = if (state.remainingSeconds <= 1) SessionPhase.Active else state.copy(remainingSeconds = state.remainingSeconds - 1)
        )
        SessionPhase.Active -> copy(elapsedSeconds = elapsedSeconds + 1)
        else -> this
    }

    fun skipRest(): WorkoutSession = if (phase is SessionPhase.Rest) copy(phase = SessionPhase.Active) else this
    fun pause(): WorkoutSession = if (phase == SessionPhase.Active || phase is SessionPhase.Rest) copy(phase = SessionPhase.Paused(phase)) else this
    fun resume(): WorkoutSession = if (phase is SessionPhase.Paused) copy(phase = phase.previous) else this
    fun end(): WorkoutSession = if (phase != SessionPhase.NotStarted) copy(phase = SessionPhase.Complete) else this
}
