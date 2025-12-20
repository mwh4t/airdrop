package com.example.cp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthActivityUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(
        AuthActivity::class.java)

    // проверка отображения всех элементов интерфейса
    @Test
    fun testAllUIElementsDisplayed() {
        onView(withId(R.id.emailInput))
            .check(matches(isDisplayed()))
        onView(withId(R.id.passwordInput))
            .check(matches(isDisplayed()))
        onView(withId(R.id.loginButton))
            .check(matches(isDisplayed()))
        onView(withId(R.id.registerButton))
            .check(matches(isDisplayed()))
        onView(withId(R.id.googleButton))
            .check(matches(isDisplayed()))
    }

    // проверка ввода email в поле
    @Test
    fun testEmailInput() {
        onView(withId(R.id.emailInput))
            .perform(typeText("test@example.com"),
                closeSoftKeyboard())
    }

    // проверка ввода пароля в поле
    @Test
    fun testPasswordInput() {
        onView(withId(R.id.passwordInput))
            .perform(typeText("password123"),
                closeSoftKeyboard())
    }

    // проверка клика на кнопку входа
    @Test
    fun testLoginButtonClick() {
        onView(withId(R.id.loginButton))
            .perform(click())
    }

    // проверка клика на кнопку регистрации
    @Test
    fun testRegisterButtonClick() {
        onView(withId(R.id.registerButton))
            .perform(click())
    }
}
