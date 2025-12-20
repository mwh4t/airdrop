package com.example.cp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // проверка отображения основных элементов интерфейса
    @Test
    fun testMainUIElementsDisplayed() {
        onView(withId(R.id.idValueText))
            .check(matches(isDisplayed()))
        onView(withId(R.id.fileSelectionCard))
            .check(matches(isDisplayed()))
        onView(withId(R.id.receiveButton))
            .check(matches(isDisplayed()))
        onView(withId(R.id.sendButton))
            .check(matches(isDisplayed()))
    }

    // проверка отображения кнопок в верхней панели
    @Test
    fun testTopBarButtonsDisplayed() {
        onView(withId(R.id.historyButton))
            .check(matches(isDisplayed()))
        onView(withId(R.id.logoutButton))
            .check(matches(isDisplayed()))
    }

    // проверка клика на кнопку истории
    @Test
    fun testHistoryButtonClick() {
        onView(withId(R.id.historyButton))
            .perform(click())
    }

    // проверка клика на карточку выбора файла
    @Test
    fun testFileSelectionCardClick() {
        onView(withId(R.id.fileSelectionCard))
            .perform(click())
    }

    // проверка клика на id для копирования
    @Test
    fun testIdClickToCopy() {
        Thread.sleep(3000)
        onView(withId(R.id.idValueText))
            .perform(click())
    }
}
