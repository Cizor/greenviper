package com.wild.smartrack.ui.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.wild.smartrack.data.Tag
import com.wild.smartrack.databinding.FragmentListOfTagsBinding
import com.wild.smartrack.viewmodels.ListOfHubsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@AndroidEntryPoint
class ListOfTags : Fragment() {
    companion object {
        private const val REQUEST_CODE = 1234
    }

    private var _binding: FragmentListOfTagsBinding? = null
    private val binding get() = _binding!!

    //Shared ViewModel
    private val listOfHubsViewModel: ListOfHubsViewModel by activityViewModels()
    private lateinit var adapter: MyTagAdapter

    // Define a list to store the tags
    private val tags = mutableListOf<Tag>()

    private var fileName: String = "_"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListOfTagsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MyTagAdapter { tag ->
            onTagClicked(tag)
        }
        binding.tagsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.tagsRecyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            listOfHubsViewModel.tag.collect { newTag ->
                if (newTag != null) {
                    tags.add(newTag)
                    adapter.submitList(tags.toList())
                }
            }
        }
    }
    private fun onTagClicked(tag: Tag) {
        val currentHub = listOfHubsViewModel.selectedHub.value?.name ?: ""
        val currentController = listOfHubsViewModel.selectedController.value?.name ?: ""

        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        fileName = "${currentHub}_${currentController}_${tag.name}"
        Log.d("ListOfTags", "onTagClicked: $fileName")
        startActivityForResult(intent, REQUEST_CODE, null)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            val selectedImageUri = data?.data ?: return
            val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, selectedImageUri)
            val byteArray: ByteArray = bitmapToByteArray(bitmap)
            val hexArray: ByteArray = bytesToHex(byteArray)

            Log.d("ListOfTags", "onActivityResult: $fileName")
            uploadHexArrayToFirebase(hexArray, fileName)
        }
    }
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
    fun bytesToHex(bytes: ByteArray): ByteArray {
        val hexArray = "0123456789ABCDEF".toCharArray()
        val hexChars = ByteArray(bytes.size * 2)
        var v: Int
        for (j in bytes.indices) {
            v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4].toByte()
            hexChars[j * 2 + 1] = hexArray[v and 0x0F].toByte()
        }
        return hexChars
    }
    private fun uploadHexArrayToFirebase(hexArray: ByteArray, fileName: String) {
        Log.d("ListOfTags", "uploadHexArrayToFirebase: $fileName")

        // Generate the .h file content
        val headerStringBuilder = StringBuilder()
        headerStringBuilder.append("#ifndef _GxBitmaps3c128x296_H_\n")
        headerStringBuilder.append("#define _GxBitmaps3c128x296_H_\n")
        headerStringBuilder.append("\n")
        headerStringBuilder.append("#if defined(ESP8266) || defined(ESP32)\n")
        headerStringBuilder.append("#include <pgmspace.h>\n")
        headerStringBuilder.append("#else\n")
        headerStringBuilder.append("#include <avr/pgmspace.h>\n")
        headerStringBuilder.append("#endif\n")
        headerStringBuilder.append("\n")
        headerStringBuilder.append("#include \"WS_Bitmaps3c128x296.h\"\n")
        headerStringBuilder.append("\n")
        headerStringBuilder.append("const unsigned char Bitmap3c128x296_1_black[] PROGMEM =\n")
        headerStringBuilder.append("{\n")

        hexArray.forEachIndexed { index, byte ->
            headerStringBuilder.append("0X${"%02X".format(byte)},")
            if (index % 16 == 15) {
                headerStringBuilder.append("\n")
            }
        }

        headerStringBuilder.append("};\n")
        headerStringBuilder.append("#endif\n")

        // Convert the string to a byte array
        val headerFileContent = headerStringBuilder.toString().toByteArray()

        // Upload the byte array to Firebase
        val storageRef = Firebase.storage.reference.child("test/$fileName.h")

        storageRef.putBytes(headerFileContent)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Upload successful", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Upload failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }


    class MyTagAdapter(private val onTagClick: (Tag) -> Unit) : androidx.recyclerview.widget.RecyclerView.Adapter<MyTagViewHolder>() {
        private var tags: List<Tag> = emptyList() // Replace Tag with your actual Tag data class

        fun submitList(tags: List<Tag>) {
            this.tags = tags
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyTagViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(
                parent.context.resources.getIdentifier(
                    "tag_item",
                    "layout",
                    parent.context.packageName
                ), parent, false
            )
            return MyTagViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: MyTagViewHolder, position: Int) {
            val tag = tags[position]
            holder.bind(tag)
            holder.itemView.setOnClickListener {
                onTagClick(tag)
            }
        }

        override fun getItemCount() = tags.size
    }

    class MyTagViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        fun bind(tag: Tag) { // Replace Tag with your actual Tag data class
            val nameTextView: android.widget.TextView = itemView.findViewById(
                itemView.resources.getIdentifier(
                    "tagName",
                    "id",
                    itemView.context.packageName
                )
            )
            nameTextView.text = tag.name // Assuming Tag data class has a "name" property
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
