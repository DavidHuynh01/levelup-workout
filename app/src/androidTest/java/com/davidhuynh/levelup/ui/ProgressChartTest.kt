package com.davidhuynh.levelup.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidhuynh.levelup.domain.logic.ProgressPoint
import com.davidhuynh.levelup.domain.model.WeightUnit
import com.davidhuynh.levelup.ui.progress.ProgressChart
import com.davidhuynh.levelup.ui.theme.LevelUpTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ProgressChartTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun point(day: Int, estimateKg: Double) = ProgressPoint(
        date = LocalDate.of(2026, 3, day),
        estimated1rmKg = estimateKg,
        volumeKg = estimateKg * 20,
        bestSetReps = 5,
        bestSetWeightKg = estimateKg * 0.9,
    )

    @Test
    fun chartShowsItsTitleSessionCountAndLatestEstimate() {
        composeTestRule.setContent {
            LevelUpTheme {
                ProgressChart(
                    points = listOf(
                        point(2, 100.0),
                        point(9, 105.0),
                        point(16, 110.0),
                    ),
                    unit = WeightUnit.KG,
                )
            }
        }

        composeTestRule.onNodeWithText("Estimated 1RM").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 sessions").assertIsDisplayed()
        composeTestRule.onNodeWithText("110 kg").assertIsDisplayed()
    }

    @Test
    fun chartRendersNothingForASinglePoint() {
        composeTestRule.setContent {
            LevelUpTheme {
                Column {
                    Text("chart probe")
                    ProgressChart(points = listOf(point(2, 100.0)), unit = WeightUnit.KG)
                }
            }
        }

        composeTestRule.onNodeWithText("chart probe").assertIsDisplayed()
        composeTestRule.onNodeWithText("Estimated 1RM").assertDoesNotExist()
    }

    @Test
    fun chartRendersNothingForAnEmptySeries() {
        composeTestRule.setContent {
            LevelUpTheme {
                Column {
                    Text("chart probe")
                    ProgressChart(points = emptyList(), unit = WeightUnit.KG)
                }
            }
        }

        composeTestRule.onNodeWithText("chart probe").assertIsDisplayed()
        composeTestRule.onNodeWithText("Estimated 1RM").assertDoesNotExist()
    }

    @Test
    fun chartCallsOutAGainForARisingSeries() {
        composeTestRule.setContent {
            LevelUpTheme {
                ProgressChart(
                    points = listOf(
                        point(2, 100.0),
                        point(9, 105.0),
                        point(16, 110.0),
                    ),
                    unit = WeightUnit.KG,
                )
            }
        }

        composeTestRule.onNodeWithText("up 10 kg since the start").assertIsDisplayed()
        composeTestRule.onNodeWithText("down 10 kg since the start").assertDoesNotExist()
    }

    @Test
    fun chartCallsOutALossAndKeepsTheBestForAFallingSeries() {
        composeTestRule.setContent {
            LevelUpTheme {
                ProgressChart(
                    points = listOf(
                        point(2, 110.0),
                        point(9, 105.0),
                        point(16, 100.0),
                    ),
                    unit = WeightUnit.KG,
                )
            }
        }

        composeTestRule.onNodeWithText("down 10 kg since the start").assertIsDisplayed()
        composeTestRule.onNodeWithText("100 kg").assertIsDisplayed()
        composeTestRule.onNodeWithText("best 110 kg").assertIsDisplayed()
    }

    @Test
    fun chartReportsNoChangeForAFlatSeries() {
        composeTestRule.setContent {
            LevelUpTheme {
                ProgressChart(
                    points = listOf(point(2, 100.0), point(16, 100.0)),
                    unit = WeightUnit.KG,
                )
            }
        }

        composeTestRule.onNodeWithText("2 sessions").assertIsDisplayed()
        composeTestRule.onNodeWithText("no change since the start").assertIsDisplayed()
    }

    @Test
    fun chartConvertsTheLatestEstimateIntoPounds() {
        composeTestRule.setContent {
            LevelUpTheme {
                ProgressChart(
                    points = listOf(point(2, 45.359237), point(16, 90.718474)),
                    unit = WeightUnit.LB,
                )
            }
        }

        composeTestRule.onNodeWithText("200 lb").assertIsDisplayed()
        composeTestRule.onNodeWithText("up 100 lb since the start").assertIsDisplayed()
    }
}
