package com.polypulse.app.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.polypulse.app.presentation.auth.AuthViewModel
import com.polypulse.app.presentation.auth.LoginScreen
import com.polypulse.app.presentation.auth.RegisterScreen

@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val state = viewModel.state.value

    if (state.isLoggedIn && state.user != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(text = "Email: ${state.user.email}")
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Member since: ${state.user.created_at}")
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(onClick = { viewModel.logout() }) {
                Text("Logout")
            }
        }
    } else {
        // If not logged in, we can show a landing for profile or redirect to login
        // For simplicity, let's just show Login Screen here directly or via navigation
        // But since we have separate routes for Login/Register, we should probably redirect
        // However, if Profile is a tab, it should host the content.
        
        // Let's make ProfileScreen act as a container that shows Login if not auth.
        // But LoginScreen has navigation callbacks.
        
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("You are not logged in")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateToLogin) {
                Text("Login")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onNavigateToRegister) {
                Text("Create Account")
            }
        }
    }
}
