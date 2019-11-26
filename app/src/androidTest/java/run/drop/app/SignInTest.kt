package run.drop.app

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.runner.AndroidJUnit4
import run.drop.app.authFragments.SignInFragment

@RunWith(AndroidJUnit4::class)
class SignInTest {

    private lateinit var stringToBetyped: String
    private lateinit var scenario: FragmentScenario<SignInFragment>

    @Before
    fun init() {
        scenario = launchFragmentInContainer(null, R.style.AppTheme)

        // Specify a valid string.
        stringToBetyped = java.util.UUID.randomUUID().toString()
    }

    @Test
    fun changeText_sameActivity() {
        // Type text and then press the button.
        onView(withId(R.id.email))
                .perform(typeText(stringToBetyped), closeSoftKeyboard())

        // Check that the text was changed.
        onView(withId(R.id.email))
                .check(matches(withText(stringToBetyped)))
    }

    @Test
    fun emptyFieldsToast() {
        onView(withId(R.id.sign_in_button)).perform(click())
        onView(withId(R.id.email))
                .check(matches(hasErrorText("Can not be empty")))
    }

    @Test
    fun wrongEmail() {
        onView(withId(R.id.email))
                .perform(typeText(java.util.UUID.randomUUID().toString()), closeSoftKeyboard())
        onView(withId(R.id.password))
                .perform(typeText(java.util.UUID.randomUUID().toString()), closeSoftKeyboard())
        onView(withId(R.id.sign_in_button)).perform(click())
        onView(withId(R.id.email))
                .check(matches(hasErrorText("Wrong email")))
    }

    @Test
    fun correctCredentials() {
        onView(withId(R.id.email))
                .perform(typeText("gauthier.cler@gmail.com"), closeSoftKeyboard())
        onView(withId(R.id.password))
                .perform(typeText("123"), closeSoftKeyboard())
        onView(withId(R.id.sign_in_button)).perform(click())
    }
}
