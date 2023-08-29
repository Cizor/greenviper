package com.wild.smartrack.ui.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.wild.smartrack.data.UiState
import com.wild.smartrack.databinding.FragmentLoginBinding
import com.wild.smartrack.viewmodels.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class Login : Fragment() {
    private val loginViewModel: LoginViewModel by viewModels()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val action = LoginDirections.actionLoginToListOfHubs()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        _binding!!.loginViewModel = loginViewModel

        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe UI state
        viewLifecycleOwner.lifecycleScope.launch {
            loginViewModel.uiState.collect { state ->
                when (state) {
                    UiState.SUCCESS -> {
                        findNavController().navigate(action)
                        loginViewModel._uiState.value = UiState.IDLE // Reset the UI state
                    }
                    UiState.FAILURE -> {
                        Toast.makeText(requireContext(), "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }

        // Observe error message
        viewLifecycleOwner.lifecycleScope.launch {
            loginViewModel.errorMessage.collect { errorMsg ->
                if (errorMsg.isNotEmpty()) {
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}