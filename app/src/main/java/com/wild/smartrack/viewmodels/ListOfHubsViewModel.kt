package com.wild.smartrack.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.wild.smartrack.data.Hub
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListOfHubsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _hubs = MutableStateFlow<List<Hub>>(emptyList())
    val hubs: StateFlow<List<Hub>> = _hubs

    init {
        viewModelScope.launch {
            fetchData()
        }
    }
    private fun fetchData() {
        Log.d("ListOfHubsViewModel", "fetchData A: called")
        val userId = auth.currentUser?.uid ?: return
        Log.d("ListOfHubsViewModel", "fetchData B: $userId")

        // Fetch data from Firestore
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val hubReferences = document.get("hubs") as List<DocumentReference>? ?: return@addOnSuccessListener
                fetchHubs(hubReferences)
            }
    }

    private fun fetchHubs(hubReferences: List<DocumentReference>) {
        val fetchedHubs = mutableListOf<Hub>()
        Log.d("ListOfHubsViewModel", "fetchHubs1: $hubReferences")

        hubReferences.forEach { hubRef ->
            Log.d("ListOfHubsViewModel", "fetchHubs2: $hubRef")
            hubRef.get()
                .addOnSuccessListener { document ->
                    val hubData = document.toObject(Hub::class.java)
                    if (hubData != null) {
                        Log.d("ListOfHubsViewModel", "add fetchHubs4: $hubData")
                        fetchedHubs.add(hubData)
                        _hubs.value = fetchedHubs
                    }
                }
                .addOnFailureListener {
                    // Handle any errors here
                }
        }
    }

}
