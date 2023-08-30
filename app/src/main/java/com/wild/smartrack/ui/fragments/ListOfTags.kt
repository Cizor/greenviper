package com.wild.smartrack.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.wild.smartrack.data.Tag  // Replace with your actual Tag data class import
import com.wild.smartrack.databinding.FragmentListOfTagsBinding
import com.wild.smartrack.viewmodels.ListOfHubsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ListOfTags : Fragment() {

    private var _binding: FragmentListOfTagsBinding? = null
    private val binding get() = _binding!!

    //Shared ViewModel
    private val listOfHubsViewModel: ListOfHubsViewModel by activityViewModels()
    private lateinit var adapter: MyTagAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListOfTagsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView
        adapter = MyTagAdapter()
        binding.tagsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.tagsRecyclerView.adapter = adapter

        // Observe the tags StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            listOfHubsViewModel.tags.collect { tags ->
                Log.d("ListOfTags", "Tags: $tags")
                adapter.submitList(tags)
            }
        }
    }

    class MyTagAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<MyTagViewHolder>() {
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
