package com.example.unidash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.unidash.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var selectedRole: String = "buyer"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(
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

        selectedRole = arguments?.getString("role") ?: "buyer"

        // If a user is already signed in, check their saved role.
        if (auth.currentUser != null) {
            checkUserRole()
            return
        }

        binding.loginButton.setOnClickListener {

            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please enter your email and password",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (_binding == null) return@addOnCompleteListener

                    if (task.isSuccessful) {
                        checkUserRole()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Login failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        binding.signupButton.setOnClickListener {

            val bundle = Bundle().apply {
                putString("role", selectedRole)
            }

            findNavController().navigate(
                R.id.action_loginFragment_to_signupFragment,
                bundle
            )
        }
    }

    private fun checkUserRole() {

        val user = auth.currentUser

        if (user == null) {
            return
        }

        database.reference
            .child("users")
            .child(user.uid)
            .child("role")
            .get()
            .addOnSuccessListener { snapshot ->

                if (_binding == null) return@addOnSuccessListener

                val savedRole = snapshot.getValue(String::class.java)

                if (savedRole == null ||
                    (savedRole != "buyer" && savedRole != "seller")
                ) {
                    auth.signOut()

                    Toast.makeText(
                        requireContext(),
                        "No valid role is saved for this account.",
                        Toast.LENGTH_LONG
                    ).show()

                    findNavController().navigate(
                        R.id.roleSelectionFragment
                    )
                    return@addOnSuccessListener
                }

                if (savedRole != selectedRole) {

                    auth.signOut()

                    Toast.makeText(
                        requireContext(),
                        "This account is registered as $savedRole. Please choose the correct role.",
                        Toast.LENGTH_LONG
                    ).show()

                    findNavController().navigate(
                        R.id.roleSelectionFragment
                    )
                    return@addOnSuccessListener
                }

                Toast.makeText(
                    requireContext(),
                    "Login successful!",
                    Toast.LENGTH_SHORT
                ).show()

                if (savedRole == "buyer") {
                    findNavController().navigate(
                        R.id.action_loginFragment_to_buyerFragment
                    )
                } else {
                    findNavController().navigate(
                        R.id.action_loginFragment_to_sellerFragment
                    )
                }
            }
            .addOnFailureListener { error ->

                if (_binding == null) return@addOnFailureListener

                auth.signOut()

                Toast.makeText(
                    requireContext(),
                    "Could not verify account role: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()

                findNavController().navigate(
                    R.id.roleSelectionFragment
                )
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
