package com.example.minisocialnetworkapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.minisocialnetworkapplication.ui.auth.AuthState
import com.example.minisocialnetworkapplication.ui.theme.MiniSocialNetworkApplicationTheme

/**
 * This file contains comprehensive UI previews for the entire application.
 * Use this to quickly see all screens and components in different states.
 */

// ========== AUTH SCREENS ==========

@Preview(name = "Login - Normal", showSystemUi = true)
@Composable
fun PreviewLoginNormal() {
    MiniSocialNetworkApplicationTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Login Screen Preview", style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = "test@example.com",
                    onValueChange = {},
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = "password",
                    onValueChange = {},
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Login")
                }
            }
        }
    }
}

@Preview(name = "Login - Loading", showSystemUi = true)
@Composable
fun PreviewLoginLoading() {
    MiniSocialNetworkApplicationTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Login Screen - Loading", style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = "test@example.com",
                    onValueChange = {},
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = "password",
                    onValueChange = {},
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Preview(name = "Login - Error", showSystemUi = true)
@Composable
fun PreviewLoginError() {
    MiniSocialNetworkApplicationTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Login Screen - Error", style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = "test@example.com",
                    onValueChange = {},
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = "wrong",
                    onValueChange = {},
                    label = { Text("Password") },
                    isError = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Login")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Invalid email or password",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ========== REGISTER SCREEN ==========

@Preview(name = "Register - Normal", showSystemUi = true)
@Composable
fun PreviewRegisterNormal() {
    MiniSocialNetworkApplicationTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Register Screen", style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = "John Doe",
                    onValueChange = {},
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = "john@example.com",
                    onValueChange = {},
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = "password123",
                    onValueChange = {},
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = "password123",
                    onValueChange = {},
                    label = { Text("Confirm Password") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Register")
                }
            }
        }
    }
}

// ========== PLACEHOLDERS ==========

@Preview(name = "Feed Placeholder", showSystemUi = true)
@Composable
fun PreviewFeedPlaceholder() {
    MiniSocialNetworkApplicationTheme {
        Surface {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(
                        text = "📱",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Feed Screen",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Coming in Week 2",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(name = "Compose Post Placeholder", showSystemUi = true)
@Composable
fun PreviewComposePostPlaceholder() {
    MiniSocialNetworkApplicationTheme {
        Surface {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(
                        text = "✍️",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Compose Post Screen",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Coming in Week 2",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ========== COMPONENTS ==========

@Preview(name = "Button States", showBackground = true)
@Composable
fun PreviewButtonStates() {
    MiniSocialNetworkApplicationTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Primary Button")
            }

            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Outlined Button")
            }

            TextButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Text Button")
            }

            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Disabled Button")
            }
        }
    }
}

@Preview(name = "Text Fields", showBackground = true)
@Composable
fun PreviewTextFields() {
    MiniSocialNetworkApplicationTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = "Normal text field",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = "Error text field",
                onValueChange = {},
                label = { Text("Label") },
                isError = true,
                supportingText = { Text("Error message") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Empty field") },
                placeholder = { Text("Placeholder text") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Loading States", showBackground = true)
@Composable
fun PreviewLoadingStates() {
    MiniSocialNetworkApplicationTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()

            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.secondary
            )

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

