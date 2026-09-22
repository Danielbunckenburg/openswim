package org.openswim.core

object SampleWorkouts {
    private fun step(m: Int, stroke: Stroke, intensity: Intensity = Intensity.MODERATE, rest: Int = 0, note: String? = null) =
        WorkoutStep(Distance(m, DistanceUnit.METERS), stroke, intensity, restAfter = RestDuration(rest), note = note)
    private fun set(count: Int, m: Int, stroke: Stroke, intensity: Intensity = Intensity.MODERATE, rest: Int = 0, note: String? = null) =
        RepetitionSet(count, step(m, stroke, intensity, rest, note))

    val all: List<Workout> = listOf(
        Workout("aerobic-1500", "Aerobic 1500", "ENDURANCE", 35, listOf(
            WorkoutSection("Warm Up", listOf(set(1, 300, Stroke.FREESTYLE, Intensity.EASY), set(4, 50, Stroke.DRILL, rest = 15))),
            WorkoutSection("Main Set", listOf(set(6, 100, Stroke.FREESTYLE, rest = 20), set(4, 50, Stroke.KICK, rest = 15))),
            WorkoutSection("Cool Down", listOf(set(1, 200, Stroke.CHOICE, Intensity.EASY)))
        )),
        Workout("technique-1200", "Technique 1200", "SKILLS", 30, listOf(
            WorkoutSection("Warm Up", listOf(set(1, 200, Stroke.FREESTYLE, Intensity.EASY))),
            WorkoutSection("Drill Set", listOf(set(4, 50, Stroke.DRILL, rest = 20), set(4, 75, Stroke.FREESTYLE, rest = 20, note = "Focus on a long catch"), set(4, 50, Stroke.KICK, rest = 15))),
            WorkoutSection("Cool Down", listOf(set(1, 300, Stroke.CHOICE, Intensity.EASY)))
        )),
        Workout("sprint-1000", "Sprint 1000", "SPEED", 25, listOf(
            WorkoutSection("Warm Up", listOf(set(1, 200, Stroke.FREESTYLE, Intensity.EASY), set(4, 50, Stroke.DRILL, rest = 15))),
            WorkoutSection("Main Set", listOf(set(8, 25, Stroke.FREESTYLE, Intensity.SPRINT, 30), set(4, 50, Stroke.FREESTYLE, Intensity.STRONG, 25))),
            WorkoutSection("Cool Down", listOf(set(1, 200, Stroke.CHOICE, Intensity.EASY)))
        )),
        Workout("endurance-2000", "Endurance 2000", "ENDURANCE", 48, listOf(
            WorkoutSection("Warm Up", listOf(set(1, 300, Stroke.CHOICE, Intensity.EASY))),
            WorkoutSection("Main Set", listOf(set(3, 400, Stroke.FREESTYLE, rest = 30), set(4, 100, Stroke.FREESTYLE, Intensity.STRONG, 20))),
            WorkoutSection("Cool Down", listOf(set(1, 100, Stroke.BACKSTROKE, Intensity.EASY)))
        ))
    )
}
