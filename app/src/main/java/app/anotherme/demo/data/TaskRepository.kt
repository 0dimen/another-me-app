package app.anotherme.demo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import app.anotherme.demo.domain.Appointment

data class EntrustedTask(
    val id: String = UUID.randomUUID().toString(),
    val date: String = LocalDate.now().toString(),
    val time: String = "15:00",
    val destination: String = "",
    val recipient: String = "",
    val request: String = "",
) {
    // User-entered places have not been matched with any real navigation route.
    fun appointment(linked: Boolean = false) = Appointment(id,
        LocalDateTime.parse("${date}T$time").toEpochSecond(ZoneOffset.UTC) / 60,
        destination, recipient, routeLinked = linked)
}

private val Context.taskStore by preferencesDataStore("entrusted_tasks")

class TaskRepository(context: Context) {
    private val store = context.applicationContext.taskStore
    private val key = stringPreferencesKey("tasks_v1")
    val tasks = store.data.map { prefs -> decode(prefs[key] ?: "[]") }

    suspend fun save(task: EntrustedTask) = store.edit { prefs ->
        val tasks = decode(prefs[key] ?: "[]").toMutableList()
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index >= 0) tasks[index] = task else tasks.add(task)
        prefs[key] = encode(tasks)
    }

    suspend fun delete(id: String) = store.edit { prefs ->
        prefs[key] = encode(decode(prefs[key] ?: "[]").filterNot { it.id == id })
    }

    private fun encode(tasks: List<EntrustedTask>) = JSONArray().apply {
        tasks.forEach { t -> put(JSONObject().apply {
            put("id", t.id); put("date", t.date); put("time", t.time)
            put("destination", t.destination); put("recipient", t.recipient); put("request", t.request)
        }) }
    }.toString()

    private fun decode(value: String): List<EntrustedTask> {
        val array = JSONArray(value)
        return (0 until array.length()).map { i -> array.getJSONObject(i).let { o ->
            EntrustedTask(o.getString("id"), o.getString("date"), o.getString("time"),
                o.getString("destination"), o.getString("recipient"), o.getString("request"))
        } }
    }
}
