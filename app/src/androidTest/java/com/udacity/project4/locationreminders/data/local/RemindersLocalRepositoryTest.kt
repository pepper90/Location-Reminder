package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var repository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    private val newReminder = ReminderDTO("title", "description", "location", 1.1, 2.2)

    @get:Rule
    var instantExecutorRule= InstantTaskExecutorRule()

    @Before
    fun setupDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        repository =
            RemindersLocalRepository(
                database.reminderDao(),
                Dispatchers.Main
            )
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveTask_retrievesTask() = runBlocking {
        // GIVEN - A new reminder saved in the database.
        repository.saveReminder(newReminder)

        // WHEN  - Reminder retrieved by ID.
        val result = repository.getReminder(newReminder.id)

        // THEN - Same reminder is returned.
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success

        assertThat(result.data.title, `is`("title"))
        assertThat(result.data.description, `is`("description"))
        assertThat(result.data.location, `is`("location"))
        assertThat(result.data.latitude, `is`(1.1))
        assertThat(result.data.longitude, `is`(2.2))
    }

    @Test
    fun deleteReminders_returnsError() = runBlocking {
        // GIVEN - Delete all reminders from database.
        repository.deleteAllReminders()

        // WHEN  - Try to retrieve reminder by ID.
        val result = repository.getReminder(newReminder.id)

        // THEN - Return an error
        assertThat(result is Result.Error, `is`(true))
        result as Result.Error

        assertThat(result.message, `is`("Reminder not found!"))
    }
}