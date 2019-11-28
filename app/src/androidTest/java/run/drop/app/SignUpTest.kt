package run.drop.app

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Matchers.not

@RunWith(AndroidJUnit4::class)
@LargeTest
class SignUpTest {

    @get:Rule
    var activityRule: ActivityTestRule<SignUpActivity>
            = ActivityTestRule(SignUpActivity::class.java)

    @Test
    fun emptyFieldsToast() {
        onView(withId(R.id.new_account_button)).perform(click())
        onView(withText("Missing fields"))
                .inRoot(withDecorView(not(activityRule.activity.window.decorView)))
                .check(matches(isDisplayed()))
    }

    @Test
    fun duplicateCredentials() {
        onView(withId(R.id.email))
                .perform(typeText("gauthier.cler@gmail.com"))
        onView(withId(R.id.username))
                .perform(typeText("Gauthier"))
        onView(withId(R.id.password))
                .perform(typeText("123"), closeSoftKeyboard())
        onView(withId(R.id.confirmed_password))
                .perform(typeText("123"), closeSoftKeyboard())
        onView(withId(R.id.new_account_button)).perform(click())
        onView(withText("Email address or username already exists"))
                .inRoot(withDecorView(not(activityRule.activity.window.decorView)))
                .check(matches(isDisplayed()))
    }

    fun notMatchingPassword() {
        onView(withId(R.id.email))
                .perform(typeText(java.util.UUID.randomUUID().toString()))
        onView(withId(R.id.username))
                .perform(typeText(java.util.UUID.randomUUID().toString()))
        onView(withId(R.id.password))
                .perform(typeText(java.util.UUID.randomUUID().toString()))
        onView(withId(R.id.confirmed_password))
                .perform(typeText(java.util.UUID.randomUUID().toString()), closeSoftKeyboard())
        onView(withId(R.id.new_account_button)).perform(click())
        onView(withText("Password confirmation failed"))
                .inRoot(withDecorView(not(activityRule.activity.window.decorView)))
                .check(matches(isDisplayed()))
    }
}