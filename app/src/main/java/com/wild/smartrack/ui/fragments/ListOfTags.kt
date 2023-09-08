package com.wild.smartrack.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.wild.smartrack.data.Tag
import com.wild.smartrack.databinding.FragmentListOfTagsBinding
import com.wild.smartrack.viewmodels.ListOfHubsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
                    if(!listOfHubsViewModel.tagList.contains(newTag)) {
                        listOfHubsViewModel.tagList.add(newTag)
                    }
                    adapter.submitList(listOfHubsViewModel.tagList.toList())
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
        startActivityForResult(intent, REQUEST_CODE, null)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedImageUri = data?.data ?: return
            val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, selectedImageUri)

            listOfHubsViewModel.convertAndScaleImage(bitmap)

            val action = ListOfTagsDirections.actionListOfTagsToConfirmImage()
            findNavController().navigate(action)
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
            "${tag.name} [${tag.count}]".also { nameTextView.text = it }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
