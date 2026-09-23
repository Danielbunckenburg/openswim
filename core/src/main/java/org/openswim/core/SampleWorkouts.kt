package org.openswim.core

/** Original, offline plans. Distances and rest intervals are authored for manual progression. */
object SampleWorkouts {
    private fun s(m: Int, stroke: Stroke, effort: Intensity = Intensity.MODERATE, rest: Int = 0, note: String? = null,
                  equipment: Set<Equipment> = emptySet()) = RepetitionSet(1,
        WorkoutStep(Distance(m, DistanceUnit.METERS), stroke, effort, equipment, RestDuration(rest), note = note))
    private fun r(count: Int, m: Int, stroke: Stroke, effort: Intensity = Intensity.MODERATE, rest: Int = 0,
                  note: String? = null, equipment: Set<Equipment> = emptySet()) = RepetitionSet(count,
        WorkoutStep(Distance(m, DistanceUnit.METERS), stroke, effort, equipment, RestDuration(rest), note = note))
    private fun w(id: String, name: String, category: String, minutes: Int,
                  warm: List<RepetitionSet>, main: List<RepetitionSet>, cool: List<RepetitionSet>) =
        Workout(id, name, category, minutes, listOf(
            WorkoutSection("Warm up", warm), WorkoutSection("Main set", main), WorkoutSection("Cool down", cool)
        ))

    val all: List<Workout> = listOf(
        w("easy-reset", "Easy Reset", "Easy", 24,
            listOf(s(200, Stroke.CHOICE, Intensity.EASY)),
            listOf(r(4, 100, Stroke.FREESTYLE, Intensity.EASY, 20), r(4, 50, Stroke.BACKSTROKE, Intensity.EASY, 15)),
            listOf(s(200, Stroke.CHOICE, Intensity.EASY))),
        w("easy-mixed", "Easy Mix", "Easy", 28,
            listOf(s(200, Stroke.FREESTYLE, Intensity.EASY)),
            listOf(r(4, 50, Stroke.KICK, Intensity.EASY, 20), r(4, 100, Stroke.CHOICE, Intensity.EASY, 20)),
            listOf(s(200, Stroke.BACKSTROKE, Intensity.EASY))),
        w("technique-catch", "Find Your Catch", "Technique", 30,
            listOf(s(200, Stroke.FREESTYLE, Intensity.EASY)),
            listOf(r(4, 50, Stroke.DRILL, Intensity.EASY, 20, "Single-arm freestyle"),
                r(4, 75, Stroke.FREESTYLE, rest = 20, note = "Long, quiet catch"),
                r(4, 50, Stroke.KICK, Intensity.EASY, 15)),
            listOf(s(200, Stroke.CHOICE, Intensity.EASY))),
        w("technique-balance", "Body Balance", "Technique", 32,
            listOf(s(300, Stroke.CHOICE, Intensity.EASY)),
            listOf(r(6, 50, Stroke.DRILL, Intensity.EASY, 20, "Side-kick and rotate"),
                r(4, 100, Stroke.FREESTYLE, rest = 20, note = "Hold a long body line")),
            listOf(s(200, Stroke.BACKSTROKE, Intensity.EASY))),
        w("aerobic-rhythm", "Steady Rhythm", "Aerobic", 35,
            listOf(s(300, Stroke.FREESTYLE, Intensity.EASY), r(4, 50, Stroke.DRILL, rest = 15)),
            listOf(r(6, 100, Stroke.FREESTYLE, rest = 20), r(4, 50, Stroke.KICK, rest = 15)),
            listOf(s(200, Stroke.CHOICE, Intensity.EASY))),
        w("aerobic-pull", "Pull & Breathe", "Aerobic", 40,
            listOf(s(300, Stroke.CHOICE, Intensity.EASY)),
            listOf(r(4, 200, Stroke.FREESTYLE, rest = 25, note = "Relaxed breathing", equipment = setOf(Equipment.PULL_BUOY)),
                r(4, 50, Stroke.BACKSTROKE, rest = 15)),
            listOf(s(200, Stroke.CHOICE, Intensity.EASY))),
        w("endurance-long", "Long Water", "Endurance", 48,
            listOf(s(300, Stroke.CHOICE, Intensity.EASY)),
            listOf(r(3, 400, Stroke.FREESTYLE, rest = 30), r(4, 100, Stroke.FREESTYLE, Intensity.STRONG, 20)),
            listOf(s(100, Stroke.BACKSTROKE, Intensity.EASY))),
        w("endurance-ladder", "Distance Ladder", "Endurance", 55,
            listOf(s(300, Stroke.FREESTYLE, Intensity.EASY)),
            listOf(s(200, Stroke.FREESTYLE, rest = 25), s(300, Stroke.FREESTYLE, rest = 30),
                s(400, Stroke.FREESTYLE, rest = 35), s(300, Stroke.FREESTYLE, rest = 30),
                s(200, Stroke.FREESTYLE, rest = 25)),
            listOf(s(200, Stroke.CHOICE, Intensity.EASY))),
        w("threshold-build", "Tempo Builder", "Threshold", 42,
            listOf(s(300, Stroke.FREESTYLE, Intensity.EASY), r(4, 50, Stroke.DRILL, rest = 15)),
            listOf(r(5, 200, Stroke.FREESTYLE, Intensity.STRONG, 30, "Even, controlled effort")),
            listOf(s(200, Stroke.CHOICE, Intensity.EASY))),
        w("threshold-hold", "Hold the Line", "Threshold", 38,
            listOf(s(300, Stroke.CHOICE, Intensity.EASY)),
            listOf(r(3, 300, Stroke.FREESTYLE, Intensity.STRONG, 35),
                r(4, 100, Stroke.FREESTYLE, Intensity.STRONG, 20)),
            listOf(s(200, Stroke.BACKSTROKE, Intensity.EASY))),
        w("sprint-finish", "Fast Finish", "Sprint", 25,
            listOf(s(200, Stroke.FREESTYLE, Intensity.EASY), r(4, 50, Stroke.DRILL, rest = 15)),
            listOf(r(8, 25, Stroke.FREESTYLE, Intensity.SPRINT, 30),
                r(4, 50, Stroke.FREESTYLE, Intensity.STRONG, 25)),
            listOf(s(200, Stroke.CHOICE, Intensity.EASY))),
        w("sprint-kick", "Kick & Go", "Sprint", 27,
            listOf(s(200, Stroke.CHOICE, Intensity.EASY)),
            listOf(r(6, 50, Stroke.KICK, Intensity.STRONG, 25),
                r(8, 25, Stroke.FREESTYLE, Intensity.SPRINT, 35),
                r(4, 50, Stroke.FREESTYLE, Intensity.EASY, 20)),
            listOf(s(200, Stroke.CHOICE, Intensity.EASY)))
    )
}
