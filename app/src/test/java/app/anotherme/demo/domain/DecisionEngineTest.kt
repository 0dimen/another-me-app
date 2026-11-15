package app.anotherme.demo.domain

import org.junit.Assert.*
import org.junit.Test

class DecisionEngineTest {
    private val appointment = Appointment("meeting", 900, "station", "recipient")
    private fun input(eta: Long? = 908, call: Boolean? = false, guidance: Boolean? = false,
                      motion: Motion = Motion.DRIVING, asked: Boolean = false) =
        DecisionInput(appointment, eta, 888, call, guidance, motion, userAlreadyAsked = asked)

    @Test fun lateAndAvailableSpeaks() {
        val d = DecisionEngine().evaluate(input())
        assertEquals(Delivery.SPEAK_NOW, d.delivery)
        assertEquals(8L, d.proposal!!.lateMinutes)
    }
    @Test fun earlyArrivalIsDiscarded() {
        assertEquals(Delivery.DISCARD, DecisionEngine().evaluate(input(895)).delivery)
    }
    @Test fun exactlyOnTimeIsDiscarded() {
        assertEquals(Reason.ON_TIME, DecisionEngine().evaluate(input(900)).reason)
    }
    @Test fun callBlocksEvenWhenParked() {
        assertEquals(Delivery.WAIT, DecisionEngine().evaluate(input(call = true, motion = Motion.PARKED)).delivery)
    }
    @Test fun guidanceStillBlocksAfterCallEnds() {
        val engine = DecisionEngine()
        engine.evaluate(input(call = true))
        assertEquals(Delivery.WAIT, engine.evaluate(input(guidance = true)).delivery)
    }
    @Test fun bothBusySignalsBlock() {
        assertEquals(Delivery.WAIT, DecisionEngine().evaluate(input(call = true, guidance = true)).delivery)
    }
    @Test fun onlyLatestPendingSurvives() {
        val engine = DecisionEngine()
        engine.evaluate(input(908, call = true))
        engine.evaluate(input(910, call = true))
        engine.evaluate(input(908, guidance = true))
        assertEquals(908L, engine.pending!!.etaMinute)
        val result = engine.evaluate(input(908))
        assertEquals(Delivery.SPEAK_NOW, result.delivery)
        assertEquals(908L, result.proposal!!.etaMinute)
        assertNull(engine.pending)
    }
    @Test fun improvementWhileWaitingClearsPending() {
        val engine = DecisionEngine()
        engine.evaluate(input(call = true))
        assertEquals(Delivery.DISCARD, engine.evaluate(input(899)).delivery)
        assertNull(engine.pending)
    }
    @Test fun alreadyAnsweredQuestionDoesNotRepeat() {
        val engine = DecisionEngine()
        val p = Proposal(appointment, 908)
        engine.recordAnswer(p) // The direct-question answer was actually shown.
        assertEquals(Reason.ALREADY_ANSWERED, engine.evaluate(input(asked = true)).reason)
        assertEquals(Delivery.DISCARD, engine.evaluate(input()).delivery)
    }
    @Test fun askingWithoutDeliveredReplyDoesNotLoseCandidate() {
        val engine = DecisionEngine()
        assertEquals(Delivery.WAIT, engine.evaluate(input(asked = true)).delivery)
        assertNotNull(engine.pending)
        assertEquals(Delivery.SPEAK_NOW, engine.evaluate(input()).delivery)
    }
    @Test fun acknowledgementPreventsRepeatedProactiveDelivery() {
        val engine = DecisionEngine()
        engine.recordAnswer(engine.evaluate(input()).proposal!!)
        assertEquals(Delivery.DISCARD, engine.evaluate(input()).delivery)
    }
    @Test fun newEtaCanBeNewHelpButOldEtaStillStaysSuppressed() {
        val engine = DecisionEngine()
        engine.recordAnswer(Proposal(appointment, 908))
        assertEquals(Delivery.SPEAK_NOW, engine.evaluate(input(915)).delivery)
        assertEquals(Delivery.DISCARD, engine.evaluate(input(908)).delivery)
    }
    @Test fun cancelledAppointmentClearsWaiting() {
        val engine = DecisionEngine()
        engine.evaluate(input(call = true))
        assertEquals(Delivery.DISCARD, engine.evaluate(input().copy(appointment = appointment.copy(active = false))).delivery)
        assertNull(engine.pending)
    }
    @Test fun routeChangeInvalidatesWaiting() {
        val engine = DecisionEngine()
        engine.evaluate(input(call = true))
        assertEquals(Reason.INVALID, engine.evaluate(input().copy(routeDestination = "other place")).reason)
        assertNull(engine.pending)
    }
    @Test fun unlinkedTaskCannotPropose() {
        assertEquals(Delivery.DISCARD, DecisionEngine().evaluate(input().copy(appointment = appointment.copy(routeLinked = false))).delivery)
    }
    @Test fun missingEtaCannotPropose() {
        assertEquals(Delivery.DISCARD, DecisionEngine().evaluate(input(null)).delivery)
    }
    @Test fun unknownCallStateWaits() {
        assertEquals(Reason.UNKNOWN_SIGNAL, DecisionEngine().evaluate(input(call = null)).reason)
    }
    @Test fun unknownGuidanceStateWaits() {
        assertEquals(Delivery.WAIT, DecisionEngine().evaluate(input(guidance = null)).delivery)
    }
    @Test fun unknownVehicleStateWaits() {
        assertEquals(Delivery.WAIT, DecisionEngine().evaluate(input(motion = Motion.UNKNOWN)).delivery)
    }
    @Test fun staleSignalsWaitForFreshData() {
        assertEquals(Delivery.WAIT, DecisionEngine().evaluate(input().copy(signalFresh = false)).delivery)
    }
    @Test fun silenceDiscardsPendingAndCannotBeBypassedByParking() {
        val engine = DecisionEngine()
        engine.evaluate(input(call = true))
        assertEquals(Reason.DISABLED, engine.evaluate(input(motion = Motion.PARKED).copy(proactiveEnabled = false)).reason)
        assertNull(engine.pending)
    }
    @Test fun parkingAloneDoesNotCreateHelp() {
        assertEquals(Delivery.DISCARD, DecisionEngine().evaluate(input(895, motion = Motion.PARKED)).delivery)
    }
    @Test fun expiredEtaDiscardedBeforeDelivery() {
        assertEquals(Delivery.DISCARD, DecisionEngine().evaluate(input().copy(nowMinute = 909)).delivery)
    }
    @Test fun midnightUsesAbsoluteMinutes() {
        val a = appointment.copy(atMinute = 1438)
        val d = DecisionEngine().evaluate(input(1446).copy(appointment = a, nowMinute = 1430))
        assertEquals(8L, d.proposal!!.lateMinutes)
    }
    @Test fun fullReplayWaitsThenDeliversOnlyOnce() {
        val engine = DecisionEngine()
        val states = listOf(input(895), input(908, call = true), input(910, call = true),
            input(908, guidance = true), input(908))
        assertEquals(listOf(Delivery.DISCARD, Delivery.WAIT, Delivery.WAIT, Delivery.WAIT, Delivery.SPEAK_NOW),
            states.map { engine.evaluate(it).delivery })
        engine.recordAnswer(Proposal(appointment, 908))
        assertEquals(Delivery.DISCARD, engine.evaluate(input()).delivery)
    }
}
