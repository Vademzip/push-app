package com.pushapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pushapp.model.FriendEntry
import com.pushapp.model.User
import com.pushapp.repository.FriendRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FriendViewModel : ViewModel() {
    private val repo = FriendRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _friends = MutableStateFlow<List<FriendEntry>>(emptyList())
    val friends: StateFlow<List<FriendEntry>> = _friends.asStateFlow()

    private val _incomingRequests = MutableStateFlow<List<FriendEntry>>(emptyList())
    val incomingRequests: StateFlow<List<FriendEntry>> = _incomingRequests.asStateFlow()

    private val _outgoingRequests = MutableStateFlow<List<FriendEntry>>(emptyList())
    val outgoingRequests: StateFlow<List<FriendEntry>> = _outgoingRequests.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _friendsInitialized = MutableStateFlow(false)
    val friendsInitialized: StateFlow<Boolean> = _friendsInitialized.asStateFlow()

    private var entriesJob: Job? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        entriesJob?.cancel()
        if (firebaseAuth.currentUser != null) {
            entriesJob = viewModelScope.launch {
                repo.allFriendEntriesFlow().collect { entries ->
                    _friends.value          = entries.filter { it.status == "accepted" }
                    _outgoingRequests.value = entries.filter { it.status == "pending" && it.sent }
                    _incomingRequests.value = entries.filter { it.status == "pending" && !it.sent }
                    _friendsInitialized.value = true
                }
            }
        } else {
            _friends.value            = emptyList()
            _incomingRequests.value   = emptyList()
            _outgoingRequests.value   = emptyList()
            _searchResults.value      = emptyList()
            _friendsInitialized.value = false
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    fun search(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = repo.searchUsers(query)
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun sendRequest(toUid: String, toUsername: String) {
        viewModelScope.launch { repo.sendRequest(toUid, toUsername) }
    }

    fun acceptRequest(fromUid: String) {
        viewModelScope.launch { repo.acceptRequest(fromUid) }
    }

    fun declineOrRemove(otherUid: String) {
        viewModelScope.launch { repo.declineOrRemove(otherUid) }
    }

    suspend fun getFriendUids(): List<String> = repo.getFriendUids()

    suspend fun getFriendStatus(otherUid: String): FriendEntry? = repo.getFriendStatus(otherUid)
}
