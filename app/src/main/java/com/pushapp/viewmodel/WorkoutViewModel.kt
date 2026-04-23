package com.pushapp.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pushapp.model.FeedComment
import com.pushapp.model.User
import com.pushapp.model.WorkoutEntry
import com.pushapp.repository.AuthRepository
import com.pushapp.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class HistoryPeriod { DAY, WEEK, MONTH }

class WorkoutViewModel(app: Application) : AndroidViewModel(app) {

    private val workoutRepo = WorkoutRepository()
    private val authRepo    = AuthRepository()
    private val prefs       = app.getSharedPreferences("pushapp_prefs", Context.MODE_PRIVATE)

    private val _todayWorkout = MutableStateFlow<WorkoutEntry?>(null)
    val todayWorkout: StateFlow<WorkoutEntry?> = _todayWorkout

    private val _isTodayLoading = MutableStateFlow(true)
    val isTodayLoading: StateFlow<Boolean> = _isTodayLoading

    private val _saveSuccess = MutableStateFlow<Boolean?>(null)
    val saveSuccess: StateFlow<Boolean?> = _saveSuccess

    private val _weeklyInsight = MutableStateFlow<String?>(null)
    val weeklyInsight: StateFlow<String?> = _weeklyInsight

    private val _feed = MutableStateFlow<List<WorkoutEntry>>(emptyList())
    val feed: StateFlow<List<WorkoutEntry>> = _feed

    private val _historyEntries = MutableStateFlow<List<WorkoutEntry>>(emptyList())
    val historyEntries: StateFlow<List<WorkoutEntry>> = _historyEntries

    private val _compareMyEntries = MutableStateFlow<List<WorkoutEntry>>(emptyList())
    val compareMyEntries: StateFlow<List<WorkoutEntry>> = _compareMyEntries

    private val _compareOtherEntries = MutableStateFlow<List<WorkoutEntry>>(emptyList())
    val compareOtherEntries: StateFlow<List<WorkoutEntry>> = _compareOtherEntries

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _calendarMyDates = MutableStateFlow<Set<String>>(emptySet())
    val calendarMyDates: StateFlow<Set<String>> = _calendarMyDates

    private val _calendarFriendDates = MutableStateFlow<Set<String>>(emptySet())
    val calendarFriendDates: StateFlow<Set<String>> = _calendarFriendDates

    private val _selectedCalendarFriend = MutableStateFlow<User?>(null)
    val selectedCalendarFriend: StateFlow<User?> = _selectedCalendarFriend

    // UID выбранного пользователя для сравнения — восстанавливается из prefs
    private val _selectedCompareUserId = MutableStateFlow<String?>(
        prefs.getString(PREF_COMPARE_UID, null)
    )
    val selectedCompareUserId: StateFlow<String?> = _selectedCompareUserId

    private val _comments = MutableStateFlow<Map<String, List<FeedComment>>>(emptyMap())
    val comments: StateFlow<Map<String, List<FeedComment>>> = _comments

    private val today: String
        get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    companion object {
        private const val PREF_COMPARE_UID         = "selected_compare_uid"
        private const val PREF_CALENDAR_FRIEND_UID = "selected_calendar_friend_uid"
    }

    // ── Тренировки ───────────────────────────────────────────────────────────

    fun loadTodayWorkout() {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            _isTodayLoading.value = true
            _todayWorkout.value = workoutRepo.getTodayWorkout(uid, today)
            _isTodayLoading.value = false
        }
    }

    fun saveWorkout(
        pushups: Int,
        squats: Int,
        pullups: Int,
        abs: Int,
        username: String,
        comment: String = "",
        skipped: Boolean = false
    ) {
        val uid = authRepo.currentUserId ?: return
        val entry = WorkoutEntry(
            userId    = uid,
            username  = username,
            date      = today,
            pushups   = pushups,
            squats    = squats,
            pullups   = pullups,
            abs       = abs,
            comment   = comment,
            skipped   = skipped,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            val result = workoutRepo.saveWorkout(entry)
            _saveSuccess.value = result.isSuccess
            if (result.isSuccess) {
                _todayWorkout.value = entry
                if (!skipped) computeWeeklyInsight(uid, pushups, squats, pullups, abs)
            }
        }
    }

    private suspend fun computeWeeklyInsight(
        uid: String,
        todayPushups: Int,
        todaySquats: Int,
        todayPullups: Int,
        todayAbs: Int
    ) {
        val fmt       = DateTimeFormatter.ISO_LOCAL_DATE
        val yesterday = LocalDate.now().minusDays(1).format(fmt)
        val weekAgo   = LocalDate.now().minusDays(7).format(fmt)
        val week      = workoutRepo.getUserWorkoutsForPeriod(uid, weekAgo, yesterday)

        val lines = mutableListOf<String>()

        fun check(todayVal: Int, getVal: (WorkoutEntry) -> Int, name: String, emoji: String) {
            if (todayVal == 0) return
            val relevant = week.filter { getVal(it) > 0 }
            if (relevant.size < 3) return
            val avg  = relevant.map { getVal(it) }.average()
            val diff = todayVal - avg
            lines += when {
                diff >= 1  -> "$emoji $name: на ${diff.toInt()} больше среднего (${avg.toInt()}). Огонь!"
                diff <= -1 -> "$emoji $name: среднее ${avg.toInt()}, сегодня ${todayVal}. Поднажми!"
                else       -> "$emoji $name: ровно в среднем (${avg.toInt()}). Чуть больше?"
            }
        }

        check(todayPushups, { it.pushups }, "Отжимания",    "💪")
        check(todaySquats,  { it.squats },  "Приседания",   "🦵")
        check(todayPullups, { it.pullups }, "Подтягивания", "🏋️")
        check(todayAbs,     { it.abs },     "Пресс",        "🤸")

        if (lines.isNotEmpty()) {
            _weeklyInsight.value = lines.joinToString("\n")
        }
    }

    fun dismissWeeklyInsight() { _weeklyInsight.value = null }

    fun resetSaveState() { _saveSuccess.value = null }

    // ── Лента ────────────────────────────────────────────────────────────────

    fun loadFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            val entries = workoutRepo.getFeed()
            _feed.value = entries
            _isLoading.value = false
            // Предзагрузка комментариев для первых 10 записей
            entries.take(10).forEach { entry ->
                launch {
                    val list = workoutRepo.getComments(entry.id)
                    _comments.value = _comments.value + (entry.id to list)
                }
            }
        }
    }

    // ── История ──────────────────────────────────────────────────────────────

    fun loadHistory(period: HistoryPeriod) {
        val uid = authRepo.currentUserId ?: return
        val (from, to) = periodRange(period)
        viewModelScope.launch {
            _isLoading.value = true
            _historyEntries.value = workoutRepo.getUserWorkoutsForPeriod(uid, from, to)
            _isLoading.value = false
        }
    }

    // ── Пользователи ─────────────────────────────────────────────────────────

    fun loadAllUsers() {
        viewModelScope.launch {
            _allUsers.value = authRepo.getAllUsers()
            // Восстанавливаем выбранного друга в календаре после загрузки списка
            val savedFriendUid = prefs.getString(PREF_CALENDAR_FRIEND_UID, null)
            if (savedFriendUid != null && _selectedCalendarFriend.value == null) {
                _selectedCalendarFriend.value = _allUsers.value.find { it.uid == savedFriendUid }
            }
        }
    }

    // ── Сравнение ─────────────────────────────────────────────────────────────

    fun selectCompareUser(uid: String?) {
        _selectedCompareUserId.value = uid
        prefs.edit().apply {
            if (uid != null) putString(PREF_COMPARE_UID, uid) else remove(PREF_COMPARE_UID)
        }.apply()
    }

    fun loadCompare(otherUserId: String, period: HistoryPeriod) {
        val myUid = authRepo.currentUserId ?: return
        val (from, to) = periodRange(period)
        viewModelScope.launch {
            _isLoading.value = true
            _compareMyEntries.value    = workoutRepo.getUserWorkoutsForPeriod(myUid, from, to)
            _compareOtherEntries.value = workoutRepo.getUserWorkoutsForPeriod(otherUserId, from, to)
            _isLoading.value = false
        }
    }

    // ── Календарь ─────────────────────────────────────────────────────────────

    fun selectCalendarFriend(user: User?) {
        _selectedCalendarFriend.value = user
        prefs.edit().apply {
            if (user != null) putString(PREF_CALENDAR_FRIEND_UID, user.uid)
            else remove(PREF_CALENDAR_FRIEND_UID)
        }.apply()
    }

    fun loadCalendarMonth(yearMonth: YearMonth) {
        val myUid = authRepo.currentUserId ?: return
        val fmt   = DateTimeFormatter.ISO_LOCAL_DATE
        val from  = yearMonth.atDay(1).format(fmt)
        val to    = yearMonth.atEndOfMonth().format(fmt)
        viewModelScope.launch {
            val myEntries = workoutRepo.getUserWorkoutsForPeriod(myUid, from, to)
            _calendarMyDates.value = myEntries.map { it.date }.toSet()

            val friendUid = _selectedCalendarFriend.value?.uid
            if (friendUid != null) {
                val friendEntries = workoutRepo.getUserWorkoutsForPeriod(friendUid, from, to)
                _calendarFriendDates.value = friendEntries.map { it.date }.toSet()
            } else {
                _calendarFriendDates.value = emptySet()
            }
        }
    }

    // ── Комментарии ───────────────────────────────────────────────────────────

    fun loadComments(workoutId: String) {
        viewModelScope.launch {
            val list = workoutRepo.getComments(workoutId)
            _comments.value = _comments.value + (workoutId to list)
        }
    }

    fun addComment(workoutId: String, text: String, username: String) {
        val uid = authRepo.currentUserId ?: return
        val comment = FeedComment(
            workoutId = workoutId,
            userId    = uid,
            username  = username,
            text      = text,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            val result = workoutRepo.addComment(workoutId, comment)
            if (result.isSuccess) loadComments(workoutId)
        }
    }

    // ── Вспомогательное ───────────────────────────────────────────────────────

    private fun periodRange(period: HistoryPeriod): Pair<String, String> {
        val now  = LocalDate.now()
        val from = when (period) {
            HistoryPeriod.DAY   -> now
            HistoryPeriod.WEEK  -> now.minusDays(6)
            HistoryPeriod.MONTH -> now.minusDays(29)
        }
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        return from.format(fmt) to now.format(fmt)
    }
}
