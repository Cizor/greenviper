package com.wild.smartrack.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.wild.smartrack.data.UploadStatus
import com.wild.smartrack.databinding.FragmentConfirmImageBinding
import com.wild.smartrack.viewmodels.ListOfHubsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConfirmImage : Fragment() {
    private val listOfHubsViewModel: ListOfHubsViewModel by activityViewModels()
    private var _binding: FragmentConfirmImageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmImageBinding.inflate(inflater, container, false)
        _binding!!.viewModel = listOfHubsViewModel
        listOfHubsViewModel.uploadStatus.value = UploadStatus.NONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            listOfHubsViewModel.selectedImage.collect { bitmap ->
                if (bitmap != null) {
                    // Using Glide to load the bitmap into the ImageView
                    Glide.with(this@ConfirmImage)
                        .load(bitmap)
                        .apply(RequestOptions().override(394, 575))
                        .into(binding.imageView)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            listOfHubsViewModel.uploadStatus.collect { status ->
                when (status) {
                    UploadStatus.SUCCESS -> {
                        Toast.makeText(context, "Upload Successful", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()  // navigate back to tag list
                    }
                    UploadStatus.FAILURE -> {
                        Toast.makeText(context, "Upload Failed", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()  // navigate back to tag list
                    }
                    else -> {
                        // Do nothing
                    }
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
