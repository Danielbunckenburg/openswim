package org.openswim.android

import org.openswim.core.CompletedWorkout
import org.openswim.core.TrainingPlan
import org.openswim.core.ScheduledSwim
import org.openswim.core.Workout

/** Domain-facing data boundary for the Supabase-backed companion. */
internal interface CompanionRepository {
    val workouts: List<Workout>
    val plans: List<TrainingPlan>
    val scheduled: List<ScheduledSwim>
    val completed: List<CompletedWorkout>
    fun workout(id: String): Workout?
}
