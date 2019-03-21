package run.drop.app

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Matchers.not

@RunWith(AndroidJUnit4::class)
@LargeTest
class SignInTest {

    private lateinit var stringToBetyped: String

    @get:Rule
    var activityRule: ActivityTestRule<SignInActivity>
            = ActivityTestRule(SignInActivity::class.java)



    @Before
    fun initValidString() {
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
        onView(withText("Please enter an email and a password"))
                .inRoot(withDecorView(not(activityRule.activity.window.decorView)))
                .check(matches(isDisplayed()))
    }

    @Test
    fun wrongCredentials() {
        onView(withId(R.id.email))
                .perform(typeText(java.util.UUID.randomUUID().toString()))
        onView(withId(R.id.password))
                .perform(typeText(java.util.UUID.randomUUID().toString()), closeSoftKeyboard())
        onView(withId(R.id.sign_in_button)).perform(click())
        onView(withText("Wrong email or password"))
                .inRoot(withDecorView(not(activityRule.activity.window.decorView)))
                .check(matches(isDisplayed()))
    }

    @Test
    fun correctCredentials() {
        onView(withId(R.id.email))
                .perform(typeText("gauthier.cler@gmail.com"))
        onView(withId(R.id.password))
                .perform(typeText("123"), closeSoftKeyboard())
        onView(withId(R.id.sign_in_button)).perform(click())
    }
}