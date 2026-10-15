package com.example.unidash

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.unidash.databinding.FragmentBuyerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

class BuyerFragment : Fragment() {

    private var _binding: FragmentBuyerBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null && _binding != null) {
                binding.profileImage.setImageURI(uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBuyerBinding.inflate(
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

        val user = auth.currentUser

        if (user == null) {
            findNavController().navigate(R.id.roleSelectionFragment)
            return
        }

        val userId = user.uid

        // Load the buyer's details from Firebase.
        database.reference.child("users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener

                val role = snapshot.child("role")
                    .getValue(String::class.java)

                if (role != "buyer") {
                    auth.signOut()

                    Toast.makeText(
                        requireContext(),
                        "This account is not registered as a buyer.",
                        Toast.LENGTH_LONG
                    ).show()

                    findNavController().navigate(
                        R.id.roleSelectionFragment
                    )
                    return@addOnSuccessListener
                }

                binding.buyerTitle.text = "Buyer Profile"

                binding.profileName.text =
                    snapshot.child("name").getValue(String::class.java) ?: ""

                binding.profileEmail.text = user.email ?: ""

                binding.profileAddress.text =
                    snapshot.child("address").getValue(String::class.java) ?: ""

                binding.profilePhone.text =
                    snapshot.child("phone").getValue(String::class.java) ?: ""
            }
            .addOnFailureListener { error ->
                if (_binding == null) return@addOnFailureListener

                Toast.makeText(
                    requireContext(),
                    "Could not load profile: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        // Download the existing profile photo from the private S3 bucket.
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val photoBytes =
                    S3ProfilePhotoManager.downloadProfilePhoto(userId)

                if (_binding == null) return@launch

                if (photoBytes != null && photoBytes.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(
                        photoBytes,
                        0,
                        photoBytes.size
                    )

                    if (bitmap != null && _binding != null) {
                        binding.profileImage.setImageBitmap(bitmap)
                    } else {
                        showMessage("The profile photo could not be decoded.")
                    }
                } else {
                    android.util.Log.d(
                        "BuyerFragment",
                        "No profile photo was returned from S3."
                    )
                }
            } catch (error: Exception) {
                android.util.Log.e(
                    "BuyerFragment",
                    "Could not download buyer profile photo from S3",
                    error
                )

                showMessage(
                    "Could not load profile photo: " +
                            (error.message ?: "Check S3 permissions.")
                )
            }
        }

        // Keep the existing image picker behavior.
        binding.profileImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_buyerFragment_to_editProfileFragment
            )
        }

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            findNavController().navigate(
                R.id.roleSelectionFragment
            )
        }
    }

    private fun showMessage(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
