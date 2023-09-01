package com.wild.smartrack.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.wild.smartrack.data.Controller
import com.wild.smartrack.data.Hub
import com.wild.smartrack.data.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListOfHubsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _hub = MutableStateFlow<Hub?>(null)
    val hub: StateFlow<Hub?> = _hub

    private val _selectedHub = MutableStateFlow<Hub?>(null)
    val selectedHub: StateFlow<Hub?> = _selectedHub

    private val _controller = MutableStateFlow<Controller?>(null)
    val controller: StateFlow<Controller?> = _controller

    private val _selectedController = MutableStateFlow<Controller?>(null)
    val selectedController: StateFlow<Controller?> = _selectedController

    private val _tag = MutableStateFlow<Tag?>(null)  // Assuming Tag is a data class you've defined
    val tag: StateFlow<Tag?> = _tag

    private val fetchedControllers = mutableListOf<Controller>()
    private val fetchedHubs = mutableListOf<Hub>()

    init {
        viewModelScope.launch {
            fetchData()
            selectedHub.collect { hub ->
                if (hub != null) {
                    fetchControllers(hub)
                }
            }
        }
        viewModelScope.launch {
            selectedController.collect {controller ->
                if (controller != null) {
                    fetchTags(controller)
                }

            }
        }
    }

    fun selectHub(hub: Hub) {
        Log.d("ListOfHubsViewModel", "selectHub: $hub")
        _selectedHub.value = hub
    }
    fun selectController(controller: Controller) {
        Log.d("ListOfHubsViewModel", "selectController: $controller")
        _selectedController.value = controller
    }

    private fun fetchData() {
        val userId = auth.currentUser?.uid ?: return

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

        hubReferences.forEach { hubRef ->
            hubRef.get()
                .addOnSuccessListener { document ->
                    val hubData = document.toObject(Hub::class.java)
                    if (hubData != null && !fetchedHubs.contains(hubData)) {
                        fetchedHubs.add(hubData)  // Add to fetchedHubs
                        Log.d("ListOfHubsViewModel", "fetchHubs: $hubData")
                        _hub.value = hubData  // Emit only the new Hub
                    }
                }
                .addOnFailureListener {
                    // Handle any errors here
                }
        }
    }

    private fun fetchControllers(hub: Hub) {
        Log.d("ListOfHubsViewModel", "fetchControllers: $hub")
        val controllerRefs = hub.controllers ?: emptyList()
        Log.d("ListOfHubsViewModel", "fetchControllers 0: $controllerRefs")

        controllerRefs.forEach { controllerRef ->
            controllerRef.get()
                .addOnSuccessListener { document ->
                    val controllerData = document.toObject(Controller::class.java)
                    if (controllerData != null && !fetchedControllers.contains(controllerData)) {
                        fetchedControllers.add(controllerData)
                        Log.d("ListOfHubsViewModel", "fetchControllers 1: $fetchedControllers")
                        _controller.value = controllerData // Emit the new controller
                        // _areControllersFetched.value = true  // No longer needed
                    }
                }
                .addOnFailureListener {
                    // Handle any errors here
                }
        }
    }

    private fun fetchTags(controller: Controller) {
        val tagRefs = controller.tags ?: emptyList()  // Assuming Controller has a field "tags"

        tagRefs.forEach { tagRef ->
            tagRef.get()
                .addOnSuccessListener { document ->
                    val tagData = document.toObject(Tag::class.java)
                    if (tagData != null) {
                        Log.d("ListOfHubsViewModel", "fetchTags: $tagData")
                        _tag.value = tagData  // emit only the new Tag
                    }
                }
                .addOnFailureListener {
                    // Handle any errors here
                }
        }
    }
}
