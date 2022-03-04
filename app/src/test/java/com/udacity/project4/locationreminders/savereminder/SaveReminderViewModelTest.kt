package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext.stopKoin
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class SaveReminderViewModelTest {

    @get: Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get: Rule
    var mainCoroutineRule = MainCoroutineRule()
    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var datasource: FakeDataSource

    @Before
    fun setUp() {
        stopKoin()
        datasource = FakeDataSource()
        saveReminderViewModel =
            SaveReminderViewModel(ApplicationProvider.getApplicationContext(), datasource)

    }

    @Test
    fun check_loading() = mainCoroutineRule.runBlockingTest {
        mainCoroutineRule.pauseDispatcher()
        val reminderDataItem = ReminderDataItem(
            "title",
            "description",
            "location",
            0.0,
            0.0
        )
        saveReminderViewModel.saveReminder(
            reminderDataItem
        )
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun shouldReturnError() = mainCoroutineRule.runBlockingTest {
        val reminderDataItem = ReminderDataItem(
            "",
            "description",
            "location",
            0.0,
            0.0
        )
        val isDataValid = saveReminderViewModel.validateEnteredData(reminderDataItem)
        assertThat(isDataValid, `is`(false))
        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_enter_title)
        )
    }
}