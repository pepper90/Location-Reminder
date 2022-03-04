@file:Suppress("USELESS_CAST")

package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.util.DataBindingIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : KoinTest {

    private lateinit var repository: ReminderDataSource
    private val dataBindingIdlingResource = DataBindingIdlingResource()
    val reminder = ReminderDTO("title", "description", "location", 10.0, 10.0)


    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initializeRepository() {
        stopKoin()

        val module = module {
            viewModel {
                RemindersListViewModel(getApplicationContext(), get() as ReminderDataSource)
            }

            // This needs to be manually casted (even though it seems useless)
            single { RemindersLocalRepository(get()) as ReminderDataSource}
            single { LocalDB.createRemindersDao(getApplicationContext()) }
        }

        startKoin {
            androidContext(getApplicationContext())
            modules(listOf(module))
        }

        repository = get()

        runBlocking {
            repository.deleteAllReminders()
        }
    }

    private fun DataBindingIdlingResource.monitorReminderListFragment(fragmentScenario: FragmentScenario<ReminderListFragment>) {
        fragmentScenario.onFragment { fragment ->
            activity = fragment.requireActivity()
        }
    }

    @Test
    fun showRemindersInFragment() = runBlockingTest {
        // GIVEN a reminder
        runBlocking {
            repository.saveReminder(reminder)
        }

        // WHEN Launching the fragment
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        dataBindingIdlingResource.monitorReminderListFragment(scenario)


        // THEN the reminder is displayed
        onView(withId(R.id.noDataTextView)).check(matches(not(isDisplayed())))
        onView(withText(reminder.title)).check(matches(withText("title")))
        onView(withText(reminder.description)).check(matches(withText("description")))
        onView(withText(reminder.location)).check(matches(withText("location")))

    }

    @Test
    fun noReminders_showsEmptyData() {
        // GIVEN delete all reminders
        runBlocking {
            repository.deleteAllReminders()
        }
        // WHEN Launching the fragment
        launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        // THEN no data textview is displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun fabClick_navigatesToSaveReminderFragment() {
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN clicking on the fab
        onView(withId(R.id.addReminderFAB)).perform(click())
        // THEN we navigate to SaveReminderFragment
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

}