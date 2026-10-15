package com.example.unidash

import android.graphics.Bitmap
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
import com.example.unidash.databinding.FragmentEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var selectedPhotoBytes: ByteArray? = null

    private val photoPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null && _binding != null) {
                loadSelectedPhoto(uri)
            }
        }

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

        loadProfile(userId, currentAuthEmail)

        binding.choosePhotoButton.setOnClickListener {
            photoPicker.launch("image/*")
        }

        binding.saveProfileButton.setOnClickListener {
            saveProfile(userId, currentAuthEmail)
        }

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadProfile(userId: String, currentAuthEmail: String) {
        database.reference
            .child("users")
            .child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener

                binding.nameEditText.setText(
                    snapshot.child("name").getValue(String::class.java) ?: ""
                )

                binding.emailEditText.setText(currentAuthEmail)

                binding.addressEditText.setText(
                    snapshot.child("address").getValue(String::class.java) ?: ""
                )

                binding.phoneEditText.setText(
                    snapshot.child("phone").getValue(String::class.java) ?: ""
                )
            }
            .addOnFailureListener { error ->
                if (_binding == null) return@addOnFailureListener

                showToast("Could not load profile: ${error.message}")
            }

        // Load the existing photo directly from the private S3 bucket.
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val photoBytes =
                    S3ProfilePhotoManager.downloadProfilePhoto(userId)

                if (_binding != null && photoBytes != null) {
                    val bitmap = BitmapFactory.decodeByteArray(
                        photoBytes,
                        0,
                        photoBytes.size
                    )

                    if (bitmap != null && _binding != null) {
                        binding.profileImageView.setImageBitmap(bitmap)
                    }
                }
            } catch (error: Exception) {
                // A user may not have uploaded a photo yet.
                // Keep the default camera image in that case.
                android.util.Log.e(
                    "EditProfileFragment",
                    "Could not load profile photo from S3",
                    error
                )
            }
        }
    }

    private fun loadSelectedPhoto(uri: Uri) {
        try {
            val bitmap = requireContext().contentResolver
                .openInputStream(uri)
                .use { input ->
                    requireNotNull(input) {
                        "Could not open the selected image."
                    }
                    BitmapFactory.decodeStream(input)
                } ?: throw IllegalArgumentException("Invalid image.")

            val compressedBytes = compressPhoto(bitmap)

            if (compressedBytes == null) {
                showToast(
                    "Could not compress the photo below 0.5 MB. Choose another image."
                )
                return
            }

            selectedPhotoBytes = compressedBytes
            binding.profileImageView.setImageBitmap(
                BitmapFactory.decodeByteArray(
                    compressedBytes,
                    0,
                    compressedBytes.size
                )
            )

            showToast("Photo selected. Tap Save Changes to upload it.")
        } catch (error: Exception) {
            showToast("Could not select photo: ${error.message}")
        }
    }

    /**
     * Produces JPEG bytes strictly below 500,000 bytes.
     */
    private fun compressPhoto(original: Bitmap): ByteArray? {
        var bitmap = original
        var attempts = 0

        while (attempts < 12) {
            for (quality in listOf(90, 80, 70, 60, 50, 40, 30)) {
                val output = ByteArrayOutputStream()

                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    quality,
                    output
                )

                val bytes = output.toByteArray()

                if (bytes.isNotEmpty() && bytes.size < 500_000) {
                    return bytes
                }
            }

            if (bitmap.width <= 160 || bitmap.height <= 160) {
                break
            }

            val newWidth = (bitmap.width * 0.75).toInt().coerceAtLeast(1)
            val newHeight = (bitmap.height * 0.75).toInt().coerceAtLeast(1)

            bitmap = Bitmap.createScaledBitmap(
                bitmap,
                newWidth,
                newHeight,
                true
            )

            attempts++
        }

        return null
    }

    private fun saveProfile(userId: String, currentAuthEmail: String) {
        val user = auth.currentUser ?: run {
            showToast("Please log in again.")
            return
        }

        val name = binding.nameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val address = binding.addressEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() ||
            address.isEmpty() || phone.isEmpty()
        ) {
            showToast("Please fill in all fields.")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please enter a valid email address.")
            return
        }

        binding.saveProfileButton.isEnabled = false
        binding.choosePhotoButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val updates = mutableMapOf<String, Any>(
                    "name" to name,
                    "address" to address,
                    "phone" to phone
                )

                // Upload the photo first. Only save its URL if S3 confirms success.
                val photoBytes = selectedPhotoBytes

                if (photoBytes != null) {
                    showToast("Uploading profile photo...")

                    val photoUrl = S3ProfilePhotoManager.uploadProfilePhoto(
                        userId,
                        photoBytes
                    )

                    updates["photoUrl"] = photoUrl
                }

                if (email != currentAuthEmail) {
                    user.verifyBeforeUpdateEmail(email)
                        .addOnSuccessListener {
                            saveUpdatesToFirebase(userId, updates) {
                                if (_binding == null) return@saveUpdatesToFirebase

                                showToast(
                                    "Verification email sent. Verify your new email, then log in again with it."
                                )

                                findNavController().navigateUp()
                            }
                        }
                        .addOnFailureListener { error ->
                            enableButtons()
                            showToast(
                                "Email verification could not be sent: ${error.message}"
                            )
                        }
                } else {
                    saveUpdatesToFirebase(userId, updates) {
                        if (_binding == null) return@saveUpdatesToFirebase

                        showToast("Profile updated successfully!")
                        findNavController().navigateUp()
                    }
                }
            } catch (error: Exception) {
                enableButtons()
                showToast(
                    "Save failed: ${error.message ?: error.javaClass.simpleName}"
                )

                android.util.Log.e(
                    "EditProfileFragment",
                    "Profile photo upload or save failed",
                    error
                )
            }
        }
    }

    private fun saveUpdatesToFirebase(
        userId: String,
        updates: Map<String, Any>,
        onSuccess: () -> Unit
    ) {
        database.reference
            .child("users")
            .child(userId)
            .updateChildren(updates)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                enableButtons()
                showToast("Firebase update failed: ${error.message}")
            }
    }

    private fun enableButtons() {
        if (_binding != null) {
            binding.saveProfileButton.isEnabled = true
            binding.choosePhotoButton.isEnabled = true
        }
    }

    private fun showToast(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        selectedPhotoBytes = null
    }
}
