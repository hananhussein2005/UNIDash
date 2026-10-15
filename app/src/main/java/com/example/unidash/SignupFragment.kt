package com.example.unidash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.unidash.databinding.FragmentSignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var selectedRole: String = "buyer"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Get the role passed from the previous screen.
        selectedRole = arguments?.getString("role") ?: "buyer"

        binding.signupButton.setOnClickListener {

            val name = binding.nameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please fill in all fields",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(
                    requireContext(),
                    "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (_binding == null) return@addOnCompleteListener

                    if (task.isSuccessful) {

                        val user = auth.currentUser

                        if (user == null) {
                            Toast.makeText(
                                requireContext(),
                                "Account created, but user information could not be loaded.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@addOnCompleteListener
                        }

                        val userId = user.uid

                        val userProfile = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "address" to "",
                            "phone" to "",
                            "role" to selectedRole
                        )

                        database.reference
                            .child("users")
                            .child(userId)
                            .setValue(userProfile)
                            .addOnCompleteListener { databaseTask ->

                                if (_binding == null) {
                                    return@addOnCompleteListener
                                }

                                if (databaseTask.isSuccessful) {

                                    Toast.makeText(
                                        requireContext(),
                                        "Account created as ${selectedRole}!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Return to Login while preserving the role.
                                    val bundle = Bundle().apply {
                                        putString("role", selectedRole)
                                    }

                                    findNavController().navigate(
                                        R.id.action_signupFragment_to_loginFragment,
                                        bundle
                                    )

                                } else {

                                    Toast.makeText(
                                        requireContext(),
                                        "Account created, but profile could not be saved: " +
                                                "${databaseTask.exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                    } else {

                        Toast.makeText(
                            requireContext(),
                            "Sign up failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        binding.backToLoginButton.setOnClickListener {
            val bundle = Bundle().apply {
                putString("role", selectedRole)
            }

            findNavController().navigate(
                R.id.action_signupFragment_to_loginFragment,
                bundle
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
