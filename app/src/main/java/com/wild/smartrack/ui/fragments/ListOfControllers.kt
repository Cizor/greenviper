package com.wild.smartrack.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.wild.smartrack.R
import com.wild.smartrack.databinding.FragmentListOfControllersBinding
import com.wild.smartrack.viewmodels.ListOfControllersViewModel
import dagger.hilt.android.AndroidEntryPoint

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class ListOfControllers : Fragment() {

    private var _binding: FragmentListOfControllersBinding? = null
    private val binding get() = _binding!!
    private val listOfControllersViewModel: ListOfControllersViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListOfControllersBinding.inflate(inflater, container, false)
        return binding.root
    }

}