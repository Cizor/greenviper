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

    private val _hubs = MutableStateFlow<List<Hub>>(emptyList())
    val hubs: StateFlow<List<Hub>> = _hubs

    private val _controllers = MutableStateFlow<List<Controller>>(emptyList())
    val controllers: StateFlow<List<Controller>> = _controllers

    private val _selectedHub = MutableStateFlow<Hub?>(null)
    val selectedHub: StateFlow<Hub?> = _selectedHub

    private val _areControllersFetched = MutableStateFlow(false)
    val areControllersFetched: StateFlow<Boolean> = _areControllersFetched

    private val _selectedController = MutableStateFlow<Controller?>(null)
    val selectedController: StateFlow<Controller?> = _selectedController

    private val _areTagsFetched = MutableStateFlow(false)
    val areTagsFetched: StateFlow<Boolean> = _areTagsFetched

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())  // Assuming Tag is a data class you've defined
    val tags: StateFlow<List<Tag>> = _tags



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
        val fetchedHubs = mutableListOf<Hub>()

        hubReferences.forEach { hubRef ->
            hubRef.get()
                .addOnSuccessListener { document ->
                    val hubData = document.toObject(Hub::class.java)
                    if (hubData != null) {
                        Log.d("ListOfHubsViewModel", "fetchHubs: $hubData")
                        fetchedHubs.add(hubData)
                        _hubs.value = fetchedHubs
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
        val fetchedControllers = mutableListOf<Controller>()
        Log.d("ListOfHubsViewModel", "fetchControllers 0: $controllerRefs")

        controllerRefs.forEach { controllerRef ->
            controllerRef.get()
                .addOnSuccessListener { document ->
                    val controllerData = document.toObject(Controller::class.java)
                    if (controllerData != null) {
                        fetchedControllers.add(controllerData)
                        Log.d("ListOfHubsViewModel", "fetchControllers 1: $fetchedControllers")
                        _controllers.value = fetchedControllers
                        _areControllersFetched.value = true
                    }
                }
                .addOnFailureListener {
                    // Handle any errors here
                }
        }
    }

    private fun fetchTags(controller: Controller) {
        Log.d("ListOfHubsViewModel", "fetchTags: $controller")
        val tagRefs = controller.tags ?: emptyList()  // Assuming Controller has a field "tags"
        val fetchedTags = mutableListOf<Tag>()

        tagRefs.forEach { tagRef ->
            tagRef.get()
                .addOnSuccessListener { document ->
                    val tagData = document.toObject(Tag::class.java)
                    if (tagData != null) {
                        fetchedTags.add(tagData)
                        Log.d("ListOfHubsViewModel", "fetchTags: $fetchedTags")
                        _tags.value = fetchedTags
                        _areTagsFetched.value = true
                    }
                }
                .addOnFailureListener {
                    // Handle any errors here
                }
        }
    }
}
