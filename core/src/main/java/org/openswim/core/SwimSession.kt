package org.openswim.core

/** Both watch modes use the same manual session surface. Sensor events can replace manual length
 * events later without changing the screens or the guided progression engine. */
enum class SwimMode { GUIDED, POOL }
enum class ManualSwimType { KICK, DRILL }

data class SwimSession(
    val mode: SwimMode,
    val poolLength: Int,
    val guided: WorkoutSession? = null,
    val freePhase: SessionPhase = SessionPhase.NotStarted,
    val freeElapsedSeconds: Int = 0,
    val loggedLengths: Int = 0,
    val manualExtraDistance: Int = 0,
    val locked: Boolean = false
) {
    init {
        require(poolLength > 0)
        require((mode == SwimMode.GUIDED) == (guided != null))
        require(loggedLengths >= 0 && manualExtraDistance >= 0 && freeElapsedSeconds >= 0)
        require(manualExtraDistance % poolLength == 0)
        if (guided != null) require(guided.workout.supportsPoolLength(poolLength))
    }

    val workout: Workout? get() = guided?.workout
    val phase: SessionPhase get() = guided?.phase ?: freePhase
    val elapsedSeconds: Int get() = guided?.elapsedSeconds ?: freeElapsedSeconds
    val completedDistance: Int get() = (guided?.completedDistance ?: loggedLengths * poolLength) + manualExtraDistance
    val completedLengths: Int get() = completedDistance / poolLength
    val current: Instruction? get() = guided?.current

    fun start(): SwimSession = when (mode) {
        SwimMode.GUIDED -> copy(guided = guided!!.start())
        SwimMode.POOL -> if (freePhase == SessionPhase.NotStarted) copy(freePhase = SessionPhase.Active) else this
    }

    fun tick(): SwimSession = when (mode) {
        SwimMode.GUIDED -> copy(guided = guided!!.tick())
        SwimMode.POOL -> if (freePhase == SessionPhase.Active)
            copy(freeElapsedSeconds = freeElapsedSeconds + 1) else this
    }

    fun advance(): SwimSession =
        if (locked || mode != SwimMode.GUIDED) this else copy(guided = guided!!.advance())

    fun skipRest(): SwimSession =
        if (locked || mode != SwimMode.GUIDED) this else copy(guided = guided!!.skipRest())

    /** A free-swim length is an explicit watch tap, never a claimed sensor reading. */
    fun logLength(): SwimSession =
        if (locked || mode != SwimMode.POOL || freePhase != SessionPhase.Active) this
        else copy(loggedLengths = loggedLengths + 1)

    /** Matching guided kick/drill entries complete that repetition; other entries add extra distance. */
    fun logManualDistance(type: ManualSwimType, meters: Int): SwimSession {
        if (locked || phase != SessionPhase.Active || meters <= 0 || meters % poolLength != 0) return this
        val instruction = current
        val matchingStroke = if (type == ManualSwimType.KICK) Stroke.KICK else Stroke.DRILL
        return if (instruction != null && instruction.set.step.stroke == matchingStroke &&
            instruction.set.step.distance.amount == meters) advance()
        else copy(manualExtraDistance = manualExtraDistance + meters)
    }

    fun pause(): SwimSession {
        if (locked) return this
        return when (mode) {
            SwimMode.GUIDED -> copy(guided = guided!!.pause())
            SwimMode.POOL -> if (freePhase == SessionPhase.Active)
                copy(freePhase = SessionPhase.Paused(SessionPhase.Active)) else this
        }
    }

    fun resume(): SwimSession = when (mode) {
        SwimMode.GUIDED -> copy(guided = guided!!.resume())
        SwimMode.POOL -> if (freePhase is SessionPhase.Paused)
            copy(freePhase = freePhase.previous) else this
    }

    fun end(): SwimSession {
        if (locked) return this
        return when (mode) {
            SwimMode.GUIDED -> copy(guided = guided!!.end())
            SwimMode.POOL -> if (freePhase != SessionPhase.NotStarted)
                copy(freePhase = SessionPhase.Complete) else this
        }
    }

    fun lock(): SwimSession =
        if (phase == SessionPhase.Active || phase is SessionPhase.Rest) copy(locked = true) else this

    fun unlock(): SwimSession = if (locked) copy(locked = false) else this

    companion object {
        fun guided(workout: Workout, poolLength: Int) =
            SwimSession(SwimMode.GUIDED, poolLength, guided = WorkoutSession(workout))

        fun pool(poolLength: Int) = SwimSession(SwimMode.POOL, poolLength)
    }
}
