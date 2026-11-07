package com.dhikr.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import java.util.Random

@Entity(tableName = "counters")
data class Counter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Int = 0,               // 0 = no target
    val count: Int = 0,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface CounterDao {
    @Query("SELECT * FROM counters ORDER BY createdAt DESC") fun all(): Flow<List<Counter>>
    @Query("SELECT * FROM counters WHERE id = :id") suspend fun get(id: Long): Counter?
    @Insert suspend fun insert(c: Counter): Long
    @Update suspend fun update(c: Counter)
    @Delete suspend fun delete(c: Counter)
}

@Database(entities = [Counter::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun counters(): CounterDao
    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(ctx: Context) = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "dhikr.db").build().also { inst = it }
        }
    }
}

data class AppSettings(
    val tick: Boolean = true,
    val completeSound: Boolean = true,
    val onboarded: Boolean = false,
    val bannerDismissedDay: Long = -1
)
data class Stats(val taps: Long = 0, val targets: Int = 0)

/** Settings + lifetime stats in SharedPreferences, exposed as StateFlows. */
class PrefsStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("dhikr_prefs", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(AppSettings(
        sp.getBoolean("tick", true), sp.getBoolean("complete", true),
        sp.getBoolean("onboarded", false), sp.getLong("bannerDay", -1)))
    private val _stats = MutableStateFlow(Stats(sp.getLong("taps", 0), sp.getInt("targets", 0)))
    val settings = _settings.asStateFlow()
    val stats = _stats.asStateFlow()

    fun setTick(v: Boolean) { sp.edit().putBoolean("tick", v).apply(); _settings.update { it.copy(tick = v) } }
    fun setComplete(v: Boolean) { sp.edit().putBoolean("complete", v).apply(); _settings.update { it.copy(completeSound = v) } }
    fun setOnboarded() { sp.edit().putBoolean("onboarded", true).apply(); _settings.update { it.copy(onboarded = true) } }
    fun dismissBanner(day: Long) { sp.edit().putLong("bannerDay", day).apply(); _settings.update { it.copy(bannerDismissedDay = day) } }
    fun addTap(targetHit: Boolean) {
        val s = _stats.value.let { it.copy(taps = it.taps + 1, targets = it.targets + if (targetHit) 1 else 0) }
        sp.edit().putLong("taps", s.taps).putInt("targets", s.targets).apply()
        _stats.value = s
    }
}

data class DailyMessage(val type: String, val text: String, val source: String)

class Repository(private val ctx: Context) {
    private val dao = AppDb.get(ctx).counters()
    val prefs = PrefsStore(ctx)
    val counters: Flow<List<Counter>> = dao.all()

    /** Loaded from assets/messages.json — append entries there to expand. */
    private val messages: List<DailyMessage> by lazy {
        val arr = JSONArray(ctx.assets.open("messages.json").bufferedReader().use { it.readText() })
        List(arr.length()) { i -> arr.getJSONObject(i).let { DailyMessage(it.getString("type"), it.getString("text"), it.optString("source")) } }
    }
    /** Deterministic "random" pick that changes every day (epoch day as seed). */
    fun messageFor(epochDay: Long): DailyMessage =
        messages[Random(epochDay * 7919).nextInt(messages.size)]

    suspend fun add(name: String, target: Int) { dao.insert(Counter(name = name.trim(), target = target)) }
    suspend fun delete(c: Counter) = dao.delete(c)
    suspend fun reset(c: Counter) = dao.update(c.copy(count = 0, completed = false))

    /** Returns null if counter missing, else whether this tap just hit the target. */
    suspend fun tap(id: Long): Boolean? {
        val c = dao.get(id) ?: return null
        val n = c.count + 1
        val hit = c.target > 0 && n == c.target
        dao.update(c.copy(count = n, completed = c.target > 0 && n >= c.target))
        prefs.addTap(hit)
        return hit
    }
}
