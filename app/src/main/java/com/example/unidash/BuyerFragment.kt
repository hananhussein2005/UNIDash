package com.example.unidash

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.unidash.databinding.FragmentBuyerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class BuyerFragment : Fragment() {

    private var _binding: FragmentBuyerBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                validateAndPreviewImage(uri)
            }
        }

    companion object {
        private const val MAX_IMAGE_BYTES = 500_000L
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

        // Tap the picture to select a new image.
        binding.profileImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        val user = auth.currentUser

        if (user == null) {
            findNavController().navigate(R.id.roleSelectionFragment)
            return
        }

        val userId = user.uid

        database.reference
            .child("users")
            .child(userId)
            .get()
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
                    snapshot.child("name")
                        .getValue(String::class.java) ?: ""

                binding.profileEmail.text = user.email ?: ""

                binding.profileAddress.text =
                    snapshot.child("address")
                        .getValue(String::class.java) ?: ""

                binding.profilePhone.text =
                    snapshot.child("phone")
                        .getValue(String::class.java) ?: ""
            }
            .addOnFailureListener { error ->

                if (_binding == null) return@addOnFailureListener

                Toast.makeText(
                    requireContext(),
                    "Could not load profile: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
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

    private fun validateAndPreviewImage(uri: Uri) {
        val context = context ?: return

        try {
            val input = context.contentResolver.openInputStream(uri)

            if (input == null) {
                Toast.makeText(
                    context,
                    "Could not read the selected image.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            var totalBytes = 0L

            input.use { stream ->
                val buffer = ByteArray(8192)

                while (true) {
                    val bytesRead = stream.read(buffer)

                    if (bytesRead == -1) break

                    totalBytes += bytesRead

                    if (totalBytes >= MAX_IMAGE_BYTES) {
                        Toast.makeText(
                            context,
                            "Image must be smaller than 0.5 MB.",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }
                }
            }

            if (_binding == null) return

            binding.profileImage.setImageURI(uri)

            Toast.makeText(
                context,
                "Profile picture selected!",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {

            if (_binding == null) return

            Toast.makeText(
                context,
                "Could not load image: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}