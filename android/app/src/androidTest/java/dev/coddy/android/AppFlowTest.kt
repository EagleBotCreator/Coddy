package dev.coddy.android
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

/** Runs only in an isolated test install. Uses synthetic sample content. */
class AppFlowTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 @Test fun emptyScreensAndReviewedProposalRemainSeparateFromMemory(){
  compose.waitUntil(15000){compose.onAllNodesWithText("Your conversation").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Memory",useUnmergedTree=true).performClick()
  compose.onNodeWithText("Bring your context along").assertExists()
  compose.onNodeWithText("Proposals · 0").performClick()
  compose.onNodeWithText("New proposal").performClick()
  compose.onNode(hasSetTextAction() and hasText("Title")).performTextInput("Example preference")
  compose.onNode(hasSetTextAction() and hasText("Proposed memory")).performTextInput("Use short paragraphs for this synthetic test.")
  compose.onNodeWithText("Save proposal").performClick()
  compose.onNodeWithText("Example preference").assertExists()
  compose.onNodeWithText("Review proposal").performScrollTo().performClick()
  compose.onNodeWithText("Mark reviewed").performClick()
  compose.onNodeWithText("Reviewed for export").assertExists()
  compose.onNodeWithText("Snapshot").performClick()
  compose.onNodeWithText("No snapshot imported yet").assertExists()
  compose.onNodeWithText("Connect",useUnmergedTree=true).performClick()
  compose.onNodeWithText("No model loaded").assertExists()
  compose.onNodeWithText("Chat",useUnmergedTree=true).performClick()
  compose.onNode(hasSetTextAction() and hasText("Message Coddy")).performTextInput("Hello")
  compose.onNodeWithContentDescription("Send message").assertIsNotEnabled()
 }
}
