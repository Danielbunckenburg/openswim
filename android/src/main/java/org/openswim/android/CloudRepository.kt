package org.openswim.android

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import org.openswim.core.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.KeyStore
import java.time.LocalDate
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/** All requests use the publishable key and, when signed in, the user's JWT. RLS remains authoritative. */
internal class CloudRepository(context: Context) : CompanionRepository {
    private val appContext = context.applicationContext
    private val worker = Executors.newSingleThreadExecutor()
    private val pendingTasks = AtomicInteger()
    private val main = Handler(Looper.getMainLooper())
    private val url = appContext.getString(R.string.supabase_url).trimEnd('/')
    private val key = appContext.getString(R.string.supabase_publishable_key)
    private val sessionStore = SessionStore(appContext)
    private var session: JSONObject? = null

    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var email by mutableStateOf<String?>(null)
        private set
    var displayName by mutableStateOf<String?>(null)
        private set
    var lastSyncAt by mutableStateOf<java.time.Instant?>(null)
        private set
    var workoutDescriptions by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var ready by mutableStateOf(false)
        private set
    override var workouts by mutableStateOf<List<Workout>>(emptyList())
        private set
    override var plans by mutableStateOf<List<TrainingPlan>>(emptyList())
        private set
    override var scheduled by mutableStateOf<List<ScheduledSwim>>(emptyList())
        private set
    override var completed by mutableStateOf<List<CompletedWorkout>>(emptyList())
        private set
    override fun workout(id: String) = workouts.firstOrNull { it.id == id }

    init { refresh() }

    fun refresh() = runTask {
        if (url.isBlank() || key.isBlank()) error("Supabase connection is not configured in local.properties")
        session = sessionStore.read()?.let(::JSONObject)
        if (session != null) {
            try { refreshSession() } catch (failure: ApiException) {
                if (failure.status == 400 || failure.status == 401) clearSession() else throw failure
            }
        }
        loadData()
    }

    fun signIn(address: String, password: String) = runTask {
        val result = request("/auth/v1/token?grant_type=password", "POST", JSONObject().put("email", address.trim()).put("password", password)) as JSONObject
        saveSession(result)
        loadData()
    }

    fun signUp(address: String, password: String) = runTask {
        val redirect = URLEncoder.encode("org.openswim.android://auth/callback", "UTF-8")
        val result = request("/auth/v1/signup?redirect_to=$redirect", "POST", JSONObject().put("email", address.trim()).put("password", password)) as JSONObject
        if (result.optString("access_token").isNotBlank()) {
            saveSession(result)
            loadData()
            main.post { notice = "Account created and connected to OpenSwim cloud." }
        } else {
            main.post { notice = "Check your email to confirm your account, then sign in." }
        }
    }

    fun acceptAuthRedirect(uri: Uri?) {
        if (uri?.scheme != "org.openswim.android" || uri.host != "auth" || uri.path != "/callback") return
        val params = Uri.parse("https://callback.invalid/?${uri.fragment.orEmpty()}")
        val access = params.getQueryParameter("access_token")
        val refresh = params.getQueryParameter("refresh_token")
        val failure = params.getQueryParameter("error_description")
        if (!failure.isNullOrBlank()) { main.post { error = failure }; return }
        if (access.isNullOrBlank() || refresh.isNullOrBlank()) {
            main.post { notice = "Email confirmed. Sign in to continue." }
            return
        }
        runTask {
            val user = request("/auth/v1/user", jwt = access) as JSONObject
            saveSession(JSONObject().put("access_token", access).put("refresh_token", refresh).put("user", user))
            loadData()
        }
    }

    fun signOut() = runTask {
        try { request("/auth/v1/logout", "POST") } catch (_: Exception) { /* local sign out must still work */ }
        clearSession()
        main.post { workouts = emptyList(); plans = emptyList(); scheduled = emptyList(); completed = emptyList(); displayName = null }
        loadData()
    }

    private fun runTask(action: () -> Unit) {
        pendingTasks.incrementAndGet()
        main.post { busy = true; error = null; notice = null }
        worker.execute {
            try { action() } catch (exception: Exception) {
                main.post { error = exception.message ?: "Connection failed" }
            } finally {
                val remaining = pendingTasks.decrementAndGet()
                main.post { busy = remaining > 0; ready = true }
            }
        }
    }

    private fun saveSession(value: JSONObject) {
        session = value
        sessionStore.write(value.toString())
        main.post { email = value.optJSONObject("user")?.optString("email") }
    }

    private fun clearSession() {
        session = null
        sessionStore.clear()
        main.post { email = null }
    }

    private fun refreshSession() {
        val token = session?.optString("refresh_token").orEmpty()
        if (token.isBlank()) error("No refresh token")
        val result = request("/auth/v1/token?grant_type=refresh_token", "POST", JSONObject().put("refresh_token", token)) as JSONObject
        saveSession(result)
    }

    private fun loadData() {
        val token = session?.optString("access_token")
        val workoutRows = getRows("workouts?select=id,title,description,category,estimated_minutes,distance_unit,workout_sections(name,position,workout_steps(position,repetitions,distance_amount,stroke,intensity,equipment,rest_after_seconds,target_pace_seconds_per_100,target_interval_seconds,note))&order=created_at.desc", token)
        val loadedWorkouts = workoutRows.objects().mapNotNull { row ->
            val sections = row.optJSONArray("workout_sections")?.objects().orEmpty().sortedBy { it.optInt("position") }.mapNotNull { section ->
                val sets = section.optJSONArray("workout_steps")?.objects().orEmpty().sortedBy { it.optInt("position") }.map { step ->
                    val equipment = step.optJSONArray("equipment")?.strings().orEmpty().map { Equipment.valueOf(it) }.toSet()
                    RepetitionSet(step.getInt("repetitions"), WorkoutStep(
                        Distance(step.getInt("distance_amount"), DistanceUnit.valueOf(row.getString("distance_unit"))),
                        Stroke.valueOf(step.getString("stroke")), Intensity.valueOf(step.getString("intensity")), equipment,
                        RestDuration(step.getInt("rest_after_seconds")),
                        step.intOrNull("target_pace_seconds_per_100")?.let(::TargetPace),
                        step.intOrNull("target_interval_seconds")?.let(::TargetInterval), step.stringOrNull("note")
                    ))
                }
                if (sets.isEmpty()) null else WorkoutSection(section.getString("name"), sets)
            }
            if (sections.isEmpty()) null else Workout(row.getString("id"), row.getString("title"), row.getString("category"), row.getInt("estimated_minutes"), sections)
        }
        var loadedPlans = emptyList<TrainingPlan>()
        var loadedScheduled = emptyList<ScheduledSwim>()
        var loadedCompleted = emptyList<CompletedWorkout>()
        var loadedDisplayName: String? = null
        if (token != null) {
            loadedDisplayName = getRows("profiles?select=display_name&limit=1", token).objects().firstOrNull()?.stringOrNull("display_name")
            val planRows = getRows("training_plans?select=id,title,objective,duration_weeks,workouts_per_week,difficulty,training_plan_workouts(id,workout_id,week_number,day_number,position)&order=created_at.desc", token)
            loadedPlans = planRows.objects().map { p ->
                val schedule = p.optJSONArray("training_plan_workouts")?.objects().orEmpty().sortedWith(compareBy({ it.getInt("week_number") }, { it.getInt("day_number") }, { it.getInt("position") })).map { item ->
                    ScheduledWorkout(item.getString("id"), item.getString("workout_id"), java.time.DayOfWeek.of(item.getInt("day_number")).name.lowercase().replaceFirstChar { it.uppercase() }, item.getInt("week_number"))
                }
                TrainingPlan(p.getString("id"), p.getString("title"), p.stringOrNull("objective").orEmpty(), p.getInt("duration_weeks"), p.getInt("workouts_per_week"), p.stringOrNull("difficulty").orEmpty(), 1, 0, schedule)
            }
            loadedScheduled = getRows("scheduled_workouts?select=id,workout_id,plan_id,scheduled_date,status&order=scheduled_date.asc", token).objects().map { row ->
                ScheduledSwim(row.getString("id"), row.stringOrNull("workout_id"), row.stringOrNull("plan_id"), row.getString("scheduled_date"), row.getString("status"))
            }
            loadedCompleted = getRows("completed_workouts?select=id,workout_id,started_at,distance_amount,distance_unit,duration_seconds&order=started_at.desc", token).objects().map { row ->
                CompletedWorkout(row.getString("id"), row.stringOrNull("workout_id").orEmpty(), LocalDate.parse(row.getString("started_at").take(10)).toString(), Distance(row.getInt("distance_amount"), DistanceUnit.valueOf(row.getString("distance_unit"))), (row.getInt("duration_seconds") + 59) / 60)
            }
        }
        val descriptions = workoutRows.objects().mapNotNull { row -> row.stringOrNull("description")?.let { row.getString("id") to it } }.toMap()
        main.post { workouts = loadedWorkouts; plans = loadedPlans; scheduled = loadedScheduled; completed = loadedCompleted; displayName = loadedDisplayName; workoutDescriptions = descriptions; lastSyncAt = java.time.Instant.now() }
    }

    fun saveSwim(workoutId: String?, distanceMeters: Int, durationSeconds: Int, poolLength: Int, startedAt: java.time.Instant) = runTask {
        val userId = session?.optJSONObject("user")?.optString("id").orEmpty()
        if (userId.isBlank()) error("Sign in to save your swim")
        val body = JSONObject()
            .put("user_id", userId)
            .put("workout_id", workoutId ?: JSONObject.NULL)
            .put("started_at", startedAt.toString())
            .put("completed_at", java.time.Instant.now().toString())
            .put("distance_amount", distanceMeters)
            .put("distance_unit", "METERS")
            .put("duration_seconds", durationSeconds)
            .put("pool_length", poolLength)
        request("/rest/v1/completed_workouts", "POST", body)
        loadData()
        main.post { notice = "Swim saved to your account." }
    }

    private fun getRows(path: String, token: String?): JSONArray = request("/rest/v1/$path", jwt = token) as JSONArray

    private fun request(path: String, method: String = "GET", body: JSONObject? = null, jwt: String? = session?.optString("access_token")): Any {
        val connection = (URL(url + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", key)
            setRequestProperty("Accept", "application/json")
            if (!jwt.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $jwt")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
        }
        try {
            val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) {
                val message = runCatching { JSONObject(response).optString("msg").ifBlank { JSONObject(response).optString("message") } }.getOrDefault("Request failed")
                throw ApiException(connection.responseCode, message)
            }
            return if (response.trimStart().startsWith("[")) JSONArray(response) else if (response.isBlank()) JSONObject() else JSONObject(response)
        } finally { connection.disconnect() }
    }
}

private class ApiException(val status: Int, detail: String) : Exception("$status: $detail")

private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
private fun JSONArray.strings() = (0 until length()).map { getString(it) }
private fun JSONObject.stringOrNull(name: String): String? = if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }
private fun JSONObject.intOrNull(name: String): Int? = if (isNull(name)) null else getInt(name)

/** Refresh tokens are encrypted with a device-bound Android Keystore key. */
private class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("openswim_session", Context.MODE_PRIVATE)
    private val alias = "openswim_session_key"
    private fun key(): java.security.Key {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        store.getKey(alias, null)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun write(value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        preferences.edit().putString("iv", Base64.getEncoder().encodeToString(cipher.iv))
            .putString("session", Base64.getEncoder().encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)))).apply()
    }
    fun read(): String? = try {
        val iv = preferences.getString("iv", null) ?: return null
        val data = preferences.getString("session", null) ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.getDecoder().decode(iv))) }
        String(cipher.doFinal(Base64.getDecoder().decode(data)), Charsets.UTF_8)
    } catch (_: Exception) { clear(); null }
    fun clear() { preferences.edit().clear().apply() }
}
