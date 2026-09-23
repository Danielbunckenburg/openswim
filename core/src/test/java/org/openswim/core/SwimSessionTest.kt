package org.openswim.core

import org.junit.Assert.*
import org.junit.Test

class SwimSessionTest {
    @Test fun poolModeOnlyChangesDistanceOnManualEvents() {
        var session = SwimSession.pool(50).start()
        repeat(90) { session = session.tick() }
        assertEquals(90, session.elapsedSeconds)
        assertEquals(0, session.completedDistance)
        assertEquals(0, session.completedLengths)
        session = session.logLength().logManualDistance(ManualSwimType.KICK, 150)
        assertEquals(200, session.completedDistance)
        assertEquals(4, session.completedLengths)
        assertEquals(session, session.logManualDistance(ManualSwimType.DRILL, 25))
        val paused = session.pause()
        assertEquals(paused, paused.tick())
        assertEquals(paused, paused.logLength())
        assertEquals(SessionPhase.Active, paused.resume().phase)
        assertEquals(SessionPhase.Complete, paused.end().phase)
    }

    @Test fun lockBlocksTouchesUntilExplicitUnlock() {
        val locked = SwimSession.pool(25).start().lock()
        assertTrue(locked.locked)
        assertEquals(locked, locked.logLength())
        assertEquals(locked, locked.logManualDistance(ManualSwimType.DRILL, 100))
        assertEquals(locked, locked.pause())
        assertEquals(locked, locked.end())
        assertEquals(25, locked.unlock().logLength().completedDistance)
    }

    @Test fun guidedKickEntryAdvancesMatchingRepetitionWithoutDoubleCounting() {
        val workout = SampleWorkouts.all.first { it.id == "easy-mixed" }
        var session = SwimSession.guided(workout, 25).start().advance()
        assertEquals(200, session.completedDistance)
        assertEquals(Stroke.KICK, session.current!!.set.step.stroke)
        session = session.logManualDistance(ManualSwimType.KICK, 50)
        assertEquals(250, session.completedDistance)
        assertEquals(2, session.guided!!.completedRepetitions)
        assertTrue(session.phase is SessionPhase.Rest)
        session = session.skipRest().logManualDistance(ManualSwimType.DRILL, 25)
        assertEquals(275, session.completedDistance)
        assertEquals(2, session.guided!!.completedRepetitions)
    }
}
