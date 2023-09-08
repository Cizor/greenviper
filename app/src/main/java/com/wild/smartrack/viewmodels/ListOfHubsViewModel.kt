package com.wild.smartrack.viewmodels

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.wild.smartrack.data.Controller
import com.wild.smartrack.data.Hub
import com.wild.smartrack.data.Tag
import com.wild.smartrack.data.UploadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class ListOfHubsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _hub = MutableStateFlow<Hub?>(null)
    val hub: StateFlow<Hub?> = _hub

    private val _selectedHub = MutableStateFlow<Hub?>(null)
    val selectedHub: StateFlow<Hub?> = _selectedHub

    var hubList = mutableListOf<Hub>()
    var controllerList = mutableListOf<Controller>()
    var tagList = mutableListOf<Tag>()

    private val _controller = MutableStateFlow<Controller?>(null)
    val controller: StateFlow<Controller?> = _controller

    private val _selectedController = MutableStateFlow<Controller?>(null)
    val selectedController: StateFlow<Controller?> = _selectedController

    private val _tag = MutableStateFlow<Tag?>(null)  // Assuming Tag is a data class you've defined
    val tag: StateFlow<Tag?> = _tag

    private val fetchedControllers = mutableListOf<Controller>()
    private val fetchedHubs = mutableListOf<Hub>()

    // MutableStateFlow
    private val _selectedImage = MutableStateFlow<Bitmap?>(null)

    // StateFlow
    val selectedImage: StateFlow<Bitmap?> = _selectedImage

    val uploadStatus = MutableStateFlow<UploadStatus>(UploadStatus.NONE)

    private val storageReference = storage.reference


    init {
        viewModelScope.launch {
            fetchData()
            selectedHub.collect { hub ->
                if (hub != null) {
                    Log.d("ListOfHubsViewModel", "selectedHub: $hub")
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
                Log.d("ListOfHubsViewModel", "fetchData: $hubReferences")
                fetchHubs(hubReferences)
            }
    }
    private fun fetchHubs(hubReferences: List<DocumentReference>) {

        fetchedHubs.clear()
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

        fetchedControllers.clear()
        val controllerRefs = hub.controllers ?: emptyList()


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

    fun convertAndScaleImage(bitmap: Bitmap) {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 296, false)
        val bwBitmap = convertToBlackAndWhite(scaledBitmap)
        _selectedImage.value = bwBitmap
    }
    private fun convertToBlackAndWhite(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height

        val bwBitmap = Bitmap.createBitmap(width, height, src.config)
        val canvas = Canvas(bwBitmap)

        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val filter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = filter

        canvas.drawBitmap(src, 0f, 0f, paint)

        return bwBitmap
    }

    fun uploadImageToFirebaseStorageWrapper() {
        viewModelScope.launch {
            uploadImageToFirebaseStorage()
        }
    }

    suspend fun uploadImageToFirebaseStorage() {
        val bitmap = selectedImage.value
        val hubName = selectedHub.value?.name ?: "unknown"
        val controllerName = selectedController.value?.name ?: "unknown"
        val tagName = tag.value?.name ?: "unknown"

        // Construct the filename
        val fileName = "${hubName}_${controllerName}_${tagName}.jpg"

        if (bitmap != null) {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            // Use the filename in your storage reference
            val imageRef = storageReference.child("images/$fileName")

            withContext(Dispatchers.IO) {
                try {
                    val uploadTask = imageRef.putBytes(data).await()
                    if (uploadTask.metadata != null) {
                        uploadStatus.emit(UploadStatus.SUCCESS)
                    } else {
                        uploadStatus.emit(UploadStatus.FAILURE)
                    }
                } catch (e: Exception) {
                    uploadStatus.emit(UploadStatus.FAILURE)
                }
            }
        }
    }



}
