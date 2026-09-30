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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Create Account button
        binding.signupButton.setOnClickListener {

            val name = binding.nameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            // Check that all fields are filled
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {

                Toast.makeText(
                    requireContext(),
                    "Please fill in all fields",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Check password length
            if (password.length < 6) {

                Toast.makeText(
                    requireContext(),
                    "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Create Firebase Authentication account
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        // Get the newly created Firebase user
                        val user = auth.currentUser

                        if (user != null) {

                            // Firebase's unique ID for this user
                            val userId = user.uid

                            // Create the user's profile
                            val userProfile = hashMapOf(
                                "name" to name,
                                "email" to email,
                                "address" to "",
                                "phone" to ""
                            )

                            // Save profile to Realtime Database
                            database.reference
                                .child("users")
                                .child(userId)
                                .setValue(userProfile)
                                .addOnCompleteListener { databaseTask ->

                                    if (databaseTask.isSuccessful) {

                                        Toast.makeText(
                                            requireContext(),
                                            "Account created successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        // Go back to Login
                                        findNavController().navigate(
                                            R.id.action_signupFragment_to_loginFragment
                                        )

                                    } else {

                                        Toast.makeText(
                                            requireContext(),
                                            "Account created, but profile could not be saved.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                        }

                    } else {

                        // Firebase Authentication failed
                        Toast.makeText(
                            requireContext(),
                            "Sign up failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // Back to Login button
        binding.backToLoginButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}