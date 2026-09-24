package org.openswim.core

/** References a shared Workout; the day belongs to the schedule, not the workout definition. */
data class ScheduledWorkout(
    val id: String,
    val workoutId: String,
    val dayLabel: String,
    val week: Int,
    val state: String = "Scheduled"
)

data class TrainingPlan(
    val id: String,
    val title: String,
    val objective: String,
    val durationWeeks: Int,
    val workoutsPerWeek: Int,
    val difficulty: String,
    val currentWeek: Int,
    val completedSessions: Int,
    val schedule: List<ScheduledWorkout>
)

/** A dated swim from the account's cloud schedule. */
data class ScheduledSwim(
    val id: String,
    val workoutId: String?,
    val planId: String?,
    val date: String,
    val status: String
)

/** Optional fields are absent until they have actually been recorded. */
data class CompletedWorkout(
    val id: String,
    val workoutId: String,
    val dateLabel: String,
    val distance: Distance,
    val durationMinutes: Int,
    val paceSecondsPer100: Int? = null,
    val lengths: Int? = null,
    val heartRateBpm: Int? = null,
    val swolf: Int? = null,
    val strokeNotes: String? = null,
    val intervalSplits: List<String> = emptyList(),
    val isDemo: Boolean = false
)
