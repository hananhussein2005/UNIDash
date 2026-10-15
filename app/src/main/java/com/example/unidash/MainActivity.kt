package com.example.unidash

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.unidash.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        setSupportActionBar(binding.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(
                R.id.nav_host_fragment_content_main
            ) as NavHostFragment

        // Use the NavController from the NavHostFragment.
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(
            navController,
            appBarConfiguration
        )

        binding.fab.setOnClickListener { view ->
            com.google.android.material.snackbar.Snackbar.make(
                view,
                "Replace with your own action",
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
            )
                .setAnchorView(R.id.fab)
                .show()
        }

        if (savedInstanceState == null) {
            restoreUserSession()
        }
    }

    private fun restoreUserSession() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return

        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(user.uid)
            .child("role")
            .get()
            .addOnSuccessListener { snapshot ->

                if (isFinishing || isDestroyed) {
                    return@addOnSuccessListener
                }

                // Only redirect if the app is still on role selection.
                if (navController.currentDestination?.id !=
                    R.id.roleSelectionFragment
                ) {
                    return@addOnSuccessListener
                }

                when (snapshot.getValue(String::class.java)) {
                    "buyer" -> {
                        navController.navigate(
                            R.id.action_roleSelectionFragment_to_buyerFragment
                        )
                    }

                    "seller" -> {
                        navController.navigate(
                            R.id.action_roleSelectionFragment_to_sellerFragment
                        )
                    }

                    else -> {
                        auth.signOut()

                        Toast.makeText(
                            this,
                            "Please sign in again and choose the correct role.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .addOnFailureListener { error ->
                if (isFinishing || isDestroyed) {
                    return@addOnFailureListener
                }

                Toast.makeText(
                    this,
                    "Could not restore your session: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return if (
            navController.currentDestination?.id == R.id.loginFragment
        ) {
            navController.navigate(R.id.roleSelectionFragment)
            true
        } else {
            navController.navigateUp(appBarConfiguration) ||
                    super.onSupportNavigateUp()
        }
    }
}