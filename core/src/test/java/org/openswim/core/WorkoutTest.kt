package org.openswim.core

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class WorkoutTest {
    @Test fun exampleWorkoutTotals1200Meters() {
        fun set(repetitions: Int, meters: Int, stroke: Stroke) = RepetitionSet(
            repetitions, WorkoutStep(Distance(meters, DistanceUnit.METERS), stroke)
        )
        val workout = Workout("example", "Example", "TEST", 30, listOf(
            WorkoutSection("Warm Up", listOf(set(1, 200, Stroke.CHOICE))),
            WorkoutSection("Main", listOf(set(4, 50, Stroke.DRILL), set(4, 100, Stroke.FREESTYLE), set(4, 50, Stroke.KICK))),
            WorkoutSection("Cool Down", listOf(set(1, 200, Stroke.CHOICE)))
        ))
        assertEquals(Distance(1200, DistanceUnit.METERS), workout.totalDistance)
    }

    @Test fun sampleTotalsAreCorrect() {
        assertEquals(listOf(1000, 1000, 1100, 1200, 1500, 1500, 2000, 1900, 1700, 1800, 1000, 1100),
            SampleWorkouts.all.map { it.totalDistance.amount })
        assertEquals(setOf("Easy", "Technique", "Aerobic", "Endurance", "Threshold", "Sprint"),
            SampleWorkouts.all.map { it.type }.toSet())
        assertTrue(SampleWorkouts.all.all { it.supportsPoolLength(25) })
        assertFalse(SampleWorkouts.all.first { it.type == "Sprint" }.supportsPoolLength(50))
    }

    @Test fun repetitionsAndSerialization() {
        val workout = SampleWorkouts.all.first()
        assertEquals(400, workout.sections[1].sets.first().totalDistance.amount)
        assertEquals(10, workout.totalRepetitions)
        assertEquals(workout, Json.decodeFromString<Workout>(Json.encodeToString(Workout.serializer(), workout)))
    }

    @Test fun mixedUnitsAreRejected() {
        val yards = RepetitionSet(1, WorkoutStep(Distance(25, DistanceUnit.YARDS), Stroke.CHOICE))
        try {
            Workout("mixed", "Mixed", "TEST", 1, listOf(SampleWorkouts.all.first().sections.first(), WorkoutSection("Yards", listOf(yards))))
            fail("Expected mixed units to be rejected")
        } catch (_: IllegalArgumentException) { }
        assertEquals("25 yd", yards.totalDistance.toString())
    }

    @Test fun sessionProgressionRestPauseAndFinish() {
        val workout = Workout("test", "Test", "TEST", 1, listOf(WorkoutSection("Main", listOf(
            RepetitionSet(2, WorkoutStep(Distance(50, DistanceUnit.METERS), Stroke.FREESTYLE, restAfter = RestDuration(2)))
        ))))
        var session = WorkoutSession(workout).start()
        assertEquals(SessionPhase.Active, session.phase)
        session = session.advance()
        assertEquals(SessionPhase.Rest(2), session.phase)
        assertEquals(50, session.completedDistance)
        session = session.pause().tick()
        assertEquals(0, session.elapsedSeconds)
        session = session.resume().tick()
        assertEquals(SessionPhase.Rest(1), session.phase)
        session = session.tick()
        assertEquals(SessionPhase.Active, session.phase)
        session = session.advance()
        assertEquals(SessionPhase.Complete, session.phase)
        assertEquals(100, session.completedDistance)
        assertEquals(2, session.completedRepetitions)
        assertEquals(1, session.completedSets)
    }

    @Test fun skipRestAndEarlyEnd() {
        var session = WorkoutSession(SampleWorkouts.all.first()).start().advance()
        session = session.advance()
        assertTrue(session.phase is SessionPhase.Rest)
        assertEquals(SessionPhase.Active, session.skipRest().phase)
        assertEquals(SessionPhase.Complete, session.end().phase)
    }

    @Test fun everyLocalWorkoutCanBeCompletedManually() {
        SampleWorkouts.all.forEach { workout ->
            var session = WorkoutSession(workout).start()
            repeat(workout.totalRepetitions) {
                session = session.skipRest().advance()
            }
            assertEquals(workout.id, SessionPhase.Complete, session.phase)
            assertEquals(workout.id, workout.totalDistance.amount, session.completedDistance)
            assertEquals(workout.id, workout.totalRepetitions, session.completedRepetitions)
            assertEquals(workout.id, workout.sections.sumOf { it.sets.size }, session.completedSets)
        }
    }

    @Test fun invalidTransitionsDoNotAdvanceTheWorkout() {
        val fresh = WorkoutSession(SampleWorkouts.all.first())
        assertEquals(fresh, fresh.advance())
        assertEquals(fresh, fresh.end())
        assertEquals(fresh, fresh.resume())
        val active = fresh.start()
        assertEquals(active, active.skipRest())
        val resting = active.advance().advance()
        assertTrue(resting.phase is SessionPhase.Rest)
        assertEquals(resting, resting.advance())
        val paused = resting.pause()
        assertEquals(paused, paused.advance())
        assertEquals(paused, paused.tick())
        assertEquals(resting, paused.resume())
    }
}
