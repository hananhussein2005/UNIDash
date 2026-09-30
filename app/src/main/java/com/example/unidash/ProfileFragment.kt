package com.example.unidash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.unidash.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var profileListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        loadProfile()

        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_profileFragment_to_editProfileFragment
            )
        }

        binding.logoutButton.setOnClickListener {

            auth.signOut()

            Toast.makeText(
                requireContext(),
                "Logged out",
                Toast.LENGTH_SHORT
            ).show()

            findNavController().navigate(R.id.loginFragment)
        }
    }

    private fun loadProfile() {

        val user = auth.currentUser

        if (user == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        val userId = user.uid
        val authEmail = user.email ?: ""

        val profileReference = database.reference
            .child("users")
            .child(userId)

        profileListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if (_binding == null) {
                    return
                }

                val name = snapshot.child("name")
                    .getValue(String::class.java)

                val address = snapshot.child("address")
                    .getValue(String::class.java)

                val phone = snapshot.child("phone")
                    .getValue(String::class.java)

                val databaseEmail = snapshot.child("email")
                    .getValue(String::class.java)

                binding.nameTextView.text = name ?: ""
                binding.emailTextView.text = authEmail
                binding.addressTextView.text = address ?: ""
                binding.phoneTextView.text = phone ?: ""

                // Keep the database email synchronized
                // with Firebase Authentication.
                if (authEmail.isNotEmpty() &&
                    authEmail != databaseEmail
                ) {
                    profileReference
                        .child("email")
                        .setValue(authEmail)
                }
            }

            override fun onCancelled(error: DatabaseError) {

                if (_binding == null) {
                    return
                }

                Toast.makeText(
                    requireContext(),
                    "Could not load profile: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        profileReference.addValueEventListener(profileListener!!)
    }

    override fun onResume() {
        super.onResume()

        if (!::auth.isInitialized) {
            return
        }

        val user = auth.currentUser ?: return

        /*
         * When the user verifies a new email, Firebase may invalidate
         * the old authentication session.
         *
         * We check whether the current session is still valid.
         */
        user.getIdToken(true)
            .addOnCompleteListener { task ->

                if (_binding == null) {
                    return@addOnCompleteListener
                }

                if (!task.isSuccessful) {

                    auth.signOut()

                    Toast.makeText(
                        requireContext(),
                        "Your email was changed successfully. Please log in again with your new email.",
                        Toast.LENGTH_LONG
                    ).show()

                    findNavController().navigate(
                        R.id.loginFragment
                    )

                    return@addOnCompleteListener
                }

                // Session is still valid.
                loadProfile()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        val user = auth.currentUser

        if (user != null && profileListener != null) {
            database.reference
                .child("users")
                .child(user.uid)
                .removeEventListener(profileListener!!)
        }

        profileListener = null
        _binding = null
    }
}