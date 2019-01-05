package com.helpyou.itproject;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class Main_Menu_MapTest {
    @Rule
    public ActivityTestRule<Main_Menu_Map> mActivityRule = new ActivityTestRule<>(Main_Menu_Map.class, false, false);

    @Before
    public void setUp() throws Exception {
        Intent intent = new Intent();
        intent.putExtra("userId", "DzXbB5EOkJOTdJmKltjVv0v1U8n1");
        intent.putExtra("phone", "+61406255366");
        mActivityRule.launchActivity(intent);
        mActivityRule.getActivity()
                .getSupportFragmentManager().beginTransaction();
    }

    @Test
    public void completeTest() throws InterruptedException {
        //Thread.sleep(100);
        onView(withId(R.id.map)).check(matches((isDisplayed())));
        onView(withId(R.id.volunteer)).check(matches(isDisplayed()));
        onView(withId(R.id.volunteer)).perform(click());
        onView(withId(R.id.volunteer)).check(matches(isChecked()));
    }

    @After
    public void tearDown() throws Exception {
    }
}