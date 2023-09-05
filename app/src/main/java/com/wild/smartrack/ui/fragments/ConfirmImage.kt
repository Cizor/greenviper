package com.wild.smartrack.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.wild.smartrack.R
import com.wild.smartrack.databinding.FragmentConfirmImageBinding
import com.wild.smartrack.viewmodels.ListOfHubsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmImage : Fragment() {
    //Shared one
    private val listOfHubsViewModel: ListOfHubsViewModel by activityViewModels()
    private var _binding: FragmentConfirmImageBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentConfirmImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}