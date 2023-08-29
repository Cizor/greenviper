package com.wild.smartrack.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wild.smartrack.data.Hub
import com.wild.smartrack.databinding.FragmentListOfHubsBinding
import com.wild.smartrack.viewmodels.ListOfHubsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ListOfHubs : Fragment() {
    private val listOfHubsViewModel: ListOfHubsViewModel by viewModels()
    private var _binding: FragmentListOfHubsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MyHubAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListOfHubsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView
        adapter = MyHubAdapter()
        binding.hubsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.hubsRecyclerView.adapter = adapter

        // Observe the hubs StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            listOfHubsViewModel.hubs.collect { hubs ->
                adapter.submitList(hubs)
            }
        }
    }

    class MyHubAdapter : RecyclerView.Adapter<MyHubViewHolder>() {
        private var hubs: List<Hub> = emptyList()

        fun submitList(hubs: List<Hub>) {
            this.hubs = hubs
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHubViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(
                parent.context.resources.getIdentifier(
                    "hub_item",
                    "layout",
                    parent.context.packageName
                ), parent, false
            )
            return MyHubViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: MyHubViewHolder, position: Int) {
            holder.bind(hubs[position])
        }

        override fun getItemCount() = hubs.size
    }

    class MyHubViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(hub: Hub) {
            val nameTextView: TextView = itemView.findViewById(
                itemView.resources.getIdentifier(
                    "hubName",
                    "id",
                    itemView.context.packageName
                )
            )
            nameTextView.text = hub.name
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
