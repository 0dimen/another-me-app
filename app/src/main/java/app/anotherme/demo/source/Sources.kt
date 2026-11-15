package app.anotherme.demo.source

import app.anotherme.demo.domain.Motion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class VehicleState(val motion: Motion = Motion.PARKED)
data class NaviState(val etaMinute: Long? = null, val guidanceSpeaking: Boolean? = false,
                     val destination: String? = null, val fresh: Boolean = true)
data class CallState(val inCall: Boolean? = false)

interface VehicleSource { val vehicle: StateFlow<VehicleState> }
interface NaviSource { val navigation: StateFlow<NaviState> }
interface CallSource { val call: StateFlow<CallState> }

/** The only implementation shipped in this APK. No real car/phone permissions are requested. */
class DemoSources : VehicleSource, NaviSource, CallSource {
    override val vehicle = MutableStateFlow(VehicleState())
    override val navigation = MutableStateFlow(NaviState())
    override val call = MutableStateFlow(CallState())
}

/* TODO(Pleos integration): implement the three interfaces in a separate SDK-backed adapter.
 * Vehicle: verify installed SDK, permissions and CarPropertyManager callbacks; use
 * CarUxRestrictionsManager for actual UI restrictions, never infer permission from gear alone.
 * NaviHelper: verify route/destination and ETA callback units/time zone/freshness. Route/TBT
 * callbacks do NOT establish the beginning/end of audible guidance. Unknown => null => WAIT.
 * Call: verify whether call occupancy is accessible on the target car; do not read call audio.
 * Unregister callbacks on close; connection loss must emit UNKNOWN/null/fresh=false.
 * No Pleos imports/dependencies here: a missing proprietary SDK must not block demo builds.
 */
