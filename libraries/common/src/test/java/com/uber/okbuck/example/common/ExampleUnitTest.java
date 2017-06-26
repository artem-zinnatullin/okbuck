package com.uber.okbuck.example.common;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 21, packageName = "com.uber.okbuck.example.common")
public class ExampleUnitTest {

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void resource_loading() {
        Context context = (Context) RuntimeEnvironment.application;
        assertEquals(context.getResources().getString(R.string.app_name), "Common");
        assertEquals(context.getResources().getColor(
            com.uber.okbuck.example.parcelable.R.color.fooColor), 0xcccccc);

    }
}
