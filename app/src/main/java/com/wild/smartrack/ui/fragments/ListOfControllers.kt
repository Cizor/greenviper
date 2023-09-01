package com.wild.smartrack.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.wild.smartrack.data.Controller
import com.wild.smartrack.databinding.FragmentListOfControllersBinding
import com.wild.smartrack.viewmodels.ListOfHubsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ListOfControllers : Fragment() {

    private var _binding: FragmentListOfControllersBinding? = null
    private val binding get() = _binding!!

    //Shared one
    private val listOfHubsViewModel: ListOfHubsViewModel by activityViewModels()
    private lateinit var adapter: MyControllerAdapter

    private val controllers = mutableListOf<Controller>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListOfControllersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView
        adapter = MyControllerAdapter { selectedController ->
            listOfHubsViewModel.selectController(selectedController)
            val action = ListOfControllersDirections.actionListOfControllersToListOfTags()
            findNavController().navigate(action)
        }
        binding.controllersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.controllersRecyclerView.adapter = adapter

        // Observe the controllers StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            listOfHubsViewModel.controller.collect { controller ->
                Log.d("ListOfControllers", "Controllers: $controllers")
                if (controller != null) {
                    controllers.add(controller)
                    adapter.submitList(controllers.toList())  // or however you update your list
                }
            }
        }
    }

    class MyControllerAdapter(private val onClick: (Controller) -> Unit) : androidx.recyclerview.widget.RecyclerView.Adapter<MyControllerViewHolder>() {
        private var controllers: List<Controller> = emptyList()

        fun submitList(controllers: List<Controller>) {
            this.controllers = controllers
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyControllerViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(
                parent.context.resources.getIdentifier(
                    "controller_item",
                    "layout",
                    parent.context.packageName
                ), parent, false
            )
            return MyControllerViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: MyControllerViewHolder, position: Int) {
            val controller = controllers[position]
            holder.bind(controller)
            holder.itemView.setOnClickListener {
                onClick(controller)
            }
        }

        override fun getItemCount() = controllers.size
    }

    class MyControllerViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        fun bind(controller: Controller) {
            val nameTextView: android.widget.TextView = itemView.findViewById(
                itemView.resources.getIdentifier(
                    "controllerName",
                    "id",
                    itemView.context.packageName
                )
            )
            nameTextView.text = controller.name
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
