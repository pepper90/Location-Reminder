# Location Reminder

A Todo list app with location reminders that remind the user to do something when he reaches a specific location. The app will require the user to create an account and login to set and access reminders.

The app has the following functionalities: 
- Login screen to ask users to login using an email address or a Google account. Upon successful login, the user navigates to the Reminders screen. If there is no account, the app navigates to a Register screen.  
- Register screen which allows an user to register using an email address or a Google account.
- A screen that displays the reminders retrieved from local storage. If there are no reminders, "No Data"  indicator is shown.  If there are any errors, an error message appears.
- A screen that shows a map with the user's current location and asks the user to select a point of interest to create a reminder.
- A screen which adds a reminder when the user reaches the selected location.  Each reminder includes
    a. title
    b. description
    c. selected location
- Reminder data is saved to local storage.
- For each reminder, a geofencing request is created in the background that fires up a notification when the user enters the geofencing area.
- Provided tests for the ViewModels, Coroutines and LiveData objects.
- Created FakeDataSource which replace the Data Layer and tests the app in isolation.
- Uses Espresso and Mockito to test each screen of the app:
    a. Added testing of DAO (Data Access Object) and Repository classes.
    b. Added testing for the error messages.
    c. Added End-To-End testing for the Fragments navigation.


## Built With

* [Koin](https://github.com/InsertKoinIO/koin) - A pragmatic lightweight dependency injection framework for Kotlin.
* [FirebaseUI Authentication](https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md) - FirebaseUI provides a drop-in auth solution that handles the UI flows for signing
* [JobIntentService](https://developer.android.com/reference/androidx/core/app/JobIntentService) - Run background service from the background application, Compatible with >= Android O.
