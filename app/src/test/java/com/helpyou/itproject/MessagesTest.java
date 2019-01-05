package com.helpyou.itproject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MessagesTest {

    Messages messages;
    @Before
    public void setUp() throws Exception {
        messages = new Messages("message", "User45", "USER42", "4:32pm", "TextMessage", "true");
    }

    @Test
    public void test1(){
        messages.setMessage("");
        assertEquals(messages.getMessage(), "");
    }
}