package app.anotherme.demo.domain

/** Absolute epoch minutes, so crossing midnight does not turn a delay into an early arrival. */
data class Appointment(
    val id: String,
    val atMinute: Long,
    val destination: String,
    val recipient: String,
    val active: Boolean = true,
    val routeLinked: Boolean = true,
)

enum class Motion { PARKED, DRIVING, UNKNOWN }
enum class Delivery { SPEAK_NOW, WAIT, DISCARD }
enum class Reason { LATE, BUSY, UNKNOWN_SIGNAL, DISABLED, INVALID, ON_TIME, ALREADY_ANSWERED }

data class DecisionInput(
    val appointment: Appointment?,
    val etaMinute: Long?,
    val nowMinute: Long,
    val inCall: Boolean?,
    val guidanceSpeaking: Boolean?,
    val motion: Motion,
    val userAlreadyAsked: Boolean = false,
    val proactiveEnabled: Boolean = true,
    val routeDestination: String? = appointment?.destination,
    val signalFresh: Boolean = true,
)

data class Proposal(val appointment: Appointment, val etaMinute: Long) {
    val lateMinutes: Long get() = etaMinute - appointment.atMinute
    val key: String get() = "${appointment.id}:${appointment.atMinute}:$etaMinute"
}
data class Decision(val delivery: Delivery, val reason: Reason, val proposal: Proposal? = null)

/** No Android dependency, no timers, no I/O. A single slot replaces queued ETA updates.
 * SPEAK_NOW means the presentation layer may deliver; acknowledge only after it was shown.
 * A user asking is distinct from an answer actually being delivered (recordAnswer).
 */
class DecisionEngine {
    var pending: Proposal? = null
        private set
    private val answered = mutableSetOf<String>()

    fun evaluate(input: DecisionInput): Decision {
        val a = input.appointment
        if (a == null || !a.active || !a.routeLinked || a.destination != input.routeDestination ||
            a.recipient.isBlank() || a.destination.isBlank() || input.etaMinute == null ||
            input.nowMinute > input.etaMinute) return discard(Reason.INVALID)
        val p = Proposal(a, input.etaMinute)
        if (p.lateMinutes <= 0) return discard(Reason.ON_TIME)
        if (p.key in answered) return discard(Reason.ALREADY_ANSWERED)
        if (!input.proactiveEnabled) return discard(Reason.DISABLED)
        pending = p // Latest candidate only, including updates received during a call.
        if (!input.signalFresh || input.motion == Motion.UNKNOWN || input.inCall == null ||
            input.guidanceSpeaking == null) return Decision(Delivery.WAIT, Reason.UNKNOWN_SIGNAL, p)
        if (input.inCall || input.guidanceSpeaking) return Decision(Delivery.WAIT, Reason.BUSY, p)
        // Merely asking is not proof of delivery: caller must recordAnswer after showing the reply.
        if (input.userAlreadyAsked) return Decision(Delivery.WAIT, Reason.BUSY, p)
        pending = null
        return Decision(Delivery.SPEAK_NOW, Reason.LATE, p)
    }

    fun recordAnswer(proposal: Proposal) {
        answered += proposal.key
        if (pending?.key == proposal.key) pending = null
    }

    private fun discard(reason: Reason): Decision {
        pending = null
        return Decision(Delivery.DISCARD, reason)
    }
}
