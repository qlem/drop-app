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
import run.drop.app.authFragments.SignUpFragment

@RunWith(AndroidJUnit4::class)
class SignUpTest {

    private lateinit var scenario: FragmentScenario<SignUpFragment>

    @Before
    fun init() {
        scenario = launchFragmentInContainer(null, R.style.AppTheme)
    }

    @Test
    fun emptyFieldsToast() {
        onView(withId(R.id.sign_up_button)).perform(click())
        onView(withId(R.id.username))
                .check(matches(hasErrorText("Can not be empty")))
    }

    @Test
    fun duplicateCredentials() {
        onView(withId(R.id.username))
                .perform(typeText("toto"), closeSoftKeyboard())
        onView(withId(R.id.email))
                .perform(typeText("toto@toto.toto"), closeSoftKeyboard())
        onView(withId(R.id.password))
                .perform(typeText("toto"), closeSoftKeyboard())
        onView(withId(R.id.confirmed_password))
                .perform(typeText("toto"), closeSoftKeyboard())
        onView(withId(R.id.sign_up_button)).perform(click())
        onView(withId(R.id.username))
                .check(matches(hasErrorText("Email address or username already exists")))
    }

    @Test
    fun notMatchingPassword() {
        onView(withId(R.id.email))
                .perform(typeText(java.util.UUID.randomUUID().toString()), closeSoftKeyboard())
        onView(withId(R.id.username))
                .perform(typeText(java.util.UUID.randomUUID().toString()), closeSoftKeyboard())
        onView(withId(R.id.password))
                .perform(typeText(java.util.UUID.randomUUID().toString()), closeSoftKeyboard())
        onView(withId(R.id.confirmed_password))
                .perform(typeText(java.util.UUID.randomUUID().toString()), closeSoftKeyboard())
        onView(withId(R.id.sign_up_button)).perform(click())
        onView(withId(R.id.confirmed_password))
                .check(matches(hasErrorText("Must be the same as the password")))
    }
}
