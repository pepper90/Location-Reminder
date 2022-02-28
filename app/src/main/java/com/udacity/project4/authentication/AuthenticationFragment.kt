package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.FragmentAuthenticationBinding

class AuthenticationFragment : Fragment() {

    private lateinit var binding: FragmentAuthenticationBinding
    private lateinit var registerForActivityResult: ActivityResultLauncher<Intent>
    private val viewModel by viewModels<AuthenticationViewModel>()

    companion object {
        const val TAG = "AuthenticationFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_authentication, container, false)

        registerForSignInResult()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loginBtn.setOnClickListener {
            launchSignInFlow()
        }

        viewModel.authenticationState.observe(viewLifecycleOwner) { authenticationState ->
            when (authenticationState) {
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> {
                    val action = AuthenticationFragmentDirections.actionAuthenticationFragmentToReminderListFragment()
                    findNavController().navigate(action)
                    Log.i(
                        TAG,
                        "User ${FirebaseAuth.getInstance().currentUser?.displayName} signed in,Should go to RemindersActivity"
                    )
                }
                else -> Log.i(TAG, "Error or not authenticated")
            }

        }

//        popUpBackStack()
    }

    private fun registerForSignInResult() {
        registerForActivityResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ result: ActivityResult ->
            val response = IdpResponse.fromResultIntent(result.data)
            // Listen to the result of the sign - in process
            if(result.resultCode == Activity.RESULT_OK){
                Log.i(TAG, "User ${FirebaseAuth.getInstance().currentUser?.displayName} has signed in")
            } else {
                Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            }
        }
    }

    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
        )

        registerForActivityResult.launch(AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build())
    }
}