package com.davidhuynh.levelup.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidhuynh.levelup.ui.common.ConfirmDialog
import com.davidhuynh.levelup.ui.common.CountBadge
import com.davidhuynh.levelup.ui.common.EmptyState
import com.davidhuynh.levelup.ui.common.ErrorBanner
import com.davidhuynh.levelup.ui.common.LevelUpTextField
import com.davidhuynh.levelup.ui.common.PasswordField
import com.davidhuynh.levelup.ui.common.StatCard
import com.davidhuynh.levelup.ui.theme.LevelUpTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommonComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun errorBannerShowsItsMessage() {
        composeTestRule.setContent {
            LevelUpTheme {
                ErrorBanner(message = "That email is already taken")
            }
        }

        composeTestRule.onNodeWithText("That email is already taken").assertIsDisplayed()
    }

    @Test
    fun errorBannerDisappearsOnceTheMessageClears() {
        var message by mutableStateOf<String?>("Could not reach the server")

        composeTestRule.setContent {
            LevelUpTheme {
                ErrorBanner(message = message)
            }
        }

        composeTestRule.onNodeWithText("Could not reach the server").assertIsDisplayed()

        composeTestRule.runOnIdle { message = null }

        composeTestRule.onNodeWithText("Could not reach the server").assertDoesNotExist()
    }

    @Test
    fun errorBannerIgnoresABlankMessage() {
        var message by mutableStateOf<String?>("Wrong password")

        composeTestRule.setContent {
            LevelUpTheme {
                Column {
                    Text("banner probe")
                    ErrorBanner(message = message)
                }
            }
        }

        composeTestRule.onNodeWithText("Wrong password").assertIsDisplayed()

        composeTestRule.runOnIdle { message = "   " }

        composeTestRule.onNodeWithText("banner probe").assertIsDisplayed()
        composeTestRule.onNodeWithText("   ").assertDoesNotExist()
    }

    @Test
    fun countBadgeHidesAtZero() {
        composeTestRule.setContent {
            LevelUpTheme {
                Column {
                    Text("badge probe")
                    CountBadge(count = 0)
                }
            }
        }

        composeTestRule.onNodeWithText("badge probe").assertIsDisplayed()
        composeTestRule.onNodeWithText("0").assertDoesNotExist()
    }

    @Test
    fun countBadgeShowsSmallCountsAndCapsAboveNine() {
        composeTestRule.setContent {
            LevelUpTheme {
                Row {
                    CountBadge(count = 3)
                    CountBadge(count = 9)
                    CountBadge(count = 12)
                }
            }
        }

        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithText("9").assertIsDisplayed()
        composeTestRule.onNodeWithText("9+").assertIsDisplayed()
        composeTestRule.onNodeWithText("12").assertDoesNotExist()
    }

    @Test
    fun emptyStateShowsItsTitleBodyAndAction() {
        var tapped = false

        composeTestRule.setContent {
            LevelUpTheme {
                EmptyState(
                    emoji = "🏋",
                    title = "No workouts yet",
                    body = "Log your first session to start a streak",
                    action = {
                        Button(onClick = { tapped = true }) { Text("Log a workout") }
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("No workouts yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Log your first session to start a streak").assertIsDisplayed()
        composeTestRule.onNodeWithText("Log a workout").performClick()

        composeTestRule.runOnIdle { assertTrue(tapped) }
    }

    @Test
    fun confirmDialogFiresConfirmWhenTheActionIsPressed() {
        var confirmed = false
        var dismissed = false

        composeTestRule.setContent {
            LevelUpTheme {
                ConfirmDialog(
                    title = "Delete this workout?",
                    body = "Your sets for this session will be gone for good",
                    confirmLabel = "Delete",
                    onConfirm = { confirmed = true },
                    onDismiss = { dismissed = true },
                    destructive = true,
                )
            }
        }

        composeTestRule.onNodeWithText("Delete this workout?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your sets for this session will be gone for good")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").performClick()

        composeTestRule.runOnIdle {
            assertTrue(confirmed)
            assertFalse(dismissed)
        }
    }

    @Test
    fun confirmDialogFiresDismissFromCancel() {
        var confirmed = false
        var dismissed = false

        composeTestRule.setContent {
            LevelUpTheme {
                ConfirmDialog(
                    title = "Remove this friend?",
                    body = "You will stop seeing each other on the leaderboard",
                    confirmLabel = "Remove",
                    onConfirm = { confirmed = true },
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel").performClick()

        composeTestRule.runOnIdle {
            assertTrue(dismissed)
            assertFalse(confirmed)
        }
    }

    @Test
    fun passwordFieldTogglesBetweenShowAndHide() {
        composeTestRule.setContent {
            LevelUpTheme {
                PasswordField(value = "hunterpass", onValueChange = {}, label = "Password")
            }
        }

        composeTestRule.onNodeWithText("Show").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hide").assertDoesNotExist()

        composeTestRule.onNodeWithText("Show").performClick()

        composeTestRule.onNodeWithText("Hide").assertIsDisplayed()
        composeTestRule.onNodeWithText("Show").assertDoesNotExist()

        composeTestRule.onNodeWithText("Hide").performClick()

        composeTestRule.onNodeWithText("Show").assertIsDisplayed()
    }

    @Test
    fun passwordFieldRevealsTheValueOnlyWhileVisible() {
        composeTestRule.setContent {
            LevelUpTheme {
                PasswordField(value = "hunterpass", onValueChange = {}, label = "Password")
            }
        }

        composeTestRule.onNodeWithText("•".repeat("hunterpass".length)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Show password").assertIsDisplayed()

        composeTestRule.onNodeWithText("Show").performClick()

        composeTestRule.onNodeWithText("hunterpass").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Hide password").assertIsDisplayed()

        composeTestRule.onNodeWithText("Hide").performClick()

        composeTestRule.onNodeWithText("•".repeat("hunterpass".length)).assertIsDisplayed()
    }

    @Test
    fun textFieldShowsAnErrorInPlaceOfItsSupportingText() {
        var error by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            LevelUpTheme {
                LevelUpTextField(
                    value = "david",
                    onValueChange = {},
                    label = "Email",
                    error = error,
                    supportingText = "We only use this to sign you in",
                )
            }
        }

        composeTestRule.onNodeWithText("We only use this to sign you in").assertIsDisplayed()

        composeTestRule.runOnIdle { error = "Enter a valid email" }

        composeTestRule.onNodeWithText("Enter a valid email").assertIsDisplayed()
        composeTestRule.onNodeWithText("We only use this to sign you in").assertDoesNotExist()
    }

    @Test
    fun textFieldReportsWhatWasTyped() {
        var typed = ""

        composeTestRule.setContent {
            LevelUpTheme {
                var text by remember { mutableStateOf("") }
                LevelUpTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        typed = it
                    },
                    label = "Email",
                )
            }
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("david@test.com")

        composeTestRule.runOnIdle { assertEquals("david@test.com", typed) }
    }

    @Test
    fun statCardUppercasesItsLabelAndShowsValueAndCaption() {
        composeTestRule.setContent {
            LevelUpTheme {
                StatCard(label = "Streak", value = "12 days", caption = "best ever 19")
            }
        }

        composeTestRule.onNodeWithText("STREAK").assertIsDisplayed()
        composeTestRule.onNodeWithText("12 days").assertIsDisplayed()
        composeTestRule.onNodeWithText("best ever 19").assertIsDisplayed()
        composeTestRule.onNodeWithText("Streak").assertDoesNotExist()
    }
}
