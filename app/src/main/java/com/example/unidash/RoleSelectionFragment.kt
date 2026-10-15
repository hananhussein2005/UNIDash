package com.example.unidash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.unidash.databinding.FragmentRoleSelectionBinding

class RoleSelectionFragment : Fragment() {

    private var _binding: FragmentRoleSelectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoleSelectionBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navigate to Login and pass the Buyer role
        binding.buyerButton.setOnClickListener {
            val bundle = Bundle().apply {
                putString("role", "buyer")
            }

            findNavController().navigate(
                R.id.action_roleSelectionFragment_to_loginFragment,
                bundle
            )
        }

        // Navigate to Login and pass the Seller role
        binding.sellerButton.setOnClickListener {
            val bundle = Bundle().apply {
                putString("role", "seller")
            }

            findNavController().navigate(
                R.id.action_roleSelectionFragment_to_loginFragment,
                bundle
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
