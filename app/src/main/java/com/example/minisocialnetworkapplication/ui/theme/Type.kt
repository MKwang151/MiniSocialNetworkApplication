package com.example.minisocialnetworkapplication.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Preview(showBackground = true, name = "Typography Styles")
@Composable
fun TypographyPreview() {
    MiniSocialNetworkApplicationTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Display Large",
                    style = MaterialTheme.typography.displayLarge
                )

                Text(
                    text = "Headline Large",
                    style = MaterialTheme.typography.headlineLarge
                )

                Text(
                    text = "Title Large",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Title Medium",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Body Large - Default body text for paragraphs",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = "Body Medium - Secondary body text",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Body Small - Tertiary text",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Label Large - Button text",
                    style = MaterialTheme.typography.labelLarge
                )

                Text(
                    text = "Label Medium",
                    style = MaterialTheme.typography.labelMedium
                )

                Text(
                    text = "Label Small - Captions",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
