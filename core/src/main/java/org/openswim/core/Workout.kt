package org.openswim.core

import kotlinx.serialization.Serializable

@Serializable enum class DistanceUnit(val symbol: String) { METERS("m"), YARDS("yd") }
@Serializable data class Distance(val amount: Int, val unit: DistanceUnit) {
    init { require(amount > 0) }
    override fun toString() = "$amount ${unit.symbol}"
}
@Serializable enum class Stroke { FREESTYLE, BACKSTROKE, BREASTSTROKE, BUTTERFLY, INDIVIDUAL_MEDLEY, KICK, DRILL, CHOICE }
@Serializable enum class Equipment { FINS, PADDLES, PULL_BUOY, KICKBOARD }
@Serializable enum class Intensity { EASY, MODERATE, STRONG, SPRINT }
@Serializable data class RestDuration(val seconds: Int) { init { require(seconds >= 0) } }
@Serializable data class TargetPace(val secondsPer100: Int) { init { require(secondsPer100 > 0) } }
@Serializable data class TargetInterval(val seconds: Int) { init { require(seconds > 0) } }

@Serializable data class WorkoutStep(
    val distance: Distance,
    val stroke: Stroke,
    val intensity: Intensity = Intensity.MODERATE,
    val equipment: Set<Equipment> = emptySet(),
    val restAfter: RestDuration = RestDuration(0),
    val targetPace: TargetPace? = null,
    val targetInterval: TargetInterval? = null,
    val note: String? = null
)
@Serializable data class RepetitionSet(val repetitions: Int, val step: WorkoutStep) {
    init { require(repetitions > 0) }
    val totalDistance: Distance get() = Distance(repetitions * step.distance.amount, step.distance.unit)
}
@Serializable data class WorkoutSection(val name: String, val sets: List<RepetitionSet>) {
    init { require(name.isNotBlank()); require(sets.isNotEmpty()) }
}
@Serializable data class Workout(
    val id: String,
    val name: String,
    val type: String,
    val estimatedMinutes: Int,
    val sections: List<WorkoutSection>
) {
    init {
        require(id.isNotBlank() && name.isNotBlank() && sections.isNotEmpty())
        require(estimatedMinutes > 0)
        val units = sections.flatMap { it.sets }.map { it.step.distance.unit }.toSet()
        require(units.size == 1) { "A workout must use one distance unit" }
    }
    val totalDistance: Distance get() = Distance(sections.sumOf { section -> section.sets.sumOf { it.totalDistance.amount } }, sections.first().sets.first().step.distance.unit)
    val totalRepetitions: Int get() = sections.sumOf { section -> section.sets.sumOf { it.repetitions } }
}

data class Position(val section: Int, val set: Int, val repetition: Int)
data class Instruction(val section: WorkoutSection, val set: RepetitionSet, val position: Position)

fun Workout.instructions(): List<Instruction> = buildList {
    sections.forEachIndexed { sectionIndex, section ->
        section.sets.forEachIndexed { setIndex, set ->
            repeat(set.repetitions) { repetition -> add(Instruction(section, set, Position(sectionIndex, setIndex, repetition))) }
        }
    }
}

fun Workout.supportsPoolLength(length: Int): Boolean =
    length > 0 && sections.flatMap { it.sets }.all { it.step.distance.amount % length == 0 }
