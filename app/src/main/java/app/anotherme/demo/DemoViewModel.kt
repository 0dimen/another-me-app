package app.anotherme.demo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.anotherme.demo.data.EntrustedTask
import app.anotherme.demo.data.TaskRepository
import app.anotherme.demo.domain.*
import app.anotherme.demo.source.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

enum class ScreenMode { MIN, MID, FULL }
enum class ButtonStyle { SATIN_BAR, DARK_KEY }

data class DemoUiState(
    val mode: ScreenMode = ScreenMode.MIN,
    val style: ButtonStyle = ButtonStyle.SATIN_BAR,
    val proactive: Boolean = true,
    val motion: Motion = Motion.PARKED,
    val inCall: Boolean = false,
    val guidance: Boolean = false,
    val running: Boolean = false,
    val stage: String = "주차 중 · 시연 준비",
    val eta: String = "14:55",
    val proposal: Proposal? = null,
    val hasAttention: Boolean = false,
    val result: String? = null,
    val error: String? = null,
)

class DemoViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = TaskRepository(application)
    val tasks = repo.tasks.catch { update { copy(error = "맡긴 일을 불러오지 못했어요. 앱 저장소를 확인해 주세요.") } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val _ui = MutableStateFlow(DemoUiState())
    val ui = _ui.asStateFlow()
    val sources = DemoSources()
    private var engine = DecisionEngine()
    private var replay: Job? = null
    private val day = LocalDate.now()
    private fun minute(time: String) = LocalDateTime.parse("${day}T$time").toEpochSecond(ZoneOffset.UTC) / 60
    private val demoAppointment get() = Appointment("demo-15", minute("15:00"), "강남역 2번 출구 앞", "대현")

    private fun update(block: DemoUiState.() -> DemoUiState) { _ui.update { it.block() } }
    fun cycleMode() = update { copy(mode = when (mode) {
        ScreenMode.MIN -> ScreenMode.MID; ScreenMode.MID -> ScreenMode.FULL; ScreenMode.FULL -> ScreenMode.MIN
    }) }
    fun expand() = update { copy(mode = ScreenMode.FULL) }
    fun minimize() = update { copy(mode = ScreenMode.MIN) }
    fun toggleStyle() = update { copy(style = if (style == ButtonStyle.SATIN_BAR) ButtonStyle.DARK_KEY else ButtonStyle.SATIN_BAR) }
    fun toggleProactive() {
        update { copy(proactive = !proactive, proposal = null, hasAttention = false, result = null) }
        evaluate()
    }
    fun toggleMotion() {
        if (ui.value.running) return
        sources.vehicle.value = VehicleState(if (ui.value.motion == Motion.PARKED) Motion.DRIVING else Motion.PARKED)
        update { copy(motion = sources.vehicle.value.motion, stage = if (motion == Motion.PARKED) "주행 중 · 입력 잠금" else "주차 중 · 시연 준비") }
    }

    fun reset() {
        replay?.cancel(); engine = DecisionEngine()
        sources.vehicle.value = VehicleState()
        sources.call.value = CallState()
        sources.navigation.value = NaviState(minute("14:55"), false, demoAppointment.destination)
        update { copy(motion = Motion.PARKED, running = false, inCall = false, guidance = false,
            stage = "주차 중 · 시연 준비", eta = "14:55", proposal = null, hasAttention = false, result = null) }
    }

    fun playScenario() {
        reset()
        update { copy(running = true, motion = Motion.DRIVING, stage = "14:48 · 처음 도착 예상 14:55") }
        sources.vehicle.value = VehicleState(Motion.DRIVING)
        replay = viewModelScope.launch {
            evaluate()
            delay(1800)
            sources.call.value = CallState(true)
            sources.navigation.value = NaviState(minute("15:08"), false, demoAppointment.destination)
            update { copy(inCall = true, eta = "15:08", stage = "통화 중 · 잠시 기다릴게요") }
            evaluate()
            delay(3200)
            // Atomic snapshot: call ends while guidance is already speaking. Never a free gap.
            sources.call.value = CallState(false)
            sources.navigation.value = sources.navigation.value.copy(guidanceSpeaking = true)
            update { copy(inCall = false, guidance = true, stage = "길 안내 중 · 잠시 기다릴게요") }
            evaluate()
            delay(3200)
            sources.navigation.value = sources.navigation.value.copy(guidanceSpeaking = false)
            update { copy(guidance = false, running = false, stage = "14:48 · 지금 필요한 도움") }
            evaluate()
        }
    }

    private fun evaluate() {
        val n = sources.navigation.value
        val decision = engine.evaluate(DecisionInput(demoAppointment, n.etaMinute, minute("14:48"),
            sources.call.value.inCall, n.guidanceSpeaking, sources.vehicle.value.motion,
            proactiveEnabled = ui.value.proactive, routeDestination = n.destination, signalFresh = n.fresh))
        when (decision.delivery) {
            Delivery.SPEAK_NOW -> {
                val p = decision.proposal ?: return
                update { copy(proposal = p, hasAttention = true, result = null,
                    mode = if (mode == ScreenMode.MIN) ScreenMode.MID else mode) }
                engine.recordAnswer(p) // The text has been delivered; this is not SMS consent.
            }
            Delivery.WAIT -> update { copy(proposal = null, hasAttention = decision.proposal != null) }
            Delivery.DISCARD -> update { copy(hasAttention = false) }
        }
    }

    fun askAi() {
        if (ui.value.inCall || ui.value.guidance) return
        val n = sources.navigation.value
        if (n.etaMinute != null && n.etaMinute > demoAppointment.atMinute) {
            val p = Proposal(demoAppointment, n.etaMinute)
            engine.recordAnswer(p)
            update { copy(proposal = p, hasAttention = true, result = null,
                mode = if (mode == ScreenMode.MIN) ScreenMode.MID else mode,
                stage = "직접 물어본 도착 예상") }
        } else update { copy(mode = if (mode == ScreenMode.MIN) ScreenMode.MID else mode) }
    }

    fun respond(send: Boolean) {
        val p = ui.value.proposal ?: return
        engine.recordAnswer(p)
        update { copy(proposal = null, hasAttention = false,
            result = if (send) "동의를 확인했어요. 시연이므로 문자는 보내지 않았어요." else "괜찮아요. 이 지연은 다시 먼저 알리지 않을게요.") }
    }

    fun save(task: EntrustedTask, onSaved: () -> Unit, onFinished: () -> Unit = {}) {
        if (ui.value.motion != Motion.PARKED) return
        viewModelScope.launch {
            try { repo.save(task); onSaved() }
            catch (_: Exception) { update { copy(error = "저장하지 못했어요. 다시 시도해 주세요.") } }
            finally { onFinished() }
        }
    }
    fun delete(id: String, onDeleted: () -> Unit) {
        if (ui.value.motion != Motion.PARKED) return
        viewModelScope.launch {
            try { repo.delete(id); onDeleted() }
            catch (_: Exception) { update { copy(error = "삭제하지 못했어요. 다시 시도해 주세요.") } }
        }
    }
    fun clearError() = update { copy(error = null) }
}
