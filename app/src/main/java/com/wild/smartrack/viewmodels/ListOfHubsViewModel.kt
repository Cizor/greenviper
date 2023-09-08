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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

    var hubList = mutableListOf<Hub>()
    var controllerList = mutableListOf<Controller>()
    var tagList = mutableListOf<Tag>()


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
        setupRealtimeUpdates()
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

        //fetchedHubs.clear()
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
        val fileName = "${hubName}_${controllerName}_${tagName}.bmp"

        if (bitmap != null) {
            val bmpData = bitmapToBMPByteArray(bitmap)  // Use the new utility function here

            // Use the filename in your storage reference
            val imageRef = storageReference.child("images/$fileName")

            withContext(Dispatchers.IO) {
                try {
                    val uploadTask = imageRef.putBytes(bmpData).await()
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


    fun bitmapToBMPByteArray(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val size = width * height * 3 + 54 // 54 is the size of the BMP header
        val bytes = ByteArray(size)
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        // BMP Header
        buffer.put(0x42.toByte()) // B
        buffer.put(0x4D.toByte()) // M
        buffer.putInt(size) // File size
        buffer.putInt(0) // Reserved
        buffer.putInt(54) // Offset to pixel data

        // DIB Header
        buffer.putInt(40) // Header size
        buffer.putInt(width)
        buffer.putInt(height)
        buffer.putShort(1) // Color planes
        buffer.putShort(24) // Bits per pixel
        buffer.putInt(0) // Compression method
        buffer.putInt(width * height * 3) // Raw bitmap data size; no compression
        buffer.putInt(2835) // Horizontal resolution (dpi)
        buffer.putInt(2835) // Vertical resolution (dpi)
        buffer.putInt(0) // Number of colors in the palette
        buffer.putInt(0) // Number of important colors

        // Bitmap data
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (y in height - 1 downTo 0) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                buffer.put((pixel and 0xFF).toByte()) // Blue
                buffer.put((pixel shr 8 and 0xFF).toByte()) // Green
                buffer.put((pixel shr 16 and 0xFF).toByte()) // Red
            }
        }

        return bytes
    }

    private fun setupRealtimeUpdates() {
        viewModelScope.launch {
            hubUpdatesFlow().collect { updatedHub ->
                // Update the hub list and notify observers
                updateHubList(updatedHub)
            }
            controllerUpdatesFlow().collect { updatedController ->
                // Update the controller list and notify observers
                updateControllerList(updatedController)
            }
            tagUpdatesFlow().collect { updatedTag ->
                // Update the tag list and notify observers
                updateTagList(updatedTag)
            }
        }
    }

    private fun hubUpdatesFlow() = callbackFlow<Hub> {
        val userId = auth.currentUser?.uid ?: return@callbackFlow
        val subscription = db.collection("users")
            .document(userId)
            .collection("hubs")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges?.forEach { documentChange ->
                    val hub = documentChange.document.toObject(Hub::class.java)
                    trySend(hub)
                }
            }
        awaitClose { subscription.remove() }
    }
    private fun controllerUpdatesFlow() = callbackFlow<Controller> {
        val userId = auth.currentUser?.uid ?: return@callbackFlow
        val subscription = db.collection("users")
            .document(userId)
            .collection("controllers")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges?.forEach { documentChange ->
                    val controller = documentChange.document.toObject(Controller::class.java)
                    trySend(controller)
                }
            }
        awaitClose { subscription.remove() }
    }
    private fun tagUpdatesFlow() = callbackFlow<Tag> {
        val userId = auth.currentUser?.uid ?: return@callbackFlow
        val subscription = db.collection("users")
            .document(userId)
            .collection("tags")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges?.forEach { documentChange ->
                    val tag = documentChange.document.toObject(Tag::class.java)
                    trySend(tag)
                }
            }
        awaitClose { subscription.remove() }
    }

    private fun updateHubList(updatedHub: Hub) {
        if (updatedHub !in hubList) {
            hubList.add(updatedHub)
        } else {
            val index = hubList.indexOf(updatedHub)
            hubList[index] = updatedHub
        }
        // Notify observers with updated hub data
        _hub.value = updatedHub
    }

    private fun updateControllerList(updatedController: Controller) {
        if (updatedController !in controllerList) {
            controllerList.add(updatedController)
        } else {
            val index = controllerList.indexOf(updatedController)
            controllerList[index] = updatedController
        }
        // Notify observers with updated controller data
        _controller.value = updatedController
    }

    private fun updateTagList(updatedTag: Tag) {
        if (updatedTag !in tagList) {
            tagList.add(updatedTag)
        } else {
            val index = tagList.indexOf(updatedTag)
            tagList[index] = updatedTag
        }
        // Notify observers with updated tag data
        _tag.value = updatedTag
    }




}
