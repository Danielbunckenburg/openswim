package org.openswim.android

import org.openswim.core.CompletedWorkout
import org.openswim.core.Distance
import org.openswim.core.DistanceUnit
import org.openswim.core.SampleWorkouts
import org.openswim.core.ScheduledWorkout
import org.openswim.core.ScheduledSwim
import org.openswim.core.TrainingPlan
import org.openswim.core.Workout

/** Local examples only. No generated sensor readings or network/device state. */
internal object SampleData : CompanionRepository {
    override val workouts: List<Workout> = SampleWorkouts.all
    override fun workout(id: String): Workout? = workouts.firstOrNull { it.id == id }

    private val samplePlan = TrainingPlan(
        id = "sample-foundation", title = "Swim Foundation", objective = "Build a steady swim routine",
        durationWeeks = 4, workoutsPerWeek = 3, difficulty = "Beginner to intermediate",
        currentWeek = 1, completedSessions = 0,
        schedule = listOf(
            ScheduledWorkout("w1-mon", "easy-reset", "Monday", 1),
            ScheduledWorkout("w1-wed", "technique-catch", "Wednesday", 1),
            ScheduledWorkout("w1-fri", "aerobic-rhythm", "Friday", 1),
            ScheduledWorkout("w2-mon", "easy-mixed", "Monday", 2),
            ScheduledWorkout("w2-wed", "technique-balance", "Wednesday", 2),
            ScheduledWorkout("w2-fri", "aerobic-pull", "Friday", 2),
            ScheduledWorkout("w3-mon", "easy-reset", "Monday", 3),
            ScheduledWorkout("w3-wed", "endurance-long", "Wednesday", 3),
            ScheduledWorkout("w3-fri", "threshold-build", "Friday", 3),
            ScheduledWorkout("w4-mon", "easy-mixed", "Monday", 4),
            ScheduledWorkout("w4-wed", "endurance-ladder", "Wednesday", 4),
            ScheduledWorkout("w4-fri", "sprint-finish", "Friday", 4)
        )
    )

    override val plans = listOf(samplePlan)
    override val scheduled: List<ScheduledSwim> = emptyList()

    override val completed = listOf(
        CompletedWorkout(
            id = "demo-swim-1", workoutId = "easy-reset", dateLabel = "Sample swim · 18 September",
            distance = Distance(1000, DistanceUnit.METERS), durationMinutes = 26, isDemo = true
        )
    )
}
