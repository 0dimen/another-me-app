package app.anotherme.demo

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.anotherme.demo.data.EntrustedTask
import app.anotherme.demo.data.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class DemoFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun capture(name: String) {
        compose.waitForIdle()
        val dir = File(compose.activity.filesDir, "verification")
        dir.mkdirs()
        FileOutputStream(File(dir, "$name.png")).use {
            // Capture the actual display, including dialogs and separately composed layers.
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun threeModesAndTwoStyles() {
        compose.onNodeWithText("펼치기").performClick()
        compose.onNodeWithText("전체 화면").assertExists().performClick()
        compose.onNodeWithText("+ 맡긴 일 추가").assertExists()
        compose.onNodeWithText("스타일 A · 새틴 바").performClick()
        compose.onNodeWithText("스타일 B · 다크 키").assertExists()
        capture("full-dark")
        compose.onNodeWithText("스타일 B · 다크 키").performClick()
        compose.onNodeWithText("접기").performClick()
        compose.onNodeWithText("+ 맡긴 일 추가").assertDoesNotExist()
        capture("min")
    }

    @Test fun inputIsLockedWhileDriving() {
        compose.onNodeWithText("맡긴 일").performClick()
        compose.onNodeWithText("P · 주차").performClick()
        compose.onNodeWithText("+ 맡긴 일 추가").assertIsNotEnabled()
        compose.onNodeWithText("D · 주행").performClick()
        compose.onNodeWithText("+ 맡긴 일 추가").assertIsEnabled()
    }

    @Test fun replayWaitsAndRequiresSeparateConsent() {
        compose.onNodeWithText("펼치기").performClick()
        compose.onNodeWithText("14:48 상황 재생").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("통화에 집중하세요.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("보내 줘").assertDoesNotExist()
        capture("waiting-call")
        compose.waitUntil(6000) { compose.onAllNodesWithText("길 안내가 끝난 뒤에\n필요한 도움을 전할게요.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("보내 줘").assertDoesNotExist()
        capture("waiting-guidance")
        compose.waitUntil(6000) { compose.onAllNodesWithText("보내 줘").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("대현님께 도착 예정 시간을\n문자로 보낼까요?").assertExists()
        compose.onNodeWithText("시연: 실제 발송 안 됨").assertExists()
        capture("mid-alert")
        compose.onNodeWithText("전체 화면").performClick()
        capture("full-alert")
        compose.onNodeWithText("보내 줘").performClick()
        compose.onNodeWithText("동의를 확인했어요. 시연이므로 문자는 보내지 않았어요.").assertExists()
    }

    @Test fun datastorePersistsUpdatesAndDeletionAcrossRepositoryInstances() = runBlocking {
        val context = compose.activity.applicationContext
        val task = EntrustedTask(id = "instrumentation-task", destination = "시연 목적지", recipient = "예시 상대", request = "준비물 확인")
        val repo = TaskRepository(context)
        repo.save(task)
        assertTrue(TaskRepository(context).tasks.first().contains(task))
        repo.save(task.copy(request = "수정한 부탁"))
        assertEquals("수정한 부탁", TaskRepository(context).tasks.first().single { it.id == task.id }.request)
        repo.delete(task.id)
        assertFalse(TaskRepository(context).tasks.first().any { it.id == task.id })
    }

    @Test fun createConfirmEditRestartAndDeleteTask() {
        compose.onNodeWithText("맡긴 일").performClick()
        compose.onNodeWithText("+ 맡긴 일 추가").performClick()
        compose.onNodeWithText("내용 확인").assertIsNotEnabled()
        compose.onNodeWithText("목적지 · 지점 또는 주소").performTextInput("시연역 1번 출구")
        compose.onNodeWithText("연락 상대").performTextInput("예시 상대")
        compose.onNodeWithText("부탁").performTextInput("시연용 준비물 챙기기")
        compose.onNodeWithText("내용 확인").performScrollTo().performClick()
        compose.onNodeWithText("맡길 내용을 확인해 주세요").assertExists()
        capture("task-confirm")
        compose.onNodeWithText("확인하고 저장").performScrollTo().performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("시연용 준비물 챙기기").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("수정").onFirst().performScrollTo().performClick()
        compose.onNodeWithText("부탁").performTextReplacement("시연용 수정한 부탁")
        compose.onNodeWithText("내용 확인").performScrollTo().performClick()
        compose.onNodeWithText("확인하고 저장").performScrollTo().performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("시연용 수정한 부탁").fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("맡긴 일").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("시연용 수정한 부탁").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("삭제").onFirst().performScrollTo().performClick()
        compose.onNodeWithText("맡긴 일을 삭제할까요?").assertExists()
        compose.onAllNodesWithText("삭제").onLast().performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("시연용 수정한 부탁").fetchSemanticsNodes().isEmpty() }
    }
}
