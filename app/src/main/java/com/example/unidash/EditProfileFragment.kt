package com.example.unidash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.unidash.databinding.FragmentEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                requireContext(),
                "Please log in first",
                Toast.LENGTH_SHORT
            ).show()

            findNavController().navigate(R.id.loginFragment)
            return
        }

        val userId = user.uid
        val currentAuthEmail = user.email ?: ""

        // Load existing profile data
        database.reference
            .child("users")
            .child(userId)
            .get()
            .addOnSuccessListener { snapshot ->

                if (_binding == null) {
                    return@addOnSuccessListener
                }

                binding.nameEditText.setText(
                    snapshot.child("name")
                        .getValue(String::class.java) ?: ""
                )

                // Email comes from Firebase Authentication
                binding.emailEditText.setText(
                    currentAuthEmail
                )

                binding.addressEditText.setText(
                    snapshot.child("address")
                        .getValue(String::class.java) ?: ""
                )

                binding.phoneEditText.setText(
                    snapshot.child("phone")
                        .getValue(String::class.java) ?: ""
                )
            }
            .addOnFailureListener { error ->

                if (_binding == null) {
                    return@addOnFailureListener
                }

                Toast.makeText(
                    requireContext(),
                    "Could not load profile: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        // Save profile
        binding.saveProfileButton.setOnClickListener {

            val name = binding.nameEditText.text
                .toString()
                .trim()

            val email = binding.emailEditText.text
                .toString()
                .trim()

            val address = binding.addressEditText.text
                .toString()
                .trim()

            val phone = binding.phoneEditText.text
                .toString()
                .trim()

            // Check that all fields are filled
            if (name.isEmpty() ||
                email.isEmpty() ||
                address.isEmpty() ||
                phone.isEmpty()
            ) {

                Toast.makeText(
                    requireContext(),
                    "Please fill in all fields",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // These fields can be updated immediately.
            // Email is intentionally NOT included here.
            val profileUpdates = mapOf(
                "name" to name,
                "address" to address,
                "phone" to phone
            )

            // ------------------------------------------------
            // EMAIL WAS CHANGED
            // ------------------------------------------------
            if (email != currentAuthEmail) {

                user.verifyBeforeUpdateEmail(email)
                    .addOnSuccessListener {

                        // Save the other profile information.
                        // DO NOT save the new email yet.
                        database.reference
                            .child("users")
                            .child(userId)
                            .updateChildren(profileUpdates)
                            .addOnSuccessListener {

                                if (_binding == null) {
                                    return@addOnSuccessListener
                                }

                                Toast.makeText(
                                    requireContext(),
                                    "Verification email sent. Verify your new email, then log in again with your new email.",
                                    Toast.LENGTH_LONG
                                ).show()

                                findNavController().navigateUp()
                            }
                            .addOnFailureListener { error ->

                                if (_binding == null) {
                                    return@addOnFailureListener
                                }

                                Toast.makeText(
                                    requireContext(),
                                    "Profile update failed: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .addOnFailureListener { error ->

                        if (_binding == null) {
                            return@addOnFailureListener
                        }

                        Toast.makeText(
                            requireContext(),
                            "Email verification could not be sent: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

            } else {

                // ------------------------------------------------
                // EMAIL WAS NOT CHANGED
                // ------------------------------------------------

                database.reference
                    .child("users")
                    .child(userId)
                    .updateChildren(profileUpdates)
                    .addOnSuccessListener {

                        if (_binding == null) {
                            return@addOnSuccessListener
                        }

                        Toast.makeText(
                            requireContext(),
                            "Profile updated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        findNavController().navigateUp()
                    }
                    .addOnFailureListener { error ->

                        if (_binding == null) {
                            return@addOnFailureListener
                        }

                        Toast.makeText(
                            requireContext(),
                            "Update failed: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }

        // Cancel
        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}