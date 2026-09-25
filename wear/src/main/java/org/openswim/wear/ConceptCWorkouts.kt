package org.openswim.wear

import org.openswim.core.Distance
import org.openswim.core.DistanceUnit
import org.openswim.core.Intensity
import org.openswim.core.RepetitionSet
import org.openswim.core.RestDuration
import org.openswim.core.Stroke
import org.openswim.core.Workout
import org.openswim.core.WorkoutSection
import org.openswim.core.WorkoutStep
import org.openswim.core.SampleWorkouts

/** The three plans pictured in Concept C, followed by the existing library. */
internal object ConceptCWorkouts {
    private fun set(reps: Int, meters: Int, stroke: Stroke, rest: Int = 0,
                    effort: Intensity = Intensity.MODERATE) =
        RepetitionSet(reps, WorkoutStep(Distance(meters, DistanceUnit.METERS), stroke,
            intensity = effort, restAfter = RestDuration(rest)))

    val featured = listOf(
        Workout("concept-technique-1200", "Technique 1200", "Technique", 30, listOf(
            WorkoutSection("Drills", listOf(set(6, 100, Stroke.FREESTYLE, 20))),
            WorkoutSection("Swim", listOf(set(4, 50, Stroke.KICK, 15))),
            WorkoutSection("Build", listOf(set(4, 100, Stroke.FREESTYLE, 20, Intensity.STRONG)))
        )),
        Workout("concept-aerobic-1500", "Aerobic 1500", "Aerobic", 35, listOf(
            WorkoutSection("Warm up", listOf(set(1, 200, Stroke.CHOICE))),
            WorkoutSection("Main set", listOf(set(10, 100, Stroke.FREESTYLE, 20))),
            WorkoutSection("Cool down", listOf(set(1, 300, Stroke.CHOICE)))
        )),
        Workout("concept-threshold-1800", "Threshold 1800", "Threshold", 40, listOf(
            WorkoutSection("Warm up", listOf(set(1, 300, Stroke.CHOICE))),
            WorkoutSection("Main set", listOf(set(6, 200, Stroke.FREESTYLE, 30, Intensity.STRONG))),
            WorkoutSection("Cool down", listOf(set(1, 300, Stroke.CHOICE)))
        ))
    )

    val all = featured + SampleWorkouts.all
}
