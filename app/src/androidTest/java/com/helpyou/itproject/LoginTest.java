package com.helpyou.itproject;

import android.app.Activity;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.rule.*;
import android.support.test.*;
import android.support.test.runner.AndroidJUnit4;

import static android.support.test.espresso.Espresso.onView;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(AndroidJUnit4.class)
public class LoginTest {

    @Rule
    public ActivityTestRule<Login> loginActivityTestRule = new ActivityTestRule<>(Login.class);

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testSendCodeAndVerify() throws InterruptedException {
        onView(withId(R.id.phoneText)).perform(replaceText("414938980"));
        onView(withId(R.id.sendButton)).perform(click());
        Thread.sleep(3000);
        onView(withId(R.id.codeText)).perform(replaceText("123456"));
        //Need to add SHA1 in firebase console for this device as this app is still in debug mode
        //This app won't run without it
        onView(withId(R.id.verify)).perform(click());
        //Thread.sleep(1000);
    }


    @After
    public void tearDown() throws Exception {
    }
}