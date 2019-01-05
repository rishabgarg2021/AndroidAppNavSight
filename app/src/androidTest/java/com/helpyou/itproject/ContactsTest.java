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

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.anything;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ContactsTest {
    @Rule
    public ActivityTestRule<Contacts> mActivityRule = new ActivityTestRule<>(Contacts.class, false, false);

    @Before
    public void setUp() throws Exception {
        Intent intent = new Intent();
        intent.putExtra("userId", "DzXbB5EOkJOTdJmKltjVv0v1U8n1");
        intent.putExtra("phone", "+61406255366");
        mActivityRule.launchActivity(intent);
    }

    @Test
    public void completeTest() throws InterruptedException {

        onView(withId(R.id.action_reload)).perform(click());
        onView(withId(R.id.contactListView)).check(matches(isDisplayed()));
        //Thread.sleep(10000);
        //You need to have at least 1 registered contact to make this test pass
        onData(anything()).inAdapterView(withId(R.id.contactListView)).atPosition(0).perform(click());
    }
    @After
    public void tearDown() throws Exception {
    }
}