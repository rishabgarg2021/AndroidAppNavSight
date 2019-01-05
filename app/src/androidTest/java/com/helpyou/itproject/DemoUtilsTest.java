package com.helpyou.itproject;

import android.app.Activity;
import android.content.Context;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.exceptions.UnavailableException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DemoUtilsTest {
    private final String TAG  = "DemoUtilityTest";

    @Mock
    Context context;

    @Mock
    Activity activity;

    @Before
    public void setUp() throws Exception {
        activity = mock(Activity.class);
        assertNotEquals(activity, null);
    }

    @Test
    public void test1() throws UnavailableException {
        DemoUtils.createArSession(activity, false);
        when(ARLocationPermissionHelper.hasPermission(activity)).thenReturn(true);
        when(ArCoreApk.getInstance().requestInstall(activity, true)).thenReturn(ArCoreApk.InstallStatus.INSTALLED);
        when(ArCoreApk.getInstance().requestInstall(activity, false)).thenReturn(ArCoreApk.InstallStatus.INSTALLED);

    }

    @After
    public void tearDown() throws Exception {
    }
}