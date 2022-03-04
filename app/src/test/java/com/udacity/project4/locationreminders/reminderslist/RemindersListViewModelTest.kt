package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
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

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
class RemindersListViewModelTest {
    private lateinit var viewModel: RemindersListViewModel
    private lateinit var dataSource: FakeDataSource

    private val reminder1 = ReminderDTO("Reminder1", "Description1", "Location1", 10.0, 10.0)
    private val reminder2 = ReminderDTO("Reminder2", "Description2", "Location2", 20.0, 20.0)
    private val remindersList = mutableListOf<ReminderDTO>()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setUpViewModel(){
        stopKoin()
        dataSource = FakeDataSource(remindersList)
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)
    }

    @Test
    fun loadReminders_addToList() = mainCoroutineRule.runBlockingTest{
        remindersList.add(reminder1)
        remindersList.add(reminder2)

        // Pause the dispatcher so that you can verify initial values. This way, inside the loadReminders(()
        // only the  _dataLoading.value = true
        mainCoroutineRule.pauseDispatcher()

        viewModel.loadReminders()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true) )

        mainCoroutineRule.resumeDispatcher()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(viewModel.showNoData.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun noReminders_showNoData() = mainCoroutineRule.runBlockingTest {
        dataSource.deleteAllReminders()
        viewModel.loadReminders()
        assertThat(viewModel.showNoData.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun unavailableReminders_showErrorMessage() = mainCoroutineRule.runBlockingTest {
        dataSource.setShouldReturnError(true)
        viewModel.loadReminders()
        assertThat(viewModel.showSnackBar.value, `is`("Reminders not found"))
    }

}